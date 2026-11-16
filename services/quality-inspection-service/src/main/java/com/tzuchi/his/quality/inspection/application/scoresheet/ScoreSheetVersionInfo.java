package com.tzuchi.his.quality.inspection.application.scoresheet;

public class ScoreSheetVersionInfo {

    private final int versionNo;
    private final boolean published;

    public ScoreSheetVersionInfo(int versionNo, boolean published) {
        this.versionNo = versionNo;
        this.published = published;
    }

    public int getVersionNo() { return versionNo; }
    public boolean isPublished() { return published; }
}
