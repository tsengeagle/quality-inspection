package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.event.ReviewCaseReassignedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Clock;
import com.tzuchi.his.quality.inspection.application.common.IdGenerator;

@Service
@Transactional
public class ReassignReviewCaseUseCase {

    private final ReviewCaseRepository repository;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;
    private final IdGenerator idGenerator;

    public ReassignReviewCaseUseCase(ReviewCaseRepository repository,
                                      DomainEventPublisher eventPublisher,
                                      Clock clock,
                                     IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    public void execute(ActorContext actor, String caseId, String reviewerId, Instant dueAt) {
        Instant now = Instant.now(clock);
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);

        ReviewCase reviewCase = repository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("CASE_NOT_FOUND",
                        "ReviewCase not found: " + caseId));

        actor.requireHospitalScope(reviewCase.getHospitalId());

        String previousReviewerId = reviewCase.getReviewerAssignment() != null
                ? reviewCase.getReviewerAssignment().getReviewerId() : null;

        reviewCase.reassignReviewer(reviewerId, dueAt, now);
        repository.save(reviewCase);

        eventPublisher.publish(new ReviewCaseReassignedEvent(
                this, idGenerator.newId(), now, actor.getUserId(), actor.getUserName(),
                reviewCase.getHospitalId(), caseId,
                previousReviewerId, reviewerId, dueAt));
    }
}
