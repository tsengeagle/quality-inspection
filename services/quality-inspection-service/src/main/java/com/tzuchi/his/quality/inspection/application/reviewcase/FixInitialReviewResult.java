package com.tzuchi.his.quality.inspection.application.reviewcase;

import java.time.Instant;

/** D-054 第 4 點：{ caseId, caseStatus, submissionId, fixedAt }。不回傳品質線。 */
public class FixInitialReviewResult {

    private final String caseId;
    private final String caseStatus;
    private final String submissionId;
    private final Instant fixedAt;

    public FixInitialReviewResult(String caseId, String caseStatus, String submissionId, Instant fixedAt) {
        this.caseId = caseId;
        this.caseStatus = caseStatus;
        this.submissionId = submissionId;
        this.fixedAt = fixedAt;
    }

    public String getCaseId() { return caseId; }
    public String getCaseStatus() { return caseStatus; }
    public String getSubmissionId() { return submissionId; }
    public Instant getFixedAt() { return fixedAt; }
}
