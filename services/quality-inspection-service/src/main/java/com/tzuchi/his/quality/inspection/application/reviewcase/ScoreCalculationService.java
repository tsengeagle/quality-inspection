package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreItem;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetDefinition;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetSection;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Server-side scoring engine.
 * Calculates derivedScore for each answer based on ScoreSheetDefinition rules.
 */
@Service
public class ScoreCalculationService {

    /**
     * Calculate scores for raw answers against the score sheet definition.
     *
     * @param rawAnswers  frontend-submitted answers (itemId + answerValue only)
     * @param scoreSheet  the published ScoreSheetDefinition
     * @return ScoreResult with scored answers and total score
     */
    public ScoreResult calculate(List<AnswerItem> rawAnswers, ScoreSheetDefinition scoreSheet) {
        Objects.requireNonNull(rawAnswers, "rawAnswers must not be null");
        Objects.requireNonNull(scoreSheet, "scoreSheet must not be null");
        // Build itemId → ScoreItem lookup map from all sections
        Map<String, ScoreItem> itemMap = new HashMap<>();
        for (ScoreSheetSection section : scoreSheet.getSections()) {
            for (ScoreItem item : section.getItems()) {
                itemMap.put(item.getItemId(), item);
            }
        }

        List<AnswerItem> scoredAnswers = new ArrayList<>();
        BigDecimal totalScore = BigDecimal.ZERO;

        for (AnswerItem raw : rawAnswers) {
            ScoreItem definition = itemMap.get(raw.getItemId());
            if (definition == null) {
                // Unknown itemId — ignore per spec
                continue;
            }

            BigDecimal derivedScore = computeScore(raw, definition);
            scoredAnswers.add(AnswerItem.ofScored(raw.getItemId(), raw.getAnswerValue(), derivedScore));
            totalScore = totalScore.add(derivedScore);
        }

        return new ScoreResult(scoredAnswers, totalScore);
    }

    private BigDecimal computeScore(AnswerItem raw, ScoreItem definition) {
        BigDecimal weight = definition.getWeight();

        switch (definition.getScoreType()) {
            case BOOLEAN:
                return "true".equals(raw.getAnswerValue())
                        ? definition.getMaxScore().multiply(weight)
                        : BigDecimal.ZERO;

            case FIXED:
                return definition.getMaxScore().multiply(weight);

            case RANGE:
                BigDecimal value;
                try {
                    value = new BigDecimal(raw.getAnswerValue());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                            "RANGE answer for item " + raw.getItemId()
                                    + " must be numeric, got: " + raw.getAnswerValue());
                }
                if (value.compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalArgumentException(
                            "RANGE answer for item " + raw.getItemId()
                                    + " must not be negative, got: " + raw.getAnswerValue());
                }
                BigDecimal capped = value.min(definition.getMaxScore());
                return capped.multiply(weight);

            default:
                throw new IllegalStateException("Unknown ScoreType: " + definition.getScoreType());
        }
    }
}
