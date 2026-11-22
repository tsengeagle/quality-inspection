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
    private final ScoredDetailMask scoredDetailMask;

    public GetCaseLifecycleUseCase(AuditTrailQueryPort queryPort, ReviewCaseRepository caseRepository,
                                   ScoredDetailMask scoredDetailMask) {
        this.queryPort = queryPort;
        this.caseRepository = caseRepository;
        this.scoredDetailMask = scoredDetailMask;
    }

    public CaseLifecycle execute(ActorContext actor, String caseId) {
        actor.requireAnyRole(
                DomainRole.HOSPITAL_MANAGER, DomainRole.REVIEWER,
                DomainRole.REREVIEWER, DomainRole.AUDITOR, DomainRole.SYSTEM_ADMIN);
        caseRepository.findById(caseId)
                .ifPresent(reviewCase -> actor.requireReadScope(reviewCase.getHospitalId()));
        List<AuditTrailEntry> entries = queryPort.findLifecycleByEntityId("ReviewCase", caseId);
        // D-033：detail 會帶初審分數與 finalScore；只要持有複審醫師角色就不給，不論同時持有哪些角色。
        // api-spec：SYSTEM_ADMIN 在生命週期只取得 metadata。
        if (actor.hasRole(DomainRole.REREVIEWER) || actor.hasRole(DomainRole.SYSTEM_ADMIN)) {
            return new CaseLifecycle(entries.stream().map(AuditTrailEntry::withoutDetail).collect(Collectors.toList()));
        }
        // D-071：帶分數的那幾筆依 D-055 第 6 點遮罩。
        return new CaseLifecycle(scoredDetailMask.apply(actor, entries));
    }
}
