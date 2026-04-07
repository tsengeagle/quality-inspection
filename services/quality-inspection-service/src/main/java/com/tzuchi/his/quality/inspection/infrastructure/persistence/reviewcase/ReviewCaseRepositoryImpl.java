package com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase;

import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class ReviewCaseRepositoryImpl implements ReviewCaseRepository {

    private final ReviewCaseJpaRepository jpa;

    public ReviewCaseRepositoryImpl(ReviewCaseJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(ReviewCase reviewCase) {
        jpa.save(toEntity(reviewCase));
    }

    @Override
    public void saveAll(List<ReviewCase> cases) {
        jpa.saveAll(cases.stream().map(this::toEntity).collect(Collectors.toList()));
    }

    private ReviewCaseEntity toEntity(ReviewCase rc) {
        ReviewCaseEntity e = new ReviewCaseEntity();
        e.setCaseId(rc.getCaseId());
        e.setBatchId(rc.getBatchId());
        e.setProjectId(rc.getProjectId());
        e.setHospitalId(rc.getHospitalId());
        e.setSourceCaseKey(rc.getSourceCaseKey());
        e.setMedicalRecordType(rc.getMedicalRecordType() != null ? rc.getMedicalRecordType().name() : null);
        e.setStatus(rc.getStatus());
        return e;
    }
}
