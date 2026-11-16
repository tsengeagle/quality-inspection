package com.tzuchi.his.quality.inspection.application.scoresheet;

import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetStatus;

public class ScoreSheetSummary {

    private final String scoreSheetId;
    private final String projectId;
    private final ScoreSheetStatus status;
    private final int versionNo;
    private final int sectionCount;
    private final int itemCount;

    public ScoreSheetSummary(String scoreSheetId, String projectId, ScoreSheetStatus status,
                              int versionNo, int sectionCount, int itemCount) {
        this.scoreSheetId = scoreSheetId;
        this.projectId = projectId;
        this.status = status;
        this.versionNo = versionNo;
        this.sectionCount = sectionCount;
        this.itemCount = itemCount;
    }

    public String getScoreSheetId() { return scoreSheetId; }
    public String getProjectId() { return projectId; }
    public ScoreSheetStatus getStatus() { return status; }
    public int getVersionNo() { return versionNo; }
    public int getSectionCount() { return sectionCount; }
    public int getItemCount() { return itemCount; }
}
