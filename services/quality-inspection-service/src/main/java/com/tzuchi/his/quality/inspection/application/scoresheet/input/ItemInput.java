package com.tzuchi.his.quality.inspection.application.scoresheet.input;

import java.math.BigDecimal;

/** 題目的輸入（specs/score-sheet/usecases.yaml 的 input_types ItemInput）。 */
public class ItemInput {

    private String itemId;
    private String code;
    private String title;
    private String scoreType;     // 原始輸入；不認得的值由 domain 回 BAD_REQUEST（SS-UPD-13，D-089）
    private BigDecimal maxScore;
    private BigDecimal weight;
    private String requiredRule;  // 同上
    private String conditionExpression;

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getScoreType() { return scoreType; }
    public void setScoreType(String scoreType) { this.scoreType = scoreType; }

    public BigDecimal getMaxScore() { return maxScore; }
    public void setMaxScore(BigDecimal maxScore) { this.maxScore = maxScore; }

    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }

    public String getRequiredRule() { return requiredRule; }
    public void setRequiredRule(String requiredRule) { this.requiredRule = requiredRule; }

    public String getConditionExpression() { return conditionExpression; }
    public void setConditionExpression(String conditionExpression) { this.conditionExpression = conditionExpression; }
}
