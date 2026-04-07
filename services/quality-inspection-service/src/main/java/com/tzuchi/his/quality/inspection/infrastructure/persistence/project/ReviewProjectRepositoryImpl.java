package com.tzuchi.his.quality.inspection.infrastructure.persistence.project;

import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;
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
        Set<MedicalRecordType> types = e.getMedicalRecordTypes().stream()
                .map(MedicalRecordType::valueOf)
                .collect(Collectors.toSet());
        return ReviewProject.reconstitute(
                e.getProjectId(),
                e.getHospitalId(),
                e.getName(),
                types,
                e.getStatus(),
                e.getVersionNumber(),
                e.isVersionPublished()
        );
    }
}
