package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.IdGenerator;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.event.RereviewStartedEvent;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class StartRereviewUseCase {

    private final ReviewCaseRepository caseRepository;
    private final ReviewSubmissionRepository submissionRepository;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;
    private final IdGenerator idGenerator;

    public StartRereviewUseCase(ReviewCaseRepository caseRepository,
                                 ReviewSubmissionRepository submissionRepository,
                                 DomainEventPublisher eventPublisher,
                                 Clock clock,
                                 IdGenerator idGenerator) {
        this.caseRepository = caseRepository;
        this.submissionRepository = submissionRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
        this.idGenerator = idGenerator;
    }

    @Transactional
    public String execute(ActorContext actor, String caseId) {
        actor.requireRole(DomainRole.REREVIEWER);
        ReviewCase reviewCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("CASE_NOT_FOUND", "ReviewCase not found: " + caseId));
        actor.requireHospitalScope(reviewCase.getHospitalId());
        // D-068 第 2 點：不是受指派的複審醫師回 403，與開始初審相同；沒有指派時交給 domain 的狀態檢查
        if (reviewCase.getRereviewerAssignment() != null &&
                !actor.getUserId().equals(reviewCase.getRereviewerAssignment().getReviewerId())) {
            throw new AccessDeniedException("NOT_ASSIGNED_REREVIEWER",
                    "Only the assigned rereviewer can start the rereview");
        }
        reviewCase.startRereview(actor.getUserId());
        ReviewSubmission rereviewDraft = ReviewSubmission.createRereviewDraft(
                idGenerator.newId(), caseId, actor.getUserId(), Instant.now(clock));
        submissionRepository.save(rereviewDraft);
        reviewCase.setLatestRereviewSubmissionId(rereviewDraft.getSubmissionId());
        caseRepository.save(reviewCase);
        eventPublisher.publish(new RereviewStartedEvent(
                this, actor.getUserId(), actor.getUserName(), reviewCase.getHospitalId(),
                reviewCase.getCaseId(), rereviewDraft.getSubmissionId(), actor.getUserId()));
        return rereviewDraft.getSubmissionId();
    }
}
