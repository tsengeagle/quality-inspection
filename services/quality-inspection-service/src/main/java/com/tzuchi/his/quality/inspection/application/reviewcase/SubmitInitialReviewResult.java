package com.tzuchi.his.quality.inspection.application.reviewcase;

import java.math.BigDecimal;

/**
 * Result of submitting an initial review.
 */
public class SubmitInitialReviewResult {

    private final String submissionId;
    private final String status;
    private final String caseId;
    private final String caseStatus;
    private final BigDecimal totalScore;
    private final boolean rereviewRequired;

    public SubmitInitialReviewResult(String submissionId, String status,
                                      String caseId, String caseStatus,
                                      BigDecimal totalScore, boolean rereviewRequired) {
        this.submissionId = submissionId;
        this.status = status;
        this.caseId = caseId;
        this.caseStatus = caseStatus;
        this.totalScore = totalScore;
        this.rereviewRequired = rereviewRequired;
    }

    public String getSubmissionId() { return submissionId; }
    public String getStatus() { return status; }
    public String getCaseId() { return caseId; }
    public String getCaseStatus() { return caseStatus; }
    public BigDecimal getTotalScore() { return totalScore; }
    public boolean isRereviewRequired() { return rereviewRequired; }
}
