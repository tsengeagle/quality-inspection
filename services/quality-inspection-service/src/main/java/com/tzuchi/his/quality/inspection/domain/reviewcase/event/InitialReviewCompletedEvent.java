package com.tzuchi.his.quality.inspection.domain.reviewcase.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Published when an initial review is submitted and the case enters INITIAL_REVIEW_COMPLETED
 * (domain-events.md §6.4, D-067). Submitting does not fix the result and does not decide on rereview.
 */
public class InitialReviewCompletedEvent extends DomainEvent {

    private final String projectId;
    private final String submissionId;
    private final String reviewerId;
    private final BigDecimal totalScore;
    private final BigDecimal qualityThreshold;

    public InitialReviewCompletedEvent(Object source, String eventId, Instant occurredAt, String actorUserId, String actorUserName,
                                        String hospitalId, String caseId, String projectId,
                                        String submissionId, String reviewerId,
                                        BigDecimal totalScore, BigDecimal qualityThreshold) {
        super(source, eventId, occurredAt, actorUserId, actorUserName,
              hospitalId, "ReviewCase", caseId, "INITIAL_REVIEW_COMPLETED",
              String.format("{\"projectId\":\"%s\",\"submissionId\":\"%s\",\"reviewerId\":\"%s\","
                              + "\"totalScore\":%s,\"qualityThreshold\":%s}",
                      projectId, submissionId, reviewerId, totalScore, qualityThreshold));
        this.projectId = projectId;
        this.submissionId = submissionId;
        this.reviewerId = reviewerId;
        this.totalScore = totalScore;
        this.qualityThreshold = qualityThreshold;
    }

    public String getCaseId() { return getEntityId(); }
    public String getProjectId() { return projectId; }
    public String getSubmissionId() { return submissionId; }
    public String getReviewerId() { return reviewerId; }
    public BigDecimal getTotalScore() { return totalScore; }
    public BigDecimal getQualityThreshold() { return qualityThreshold; }
}
