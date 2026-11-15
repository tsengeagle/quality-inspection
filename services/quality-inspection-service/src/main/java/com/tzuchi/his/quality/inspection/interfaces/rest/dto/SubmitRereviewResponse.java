package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

/** 不含 finalScore：回應複審醫師時不得回傳可反推初審分數的資訊（D-033、D-053）。 */
public class SubmitRereviewResponse {

    private final String submissionId;
    private final String status;
    private final String caseId;
    private final String caseStatus;

    public SubmitRereviewResponse(String submissionId, String status,
                                    String caseId, String caseStatus) {
        this.submissionId = submissionId;
        this.status = status;
        this.caseId = caseId;
        this.caseStatus = caseStatus;
    }

    public String getSubmissionId() { return submissionId; }
    public String getStatus() { return status; }
    public String getCaseId() { return caseId; }
    public String getCaseStatus() { return caseStatus; }
}
