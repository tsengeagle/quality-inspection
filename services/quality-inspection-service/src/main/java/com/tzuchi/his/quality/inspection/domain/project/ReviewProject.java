package com.tzuchi.his.quality.inspection.domain.project;

import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;

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
 */
public class ReviewProject {

    private String projectId;
    private String hospitalId;
    private String name;
    private Set<MedicalRecordType> medicalRecordTypes;
    private ProjectStatus status;
    private ProjectVersion currentVersion;

    protected ReviewProject() {
        // for JPA
    }

    private ReviewProject(String projectId, String hospitalId, String name,
                          Set<MedicalRecordType> medicalRecordTypes) {
        Objects.requireNonNull(projectId, "projectId must not be null");
        Objects.requireNonNull(hospitalId, "hospitalId must not be null");
        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Project name must not be blank");
        }
        this.projectId = projectId;
        this.hospitalId = hospitalId;
        this.name = name;
        this.medicalRecordTypes = medicalRecordTypes != null
                ? EnumSet.copyOf(medicalRecordTypes)
                : EnumSet.noneOf(MedicalRecordType.class);
        this.status = ProjectStatus.DRAFT;
        this.currentVersion = new ProjectVersion(1, false);
    }

    public static ReviewProject create(String projectId, String hospitalId, String name,
                                        Set<MedicalRecordType> medicalRecordTypes) {
        return new ReviewProject(projectId, hospitalId, name, medicalRecordTypes);
    }

    public void activate() {
        if (this.status == ProjectStatus.ACTIVE) {
            return;
        }
        if (!this.currentVersion.isPublished()) {
            throw new IllegalStateException("Cannot activate project without a published version");
        }
        this.status = ProjectStatus.ACTIVE;
    }

    public void deactivate() {
        if (this.status == ProjectStatus.DEACTIVATED || this.status == ProjectStatus.DRAFT) {
            return;
        }
        this.status = ProjectStatus.DEACTIVATED;
    }

    /**
     * Publish the current draft version, making it immutable.
     * After publishing, a new draft version is prepared for future edits.
     */
    public void publishNewVersion() {
        if (this.currentVersion.isPublished()) {
            throw new IllegalStateException(
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
            throw new IllegalStateException("A draft version already exists");
        }
        this.currentVersion = this.currentVersion.nextDraft();
    }

    public void updateName(String name) {
        requireDraft();
        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Project name must not be blank");
        }
        this.name = name;
    }

    private void requireDraft() {
        if (this.currentVersion.isPublished()) {
            throw new IllegalStateException("Cannot modify a published version");
        }
    }

    // --- Getters ---

    public String getProjectId() {
        return projectId;
    }

    public String getHospitalId() {
        return hospitalId;
    }

    public String getName() {
        return name;
    }

    public Set<MedicalRecordType> getMedicalRecordTypes() {
        return Collections.unmodifiableSet(medicalRecordTypes);
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public ProjectVersion getCurrentVersion() {
        return currentVersion;
    }
}
