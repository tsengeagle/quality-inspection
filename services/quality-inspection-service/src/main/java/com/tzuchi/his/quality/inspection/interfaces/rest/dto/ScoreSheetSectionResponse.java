package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import java.util.List;

public class ScoreSheetSectionResponse {
    private String sectionId;
    private String title;
    private List<ScoreSheetItemResponse> items;

    public ScoreSheetSectionResponse(String sectionId, String title, List<ScoreSheetItemResponse> items) {
        this.sectionId = sectionId;
        this.title = title;
        this.items = items;
    }

    public String getSectionId() { return sectionId; }
    public String getTitle() { return title; }
    public List<ScoreSheetItemResponse> getItems() { return items; }
}
