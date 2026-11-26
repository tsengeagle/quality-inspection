package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.DomainValidationException;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.event.ReviewCaseCancelledEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Instant;
import com.tzuchi.his.quality.inspection.application.common.IdGenerator;

@Service
@Transactional
public class CancelReviewCaseUseCase {

    private final ReviewCaseRepository repository;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;
    private final IdGenerator idGenerator;

    public CancelReviewCaseUseCase(ReviewCaseRepository repository,
                                    DomainEventPublisher eventPublisher,
                                    Clock clock,
                                   IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    public void execute(ActorContext actor, String caseId, String reason) {
        Instant now = Instant.now(clock);
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);

        if (reason == null || reason.isBlank()) {
            throw new DomainValidationException("VALIDATION_ERROR", "Cancel reason must not be blank");
        }

        ReviewCase reviewCase = repository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("CASE_NOT_FOUND",
                        "ReviewCase not found: " + caseId));

        actor.requireHospitalScope(reviewCase.getHospitalId());

        reviewCase.cancel(reason, now);
        repository.save(reviewCase);

        eventPublisher.publish(new ReviewCaseCancelledEvent(
                this, idGenerator.newId(), now, actor.getUserId(), actor.getUserName(),
                reviewCase.getHospitalId(), caseId, reason));
    }
}
