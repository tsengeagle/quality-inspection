package com.tzuchi.his.quality.inspection.application.reviewcase;

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
    void calculate_range_exceedingMax_shouldCapAtMax() {
        ScoreSheetDefinition ss = buildScoreSheet(
                new ScoreItem("Q001", "Q001", "Range Item", ScoreType.RANGE,
                        new BigDecimal("100"), new BigDecimal("1.0")));

        List<AnswerItem> raw = Collections.singletonList(
                AnswerItem.ofRaw("Q001", "150"));

        ScoreResult result = service.calculate(raw, ss);

        assertEquals(0, new BigDecimal("100.0").compareTo(result.getScoredAnswers().get(0).getDerivedScore()));
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
    void calculate_unknownItemId_shouldBeIgnored() {
        ScoreSheetDefinition ss = buildScoreSheet(
                new ScoreItem("Q001", "Q001", "Check", ScoreType.BOOLEAN,
                        new BigDecimal("10"), new BigDecimal("1.0")));

        List<AnswerItem> raw = Arrays.asList(
                AnswerItem.ofRaw("Q001", "true"),
                AnswerItem.ofRaw("Q999", "unknown"));

        ScoreResult result = service.calculate(raw, ss);

        assertEquals(1, result.getScoredAnswers().size());
        assertEquals(0, new BigDecimal("10.0").compareTo(result.getTotalScore()));
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
    void calculate_range_negativeValue_shouldThrow() {
        ScoreSheetDefinition ss = buildScoreSheet(
                new ScoreItem("Q001", "Q001", "Range", ScoreType.RANGE,
                        new BigDecimal("100"), new BigDecimal("1.0")));

        List<AnswerItem> raw = Collections.singletonList(
                AnswerItem.ofRaw("Q001", "-5"));

        assertThrows(IllegalArgumentException.class,
                () -> service.calculate(raw, ss));
    }

    @Test
    void calculate_range_nonNumeric_shouldThrow() {
        ScoreSheetDefinition ss = buildScoreSheet(
                new ScoreItem("Q001", "Q001", "Range", ScoreType.RANGE,
                        new BigDecimal("100"), new BigDecimal("1.0")));

        List<AnswerItem> raw = Collections.singletonList(
                AnswerItem.ofRaw("Q001", "abc"));

        assertThrows(IllegalArgumentException.class,
                () -> service.calculate(raw, ss));
    }
}
