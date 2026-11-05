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
import com.tzuchi.his.quality.inspection.domain.reviewcase.event.CaseEscalatedToRereviewEvent;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import com.tzuchi.his.quality.inspection.domain.scoresheet.RequiredRule;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreItem;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetDefinition;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class SubmitInitialReviewUseCase {

    private final ReviewCaseRepository caseRepository;
    private final ReviewSubmissionRepository submissionRepository;
    private final ReviewProjectRepository projectRepository;
    private final ScoreSheetRepository scoreSheetRepository;
    private final DomainEventPublisher eventPublisher;
    private final ScoreCalculationService scoreCalculationService;

    public SubmitInitialReviewUseCase(ReviewCaseRepository caseRepository,
                                       ReviewSubmissionRepository submissionRepository,
                                       ReviewProjectRepository projectRepository,
                                       ScoreSheetRepository scoreSheetRepository,
                                       DomainEventPublisher eventPublisher,
                                       ScoreCalculationService scoreCalculationService) {
        this.caseRepository = caseRepository;
        this.submissionRepository = submissionRepository;
        this.projectRepository = projectRepository;
        this.scoreSheetRepository = scoreSheetRepository;
        this.eventPublisher = eventPublisher;
        this.scoreCalculationService = scoreCalculationService;
    }

    public SubmitInitialReviewResult execute(ActorContext actor, String caseId) {
        actor.requireRole(DomainRole.REVIEWER);

        // Load case first to enforce hospital scope before touching submission data
        ReviewCase reviewCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("CASE_NOT_FOUND",
                        "ReviewCase not found: " + caseId));

        actor.requireHospitalScope(reviewCase.getHospitalId());

        String activeSubmissionId = reviewCase.getLatestInitialSubmissionId();
        if (activeSubmissionId == null || activeSubmissionId.isBlank()) {
            throw new DomainStateConflictException("CONFLICT", "Case has no active INITIAL submission reference");
        }
        ReviewSubmission submission = submissionRepository.findById(activeSubmissionId)
                .orElseThrow(() -> new DomainStateConflictException("CONFLICT",
                        "Case references an unavailable INITIAL submission"));

        if (!actor.getUserId().equals(submission.getReviewerId())) {
            throw new AccessDeniedException("NOT_SUBMISSION_OWNER",
                    "Only the reviewer who owns this submission can submit");
        }

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

        if (!submission.getSubmissionId().equals(reviewCase.getLatestInitialSubmissionId())
                || !caseId.equals(submission.getCaseId())
                || submission.getStage() != ReviewStage.INITIAL
                || submission.getSupersededBySubmissionId() != null) {
            throw new DomainStateConflictException("CONFLICT",
                    "Submission is not the current INITIAL result for this case");
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

        // Load quality threshold from the project's current version
        ReviewProject project = projectRepository.findById(reviewCase.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("ReviewProject", reviewCase.getProjectId()));
        BigDecimal qualityThreshold = project.getCurrentVersion().getQualityThreshold();

        // Complete review case
        reviewCase.completeInitialReview(result.getTotalScore(), qualityThreshold);
        caseRepository.save(reviewCase);

        // If escalated to re-review, publish escalation event
        if (reviewCase.getStatus() == CaseStatus.PENDING_REREVIEW) {
            eventPublisher.publish(new CaseEscalatedToRereviewEvent(
                    this, actor.getUserId(), actor.getUserName(), reviewCase.getHospitalId(),
                    reviewCase.getCaseId(),
                    String.format("{\"score\":%s,\"threshold\":%s}",
                            result.getTotalScore(), qualityThreshold)));
        }

        // Publish event
        eventPublisher.publish(new ReviewSubmissionSubmittedEvent(
                this, actor.getUserId(), actor.getUserName(),
                reviewCase.getHospitalId(), caseId,
                submission.getSubmissionId(), result.getTotalScore(), actor.getUserId()));

        return new SubmitInitialReviewResult(
                submission.getSubmissionId(),
                submission.getStatus().name(),
                caseId,
                reviewCase.getStatus().name(),
                result.getTotalScore(),
                reviewCase.isRereviewRequired());
    }
}
