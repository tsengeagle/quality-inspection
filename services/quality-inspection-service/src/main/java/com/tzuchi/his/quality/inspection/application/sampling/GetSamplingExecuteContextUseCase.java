package com.tzuchi.his.quality.inspection.application.sampling;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.project.ProjectStatus;
import com.tzuchi.his.quality.inspection.domain.project.ProjectVersion;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import com.tzuchi.his.quality.inspection.domain.sampling.BatchStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class GetSamplingExecuteContextUseCase {

    private final ReviewProjectRepository projectRepository;
    private final SamplingBatchQueryPort batchQueryPort;

    public GetSamplingExecuteContextUseCase(ReviewProjectRepository projectRepository,
                                            SamplingBatchQueryPort batchQueryPort) {
        this.projectRepository = projectRepository;
        this.batchQueryPort = batchQueryPort;
    }

    public SamplingExecuteContextView execute(ActorContext actor, String projectId) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);

        ReviewProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("PROJECT_NOT_FOUND",
                        "Project not found: " + projectId));

        actor.requireHospitalScope(project.getHospitalId());

        SamplingBatchDetailView latestBatch = batchQueryPort.findLatestDetailByProjectId(projectId).orElse(null);
        boolean executing = latestBatch != null && isExecuting(latestBatch.getStatus());
        boolean executeFailed = latestBatch != null && latestBatch.getStatus() == BatchStatus.FAILED;
        String executedBatchId = latestBatch != null && latestBatch.getStatus() == BatchStatus.COMPLETED
                ? latestBatch.getBatchId()
                : null;

        return new SamplingExecuteContextView(
                project.getProjectId(),
                project.getName(),
                project.getHospitalId(),
                buildRuleSummary(project),
                false,
                executing,
                executeFailed,
                executedBatchId,
                resolveBlockedReason(project),
                null,
                buildPostExecuteSummary(latestBatch)
        );
    }

    private boolean isExecuting(BatchStatus status) {
        return status == BatchStatus.STARTED
                || status == BatchStatus.COLLECTING_CANDIDATES
                || status == BatchStatus.SAMPLING;
    }

    private String resolveBlockedReason(ReviewProject project) {
        if (!project.getCurrentVersion().isPublished()) {
            return "PROJECT_VERSION_NOT_PUBLISHED";
        }
        if (project.getStatus() != ProjectStatus.ACTIVE) {
            return "PROJECT_NOT_ACTIVE";
        }
        return null;
    }

    private SamplingExecuteContextView.PostExecuteSummary buildPostExecuteSummary(SamplingBatchDetailView latestBatch) {
        if (latestBatch == null) {
            return null;
        }

        if (latestBatch.getStatus() == BatchStatus.COMPLETED) {
            return new SamplingExecuteContextView.PostExecuteSummary(
                    "success",
                    "抽選完成",
                    "已建立正式抽選批次，selectedCount=" + latestBatch.getSelectedCount(),
                    latestBatch.getBatchId()
            );
        }

        if (latestBatch.getStatus() != BatchStatus.FAILED) {
            return null;
        }

        String description = latestBatch.getFailureReason();
        if (description == null || description.isBlank()) {
            description = "抽選失敗，請重新預估後再試。";
        }

        return new SamplingExecuteContextView.PostExecuteSummary(
                "failed",
                "抽選失敗",
                description,
                latestBatch.getBatchId()
        );
    }

    private String buildRuleSummary(ReviewProject project) {
        ProjectVersion currentVersion = project.getCurrentVersion();
        String medicalRecordTypes = project.getMedicalRecordTypes().stream()
                .sorted(Comparator.comparing(Enum::name))
                .map(Enum::name)
                .collect(Collectors.joining(", "));
        String scoreSheetId = project.getBoundScoreSheetId() != null ? project.getBoundScoreSheetId() : "UNBOUND";
        String threshold = currentVersion.getQualityThreshold() != null
                ? currentVersion.getQualityThreshold().toPlainString()
                : "未設定";

        return String.join("\n",
                "專案版本：v" + currentVersion.getVersionNumber(),
                "病歷類型：" + (medicalRecordTypes.isBlank() ? "未設定" : medicalRecordTypes),
                "評分表：" + scoreSheetId,
                "品質門檻：" + threshold
        );
    }
}
