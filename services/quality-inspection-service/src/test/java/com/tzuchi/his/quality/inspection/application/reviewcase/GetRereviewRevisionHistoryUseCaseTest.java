package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.GetRereviewRevisionHistoryQuery;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewStage;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryReviewSubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GetRereviewRevisionHistoryUseCaseTest {

    private InMemoryReviewSubmissionRepository submissionRepo;
    private GetRereviewRevisionHistoryUseCase useCase;

    private static final String CASE_ID = "c1";
    private static final String REVIEWER_ID = "reviewer-1";
    private static final String REREVIEWER_ID = "rereviewer-1";
    private static final String HOSPITAL_ID = "H001";
    private static final String SCORE_SHEET_ID = "ss-1";

    @BeforeEach
    void setUp() {
        submissionRepo = new InMemoryReviewSubmissionRepository();
        useCase = new GetRereviewRevisionHistoryUseCase(submissionRepo);
    }

    private ActorContext hospitalManagerActor() {
        return new ActorContext("manager-1", "Hospital Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of(HOSPITAL_ID));
    }

    @Test
    void execute_returnsOnlyRereviewSubmissions() {
        // Seed initial review submission
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

        GetRereviewRevisionHistoryQuery query = new GetRereviewRevisionHistoryQuery();
        query.setCaseId(CASE_ID);

        List<ReviewSubmission> result = useCase.execute(hospitalManagerActor(), query);

        assertEquals(1, result.size());
        assertEquals("sub-r1", result.get(0).getSubmissionId());
        assertEquals(ReviewStage.REREVIEW, result.get(0).getStage());
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

        GetRereviewRevisionHistoryQuery query = new GetRereviewRevisionHistoryQuery();
        query.setCaseId(CASE_ID);

        List<ReviewSubmission> result = useCase.execute(hospitalManagerActor(), query);

        assertEquals(2, result.size());
        assertEquals(1, result.get(0).getRevisionNo());
        assertEquals(2, result.get(1).getRevisionNo());
    }

    @Test
    void execute_emptyCaseReturnsEmptyList() {
        GetRereviewRevisionHistoryQuery query = new GetRereviewRevisionHistoryQuery();
        query.setCaseId("nonexistent");

        List<ReviewSubmission> result = useCase.execute(hospitalManagerActor(), query);

        assertTrue(result.isEmpty());
    }
}
