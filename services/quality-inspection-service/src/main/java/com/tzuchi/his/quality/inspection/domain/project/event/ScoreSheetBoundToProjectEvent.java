package com.tzuchi.his.quality.inspection.domain.project.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

public class ScoreSheetBoundToProjectEvent extends DomainEvent {

    private final String scoreSheetId;

    public ScoreSheetBoundToProjectEvent(Object source, String actorUserId, String actorUserName,
                                          String projectId, String hospitalId,
                                          String scoreSheetId) {
        super(source, actorUserId, actorUserName,
              hospitalId, "ReviewProject", projectId, "SCORE_SHEET_BOUND",
              "{\"scoreSheetId\":\"" + scoreSheetId + "\"}");
        this.scoreSheetId = scoreSheetId;
    }

    public String getScoreSheetId() { return scoreSheetId; }
}