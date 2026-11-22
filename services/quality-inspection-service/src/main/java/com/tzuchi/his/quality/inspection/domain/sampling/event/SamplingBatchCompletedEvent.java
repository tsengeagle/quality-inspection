package com.tzuchi.his.quality.inspection.domain.sampling.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

public class SamplingBatchCompletedEvent extends DomainEvent {

    private final String projectId;
    private final int selectedCount;
    private final int populationCount;
    private final int eligibleCount;
    private final int excludedCount;

    public SamplingBatchCompletedEvent(Object source, String actorUserId, String actorUserName,
                                        String hospitalId, String batchId, String projectId,
                                        int selectedCount, int populationCount,
                                        int eligibleCount, int excludedCount) {
        super(source, actorUserId, actorUserName,
              hospitalId, "SamplingBatch", batchId, "COMPLETED",
              // D-068 第 3 點：原有三個鍵的順序不變，兩個計數接在後面
              String.format("{\"projectId\":\"%s\",\"selectedCount\":%d,\"populationCount\":%d,"
                              + "\"eligibleCount\":%d,\"excludedCount\":%d}",
                      projectId, selectedCount, populationCount, eligibleCount, excludedCount));
        this.projectId = projectId;
        this.selectedCount = selectedCount;
        this.populationCount = populationCount;
        this.eligibleCount = eligibleCount;
        this.excludedCount = excludedCount;
    }

    public String getBatchId() { return getEntityId(); }
    public String getProjectId() { return projectId; }
    public int getSelectedCount() { return selectedCount; }
    public int getPopulationCount() { return populationCount; }
    public int getEligibleCount() { return eligibleCount; }
    public int getExcludedCount() { return excludedCount; }
}
