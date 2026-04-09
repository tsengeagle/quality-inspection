package com.tzuchi.his.quality.inspection.application.reviewcase;

import java.math.BigDecimal;

/**
 * Result of a revision operation — returned to the caller with new submission details.
 */
public class RevisionResult {

    private final String newSubmissionId;
    private final int revisionNo;
    private final BigDecimal totalScore;
    private final String status;

    public RevisionResult(String newSubmissionId, int revisionNo,
                          BigDecimal totalScore, String status) {
        this.newSubmissionId = newSubmissionId;
        this.revisionNo = revisionNo;
        this.totalScore = totalScore;
        this.status = status;
    }

    public String getNewSubmissionId() { return newSubmissionId; }
    public int getRevisionNo() { return revisionNo; }
    public BigDecimal getTotalScore() { return totalScore; }
    public String getStatus() { return status; }
}