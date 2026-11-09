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
    private final String latestInitialSubmissionId;
    private final String scoreSheetVersionRef;
    private final List<SubmissionSummaryItem> submissions;
    private final String latestRereviewSubmissionId;
    private final String projectionType;
    private final String actorRole;
    private final boolean isOwner;
    private final InitialReviewBootstrapView initialReviewContext;
    private final RereviewBootstrapView rereviewContext;

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
        this(caseId, projectId, batchId, hospitalId, status, reviewerId, rereviewerId,
                medicalRecordType, sourceCaseKey, initialReviewScore, finalScore,
                finalConclusion, createdAt, assignedAt, dueAt, closedAt, cancelledAt,
                null, null, submissions, latestRereviewSubmissionId,
                projectionType, actorRole, isOwner, null, null);
    }

    public ReviewCaseDetailView(String caseId, String projectId, String batchId,
                                 String hospitalId, String status, String reviewerId,
                                 String rereviewerId, String medicalRecordType,
                                 String sourceCaseKey, BigDecimal initialReviewScore,
                                 BigDecimal finalScore, String finalConclusion,
                                 LocalDateTime createdAt, LocalDateTime assignedAt,
                                 LocalDateTime dueAt, LocalDateTime closedAt,
                                 LocalDateTime cancelledAt,
                                 String latestInitialSubmissionId,
                                 String scoreSheetVersionRef,
                                 List<SubmissionSummaryItem> submissions,
                                 String latestRereviewSubmissionId,
                                 String projectionType, String actorRole, boolean isOwner,
                                 InitialReviewBootstrapView initialReviewContext,
                                 RereviewBootstrapView rereviewContext) {
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
        this.latestInitialSubmissionId = latestInitialSubmissionId;
        this.scoreSheetVersionRef = scoreSheetVersionRef;
        this.submissions = submissions;
        this.latestRereviewSubmissionId = latestRereviewSubmissionId;
        this.projectionType = projectionType;
        this.actorRole = actorRole;
        this.isOwner = isOwner;
        this.initialReviewContext = initialReviewContext;
        this.rereviewContext = rereviewContext;
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
    public String getLatestInitialSubmissionId() { return latestInitialSubmissionId; }
    public String getScoreSheetVersionRef() { return scoreSheetVersionRef; }
    public List<SubmissionSummaryItem> getSubmissions() { return submissions; }
    public String getLatestRereviewSubmissionId() { return latestRereviewSubmissionId; }
    public String getProjectionType() { return projectionType; }
    public String getActorRole() { return actorRole; }
    public boolean isOwner() { return isOwner; }
    public InitialReviewBootstrapView getInitialReviewContext() { return initialReviewContext; }
    public RereviewBootstrapView getRereviewContext() { return rereviewContext; }

    public ReviewCaseDetailView withRestrictedContent(String actorId, boolean metadataOnly) {
        List<SubmissionSummaryItem> visibleSubmissions = this.submissions.stream()
                .map(s -> new SubmissionSummaryItem(s.getSubmissionId(), s.getStage(), s.getReviewerId(),
                        s.getRevisionNo(), s.getStatus(),
                        !metadataOnly && "REREVIEW".equals(s.getStage()) && actorId.equals(s.getReviewerId())
                                ? s.getTotalScore() : null,
                        s.getSubmittedAt()))
                .collect(java.util.stream.Collectors.toList());
        return new ReviewCaseDetailView(caseId, projectId, batchId, hospitalId, status, reviewerId,
                rereviewerId, metadataOnly ? null : medicalRecordType, metadataOnly ? null : sourceCaseKey,
                null, null, null, createdAt, assignedAt, dueAt, closedAt, cancelledAt,
                latestInitialSubmissionId, scoreSheetVersionRef, visibleSubmissions, latestRereviewSubmissionId,
                projectionType, actorRole, isOwner, null, null);
    }

    public ReviewCaseDetailView enrich(String projectionType, String actorRole, boolean isOwner,
                                       InitialReviewBootstrapView initialReviewContext,
                                       RereviewBootstrapView rereviewContext) {
        return new ReviewCaseDetailView(
                this.caseId, this.projectId, this.batchId, this.hospitalId,
                this.status, this.reviewerId, this.rereviewerId,
                this.medicalRecordType, this.sourceCaseKey,
                this.initialReviewScore, this.finalScore, this.finalConclusion,
                this.createdAt, this.assignedAt, this.dueAt,
                this.closedAt, this.cancelledAt,
                this.latestInitialSubmissionId, this.scoreSheetVersionRef,
                this.submissions,
                this.latestRereviewSubmissionId,
                projectionType, actorRole, isOwner,
                initialReviewContext, rereviewContext);
    }
}
