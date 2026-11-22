package com.tzuchi.his.quality.inspection.domain.reviewcase.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

/**
 * 複審路徑的正式結案（D-034、D-053；domain-events.md §6.9）。D-069 第 4 點由 RereviewCaseClosedEvent 改名，action 不變。操作者記在稽核事件（D-053 第 3 點）。
 */
public class ReviewCaseClosedEvent extends DomainEvent {

    public ReviewCaseClosedEvent(Object source,
                                    String actorUserId,
                                    String actorUserName,
                                    String hospitalId,
                                    String caseId,
                                    String detail) {
        super(source, actorUserId, actorUserName, hospitalId, "ReviewCase", caseId, "CASE_CLOSED_AFTER_REREVIEW", detail);
    }

    public String getCaseId() { return getEntityId(); }
}
