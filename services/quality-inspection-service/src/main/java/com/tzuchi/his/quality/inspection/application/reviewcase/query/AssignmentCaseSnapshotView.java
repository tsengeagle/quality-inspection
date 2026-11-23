package com.tzuchi.his.quality.inspection.application.reviewcase.query;

import java.time.Instant;

/** Read model for assignment-specific case snapshot data. */
public class AssignmentCaseSnapshotView {

    private final String caseId;
    private final String projectId;
    private final String projectName;
    private final String batchId;
    private final String hospitalId;
    private final String sourceCaseKey;
    private final String medicalRecordType;
    private final String status;
    private final String reviewerId;
    private final Instant reviewerAssignedAt;
    private final Instant reviewerDueAt;
    private final String rereviewerId;
    private final Instant rereviewerAssignedAt;
    private final Instant rereviewerDueAt;

    public AssignmentCaseSnapshotView(String caseId, String projectId, String projectName, String batchId,
                                      String hospitalId, String sourceCaseKey, String medicalRecordType,
                                      String status, String reviewerId,
                                      Instant reviewerAssignedAt,
                                      Instant reviewerDueAt,
                                      String rereviewerId,
                                      Instant rereviewerAssignedAt,
                                      Instant rereviewerDueAt) {
        this.caseId = caseId;
        this.projectId = projectId;
        this.projectName = projectName;
        this.batchId = batchId;
        this.hospitalId = hospitalId;
        this.sourceCaseKey = sourceCaseKey;
        this.medicalRecordType = medicalRecordType;
        this.status = status;
        this.reviewerId = reviewerId;
        this.reviewerAssignedAt = reviewerAssignedAt;
        this.reviewerDueAt = reviewerDueAt;
        this.rereviewerId = rereviewerId;
        this.rereviewerAssignedAt = rereviewerAssignedAt;
        this.rereviewerDueAt = rereviewerDueAt;
    }

    public String getCaseId() { return caseId; }
    public String getProjectId() { return projectId; }
    public String getProjectName() { return projectName; }
    public String getBatchId() { return batchId; }
    public String getHospitalId() { return hospitalId; }
    public String getSourceCaseKey() { return sourceCaseKey; }
    public String getMedicalRecordType() { return medicalRecordType; }
    public String getStatus() { return status; }
    public String getReviewerId() { return reviewerId; }
    public Instant getReviewerAssignedAt() { return reviewerAssignedAt; }
    public Instant getReviewerDueAt() { return reviewerDueAt; }
    public String getRereviewerId() { return rereviewerId; }
    public Instant getRereviewerAssignedAt() { return rereviewerAssignedAt; }
    public Instant getRereviewerDueAt() { return rereviewerDueAt; }
}
