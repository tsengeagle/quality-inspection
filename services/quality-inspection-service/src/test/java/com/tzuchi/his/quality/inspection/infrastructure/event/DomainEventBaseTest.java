package com.tzuchi.his.quality.inspection.infrastructure.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class DomainEventBaseTest {

    private static final Instant AT = Instant.parse("2026-11-19T09:00:00Z");

    /** D-092：事件的 id 與發生時間由 use case 傳入，原樣保留。 */
    @Test
    void domainEvent_keepsTheGivenEventIdAndOccurredAt() {
        TestDomainEvent event = new TestDomainEvent(
                this, "e-001", AT, "u-001", "Test User", "H001",
                "ReviewProject", "p-001", "CREATED", null);

        assertEquals("e-001", event.getEventId());
        assertEquals(AT, event.getOccurredAt());
        assertEquals("u-001", event.getActorUserId());
        assertEquals("Test User", event.getActorUserName());
        assertEquals("H001", event.getHospitalId());
        assertEquals("ReviewProject", event.getEntityType());
        assertEquals("p-001", event.getEntityId());
        assertEquals("CREATED", event.getAction());
        assertNull(event.getDetail());
    }

    @Test
    void domainEvent_withDetail_shouldReturnDetail() {
        String detail = "{\"version\":1}";
        TestDomainEvent event = new TestDomainEvent(
                this, "e-001", AT, "u-001", "User", "H001",
                "ReviewProject", "p-001", "PUBLISHED", detail);

        assertEquals(detail, event.getDetail());
    }

    @Test
    void domainEvent_withNullHospitalId_shouldAccept() {
        TestDomainEvent event = new TestDomainEvent(
                this, "e-001", AT, "u-001", "User", null,
                "ScoreSheet", "s-001", "CREATED", null);

        assertNull(event.getHospitalId());
    }

    /** Concrete subclass for testing the abstract base class. */
    static class TestDomainEvent extends DomainEvent {
        TestDomainEvent(Object source, String eventId, Instant occurredAt, String actorUserId, String actorUserName,
                        String hospitalId, String entityType, String entityId,
                        String action, String detail) {
            super(source, eventId, occurredAt, actorUserId, actorUserName, hospitalId,
                    entityType, entityId, action, detail);
        }
    }
}
