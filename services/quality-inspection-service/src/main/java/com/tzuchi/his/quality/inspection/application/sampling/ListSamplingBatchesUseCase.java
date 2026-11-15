package com.tzuchi.his.quality.inspection.application.sampling;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ListSamplingBatchesUseCase {

    private final SamplingBatchQueryPort queryPort;

    public ListSamplingBatchesUseCase(SamplingBatchQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    public Page<SamplingBatchSummary> execute(ActorContext actor,
                                               SamplingBatchQueryCriteria criteria) {
        actor.requireAnyRole(DomainRole.HOSPITAL_MANAGER, DomainRole.AUDITOR, DomainRole.SYSTEM_ADMIN);
        return queryPort.findBatches(criteria.withHospitalId(actor.readScopeHospitalId(criteria.getHospitalId())));
    }
}
