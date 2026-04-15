package com.tzuchi.his.quality.inspection.application.reviewcase.query;

import java.time.LocalDateTime;

/** Read model for a submission reference used by review page bootstrap contexts. */
public class ReviewSubmissionRefView {

    private final String submissionId;
    private final int revisionNo;
    private final String status;
    private final LocalDateTime submittedAt;

    public ReviewSubmissionRefView(String submissionId, int revisionNo, String status,
                                   LocalDateTime submittedAt) {
        this.submissionId = submissionId;
        this.revisionNo = revisionNo;
        this.status = status;
        this.submittedAt = submittedAt;
    }

    public String getSubmissionId() { return submissionId; }
    public int getRevisionNo() { return revisionNo; }
    public String getStatus() { return status; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
}
