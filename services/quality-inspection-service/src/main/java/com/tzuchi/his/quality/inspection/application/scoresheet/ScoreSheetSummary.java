package com.tzuchi.his.quality.inspection.application.scoresheet;

import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetStatus;

public class ScoreSheetSummary {

    private final String scoreSheetId;
    private final String projectId;
    private final ScoreSheetStatus status;
    private final int version;
    private final int sectionCount;
    private final int itemCount;

    public ScoreSheetSummary(String scoreSheetId, String projectId, ScoreSheetStatus status,
                              int version, int sectionCount, int itemCount) {
        this.scoreSheetId = scoreSheetId;
        this.projectId = projectId;
        this.status = status;
        this.version = version;
        this.sectionCount = sectionCount;
        this.itemCount = itemCount;
    }

    public String getScoreSheetId() { return scoreSheetId; }
    public String getProjectId() { return projectId; }
    public ScoreSheetStatus getStatus() { return status; }
    public int getVersion() { return version; }
    public int getSectionCount() { return sectionCount; }
    public int getItemCount() { return itemCount; }
}
