package com.tzuchi.his.quality.inspection.domain.reviewcase.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Published when a reviewer is assigned (or when a case is reopened to PENDING_INITIAL_REVIEW).
 */
public class ReviewCaseAssignedEvent extends DomainEvent {

    /** D-072 第 4 點第 4 項：dueAt 是台北的牆上時刻；detail 依憲法第一條換成時間點，輸出 UTC。
     *  重新開啟初審（ReopenInitialReview）沒有 dueAt，detail 照舊是 "null"。 */
    private static final ZoneId TAIPEI = ZoneId.of("Asia/Taipei");

    private final String assignedReviewerId;

    public ReviewCaseAssignedEvent(Object source, String actorUserId, String actorUserName,
                                    String hospitalId, String caseId,
                                    String reviewerId, LocalDateTime dueAt) {
        super(source, actorUserId, actorUserName,
              hospitalId, "ReviewCase", caseId, "ASSIGNED",
              String.format("{\"reviewerId\":\"%s\",\"dueAt\":\"%s\"}", reviewerId,
                      dueAt == null ? null : dueAt.atZone(TAIPEI).toInstant()));
        this.assignedReviewerId = reviewerId;
    }

    public String getCaseId() { return getEntityId(); }
    public String getAssignedReviewerId() { return assignedReviewerId; }
}
