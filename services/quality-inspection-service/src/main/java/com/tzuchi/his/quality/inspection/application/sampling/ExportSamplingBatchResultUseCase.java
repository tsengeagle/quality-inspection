package com.tzuchi.his.quality.inspection.application.sampling;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ExportSamplingBatchResultUseCase {

    private final SamplingBatchQueryPort queryPort;
    private final ReviewProjectRepository projectRepository;

    public ExportSamplingBatchResultUseCase(SamplingBatchQueryPort queryPort,
                                            ReviewProjectRepository projectRepository) {
        this.queryPort = queryPort;
        this.projectRepository = projectRepository;
    }

    public SamplingBatchExportResult execute(ActorContext actor, String batchId) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        SamplingBatchDetailView detail = queryPort.findDetailById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("SAMPLING_BATCH_NOT_FOUND",
                        "SamplingBatch not found: " + batchId));
        ReviewProject project = projectRepository.findById(detail.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("PROJECT_NOT_FOUND",
                        "Project not found: " + detail.getProjectId()));
        actor.requireHospitalScope(project.getHospitalId());
        return new SamplingBatchExportResult(detail);
    }
}
