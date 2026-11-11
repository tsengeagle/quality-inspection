package com.tzuchi.his.quality.inspection.application.audit;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class GetCaseLifecycleUseCase {

    private final AuditTrailQueryPort queryPort;
    private final ReviewCaseRepository caseRepository;

    public GetCaseLifecycleUseCase(AuditTrailQueryPort queryPort, ReviewCaseRepository caseRepository) {
        this.queryPort = queryPort;
        this.caseRepository = caseRepository;
    }

    public List<AuditTrailEntry> execute(ActorContext actor, String caseId) {
        actor.requireAnyRole(
                DomainRole.HOSPITAL_MANAGER, DomainRole.REVIEWER,
                DomainRole.REREVIEWER, DomainRole.AUDITOR, DomainRole.SYSTEM_ADMIN);
        caseRepository.findById(caseId)
                .ifPresent(reviewCase -> actor.requireReadScope(reviewCase.getHospitalId()));
        return queryPort.findLifecycleByEntityId("ReviewCase", caseId);
    }
}
