package com.tzuchi.his.quality.inspection.reporting;

import java.math.BigDecimal;
import java.time.Instant;
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
    /** D-057：案件建立時間（UTC）。 */
    private final Instant createdAt;
    private final Instant closedAt;
    /** D-055 第 6 點：遮罩為 null 的欄位名稱；沒有遮罩時為空清單。 */
    private final List<String> maskedFields;

    public ExportRow(String caseId, String projectId, String hospitalId, String status,
                      String reviewerId, String rereviewerId, BigDecimal initialScore,
                      BigDecimal finalScore, Instant createdAt, Instant closedAt) {
        this(caseId, projectId, hospitalId, status, reviewerId, rereviewerId, initialScore, finalScore,
                createdAt, closedAt, List.of());
    }

    public ExportRow(String caseId, String projectId, String hospitalId, String status,
                      String reviewerId, String rereviewerId, BigDecimal initialScore,
                      BigDecimal finalScore, Instant createdAt, Instant closedAt,
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
    public Instant getCreatedAt() { return createdAt; }
    public Instant getClosedAt() { return closedAt; }
    public List<String> getMaskedFields() { return maskedFields; }
}
