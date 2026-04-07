package com.tzuchi.his.quality.inspection.domain.common;

import org.springframework.context.ApplicationEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Abstract base class for all domain events.
 * Extends Spring ApplicationEvent for direct integration with Spring's event mechanism.
 *
 * Subclasses should provide concrete event types (e.g., ReviewProjectCreatedEvent).
 * The AuditTrailListener automatically persists all DomainEvent subclasses.
 */
public abstract class DomainEvent extends ApplicationEvent {

    private final String eventId;
    private final Instant occurredAt;
    private final String actorUserId;
    private final String actorUserName;
    private final String hospitalId;
    private final String entityType;
    private final String entityId;
    private final String action;
    private final String detail;

    /**
     * @param source        the object that published this event (typically the application service)
     * @param actorUserId   the authenticated user who triggered the action
     * @param actorUserName display name of the actor
     * @param hospitalId    hospital context (nullable — null for global operations)
     * @param entityType    aggregate type name (e.g., "ReviewProject")
     * @param entityId      aggregate root ID
     * @param action        action name (e.g., "CREATED", "PUBLISHED")
     * @param detail        optional JSON detail (nullable)
     */
    protected DomainEvent(Object source, String actorUserId, String actorUserName,
                          String hospitalId, String entityType, String entityId,
                          String action, String detail) {
        super(source);
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = Instant.now();
        this.actorUserId = actorUserId;
        this.actorUserName = actorUserName;
        this.hospitalId = hospitalId;
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.detail = detail;
    }

    public String getEventId() { return eventId; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getActorUserId() { return actorUserId; }
    public String getActorUserName() { return actorUserName; }
    public String getHospitalId() { return hospitalId; }
    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
    public String getAction() { return action; }
    public String getDetail() { return detail; }
}
