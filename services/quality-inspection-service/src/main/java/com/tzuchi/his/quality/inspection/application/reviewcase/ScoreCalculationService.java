package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.domain.common.DomainValidationException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreItem;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetDefinition;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetSection;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Server-side scoring engine.
 * Calculates derivedScore for each answer based on ScoreSheetDefinition rules.
 */
@Service
public class ScoreCalculationService {

    /** D-085 第 4 點：題目分數與總分四捨五入到兩位小數。 */
    private static final int SCORE_SCALE = 2;

    /**
     * RC-INV-07（D-085 第 1～7 點）：計分用的作答必須合法，否則 400 VALIDATION_ERROR。
     * 每一筆都有 itemId 與 answerValue；itemId 是評分表的題目；同一題只作答一次；
     * BOOLEAN 是 true 或 false；RANGE 是 0 到滿分之間的數字，最多兩位小數；FIXED 的作答內容不檢查。
     *
     * <p>use case 在狀態、指派、提交歸屬的檢查之後、必答題的檢查之前呼叫（D-085 第 8 點）。
     */
    public void requireValidAnswers(List<AnswerItem> answers, ScoreSheetDefinition scoreSheet) {
        Objects.requireNonNull(answers, "answers must not be null");
        Objects.requireNonNull(scoreSheet, "scoreSheet must not be null");
        Map<String, ScoreItem> itemMap = itemsById(scoreSheet);
        Set<String> seen = new HashSet<>();

        for (AnswerItem answer : answers) {
            if (answer == null || answer.getItemId() == null) {
                throw invalid("Every answer must have an itemId");
            }
            String itemId = answer.getItemId();
            if (answer.getAnswerValue() == null) {
                throw invalid("Answer for item " + itemId + " must have an answerValue");
            }
            ScoreItem definition = itemMap.get(itemId);
            if (definition == null) {
                throw invalid("Item " + itemId + " is not on the score sheet");
            }
            if (!seen.add(itemId)) {
                throw invalid("Item " + itemId + " is answered more than once");
            }
            requireValidValue(answer, definition);
        }
    }

    /**
     * Calculate scores for raw answers against the score sheet definition.
     * 作答先經過 {@link #requireValidAnswers}，不合法時丟 VALIDATION_ERROR。
     *
     * @param rawAnswers  frontend-submitted answers (itemId + answerValue only)
     * @param scoreSheet  the published ScoreSheetDefinition
     * @return ScoreResult with scored answers and total score
     */
    public ScoreResult calculate(List<AnswerItem> rawAnswers, ScoreSheetDefinition scoreSheet) {
        requireValidAnswers(rawAnswers, scoreSheet);
        Map<String, ScoreItem> itemMap = itemsById(scoreSheet);

        List<AnswerItem> scoredAnswers = new ArrayList<>();
        BigDecimal totalScore = BigDecimal.ZERO;

        for (AnswerItem raw : rawAnswers) {
            // RC-INV-08：題目分數四捨五入到兩位小數，總分是各題分數相加
            BigDecimal derivedScore = round(computeScore(raw, itemMap.get(raw.getItemId())));
            scoredAnswers.add(AnswerItem.ofScored(raw.getItemId(), raw.getAnswerValue(), derivedScore));
            totalScore = totalScore.add(derivedScore);
        }

        return new ScoreResult(scoredAnswers, round(totalScore));
    }

    /**
     * 四捨五入到兩位小數（HALF_UP）。不超過兩位小數的分數維持原來的寫法（20 不變成 20.00）：
     * 稽核明細以文字輸出分數，既有的範例（例如 {"totalScore":20}）規定了這個寫法。
     */
    private static BigDecimal round(BigDecimal score) {
        return score.scale() > SCORE_SCALE ? score.setScale(SCORE_SCALE, RoundingMode.HALF_UP) : score;
    }

    private static Map<String, ScoreItem> itemsById(ScoreSheetDefinition scoreSheet) {
        Map<String, ScoreItem> itemMap = new HashMap<>();
        for (ScoreSheetSection section : scoreSheet.getSections()) {
            for (ScoreItem item : section.getItems()) {
                itemMap.put(item.getItemId(), item);
            }
        }
        return itemMap;
    }

    private static void requireValidValue(AnswerItem answer, ScoreItem definition) {
        String itemId = answer.getItemId();
        String value = answer.getAnswerValue();
        switch (definition.getScoreType()) {
            case BOOLEAN:
                if (!"true".equals(value) && !"false".equals(value)) {
                    throw invalid("BOOLEAN answer for item " + itemId + " must be true or false");
                }
                return;

            case FIXED:
                return;

            case RANGE:
                BigDecimal number;
                try {
                    number = new BigDecimal(value);
                } catch (NumberFormatException e) {
                    throw invalid("RANGE answer for item " + itemId + " must be numeric");
                }
                if (number.signum() < 0 || number.compareTo(definition.getMaxScore()) > 0) {
                    throw invalid("RANGE answer for item " + itemId
                            + " must be between 0 and " + definition.getMaxScore().toPlainString());
                }
                if (number.stripTrailingZeros().scale() > SCORE_SCALE) {
                    throw invalid("RANGE answer for item " + itemId + " must have at most two decimal places");
                }
                return;

            default:
                throw new IllegalStateException("Unknown ScoreType: " + definition.getScoreType());
        }
    }

    private static BigDecimal computeScore(AnswerItem raw, ScoreItem definition) {
        BigDecimal weight = definition.getWeight();

        switch (definition.getScoreType()) {
            case BOOLEAN:
                return "true".equals(raw.getAnswerValue())
                        ? definition.getMaxScore().multiply(weight)
                        : BigDecimal.ZERO;

            case FIXED:
                return definition.getMaxScore().multiply(weight);

            case RANGE:
                return new BigDecimal(raw.getAnswerValue()).multiply(weight);

            default:
                throw new IllegalStateException("Unknown ScoreType: " + definition.getScoreType());
        }
    }

    private static DomainValidationException invalid(String message) {
        return new DomainValidationException("VALIDATION_ERROR", message);
    }
}
