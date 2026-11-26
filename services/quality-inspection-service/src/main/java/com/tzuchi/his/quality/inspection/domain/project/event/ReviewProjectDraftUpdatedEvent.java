package com.tzuchi.his.quality.inspection.domain.project.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;
import java.time.Instant;

public class ReviewProjectDraftUpdatedEvent extends DomainEvent {
    public ReviewProjectDraftUpdatedEvent(Object source, String eventId, Instant occurredAt, String actorUserId, String actorUserName,
                                           String projectId, String hospitalId) {
        super(source, eventId, occurredAt, actorUserId, actorUserName,
              hospitalId, "ReviewProject", projectId, "DRAFT_UPDATED", null);
    }

    public String getProjectId() { return getEntityId(); }
}
