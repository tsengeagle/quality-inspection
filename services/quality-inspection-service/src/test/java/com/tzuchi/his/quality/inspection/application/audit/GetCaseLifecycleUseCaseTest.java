package com.tzuchi.his.quality.inspection.application.audit;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.Page;
import com.tzuchi.his.quality.inspection.application.common.RoleDeniedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GetCaseLifecycleUseCaseTest {

    private InMemoryAuditPort queryPort;
    private GetCaseLifecycleUseCase useCase;
    private com.tzuchi.his.quality.inspection.testfixture.InMemoryReviewCaseRepository caseRepository;

    @BeforeEach
    void setUp() {
        queryPort = new InMemoryAuditPort();
        caseRepository = new com.tzuchi.his.quality.inspection.testfixture.InMemoryReviewCaseRepository();
        caseRepository.save(com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase.createFromSampling(
                "c1", "b1", "p1", "H001", "SK-001", com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType.OUTPATIENT));
        useCase = new GetCaseLifecycleUseCase(queryPort, caseRepository, ScoredDetailMaskTest.maskWith(Map.of()));
    }

    @Test
    void returnsLifecycleEvents() {
        AuditTrailEntry e1 = new AuditTrailEntry("e1", "ReviewCase", "c1", "CREATED", "u1", "Mgr", "H001", Instant.now(), "{}");
        queryPort.setLifecycle("ReviewCase", "c1", Collections.singletonList(e1));
        ActorContext actor = new ActorContext("u1", "Manager", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        List<AuditTrailEntry> result = useCase.execute(actor, "c1").getEntries();
        assertEquals(1, result.size());
        assertEquals("CREATED", result.get(0).getAction());
    }

    @Test
    void unknownRole_shouldThrowUnauthorized() {
        ActorContext actor = new ActorContext("u1", "Nobody", EnumSet.noneOf(DomainRole.class), Optional.empty());
        assertThrows(RoleDeniedException.class, () -> useCase.execute(actor, "c1"));
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

    @Test
    void otherHospital_shouldThrowAccessDenied() {
        ActorContext actor = new ActorContext("u2", "Auditor", EnumSet.of(DomainRole.AUDITOR), Optional.of("H002"));
        assertThrows(com.tzuchi.his.quality.inspection.application.common.AccessDeniedException.class, () -> useCase.execute(actor, "c1"));
    }

    @Test
    void systemAdmin_readsAcrossHospitals() {
        AuditTrailEntry e1 = new AuditTrailEntry("e1", "ReviewCase", "c1", "CREATED", "u1", "Mgr", "H001", Instant.now(), "{}");
        queryPort.setLifecycle("ReviewCase", "c1", Collections.singletonList(e1));
        ActorContext actor = new ActorContext("u3", "Admin", EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.of("H002"));
        assertEquals(1, useCase.execute(actor, "c1").getEntries().size());
    }
}
