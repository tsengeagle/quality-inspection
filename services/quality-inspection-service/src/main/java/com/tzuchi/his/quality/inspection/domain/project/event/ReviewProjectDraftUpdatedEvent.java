package com.tzuchi.his.quality.inspection.domain.project.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

public class ReviewProjectDraftUpdatedEvent extends DomainEvent {
    public ReviewProjectDraftUpdatedEvent(Object source, String actorUserId, String actorUserName,
                                           String projectId, String hospitalId) {
        super(source, actorUserId, actorUserName,
              hospitalId, "ReviewProject", projectId, "DRAFT_UPDATED", null);
    }

    public String getProjectId() { return getEntityId(); }
}
