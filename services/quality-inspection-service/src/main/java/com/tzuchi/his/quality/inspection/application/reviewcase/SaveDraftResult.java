package com.tzuchi.his.quality.inspection.application.reviewcase;

import java.math.BigDecimal;

/**
 * Result of saving a review draft — returned to the caller.
 */
public class SaveDraftResult {

    private final String submissionId;
    private final String status;
    private final String stage;
    private final BigDecimal totalScore;

    public SaveDraftResult(String submissionId, String status, String stage, BigDecimal totalScore) {
        this.submissionId = submissionId;
        this.status = status;
        this.stage = stage;
        this.totalScore = totalScore;
    }

    public String getSubmissionId() { return submissionId; }
    public String getStatus() { return status; }
    public String getStage() { return stage; }
    public BigDecimal getTotalScore() { return totalScore; }
}
