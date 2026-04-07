package com.tzuchi.his.quality.inspection.infrastructure.audit;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AuditTrailListenerIntegrationTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private AuditTrailJpaRepository auditRepo;

    @Test
    void publishDomainEvent_shouldPersistAuditTrailEntry() {
        TestAuditEvent event = new TestAuditEvent(
                this, "u-001", "Test User", "H001",
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
        assertNotNull(entry.getOccurredAt());
        assertEquals("{\"name\":\"Test\"}", entry.getDetail());
    }

    @Test
    void publishDomainEvent_nullHospitalId_shouldPersist() {
        TestAuditEvent event = new TestAuditEvent(
                this, "u-admin", "Admin User", null,
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
                this, "u-001", "User", "H001",
                "ReviewProject", "p-001", "CREATED", null));
        eventPublisher.publishEvent(new TestAuditEvent(
                this, "u-001", "User", "H001",
                "ReviewProject", "p-001", "PUBLISHED", null));

        List<AuditTrailEntity> entries = auditRepo.findAll();
        assertEquals(2, entries.size());
    }

    /** Concrete test event extending DomainEvent. */
    static class TestAuditEvent extends DomainEvent {
        TestAuditEvent(Object source, String actorUserId, String actorUserName,
                       String hospitalId, String entityType, String entityId,
                       String action, String detail) {
            super(source, actorUserId, actorUserName, hospitalId,
                    entityType, entityId, action, detail);
        }
    }
}
