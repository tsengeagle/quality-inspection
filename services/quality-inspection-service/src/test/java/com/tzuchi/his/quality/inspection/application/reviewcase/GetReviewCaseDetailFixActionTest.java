package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.InitialReviewBootstrapView;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.ReviewCaseDetailView;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.ReviewCaseQueryPort;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.SubmissionSummaryItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * D-054 第 9 點：初審頁的 allowedActions 何時帶 FIX_INITIAL_REVIEW。
 * 讀取授權（D-035）與本測試無關，以 mock 的存取政策略過；目前 REVIEWER 讀案件詳情仍被 D-035 拒絕。
 */
class GetReviewCaseDetailFixActionTest {

    private ReviewCaseQueryPort queryPort;
    private GetReviewCaseDetailUseCase useCase;

    @BeforeEach
    void setUp() {
        queryPort = mock(ReviewCaseQueryPort.class);
        useCase = new GetReviewCaseDetailUseCase(queryPort, mock(ReviewReadAccessPolicy.class),
                mock(CaseDetailMetadataMapper.class));
    }

    private static ActorContext doctor() {
        return new ActorContext("doctor", "Doctor", EnumSet.of(DomainRole.REVIEWER), Optional.of("H001"));
    }

    private void caseWith(String status, String latestInitial, boolean fixed, SubmissionSummaryItem... submissions) {
        ReviewCaseDetailView view = new ReviewCaseDetailView(
                "c1", "p1", "b1", "H001", status, "doctor", null, "OUTPATIENT", "SK-1",
                new BigDecimal("7"), new BigDecimal("7"), null, null, null, null, null, null,
                latestInitial, "ss:v1", List.of(submissions), null, null, null, false, null, null)
                .withInitialReviewFixed(fixed);
        when(queryPort.findDetailById("c1")).thenReturn(Optional.of(view));
    }

    private static SubmissionSummaryItem initial(String id, String status) {
        return new SubmissionSummaryItem(id, "INITIAL", "doctor", 1, status, new BigDecimal("7"), null);
    }

    private InitialReviewBootstrapView context() {
        return useCase.execute(doctor(), "c1").getInitialReviewContext();
    }

    @Test
    void submittedAndUnfixed_offersFixAlongsideRevise() {
        caseWith("INITIAL_REVIEW_COMPLETED", "i1", false, initial("i1", "SUBMITTED"));

        assertEquals("REVISION", context().getPageMode());
        assertEquals(List.of("REVISE_INITIAL_REVIEW", "FIX_INITIAL_REVIEW"), context().getAllowedActions());
    }

    @Test
    void fixed_isReadOnlyWithNeitherReviseNorFix() {
        caseWith("INITIAL_REVIEW_COMPLETED", "i1", true, initial("i1", "SUBMITTED"));

        assertEquals("READONLY_BLOCKED", context().getPageMode());
        assertEquals("INITIAL_REVIEW_FIXED", context().getBlockedReason());
        assertTrue(context().getAllowedActions().isEmpty());
    }

    @Test
    void ownSubmissionThatIsNoLongerCurrent_doesNotOfferFix() {
        caseWith("INITIAL_REVIEW_COMPLETED", "someone-elses", false, initial("i1", "SUBMITTED"));

        assertFalse(context().getAllowedActions().contains("FIX_INITIAL_REVIEW"));
    }
}
