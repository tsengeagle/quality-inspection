package com.tzuchi.his.quality.inspection.domain.reviewcase.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

public class CaseEscalatedToRereviewEvent extends DomainEvent {

    public CaseEscalatedToRereviewEvent(Object source,
                                         String actorUserId,
                                         String actorUserName,
                                         String hospitalId,
                                         String caseId,
                                         String detail) {
        super(source, actorUserId, actorUserName, hospitalId, "ReviewCase", caseId, "CASE_ESCALATED_TO_REREVIEW", detail);
    }
}
