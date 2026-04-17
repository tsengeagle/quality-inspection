package com.tzuchi.his.quality.inspection.application.sampling;

import com.tzuchi.his.quality.inspection.application.common.Page;

import java.util.Optional;

/**
 * Read-model query port for SamplingBatch summaries and details.
 * Implementations are in the infrastructure layer.
 */
public interface SamplingBatchQueryPort {
    Page<SamplingBatchSummary> findBatches(SamplingBatchQueryCriteria criteria);
    Optional<SamplingBatchDetailView> findDetailById(String batchId);
    Optional<SamplingBatchDetailView> findLatestDetailByProjectId(String projectId);
}
