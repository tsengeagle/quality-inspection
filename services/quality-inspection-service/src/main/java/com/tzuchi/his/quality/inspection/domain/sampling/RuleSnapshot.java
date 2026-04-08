package com.tzuchi.his.quality.inspection.domain.sampling;

import java.util.Objects;

/**
 * Snapshot of the rules used at the time of batch execution.
 * Preserved for traceability (D-011) — must not be modified after batch completion.
 *
 * This is the simplified version: stores version IDs + key parameters.
 * Full snapshot_json serialization + checksum deferred to B5.
 */
public class RuleSnapshot {

    private final String projectVersionId;
    private final int projectVersionNumber;
    private final String scoreSheetVersionId;
    private final String samplingPolicy;
    private final String snapshotSummary;

    public RuleSnapshot(String projectVersionId, int projectVersionNumber,
                        String scoreSheetVersionId, String samplingPolicy,
                        String snapshotSummary) {
        Objects.requireNonNull(projectVersionId, "projectVersionId must not be null");
        Objects.requireNonNull(scoreSheetVersionId, "scoreSheetVersionId must not be null");
        Objects.requireNonNull(samplingPolicy, "samplingPolicy must not be null");
        this.projectVersionId = projectVersionId;
        this.projectVersionNumber = projectVersionNumber;
        this.scoreSheetVersionId = scoreSheetVersionId;
        this.samplingPolicy = samplingPolicy;
        this.snapshotSummary = snapshotSummary;
    }

    public String getProjectVersionId() { return projectVersionId; }
    public int getProjectVersionNumber() { return projectVersionNumber; }
    public String getScoreSheetVersionId() { return scoreSheetVersionId; }
    public String getSamplingPolicy() { return samplingPolicy; }
    public String getSnapshotSummary() { return snapshotSummary; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RuleSnapshot that = (RuleSnapshot) o;
        return projectVersionNumber == that.projectVersionNumber
                && projectVersionId.equals(that.projectVersionId)
                && scoreSheetVersionId.equals(that.scoreSheetVersionId)
                && samplingPolicy.equals(that.samplingPolicy)
                && Objects.equals(snapshotSummary, that.snapshotSummary);
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectVersionId, projectVersionNumber,
                scoreSheetVersionId, samplingPolicy, snapshotSummary);
    }

    @Override
    public String toString() {
        return "RuleSnapshot{" +
                "projectVersionId='" + projectVersionId + '\'' +
                ", projectVersionNumber=" + projectVersionNumber +
                ", scoreSheetVersionId='" + scoreSheetVersionId + '\'' +
                ", samplingPolicy='" + samplingPolicy + '\'' +
                '}';
    }
}
