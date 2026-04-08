package com.tzuchi.his.quality.inspection.domain.scoresheet;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * A single scorable item within a score sheet section.
 */
public class ScoreItem {

    private String itemId;
    private String code;
    private String title;
    private ScoreType scoreType;
    private BigDecimal maxScore;
    private BigDecimal weight;
    private RequiredRule requiredRule;
    private String conditionExpression;

    protected ScoreItem() {
        // for JPA
    }

    public ScoreItem(String itemId, String code, String title,
                     ScoreType scoreType, BigDecimal maxScore, BigDecimal weight,
                     RequiredRule requiredRule, String conditionExpression) {
        Objects.requireNonNull(itemId, "itemId must not be null");
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(scoreType, "scoreType must not be null");
        Objects.requireNonNull(maxScore, "maxScore must not be null");
        if (maxScore.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("maxScore must not be negative");
        }
        Objects.requireNonNull(requiredRule, "requiredRule must not be null");
        if (requiredRule == RequiredRule.CONDITIONAL) {
            if (conditionExpression == null || conditionExpression.isBlank()) {
                throw new IllegalArgumentException(
                        "conditionExpression must not be blank when requiredRule is CONDITIONAL");
            }
        }
        this.itemId = itemId;
        this.code = code;
        this.title = title;
        this.scoreType = scoreType;
        this.maxScore = maxScore;
        this.weight = weight != null ? weight : BigDecimal.ONE;
        this.requiredRule = requiredRule;
        this.conditionExpression = conditionExpression;
    }

    public ScoreItem(String itemId, String code, String title,
                     ScoreType scoreType, BigDecimal maxScore, BigDecimal weight) {
        this(itemId, code, title, scoreType, maxScore, weight,
             RequiredRule.ALWAYS_REQUIRED, null);
    }

    public String getItemId() { return itemId; }
    public String getCode() { return code; }
    public String getTitle() { return title; }
    public ScoreType getScoreType() { return scoreType; }
    public BigDecimal getMaxScore() { return maxScore; }
    public BigDecimal getWeight() { return weight; }
    public RequiredRule getRequiredRule() { return requiredRule; }
    public String getConditionExpression() { return conditionExpression; }
}
