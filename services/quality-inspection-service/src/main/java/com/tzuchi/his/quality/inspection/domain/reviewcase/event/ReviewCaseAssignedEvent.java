package com.tzuchi.his.quality.inspection.domain.reviewcase.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

import java.time.Instant;

/**
 * Published when a reviewer is assigned.
 */
public class ReviewCaseAssignedEvent extends DomainEvent {

    // dueAt 是時間點（D-075），detail 輸出 UTC（憲法第一條）。

    private final String assignedReviewerId;

    public ReviewCaseAssignedEvent(Object source, String actorUserId, String actorUserName,
                                    String hospitalId, String caseId,
                                    String reviewerId, Instant dueAt) {
        super(source, actorUserId, actorUserName,
              hospitalId, "ReviewCase", caseId, "ASSIGNED",
              String.format("{\"reviewerId\":\"%s\",\"dueAt\":\"%s\"}", reviewerId,
                      dueAt));
        this.assignedReviewerId = reviewerId;
    }

    public String getCaseId() { return getEntityId(); }
    public String getAssignedReviewerId() { return assignedReviewerId; }
}
