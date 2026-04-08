package com.tzuchi.his.quality.inspection.domain.scoresheet;

import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;

import java.util.*;

/**
 * Aggregate root for ScoreSheetDefinition.
 * <p>
 * Manages score sheet versions with sections and items.
 * <p>
 * Invariants:
 * - Published version is immutable.
 * - No duplicate itemId across all sections within the same version.
 * - No duplicate item code across all sections within the same version.
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

    /**
     * Reconstitute a ScoreSheetDefinition from persisted state.
     * FOR INFRASTRUCTURE USE ONLY — do not call from domain or application code.
     */
    public static ScoreSheetDefinition reconstitute(String scoreSheetId, String projectId,
                                                     ScoreSheetStatus status, int version,
                                                     List<ScoreSheetSection> sections) {
        ScoreSheetDefinition ss = new ScoreSheetDefinition();
        ss.scoreSheetId = scoreSheetId;
        ss.projectId = projectId;
        ss.status = status;
        ss.version = version;
        ss.sections = sections != null ? new ArrayList<>(sections) : new ArrayList<>();
        return ss;
    }

    public void addSection(ScoreSheetSection section) {
        requireDraft();
        Objects.requireNonNull(section, "section must not be null");

        // Duplicate sectionId check
        boolean duplicateSection = sections.stream()
                .anyMatch(s -> s.getSectionId().equals(section.getSectionId()));
        if (duplicateSection) {
            throw new DomainStateConflictException("INVALID_STATE_TRANSITION",
                    "Duplicate sectionId: " + section.getSectionId());
        }

        // H-001: cross-section itemId uniqueness check
        Set<String> existingItemIds = new HashSet<>();
        Set<String> existingCodes = new HashSet<>();
        for (ScoreSheetSection existing : sections) {
            for (ScoreItem item : existing.getItems()) {
                existingItemIds.add(item.getItemId());
                existingCodes.add(item.getCode());
            }
        }
        for (ScoreItem newItem : section.getItems()) {
            if (!existingItemIds.add(newItem.getItemId())) {
                throw new DomainStateConflictException("DUPLICATE_ITEM_ID",
                        "Duplicate itemId across sections: " + newItem.getItemId());
            }
            if (!existingCodes.add(newItem.getCode())) {
                throw new DomainStateConflictException("DUPLICATE_ITEM_CODE",
                        "Duplicate item code across sections: " + newItem.getCode());
            }
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
            throw new DomainStateConflictException("SCORE_SHEET_NOT_PUBLISHABLE",
                    "Cannot publish an empty score sheet");
        }
        boolean hasItems = sections.stream().anyMatch(s -> !s.getItems().isEmpty());
        if (!hasItems) {
            throw new DomainStateConflictException("SCORE_SHEET_NOT_PUBLISHABLE",
                    "Cannot publish a score sheet with no items");
        }
        this.status = ScoreSheetStatus.PUBLISHED;
    }

    public void updateDraft(UpdateDraftCommand command) {
        requireDraft();
        Objects.requireNonNull(command, "command must not be null");

        // 1. Remove sections first
        for (String sectionId : command.getRemoveSectionIds()) {
            boolean removed = sections.removeIf(s -> s.getSectionId().equals(sectionId));
            if (!removed) {
                throw new DomainRuleViolationException("SECTION_NOT_FOUND",
                        "Section not found: " + sectionId);
            }
        }

        // 2. Remove items (cross-section search)
        for (String itemId : command.getRemoveItemIds()) {
            boolean found = false;
            for (ScoreSheetSection section : sections) {
                if (section.getItems().stream().anyMatch(i -> i.getItemId().equals(itemId))) {
                    section.removeItem(itemId);
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new DomainRuleViolationException("ITEM_NOT_FOUND",
                        "Item not found: " + itemId);
            }
        }

        // 3. Update section titles
        for (UpdateDraftCommand.UpdateSectionCommand update : command.getUpdateSections()) {
            ScoreSheetSection section = findSection(update.getSectionId());
            section.updateTitle(update.getTitle());
        }

        // 4. Update items (find item across sections, replace it)
        for (UpdateDraftCommand.UpdateItemCommand update : command.getUpdateItems()) {
            updateItemInSections(update);
        }

        // 5. Add new sections (reuses existing addSection() which enforces uniqueness)
        for (UpdateDraftCommand.AddSectionCommand add : command.getAddSections()) {
            ScoreSheetSection newSection = new ScoreSheetSection(add.getSectionId(), add.getTitle());
            for (UpdateDraftCommand.AddItemCommand itemCmd : add.getItems()) {
                newSection.addItem(toScoreItem(itemCmd));
            }
            addSection(newSection);
        }

        // 6. Add items to existing sections (with cross-section uniqueness)
        for (UpdateDraftCommand.AddItemCommand add : command.getAddItems()) {
            ScoreSheetSection section = findSection(add.getTargetSectionId());
            ScoreItem newItem = toScoreItem(add);
            validateCrossSectionUniqueness(newItem, section);
            section.addItem(newItem);
        }
    }

    private ScoreSheetSection findSection(String sectionId) {
        return sections.stream()
                .filter(s -> s.getSectionId().equals(sectionId))
                .findFirst()
                .orElseThrow(() -> new DomainRuleViolationException("SECTION_NOT_FOUND",
                        "Section not found: " + sectionId));
    }

    private void updateItemInSections(UpdateDraftCommand.UpdateItemCommand update) {
        for (ScoreSheetSection section : sections) {
            for (int i = 0; i < section.getItems().size(); i++) {
                if (section.getItems().get(i).getItemId().equals(update.getItemId())) {
                    // Check code uniqueness if code changed
                    ScoreItem oldItem = section.getItems().get(i);
                    if (!oldItem.getCode().equals(update.getCode())) {
                        ScoreItem tempItem = new ScoreItem(update.getItemId(), update.getCode(),
                                update.getTitle(), update.getScoreType(), update.getMaxScore(),
                                update.getWeight(), update.getRequiredRule(), update.getConditionExpression());
                        validateCrossSectionUniqueness(tempItem, null);
                    }
                    // Replace the item — remove old, add new at same position
                    section.removeItem(update.getItemId());
                    ScoreItem replacement = new ScoreItem(update.getItemId(), update.getCode(),
                            update.getTitle(), update.getScoreType(), update.getMaxScore(),
                            update.getWeight(), update.getRequiredRule(), update.getConditionExpression());
                    section.addItem(replacement);
                    return;
                }
            }
        }
        throw new DomainRuleViolationException("ITEM_NOT_FOUND",
                "Item not found: " + update.getItemId());
    }

    private void validateCrossSectionUniqueness(ScoreItem newItem, ScoreSheetSection excludeSection) {
        for (ScoreSheetSection section : sections) {
            if (section == excludeSection) continue;
            for (ScoreItem existing : section.getItems()) {
                if (existing.getItemId().equals(newItem.getItemId())) {
                    throw new DomainStateConflictException("DUPLICATE_ITEM_ID",
                            "Duplicate itemId across sections: " + newItem.getItemId());
                }
                if (existing.getCode().equals(newItem.getCode())) {
                    throw new DomainStateConflictException("DUPLICATE_ITEM_CODE",
                            "Duplicate item code across sections: " + newItem.getCode());
                }
            }
        }
    }

    private ScoreItem toScoreItem(UpdateDraftCommand.AddItemCommand cmd) {
        return new ScoreItem(cmd.getItemId(), cmd.getCode(), cmd.getTitle(),
                cmd.getScoreType(), cmd.getMaxScore(), cmd.getWeight(),
                cmd.getRequiredRule(), cmd.getConditionExpression());
    }

    private void requireDraft() {
        if (this.status == ScoreSheetStatus.PUBLISHED) {
            throw new DomainStateConflictException("INVALID_STATE_TRANSITION",
                    "Cannot modify a published score sheet version");
        }
    }

    // --- Getters ---

    public String getScoreSheetId() { return scoreSheetId; }
    public String getProjectId() { return projectId; }
    public int getVersion() { return version; }
    public ScoreSheetStatus getStatus() { return status; }
    public List<ScoreSheetSection> getSections() { return Collections.unmodifiableList(sections); }
}
