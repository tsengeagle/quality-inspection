package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.RoleDeniedException;
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
        GetReviewCaseDetailUseCaseTest.InMemoryQueryPort casePort = new GetReviewCaseDetailUseCaseTest.InMemoryQueryPort();
        for (int i = 1; i <= 3; i++) {
            casePort.setDetail("c" + i, new com.tzuchi.his.quality.inspection.application.reviewcase.query.ReviewCaseDetailView(
                    "c" + i, "p", "b", "H001", "PENDING_REREVIEW", "r" + i, i == 1 ? "assigned-rereviewer" : "u" + i,
                    null, null, null, null, null, null, null, null, null, null,
                    Collections.emptyList(), null, null, null, false));
        }
        useCase = new GetSubmissionDetailUseCase(queryPort, casePort, new ReviewReadAccessPolicy(org.mockito.Mockito.mock(com.tzuchi.his.quality.inspection.application.audit.AuditTrailQueryPort.class), new com.fasterxml.jackson.databind.ObjectMapper()));
    }

    @Test
    void managerWithUnknownHistoryIsDenied_D035() {
        SubmissionDetailView detail = new SubmissionDetailView(
                "sub1", "c1", "INITIAL", 1, "SUBMITTED", "r1",
                Collections.emptyList(), "ok", null, BigDecimal.valueOf(90), null, null, null);
        queryPort.setDetail("sub1", detail);
        ActorContext actor = new ActorContext("u1", "Manager", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        assertThrows(com.tzuchi.his.quality.inspection.application.common.AccessDeniedException.class,
                () -> useCase.execute(actor, "sub1"));
    }

    @Test
    void throwsNotFoundWhenMissing() {
        ActorContext actor = new ActorContext("u1", "Manager", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));
        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(actor, "missing"));
    }

    @Test
    void unknownRole_shouldThrowUnauthorized() {
        ActorContext actor = new ActorContext("u1", "Nobody", EnumSet.noneOf(DomainRole.class), Optional.empty());
        assertThrows(RoleDeniedException.class, () -> useCase.execute(actor, "sub1"));
    }

    @Test
    void execute_rerereviewerBeforeRereviewSubmitted_shouldMaskSensitiveFields() {
        SubmissionDetailView detail = new SubmissionDetailView(
                "sub2", "c2", "INITIAL", 1, "SUBMITTED", "r2",
                Collections.emptyList(), "some comment", null, BigDecimal.valueOf(80), null, null, null);
        queryPort.setDetail("sub2", detail);
        queryPort.setSubmittedRereviewExists("c2", false);

        ActorContext actor = new ActorContext("u2", "Rereviewer", EnumSet.of(DomainRole.REREVIEWER), Optional.of("H001"));

        SubmissionDetailView result = useCase.execute(actor, "sub2");

        assertNull(result.getAnswers());
        assertNull(result.getComment());
        assertNull(result.getTotalScore());
        assertNotNull(result.getMaskedFields());
        assertTrue(result.getMaskedFields().contains("answers"));
        assertTrue(result.getMaskedFields().contains("comment"));
        assertTrue(result.getMaskedFields().contains("totalScore"));
    }

    @Test
    void execute_rerereviewerAfterRereviewSubmitted_shouldStillMask() {
        SubmissionDetailView detail = new SubmissionDetailView(
                "sub3", "c3", "INITIAL", 1, "SUBMITTED", "r3",
                Collections.emptyList(), "some comment", null, BigDecimal.valueOf(70), null, null, null);
        queryPort.setDetail("sub3", detail);
        queryPort.setSubmittedRereviewExists("c3", true);

        ActorContext actor = new ActorContext("u3", "Rereviewer", EnumSet.of(DomainRole.REREVIEWER), Optional.of("H001"));

        SubmissionDetailView result = useCase.execute(actor, "sub3");

        assertNull(result.getComment());
        assertNull(result.getTotalScore());
        assertNotNull(result.getMaskedFields());
    }

    static class InMemorySubmissionQueryPort implements SubmissionQueryPort {
        private final Map<String, SubmissionDetailView> map = new HashMap<>();
        private final Map<String, Boolean> submittedRereviewMap = new HashMap<>();

        void setDetail(String id, SubmissionDetailView v) { map.put(id, v); }
        void setSubmittedRereviewExists(String caseId, boolean exists) {
            submittedRereviewMap.put(caseId, exists);
        }

        @Override
        public Optional<SubmissionDetailView> findDetailById(String submissionId) {
            return Optional.ofNullable(map.get(submissionId));
        }

        @Override
        public boolean existsSubmittedRereview(String caseId) {
            return submittedRereviewMap.getOrDefault(caseId, false);
        }
    }
}
