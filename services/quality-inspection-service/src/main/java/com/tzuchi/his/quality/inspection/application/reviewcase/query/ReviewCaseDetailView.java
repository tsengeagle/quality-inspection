package com.tzuchi.his.quality.inspection.application.reviewcase.query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ReviewCaseDetailView {

    private final String caseId;
    private final String projectId;
    private final String batchId;
    private final String hospitalId;
    private final String status;
    private final String reviewerId;
    private final String rereviewerId;
    private final String medicalRecordType;
    private final String sourceCaseKey;
    private final BigDecimal initialReviewScore;
    private final BigDecimal finalScore;
    private final String finalConclusion;
    private final LocalDateTime createdAt;
    private final LocalDateTime assignedAt;
    private final LocalDateTime dueAt;
    private final LocalDateTime closedAt;
    private final LocalDateTime cancelledAt;
    private final List<SubmissionSummaryItem> submissions;
    private final String latestRereviewSubmissionId;
    private final String projectionType;
    private final String actorRole;
    private final boolean isOwner;

    public ReviewCaseDetailView(String caseId, String projectId, String batchId,
                                 String hospitalId, String status, String reviewerId,
                                 String rereviewerId, String medicalRecordType,
                                 String sourceCaseKey, BigDecimal initialReviewScore,
                                 BigDecimal finalScore, String finalConclusion,
                                 LocalDateTime createdAt, LocalDateTime assignedAt,
                                 LocalDateTime dueAt, LocalDateTime closedAt,
                                 LocalDateTime cancelledAt,
                                 List<SubmissionSummaryItem> submissions,
                                 String latestRereviewSubmissionId,
                                 String projectionType, String actorRole, boolean isOwner) {
        this.caseId = caseId;
        this.projectId = projectId;
        this.batchId = batchId;
        this.hospitalId = hospitalId;
        this.status = status;
        this.reviewerId = reviewerId;
        this.rereviewerId = rereviewerId;
        this.medicalRecordType = medicalRecordType;
        this.sourceCaseKey = sourceCaseKey;
        this.initialReviewScore = initialReviewScore;
        this.finalScore = finalScore;
        this.finalConclusion = finalConclusion;
        this.createdAt = createdAt;
        this.assignedAt = assignedAt;
        this.dueAt = dueAt;
        this.closedAt = closedAt;
        this.cancelledAt = cancelledAt;
        this.submissions = submissions;
        this.latestRereviewSubmissionId = latestRereviewSubmissionId;
        this.projectionType = projectionType;
        this.actorRole = actorRole;
        this.isOwner = isOwner;
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
    public BigDecimal getInitialReviewScore() { return initialReviewScore; }
    public BigDecimal getFinalScore() { return finalScore; }
    public String getFinalConclusion() { return finalConclusion; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
    public LocalDateTime getDueAt() { return dueAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public List<SubmissionSummaryItem> getSubmissions() { return submissions; }
    public String getLatestRereviewSubmissionId() { return latestRereviewSubmissionId; }
    public String getProjectionType() { return projectionType; }
    public String getActorRole() { return actorRole; }
    public boolean isOwner() { return isOwner; }

    public ReviewCaseDetailView enrich(String projectionType, String actorRole, boolean isOwner) {
        return new ReviewCaseDetailView(
                this.caseId, this.projectId, this.batchId, this.hospitalId,
                this.status, this.reviewerId, this.rereviewerId,
                this.medicalRecordType, this.sourceCaseKey,
                this.initialReviewScore, this.finalScore, this.finalConclusion,
                this.createdAt, this.assignedAt, this.dueAt,
                this.closedAt, this.cancelledAt, this.submissions,
                this.latestRereviewSubmissionId,
                projectionType, actorRole, isOwner);
    }
}
