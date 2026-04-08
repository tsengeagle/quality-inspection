package com.tzuchi.his.quality.inspection.domain.reviewcase.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

import java.time.LocalDateTime;

/**
 * Published when a reviewer is assigned (or when a case is reopened to PENDING_INITIAL_REVIEW).
 */
public class ReviewCaseAssignedEvent extends DomainEvent {

    public ReviewCaseAssignedEvent(Object source, String actorUserId, String actorUserName,
                                    String hospitalId, String caseId,
                                    String reviewerId, LocalDateTime dueAt) {
        super(source, actorUserId, actorUserName,
              hospitalId, "ReviewCase", caseId, "ASSIGNED",
              String.format("{\"reviewerId\":\"%s\",\"dueAt\":\"%s\"}", reviewerId, dueAt));
    }
}
