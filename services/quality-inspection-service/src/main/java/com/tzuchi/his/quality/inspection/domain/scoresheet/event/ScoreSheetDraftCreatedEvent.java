package com.tzuchi.his.quality.inspection.domain.scoresheet.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

public class ScoreSheetDraftCreatedEvent extends DomainEvent {
    public ScoreSheetDraftCreatedEvent(Object source, String actorUserId, String actorUserName,
                                        String scoreSheetId, String projectId,
                                        String hospitalId) {
        super(source, actorUserId, actorUserName,
              hospitalId, "ScoreSheetDefinition", scoreSheetId, "DRAFT_CREATED",
              "{\"projectId\":\"" + projectId + "\"}");
    }
}
