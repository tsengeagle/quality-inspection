package com.tzuchi.his.quality.inspection.domain.project;

import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

class ReviewProjectTest {

    @Test
    void create_shouldStartAsDraftV1() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Test Project",
                EnumSet.of(MedicalRecordType.OUTPATIENT));

        assertEquals("p1", project.getProjectId());
        assertEquals(ProjectStatus.DRAFT, project.getStatus());
        assertEquals(1, project.getCurrentVersion().getVersionNumber());
        assertFalse(project.getCurrentVersion().isPublished());
    }

    @Test
    void publishNewVersion_shouldMakeCurrentVersionPublished() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Test",
                EnumSet.of(MedicalRecordType.INPATIENT));

        project.publishNewVersion();

        assertTrue(project.getCurrentVersion().isPublished());
        assertEquals(1, project.getCurrentVersion().getVersionNumber());
    }

    @Test
    void publishNewVersion_alreadyPublished_shouldThrow() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Test",
                EnumSet.of(MedicalRecordType.INPATIENT));
        project.publishNewVersion();

        assertThrows(IllegalStateException.class, project::publishNewVersion);
    }

    @Test
    void activate_withPublishedVersion_shouldSucceed() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Test",
                EnumSet.of(MedicalRecordType.OUTPATIENT));
        project.publishNewVersion();

        project.activate();

        assertEquals(ProjectStatus.ACTIVE, project.getStatus());
    }

    @Test
    void activate_withoutPublishedVersion_shouldThrow() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Test",
                EnumSet.of(MedicalRecordType.OUTPATIENT));

        assertThrows(IllegalStateException.class, project::activate);
    }

    @Test
    void deactivate_activeProject_shouldSucceed() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Test",
                EnumSet.of(MedicalRecordType.OUTPATIENT));
        project.publishNewVersion();
        project.activate();

        project.deactivate();

        assertEquals(ProjectStatus.DEACTIVATED, project.getStatus());
    }

    @Test
    void updateName_draftVersion_shouldSucceed() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Old Name",
                EnumSet.of(MedicalRecordType.OUTPATIENT));

        project.updateName("New Name");

        assertEquals("New Name", project.getName());
    }

    @Test
    void updateName_publishedVersion_shouldThrow() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Test",
                EnumSet.of(MedicalRecordType.OUTPATIENT));
        project.publishNewVersion();

        assertThrows(IllegalStateException.class, () -> project.updateName("New"));
    }

    @Test
    void prepareDraftVersion_afterPublish_shouldCreateNextDraft() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Test",
                EnumSet.of(MedicalRecordType.OUTPATIENT));
        project.publishNewVersion();

        project.prepareDraftVersion();

        assertEquals(2, project.getCurrentVersion().getVersionNumber());
        assertFalse(project.getCurrentVersion().isPublished());
    }

    @Test
    void create_withBlankName_shouldThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> ReviewProject.create("p1", "h1", "  ",
                        EnumSet.of(MedicalRecordType.OUTPATIENT)));
    }
}
