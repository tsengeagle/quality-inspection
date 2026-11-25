package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.reviewcase.dto.AnswerInput;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.time.Clock;
import java.time.Instant;

@Service
@Transactional
public class ReviseInitialReviewUseCase {

    private final ReviewCaseRepository caseRepository;
    private final ReviewSubmissionRepository submissionRepository;
    private final ScoreSheetRepository scoreSheetRepository;
    private final DomainEventPublisher eventPublisher;
    private final ScoreCalculationService scoreCalculationService;
    private final Clock clock;

    public ReviseInitialReviewUseCase(ReviewCaseRepository caseRepository,
                                       ReviewSubmissionRepository submissionRepository,
                                       ScoreSheetRepository scoreSheetRepository,
                                       DomainEventPublisher eventPublisher,
                                       ScoreCalculationService scoreCalculationService,
                                       Clock clock) {
        this.caseRepository = caseRepository;
        this.submissionRepository = submissionRepository;
        this.scoreSheetRepository = scoreSheetRepository;
        this.eventPublisher = eventPublisher;
        this.scoreCalculationService = scoreCalculationService;
        this.clock = clock;
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

        // D-083 第 4 點：不屬於本案或不是初審，一律 409 CONFLICT
        if (!caseId.equals(oldSubmission.getCaseId())
                || oldSubmission.getStage() != ReviewStage.INITIAL) {
            throw new DomainStateConflictException("CONFLICT",
                    "Submission must belong to this case and INITIAL stage");
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
        // D-083 第 7 點：固定的檢查在必答題之前（domain 的 recordInitialReviewRevision 仍會再檢查）
        if (reviewCase.isInitialReviewFixed()) {
            throw new DomainStateConflictException("INVALID_STATE_TRANSITION",
                    "Initial review is fixed and can no longer be revised (D-031, D-054)");
        }

        // Load score sheet for scoring
        ScoreSheetDefinition scoreSheet = scoreSheetRepository
                .findByVersionId(oldSubmission.getBasedOnScoreSheetVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("SCORE_SHEET_NOT_FOUND",
                        "ScoreSheetDefinition not found: " + oldSubmission.getBasedOnScoreSheetVersionId()));

        List<AnswerItem> rawItems = RevisionChecks.rawAnswers(answers);
        // RC-RVI-10（D-085 第 8 點）：作答的檢查在必答題與沒有變更之前
        scoreCalculationService.requireValidAnswers(rawItems, scoreSheet);
        // D-083 第 1 點：必答題與提交相同（D-067）；第 2 點：沒有變更回 NO_CHANGES（D-038）
        RevisionChecks.requireRequiredItemsAnswered(rawItems, scoreSheet);
        RevisionChecks.rejectIfUnchanged(rawItems, comment, oldSubmission);

        // Calculate scores
        ScoreResult result = scoreCalculationService.calculate(rawItems, scoreSheet);

        // Create new revision
        String newSubmissionId = UUID.randomUUID().toString();
        Instant now = Instant.now(clock);
        ReviewSubmission newSubmission = ReviewSubmission.createRevision(
                newSubmissionId, oldSubmission, result.getScoredAnswers(),
                result.getTotalScore(), comment, now);

        // Mark old as superseded
        oldSubmission.markSuperseded(newSubmissionId, now);

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