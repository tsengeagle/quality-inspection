package com.tzuchi.his.quality.inspection.domain.reviewcase;

import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
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

    private ReviewCase caseAtInitialReviewCompleted() {
        ReviewCase rc = caseInProgress();
        rc.completeInitialReview(new BigDecimal("60"), new BigDecimal("80"));
        // score 60 <= threshold 80 → INITIAL_REVIEW_COMPLETED
        return rc;
    }

    // --- closeWithoutRereview tests ---

    @Test
    void closeWithoutRereview_happyPath_shouldCloseCaseWithInitialScore() {
        ReviewCase rc = caseAtInitialReviewCompleted();
        rc.closeWithoutRereview();
        assertEquals(CaseStatus.CLOSED, rc.getStatus());
        assertEquals(new BigDecimal("60"), rc.getFinalScore());
        assertNotNull(rc.getClosedAt());
    }

    @Test
    void closeWithoutRereview_wrongState_shouldThrow() {
        ReviewCase rc = caseInProgress();
        assertThrows(DomainStateConflictException.class, rc::closeWithoutRereview);
    }

    // --- sendToRereview tests ---

    @Test
    void sendToRereview_happyPath_shouldEscalate() {
        ReviewCase rc = caseAtInitialReviewCompleted();
        rc.sendToRereview();
        assertEquals(CaseStatus.PENDING_REREVIEW, rc.getStatus());
    }

    @Test
    void sendToRereview_wrongState_shouldThrow() {
        ReviewCase rc = caseInProgress();
        assertThrows(DomainStateConflictException.class, rc::sendToRereview);
    }

    private ReviewCase caseAtPendingRereview() {
        ReviewCase rc = caseInProgress();
        rc.completeInitialReview(new BigDecimal("90"), new BigDecimal("80"));
        // score 90 > threshold 80 → PENDING_REREVIEW
        return rc;
    }

    // --- assignRereviewer tests ---

    @Test
    void assignRereviewer_happyPath_shouldSetAssignment() {
        ReviewCase rc = caseAtPendingRereview();
        LocalDateTime dueAt = LocalDateTime.of(2026, 6, 30, 23, 59);
        rc.assignRereviewer("rereviewer-1", dueAt);
        assertNotNull(rc.getRereviewerAssignment());
        assertEquals("rereviewer-1", rc.getRereviewerAssignment().getReviewerId());
        assertEquals(dueAt, rc.getRereviewerDueAt());
        assertEquals(CaseStatus.PENDING_REREVIEW, rc.getStatus());
    }

    @Test
    void assignRereviewer_sameAsInitialReviewer_shouldThrow() {
        ReviewCase rc = caseAtPendingRereview();
        // initial reviewer is "reviewer-1" (set in caseInProgress)
        assertThrows(DomainRuleViolationException.class,
                () -> rc.assignRereviewer("reviewer-1", LocalDateTime.now()));
    }

    @Test
    void assignRereviewer_wrongState_shouldThrow() {
        ReviewCase rc = caseInProgress(); // INITIAL_REVIEW_IN_PROGRESS
        assertThrows(DomainStateConflictException.class,
                () -> rc.assignRereviewer("rereviewer-1", LocalDateTime.now()));
    }

    // --- reassignRereviewer tests ---

    @Test
    void reassignRereviewer_happyPath_shouldReplaceAssignment() {
        ReviewCase rc = caseAtPendingRereview();
        rc.assignRereviewer("rereviewer-1", LocalDateTime.now());
        LocalDateTime newDue = LocalDateTime.of(2026, 7, 15, 23, 59);
        rc.reassignRereviewer("rereviewer-2", newDue);
        assertEquals("rereviewer-2", rc.getRereviewerAssignment().getReviewerId());
        assertEquals(newDue, rc.getRereviewerDueAt());
    }

    @Test
    void reassignRereviewer_sameAsInitialReviewer_shouldThrow() {
        ReviewCase rc = caseAtPendingRereview();
        rc.assignRereviewer("rereviewer-1", LocalDateTime.now());
        assertThrows(DomainRuleViolationException.class,
                () -> rc.reassignRereviewer("reviewer-1", LocalDateTime.now()));
    }

    // --- startRereview tests ---

    @Test
    void startRereview_happyPath_shouldTransitionToInProgress() {
        ReviewCase rc = caseAtPendingRereview();
        rc.assignRereviewer("rereviewer-1", LocalDateTime.now());
        rc.startRereview("rereviewer-1");
        assertEquals(CaseStatus.REREVIEW_IN_PROGRESS, rc.getStatus());
    }

    @Test
    void startRereview_wrongActor_shouldThrow() {
        ReviewCase rc = caseAtPendingRereview();
        rc.assignRereviewer("rereviewer-1", LocalDateTime.now());
        assertThrows(DomainRuleViolationException.class,
                () -> rc.startRereview("someone-else"));
    }

    @Test
    void startRereview_wrongState_shouldThrow() {
        ReviewCase rc = caseInProgress();
        assertThrows(DomainStateConflictException.class,
                () -> rc.startRereview("rereviewer-1"));
    }

    @Test
    void startRereview_noAssignment_shouldThrow() {
        ReviewCase rc = caseAtPendingRereview();
        // no assignRereviewer called
        assertThrows(DomainRuleViolationException.class,
                () -> rc.startRereview("rereviewer-1"));
    }

    private ReviewCase caseAtRereviewInProgress() {
        ReviewCase rc = caseAtPendingRereview();
        rc.assignRereviewer("rereviewer-1", LocalDateTime.of(2026, 6, 30, 23, 59));
        rc.startRereview("rereviewer-1");
        return rc;
    }

    // --- completeRereview tests ---

    @Test
    void completeRereview_happyPath_shouldCloseWithWeightedScore() {
        ReviewCase rc = caseAtRereviewInProgress();
        // initialReviewScore = 90 (from caseAtPendingRereview)
        // rereviewScore = 70, weights = 0.5/0.5 → finalScore = 80.00
        rc.completeRereview(
                new BigDecimal("70"),
                new BigDecimal("0.5"),
                new BigDecimal("0.5"));
        assertEquals(CaseStatus.CLOSED, rc.getStatus());
        assertEquals(0, new BigDecimal("80.00").compareTo(rc.getFinalScore()));
        assertNotNull(rc.getClosedAt());
    }

    @Test
    void completeRereview_asymmetricWeights_shouldCalculateCorrectly() {
        ReviewCase rc = caseAtRereviewInProgress();
        // initialReviewScore = 90, rereviewScore = 70
        // weights = 0.7/0.3 → 90*0.7 + 70*0.3 = 63 + 21 = 84.00
        rc.completeRereview(
                new BigDecimal("70"),
                new BigDecimal("0.7"),
                new BigDecimal("0.3"));
        assertEquals(0, new BigDecimal("84.00").compareTo(rc.getFinalScore()));
    }

    @Test
    void completeRereview_wrongState_shouldThrow() {
        ReviewCase rc = caseAtPendingRereview(); // not REREVIEW_IN_PROGRESS
        assertThrows(DomainStateConflictException.class,
                () -> rc.completeRereview(new BigDecimal("70"), new BigDecimal("0.5"), new BigDecimal("0.5")));
    }

    // --- updateFinalScoreForRereview tests (rereview revision) ---

    @Test
    void updateFinalScoreForRereview_closedCase_shouldUpdateScore() {
        ReviewCase rc = caseAtRereviewInProgress();
        rc.completeRereview(new BigDecimal("70"), new BigDecimal("0.5"), new BigDecimal("0.5"));
        // CLOSED state
        assertEquals(CaseStatus.CLOSED, rc.getStatus());
        // Rereview revision updates final score
        rc.updateFinalScoreForRereview(
                new BigDecimal("80"),
                new BigDecimal("0.5"),
                new BigDecimal("0.5"));
        // 90*0.5 + 80*0.5 = 85.00
        assertEquals(0, new BigDecimal("85.00").compareTo(rc.getFinalScore()));
    }

    @Test
    void updateFinalScoreForRereview_cancelledCase_shouldThrow() {
        ReviewCase rc = caseAtPendingRereview();
        rc.assignRereviewer("rereviewer-1", LocalDateTime.now());
        rc.startRereview("rereviewer-1");
        // Force cancel for test — use reconstitute
        ReviewCase cancelledCase = ReviewCase.reconstitute(
                "c1", "b1", "p1", "H001", "SK-001", MedicalRecordType.OUTPATIENT,
                CaseStatus.CANCELLED, null, null,
                "test", null, LocalDateTime.now(), null,
                false, null, null, null, null, null,
                null, null, null, null);
        assertThrows(DomainStateConflictException.class,
                () -> cancelledCase.updateFinalScoreForRereview(
                        new BigDecimal("80"), new BigDecimal("0.5"), new BigDecimal("0.5")));
    }
}
