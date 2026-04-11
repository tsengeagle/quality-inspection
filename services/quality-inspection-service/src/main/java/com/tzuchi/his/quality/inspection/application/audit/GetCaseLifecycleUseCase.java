package com.tzuchi.his.quality.inspection.application.audit;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class GetCaseLifecycleUseCase {

    private final AuditTrailQueryPort queryPort;

    public GetCaseLifecycleUseCase(AuditTrailQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    public List<AuditTrailEntry> execute(ActorContext actor, String caseId) {
        actor.requireAnyRole(
                DomainRole.HOSPITAL_MANAGER, DomainRole.REVIEWER,
                DomainRole.REREVIEWER, DomainRole.AUDITOR, DomainRole.SYSTEM_ADMIN);
        return queryPort.findLifecycleByEntityId("ReviewCase", caseId);
    }
}
