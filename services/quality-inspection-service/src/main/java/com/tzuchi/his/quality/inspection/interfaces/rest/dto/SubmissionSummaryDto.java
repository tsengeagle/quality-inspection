package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class SubmissionSummaryDto {

    private String submissionId;
    private String stage;
    private String reviewerId;
    private String status;
    private int revisionNo;
    private BigDecimal totalScore;
    private String comment;
    private Instant submittedAt;
    private String supersededBySubmissionId;

    public SubmissionSummaryDto() {}

    public SubmissionSummaryDto(String submissionId, String stage, String reviewerId,
                                 String status, int revisionNo, BigDecimal totalScore,
                                 String comment, Instant submittedAt,
                                 String supersededBySubmissionId) {
        this.submissionId = submissionId;
        this.stage = stage;
        this.reviewerId = reviewerId;
        this.status = status;
        this.revisionNo = revisionNo;
        this.totalScore = totalScore;
        this.comment = comment;
        this.submittedAt = submittedAt;
        this.supersededBySubmissionId = supersededBySubmissionId;
    }

    public String getSubmissionId() { return submissionId; }
    public void setSubmissionId(String submissionId) { this.submissionId = submissionId; }
    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }
    public String getReviewerId() { return reviewerId; }
    public void setReviewerId(String reviewerId) { this.reviewerId = reviewerId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getRevisionNo() { return revisionNo; }
    public void setRevisionNo(int revisionNo) { this.revisionNo = revisionNo; }
    public BigDecimal getTotalScore() { return totalScore; }
    public void setTotalScore(BigDecimal totalScore) { this.totalScore = totalScore; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
    public String getSupersededBySubmissionId() { return supersededBySubmissionId; }
    public void setSupersededBySubmissionId(String v) { this.supersededBySubmissionId = v; }
}