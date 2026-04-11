package com.tzuchi.his.quality.inspection.application.reviewcase.query;

import com.tzuchi.his.quality.inspection.application.common.Page;

import java.util.Optional;

/**
 * Read-model query port for ReviewCase summaries and details.
 * Implementations are in the infrastructure layer.
 */
public interface ReviewCaseQueryPort {
    Page<ReviewCaseSummary> findCases(ReviewCaseQueryCriteria criteria);
    Optional<ReviewCaseDetailView> findDetailById(String caseId);
}
