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

        List<ScoreSheetSummary> result = useCase.execute(actor, null).getItems();

        assertEquals(1, result.size());
        assertEquals("ss1", result.get(0).getScoreSheetId());
    }

    @Test
    void execute_sa_shouldReturnAll() {
        ScoreSheetQueryPort queryPort = stubQueryPort(List.of(summary1), List.of(summary1, summary2));
        ListScoreSheetSummariesUseCase useCase = new ListScoreSheetSummariesUseCase(queryPort);
        ActorContext actor = new ActorContext("u1", "Admin",
                EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.empty());

        List<ScoreSheetSummary> result = useCase.execute(actor, null).getItems();

        assertEquals(2, result.size());
    }

    @Test
    void execute_noRole_shouldThrow() {
        ScoreSheetQueryPort queryPort = stubQueryPort(List.of(), List.of());
        ListScoreSheetSummariesUseCase useCase = new ListScoreSheetSummariesUseCase(queryPort);
        ActorContext actor = new ActorContext("u1", "Reviewer",
                EnumSet.of(DomainRole.REVIEWER), Optional.of("H001"));

        assertThrows(RoleDeniedException.class, () -> useCase.execute(actor, null));
    }

    @Test
    void execute_sa_withHospitalId_shouldNarrowToThatHospital() {
        // D-047 第 1 點
        RecordingPort port = new RecordingPort();
        ListScoreSheetSummariesUseCase useCase = new ListScoreSheetSummariesUseCase(port);
        ActorContext actor = new ActorContext("u1", "Admin",
                EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.empty());

        useCase.execute(actor, "H002");

        assertEquals("H002", port.requestedHospital);
        assertFalse(port.allRequested);
    }

    @Test
    void execute_hm_requestedHospitalIsIgnored() {
        // D-047 第 2 點
        RecordingPort port = new RecordingPort();
        ListScoreSheetSummariesUseCase useCase = new ListScoreSheetSummariesUseCase(port);
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        useCase.execute(actor, "H002");

        assertEquals("H001", port.requestedHospital);
        assertFalse(port.allRequested);
    }

    private static class RecordingPort implements ScoreSheetQueryPort {
        String requestedHospital;
        boolean allRequested;
        @Override
        public List<ScoreSheetSummary> findByHospitalId(String hospitalId) { requestedHospital = hospitalId; return List.of(); }
        @Override
        public List<ScoreSheetSummary> findAll() { allRequested = true; return List.of(); }
        @Override
        public Optional<ScoreSheetSummary> findSummaryById(String id) { return Optional.empty(); }
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

    @Test
    void execute_auditor_listsOwnHospital_D050() {
        RecordingPort port = new RecordingPort();
        ListScoreSheetSummariesUseCase useCase = new ListScoreSheetSummariesUseCase(port);
        ActorContext auditor = new ActorContext("u1", "Auditor",
                EnumSet.of(DomainRole.AUDITOR), Optional.of("H001"));

        useCase.execute(auditor, "H002");

        assertEquals("H001", port.requestedHospital);
        assertFalse(port.allRequested);
    }
}
