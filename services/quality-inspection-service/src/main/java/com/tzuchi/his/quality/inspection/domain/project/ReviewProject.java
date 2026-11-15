package com.tzuchi.his.quality.inspection.domain.project;

import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.common.DomainValidationException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;

import java.math.BigDecimal;
import java.util.*;

/**
 * Aggregate root for ReviewProject.
 * <p>
 * A ReviewProject defines a quality review activity including sampling policy,
 * score sheet binding, reviewer pool, quality threshold, and effective period.
 * <p>
 * Invariants:
 * - An unpublished project cannot execute sampling.
 * - A published version is immutable.
 * - Version publication creates a new immutable snapshot.
 * - Activation requires a published version AND a bound score sheet.
 * - DEACTIVATED is terminal — no re-activation.
 */
public class ReviewProject {

    private String projectId;
    private String hospitalId;
    private String name;
    private String description;
    private String boundScoreSheetId;
    private Set<MedicalRecordType> medicalRecordTypes;
    private ProjectStatus status;
    private ProjectVersion currentVersion;

    protected ReviewProject() {
        // for JPA
    }

    private static final BigDecimal MIN_QUALITY_THRESHOLD = BigDecimal.ONE;
    private static final BigDecimal MAX_QUALITY_THRESHOLD = BigDecimal.TEN;

    private ReviewProject(String projectId, String hospitalId, String name,
                          String description, Set<MedicalRecordType> medicalRecordTypes,
                          BigDecimal qualityThreshold, BigDecimal initialReviewWeight,
                          BigDecimal rereviewWeight) {
        Objects.requireNonNull(projectId, "projectId must not be null");
        Objects.requireNonNull(hospitalId, "hospitalId must not be null");
        validateName(name);
        validateMedicalRecordTypes(medicalRecordTypes);
        this.projectId = projectId;
        this.hospitalId = hospitalId;
        this.name = name;
        this.description = description;
        this.medicalRecordTypes = (medicalRecordTypes != null && !medicalRecordTypes.isEmpty())
                ? EnumSet.copyOf(medicalRecordTypes)
                : EnumSet.noneOf(MedicalRecordType.class);
        validateQualityThreshold(qualityThreshold);
        validateWeightFormat("initialReviewWeight", initialReviewWeight);
        validateWeightFormat("rereviewWeight", rereviewWeight);
        this.status = ProjectStatus.DRAFT;
        // 權重沒給的是 0.50（D-020、RP-CRT-S4）；建立時不驗證權重，發布時才驗證（D-047、RP-PUB-03）
        this.currentVersion = new ProjectVersion(1, false, qualityThreshold, initialReviewWeight, rereviewWeight);
    }

    public static ReviewProject create(String projectId, String hospitalId, String name,
                                        String description,
                                        Set<MedicalRecordType> medicalRecordTypes) {
        return create(projectId, hospitalId, name, description, medicalRecordTypes, null, null, null);
    }

    /**
     * Create a draft project with its scoring inputs (D-046, D-047).
     * qualityThreshold may be null in a draft; weights not given default to 0.50.
     */
    public static ReviewProject create(String projectId, String hospitalId, String name,
                                        String description,
                                        Set<MedicalRecordType> medicalRecordTypes,
                                        BigDecimal qualityThreshold,
                                        BigDecimal initialReviewWeight,
                                        BigDecimal rereviewWeight) {
        return new ReviewProject(projectId, hospitalId, name, description, medicalRecordTypes,
                qualityThreshold, initialReviewWeight, rereviewWeight);
    }

    /** D-040: the name is required and must not be blank (null or blank is an invalid input). */
    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new DomainValidationException("VALIDATION_ERROR", "Project name must not be null or blank");
        }
    }

    /** D-040: at least one medical record type. */
    private static void validateMedicalRecordTypes(Set<MedicalRecordType> types) {
        if (types == null || types.isEmpty()) {
            throw new DomainValidationException("VALIDATION_ERROR", "At least one medical record type is required");
        }
    }

    /**
     * Weights have type decimal(2): at most two decimals and one integer digit, so that a draft can store
     * any value it accepts. The D-020 rules (each in (0, 1], sum 1) are checked at publish, not here (D-047).
     */
    private static void validateWeightFormat(String field, BigDecimal weight) {
        if (weight == null) {
            return;
        }
        if (weight.stripTrailingZeros().scale() > 2 || weight.abs().compareTo(BigDecimal.TEN) >= 0) {
            throw new DomainValidationException("VALIDATION_ERROR",
                    field + " must have at most two decimals and one integer digit, got: " + weight.toPlainString());
        }
    }

    /** D-046: the quality threshold is optional in a draft; when given it is an integer from 1 to 10. */
    private static void validateQualityThreshold(BigDecimal threshold) {
        if (threshold == null) {
            return;
        }
        boolean integer = threshold.signum() == 0 || threshold.stripTrailingZeros().scale() <= 0;
        if (!integer || threshold.compareTo(MIN_QUALITY_THRESHOLD) < 0 || threshold.compareTo(MAX_QUALITY_THRESHOLD) > 0) {
            throw new DomainValidationException("VALIDATION_ERROR",
                    "qualityThreshold must be an integer from 1 to 10, got: " + threshold.toPlainString());
        }
    }

    /**
     * Reconstitute a ReviewProject from persisted state.
     * FOR INFRASTRUCTURE USE ONLY — do not call from domain or application code.
     */
    public static ReviewProject reconstitute(String projectId, String hospitalId, String name,
                                              String description, String boundScoreSheetId,
                                              Set<MedicalRecordType> medicalRecordTypes,
                                              ProjectStatus status, int versionNumber,
                                              boolean versionPublished,
                                              BigDecimal qualityThreshold,
                                              BigDecimal initialReviewWeight,
                                              BigDecimal rereviewWeight) {
        ReviewProject project = new ReviewProject();
        project.projectId = projectId;
        project.hospitalId = hospitalId;
        project.name = name;
        project.description = description;
        project.boundScoreSheetId = boundScoreSheetId;
        project.medicalRecordTypes = (medicalRecordTypes != null && !medicalRecordTypes.isEmpty())
                ? EnumSet.copyOf(medicalRecordTypes)
                : EnumSet.noneOf(MedicalRecordType.class);
        project.status = status;
        project.currentVersion = new ProjectVersion(versionNumber, versionPublished,
                qualityThreshold, initialReviewWeight, rereviewWeight);
        return project;
    }

    public void activate() {
        if (this.status == ProjectStatus.ACTIVE) {
            return; // idempotent
        }
        if (this.status != ProjectStatus.DRAFT) {
            throw new DomainStateConflictException("INVALID_STATE_TRANSITION",
                    "Can only activate from DRAFT status, current: " + this.status);
        }
        if (!this.currentVersion.isPublished()) {
            throw new DomainStateConflictException("INVALID_STATE_TRANSITION",
                    "Cannot activate project without a published version");
        }
        if (this.boundScoreSheetId == null) {
            throw new DomainRuleViolationException("SCORE_SHEET_NOT_BOUND",
                    "Cannot activate project without a bound score sheet");
        }
        this.status = ProjectStatus.ACTIVE;
    }

    public void deactivate() {
        if (this.status == ProjectStatus.DEACTIVATED) {
            return; // idempotent
        }
        if (this.status != ProjectStatus.ACTIVE) {
            throw new DomainStateConflictException("INVALID_STATE_TRANSITION",
                    "Can only deactivate from ACTIVE status, current: " + this.status);
        }
        this.status = ProjectStatus.DEACTIVATED;
    }

    /**
     * Update draft project fields. Only allowed in DRAFT status.
     * The inputs replace the draft as a whole (D-040, D-046, D-049): a threshold not given becomes null,
     * a weight not given returns to 0.50. Weights are validated at publish, not here.
     */
    public void updateDraft(String name, String description, Set<MedicalRecordType> types,
                            BigDecimal qualityThreshold, BigDecimal initialReviewWeight,
                            BigDecimal rereviewWeight) {
        requireStatusDraft();
        // 已發布但尚未啟用時狀態仍是 DRAFT，版本內容仍不可改（D-040 第 4 點、RP-UPD-01）
        requireDraft();
        validateName(name);
        validateMedicalRecordTypes(types);
        validateQualityThreshold(qualityThreshold);
        validateWeightFormat("initialReviewWeight", initialReviewWeight);
        validateWeightFormat("rereviewWeight", rereviewWeight);
        this.currentVersion = new ProjectVersion(currentVersion.getVersionNumber(), currentVersion.isPublished(),
                qualityThreshold, initialReviewWeight, rereviewWeight);
        this.name = name;
        this.description = description;
        this.medicalRecordTypes = (types != null && !types.isEmpty())
                ? EnumSet.copyOf(types)
                : EnumSet.noneOf(MedicalRecordType.class);
    }

    /**
     * Bind a score sheet definition to this project. Only allowed in DRAFT status.
     * Does NOT cross-validate that the scoreSheetId exists or is published.
     */
    public void bindScoreSheet(String scoreSheetId) {
        requireStatusDraft();
        Objects.requireNonNull(scoreSheetId, "scoreSheetId must not be null");
        this.boundScoreSheetId = scoreSheetId;
    }

    /**
     * Publish the current draft version, making it immutable.
     */
    public void publishNewVersion() {
        if (this.currentVersion.isPublished()) {
            throw new DomainStateConflictException("INVALID_STATE_TRANSITION",
                    "Current version is already published. Cannot re-publish without creating a new draft.");
        }
        // Checked in the order of specs/review-project: RP-PUB-02, RP-PUB-03, RP-PUB-04
        if (this.boundScoreSheetId == null) {
            // D-041: the score sheet must be bound before publishing (it used to be checked only at activation)
            throw new DomainRuleViolationException("PROJECT_VERSION_NOT_PUBLISHABLE",
                    "Cannot publish a version without a bound score sheet");
        }
        // RP-PUB-03: weights are free in a draft (D-047) and must satisfy D-020 to be published
        this.currentVersion.validateWeightsForPublish();
        if (this.currentVersion.getQualityThreshold() == null) {
            // D-046: the quality threshold may be null in a draft, but is required to publish
            throw new DomainRuleViolationException("PROJECT_VERSION_NOT_PUBLISHABLE",
                    "Cannot publish a version without a quality threshold");
        }
        this.currentVersion = this.currentVersion.publish();
    }

    /**
     * Prepare a new draft version for future edits.
     * Can only be called when the current version is published.
     */
    public void prepareDraftVersion() {
        if (!this.currentVersion.isPublished()) {
            throw new DomainStateConflictException("INVALID_STATE_TRANSITION",
                    "A draft version already exists");
        }
        this.currentVersion = this.currentVersion.nextDraft();
    }

    public void updateName(String name) {
        requireDraft();
        validateName(name);
        this.name = name;
    }

    private void requireDraft() {
        if (this.currentVersion.isPublished()) {
            throw new DomainStateConflictException("INVALID_STATE_TRANSITION",
                    "Cannot modify a published version");
        }
    }

    private void requireStatusDraft() {
        if (this.status != ProjectStatus.DRAFT) {
            throw new DomainStateConflictException("INVALID_STATE_TRANSITION",
                    "Operation only allowed in DRAFT status, current: " + this.status);
        }
    }

    // --- Getters ---

    public String getProjectId() { return projectId; }
    public String getHospitalId() { return hospitalId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getBoundScoreSheetId() { return boundScoreSheetId; }
    public Set<MedicalRecordType> getMedicalRecordTypes() { return Collections.unmodifiableSet(medicalRecordTypes); }
    public ProjectStatus getStatus() { return status; }
    public ProjectVersion getCurrentVersion() { return currentVersion; }
}
