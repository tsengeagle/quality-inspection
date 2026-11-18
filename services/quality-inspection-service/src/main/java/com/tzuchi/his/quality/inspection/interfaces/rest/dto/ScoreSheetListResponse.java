package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import java.util.List;

/** GET /score-sheets 的回應：{ items }（D-050 第 7 點）。 */
public class ScoreSheetListResponse {

    private final List<ScoreSheetSummaryResponse> items;

    public ScoreSheetListResponse(List<ScoreSheetSummaryResponse> items) {
        this.items = items;
    }

    public List<ScoreSheetSummaryResponse> getItems() { return items; }
}
