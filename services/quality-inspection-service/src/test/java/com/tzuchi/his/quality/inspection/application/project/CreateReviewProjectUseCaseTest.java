package com.tzuchi.his.quality.inspection.application.project;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.UnauthorizedException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import com.tzuchi.his.quality.inspection.domain.project.event.ReviewProjectCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CreateReviewProjectUseCaseTest {

    private InMemoryProjectRepo repository;
    private CapturingEventPublisher eventPublisher;
    private CreateReviewProjectUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryProjectRepo();
        eventPublisher = new CapturingEventPublisher();
        useCase = new CreateReviewProjectUseCase(repository, eventPublisher);
    }

    @Test
    void execute_hospitalManager_shouldCreateProject() {
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        String projectId = useCase.execute(actor, "Test Project", "A desc",
                EnumSet.of(MedicalRecordType.OUTPATIENT));

        assertNotNull(projectId);
        ReviewProject saved = repository.store.get(projectId);
        assertNotNull(saved);
        assertEquals("H001", saved.getHospitalId());
        assertEquals("Test Project", saved.getName());
        assertEquals("A desc", saved.getDescription());
    }

    @Test
    void execute_shouldPublishCreatedEvent() {
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        String projectId = useCase.execute(actor, "Test", null,
                EnumSet.of(MedicalRecordType.OUTPATIENT));

        assertEquals(1, eventPublisher.events.size());
        assertInstanceOf(ReviewProjectCreatedEvent.class, eventPublisher.events.get(0));
    }

    @Test
    void execute_wrongRole_shouldThrow() {
        ActorContext actor = new ActorContext("u1", "Admin",
                EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.of("H001"));

        assertThrows(UnauthorizedException.class,
                () -> useCase.execute(actor, "Test", null,
                        EnumSet.of(MedicalRecordType.OUTPATIENT)));
    }

    @Test
    void execute_noHospitalId_shouldThrow() {
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.empty());

        assertThrows(Exception.class,
                () -> useCase.execute(actor, "Test", null,
                        EnumSet.of(MedicalRecordType.OUTPATIENT)));
    }

    // --- Test doubles ---

    static class InMemoryProjectRepo implements ReviewProjectRepository {
        final Map<String, ReviewProject> store = new HashMap<>();
        @Override public void save(ReviewProject p) { store.put(p.getProjectId(), p); }
        @Override public Optional<ReviewProject> findById(String id) { return Optional.ofNullable(store.get(id)); }
    }

    static class CapturingEventPublisher implements DomainEventPublisher {
        final List<com.tzuchi.his.quality.inspection.domain.common.DomainEvent> events = new ArrayList<>();
        @Override public void publish(com.tzuchi.his.quality.inspection.domain.common.DomainEvent event) { events.add(event); }
    }
}
