package com.tzuchi.his.quality.inspection.domain.reviewcase.submission.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

/**
 * Published when an initial review submission is revised.
 */
public class ReviewSubmissionRevisedEvent extends DomainEvent {

    public ReviewSubmissionRevisedEvent(Object source, String actorUserId, String actorUserName,
                                         String hospitalId, String caseId,
                                         String oldSubmissionId, String newSubmissionId,
                                         String stage, int revisionNo) {
        super(source, actorUserId, actorUserName,
              hospitalId, "ReviewSubmission", newSubmissionId, "REVISED",
              String.format("{\"caseId\":\"%s\",\"oldSubmissionId\":\"%s\",\"stage\":\"%s\",\"revisionNo\":%d}",
                      caseId, oldSubmissionId, stage, revisionNo));
    }
}