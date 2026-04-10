package com.tzuchi.his.quality.inspection.domain.project;

import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
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

    private ReviewProject(String projectId, String hospitalId, String name,
                          String description, Set<MedicalRecordType> medicalRecordTypes) {
        Objects.requireNonNull(projectId, "projectId must not be null");
        Objects.requireNonNull(hospitalId, "hospitalId must not be null");
        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank()) {
            throw new DomainRuleViolationException("INVALID_PROJECT_NAME",
                    "Project name must not be blank");
        }
        this.projectId = projectId;
        this.hospitalId = hospitalId;
        this.name = name;
        this.description = description;
        this.medicalRecordTypes = (medicalRecordTypes != null && !medicalRecordTypes.isEmpty())
                ? EnumSet.copyOf(medicalRecordTypes)
                : EnumSet.noneOf(MedicalRecordType.class);
        this.status = ProjectStatus.DRAFT;
        this.currentVersion = new ProjectVersion(1, false);
    }

    public static ReviewProject create(String projectId, String hospitalId, String name,
                                        String description,
                                        Set<MedicalRecordType> medicalRecordTypes) {
        return new ReviewProject(projectId, hospitalId, name, description, medicalRecordTypes);
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
     */
    public void updateDraft(String name, String description, Set<MedicalRecordType> types) {
        requireStatusDraft();
        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank()) {
            throw new DomainRuleViolationException("INVALID_PROJECT_NAME",
                    "Project name must not be blank");
        }
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
        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank()) {
            throw new DomainRuleViolationException("INVALID_PROJECT_NAME",
                    "Project name must not be blank");
        }
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
