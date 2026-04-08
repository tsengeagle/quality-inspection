package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.reviewcase.*;
import com.tzuchi.his.quality.inspection.interfaces.rest.dto.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/review-cases")
public class ReviewCaseController {

    private final AssignReviewCaseUseCase assignUseCase;
    private final ReassignReviewCaseUseCase reassignUseCase;
    private final CancelReviewCaseUseCase cancelUseCase;
    private final ReturnInitialReviewUseCase returnUseCase;
    private final ReopenInitialReviewUseCase reopenUseCase;

    public ReviewCaseController(AssignReviewCaseUseCase assignUseCase,
                                 ReassignReviewCaseUseCase reassignUseCase,
                                 CancelReviewCaseUseCase cancelUseCase,
                                 ReturnInitialReviewUseCase returnUseCase,
                                 ReopenInitialReviewUseCase reopenUseCase) {
        this.assignUseCase = assignUseCase;
        this.reassignUseCase = reassignUseCase;
        this.cancelUseCase = cancelUseCase;
        this.returnUseCase = returnUseCase;
        this.reopenUseCase = reopenUseCase;
    }

    @PostMapping("/{caseId}/assign-reviewer")
    public ResponseEntity<ApiResponse<Void>> assignReviewer(
            @CurrentActor ActorContext actor,
            @PathVariable String caseId,
            @Valid @RequestBody AssignReviewerRequest request) {
        assignUseCase.execute(actor, caseId, request.getReviewerId(), request.getDueAt());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{caseId}/reassign-reviewer")
    public ResponseEntity<ApiResponse<Void>> reassignReviewer(
            @CurrentActor ActorContext actor,
            @PathVariable String caseId,
            @Valid @RequestBody ReassignReviewerRequest request) {
        reassignUseCase.execute(actor, caseId, request.getReviewerId(), request.getDueAt());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{caseId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancel(
            @CurrentActor ActorContext actor,
            @PathVariable String caseId,
            @Valid @RequestBody CancelReviewCaseRequest request) {
        cancelUseCase.execute(actor, caseId, request.getReason());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{caseId}/return")
    public ResponseEntity<ApiResponse<Void>> returnCase(
            @CurrentActor ActorContext actor,
            @PathVariable String caseId,
            @Valid @RequestBody ReturnReviewCaseRequest request) {
        returnUseCase.execute(actor, caseId, request.getReason());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{caseId}/reopen")
    public ResponseEntity<ApiResponse<Void>> reopen(
            @CurrentActor ActorContext actor,
            @PathVariable String caseId) {
        reopenUseCase.execute(actor, caseId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
