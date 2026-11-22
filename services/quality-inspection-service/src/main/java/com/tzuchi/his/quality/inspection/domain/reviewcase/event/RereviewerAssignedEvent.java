package com.tzuchi.his.quality.inspection.domain.reviewcase.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

public class RereviewerAssignedEvent extends DomainEvent {

    public RereviewerAssignedEvent(Object source,
                                    String actorUserId,
                                    String actorUserName,
                                    String hospitalId,
                                    String caseId,
                                    String detail) {
        super(source, actorUserId, actorUserName, hospitalId, "ReviewCase", caseId, "REREVIEWER_ASSIGNED", detail);
    }

    public String getCaseId() { return getEntityId(); }
}
