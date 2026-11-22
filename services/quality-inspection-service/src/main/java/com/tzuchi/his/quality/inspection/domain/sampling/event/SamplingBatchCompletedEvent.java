package com.tzuchi.his.quality.inspection.domain.sampling.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

public class SamplingBatchCompletedEvent extends DomainEvent {

    private final String projectId;

    public SamplingBatchCompletedEvent(Object source, String actorUserId, String actorUserName,
                                        String hospitalId, String batchId, String projectId,
                                        int selectedCount, int populationCount) {
        super(source, actorUserId, actorUserName,
              hospitalId, "SamplingBatch", batchId, "COMPLETED",
              String.format("{\"projectId\":\"%s\",\"selectedCount\":%d,\"populationCount\":%d}",
                      projectId, selectedCount, populationCount));
        this.projectId = projectId;
    }

    public String getBatchId() { return getEntityId(); }
    public String getProjectId() { return projectId; }
}
