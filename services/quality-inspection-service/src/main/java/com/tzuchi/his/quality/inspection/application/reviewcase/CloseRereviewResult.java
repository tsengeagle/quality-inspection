package com.tzuchi.his.quality.inspection.application.reviewcase;

/**
 * 複審結案的結果（D-053 第 2 點）：{@code caseId}、{@code caseStatus}、{@code submissionId}。
 * 刻意不含 finalScore 或任何可反推初審分數的資訊（D-033）。
 */
public class CloseRereviewResult {

    private final String caseId;
    private final String caseStatus;
    private final String submissionId;

    public CloseRereviewResult(String caseId, String caseStatus, String submissionId) {
        this.caseId = caseId;
        this.caseStatus = caseStatus;
        this.submissionId = submissionId;
    }

    public String getCaseId() { return caseId; }
    public String getCaseStatus() { return caseStatus; }
    public String getSubmissionId() { return submissionId; }
}
