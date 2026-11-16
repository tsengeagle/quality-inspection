package com.tzuchi.his.quality.inspection.application.audit;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

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
        List<AuditTrailEntry> entries = queryPort.findLifecycleByEntityId("ReviewCase", caseId);
        // D-033：detail 會帶初審分數與 finalScore；只要持有複審醫師角色就不給，不論同時持有哪些角色。
        // api-spec：SYSTEM_ADMIN 在生命週期只取得 metadata。
        if (actor.hasRole(DomainRole.REREVIEWER) || actor.hasRole(DomainRole.SYSTEM_ADMIN)) {
            return entries.stream().map(AuditTrailEntry::withoutDetail).collect(Collectors.toList());
        }
        return entries;
    }
}
