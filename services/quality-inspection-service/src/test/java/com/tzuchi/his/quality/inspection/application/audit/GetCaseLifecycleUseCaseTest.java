package com.tzuchi.his.quality.inspection.application.audit;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.Page;
import com.tzuchi.his.quality.inspection.application.common.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GetCaseLifecycleUseCaseTest {

    private InMemoryAuditPort queryPort;
    private GetCaseLifecycleUseCase useCase;

    @BeforeEach
    void setUp() {
        queryPort = new InMemoryAuditPort();
        useCase = new GetCaseLifecycleUseCase(queryPort);
    }

    @Test
    void returnsLifecycleEvents() {
        AuditTrailEntry e1 = new AuditTrailEntry("e1", "ReviewCase", "c1", "CREATED", "u1", "Mgr", "H001", Instant.now(), "{}");
        queryPort.setLifecycle("ReviewCase", "c1", Collections.singletonList(e1));
        ActorContext actor = new ActorContext("u1", "Manager", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        List<AuditTrailEntry> result = useCase.execute(actor, "c1");
        assertEquals(1, result.size());
        assertEquals("CREATED", result.get(0).getAction());
    }

    @Test
    void unknownRole_shouldThrowUnauthorized() {
        ActorContext actor = new ActorContext("u1", "Nobody", EnumSet.noneOf(DomainRole.class), Optional.empty());
        assertThrows(UnauthorizedException.class, () -> useCase.execute(actor, "c1"));
    }

    static class InMemoryAuditPort implements AuditTrailQueryPort {
        private final Map<String, List<AuditTrailEntry>> lifecycles = new HashMap<>();
        void setLifecycle(String entityType, String entityId, List<AuditTrailEntry> entries) {
            lifecycles.put(entityType + ":" + entityId, entries);
        }

        @Override
        public Page<AuditTrailEntry> findAuditTrails(AuditTrailQueryCriteria criteria) {
            return new Page<>(Collections.emptyList(), 0, 20, 0);
        }

        @Override
        public List<AuditTrailEntry> findLifecycleByEntityId(String entityType, String entityId) {
            return lifecycles.getOrDefault(entityType + ":" + entityId, Collections.emptyList());
        }
    }
}
