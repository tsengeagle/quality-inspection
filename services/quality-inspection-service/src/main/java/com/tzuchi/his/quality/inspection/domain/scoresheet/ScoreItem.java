package com.tzuchi.his.quality.inspection.domain.scoresheet;

import com.tzuchi.his.quality.inspection.domain.common.DomainValidationException;

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

    /**
     * 欄位驗證依 D-050 第 4 點（SS-UPD-04、06、07、08），違反時回 VALIDATION_ERROR。
     * 沒有輸入 weight 時為 1（SS-UPD-S3）。requiredRule 不是 CONDITIONAL 時，空白的 conditionExpression 視為沒有值。
     */
    public ScoreItem(String itemId, String code, String title,
                     ScoreType scoreType, BigDecimal maxScore, BigDecimal weight,
                     RequiredRule requiredRule, String conditionExpression) {
        requireText(itemId, "itemId");
        requireText(code, "code");
        requireText(title, "title");
        requireValue(scoreType, "scoreType");
        requireValue(maxScore, "maxScore");
        requireValue(requiredRule, "requiredRule");
        if (maxScore.compareTo(BigDecimal.ZERO) < 0) {
            throw invalid("maxScore must not be negative");
        }
        boolean hasExpression = conditionExpression != null && !conditionExpression.isBlank();
        if (requiredRule == RequiredRule.CONDITIONAL && !hasExpression) {
            throw invalid("conditionExpression must not be blank when requiredRule is CONDITIONAL");
        }
        if (requiredRule != RequiredRule.CONDITIONAL && hasExpression) {
            throw invalid("conditionExpression is only allowed when requiredRule is CONDITIONAL");
        }
        this.itemId = itemId;
        this.code = code;
        this.title = title;
        this.scoreType = scoreType;
        this.maxScore = maxScore;
        this.weight = weight != null ? weight : BigDecimal.ONE;
        this.requiredRule = requiredRule;
        this.conditionExpression = hasExpression ? conditionExpression : null;
    }

    static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw invalid(field + " must not be blank");
        }
    }

    private static void requireValue(Object value, String field) {
        if (value == null) {
            throw invalid(field + " must not be null");
        }
    }

    static DomainValidationException invalid(String message) {
        return new DomainValidationException("VALIDATION_ERROR", message);
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
