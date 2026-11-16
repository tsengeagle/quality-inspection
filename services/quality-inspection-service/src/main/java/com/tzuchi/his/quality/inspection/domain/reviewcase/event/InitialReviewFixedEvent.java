package com.tzuchi.his.quality.inspection.domain.reviewcase.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

/** D-054 第 8 點：初審固定。detail 只記 submissionId，不記分數。 */
public class InitialReviewFixedEvent extends DomainEvent {

    public InitialReviewFixedEvent(Object source,
                                   String actorUserId,
                                   String actorUserName,
                                   String hospitalId,
                                   String caseId,
                                   String detail) {
        super(source, actorUserId, actorUserName, hospitalId, "ReviewCase", caseId, "INITIAL_REVIEW_FIXED", detail);
    }
}
