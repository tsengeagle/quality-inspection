package com.tzuchi.his.quality.inspection.interfaces.rest.dto;


public class BindScoreSheetRequest {

    private String scoreSheetVersionId;

    /** 評分表版本 id：發布評分表時回傳（api-spec §7.6、D-050 第 1 點） */
    public String getScoreSheetVersionId() { return scoreSheetVersionId; }
    public void setScoreSheetVersionId(String scoreSheetVersionId) { this.scoreSheetVersionId = scoreSheetVersionId; }
}