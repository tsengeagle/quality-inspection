package com.tzuchi.his.quality.inspection.domain.reviewcase.submission.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

import java.math.BigDecimal;

/**
 * Published when a reviewer saves a draft of the initial review.
 */
public class ReviewDraftSavedEvent extends DomainEvent {

    public ReviewDraftSavedEvent(Object source, String actorUserId, String actorUserName,
                                  String hospitalId, String caseId,
                                  String submissionId, BigDecimal totalScore) {
        super(source, actorUserId, actorUserName,
              hospitalId, "ReviewSubmission", submissionId, "DRAFT_SAVED",
              String.format("{\"caseId\":\"%s\",\"totalScore\":%s}", caseId, totalScore));
    }

    public String getSubmissionId() { return getEntityId(); }
}
