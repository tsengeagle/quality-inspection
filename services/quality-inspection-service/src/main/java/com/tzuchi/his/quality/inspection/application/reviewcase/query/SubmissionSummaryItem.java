package com.tzuchi.his.quality.inspection.application.reviewcase.query;

import java.time.LocalDateTime;

public class SubmissionSummaryItem {

    private final String submissionId;
    private final String stage;
    private final int revisionNo;
    private final String status;
    private final LocalDateTime submittedAt;

    public SubmissionSummaryItem(String submissionId, String stage, int revisionNo,
                                  String status, LocalDateTime submittedAt) {
        this.submissionId = submissionId;
        this.stage = stage;
        this.revisionNo = revisionNo;
        this.status = status;
        this.submittedAt = submittedAt;
    }

    public String getSubmissionId() { return submissionId; }
    public String getStage() { return stage; }
    public int getRevisionNo() { return revisionNo; }
    public String getStatus() { return status; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
}
