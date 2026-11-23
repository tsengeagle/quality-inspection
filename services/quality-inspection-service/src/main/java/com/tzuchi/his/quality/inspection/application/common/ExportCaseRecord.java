package com.tzuchi.his.quality.inspection.application.common;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * D-057：匯出用的案件唯讀資料（逐案資料列的來源）。放在 application.common，
 * 因為 reporting 只能依賴 application.common（ARCH-05、D-048），不得直接依賴 persistence entity。
 */
public final class ExportCaseRecord {

    private final String caseId;
    private final String projectId;
    private final String hospitalId;
    private final String status;
    private final String reviewerId;
    private final String rereviewerId;
    private final BigDecimal initialScore;
    private final BigDecimal finalScore;
    private final Instant createdAt;
    private final Instant closedAt;

    public ExportCaseRecord(String caseId, String projectId, String hospitalId, String status,
                            String reviewerId, String rereviewerId, BigDecimal initialScore,
                            BigDecimal finalScore, Instant createdAt, Instant closedAt) {
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
}
