package com.tzuchi.his.quality.inspection.domain.reviewcase.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;
import java.time.Instant;

public class CaseClosedWithoutRereviewEvent extends DomainEvent {

    public CaseClosedWithoutRereviewEvent(Object source, String eventId, Instant occurredAt,
                                           String actorUserId,
                                           String actorUserName,
                                           String hospitalId,
                                           String caseId,
                                           String detail) {
        super(source, eventId, occurredAt, actorUserId, actorUserName, hospitalId, "ReviewCase", caseId, "CASE_CLOSED_WITHOUT_REREVIEW", detail);
    }

    public String getCaseId() { return getEntityId(); }
}
