package com.tzuchi.his.quality.inspection.domain.project.event;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

public class ScoreSheetBoundToProjectEvent extends DomainEvent {

    private final String scoreSheetId;

    public ScoreSheetBoundToProjectEvent(Object source, ActorContext actor,
                                          String projectId, String hospitalId,
                                          String scoreSheetId) {
        super(source, actor.getUserId(), actor.getUserName(),
              hospitalId, "ReviewProject", projectId, "SCORE_SHEET_BOUND",
              "{\"scoreSheetId\":\"" + scoreSheetId + "\"}");
        this.scoreSheetId = scoreSheetId;
    }

    public String getScoreSheetId() { return scoreSheetId; }
}