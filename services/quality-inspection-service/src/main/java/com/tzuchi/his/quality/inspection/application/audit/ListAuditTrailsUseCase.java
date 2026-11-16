package com.tzuchi.his.quality.inspection.application.audit;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ListAuditTrailsUseCase {

    private final AuditTrailQueryPort queryPort;

    public ListAuditTrailsUseCase(AuditTrailQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    public Page<AuditTrailEntry> execute(ActorContext actor, AuditTrailQueryCriteria criteria) {
        actor.requireAnyRole(DomainRole.SYSTEM_ADMIN, DomainRole.HOSPITAL_MANAGER, DomainRole.AUDITOR);
        Page<AuditTrailEntry> page = queryPort.findAuditTrails(
                criteria.withHospitalId(actor.readScopeHospitalId(criteria.getHospitalId())));
        // D-033：同時持有複審醫師角色的人不可改由稽核列表取得 detail 裡的初審分數與 finalScore。
        if (!actor.hasRole(DomainRole.REREVIEWER)) {
            return page;
        }
        return new Page<>(page.getContent().stream().map(AuditTrailEntry::withoutDetail).collect(Collectors.toList()),
                page.getPage(), page.getSize(), page.getTotalElements());
    }
}
