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

class ListAuditTrailsUseCaseTest {

    private InMemoryAuditTrailQueryPort queryPort;
    private ListAuditTrailsUseCase useCase;

    @BeforeEach
    void setUp() {
        queryPort = new InMemoryAuditTrailQueryPort();
        useCase = new ListAuditTrailsUseCase(queryPort);
    }

    @Test
    void hospitalManager_canQuery() {
        ActorContext actor = new ActorContext("u1", "Manager", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));
        AuditTrailQueryCriteria criteria = new AuditTrailQueryCriteria(null, null, null, null, null, null, 0, 20);

        Page<AuditTrailEntry> result = useCase.execute(actor, criteria);
        assertNotNull(result);
    }

    @Test
    void reviewer_shouldThrowUnauthorized() {
        ActorContext actor = new ActorContext("r1", "Reviewer", EnumSet.of(DomainRole.REVIEWER), Optional.of("H001"));
        AuditTrailQueryCriteria criteria = new AuditTrailQueryCriteria(null, null, null, null, null, null, 0, 20);

        assertThrows(UnauthorizedException.class, () -> useCase.execute(actor, criteria));
    }

    @Test
    void delegatesToQueryPort() {
        AuditTrailEntry entry = new AuditTrailEntry("e1", "ReviewCase", "c1", "ASSIGN_REVIEWER", "u1", "Mgr", "H001", Instant.now(), "{}");
        queryPort.setPage(new Page<>(Collections.singletonList(entry), 0, 20, 1));
        ActorContext actor = new ActorContext("u1", "Admin", EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.of("H001"));
        AuditTrailQueryCriteria criteria = new AuditTrailQueryCriteria(null, null, null, null, null, null, 0, 20);

        Page<AuditTrailEntry> result = useCase.execute(actor, criteria);
        assertEquals(1, result.getContent().size());
    }

    static class InMemoryAuditTrailQueryPort implements AuditTrailQueryPort {
        private Page<AuditTrailEntry> page = new Page<>(Collections.emptyList(), 0, 20, 0);
        void setPage(Page<AuditTrailEntry> p) { this.page = p; }

        @Override
        public Page<AuditTrailEntry> findAuditTrails(AuditTrailQueryCriteria criteria) { return page; }

        @Override
        public List<AuditTrailEntry> findLifecycleByEntityId(String entityType, String entityId) {
            return Collections.emptyList();
        }
    }
}
