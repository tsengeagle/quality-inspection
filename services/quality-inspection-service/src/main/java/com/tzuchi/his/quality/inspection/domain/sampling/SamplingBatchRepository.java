package com.tzuchi.his.quality.inspection.domain.sampling;

import java.util.Optional;

public interface SamplingBatchRepository {

    void save(SamplingBatch batch);

    Optional<SamplingBatch> findById(String batchId);
}
