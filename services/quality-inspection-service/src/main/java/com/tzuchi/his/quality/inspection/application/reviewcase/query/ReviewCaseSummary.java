package com.tzuchi.his.quality.inspection.application.reviewcase.query;

import java.time.LocalDateTime;

public class ReviewCaseSummary {

    private final String caseId;
    private final String projectId;
    private final String batchId;
    private final String hospitalId;
    private final String status;
    private final String reviewerId;
    private final String rereviewerId;
    private final String medicalRecordType;
    private final String sourceCaseKey;
    private final LocalDateTime createdAt;
    private final LocalDateTime assignedAt;
    private final LocalDateTime dueAt;

    public ReviewCaseSummary(String caseId, String projectId, String batchId,
                              String hospitalId, String status, String reviewerId,
                              String rereviewerId, String medicalRecordType,
                              String sourceCaseKey, LocalDateTime createdAt,
                              LocalDateTime assignedAt, LocalDateTime dueAt) {
        this.caseId = caseId;
        this.projectId = projectId;
        this.batchId = batchId;
        this.hospitalId = hospitalId;
        this.status = status;
        this.reviewerId = reviewerId;
        this.rereviewerId = rereviewerId;
        this.medicalRecordType = medicalRecordType;
        this.sourceCaseKey = sourceCaseKey;
        this.createdAt = createdAt;
        this.assignedAt = assignedAt;
        this.dueAt = dueAt;
    }

    public String getCaseId() { return caseId; }
    public String getProjectId() { return projectId; }
    public String getBatchId() { return batchId; }
    public String getHospitalId() { return hospitalId; }
    public String getStatus() { return status; }
    public String getReviewerId() { return reviewerId; }
    public String getRereviewerId() { return rereviewerId; }
    public String getMedicalRecordType() { return medicalRecordType; }
    public String getSourceCaseKey() { return sourceCaseKey; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
    public LocalDateTime getDueAt() { return dueAt; }
}
