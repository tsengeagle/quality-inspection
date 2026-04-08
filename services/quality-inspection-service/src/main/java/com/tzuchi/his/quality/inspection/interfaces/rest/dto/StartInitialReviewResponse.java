package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

public class StartInitialReviewResponse {
    private String submissionId;

    public StartInitialReviewResponse() {}

    public StartInitialReviewResponse(String submissionId) {
        this.submissionId = submissionId;
    }

    public String getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(String submissionId) {
        this.submissionId = submissionId;
    }
}
