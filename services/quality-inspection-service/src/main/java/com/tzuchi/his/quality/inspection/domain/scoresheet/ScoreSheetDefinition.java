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
 * Error categories follow specs/score-sheet (D-050): field validation is VALIDATION_ERROR,
 * duplicates and missing targets are rule violations, changes to a published version are state conflicts.
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

        requireNewSectionId(section.getSectionId());

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
                throw new DomainRuleViolationException("DUPLICATE_ITEM_ID",
                        "Duplicate itemId across sections: " + newItem.getItemId());
            }
            if (!existingCodes.add(newItem.getCode())) {
                throw new DomainRuleViolationException("DUPLICATE_ITEM_CODE",
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
            throw new DomainRuleViolationException("SCORE_SHEET_NOT_PUBLISHABLE",
                    "Cannot publish an empty score sheet");
        }
        boolean hasItems = sections.stream().anyMatch(s -> !s.getItems().isEmpty());
        if (!hasItems) {
            throw new DomainRuleViolationException("SCORE_SHEET_NOT_PUBLISHABLE",
                    "Cannot publish a score sheet with no items");
        }
        this.status = ScoreSheetStatus.PUBLISHED;
    }

    public void updateDraft(UpdateDraftCommand command) {
        requireDraft();
        Objects.requireNonNull(command, "command must not be null");

        // 1. Remove sections first
        for (String sectionId : command.getRemoveSectionIds()) {
            ScoreItem.requireText(sectionId, "sectionId");
            boolean removed = sections.removeIf(s -> s.getSectionId().equals(sectionId));
            if (!removed) {
                throw new DomainRuleViolationException("SECTION_NOT_FOUND",
                        "Section not found: " + sectionId);
            }
        }

        // 2. Remove items (cross-section search)
        for (String itemId : command.getRemoveItemIds()) {
            ScoreItem.requireText(itemId, "itemId");
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
            requireElement(update);
            ScoreItem.requireText(update.getSectionId(), "sectionId");
            ScoreItem.requireText(update.getTitle(), "title");
            ScoreSheetSection section = findSection(update.getSectionId());
            section.updateTitle(update.getTitle());
        }

        // 4. Update items (find item across sections, replace it)
        for (UpdateDraftCommand.UpdateItemCommand update : command.getUpdateItems()) {
            requireElement(update);
            updateItemInSections(update);
        }

        // 5. Add new sections (reuses existing addSection() which enforces uniqueness)
        for (UpdateDraftCommand.AddSectionCommand add : command.getAddSections()) {
            requireElement(add);
            // 先驗證這個章節與它所有題目的輸入，再檢查重複
            ScoreSheetSection newSection = new ScoreSheetSection(add.getSectionId(), add.getTitle());
            List<ScoreItem> newItems = new ArrayList<>();
            for (UpdateDraftCommand.AddItemCommand itemCmd : add.getItems()) {
                requireElement(itemCmd);
                newItems.add(toScoreItem(itemCmd));
            }
            // 重複的檢查順序：章節 id（SS-UPD-09）→ 題目 id（SS-UPD-10）→ 題目代碼（SS-UPD-11）
            requireNewSectionId(newSection.getSectionId());
            requireNoDuplicates(newItems, ScoreItem::getItemId, "DUPLICATE_ITEM_ID");
            requireNoDuplicates(newItems, ScoreItem::getCode, "DUPLICATE_ITEM_CODE");
            newItems.forEach(newSection::addItem);
            addSection(newSection);
        }

        // 6. Add items to existing sections (with cross-section uniqueness)
        for (UpdateDraftCommand.AddItemCommand add : command.getAddItems()) {
            requireElement(add);
            ScoreItem.requireText(add.getTargetSectionId(), "targetSectionId");
            ScoreItem newItem = toScoreItem(add);
            ScoreSheetSection section = findSection(add.getTargetSectionId());
            validateCrossSectionUniqueness(newItem, section);
            section.addItem(newItem);
        }
    }

    /** 新題目之間、以及與版本內既有題目之間，指定欄位不得重複。 */
    private void requireNoDuplicates(List<ScoreItem> newItems, java.util.function.Function<ScoreItem, String> key,
                                     String errorCode) {
        Set<String> seen = new HashSet<>();
        for (ScoreSheetSection existing : sections) {
            for (ScoreItem item : existing.getItems()) {
                seen.add(key.apply(item));
            }
        }
        for (ScoreItem item : newItems) {
            if (!seen.add(key.apply(item))) {
                throw new DomainRuleViolationException(errorCode, "Duplicate in score sheet version: " + key.apply(item));
            }
        }
    }

    private void requireNewSectionId(String sectionId) {
        if (sections.stream().anyMatch(s -> s.getSectionId().equals(sectionId))) {
            throw new DomainRuleViolationException("DUPLICATE_SECTION_ID", "Duplicate sectionId: " + sectionId);
        }
    }

    /** 清單裡的 null 元素是輸入錯誤（D-050 第 4 點），在輪到它的相位才回報。 */
    private static void requireElement(Object element) {
        if (element == null) {
            throw ScoreItem.invalid("List elements must not be null");
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
        // 先驗證輸入，再查找題目（D-050 第 4 點的檢查順序）
        ScoreItem replacement = new ScoreItem(update.getItemId(), update.getCode(),
                update.getTitle(), update.getScoreType(), update.getMaxScore(),
                update.getWeight(), update.getRequiredRule(), update.getConditionExpression());
        for (ScoreSheetSection section : sections) {
            for (int i = 0; i < section.getItems().size(); i++) {
                if (section.getItems().get(i).getItemId().equals(update.getItemId())) {
                    ScoreItem oldItem = section.getItems().get(i);
                    if (!oldItem.getCode().equals(update.getCode())) {
                        validateCrossSectionUniqueness(replacement, section);
                        // 同一章節的其他題目也不能有相同代碼（題目本身被取代，不算重複）
                        for (int j = 0; j < section.getItems().size(); j++) {
                            if (j != i && section.getItems().get(j).getCode().equals(update.getCode())) {
                                throw new DomainRuleViolationException("DUPLICATE_ITEM_CODE",
                                        "Duplicate item code in section: " + update.getCode());
                            }
                        }
                    }
                    section.replaceItemAt(i, replacement);
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
                    throw new DomainRuleViolationException("DUPLICATE_ITEM_ID",
                            "Duplicate itemId across sections: " + newItem.getItemId());
                }
                if (existing.getCode().equals(newItem.getCode())) {
                    throw new DomainRuleViolationException("DUPLICATE_ITEM_CODE",
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
