package com.tzuchi.his.quality.inspection.domain.scoresheet;

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
        Objects.requireNonNull(sectionId, "sectionId must not be null");
        Objects.requireNonNull(title, "title must not be null");
        this.sectionId = sectionId;
        this.title = title;
        this.items = new ArrayList<>();
    }

    public void addItem(ScoreItem item) {
        Objects.requireNonNull(item, "item must not be null");
        boolean duplicateItemId = items.stream().anyMatch(i -> i.getItemId().equals(item.getItemId()));
        if (duplicateItemId) {
            throw new IllegalArgumentException("Duplicate itemId: " + item.getItemId());
        }
        boolean duplicateCode = items.stream().anyMatch(i -> i.getCode().equals(item.getCode()));
        if (duplicateCode) {
            throw new IllegalArgumentException("Duplicate code: " + item.getCode());
        }
        this.items.add(item);
    }

    public String getSectionId() { return sectionId; }
    public String getTitle() { return title; }
    public List<ScoreItem> getItems() { return Collections.unmodifiableList(items); }
}
