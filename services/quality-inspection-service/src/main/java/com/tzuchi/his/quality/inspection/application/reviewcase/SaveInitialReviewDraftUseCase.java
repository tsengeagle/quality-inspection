package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.reviewcase.dto.AnswerInput;
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.SubmissionStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.DraftContent;
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
import java.time.Clock;
import java.time.LocalDateTime;

@Service
@Transactional
public class SaveInitialReviewDraftUseCase {

    private final ReviewSubmissionRepository submissionRepository;
    private final ScoreSheetRepository scoreSheetRepository;
    private final DomainEventPublisher eventPublisher;
    private final ScoreCalculationService scoreCalculationService;
    private final ReviewCaseRepository caseRepository;
    private final Clock clock;

    public SaveInitialReviewDraftUseCase(ReviewSubmissionRepository submissionRepository,
                                          ScoreSheetRepository scoreSheetRepository,
                                          DomainEventPublisher eventPublisher,
                                          ScoreCalculationService scoreCalculationService,
                                          ReviewCaseRepository caseRepository,
                                          Clock clock) {
        this.submissionRepository = submissionRepository;
        this.scoreSheetRepository = scoreSheetRepository;
        this.eventPublisher = eventPublisher;
        this.scoreCalculationService = scoreCalculationService;
        this.caseRepository = caseRepository;
        this.clock = clock;
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
            // D-068 第 1 點：與開始、提交初審共用代碼（422）
            throw new DomainRuleViolationException("CASE_NOT_IN_INITIAL_REVIEW_STAGE",
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
            throw new AccessDeniedException("SUBMISSION_NOT_OWNED_BY_ACTOR",
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
            rejectIfUnchanged(Collections.emptyList(), comment, submission);
            submission.updateDraft(Collections.emptyList(), BigDecimal.ZERO, comment, LocalDateTime.now(clock));
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
                .findByVersionId(submission.getBasedOnScoreSheetVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("SCORE_SHEET_NOT_FOUND",
                        "ScoreSheetDefinition not found: " + submission.getBasedOnScoreSheetVersionId()));

        // Calculate scores
        ScoreResult result = scoreCalculationService.calculate(rawItems, scoreSheet);

        // 計分時會丟掉評分表沒有的題目，存下的也是過濾後的答案；所以用計分後的答案比較
        rejectIfUnchanged(result.getScoredAnswers(), comment, submission);

        // Update draft
        submission.updateDraft(result.getScoredAnswers(), result.getTotalScore(), comment, LocalDateTime.now(clock));
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

    /**
     * D-038、D-044：原有檢查都通過之後，業務內容沒有變更就拒絕，不寫入、不發事件。
     * 初審的分數由系統依答案計算，不是輸入，所以只比答案（題目 id 與答案值）與評語。
     */
    private static void rejectIfUnchanged(List<AnswerItem> incoming, String comment, ReviewSubmission submission) {
        if (new DraftContent(raw(incoming), null, comment)
                .sameAs(new DraftContent(raw(submission.getAnswerSet()), null, submission.getComment()))) {
            throw new DomainStateConflictException("NO_CHANGES", "Draft content is unchanged");
        }
    }

    private static List<AnswerItem> raw(List<AnswerItem> answers) {
        return answers.stream()
                .map(a -> AnswerItem.ofRaw(a.getItemId(), a.getAnswerValue()))
                .collect(Collectors.toList());
    }
}
