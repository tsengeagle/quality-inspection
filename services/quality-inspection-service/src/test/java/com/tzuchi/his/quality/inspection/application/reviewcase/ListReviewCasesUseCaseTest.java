package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.Page;
import com.tzuchi.his.quality.inspection.application.common.RoleDeniedException;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.ReviewCaseQueryCriteria;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
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

        Page<ReviewCaseSummary> result = useCase.execute(actor, null, null, null, null, null, null, null, 0, 20);

        assertNotNull(result);
        assertEquals(0, result.getContent().size());
    }

    @Test
    void reviewer_canListCases() {
        ActorContext actor = new ActorContext("r1", "Reviewer", EnumSet.of(DomainRole.REVIEWER), Optional.of("H001"));

        Page<ReviewCaseSummary> result = useCase.execute(actor, null, null, null, null, null, null, null, 0, 20);

        assertNotNull(result);
    }

    @Test
    void unknownRole_shouldThrowUnauthorized() {
        ActorContext actor = new ActorContext("u1", "Nobody", EnumSet.noneOf(DomainRole.class), Optional.empty());

        assertThrows(RoleDeniedException.class, () -> useCase.execute(actor, null, null, null, null, null, null, null, 0, 20));
    }

    @Test
    void delegatesToQueryPort() {
        queryPort.setPage(new Page<>(
                Collections.singletonList(new ReviewCaseSummary("c1", "p1", "b1", "H001", "CLOSED", "r1", null, "OUTPATIENT", "SK-1", null, null, null, null)),
                0, 20, 1));
        ActorContext actor = new ActorContext("u1", "Manager", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        Page<ReviewCaseSummary> result = useCase.execute(actor, null, null, null, null, null, null, null, 0, 20);

        assertEquals(1, result.getContent().size());
        assertEquals("c1", result.getContent().get(0).getCaseId());
    }

    @Test
    void nonAdmin_queriesOwnHospital_ignoringRequestedHospital() {
        ActorContext actor = new ActorContext("u1", "Manager", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        useCase.execute(actor, "H002", null, null, null, null, null, null, 0, 20);
        assertEquals("H001", queryPort.lastCriteria.getHospitalId());

        useCase.execute(actor, null, null, null, null, null, null, null, 0, 20);
        assertEquals("H001", queryPort.lastCriteria.getHospitalId());
    }

    @Test
    void systemAdmin_mayQueryAnyHospital() {
        ActorContext actor = new ActorContext("u1", "Admin", EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.empty());

        useCase.execute(actor, "H002", null, null, null, null, null, null, 0, 20);
        assertEquals("H002", queryPort.lastCriteria.getHospitalId());

        useCase.execute(actor, null, null, null, null, null, null, null, 0, 20);
        assertNull(queryPort.lastCriteria.getHospitalId());
    }

    @Test
    void pageAndSize_defaultWhenNotGiven() {
        ActorContext actor = new ActorContext("u1", "Manager", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        useCase.execute(actor, null, null, "b1", "PENDING_INITIAL_REVIEW", null, null, null, null, null);

        assertEquals(0, queryPort.lastCriteria.getPage());
        assertEquals(20, queryPort.lastCriteria.getSize());
        assertEquals("b1", queryPort.lastCriteria.getBatchId());
        assertEquals(CaseStatus.PENDING_INITIAL_REVIEW, queryPort.lastCriteria.getStatus());
    }

    @Test
    void size_isCappedAt100() {
        ActorContext actor = new ActorContext("u1", "Manager", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        useCase.execute(actor, null, null, null, null, null, null, null, 2, 500);

        assertEquals(2, queryPort.lastCriteria.getPage());
        assertEquals(100, queryPort.lastCriteria.getSize());
    }

    // --- InMemory test double ---
    static class InMemoryReviewCaseQueryPort implements ReviewCaseQueryPort {
        private Page<ReviewCaseSummary> page = new Page<>(Collections.emptyList(), 0, 20, 0);

        ReviewCaseQueryCriteria lastCriteria;

        void setPage(Page<ReviewCaseSummary> page) { this.page = page; }

        @Override
        public Page<ReviewCaseSummary> findCases(ReviewCaseQueryCriteria criteria) {
            lastCriteria = criteria;
            return page;
        }

        @Override
        public Optional<com.tzuchi.his.quality.inspection.application.reviewcase.query.ReviewCaseDetailView> findDetailById(String caseId) {
            return Optional.empty();
        }
    }
}
