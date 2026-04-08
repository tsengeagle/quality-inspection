package com.tzuchi.his.quality.inspection.application.sampling;

import com.tzuchi.his.quality.inspection.domain.sampling.BatchStatus;
import com.tzuchi.his.quality.inspection.domain.sampling.ExecutionMode;

import java.time.Instant;

/**
 * Read model for sampling batch list view.
 */
public class SamplingBatchSummary {

    private final String batchId;
    private final String projectId;
    private final ExecutionMode executionMode;
    private final BatchStatus status;
    private final int populationCount;
    private final int eligibleCount;
    private final int selectedCount;
    private final String retryOfBatchId;
    private final Instant startedAt;
    private final Instant finishedAt;

    public SamplingBatchSummary(String batchId, String projectId, ExecutionMode executionMode,
                                 BatchStatus status, int populationCount, int eligibleCount,
                                 int selectedCount, String retryOfBatchId,
                                 Instant startedAt, Instant finishedAt) {
        this.batchId = batchId;
        this.projectId = projectId;
        this.executionMode = executionMode;
        this.status = status;
        this.populationCount = populationCount;
        this.eligibleCount = eligibleCount;
        this.selectedCount = selectedCount;
        this.retryOfBatchId = retryOfBatchId;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
    }

    public String getBatchId() { return batchId; }
    public String getProjectId() { return projectId; }
    public ExecutionMode getExecutionMode() { return executionMode; }
    public BatchStatus getStatus() { return status; }
    public int getPopulationCount() { return populationCount; }
    public int getEligibleCount() { return eligibleCount; }
    public int getSelectedCount() { return selectedCount; }
    public String getRetryOfBatchId() { return retryOfBatchId; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
}
