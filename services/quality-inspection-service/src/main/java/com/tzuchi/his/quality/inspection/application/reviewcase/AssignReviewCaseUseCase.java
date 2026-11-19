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

import java.time.LocalDateTime;
import java.time.Clock;

@Service
@Transactional
public class AssignReviewCaseUseCase {

    private final ReviewCaseRepository repository;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;

    public AssignReviewCaseUseCase(ReviewCaseRepository repository,
                                    DomainEventPublisher eventPublisher,
                                    Clock clock) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    public void execute(ActorContext actor, String caseId, String reviewerId, LocalDateTime dueAt) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);

        ReviewCase reviewCase = repository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("CASE_NOT_FOUND",
                        "ReviewCase not found: " + caseId));

        actor.requireHospitalScope(reviewCase.getHospitalId());

        reviewCase.assignReviewer(reviewerId, dueAt, LocalDateTime.now(clock));
        repository.save(reviewCase);

        eventPublisher.publish(new ReviewCaseAssignedEvent(
                this, actor.getUserId(), actor.getUserName(),
                reviewCase.getHospitalId(), caseId, reviewerId, dueAt));
    }
}
