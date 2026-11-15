package com.tzuchi.his.quality.inspection.domain.reviewcase.submission;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 草稿的業務內容，用來判斷一次草稿儲存有沒有實際變更（D-038、D-044）。
 * <p>
 * 比較規則（D-044）：
 * <ol>
 *   <li>答案以題目 id 對應後比較，不看順序（同一題目 id 出現多次時，視為多重集合比較）。</li>
 *   <li>數值以數值比較，不看小數位數：答案值兩邊都是數字時依數值比較；分數一律依數值比較。</li>
 *   <li>文字欄位的 null 與空字串視為相同；不做修剪。</li>
 *   <li>附件不納入比較（附件尚未傳入草稿 use case）。</li>
 * </ol>
 */
public final class DraftContent {

    private final List<Answer> answers;
    private final BigDecimal totalScore;
    private final String comment;

    public DraftContent(List<AnswerItem> answers, BigDecimal totalScore, String comment) {
        List<Answer> list = new ArrayList<>();
        if (answers != null) {
            for (AnswerItem item : answers) {
                if (item != null) {
                    list.add(new Answer(item.getItemId(), item.getAnswerValue(), item.getDerivedScore()));
                }
            }
        }
        list.sort(Comparator.comparing((Answer a) -> text(a.itemId)).thenComparing(a -> a.sortKey));
        this.answers = Collections.unmodifiableList(list);
        this.totalScore = totalScore;
        this.comment = comment;
    }

    /** 兩份草稿的業務內容是否相同。 */
    public boolean sameAs(DraftContent other) {
        if (other == null || answers.size() != other.answers.size()) {
            return false;
        }
        if (!sameNumber(totalScore, other.totalScore) || !text(comment).equals(text(other.comment))) {
            return false;
        }
        for (int i = 0; i < answers.size(); i++) {
            if (!answers.get(i).sameAs(other.answers.get(i))) {
                return false;
            }
        }
        return true;
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }

    private static BigDecimal number(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean sameNumber(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return a == null && b == null;
        }
        return a.compareTo(b) == 0;
    }

    private static boolean sameValue(String a, String b) {
        BigDecimal x = number(a);
        BigDecimal y = number(b);
        if (x != null && y != null) {
            return x.compareTo(y) == 0;
        }
        return text(a).equals(text(b));
    }

    private static final class Answer {
        private final String itemId;
        private final String value;
        private final BigDecimal derivedScore;
        /** 同一題目 id 內的排序鍵：數字依數值正規化，讓 "20" 與 "20.0" 排在同一位置。 */
        private final String sortKey;

        private Answer(String itemId, String value, BigDecimal derivedScore) {
            this.itemId = itemId;
            this.value = value;
            this.derivedScore = derivedScore;
            BigDecimal n = number(value);
            this.sortKey = n != null ? "n:" + n.stripTrailingZeros().toPlainString() : "t:" + text(value);
        }

        private boolean sameAs(Answer other) {
            return Objects.equals(text(itemId), text(other.itemId))
                    && sameValue(value, other.value)
                    && sameNumber(derivedScore, other.derivedScore);
        }
    }
}
