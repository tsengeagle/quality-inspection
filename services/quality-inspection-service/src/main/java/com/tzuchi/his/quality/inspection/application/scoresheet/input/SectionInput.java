package com.tzuchi.his.quality.inspection.application.scoresheet.input;

import java.util.List;

/** 新增章節的輸入，含它的題目（input_types SectionInput）。 */
public class SectionInput {

    private String sectionId;
    private String title;
    private List<ItemInput> items;

    public String getSectionId() { return sectionId; }
    public void setSectionId(String sectionId) { this.sectionId = sectionId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public List<ItemInput> getItems() { return items; }
    public void setItems(List<ItemInput> items) { this.items = items; }
}
