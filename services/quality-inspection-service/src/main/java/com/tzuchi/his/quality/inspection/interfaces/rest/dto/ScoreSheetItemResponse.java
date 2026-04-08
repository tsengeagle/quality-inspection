package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import java.math.BigDecimal;

public class ScoreSheetItemResponse {
    private String itemId;
    private String code;
    private String title;
    private String scoreType;
    private BigDecimal maxScore;
    private BigDecimal weight;
    private String requiredRule;
    private String conditionExpression;

    public ScoreSheetItemResponse(String itemId, String code, String title, String scoreType,
                                   BigDecimal maxScore, BigDecimal weight,
                                   String requiredRule, String conditionExpression) {
        this.itemId = itemId;
        this.code = code;
        this.title = title;
        this.scoreType = scoreType;
        this.maxScore = maxScore;
        this.weight = weight;
        this.requiredRule = requiredRule;
        this.conditionExpression = conditionExpression;
    }

    public String getItemId() { return itemId; }
    public String getCode() { return code; }
    public String getTitle() { return title; }
    public String getScoreType() { return scoreType; }
    public BigDecimal getMaxScore() { return maxScore; }
    public BigDecimal getWeight() { return weight; }
    public String getRequiredRule() { return requiredRule; }
    public String getConditionExpression() { return conditionExpression; }
}
