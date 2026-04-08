package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.event.ReviewCaseAssignedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReopenInitialReviewUseCase {

    private final ReviewCaseRepository repository;
    private final DomainEventPublisher eventPublisher;

    public ReopenInitialReviewUseCase(ReviewCaseRepository repository,
                                       DomainEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    public void execute(ActorContext actor, String caseId) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);

        ReviewCase reviewCase = repository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("CASE_NOT_FOUND",
                        "ReviewCase not found: " + caseId));

        actor.requireHospitalScope(reviewCase.getHospitalId());

        reviewCase.reopenInitialReview();
        repository.save(reviewCase);

        eventPublisher.publish(new ReviewCaseAssignedEvent(
                this, actor.getUserId(), actor.getUserName(),
                reviewCase.getHospitalId(), caseId,
                reviewCase.getReviewerAssignment().getReviewerId(),
                reviewCase.getDueAt()));
    }
}
