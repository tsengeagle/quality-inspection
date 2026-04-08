package com.tzuchi.his.quality.inspection.domain.reviewcase.submission;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ReviewSubmission.
 * B3a: interface only — no infrastructure implementation.
 * JPA implementation will be added in B3b.
 */
public interface ReviewSubmissionRepository {

    void save(ReviewSubmission submission);

    Optional<ReviewSubmission> findById(String submissionId);

    List<ReviewSubmission> findByCaseIdAndStage(String caseId, ReviewStage stage);

    Optional<ReviewSubmission> findLatestByCaseIdAndStage(String caseId, ReviewStage stage);
}
