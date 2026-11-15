package com.tzuchi.his.quality.inspection.infrastructure.persistence.project;

import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.project.ProjectStatus;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ReviewProjectIntegrationTest {

    @Autowired
    private ReviewProjectRepository projectRepository;

    @Test
    void reconstitution_versionAndStatus_shouldBeCorrect() {
        ReviewProject project = ReviewProject.create("p-int-1", "h1", "Test Project",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        com.tzuchi.his.quality.inspection.testfixture.PublishableProjects.prepare(project);
        project.publishNewVersion();
        project.prepareDraftVersion();
        projectRepository.save(project);

        ReviewProject loaded = projectRepository.findById("p-int-1")
                .orElseThrow(() -> new AssertionError("Project not found after save"));

        assertEquals(2, loaded.getCurrentVersion().getVersionNumber(),
                "After publish + prepareDraft, version should be 2");
        assertFalse(loaded.getCurrentVersion().isPublished(),
                "New draft version should not be published");
        assertEquals(ProjectStatus.DRAFT, loaded.getStatus(),
                "Status should remain DRAFT");
        assertEquals("Test Project", loaded.getName(),
                "Name should survive round-trip");
        assertEquals("h1", loaded.getHospitalId(),
                "HospitalId should survive round-trip");
    }
}
