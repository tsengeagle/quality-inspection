package com.tzuchi.his.quality.inspection.testfixture;

import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;

import java.util.*;

public class InMemoryReviewCaseRepository implements ReviewCaseRepository {
    public final Map<String, ReviewCase> store = new HashMap<>();

    @Override
    public void save(ReviewCase reviewCase) {
        store.put(reviewCase.getCaseId(), reviewCase);
    }

    @Override
    public void saveAll(List<ReviewCase> cases) {
        cases.forEach(rc -> store.put(rc.getCaseId(), rc));
    }

    @Override
    public Optional<ReviewCase> findById(String caseId) {
        return Optional.ofNullable(store.get(caseId));
    }
}
