package com.tzuchi.his.quality.inspection.domain.scoresheet;

import java.util.*;

/**
 * Aggregate root for ScoreSheetDefinition.
 * <p>
 * Manages score sheet versions with sections and items.
 * <p>
 * Invariants:
 * - Published version is immutable.
 * - No duplicate itemId within the same version.
 * - No duplicate code within the same version.
 * - maxScore must not be negative.
 * - Only evolves via new version; no patching of published content.
 */
public class ScoreSheetDefinition {

    private String scoreSheetId;
    private String projectId;
    private int version;
    private ScoreSheetStatus status;
    private List<ScoreSheetSection> sections;

    protected ScoreSheetDefinition() {
        // for JPA
    }

    private ScoreSheetDefinition(String scoreSheetId, String projectId) {
        Objects.requireNonNull(scoreSheetId, "scoreSheetId must not be null");
        Objects.requireNonNull(projectId, "projectId must not be null");
        this.scoreSheetId = scoreSheetId;
        this.projectId = projectId;
        this.version = 1;
        this.status = ScoreSheetStatus.DRAFT;
        this.sections = new ArrayList<>();
    }

    public static ScoreSheetDefinition createDraft(String scoreSheetId, String projectId) {
        return new ScoreSheetDefinition(scoreSheetId, projectId);
    }

    public void addSection(ScoreSheetSection section) {
        requireDraft();
        Objects.requireNonNull(section, "section must not be null");
        boolean duplicate = sections.stream()
                .anyMatch(s -> s.getSectionId().equals(section.getSectionId()));
        if (duplicate) {
            throw new IllegalArgumentException("Duplicate sectionId: " + section.getSectionId());
        }
        this.sections.add(section);
    }

    /**
     * Publish this draft version.
     * After publishing, the version becomes immutable.
     */
    public void publishVersion() {
        requireDraft();
        if (sections.isEmpty()) {
            throw new IllegalStateException("Cannot publish an empty score sheet");
        }
        boolean hasItems = sections.stream().anyMatch(s -> !s.getItems().isEmpty());
        if (!hasItems) {
            throw new IllegalStateException("Cannot publish a score sheet with no items");
        }
        this.status = ScoreSheetStatus.PUBLISHED;
    }

    private void requireDraft() {
        if (this.status == ScoreSheetStatus.PUBLISHED) {
            throw new IllegalStateException("Cannot modify a published score sheet version");
        }
    }

    // --- Getters ---

    public String getScoreSheetId() { return scoreSheetId; }
    public String getProjectId() { return projectId; }
    public int getVersion() { return version; }
    public ScoreSheetStatus getStatus() { return status; }
    public List<ScoreSheetSection> getSections() { return Collections.unmodifiableList(sections); }
}
