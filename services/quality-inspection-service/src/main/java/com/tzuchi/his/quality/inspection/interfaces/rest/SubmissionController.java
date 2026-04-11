package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.reviewcase.GetSubmissionDetailUseCase;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.SubmissionDetailView;
import com.tzuchi.his.quality.inspection.interfaces.rest.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/review-submissions")
public class SubmissionController {

    private final GetSubmissionDetailUseCase getSubmissionDetailUseCase;

    public SubmissionController(GetSubmissionDetailUseCase getSubmissionDetailUseCase) {
        this.getSubmissionDetailUseCase = getSubmissionDetailUseCase;
    }

    @GetMapping("/{submissionId}")
    public ResponseEntity<ApiResponse<SubmissionDetailView>> getSubmissionDetail(
            @CurrentActor ActorContext actor,
            @PathVariable String submissionId) {
        SubmissionDetailView detail = getSubmissionDetailUseCase.execute(actor, submissionId);
        return ResponseEntity.ok(ApiResponse.ok(detail));
    }
}
