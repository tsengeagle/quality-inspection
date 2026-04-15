package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.Page;
import com.tzuchi.his.quality.inspection.application.common.RoleDeniedException;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.ReviewCaseQueryCriteria;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.ReviewCaseQueryPort;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.ReviewCaseSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ListReviewCasesUseCaseTest {

    private InMemoryReviewCaseQueryPort queryPort;
    private ListReviewCasesUseCase useCase;

    @BeforeEach
    void setUp() {
        queryPort = new InMemoryReviewCaseQueryPort();
        useCase = new ListReviewCasesUseCase(queryPort);
    }

    @Test
    void hospitalManager_canListCases() {
        ActorContext actor = new ActorContext("u1", "Manager", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));
        ReviewCaseQueryCriteria criteria = new ReviewCaseQueryCriteria(null, null, null, null, null, null, null, 0, 20);

        Page<ReviewCaseSummary> result = useCase.execute(actor, criteria);

        assertNotNull(result);
        assertEquals(0, result.getContent().size());
    }

    @Test
    void reviewer_canListCases() {
        ActorContext actor = new ActorContext("r1", "Reviewer", EnumSet.of(DomainRole.REVIEWER), Optional.of("H001"));
        ReviewCaseQueryCriteria criteria = new ReviewCaseQueryCriteria(null, null, null, null, null, null, null, 0, 20);

        Page<ReviewCaseSummary> result = useCase.execute(actor, criteria);

        assertNotNull(result);
    }

    @Test
    void unknownRole_shouldThrowUnauthorized() {
        ActorContext actor = new ActorContext("u1", "Nobody", EnumSet.noneOf(DomainRole.class), Optional.empty());
        ReviewCaseQueryCriteria criteria = new ReviewCaseQueryCriteria(null, null, null, null, null, null, null, 0, 20);

        assertThrows(RoleDeniedException.class, () -> useCase.execute(actor, criteria));
    }

    @Test
    void delegatesToQueryPort() {
        queryPort.setPage(new Page<>(
                Collections.singletonList(new ReviewCaseSummary("c1", "p1", "b1", "H001", "CLOSED", "r1", null, "OUTPATIENT", "SK-1", null, null, null, null)),
                0, 20, 1));
        ActorContext actor = new ActorContext("u1", "Manager", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));
        ReviewCaseQueryCriteria criteria = new ReviewCaseQueryCriteria(null, null, null, null, null, null, null, 0, 20);

        Page<ReviewCaseSummary> result = useCase.execute(actor, criteria);

        assertEquals(1, result.getContent().size());
        assertEquals("c1", result.getContent().get(0).getCaseId());
    }

    // --- InMemory test double ---
    static class InMemoryReviewCaseQueryPort implements ReviewCaseQueryPort {
        private Page<ReviewCaseSummary> page = new Page<>(Collections.emptyList(), 0, 20, 0);

        void setPage(Page<ReviewCaseSummary> page) { this.page = page; }

        @Override
        public Page<ReviewCaseSummary> findCases(ReviewCaseQueryCriteria criteria) {
            return page;
        }

        @Override
        public Optional<com.tzuchi.his.quality.inspection.application.reviewcase.query.ReviewCaseDetailView> findDetailById(String caseId) {
            return Optional.empty();
        }
    }
}
