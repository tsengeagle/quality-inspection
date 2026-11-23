package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.reviewcase.dto.AnswerInput;
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.DraftContent;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewStage;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.SubmissionStatus;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmissionRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.event.ReviewDraftSavedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.time.Clock;
import java.time.Instant;

@Service
public class SaveRereviewDraftUseCase {

    private final ReviewCaseRepository caseRepository;
    private final ReviewSubmissionRepository submissionRepository;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;

    public SaveRereviewDraftUseCase(ReviewCaseRepository caseRepository,
                                     ReviewSubmissionRepository submissionRepository,
                                     DomainEventPublisher eventPublisher,
                                     Clock clock) {
        this.caseRepository = caseRepository;
        this.submissionRepository = submissionRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public SaveDraftResult execute(ActorContext actor, String caseId, String submissionId,
                                   List<AnswerInput> answers, String comment) {
        actor.requireRole(DomainRole.REREVIEWER);
        ReviewCase reviewCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("CASE_NOT_FOUND", "ReviewCase not found: " + caseId));
        actor.requireHospitalScope(reviewCase.getHospitalId());
        reviewCase.requireNotTerminal();
        if (reviewCase.getStatus() != CaseStatus.REREVIEW_IN_PROGRESS) {
            // D-069 第 3 點：複審的狀態錯誤共用一個代碼（422）
            throw new DomainRuleViolationException("CASE_NOT_IN_REREVIEW_STAGE",
                    "Case must be in REREVIEW_IN_PROGRESS to save a rereview draft");
        }
        if (reviewCase.getRereviewerAssignment() == null
                || !actor.getUserId().equals(reviewCase.getRereviewerAssignment().getReviewerId())) {
            throw new AccessDeniedException("NOT_ASSIGNED_REREVIEWER",
                    "Only the assigned rereviewer can save this draft");
        }
        ReviewSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("SUBMISSION_NOT_FOUND", "ReviewSubmission not found: " + submissionId));
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

        List<AnswerItem> rawAnswers = answers == null ? Collections.emptyList()
                : answers.stream().map(a -> AnswerItem.ofRaw(a.getItemId(), a.getAnswerValue())).collect(Collectors.toList());
        // D-038、D-044：原有檢查都通過之後，業務內容沒有變更就拒絕，不寫入
        // D-069 第 7 點：複審草稿的分數不是輸入，提交時才由系統計分；只比答案與評語，分數維持原值
        if (new DraftContent(rawAnswers, null, comment)
                .sameAs(new DraftContent(raw(submission.getAnswerSet()), null, submission.getComment()))) {
            throw new DomainStateConflictException("NO_CHANGES", "Draft content is unchanged");
        }

        BigDecimal keptScore = submission.getTotalScore() != null ? submission.getTotalScore() : BigDecimal.ZERO;
        submission.updateDraft(rawAnswers, keptScore, comment, Instant.now(clock));
        submissionRepository.save(submission);

        // D-076 第 1 點：與初審一致發 ReviewDraftSaved；複審草稿沒有有意義的分數，detail 的 totalScore 寫 null
        eventPublisher.publish(new ReviewDraftSavedEvent(
                this, actor.getUserId(), actor.getUserName(),
                reviewCase.getHospitalId(), caseId, submission.getSubmissionId(), null));

        return new SaveDraftResult(
                submission.getSubmissionId(),
                submission.getStatus().name(),
                submission.getStage().name(),
                submission.getTotalScore());
    }

    private static List<AnswerItem> raw(List<AnswerItem> answers) {
        return answers.stream()
                .map(a -> AnswerItem.ofRaw(a.getItemId(), a.getAnswerValue()))
                .collect(Collectors.toList());
    }
}
