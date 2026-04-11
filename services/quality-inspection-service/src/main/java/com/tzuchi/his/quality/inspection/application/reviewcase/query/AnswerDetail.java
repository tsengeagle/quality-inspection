package com.tzuchi.his.quality.inspection.application.reviewcase.query;

import java.math.BigDecimal;

public class AnswerDetail {

    private final String itemId;
    private final String answerValue;
    private final BigDecimal derivedScore;

    public AnswerDetail(String itemId, String answerValue, BigDecimal derivedScore) {
        this.itemId = itemId;
        this.answerValue = answerValue;
        this.derivedScore = derivedScore;
    }

    public String getItemId() { return itemId; }
    public String getAnswerValue() { return answerValue; }
    public BigDecimal getDerivedScore() { return derivedScore; }
}
