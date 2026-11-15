package com.tzuchi.his.quality.inspection.domain.reviewcase.submission;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 草稿內容是否有變更（D-038、D-044）。
 * D-044：答案以題目 id 對應、不看順序；數值以數值比較、不看小數位數；文字的 null 與空字串視為相同；附件不納入。
 */
class DraftContentTest {

    private static AnswerItem raw(String id, String value) { return AnswerItem.ofRaw(id, value); }
    private static AnswerItem scored(String id, String value, String score) {
        return AnswerItem.ofScored(id, value, score == null ? null : new BigDecimal(score));
    }
    private static DraftContent draft(List<AnswerItem> answers, String total, String comment) {
        return new DraftContent(answers, total == null ? null : new BigDecimal(total), comment);
    }

    @Test
    void identicalContentIsUnchanged() {
        assertTrue(draft(List.of(raw("Q1", "true")), "10", "c").sameAs(draft(List.of(raw("Q1", "true")), "10", "c")));
    }

    @Test
    void answerOrderDoesNotMatter() {
        DraftContent a = draft(List.of(raw("Q1", "true"), raw("Q2", "3")), null, "c");
        DraftContent b = draft(List.of(raw("Q2", "3"), raw("Q1", "true")), null, "c");
        assertTrue(a.sameAs(b));
    }

    @Test
    void numbersCompareByValueNotScale() {
        assertTrue(draft(List.of(raw("Q1", "20")), "3", "c").sameAs(draft(List.of(raw("Q1", "20.0")), "3.00", "c")));
        assertTrue(draft(List.of(scored("Q1", "1", "5")), null, "c").sameAs(draft(List.of(scored("Q1", "1", "5.0")), null, "c")));
    }

    @Test
    void nullAndEmptyTextAreTheSame() {
        assertTrue(draft(List.of(), null, null).sameAs(draft(List.of(), null, "")));
        // 答案值不會是 null：AnswerItem 不允許（ofRaw 會拒絕），所以只有評語會遇到 null
        assertTrue(draft(null, null, "c").sameAs(draft(Collections.emptyList(), null, "c")));
    }

    @Test
    void changedValueIsAChange() {
        assertFalse(draft(List.of(raw("Q1", "true")), null, "c").sameAs(draft(List.of(raw("Q1", "false")), null, "c")));
        assertFalse(draft(List.of(raw("Q1", "20")), null, "c").sameAs(draft(List.of(raw("Q1", "21")), null, "c")));
    }

    @Test
    void changedCommentIsAChange() {
        assertFalse(draft(List.of(), null, "a").sameAs(draft(List.of(), null, "b")));
        assertFalse(draft(List.of(), null, "a").sameAs(draft(List.of(), null, " a")), "文字不做修剪");
    }

    @Test
    void addedOrRemovedAnswerIsAChange() {
        assertFalse(draft(List.of(raw("Q1", "true")), null, "c")
                .sameAs(draft(List.of(raw("Q1", "true"), raw("Q2", "1")), null, "c")));
        assertFalse(draft(List.of(raw("Q1", "true"), raw("Q2", "1")), null, "c")
                .sameAs(draft(List.of(raw("Q1", "true")), null, "c")));
    }

    @Test
    void changedDerivedScoreOrTotalIsAChange() {
        // 複審草稿的分數由請求帶入，屬於業務內容
        assertFalse(draft(List.of(scored("Q1", "true", "10")), "10", "c")
                .sameAs(draft(List.of(scored("Q1", "true", "5")), "10", "c")));
        assertFalse(draft(List.of(), "10", "c").sameAs(draft(List.of(), "3", "c")));
    }

    @Test
    void duplicateItemIdsAreComparedAsAMultiset() {
        DraftContent a = draft(List.of(raw("Q1", "1"), raw("Q1", "2")), null, "c");
        assertTrue(a.sameAs(draft(List.of(raw("Q1", "2"), raw("Q1", "1")), null, "c")));
        assertFalse(a.sameAs(draft(List.of(raw("Q1", "1"), raw("Q1", "1")), null, "c")));
    }

    @Test
    void hugeExponentsAreComparedWithoutExpandingThem() {
        // 不能把 1e999999999 這類數字展開成十億個字元（Codex 審查：記憶體耗盡）
        DraftContent a = draft(List.of(raw("Q1", "1e999999999"), raw("Q1", "2")), null, "c");
        DraftContent b = draft(List.of(raw("Q1", "2"), raw("Q1", "10e999999998")), null, "c");
        assertTimeoutPreemptively(java.time.Duration.ofSeconds(2), () -> assertTrue(a.sameAs(b)));
        assertTimeoutPreemptively(java.time.Duration.ofSeconds(2),
                () -> assertFalse(a.sameAs(draft(List.of(raw("Q1", "2"), raw("Q1", "1e999999998")), null, "c"))));
    }

    @Test
    void duplicateItemsWithDifferentScoresAreComparedAsAMultiset() {
        // 同一題目、相同答案值、分數不同的兩筆，交換順序仍是相同內容（Copilot 審查）
        DraftContent a = draft(List.of(scored("Q1", "true", "5"), scored("Q1", "true", "10")), "15", "c");
        DraftContent b = draft(List.of(scored("Q1", "true", "10"), scored("Q1", "true", "5.0")), "15", "c");
        assertTrue(a.sameAs(b));
        assertFalse(a.sameAs(draft(List.of(scored("Q1", "true", "10"), scored("Q1", "true", "10")), "15", "c")));
        // 沒有分數的答案（ofRaw）與有分數的混在一起時，排序同樣要穩定
        assertTrue(draft(List.of(raw("Q1", "true"), scored("Q1", "true", "1")), null, "c")
                .sameAs(draft(List.of(scored("Q1", "true", "1"), raw("Q1", "true")), null, "c")));
    }

    @Test
    void extremeScalesDoNotOverflow() {
        // BigDecimal("100e2147483647") 建得起來，但 stripTrailingZeros() 會讓 scale 溢位（Copilot 審查）
        DraftContent a = draft(List.of(raw("Q1", "100e2147483647"), raw("Q1", "1")), null, "c");
        DraftContent b = draft(List.of(raw("Q1", "1"), raw("Q1", "100e2147483647")), null, "c");
        assertTrue(a.sameAs(b));
        assertFalse(a.sameAs(draft(List.of(raw("Q1", "1"), raw("Q1", "1e2147483647")), null, "c")));
    }
}

