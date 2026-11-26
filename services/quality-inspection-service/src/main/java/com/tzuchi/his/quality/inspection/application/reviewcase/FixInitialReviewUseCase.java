package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.event.ReviewCaseEscalatedToRereviewEvent;
import com.tzuchi.his.quality.inspection.domain.reviewcase.event.InitialReviewFixedEvent;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewStage;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmissionRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.SubmissionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import com.tzuchi.his.quality.inspection.application.common.IdGenerator;

/**
 * 固定初審（D-031、DD-03、D-054）。檢查順序依 D-024：角色 → 載入 → 院區 → 領域規則；
 * 領域規則的順序與錯誤代碼依 D-054 第 6 點。分數 > 品質線時固定後進複審（第 3 點）。
 */
@Service
public class FixInitialReviewUseCase {

    private final ReviewCaseRepository caseRepository;
    private final ReviewSubmissionRepository submissionRepository;
    private final ReviewProjectRepository projectRepository;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;
    private final IdGenerator idGenerator;

    public FixInitialReviewUseCase(ReviewCaseRepository caseRepository,
                                   ReviewSubmissionRepository submissionRepository,
                                   ReviewProjectRepository projectRepository,
                                   DomainEventPublisher eventPublisher,
                                   Clock clock,
                                   IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
        this.caseRepository = caseRepository;
        this.submissionRepository = submissionRepository;
        this.projectRepository = projectRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public FixInitialReviewResult execute(ActorContext actor, String caseId, String submissionId) {
        Instant now = Instant.now(clock);
        actor.requireRole(DomainRole.REVIEWER);

        ReviewCase reviewCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("CASE_NOT_FOUND", "Case not found: " + caseId));
        ReviewSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("SUBMISSION_NOT_FOUND",
                        "Submission not found: " + submissionId));

        actor.requireHospitalScope(reviewCase.getHospitalId());
        reviewCase.requireFixable();

        if (!reviewCase.getCaseId().equals(submission.getCaseId())
                || submission.getStage() != ReviewStage.INITIAL
                || submission.getStatus() == SubmissionStatus.SUPERSEDED
                || submission.getSupersededBySubmissionId() != null
                || !submission.getSubmissionId().equals(reviewCase.getLatestInitialSubmissionId())) {
            throw new DomainStateConflictException("CONFLICT",
                    "Submission is not the current initial review result of this case");
        }
        if (submission.getStatus() != SubmissionStatus.SUBMITTED) {
            throw new DomainStateConflictException("SUBMISSION_NOT_SUBMITTED", "Submission must be SUBMITTED");
        }
        if (!actor.getUserId().equals(submission.getReviewerId())) {
            throw new AccessDeniedException("NOT_SUBMISSION_OWNER",
                    "Only the submission owner can perform this operation");
        }

        ReviewProject project = projectRepository.findById(reviewCase.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("PROJECT_NOT_FOUND",
                        "Project not found: " + reviewCase.getProjectId()));
        Instant fixedAt = now.truncatedTo(ChronoUnit.MICROS);

        reviewCase.fixInitialReview(submission.getSubmissionId(), actor.getUserId(), fixedAt,
                submission.getTotalScore(), project.getCurrentVersion().getQualityThreshold());
        caseRepository.save(reviewCase);

        eventPublisher.publish(new InitialReviewFixedEvent(this, idGenerator.newId(), now, actor.getUserId(), actor.getUserName(),
                reviewCase.getHospitalId(), reviewCase.getCaseId(), submission.getSubmissionId()));
        if (reviewCase.getStatus() == CaseStatus.PENDING_REREVIEW) {
            eventPublisher.publish(ReviewCaseEscalatedToRereviewEvent.onFix(this, idGenerator.newId(), now, actor.getUserId(), actor.getUserName(),
                    reviewCase.getHospitalId(), reviewCase.getCaseId(), reviewCase.getProjectId(),
                    submission.getSubmissionId(), submission.getTotalScore(),
                    project.getCurrentVersion().getQualityThreshold()));
        }

        return new FixInitialReviewResult(reviewCase.getCaseId(), reviewCase.getStatus().name(),
                submission.getSubmissionId(), fixedAt);
    }
}
