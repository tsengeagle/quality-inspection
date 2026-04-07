package com.tzuchi.his.quality.inspection.application.project;

import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class CreateReviewProjectUseCase {

    private final ReviewProjectRepository repository;

    public CreateReviewProjectUseCase(ReviewProjectRepository repository) {
        this.repository = repository;
    }

    public String execute(String hospitalId, String name, Set<MedicalRecordType> medicalRecordTypes) {
        String projectId = UUID.randomUUID().toString();
        ReviewProject project = ReviewProject.create(projectId, hospitalId, name, medicalRecordTypes);
        repository.save(project);
        return projectId;
    }
}
