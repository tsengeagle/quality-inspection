package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.reviewcase.command.SaveRereviewDraftCommand;
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
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
        if (reviewCase.getRereviewerAssignment() == null
                || !actor.getUserId().equals(reviewCase.getRereviewerAssignment().getReviewerId())) {
            throw new DomainRuleViolationException("NOT_ASSIGNED_REREVIEWER",
                    "Only the assigned rereviewer can save this draft");
        }
        ReviewSubmission submission = submissionRepository.findById(cmd.getSubmissionId())
                .orElseThrow(() -> new ResourceNotFoundException("ReviewSubmission", cmd.getSubmissionId()));
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
