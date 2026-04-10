package com.tzuchi.his.quality.inspection.domain.reviewcase.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

public class RereviewSubmittedEvent extends DomainEvent {

    public RereviewSubmittedEvent(Object source,
                                   String actorUserId,
                                   String actorUserName,
                                   String hospitalId,
                                   String caseId,
                                   String detail) {
        super(source, actorUserId, actorUserName, hospitalId, "ReviewCase", caseId, "REREVIEW_SUBMITTED", detail);
    }
}
