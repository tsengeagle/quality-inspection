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
import java.time.LocalDateTime;

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
        rc.assignReviewer(REVIEWER_ID, LocalDateTime.now());
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
                "sub-1", CASE_ID, REVIEWER_ID, SCORE_SHEET_ID);
        sub1.submit();
        ReviewSubmission sub2 = ReviewSubmission.createRevision(
                "sub-2", sub1, Collections.emptyList(), BigDecimal.ZERO, null);
        sub1.markSuperseded("sub-2");

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
                "sub-1", CASE_ID, REVIEWER_ID, SCORE_SHEET_ID);
        sub1.submit();
        ReviewSubmission sub2 = ReviewSubmission.createRevision(
                "sub-2", sub1, Collections.emptyList(), BigDecimal.ZERO, null);
        sub1.markSuperseded("sub-2");

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

    @Test
    void getHistory_hasExceptionEntry_falseWhenCaseNotClosed() {
        // Case is PENDING (not CLOSED) — hasExceptionEntry must be false
        ReviewSubmission sub = ReviewSubmission.createRereviewDraft(
                "sub-1", CASE_ID, REREVIEWER_ID);
        sub.submit();
        submissionRepo.save(sub);

        RevisionHistoryView view = useCase.execute(rereviewer(), CASE_ID);

        assertFalse(view.isHasExceptionEntry());
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
        rc.assignRereviewer(REREVIEWER_ID, LocalDateTime.now());
        ReviewSubmission initial = ReviewSubmission.createInitialDraft("initial", CASE_ID, REVIEWER_ID, SCORE_SHEET_ID);
        initial.updateDraft(Collections.emptyList(), new BigDecimal("92"), "private initial comment");
        initial.submit();
        submissionRepo.save(initial);
        if (!phase.equals("before")) {
            rc.startRereview(REREVIEWER_ID);
            ReviewSubmission own = ReviewSubmission.createRereviewDraft("own", CASE_ID, REREVIEWER_ID);
            own.updateDraft(Collections.emptyList(), new BigDecimal("80"), "own comment");
            if (!phase.equals("progress")) own.submit();
            submissionRepo.save(own);
        }
        if (phase.equals("closed")) { rc.completeRereview(new BigDecimal("80"), new BigDecimal("0.5"), new BigDecimal("0.5")); rc.closeAfterRereview(LocalDateTime.now()); }
        RevisionHistoryView result = useCase.execute(rereviewer(), CASE_ID);
        assertNull(result.getEntries().get(0).getTotalScore());
        assertNull(result.getEntries().get(0).getComment());
        assertEquals(new BigDecimal("92"), initial.getTotalScore());
        assertEquals("private initial comment", initial.getComment());
    }
}
