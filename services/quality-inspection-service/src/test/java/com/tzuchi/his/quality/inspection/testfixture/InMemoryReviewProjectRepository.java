package com.tzuchi.his.quality.inspection.testfixture;

import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryReviewProjectRepository implements ReviewProjectRepository {
    public final Map<String, ReviewProject> store = new HashMap<>();

    @Override
    public void save(ReviewProject project) {
        store.put(project.getProjectId(), project);
    }

    @Override
    public Optional<ReviewProject> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }
}
