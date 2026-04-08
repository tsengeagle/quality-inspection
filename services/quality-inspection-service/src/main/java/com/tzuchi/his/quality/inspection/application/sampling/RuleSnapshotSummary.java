package com.tzuchi.his.quality.inspection.application.sampling;

/**
 * Read model for rule snapshot display.
 */
public class RuleSnapshotSummary {

    private final String projectVersionId;
    private final int projectVersionNumber;
    private final String scoreSheetVersionId;
    private final String samplingPolicy;

    public RuleSnapshotSummary(String projectVersionId, int projectVersionNumber,
                                String scoreSheetVersionId, String samplingPolicy) {
        this.projectVersionId = projectVersionId;
        this.projectVersionNumber = projectVersionNumber;
        this.scoreSheetVersionId = scoreSheetVersionId;
        this.samplingPolicy = samplingPolicy;
    }

    public String getProjectVersionId() { return projectVersionId; }
    public int getProjectVersionNumber() { return projectVersionNumber; }
    public String getScoreSheetVersionId() { return scoreSheetVersionId; }
    public String getSamplingPolicy() { return samplingPolicy; }
}
