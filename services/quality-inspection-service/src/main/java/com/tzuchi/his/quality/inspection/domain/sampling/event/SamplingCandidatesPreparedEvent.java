package com.tzuchi.his.quality.inspection.domain.sampling.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

public class SamplingCandidatesPreparedEvent extends DomainEvent {
    public SamplingCandidatesPreparedEvent(Object source, String actorUserId, String actorUserName,
                                            String hospitalId, String batchId,
                                            int populationCount, int eligibleCount, int excludedCount) {
        super(source, actorUserId, actorUserName,
              hospitalId, "SamplingBatch", batchId, "CANDIDATES_PREPARED",
              String.format("{\"populationCount\":%d,\"eligibleCount\":%d,\"excludedCount\":%d}",
                      populationCount, eligibleCount, excludedCount));
    }

    public String getBatchId() { return getEntityId(); }
}
