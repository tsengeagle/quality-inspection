package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmissionRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.SubmissionStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.event.InitialReviewStartedEvent;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetDefinition;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetRepository;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import com.tzuchi.his.quality.inspection.application.common.IdGenerator;

@Service
@Transactional
public class StartInitialReviewUseCase {

    private final ReviewCaseRepository caseRepository;
    private final ReviewProjectRepository projectRepository;
    private final ScoreSheetRepository scoreSheetRepository;
    private final ReviewSubmissionRepository submissionRepository;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;
    private final IdGenerator idGenerator;

    public StartInitialReviewUseCase(ReviewCaseRepository caseRepository,
                                      ReviewProjectRepository projectRepository,
                                      ScoreSheetRepository scoreSheetRepository,
                                      ReviewSubmissionRepository submissionRepository,
                                      DomainEventPublisher eventPublisher,
                                      Clock clock,
                                     IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
        this.caseRepository = caseRepository;
        this.projectRepository = projectRepository;
        this.scoreSheetRepository = scoreSheetRepository;
        this.submissionRepository = submissionRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    /**
     * Start initial review: create a draft submission and transition the case.
     *
     * @return 初審提交 ID（新建，或退回後重開時沿用的 DRAFT）
     */
    public StartInitialReviewResult execute(ActorContext actor, String caseId) {
        Instant now = Instant.now(clock);
        actor.requireRole(DomainRole.REVIEWER);

        ReviewCase reviewCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("CASE_NOT_FOUND",
                        "ReviewCase not found: " + caseId));

        actor.requireHospitalScope(reviewCase.getHospitalId());

        // Identity guard: only the assigned reviewer can start
        // If no assignment exists, let domain state check handle it
        if (reviewCase.getReviewerAssignment() != null &&
                !actor.getUserId().equals(reviewCase.getReviewerAssignment().getReviewerId())) {
            throw new AccessDeniedException("NOT_ASSIGNED_REVIEWER",
                    "Only the assigned reviewer can start the initial review");
        }

        // Load project to get bound score sheet ID
        ReviewProject project = projectRepository.findById(reviewCase.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("PROJECT_NOT_FOUND",
                        "ReviewProject not found: " + reviewCase.getProjectId()));

        String scoreSheetVersionRef = project.getScoreSheetVersionRef();
        if (scoreSheetVersionRef == null) {
            throw new DomainRuleViolationException("NO_BOUND_SCORE_SHEET",
                    "Project has no bound score sheet");
        }

        // Load and validate score sheet is published. The project binds a score sheet version id (D-041, D-050 pt 1).
        ScoreSheetDefinition scoreSheet = scoreSheetRepository.findByVersionId(scoreSheetVersionRef)
                .orElseThrow(() -> new ResourceNotFoundException("SCORE_SHEET_NOT_FOUND",
                        "ScoreSheetDefinition not found: " + scoreSheetVersionRef));

        if (scoreSheet.getStatus() != ScoreSheetStatus.PUBLISHED) {
            throw new DomainRuleViolationException("SCORE_SHEET_NOT_PUBLISHED",
                    "Score sheet must be PUBLISHED to start review");
        }

        // 退回後重開：已有初審提交、仍是 DRAFT、且是操作者的才沿用，作答與評語保留（D-080 第 3 點、RC-STI-S4）。
        // 改派後換了醫師就建立新的提交，舊的留在修正歷史（D-081、RC-STI-S5）。
        ReviewSubmission existingDraft = reviewCase.getLatestInitialSubmissionId() == null ? null
                : submissionRepository.findById(reviewCase.getLatestInitialSubmissionId())
                        .filter(s -> s.getStatus() == SubmissionStatus.DRAFT)
                        .filter(s -> actor.getUserId().equals(s.getReviewerId()))
                        .orElse(null);

        String submissionId;
        if (existingDraft != null) {
            submissionId = existingDraft.getSubmissionId();
        } else {
            // Create draft submission
            submissionId = idGenerator.newId();
            submissionRepository.save(ReviewSubmission.createInitialDraft(
                    submissionId, caseId, actor.getUserId(), scoreSheetVersionRef, now));
        }

        // Transition case
        reviewCase.startInitialReview(submissionId, scoreSheetVersionRef);
        caseRepository.save(reviewCase);

        // Publish event
        eventPublisher.publish(new InitialReviewStartedEvent(
                this, idGenerator.newId(), now, actor.getUserId(), actor.getUserName(),
                reviewCase.getHospitalId(), caseId,
                actor.getUserId(), submissionId));

        return new StartInitialReviewResult(submissionId);
    }
}
