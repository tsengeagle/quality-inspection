package com.tzuchi.his.quality.inspection.infrastructure.persistence.project;

import com.tzuchi.his.quality.inspection.application.project.query.ReviewProjectQueryPort;
import com.tzuchi.his.quality.inspection.application.project.query.ReviewProjectSummary;
import com.tzuchi.his.quality.inspection.domain.project.ProjectStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ReviewProjectQueryAdapter implements ReviewProjectQueryPort {

    private final ReviewProjectJpaRepository jpaRepository;

    public ReviewProjectQueryAdapter(ReviewProjectJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<ReviewProjectSummary> findByHospitalId(String hospitalId) {
        return jpaRepository.findByHospitalId(hospitalId).stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReviewProjectSummary> findAll() {
        return jpaRepository.findAll().stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ReviewProjectSummary> findSummaryById(String projectId) {
        return jpaRepository.findById(projectId).map(this::toSummary);
    }

    private ReviewProjectSummary toSummary(ReviewProjectEntity entity) {
        return new ReviewProjectSummary(
                entity.getProjectId(),
                entity.getHospitalId(),
                entity.getName(),
                entity.getDescription(),
                toMedicalRecordTypes(entity.getMedicalRecordTypes()),
                entity.getStatus(),
                entity.getBoundScoreSheetId(),
                entity.getVersionNumber(),
                entity.isVersionPublished(),
                entity.getQualityThreshold(),
                entity.getInitialReviewWeight(),
                entity.getRereviewWeight()
        );
    }

    private static java.util.Set<com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType> toMedicalRecordTypes(
            java.util.Set<String> stored) {
        java.util.Set<com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType> types =
                java.util.EnumSet.noneOf(com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType.class);
        if (stored != null) {
            for (String type : stored) {
                types.add(com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType.valueOf(type));
            }
        }
        return types;
    }
}