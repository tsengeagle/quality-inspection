package com.tzuchi.his.quality.inspection.domain.project.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

public class ReviewProjectActivatedEvent extends DomainEvent {
    public ReviewProjectActivatedEvent(Object source, String actorUserId, String actorUserName,
                                        String projectId, String hospitalId) {
        super(source, actorUserId, actorUserName,
              hospitalId, "ReviewProject", projectId, "ACTIVATED", null);
    }

    public String getProjectId() { return getEntityId(); }
}
