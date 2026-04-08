package com.tzuchi.his.quality.inspection.domain.reviewcase;

import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ReviewCaseTest {

    private static final LocalDateTime DUE = LocalDateTime.of(2026, 2, 1, 23, 59);

    // --- Helper ---

    private ReviewCase createPendingAssignment() {
        return ReviewCase.createFromSampling("c1", "b1", "p1", "H001", "SK-001", MedicalRecordType.OUTPATIENT);
    }

    private ReviewCase createPendingInitialReview() {
        ReviewCase rc = createPendingAssignment();
        rc.assignReviewer("reviewer-1", DUE);
        return rc;
    }

    private ReviewCase createInProgress() {
        ReviewCase rc = createPendingInitialReview();
        rc.forceStatus(CaseStatus.INITIAL_REVIEW_IN_PROGRESS);
        return rc;
    }

    private ReviewCase createReturned() {
        ReviewCase rc = createInProgress();
        rc.returnToInitialReview("needs correction");
        return rc;
    }

    // --- createFromSampling ---

    @Test
    void createFromSampling_shouldStartAtSelectedPendingAssignment() {
        ReviewCase rc = createPendingAssignment();

        assertEquals(CaseStatus.SELECTED_PENDING_ASSIGNMENT, rc.getStatus());
        assertNull(rc.getReviewerAssignment());
        assertNull(rc.getDueAt());
    }

    // --- assignReviewer ---

    @Test
    void assignReviewer_fromSelectedPending_shouldSucceed() {
        ReviewCase rc = createPendingAssignment();

        rc.assignReviewer("reviewer-1", DUE);

        assertEquals(CaseStatus.PENDING_INITIAL_REVIEW, rc.getStatus());
        assertNotNull(rc.getReviewerAssignment());
        assertEquals("reviewer-1", rc.getReviewerAssignment().getReviewerId());
        assertEquals(DUE, rc.getDueAt());
    }

    @Test
    void assignReviewer_fromNonSelectedPending_shouldThrow() {
        ReviewCase rc = createPendingInitialReview(); // already PENDING_INITIAL_REVIEW

        assertThrows(DomainStateConflictException.class,
                () -> rc.assignReviewer("reviewer-2", DUE));
    }

    @Test
    void assignReviewer_nullReviewerId_shouldThrow() {
        ReviewCase rc = createPendingAssignment();

        assertThrows(NullPointerException.class,
                () -> rc.assignReviewer(null, DUE));
    }

    // --- reassignReviewer ---

    @Test
    void reassignReviewer_fromPendingInitialReview_shouldSucceed() {
        ReviewCase rc = createPendingInitialReview();
        LocalDateTime newDue = DUE.plusDays(7);

        rc.reassignReviewer("reviewer-2", newDue);

        assertEquals(CaseStatus.PENDING_INITIAL_REVIEW, rc.getStatus());
        assertEquals("reviewer-2", rc.getReviewerAssignment().getReviewerId());
        assertEquals(newDue, rc.getDueAt());
    }

    @Test
    void reassignReviewer_fromNonPendingInitialReview_shouldThrow() {
        ReviewCase rc = createPendingAssignment(); // SELECTED_PENDING_ASSIGNMENT

        assertThrows(DomainStateConflictException.class,
                () -> rc.reassignReviewer("reviewer-2", DUE));
    }

    // --- cancel ---

    @Test
    void cancel_fromSelectedPending_shouldSucceed() {
        ReviewCase rc = createPendingAssignment();

        rc.cancel("project cancelled");

        assertEquals(CaseStatus.CANCELLED, rc.getStatus());
        assertEquals("project cancelled", rc.getCancelReason());
        assertNotNull(rc.getCancelledAt());
    }

    @Test
    void cancel_fromPendingInitialReview_shouldSucceed() {
        ReviewCase rc = createPendingInitialReview();

        rc.cancel("reviewer unavailable");

        assertEquals(CaseStatus.CANCELLED, rc.getStatus());
    }

    @Test
    void cancel_fromInProgress_shouldSucceed() {
        ReviewCase rc = createInProgress();

        rc.cancel("duplicate case");

        assertEquals(CaseStatus.CANCELLED, rc.getStatus());
    }

    @Test
    void cancel_fromReturned_shouldSucceed() {
        ReviewCase rc = createReturned();

        rc.cancel("no longer needed");

        assertEquals(CaseStatus.CANCELLED, rc.getStatus());
    }

    @Test
    void cancel_fromClosed_shouldThrow() {
        ReviewCase rc = createPendingAssignment();
        rc.forceStatus(CaseStatus.CLOSED);

        assertThrows(DomainStateConflictException.class,
                () -> rc.cancel("too late"));
    }

    @Test
    void cancel_fromCancelled_shouldThrow() {
        ReviewCase rc = createPendingAssignment();
        rc.cancel("first cancel");

        assertThrows(DomainStateConflictException.class,
                () -> rc.cancel("second cancel"));
    }

    // --- returnToInitialReview ---

    @Test
    void returnToInitialReview_fromInProgress_shouldSucceed() {
        ReviewCase rc = createInProgress();

        rc.returnToInitialReview("needs correction");

        assertEquals(CaseStatus.RETURNED, rc.getStatus());
        assertEquals("needs correction", rc.getReturnReason());
    }

    @Test
    void returnToInitialReview_fromNonInProgress_shouldThrow() {
        ReviewCase rc = createPendingInitialReview(); // PENDING_INITIAL_REVIEW

        assertThrows(DomainStateConflictException.class,
                () -> rc.returnToInitialReview("nope"));
    }

    // --- reopenInitialReview ---

    @Test
    void reopenInitialReview_fromReturned_shouldSucceed() {
        ReviewCase rc = createReturned();

        rc.reopenInitialReview();

        assertEquals(CaseStatus.PENDING_INITIAL_REVIEW, rc.getStatus());
        assertNull(rc.getReturnReason());
    }

    @Test
    void reopenInitialReview_fromNonReturned_shouldThrow() {
        ReviewCase rc = createPendingInitialReview(); // PENDING_INITIAL_REVIEW

        assertThrows(DomainStateConflictException.class,
                () -> rc.reopenInitialReview());
    }

    // --- reconstitute ---

    @Test
    void reconstitute_shouldRestoreAllFields() {
        LocalDateTime assignedAt = LocalDateTime.of(2026, 1, 10, 9, 0);
        ReviewerAssignment assignment = new ReviewerAssignment("reviewer-1", assignedAt);

        ReviewCase rc = ReviewCase.reconstitute(
                "c1", "b1", "p1", "H001", "SK-001", MedicalRecordType.OUTPATIENT,
                CaseStatus.PENDING_INITIAL_REVIEW, assignment, DUE,
                null, null, null, null,
                false, null, null, null, null, null);

        assertEquals("c1", rc.getCaseId());
        assertEquals(CaseStatus.PENDING_INITIAL_REVIEW, rc.getStatus());
        assertEquals("reviewer-1", rc.getReviewerAssignment().getReviewerId());
        assertEquals(DUE, rc.getDueAt());
    }
}
