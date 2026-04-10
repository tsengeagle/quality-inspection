package com.tzuchi.his.quality.inspection.domain.reviewcase;

import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Aggregate root for ReviewCase.
 * <p>
 * Created from sampling selection. Starts at SELECTED_PENDING_ASSIGNMENT.
 * B3a adds assignment lifecycle: assign, reassign, cancel, return, reopen.
 */
public class ReviewCase {

    // --- Existing fields (B2) ---
    private String caseId;
    private String batchId;
    private String projectId;
    private String hospitalId;
    private String sourceCaseKey;
    private MedicalRecordType medicalRecordType;
    private CaseStatus status;

    // --- B3a: assignment ---
    private ReviewerAssignment reviewerAssignment;
    private LocalDateTime dueAt;
    private String cancelReason;
    private String returnReason;
    private LocalDateTime cancelledAt;
    private LocalDateTime closedAt;

    // --- B3b/B3c: reserved (nullable, not used in B3a) ---
    private boolean rereviewRequired;
    private BigDecimal finalScore;
    private String finalConclusion;
    private String latestInitialSubmissionId;
    private String ruleSnapshotRef;
    private String scoreSheetVersionRef;

    // --- B4: rereview ---
    private ReviewerAssignment rereviewerAssignment;
    private LocalDateTime rereviewerDueAt;
    private BigDecimal initialReviewScore;

    protected ReviewCase() {
        // for persistence
    }

    private ReviewCase(String caseId, String batchId, String projectId,
                       String hospitalId, String sourceCaseKey,
                       MedicalRecordType medicalRecordType) {
        Objects.requireNonNull(caseId, "caseId must not be null");
        Objects.requireNonNull(batchId, "batchId must not be null");
        Objects.requireNonNull(projectId, "projectId must not be null");
        this.caseId = caseId;
        this.batchId = batchId;
        this.projectId = projectId;
        this.hospitalId = hospitalId;
        this.sourceCaseKey = sourceCaseKey;
        this.medicalRecordType = medicalRecordType;
        this.status = CaseStatus.SELECTED_PENDING_ASSIGNMENT;
    }

    public static ReviewCase createFromSampling(String caseId, String batchId, String projectId,
                                                 String hospitalId, String sourceCaseKey,
                                                 MedicalRecordType medicalRecordType) {
        return new ReviewCase(caseId, batchId, projectId, hospitalId, sourceCaseKey, medicalRecordType);
    }

    /**
     * Reconstitute from persistence. All fields explicitly set — no business validation.
     */
    public static ReviewCase reconstitute(
            String caseId, String batchId, String projectId, String hospitalId,
            String sourceCaseKey, MedicalRecordType medicalRecordType,
            CaseStatus status, ReviewerAssignment reviewerAssignment, LocalDateTime dueAt,
            String cancelReason, String returnReason, LocalDateTime cancelledAt, LocalDateTime closedAt,
            boolean rereviewRequired, BigDecimal finalScore, String finalConclusion,
            String latestInitialSubmissionId, String ruleSnapshotRef, String scoreSheetVersionRef,
            ReviewerAssignment rereviewerAssignment, LocalDateTime rereviewerDueAt,
            BigDecimal initialReviewScore) {
        ReviewCase rc = new ReviewCase();
        rc.caseId = caseId;
        rc.batchId = batchId;
        rc.projectId = projectId;
        rc.hospitalId = hospitalId;
        rc.sourceCaseKey = sourceCaseKey;
        rc.medicalRecordType = medicalRecordType;
        rc.status = status;
        rc.reviewerAssignment = reviewerAssignment;
        rc.dueAt = dueAt;
        rc.cancelReason = cancelReason;
        rc.returnReason = returnReason;
        rc.cancelledAt = cancelledAt;
        rc.closedAt = closedAt;
        rc.rereviewRequired = rereviewRequired;
        rc.finalScore = finalScore;
        rc.finalConclusion = finalConclusion;
        rc.latestInitialSubmissionId = latestInitialSubmissionId;
        rc.ruleSnapshotRef = ruleSnapshotRef;
        rc.scoreSheetVersionRef = scoreSheetVersionRef;
        rc.rereviewerAssignment = rereviewerAssignment;
        rc.rereviewerDueAt = rereviewerDueAt;
        rc.initialReviewScore = initialReviewScore;
        return rc;
    }

    // --- B3a Domain Methods ---

    public void assignReviewer(String reviewerId, LocalDateTime dueAt) {
        if (this.status != CaseStatus.SELECTED_PENDING_ASSIGNMENT) {
            throw new DomainStateConflictException("CASE_NOT_PENDING_ASSIGNMENT",
                    "Cannot assign reviewer: case status is " + this.status);
        }
        Objects.requireNonNull(reviewerId, "reviewerId must not be null");
        this.reviewerAssignment = new ReviewerAssignment(reviewerId, LocalDateTime.now());
        this.dueAt = dueAt;
        this.status = CaseStatus.PENDING_INITIAL_REVIEW;
    }

    public void reassignReviewer(String newReviewerId, LocalDateTime dueAt) {
        if (this.status != CaseStatus.PENDING_INITIAL_REVIEW) {
            throw new DomainStateConflictException("CASE_NOT_PENDING_INITIAL_REVIEW",
                    "Cannot reassign reviewer: case status is " + this.status);
        }
        Objects.requireNonNull(newReviewerId, "newReviewerId must not be null");
        this.reviewerAssignment = new ReviewerAssignment(newReviewerId, LocalDateTime.now());
        this.dueAt = dueAt;
    }

    public void cancel(String reason) {
        if (this.status == CaseStatus.CLOSED || this.status == CaseStatus.CANCELLED) {
            throw new DomainStateConflictException("CASE_TERMINAL",
                    "Cannot cancel: case is in terminal status " + this.status);
        }
        Objects.requireNonNull(reason, "cancel reason must not be null");
        this.cancelReason = reason;
        this.cancelledAt = LocalDateTime.now();
        this.status = CaseStatus.CANCELLED;
    }

    public void returnToInitialReview(String reason) {
        if (this.status != CaseStatus.INITIAL_REVIEW_IN_PROGRESS) {
            throw new DomainStateConflictException("CASE_NOT_IN_PROGRESS",
                    "Cannot return: case status is " + this.status);
        }
        Objects.requireNonNull(reason, "return reason must not be null");
        this.returnReason = reason;
        this.status = CaseStatus.RETURNED;
    }

    public void reopenInitialReview() {
        if (this.status != CaseStatus.RETURNED) {
            throw new DomainStateConflictException("CASE_NOT_RETURNED",
                    "Cannot reopen: case status is " + this.status);
        }
        this.returnReason = null;
        this.status = CaseStatus.PENDING_INITIAL_REVIEW;
    }

    // --- B3b Domain Methods ---

    public void startInitialReview(String submissionId, String scoreSheetVersionRef) {
        if (this.status != CaseStatus.PENDING_INITIAL_REVIEW) {
            throw new DomainStateConflictException("CASE_NOT_PENDING_INITIAL_REVIEW",
                    "Case must be in PENDING_INITIAL_REVIEW to start. Current: " + this.status);
        }
        Objects.requireNonNull(submissionId, "submissionId must not be null");
        Objects.requireNonNull(scoreSheetVersionRef, "scoreSheetVersionRef must not be null");
        this.latestInitialSubmissionId = submissionId;
        this.scoreSheetVersionRef = scoreSheetVersionRef;
        this.status = CaseStatus.INITIAL_REVIEW_IN_PROGRESS;
    }

    public void completeInitialReview(BigDecimal totalScore, BigDecimal qualityThreshold) {
        if (this.status != CaseStatus.INITIAL_REVIEW_IN_PROGRESS) {
            throw new DomainStateConflictException("CASE_NOT_IN_INITIAL_REVIEW",
                    "Case must be in INITIAL_REVIEW_IN_PROGRESS to complete. Current: " + this.status);
        }
        Objects.requireNonNull(totalScore, "totalScore must not be null");
        this.initialReviewScore = totalScore;
        if (qualityThreshold != null && totalScore.compareTo(qualityThreshold) > 0) {
            this.status = CaseStatus.PENDING_REREVIEW;
        } else {
            this.finalScore = totalScore;
            this.status = CaseStatus.INITIAL_REVIEW_COMPLETED;
        }
    }

    /**
     * Guard: reject operations on terminal cases (CLOSED or CANCELLED).
     */
    public void requireNotTerminal() {
        if (this.status == CaseStatus.CLOSED || this.status == CaseStatus.CANCELLED) {
            throw new DomainStateConflictException("CASE_TERMINAL",
                    "Case is in terminal status: " + this.status + ". Revision not allowed.");
        }
    }

    /**
     * Update finalScore after a revision. Does not change case status.
     */
    public void updateFinalScore(BigDecimal newScore) {
        requireNotTerminal();
        Objects.requireNonNull(newScore, "newScore must not be null");
        this.initialReviewScore = newScore;
        this.finalScore = newScore;
    }

    /**
     * Test-only: force status for setting up test scenarios.
     * Package-private — only accessible from tests in the same package.
     */
    void forceStatus(CaseStatus status) {
        this.status = status;
    }

    // --- Getters ---

    public String getCaseId() { return caseId; }
    public String getBatchId() { return batchId; }
    public String getProjectId() { return projectId; }
    public String getHospitalId() { return hospitalId; }
    public String getSourceCaseKey() { return sourceCaseKey; }
    public MedicalRecordType getMedicalRecordType() { return medicalRecordType; }
    public CaseStatus getStatus() { return status; }
    public ReviewerAssignment getReviewerAssignment() { return reviewerAssignment; }
    public LocalDateTime getDueAt() { return dueAt; }
    public String getCancelReason() { return cancelReason; }
    public String getReturnReason() { return returnReason; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public boolean isRereviewRequired() { return rereviewRequired; }
    public BigDecimal getFinalScore() { return finalScore; }
    public String getFinalConclusion() { return finalConclusion; }
    public String getLatestInitialSubmissionId() { return latestInitialSubmissionId; }
    public String getRuleSnapshotRef() { return ruleSnapshotRef; }
    public String getScoreSheetVersionRef() { return scoreSheetVersionRef; }
    public ReviewerAssignment getRereviewerAssignment() { return rereviewerAssignment; }
    public LocalDateTime getRereviewerDueAt() { return rereviewerDueAt; }
    public BigDecimal getInitialReviewScore() { return initialReviewScore; }
}
