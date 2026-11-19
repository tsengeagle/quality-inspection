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
        rc.assignReviewer("reviewer-1", LocalDateTime.of(2026, 12, 31, 23, 59), LocalDateTime.now());
        rc.startInitialReview("sub-1", "ss-1");
        return rc;
    }

    private static final java.time.Instant FIXED_AT = java.time.Instant.parse("2026-01-01T00:00:00Z");

    /** 提交後、尚未固定。 */
    private ReviewCase submitted(String score) {
        ReviewCase rc = caseInProgress();
        rc.completeInitialReview(new BigDecimal(score));
        return rc;
    }

    // --- D-031、D-054：提交不判斷品質線，固定時才判斷 ---

    @Test
    void completeInitialReview_alwaysStaysCompletedAndUnfixed_D054() {
        ReviewCase rc = submitted("90");
        assertEquals(CaseStatus.INITIAL_REVIEW_COMPLETED, rc.getStatus());
        assertEquals(new BigDecimal("90"), rc.getInitialReviewScore());
        assertFalse(rc.isInitialReviewFixed());
    }

    @Test
    void fix_scoreAboveThreshold_shouldEscalateToPendingRereview() {
        ReviewCase rc = submitted("90");
        rc.fixInitialReview("sub-1", "reviewer-1", FIXED_AT, new BigDecimal("90"), new BigDecimal("80"));
        assertEquals(CaseStatus.PENDING_REREVIEW, rc.getStatus());
        assertTrue(rc.isRereviewRequired());
        // 進複審時還沒有最終分數：保留初審分數，finalScore 待複審提交時加權計算（與舊的自動進複審路徑相同）。
        assertEquals(new BigDecimal("90"), rc.getInitialReviewScore());
        assertNull(rc.getFinalScore());
        assertEquals(FIXED_AT, rc.getInitialFixedAt());
        assertEquals("reviewer-1", rc.getInitialFixedBy());
        assertEquals("sub-1", rc.getInitialFixedSubmissionId());
    }

    @Test
    void fix_scoreEqualToThreshold_shouldStayCompleted() {
        ReviewCase rc = submitted("80");
        rc.fixInitialReview("sub-1", "reviewer-1", FIXED_AT, new BigDecimal("80"), new BigDecimal("80"));
        assertEquals(CaseStatus.INITIAL_REVIEW_COMPLETED, rc.getStatus());
        assertTrue(rc.isInitialReviewFixed());
        assertEquals(new BigDecimal("80"), rc.getFinalScore());
    }

    @Test
    void fix_scoreBelowThreshold_shouldStayCompleted() {
        ReviewCase rc = submitted("60");
        rc.fixInitialReview("sub-1", "reviewer-1", FIXED_AT, new BigDecimal("60"), new BigDecimal("80"));
        assertEquals(CaseStatus.INITIAL_REVIEW_COMPLETED, rc.getStatus());
    }

    @Test
    void fix_nullThreshold_shouldStayCompleted() {
        ReviewCase rc = submitted("90");
        rc.fixInitialReview("sub-1", "reviewer-1", FIXED_AT, new BigDecimal("90"), null);
        assertEquals(CaseStatus.INITIAL_REVIEW_COMPLETED, rc.getStatus());
    }

    @Test
    void fix_notTheCurrentInitialSubmission_isConflict() {
        ReviewCase rc = submitted("90");
        DomainStateConflictException e = assertThrows(DomainStateConflictException.class,
                () -> rc.fixInitialReview("old", "reviewer-1", FIXED_AT, new BigDecimal("90"), null));
        assertEquals("CONFLICT", e.getErrorCode());
        assertFalse(rc.isInitialReviewFixed());
    }

    @Test
    void afterFixing_revisionIsRejected_D054() {
        ReviewCase rc = submitted("60");
        rc.fixInitialReview("sub-1", "reviewer-1", FIXED_AT, new BigDecimal("60"), new BigDecimal("80"));
        DomainStateConflictException e = assertThrows(DomainStateConflictException.class,
                () -> rc.recordInitialReviewRevision("sub-1", "sub-2", new BigDecimal("70")));
        assertEquals("INVALID_STATE_TRANSITION", e.getErrorCode());
    }

    @Test
    void unfixed_closeWithoutRereviewAndSendToRereviewAreRejected_D054() {
        ReviewCase rc = submitted("60");
        assertEquals("INVALID_STATE_TRANSITION",
                assertThrows(DomainStateConflictException.class, () -> rc.closeWithoutRereview(LocalDateTime.now())).getErrorCode());
        assertEquals("INVALID_STATE_TRANSITION",
                assertThrows(DomainStateConflictException.class, rc::sendToRereview).getErrorCode());
        assertEquals(CaseStatus.INITIAL_REVIEW_COMPLETED, rc.getStatus());
    }

    @Test
    void completeInitialReview_wrongState_shouldThrow() {
        ReviewCase rc = ReviewCase.createFromSampling(
                "c1", "b1", "p1", "H001", "SK-001", MedicalRecordType.OUTPATIENT);
        assertThrows(DomainStateConflictException.class,
                () -> rc.completeInitialReview(new BigDecimal("90")));
    }

    /** 已提交且已固定（60 ≤ 品質線 80），留在 INITIAL_REVIEW_COMPLETED 等管理者處理。 */
    private ReviewCase caseAtInitialReviewCompleted() {
        ReviewCase rc = submitted("60");
        rc.fixInitialReview("sub-1", "reviewer-1", FIXED_AT, new BigDecimal("60"), new BigDecimal("80"));
        return rc;
    }

    // --- closeWithoutRereview tests ---

    @Test
    void closeWithoutRereview_happyPath_shouldCloseCaseWithInitialScore() {
        ReviewCase rc = caseAtInitialReviewCompleted();
        rc.closeWithoutRereview(LocalDateTime.now());
        assertEquals(CaseStatus.CLOSED, rc.getStatus());
        assertEquals(new BigDecimal("60"), rc.getFinalScore());
        assertNotNull(rc.getClosedAt());
    }

    @Test
    void closeWithoutRereview_wrongState_shouldThrow() {
        ReviewCase rc = caseInProgress();
        assertThrows(DomainStateConflictException.class, () -> rc.closeWithoutRereview(LocalDateTime.now()));
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

    /** 已提交且已固定（90 > 品質線 80），進 PENDING_REREVIEW。 */
    private ReviewCase caseAtPendingRereview() {
        ReviewCase rc = submitted("90");
        rc.fixInitialReview("sub-1", "reviewer-1", FIXED_AT, new BigDecimal("90"), new BigDecimal("80"));
        return rc;
    }

    // --- assignRereviewer tests ---

    @Test
    void assignRereviewer_happyPath_shouldSetAssignment() {
        ReviewCase rc = caseAtPendingRereview();
        LocalDateTime dueAt = LocalDateTime.of(2026, 6, 30, 23, 59);
        rc.assignRereviewer("rereviewer-1", dueAt, LocalDateTime.now());
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
                () -> rc.assignRereviewer("reviewer-1", LocalDateTime.now(), LocalDateTime.now()));
    }

    @Test
    void assignRereviewer_wrongState_shouldThrow() {
        ReviewCase rc = caseInProgress(); // INITIAL_REVIEW_IN_PROGRESS
        assertThrows(DomainStateConflictException.class,
                () -> rc.assignRereviewer("rereviewer-1", LocalDateTime.now(), LocalDateTime.now()));
    }

    // --- reassignRereviewer tests ---

    @Test
    void reassignRereviewer_happyPath_shouldReplaceAssignment() {
        ReviewCase rc = caseAtPendingRereview();
        rc.assignRereviewer("rereviewer-1", LocalDateTime.now(), LocalDateTime.now());
        LocalDateTime newDue = LocalDateTime.of(2026, 7, 15, 23, 59);
        rc.reassignRereviewer("rereviewer-2", newDue, LocalDateTime.now());
        assertEquals("rereviewer-2", rc.getRereviewerAssignment().getReviewerId());
        assertEquals(newDue, rc.getRereviewerDueAt());
    }

    @Test
    void reassignRereviewer_sameAsInitialReviewer_shouldThrow() {
        ReviewCase rc = caseAtPendingRereview();
        rc.assignRereviewer("rereviewer-1", LocalDateTime.now(), LocalDateTime.now());
        assertThrows(DomainRuleViolationException.class,
                () -> rc.reassignRereviewer("reviewer-1", LocalDateTime.now(), LocalDateTime.now()));
    }

    // --- startRereview tests ---

    @Test
    void startRereview_happyPath_shouldTransitionToInProgress() {
        ReviewCase rc = caseAtPendingRereview();
        rc.assignRereviewer("rereviewer-1", LocalDateTime.now(), LocalDateTime.now());
        rc.startRereview("rereviewer-1");
        assertEquals(CaseStatus.REREVIEW_IN_PROGRESS, rc.getStatus());
    }

    @Test
    void startRereview_wrongActor_shouldThrow() {
        ReviewCase rc = caseAtPendingRereview();
        rc.assignRereviewer("rereviewer-1", LocalDateTime.now(), LocalDateTime.now());
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
        rc.assignRereviewer("rereviewer-1", LocalDateTime.of(2026, 6, 30, 23, 59), LocalDateTime.now());
        rc.startRereview("rereviewer-1");
        return rc;
    }

    // --- completeRereview tests ---

    @Test
    void completeRereview_happyPath_shouldComputeWeightedScoreWithoutClosing_D032() {
        ReviewCase rc = caseAtRereviewInProgress();
        // initialReviewScore = 90 (from caseAtPendingRereview)
        // rereviewScore = 70, weights = 0.5/0.5 → finalScore = 80.00
        rc.completeRereview(
                new BigDecimal("70"),
                new BigDecimal("0.5"),
                new BigDecimal("0.5"));
        // D-032／D-053 第 4 點：提交後維持 REREVIEW_IN_PROGRESS，不結案
        assertEquals(CaseStatus.REREVIEW_IN_PROGRESS, rc.getStatus());
        assertEquals(0, new BigDecimal("80.00").compareTo(rc.getFinalScore()));
        assertNull(rc.getClosedAt());
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

    // --- closeAfterRereview tests (D-034／D-053) ---

    private static final LocalDateTime CLOSED_AT = LocalDateTime.of(2026, 11, 15, 10, 0);

    @Test
    void closeAfterRereview_fromRereviewInProgress_shouldCloseAndKeepScore() {
        ReviewCase rc = caseAtRereviewInProgress();
        rc.completeRereview(new BigDecimal("70"), new BigDecimal("0.5"), new BigDecimal("0.5"));
        rc.closeAfterRereview(CLOSED_AT);
        assertEquals(CaseStatus.CLOSED, rc.getStatus());
        assertEquals(CLOSED_AT, rc.getClosedAt());
        assertEquals(0, new BigDecimal("80.00").compareTo(rc.getFinalScore()));
    }

    @Test
    void closeAfterRereview_alreadyClosed_shouldRejectWithCaseTerminal_D053() {
        ReviewCase rc = caseAtRereviewInProgress();
        rc.completeRereview(new BigDecimal("70"), new BigDecimal("0.5"), new BigDecimal("0.5"));
        rc.closeAfterRereview(CLOSED_AT);
        LocalDateTime closedAt = rc.getClosedAt();
        DomainStateConflictException e = assertThrows(DomainStateConflictException.class, () -> rc.closeAfterRereview(CLOSED_AT.plusMinutes(5)));
        assertEquals("CASE_TERMINAL", e.getErrorCode());
        assertEquals(closedAt, rc.getClosedAt());
    }

    @Test
    void closeAfterRereview_notInRereview_shouldReject() {
        ReviewCase rc = caseAtPendingRereview();
        DomainStateConflictException e = assertThrows(DomainStateConflictException.class, () -> rc.closeAfterRereview(CLOSED_AT.plusMinutes(5)));
        assertEquals("CASE_NOT_REREVIEW_IN_PROGRESS", e.getErrorCode());
        assertEquals(CaseStatus.PENDING_REREVIEW, rc.getStatus());
        assertNull(rc.getClosedAt());
    }

    // --- updateFinalScoreForRereview tests (rereview revision) ---

    @Test
    void updateFinalScoreForRereview_closedCase_shouldRejectWithoutChangingScore() {
        ReviewCase rc = caseAtRereviewInProgress();
        rc.completeRereview(new BigDecimal("70"), new BigDecimal("0.5"), new BigDecimal("0.5"));
        rc.closeAfterRereview(CLOSED_AT);
        BigDecimal beforeScore = rc.getFinalScore();
        LocalDateTime beforeClosedAt = rc.getClosedAt();
        assertThrows(DomainStateConflictException.class, () -> rc.updateFinalScoreForRereview(
                new BigDecimal("80"), new BigDecimal("0.5"), new BigDecimal("0.5")));
        assertEquals(beforeScore, rc.getFinalScore());
        assertEquals(beforeClosedAt, rc.getClosedAt());
        assertEquals(CaseStatus.CLOSED, rc.getStatus());
    }

    @Test
    void updateFinalScoreForRereview_cancelledCase_shouldThrow() {
        ReviewCase rc = caseAtPendingRereview();
        rc.assignRereviewer("rereviewer-1", LocalDateTime.now(), LocalDateTime.now());
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
