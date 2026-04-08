package com.tzuchi.his.quality.inspection.domain.scoresheet.event;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

public class ScoreSheetDraftUpdatedEvent extends DomainEvent {
    public ScoreSheetDraftUpdatedEvent(Object source, ActorContext actor,
                                        String scoreSheetId, String projectId,
                                        String hospitalId) {
        super(source, actor.getUserId(), actor.getUserName(),
              hospitalId, "ScoreSheetDefinition", scoreSheetId, "DRAFT_UPDATED",
              "{\"projectId\":\"" + projectId + "\"}");
    }
}
