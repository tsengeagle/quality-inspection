package com.tzuchi.his.quality.inspection.domain.reviewcase.submission;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Domain model for a review submission.
 * B3a: framework only — getters, no business methods.
 * Business methods (create draft, submit, supersede) will be added in B3b/B3c.
 */
public class ReviewSubmission {

    private String submissionId;
    private String caseId;
    private ReviewStage stage;
    private String reviewerId;
    private SubmissionStatus status;
    private int revisionNo;
    private BigDecimal totalScore;
    private String comment;
    private LocalDateTime submittedAt;
    private String basedOnScoreSheetVersionId;
    private String supersededBySubmissionId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected ReviewSubmission() {
        // for persistence / future factory methods
    }

    // --- Getters ---

    public String getSubmissionId() { return submissionId; }
    public String getCaseId() { return caseId; }
    public ReviewStage getStage() { return stage; }
    public String getReviewerId() { return reviewerId; }
    public SubmissionStatus getStatus() { return status; }
    public int getRevisionNo() { return revisionNo; }
    public BigDecimal getTotalScore() { return totalScore; }
    public String getComment() { return comment; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public String getBasedOnScoreSheetVersionId() { return basedOnScoreSheetVersionId; }
    public String getSupersededBySubmissionId() { return supersededBySubmissionId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
