package com.tzuchi.his.quality.inspection.application.project;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.RoleDeniedException;
import com.tzuchi.his.quality.inspection.application.project.query.ReviewProjectQueryPort;
import com.tzuchi.his.quality.inspection.application.project.query.ReviewProjectSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ListReviewProjectsUseCase {

    private final ReviewProjectQueryPort queryPort;

    public ListReviewProjectsUseCase(ReviewProjectQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    public List<ReviewProjectSummary> execute(ActorContext actor) {
        if (actor.hasRole(DomainRole.SYSTEM_ADMIN)) {
            return queryPort.findAll();
        }
        if (actor.hasRole(DomainRole.HOSPITAL_MANAGER)) {
            String hospitalId = actor.requireHospitalId();
            return queryPort.findByHospitalId(hospitalId);
        }
        throw new RoleDeniedException("Insufficient role to list review projects");
    }
}