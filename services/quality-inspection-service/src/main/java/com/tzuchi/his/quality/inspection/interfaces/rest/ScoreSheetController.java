package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.tzuchi.his.quality.inspection.application.scoresheet.CreateScoreSheetDraftUseCase;
import com.tzuchi.his.quality.inspection.application.scoresheet.PublishScoreSheetVersionUseCase;
import com.tzuchi.his.quality.inspection.interfaces.rest.dto.ApiResponse;
import com.tzuchi.his.quality.inspection.interfaces.rest.dto.CreateScoreSheetDraftRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/score-sheets")
public class ScoreSheetController {

    private final CreateScoreSheetDraftUseCase createDraftUseCase;
    private final PublishScoreSheetVersionUseCase publishUseCase;

    public ScoreSheetController(CreateScoreSheetDraftUseCase createDraftUseCase,
                                PublishScoreSheetVersionUseCase publishUseCase) {
        this.createDraftUseCase = createDraftUseCase;
        this.publishUseCase = publishUseCase;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> createDraft(
            @RequestBody CreateScoreSheetDraftRequest request) {
        String scoreSheetId = createDraftUseCase.execute(request.getProjectId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(Map.of("scoreSheetId", scoreSheetId)));
    }

    @PostMapping("/{scoreSheetId}/publish-version")
    public ResponseEntity<ApiResponse<Void>> publishVersion(@PathVariable String scoreSheetId) {
        publishUseCase.execute(scoreSheetId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
