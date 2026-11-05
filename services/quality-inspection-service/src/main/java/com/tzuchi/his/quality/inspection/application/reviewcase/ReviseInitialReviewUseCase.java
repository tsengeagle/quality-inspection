package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.reviewcase.dto.AnswerInput;
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.SubmissionStatus;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewStage;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmissionRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.event.ReviewSubmissionRevisedEvent;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetDefinition;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetRepository;
import com.tzuchi.his.quality.inspection.interfaces.rest.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReviseInitialReviewUseCase {

    private final ReviewCaseRepository caseRepository;
    private final ReviewSubmissionRepository submissionRepository;
    private final ScoreSheetRepository scoreSheetRepository;
    private final DomainEventPublisher eventPublisher;
    private final ScoreCalculationService scoreCalculationService;

    public ReviseInitialReviewUseCase(ReviewCaseRepository caseRepository,
                                       ReviewSubmissionRepository submissionRepository,
                                       ScoreSheetRepository scoreSheetRepository,
                                       DomainEventPublisher eventPublisher,
                                       ScoreCalculationService scoreCalculationService) {
        this.caseRepository = caseRepository;
        this.submissionRepository = submissionRepository;
        this.scoreSheetRepository = scoreSheetRepository;
        this.eventPublisher = eventPublisher;
        this.scoreCalculationService = scoreCalculationService;
    }

    public RevisionResult execute(ActorContext actor, String caseId, String submissionId,
                                   List<AnswerInput> answers, String comment) {
        actor.requireRole(DomainRole.REVIEWER);

        // Load and validate ReviewCase
        ReviewCase reviewCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("CASE_NOT_FOUND",
                        "ReviewCase not found: " + caseId));
        actor.requireHospitalScope(reviewCase.getHospitalId());
        reviewCase.requireNotTerminal();

        // Load and validate old submission
        ReviewSubmission oldSubmission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("SUBMISSION_NOT_FOUND",
                        "Submission not found: " + submissionId));

        if (!oldSubmission.getCaseId().equals(caseId)) {
            throw new BadRequestException(
                    "Submission " + submissionId + " does not belong to case " + caseId);
        }

        if (oldSubmission.getStage() != ReviewStage.INITIAL) {
            throw new DomainRuleViolationException("SUBMISSION_NOT_INITIAL",
                    "Can only revise INITIAL stage submissions. Current: " + oldSubmission.getStage());
        }

        if (!actor.getUserId().equals(oldSubmission.getReviewerId())) {
            throw new AccessDeniedException("NOT_SUBMISSION_OWNER",
                    "Only the original reviewer can revise this submission");
        }

        if (oldSubmission.getStatus() != SubmissionStatus.SUBMITTED) {
            throw new DomainStateConflictException("SUBMISSION_NOT_SUBMITTED",
                    "Submission must be SUBMITTED");
        }
        if (!submissionId.equals(reviewCase.getLatestInitialSubmissionId())
                || oldSubmission.getSupersededBySubmissionId() != null) {
            throw new DomainStateConflictException("CONFLICT",
                    "Submission is not the current INITIAL result");
        }

        // Load score sheet for scoring
        ScoreSheetDefinition scoreSheet = scoreSheetRepository
                .findById(oldSubmission.getBasedOnScoreSheetVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("SCORE_SHEET_NOT_FOUND",
                        "ScoreSheetDefinition not found: " + oldSubmission.getBasedOnScoreSheetVersionId()));

        // Calculate scores
        List<AnswerItem> rawItems = answers.stream()
                .map(a -> AnswerItem.ofRaw(a.getItemId(), a.getAnswerValue()))
                .collect(Collectors.toList());
        ScoreResult result = scoreCalculationService.calculate(rawItems, scoreSheet);

        // Create new revision
        String newSubmissionId = UUID.randomUUID().toString();
        ReviewSubmission newSubmission = ReviewSubmission.createRevision(
                newSubmissionId, oldSubmission, result.getScoredAnswers(),
                result.getTotalScore(), comment);

        // Mark old as superseded
        oldSubmission.markSuperseded(newSubmissionId);

        // Update case's final score (no status change)
        reviewCase.recordInitialReviewRevision(submissionId, newSubmissionId, result.getTotalScore());

        // Persist
        submissionRepository.save(oldSubmission);
        submissionRepository.save(newSubmission);
        caseRepository.save(reviewCase);

        // Publish event
        eventPublisher.publish(new ReviewSubmissionRevisedEvent(
                this, actor.getUserId(), actor.getUserName(),
                reviewCase.getHospitalId(), caseId,
                submissionId, newSubmissionId,
                oldSubmission.getStage().name(), newSubmission.getRevisionNo()));

        return new RevisionResult(
                newSubmissionId, submissionId,
                newSubmission.getRevisionNo(),
                result.getTotalScore(),
                newSubmission.getStatus().name(),
                newSubmission.getStage().name(),
                caseId);
    }
}