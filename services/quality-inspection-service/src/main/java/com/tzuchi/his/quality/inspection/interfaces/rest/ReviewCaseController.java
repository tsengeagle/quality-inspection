package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.reviewcase.*;
import com.tzuchi.his.quality.inspection.application.reviewcase.command.*;
import com.tzuchi.his.quality.inspection.application.reviewcase.dto.AnswerInput;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.GetRereviewRevisionHistoryQuery;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem;
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
    // B4 dependencies
    private final CloseWithoutRereviewUseCase closeWithoutRereviewUseCase;
    private final SendToRereviewUseCase sendToRereviewUseCase;
    private final AssignRereviewerUseCase assignRereviewerUseCase;
    private final ReassignRereviewerUseCase reassignRereviewerUseCase;
    private final StartRereviewUseCase startRereviewUseCase;
    private final SaveRereviewDraftUseCase saveRereviewDraftUseCase;
    private final SubmitRereviewUseCase submitRereviewUseCase;
    private final ReviseRereviewUseCase reviseRereviewUseCase;
    private final GetRereviewRevisionHistoryUseCase getRereviewRevisionHistoryUseCase;

    public ReviewCaseController(AssignReviewCaseUseCase assignUseCase,
                                 ReassignReviewCaseUseCase reassignUseCase,
                                 CancelReviewCaseUseCase cancelUseCase,
                                 ReturnInitialReviewUseCase returnUseCase,
                                 ReopenInitialReviewUseCase reopenUseCase,
                                 StartInitialReviewUseCase startInitialReviewUseCase,
                                 SaveInitialReviewDraftUseCase saveInitialReviewDraftUseCase,
                                 SubmitInitialReviewUseCase submitInitialReviewUseCase,
                                 ReviseInitialReviewUseCase reviseInitialReviewUseCase,
                                 GetRevisionHistoryUseCase getRevisionHistoryUseCase,
                                 CloseWithoutRereviewUseCase closeWithoutRereviewUseCase,
                                 SendToRereviewUseCase sendToRereviewUseCase,
                                 AssignRereviewerUseCase assignRereviewerUseCase,
                                 ReassignRereviewerUseCase reassignRereviewerUseCase,
                                 StartRereviewUseCase startRereviewUseCase,
                                 SaveRereviewDraftUseCase saveRereviewDraftUseCase,
                                 SubmitRereviewUseCase submitRereviewUseCase,
                                 ReviseRereviewUseCase reviseRereviewUseCase,
                                 GetRereviewRevisionHistoryUseCase getRereviewRevisionHistoryUseCase) {
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
        this.closeWithoutRereviewUseCase = closeWithoutRereviewUseCase;
        this.sendToRereviewUseCase = sendToRereviewUseCase;
        this.assignRereviewerUseCase = assignRereviewerUseCase;
        this.reassignRereviewerUseCase = reassignRereviewerUseCase;
        this.startRereviewUseCase = startRereviewUseCase;
        this.saveRereviewDraftUseCase = saveRereviewDraftUseCase;
        this.submitRereviewUseCase = submitRereviewUseCase;
        this.reviseRereviewUseCase = reviseRereviewUseCase;
        this.getRereviewRevisionHistoryUseCase = getRereviewRevisionHistoryUseCase;
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

    // --- B4 Rereview Endpoints ---

    @PostMapping("/{caseId}/close-without-rereview")
    public ResponseEntity<ApiResponse<Void>> closeWithoutRereview(
            @CurrentActor ActorContext actor,
            @PathVariable String caseId) {
        CloseWithoutRereviewCommand cmd = new CloseWithoutRereviewCommand();
        cmd.setCaseId(caseId);
        closeWithoutRereviewUseCase.execute(actor, cmd);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{caseId}/send-to-rereview")
    public ResponseEntity<ApiResponse<Void>> sendToRereview(
            @CurrentActor ActorContext actor,
            @PathVariable String caseId) {
        SendToRereviewCommand cmd = new SendToRereviewCommand();
        cmd.setCaseId(caseId);
        sendToRereviewUseCase.execute(actor, cmd);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{caseId}/assign-rereviewer")
    public ResponseEntity<ApiResponse<Void>> assignRereviewer(
            @CurrentActor ActorContext actor,
            @PathVariable String caseId,
            @RequestBody @Valid AssignRereviewerRequest request) {
        AssignRereviewerCommand cmd = new AssignRereviewerCommand();
        cmd.setCaseId(caseId);
        cmd.setRereviewerId(request.getRereviewerId());
        cmd.setDueAt(request.getDueAt());
        assignRereviewerUseCase.execute(actor, cmd);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{caseId}/reassign-rereviewer")
    public ResponseEntity<ApiResponse<Void>> reassignRereviewer(
            @CurrentActor ActorContext actor,
            @PathVariable String caseId,
            @RequestBody @Valid ReassignRereviewerRequest request) {
        ReassignRereviewerCommand cmd = new ReassignRereviewerCommand();
        cmd.setCaseId(caseId);
        cmd.setRereviewerId(request.getRereviewerId());
        cmd.setDueAt(request.getDueAt());
        reassignRereviewerUseCase.execute(actor, cmd);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{caseId}/start-rereview")
    public ResponseEntity<ApiResponse<Void>> startRereview(
            @CurrentActor ActorContext actor,
            @PathVariable String caseId) {
        StartRereviewCommand cmd = new StartRereviewCommand();
        cmd.setCaseId(caseId);
        startRereviewUseCase.execute(actor, cmd);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PutMapping("/{caseId}/rereview-draft")
    public ResponseEntity<ApiResponse<Void>> saveRereviewDraft(
            @CurrentActor ActorContext actor,
            @PathVariable String caseId,
            @RequestBody @Valid SaveRereviewDraftRequest request) {
        SaveRereviewDraftCommand cmd = new SaveRereviewDraftCommand();
        cmd.setCaseId(caseId);
        cmd.setSubmissionId(request.getSubmissionId());
        if (request.getAnswers() != null) {
            cmd.setAnswers(request.getAnswers().stream()
                    .filter(Objects::nonNull)
                    .map(a -> AnswerItem.ofRaw(a.getItemId(), a.getAnswerValue()))
                    .collect(Collectors.toList()));
        }
        cmd.setTotalScore(request.getTotalScore());
        cmd.setComment(request.getComment());
        saveRereviewDraftUseCase.execute(actor, cmd);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{caseId}/submit-rereview")
    public ResponseEntity<ApiResponse<Void>> submitRereview(
            @CurrentActor ActorContext actor,
            @PathVariable String caseId,
            @RequestBody @Valid SubmitRereviewRequest request) {
        SubmitRereviewCommand cmd = new SubmitRereviewCommand();
        cmd.setCaseId(caseId);
        cmd.setSubmissionId(request.getSubmissionId());
        if (request.getAnswers() != null) {
            cmd.setAnswers(request.getAnswers().stream()
                    .filter(Objects::nonNull)
                    .map(a -> AnswerItem.ofRaw(a.getItemId(), a.getAnswerValue()))
                    .collect(Collectors.toList()));
        }
        cmd.setComment(request.getComment());
        submitRereviewUseCase.execute(actor, cmd);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{caseId}/revise-rereview")
    public ResponseEntity<ApiResponse<Void>> reviseRereview(
            @CurrentActor ActorContext actor,
            @PathVariable String caseId,
            @RequestBody @Valid ReviseRereviewRequest request) {
        ReviseRereviewCommand cmd = new ReviseRereviewCommand();
        cmd.setCaseId(caseId);
        cmd.setSubmissionId(request.getSubmissionId());
        if (request.getAnswers() != null) {
            cmd.setAnswers(request.getAnswers().stream()
                    .filter(Objects::nonNull)
                    .map(a -> AnswerItem.ofRaw(a.getItemId(), a.getAnswerValue()))
                    .collect(Collectors.toList()));
        }
        cmd.setComment(request.getComment());
        reviseRereviewUseCase.execute(actor, cmd);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/{caseId}/rereview-revisions")
    public ResponseEntity<ApiResponse<RevisionHistoryResponse>> getRereviewRevisionHistory(
            @CurrentActor ActorContext actor,
            @PathVariable String caseId) {
        GetRereviewRevisionHistoryQuery query = new GetRereviewRevisionHistoryQuery();
        query.setCaseId(caseId);
        List<ReviewSubmission> submissions = getRereviewRevisionHistoryUseCase.execute(actor, query);
        List<SubmissionSummaryDto> dtos = submissions.stream()
                .map(s -> new SubmissionSummaryDto(
                        s.getSubmissionId(), s.getStage().name(), s.getReviewerId(),
                        s.getStatus().name(), s.getRevisionNo(), s.getTotalScore(),
                        s.getComment(), s.getSubmittedAt(), s.getSupersededBySubmissionId()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(new RevisionHistoryResponse(caseId, dtos)));
    }
}
