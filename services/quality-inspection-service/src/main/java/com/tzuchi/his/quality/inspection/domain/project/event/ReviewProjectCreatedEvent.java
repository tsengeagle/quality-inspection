package com.tzuchi.his.quality.inspection.domain.project.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;
import java.time.Instant;

public class ReviewProjectCreatedEvent extends DomainEvent {
    public ReviewProjectCreatedEvent(Object source, String eventId, Instant occurredAt, String actorUserId, String actorUserName,
                                      String projectId, String hospitalId) {
        super(source, eventId, occurredAt, actorUserId, actorUserName,
              hospitalId, "ReviewProject", projectId, "CREATED", null);
    }

    public String getProjectId() { return getEntityId(); }
}
