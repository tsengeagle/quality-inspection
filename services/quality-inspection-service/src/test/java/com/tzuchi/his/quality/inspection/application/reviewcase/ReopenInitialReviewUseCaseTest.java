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
import com.tzuchi.his.quality.inspection.domain.reviewcase.event.ReviewCaseReopenedEvent;
import com.tzuchi.his.quality.inspection.testfixture.CapturingDomainEventPublisher;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryReviewCaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ReopenInitialReviewUseCaseTest {

    private InMemoryReviewCaseRepository repository;
    private CapturingDomainEventPublisher eventPublisher;
    private ReopenInitialReviewUseCase useCase;

    private static final Instant DUE = Instant.parse("2026-02-01T23:59:00Z");

    @BeforeEach
    void setUp() {
        repository = new InMemoryReviewCaseRepository();
        eventPublisher = new CapturingDomainEventPublisher();
        useCase = new ReopenInitialReviewUseCase(repository, eventPublisher);
    }

    private ActorContext hospitalManager() {
        return new ActorContext("u1", "Manager", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));
    }

    private ReviewCase createReturnedCase() {
        // Use reconstitute() since forceStatus is package-private in domain.reviewcase
        ReviewCase rc = ReviewCase.reconstitute(
                "c1", "b1", "p1", "H001", "SK-001", MedicalRecordType.OUTPATIENT,
                CaseStatus.RETURNED,
                new ReviewerAssignment("reviewer-1", Instant.now()),
                DUE,
                null, "needs correction", null, null,
                false, null, null, null, null, null,
                null, null, null, null);
        repository.save(rc);
        return rc;
    }

    @Test
    void execute_happyPath_shouldReopen() {
        createReturnedCase();

        useCase.execute(hospitalManager(), "c1");

        ReviewCase saved = repository.store.get("c1");
        assertEquals(CaseStatus.PENDING_INITIAL_REVIEW, saved.getStatus());
        assertNull(saved.getReturnReason());
    }

    @Test
    void execute_shouldPublishReopenedEvent() {
        createReturnedCase();

        useCase.execute(hospitalManager(), "c1");

        assertEquals(1, eventPublisher.events.size());
        ReviewCaseReopenedEvent event = (ReviewCaseReopenedEvent) eventPublisher.events.get(0);
        assertEquals("c1", event.getCaseId());
        assertEquals("REOPENED", event.getAction());
        assertEquals("reviewer-1", event.getReviewerId());
    }

    @Test
    void execute_wrongRole_shouldThrow() {
        ActorContext admin = new ActorContext("u1", "Admin", EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.of("H001"));

        assertThrows(RoleDeniedException.class,
                () -> useCase.execute(admin, "c1"));
    }

    @Test
    void execute_crossHospital_shouldThrow() {
        createReturnedCase();
        ActorContext other = new ActorContext("u1", "Manager", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H999"));

        assertThrows(AccessDeniedException.class,
                () -> useCase.execute(other, "c1"));
    }

    @Test
    void execute_notFound_shouldThrow() {
        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(hospitalManager(), "no-such"));
    }

    @Test
    void execute_notReturned_shouldThrow() {
        ReviewCase rc = ReviewCase.createFromSampling("c1", "b1", "p1", "H001", "SK-001", MedicalRecordType.OUTPATIENT);
        rc.assignReviewer("reviewer-1", DUE, Instant.now());
        repository.save(rc); // PENDING_INITIAL_REVIEW

        assertThrows(DomainStateConflictException.class,
                () -> useCase.execute(hospitalManager(), "c1"));
    }
}
