package com.tzuchi.his.quality.inspection.domain.reviewcase;

import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.common.DomainValidationException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ReviewCaseTest {

    private static final Instant DUE = Instant.parse("2026-02-01T23:59:00Z");

    // --- Helper ---

    private ReviewCase createPendingAssignment() {
        return ReviewCase.createFromSampling("c1", "b1", "p1", "H001", "SK-001", MedicalRecordType.OUTPATIENT);
    }

    private ReviewCase createPendingInitialReview() {
        ReviewCase rc = createPendingAssignment();
        rc.assignReviewer("reviewer-1", DUE, Instant.now());
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

        rc.assignReviewer("reviewer-1", DUE, Instant.now());

        assertEquals(CaseStatus.PENDING_INITIAL_REVIEW, rc.getStatus());
        assertNotNull(rc.getReviewerAssignment());
        assertEquals("reviewer-1", rc.getReviewerAssignment().getReviewerId());
        assertEquals(DUE, rc.getDueAt());
    }

    @Test
    void assignReviewer_fromNonSelectedPending_shouldThrow() {
        ReviewCase rc = createPendingInitialReview(); // already PENDING_INITIAL_REVIEW

        DomainRuleViolationException e = assertThrows(DomainRuleViolationException.class,
                () -> rc.assignReviewer("reviewer-2", DUE, Instant.now()));
        assertEquals("CASE_NOT_ASSIGNABLE", e.getErrorCode());
    }

    @Test
    void assignReviewer_nullReviewerId_shouldThrow() {
        ReviewCase rc = createPendingAssignment();

        DomainValidationException e = assertThrows(DomainValidationException.class,
                () -> rc.assignReviewer(null, DUE, Instant.now()));
        assertEquals("VALIDATION_ERROR", e.getErrorCode());
    }

    @Test
    void assignReviewer_nullDueAt_shouldThrow() {
        ReviewCase rc = createPendingAssignment();

        DomainValidationException e = assertThrows(DomainValidationException.class,
                () -> rc.assignReviewer("reviewer-1", null, Instant.now()));
        assertEquals("VALIDATION_ERROR", e.getErrorCode());
        assertEquals(CaseStatus.SELECTED_PENDING_ASSIGNMENT, rc.getStatus());
    }

    // --- reassignReviewer ---

    @Test
    void reassignReviewer_fromPendingInitialReview_shouldSucceed() {
        ReviewCase rc = createPendingInitialReview();
        Instant newDue = DUE.plus(java.time.Duration.ofDays(7));

        rc.reassignReviewer("reviewer-2", newDue, Instant.now());

        assertEquals(CaseStatus.PENDING_INITIAL_REVIEW, rc.getStatus());
        assertEquals("reviewer-2", rc.getReviewerAssignment().getReviewerId());
        assertEquals(newDue, rc.getDueAt());
    }

    @Test
    void reassignReviewer_fromNonPendingInitialReview_shouldThrow() {
        ReviewCase rc = createPendingAssignment(); // SELECTED_PENDING_ASSIGNMENT

        // D-082 第 1 點：與指派一致（422）
        DomainRuleViolationException e = assertThrows(DomainRuleViolationException.class,
                () -> rc.reassignReviewer("reviewer-2", DUE, Instant.now()));
        assertEquals("CASE_NOT_ASSIGNABLE", e.getErrorCode());
    }

    @Test
    void reassignReviewer_blankInput_shouldFailValidationBeforeStateCheck() {
        // D-082 第 2 點：輸入檢查在狀態檢查之前（案件仍是待指派）
        ReviewCase rc = createPendingAssignment();
        assertEquals("VALIDATION_ERROR", assertThrows(DomainValidationException.class,
                () -> rc.reassignReviewer(" ", DUE, Instant.now())).getErrorCode());
        assertEquals("VALIDATION_ERROR", assertThrows(DomainValidationException.class,
                () -> rc.reassignReviewer("reviewer-2", null, Instant.now())).getErrorCode());
    }

    // --- cancel ---

    @Test
    void cancel_fromSelectedPending_shouldSucceed() {
        ReviewCase rc = createPendingAssignment();

        rc.cancel("project cancelled", Instant.now());

        assertEquals(CaseStatus.CANCELLED, rc.getStatus());
        assertEquals("project cancelled", rc.getCancelReason());
        assertNotNull(rc.getCancelledAt());
    }

    @Test
    void cancel_fromPendingInitialReview_shouldSucceed() {
        ReviewCase rc = createPendingInitialReview();

        rc.cancel("reviewer unavailable", Instant.now());

        assertEquals(CaseStatus.CANCELLED, rc.getStatus());
    }

    @Test
    void cancel_fromInProgress_shouldSucceed() {
        ReviewCase rc = createInProgress();

        rc.cancel("duplicate case", Instant.now());

        assertEquals(CaseStatus.CANCELLED, rc.getStatus());
    }

    @Test
    void cancel_fromReturned_shouldSucceed() {
        ReviewCase rc = createReturned();

        rc.cancel("no longer needed", Instant.now());

        assertEquals(CaseStatus.CANCELLED, rc.getStatus());
    }

    @Test
    void cancel_fromClosed_shouldThrow() {
        ReviewCase rc = createPendingAssignment();
        rc.forceStatus(CaseStatus.CLOSED);

        assertThrows(DomainStateConflictException.class,
                () -> rc.cancel("too late", Instant.now()));
    }

    @Test
    void cancel_fromCancelled_shouldThrow() {
        ReviewCase rc = createPendingAssignment();
        rc.cancel("first cancel", Instant.now());

        assertThrows(DomainStateConflictException.class,
                () -> rc.cancel("second cancel", Instant.now()));
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
        Instant assignedAt = Instant.parse("2026-01-10T09:00:00Z");
        ReviewerAssignment assignment = new ReviewerAssignment("reviewer-1", assignedAt);

        ReviewCase rc = ReviewCase.reconstitute(
                "c1", "b1", "p1", "H001", "SK-001", MedicalRecordType.OUTPATIENT,
                CaseStatus.PENDING_INITIAL_REVIEW, assignment, DUE,
                null, null, null, null, null, null, null,
                null, null, null, null);

        assertEquals("c1", rc.getCaseId());
        assertEquals(CaseStatus.PENDING_INITIAL_REVIEW, rc.getStatus());
        assertEquals("reviewer-1", rc.getReviewerAssignment().getReviewerId());
        assertEquals(DUE, rc.getDueAt());
    }

    // --- startInitialReview (B3b) ---

    @Test
    void startInitialReview_fromPendingInitialReview_shouldSucceed() {
        ReviewCase rc = createPendingInitialReview();

        rc.startInitialReview("sub-1", "ss-v1");

        assertEquals(CaseStatus.INITIAL_REVIEW_IN_PROGRESS, rc.getStatus());
        assertEquals("sub-1", rc.getLatestInitialSubmissionId());
        assertEquals("ss-v1", rc.getScoreSheetVersionRef());
    }

    @Test
    void startInitialReview_fromNonPending_shouldThrow() {
        ReviewCase rc = createPendingAssignment(); // SELECTED_PENDING_ASSIGNMENT

        DomainRuleViolationException e = assertThrows(DomainRuleViolationException.class,
                () -> rc.startInitialReview("sub-1", "ss-v1"));
        assertEquals("CASE_NOT_IN_INITIAL_REVIEW_STAGE", e.getErrorCode());
    }

    @Test
    void startInitialReview_nullSubmissionId_shouldThrow() {
        ReviewCase rc = createPendingInitialReview();

        assertThrows(NullPointerException.class,
                () -> rc.startInitialReview(null, "ss-v1"));
    }

    @Test
    void startInitialReview_nullScoreSheetVersionRef_shouldThrow() {
        ReviewCase rc = createPendingInitialReview();

        assertThrows(NullPointerException.class,
                () -> rc.startInitialReview("sub-1", null));
    }

    // --- completeInitialReview (B3b) ---

    @Test
    void completeInitialReview_fromInProgress_shouldSucceed() {
        ReviewCase rc = createPendingInitialReview();
        rc.startInitialReview("sub-1", "ss-v1");

        rc.completeInitialReview(new BigDecimal("85.50"));

        assertEquals(CaseStatus.INITIAL_REVIEW_COMPLETED, rc.getStatus());
        assertEquals(new BigDecimal("85.50"), rc.getFinalScore());
    }

    @Test
    void completeInitialReview_fromNonInProgress_shouldThrow() {
        ReviewCase rc = createPendingInitialReview(); // PENDING_INITIAL_REVIEW

        assertThrows(DomainStateConflictException.class,
                () -> rc.completeInitialReview(new BigDecimal("85.50")));
    }

    @Test
    void completeInitialReview_nullTotalScore_shouldThrow() {
        ReviewCase rc = createPendingInitialReview();
        rc.startInitialReview("sub-1", "ss-v1");

        assertThrows(NullPointerException.class,
                () -> rc.completeInitialReview(null));
    }

    // --- requireNotTerminal ---

    @Test
    void requireNotTerminal_whenClosed_throwsConflict() {
        ReviewCase rc = createPendingAssignment();
        rc.forceStatus(CaseStatus.CLOSED);

        assertThrows(DomainStateConflictException.class, rc::requireNotTerminal);
    }

    @Test
    void requireNotTerminal_whenCancelled_throwsConflict() {
        ReviewCase rc = createPendingAssignment();
        rc.forceStatus(CaseStatus.CANCELLED);

        assertThrows(DomainStateConflictException.class, rc::requireNotTerminal);
    }

    @Test
    void requireNotTerminal_whenInitialReviewCompleted_doesNotThrow() {
        ReviewCase rc = createPendingAssignment();
        rc.forceStatus(CaseStatus.INITIAL_REVIEW_COMPLETED);

        assertDoesNotThrow(rc::requireNotTerminal);
    }

    @Test
    void requireNotTerminal_whenInProgress_doesNotThrow() {
        ReviewCase rc = createInProgress();

        assertDoesNotThrow(rc::requireNotTerminal);
    }

    // --- recordInitialReviewRevision（updateFinalScore 不再公開，D-083 第 9 點） ---

    @Test
    void recordInitialReviewRevision_updatesScore() {
        ReviewCase rc = createPendingAssignment();
        rc.forceStatus(CaseStatus.INITIAL_REVIEW_COMPLETED);

        rc.recordInitialReviewRevision(rc.getLatestInitialSubmissionId(), "sub-new", new BigDecimal("85.00"));
        assertEquals("sub-new", rc.getLatestInitialSubmissionId());

        assertEquals(new BigDecimal("85.00"), rc.getFinalScore());
    }

    @Test
    void recordInitialReviewRevision_whenClosed_throwsConflict() {
        ReviewCase rc = createPendingAssignment();
        rc.forceStatus(CaseStatus.CLOSED);

        assertThrows(DomainStateConflictException.class,
                () -> rc.recordInitialReviewRevision(rc.getLatestInitialSubmissionId(), "sub-new", new BigDecimal("85.00")));
    }

    @Test
    void recordInitialReviewRevision_whenCancelled_throwsConflict() {
        ReviewCase rc = createPendingAssignment();
        rc.forceStatus(CaseStatus.CANCELLED);

        assertThrows(DomainStateConflictException.class,
                () -> rc.recordInitialReviewRevision(rc.getLatestInitialSubmissionId(), "sub-new", new BigDecimal("85.00")));
    }

    @Test
    void recordInitialReviewRevision_nullScore_shouldThrow() {
        ReviewCase rc = createPendingAssignment();
        rc.forceStatus(CaseStatus.INITIAL_REVIEW_COMPLETED);

        assertThrows(NullPointerException.class,
                () -> rc.recordInitialReviewRevision(rc.getLatestInitialSubmissionId(), "sub-new", null));
    }
}
