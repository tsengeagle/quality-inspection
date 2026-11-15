package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.Page;
import com.tzuchi.his.quality.inspection.application.common.RoleDeniedException;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.*;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GetReviewCaseDetailUseCaseTest {

    private InMemoryQueryPort queryPort;
    private GetReviewCaseDetailUseCase useCase;

    @BeforeEach
    void setUp() {
        queryPort = new InMemoryQueryPort();
        useCase = new GetReviewCaseDetailUseCase(queryPort, new ReviewReadAccessPolicy(org.mockito.Mockito.mock(com.tzuchi.his.quality.inspection.application.audit.AuditTrailQueryPort.class), new com.fasterxml.jackson.databind.ObjectMapper()),
                new CaseDetailMetadataMapper(org.mockito.Mockito.mock(com.tzuchi.his.quality.inspection.application.project.query.ReviewProjectQueryPort.class)));
    }

    @Test
    void managerWithUnknownHistoryIsDenied_D035() {
        ReviewCaseDetailView detail = new ReviewCaseDetailView(
                "c1", "p1", "b1", "H001", "CLOSED", "r1", null,
                "OUTPATIENT", "SK-1", BigDecimal.valueOf(85), BigDecimal.valueOf(85),
                "PASS", null, null, null, null, null, Collections.emptyList(), null,
                null, null, false);
        queryPort.setDetail("c1", detail);
        ActorContext actor = new ActorContext("u1", "Manager", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        assertThrows(com.tzuchi.his.quality.inspection.application.common.AccessDeniedException.class,
                () -> useCase.execute(actor, "c1"));
    }

    @Test
    void throwsNotFoundWhenMissing() {
        ActorContext actor = new ActorContext("u1", "Manager", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(actor, "c1"));
    }

    @Test
    void unknownRole_shouldThrowUnauthorized() {
        ActorContext actor = new ActorContext("u1", "Nobody", EnumSet.noneOf(DomainRole.class), Optional.empty());

        assertThrows(RoleDeniedException.class, () -> useCase.execute(actor, "c1"));
    }

        @Test
        void assignedReviewerWithUnknownHistoryIsDenied_D035() {
        ReviewCaseDetailView detail = new ReviewCaseDetailView(
            "c1", "p1", "b1", "H001", "INITIAL_REVIEW_IN_PROGRESS",
            "reviewer-1", null, "OUTPATIENT", "SK-1",
            null, null, null,
            null, null, null, null, null,
            "sub-init-draft", "ss-init-v1",
            List.of(new SubmissionSummaryItem(
                "sub-init-draft", "INITIAL", "reviewer-1", 1,
                "DRAFT", BigDecimal.ZERO, null)),
            null, null, null, false, null, null);
        queryPort.setDetail("c1", detail);
        ActorContext actor = new ActorContext(
            "reviewer-1", "Reviewer", EnumSet.of(DomainRole.REVIEWER), Optional.of("H001"));

        assertThrows(com.tzuchi.his.quality.inspection.application.common.AccessDeniedException.class,
                () -> useCase.execute(actor, "c1"));
        }

        @Test
        void assignedRereviewer_closedCase_shouldBeReadonlyWithoutRevision_D032() {
        ReviewCaseDetailView detail = new ReviewCaseDetailView(
            "c1", "p1", "b1", "H001", "CLOSED",
            "reviewer-1", "rereviewer-1", "OUTPATIENT", "SK-1",
            BigDecimal.valueOf(82), BigDecimal.valueOf(85), "FINAL_PASS",
            null, null, null, null, null,
            "sub-init-1", "ss-rereview-v1",
            List.of(
                new SubmissionSummaryItem(
                    "sub-init-1", "INITIAL", "reviewer-1", 1,
                    "SUBMITTED", BigDecimal.valueOf(82), null),
                new SubmissionSummaryItem(
                    "sub-rereview-1", "REREVIEW", "rereviewer-1", 1,
                    "SUBMITTED", BigDecimal.valueOf(88), null)),
            "sub-rereview-1", null, null, false, null, null);
        queryPort.setDetail("c1", detail);
        ActorContext actor = new ActorContext(
            "rereviewer-1", "ReReviewer", EnumSet.of(DomainRole.REREVIEWER), Optional.of("H001"));

        ReviewCaseDetailView result = useCase.execute(actor, "c1");

        assertNotNull(result.getRereviewContext());
        // D-032：CLOSED 之後沒有例外修正；結案後頁面唯讀（D-053 第 8 點）
        assertEquals("READONLY_BLOCKED", result.getRereviewContext().getPageMode());
        assertFalse(result.getRereviewContext().isClosedRevisionException());
        assertEquals("CASE_ALREADY_CLOSED", result.getRereviewContext().getBlockedReason());
        assertEquals("sub-rereview-1",
            result.getRereviewContext().getBasedOnVersion().getSubmissionId());
        assertEquals("sub-init-1", result.getRereviewContext().getInitialSubmissionId());
        assertEquals("reviewer-1", result.getRereviewContext().getInitialReviewerId());
        assertNull(result.getRereviewContext().getFinalScore());
        assertNull(result.getRereviewContext().getInitialTotalScore());
        assertNull(result.getInitialReviewScore());
        assertNull(result.getFinalScore());
        assertEquals(List.of(), result.getRereviewContext().getAllowedActions());
        }

        @Test
        void submitter_submittedNotClosed_shouldAllowReviseAndClose_D053() {
        ReviewCaseDetailView detail = new ReviewCaseDetailView(
            "c1", "p1", "b1", "H001", "REREVIEW_IN_PROGRESS",
            "reviewer-1", "rereviewer-1", "OUTPATIENT", "SK-1",
            BigDecimal.valueOf(82), BigDecimal.valueOf(85), null,
            null, null, null, null, null,
            "sub-init-1", "ss-rereview-v1",
            List.of(
                new SubmissionSummaryItem(
                    "sub-init-1", "INITIAL", "reviewer-1", 1,
                    "SUBMITTED", BigDecimal.valueOf(82), null),
                new SubmissionSummaryItem(
                    "sub-rereview-1", "REREVIEW", "rereviewer-1", 1,
                    "SUBMITTED", BigDecimal.valueOf(88), null)),
            "sub-rereview-1", null, null, false, null, null);
        queryPort.setDetail("c1", detail);
        ActorContext actor = new ActorContext(
            "rereviewer-1", "ReReviewer", EnumSet.of(DomainRole.REREVIEWER), Optional.of("H001"));

        ReviewCaseDetailView result = useCase.execute(actor, "c1");

        assertEquals("REVISION", result.getRereviewContext().getPageMode());
        assertEquals(List.of("REVISE_REREVIEW", "CLOSE_REREVIEW"), result.getRereviewContext().getAllowedActions());
        assertEquals("sub-rereview-1", result.getRereviewContext().getBasedOnVersion().getSubmissionId());
        assertNull(result.getRereviewContext().getFinalScore());
        assertNull(result.getFinalScore());
        }

    static class InMemoryQueryPort implements ReviewCaseQueryPort {
        private final Map<String, ReviewCaseDetailView> details = new HashMap<>();

        void setDetail(String caseId, ReviewCaseDetailView view) { details.put(caseId, view); }

        @Override
        public Page<ReviewCaseSummary> findCases(ReviewCaseQueryCriteria criteria) {
            return new Page<>(Collections.emptyList(), 0, 20, 0);
        }

        @Override
        public Optional<ReviewCaseDetailView> findDetailById(String caseId) {
            return Optional.ofNullable(details.get(caseId));
        }
    }
}
