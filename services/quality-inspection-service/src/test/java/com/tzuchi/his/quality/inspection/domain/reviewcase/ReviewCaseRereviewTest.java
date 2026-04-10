package com.tzuchi.his.quality.inspection.domain.reviewcase;

import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ReviewCaseRereviewTest {

    private ReviewCase caseInProgress() {
        ReviewCase rc = ReviewCase.createFromSampling(
                "c1", "b1", "p1", "H001", "SK-001", MedicalRecordType.OUTPATIENT);
        rc.assignReviewer("reviewer-1", LocalDateTime.of(2026, 12, 31, 23, 59));
        rc.startInitialReview("sub-1", "ss-1");
        return rc;
    }

    // --- completeInitialReview threshold tests ---

    @Test
    void completeInitialReview_scoreAboveThreshold_shouldEscalateToPendingRereview() {
        ReviewCase rc = caseInProgress();
        rc.completeInitialReview(new BigDecimal("90"), new BigDecimal("80"));
        assertEquals(CaseStatus.PENDING_REREVIEW, rc.getStatus());
        assertEquals(new BigDecimal("90"), rc.getInitialReviewScore());
        assertNull(rc.getFinalScore());
    }

    @Test
    void completeInitialReview_scoreEqualToThreshold_shouldStayCompleted() {
        ReviewCase rc = caseInProgress();
        rc.completeInitialReview(new BigDecimal("80"), new BigDecimal("80"));
        assertEquals(CaseStatus.INITIAL_REVIEW_COMPLETED, rc.getStatus());
        assertEquals(new BigDecimal("80"), rc.getInitialReviewScore());
        assertEquals(new BigDecimal("80"), rc.getFinalScore());
    }

    @Test
    void completeInitialReview_scoreBelowThreshold_shouldStayCompleted() {
        ReviewCase rc = caseInProgress();
        rc.completeInitialReview(new BigDecimal("60"), new BigDecimal("80"));
        assertEquals(CaseStatus.INITIAL_REVIEW_COMPLETED, rc.getStatus());
        assertEquals(new BigDecimal("60"), rc.getInitialReviewScore());
        assertEquals(new BigDecimal("60"), rc.getFinalScore());
    }

    @Test
    void completeInitialReview_nullThreshold_shouldStayCompleted() {
        ReviewCase rc = caseInProgress();
        rc.completeInitialReview(new BigDecimal("90"), null);
        assertEquals(CaseStatus.INITIAL_REVIEW_COMPLETED, rc.getStatus());
        assertEquals(new BigDecimal("90"), rc.getInitialReviewScore());
        assertEquals(new BigDecimal("90"), rc.getFinalScore());
    }

    @Test
    void completeInitialReview_wrongState_shouldThrow() {
        ReviewCase rc = ReviewCase.createFromSampling(
                "c1", "b1", "p1", "H001", "SK-001", MedicalRecordType.OUTPATIENT);
        assertThrows(DomainStateConflictException.class,
                () -> rc.completeInitialReview(new BigDecimal("90"), new BigDecimal("80")));
    }
}
