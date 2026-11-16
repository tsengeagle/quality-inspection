package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

/** D-054 第 4 點：初審固定的回應。fixedAt 是 UTC 的 ISO 8601（例如 2026-11-16T10:00:00Z）。 */
public class FixInitialReviewResponse {

    private final String caseId;
    private final String caseStatus;
    private final String submissionId;
    private final String fixedAt;

    public FixInitialReviewResponse(String caseId, String caseStatus, String submissionId, String fixedAt) {
        this.caseId = caseId;
        this.caseStatus = caseStatus;
        this.submissionId = submissionId;
        this.fixedAt = fixedAt;
    }

    public String getCaseId() { return caseId; }
    public String getCaseStatus() { return caseStatus; }
    public String getSubmissionId() { return submissionId; }
    public String getFixedAt() { return fixedAt; }
}
