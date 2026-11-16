package com.tzuchi.his.quality.inspection.domain.scoresheet;

import java.util.Objects;
import java.util.Optional;

/**
 * 評分表版本 id（D-050 第 1 點）：「評分表 id」加上「:v」與版本號，例如 {@code ss-123:v1}。
 *
 * <p>由評分表 id 與版本號組成，不新增資料欄位（D-045：正式資料庫與 migration 工具未定前不得新增欄位；
 * 負責人 2026-11-16 裁決採組合 id）。對外是不透明的 id，規格與範例不得假設它的格式；只有 domain 會拆解它。
 * 一份評分表日後有多個版本時，沿用同一個格式。
 */
public final class ScoreSheetVersionId {

    private static final String SEPARATOR = ":v";

    private final String scoreSheetId;
    private final int versionNo;

    private ScoreSheetVersionId(String scoreSheetId, int versionNo) {
        this.scoreSheetId = scoreSheetId;
        this.versionNo = versionNo;
    }

    public static String of(String scoreSheetId, int versionNo) {
        Objects.requireNonNull(scoreSheetId, "scoreSheetId must not be null");
        return scoreSheetId + SEPARATOR + versionNo;
    }

    /** 不是合法的版本 id 時回傳 empty（不丟例外）：呼叫端依情境決定回哪個錯誤代碼。 */
    public static Optional<ScoreSheetVersionId> parse(String value) {
        if (value == null) {
            return Optional.empty();
        }
        int at = value.lastIndexOf(SEPARATOR);
        if (at <= 0) {
            return Optional.empty();
        }
        String number = value.substring(at + SEPARATOR.length());
        // 只接受 of() 會產生的寫法：十進位數字、沒有前導零（同一個版本只有一個 id）
        if (number.isEmpty() || number.length() > 9 || number.charAt(0) == '0'
                || !number.chars().allMatch(c -> c >= '0' && c <= '9')) {
            return Optional.empty();
        }
        int versionNo = Integer.parseInt(number);
        if (versionNo < 1) {
            return Optional.empty();
        }
        return Optional.of(new ScoreSheetVersionId(value.substring(0, at), versionNo));
    }

    public String getScoreSheetId() { return scoreSheetId; }
    public int getVersionNo() { return versionNo; }
}
