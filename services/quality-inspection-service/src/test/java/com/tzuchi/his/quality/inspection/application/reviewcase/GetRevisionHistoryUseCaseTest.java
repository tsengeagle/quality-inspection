package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.RoleDeniedException;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.RevisionHistoryView;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryReviewCaseRepository;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryReviewSubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GetRevisionHistoryUseCaseTest {

    /** D-055：預設案件歷史完整、沒有歷史紀錄；需要時由個別測試調整。 */
    private final com.tzuchi.his.quality.inspection.testfixture.InMemoryRereviewAccessRecords accessRecords =
            new com.tzuchi.his.quality.inspection.testfixture.InMemoryRereviewAccessRecords();

    private ReviewReadAccessPolicy newPolicy() {
        return new ReviewReadAccessPolicy(accessRecords, accessRecords, java.time.Clock.systemUTC());
    }

    private InMemoryReviewSubmissionRepository submissionRepo;
    private InMemoryReviewCaseRepository caseRepo;
    private GetRevisionHistoryUseCase useCase;

    private static final String CASE_ID = "c1";
    private static final String REVIEWER_ID = "reviewer-1";
    private static final String REREVIEWER_ID = "rereviewer-1";
    private static final String HOSPITAL_ID = "H001";
    private static final String SCORE_SHEET_ID = "ss-1";

    @BeforeEach
    void setUp() {
        submissionRepo = new InMemoryReviewSubmissionRepository();
        caseRepo = new InMemoryReviewCaseRepository();
        useCase = new GetRevisionHistoryUseCase(submissionRepo, caseRepo, newPolicy());

        // Default: active (non-CLOSED) case
        ReviewCase rc = ReviewCase.createFromSampling(
                CASE_ID, "b1", "p1", HOSPITAL_ID, "SK-001", MedicalRecordType.OUTPATIENT);
        rc.assignReviewer(REVIEWER_ID, Instant.now(), Instant.now());
        caseRepo.save(rc);
    }

    private ActorContext reviewer() {
        return new ActorContext(REVIEWER_ID, "Test Reviewer",
                EnumSet.of(DomainRole.REVIEWER), Optional.of(HOSPITAL_ID));
    }

    private ActorContext rereviewer() {
        return new ActorContext(REREVIEWER_ID, "Test Rereviewer",
                EnumSet.of(DomainRole.REREVIEWER), Optional.of(HOSPITAL_ID));
    }

    @Test
    void getHistory_reviewerWithIncompleteHistoryIsDenied_D055() {
        // Seed rev-2 first, rev-1 second (to test sorting)
        ReviewSubmission sub1 = ReviewSubmission.createInitialDraft(
                "sub-1", CASE_ID, REVIEWER_ID, SCORE_SHEET_ID, Instant.now());
        sub1.submit(Instant.now());
        ReviewSubmission sub2 = ReviewSubmission.createRevision(
                "sub-2", sub1, Collections.emptyList(), BigDecimal.ZERO, null, Instant.now());
        sub1.markSuperseded("sub-2", Instant.now());

        // Store in reverse order
        submissionRepo.save(sub2);
        submissionRepo.save(sub1);

        accessRecords.incompleteCases.add(CASE_ID); // D-055：歷史不完整 → UNKNOWN
        assertThrows(com.tzuchi.his.quality.inspection.application.common.AccessDeniedException.class,
                () -> useCase.execute(reviewer(), CASE_ID));
        assertFalse(accessRecords.hasRead(CASE_ID, REVIEWER_ID));
    }

    @Test
    void getHistory_reviewerWithCompleteHistoryIsGrantedAndRecorded_D055() {
        // Seed rev-2 first, rev-1 second (to test sorting)
        ReviewSubmission sub1 = ReviewSubmission.createInitialDraft(
                "sub-1", CASE_ID, REVIEWER_ID, SCORE_SHEET_ID, Instant.now());
        sub1.submit(Instant.now());
        ReviewSubmission sub2 = ReviewSubmission.createRevision(
                "sub-2", sub1, Collections.emptyList(), BigDecimal.ZERO, null, Instant.now());
        sub1.markSuperseded("sub-2", Instant.now());

        // Store in reverse order
        submissionRepo.save(sub2);
        submissionRepo.save(sub1);

        // D-055：歷史完整且不在歷史中 → Proven non-historical，准許並記錄讀取者
        assertNotNull(useCase.execute(reviewer(), CASE_ID));
        assertTrue(accessRecords.hasRead(CASE_ID, REVIEWER_ID));
    }

    @Test
    void getHistory_emptyCaseWithIncompleteHistoryIsDenied_D055() {
        accessRecords.incompleteCases.add(CASE_ID); // D-055：歷史不完整 → UNKNOWN
        assertThrows(com.tzuchi.his.quality.inspection.application.common.AccessDeniedException.class,
                () -> useCase.execute(reviewer(), CASE_ID));
        assertFalse(accessRecords.hasRead(CASE_ID, REVIEWER_ID));
    }

    @Test
    void getHistory_emptyCaseWithCompleteHistoryIsGrantedAndRecorded_D055() {
        // D-055：歷史完整且不在歷史中 → Proven non-historical，准許並記錄讀取者
        assertNotNull(useCase.execute(reviewer(), CASE_ID));
        assertTrue(accessRecords.hasRead(CASE_ID, REVIEWER_ID));
    }

    @Test
    void getHistory_wrongRole_throwsUnauthorized() {
        ActorContext noRole = new ActorContext("u1", "NoRole",
                EnumSet.noneOf(DomainRole.class), Optional.of(HOSPITAL_ID));

        assertThrows(RoleDeniedException.class,
                () -> useCase.execute(noRole, CASE_ID));
    }

    @ParameterizedTest
    @ValueSource(strings = {"before", "progress", "submitted", "closed"})
    void initialHistoryRemainsBlindThroughoutRereviewLifecycle(String phase) {
        ReviewCase rc = caseRepo.store.get(CASE_ID);
        rc.startInitialReview("initial", SCORE_SHEET_ID);
        rc.completeInitialReview(new BigDecimal("92"));
        // D-054：先固定初審，才依品質線判斷流向。
        rc.fixInitialReview(rc.getLatestInitialSubmissionId(), "reviewer-1",
                java.time.Instant.parse("2026-01-01T00:00:00Z"), new BigDecimal("92"), new BigDecimal("80"));
        rc.assignRereviewer(REREVIEWER_ID, Instant.now(), Instant.now());
        ReviewSubmission initial = ReviewSubmission.createInitialDraft("initial", CASE_ID, REVIEWER_ID, SCORE_SHEET_ID, Instant.now());
        initial.updateDraft(Collections.emptyList(), new BigDecimal("92"), "private initial comment", Instant.now());
        initial.submit(Instant.now());
        submissionRepo.save(initial);
        if (!phase.equals("before")) {
            rc.startRereview(REREVIEWER_ID);
            ReviewSubmission own = ReviewSubmission.createRereviewDraft("own", CASE_ID, REREVIEWER_ID, "ssv-1", Instant.now());
            own.updateDraft(Collections.emptyList(), new BigDecimal("80"), "own comment", Instant.now());
            if (!phase.equals("progress")) own.submit(Instant.now());
            submissionRepo.save(own);
        }
        if (phase.equals("closed")) { rc.completeRereview(new BigDecimal("80"), new BigDecimal("0.5"), new BigDecimal("0.5")); rc.closeAfterRereview(Instant.now()); }
        RevisionHistoryView result = useCase.execute(rereviewer(), CASE_ID);
        assertNull(result.getEntries().get(0).getTotalScore());
        assertNull(result.getEntries().get(0).getComment());
        assertEquals(new BigDecimal("92"), initial.getTotalScore());
        assertEquals("private initial comment", initial.getComment());
    }

    // --- D-059 第 3 點：revision-history 對複審資料的保護（取代已移除的 rereview-revisions） ---

    private void seedSubmittedRereview() {
        ReviewSubmission rereview = ReviewSubmission.createRereviewDraft("sub-r1", CASE_ID, REREVIEWER_ID, "ssv-1", Instant.now());
        rereview.updateDraft(
                java.util.Arrays.asList(
                        com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem
                                .ofScored("Q001", "false", new BigDecimal("3"))),
                new BigDecimal("3"),
                "複審評語", Instant.now());
        rereview.submit(Instant.now());
        submissionRepo.save(rereview);
    }

    @Test
    void rereviewContent_systemAdminGetsMetadataOnly_D059() {
        seedSubmittedRereview();
        ActorContext admin = new ActorContext("admin-1", "Admin",
                EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.empty());
        com.tzuchi.his.quality.inspection.application.reviewcase.query.RevisionHistoryEntry entry =
                useCase.execute(admin, CASE_ID).getEntries().stream()
                        .filter(e -> "sub-r1".equals(e.getSubmissionId())).findFirst().orElseThrow();
        assertNull(entry.getTotalScore(), "SYSTEM_ADMIN 只應取得 metadata（D-010）");
        assertNull(entry.getComment(), "SYSTEM_ADMIN 只應取得 metadata（D-010）");
    }

    @Test
    void rereviewContent_reviewerNotAssignedToCaseIsDenied_D059() {
        seedSubmittedRereview();
        ActorContext outsider = new ActorContext("reviewer-x", "Other Reviewer",
                EnumSet.of(DomainRole.REVIEWER), Optional.of(HOSPITAL_ID));
        assertThrows(com.tzuchi.his.quality.inspection.application.common.AccessDeniedException.class,
                () -> useCase.execute(outsider, CASE_ID));
    }

    @Test
    void rereviewContent_otherRereviewerIsDenied_D059() {
        seedSubmittedRereview();
        ActorContext other = new ActorContext("rereviewer-2", "Other Rereviewer",
                EnumSet.of(DomainRole.REREVIEWER), Optional.of(HOSPITAL_ID));
        assertThrows(com.tzuchi.his.quality.inspection.application.common.AccessDeniedException.class,
                () -> useCase.execute(other, CASE_ID));
    }

    @Test
    void rereviewContent_multiRoleActorIsDenied_D059() {
        seedSubmittedRereview();
        ActorContext multi = new ActorContext("user-m", "Multi",
                EnumSet.of(DomainRole.REVIEWER, DomainRole.AUDITOR), Optional.of(HOSPITAL_ID));
        assertThrows(com.tzuchi.his.quality.inspection.application.common.AccessDeniedException.class,
                () -> useCase.execute(multi, CASE_ID));
    }
}
