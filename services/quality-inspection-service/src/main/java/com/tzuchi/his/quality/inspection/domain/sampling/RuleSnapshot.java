package com.tzuchi.his.quality.inspection.domain.sampling;

/**
 * Snapshot of the rules used at the time of batch execution.
 * Preserved for traceability — must not be modified after batch completion.
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
