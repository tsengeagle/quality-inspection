package com.tzuchi.his.quality.inspection.application.sampling;

import com.tzuchi.his.quality.inspection.domain.sampling.BatchStatus;
import com.tzuchi.his.quality.inspection.domain.sampling.ExecutionMode;

import java.time.Instant;
import java.util.List;

/**
 * Read model for sampling batch detail view.
 */
public class SamplingBatchDetailView {

    private final String batchId;
    private final String projectId;
    private final ExecutionMode executionMode;
    private final BatchStatus status;
    private final int populationCount;
    private final int eligibleCount;
    private final int excludedCount;
    private final int selectedCount;
    private final String retryOfBatchId;
    private final String failureReason;
    private final Instant startedAt;
    private final Instant finishedAt;
    private final RuleSnapshotSummary ruleSnapshot;
    private final List<SelectedCaseRefView> selectedCases;

    public SamplingBatchDetailView(String batchId, String projectId, ExecutionMode executionMode,
                                    BatchStatus status, int populationCount, int eligibleCount,
                                    int excludedCount, int selectedCount,
                                    String retryOfBatchId, String failureReason,
                                    Instant startedAt, Instant finishedAt,
                                    RuleSnapshotSummary ruleSnapshot,
                                    List<SelectedCaseRefView> selectedCases) {
        this.batchId = batchId;
        this.projectId = projectId;
        this.executionMode = executionMode;
        this.status = status;
        this.populationCount = populationCount;
        this.eligibleCount = eligibleCount;
        this.excludedCount = excludedCount;
        this.selectedCount = selectedCount;
        this.retryOfBatchId = retryOfBatchId;
        this.failureReason = failureReason;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.ruleSnapshot = ruleSnapshot;
        this.selectedCases = selectedCases;
    }

    /** 同一個批次的 metadata：沒有 selectedCases（SB-DET-02）。 */
    public SamplingBatchDetailView withoutSelectedCases() {
        return new SamplingBatchDetailView(batchId, projectId, executionMode, status, populationCount,
                eligibleCount, excludedCount, selectedCount, retryOfBatchId, failureReason,
                startedAt, finishedAt, ruleSnapshot, null);
    }

    public String getBatchId() { return batchId; }
    public String getProjectId() { return projectId; }
    public ExecutionMode getExecutionMode() { return executionMode; }
    public BatchStatus getStatus() { return status; }
    public int getPopulationCount() { return populationCount; }
    public int getEligibleCount() { return eligibleCount; }
    public int getExcludedCount() { return excludedCount; }
    public int getSelectedCount() { return selectedCount; }
    public String getRetryOfBatchId() { return retryOfBatchId; }
    public String getFailureReason() { return failureReason; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public RuleSnapshotSummary getRuleSnapshot() { return ruleSnapshot; }
    public List<SelectedCaseRefView> getSelectedCases() { return selectedCases; }
}
