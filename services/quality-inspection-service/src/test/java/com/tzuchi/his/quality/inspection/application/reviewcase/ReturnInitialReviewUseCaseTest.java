package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.RoleDeniedException;
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewerAssignment;
import com.tzuchi.his.quality.inspection.domain.reviewcase.event.ReviewCaseReturnedEvent;
import com.tzuchi.his.quality.inspection.testfixture.CapturingDomainEventPublisher;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryReviewCaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ReturnInitialReviewUseCaseTest {

    private InMemoryReviewCaseRepository repository;
    private CapturingDomainEventPublisher eventPublisher;
    private ReturnInitialReviewUseCase useCase;

    private static final Instant DUE = Instant.parse("2026-02-01T23:59:00Z");

    @BeforeEach
    void setUp() {
        repository = new InMemoryReviewCaseRepository();
        eventPublisher = new CapturingDomainEventPublisher();
        useCase = new ReturnInitialReviewUseCase(repository, eventPublisher);
    }

    private ActorContext hospitalManager() {
        return new ActorContext("u1", "Manager", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));
    }

    private ReviewCase createInProgressCase() {
        // Use reconstitute() since forceStatus is package-private in domain.reviewcase
        ReviewCase rc = ReviewCase.reconstitute(
                "c1", "b1", "p1", "H001", "SK-001", MedicalRecordType.OUTPATIENT,
                CaseStatus.INITIAL_REVIEW_IN_PROGRESS,
                new ReviewerAssignment("reviewer-1", Instant.now()),
                DUE,
                null, null, null, null,
                false, null, null, null, null, null,
                null, null, null, null);
        repository.save(rc);
        return rc;
    }

    @Test
    void execute_happyPath_shouldReturn() {
        createInProgressCase();

        useCase.execute(hospitalManager(), "c1", "needs correction");

        assertEquals(CaseStatus.RETURNED, repository.store.get("c1").getStatus());
        assertEquals("needs correction", repository.store.get("c1").getReturnReason());
    }

    @Test
    void execute_shouldPublishReturnedEvent() {
        createInProgressCase();

        useCase.execute(hospitalManager(), "c1", "reason");

        assertEquals(1, eventPublisher.events.size());
        assertInstanceOf(ReviewCaseReturnedEvent.class, eventPublisher.events.get(0));
    }

    @Test
    void execute_wrongRole_shouldThrow() {
        ActorContext admin = new ActorContext("u1", "Admin", EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.of("H001"));

        assertThrows(RoleDeniedException.class,
                () -> useCase.execute(admin, "c1", "reason"));
    }

    @Test
    void execute_crossHospital_shouldThrow() {
        createInProgressCase();
        ActorContext other = new ActorContext("u1", "Manager", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H999"));

        assertThrows(AccessDeniedException.class,
                () -> useCase.execute(other, "c1", "reason"));
    }

    @Test
    void execute_notFound_shouldThrow() {
        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(hospitalManager(), "no-such", "reason"));
    }

    @Test
    void execute_notInProgress_shouldThrow() {
        ReviewCase rc = ReviewCase.createFromSampling("c1", "b1", "p1", "H001", "SK-001", MedicalRecordType.OUTPATIENT);
        repository.save(rc); // SELECTED_PENDING_ASSIGNMENT

        assertThrows(DomainStateConflictException.class,
                () -> useCase.execute(hospitalManager(), "c1", "reason"));
    }
}
