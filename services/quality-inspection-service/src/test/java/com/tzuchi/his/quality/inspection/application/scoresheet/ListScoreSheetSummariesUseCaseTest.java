package com.tzuchi.his.quality.inspection.application.scoresheet;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.RoleDeniedException;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetStatus;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ListScoreSheetSummariesUseCaseTest {

    private final ScoreSheetSummary summary1 = new ScoreSheetSummary("ss1", "p1",
            ScoreSheetStatus.DRAFT, 1, 2, 5);
    private final ScoreSheetSummary summary2 = new ScoreSheetSummary("ss2", "p2",
            ScoreSheetStatus.PUBLISHED, 1, 1, 3);

    @Test
    void execute_hm_shouldReturnOwnHospital() {
        ScoreSheetQueryPort queryPort = stubQueryPort(List.of(summary1), List.of(summary1, summary2));
        ListScoreSheetSummariesUseCase useCase = new ListScoreSheetSummariesUseCase(queryPort);
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        List<ScoreSheetSummary> result = useCase.execute(actor);

        assertEquals(1, result.size());
        assertEquals("ss1", result.get(0).getScoreSheetId());
    }

    @Test
    void execute_sa_shouldReturnAll() {
        ScoreSheetQueryPort queryPort = stubQueryPort(List.of(summary1), List.of(summary1, summary2));
        ListScoreSheetSummariesUseCase useCase = new ListScoreSheetSummariesUseCase(queryPort);
        ActorContext actor = new ActorContext("u1", "Admin",
                EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.empty());

        List<ScoreSheetSummary> result = useCase.execute(actor);

        assertEquals(2, result.size());
    }

    @Test
    void execute_noRole_shouldThrow() {
        ScoreSheetQueryPort queryPort = stubQueryPort(List.of(), List.of());
        ListScoreSheetSummariesUseCase useCase = new ListScoreSheetSummariesUseCase(queryPort);
        ActorContext actor = new ActorContext("u1", "Reviewer",
                EnumSet.of(DomainRole.REVIEWER), Optional.of("H001"));

        assertThrows(RoleDeniedException.class, () -> useCase.execute(actor));
    }

    private ScoreSheetQueryPort stubQueryPort(List<ScoreSheetSummary> byHospital,
                                               List<ScoreSheetSummary> all) {
        return new ScoreSheetQueryPort() {
            @Override
            public List<ScoreSheetSummary> findByHospitalId(String hospitalId) { return byHospital; }
            @Override
            public List<ScoreSheetSummary> findAll() { return all; }
            @Override
            public Optional<ScoreSheetSummary> findSummaryById(String id) { return Optional.empty(); }
        };
    }
}
