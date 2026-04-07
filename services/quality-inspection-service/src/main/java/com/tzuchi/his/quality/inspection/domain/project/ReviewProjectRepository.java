package com.tzuchi.his.quality.inspection.domain.project;

import java.util.Optional;

public interface ReviewProjectRepository {

    void save(ReviewProject project);

    Optional<ReviewProject> findById(String projectId);
}
