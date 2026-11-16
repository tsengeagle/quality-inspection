package com.tzuchi.his.quality.inspection.reporting;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ExportRow {

    private final String caseId;
    private final String projectId;
    private final String hospitalId;
    private final String status;
    private final String reviewerId;
    private final String rereviewerId;
    private final BigDecimal initialScore;
    private final BigDecimal finalScore;
    private final LocalDateTime createdAt;
    private final LocalDateTime closedAt;
    /** D-055 第 6 點：遮罩為 null 的欄位名稱；沒有遮罩時為空清單。 */
    private final List<String> maskedFields;

    public ExportRow(String caseId, String projectId, String hospitalId, String status,
                      String reviewerId, String rereviewerId, BigDecimal initialScore,
                      BigDecimal finalScore, LocalDateTime createdAt, LocalDateTime closedAt) {
        this(caseId, projectId, hospitalId, status, reviewerId, rereviewerId, initialScore, finalScore,
                createdAt, closedAt, List.of());
    }

    public ExportRow(String caseId, String projectId, String hospitalId, String status,
                      String reviewerId, String rereviewerId, BigDecimal initialScore,
                      BigDecimal finalScore, LocalDateTime createdAt, LocalDateTime closedAt,
                      List<String> maskedFields) {
        this.caseId = caseId;
        this.projectId = projectId;
        this.hospitalId = hospitalId;
        this.status = status;
        this.reviewerId = reviewerId;
        this.rereviewerId = rereviewerId;
        this.initialScore = initialScore;
        this.finalScore = finalScore;
        this.createdAt = createdAt;
        this.closedAt = closedAt;
        this.maskedFields = List.copyOf(maskedFields);
    }

    public String getCaseId() { return caseId; }
    public String getProjectId() { return projectId; }
    public String getHospitalId() { return hospitalId; }
    public String getStatus() { return status; }
    public String getReviewerId() { return reviewerId; }
    public String getRereviewerId() { return rereviewerId; }
    public BigDecimal getInitialScore() { return initialScore; }
    public BigDecimal getFinalScore() { return finalScore; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public List<String> getMaskedFields() { return maskedFields; }
}
