package com.tzuchi.his.quality.inspection.domain.scoresheet.event;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

public class ScoreSheetDraftCreatedEvent extends DomainEvent {
    public ScoreSheetDraftCreatedEvent(Object source, ActorContext actor,
                                        String scoreSheetId, String projectId,
                                        String hospitalId) {
        super(source, actor.getUserId(), actor.getUserName(),
              hospitalId, "ScoreSheetDefinition", scoreSheetId, "DRAFT_CREATED",
              "{\"projectId\":\"" + projectId + "\"}");
    }
}
