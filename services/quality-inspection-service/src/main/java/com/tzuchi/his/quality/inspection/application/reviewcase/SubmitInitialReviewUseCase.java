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
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewStage;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmissionRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.SubmissionStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.event.ReviewSubmissionSubmittedEvent;
import com.tzuchi.his.quality.inspection.domain.scoresheet.RequiredRule;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreItem;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetDefinition;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class SubmitInitialReviewUseCase {

    private final ReviewCaseRepository caseRepository;
    private final ReviewSubmissionRepository submissionRepository;
    private final ScoreSheetRepository scoreSheetRepository;
    private final DomainEventPublisher eventPublisher;
    private final ScoreCalculationService scoreCalculationService;

    public SubmitInitialReviewUseCase(ReviewCaseRepository caseRepository,
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

    public void execute(ActorContext actor, String caseId) {
        actor.requireRole(DomainRole.REVIEWER);

        ReviewSubmission submission = submissionRepository
                .findLatestByCaseIdAndStage(caseId, ReviewStage.INITIAL)
                .orElseThrow(() -> new ResourceNotFoundException("SUBMISSION_NOT_FOUND",
                        "No INITIAL submission found for case: " + caseId));

        if (!actor.getUserId().equals(submission.getReviewerId())) {
            throw new AccessDeniedException("NOT_SUBMISSION_OWNER",
                    "Only the reviewer who owns this submission can submit");
        }

        // Load ReviewCase and verify status
        ReviewCase reviewCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("CASE_NOT_FOUND",
                        "ReviewCase not found: " + caseId));

        if (reviewCase.getStatus() != CaseStatus.INITIAL_REVIEW_IN_PROGRESS) {
            throw new DomainStateConflictException(
                    "CASE_NOT_IN_PROGRESS",
                    "Case must be in INITIAL_REVIEW_IN_PROGRESS. Current: " + reviewCase.getStatus());
        }

        // Check if already submitted early (will throw DomainStateConflictException)
        if (submission.getStatus() != SubmissionStatus.DRAFT) {
            throw new DomainStateConflictException(
                    "SUBMISSION_NOT_DRAFT",
                    "Submission must be in DRAFT status. Current: " + submission.getStatus());
        }

        ScoreSheetDefinition scoreSheet = scoreSheetRepository
                .findById(submission.getBasedOnScoreSheetVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("SCORE_SHEET_NOT_FOUND",
                        "ScoreSheetDefinition not found: " + submission.getBasedOnScoreSheetVersionId()));

        List<AnswerItem> currentAnswers = submission.getAnswerSet();

        // Validate required items
        Set<String> answeredItemIds = currentAnswers.stream()
                .map(AnswerItem::getItemId)
                .collect(Collectors.toSet());

        List<String> missingRequired = scoreSheet.getSections().stream()
                .flatMap(s -> s.getItems().stream())
                .filter(item -> item.getRequiredRule() == RequiredRule.ALWAYS_REQUIRED)
                .map(ScoreItem::getItemId)
                .filter(id -> !answeredItemIds.contains(id))
                .collect(Collectors.toList());

        if (!missingRequired.isEmpty()) {
            throw new DomainRuleViolationException("REQUIRED_ITEMS_NOT_ANSWERED",
                    "Required items not answered: " + String.join(", ", missingRequired));
        }

        // Recalculate final scores
        List<AnswerItem> rawItems = currentAnswers.stream()
                .map(a -> AnswerItem.ofRaw(a.getItemId(), a.getAnswerValue()))
                .collect(Collectors.toList());

        ScoreResult result = scoreCalculationService.calculate(rawItems, scoreSheet);

        // Update with final scores, then submit
        submission.updateDraft(result.getScoredAnswers(), result.getTotalScore(), submission.getComment());
        submission.submit();
        submissionRepository.save(submission);

        // Complete review case
        reviewCase.completeInitialReview(result.getTotalScore(), null);
        caseRepository.save(reviewCase);

        // Publish event
        eventPublisher.publish(new ReviewSubmissionSubmittedEvent(
                this, actor.getUserId(), actor.getUserName(),
                reviewCase.getHospitalId(), caseId,
                submission.getSubmissionId(), result.getTotalScore(), actor.getUserId()));
    }
}
