package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

public class ScoreSheetSummaryResponse {
    private String scoreSheetId;
    private String projectId;
    private String status;
    private int version;
    private int sectionCount;
    private int itemCount;

    public ScoreSheetSummaryResponse(String scoreSheetId, String projectId, String status,
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
    public String getStatus() { return status; }
    public int getVersion() { return version; }
    public int getSectionCount() { return sectionCount; }
    public int getItemCount() { return itemCount; }
}
