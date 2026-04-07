package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.sampling.ExecuteSamplingUseCase;
import com.tzuchi.his.quality.inspection.application.sampling.PreviewSamplingPopulationUseCase;
import com.tzuchi.his.quality.inspection.application.sampling.RetrySamplingBatchUseCase;
import com.tzuchi.his.quality.inspection.application.sampling.SamplingPreviewResult;
import com.tzuchi.his.quality.inspection.interfaces.rest.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class SamplingController {

    private final PreviewSamplingPopulationUseCase previewUseCase;
    private final ExecuteSamplingUseCase executeUseCase;
    private final RetrySamplingBatchUseCase retryUseCase;

    public SamplingController(PreviewSamplingPopulationUseCase previewUseCase,
                               ExecuteSamplingUseCase executeUseCase,
                               RetrySamplingBatchUseCase retryUseCase) {
        this.previewUseCase = previewUseCase;
        this.executeUseCase = executeUseCase;
        this.retryUseCase = retryUseCase;
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
}
