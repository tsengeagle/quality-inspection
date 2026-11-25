package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.Page;
import com.tzuchi.his.quality.inspection.application.sampling.*;
import com.tzuchi.his.quality.inspection.domain.sampling.BatchStatus;
import com.tzuchi.his.quality.inspection.domain.sampling.ExecutionMode;
import com.tzuchi.his.quality.inspection.interfaces.rest.dto.ApiResponse;
import com.tzuchi.his.quality.inspection.interfaces.rest.dto.SamplingExecuteContextResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class SamplingController {

    private final PreviewSamplingPopulationUseCase previewUseCase;
    private final ExecuteSamplingUseCase executeUseCase;
    private final RetrySamplingBatchUseCase retryUseCase;
    private final ListSamplingBatchesUseCase listUseCase;
    private final GetSamplingBatchDetailUseCase detailUseCase;
    private final ExportSamplingBatchResultUseCase exportUseCase;
    private final GetSamplingExecuteContextUseCase executeContextUseCase;

    public SamplingController(PreviewSamplingPopulationUseCase previewUseCase,
                               ExecuteSamplingUseCase executeUseCase,
                               RetrySamplingBatchUseCase retryUseCase,
                               ListSamplingBatchesUseCase listUseCase,
                               GetSamplingBatchDetailUseCase detailUseCase,
                               ExportSamplingBatchResultUseCase exportUseCase,
                               GetSamplingExecuteContextUseCase executeContextUseCase) {
        this.previewUseCase = previewUseCase;
        this.executeUseCase = executeUseCase;
        this.retryUseCase = retryUseCase;
        this.listUseCase = listUseCase;
        this.detailUseCase = detailUseCase;
        this.exportUseCase = exportUseCase;
        this.executeContextUseCase = executeContextUseCase;
    }

    @GetMapping("/review-projects/{projectId}/sampling/execute-context")
    public ResponseEntity<ApiResponse<SamplingExecuteContextResponse>> getExecuteContext(
            @CurrentActor ActorContext actor,
            @PathVariable String projectId) {
        SamplingExecuteContextView view = executeContextUseCase.execute(actor, projectId);
        return ResponseEntity.ok(ApiResponse.ok(toExecuteContextResponse(view)));
    }

    @PostMapping("/review-projects/{projectId}/sampling/preview")
    public ResponseEntity<ApiResponse<SamplingPreviewResult>> previewSampling(
            @CurrentActor ActorContext actor,
            @PathVariable String projectId) {
        SamplingPreviewResult result = previewUseCase.execute(actor, projectId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/review-projects/{projectId}/sampling/execute")
    public ResponseEntity<ApiResponse<Map<String, String>>> executeSampling(
            @CurrentActor ActorContext actor,
            @PathVariable String projectId) {
        String batchId = executeUseCase.execute(actor, projectId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(Map.of("batchId", batchId)));
    }

    @PostMapping("/sampling-batches/{batchId}/retry")
    public ResponseEntity<ApiResponse<Map<String, String>>> retrySampling(
            @CurrentActor ActorContext actor,
            @PathVariable String batchId) {
        String newBatchId = retryUseCase.execute(actor, batchId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(Map.of("batchId", newBatchId)));
    }

    @GetMapping("/sampling-batches")
    public ResponseEntity<ApiResponse<Page<SamplingBatchSummary>>> listBatches(
            @CurrentActor ActorContext actor,
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String executionMode,
            @RequestParam(required = false) String hospitalId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        BatchStatus batchStatus = null;
        if (status != null) {
            try {
                batchStatus = BatchStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid status: " + status);
            }
        }
        ExecutionMode mode = null;
        if (executionMode != null) {
            try {
                mode = ExecutionMode.valueOf(executionMode);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid executionMode: " + executionMode);
            }
        }
        Page<SamplingBatchSummary> result = listUseCase.execute(
                actor, projectId, batchStatus, mode, hospitalId, page, size);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/sampling-batches/{batchId}")
    public ResponseEntity<ApiResponse<SamplingBatchDetailView>> getBatchDetail(
            @CurrentActor ActorContext actor,
            @PathVariable String batchId) {
        SamplingBatchDetailView detail = detailUseCase.execute(actor, batchId);
        return ResponseEntity.ok(ApiResponse.ok(detail));
    }

    @GetMapping("/sampling-batches/{batchId}/export")
    public ResponseEntity<ApiResponse<SamplingBatchExportResult>> exportBatchResult(
            @CurrentActor ActorContext actor,
            @PathVariable String batchId) {
        SamplingBatchExportResult result = exportUseCase.execute(actor, batchId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    private SamplingExecuteContextResponse toExecuteContextResponse(SamplingExecuteContextView view) {
        SamplingExecuteContextResponse.PreviewStatsDto previewStats = null;
        if (view.getPreviewStats() != null) {
            previewStats = new SamplingExecuteContextResponse.PreviewStatsDto(
                    view.getPreviewStats().getPopulationCount(),
                    view.getPreviewStats().getEligibleCount(),
                    view.getPreviewStats().getExcludedCount()
            );
        }

        SamplingExecuteContextResponse.PostExecuteSummaryDto postExecuteSummary = null;
        if (view.getPostExecuteSummary() != null) {
            postExecuteSummary = new SamplingExecuteContextResponse.PostExecuteSummaryDto(
                    view.getPostExecuteSummary().getTone(),
                    view.getPostExecuteSummary().getTitle(),
                    view.getPostExecuteSummary().getDescription(),
                view.getPostExecuteSummary().getBatchId(),
                view.getPostExecuteSummary().getStartedAt() != null
                    ? view.getPostExecuteSummary().getStartedAt().toString()
                    : null,
                view.getPostExecuteSummary().getFinishedAt() != null
                    ? view.getPostExecuteSummary().getFinishedAt().toString()
                    : null
            );
        }

        return new SamplingExecuteContextResponse(
                view.getProjectId(),
                view.getProjectName(),
                view.getHospitalId(),
            view.getProjectStatus(),
            view.getVersionNo(),
            view.getScoreSheetId(),
            view.getQualityThreshold(),
                view.getRuleSummary(),
                view.isPreviewReady(),
                view.isExecuting(),
                view.isExecuteFailed(),
                view.getExecutedBatchId(),
                view.getBlockedReason(),
                previewStats,
                postExecuteSummary
        );
    }
}
