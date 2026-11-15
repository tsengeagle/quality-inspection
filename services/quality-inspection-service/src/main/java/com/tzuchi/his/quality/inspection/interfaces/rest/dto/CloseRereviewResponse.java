package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

/**
 * 複審結案的回應（D-053 第 2 點）。不含 finalScore（D-033）。
 */
public class CloseRereviewResponse {

    private final String caseId;
    private final String caseStatus;
    private final String submissionId;

    public CloseRereviewResponse(String caseId, String caseStatus, String submissionId) {
        this.caseId = caseId;
        this.caseStatus = caseStatus;
        this.submissionId = submissionId;
    }

    public String getCaseId() { return caseId; }
    public String getCaseStatus() { return caseStatus; }
    public String getSubmissionId() { return submissionId; }
}
