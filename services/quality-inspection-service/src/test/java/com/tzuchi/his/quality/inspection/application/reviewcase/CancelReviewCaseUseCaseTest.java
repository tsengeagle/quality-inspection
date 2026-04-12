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
import com.tzuchi.his.quality.inspection.domain.reviewcase.event.ReviewCaseCancelledEvent;
import com.tzuchi.his.quality.inspection.testfixture.CapturingDomainEventPublisher;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryReviewCaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CancelReviewCaseUseCaseTest {

    private InMemoryReviewCaseRepository repository;
    private CapturingDomainEventPublisher eventPublisher;
    private CancelReviewCaseUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryReviewCaseRepository();
        eventPublisher = new CapturingDomainEventPublisher();
        useCase = new CancelReviewCaseUseCase(repository, eventPublisher);
    }

    private ActorContext hospitalManager() {
        return new ActorContext("u1", "Manager", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));
    }

    @Test
    void execute_fromSelectedPending_shouldCancel() {
        ReviewCase rc = ReviewCase.createFromSampling("c1", "b1", "p1", "H001", "SK-001", MedicalRecordType.OUTPATIENT);
        repository.save(rc);

        useCase.execute(hospitalManager(), "c1", "project cancelled");

        assertEquals(CaseStatus.CANCELLED, repository.store.get("c1").getStatus());
        assertEquals("project cancelled", repository.store.get("c1").getCancelReason());
    }

    @Test
    void execute_shouldPublishCancelledEvent() {
        ReviewCase rc = ReviewCase.createFromSampling("c1", "b1", "p1", "H001", "SK-001", MedicalRecordType.OUTPATIENT);
        repository.save(rc);

        useCase.execute(hospitalManager(), "c1", "reason");

        assertEquals(1, eventPublisher.events.size());
        assertInstanceOf(ReviewCaseCancelledEvent.class, eventPublisher.events.get(0));
    }

    @Test
    void execute_wrongRole_shouldThrow() {
        ActorContext admin = new ActorContext("u1", "Admin", EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.of("H001"));

        assertThrows(RoleDeniedException.class,
                () -> useCase.execute(admin, "c1", "reason"));
    }

    @Test
    void execute_crossHospital_shouldThrow() {
        ReviewCase rc = ReviewCase.createFromSampling("c1", "b1", "p1", "H001", "SK-001", MedicalRecordType.OUTPATIENT);
        repository.save(rc);
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
    void execute_fromClosed_shouldThrow() {
        // Use reconstitute() since forceStatus is package-private in domain.reviewcase
        ReviewCase rc = ReviewCase.reconstitute(
                "c1", "b1", "p1", "H001", "SK-001", MedicalRecordType.OUTPATIENT,
                CaseStatus.CLOSED, null, null,
                null, null, LocalDateTime.now(), LocalDateTime.now(),
                false, null, null, null, null, null,
                null, null, null);
        repository.save(rc);

        assertThrows(DomainStateConflictException.class,
                () -> useCase.execute(hospitalManager(), "c1", "reason"));
    }
}
