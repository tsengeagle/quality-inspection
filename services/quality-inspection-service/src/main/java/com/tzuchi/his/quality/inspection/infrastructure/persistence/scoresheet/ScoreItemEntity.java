package com.tzuchi.his.quality.inspection.infrastructure.persistence.scoresheet;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "score_sheet_item")
public class ScoreItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_id", nullable = false)
    private String itemId;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String title;

    @Column(name = "score_type", nullable = false)
    private String scoreType;

    @Column(name = "max_score", nullable = false)
    private BigDecimal maxScore;

    @Column(nullable = false)
    private BigDecimal weight;

    @Column(name = "required_rule", nullable = false)
    private String requiredRule;

    @Column(name = "condition_expression")
    private String conditionExpression;

    public ScoreItemEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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
