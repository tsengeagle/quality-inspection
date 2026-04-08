package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import javax.validation.constraints.NotBlank;

public class BindScoreSheetRequest {

    @NotBlank(message = "must not be blank")
    private String scoreSheetId;

    public String getScoreSheetId() { return scoreSheetId; }
    public void setScoreSheetId(String scoreSheetId) { this.scoreSheetId = scoreSheetId; }
}