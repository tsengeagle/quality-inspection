package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.event.ReviewCaseClosedEvent;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewStage;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmissionRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.SubmissionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * 複審的獨立結案（D-034、D-053）。
 *
 * <p>檢查順序依 D-024：角色 → 載入 → 院區 → 領域規則；錯誤代碼依 D-053 第 5 點（狀態錯誤依 D-069 第 3 點改成 CASE_NOT_IN_REREVIEW_STAGE），
 * 領域規則的順序與複審 revision 相同，只有一處不同：已被取代的版本依 D-053 回 CONFLICT（revision 回 SUBMISSION_NOT_SUBMITTED）。結案與 revision 同時發生的競爭，要等 D-045 的樂觀鎖才能完全擋住（D-053 第 7 點）。
 */
@Service
public class CloseRereviewUseCase {

    private final ReviewCaseRepository caseRepository;
    private final ReviewSubmissionRepository submissionRepository;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;

    public CloseRereviewUseCase(ReviewCaseRepository caseRepository,
                                ReviewSubmissionRepository submissionRepository,
                                DomainEventPublisher eventPublisher,
                                Clock clock) {
        this.caseRepository = caseRepository;
        this.submissionRepository = submissionRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public CloseRereviewResult execute(ActorContext actor, String caseId, String submissionId) {
        actor.requireRole(DomainRole.REREVIEWER);

        ReviewCase reviewCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("CASE_NOT_FOUND", "ReviewCase not found: " + caseId));

        actor.requireHospitalScope(reviewCase.getHospitalId());
        reviewCase.requireNotTerminal();
        if (reviewCase.getStatus() != CaseStatus.REREVIEW_IN_PROGRESS) {
            // D-069 第 3 點：複審的狀態錯誤共用一個代碼（422）
            throw new DomainRuleViolationException("CASE_NOT_IN_REREVIEW_STAGE",
                    "Case must be in REREVIEW_IN_PROGRESS to close after rereview");
        }

        ReviewSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("SUBMISSION_NOT_FOUND", "ReviewSubmission not found: " + submissionId));

        if (!reviewCase.getCaseId().equals(submission.getCaseId())
                || submission.getStage() != ReviewStage.REREVIEW) {
            throw new DomainStateConflictException("CONFLICT",
                    "Submission must belong to this case and REREVIEW stage");
        }
        if (!actor.getUserId().equals(submission.getReviewerId())) {
            throw new AccessDeniedException("NOT_SUBMISSION_OWNER",
                    "Only the submission owner can perform this operation");
        }
        if (submission.getStatus() == SubmissionStatus.DRAFT) {
            throw new DomainStateConflictException("SUBMISSION_NOT_SUBMITTED",
                    "Submission must be SUBMITTED");
        }
        if (submission.getStatus() != SubmissionStatus.SUBMITTED
                || submission.getSupersededBySubmissionId() != null
                || !submission.getSubmissionId().equals(reviewCase.getLatestRereviewSubmissionId())) {
            throw new DomainStateConflictException("CONFLICT",
                    "Submission is not the current rereview result");
        }

        reviewCase.closeAfterRereview(Instant.now(clock));
        caseRepository.save(reviewCase);

        eventPublisher.publish(new ReviewCaseClosedEvent(
                this, actor.getUserId(), actor.getUserName(), reviewCase.getHospitalId(),
                reviewCase.getCaseId(), submission.getSubmissionId()));

        return new CloseRereviewResult(
                reviewCase.getCaseId(),
                reviewCase.getStatus().name(),
                submission.getSubmissionId());
    }
}
