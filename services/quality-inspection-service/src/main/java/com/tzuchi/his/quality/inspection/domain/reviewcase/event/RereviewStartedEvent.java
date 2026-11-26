package com.tzuchi.his.quality.inspection.domain.reviewcase.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;
import java.time.Instant;

/** 開始複審（domain-events.md §6.7，D-070 第 2 點）。action 與 detail 與加欄位之前逐字相同。 */
public class RereviewStartedEvent extends DomainEvent {

    private final String submissionId;
    private final String rereviewerId;

    public RereviewStartedEvent(Object source, String eventId, Instant occurredAt,
                                String actorUserId,
                                String actorUserName,
                                String hospitalId,
                                String caseId,
                                String submissionId,
                                String rereviewerId) {
        super(source, eventId, occurredAt, actorUserId, actorUserName, hospitalId, "ReviewCase", caseId, "REREVIEW_STARTED",
                String.format("{\"submissionId\":\"%s\"}", submissionId));
        this.submissionId = submissionId;
        this.rereviewerId = rereviewerId;
    }

    public String getCaseId() { return getEntityId(); }
    public String getSubmissionId() { return submissionId; }
    public String getRereviewerId() { return rereviewerId; }
}
