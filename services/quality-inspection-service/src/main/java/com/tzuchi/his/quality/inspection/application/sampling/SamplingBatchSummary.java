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
    private final String projectName;
    private final ExecutionMode executionMode;
    private final BatchStatus status;
    private final int populationCount;
    private final int eligibleCount;
    private final int excludedCount;
    private final int selectedCount;
    private final String projectVersionId;
    private final int projectVersionNumber;
    private final String retryOfBatchId;
    private final Instant startedAt;
    private final Instant finishedAt;

    public SamplingBatchSummary(String batchId, String projectId, String projectName, ExecutionMode executionMode,
                                 BatchStatus status, int populationCount, int eligibleCount,
                                 int excludedCount, int selectedCount, String projectVersionId,
                                 int projectVersionNumber, String retryOfBatchId,
                                 Instant startedAt, Instant finishedAt) {
        this.batchId = batchId;
        this.projectId = projectId;
        this.projectName = projectName;
        this.executionMode = executionMode;
        this.status = status;
        this.populationCount = populationCount;
        this.eligibleCount = eligibleCount;
        this.excludedCount = excludedCount;
        this.selectedCount = selectedCount;
        this.projectVersionId = projectVersionId;
        this.projectVersionNumber = projectVersionNumber;
        this.retryOfBatchId = retryOfBatchId;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
    }

    public String getBatchId() { return batchId; }
    public String getProjectId() { return projectId; }
    public String getProjectName() { return projectName; }
    public ExecutionMode getExecutionMode() { return executionMode; }
    public BatchStatus getStatus() { return status; }
    public int getPopulationCount() { return populationCount; }
    public int getEligibleCount() { return eligibleCount; }
    public int getExcludedCount() { return excludedCount; }
    public int getSelectedCount() { return selectedCount; }
    public String getProjectVersionId() { return projectVersionId; }
    public int getProjectVersionNumber() { return projectVersionNumber; }
    public String getRetryOfBatchId() { return retryOfBatchId; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
}
