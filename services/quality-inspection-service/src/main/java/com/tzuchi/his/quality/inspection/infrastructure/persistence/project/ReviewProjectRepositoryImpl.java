package com.tzuchi.his.quality.inspection.infrastructure.persistence.project;

import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.project.ProjectVersion;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class ReviewProjectRepositoryImpl implements ReviewProjectRepository {

    private final ReviewProjectJpaRepository jpa;

    public ReviewProjectRepositoryImpl(ReviewProjectJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(ReviewProject project) {
        ReviewProjectEntity entity = toEntity(project);
        jpa.save(entity);
    }

    @Override
    public Optional<ReviewProject> findById(String projectId) {
        return jpa.findById(projectId).map(this::toDomain);
    }

    private ReviewProjectEntity toEntity(ReviewProject project) {
        ReviewProjectEntity e = new ReviewProjectEntity();
        e.setProjectId(project.getProjectId());
        e.setHospitalId(project.getHospitalId());
        e.setName(project.getName());
        e.setMedicalRecordTypes(
                project.getMedicalRecordTypes().stream()
                        .map(Enum::name)
                        .collect(Collectors.toSet())
        );
        e.setStatus(project.getStatus());
        e.setVersionNumber(project.getCurrentVersion().getVersionNumber());
        e.setVersionPublished(project.getCurrentVersion().isPublished());
        return e;
    }

    private ReviewProject toDomain(ReviewProjectEntity e) {
        var types = e.getMedicalRecordTypes().stream()
                .map(MedicalRecordType::valueOf)
                .collect(Collectors.toSet());
        // Reconstruct via factory and reflection-free approach:
        // We use the create factory then reconcile state.
        // For a proper implementation, a dedicated reconstitution constructor
        // should be used. For now, we use create + internal state alignment.
        ReviewProject project = ReviewProject.create(
                e.getProjectId(), e.getHospitalId(), e.getName(), types);

        // Align version: the create factory starts at v1-draft.
        // We need to align to the persisted version state.
        alignVersion(project, e.getVersionNumber(), e.isVersionPublished());
        alignStatus(project, e.getStatus());
        return project;
    }

    private void alignVersion(ReviewProject project, int targetVersion, boolean published) {
        // Advance through versions to reach the target state
        for (int v = 1; v < targetVersion; v++) {
            project.publishNewVersion();
            project.prepareDraftVersion();
        }
        if (published && !project.getCurrentVersion().isPublished()) {
            project.publishNewVersion();
        }
    }

    private void alignStatus(ReviewProject project, com.tzuchi.his.quality.inspection.domain.project.ProjectStatus targetStatus) {
        switch (targetStatus) {
            case ACTIVE:
                project.activate();
                break;
            case DEACTIVATED:
                if (project.getStatus() == com.tzuchi.his.quality.inspection.domain.project.ProjectStatus.ACTIVE) {
                    project.deactivate();
                } else if (project.getCurrentVersion().isPublished()) {
                    project.activate();
                    project.deactivate();
                }
                break;
            default:
                break;
        }
    }
}
