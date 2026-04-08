package com.tzuchi.his.quality.inspection.domain.project;

import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

class ReviewProjectTest {

    // --- Existing tests (updated for 5-param create) ---

    @Test
    void create_shouldStartAsDraftV1() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Test Project",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));

        assertEquals("p1", project.getProjectId());
        assertEquals(ProjectStatus.DRAFT, project.getStatus());
        assertEquals(1, project.getCurrentVersion().getVersionNumber());
        assertFalse(project.getCurrentVersion().isPublished());
    }

    @Test
    void publishNewVersion_shouldMakeCurrentVersionPublished() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Test",
                null, EnumSet.of(MedicalRecordType.INPATIENT));

        project.publishNewVersion();

        assertTrue(project.getCurrentVersion().isPublished());
        assertEquals(1, project.getCurrentVersion().getVersionNumber());
    }

    @Test
    void publishNewVersion_alreadyPublished_shouldThrow() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Test",
                null, EnumSet.of(MedicalRecordType.INPATIENT));
        project.publishNewVersion();

        assertThrows(DomainStateConflictException.class, project::publishNewVersion);
    }

    @Test
    void activate_withPublishedVersion_shouldSucceed() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        project.publishNewVersion();
        project.bindScoreSheet("ss-1");

        project.activate();

        assertEquals(ProjectStatus.ACTIVE, project.getStatus());
    }

    @Test
    void activate_withoutPublishedVersion_shouldThrow() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));

        assertThrows(DomainStateConflictException.class, project::activate);
    }

    @Test
    void deactivate_activeProject_shouldSucceed() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        project.publishNewVersion();
        project.bindScoreSheet("ss-1");
        project.activate();

        project.deactivate();

        assertEquals(ProjectStatus.DEACTIVATED, project.getStatus());
    }

    @Test
    void updateName_draftVersion_shouldSucceed() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Old Name",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));

        project.updateName("New Name");

        assertEquals("New Name", project.getName());
    }

    @Test
    void updateName_publishedVersion_shouldThrow() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        project.publishNewVersion();

        assertThrows(DomainStateConflictException.class, () -> project.updateName("New"));
    }

    @Test
    void prepareDraftVersion_afterPublish_shouldCreateNextDraft() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        project.publishNewVersion();

        project.prepareDraftVersion();

        assertEquals(2, project.getCurrentVersion().getVersionNumber());
        assertFalse(project.getCurrentVersion().isPublished());
    }

    @Test
    void create_withBlankName_shouldThrow() {
        assertThrows(DomainRuleViolationException.class,
                () -> ReviewProject.create("p1", "h1", "  ",
                        null, EnumSet.of(MedicalRecordType.OUTPATIENT)));
    }

    // --- New B1a tests ---

    @Test
    void create_withDescription_shouldStoreDescription() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Test",
                "A description", EnumSet.of(MedicalRecordType.OUTPATIENT));

        assertEquals("A description", project.getDescription());
    }

    @Test
    void create_withNullDescription_shouldAllowNull() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));

        assertNull(project.getDescription());
    }

    @Test
    void updateDraft_draftStatus_shouldUpdateAllFields() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Old",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));

        project.updateDraft("New Name", "New Desc",
                EnumSet.of(MedicalRecordType.INPATIENT));

        assertEquals("New Name", project.getName());
        assertEquals("New Desc", project.getDescription());
        assertTrue(project.getMedicalRecordTypes().contains(MedicalRecordType.INPATIENT));
        assertFalse(project.getMedicalRecordTypes().contains(MedicalRecordType.OUTPATIENT));
    }

    @Test
    void updateDraft_activeStatus_shouldThrow() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        project.publishNewVersion();
        project.bindScoreSheet("ss-1");
        project.activate();

        DomainStateConflictException ex = assertThrows(DomainStateConflictException.class,
                () -> project.updateDraft("X", null, EnumSet.of(MedicalRecordType.OUTPATIENT)));
        assertEquals("INVALID_STATE_TRANSITION", ex.getErrorCode());
    }

    @Test
    void bindScoreSheet_draftStatus_shouldSucceed() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));

        project.bindScoreSheet("ss-123");

        assertEquals("ss-123", project.getBoundScoreSheetId());
    }

    @Test
    void bindScoreSheet_activeStatus_shouldThrow() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        project.publishNewVersion();
        project.bindScoreSheet("ss-1");
        project.activate();

        assertThrows(DomainStateConflictException.class,
                () -> project.bindScoreSheet("ss-2"));
    }

    @Test
    void bindScoreSheet_nullId_shouldThrow() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));

        assertThrows(NullPointerException.class,
                () -> project.bindScoreSheet(null));
    }

    @Test
    void activate_withBoundScoreSheet_shouldSucceed() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        project.publishNewVersion();
        project.bindScoreSheet("ss-1");

        project.activate();

        assertEquals(ProjectStatus.ACTIVE, project.getStatus());
    }

    @Test
    void activate_withoutBoundScoreSheet_shouldThrow() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        project.publishNewVersion();

        DomainRuleViolationException ex = assertThrows(DomainRuleViolationException.class,
                project::activate);
        assertEquals("SCORE_SHEET_NOT_BOUND", ex.getErrorCode());
    }

    @Test
    void activate_fromDeactivated_shouldThrow() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        project.publishNewVersion();
        project.bindScoreSheet("ss-1");
        project.activate();
        project.deactivate();

        DomainStateConflictException ex = assertThrows(DomainStateConflictException.class,
                project::activate);
        assertEquals("INVALID_STATE_TRANSITION", ex.getErrorCode());
    }

    @Test
    void activate_alreadyActive_shouldBeIdempotent() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        project.publishNewVersion();
        project.bindScoreSheet("ss-1");
        project.activate();

        project.activate(); // no throw

        assertEquals(ProjectStatus.ACTIVE, project.getStatus());
    }

    @Test
    void deactivate_activeProject_shouldBecomeDeactivated() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        project.publishNewVersion();
        project.bindScoreSheet("ss-1");
        project.activate();

        project.deactivate();

        assertEquals(ProjectStatus.DEACTIVATED, project.getStatus());
    }

    @Test
    void deactivate_draftProject_shouldThrow() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));

        DomainStateConflictException ex = assertThrows(DomainStateConflictException.class,
                project::deactivate);
        assertEquals("INVALID_STATE_TRANSITION", ex.getErrorCode());
    }

    @Test
    void deactivate_alreadyDeactivated_shouldBeIdempotent() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        project.publishNewVersion();
        project.bindScoreSheet("ss-1");
        project.activate();
        project.deactivate();

        project.deactivate(); // no throw

        assertEquals(ProjectStatus.DEACTIVATED, project.getStatus());
    }

    @Test
    void reconstitute_withNewFields_shouldRoundTrip() {
        ReviewProject project = ReviewProject.reconstitute("p1", "h1", "Test",
                "My Desc", "ss-99",
                EnumSet.of(MedicalRecordType.OUTPATIENT),
                ProjectStatus.ACTIVE, 2, true);

        assertEquals("My Desc", project.getDescription());
        assertEquals("ss-99", project.getBoundScoreSheetId());
        assertEquals(ProjectStatus.ACTIVE, project.getStatus());
        assertEquals(2, project.getCurrentVersion().getVersionNumber());
        assertTrue(project.getCurrentVersion().isPublished());
    }
}
