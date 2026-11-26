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
        projectRepository.save(project);

        ReviewProject loaded = projectRepository.findById("p-int-1")
                .orElseThrow(() -> new AssertionError("Project not found after save"));

        // D-094：不開放改版，發布後的版本就是最後的版本
        assertEquals(1, loaded.getCurrentVersion().getVersionNumber(),
                "After publish, version should stay 1");
        assertTrue(loaded.getCurrentVersion().isPublished(),
                "Published version should survive round-trip");
        assertEquals(ProjectStatus.DRAFT, loaded.getStatus(),
                "Status should remain DRAFT");
        assertEquals("Test Project", loaded.getName(),
                "Name should survive round-trip");
        assertEquals("h1", loaded.getHospitalId(),
                "HospitalId should survive round-trip");
    }
}
