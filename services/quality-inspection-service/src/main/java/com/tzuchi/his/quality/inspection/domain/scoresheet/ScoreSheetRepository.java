package com.tzuchi.his.quality.inspection.domain.scoresheet;

import java.util.Optional;

public interface ScoreSheetRepository {

    void save(ScoreSheetDefinition scoreSheet);

    Optional<ScoreSheetDefinition> findById(String scoreSheetId);

    /**
     * 依版本 id（{@link ScoreSheetVersionId}）找評分表，而且它目前的版本號要相符。
     * 不是合法的版本 id、評分表不存在、版本號不符，都回傳 empty。不檢查是否已發布：由呼叫端依規則決定。
     */
    default Optional<ScoreSheetDefinition> findByVersionId(String scoreSheetVersionId) {
        return ScoreSheetVersionId.parse(scoreSheetVersionId)
                .flatMap(v -> findById(v.getScoreSheetId()).filter(ss -> ss.getVersion() == v.getVersionNo()));
    }
}
