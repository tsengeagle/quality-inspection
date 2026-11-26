package com.tzuchi.his.quality.inspection.domain.reviewcase.submission.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;
import java.time.Instant;

/**
 * Published when a reviewer starts the initial review on a case.
 */
public class InitialReviewStartedEvent extends DomainEvent {

    private final String reviewerId;
    private final String submissionId;

    public InitialReviewStartedEvent(Object source, String eventId, Instant occurredAt, String actorUserId, String actorUserName,
                                      String hospitalId, String caseId,
                                      String reviewerId, String submissionId) {
        super(source, eventId, occurredAt, actorUserId, actorUserName,
              hospitalId, "ReviewCase", caseId, "INITIAL_REVIEW_STARTED",
              String.format("{\"reviewerId\":\"%s\",\"submissionId\":\"%s\"}", reviewerId, submissionId));
        this.reviewerId = reviewerId;
        this.submissionId = submissionId;
    }

    public String getCaseId() { return getEntityId(); }
    public String getSubmissionId() { return submissionId; }
    public String getReviewerId() { return reviewerId; }
}
