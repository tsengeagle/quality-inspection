package com.tzuchi.his.quality.inspection.application.scoresheet.input;

import com.tzuchi.his.quality.inspection.domain.scoresheet.RequiredRule;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreType;

import java.math.BigDecimal;

/** 新增到既有章節的題目（input_types ItemToSectionInput）。 */
public class ItemToSectionInput {

    private String targetSectionId;
    private String itemId;
    private String code;
    private String title;
    private ScoreType scoreType;
    private BigDecimal maxScore;
    private BigDecimal weight;
    private RequiredRule requiredRule;
    private String conditionExpression;

    public String getTargetSectionId() { return targetSectionId; }
    public void setTargetSectionId(String targetSectionId) { this.targetSectionId = targetSectionId; }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public ScoreType getScoreType() { return scoreType; }
    public void setScoreType(ScoreType scoreType) { this.scoreType = scoreType; }

    public BigDecimal getMaxScore() { return maxScore; }
    public void setMaxScore(BigDecimal maxScore) { this.maxScore = maxScore; }

    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }

    public RequiredRule getRequiredRule() { return requiredRule; }
    public void setRequiredRule(RequiredRule requiredRule) { this.requiredRule = requiredRule; }

    public String getConditionExpression() { return conditionExpression; }
    public void setConditionExpression(String conditionExpression) { this.conditionExpression = conditionExpression; }
}
