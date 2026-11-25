package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import java.math.BigDecimal;

public class SubmitInitialReviewResponse {

    private final String submissionId;
    private final String status;
    private final String caseId;
    private final String caseStatus;
    private final BigDecimal totalScore;

    public SubmitInitialReviewResponse(String submissionId, String status,
                                        String caseId, String caseStatus,
                                        BigDecimal totalScore) {
        this.submissionId = submissionId;
        this.status = status;
        this.caseId = caseId;
        this.caseStatus = caseStatus;
        this.totalScore = totalScore;
    }

    public String getSubmissionId() { return submissionId; }
    public String getStatus() { return status; }
    public String getCaseId() { return caseId; }
    public String getCaseStatus() { return caseStatus; }
    public BigDecimal getTotalScore() { return totalScore; }
}
