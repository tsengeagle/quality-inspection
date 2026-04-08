package com.tzuchi.his.quality.inspection.infrastructure.adapter;

import com.tzuchi.his.quality.inspection.application.common.HospitalIdResolver;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import org.springframework.stereotype.Component;

@Component
public class HospitalIdResolverImpl implements HospitalIdResolver {

    private final ReviewProjectRepository projectRepository;

    public HospitalIdResolverImpl(ReviewProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Override
    public String resolveByProjectId(String projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("PROJECT_NOT_FOUND",
                        "ReviewProject not found: " + projectId))
                .getHospitalId();
    }
}
