package com.tzuchi.his.quality.inspection.domain.project.event;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

public class ReviewProjectDeactivatedEvent extends DomainEvent {
    public ReviewProjectDeactivatedEvent(Object source, ActorContext actor,
                                          String projectId, String hospitalId) {
        super(source, actor.getUserId(), actor.getUserName(),
              hospitalId, "ReviewProject", projectId, "DEACTIVATED", null);
    }
}