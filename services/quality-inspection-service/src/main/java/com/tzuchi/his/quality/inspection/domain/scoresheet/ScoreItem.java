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

    protected ScoreItem() {
        // for JPA
    }

    public ScoreItem(String itemId, String code, String title,
                     ScoreType scoreType, BigDecimal maxScore, BigDecimal weight) {
        Objects.requireNonNull(itemId, "itemId must not be null");
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(scoreType, "scoreType must not be null");
        Objects.requireNonNull(maxScore, "maxScore must not be null");
        if (maxScore.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("maxScore must not be negative");
        }
        this.itemId = itemId;
        this.code = code;
        this.title = title;
        this.scoreType = scoreType;
        this.maxScore = maxScore;
        this.weight = weight != null ? weight : BigDecimal.ONE;
    }

    public String getItemId() { return itemId; }
    public String getCode() { return code; }
    public String getTitle() { return title; }
    public ScoreType getScoreType() { return scoreType; }
    public BigDecimal getMaxScore() { return maxScore; }
    public BigDecimal getWeight() { return weight; }
}
