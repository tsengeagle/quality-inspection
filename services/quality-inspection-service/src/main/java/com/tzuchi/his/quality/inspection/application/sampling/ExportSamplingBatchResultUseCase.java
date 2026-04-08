package com.tzuchi.his.quality.inspection.application.sampling;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ExportSamplingBatchResultUseCase {

    private final SamplingBatchQueryPort queryPort;

    public ExportSamplingBatchResultUseCase(SamplingBatchQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    public SamplingBatchExportResult execute(ActorContext actor, String batchId) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        SamplingBatchDetailView detail = queryPort.findDetailById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("SAMPLING_BATCH_NOT_FOUND",
                        "SamplingBatch not found: " + batchId));
        return new SamplingBatchExportResult(detail);
    }
}
