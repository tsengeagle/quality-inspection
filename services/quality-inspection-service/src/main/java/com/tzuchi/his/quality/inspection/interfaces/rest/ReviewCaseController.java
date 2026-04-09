package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.reviewcase.*;
import com.tzuchi.his.quality.inspection.application.reviewcase.dto.AnswerInput;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.interfaces.rest.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/review-cases")
public class ReviewCaseController {

    private final AssignReviewCaseUseCase assignUseCase;
    private final ReassignReviewCaseUseCase reassignUseCase;
    private final CancelReviewCaseUseCase cancelUseCase;
    private final ReturnInitialReviewUseCase returnUseCase;
    private final ReopenInitialReviewUseCase reopenUseCase;
    private final StartInitialReviewUseCase startInitialReviewUseCase;
    private final SaveInitialReviewDraftUseCase saveInitialReviewDraftUseCase;
    private final SubmitInitialReviewUseCase submitInitialReviewUseCase;
    private final ReviseInitialReviewUseCase reviseInitialReviewUseCase;
    private final GetRevisionHistoryUseCase getRevisionHistoryUseCase;

    public ReviewCaseController(AssignReviewCaseUseCase assignUseCase,
                                 ReassignReviewCaseUseCase reassignUseCase,
                                 CancelReviewCaseUseCase cancelUseCase,
                                 ReturnInitialReviewUseCase returnUseCase,
                                 ReopenInitialReviewUseCase reopenUseCase,
                                 StartInitialReviewUseCase startInitialReviewUseCase,
                                 SaveInitialReviewDraftUseCase saveInitialReviewDraftUseCase,
                                 SubmitInitialReviewUseCase submitInitialReviewUseCase,
                                 ReviseInitialReviewUseCase reviseInitialReviewUseCase,
                                 GetRevisionHistoryUseCase getRevisionHistoryUseCase) {
        this.assignUseCase = assignUseCase;
        this.reassignUseCase = reassignUseCase;
        this.cancelUseCase = cancelUseCase;
        this.returnUseCase = returnUseCase;
        this.reopenUseCase = reopenUseCase;
        this.startInitialReviewUseCase = startInitialReviewUseCase;
        this.saveInitialReviewDraftUseCase = saveInitialReviewDraftUseCase;
        this.submitInitialReviewUseCase = submitInitialReviewUseCase;
        this.reviseInitialReviewUseCase = reviseInitialReviewUseCase;
        this.getRevisionHistoryUseCase = getRevisionHistoryUseCase;
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

    // --- B3b endpoints ---

    @PostMapping("/{caseId}/start-initial-review")
    public ResponseEntity<ApiResponse<StartInitialReviewResponse>> startInitialReview(
            @CurrentActor ActorContext actor,
            @PathVariable String caseId) {
        String submissionId = startInitialReviewUseCase.execute(actor, caseId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(new StartInitialReviewResponse(submissionId)));
    }

    @PostMapping("/{caseId}/initial-review/save-draft")
    public ResponseEntity<ApiResponse<SaveDraftResponse>> saveInitialReviewDraft(
            @CurrentActor ActorContext actor,
            @PathVariable String caseId,
            @Valid @RequestBody SaveInitialReviewDraftRequest request) {
        List<AnswerInput> answers = request.getAnswers().stream()
                .filter(Objects::nonNull)
                .map(dto -> new AnswerInput(dto.getItemId(), dto.getAnswerValue()))
                .collect(Collectors.toList());
        BigDecimal totalScore = saveInitialReviewDraftUseCase.execute(
                actor, caseId, answers, request.getComment());
        return ResponseEntity.ok(ApiResponse.ok(new SaveDraftResponse(totalScore)));
    }

    @PostMapping("/{caseId}/initial-review/submit")
    public ResponseEntity<ApiResponse<Void>> submitInitialReview(
            @CurrentActor ActorContext actor,
            @PathVariable String caseId) {
        submitInitialReviewUseCase.execute(actor, caseId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // --- B3c endpoints ---

    @PostMapping("/{caseId}/submissions/{submissionId}/revise")
    public ResponseEntity<ApiResponse<RevisionResultResponse>> reviseInitialReview(
            @CurrentActor ActorContext actor,
            @PathVariable String caseId,
            @PathVariable String submissionId,
            @Valid @RequestBody ReviseInitialReviewRequest request) {
        List<AnswerInput> answers =
                request.getAnswers().stream()
                        .filter(Objects::nonNull)
                        .map(dto -> new AnswerInput(dto.getItemId(), dto.getAnswerValue()))
                        .collect(Collectors.toList());
        RevisionResult result = reviseInitialReviewUseCase.execute(
                actor, caseId, submissionId, answers, request.getComment());
        return ResponseEntity.ok(ApiResponse.ok(new RevisionResultResponse(
                result.getNewSubmissionId(), result.getRevisionNo(),
                result.getTotalScore(), result.getStatus())));
    }

    @GetMapping("/{caseId}/revision-history")
    public ResponseEntity<ApiResponse<RevisionHistoryResponse>> getRevisionHistory(
            @CurrentActor ActorContext actor,
            @PathVariable String caseId) {
        List<ReviewSubmission> submissions =
                getRevisionHistoryUseCase.execute(actor, caseId);
        List<SubmissionSummaryDto> dtos = submissions.stream()
                .map(s -> new SubmissionSummaryDto(
                        s.getSubmissionId(), s.getStage().name(), s.getReviewerId(),
                        s.getStatus().name(), s.getRevisionNo(), s.getTotalScore(),
                        s.getComment(), s.getSubmittedAt(), s.getSupersededBySubmissionId()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(new RevisionHistoryResponse(caseId, dtos)));
    }
}
