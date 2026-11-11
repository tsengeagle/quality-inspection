package com.tzuchi.his.quality.inspection.application.audit;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ListAuditTrailsUseCase {

    private final AuditTrailQueryPort queryPort;

    public ListAuditTrailsUseCase(AuditTrailQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    public Page<AuditTrailEntry> execute(ActorContext actor, AuditTrailQueryCriteria criteria) {
        actor.requireAnyRole(DomainRole.SYSTEM_ADMIN, DomainRole.HOSPITAL_MANAGER, DomainRole.AUDITOR);
        return queryPort.findAuditTrails(
                criteria.withHospitalId(actor.readScopeHospitalId(criteria.getHospitalId())));
    }
}
