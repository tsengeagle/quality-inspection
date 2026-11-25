package com.tzuchi.his.quality.inspection.application.sampling;

import com.tzuchi.his.quality.inspection.domain.common.DomainValidationException;
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
    public Page<SamplingBatchSummary> execute(ActorContext actor, String projectId, String status,
                                               String executionMode, String hospitalId,
                                               Integer page, Integer size) {
        actor.requireAnyRole(DomainRole.HOSPITAL_MANAGER, DomainRole.AUDITOR, DomainRole.SYSTEM_ADMIN);
        SamplingBatchQueryCriteria criteria = new SamplingBatchQueryCriteria(
                projectId, parse(BatchStatus.class, "status", status),
                parse(ExecutionMode.class, "executionMode", executionMode), page != null ? page : 0, size != null ? size : 0);
        return queryPort.findBatches(criteria.withHospitalId(actor.readScopeHospitalId(hospitalId)));
    }

    /** SB-LST-05（D-088 第 13 點）：不認得的 status、executionMode 在角色檢查之後回 BAD_REQUEST。 */
    private static <E extends Enum<E>> E parse(Class<E> type, String field, String value) {
        if (value == null) return null;
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException e) {
            throw new DomainValidationException("BAD_REQUEST", "Invalid " + field + ": " + value);
        }
    }
}
