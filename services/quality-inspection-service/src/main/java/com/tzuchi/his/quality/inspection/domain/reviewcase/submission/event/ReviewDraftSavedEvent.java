package com.tzuchi.his.quality.inspection.domain.reviewcase.submission.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Published when a reviewer saves a draft of the initial review or a rereviewer saves a rereview draft.
 */
public class ReviewDraftSavedEvent extends DomainEvent {

    private final String caseId;

    public ReviewDraftSavedEvent(Object source, String eventId, Instant occurredAt, String actorUserId, String actorUserName,
                                  String hospitalId, String caseId,
                                  String submissionId, BigDecimal totalScore) {
        super(source, eventId, occurredAt, actorUserId, actorUserName,
              hospitalId, "ReviewSubmission", submissionId, "DRAFT_SAVED",
              String.format("{\"caseId\":\"%s\",\"totalScore\":%s}", caseId, totalScore));
        this.caseId = caseId;
    }

    public String getSubmissionId() { return getEntityId(); }
    public String getCaseId() { return caseId; }
}
