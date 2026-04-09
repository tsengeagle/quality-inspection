package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import java.math.BigDecimal;

public class RevisionResultResponse {

    private String newSubmissionId;
    private int revisionNo;
    private BigDecimal totalScore;
    private String status;

    public RevisionResultResponse() {}

    public RevisionResultResponse(String newSubmissionId, int revisionNo,
                                   BigDecimal totalScore, String status) {
        this.newSubmissionId = newSubmissionId;
        this.revisionNo = revisionNo;
        this.totalScore = totalScore;
        this.status = status;
    }

    public String getNewSubmissionId() { return newSubmissionId; }
    public void setNewSubmissionId(String newSubmissionId) { this.newSubmissionId = newSubmissionId; }
    public int getRevisionNo() { return revisionNo; }
    public void setRevisionNo(int revisionNo) { this.revisionNo = revisionNo; }
    public BigDecimal getTotalScore() { return totalScore; }
    public void setTotalScore(BigDecimal totalScore) { this.totalScore = totalScore; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}