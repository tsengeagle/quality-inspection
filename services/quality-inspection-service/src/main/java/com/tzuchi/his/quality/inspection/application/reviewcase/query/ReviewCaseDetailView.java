package com.tzuchi.his.quality.inspection.application.reviewcase.query;

import java.math.BigDecimal;
import java.time.Instant;
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
    private final Instant createdAt;
    private final Instant assignedAt;
    private final Instant dueAt;
    private final Instant closedAt;
    private final Instant cancelledAt;
    private final String latestInitialSubmissionId;
    private final String scoreSheetVersionRef;
    private final List<SubmissionSummaryItem> submissions;
    private final String latestRereviewSubmissionId;
    private final String projectionType;
    private final String actorRole;
    private final boolean isOwner;
    private final InitialReviewBootstrapView initialReviewContext;
    private final RereviewBootstrapView rereviewContext;

    private java.time.Instant updatedAt;
    private boolean initialReviewFixed;

    public ReviewCaseDetailView(String caseId, String projectId, String batchId,
                                 String hospitalId, String status, String reviewerId,
                                 String rereviewerId, String medicalRecordType,
                                 String sourceCaseKey, BigDecimal initialReviewScore,
                                 BigDecimal finalScore,
                                 Instant createdAt, Instant assignedAt,
                                 Instant dueAt, Instant closedAt,
                                 Instant cancelledAt,
                                 List<SubmissionSummaryItem> submissions,
                                 String latestRereviewSubmissionId,
                                 String projectionType, String actorRole, boolean isOwner) {
        this(caseId, projectId, batchId, hospitalId, status, reviewerId, rereviewerId,
                medicalRecordType, sourceCaseKey, initialReviewScore, finalScore,
                createdAt, assignedAt, dueAt, closedAt, cancelledAt,
                null, null, submissions, latestRereviewSubmissionId,
                projectionType, actorRole, isOwner, null, null);
    }

    public ReviewCaseDetailView(String caseId, String projectId, String batchId,
                                 String hospitalId, String status, String reviewerId,
                                 String rereviewerId, String medicalRecordType,
                                 String sourceCaseKey, BigDecimal initialReviewScore,
                                 BigDecimal finalScore,
                                 Instant createdAt, Instant assignedAt,
                                 Instant dueAt, Instant closedAt,
                                 Instant cancelledAt,
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

    /**
     * D-037：案件的持久化 updatedAt（UTC）。只用於 SYSTEM_ADMIN 的 metadata 投影（D-036 八欄），
     * 完整明細的回應格式不變，所以不輸出到 JSON。由查詢 adapter 在建立 view 時設定。
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public java.time.Instant getUpdatedAt() { return updatedAt; }

    public ReviewCaseDetailView withUpdatedAt(java.time.Instant updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    /** D-054：初審是否已固定（決定 FIX_INITIAL_REVIEW 與初審 revision 是否可用）。不輸出到 JSON。 */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isInitialReviewFixed() { return initialReviewFixed; }

    public ReviewCaseDetailView withInitialReviewFixed(boolean initialReviewFixed) {
        this.initialReviewFixed = initialReviewFixed;
        return this;
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
    public Instant getCreatedAt() { return createdAt; }
    public Instant getAssignedAt() { return assignedAt; }
    public Instant getDueAt() { return dueAt; }
    public Instant getClosedAt() { return closedAt; }
    public Instant getCancelledAt() { return cancelledAt; }
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
                null, null, createdAt, assignedAt, dueAt, closedAt, cancelledAt,
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
                this.initialReviewScore, this.finalScore,
                this.createdAt, this.assignedAt, this.dueAt,
                this.closedAt, this.cancelledAt,
                this.latestInitialSubmissionId, this.scoreSheetVersionRef,
                this.submissions,
                this.latestRereviewSubmissionId,
                projectionType, actorRole, isOwner,
                initialReviewContext, rereviewContext);
    }
}
