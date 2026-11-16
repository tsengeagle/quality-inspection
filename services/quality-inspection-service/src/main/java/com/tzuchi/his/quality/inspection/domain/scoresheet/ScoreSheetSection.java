package com.tzuchi.his.quality.inspection.domain.scoresheet;

import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import java.util.*;

/**
 * A section within a score sheet, grouping related score items.
 */
public class ScoreSheetSection {

    private String sectionId;
    private String title;
    private List<ScoreItem> items;

    protected ScoreSheetSection() {
        // for JPA
    }

    public ScoreSheetSection(String sectionId, String title) {
        ScoreItem.requireText(sectionId, "sectionId");
        ScoreItem.requireText(title, "title");
        this.sectionId = sectionId;
        this.title = title;
        this.items = new ArrayList<>();
    }

    /** 從已儲存的資料還原，不做新輸入的驗證與重複檢查。FOR INFRASTRUCTURE USE ONLY。 */
    public static ScoreSheetSection reconstitute(String sectionId, String title, List<ScoreItem> items) {
        ScoreSheetSection section = new ScoreSheetSection();
        section.sectionId = sectionId;
        section.title = title;
        section.items = new ArrayList<>(items);
        return section;
    }

    public void addItem(ScoreItem item) {
        Objects.requireNonNull(item, "item must not be null");
        boolean duplicateItemId = items.stream().anyMatch(i -> i.getItemId().equals(item.getItemId()));
        if (duplicateItemId) {
            throw new DomainRuleViolationException("DUPLICATE_ITEM_ID", "Duplicate itemId: " + item.getItemId());
        }
        boolean duplicateCode = items.stream().anyMatch(i -> i.getCode().equals(item.getCode()));
        if (duplicateCode) {
            throw new DomainRuleViolationException("DUPLICATE_ITEM_CODE", "Duplicate code: " + item.getCode());
        }
        this.items.add(item);
    }

    public String getSectionId() { return sectionId; }
    public String getTitle() { return title; }
    public List<ScoreItem> getItems() { return Collections.unmodifiableList(items); }

    public void updateTitle(String newTitle) {
        ScoreItem.requireText(newTitle, "title");
        this.title = newTitle;
    }

    public void replaceItemAt(int index, ScoreItem item) {
        Objects.requireNonNull(item, "item must not be null");
        items.set(index, item);
    }

    public void removeItem(String itemId) {
        boolean removed = items.removeIf(i -> i.getItemId().equals(itemId));
        if (!removed) {
            throw new DomainRuleViolationException("ITEM_NOT_FOUND",
                    "Item not found in section: " + itemId);
        }
    }
}
