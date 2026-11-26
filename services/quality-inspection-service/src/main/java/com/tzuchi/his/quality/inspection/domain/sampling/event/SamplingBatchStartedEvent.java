package com.tzuchi.his.quality.inspection.domain.sampling.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;
import java.time.Instant;

public class SamplingBatchStartedEvent extends DomainEvent {

    private final String projectId;

    public SamplingBatchStartedEvent(Object source, String eventId, Instant occurredAt, String actorUserId, String actorUserName,
                                      String hospitalId, String batchId, String projectId,
                                      String executionMode) {
        super(source, eventId, occurredAt, actorUserId, actorUserName,
              hospitalId, "SamplingBatch", batchId, "STARTED",
              String.format("{\"projectId\":\"%s\",\"executionMode\":\"%s\"}", projectId, executionMode));
        this.projectId = projectId;
    }

    public String getBatchId() { return getEntityId(); }
    public String getProjectId() { return projectId; }
}
