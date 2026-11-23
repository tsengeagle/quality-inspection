package com.tzuchi.his.quality.inspection.application.reviewcase.query;

import java.time.Instant;
import java.math.BigDecimal;

public class SubmissionSummaryItem {

    private final String submissionId;
    private final String stage;
    private final String reviewerId;
    private final int revisionNo;
    private final String status;
    private final BigDecimal totalScore;
    private final Instant submittedAt;

    public SubmissionSummaryItem(String submissionId, String stage, int revisionNo,
                                  String status, Instant submittedAt) {
        this(submissionId, stage, null, revisionNo, status, null, submittedAt);
    }

    public SubmissionSummaryItem(String submissionId, String stage, String reviewerId,
                                  int revisionNo, String status, BigDecimal totalScore,
                                  Instant submittedAt) {
        this.submissionId = submissionId;
        this.stage = stage;
        this.reviewerId = reviewerId;
        this.revisionNo = revisionNo;
        this.status = status;
        this.totalScore = totalScore;
        this.submittedAt = submittedAt;
    }

    public String getSubmissionId() { return submissionId; }
    public String getStage() { return stage; }
    public String getReviewerId() { return reviewerId; }
    public int getRevisionNo() { return revisionNo; }
    public String getStatus() { return status; }
    public BigDecimal getTotalScore() { return totalScore; }
    public Instant getSubmittedAt() { return submittedAt; }
}
