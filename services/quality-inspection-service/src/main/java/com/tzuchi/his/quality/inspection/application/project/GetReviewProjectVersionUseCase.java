package com.tzuchi.his.quality.inspection.application.project;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.RoleDeniedException;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.project.ProjectVersion;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetReviewProjectVersionUseCase {

    private final ReviewProjectRepository repository;

    public GetReviewProjectVersionUseCase(ReviewProjectRepository repository) {
        this.repository = repository;
    }

    public ProjectVersion execute(ActorContext actor, String projectId) {
        if (!actor.hasRole(DomainRole.HOSPITAL_MANAGER) && !actor.hasRole(DomainRole.SYSTEM_ADMIN)) {
            throw new RoleDeniedException("Insufficient role to view project version");
        }
        ReviewProject project = repository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("PROJECT_NOT_FOUND",
                        "Project not found: " + projectId));
        if (actor.hasRole(DomainRole.HOSPITAL_MANAGER) && !actor.hasRole(DomainRole.SYSTEM_ADMIN)) {
            actor.requireHospitalScope(project.getHospitalId());
        }
        return project.getCurrentVersion();
    }
}