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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GetRevisionHistoryUseCaseTest {

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
        useCase = new GetRevisionHistoryUseCase(submissionRepo, caseRepo);

        // Default: active (non-CLOSED) case
        ReviewCase rc = ReviewCase.createFromSampling(
                CASE_ID, "b1", "p1", HOSPITAL_ID, "SK-001", MedicalRecordType.OUTPATIENT);
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
    void getHistory_returnsAllSubmissions_sortedByRevisionNo() {
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

        RevisionHistoryView view = useCase.execute(reviewer(), CASE_ID);

        assertEquals(CASE_ID, view.getCaseId());
        assertEquals(2, view.getEntries().size());
        assertEquals(1, view.getEntries().get(0).getRevisionNo());
        assertEquals(2, view.getEntries().get(1).getRevisionNo());
    }

    @Test
    void getHistory_emptyCase_returnsEmptyEntries() {
        RevisionHistoryView view = useCase.execute(reviewer(), CASE_ID);

        assertEquals(CASE_ID, view.getCaseId());
        assertTrue(view.getEntries().isEmpty());
        assertFalse(view.isHasExceptionEntry());
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
        ReviewSubmission sub = ReviewSubmission.createInitialDraft(
                "sub-1", CASE_ID, REREVIEWER_ID, SCORE_SHEET_ID);
        sub.submit();
        submissionRepo.save(sub);

        RevisionHistoryView view = useCase.execute(rereviewer(), CASE_ID);

        assertFalse(view.isHasExceptionEntry());
    }
}
