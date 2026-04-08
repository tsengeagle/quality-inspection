package com.tzuchi.his.quality.inspection.domain.project.event;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

public class ReviewProjectCreatedEvent extends DomainEvent {
    public ReviewProjectCreatedEvent(Object source, ActorContext actor,
                                      String projectId, String hospitalId) {
        super(source, actor.getUserId(), actor.getUserName(),
              hospitalId, "ReviewProject", projectId, "CREATED", null);
    }
}