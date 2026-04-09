# B3c — Initial Review Revision Workflow Design Spec

## 1. Scope

本次目標：實作初審 revision 流程 — ReviseInitialReview command 和 GetRevisionHistory query，以及相關的 ReviewSubmission domain methods、ReviewCase guard、server-side 計分、JPA persistence 擴展和 REST endpoints。

### 涵蓋範圍

- ReviewSubmission 新增 domain methods：`createRevision()`（static factory）、`markSuperseded()`
- ReviewCase 新增 guard method：`requireNotTerminal()`
- ReviewCase 新增 method：`updateFinalScore(newScore)`（revision 後更新 finalScore）
- `ReviseInitialReviewUseCase` — command use case
- `GetRevisionHistoryUseCase` — query use case
- `ReviewSubmissionRevised` domain event
- ReviewSubmissionRepository 新增 `findAllByCaseId(caseId)`
- JPA repository impl 擴展
- 2 個 REST endpoints + request/response DTOs
- Domain / Use Case / REST / Integration 四層測試

### 不在範圍

- 複審 revision（`ReviseRereviewUseCase`）→ B4 或更後
- `GetCaseLifecycleUseCase` → 需 audit trail 基礎設施，推遲
- 品質線重判（revision 後 re-evaluate threshold）→ B4
- 附件上傳實作 → 只預留欄位
- 複審作業（assign rereviewer / start rereview / submit rereview）→ B4

### 前置依賴

| Phase | 狀態 | 提供 |
|-------|------|------|
| B3a | ✅ 完成 | ReviewCase aggregate (assignment lifecycle), ReviewerAssignment, 4 events, JPA entity |
| B3b | ✅ 完成 | ReviewSubmission aggregate (createInitialDraft, updateDraft, submit), ScoreCalculationService, AnswerItem, JPA entity, 3 events, REST endpoints |

---

## 2. ReviewSubmission Domain Methods（B3c 新增）

### 2.1 createRevision(oldSubmission, scoredAnswers, totalScore, comment)

Static factory method。從已提交的 submission 建立新 revision。

```java
public static ReviewSubmission createRevision(
        String newSubmissionId,
        ReviewSubmission oldSubmission,
        List<AnswerItem> scoredAnswers,
        BigDecimal totalScore,
        String comment) {
    // Guard: old submission must be SUBMITTED
    if (oldSubmission.getStatus() != SubmissionStatus.SUBMITTED) {
        throw new DomainStateConflictException("SUBMISSION_NOT_SUBMITTED",
                "Can only revise a SUBMITTED submission. Current: " + oldSubmission.getStatus());
    }

    ReviewSubmission rev = new ReviewSubmission();
    rev.submissionId = newSubmissionId;
    rev.caseId = oldSubmission.getCaseId();
    rev.stage = oldSubmission.getStage();          // inherit stage (INITIAL)
    rev.reviewerId = oldSubmission.getReviewerId(); // same reviewer
    rev.status = SubmissionStatus.SUBMITTED;        // direct submit, no DRAFT phase
    rev.revisionNo = oldSubmission.getRevisionNo() + 1;
    rev.answerSet = new ArrayList<>(scoredAnswers);
    rev.totalScore = totalScore;
    rev.comment = comment;
    rev.submittedAt = LocalDateTime.now();
    rev.basedOnScoreSheetVersionId = oldSubmission.getBasedOnScoreSheetVersionId();
    rev.createdAt = LocalDateTime.now();
    rev.updatedAt = rev.createdAt;
    return rev;
}
```

**設計決策：**
- 新 revision 直接為 `SUBMITTED` 狀態，不經 DRAFT 階段
- `revisionNo` 自動遞增
- 繼承 `caseId`、`stage`、`reviewerId`、`basedOnScoreSheetVersionId` from old submission
- `scoredAnswers` 和 `totalScore` 由 ScoreCalculationService 計算後傳入

### 2.2 markSuperseded(newSubmissionId)

標記舊 submission 為 SUPERSEDED。

```java
public void markSuperseded(String newSubmissionId) {
    if (this.status != SubmissionStatus.SUBMITTED) {
        throw new DomainStateConflictException("SUBMISSION_NOT_SUBMITTED",
                "Can only supersede a SUBMITTED submission. Current: " + this.status);
    }
    Objects.requireNonNull(newSubmissionId, "newSubmissionId must not be null");
    this.supersededBySubmissionId = newSubmissionId;
    this.status = SubmissionStatus.SUPERSEDED;
    this.updatedAt = LocalDateTime.now();
}
```

---

## 3. ReviewCase Domain Methods（B3c 新增）

### 3.1 requireNotTerminal()

Guard method，驗證案件非 terminal state。

```java
public void requireNotTerminal() {
    if (this.status == CaseStatus.CLOSED || this.status == CaseStatus.CANCELLED) {
        throw new DomainStateConflictException("CASE_TERMINAL",
                "Case is in terminal status: " + this.status + ". Revision not allowed.");
    }
}
```

### 3.2 updateFinalScore(newScore)

Revision 後更新 finalScore。不改變案件主狀態。

```java
public void updateFinalScore(BigDecimal newScore) {
    requireNotTerminal();
    Objects.requireNonNull(newScore, "newScore must not be null");
    this.finalScore = newScore;
}
```

---

## 4. Domain Event

### 4.1 ReviewSubmissionRevisedEvent

```java
package com.tzuchi.his.quality.inspection.domain.reviewcase.submission.event;

public class ReviewSubmissionRevisedEvent {
    private final String oldSubmissionId;
    private final String newSubmissionId;
    private final String caseId;
    private final String stage;     // "INITIAL"
    private final int revisionNo;   // new revision number
    private final String triggeredBy; // reviewer ID

    // constructor + getters
}
```

對應 domain-events.md §8.1 `ReviewSubmissionRevised`。

---

## 5. ReviseInitialReviewUseCase

### 5.1 Orchestration Flow

```
1. actor.requireRole("REVIEWER")
2. Load ReviewCase by caseId
3. reviewCase.requireNotTerminal()
4. Load old ReviewSubmission by submissionId
5. Validate: oldSubmission.getCaseId() == caseId
6. Validate: oldSubmission.getStage() == INITIAL
7. Validate: oldSubmission.getStatus() == SUBMITTED (domain guard in createRevision)
8. Validate: oldSubmission.getReviewerId() == actor.getUserId()
9. Load ScoreSheetDefinition by oldSubmission.getBasedOnScoreSheetVersionId()
10. Convert AnswerInput[] → raw AnswerItem[]
11. scoreCalculationService.calculate(rawAnswers, scoreSheet) → ScoreResult
12. ReviewSubmission.createRevision(newId, oldSubmission, scoredAnswers, totalScore, comment)
13. oldSubmission.markSuperseded(newSubmissionId)
14. reviewCase.updateFinalScore(totalScore)
15. Save old submission (status updated)
16. Save new submission
17. Save reviewCase (finalScore updated)
18. Publish ReviewSubmissionRevisedEvent
```

### 5.2 Input

```java
public class ReviseInitialReviewCommand {
    private String caseId;
    private String submissionId;
    private List<AnswerInput> answers;
    private String comment;
}
```

### 5.3 Output

```java
public class RevisionResult {
    private String newSubmissionId;
    private int revisionNo;
    private BigDecimal totalScore;
    private String status; // "SUBMITTED"
}
```

### 5.4 Error Cases

| Condition | Exception | HTTP |
|-----------|-----------|------|
| Actor not REVIEWER | UnauthorizedException | 403 |
| Case CLOSED / CANCELLED | DomainStateConflictException | 409 |
| Submission not found | ResourceNotFoundException | 404 |
| Case not found | ResourceNotFoundException | 404 |
| Submission caseId ≠ path caseId | BadRequestException | 400 |
| Submission stage ≠ INITIAL | DomainRuleViolationException | 422 |
| Submission status ≠ SUBMITTED | DomainStateConflictException | 409 |
| Actor ≠ submission reviewerId | UnauthorizedException | 403 |
| Score sheet not found | ResourceNotFoundException | 404 |

---

## 6. GetRevisionHistoryUseCase

### 6.1 Orchestration Flow

```
1. actor.requireAnyRole("REVIEWER", "HOSPITAL_MANAGER", "REREVIEWER", "AUDITOR")
2. Load all submissions by caseId via findAllByCaseId(caseId)
3. Sort by stage, then by revisionNo ASC
4. Map to response DTOs
```

### 6.2 Input

```java
String caseId
```

### 6.3 Output

```java
public class RevisionHistoryResponse {
    private String caseId;
    private List<SubmissionSummary> submissions;
}

public class SubmissionSummary {
    private String submissionId;
    private String stage;
    private String reviewerId;
    private String status;
    private int revisionNo;
    private BigDecimal totalScore;
    private String comment;
    private LocalDateTime submittedAt;
    private String supersededBySubmissionId;
}
```

### 6.4 Permissions

根據 permission-matrix.md：
- `HOSPITAL_MANAGER`：可查看本院案件的完整歷程
- `REVIEWER`：可查看自己被指派案件的歷程
- `AUDITOR`：授權範圍內可查看
- `SYSTEM_ADMIN`：metadata only（不含完整作答內容）

本 phase 先實作基本角色驗證（requireAnyRole），精細的 scope 驗證留給後續整合。

---

## 7. Repository 擴展

### 7.1 ReviewSubmissionRepository 新增

```java
List<ReviewSubmission> findAllByCaseId(String caseId);
```

- 不指定排序，由 use case 層處理
- InMemoryReviewSubmissionRepository test fixture 同步新增此 method

### 7.2 JPA Implementation

在既有 JpaReviewSubmissionRepository 實作中新增對應 JPA query method。

---

## 8. REST Endpoints

### 8.1 Revise Initial Review

```
POST /api/quality-inspection/cases/{caseId}/submissions/{submissionId}/revise
```

**Request Body:**

```json
{
  "answers": [
    { "itemId": "ITEM-001", "answerValue": "true" },
    { "itemId": "ITEM-002", "answerValue": "85" }
  ],
  "comment": "修正評分：重新檢視後調整第二題分數"
}
```

**Response (200 OK):**

```json
{
  "success": true,
  "data": {
    "newSubmissionId": "SUB-INIT-002",
    "revisionNo": 2,
    "totalScore": 88.5,
    "status": "SUBMITTED"
  }
}
```

Wrapped in `ApiResponse.ok(data)` per existing convention.

### 8.2 Get Revision History

```
GET /api/quality-inspection/cases/{caseId}/revision-history
```

**Response (200 OK):**

```json
{
  "success": true,
  "data": {
    "caseId": "RC-001",
    "submissions": [
      {
        "submissionId": "SUB-INIT-001",
        "stage": "INITIAL",
        "reviewerId": "DOC-123",
        "status": "SUPERSEDED",
        "revisionNo": 1,
        "totalScore": 75.0,
        "comment": "initial review",
        "submittedAt": "2026-04-02T14:10:00",
        "supersededBySubmissionId": "SUB-INIT-002"
      },
      {
        "submissionId": "SUB-INIT-002",
        "stage": "INITIAL",
        "reviewerId": "DOC-123",
        "status": "SUBMITTED",
        "revisionNo": 2,
        "totalScore": 88.5,
        "comment": "修正評分",
        "submittedAt": "2026-04-02T15:00:00",
        "supersededBySubmissionId": null
      }
    ]
  }
}
```

不分頁（一個案件的 revision 筆數通常很少）。

---

## 9. Testing Strategy

### 9.1 Domain Tests

**ReviewSubmission revision tests:**
- `createRevision_fromSubmittedSubmission_succeeds` — happy path
- `createRevision_fromDraftSubmission_throwsConflict` — DRAFT → rejected
- `createRevision_fromSupersededSubmission_throwsConflict` — SUPERSEDED → rejected
- `createRevision_inheritsFieldsFromOldSubmission` — caseId, stage, reviewerId, scoreSheetVersionId preserved
- `createRevision_incrementsRevisionNo` — revisionNo = old + 1
- `createRevision_statusIsSubmitted` — new submission is directly SUBMITTED
- `markSuperseded_fromSubmitted_succeeds` — sets SUPERSEDED + supersededBySubmissionId
- `markSuperseded_fromDraft_throwsConflict` — DRAFT → rejected

**ReviewCase guard tests:**
- `requireNotTerminal_whenClosed_throws`
- `requireNotTerminal_whenCancelled_throws`
- `requireNotTerminal_whenActive_passes`
- `updateFinalScore_updatesScore`
- `updateFinalScore_whenTerminal_throws`

### 9.2 Use Case Tests

**ReviseInitialReviewUseCase:**
- `revise_happyPath_createsNewSubmission` — new submission saved, old SUPERSEDED, finalScore updated, event published
- `revise_actorNotReviewer_throwsUnauthorized`
- `revise_actorNotSubmissionOwner_throwsUnauthorized`
- `revise_caseClosed_throwsConflict`
- `revise_caseCancelled_throwsConflict`
- `revise_submissionNotFound_throwsNotFound`
- `revise_caseNotFound_throwsNotFound`
- `revise_submissionCaseIdMismatch_throwsBadRequest`
- `revise_submissionNotInitialStage_throwsRuleViolation`
- `revise_submissionIsDraft_throwsConflict`

**GetRevisionHistoryUseCase:**
- `getHistory_returnsAllSubmissions_sortedByRevisionNo`
- `getHistory_emptyCase_returnsEmptyList`

### 9.3 REST / Integration Tests

**REST (MockMvc):**
- `revise_returns200_withRevisionResult`
- `revise_unauthorized_returns403`
- `getRevisionHistory_returns200_withSubmissions`

**Integration (@SpringBootTest + H2):**
- Full round-trip: create case → start review → submit → revise → verify old SUPERSEDED + new SUBMITTED in DB
- Revision history query returns correct data

---

## 10. Intentional Deviations from Authority

| # | Authority Says | This Spec Does | Reason |
|---|---------------|----------------|--------|
| 1 | application-service §9 lists `ReviseRereviewUseCase` | Not implemented | Out of scope — rereview not yet available |
| 2 | application-service §9 lists `GetCaseLifecycleUseCase` | Not implemented | Needs audit trail infrastructure, deferred |
| 3 | state-machine §3.5.2 says "INITIAL stage revision 僅由原審查醫師發起" | Verified against submission.reviewerId (not case assignment) | A reassigned case's old submission belongs to the original reviewer |
| 4 | domain-events §8.1 shows oldSubmissionId + newSubmissionId | Event includes both IDs + caseId + stage + revisionNo + triggeredBy | Full traceability |
| 5 | Revision 不改變案件主狀態 (state-machine §3.5.1) | updateFinalScore changes finalScore but NOT case status | Score update is data, not state transition |

---

## 11. File Change Summary

| File | Action | Layer |
|------|--------|-------|
| `ReviewSubmission.java` | Add `createRevision()` + `markSuperseded()` | domain |
| `ReviewCase.java` | Add `requireNotTerminal()` + `updateFinalScore()` | domain |
| `ReviewSubmissionRevisedEvent.java` | New | domain/event |
| `ReviewSubmissionRepository.java` | Add `findAllByCaseId()` | domain/port |
| `ReviseInitialReviewUseCase.java` | New | application |
| `GetRevisionHistoryUseCase.java` | New | application |
| `RevisionResult.java` | New DTO | application |
| `InMemoryReviewSubmissionRepository.java` | Add `findAllByCaseId()` | test fixture |
| `JpaReviewSubmissionRepository` / `ReviewSubmissionEntity` | Extend query | infrastructure |
| `ReviewCaseController.java` | Add 2 endpoints | interfaces |
| REST DTOs (request/response) | New | interfaces/dto |
| Tests (domain, use case, REST, integration) | New | test |
