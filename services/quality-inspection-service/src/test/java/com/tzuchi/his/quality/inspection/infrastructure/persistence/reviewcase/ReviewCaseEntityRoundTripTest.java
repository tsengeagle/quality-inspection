package com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase;

import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewerAssignment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(ReviewCaseRepositoryImpl.class)
class ReviewCaseEntityRoundTripTest {

    @Autowired
    private ReviewCaseRepository repo;

    @Test
    void roundTrip_allFields_persistAndLoad() {
        ReviewCase original = ReviewCase.reconstitute(
                "case-rt-1", "batch-1", "proj-1", "hosp-1",
                "SK-001", MedicalRecordType.INPATIENT,
                CaseStatus.PENDING_INITIAL_REVIEW,
                new ReviewerAssignment("reviewer-1", LocalDateTime.of(2025, 6, 1, 10, 0)),
                LocalDateTime.of(2025, 6, 15, 23, 59),
                null, null, null, null,
                false, null, null,
                null, null, null,
                null, null, null
        );

        repo.save(original);
        Optional<ReviewCase> loaded = repo.findById("case-rt-1");

        assertThat(loaded).isPresent();
        ReviewCase rc = loaded.get();
        assertThat(rc.getCaseId()).isEqualTo("case-rt-1");
        assertThat(rc.getBatchId()).isEqualTo("batch-1");
        assertThat(rc.getProjectId()).isEqualTo("proj-1");
        assertThat(rc.getHospitalId()).isEqualTo("hosp-1");
        assertThat(rc.getSourceCaseKey()).isEqualTo("SK-001");
        assertThat(rc.getMedicalRecordType()).isEqualTo(MedicalRecordType.INPATIENT);
        assertThat(rc.getStatus()).isEqualTo(CaseStatus.PENDING_INITIAL_REVIEW);
        assertThat(rc.getReviewerAssignment()).isNotNull();
        assertThat(rc.getReviewerAssignment().getReviewerId()).isEqualTo("reviewer-1");
        assertThat(rc.getDueAt()).isEqualTo(LocalDateTime.of(2025, 6, 15, 23, 59));
    }

    @Test
    void roundTrip_minimalFields_backwardCompat() {
        ReviewCase minimal = ReviewCase.reconstitute(
                "case-rt-2", "batch-2", "proj-2", "hosp-2",
                "SK-002", null,
                CaseStatus.SELECTED_PENDING_ASSIGNMENT,
                null, null, null, null, null, null,
                false, null, null,
                null, null, null,
                null, null, null
        );

        repo.save(minimal);
        Optional<ReviewCase> loaded = repo.findById("case-rt-2");

        assertThat(loaded).isPresent();
        ReviewCase rc = loaded.get();
        assertThat(rc.getStatus()).isEqualTo(CaseStatus.SELECTED_PENDING_ASSIGNMENT);
        assertThat(rc.getReviewerAssignment()).isNull();
        assertThat(rc.getDueAt()).isNull();
        assertThat(rc.getCancelReason()).isNull();
        assertThat(rc.getReturnReason()).isNull();
        assertThat(rc.getCancelledAt()).isNull();
        assertThat(rc.getClosedAt()).isNull();
        assertThat(rc.isRereviewRequired()).isFalse();
        assertThat(rc.getFinalScore()).isNull();
        assertThat(rc.getFinalConclusion()).isNull();
    }

    @Test
    void roundTrip_cancelledCase_withReason() {
        ReviewCase cancelled = ReviewCase.reconstitute(
                "case-rt-3", "batch-3", "proj-3", "hosp-3",
                "SK-003", null,
                CaseStatus.CANCELLED,
                null, null,
                "Duplicate case", null,
                LocalDateTime.of(2025, 7, 1, 12, 0), null,
                false, null, null,
                null, null, null,
                null, null, null
        );

        repo.save(cancelled);
        Optional<ReviewCase> loaded = repo.findById("case-rt-3");

        assertThat(loaded).isPresent();
        ReviewCase rc = loaded.get();
        assertThat(rc.getStatus()).isEqualTo(CaseStatus.CANCELLED);
        assertThat(rc.getCancelReason()).isEqualTo("Duplicate case");
        assertThat(rc.getCancelledAt()).isEqualTo(LocalDateTime.of(2025, 7, 1, 12, 0));
    }
}
