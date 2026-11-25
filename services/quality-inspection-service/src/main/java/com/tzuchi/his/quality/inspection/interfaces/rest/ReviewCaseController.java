package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.tzuchi.his.quality.inspection.application.audit.AuditTrailEntry;
import com.tzuchi.his.quality.inspection.application.audit.GetCaseLifecycleUseCase;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.Page;
import com.tzuchi.his.quality.inspection.application.reviewcase.*;
import com.tzuchi.his.quality.inspection.application.reviewcase.dto.AnswerInput;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.AssignmentBootstrapView;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.AssignmentCandidateRole;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.AssignmentCandidateView;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.ReviewCaseSummary;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.ReviewCaseDetailView;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.RevisionHistoryView;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.interfaces.rest.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.Instant;
import java.time.OffsetDateTime;
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
    private final CloseRereviewUseCase closeRereviewUseCase;
    private final FixInitialReviewUseCase fixInitialReviewUseCase;
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
                                 CloseRereviewUseCase closeRereviewUseCase,
                                 ListReviewCasesUseCase listReviewCasesUseCase,
                                 GetReviewCaseDetailUseCase getReviewCaseDetailUseCase,
                                 GetCaseAssignmentContextUseCase getCaseAssignmentContextUseCase,
                                 ListCaseAssignmentCandidatesUseCase listCaseAssignmentCandidatesUseCase,
                                 GetCaseLifecycleUseCase getCaseLifecycleUseCase,
                                 FixInitialReviewUseCase fixInitialReviewUseCase) {
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
        this.closeRereviewUseCase = closeRereviewUseCase;
        this.fixInitialReviewUseCase = fixInitialReviewUseCase;
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
        Page<ReviewCaseSummary> result = listReviewCasesUseCase.execute(actor,
                hospitalId, projectId, batchId, caseStatus,
                reviewerId, rereviewerId, medicalRecordType, page, size);
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
        List<AuditTrailEntry> entries = getCaseLifecycleUseCase.execute(actor, caseId).getEntries();
        return ResponseEntity.ok(ApiResponse.ok(entries));
    }

    @PostMapping("/{caseId}/assign-reviewer")
    public ResponseEntity<ApiResponse<Void>> assignReviewer(
            @CurrentActor ActorContext actor,
            @PathVariable String caseId,
            @Valid @RequestBody AssignReviewerRequest request) {
        assignUseCase.execute(actor, caseId, request.getReviewerId(), toInstant(request.getDueAt()));
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{caseId}/reassign-reviewer")
    public ResponseEntity<ApiResponse<Void>> reassignReviewer(
            @CurrentActor ActorContext actor,
            @PathVariable String caseId,
            @Valid @RequestBody ReassignReviewerRequest request) {
        reassignUseCase.execute(actor, caseId, request.getReviewerId(), toInstant(request.getDueAt()));
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
        String submissionId = startInitialReviewUseCase.execute(actor, caseId).getSubmissionId();
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
                result.getTotalScore())));
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
                new RevisionHistoryResponse(view.getCaseId(), dtos)));
    }

    // --- B4 Rereview Endpoints ---

    @PostMapping("/{caseId}/close-without-rereview")
    public ResponseEntity<ApiResponse<Void>> closeWithoutRereview(
            @CurrentActor ActorContext actor,
            @PathVariable String caseId) {
        closeWithoutRereviewUseCase.execute(actor, caseId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{caseId}/send-to-rereview")
    public ResponseEntity<ApiResponse<Void>> sendToRereview(
            @CurrentActor ActorContext actor,
            @PathVariable String caseId) {
        sendToRereviewUseCase.execute(actor, caseId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{caseId}/assign-rereviewer")
    public ResponseEntity<ApiResponse<Void>> assignRereviewer(
            @CurrentActor ActorContext actor,
            @PathVariable String caseId,
            @RequestBody @Valid AssignRereviewerRequest request) {
        assignRereviewerUseCase.execute(actor, caseId, request.getRereviewerId(), toInstant(request.getDueAt()));
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{caseId}/reassign-rereviewer")
    public ResponseEntity<ApiResponse<Void>> reassignRereviewer(
            @CurrentActor ActorContext actor,
            @PathVariable String caseId,
            @RequestBody @Valid ReassignRereviewerRequest request) {
        reassignRereviewerUseCase.execute(actor, caseId, request.getRereviewerId(), toInstant(request.getDueAt()));
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{caseId}/start-rereview")
    public ResponseEntity<ApiResponse<Void>> startRereview(
            @CurrentActor ActorContext actor,
            @PathVariable String caseId) {
        startRereviewUseCase.execute(actor, caseId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{caseId}/rereview/draft")
    public ResponseEntity<ApiResponse<SaveDraftResponse>> saveRereviewDraft(
            @CurrentActor ActorContext actor,
            @PathVariable String caseId,
            @RequestBody @Valid SaveRereviewDraftRequest request) {
        // D-069 第 7 點：複審草稿的分數不是輸入；請求裡的 totalScore 不再使用
        SaveDraftResult result = saveRereviewDraftUseCase.execute(actor, caseId, request.getSubmissionId(),
                toAnswerInputs(request.getAnswers()), request.getComment());
        return ResponseEntity.ok(ApiResponse.ok(new SaveDraftResponse(
                result.getSubmissionId(), result.getStatus(),
                result.getStage(), result.getTotalScore())));
    }

    @PostMapping("/{caseId}/rereview/submit")
    public ResponseEntity<ApiResponse<SubmitRereviewResponse>> submitRereview(
            @CurrentActor ActorContext actor,
            @PathVariable String caseId,
            @RequestBody @Valid SubmitRereviewRequest request) {
        SubmitRereviewResult result = submitRereviewUseCase.execute(actor, caseId, request.getSubmissionId(),
                toAnswerInputs(request.getAnswers()), request.getComment());
        return ResponseEntity.ok(ApiResponse.ok(new SubmitRereviewResponse(
                result.getSubmissionId(), result.getStatus(),
                result.getCaseId(), result.getCaseStatus())));
    }

    /**
     * 初審固定（D-031、DD-03、D-054）：不需要請求內容，路徑上的 submissionId 是要固定的有效初審結果。
     * 回應 { caseId, caseStatus, submissionId, fixedAt }；caseStatus 為 PENDING_REREVIEW 或 INITIAL_REVIEW_COMPLETED。
     */
    @PostMapping("/{caseId}/initial-review/submissions/{submissionId}/fix")
    public ResponseEntity<ApiResponse<FixInitialReviewResponse>> fixInitialReview(
            @CurrentActor ActorContext actor,
            @PathVariable String caseId,
            @PathVariable String submissionId) {
        FixInitialReviewResult result = fixInitialReviewUseCase.execute(actor, caseId, submissionId);
        return ResponseEntity.ok(ApiResponse.ok(new FixInitialReviewResponse(
                result.getCaseId(), result.getCaseStatus(), result.getSubmissionId(), result.getFixedAt().toString())));
    }

    /** 複審的獨立結案（D-034、D-053）：不需要請求內容，路徑上的 submissionId 是要結案的有效複審結果。 */
    @PostMapping("/{caseId}/rereview/submissions/{submissionId}/close")
    public ResponseEntity<ApiResponse<CloseRereviewResponse>> closeRereview(
            @CurrentActor ActorContext actor,
            @PathVariable String caseId,
            @PathVariable String submissionId) {
        CloseRereviewResult result = closeRereviewUseCase.execute(actor, caseId, submissionId);
        return ResponseEntity.ok(ApiResponse.ok(new CloseRereviewResponse(
                result.getCaseId(), result.getCaseStatus(), result.getSubmissionId())));
    }

    @PostMapping("/{caseId}/rereview/submissions/{submissionId}/revise")
    public ResponseEntity<ApiResponse<RevisionResultResponse>> reviseRereview(
            @CurrentActor ActorContext actor,
            @PathVariable String caseId,
            @PathVariable String submissionId,
            @RequestBody @Valid ReviseRereviewRequest request) {
        // D-083 第 6 點：修正複審改收參數，answers 與修正初審同型別
        RevisionResult result = reviseRereviewUseCase.execute(
                actor, caseId, submissionId, toAnswerInputs(request.getAnswers()), request.getComment());
        return ResponseEntity.ok(ApiResponse.ok(new RevisionResultResponse(
                result.getNewSubmissionId(), result.getOldSubmissionId(),
                result.getRevisionNo(), result.getTotalScore(),
                result.getStatus(), result.getStage(), result.getCaseId())));
    }

    private static List<AnswerInput> toAnswerInputs(List<SaveInitialReviewDraftRequest.AnswerInputDto> answers) {
        if (answers == null) {
            return null;
        }
        return answers.stream()
                .filter(Objects::nonNull)
                .map(a -> new AnswerInput(a.getItemId(), a.getAnswerValue()))
                .collect(Collectors.toList());
    }

    /** D-073、D-075 第 1 點：請求的 dueAt 帶時區，直接換成時間點交給 use case。 */
    private static Instant toInstant(OffsetDateTime dueAt) {
        return dueAt == null ? null : dueAt.toInstant();
    }
}
