package com.tzuchi.his.quality.inspection.application.project;

import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.UnauthorizedException;
import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import com.tzuchi.his.quality.inspection.domain.project.event.ReviewProjectDraftUpdatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class UpdateReviewProjectDraftUseCaseTest {

    private InMemoryProjectRepo repository;
    private CapturingEventPublisher eventPublisher;
    private UpdateReviewProjectDraftUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryProjectRepo();
        eventPublisher = new CapturingEventPublisher();
        useCase = new UpdateReviewProjectDraftUseCase(repository, eventPublisher);
    }

    @Test
    void execute_happyPath_shouldUpdateDraft() {
        ReviewProject project = ReviewProject.create("p1", "H001", "Old",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        repository.save(project);
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        useCase.execute(actor, "p1", "New Name", "New Desc",
                EnumSet.of(MedicalRecordType.INPATIENT));

        ReviewProject updated = repository.store.get("p1");
        assertEquals("New Name", updated.getName());
        assertEquals("New Desc", updated.getDescription());
    }

    @Test
    void execute_shouldPublishEvent() {
        ReviewProject project = ReviewProject.create("p1", "H001", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        repository.save(project);
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        useCase.execute(actor, "p1", "New", null,
                EnumSet.of(MedicalRecordType.OUTPATIENT));

        assertEquals(1, eventPublisher.events.size());
        assertInstanceOf(ReviewProjectDraftUpdatedEvent.class, eventPublisher.events.get(0));
    }

    @Test
    void execute_wrongRole_shouldThrow() {
        ActorContext actor = new ActorContext("u1", "Admin",
                EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.of("H001"));

        assertThrows(UnauthorizedException.class,
                () -> useCase.execute(actor, "p1", "X", null,
                        EnumSet.of(MedicalRecordType.OUTPATIENT)));
    }

    @Test
    void execute_crossHospital_shouldThrow() {
        ReviewProject project = ReviewProject.create("p1", "H001", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        repository.save(project);
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H999"));

        assertThrows(AccessDeniedException.class,
                () -> useCase.execute(actor, "p1", "X", null,
                        EnumSet.of(MedicalRecordType.OUTPATIENT)));
    }

    @Test
    void execute_activeProject_shouldThrow() {
        ReviewProject project = ReviewProject.create("p1", "H001", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        project.publishNewVersion();
        project.bindScoreSheet("ss-1");
        project.activate();
        repository.save(project);
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        assertThrows(DomainStateConflictException.class,
                () -> useCase.execute(actor, "p1", "X", null,
                        EnumSet.of(MedicalRecordType.OUTPATIENT)));
    }

    @Test
    void execute_notFound_shouldThrow() {
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(actor, "no-such", "X", null,
                        EnumSet.of(MedicalRecordType.OUTPATIENT)));
    }

    // --- Test doubles ---

    static class InMemoryProjectRepo implements ReviewProjectRepository {
        final Map<String, ReviewProject> store = new HashMap<>();
        @Override public void save(ReviewProject p) { store.put(p.getProjectId(), p); }
        @Override public Optional<ReviewProject> findById(String id) { return Optional.ofNullable(store.get(id)); }
    }

    static class CapturingEventPublisher implements DomainEventPublisher {
        final List<DomainEvent> events = new ArrayList<>();
        @Override public void publish(DomainEvent event) { events.add(event); }
    }
}