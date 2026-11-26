package com.tzuchi.his.quality.inspection.infrastructure.audit;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import com.tzuchi.his.quality.inspection.application.audit.AuditTrailEntry;
import com.tzuchi.his.quality.inspection.application.audit.AuditTrailQueryCriteria;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AuditTrailListenerIntegrationTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private AuditTrailJpaRepository auditRepo;

    @Autowired
    private AuditTrailQueryAdapter queryAdapter;

    private static final Instant AT = Instant.parse("2026-11-19T09:00:00Z");

    @Test
    void publishDomainEvent_shouldPersistAuditTrailEntry() {
        TestAuditEvent event = new TestAuditEvent(
                this, UUID.randomUUID().toString(), AT, "u-001", "Test User", "H001",
                "ReviewProject", "p-001", "CREATED", "{\"name\":\"Test\"}");

        eventPublisher.publishEvent(event);

        List<AuditTrailEntity> entries = auditRepo.findAll();
        assertEquals(1, entries.size());

        AuditTrailEntity entry = entries.get(0);
        assertEquals(event.getEventId(), entry.getId());
        assertEquals("ReviewProject", entry.getEntityType());
        assertEquals("p-001", entry.getEntityId());
        assertEquals("CREATED", entry.getAction());
        assertEquals("u-001", entry.getActorUserId());
        assertEquals("Test User", entry.getActorUserName());
        assertEquals("H001", entry.getHospitalId());
        assertEquals(AT, entry.getOccurredAt());
        assertEquals("{\"name\":\"Test\"}", entry.getDetail());
    }

    @Test
    void publishDomainEvent_nullHospitalId_shouldPersist() {
        TestAuditEvent event = new TestAuditEvent(
                this, UUID.randomUUID().toString(), AT, "u-admin", "Admin User", null,
                "ScoreSheet", "s-001", "PUBLISHED", null);

        eventPublisher.publishEvent(event);

        List<AuditTrailEntity> entries = auditRepo.findAll();
        assertEquals(1, entries.size());

        AuditTrailEntity entry = entries.get(0);
        assertNull(entry.getHospitalId());
        assertNull(entry.getDetail());
    }

    @Test
    void publishMultipleEvents_shouldPersistAll() {
        eventPublisher.publishEvent(new TestAuditEvent(
                this, UUID.randomUUID().toString(), AT, "u-001", "User", "H001",
                "ReviewProject", "p-001", "CREATED", null));
        eventPublisher.publishEvent(new TestAuditEvent(
                this, UUID.randomUUID().toString(), AT, "u-001", "User", "H001",
                "ReviewProject", "p-001", "PUBLISHED", null));

        List<AuditTrailEntity> entries = auditRepo.findAll();
        assertEquals(2, entries.size());
    }

    /**
     * D-092 第 3 點：同一支 use case 發出的事件時間相同。生命週期依發出的順序（遞增），
     * 稽核軌跡反過來（遞減）。事件 id 是隨機的，不能拿來排序。
     */
    @Test
    void eventsWithTheSameOccurredAt_areOrderedByTheOrderTheyWerePublished() {
        List<String> actions = List.of("A_FIRST", "B_SECOND", "C_THIRD", "D_FOURTH");
        for (String action : actions) {
            eventPublisher.publishEvent(new TestAuditEvent(
                    this, UUID.randomUUID().toString(), AT, "u-001", "User", "H001",
                    "ReviewCase", "c-001", action, null));
        }
        eventPublisher.publishEvent(new TestAuditEvent(
                this, UUID.randomUUID().toString(), AT.minusSeconds(60), "u-001", "User", "H001",
                "ReviewCase", "c-001", "Z_EARLIER", null));

        List<String> lifecycle = queryAdapter.findLifecycleByEntityId("ReviewCase", "c-001").stream()
                .map(AuditTrailEntry::getAction).collect(Collectors.toList());
        assertEquals(List.of("Z_EARLIER", "A_FIRST", "B_SECOND", "C_THIRD", "D_FOURTH"), lifecycle);

        List<String> trails = queryAdapter.findAuditTrails(
                        new AuditTrailQueryCriteria("ReviewCase", "c-001", null, null, null, null, 0, 20))
                .getContent().stream().map(AuditTrailEntry::getAction).collect(Collectors.toList());
        assertEquals(List.of("D_FOURTH", "C_THIRD", "B_SECOND", "A_FIRST", "Z_EARLIER"), trails);
    }

    /** Concrete test event extending DomainEvent. */
    static class TestAuditEvent extends DomainEvent {
        TestAuditEvent(Object source, String eventId, Instant occurredAt, String actorUserId, String actorUserName,
                       String hospitalId, String entityType, String entityId,
                       String action, String detail) {
            super(source, eventId, occurredAt, actorUserId, actorUserName, hospitalId,
                    entityType, entityId, action, detail);
        }
    }
}
