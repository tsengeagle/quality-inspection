package com.tzuchi.his.quality.inspection.application.project;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
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
        // D-043: SYSTEM_ADMIN, HOSPITAL_MANAGER and AUDITOR may read. REVIEWER / REREVIEWER may not (D-051).
        actor.requireAnyRole(DomainRole.SYSTEM_ADMIN, DomainRole.HOSPITAL_MANAGER, DomainRole.AUDITOR);
        ReviewProjectSummary summary = queryPort.findSummaryById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("PROJECT_NOT_FOUND",
                        "Project not found: " + projectId));
        // D-042, D-047: only SYSTEM_ADMIN reads across hospitals
        actor.requireReadScope(summary.getHospitalId());
        return summary;
    }
}