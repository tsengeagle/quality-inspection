package com.tzuchi.his.quality.inspection.application.scoresheet;

import java.util.List;
import java.util.Optional;

/**
 * Read-model query port for ScoreSheetDefinition summaries.
 * Implementations are in the infrastructure layer.
 */
public interface ScoreSheetQueryPort {
    List<ScoreSheetSummary> findByHospitalId(String hospitalId);
    List<ScoreSheetSummary> findAll();
    Optional<ScoreSheetSummary> findSummaryById(String scoreSheetId);
}
