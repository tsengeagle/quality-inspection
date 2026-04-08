package com.tzuchi.his.quality.inspection.application.project.query;

import java.util.List;
import java.util.Optional;

/**
 * Read-only query port for ReviewProject projections.
 * Implementations are in the infrastructure layer.
 */
public interface ReviewProjectQueryPort {

    List<ReviewProjectSummary> findByHospitalId(String hospitalId);

    List<ReviewProjectSummary> findAll();

    Optional<ReviewProjectSummary> findSummaryById(String projectId);
}