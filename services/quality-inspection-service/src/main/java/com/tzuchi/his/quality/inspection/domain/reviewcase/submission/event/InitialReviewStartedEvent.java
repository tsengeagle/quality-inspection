package com.tzuchi.his.quality.inspection.domain.reviewcase.submission.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

/**
 * Published when a reviewer starts the initial review on a case.
 */
public class InitialReviewStartedEvent extends DomainEvent {

    public InitialReviewStartedEvent(Object source, String actorUserId, String actorUserName,
                                      String hospitalId, String caseId,
                                      String reviewerId, String submissionId) {
        super(source, actorUserId, actorUserName,
              hospitalId, "ReviewCase", caseId, "INITIAL_REVIEW_STARTED",
              String.format("{\"reviewerId\":\"%s\",\"submissionId\":\"%s\"}", reviewerId, submissionId));
    }

    public String getCaseId() { return getEntityId(); }
}
