package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.reviewcase.command.SubmitRereviewCommand;
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewStage;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.SubmissionStatus;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.project.ProjectVersion;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.event.RereviewSubmittedEvent;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmissionRepository;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetDefinition;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class SubmitRereviewUseCase {

    private final ReviewCaseRepository caseRepository;
    private final ReviewSubmissionRepository submissionRepository;
    private final ReviewProjectRepository projectRepository;
    private final ScoreSheetRepository scoreSheetRepository;
    private final DomainEventPublisher eventPublisher;
    private final ScoreCalculationService scoreCalculationService;
    private final Clock clock;

    public SubmitRereviewUseCase(ReviewCaseRepository caseRepository,
                                  ReviewSubmissionRepository submissionRepository,
                                  ReviewProjectRepository projectRepository,
                                  ScoreSheetRepository scoreSheetRepository,
                                  DomainEventPublisher eventPublisher,
                                  ScoreCalculationService scoreCalculationService,
                                  Clock clock) {
        this.caseRepository = caseRepository;
        this.submissionRepository = submissionRepository;
        this.projectRepository = projectRepository;
        this.scoreSheetRepository = scoreSheetRepository;
        this.eventPublisher = eventPublisher;
        this.scoreCalculationService = scoreCalculationService;
        this.clock = clock;
    }

    @Transactional
    public SubmitRereviewResult execute(ActorContext actor, SubmitRereviewCommand cmd) {
        actor.requireRole(DomainRole.REREVIEWER);

        ReviewCase reviewCase = caseRepository.findById(cmd.getCaseId())
                .orElseThrow(() -> new ResourceNotFoundException("CASE_NOT_FOUND", "ReviewCase not found: " + cmd.getCaseId()));

        actor.requireHospitalScope(reviewCase.getHospitalId());
        reviewCase.requireNotTerminal();
        if (reviewCase.getStatus() != CaseStatus.REREVIEW_IN_PROGRESS) {
            throw new DomainStateConflictException("CASE_NOT_REREVIEW_IN_PROGRESS",
                    "Case must be in REREVIEW_IN_PROGRESS to submit");
        }

        // Validate actor is the assigned rereviewer
        if (reviewCase.getRereviewerAssignment() == null
                || !actor.getUserId().equals(reviewCase.getRereviewerAssignment().getReviewerId())) {
            throw new AccessDeniedException("NOT_ASSIGNED_REREVIEWER",
                    "Only the assigned rereviewer can submit this rereview");
        }

        ReviewSubmission submission = submissionRepository.findById(cmd.getSubmissionId())
                .orElseThrow(() -> new ResourceNotFoundException("SUBMISSION_NOT_FOUND", "ReviewSubmission not found: " + cmd.getSubmissionId()));

        if (!reviewCase.getCaseId().equals(submission.getCaseId())
                || submission.getStage() != ReviewStage.REREVIEW) {
            throw new DomainStateConflictException("CONFLICT",
                    "Submission must belong to this case and REREVIEW stage");
        }
        if (!actor.getUserId().equals(submission.getReviewerId())) {
            throw new AccessDeniedException("NOT_SUBMISSION_OWNER",
                    "Only the submission owner can perform this operation");
        }
        if (submission.getStatus() != SubmissionStatus.DRAFT) {
            throw new DomainStateConflictException("SUBMISSION_NOT_DRAFT",
                    "Submission must be DRAFT");
        }
        if (!submission.getSubmissionId().equals(reviewCase.getLatestRereviewSubmissionId())
                || submission.getSupersededBySubmissionId() != null) {
            throw new DomainStateConflictException("CONFLICT",
                    "Submission is not the current rereview result");
        }

        // Load score sheet for scoring
        ScoreSheetDefinition scoreSheet = scoreSheetRepository
                .findByVersionId(reviewCase.getScoreSheetVersionRef())
                .orElseThrow(() -> new ResourceNotFoundException("SCORE_SHEET_NOT_FOUND", "ScoreSheetDefinition not found: " + reviewCase.getScoreSheetVersionRef()));

        // Get current answers (from command if provided, else from draft)
        List<AnswerItem> currentAnswers = cmd.getAnswers() != null
                ? cmd.getAnswers()
                : submission.getAnswerSet();

        // Calculate scores server-side
        List<AnswerItem> rawItems = currentAnswers.stream()
                .map(a -> AnswerItem.ofRaw(a.getItemId(), a.getAnswerValue()))
                .collect(Collectors.toList());
        ScoreResult result = scoreCalculationService.calculate(rawItems, scoreSheet);

        // Update draft with scored answers, then submit
        LocalDateTime now = LocalDateTime.now(clock);
        submission.updateDraft(result.getScoredAnswers(), result.getTotalScore(), cmd.getComment(), now);
        submission.submit(now);
        submissionRepository.save(submission);

        // Load project for weights
        ReviewProject project = projectRepository.findById(reviewCase.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("PROJECT_NOT_FOUND", "ReviewProject not found: " + reviewCase.getProjectId()));
        ProjectVersion version = project.getCurrentVersion();

        // 計算並保存加權 finalScore；不結案（D-032、D-053 第 4 點），結案由 CloseRereviewUseCase 另行執行
        reviewCase.completeRereview(
                result.getTotalScore(),
                version.getInitialReviewWeight(),
                version.getRereviewWeight());
        caseRepository.save(reviewCase);

        eventPublisher.publish(new RereviewSubmittedEvent(
                this, actor.getUserId(), actor.getUserName(), reviewCase.getHospitalId(),
                reviewCase.getCaseId(),
                String.format("{\"rereviewScore\":%s,\"finalScore\":%s}",
                        result.getTotalScore(), reviewCase.getFinalScore())));

        return new SubmitRereviewResult(
                submission.getSubmissionId(),
                submission.getStatus().name(),
                cmd.getCaseId(),
                reviewCase.getStatus().name());
    }
}
