package com.tzuchi.his.quality.inspection.domain.reviewcase;

import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import com.tzuchi.his.quality.inspection.domain.common.DomainValidationException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;

import java.math.BigDecimal;
import java.time.Instant;
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
    private Instant dueAt;
    private String cancelReason;
    private String returnReason;
    private Instant cancelledAt;
    private Instant closedAt;

    // --- B3b/B3c: reserved (nullable, not used in B3a) ---
    private boolean rereviewRequired;
    private BigDecimal finalScore;
    private String finalConclusion;
    private String latestInitialSubmissionId;
    private String ruleSnapshotRef;
    private String scoreSheetVersionRef;

    // --- B4: rereview ---
    private ReviewerAssignment rereviewerAssignment;
    private Instant rereviewerDueAt;
    private BigDecimal initialReviewScore;
    private String latestRereviewSubmissionId;

    // --- D-054: initial review fixation (null until fixed) ---
    private Instant initialFixedAt;
    private String initialFixedBy;
    private String initialFixedSubmissionId;

    protected ReviewCase() {
        // for persistence
    }

    private ReviewCase(String caseId, String batchId, String projectId,
                       String hospitalId, String sourceCaseKey,
                       MedicalRecordType medicalRecordType) {
        Objects.requireNonNull(caseId, "caseId must not be null");
        Objects.requireNonNull(batchId, "batchId must not be null");
        Objects.requireNonNull(projectId, "projectId must not be null");
        Objects.requireNonNull(hospitalId, "hospitalId must not be null");
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
            CaseStatus status, ReviewerAssignment reviewerAssignment, Instant dueAt,
            String cancelReason, String returnReason, Instant cancelledAt, Instant closedAt,
            boolean rereviewRequired, BigDecimal finalScore, String finalConclusion,
            String latestInitialSubmissionId, String ruleSnapshotRef, String scoreSheetVersionRef,
            ReviewerAssignment rereviewerAssignment, Instant rereviewerDueAt,
            BigDecimal initialReviewScore,
            String latestRereviewSubmissionId) {
        return reconstitute(caseId, batchId, projectId, hospitalId, sourceCaseKey, medicalRecordType,
                status, reviewerAssignment, dueAt, cancelReason, returnReason, cancelledAt, closedAt,
                rereviewRequired, finalScore, finalConclusion, latestInitialSubmissionId, ruleSnapshotRef,
                scoreSheetVersionRef, rereviewerAssignment, rereviewerDueAt, initialReviewScore,
                latestRereviewSubmissionId, null, null, null);
    }

    /** Reconstitute including the D-054 initial review fixation (all three null when not fixed). */
    public static ReviewCase reconstitute(
            String caseId, String batchId, String projectId, String hospitalId,
            String sourceCaseKey, MedicalRecordType medicalRecordType,
            CaseStatus status, ReviewerAssignment reviewerAssignment, Instant dueAt,
            String cancelReason, String returnReason, Instant cancelledAt, Instant closedAt,
            boolean rereviewRequired, BigDecimal finalScore, String finalConclusion,
            String latestInitialSubmissionId, String ruleSnapshotRef, String scoreSheetVersionRef,
            ReviewerAssignment rereviewerAssignment, Instant rereviewerDueAt,
            BigDecimal initialReviewScore,
            String latestRereviewSubmissionId,
            Instant initialFixedAt, String initialFixedBy, String initialFixedSubmissionId) {
        ReviewCase rc = new ReviewCase();
        rc.initialFixedAt = initialFixedAt;
        rc.initialFixedBy = initialFixedBy;
        rc.initialFixedSubmissionId = initialFixedSubmissionId;
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
        rc.latestRereviewSubmissionId = latestRereviewSubmissionId;
        return rc;
    }

    // --- B3a Domain Methods ---

    public void assignReviewer(String reviewerId, Instant dueAt, Instant assignedAt) {
        // D-067 第 2 點：輸入檢查在狀態檢查之前
        if (reviewerId == null || reviewerId.isBlank()) {
            throw new DomainValidationException("VALIDATION_ERROR", "reviewerId must not be blank");
        }
        if (dueAt == null) {
            throw new DomainValidationException("VALIDATION_ERROR", "dueAt must not be null");
        }
        if (this.status != CaseStatus.SELECTED_PENDING_ASSIGNMENT) {
            throw new DomainRuleViolationException("CASE_NOT_ASSIGNABLE",
                    "Cannot assign reviewer: case status is " + this.status);
        }
        Objects.requireNonNull(assignedAt, "assignedAt must not be null");
        this.reviewerAssignment = new ReviewerAssignment(reviewerId, assignedAt);
        this.dueAt = dueAt;
        this.status = CaseStatus.PENDING_INITIAL_REVIEW;
    }

    public void reassignReviewer(String newReviewerId, Instant dueAt, Instant assignedAt) {
        // D-082 第 1、2 點：輸入檢查在狀態檢查之前；狀態不對與指派一致（422）
        if (newReviewerId == null || newReviewerId.isBlank()) {
            throw new DomainValidationException("VALIDATION_ERROR", "reviewerId must not be blank");
        }
        if (dueAt == null) {
            throw new DomainValidationException("VALIDATION_ERROR", "dueAt must not be null");
        }
        if (this.status != CaseStatus.PENDING_INITIAL_REVIEW) {
            throw new DomainRuleViolationException("CASE_NOT_ASSIGNABLE",
                    "Cannot reassign reviewer: case status is " + this.status);
        }
        Objects.requireNonNull(assignedAt, "assignedAt must not be null");
        this.reviewerAssignment = new ReviewerAssignment(newReviewerId, assignedAt);
        this.dueAt = dueAt;
    }

    public void cancel(String reason, Instant cancelledAt) {
        if (this.status == CaseStatus.CLOSED || this.status == CaseStatus.CANCELLED) {
            throw new DomainStateConflictException("CASE_TERMINAL",
                    "Cannot cancel: case is in terminal status " + this.status);
        }
        Objects.requireNonNull(reason, "cancel reason must not be null");
        Objects.requireNonNull(cancelledAt, "cancelledAt must not be null");
        this.cancelReason = reason;
        this.cancelledAt = cancelledAt;
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
            throw new DomainRuleViolationException("CASE_NOT_IN_INITIAL_REVIEW_STAGE",
                    "Case must be in PENDING_INITIAL_REVIEW to start. Current: " + this.status);
        }
        Objects.requireNonNull(submissionId, "submissionId must not be null");
        Objects.requireNonNull(scoreSheetVersionRef, "scoreSheetVersionRef must not be null");
        this.latestInitialSubmissionId = submissionId;
        this.scoreSheetVersionRef = scoreSheetVersionRef;
        this.status = CaseStatus.INITIAL_REVIEW_IN_PROGRESS;
    }

    /**
     * 提交初審：案件一律停在 INITIAL_REVIEW_COMPLETED。是否進複審在固定時才判斷（D-031、D-054 第 3 點）。
     */
    public void completeInitialReview(BigDecimal totalScore) {
        if (this.status != CaseStatus.INITIAL_REVIEW_IN_PROGRESS) {
            throw new DomainStateConflictException("CASE_NOT_IN_INITIAL_REVIEW",
                    "Case must be in INITIAL_REVIEW_IN_PROGRESS to complete. Current: " + this.status);
        }
        Objects.requireNonNull(totalScore, "totalScore must not be null");
        this.initialReviewScore = totalScore;
        this.finalScore = totalScore;
        this.status = CaseStatus.INITIAL_REVIEW_COMPLETED;
    }

    /**
     * 固定初審（D-031、DD-03、D-054）。呼叫端已確認目標是本案目前有效、已提交、由操作者提交的初審結果。
     * 分數 > 品質線就進 PENDING_REREVIEW；≤ 品質線或品質線為 null，留在 INITIAL_REVIEW_COMPLETED。
     *
     * @param fixedScore 被固定那份提交的分數
     */
    public void fixInitialReview(String submissionId, String fixedBy, Instant fixedAt,
                                 BigDecimal fixedScore, BigDecimal qualityThreshold) {
        requireFixable();
        if (!Objects.equals(submissionId, this.latestInitialSubmissionId)) {
            throw new DomainStateConflictException("CONFLICT", "Initial submission is no longer current");
        }
        Objects.requireNonNull(fixedBy, "fixedBy must not be null");
        Objects.requireNonNull(fixedAt, "fixedAt must not be null");
        Objects.requireNonNull(fixedScore, "fixedScore must not be null");
        this.initialFixedAt = fixedAt;
        this.initialFixedBy = fixedBy;
        this.initialFixedSubmissionId = submissionId;
        if (qualityThreshold != null && fixedScore.compareTo(qualityThreshold) > 0) {
            this.rereviewRequired = true;
            // 進複審後還沒有最終分數：保留 initialReviewScore，finalScore 於複審提交時依 D-012／D-020 加權計算。
            this.finalScore = null;
            this.status = CaseStatus.PENDING_REREVIEW;
        }
    }

    /** D-054 第 6 點的順序：已結束 → 已固定 → 不在 INITIAL_REVIEW_COMPLETED。 */
    public void requireFixable() {
        requireNotTerminal();
        if (isInitialReviewFixed()) {
            throw new DomainStateConflictException("INVALID_STATE_TRANSITION", "Initial review is already fixed");
        }
        if (this.status != CaseStatus.INITIAL_REVIEW_COMPLETED) {
            throw new DomainStateConflictException("CASE_NOT_INITIAL_REVIEW_COMPLETED",
                    "Case must be in INITIAL_REVIEW_COMPLETED to fix the initial review. Current: " + this.status);
        }
    }

    public boolean isInitialReviewFixed() {
        return this.initialFixedAt != null;
    }

    private void requireInitialReviewFixed() {
        if (!isInitialReviewFixed()) {
            throw new DomainStateConflictException("INVALID_STATE_TRANSITION",
                    "Initial review must be fixed first (D-031, D-054)");
        }
    }

    // --- B4 Domain Methods ---

    public void closeWithoutRereview(Instant closedAt) {
        if (this.status != CaseStatus.INITIAL_REVIEW_COMPLETED) {
            throw new DomainStateConflictException("CASE_NOT_INITIAL_REVIEW_COMPLETED",
                    "Can only close-without-rereview from INITIAL_REVIEW_COMPLETED. Current: " + this.status);
        }
        requireInitialReviewFixed();
        Objects.requireNonNull(closedAt, "closedAt must not be null");
        this.finalScore = this.initialReviewScore;
        this.closedAt = closedAt;
        this.status = CaseStatus.CLOSED;
    }

    public void sendToRereview() {
        if (this.status != CaseStatus.INITIAL_REVIEW_COMPLETED) {
            throw new DomainStateConflictException("CASE_NOT_INITIAL_REVIEW_COMPLETED",
                    "Can only send-to-rereview from INITIAL_REVIEW_COMPLETED. Current: " + this.status);
        }
        requireInitialReviewFixed();
        this.status = CaseStatus.PENDING_REREVIEW;
    }

    /**
     * 指派複審醫師前的檢查，不改變案件（D-082 第 7 點）。
     * 順序：輸入 → 狀態 → 與初審醫師相同；讀過初審由呼叫端在這之後檢查（D-055 第 5 點）。
     */
    public void checkRereviewerAssignable(String rereviewerId, Instant dueAt) {
        checkRereviewerInput(rereviewerId, dueAt);
        requirePendingRereview("assign");
        requireNotInitialReviewer(rereviewerId);
    }

    /**
     * 改派複審醫師前的檢查，不改變案件（D-082 第 4、7 點）。
     * 順序：輸入 → 狀態 → 尚未指派 → 與初審醫師相同；讀過初審由呼叫端在這之後檢查。
     */
    public void checkRereviewerReassignable(String rereviewerId, Instant dueAt) {
        checkRereviewerInput(rereviewerId, dueAt);
        requirePendingRereview("reassign");
        if (this.rereviewerAssignment == null) {
            throw new DomainRuleViolationException("NO_REREVIEWER_ASSIGNED",
                    "Cannot reassign rereviewer: no rereviewer has been assigned");
        }
        requireNotInitialReviewer(rereviewerId);
    }

    public void assignRereviewer(String rereviewerId, Instant dueAt, Instant assignedAt) {
        checkRereviewerAssignable(rereviewerId, dueAt);
        Objects.requireNonNull(assignedAt, "assignedAt must not be null");
        this.rereviewerAssignment = new ReviewerAssignment(rereviewerId, assignedAt);
        this.rereviewerDueAt = dueAt;
    }

    public void reassignRereviewer(String rereviewerId, Instant dueAt, Instant assignedAt) {
        checkRereviewerReassignable(rereviewerId, dueAt);
        Objects.requireNonNull(assignedAt, "assignedAt must not be null");
        this.rereviewerAssignment = new ReviewerAssignment(rereviewerId, assignedAt);
        this.rereviewerDueAt = dueAt;
    }

    private static void checkRereviewerInput(String rereviewerId, Instant dueAt) {
        // D-069 第 2 點、D-082 第 2 點：與指派初審醫師相同，輸入檢查在狀態檢查之前
        if (rereviewerId == null || rereviewerId.isBlank()) {
            throw new DomainValidationException("VALIDATION_ERROR", "rereviewerId must not be blank");
        }
        if (dueAt == null) {
            throw new DomainValidationException("VALIDATION_ERROR", "dueAt must not be null");
        }
    }

    private void requirePendingRereview(String operation) {
        if (this.status != CaseStatus.PENDING_REREVIEW) {
            // D-069 第 2 點：與初審的對應操作一致（422）；複審中不得改派（D-055）
            throw new DomainRuleViolationException("CASE_NOT_ASSIGNABLE",
                    "Can only " + operation + " rereviewer in PENDING_REREVIEW. Current: " + this.status);
        }
    }

    private void requireNotInitialReviewer(String rereviewerId) {
        if (this.reviewerAssignment != null
                && rereviewerId.equals(this.reviewerAssignment.getReviewerId())) {
            throw new DomainRuleViolationException("REREVIEWER_SAME_AS_REVIEWER",
                    "Re-reviewer must not be the same person as the initial reviewer (D-017)");
        }
    }

    public void startRereview(String actorId) {
        if (this.status != CaseStatus.PENDING_REREVIEW) {
            // D-069 第 2 點：與初審的對應操作一致（422）
            throw new DomainRuleViolationException("CASE_NOT_IN_REREVIEW_STAGE",
                    "Can only start rereview in PENDING_REREVIEW. Current: " + this.status);
        }
        if (this.rereviewerAssignment == null) {
            throw new DomainRuleViolationException("NO_REREVIEWER_ASSIGNED",
                    "Cannot start rereview: no rereviewer has been assigned");
        }
        Objects.requireNonNull(actorId, "actorId must not be null");
        if (!actorId.equals(this.rereviewerAssignment.getReviewerId())) {
            throw new DomainRuleViolationException("NOT_ASSIGNED_REREVIEWER",
                    "Only the assigned rereviewer can start the rereview");
        }
        this.status = CaseStatus.REREVIEW_IN_PROGRESS;
    }

    /** 開始複審並指向新建立的複審草稿（D-083 第 9 點：案件的複審指標只由 domain 方法改變）。 */
    public void startRereview(String actorId, String draftSubmissionId) {
        Objects.requireNonNull(draftSubmissionId, "draftSubmissionId must not be null");
        startRereview(actorId);
        this.latestRereviewSubmissionId = draftSubmissionId;
    }

    /**
     * 複審提交：依 D-012／D-020 計算並保存 finalScore。
     * 不結案、不固定，案件維持 REREVIEW_IN_PROGRESS（D-032、D-053 第 4 點）；結案由 {@link #closeAfterRereview()} 另行執行。
     */
    public void completeRereview(BigDecimal rereviewScore,
                                  BigDecimal initialWeight,
                                  BigDecimal rereviewWeight) {
        if (this.status != CaseStatus.REREVIEW_IN_PROGRESS) {
            // D-069 第 3 點：複審的狀態錯誤共用一個代碼（422）
            throw new DomainRuleViolationException("CASE_NOT_IN_REREVIEW_STAGE",
                    "Can only complete rereview in REREVIEW_IN_PROGRESS. Current: " + this.status);
        }
        Objects.requireNonNull(rereviewScore, "rereviewScore must not be null");
        Objects.requireNonNull(initialWeight, "initialWeight must not be null");
        Objects.requireNonNull(rereviewWeight, "rereviewWeight must not be null");
        this.finalScore = this.initialReviewScore.multiply(initialWeight)
                .add(rereviewScore.multiply(rereviewWeight))
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * 複審的獨立結案（D-034、D-053）。CLOSED 即代表複審結果已固定，不另設欄位（D-053 第 3 點）。
     * 結案目標是否為目前有效的已提交複審結果、操作者是否為提交者，由 application 層在呼叫前檢查。
     * 已結案或已取消回 CASE_TERMINAL（D-053 第 6 點）。結案時間由呼叫端傳入（ARCH-02）。
     */
    public void closeAfterRereview(Instant closedAt) {
        Objects.requireNonNull(closedAt, "closedAt must not be null");
        requireNotTerminal();
        if (this.status != CaseStatus.REREVIEW_IN_PROGRESS) {
            // D-069 第 3 點：複審的狀態錯誤共用一個代碼（422）
            throw new DomainRuleViolationException("CASE_NOT_IN_REREVIEW_STAGE",
                    "Can only close after rereview in REREVIEW_IN_PROGRESS. Current: " + this.status);
        }
        this.closedAt = closedAt;
        this.status = CaseStatus.CLOSED;
    }

    /**
     * 記錄複審修正（D-083 第 9 點，對應 {@link #recordInitialReviewRevision}）：
     * 舊提交必須是目前有效的複審結果；依 D-012／D-020 以固定的初審分數重算 finalScore；改指新提交。status 不變。
     */
    public void recordRereviewRevision(String previousSubmissionId, String newSubmissionId,
                                       BigDecimal newRereviewScore,
                                       BigDecimal initialWeight,
                                       BigDecimal rereviewWeight) {
        requireNotTerminal();
        if (!Objects.equals(previousSubmissionId, this.latestRereviewSubmissionId)) {
            throw new DomainStateConflictException("CONFLICT", "Rereview submission is no longer current");
        }
        Objects.requireNonNull(newSubmissionId, "newSubmissionId must not be null");
        updateFinalScoreForRereview(newRereviewScore, initialWeight, rereviewWeight);
        this.latestRereviewSubmissionId = newSubmissionId;
    }

    private void updateFinalScoreForRereview(BigDecimal newRereviewScore,
                                             BigDecimal initialWeight,
                                             BigDecimal rereviewWeight) {
        Objects.requireNonNull(newRereviewScore, "newRereviewScore must not be null");
        this.finalScore = this.initialReviewScore.multiply(initialWeight)
                .add(newRereviewScore.multiply(rereviewWeight))
                .setScale(2, java.math.RoundingMode.HALF_UP);
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
    private void updateFinalScore(BigDecimal newScore) {
        Objects.requireNonNull(newScore, "newScore must not be null");
        this.initialReviewScore = newScore;
        this.finalScore = newScore;
    }

    public void recordInitialReviewRevision(String previousSubmissionId, String newSubmissionId,
                                            BigDecimal newScore) {
        requireNotTerminal();
        if (isInitialReviewFixed()) {
            throw new DomainStateConflictException("INVALID_STATE_TRANSITION",
                    "Initial review is fixed and can no longer be revised (D-031, D-054)");
        }
        if (!Objects.equals(previousSubmissionId, this.latestInitialSubmissionId)) {
            throw new DomainStateConflictException("CONFLICT", "Initial submission is no longer current");
        }
        Objects.requireNonNull(newSubmissionId, "newSubmissionId must not be null");
        updateFinalScore(newScore);
        this.latestInitialSubmissionId = newSubmissionId;
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
    public Instant getDueAt() { return dueAt; }
    public String getCancelReason() { return cancelReason; }
    public String getReturnReason() { return returnReason; }
    public Instant getCancelledAt() { return cancelledAt; }
    public Instant getClosedAt() { return closedAt; }
    public boolean isRereviewRequired() { return rereviewRequired; }
    public BigDecimal getFinalScore() { return finalScore; }
    public String getFinalConclusion() { return finalConclusion; }
    public String getLatestInitialSubmissionId() { return latestInitialSubmissionId; }
    public String getRuleSnapshotRef() { return ruleSnapshotRef; }
    public String getScoreSheetVersionRef() { return scoreSheetVersionRef; }
    public ReviewerAssignment getRereviewerAssignment() { return rereviewerAssignment; }
    public Instant getRereviewerDueAt() { return rereviewerDueAt; }
    public BigDecimal getInitialReviewScore() { return initialReviewScore; }
    public String getLatestRereviewSubmissionId() { return latestRereviewSubmissionId; }
    public Instant getInitialFixedAt() { return initialFixedAt; }
    public String getInitialFixedBy() { return initialFixedBy; }
    public String getInitialFixedSubmissionId() { return initialFixedSubmissionId; }

    /** Test-only: package-private, like {@link #forceStatus}. 正式程式改用 startRereview／recordRereviewRevision（D-083 第 9 點）。 */
    void setLatestRereviewSubmissionId(String submissionId) {
        this.latestRereviewSubmissionId = submissionId;
    }
}
