package com.tzuchi.his.quality.inspection.application.audit;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ListAuditTrailsUseCase {

    private final AuditTrailQueryPort queryPort;
    private final ScoredDetailMask scoredDetailMask;

    public ListAuditTrailsUseCase(AuditTrailQueryPort queryPort, ScoredDetailMask scoredDetailMask) {
        this.queryPort = queryPort;
        this.scoredDetailMask = scoredDetailMask;
    }

    /** D-071 第 6 點：平的參數；page、size 沒給時用 0、20。 */
    public Page<AuditTrailEntry> execute(ActorContext actor, String aggregateType, String aggregateId,
                                         String hospitalId, String eventName, Instant from, Instant to,
                                         Integer page, Integer size) {
        actor.requireAnyRole(DomainRole.SYSTEM_ADMIN, DomainRole.HOSPITAL_MANAGER, DomainRole.AUDITOR);
        AuditTrailQueryCriteria criteria = new AuditTrailQueryCriteria(aggregateType, aggregateId,
                actor.readScopeHospitalId(hospitalId), eventName, from, to,
                page == null ? 0 : page, size == null ? 20 : size);
        Page<AuditTrailEntry> result = queryPort.findAuditTrails(criteria);
        List<AuditTrailEntry> content;
        // D-033：同時持有複審醫師角色的人不可改由稽核列表取得 detail 裡的初審分數與 finalScore。
        // D-071 第 5 點：SYSTEM_ADMIN 只看 metadata，與生命週期查詢一致。
        if (actor.hasRole(DomainRole.REREVIEWER) || actor.hasRole(DomainRole.SYSTEM_ADMIN)) {
            content = result.getContent().stream().map(AuditTrailEntry::withoutDetail).collect(Collectors.toList());
        } else {
            content = scoredDetailMask.apply(actor, result.getContent());
        }
        return new Page<>(content, result.getPage(), result.getSize(), result.getTotalElements());
    }
}
