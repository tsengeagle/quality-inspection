package com.tzuchi.his.quality.inspection.domain.reviewcase.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

/**
 * Published when a case is returned to initial review.
 */
public class ReviewCaseReturnedEvent extends DomainEvent {

    public ReviewCaseReturnedEvent(Object source, String actorUserId, String actorUserName,
                                    String hospitalId, String caseId, String reason) {
        super(source, actorUserId, actorUserName,
              hospitalId, "ReviewCase", caseId, "RETURNED",
              String.format("{\"reason\":\"%s\"}", reason));
    }
}
