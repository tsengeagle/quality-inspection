package com.tzuchi.his.quality.inspection.application.reviewcase.dto;

public class AnswerInput {
    private final String itemId;
    private final String answerValue;

    public AnswerInput(String itemId, String answerValue) {
        this.itemId = itemId;
        this.answerValue = answerValue;
    }

    public String getItemId() {
        return itemId;
    }

    public String getAnswerValue() {
        return answerValue;
    }
}
