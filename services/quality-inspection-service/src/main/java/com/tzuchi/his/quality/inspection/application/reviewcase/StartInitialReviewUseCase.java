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
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.event.InitialReviewStartedEvent;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetDefinition;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetRepository;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class StartInitialReviewUseCase {

    private final ReviewCaseRepository caseRepository;
    private final ReviewProjectRepository projectRepository;
    private final ScoreSheetRepository scoreSheetRepository;
    private final ReviewSubmissionRepository submissionRepository;
    private final DomainEventPublisher eventPublisher;

    public StartInitialReviewUseCase(ReviewCaseRepository caseRepository,
                                      ReviewProjectRepository projectRepository,
                                      ScoreSheetRepository scoreSheetRepository,
                                      ReviewSubmissionRepository submissionRepository,
                                      DomainEventPublisher eventPublisher) {
        this.caseRepository = caseRepository;
        this.projectRepository = projectRepository;
        this.scoreSheetRepository = scoreSheetRepository;
        this.submissionRepository = submissionRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Start initial review: create a draft submission and transition the case.
     *
     * @return the new submission ID
     */
    public String execute(ActorContext actor, String caseId) {
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

        // Create draft submission
        String submissionId = UUID.randomUUID().toString();
        ReviewSubmission submission = ReviewSubmission.createInitialDraft(
                submissionId, caseId, actor.getUserId(), scoreSheetVersionRef);

        // Transition case
        reviewCase.startInitialReview(submissionId, scoreSheetVersionRef);

        // Persist
        submissionRepository.save(submission);
        caseRepository.save(reviewCase);

        // Publish event
        eventPublisher.publish(new InitialReviewStartedEvent(
                this, actor.getUserId(), actor.getUserName(),
                reviewCase.getHospitalId(), caseId,
                actor.getUserId(), submissionId));

        return submissionId;
    }
}
