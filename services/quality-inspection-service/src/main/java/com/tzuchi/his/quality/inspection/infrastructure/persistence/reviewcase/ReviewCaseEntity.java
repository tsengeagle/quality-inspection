package com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase;

import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "review_case")
public class ReviewCaseEntity {

    @Id
    @Column(name = "case_id")
    private String caseId;

    /** D-045：樂觀鎖。由 Hibernate 維護，程式不得自行設定。 */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /** D-037：案件層級的最後更新時間（UTC）。由 ReviewCaseRepositoryImpl 維護；legacy 為 null。 */
    @Column(name = "updated_at")
    private Instant updatedAt;

    /** D-054：初審固定的時間（UTC）、固定者、固定的提交；尚未固定時皆為 null。 */
    @Column(name = "initial_fixed_at")
    private Instant initialFixedAt;

    @Column(name = "initial_fixed_by")
    private String initialFixedBy;

    @Column(name = "initial_fixed_submission_id")
    private String initialFixedSubmissionId;

    @Column(name = "batch_id", nullable = false)
    private String batchId;

    @Column(name = "project_id", nullable = false)
    private String projectId;

    @Column(name = "hospital_id")
    private String hospitalId;

    @Column(name = "source_case_key")
    private String sourceCaseKey;

    @Column(name = "medical_record_type")
    private String medicalRecordType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CaseStatus status;

    // --- B3a: assignment ---

    @Column(name = "reviewer_id")
    private String reviewerId;

    @Column(name = "reviewer_assigned_at")
    private LocalDateTime reviewerAssignedAt;

    @Column(name = "due_at")
    private LocalDateTime dueAt;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Column(name = "return_reason")
    private String returnReason;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    // --- B3b/B3c reserved ---

    @Column(name = "rereview_required")
    private boolean rereviewRequired;

    @Column(name = "final_score", precision = 10, scale = 2)
    private BigDecimal finalScore;

    @Column(name = "final_conclusion")
    private String finalConclusion;

    @Column(name = "latest_initial_submission_id")
    private String latestInitialSubmissionId;

    @Column(name = "rule_snapshot_ref")
    private String ruleSnapshotRef;

    @Column(name = "score_sheet_version_ref")
    private String scoreSheetVersionRef;

    // --- B4: rereview ---
    @Column(name = "rereviewer_id")
    private String rereviewerId;

    @Column(name = "rereviewer_assigned_at")
    private LocalDateTime rereviewerAssignedAt;

    @Column(name = "rereviewer_due_at")
    private LocalDateTime rereviewerDueAt;

    @Column(name = "initial_review_score", precision = 10, scale = 2)
    private BigDecimal initialReviewScore;

    @Column(name = "latest_rereview_submission_id")
    private String latestRereviewSubmissionId;

    public ReviewCaseEntity() {}

    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }

    public String getSourceCaseKey() { return sourceCaseKey; }
    public void setSourceCaseKey(String sourceCaseKey) { this.sourceCaseKey = sourceCaseKey; }

    public String getMedicalRecordType() { return medicalRecordType; }
    public void setMedicalRecordType(String medicalRecordType) { this.medicalRecordType = medicalRecordType; }

    public CaseStatus getStatus() { return status; }
    public void setStatus(CaseStatus status) { this.status = status; }

    public String getReviewerId() { return reviewerId; }
    public void setReviewerId(String reviewerId) { this.reviewerId = reviewerId; }

    public LocalDateTime getReviewerAssignedAt() { return reviewerAssignedAt; }
    public void setReviewerAssignedAt(LocalDateTime reviewerAssignedAt) { this.reviewerAssignedAt = reviewerAssignedAt; }

    public LocalDateTime getDueAt() { return dueAt; }
    public void setDueAt(LocalDateTime dueAt) { this.dueAt = dueAt; }

    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }

    public String getReturnReason() { return returnReason; }
    public void setReturnReason(String returnReason) { this.returnReason = returnReason; }

    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }

    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }

    public boolean isRereviewRequired() { return rereviewRequired; }
    public void setRereviewRequired(boolean rereviewRequired) { this.rereviewRequired = rereviewRequired; }

    public BigDecimal getFinalScore() { return finalScore; }
    public void setFinalScore(BigDecimal finalScore) { this.finalScore = finalScore; }

    public String getFinalConclusion() { return finalConclusion; }
    public void setFinalConclusion(String finalConclusion) { this.finalConclusion = finalConclusion; }

    public String getLatestInitialSubmissionId() { return latestInitialSubmissionId; }
    public void setLatestInitialSubmissionId(String latestInitialSubmissionId) { this.latestInitialSubmissionId = latestInitialSubmissionId; }

    public String getRuleSnapshotRef() { return ruleSnapshotRef; }
    public void setRuleSnapshotRef(String ruleSnapshotRef) { this.ruleSnapshotRef = ruleSnapshotRef; }

    public String getScoreSheetVersionRef() { return scoreSheetVersionRef; }
    public void setScoreSheetVersionRef(String scoreSheetVersionRef) { this.scoreSheetVersionRef = scoreSheetVersionRef; }

    public String getRereviewerId() { return rereviewerId; }
    public void setRereviewerId(String rereviewerId) { this.rereviewerId = rereviewerId; }

    public LocalDateTime getRereviewerAssignedAt() { return rereviewerAssignedAt; }
    public void setRereviewerAssignedAt(LocalDateTime rereviewerAssignedAt) { this.rereviewerAssignedAt = rereviewerAssignedAt; }

    public LocalDateTime getRereviewerDueAt() { return rereviewerDueAt; }
    public void setRereviewerDueAt(LocalDateTime rereviewerDueAt) { this.rereviewerDueAt = rereviewerDueAt; }

    public BigDecimal getInitialReviewScore() { return initialReviewScore; }
    public void setInitialReviewScore(BigDecimal initialReviewScore) { this.initialReviewScore = initialReviewScore; }

    public String getLatestRereviewSubmissionId() { return latestRereviewSubmissionId; }
    public void setLatestRereviewSubmissionId(String id) { this.latestRereviewSubmissionId = id; }

    public Long getVersion() { return version; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Instant getInitialFixedAt() { return initialFixedAt; }
    public void setInitialFixedAt(Instant initialFixedAt) { this.initialFixedAt = initialFixedAt; }
    public String getInitialFixedBy() { return initialFixedBy; }
    public void setInitialFixedBy(String initialFixedBy) { this.initialFixedBy = initialFixedBy; }
    public String getInitialFixedSubmissionId() { return initialFixedSubmissionId; }
    public void setInitialFixedSubmissionId(String id) { this.initialFixedSubmissionId = id; }
}
