package com.tzuchi.his.quality.inspection.application.sampling;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetSamplingBatchDetailUseCase {

    private final SamplingBatchQueryPort queryPort;

    public GetSamplingBatchDetailUseCase(SamplingBatchQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    public SamplingBatchDetailView execute(ActorContext actor, String batchId) {
        actor.requireAnyRole(DomainRole.HOSPITAL_MANAGER, DomainRole.AUDITOR, DomainRole.SYSTEM_ADMIN);
        return queryPort.findDetailById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("SAMPLING_BATCH_NOT_FOUND",
                        "SamplingBatch not found: " + batchId));
    }
}
