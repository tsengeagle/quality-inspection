package com.tzuchi.his.quality.inspection.domain.sampling.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;
import java.time.Instant;

public class ReviewCasesSelectedEvent extends DomainEvent {

    private final int selectedCount;

    public ReviewCasesSelectedEvent(Object source, String eventId, Instant occurredAt, String actorUserId, String actorUserName,
                                     String hospitalId, String batchId, int selectedCount) {
        super(source, eventId, occurredAt, actorUserId, actorUserName,
              hospitalId, "SamplingBatch", batchId, "CASES_SELECTED",
              String.format("{\"selectedCount\":%d}", selectedCount));
        this.selectedCount = selectedCount;
    }

    public String getBatchId() { return getEntityId(); }
    public int getSelectedCount() { return selectedCount; }
}
