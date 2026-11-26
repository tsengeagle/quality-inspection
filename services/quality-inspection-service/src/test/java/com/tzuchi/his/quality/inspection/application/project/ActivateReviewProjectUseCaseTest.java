package com.tzuchi.his.quality.inspection.application.project;

import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.RoleDeniedException;
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.project.ProjectStatus;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.event.ReviewProjectActivatedEvent;
import com.tzuchi.his.quality.inspection.testfixture.CapturingDomainEventPublisher;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryReviewProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ActivateReviewProjectUseCaseTest {

    private InMemoryReviewProjectRepository repository;
    private CapturingDomainEventPublisher eventPublisher;
    private ActivateReviewProjectUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryReviewProjectRepository();
        eventPublisher = new CapturingDomainEventPublisher();
        useCase = new ActivateReviewProjectUseCase(repository, eventPublisher, java.time.Clock.systemUTC(), () -> java.util.UUID.randomUUID().toString());
    }

    @Test
    void execute_happyPath_shouldActivate() {
        ReviewProject project = ReviewProject.create("p1", "H001", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        com.tzuchi.his.quality.inspection.testfixture.PublishableProjects.prepare(project);
        project.publishNewVersion();
        repository.save(project);
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        useCase.execute(actor, "p1");

        assertEquals(ProjectStatus.ACTIVE, repository.store.get("p1").getStatus());
    }

    @Test
    void execute_shouldPublishEvent() {
        ReviewProject project = ReviewProject.create("p1", "H001", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        com.tzuchi.his.quality.inspection.testfixture.PublishableProjects.prepare(project);
        project.publishNewVersion();
        repository.save(project);
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        useCase.execute(actor, "p1");

        assertEquals(1, eventPublisher.events.size());
        assertInstanceOf(ReviewProjectActivatedEvent.class, eventPublisher.events.get(0));
    }

    @Test
    void execute_doesNotCheckTheScoreSheetBinding() {
        // D-041：「啟用不再另外檢查綁定」。已發布但未綁定只會出現在 D-041 生效前存的資料
        ReviewProject project = ReviewProject.reconstitute("p1", "H001", "Test", null, null,
                EnumSet.of(MedicalRecordType.OUTPATIENT), ProjectStatus.DRAFT, 1, true,
                null, null, null);
        repository.save(project);
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        useCase.execute(actor, "p1");

        assertEquals(ProjectStatus.ACTIVE, repository.store.get("p1").getStatus());
    }

    @Test
    void execute_noPublishedVersion_shouldThrow() {
        ReviewProject project = ReviewProject.create("p1", "H001", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        project.bindScoreSheet("ss-1");
        repository.save(project);
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        assertThrows(DomainStateConflictException.class,
                () -> useCase.execute(actor, "p1"));
    }

    @Test
    void execute_wrongRole_shouldThrow() {
        ActorContext actor = new ActorContext("u1", "Admin",
                EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.of("H001"));

        assertThrows(RoleDeniedException.class,
                () -> useCase.execute(actor, "p1"));
    }

    @Test
    void execute_crossHospital_shouldThrow() {
        ReviewProject project = ReviewProject.create("p1", "H001", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        com.tzuchi.his.quality.inspection.testfixture.PublishableProjects.prepare(project);
        project.publishNewVersion();
        repository.save(project);
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H999"));

        assertThrows(AccessDeniedException.class,
                () -> useCase.execute(actor, "p1"));
    }

    @Test
    void execute_notFound_shouldThrow() {
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(actor, "no-such"));
    }

}