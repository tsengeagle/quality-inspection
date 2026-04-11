package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.Page;
import com.tzuchi.his.quality.inspection.application.common.UnauthorizedException;
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
        useCase = new GetReviewCaseDetailUseCase(queryPort);
    }

    @Test
    void returnsDetailWhenFound() {
        ReviewCaseDetailView detail = new ReviewCaseDetailView(
                "c1", "p1", "b1", "H001", "CLOSED", "r1", null,
                "OUTPATIENT", "SK-1", BigDecimal.valueOf(85), BigDecimal.valueOf(85),
                "PASS", null, null, null, null, null, Collections.emptyList());
        queryPort.setDetail("c1", detail);
        ActorContext actor = new ActorContext("u1", "Manager", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        ReviewCaseDetailView result = useCase.execute(actor, "c1");

        assertEquals("c1", result.getCaseId());
        assertEquals("CLOSED", result.getStatus());
    }

    @Test
    void throwsNotFoundWhenMissing() {
        ActorContext actor = new ActorContext("u1", "Manager", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(actor, "c1"));
    }

    @Test
    void unknownRole_shouldThrowUnauthorized() {
        ActorContext actor = new ActorContext("u1", "Nobody", EnumSet.noneOf(DomainRole.class), Optional.empty());

        assertThrows(UnauthorizedException.class, () -> useCase.execute(actor, "c1"));
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
