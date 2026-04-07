package com.tzuchi.his.quality.inspection.domain.scoresheet;

import java.util.Optional;

public interface ScoreSheetRepository {

    void save(ScoreSheetDefinition scoreSheet);

    Optional<ScoreSheetDefinition> findById(String scoreSheetId);
}
