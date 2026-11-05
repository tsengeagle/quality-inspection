package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.reviewcase.command.SaveRereviewDraftCommand;
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewStage;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.SubmissionStatus;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;

@Service
public class SaveRereviewDraftUseCase {

    private final ReviewCaseRepository caseRepository;
    private final ReviewSubmissionRepository submissionRepository;

    public SaveRereviewDraftUseCase(ReviewCaseRepository caseRepository,
                                     ReviewSubmissionRepository submissionRepository) {
        this.caseRepository = caseRepository;
        this.submissionRepository = submissionRepository;
    }

    @Transactional
    public SaveDraftResult execute(ActorContext actor, SaveRereviewDraftCommand cmd) {
        actor.requireRole(DomainRole.REREVIEWER);
        ReviewCase reviewCase = caseRepository.findById(cmd.getCaseId())
                .orElseThrow(() -> new ResourceNotFoundException("ReviewCase", cmd.getCaseId()));
        actor.requireHospitalScope(reviewCase.getHospitalId());
        reviewCase.requireNotTerminal();
        if (reviewCase.getStatus() != CaseStatus.REREVIEW_IN_PROGRESS) {
            throw new DomainStateConflictException("CASE_NOT_REREVIEW_IN_PROGRESS",
                    "Case must be in REREVIEW_IN_PROGRESS to save a rereview draft");
        }
        if (reviewCase.getRereviewerAssignment() == null
                || !actor.getUserId().equals(reviewCase.getRereviewerAssignment().getReviewerId())) {
            throw new DomainRuleViolationException("NOT_ASSIGNED_REREVIEWER",
                    "Only the assigned rereviewer can save this draft");
        }
        ReviewSubmission submission = submissionRepository.findById(cmd.getSubmissionId())
                .orElseThrow(() -> new ResourceNotFoundException("ReviewSubmission", cmd.getSubmissionId()));
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

        submission.updateDraft(
                cmd.getAnswers() != null ? cmd.getAnswers() : Collections.emptyList(),
                cmd.getTotalScore() != null ? cmd.getTotalScore() : BigDecimal.ZERO,
                cmd.getComment());
        submissionRepository.save(submission);
        return new SaveDraftResult(
                submission.getSubmissionId(),
                submission.getStatus().name(),
                submission.getStage().name(),
                submission.getTotalScore());
    }
}
