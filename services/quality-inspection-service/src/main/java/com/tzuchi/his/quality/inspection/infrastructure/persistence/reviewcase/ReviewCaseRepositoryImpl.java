package com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase;

import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewerAssignment;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
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

    @Override
    public Optional<ReviewCase> findById(String caseId) {
        return jpa.findById(caseId).map(this::toDomain);
    }

    private ReviewCase toDomain(ReviewCaseEntity e) {
        ReviewerAssignment assignment = null;
        if (e.getReviewerId() != null && e.getReviewerAssignedAt() != null) {
            assignment = new ReviewerAssignment(e.getReviewerId(), e.getReviewerAssignedAt());
        }

        ReviewerAssignment rereviewerAssignment = null;
        if (e.getRereviewerId() != null && e.getRereviewerAssignedAt() != null) {
            rereviewerAssignment = new ReviewerAssignment(e.getRereviewerId(), e.getRereviewerAssignedAt());
        }

        MedicalRecordType mrt = null;
        if (e.getMedicalRecordType() != null) {
            mrt = MedicalRecordType.valueOf(e.getMedicalRecordType());
        }

        return ReviewCase.reconstitute(
                e.getCaseId(), e.getBatchId(), e.getProjectId(), e.getHospitalId(),
                e.getSourceCaseKey(), mrt,
                e.getStatus(), assignment, e.getDueAt(),
                e.getCancelReason(), e.getReturnReason(), e.getCancelledAt(), e.getClosedAt(),
                e.isRereviewRequired(), e.getFinalScore(), e.getFinalConclusion(),
                e.getLatestInitialSubmissionId(), e.getRuleSnapshotRef(), e.getScoreSheetVersionRef(),
                rereviewerAssignment, e.getRereviewerDueAt(), e.getInitialReviewScore());
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

        if (rc.getReviewerAssignment() != null) {
            e.setReviewerId(rc.getReviewerAssignment().getReviewerId());
            e.setReviewerAssignedAt(rc.getReviewerAssignment().getAssignedAt());
        }
        e.setDueAt(rc.getDueAt());
        e.setCancelReason(rc.getCancelReason());
        e.setReturnReason(rc.getReturnReason());
        e.setCancelledAt(rc.getCancelledAt());
        e.setClosedAt(rc.getClosedAt());
        e.setRereviewRequired(rc.isRereviewRequired());
        e.setFinalScore(rc.getFinalScore());
        e.setFinalConclusion(rc.getFinalConclusion());
        e.setLatestInitialSubmissionId(rc.getLatestInitialSubmissionId());
        e.setRuleSnapshotRef(rc.getRuleSnapshotRef());
        e.setScoreSheetVersionRef(rc.getScoreSheetVersionRef());

        if (rc.getRereviewerAssignment() != null) {
            e.setRereviewerId(rc.getRereviewerAssignment().getReviewerId());
            e.setRereviewerAssignedAt(rc.getRereviewerAssignment().getAssignedAt());
        }
        e.setRereviewerDueAt(rc.getRereviewerDueAt());
        e.setInitialReviewScore(rc.getInitialReviewScore());
        return e;
    }
}
