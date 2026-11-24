package com.tzuchi.his.quality.inspection.domain.reviewcase.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

import java.time.Instant;

/**
 * Published when a returned case is reopened to PENDING_INITIAL_REVIEW（D-080 第 2 點）.
 */
public class ReviewCaseReopenedEvent extends DomainEvent {

    // 沿用原本的初審醫師與期限（D-080 第 4 點）；dueAt 輸出 UTC（憲法第一條）。

    private final String reviewerId;
    private final Instant dueAt;

    public ReviewCaseReopenedEvent(Object source, String actorUserId, String actorUserName,
                                    String hospitalId, String caseId,
                                    String reviewerId, Instant dueAt) {
        super(source, actorUserId, actorUserName,
              hospitalId, "ReviewCase", caseId, "REOPENED",
              String.format("{\"reviewerId\":\"%s\",\"dueAt\":\"%s\"}", reviewerId, dueAt));
        this.reviewerId = reviewerId;
        this.dueAt = dueAt;
    }

    public String getCaseId() { return getEntityId(); }
    public String getReviewerId() { return reviewerId; }
    public Instant getDueAt() { return dueAt; }
}
