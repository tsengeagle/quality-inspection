package com.tzuchi.his.quality.inspection.domain.reviewcase.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

import java.time.LocalDateTime;

/**
 * Published when a case reviewer is changed (reassigned).
 */
public class ReviewCaseReassignedEvent extends DomainEvent {

    public ReviewCaseReassignedEvent(Object source, String actorUserId, String actorUserName,
                                      String hospitalId, String caseId,
                                      String previousReviewerId, String newReviewerId,
                                      LocalDateTime dueAt) {
        super(source, actorUserId, actorUserName,
              hospitalId, "ReviewCase", caseId, "REASSIGNED",
              String.format("{\"previousReviewerId\":\"%s\",\"newReviewerId\":\"%s\",\"dueAt\":\"%s\"}",
                      previousReviewerId, newReviewerId, dueAt));
    }
}
