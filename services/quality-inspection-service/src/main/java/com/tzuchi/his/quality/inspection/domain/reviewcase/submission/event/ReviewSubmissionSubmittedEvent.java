package com.tzuchi.his.quality.inspection.domain.reviewcase.submission.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

import java.math.BigDecimal;

/**
 * Published when a reviewer submits the initial review.
 */
public class ReviewSubmissionSubmittedEvent extends DomainEvent {

    public ReviewSubmissionSubmittedEvent(Object source, String actorUserId, String actorUserName,
                                           String hospitalId, String caseId,
                                           String submissionId, BigDecimal totalScore,
                                           String reviewerId) {
        super(source, actorUserId, actorUserName,
              hospitalId, "ReviewSubmission", submissionId, "SUBMITTED",
              String.format("{\"caseId\":\"%s\",\"totalScore\":%s,\"reviewerId\":\"%s\"}",
                      caseId, totalScore, reviewerId));
    }
}
