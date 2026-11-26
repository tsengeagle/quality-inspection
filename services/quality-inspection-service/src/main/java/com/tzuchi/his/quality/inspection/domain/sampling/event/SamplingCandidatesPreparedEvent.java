package com.tzuchi.his.quality.inspection.domain.sampling.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;
import java.time.Instant;

public class SamplingCandidatesPreparedEvent extends DomainEvent {

    private final int populationCount;
    private final int eligibleCount;
    private final int excludedCount;

    public SamplingCandidatesPreparedEvent(Object source, String eventId, Instant occurredAt, String actorUserId, String actorUserName,
                                            String hospitalId, String batchId,
                                            int populationCount, int eligibleCount, int excludedCount) {
        super(source, eventId, occurredAt, actorUserId, actorUserName,
              hospitalId, "SamplingBatch", batchId, "CANDIDATES_PREPARED",
              String.format("{\"populationCount\":%d,\"eligibleCount\":%d,\"excludedCount\":%d}",
                      populationCount, eligibleCount, excludedCount));
        this.populationCount = populationCount;
        this.eligibleCount = eligibleCount;
        this.excludedCount = excludedCount;
    }

    public String getBatchId() { return getEntityId(); }
    public int getPopulationCount() { return populationCount; }
    public int getEligibleCount() { return eligibleCount; }
    public int getExcludedCount() { return excludedCount; }
}
