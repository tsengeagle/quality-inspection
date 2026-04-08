package com.tzuchi.his.quality.inspection.domain.project.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

public class ReviewProjectDeactivatedEvent extends DomainEvent {
    public ReviewProjectDeactivatedEvent(Object source, String actorUserId, String actorUserName,
                                          String projectId, String hospitalId) {
        super(source, actorUserId, actorUserName,
              hospitalId, "ReviewProject", projectId, "DEACTIVATED", null);
    }
}