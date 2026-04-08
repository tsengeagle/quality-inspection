package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Result of score calculation — scored answers with derived scores and total.
 */
public class ScoreResult {

    private final List<AnswerItem> scoredAnswers;
    private final BigDecimal totalScore;

    public ScoreResult(List<AnswerItem> scoredAnswers, BigDecimal totalScore) {
        this.scoredAnswers = Objects.requireNonNull(scoredAnswers, "scoredAnswers must not be null");
        this.totalScore = Objects.requireNonNull(totalScore, "totalScore must not be null");
    }

    public List<AnswerItem> getScoredAnswers() { return Collections.unmodifiableList(scoredAnswers); }
    public BigDecimal getTotalScore() { return totalScore; }
}
