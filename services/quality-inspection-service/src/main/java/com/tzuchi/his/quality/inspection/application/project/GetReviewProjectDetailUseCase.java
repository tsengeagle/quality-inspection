package com.tzuchi.his.quality.inspection.application.project;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.UnauthorizedException;
import com.tzuchi.his.quality.inspection.application.project.query.ReviewProjectQueryPort;
import com.tzuchi.his.quality.inspection.application.project.query.ReviewProjectSummary;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetReviewProjectDetailUseCase {

    private final ReviewProjectQueryPort queryPort;

    public GetReviewProjectDetailUseCase(ReviewProjectQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    public ReviewProjectSummary execute(ActorContext actor, String projectId) {
        if (!actor.hasRole(DomainRole.HOSPITAL_MANAGER) && !actor.hasRole(DomainRole.SYSTEM_ADMIN)) {
            throw new UnauthorizedException("Insufficient role to view project detail");
        }
        ReviewProjectSummary summary = queryPort.findSummaryById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("PROJECT_NOT_FOUND",
                        "Project not found: " + projectId));
        if (actor.hasRole(DomainRole.HOSPITAL_MANAGER) && !actor.hasRole(DomainRole.SYSTEM_ADMIN)) {
            actor.requireHospitalScope(summary.getHospitalId());
        }
        return summary;
    }
}