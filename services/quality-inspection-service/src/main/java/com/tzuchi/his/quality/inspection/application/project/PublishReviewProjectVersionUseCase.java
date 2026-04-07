package com.tzuchi.his.quality.inspection.application.project;

import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PublishReviewProjectVersionUseCase {

    private final ReviewProjectRepository repository;

    public PublishReviewProjectVersionUseCase(ReviewProjectRepository repository) {
        this.repository = repository;
    }

    public void execute(String projectId) {
        ReviewProject project = repository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        project.publishNewVersion();
        repository.save(project);
    }
}
