package com.tzuchi.his.quality.inspection.domain.reviewcase.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;
import java.time.Instant;

/**
 * 複審路徑的正式結案（D-034、D-053；domain-events.md §6.9，D-070 第 4 點）。D-069 第 4 點由 RereviewCaseClosedEvent 改名，action 不變。
 * submissionId 是結案時固定的那份複審提交（D-053 第 3 點）。操作者記在稽核事件。
 */
public class ReviewCaseClosedEvent extends DomainEvent {

    private final String submissionId;

    public ReviewCaseClosedEvent(Object source, String eventId, Instant occurredAt,
                                 String actorUserId,
                                 String actorUserName,
                                 String hospitalId,
                                 String caseId,
                                 String submissionId) {
        super(source, eventId, occurredAt, actorUserId, actorUserName, hospitalId, "ReviewCase", caseId, "CASE_CLOSED_AFTER_REREVIEW",
                String.format("{\"submissionId\":\"%s\"}", submissionId));
        this.submissionId = submissionId;
    }

    public String getCaseId() { return getEntityId(); }
    public String getSubmissionId() { return submissionId; }
}
