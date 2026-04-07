package com.tzuchi.his.quality.inspection.infrastructure.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DomainEventBaseTest {

    @Test
    void domainEvent_shouldAutoGenerateEventIdAndTimestamp() {
        TestDomainEvent event = new TestDomainEvent(
                this, "u-001", "Test User", "H001",
                "ReviewProject", "p-001", "CREATED", null);

        assertNotNull(event.getEventId());
        assertFalse(event.getEventId().isBlank());
        assertNotNull(event.getOccurredAt());
        assertEquals("u-001", event.getActorUserId());
        assertEquals("Test User", event.getActorUserName());
        assertEquals("H001", event.getHospitalId());
        assertEquals("ReviewProject", event.getEntityType());
        assertEquals("p-001", event.getEntityId());
        assertEquals("CREATED", event.getAction());
        assertNull(event.getDetail());
    }

    @Test
    void domainEvent_twoInstances_shouldHaveDifferentEventIds() {
        TestDomainEvent e1 = new TestDomainEvent(
                this, "u-001", "User", null,
                "SamplingBatch", "b-001", "EXECUTED", null);
        TestDomainEvent e2 = new TestDomainEvent(
                this, "u-001", "User", null,
                "SamplingBatch", "b-002", "EXECUTED", null);

        assertNotEquals(e1.getEventId(), e2.getEventId());
    }

    @Test
    void domainEvent_withDetail_shouldReturnDetail() {
        String detail = "{\"version\":1}";
        TestDomainEvent event = new TestDomainEvent(
                this, "u-001", "User", "H001",
                "ReviewProject", "p-001", "PUBLISHED", detail);

        assertEquals(detail, event.getDetail());
    }

    @Test
    void domainEvent_withNullHospitalId_shouldAccept() {
        TestDomainEvent event = new TestDomainEvent(
                this, "u-001", "User", null,
                "ScoreSheet", "s-001", "CREATED", null);

        assertNull(event.getHospitalId());
    }

    /** Concrete subclass for testing the abstract base class. */
    static class TestDomainEvent extends DomainEvent {
        TestDomainEvent(Object source, String actorUserId, String actorUserName,
                        String hospitalId, String entityType, String entityId,
                        String action, String detail) {
            super(source, actorUserId, actorUserName, hospitalId,
                    entityType, entityId, action, detail);
        }
    }
}
