package com.tzuchi.his.quality.inspection.domain.reviewcase.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

import java.time.Instant;

/**
 * 指派或改派複審醫師（domain-events.md §6.6，D-070 第 1 點）。
 * action 與 detail 與加欄位之前逐字相同（稽核紀錄不變）；改派的 detail 多 "reassignment":true。
 */
public class RereviewerAssignedEvent extends DomainEvent {

    private final String rereviewerId;
    private final Instant dueAt;
    private final boolean reassignment;

    public RereviewerAssignedEvent(Object source, String eventId, Instant occurredAt,
                                   String actorUserId,
                                   String actorUserName,
                                   String hospitalId,
                                   String caseId,
                                   String rereviewerId,
                                   Instant dueAt,
                                   boolean reassignment) {
        super(source, eventId, occurredAt, actorUserId, actorUserName, hospitalId, "ReviewCase", caseId, "REREVIEWER_ASSIGNED",
                reassignment
                        ? String.format("{\"rereviewerId\":\"%s\",\"reassignment\":true}", rereviewerId)
                        : String.format("{\"rereviewerId\":\"%s\"}", rereviewerId));
        this.rereviewerId = rereviewerId;
        this.dueAt = dueAt;
        this.reassignment = reassignment;
    }

    public String getCaseId() { return getEntityId(); }
    public String getRereviewerId() { return rereviewerId; }
    public Instant getDueAt() { return dueAt; }
    /** 改派為 true，指派為 false（D-082 第 5 點）。 */
    public boolean isReassignment() { return reassignment; }
}
