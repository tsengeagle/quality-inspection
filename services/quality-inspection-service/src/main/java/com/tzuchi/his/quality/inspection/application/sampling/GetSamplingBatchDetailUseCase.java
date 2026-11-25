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
public class GetSamplingBatchDetailUseCase {

    private final SamplingBatchQueryPort queryPort;
    private final ReviewProjectRepository projectRepository;

    public GetSamplingBatchDetailUseCase(SamplingBatchQueryPort queryPort,
                                         ReviewProjectRepository projectRepository) {
        this.queryPort = queryPort;
        this.projectRepository = projectRepository;
    }

    public SamplingBatchDetailView execute(ActorContext actor, String batchId) {
        actor.requireAnyRole(DomainRole.HOSPITAL_MANAGER, DomainRole.AUDITOR, DomainRole.SYSTEM_ADMIN);
        SamplingBatchDetailView detail = queryPort.findDetailById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("SAMPLING_BATCH_NOT_FOUND",
                        "SamplingBatch not found: " + batchId));
        ReviewProject project = projectRepository.findById(detail.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("PROJECT_NOT_FOUND",
                        "Project not found: " + detail.getProjectId()));
        actor.requireReadScope(project.getHospitalId());
        // SB-DET-02（D-086 第 12 點）：SYSTEM_ADMIN 只有 metadata，沒有抽中的每一筆病歷
        return actor.hasRole(DomainRole.SYSTEM_ADMIN) ? detail.withoutSelectedCases() : detail;
    }
}
