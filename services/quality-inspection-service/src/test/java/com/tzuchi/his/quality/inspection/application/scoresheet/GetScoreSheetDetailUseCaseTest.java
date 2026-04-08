package com.tzuchi.his.quality.inspection.application.scoresheet;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.HospitalIdResolver;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetStatus;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GetScoreSheetDetailUseCaseTest {

    private final ScoreSheetSummary summary = new ScoreSheetSummary("ss1", "p1",
            ScoreSheetStatus.DRAFT, 1, 2, 5);

    @Test
    void execute_hm_shouldReturn() {
        ScoreSheetQueryPort queryPort = stubQueryPortWithSummary(summary);
        HospitalIdResolver resolver = projectId -> "H001";
        GetScoreSheetDetailUseCase useCase = new GetScoreSheetDetailUseCase(queryPort, resolver);
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        ScoreSheetSummary result = useCase.execute(actor, "ss1");

        assertEquals("ss1", result.getScoreSheetId());
    }

    @Test
    void execute_sa_shouldReturn() {
        ScoreSheetQueryPort queryPort = stubQueryPortWithSummary(summary);
        HospitalIdResolver resolver = projectId -> "H001";
        GetScoreSheetDetailUseCase useCase = new GetScoreSheetDetailUseCase(queryPort, resolver);
        ActorContext actor = new ActorContext("u1", "Admin",
                EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.empty());

        ScoreSheetSummary result = useCase.execute(actor, "ss1");

        assertEquals("ss1", result.getScoreSheetId());
    }

    @Test
    void execute_hm_wrongHospital_shouldThrow() {
        ScoreSheetQueryPort queryPort = stubQueryPortWithSummary(summary);
        HospitalIdResolver resolver = projectId -> "H001";
        GetScoreSheetDetailUseCase useCase = new GetScoreSheetDetailUseCase(queryPort, resolver);
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H999"));

        assertThrows(AccessDeniedException.class, () -> useCase.execute(actor, "ss1"));
    }

    @Test
    void execute_notFound_shouldThrow404() {
        ScoreSheetQueryPort queryPort = stubQueryPortEmpty();
        HospitalIdResolver resolver = projectId -> "H001";
        GetScoreSheetDetailUseCase useCase = new GetScoreSheetDetailUseCase(queryPort, resolver);
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(actor, "ss1"));
    }

    private ScoreSheetQueryPort stubQueryPortWithSummary(ScoreSheetSummary s) {
        return new ScoreSheetQueryPort() {
            @Override
            public List<ScoreSheetSummary> findByHospitalId(String hospitalId) { return List.of(); }
            @Override
            public List<ScoreSheetSummary> findAll() { return List.of(); }
            @Override
            public Optional<ScoreSheetSummary> findSummaryById(String id) {
                return id.equals(s.getScoreSheetId()) ? Optional.of(s) : Optional.empty();
            }
        };
    }

    private ScoreSheetQueryPort stubQueryPortEmpty() {
        return new ScoreSheetQueryPort() {
            @Override
            public List<ScoreSheetSummary> findByHospitalId(String hospitalId) { return List.of(); }
            @Override
            public List<ScoreSheetSummary> findAll() { return List.of(); }
            @Override
            public Optional<ScoreSheetSummary> findSummaryById(String id) { return Optional.empty(); }
        };
    }
}
