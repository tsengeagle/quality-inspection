package com.tzuchi.his.quality.inspection.domain.reviewcase.submission.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewStage;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Published when a reviewer submits the initial review or a rereviewer submits the rereview.
 * D-076 第 2 點：帶 stage；稽核的 action、detail 逐字不變。
 */
public class ReviewSubmissionSubmittedEvent extends DomainEvent {

    private final String caseId;
    private final ReviewStage stage;

    public ReviewSubmissionSubmittedEvent(Object source, String eventId, Instant occurredAt, String actorUserId, String actorUserName,
                                           String hospitalId, String caseId,
                                           String submissionId, ReviewStage stage,
                                           BigDecimal totalScore, String reviewerId) {
        super(source, eventId, occurredAt, actorUserId, actorUserName,
              hospitalId, "ReviewSubmission", submissionId, "SUBMITTED",
              String.format("{\"caseId\":\"%s\",\"totalScore\":%s,\"reviewerId\":\"%s\"}",
                      caseId, totalScore, reviewerId));
        this.caseId = caseId;
        this.stage = stage;
    }

    public String getCaseId() { return caseId; }
    public String getSubmissionId() { return getEntityId(); }
    public ReviewStage getStage() { return stage; }
}
