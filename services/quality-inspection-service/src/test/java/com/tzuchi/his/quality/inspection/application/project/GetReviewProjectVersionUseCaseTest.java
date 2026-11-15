package com.tzuchi.his.quality.inspection.application.project;

import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.RoleDeniedException;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.project.ProjectVersion;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryReviewProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GetReviewProjectVersionUseCaseTest {

    private InMemoryReviewProjectRepository repository;
    private GetReviewProjectVersionUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryReviewProjectRepository();
        useCase = new GetReviewProjectVersionUseCase(repository);
    }

    @Test
    void execute_hospitalManager_ownHospital_shouldReturn() {
        ReviewProject project = ReviewProject.create("p1", "H001", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        com.tzuchi.his.quality.inspection.testfixture.PublishableProjects.prepare(project);
        project.publishNewVersion();
        repository.save(project);
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        ProjectVersion version = useCase.execute(actor, "p1");

        assertNotNull(version);
        assertEquals(1, version.getVersionNumber());
        assertTrue(version.isPublished());
    }

    @Test
    void execute_systemAdmin_shouldReturnAny() {
        ReviewProject project = ReviewProject.create("p1", "H001", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        repository.save(project);
        ActorContext actor = new ActorContext("u1", "Admin",
                EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.empty());

        ProjectVersion version = useCase.execute(actor, "p1");

        assertNotNull(version);
    }

    @Test
    void execute_crossHospital_shouldThrow() {
        ReviewProject project = ReviewProject.create("p1", "H001", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
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

    @Test
    void execute_noValidRole_shouldThrow() {
        ActorContext actor = new ActorContext("u1", "Auditor",
                EnumSet.of(DomainRole.AUDITOR), Optional.empty());

        assertThrows(RoleDeniedException.class,
                () -> useCase.execute(actor, "p1"));
    }

}