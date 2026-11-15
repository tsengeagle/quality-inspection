package com.tzuchi.his.quality.inspection.domain.reviewcase.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

/**
 * 複審路徑的正式結案（D-034、D-053）。操作者記在稽核事件（D-053 第 3 點）。
 */
public class RereviewCaseClosedEvent extends DomainEvent {

    public RereviewCaseClosedEvent(Object source,
                                    String actorUserId,
                                    String actorUserName,
                                    String hospitalId,
                                    String caseId,
                                    String detail) {
        super(source, actorUserId, actorUserName, hospitalId, "ReviewCase", caseId, "CASE_CLOSED_AFTER_REREVIEW", detail);
    }
}
