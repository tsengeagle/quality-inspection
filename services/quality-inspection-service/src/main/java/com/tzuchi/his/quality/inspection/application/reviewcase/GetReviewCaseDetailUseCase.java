package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.InitialReviewBootstrapView;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.ReviewSubmissionRefView;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.ReviewCaseDetailView;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.RereviewBootstrapView;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.SubmissionSummaryItem;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.ReviewCaseQueryPort;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
// D-055 第 5 點：准許完整初審讀取時會寫入讀取者紀錄並遞增案件版本，所以是可寫入的交易；
// 紀錄在回應送出前 commit，commit 失敗即拒絕這次讀取。
@Transactional
public class GetReviewCaseDetailUseCase {

    private final ReviewCaseQueryPort queryPort;

    private final ReviewReadAccessPolicy accessPolicy;
    private final CaseDetailMetadataMapper metadataMapper;

    public GetReviewCaseDetailUseCase(ReviewCaseQueryPort queryPort, ReviewReadAccessPolicy accessPolicy,
                                       CaseDetailMetadataMapper metadataMapper) {
        this.queryPort = queryPort;
        this.accessPolicy = accessPolicy;
        this.metadataMapper = metadataMapper;
    }

    public ReviewCaseDetailView execute(ActorContext actor, String caseId) {
        actor.requireAnyRole(
                DomainRole.HOSPITAL_MANAGER, DomainRole.REVIEWER,
                DomainRole.REREVIEWER, DomainRole.AUDITOR, DomainRole.SYSTEM_ADMIN);

        ReviewCaseDetailView raw = queryPort.findDetailById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("ReviewCase", caseId));

        accessPolicy.requireAccess(actor, raw);
        if (actor.hasRole(DomainRole.SYSTEM_ADMIN)) return metadataMapper.project(raw);
        if (actor.hasRole(DomainRole.REREVIEWER)) {
            raw = raw.withRestrictedContent(actor.getUserId(), false);
        }

        String projectionType = actor.hasRole(DomainRole.SYSTEM_ADMIN) ? "METADATA_ONLY" : "FULL";

        String actorRole;
        if (actor.hasRole(DomainRole.SYSTEM_ADMIN))           actorRole = "SYSTEM_ADMIN";
        else if (actor.hasRole(DomainRole.HOSPITAL_MANAGER))  actorRole = "HOSPITAL_MANAGER";
        else if (actor.hasRole(DomainRole.REREVIEWER))        actorRole = "REREVIEWER";
        else if (actor.hasRole(DomainRole.REVIEWER))          actorRole = "REVIEWER";
        else if (actor.hasRole(DomainRole.AUDITOR))           actorRole = "AUDITOR";
        else                                                   actorRole = "UNKNOWN";

        boolean isOwner = actor.getUserId().equals(raw.getReviewerId())
                       || actor.getUserId().equals(raw.getRereviewerId());

        InitialReviewBootstrapView initialReviewContext = actor.hasRole(DomainRole.SYSTEM_ADMIN) ? null : buildInitialReviewContext(actor, actorRole, raw);
        RereviewBootstrapView rereviewContext = actor.hasRole(DomainRole.SYSTEM_ADMIN) ? null : buildRereviewContext(actor, actorRole, raw);

        return raw.enrich(projectionType, actorRole, isOwner, initialReviewContext, rereviewContext);
    }

    private InitialReviewBootstrapView buildInitialReviewContext(ActorContext actor, String actorRole,
                                                                 ReviewCaseDetailView raw) {
        List<SubmissionSummaryItem> initialSubmissions = filterByStage(raw, "INITIAL");
        SubmissionSummaryItem latestDraft = findLatestByStatus(initialSubmissions, actor.getUserId(), "DRAFT");
        SubmissionSummaryItem latestSubmittedBase = findLatestByStatus(initialSubmissions, actor.getUserId(), "SUBMITTED");

        boolean hasDraft = latestDraft != null;
        boolean hasSubmittedBase = latestSubmittedBase != null;
        String blockedReason = resolveInitialBlockedReason(actor, actorRole, raw, hasSubmittedBase);
        boolean blocked = blockedReason != null;

        String pageMode;
        if (blocked) {
            pageMode = "READONLY_BLOCKED";
        } else if (hasSubmittedBase) {
            pageMode = "REVISION";
        } else if (hasDraft) {
            pageMode = "EDIT_DRAFT";
        } else {
            pageMode = "NEW_INITIAL_REVIEW";
        }

        ReviewSubmissionRefView currentSubmission = hasDraft
                ? toRef(latestDraft)
                : toRef(latestSubmittedBase);
        ReviewSubmissionRefView editableSubmission = hasDraft ? toRef(latestDraft) : null;

        return new InitialReviewBootstrapView(
                pageMode,
                hasDraft,
                hasSubmittedBase,
                blocked,
                blocked,
                blockedReason,
                raw.getScoreSheetVersionRef(),
                resolveInitialAllowedActions(pageMode, canFixInitialReview(raw, latestSubmittedBase)),
                currentSubmission,
                editableSubmission,
                toRef(latestSubmittedBase));
    }

    private RereviewBootstrapView buildRereviewContext(ActorContext actor, String actorRole,
                                                       ReviewCaseDetailView raw) {
        List<SubmissionSummaryItem> initialSubmissions = filterByStage(raw, "INITIAL");
        List<SubmissionSummaryItem> rereviewSubmissions = filterByStage(raw, "REREVIEW");

        SubmissionSummaryItem latestInitialSubmitted = findLatestByStatus(initialSubmissions, null, "SUBMITTED");
        SubmissionSummaryItem latestRereviewDraft = findLatestByStatus(rereviewSubmissions, actor.getUserId(), "DRAFT");
        SubmissionSummaryItem latestRereviewSubmitted = findLatestByStatus(rereviewSubmissions, actor.getUserId(), "SUBMITTED");

        boolean hasDraft = latestRereviewDraft != null;
        boolean hasSubmittedBase = latestRereviewSubmitted != null;
        // D-032 撤銷了 CLOSED 的例外修正：CLOSED 之後一律唯讀（CASE_ALREADY_CLOSED）。
        String blockedReason = resolveRereviewBlockedReason(actor, actorRole, raw, hasSubmittedBase);
        boolean blocked = blockedReason != null;

        String pageMode;
        if (blocked) {
            pageMode = "READONLY_BLOCKED";
        } else if (hasSubmittedBase) {
            pageMode = "REVISION";
        } else if (hasDraft) {
            pageMode = "EDIT_DRAFT";
        } else {
            pageMode = "NEW_REREVIEW";
        }

        ReviewSubmissionRefView currentSubmission = hasDraft
                ? toRef(latestRereviewDraft)
                : toRef(latestRereviewSubmitted);
        ReviewSubmissionRefView editableSubmission = hasDraft ? toRef(latestRereviewDraft) : null;

        return new RereviewBootstrapView(
                pageMode,
                hasDraft,
                hasSubmittedBase,
                false, // closedRevisionException：D-032 撤銷，欄位保留以維持回應格式
                blocked,
                blocked,
                blockedReason,
                raw.getScoreSheetVersionRef(),
                resolveRereviewAllowedActions(pageMode),
                currentSubmission,
                editableSubmission,
                toRef(latestRereviewSubmitted),
                latestInitialSubmitted != null ? latestInitialSubmitted.getSubmissionId() : raw.getLatestInitialSubmissionId(),
                latestInitialSubmitted != null ? latestInitialSubmitted.getRevisionNo() : null,
                latestInitialSubmitted != null ? latestInitialSubmitted.getReviewerId() : raw.getReviewerId(),
                null,
                latestInitialSubmitted != null ? latestInitialSubmitted.getTotalScore() : raw.getInitialReviewScore(),
                latestInitialSubmitted != null ? latestInitialSubmitted.getSubmittedAt() : null,
                raw.getFinalScore(),
                raw.getFinalConclusion());
    }

    private List<SubmissionSummaryItem> filterByStage(ReviewCaseDetailView raw, String stage) {
        return raw.getSubmissions().stream()
                .filter(s -> stage.equals(s.getStage()))
                .sorted(Comparator.comparingInt(SubmissionSummaryItem::getRevisionNo).reversed())
                .collect(Collectors.toList());
    }

    private SubmissionSummaryItem findLatestByStatus(List<SubmissionSummaryItem> submissions,
                                                     String reviewerId, String status) {
        return submissions.stream()
                .filter(s -> status.equals(s.getStatus()))
                .filter(s -> reviewerId == null || Objects.equals(reviewerId, s.getReviewerId()))
                .findFirst()
                .orElse(null);
    }

    private String resolveInitialBlockedReason(ActorContext actor, String actorRole,
                                               ReviewCaseDetailView raw, boolean hasSubmittedBase) {
        if (!"REVIEWER".equals(actorRole)) {
            return "ROLE_DENIED";
        }
        if (!Objects.equals(actor.getUserId(), raw.getReviewerId())) {
            return "NOT_ASSIGNED_REVIEWER";
        }
        if ("CLOSED".equals(raw.getStatus())) {
            return "CASE_ALREADY_CLOSED";
        }
        if ("CANCELLED".equals(raw.getStatus())) {
            return "CASE_ALREADY_CANCELLED";
        }
        // D-031、D-054：初審固定後不得再 revision，初審頁唯讀。
        if (raw.isInitialReviewFixed()) {
            return "INITIAL_REVIEW_FIXED";
        }
        if (!hasSubmittedBase && !List.of("PENDING_INITIAL_REVIEW", "INITIAL_REVIEW_IN_PROGRESS").contains(raw.getStatus())) {
            return "CASE_NOT_IN_INITIAL_REVIEW_STAGE";
        }
        return null;
    }

    private String resolveRereviewBlockedReason(ActorContext actor, String actorRole,
                                                ReviewCaseDetailView raw,
                                                boolean hasSubmittedBase) {
        if (!"REREVIEWER".equals(actorRole)) {
            return "ROLE_DENIED";
        }
        if (!Objects.equals(actor.getUserId(), raw.getRereviewerId())) {
            return "NOT_ASSIGNED_REREVIEWER";
        }
        if ("CANCELLED".equals(raw.getStatus())) {
            return "CASE_ALREADY_CANCELLED";
        }
        if ("CLOSED".equals(raw.getStatus())) {
            return "CASE_ALREADY_CLOSED";
        }
        if (!hasSubmittedBase && !List.of("PENDING_REREVIEW", "REREVIEW_IN_PROGRESS").contains(raw.getStatus())) {
            return "CASE_NOT_IN_REREVIEW_STAGE";
        }
        return null;
    }

    /**
     * D-054 第 9 點：案件為 INITIAL_REVIEW_COMPLETED、尚未固定，且登入者提交的最新初審結果就是案件目前有效的初審結果。
     * （登入者已確認是 REVIEWER、是本案指派醫師，否則 pageMode 已是 READONLY_BLOCKED。）
     */
    private boolean canFixInitialReview(ReviewCaseDetailView raw, SubmissionSummaryItem latestSubmittedBase) {
        return "INITIAL_REVIEW_COMPLETED".equals(raw.getStatus())
                && !raw.isInitialReviewFixed()
                && latestSubmittedBase != null
                && Objects.equals(latestSubmittedBase.getSubmissionId(), raw.getLatestInitialSubmissionId());
    }

    private List<String> resolveInitialAllowedActions(String pageMode, boolean canFix) {
        List<String> actions = new ArrayList<>();
        if ("NEW_INITIAL_REVIEW".equals(pageMode)) {
            actions.add("START_INITIAL_REVIEW");
        }
        if ("EDIT_DRAFT".equals(pageMode)) {
            actions.add("SAVE_INITIAL_REVIEW_DRAFT");
            actions.add("SUBMIT_INITIAL_REVIEW");
        }
        if ("REVISION".equals(pageMode)) {
            actions.add("REVISE_INITIAL_REVIEW");
            if (canFix) {
                actions.add("FIX_INITIAL_REVIEW");
            }
        }
        return actions;
    }

    private List<String> resolveRereviewAllowedActions(String pageMode) {
        List<String> actions = new ArrayList<>();
        if ("NEW_REREVIEW".equals(pageMode)) {
            actions.add("START_REREVIEW");
        }
        if ("EDIT_DRAFT".equals(pageMode)) {
            actions.add("SAVE_REREVIEW_DRAFT");
            actions.add("SUBMIT_REREVIEW");
        }
        if ("REVISION".equals(pageMode)) {
            // 已提交、未結案：可修訂，也可另行結案（D-032、D-034、D-053 第 8 點）
            actions.add("REVISE_REREVIEW");
            actions.add("CLOSE_REREVIEW");
        }
        return actions;
    }

    private ReviewSubmissionRefView toRef(SubmissionSummaryItem item) {
        if (item == null) {
            return null;
        }
        return new ReviewSubmissionRefView(
                item.getSubmissionId(),
                item.getRevisionNo(),
                item.getStatus(),
                item.getSubmittedAt());
    }
}
