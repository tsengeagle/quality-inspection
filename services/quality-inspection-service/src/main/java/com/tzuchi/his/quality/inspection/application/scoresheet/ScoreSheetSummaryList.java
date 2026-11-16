package com.tzuchi.his.quality.inspection.application.scoresheet;

import java.util.List;

/** ListScoreSheetSummaries 的輸出（specs/score-sheet/usecases.yaml）：每一項與 GetScoreSheetDetail 的輸出相同。 */
public class ScoreSheetSummaryList {

    private final List<ScoreSheetSummary> items;

    public ScoreSheetSummaryList(List<ScoreSheetSummary> items) {
        this.items = List.copyOf(items);
    }

    public List<ScoreSheetSummary> getItems() { return items; }
}
