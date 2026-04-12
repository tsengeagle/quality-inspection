package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.SubmissionDetailView;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.SubmissionQueryPort;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class GetSubmissionDetailUseCase {

    private final SubmissionQueryPort queryPort;

    public GetSubmissionDetailUseCase(SubmissionQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    public SubmissionDetailView execute(ActorContext actor, String submissionId) {
        actor.requireAnyRole(
                DomainRole.HOSPITAL_MANAGER, DomainRole.REVIEWER,
                DomainRole.REREVIEWER, DomainRole.AUDITOR, DomainRole.SYSTEM_ADMIN);

        SubmissionDetailView view = queryPort.findDetailById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("ReviewSubmission", submissionId));

        boolean shouldMask = actor.hasRole(DomainRole.REREVIEWER)
                && !queryPort.existsSubmittedRereview(view.getCaseId());

        if (!shouldMask) {
            return view;
        }

        return new SubmissionDetailView(
                view.getSubmissionId(),
                view.getCaseId(),
                view.getStage(),
                view.getRevisionNo(),
                view.getStatus(),
                view.getReviewerId(),
                null,
                null,
                view.getAttachmentRefsJson(),
                null,
                view.getCreatedAt(),
                view.getSubmittedAt(),
                List.of("answers", "comment", "totalScore"));
    }
}
