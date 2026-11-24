package com.tzuchi.his.quality.inspection.application.audit;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.RereviewerHistoryQuery;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * D-071 第 1～3 點：ReviewCase 帶分數的 detail，依 D-055 第 6 點遮罩。
 * D-078：ReviewSubmission 的 DRAFT_SAVED、SUBMITTED 也帶分數，以所屬案件同樣判斷；找不到所屬案件時遮罩。
 * 依案件「目前」的狀態與複審指派歷史判斷；不寫讀取者紀錄，也不遞增案件版本。
 */
@Component
public class ScoredDetailMask {

    /** D-071 第 1 點。新增會帶分數的 action 時必須加進來。 */
    static final Set<String> SCORED_ACTIONS = Set.of(
            "INITIAL_REVIEW_COMPLETED", "CASE_ESCALATED_TO_REREVIEW", "REREVIEW_SUBMITTED",
            "REREVIEW_REVISED", "CASE_CLOSED_WITHOUT_REREVIEW");

    /** D-078 第 1 點：ReviewSubmission 帶分數的 action。 */
    static final Set<String> SCORED_SUBMISSION_ACTIONS = Set.of("DRAFT_SAVED", "SUBMITTED");

    private static final Set<CaseStatus> NO_FURTHER_REREVIEWER = EnumSet.of(
            CaseStatus.REREVIEW_IN_PROGRESS, CaseStatus.CLOSED, CaseStatus.CANCELLED);

    private final AuditCaseStateQuery caseStates;
    private final RereviewerHistoryQuery historyQuery;
    private final SubmissionCaseQuery submissionCases;

    public ScoredDetailMask(AuditCaseStateQuery caseStates, RereviewerHistoryQuery historyQuery,
                            SubmissionCaseQuery submissionCases) {
        this.caseStates = caseStates;
        this.historyQuery = historyQuery;
        this.submissionCases = submissionCases;
    }

    public List<AuditTrailEntry> apply(ActorContext actor, List<AuditTrailEntry> entries) {
        Set<String> submissionIds = entries.stream().filter(ScoredDetailMask::scoredSubmission)
                .map(AuditTrailEntry::getEntityId).collect(Collectors.toSet());
        Map<String, String> caseOfSubmission =
                submissionIds.isEmpty() ? Map.of() : submissionCases.findCaseIds(submissionIds);
        Set<String> caseIds = entries.stream().filter(ScoredDetailMask::scored)
                .map(e -> caseIdOf(e, caseOfSubmission)).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (caseIds.isEmpty()) {
            return entries.stream().map(e -> scored(e) ? e.withoutDetail() : e).collect(Collectors.toList());
        }
        Map<String, AuditCaseStateQuery.CaseState> states = caseStates.findStates(caseIds);
        // 只有狀態已不可能再指派、且讀者不是現任複審醫師的案件需要查歷史；其他一律遮罩。
        Set<String> candidates = caseIds.stream()
                .filter(id -> !maskedWithoutHistory(actor, states.get(id)))
                .collect(Collectors.toSet());
        Set<String> visible = candidates.isEmpty() ? Set.of() : visibleAfterHistory(actor, candidates);
        return entries.stream()
                .map(e -> scored(e) && !isVisible(visible, caseIdOf(e, caseOfSubmission)) ? e.withoutDetail() : e)
                .collect(Collectors.toList());
    }

    private Set<String> visibleAfterHistory(ActorContext actor, Set<String> candidates) {
        Set<String> historical = historyQuery.findCaseIdsAssignedTo(actor.getUserId(), candidates);
        Set<String> incomplete = historyQuery.findHistoryIncompleteCaseIds(candidates);
        return candidates.stream()
                .filter(id -> !historical.contains(id) && !incomplete.contains(id))
                .collect(Collectors.toSet());
    }

    private static boolean scored(AuditTrailEntry e) {
        return ("ReviewCase".equals(e.getEntityType()) && SCORED_ACTIONS.contains(e.getAction()))
                || scoredSubmission(e);
    }

    private static boolean scoredSubmission(AuditTrailEntry e) {
        return "ReviewSubmission".equals(e.getEntityType()) && SCORED_SUBMISSION_ACTIONS.contains(e.getAction());
    }

    /** Set.of() 不接受 contains(null)；找不到所屬案件就是看不到。 */
    private static boolean isVisible(Set<String> visible, String caseId) {
        return caseId != null && visible.contains(caseId);
    }

    /** 帶分數紀錄所屬的案件；提交紀錄找不到所屬案件時回傳 null，之後一律遮罩（D-078 第 2 點）。 */
    private static String caseIdOf(AuditTrailEntry e, Map<String, String> caseOfSubmission) {
        return scoredSubmission(e) ? caseOfSubmission.get(e.getEntityId()) : e.getEntityId();
    }

    /** 找不到案件時無法證明，遮罩（D-035 fail-closed）。 */
    private static boolean maskedWithoutHistory(ActorContext actor, AuditCaseStateQuery.CaseState state) {
        return state == null || !NO_FURTHER_REREVIEWER.contains(state.getStatus())
                || Objects.equals(actor.getUserId(), state.getRereviewerId());
    }
}
