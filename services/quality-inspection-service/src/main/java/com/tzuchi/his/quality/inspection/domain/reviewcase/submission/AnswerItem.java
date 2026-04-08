package com.tzuchi.his.quality.inspection.domain.reviewcase.submission;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Immutable value object representing a single answer within a review submission.
 * answerValue is always String (unified serialization).
 * derivedScore is computed by ScoreCalculationService.
 */
public class AnswerItem {

    private final String itemId;
    private final String answerValue;
    private final BigDecimal derivedScore;

    private AnswerItem(String itemId, String answerValue, BigDecimal derivedScore) {
        this.itemId = itemId;
        this.answerValue = answerValue;
        this.derivedScore = derivedScore;
    }

    /** Frontend-submitted raw answer — no derived score yet. */
    public static AnswerItem ofRaw(String itemId, String answerValue) {
        Objects.requireNonNull(itemId, "itemId must not be null");
        Objects.requireNonNull(answerValue, "answerValue must not be null");
        return new AnswerItem(itemId, answerValue, null);
    }

    /** Server-scored answer with derived score. */
    public static AnswerItem ofScored(String itemId, String answerValue, BigDecimal derivedScore) {
        Objects.requireNonNull(itemId, "itemId must not be null");
        Objects.requireNonNull(answerValue, "answerValue must not be null");
        Objects.requireNonNull(derivedScore, "derivedScore must not be null");
        return new AnswerItem(itemId, answerValue, derivedScore);
    }

    /** Reconstitute from persistence — no validation. */
    public static AnswerItem reconstitute(String itemId, String answerValue, BigDecimal derivedScore) {
        return new AnswerItem(itemId, answerValue, derivedScore);
    }

    public String getItemId() { return itemId; }
    public String getAnswerValue() { return answerValue; }
    public BigDecimal getDerivedScore() { return derivedScore; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AnswerItem)) return false;
        AnswerItem that = (AnswerItem) o;
        return Objects.equals(itemId, that.itemId) &&
                Objects.equals(answerValue, that.answerValue) &&
                Objects.equals(derivedScore, that.derivedScore);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemId, answerValue, derivedScore);
    }
}
