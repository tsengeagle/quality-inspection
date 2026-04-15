package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.GetRereviewRevisionHistoryQuery;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.RevisionHistoryView;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewStage;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryReviewCaseRepository;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryReviewSubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GetRereviewRevisionHistoryUseCaseTest {

    private InMemoryReviewSubmissionRepository submissionRepo;
    private InMemoryReviewCaseRepository caseRepo;
    private GetRereviewRevisionHistoryUseCase useCase;

    private static final String CASE_ID = "c1";
    private static final String REVIEWER_ID = "reviewer-1";
    private static final String REREVIEWER_ID = "rereviewer-1";
    private static final String HOSPITAL_ID = "H001";
    private static final String SCORE_SHEET_ID = "ss-1";

    @BeforeEach
    void setUp() {
        submissionRepo = new InMemoryReviewSubmissionRepository();
        caseRepo = new InMemoryReviewCaseRepository();
        useCase = new GetRereviewRevisionHistoryUseCase(submissionRepo, caseRepo);

        ReviewCase rc = ReviewCase.createFromSampling(
                CASE_ID, "b1", "p1", HOSPITAL_ID, "SK-001", MedicalRecordType.OUTPATIENT);
        caseRepo.save(rc);
    }

    private ActorContext hospitalManagerActor() {
        return new ActorContext("manager-1", "Hospital Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of(HOSPITAL_ID));
    }

    private GetRereviewRevisionHistoryQuery queryFor(String caseId) {
        GetRereviewRevisionHistoryQuery q = new GetRereviewRevisionHistoryQuery();
        q.setCaseId(caseId);
        return q;
    }

    @Test
    void execute_returnsOnlyRereviewSubmissions() {
        // Seed initial review submission — should be filtered out
        ReviewSubmission initial = ReviewSubmission.createInitialDraft(
                "sub-1", CASE_ID, REVIEWER_ID, SCORE_SHEET_ID);
        initial.updateDraft(
                Arrays.asList(AnswerItem.ofScored("Q001", "true", new BigDecimal("10"))),
                new BigDecimal("10"),
                null
        );
        initial.submit();
        submissionRepo.save(initial);

        // Seed rereview submission
        ReviewSubmission rereview = ReviewSubmission.createRereviewDraft(
                "sub-r1", CASE_ID, REREVIEWER_ID);
        rereview.updateDraft(
                Arrays.asList(AnswerItem.ofScored("Q001", "false", new BigDecimal("0"))),
                new BigDecimal("0"),
                "rereview comment"
        );
        rereview.submit();
        submissionRepo.save(rereview);

        RevisionHistoryView view = useCase.execute(hospitalManagerActor(), queryFor(CASE_ID));

        assertEquals(1, view.getEntries().size());
        assertEquals("sub-r1", view.getEntries().get(0).getSubmissionId());
        assertEquals(ReviewStage.REREVIEW.name(), view.getEntries().get(0).getStage());
    }

    @Test
    void execute_returnsRevisionsInOrder() {
        // Seed submitted rereview
        ReviewSubmission rereview1 = ReviewSubmission.createRereviewDraft(
                "sub-r1", CASE_ID, REREVIEWER_ID);
        rereview1.updateDraft(
                Arrays.asList(AnswerItem.ofScored("Q001", "true", new BigDecimal("10"))),
                new BigDecimal("10"),
                "comment"
        );
        rereview1.submit();
        submissionRepo.save(rereview1);

        // Seed revision
        ReviewSubmission revision = ReviewSubmission.createRevision(
                "sub-r2", rereview1,
                Arrays.asList(AnswerItem.ofScored("Q001", "false", new BigDecimal("0"))),
                new BigDecimal("0"),
                "revised comment"
        );
        rereview1.markSuperseded("sub-r2");
        submissionRepo.save(rereview1);
        submissionRepo.save(revision);

        RevisionHistoryView view = useCase.execute(hospitalManagerActor(), queryFor(CASE_ID));

        assertEquals(2, view.getEntries().size());
        assertEquals(1, view.getEntries().get(0).getRevisionNo());
        assertEquals(2, view.getEntries().get(1).getRevisionNo());
    }

    @Test
    void execute_emptyCaseReturnsEmptyEntries() {
        RevisionHistoryView view = useCase.execute(hospitalManagerActor(), queryFor(CASE_ID));

        assertTrue(view.getEntries().isEmpty());
        assertFalse(view.isHasExceptionEntry());
    }
}
