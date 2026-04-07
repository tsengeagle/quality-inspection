package com.tzuchi.his.quality.inspection.domain.sampling;

/**
 * Snapshot of the rules used at the time of batch execution.
 * Preserved for traceability — must not be modified after batch completion.
 *
 * TODO(D-011): This is a stub. Replace with a full immutable snapshot containing
 *              snapshot_json and checksum per decision-log.md D-011 and
 *              persistence-model.md §5.9 (sampling_batch_snapshot table).
 */
public class RuleSnapshot {

    private final String projectVersionInfo;
    private final String samplingPolicyInfo;

    public RuleSnapshot(String projectVersionInfo, String samplingPolicyInfo) {
        this.projectVersionInfo = projectVersionInfo;
        this.samplingPolicyInfo = samplingPolicyInfo;
    }

    public String getProjectVersionInfo() { return projectVersionInfo; }
    public String getSamplingPolicyInfo() { return samplingPolicyInfo; }
}
