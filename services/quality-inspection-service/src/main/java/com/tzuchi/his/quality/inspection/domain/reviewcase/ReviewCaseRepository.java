package com.tzuchi.his.quality.inspection.domain.reviewcase;

import java.util.List;
import java.util.Optional;

public interface ReviewCaseRepository {

    void save(ReviewCase reviewCase);

    void saveAll(List<ReviewCase> cases);

    Optional<ReviewCase> findById(String caseId);
}
