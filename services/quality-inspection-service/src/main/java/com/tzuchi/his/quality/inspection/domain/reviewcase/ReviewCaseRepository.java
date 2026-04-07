package com.tzuchi.his.quality.inspection.domain.reviewcase;

import java.util.List;

public interface ReviewCaseRepository {

    void save(ReviewCase reviewCase);

    void saveAll(List<ReviewCase> cases);
}
