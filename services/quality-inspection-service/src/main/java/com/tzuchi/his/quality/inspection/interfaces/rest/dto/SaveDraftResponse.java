package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import java.math.BigDecimal;

public class SaveDraftResponse {
    private BigDecimal totalScore;

    public SaveDraftResponse() {}

    public SaveDraftResponse(BigDecimal totalScore) {
        this.totalScore = totalScore;
    }

    public BigDecimal getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(BigDecimal totalScore) {
        this.totalScore = totalScore;
    }
}
