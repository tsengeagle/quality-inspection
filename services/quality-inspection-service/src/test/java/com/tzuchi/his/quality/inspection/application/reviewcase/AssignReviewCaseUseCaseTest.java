package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.RoleDeniedException;
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.event.ReviewCaseAssignedEvent;
import com.tzuchi.his.quality.inspection.testfixture.CapturingDomainEventPublisher;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryReviewCaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Clock;

class AssignReviewCaseUseCaseTest {

    private InMemoryReviewCaseRepository repository;
    private CapturingDomainEventPublisher eventPublisher;
    private AssignReviewCaseUseCase useCase;

    private static final Instant DUE = Instant.parse("2026-02-01T23:59:00Z");

    @BeforeEach
    void setUp() {
        repository = new InMemoryReviewCaseRepository();
        eventPublisher = new CapturingDomainEventPublisher();
        useCase = new AssignReviewCaseUseCase(repository, eventPublisher, Clock.systemDefaultZone(), () -> java.util.UUID.randomUUID().toString());
    }

    private ReviewCase createCase() {
        ReviewCase rc = ReviewCase.createFromSampling("c1", "b1", "p1", "H001", "SK-001", MedicalRecordType.OUTPATIENT);
        repository.save(rc);
        return rc;
    }

    private ActorContext hospitalManager() {
        return new ActorContext("u1", "Manager", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));
    }

    @Test
    void execute_happyPath_shouldAssign() {
        createCase();
        useCase.execute(hospitalManager(), "c1", "reviewer-1", DUE);

        ReviewCase saved = repository.store.get("c1");
        assertEquals(CaseStatus.PENDING_INITIAL_REVIEW, saved.getStatus());
        assertEquals("reviewer-1", saved.getReviewerAssignment().getReviewerId());
    }

    @Test
    void execute_shouldPublishAssignedEvent() {
        createCase();
        useCase.execute(hospitalManager(), "c1", "reviewer-1", DUE);

        assertEquals(1, eventPublisher.events.size());
        assertInstanceOf(ReviewCaseAssignedEvent.class, eventPublisher.events.get(0));
    }

    @Test
    void execute_wrongRole_shouldThrow() {
        ActorContext admin = new ActorContext("u1", "Admin", EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.of("H001"));

        assertThrows(RoleDeniedException.class,
                () -> useCase.execute(admin, "c1", "reviewer-1", DUE));
    }

    @Test
    void execute_crossHospital_shouldThrow() {
        createCase();
        ActorContext other = new ActorContext("u1", "Manager", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H999"));

        assertThrows(AccessDeniedException.class,
                () -> useCase.execute(other, "c1", "reviewer-1", DUE));
    }

    @Test
    void execute_notFound_shouldThrow() {
        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(hospitalManager(), "no-such", "reviewer-1", DUE));
    }

    @Test
    void execute_alreadyAssigned_shouldThrow() {
        createCase();
        useCase.execute(hospitalManager(), "c1", "reviewer-1", DUE);

        DomainRuleViolationException e = assertThrows(DomainRuleViolationException.class,
                () -> useCase.execute(hospitalManager(), "c1", "reviewer-2", DUE));
        assertEquals("CASE_NOT_ASSIGNABLE", e.getErrorCode());
    }
}
