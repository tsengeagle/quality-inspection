package com.tzuchi.his.quality.inspection.application.reviewcase;

/**
 * Result of submitting a rereview. 不含 finalScore：回應複審醫師時不得回傳可反推初審分數的資訊（D-033、D-053 第 2 點）。
 */
public class SubmitRereviewResult {

    private final String submissionId;
    private final String status;
    private final String caseId;
    private final String caseStatus;

    public SubmitRereviewResult(String submissionId, String status,
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
