package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.reviewcase.command.ReviseRereviewCommand;
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.project.ProjectVersion;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.event.RereviewRevisedEvent;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmissionRepository;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetDefinition;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Use case: Revise a submitted rereview.
 * Per D-021, CLOSED cases are allowed rereview revision (not CANCELLED).
 * Creates a new revision of the rereview submission and recalculates the weighted final score.
 * This is the "append-only revision" pattern — old submission is marked SUPERSEDED,
 * new revision is directly SUBMITTED.
 */
@Service
public class ReviseRereviewUseCase {
    private final ReviewCaseRepository caseRepository;
    private final ReviewSubmissionRepository submissionRepository;
    private final ReviewProjectRepository projectRepository;
    private final ScoreSheetRepository scoreSheetRepository;
    private final DomainEventPublisher eventPublisher;
    private final ScoreCalculationService scoreCalculationService;

    public ReviseRereviewUseCase(ReviewCaseRepository caseRepository,
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

    @Transactional
    public RevisionResult execute(ActorContext actor, ReviseRereviewCommand cmd) {
        // 1. Require REREVIEWER role
        actor.requireRole(DomainRole.REREVIEWER);

        // 2. Load ReviewCase and validate actor is the assigned rereviewer
        ReviewCase reviewCase = caseRepository.findById(cmd.getCaseId())
                .orElseThrow(() -> new ResourceNotFoundException("ReviewCase", cmd.getCaseId()));

        if (reviewCase.getRereviewerAssignment() == null
                || !actor.getUserId().equals(reviewCase.getRereviewerAssignment().getReviewerId())) {
            throw new DomainRuleViolationException("NOT_ASSIGNED_REREVIEWER",
                    "Only the assigned rereviewer can revise this rereview");
        }

        // 3. Load old ReviewSubmission by submissionId
        ReviewSubmission oldSubmission = submissionRepository.findById(cmd.getSubmissionId())
                .orElseThrow(() -> new ResourceNotFoundException("ReviewSubmission", cmd.getSubmissionId()));

        // 4. Load ScoreSheetDefinition via reviewCase.getScoreSheetVersionRef()
        ScoreSheetDefinition scoreSheet = scoreSheetRepository
                .findById(reviewCase.getScoreSheetVersionRef())
                .orElseThrow(() -> new ResourceNotFoundException("ScoreSheetDefinition", reviewCase.getScoreSheetVersionRef()));

        // 5. Build raw AnswerItem list from cmd answers
        List<AnswerItem> rawItems = cmd.getAnswers().stream()
                .map(a -> AnswerItem.ofRaw(a.getItemId(), a.getAnswerValue()))
                .collect(Collectors.toList());

        // 6. Calculate scores via ScoreCalculationService
        ScoreResult result = scoreCalculationService.calculate(rawItems, scoreSheet);

        // 7. Create revision using static method
        String newSubmissionId = "sub-rev-" + UUID.randomUUID().toString();
        ReviewSubmission revision = ReviewSubmission.createRevision(
                newSubmissionId,
                oldSubmission,
                result.getScoredAnswers(),
                result.getTotalScore(),
                cmd.getComment());

        // 8. Mark old submission superseded
        oldSubmission.markSuperseded(newSubmissionId);

        // 9. Save both old and new submissions
        submissionRepository.save(oldSubmission);
        submissionRepository.save(revision);

        // 10. Load ReviewProject and get weights from getCurrentVersion()
        ReviewProject project = projectRepository.findById(reviewCase.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("ReviewProject", reviewCase.getProjectId()));
        ProjectVersion version = project.getCurrentVersion();

        // 11. Update case final score for rereview (D-021: CLOSED cases allowed)
        reviewCase.updateFinalScoreForRereview(
                result.getTotalScore(),
                version.getInitialReviewWeight(),
                version.getRereviewWeight());
        reviewCase.setLatestRereviewSubmissionId(newSubmissionId);

        // 12. Save case
        caseRepository.save(reviewCase);

        // 13. Publish RereviewRevisedEvent
        eventPublisher.publish(new RereviewRevisedEvent(
                this,
                actor.getUserId(),
                actor.getUserName(),
                reviewCase.getHospitalId(),
                reviewCase.getCaseId(),
                String.format("{\"newSubmissionId\":\"%s\",\"oldSubmissionId\":\"%s\",\"rereviewScore\":%s,\"finalScore\":%s}",
                        newSubmissionId, cmd.getSubmissionId(), result.getTotalScore(), reviewCase.getFinalScore())));

        return new RevisionResult(
                newSubmissionId,
                cmd.getSubmissionId(),
                revision.getRevisionNo(),
                result.getTotalScore(),
                revision.getStatus().name(),
                revision.getStage().name(),
                cmd.getCaseId());
    }
}
