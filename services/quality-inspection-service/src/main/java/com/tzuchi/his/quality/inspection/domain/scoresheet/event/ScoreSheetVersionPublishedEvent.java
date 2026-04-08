package com.tzuchi.his.quality.inspection.domain.scoresheet.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

public class ScoreSheetVersionPublishedEvent extends DomainEvent {
    public ScoreSheetVersionPublishedEvent(Object source, String actorUserId, String actorUserName,
                                            String scoreSheetId, String projectId,
                                            String hospitalId) {
        super(source, actorUserId, actorUserName,
              hospitalId, "ScoreSheetDefinition", scoreSheetId, "VERSION_PUBLISHED",
              "{\"projectId\":\"" + projectId + "\"}");
    }
}
