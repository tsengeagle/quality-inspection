package com.tzuchi.his.quality.inspection.domain.sampling.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

public class ReviewCasesSelectedEvent extends DomainEvent {

    private final int selectedCount;

    public ReviewCasesSelectedEvent(Object source, String actorUserId, String actorUserName,
                                     String hospitalId, String batchId, int selectedCount) {
        super(source, actorUserId, actorUserName,
              hospitalId, "SamplingBatch", batchId, "CASES_SELECTED",
              String.format("{\"selectedCount\":%d}", selectedCount));
        this.selectedCount = selectedCount;
    }

    public String getBatchId() { return getEntityId(); }
    public int getSelectedCount() { return selectedCount; }
}
