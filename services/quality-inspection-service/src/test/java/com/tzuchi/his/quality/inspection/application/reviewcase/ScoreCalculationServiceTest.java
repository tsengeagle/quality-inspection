package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.domain.common.DomainValidationException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreItem;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetDefinition;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetSection;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScoreCalculationServiceTest {

    private ScoreCalculationService service;

    @BeforeEach
    void setUp() {
        service = new ScoreCalculationService();
    }

    private ScoreSheetDefinition buildScoreSheet(ScoreItem... items) {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft("ss-1", "p1");
        ScoreSheetSection section = new ScoreSheetSection("sec-1", "Section 1");
        for (ScoreItem item : items) {
            section.addItem(item);
        }
        ss.addSection(section);
        ss.publishVersion();
        return ss;
    }

    // --- BOOLEAN ---

    @Test
    void calculate_booleanTrue_shouldReturnMaxScoreTimesWeight() {
        ScoreSheetDefinition ss = buildScoreSheet(
                new ScoreItem("Q001", "Q001", "Check 1", ScoreType.BOOLEAN,
                        new BigDecimal("10"), new BigDecimal("1.0")));

        List<AnswerItem> raw = Collections.singletonList(
                AnswerItem.ofRaw("Q001", "true"));

        ScoreResult result = service.calculate(raw, ss);

        assertEquals(1, result.getScoredAnswers().size());
        assertEquals(0, new BigDecimal("10.0").compareTo(result.getScoredAnswers().get(0).getDerivedScore()));
        assertEquals(0, new BigDecimal("10.0").compareTo(result.getTotalScore()));
    }

    @Test
    void calculate_booleanFalse_shouldReturnZero() {
        ScoreSheetDefinition ss = buildScoreSheet(
                new ScoreItem("Q001", "Q001", "Check 1", ScoreType.BOOLEAN,
                        new BigDecimal("10"), new BigDecimal("1.0")));

        List<AnswerItem> raw = Collections.singletonList(
                AnswerItem.ofRaw("Q001", "false"));

        ScoreResult result = service.calculate(raw, ss);

        assertEquals(0, BigDecimal.ZERO.compareTo(result.getScoredAnswers().get(0).getDerivedScore()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getTotalScore()));
    }

    // --- FIXED ---

    @Test
    void calculate_fixed_shouldReturnMaxScoreTimesWeight() {
        ScoreSheetDefinition ss = buildScoreSheet(
                new ScoreItem("Q001", "Q001", "Fixed Item", ScoreType.FIXED,
                        new BigDecimal("5"), new BigDecimal("2.0")));

        List<AnswerItem> raw = Collections.singletonList(
                AnswerItem.ofRaw("Q001", "anything"));

        ScoreResult result = service.calculate(raw, ss);

        assertEquals(0, new BigDecimal("10.0").compareTo(result.getScoredAnswers().get(0).getDerivedScore()));
        assertEquals(0, new BigDecimal("10.0").compareTo(result.getTotalScore()));
    }

    // --- RANGE ---

    @Test
    void calculate_range_withinMax_shouldReturnValueTimesWeight() {
        ScoreSheetDefinition ss = buildScoreSheet(
                new ScoreItem("Q001", "Q001", "Range Item", ScoreType.RANGE,
                        new BigDecimal("100"), new BigDecimal("1.0")));

        List<AnswerItem> raw = Collections.singletonList(
                AnswerItem.ofRaw("Q001", "85"));

        ScoreResult result = service.calculate(raw, ss);

        assertEquals(0, new BigDecimal("85.0").compareTo(result.getScoredAnswers().get(0).getDerivedScore()));
        assertEquals(0, new BigDecimal("85.0").compareTo(result.getTotalScore()));
    }

    @Test
    void calculate_range_exceedingMax_isRejected() {
        ScoreSheetDefinition ss = buildScoreSheet(
                new ScoreItem("Q001", "Q001", "Range Item", ScoreType.RANGE,
                        new BigDecimal("100"), new BigDecimal("1.0")));

        assertValidationError(ss, AnswerItem.ofRaw("Q001", "150"));
        assertValidationError(ss, AnswerItem.ofRaw("Q001", "100.01"));
    }

    @Test
    void calculate_range_equalToMaxAndZero_areAccepted() {
        ScoreSheetDefinition ss = buildScoreSheet(
                new ScoreItem("Q001", "Q001", "Range Item", ScoreType.RANGE,
                        new BigDecimal("100"), new BigDecimal("1.0")));

        assertEquals(0, new BigDecimal("100").compareTo(
                service.calculate(Collections.singletonList(AnswerItem.ofRaw("Q001", "100")), ss).getTotalScore()));
        assertEquals(0, BigDecimal.ZERO.compareTo(
                service.calculate(Collections.singletonList(AnswerItem.ofRaw("Q001", "0")), ss).getTotalScore()));
    }

    @Test
    void calculate_range_moreThanTwoDecimals_isRejected() {
        ScoreSheetDefinition ss = buildScoreSheet(
                new ScoreItem("Q001", "Q001", "Range Item", ScoreType.RANGE,
                        new BigDecimal("100"), new BigDecimal("1.0")));

        assertValidationError(ss, AnswerItem.ofRaw("Q001", "5.555"));
        assertEquals(new BigDecimal("5.55"),
                service.calculate(Collections.singletonList(AnswerItem.ofRaw("Q001", "5.55")), ss).getTotalScore());
    }

    // --- D-085 第 4 點：權重不是 1 時，題目分數與總分四捨五入到兩位小數（HALF_UP） ---

    @Test
    void calculate_weightNotOne_roundsItemScoreHalfUp() {
        ScoreSheetDefinition ss = buildScoreSheet(
                new ScoreItem("Q001", "Q001", "Range", ScoreType.RANGE,
                        new BigDecimal("100"), new BigDecimal("0.33")),
                new ScoreItem("Q002", "Q002", "Range", ScoreType.RANGE,
                        new BigDecimal("100"), new BigDecimal("0.5")));

        // 5.55 × 0.33 = 1.8315 → 1.83；0.25 × 0.5 = 0.125 → 0.13（HALF_UP）
        ScoreResult result = service.calculate(Arrays.asList(
                AnswerItem.ofRaw("Q001", "5.55"),
                AnswerItem.ofRaw("Q002", "0.25")), ss);

        assertEquals(new BigDecimal("1.83"), result.getScoredAnswers().get(0).getDerivedScore());
        assertEquals(new BigDecimal("0.13"), result.getScoredAnswers().get(1).getDerivedScore());
        // 總分是四捨五入後的題目分數相加
        assertEquals(new BigDecimal("1.96"), result.getTotalScore());
    }

    @Test
    void calculate_booleanAndFixedWithWeight_roundToTwoDecimals() {
        ScoreSheetDefinition ss = buildScoreSheet(
                new ScoreItem("Q001", "Q001", "Boolean", ScoreType.BOOLEAN,
                        new BigDecimal("10"), new BigDecimal("0.333")),
                new ScoreItem("Q002", "Q002", "Fixed", ScoreType.FIXED,
                        new BigDecimal("5"), new BigDecimal("0.333")));

        ScoreResult result = service.calculate(Arrays.asList(
                AnswerItem.ofRaw("Q001", "true"),
                AnswerItem.ofRaw("Q002", "whatever")), ss);

        assertEquals(new BigDecimal("3.33"), result.getScoredAnswers().get(0).getDerivedScore());
        assertEquals(new BigDecimal("1.67"), result.getScoredAnswers().get(1).getDerivedScore());
        assertEquals(new BigDecimal("5.00"), result.getTotalScore());
    }

    /** 不超過兩位小數的分數維持原來的寫法：稽核明細以文字輸出分數（例如 {"totalScore":20}）。 */
    @Test
    void calculate_scoreWithinTwoDecimals_keepsItsRepresentation() {
        ScoreSheetDefinition ss = buildScoreSheet(
                new ScoreItem("Q001", "Q001", "Fixed", ScoreType.FIXED,
                        new BigDecimal("20"), new BigDecimal("1")));

        ScoreResult result = service.calculate(Collections.singletonList(AnswerItem.ofRaw("Q001", "x")), ss);

        assertEquals("20", result.getTotalScore().toString());
    }

    // --- D-085 第 7 點：BOOLEAN 的作答只能是 true 或 false ---

    @Test
    void calculate_booleanOtherThanTrueOrFalse_isRejected() {
        ScoreSheetDefinition ss = buildScoreSheet(
                new ScoreItem("Q001", "Q001", "Check 1", ScoreType.BOOLEAN,
                        new BigDecimal("10"), new BigDecimal("1.0")));

        assertValidationError(ss, AnswerItem.ofRaw("Q001", "yes"));
        assertValidationError(ss, AnswerItem.ofRaw("Q001", "TRUE"));
        assertValidationError(ss, AnswerItem.ofRaw("Q001", "1"));
        assertValidationError(ss, AnswerItem.ofRaw("Q001", ""));
    }

    @Test
    void calculate_duplicateItem_isRejected() {
        ScoreSheetDefinition ss = buildScoreSheet(
                new ScoreItem("Q001", "Q001", "Check 1", ScoreType.BOOLEAN,
                        new BigDecimal("10"), new BigDecimal("1.0")));

        assertValidationError(ss, AnswerItem.ofRaw("Q001", "true"), AnswerItem.ofRaw("Q001", "false"));
    }

    @Test
    void calculate_missingItemIdOrAnswerValue_isRejected() {
        ScoreSheetDefinition ss = buildScoreSheet(
                new ScoreItem("Q001", "Q001", "Fixed", ScoreType.FIXED,
                        new BigDecimal("10"), new BigDecimal("1.0")));

        assertValidationError(ss, AnswerItem.reconstitute(null, "x", null));
        assertValidationError(ss, AnswerItem.reconstitute("Q001", null, null));
        assertValidationError(ss, (AnswerItem) null);
    }

    private void assertValidationError(ScoreSheetDefinition ss, AnswerItem... answers) {
        DomainValidationException e = assertThrows(DomainValidationException.class,
                () -> service.calculate(Arrays.asList(answers), ss));
        assertEquals("VALIDATION_ERROR", e.getErrorCode());
        DomainValidationException e2 = assertThrows(DomainValidationException.class,
                () -> service.requireValidAnswers(Arrays.asList(answers), ss));
        assertEquals("VALIDATION_ERROR", e2.getErrorCode());
    }

    @Test
    void calculate_range_withWeight_shouldMultiply() {
        ScoreSheetDefinition ss = buildScoreSheet(
                new ScoreItem("Q001", "Q001", "Range Item", ScoreType.RANGE,
                        new BigDecimal("100"), new BigDecimal("0.5")));

        List<AnswerItem> raw = Collections.singletonList(
                AnswerItem.ofRaw("Q001", "80"));

        ScoreResult result = service.calculate(raw, ss);

        assertEquals(0, new BigDecimal("40.0").compareTo(result.getScoredAnswers().get(0).getDerivedScore()));
    }

    // --- Multiple items ---

    @Test
    void calculate_multipleItems_shouldSumTotalScore() {
        ScoreSheetDefinition ss = buildScoreSheet(
                new ScoreItem("Q001", "Q001", "Boolean", ScoreType.BOOLEAN,
                        new BigDecimal("10"), new BigDecimal("1.0")),
                new ScoreItem("Q002", "Q002", "Range", ScoreType.RANGE,
                        new BigDecimal("100"), new BigDecimal("1.0")));

        List<AnswerItem> raw = Arrays.asList(
                AnswerItem.ofRaw("Q001", "true"),
                AnswerItem.ofRaw("Q002", "75"));

        ScoreResult result = service.calculate(raw, ss);

        assertEquals(2, result.getScoredAnswers().size());
        assertEquals(0, new BigDecimal("85.0").compareTo(result.getTotalScore()));
    }

    // --- Edge cases ---

    @Test
    void calculate_unknownItemId_isRejected() {
        ScoreSheetDefinition ss = buildScoreSheet(
                new ScoreItem("Q001", "Q001", "Check", ScoreType.BOOLEAN,
                        new BigDecimal("10"), new BigDecimal("1.0")));

        assertValidationError(ss, AnswerItem.ofRaw("Q001", "true"), AnswerItem.ofRaw("Q999", "unknown"));
    }

    @Test
    void calculate_emptyAnswers_shouldReturnZero() {
        ScoreSheetDefinition ss = buildScoreSheet(
                new ScoreItem("Q001", "Q001", "Check", ScoreType.BOOLEAN,
                        new BigDecimal("10"), new BigDecimal("1.0")));

        ScoreResult result = service.calculate(Collections.emptyList(), ss);

        assertTrue(result.getScoredAnswers().isEmpty());
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getTotalScore()));
    }

    @Test
    void calculate_range_negativeValue_isRejected() {
        ScoreSheetDefinition ss = buildScoreSheet(
                new ScoreItem("Q001", "Q001", "Range", ScoreType.RANGE,
                        new BigDecimal("100"), new BigDecimal("1.0")));

        assertValidationError(ss, AnswerItem.ofRaw("Q001", "-5"));
    }

    @Test
    void calculate_range_nonNumeric_isRejected() {
        ScoreSheetDefinition ss = buildScoreSheet(
                new ScoreItem("Q001", "Q001", "Range", ScoreType.RANGE,
                        new BigDecimal("100"), new BigDecimal("1.0")));

        assertValidationError(ss, AnswerItem.ofRaw("Q001", "abc"));
    }
}
