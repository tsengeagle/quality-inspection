package com.tzuchi.his.quality.inspection.application.project;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
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

    public ReviewProjectVersionView execute(ActorContext actor, String projectId) {
        // D-062 第 3 點：讀取角色與專案詳情相同（D-043）；REVIEWER／REREVIEWER 不開放（D-051）
        actor.requireAnyRole(DomainRole.SYSTEM_ADMIN, DomainRole.HOSPITAL_MANAGER, DomainRole.AUDITOR);
        ReviewProject project = repository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("PROJECT_NOT_FOUND",
                        "Project not found: " + projectId));
        // D-042、D-047：只有 SYSTEM_ADMIN 可以跨院區
        actor.requireReadScope(project.getHospitalId());
        ProjectVersion version = project.getCurrentVersion();
        return new ReviewProjectVersionView(version.getVersionNumber(), version.isPublished());
    }
}