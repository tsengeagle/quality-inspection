package com.tzuchi.his.quality.inspection.infrastructure.audit;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.Instant;

/**
 * JPA entity for the audit_trail table.
 * Records every domain event for traceability.
 */
@Entity
@Table(name = "audit_trail")
public class AuditTrailEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id", nullable = false, length = 36)
    private String entityId;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(name = "actor_user_id", nullable = false, length = 50)
    private String actorUserId;

    @Column(name = "actor_user_name", nullable = false, length = 100)
    private String actorUserName;

    @Column(name = "hospital_id", length = 50)
    private String hospitalId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(columnDefinition = "TEXT")
    private String detail;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getActorUserId() { return actorUserId; }
    public void setActorUserId(String actorUserId) { this.actorUserId = actorUserId; }

    public String getActorUserName() { return actorUserName; }
    public void setActorUserName(String actorUserName) { this.actorUserName = actorUserName; }

    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }

    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
}
