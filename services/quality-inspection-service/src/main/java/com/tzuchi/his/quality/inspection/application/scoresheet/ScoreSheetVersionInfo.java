package com.tzuchi.his.quality.inspection.application.scoresheet;

public class ScoreSheetVersionInfo {

    private final int version;
    private final boolean published;

    public ScoreSheetVersionInfo(int version, boolean published) {
        this.version = version;
        this.published = published;
    }

    public int getVersion() { return version; }
    public boolean isPublished() { return published; }
}
