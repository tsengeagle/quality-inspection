package com.tzuchi.his.quality.inspection.application.sampling;

import com.tzuchi.his.quality.inspection.domain.sampling.BatchStatus;
import com.tzuchi.his.quality.inspection.domain.sampling.ExecutionMode;

/**
 * Criteria for querying sampling batches.
 */
public class SamplingBatchQueryCriteria {

    private final String projectId;
    private final BatchStatus status;
    private final ExecutionMode executionMode;
    private final int page;
    private final int size;

    public SamplingBatchQueryCriteria(String projectId, BatchStatus status,
                                      ExecutionMode executionMode, int page, int size) {
        this.projectId = projectId;
        this.status = status;
        this.executionMode = executionMode;
        this.page = Math.max(0, page);
        this.size = size > 0 ? Math.min(size, 100) : 20;
    }

    public String getProjectId() { return projectId; }
    public BatchStatus getStatus() { return status; }
    public ExecutionMode getExecutionMode() { return executionMode; }
    public int getPage() { return page; }
    public int getSize() { return size; }
}
