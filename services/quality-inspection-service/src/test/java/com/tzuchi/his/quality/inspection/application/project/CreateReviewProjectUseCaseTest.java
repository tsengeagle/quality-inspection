package com.tzuchi.his.quality.inspection.application.project;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.RoleDeniedException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.event.ReviewProjectCreatedEvent;
import com.tzuchi.his.quality.inspection.testfixture.CapturingDomainEventPublisher;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryReviewProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CreateReviewProjectUseCaseTest {

    private InMemoryReviewProjectRepository repository;
    private CapturingDomainEventPublisher eventPublisher;
    private CreateReviewProjectUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryReviewProjectRepository();
        eventPublisher = new CapturingDomainEventPublisher();
        useCase = new CreateReviewProjectUseCase(repository, eventPublisher, () -> "p-" + (repository.store.size() + 1));
    }

    @Test
    void execute_hospitalManager_shouldCreateProject() {
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        String projectId = useCase.execute(actor, "Test Project", "A desc",
                Set.of("OUTPATIENT"), null, null, null);

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
                Set.of("OUTPATIENT"), null, null, null);

        assertEquals(1, eventPublisher.events.size());
        assertInstanceOf(ReviewProjectCreatedEvent.class, eventPublisher.events.get(0));
    }

    @Test
    void execute_wrongRole_shouldThrow() {
        ActorContext actor = new ActorContext("u1", "Admin",
                EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.of("H001"));

        assertThrows(RoleDeniedException.class,
                () -> useCase.execute(actor, "Test", null,
                        Set.of("OUTPATIENT"), null, null, null));
    }

    @Test
    void execute_noHospitalId_shouldThrow() {
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.empty());

        assertThrows(Exception.class,
                () -> useCase.execute(actor, "Test", null,
                        Set.of("OUTPATIENT"), null, null, null));
    }

}
