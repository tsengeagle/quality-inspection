package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.tzuchi.his.quality.inspection.application.audit.AuditTrailEntry;
import com.tzuchi.his.quality.inspection.application.audit.GetCaseLifecycleUseCase;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.Page;
import com.tzuchi.his.quality.inspection.application.reviewcase.*;
import com.tzuchi.his.quality.inspection.application.reviewcase.command.*;
import com.tzuchi.his.quality.inspection.application.reviewcase.dto.AnswerInput;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.GetRereviewRevisionHistoryQuery;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.AssignmentBootstrapView;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.AssignmentCandidateRole;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.AssignmentCandidateView;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.ReviewCaseQueryCriteria;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.ReviewCaseSummary;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.ReviewCaseDetailView;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.RevisionHistoryView;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem;
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
    private final ListReviewCasesUseCase listReviewCasesUseCase;
    private final GetReviewCaseDetailUseCase getReviewCaseDetailUseCase;
        private final GetCaseAssignmentContextUseCase getCaseAssignmentContextUseCase;
        private final ListCaseAssignmentCandidatesUseCase listCaseAssignmentCandidatesUseCase;
    private final GetCaseLifecycleUseCase getCaseLifecycleUseCase;

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
                                 GetRereviewRevisionHistoryUseCase getRereviewRevisionHistoryUseCase,
                                 ListReviewCasesUseCase listReviewCasesUseCase,
                                 GetReviewCaseDetailUseCase getReviewCaseDetailUseCase,
                                 GetCaseAssignmentContextUseCase getCaseAssignmentContextUseCase,
                                 ListCaseAssignmentCandidatesUseCase listCaseAssignmentCandidatesUseCase,
                                 GetCaseLifecycleUseCase getCaseLifecycleUseCase) {
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
        this.listReviewCasesUseCase = listReviewCasesUseCase;
        this.getReviewCaseDetailUseCase = getReviewCaseDetailUseCase;
                this.getCaseAssignmentContextUseCase = getCaseAssignmentContextUseCase;
                this.listCaseAssignmentCandidatesUseCase = listCaseAssignmentCandidatesUseCase;
        this.getCaseLifecycleUseCase = getCaseLifecycleUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ReviewCaseSummary>>> listCases(
            @CurrentActor ActorContext actor,
            @RequestParam(required = false) String hospitalId,
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) String batchId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String reviewerId,
            @RequestParam(required = false) String rereviewerId,
            @RequestParam(required = false) String medicalRecordType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        CaseStatus caseStatus = null;
        if (status != null) {
            try {
                caseStatus = CaseStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid status: " + status);
            }
        }
        ReviewCaseQueryCriteria criteria = new ReviewCaseQueryCriteria(
                hospitalId, projectId, batchId, caseStatus,
                reviewerId, rereviewerId, medicalRecordType, page, size);
        Page<ReviewCaseSummary> result = listReviewCasesUseCase.execute(actor, criteria);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{caseId}")
    public ResponseEntity<ApiResponse<ReviewCaseDetailView>> getCaseDetail(
            @CurrentActor ActorContext actor,
            @PathVariable String caseId) {
        ReviewCaseDetailView detail = getReviewCaseDetailUseCase.execute(actor, caseId);
        return ResponseEntity.ok(ApiResponse.ok(detail));
    }

        @GetMapping("/{caseId}/assignment-context")
        public ResponseEntity<ApiResponse<AssignmentBootstrapView>> getAssignmentContext(
                        @CurrentActor ActorContext actor,
                        @PathVariable String caseId) {
                AssignmentBootstrapView view = getCaseAssignmentContextUseCase.execute(actor, caseId);
                return ResponseEntity.ok(ApiResponse.ok(view));
        }

        @GetMapping("/{caseId}/assignment-candidates")
        public ResponseEntity<ApiResponse<List<AssignmentCandidateView>>> listAssignmentCandidates(
                        @CurrentActor ActorContext actor,
                        @PathVariable String caseId,
                        @RequestParam String role,
                        @RequestParam(required = false) String keyword) {
                AssignmentCandidateRole candidateRole;
                try {
                        candidateRole = AssignmentCandidateRole.valueOf(role);
                } catch (IllegalArgumentException ex) {
                        throw new BadRequestException("Invalid candidate role: " + role);
                }
                List<AssignmentCandidateView> candidates = listCaseAssignmentCandidatesUseCase.execute(
                                actor, caseId, candidateRole, keyword);
                return ResponseEntity.ok(ApiResponse.ok(candidates));
        }

    @GetMapping("/{caseId}/lifecycle")
    public ResponseEntity<ApiResponse<List<AuditTrailEntry>>> getCaseLifecycle(
            @CurrentActor ActorContext actor,
            @PathVariable String caseId) {
        List<AuditTrailEntry> entries = getCaseLifecycleUseCase.execute(actor, caseId);
        return ResponseEntity.ok(ApiResponse.ok(entries));
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

    @PostMapping("/{caseId}/initial-review/return")
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

    @PostMapping("/{caseId}/initial-review/start")
    public ResponseEntity<ApiResponse<StartInitialReviewResponse>> startInitialReview(
            @CurrentActor ActorContext actor,
            @PathVariable String caseId) {
        String submissionId = startInitialReviewUseCase.execute(actor, caseId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(new StartInitialReviewResponse(submissionId)));
    }

    @PostMapping("/{caseId}/initial-review/draft")
    public ResponseEntity<ApiResponse<SaveDraftResponse>> saveInitialReviewDraft(
            @CurrentActor ActorContext actor,
            @PathVariable String caseId,
            @Valid @RequestBody SaveInitialReviewDraftRequest request) {
        List<AnswerInput> answers = request.getAnswers().stream()
                .filter(Objects::nonNull)
                .map(dto -> new AnswerInput(dto.getItemId(), dto.getAnswerValue()))
                .collect(Collectors.toList());
        SaveDraftResult result = saveInitialReviewDraftUseCase.execute(
                actor, caseId, answers, request.getComment());
        return ResponseEntity.ok(ApiResponse.ok(new SaveDraftResponse(
                result.getSubmissionId(), result.getStatus(),
                result.getStage(), result.getTotalScore())));
    }

    @PostMapping("/{caseId}/initial-review/submit")
    public ResponseEntity<ApiResponse<SubmitInitialReviewResponse>> submitInitialReview(
            @CurrentActor ActorContext actor,
            @PathVariable String caseId) {
        SubmitInitialReviewResult result = submitInitialReviewUseCase.execute(actor, caseId);
        return ResponseEntity.ok(ApiResponse.ok(new SubmitInitialReviewResponse(
                result.getSubmissionId(), result.getStatus(),
                result.getCaseId(), result.getCaseStatus(),
                result.getTotalScore(), result.isRereviewRequired())));
    }

    // --- B3c endpoints ---

    @PostMapping("/{caseId}/initial-review/submissions/{submissionId}/revise")
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
                result.getNewSubmissionId(), result.getOldSubmissionId(),
                result.getRevisionNo(), result.getTotalScore(),
                result.getStatus(), result.getStage(), result.getCaseId())));
    }

    @GetMapping("/{caseId}/revision-history")
    public ResponseEntity<ApiResponse<RevisionHistoryResponse>> getRevisionHistory(
            @CurrentActor ActorContext actor,
            @PathVariable String caseId) {
        RevisionHistoryView view = getRevisionHistoryUseCase.execute(actor, caseId);
        List<SubmissionSummaryDto> dtos = view.getEntries().stream()
                .map(e -> new SubmissionSummaryDto(
                        e.getSubmissionId(), e.getStage(), e.getReviewerId(),
                        e.getStatus(), e.getRevisionNo(), e.getTotalScore(),
                        e.getComment(), e.getSubmittedAt(), e.getSupersededBySubmissionId()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(
                new RevisionHistoryResponse(view.getCaseId(), dtos, view.isHasExceptionEntry())));
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

    @PostMapping("/{caseId}/rereview/draft")
    public ResponseEntity<ApiResponse<SaveDraftResponse>> saveRereviewDraft(
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
        SaveDraftResult result = saveRereviewDraftUseCase.execute(actor, cmd);
        return ResponseEntity.ok(ApiResponse.ok(new SaveDraftResponse(
                result.getSubmissionId(), result.getStatus(),
                result.getStage(), result.getTotalScore())));
    }

    @PostMapping("/{caseId}/rereview/submit")
    public ResponseEntity<ApiResponse<SubmitRereviewResponse>> submitRereview(
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
        SubmitRereviewResult result = submitRereviewUseCase.execute(actor, cmd);
        return ResponseEntity.ok(ApiResponse.ok(new SubmitRereviewResponse(
                result.getSubmissionId(), result.getStatus(),
                result.getCaseId(), result.getCaseStatus(),
                result.getFinalScore())));
    }

    @PostMapping("/{caseId}/rereview/submissions/{submissionId}/revise")
    public ResponseEntity<ApiResponse<RevisionResultResponse>> reviseRereview(
            @CurrentActor ActorContext actor,
            @PathVariable String caseId,
            @PathVariable String submissionId,
            @RequestBody @Valid ReviseRereviewRequest request) {
        ReviseRereviewCommand cmd = new ReviseRereviewCommand();
        cmd.setCaseId(caseId);
        cmd.setSubmissionId(submissionId);
        if (request.getAnswers() != null) {
            cmd.setAnswers(request.getAnswers().stream()
                    .filter(Objects::nonNull)
                    .map(a -> AnswerItem.ofRaw(a.getItemId(), a.getAnswerValue()))
                    .collect(Collectors.toList()));
        }
        cmd.setComment(request.getComment());
        RevisionResult result = reviseRereviewUseCase.execute(actor, cmd);
        return ResponseEntity.ok(ApiResponse.ok(new RevisionResultResponse(
                result.getNewSubmissionId(), result.getOldSubmissionId(),
                result.getRevisionNo(), result.getTotalScore(),
                result.getStatus(), result.getStage(), result.getCaseId())));
    }

    @GetMapping("/{caseId}/rereview-revisions")
    public ResponseEntity<ApiResponse<RevisionHistoryResponse>> getRereviewRevisionHistory(
            @CurrentActor ActorContext actor,
            @PathVariable String caseId) {
        GetRereviewRevisionHistoryQuery query = new GetRereviewRevisionHistoryQuery();
        query.setCaseId(caseId);
        RevisionHistoryView view = getRereviewRevisionHistoryUseCase.execute(actor, query);
        List<SubmissionSummaryDto> dtos = view.getEntries().stream()
                .map(e -> new SubmissionSummaryDto(
                        e.getSubmissionId(), e.getStage(), e.getReviewerId(),
                        e.getStatus(), e.getRevisionNo(), e.getTotalScore(),
                        e.getComment(), e.getSubmittedAt(), e.getSupersededBySubmissionId()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(
                new RevisionHistoryResponse(view.getCaseId(), dtos, view.isHasExceptionEntry())));
    }
}
