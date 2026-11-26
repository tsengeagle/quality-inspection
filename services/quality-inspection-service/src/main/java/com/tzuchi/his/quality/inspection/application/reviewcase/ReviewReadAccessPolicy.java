package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.*;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.ReviewCaseDetailView;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.*;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * D-035、D-055：完整初審讀取的授權。
 * 除了既有的角色、院區、指派範圍之外，必須以複審指派歷史證明登入者「不是本案的現任或歷史複審醫師」
 * （Proven non-historical）才准許；准許時記錄讀取者（D-055 第 5 點），讀過的人之後不得受派同案複審。
 * 呼叫端必須在可寫入的交易中，讀取者紀錄隨交易 commit；commit 失敗則整筆讀取失敗。
 */
@Component
public class ReviewReadAccessPolicy {
    private final RereviewerAssignmentHistoryPort historyPort;
    private final InitialReviewReaderPort readerPort;
    private final Clock clock;

    public ReviewReadAccessPolicy(RereviewerAssignmentHistoryPort historyPort,
                                  InitialReviewReaderPort readerPort,
                                  Clock clock) {
        this.historyPort = historyPort;
        this.readerPort = readerPort;
        this.clock = clock;
    }

    public void requireAccess(ActorContext actor, ReviewCaseDetailView view) {
        requireAccess(actor, view.getCaseId(), view.getHospitalId(), view.getReviewerId(), view.getRereviewerId(),
                view.getSubmissions().stream().anyMatch(s -> "INITIAL".equals(s.getStage())
                        && submitted(s.getStatus()) && Objects.equals(actor.getUserId(), s.getReviewerId())),
                view.getSubmissions().stream().anyMatch(s -> "REREVIEW".equals(s.getStage())
                        && submitted(s.getStatus()) && Objects.equals(actor.getUserId(), s.getReviewerId())));
    }

    public void requireAccess(ActorContext actor, ReviewCase rc, List<ReviewSubmission> submissions) {
        requireAccess(actor, rc.getCaseId(), rc.getHospitalId(),
                rc.getReviewerAssignment() == null ? null : rc.getReviewerAssignment().getReviewerId(),
                rc.getRereviewerAssignment() == null ? null : rc.getRereviewerAssignment().getReviewerId(),
                submissions.stream().anyMatch(s -> s.getStage() == ReviewStage.INITIAL
                        && submitted(s.getStatus().name()) && Objects.equals(actor.getUserId(), s.getReviewerId())),
                submissions.stream().anyMatch(s -> s.getStage() == ReviewStage.REREVIEW
                        && submitted(s.getStatus().name()) && Objects.equals(actor.getUserId(), s.getReviewerId())));
    }

    private void requireAccess(ActorContext actor, String caseId, String hospital, String reviewer, String rereviewer,
                               boolean initialSubmitter, boolean rereviewSubmitter) {
        // D-084 第 1 點：照 D-024 的順序，院區先於多角色
        actor.requireReadScope(hospital);
        if (actor.getRoles().size() != 1) {
            throw new AccessDeniedException("Actors holding more than one role cannot read case content (D-094)");
        }
        if (actor.hasRole(DomainRole.SYSTEM_ADMIN)) return;
        boolean allowed = actor.hasRole(DomainRole.HOSPITAL_MANAGER) || actor.hasRole(DomainRole.AUDITOR);
        allowed |= actor.hasRole(DomainRole.REVIEWER)
                && (Objects.equals(actor.getUserId(), reviewer) || initialSubmitter);
        allowed |= actor.hasRole(DomainRole.REREVIEWER)
                && (Objects.equals(actor.getUserId(), rereviewer) || rereviewSubmitter);
        if (!allowed) throw new AccessDeniedException("Case is outside the actor's assignment/submission scope");
        // This role only receives its existing blind/own-rereview projection, never full initial content.
        if (actor.hasRole(DomainRole.REREVIEWER)) return;
        // D-055 第 5 點：先把案件版本固定在本交易，再判斷身分；兩者之間若有指派先 commit，本交易 commit 時失敗。
        readerPort.pinCaseVersion(caseId);
        HistoricalIdentity identity = Objects.equals(actor.getUserId(), rereviewer) || rereviewSubmitter
                ? HistoricalIdentity.PROVEN_HISTORICAL
                : assessHistoricalIdentity(caseId, actor.getUserId());
        if (identity != HistoricalIdentity.PROVEN_NON_HISTORICAL) {
            throw new AccessDeniedException("Historical rereviewer identity cannot be reliably excluded");
        }
        readerPort.record(caseId, actor.getUserId(), Instant.now(clock));
    }

    enum HistoricalIdentity {
        PROVEN_HISTORICAL, PROVEN_NON_HISTORICAL, UNKNOWN
    }

    /** D-055 第 3 點：出現在歷史中 → historical；歷史完整且不在其中 → non-historical；歷史不完整 → UNKNOWN。 */
    HistoricalIdentity assessHistoricalIdentity(String caseId, String userId) {
        if (historyPort.contains(caseId, userId)) return HistoricalIdentity.PROVEN_HISTORICAL;
        return historyPort.isComplete(caseId) ? HistoricalIdentity.PROVEN_NON_HISTORICAL : HistoricalIdentity.UNKNOWN;
    }

    public boolean canReadContent(ActorContext actor, String stage, String submitter) {
        if (actor.hasRole(DomainRole.SYSTEM_ADMIN)) return false;
        if (actor.hasRole(DomainRole.REREVIEWER)) {
            return "REREVIEW".equals(stage) && Objects.equals(actor.getUserId(), submitter);
        }
        return true;
    }

    private boolean submitted(String status) {
        return "SUBMITTED".equals(status) || "SUPERSEDED".equals(status);
    }
}
