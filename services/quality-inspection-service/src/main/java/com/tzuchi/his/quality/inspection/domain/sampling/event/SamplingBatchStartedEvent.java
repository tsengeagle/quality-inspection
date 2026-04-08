package com.tzuchi.his.quality.inspection.domain.sampling.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

public class SamplingBatchStartedEvent extends DomainEvent {
    public SamplingBatchStartedEvent(Object source, String actorUserId, String actorUserName,
                                      String hospitalId, String batchId, String projectId,
                                      String executionMode) {
        super(source, actorUserId, actorUserName,
              hospitalId, "SamplingBatch", batchId, "STARTED",
              String.format("{\"projectId\":\"%s\",\"executionMode\":\"%s\"}", projectId, executionMode));
    }
}
