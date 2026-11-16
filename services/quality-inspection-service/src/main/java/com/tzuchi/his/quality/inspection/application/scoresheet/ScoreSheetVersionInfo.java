package com.tzuchi.his.quality.inspection.application.scoresheet;

public class ScoreSheetVersionInfo {

    private final int versionNo;
    private final boolean published;
    private final String scoreSheetVersionId;

    /** @param scoreSheetVersionId 已發布時是發布產生的 id，未發布時是 null（D-050 第 1 點） */
    public ScoreSheetVersionInfo(int versionNo, boolean published, String scoreSheetVersionId) {
        this.versionNo = versionNo;
        this.scoreSheetVersionId = scoreSheetVersionId;
        this.published = published;
    }

    public int getVersionNo() { return versionNo; }
    public boolean isPublished() { return published; }
    public String getScoreSheetVersionId() { return scoreSheetVersionId; }
}
