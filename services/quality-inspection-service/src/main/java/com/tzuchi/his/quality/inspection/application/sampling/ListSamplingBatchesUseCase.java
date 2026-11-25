package com.tzuchi.his.quality.inspection.application.sampling;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.Page;
import com.tzuchi.his.quality.inspection.domain.sampling.BatchStatus;
import com.tzuchi.his.quality.inspection.domain.sampling.ExecutionMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ListSamplingBatchesUseCase {

    private final SamplingBatchQueryPort queryPort;

    public ListSamplingBatchesUseCase(SamplingBatchQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    /**
     * SB-LST-01～04（D-086 第 11 點）。不是 SYSTEM_ADMIN 只看本院，輸入的 hospitalId 不採用；
     * size 沒有給或不是正數時是 20，最大 100；page 沒有給或是負數時是 0。
     */
    public Page<SamplingBatchSummary> execute(ActorContext actor, String projectId, BatchStatus status,
                                               ExecutionMode executionMode, String hospitalId,
                                               Integer page, Integer size) {
        actor.requireAnyRole(DomainRole.HOSPITAL_MANAGER, DomainRole.AUDITOR, DomainRole.SYSTEM_ADMIN);
        SamplingBatchQueryCriteria criteria = new SamplingBatchQueryCriteria(
                projectId, status, executionMode, page != null ? page : 0, size != null ? size : 0);
        return queryPort.findBatches(criteria.withHospitalId(actor.readScopeHospitalId(hospitalId)));
    }
}
