package com.tzuchi.his.quality.inspection.application.reviewcase.query;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Read model for a single entry in the revision history of a review case. */
public class RevisionHistoryEntry {

    private final String submissionId;
    private final String stage;
    private final String reviewerId;
    private final String status;
    private final int revisionNo;
    private final BigDecimal totalScore;
    private final String comment;
    private final LocalDateTime submittedAt;
    private final String supersededBySubmissionId;

    public RevisionHistoryEntry(String submissionId, String stage, String reviewerId,
                                String status, int revisionNo, BigDecimal totalScore,
                                String comment, LocalDateTime submittedAt,
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
    public String getStage() { return stage; }
    public String getReviewerId() { return reviewerId; }
    public String getStatus() { return status; }
    public int getRevisionNo() { return revisionNo; }
    public BigDecimal getTotalScore() { return totalScore; }
    public String getComment() { return comment; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public String getSupersededBySubmissionId() { return supersededBySubmissionId; }
}
