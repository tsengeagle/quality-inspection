package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.IdGenerator;
import com.tzuchi.his.quality.inspection.application.reviewcase.dto.AnswerInput;
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewStage;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.SubmissionStatus;
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
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.event.ReviewSubmissionRevisedEvent;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetDefinition;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.Clock;
import java.time.Instant;

/**
 * Use case: Revise a submitted rereview.
 * D-032: only non-terminal cases may receive a rereview revision. D-083：檢查順序、必答題、沒有變更與事件。
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
    private final Clock clock;
    private final IdGenerator idGenerator;

    public ReviseRereviewUseCase(ReviewCaseRepository caseRepository,
                                  ReviewSubmissionRepository submissionRepository,
                                  ReviewProjectRepository projectRepository,
                                  ScoreSheetRepository scoreSheetRepository,
                                  DomainEventPublisher eventPublisher,
                                  ScoreCalculationService scoreCalculationService,
                                  Clock clock,
                                  IdGenerator idGenerator) {
        this.caseRepository = caseRepository;
        this.submissionRepository = submissionRepository;
        this.projectRepository = projectRepository;
        this.scoreSheetRepository = scoreSheetRepository;
        this.eventPublisher = eventPublisher;
        this.scoreCalculationService = scoreCalculationService;
        this.clock = clock;
        this.idGenerator = idGenerator;
    }

    @Transactional
    public RevisionResult execute(ActorContext actor, String caseId, String submissionId,
                                   List<AnswerInput> answers, String comment) {
        // D-083 第 7 點的檢查順序
        actor.requireRole(DomainRole.REREVIEWER);

        ReviewCase reviewCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("CASE_NOT_FOUND", "ReviewCase not found: " + caseId));

        actor.requireHospitalScope(reviewCase.getHospitalId());
        reviewCase.requireNotTerminal();

        if (reviewCase.getRereviewerAssignment() == null
                || !actor.getUserId().equals(reviewCase.getRereviewerAssignment().getReviewerId())) {
            throw new AccessDeniedException("NOT_ASSIGNED_REREVIEWER",
                    "Only the assigned rereviewer can revise this rereview");
        }

        ReviewSubmission oldSubmission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("SUBMISSION_NOT_FOUND", "ReviewSubmission not found: " + submissionId));

        if (!reviewCase.getCaseId().equals(oldSubmission.getCaseId())
                || oldSubmission.getStage() != ReviewStage.REREVIEW) {
            throw new DomainStateConflictException("CONFLICT",
                    "Submission must belong to this case and REREVIEW stage");
        }
        if (!actor.getUserId().equals(oldSubmission.getReviewerId())) {
            throw new AccessDeniedException("NOT_SUBMISSION_OWNER",
                    "Only the submission owner can perform this operation");
        }
        if (oldSubmission.getStatus() != SubmissionStatus.SUBMITTED) {
            throw new DomainStateConflictException("SUBMISSION_NOT_SUBMITTED",
                    "Submission must be SUBMITTED");
        }
        if (!oldSubmission.getSubmissionId().equals(reviewCase.getLatestRereviewSubmissionId())
                || oldSubmission.getSupersededBySubmissionId() != null) {
            throw new DomainStateConflictException("CONFLICT",
                    "Submission is not the current rereview result");
        }

        ScoreSheetDefinition scoreSheet = scoreSheetRepository
                .findByVersionId(reviewCase.getScoreSheetVersionRef())
                .orElseThrow(() -> new ResourceNotFoundException("SCORE_SHEET_NOT_FOUND", "ScoreSheetDefinition not found: " + reviewCase.getScoreSheetVersionRef()));

        List<AnswerItem> rawItems = RevisionChecks.rawAnswers(answers);
        // D-083 第 1 點：必答題與提交相同（D-076）；第 2 點：沒有變更回 NO_CHANGES（D-038）
        RevisionChecks.requireRequiredItemsAnswered(rawItems, scoreSheet);
        RevisionChecks.rejectIfUnchanged(rawItems, comment, oldSubmission);

        ScoreResult result = scoreCalculationService.calculate(rawItems, scoreSheet);

        // D-083 第 6 點：新提交 ID 一律是 UUID
        String newSubmissionId = idGenerator.newId();
        Instant now = Instant.now(clock);
        ReviewSubmission revision = ReviewSubmission.createRevision(
                newSubmissionId, oldSubmission, result.getScoredAnswers(),
                result.getTotalScore(), comment, now);
        oldSubmission.markSuperseded(newSubmissionId, now);

        // D-068 第 4 點的已知問題：加權取專案目前的版本（D-083 第 11 點，與提交複審相同）
        ReviewProject project = projectRepository.findById(reviewCase.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("PROJECT_NOT_FOUND", "ReviewProject not found: " + reviewCase.getProjectId()));
        ProjectVersion version = project.getCurrentVersion();

        // D-083 第 9 點：由 domain 檢查目前有效、重算 finalScore、改指新提交；status 不變
        reviewCase.recordRereviewRevision(
                submissionId, newSubmissionId, result.getTotalScore(),
                version.getInitialReviewWeight(), version.getRereviewWeight());

        submissionRepository.save(oldSubmission);
        submissionRepository.save(revision);
        caseRepository.save(reviewCase);

        // D-083 第 3 點：與修正初審對稱先發 ReviewSubmissionRevised（不帶分數），再發 RereviewRevised（帶分數，D-071 遮罩）
        eventPublisher.publish(new ReviewSubmissionRevisedEvent(
                this, actor.getUserId(), actor.getUserName(),
                reviewCase.getHospitalId(), caseId,
                submissionId, newSubmissionId,
                ReviewStage.REREVIEW.name(), revision.getRevisionNo()));
        eventPublisher.publish(new RereviewRevisedEvent(
                this, actor.getUserId(), actor.getUserName(),
                reviewCase.getHospitalId(), caseId,
                submissionId, newSubmissionId,
                result.getTotalScore(), reviewCase.getFinalScore()));

        // D-033：輸出只有自己階段的分數，沒有 finalScore 與初審分數
        return new RevisionResult(
                newSubmissionId, submissionId,
                revision.getRevisionNo(),
                result.getTotalScore(),
                revision.getStatus().name(),
                revision.getStage().name(),
                caseId);
    }
}
