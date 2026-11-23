package com.tzuchi.his.quality.inspection.application.reviewcase.query;

import java.time.Instant;
import java.util.List;

/** Read model for UI-011 manager-facing assignment bootstrap context. */
public class AssignmentBootstrapView {

    private final String caseId;
    private final String projectId;
    private final String projectName;
    private final String batchId;
    private final String hospitalId;
    private final String sourceCaseKey;
    private final String medicalRecordType;
    private final String status;
    private final String pageMode;
    private final boolean blocked;
    private final String blockedReason;
    private final List<String> allowedActions;
    private final AssignmentAssigneeSummaryView currentReviewer;
    private final AssignmentAssigneeSummaryView currentRereviewer;
    private final Instant reviewerDueAt;
    private final Instant rereviewerDueAt;
    private final Instant effectiveDueAt;

    public AssignmentBootstrapView(String caseId, String projectId, String projectName, String batchId,
                                   String hospitalId, String sourceCaseKey, String medicalRecordType,
                                   String status, String pageMode, boolean blocked,
                                   String blockedReason, List<String> allowedActions,
                                   AssignmentAssigneeSummaryView currentReviewer,
                                   AssignmentAssigneeSummaryView currentRereviewer,
                                   Instant reviewerDueAt,
                                   Instant rereviewerDueAt,
                                   Instant effectiveDueAt) {
        this.caseId = caseId;
        this.projectId = projectId;
        this.projectName = projectName;
        this.batchId = batchId;
        this.hospitalId = hospitalId;
        this.sourceCaseKey = sourceCaseKey;
        this.medicalRecordType = medicalRecordType;
        this.status = status;
        this.pageMode = pageMode;
        this.blocked = blocked;
        this.blockedReason = blockedReason;
        this.allowedActions = allowedActions;
        this.currentReviewer = currentReviewer;
        this.currentRereviewer = currentRereviewer;
        this.reviewerDueAt = reviewerDueAt;
        this.rereviewerDueAt = rereviewerDueAt;
        this.effectiveDueAt = effectiveDueAt;
    }

    public String getCaseId() { return caseId; }
    public String getProjectId() { return projectId; }
    public String getProjectName() { return projectName; }
    public String getBatchId() { return batchId; }
    public String getHospitalId() { return hospitalId; }
    public String getSourceCaseKey() { return sourceCaseKey; }
    public String getMedicalRecordType() { return medicalRecordType; }
    public String getStatus() { return status; }
    public String getPageMode() { return pageMode; }
    public boolean isBlocked() { return blocked; }
    public String getBlockedReason() { return blockedReason; }
    public List<String> getAllowedActions() { return allowedActions; }
    public AssignmentAssigneeSummaryView getCurrentReviewer() { return currentReviewer; }
    public AssignmentAssigneeSummaryView getCurrentRereviewer() { return currentRereviewer; }
    public Instant getReviewerDueAt() { return reviewerDueAt; }
    public Instant getRereviewerDueAt() { return rereviewerDueAt; }
    public Instant getEffectiveDueAt() { return effectiveDueAt; }
}
