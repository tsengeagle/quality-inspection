package com.tzuchi.his.quality.inspection.domain.reviewcase.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

/** 複審提交完成（domain-events.md §6.8；D-069 第 4 點由 RereviewSubmittedEvent 改名，action 不變）。 */
public class RereviewCompletedEvent extends DomainEvent {

    public RereviewCompletedEvent(Object source,
                                   String actorUserId,
                                   String actorUserName,
                                   String hospitalId,
                                   String caseId,
                                   String detail) {
        super(source, actorUserId, actorUserName, hospitalId, "ReviewCase", caseId, "REREVIEW_SUBMITTED", detail);
    }

    public String getCaseId() { return getEntityId(); }
}
