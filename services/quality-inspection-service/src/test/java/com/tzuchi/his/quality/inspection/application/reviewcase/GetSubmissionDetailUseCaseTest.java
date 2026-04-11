package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.UnauthorizedException;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.SubmissionDetailView;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.SubmissionQueryPort;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GetSubmissionDetailUseCaseTest {

    private InMemorySubmissionQueryPort queryPort;
    private GetSubmissionDetailUseCase useCase;

    @BeforeEach
    void setUp() {
        queryPort = new InMemorySubmissionQueryPort();
        useCase = new GetSubmissionDetailUseCase(queryPort);
    }

    @Test
    void returnsDetailWhenFound() {
        SubmissionDetailView detail = new SubmissionDetailView(
                "sub1", "c1", "INITIAL", 1, "SUBMITTED", "r1",
                Collections.emptyList(), "ok", null, BigDecimal.valueOf(90), null, null);
        queryPort.setDetail("sub1", detail);
        ActorContext actor = new ActorContext("u1", "Manager", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        SubmissionDetailView result = useCase.execute(actor, "sub1");

        assertEquals("sub1", result.getSubmissionId());
    }

    @Test
    void throwsNotFoundWhenMissing() {
        ActorContext actor = new ActorContext("u1", "Manager", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));
        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(actor, "missing"));
    }

    @Test
    void unknownRole_shouldThrowUnauthorized() {
        ActorContext actor = new ActorContext("u1", "Nobody", EnumSet.noneOf(DomainRole.class), Optional.empty());
        assertThrows(UnauthorizedException.class, () -> useCase.execute(actor, "sub1"));
    }

    static class InMemorySubmissionQueryPort implements SubmissionQueryPort {
        private final Map<String, SubmissionDetailView> map = new HashMap<>();
        void setDetail(String id, SubmissionDetailView v) { map.put(id, v); }

        @Override
        public Optional<SubmissionDetailView> findDetailById(String submissionId) {
            return Optional.ofNullable(map.get(submissionId));
        }
    }
}
