# B4 — Re-review Operations & Closure Design Spec

## 1. Overview

本 spec 定義 B4 階段的設計，涵蓋：

- 初審提交後的自動閾值判斷
- 主管手動結案 / 手動送複審
- 複審員分派與重新分派
- 複審作業（開始、草稿、提交）
- 複審提交後自動結案與加權平均最終分數計算
- 複審修訂（append-only revision）
- 複審修訂後 finalScore 重算

**不包含：** 前端頁面（後端優先）

---

## 2. Decision Log Updates

B4 需更新以下 decision：

### 修改 D-012

- **Before:** Final score set by re-reviewer.
- **After:** Final score = weighted average of initial review score and re-review score. Default weight 50/50, configurable per ProjectVersion.

### 新增 D-017

Re-reviewer must not be the same person as the initial reviewer for the same case.

### 新增 D-018

Re-reviewer cannot see initial review answers or scores during re-review (independent evaluation).

### 新增 D-019

Threshold auto-evaluation: when initial score > quality threshold → auto PENDING_REREVIEW. When score <= threshold, case stays at INITIAL_REVIEW_COMPLETED; HOSPITAL_MANAGER may manually close or send to re-review.

### 新增 D-020

Scoring weight (initialReviewWeight, rereviewWeight) is stored in ProjectVersion, default 0.5/0.5. Invariant: initialReviewWeight + rereviewWeight = 1.0, both > 0 and <= 1.

---

## 3. Domain Model Extensions

### 3.1 ReviewCase Aggregate — New Fields

```yaml
ReviewCase:
  existing_fields:
    - id: ReviewCaseId
    - projectId: ReviewProjectId
    - status: CaseStatus
    - reviewerAssignment: ReviewerAssignment  # initial reviewer
    # ... (existing fields omitted)

  new_fields:
    - rereviewerAssignment: ReviewerAssignment  # nullable, set on assignRereviewer
    - initialReviewScore: BigDecimal            # set on completeInitialReview
    - finalScore: BigDecimal                    # set on close (manual or rereview)
```

### 3.2 ReviewCase Aggregate — Modified Methods

#### `completeInitialReview(score, qualityThreshold)` — MODIFIED

Current signature: `completeInitialReview(score)`

New behavior:
- Records `initialReviewScore = score`
- If `score > qualityThreshold` → status = `PENDING_REREVIEW`, raises `ReviewCaseEscalatedToRereviewEvent(reason=THRESHOLD_EXCEEDED)`
- If `score <= qualityThreshold` → status = `INITIAL_REVIEW_COMPLETED`
- Guard: status must be `INITIAL_REVIEW_IN_PROGRESS`

### 3.3 ReviewCase Aggregate — New Methods

#### `assignRereviewer(rereviewerId, dueAt)`

- Guard: status = `PENDING_REREVIEW`
- Guard: `rereviewerId ≠ reviewerAssignment.reviewerId` (D-017)
- Sets `rereviewerAssignment = new ReviewerAssignment(rereviewerId, dueAt)`
- Raises `RereviewerAssignedEvent`

#### `reassignRereviewer(rereviewerId, dueAt)`

- Guard: status = `PENDING_REREVIEW`
- Guard: `rereviewerId ≠ reviewerAssignment.reviewerId` (D-017)
- Replaces `rereviewerAssignment`
- Raises `RereviewerAssignedEvent`

#### `startRereview(actorId)`

- Guard: status = `PENDING_REREVIEW`
- Guard: `actorId = rereviewerAssignment.reviewerId`
- Status → `REREVIEW_IN_PROGRESS`
- Raises `RereviewStartedEvent`

#### `completeRereview(rereviewScore, initialWeight, rereviewWeight)`

- Guard: status = `REREVIEW_IN_PROGRESS`
- Calculates: `finalScore = initialReviewScore × initialWeight + rereviewScore × rereviewWeight`
- Status → `CLOSED`
- Raises `RereviewCompletedEvent`, `ReviewCaseClosedEvent`

#### `closeWithoutRereview()`

- Guard: status = `INITIAL_REVIEW_COMPLETED`
- Sets `finalScore = initialReviewScore`
- Status → `CLOSED`
- Raises `ReviewCaseClosedEvent(closedBy=MANUAL_CLOSE)`

#### `sendToRereview()`

- Guard: status = `INITIAL_REVIEW_COMPLETED`
- Status → `PENDING_REREVIEW`
- Raises `ReviewCaseEscalatedToRereviewEvent(reason=MANUAL)`

#### `updateFinalScoreOnRereviewRevision(newRereviewScore, initialWeight, rereviewWeight)`

- Guard: status not `CLOSED` and not `CANCELLED`
- Recalculates: `finalScore = initialReviewScore × initialWeight + newRereviewScore × rereviewWeight`
- Used by `ReviseRereviewUseCase` after revision creates new submission

### 3.4 ReviewSubmission — New Methods

#### `createRereviewDraft(caseId, rereviewerId, scoreSheetSnapshotId)`

- Creates a new `ReviewSubmission` with `stage = REREVIEW`, `status = DRAFT`
- Same structure as `createInitialDraft()` but with `stage = REREVIEW`

#### `createRereviewRevision(previousSubmission)`

- Same logic as `createRevision()` but validates `stage = REREVIEW`
- New submission inherits `stage = REREVIEW`

### 3.5 CaseStatus Enum — New Values

```java
PENDING_REREVIEW,          // awaiting re-review start
REREVIEW_IN_PROGRESS       // re-review in progress
```

These values are already defined in `state-machine.md` but not yet in code.

### 3.6 New Domain Invariants

1. **Rereviewer ≠ Initial Reviewer:** `rereviewerAssignment.reviewerId` must differ from `reviewerAssignment.reviewerId` on the same case.
2. **Scoring Weight Sum:** `initialReviewWeight + rereviewWeight == 1.0` (enforced in ProjectVersion).
3. **finalScore Calculation:** Always uses the configured weights from the active ProjectVersion.

---

## 4. State Machine Changes

### 4.1 New Triggers in Code

| From | Trigger | To | Guard |
|---|---|---|---|
| `INITIAL_REVIEW_COMPLETED` | `evaluateThreshold` (auto) | `PENDING_REREVIEW` | `score > qualityThreshold` |
| `INITIAL_REVIEW_COMPLETED` | `closeWithoutRereview` (manual) | `CLOSED` | actor = HOSPITAL_MANAGER |
| `INITIAL_REVIEW_COMPLETED` | `sendToRereview` (manual) | `PENDING_REREVIEW` | actor = HOSPITAL_MANAGER |
| `PENDING_REREVIEW` | `assignRereviewer` | `PENDING_REREVIEW` | state guard only |
| `PENDING_REREVIEW` | `startRereview` | `REREVIEW_IN_PROGRESS` | actorId = rereviewerId |
| `PENDING_REREVIEW` | `cancel` | `CANCELLED` | (already exists) |
| `REREVIEW_IN_PROGRESS` | `submitRereview` | `CLOSED` | submission = SUBMITTED |

### 4.2 State Machine Doc Updates

1. **New trigger `sendToRereview`:** INITIAL_REVIEW_COMPLETED → PENDING_REREVIEW (manual, HOSPITAL_MANAGER)
2. **Modified `closeWithoutRereview` guard:** Changed from `score <= qualityThreshold` to `HOSPITAL_MANAGER manual trigger` (auto threshold handled by evaluateThreshold in completeInitialReview)
3. **Design principle §2.9:** Updated from "最終分數由複審醫師決定" to "最終分數為初審與複審分數之加權平均"

---

## 5. ProjectVersion Extension — Scoring Weights

### 5.1 New Fields

```yaml
ProjectVersion:
  existing_fields:
    - versionNumber
    - samplingPolicy
    - qualityThreshold
    - scoreSheetBinding
    # ...
  new_fields:
    - initialReviewWeight: BigDecimal  # default 0.5
    - rereviewWeight: BigDecimal       # default 0.5
```

### 5.2 Invariant

```
initialReviewWeight + rereviewWeight == 1.0
initialReviewWeight > 0 && initialReviewWeight <= 1
rereviewWeight > 0 && rereviewWeight <= 1
```

Validated in `publishNewVersion()`.

### 5.3 Persistence

Add two columns to project version table:
- `initial_review_weight DECIMAL(3,2) DEFAULT 0.50`
- `rereview_weight DECIMAL(3,2) DEFAULT 0.50`

### 5.4 API Impact

- `CreateReviewProjectUseCase` — optional weights in draft (default 0.5/0.5)
- `UpdateReviewProjectDraftUseCase` — allow setting weights
- `GetReviewProjectDetailUseCase` — return weights in response
- Project API DTOs — add weight fields

---

## 6. Use Cases

### 6.1 CloseWithoutRereviewUseCase

```yaml
actor: HOSPITAL_MANAGER
input: caseId
steps:
  1. actor.requireRole(HOSPITAL_MANAGER)
  2. Load ReviewCase
  3. actor.requireHospitalScope(case.hospitalId)
  4. case.closeWithoutRereview()
  5. Save case
  6. Publish ReviewCaseClosedEvent
```

### 6.2 SendToRereviewUseCase

```yaml
actor: HOSPITAL_MANAGER
input: caseId
steps:
  1. actor.requireRole(HOSPITAL_MANAGER)
  2. Load ReviewCase
  3. actor.requireHospitalScope(case.hospitalId)
  4. case.sendToRereview()
  5. Save case
  6. Publish ReviewCaseEscalatedToRereviewEvent(reason=MANUAL)
```

### 6.3 AssignRereviewerUseCase

```yaml
actor: HOSPITAL_MANAGER
input: caseId, rereviewerId, dueAt
steps:
  1. actor.requireRole(HOSPITAL_MANAGER)
  2. Load ReviewCase
  3. actor.requireHospitalScope(case.hospitalId)
  4. case.assignRereviewer(rereviewerId, dueAt)
  5. Save case
  6. Publish RereviewerAssignedEvent
```

### 6.4 ReassignRereviewerUseCase

```yaml
actor: HOSPITAL_MANAGER
input: caseId, rereviewerId, dueAt
steps:
  1. actor.requireRole(HOSPITAL_MANAGER)
  2. Load ReviewCase
  3. actor.requireHospitalScope(case.hospitalId)
  4. case.reassignRereviewer(rereviewerId, dueAt)
  5. Save case
  6. Publish RereviewerAssignedEvent
```

### 6.5 StartRereviewUseCase

```yaml
actor: REREVIEWER (assigned re-reviewer)
input: caseId
steps:
  1. Load ReviewCase
  2. Verify actor.userId == case.rereviewerAssignment.reviewerId
  3. case.startRereview(actor.userId)
  4. Load ScoreSheetSnapshot for the case
  5. Create REREVIEW draft submission via ReviewSubmission.createRereviewDraft()
  6. Save case + submission
  7. Publish RereviewStartedEvent
```

### 6.6 SaveRereviewDraftUseCase

```yaml
actor: REREVIEWER (assigned re-reviewer)
input: caseId, answers[], comment
steps:
  1. Load ReviewCase
  2. Verify actor.userId == case.rereviewerAssignment.reviewerId
  3. Verify case.status == REREVIEW_IN_PROGRESS
  4. Load current REREVIEW draft submission
  5. Load ScoreSheetSnapshot
  6. Calculate scores via ScoreCalculationService
  7. submission.updateDraft(answers, calculatedScore, comment)
  8. Save submission
```

### 6.7 SubmitRereviewUseCase

```yaml
actor: REREVIEWER (assigned re-reviewer)
input: caseId, submissionId
steps:
  1. Load ReviewCase
  2. Verify actor.userId == case.rereviewerAssignment.reviewerId
  3. Load ReviewSubmission (verify stage=REREVIEW, status=DRAFT)
  4. submission.submit()
  5. Load ProjectVersion → get initialReviewWeight, rereviewWeight
  6. case.completeRereview(submission.totalScore, initialWeight, rereviewWeight)
  7. Save case + submission
  8. Publish RereviewCompletedEvent, ReviewCaseClosedEvent
```

### 6.8 ReviseRereviewUseCase

```yaml
actor: REREVIEWER (original re-reviewer)
input: caseId, submissionId, answers[], comment
steps:
  1. Load ReviewCase
  2. Verify case is not CLOSED and not CANCELLED
  3. Load current SUBMITTED REREVIEW submission
  4. Verify actor.userId == case.rereviewerAssignment.reviewerId
  5. Load ScoreSheetSnapshot
  6. Calculate scores via ScoreCalculationService
  7. previousSubmission.markSuperseded()
  8. Create new revision via createRereviewRevision()
  9. Load ProjectVersion → get weights
  10. case.updateFinalScoreOnRereviewRevision(newScore, initialWeight, rereviewWeight)
  11. Save case + old submission + new submission
  12. Publish RereviewSubmissionRevisedEvent
```

### 6.9 GetRereviewRevisionHistoryUseCase

```yaml
actor: multiple roles (per permission matrix)
input: caseId
steps:
  1. Load all REREVIEW stage submissions for caseId
  2. Return ordered by revisionNo
  3. Apply visibility rules per permission matrix
```

### 6.10 Modified: SubmitInitialReviewUseCase

```yaml
changes:
  - After submission.submit(), load ProjectVersion to get qualityThreshold
  - Call case.completeInitialReview(submission.totalScore, qualityThreshold) instead of case.completeInitialReview(submission.totalScore)
  - Domain decides state transition based on threshold comparison
```

---

## 7. Domain Events

| Event | Trigger | Key Payload |
|---|---|---|
| `RereviewerAssignedEvent` | assignRereviewer / reassignRereviewer | caseId, rereviewerId, dueAt |
| `RereviewStartedEvent` | startRereview | caseId, rereviewerId |
| `RereviewCompletedEvent` | submitRereview | caseId, submissionId, rereviewScore, finalScore |
| `ReviewCaseClosedEvent` | completeRereview / closeWithoutRereview | caseId, finalScore, closedBy (AUTO_REREVIEW / MANUAL_CLOSE) |
| `ReviewCaseEscalatedToRereviewEvent` | evaluateThreshold (auto) / sendToRereview (manual) | caseId, reason (THRESHOLD_EXCEEDED / MANUAL) |
| `RereviewSubmissionRevisedEvent` | reviseRereview | caseId, submissionId, revisionNo, newFinalScore |

---

## 8. API Endpoints

### 8.1 Case Control Endpoints

```
POST /review-cases/{caseId}/close-without-rereview
  → CloseWithoutRereviewUseCase
  → Request: (empty body)
  → Response: ApiResponse<CaseStatusResponse>
  → Errors: 409 (wrong state), 403 (not HOSPITAL_MANAGER)

POST /review-cases/{caseId}/send-to-rereview
  → SendToRereviewUseCase
  → Request: (empty body)
  → Response: ApiResponse<CaseStatusResponse>
  → Errors: 409 (wrong state), 403 (not HOSPITAL_MANAGER)
```

### 8.2 Rereviewer Assignment Endpoints

```
POST /review-cases/{caseId}/assign-rereviewer
  → AssignRereviewerUseCase
  → Request: { rereviewerId, dueAt }
  → Response: ApiResponse<CaseStatusResponse>
  → Errors: 409 (wrong state), 422 (rereviewer == reviewer), 403

PUT /review-cases/{caseId}/reassign-rereviewer
  → ReassignRereviewerUseCase
  → Request: { rereviewerId, dueAt }
  → Response: ApiResponse<CaseStatusResponse>
  → Errors: 409 (wrong state), 422 (rereviewer == reviewer), 403
```

### 8.3 Rereview Operation Endpoints

```
POST /review-cases/{caseId}/rereview/start
  → StartRereviewUseCase
  → Request: (empty body)
  → Response: ApiResponse<RereviewStartResponse> (includes submissionId)
  → Errors: 409 (wrong state), 403 (not assigned rereviewer)

PUT /review-cases/{caseId}/rereview/draft
  → SaveRereviewDraftUseCase
  → Request: { answers: [{ itemId, answerValue }], comment }
  → Response: ApiResponse<RereviewDraftResponse> (includes calculated scores)
  → Errors: 409 (wrong state), 403

POST /review-cases/{caseId}/rereview/submit
  → SubmitRereviewUseCase
  → Request: { submissionId }
  → Response: ApiResponse<RereviewSubmitResponse> (includes finalScore)
  → Errors: 409 (wrong state), 403
```

### 8.4 Rereview Revision Endpoints

```
POST /review-cases/{caseId}/rereview/revise
  → ReviseRereviewUseCase
  → Request: { submissionId, answers: [{ itemId, answerValue }], comment }
  → Response: ApiResponse<RevisionResponse>
  → Errors: 409 (CLOSED/CANCELLED), 403 (not original rereviewer), 422

GET /review-cases/{caseId}/rereview/revision-history
  → GetRereviewRevisionHistoryUseCase
  → Response: ApiResponse<List<RevisionHistoryItem>>
```

---

## 9. Existing Code Modification Summary

### 9.1 Domain Layer (Existing Files)

| File | Change |
|---|---|
| `CaseStatus.java` | Add `PENDING_REREVIEW`, `REREVIEW_IN_PROGRESS` |
| `ReviewCase.java` | Add fields: rereviewerAssignment, initialReviewScore, finalScore. Add 6 new methods. Modify `completeInitialReview()` signature. |
| `ReviewSubmission.java` | Add `createRereviewDraft()`, `createRereviewRevision()` |

### 9.2 Application Layer (Existing Files)

| File | Change |
|---|---|
| `SubmitInitialReviewUseCase.java` | Load ProjectVersion, pass qualityThreshold to completeInitialReview() |

### 9.3 Infrastructure Layer (Existing Files)

| File | Change |
|---|---|
| `ReviewCaseEntity.java` | Add JPA columns: rereviewer_id, rereviewer_due_at, initial_review_score, final_score |
| `ReviewProjectEntity.java` (or version sub-entity) | Add JPA columns: initial_review_weight, rereview_weight |

### 9.4 Interfaces Layer (Existing Files)

| File | Change |
|---|---|
| `ReviewCaseController.java` | Add 9 new endpoints |

### 9.5 Test Files (Existing)

| File | Change |
|---|---|
| `SubmitInitialReviewUseCaseTest.java` | Update tests to pass qualityThreshold |
| Various ReviewCase domain tests | Update completeInitialReview() calls |

---

## 10. New Files Summary

### Domain (~6 files)

- `domain/reviewcase/event/RereviewerAssignedEvent.java`
- `domain/reviewcase/event/RereviewStartedEvent.java`
- `domain/reviewcase/event/RereviewCompletedEvent.java`
- `domain/reviewcase/event/ReviewCaseClosedEvent.java`
- `domain/reviewcase/event/ReviewCaseEscalatedToRereviewEvent.java`
- `domain/reviewcase/event/RereviewSubmissionRevisedEvent.java`

### Application (~9 files)

- `application/reviewcase/CloseWithoutRereviewUseCase.java`
- `application/reviewcase/SendToRereviewUseCase.java`
- `application/reviewcase/AssignRereviewerUseCase.java`
- `application/reviewcase/ReassignRereviewerUseCase.java`
- `application/reviewcase/StartRereviewUseCase.java`
- `application/reviewcase/SaveRereviewDraftUseCase.java`
- `application/reviewcase/SubmitRereviewUseCase.java`
- `application/reviewcase/ReviseRereviewUseCase.java`
- `application/reviewcase/GetRereviewRevisionHistoryUseCase.java`

### Interfaces DTOs (~4-6 files)

- Request/Response DTOs for rereview operations

### Tests (~12 files)

- Domain tests for new ReviewCase methods
- Use case tests for each of the 9 use cases
- API integration tests for new endpoints

---

## 11. Testing Strategy

### 11.1 Domain Tests

- Each new ReviewCase method: happy path + guard violation
  - `assignRereviewer`: valid + same-as-reviewer guard + wrong state
  - `startRereview`: valid + wrong actor + wrong state
  - `completeRereview`: valid + finalScore calculation + wrong state
  - `closeWithoutRereview`: valid + wrong state
  - `sendToRereview`: valid + wrong state
  - `completeInitialReview` (modified): threshold auto-evaluation (above, at, below threshold)

### 11.2 Application Tests

- Each use case: ~5-10 tests using InMemory test doubles
- Focus: actor guard, domain invariant delegation, score calculation, revision append-only

### 11.3 API Integration Tests

- Each endpoint: happy path + common errors
- Using TestTokenFactory pattern
- Focus: HTTP status codes, response body, error envelope

### 11.4 Key Edge Cases

1. Rereviewer == initial reviewer → 422 DomainRuleViolationException
2. Case already CLOSED → revision attempt → 409 DomainStateConflictException
3. Weighted average calculation precision (BigDecimal HALF_UP rounding)
4. Rereview revision → finalScore recalculation correctness
5. Threshold boundary: score == threshold → stays INITIAL_REVIEW_COMPLETED (not escalated)
6. Manual sendToRereview on case already at INITIAL_REVIEW_COMPLETED with score <= threshold → allowed

---

## 12. Authority Document Updates Required

| Document | Update |
|---|---|
| `decision-log.md` | Modify D-012, add D-017 through D-020 |
| `state-machine.md` | Add `sendToRereview` trigger, modify `closeWithoutRereview` guard, update principle §2.9 |
| `domain-model.md` | Add rereviewerAssignment, initialReviewScore, finalScore to ReviewCase; add scoring weight to ProjectVersion |
| `domain-events.md` | Add 6 new events |
| `api-spec.md` | Add 9 new endpoints |
| `persistence-model.md` | Add new columns to review_case and project_version tables |
| `application-service.md` | Update SubmitInitialReview description; confirm use case list matches |

---

## 13. Intentional Deviations from Current Docs

| # | Current Doc Statement | B4 Design | Reason |
|---|---|---|---|
| 1 | D-012: "Final score set by re-reviewer" | Final score = weighted average | User requirement; will update D-012 |
| 2 | state-machine §2.9: "最終分數由複審醫師決定" | 最終分數為加權平均 | Aligned with D-012 update |
| 3 | application-service §8.1: "最終分數由複審醫師提交結果決定" | 最終分數為加權平均 | Aligned with D-012 update |
| 4 | state-machine: `closeWithoutRereview` guard = `score <= threshold` | Manual HOSPITAL_MANAGER trigger | Auto threshold handled in `completeInitialReview()` |
| 5 | No `sendToRereview` trigger in state-machine | New trigger added | User requirement for manual override |
