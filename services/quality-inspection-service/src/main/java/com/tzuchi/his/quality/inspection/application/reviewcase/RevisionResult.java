package com.tzuchi.his.quality.inspection.application.reviewcase;

import java.math.BigDecimal;

/**
 * Result of a revision operation — returned to the caller with new submission details.
 */
public class RevisionResult {

    private final String newSubmissionId;
    private final String oldSubmissionId;
    private final int revisionNo;
    private final BigDecimal totalScore;
    private final String status;
    private final String stage;
    private final String caseId;

    public RevisionResult(String newSubmissionId, String oldSubmissionId,
                          int revisionNo, BigDecimal totalScore,
                          String status, String stage, String caseId) {
        this.newSubmissionId = newSubmissionId;
        this.oldSubmissionId = oldSubmissionId;
        this.revisionNo = revisionNo;
        this.totalScore = totalScore;
        this.status = status;
        this.stage = stage;
        this.caseId = caseId;
    }

    public String getNewSubmissionId() { return newSubmissionId; }
    public String getOldSubmissionId() { return oldSubmissionId; }
    public int getRevisionNo() { return revisionNo; }
    public BigDecimal getTotalScore() { return totalScore; }
    public String getStatus() { return status; }
    public String getStage() { return stage; }
    public String getCaseId() { return caseId; }
}