package com.tzuchi.his.quality.inspection.application.project;

import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.RoleDeniedException;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryReviewProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

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

        ReviewProjectVersionView version = useCase.execute(actor, "p1");

        assertNotNull(version);
        assertEquals(1, version.getVersionNo());
        assertTrue(version.isPublished());
    }

    @Test
    void execute_systemAdmin_shouldReturnAny() {
        ReviewProject project = ReviewProject.create("p1", "H001", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        repository.save(project);
        ActorContext actor = new ActorContext("u1", "Admin",
                EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.empty());

        ReviewProjectVersionView version = useCase.execute(actor, "p1");

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
    void execute_auditor_ownHospital_shouldReturn() {
        // D-062 第 3 點：AUDITOR 可以查詢本院專案的版本
        ReviewProject project = ReviewProject.create("p1", "H001", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        repository.save(project);
        ActorContext actor = new ActorContext("u1", "Auditor",
                EnumSet.of(DomainRole.AUDITOR), Optional.of("H001"));

        ReviewProjectVersionView version = useCase.execute(actor, "p1");

        assertEquals(1, version.getVersionNo());
        assertFalse(version.isPublished());
    }

    @Test
    void execute_auditor_crossHospital_shouldThrow() {
        // D-062 第 3 點：院區範圍依 D-042、D-047
        ReviewProject project = ReviewProject.create("p1", "H001", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        repository.save(project);
        ActorContext actor = new ActorContext("u1", "Auditor",
                EnumSet.of(DomainRole.AUDITOR), Optional.of("H999"));

        assertThrows(AccessDeniedException.class,
                () -> useCase.execute(actor, "p1"));
    }

    @ParameterizedTest
    @EnumSource(value = DomainRole.class, names = {"REVIEWER", "REREVIEWER"})
    void execute_reviewerRoles_areDenied(DomainRole role) {
        // D-051、D-062 第 3 點：REVIEWER／REREVIEWER 不開放
        ReviewProject project = ReviewProject.create("p1", "H001", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        repository.save(project);
        ActorContext actor = new ActorContext("u1", role.name(), EnumSet.of(role), Optional.of("H001"));

        RoleDeniedException e = assertThrows(RoleDeniedException.class,
                () -> useCase.execute(actor, "p1"));
        assertEquals("ROLE_DENIED", e.getErrorCode());
    }

}