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
    public String getProjectId() { return getEntityId(); }
    /** D-090 第 4 點：detail 裡 scoreSheetId 的值，是評分表版本的 id。 */
    public String getScoreSheetVersionId() { return scoreSheetId; }
}