package com.tzuchi.his.quality.inspection.application.audit;

import java.time.Instant;

public class AuditTrailEntry {

    private final String id;
    private final String entityType;
    private final String entityId;
    private final String action;
    private final String actorUserId;
    private final String actorUserName;
    private final String hospitalId;
    private final Instant occurredAt;
    private final String detail;

    public AuditTrailEntry(String id, String entityType, String entityId, String action,
                            String actorUserId, String actorUserName, String hospitalId,
                            Instant occurredAt, String detail) {
        this.id = id;
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.actorUserId = actorUserId;
        this.actorUserName = actorUserName;
        this.hospitalId = hospitalId;
        this.occurredAt = occurredAt;
        this.detail = detail;
    }

    public String getId() { return id; }
    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
    public String getAction() { return action; }
    public String getActorUserId() { return actorUserId; }
    public String getActorUserName() { return actorUserName; }
    public String getHospitalId() { return hospitalId; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getDetail() { return detail; }

    /**
     * 同一筆紀錄但不含 detail。detail 可能帶有初審分數或 finalScore（D-033），
     * 不得取得這些值的讀者只拿到動作、操作者與時間。
     */
    public AuditTrailEntry withoutDetail() {
        return new AuditTrailEntry(id, entityType, entityId, action, actorUserId, actorUserName, hospitalId,
                occurredAt, null);
    }
}
