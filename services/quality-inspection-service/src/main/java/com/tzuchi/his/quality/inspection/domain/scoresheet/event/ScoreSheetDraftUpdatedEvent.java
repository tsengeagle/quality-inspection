package com.tzuchi.his.quality.inspection.domain.scoresheet.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;
import java.time.Instant;

public class ScoreSheetDraftUpdatedEvent extends DomainEvent {

    private final String projectId;

    public ScoreSheetDraftUpdatedEvent(Object source, String eventId, Instant occurredAt, String actorUserId, String actorUserName,
                                        String scoreSheetId, String projectId,
                                        String hospitalId) {
        super(source, eventId, occurredAt, actorUserId, actorUserName,
              hospitalId, "ScoreSheetDefinition", scoreSheetId, "DRAFT_UPDATED",
              "{\"projectId\":\"" + projectId + "\"}");
        this.projectId = projectId;
    }

    public String getScoreSheetId() { return getEntityId(); }
    public String getProjectId() { return projectId; }
}
