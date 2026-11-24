package com.tzuchi.his.quality.inspection.application.reviewcase;

/** RC-STI 的輸出 { submissionId }；退回後重開時是沿用的原提交（D-080 第 3 點）。 */
public class StartInitialReviewResult {

    private final String submissionId;

    public StartInitialReviewResult(String submissionId) {
        this.submissionId = submissionId;
    }

    public String getSubmissionId() { return submissionId; }
}
