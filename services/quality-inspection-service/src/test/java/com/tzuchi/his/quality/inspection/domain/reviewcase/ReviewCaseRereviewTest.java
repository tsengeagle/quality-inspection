package com.tzuchi.his.quality.inspection.domain.reviewcase;

import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ReviewCaseRereviewTest {

    private ReviewCase caseInProgress() {
        ReviewCase rc = ReviewCase.createFromSampling(
                "c1", "b1", "p1", "H001", "SK-001", MedicalRecordType.OUTPATIENT);
        rc.assignReviewer("reviewer-1", Instant.parse("2026-12-31T23:59:00Z"), Instant.now());
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
                assertThrows(DomainStateConflictException.class, () -> rc.closeWithoutRereview(Instant.now())).getErrorCode());
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
        rc.closeWithoutRereview(Instant.now());
        assertEquals(CaseStatus.CLOSED, rc.getStatus());
        assertEquals(new BigDecimal("60"), rc.getFinalScore());
        assertNotNull(rc.getClosedAt());
    }

    @Test
    void closeWithoutRereview_wrongState_shouldThrow() {
        ReviewCase rc = caseInProgress();
        assertThrows(DomainStateConflictException.class, () -> rc.closeWithoutRereview(Instant.now()));
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
        Instant dueAt = Instant.parse("2026-06-30T23:59:00Z");
        rc.assignRereviewer("rereviewer-1", dueAt, Instant.now());
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
                () -> rc.assignRereviewer("reviewer-1", Instant.now(), Instant.now()));
    }

    @Test
    void assignRereviewer_wrongState_shouldThrow() {
        ReviewCase rc = caseInProgress(); // INITIAL_REVIEW_IN_PROGRESS
        // D-069 第 2 點：與指派初審醫師相同
        DomainRuleViolationException e = assertThrows(DomainRuleViolationException.class,
                () -> rc.assignRereviewer("rereviewer-1", Instant.now(), Instant.now()));
        assertEquals("CASE_NOT_ASSIGNABLE", e.getErrorCode());
    }

    // --- reassignRereviewer tests ---

    @Test
    void reassignRereviewer_happyPath_shouldReplaceAssignment() {
        ReviewCase rc = caseAtPendingRereview();
        rc.assignRereviewer("rereviewer-1", Instant.now(), Instant.now());
        Instant newDue = Instant.parse("2026-07-15T23:59:00Z");
        rc.reassignRereviewer("rereviewer-2", newDue, Instant.now());
        assertEquals("rereviewer-2", rc.getRereviewerAssignment().getReviewerId());
        assertEquals(newDue, rc.getRereviewerDueAt());
    }

    @Test
    void reassignRereviewer_sameAsInitialReviewer_shouldThrow() {
        ReviewCase rc = caseAtPendingRereview();
        rc.assignRereviewer("rereviewer-1", Instant.now(), Instant.now());
        assertThrows(DomainRuleViolationException.class,
                () -> rc.reassignRereviewer("reviewer-1", Instant.now(), Instant.now()));
    }

    @Test
    void reassignRereviewer_beforeAnyAssignment_shouldThrowNoRereviewerAssigned() {
        // D-082 第 4 點
        ReviewCase rc = caseAtPendingRereview();
        DomainRuleViolationException e = assertThrows(DomainRuleViolationException.class,
                () -> rc.reassignRereviewer("rereviewer-2", Instant.now(), Instant.now()));
        assertEquals("NO_REREVIEWER_ASSIGNED", e.getErrorCode());
        assertNull(rc.getRereviewerAssignment());
    }

    @Test
    void reassignRereviewer_toSamePerson_shouldOnlyChangeDueDate() {
        // D-082 第 3 點
        ReviewCase rc = caseAtPendingRereview();
        rc.assignRereviewer("rereviewer-1", Instant.now(), Instant.now());
        Instant newDue = Instant.parse("2026-07-20T23:59:00Z");
        rc.reassignRereviewer("rereviewer-1", newDue, Instant.now());
        assertEquals("rereviewer-1", rc.getRereviewerAssignment().getReviewerId());
        assertEquals(newDue, rc.getRereviewerDueAt());
    }

    // --- startRereview tests ---

    @Test
    void startRereview_happyPath_shouldTransitionToInProgress() {
        ReviewCase rc = caseAtPendingRereview();
        rc.assignRereviewer("rereviewer-1", Instant.now(), Instant.now());
        rc.startRereview("rereviewer-1");
        assertEquals(CaseStatus.REREVIEW_IN_PROGRESS, rc.getStatus());
    }

    @Test
    void startRereview_wrongActor_shouldThrow() {
        ReviewCase rc = caseAtPendingRereview();
        rc.assignRereviewer("rereviewer-1", Instant.now(), Instant.now());
        assertThrows(DomainRuleViolationException.class,
                () -> rc.startRereview("someone-else"));
    }

    @Test
    void startRereview_wrongState_shouldThrow() {
        ReviewCase rc = caseInProgress();
        // D-069 第 3 點：複審的狀態錯誤共用一個代碼
        DomainRuleViolationException e = assertThrows(DomainRuleViolationException.class,
                () -> rc.startRereview("rereviewer-1"));
        assertEquals("CASE_NOT_IN_REREVIEW_STAGE", e.getErrorCode());
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
        rc.assignRereviewer("rereviewer-1", Instant.parse("2026-06-30T23:59:00Z"), Instant.now());
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
        DomainRuleViolationException e = assertThrows(DomainRuleViolationException.class,
                () -> rc.completeRereview(new BigDecimal("70"), new BigDecimal("0.5"), new BigDecimal("0.5")));
        assertEquals("CASE_NOT_IN_REREVIEW_STAGE", e.getErrorCode());
    }

    // --- closeAfterRereview tests (D-034／D-053) ---

    private static final Instant CLOSED_AT = Instant.parse("2026-11-15T10:00:00Z");

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
        Instant closedAt = rc.getClosedAt();
        DomainStateConflictException e = assertThrows(DomainStateConflictException.class, () -> rc.closeAfterRereview(CLOSED_AT.plus(java.time.Duration.ofMinutes(5))));
        assertEquals("CASE_TERMINAL", e.getErrorCode());
        assertEquals(closedAt, rc.getClosedAt());
    }

    @Test
    void closeAfterRereview_notInRereview_shouldReject() {
        ReviewCase rc = caseAtPendingRereview();
        DomainRuleViolationException e = assertThrows(DomainRuleViolationException.class, () -> rc.closeAfterRereview(CLOSED_AT.plus(java.time.Duration.ofMinutes(5))));
        assertEquals("CASE_NOT_IN_REREVIEW_STAGE", e.getErrorCode());
        assertEquals(CaseStatus.PENDING_REREVIEW, rc.getStatus());
        assertNull(rc.getClosedAt());
    }

    // --- recordRereviewRevision tests (rereview revision, D-083 第 9 點) ---

    @Test
    void recordRereviewRevision_recalculatesFinalScoreAndPointsAtNewSubmission() {
        ReviewCase rc = caseAtRereviewInProgress();
        ReviewCaseTestSupport.pointRereviewAt(rc, "sub-r1");
        rc.completeRereview(new BigDecimal("70"), new BigDecimal("0.5"), new BigDecimal("0.5"));
        CaseStatus before = rc.getStatus();

        rc.recordRereviewRevision("sub-r1", "sub-r2", new BigDecimal("20"),
                new BigDecimal("0.5"), new BigDecimal("0.5"));

        assertEquals(0, rc.getInitialReviewScore().multiply(new BigDecimal("0.5"))
                .add(new BigDecimal("10")).compareTo(rc.getFinalScore()));
        assertEquals("sub-r2", rc.getLatestRereviewSubmissionId());
        assertEquals(before, rc.getStatus());
    }

    @Test
    void recordRereviewRevision_staleSubmission_shouldRejectWithoutChange() {
        ReviewCase rc = caseAtRereviewInProgress();
        ReviewCaseTestSupport.pointRereviewAt(rc, "sub-r2");
        rc.completeRereview(new BigDecimal("70"), new BigDecimal("0.5"), new BigDecimal("0.5"));
        BigDecimal beforeScore = rc.getFinalScore();

        DomainStateConflictException e = assertThrows(DomainStateConflictException.class,
                () -> rc.recordRereviewRevision("sub-r1", "sub-r3", new BigDecimal("20"),
                        new BigDecimal("0.5"), new BigDecimal("0.5")));
        assertEquals("CONFLICT", e.getErrorCode());
        assertEquals(beforeScore, rc.getFinalScore());
        assertEquals("sub-r2", rc.getLatestRereviewSubmissionId());
    }

    @Test
    void recordRereviewRevision_closedCase_shouldRejectWithoutChangingScore() {
        ReviewCase rc = caseAtRereviewInProgress();
        rc.completeRereview(new BigDecimal("70"), new BigDecimal("0.5"), new BigDecimal("0.5"));
        rc.closeAfterRereview(CLOSED_AT);
        BigDecimal beforeScore = rc.getFinalScore();
        Instant beforeClosedAt = rc.getClosedAt();
        assertThrows(DomainStateConflictException.class, () -> rc.recordRereviewRevision(
                rc.getLatestRereviewSubmissionId(), "sub-new",
                new BigDecimal("80"), new BigDecimal("0.5"), new BigDecimal("0.5")));
        assertEquals(beforeScore, rc.getFinalScore());
        assertEquals(beforeClosedAt, rc.getClosedAt());
        assertEquals(CaseStatus.CLOSED, rc.getStatus());
    }

    @Test
    void recordRereviewRevision_cancelledCase_shouldThrow() {
        ReviewCase cancelledCase = ReviewCase.reconstitute(
                "c1", "b1", "p1", "H001", "SK-001", MedicalRecordType.OUTPATIENT,
                CaseStatus.CANCELLED, null, null,
                "test", null, Instant.now(), null,
                false, null, null, null, null, null,
                null, null, null, null);
        assertThrows(DomainStateConflictException.class,
                () -> cancelledCase.recordRereviewRevision(null, "sub-new",
                        new BigDecimal("80"), new BigDecimal("0.5"), new BigDecimal("0.5")));
    }

    @Test
    void startRereview_withDraft_pointsAtDraft() {
        ReviewCase rc = caseAtPendingRereview();
        rc.assignRereviewer("rereviewer-1", Instant.now(), Instant.now());
        rc.startRereview("rereviewer-1", "sub-draft");
        assertEquals(CaseStatus.REREVIEW_IN_PROGRESS, rc.getStatus());
        assertEquals("sub-draft", rc.getLatestRereviewSubmissionId());
    }
}
