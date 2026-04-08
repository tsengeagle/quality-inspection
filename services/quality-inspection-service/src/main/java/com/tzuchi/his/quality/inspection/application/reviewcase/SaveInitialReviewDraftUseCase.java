package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.reviewcase.dto.AnswerInput;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
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

    public SaveInitialReviewDraftUseCase(ReviewSubmissionRepository submissionRepository,
                                          ScoreSheetRepository scoreSheetRepository,
                                          DomainEventPublisher eventPublisher,
                                          ScoreCalculationService scoreCalculationService) {
        this.submissionRepository = submissionRepository;
        this.scoreSheetRepository = scoreSheetRepository;
        this.eventPublisher = eventPublisher;
        this.scoreCalculationService = scoreCalculationService;
    }

    /**
     * Save draft answers, calculate scores, return totalScore.
     */
    public BigDecimal execute(ActorContext actor, String caseId,
                               List<AnswerInput> answers, String comment) {
        actor.requireRole(DomainRole.REVIEWER);

        ReviewSubmission submission = submissionRepository
                .findLatestByCaseIdAndStage(caseId, ReviewStage.INITIAL)
                .orElseThrow(() -> new ResourceNotFoundException("SUBMISSION_NOT_FOUND",
                        "No INITIAL submission found for case: " + caseId));

        // Identity guard: only the submission owner
        if (!actor.getUserId().equals(submission.getReviewerId())) {
            throw new AccessDeniedException("NOT_SUBMISSION_OWNER",
                    "Only the reviewer who owns this submission can save a draft");
        }

        // If no answers, update with empty + zero
        if (answers == null || answers.isEmpty()) {
            submission.updateDraft(Collections.emptyList(), BigDecimal.ZERO, comment);
            submissionRepository.save(submission);

            eventPublisher.publish(new ReviewDraftSavedEvent(
                    this, actor.getUserId(), actor.getUserName(),
                    "", caseId, submission.getSubmissionId(), BigDecimal.ZERO));

            return BigDecimal.ZERO;
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

        return result.getTotalScore();
    }
}
