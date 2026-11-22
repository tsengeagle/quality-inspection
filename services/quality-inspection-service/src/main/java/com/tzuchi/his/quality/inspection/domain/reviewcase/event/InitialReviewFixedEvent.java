package com.tzuchi.his.quality.inspection.domain.reviewcase.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

/** D-054 第 8 點：初審固定（domain-events.md §6.12，D-069）。detail 只記 submissionId，不記分數。 */
public class InitialReviewFixedEvent extends DomainEvent {

    private final String submissionId;

    public InitialReviewFixedEvent(Object source,
                                   String actorUserId,
                                   String actorUserName,
                                   String hospitalId,
                                   String caseId,
                                   String submissionId) {
        super(source, actorUserId, actorUserName, hospitalId, "ReviewCase", caseId, "INITIAL_REVIEW_FIXED",
                String.format("{\"submissionId\":\"%s\"}", submissionId));
        this.submissionId = submissionId;
    }

    public String getCaseId() { return getEntityId(); }
    public String getSubmissionId() { return submissionId; }
}
