package com.tzuchi.his.quality.inspection.domain.sampling.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

public class SamplingBatchFailedEvent extends DomainEvent {
    public SamplingBatchFailedEvent(Object source, String actorUserId, String actorUserName,
                                     String hospitalId, String batchId, String failureReason) {
        super(source, actorUserId, actorUserName,
              hospitalId, "SamplingBatch", batchId, "FAILED",
              String.format("{\"failureReason\":\"%s\"}", failureReason));
    }
}
