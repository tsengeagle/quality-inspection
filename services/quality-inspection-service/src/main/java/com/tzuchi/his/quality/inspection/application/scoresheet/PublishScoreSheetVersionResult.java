package com.tzuchi.his.quality.inspection.application.scoresheet;

/** PublishScoreSheetVersion 的輸出（specs/score-sheet）：發布產生的版本 id，用於綁定到專案（SS-PUB-S2、D-041）。 */
public class PublishScoreSheetVersionResult {

    private final String scoreSheetVersionId;

    public PublishScoreSheetVersionResult(String scoreSheetVersionId) {
        this.scoreSheetVersionId = scoreSheetVersionId;
    }

    public String getScoreSheetVersionId() { return scoreSheetVersionId; }
}
