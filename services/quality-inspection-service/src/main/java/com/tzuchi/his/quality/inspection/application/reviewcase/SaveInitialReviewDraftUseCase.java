package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.reviewcase.dto.AnswerInput;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.SubmissionStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewStage;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmissionRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.event.ReviewDraftSavedEvent;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetDefinition;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional
public class SaveInitialReviewDraftUseCase {

    private final ReviewSubmissionRepository submissionRepository;
    private final ScoreSheetRepository scoreSheetRepository;
    private final DomainEventPublisher eventPublisher;
    private final ScoreCalculationService scoreCalculationService;
    private final ReviewCaseRepository caseRepository;

    public SaveInitialReviewDraftUseCase(ReviewSubmissionRepository submissionRepository,
                                          ScoreSheetRepository scoreSheetRepository,
                                          DomainEventPublisher eventPublisher,
                                          ScoreCalculationService scoreCalculationService,
                                          ReviewCaseRepository caseRepository) {
        this.submissionRepository = submissionRepository;
        this.scoreSheetRepository = scoreSheetRepository;
        this.eventPublisher = eventPublisher;
        this.scoreCalculationService = scoreCalculationService;
        this.caseRepository = caseRepository;
    }

    /**
     * Save draft answers, calculate scores, return SaveDraftResult.
     */
    public SaveDraftResult execute(ActorContext actor, String caseId,
                               List<AnswerInput> answers, String comment) {
        actor.requireRole(DomainRole.REVIEWER);

        ReviewCase reviewCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("CASE_NOT_FOUND",
                        "ReviewCase not found: " + caseId));
        actor.requireHospitalScope(reviewCase.getHospitalId());
        reviewCase.requireNotTerminal();
        if (reviewCase.getStatus() != CaseStatus.INITIAL_REVIEW_IN_PROGRESS) {
            throw new DomainStateConflictException("CASE_NOT_IN_PROGRESS",
                    "Case must be in INITIAL_REVIEW_IN_PROGRESS to save a draft");
        }

        String activeSubmissionId = reviewCase.getLatestInitialSubmissionId();
        if (activeSubmissionId == null || activeSubmissionId.isBlank()) {
            throw new DomainStateConflictException("CONFLICT", "Case has no active INITIAL submission reference");
        }
        ReviewSubmission submission = submissionRepository.findById(activeSubmissionId)
                .orElseThrow(() -> new DomainStateConflictException("CONFLICT",
                        "Case references an unavailable INITIAL submission"));

        // Identity guard: only the submission owner
        if (!actor.getUserId().equals(submission.getReviewerId())) {
            throw new AccessDeniedException("NOT_SUBMISSION_OWNER",
                    "Only the reviewer who owns this submission can save a draft");
        }

        if (!submission.getSubmissionId().equals(reviewCase.getLatestInitialSubmissionId())
                || !caseId.equals(submission.getCaseId())
                || submission.getStage() != ReviewStage.INITIAL
                || submission.getSupersededBySubmissionId() != null) {
            throw new DomainStateConflictException("CONFLICT",
                    "Submission is not the current INITIAL result for this case");
        }
        if (submission.getStatus() != SubmissionStatus.DRAFT) {
            throw new DomainStateConflictException("SUBMISSION_NOT_DRAFT",
                    "Submission must be DRAFT");
        }

        // If no answers, update with empty + zero
        if (answers == null || answers.isEmpty()) {
            submission.updateDraft(Collections.emptyList(), BigDecimal.ZERO, comment);
            submissionRepository.save(submission);

            eventPublisher.publish(new ReviewDraftSavedEvent(
                    this, actor.getUserId(), actor.getUserName(),
                    "", caseId, submission.getSubmissionId(), BigDecimal.ZERO));

            return new SaveDraftResult(
                    submission.getSubmissionId(),
                    submission.getStatus().name(),
                    submission.getStage().name(),
                    BigDecimal.ZERO);
        }

        // Convert AnswerInput -> raw AnswerItem (no score yet)
        List<AnswerItem> rawItems = answers.stream()
                .filter(Objects::nonNull)
                .map(a -> AnswerItem.ofRaw(a.getItemId(), a.getAnswerValue()))
                .collect(Collectors.toList());

        // Load score sheet for scoring
        ScoreSheetDefinition scoreSheet = scoreSheetRepository
                .findById(submission.getBasedOnScoreSheetVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("SCORE_SHEET_NOT_FOUND",
                        "ScoreSheetDefinition not found: " + submission.getBasedOnScoreSheetVersionId()));

        // Calculate scores
        ScoreResult result = scoreCalculationService.calculate(rawItems, scoreSheet);

        // Update draft
        submission.updateDraft(result.getScoredAnswers(), result.getTotalScore(), comment);
        submissionRepository.save(submission);

        // Publish event
        eventPublisher.publish(new ReviewDraftSavedEvent(
                this, actor.getUserId(), actor.getUserName(),
                "", caseId, submission.getSubmissionId(), result.getTotalScore()));

        return new SaveDraftResult(
                submission.getSubmissionId(),
                submission.getStatus().name(),
                submission.getStage().name(),
                result.getTotalScore());
    }
}
