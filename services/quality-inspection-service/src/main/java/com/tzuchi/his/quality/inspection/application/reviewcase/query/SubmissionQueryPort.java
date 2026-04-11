package com.tzuchi.his.quality.inspection.application.reviewcase.query;

import java.util.Optional;

/**
 * Read-model query port for ReviewSubmission details.
 * Implementations are in the infrastructure layer.
 */
public interface SubmissionQueryPort {
    Optional<SubmissionDetailView> findDetailById(String submissionId);
}
