package com.tzuchi.his.quality.inspection.application.reviewcase.query;

import java.time.LocalDateTime;

/** Read model for assignment-specific case snapshot data. */
public class AssignmentCaseSnapshotView {

    private final String caseId;
    private final String projectId;
    private final String batchId;
    private final String hospitalId;
    private final String medicalRecordType;
    private final String status;
    private final String reviewerId;
    private final LocalDateTime reviewerAssignedAt;
    private final LocalDateTime reviewerDueAt;
    private final String rereviewerId;
    private final LocalDateTime rereviewerAssignedAt;
    private final LocalDateTime rereviewerDueAt;

    public AssignmentCaseSnapshotView(String caseId, String projectId, String batchId,
                                      String hospitalId, String medicalRecordType,
                                      String status, String reviewerId,
                                      LocalDateTime reviewerAssignedAt,
                                      LocalDateTime reviewerDueAt,
                                      String rereviewerId,
                                      LocalDateTime rereviewerAssignedAt,
                                      LocalDateTime rereviewerDueAt) {
        this.caseId = caseId;
        this.projectId = projectId;
        this.batchId = batchId;
        this.hospitalId = hospitalId;
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
    public String getBatchId() { return batchId; }
    public String getHospitalId() { return hospitalId; }
    public String getMedicalRecordType() { return medicalRecordType; }
    public String getStatus() { return status; }
    public String getReviewerId() { return reviewerId; }
    public LocalDateTime getReviewerAssignedAt() { return reviewerAssignedAt; }
    public LocalDateTime getReviewerDueAt() { return reviewerDueAt; }
    public String getRereviewerId() { return rereviewerId; }
    public LocalDateTime getRereviewerAssignedAt() { return rereviewerAssignedAt; }
    public LocalDateTime getRereviewerDueAt() { return rereviewerDueAt; }
}
