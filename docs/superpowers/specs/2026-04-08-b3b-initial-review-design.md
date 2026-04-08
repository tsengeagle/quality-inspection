# B3b — Initial Review Operations Design Spec

## 1. Scope

本次目標：實作初審作業流程 — StartInitialReview、SaveInitialReviewDraft、SubmitInitialReview，以及相關的 ReviewSubmission aggregate business methods、server-side 計分引擎、JPA persistence 和 REST endpoints。

### 涵蓋範圍

- ReviewCase 新增 domain methods：`startInitialReview()`、`completeInitialReview()`
- ReviewSubmission aggregate 的完整 business methods（createDraft, updateDraft, submit）
- ScoreCalculationService — server-side 計分引擎
- AnswerItem value object
- ReviewSubmission JPA entity + repository impl
- 3 個 application use cases
- 3 個 REST endpoints + request/response DTOs
- 3 個 domain events
- API integration tests + unit tests

### 不在範圍

- 品質線判定（evaluateThreshold / closeWithoutRereview）→ B3b-2 或 B4
- Revision workflow → B3c
- 附件上傳實作 → 只預留欄位
- 複審作業 → B4
- ScoreSheetDefinition 的 `snapshotForSubmission()` method → 使用已有的 published version 查詢

### 前置依賴

| Phase | 狀態 | 提供 |
|-------|------|------|
| B3a | ✅ 完成 | ReviewCase aggregate (5 methods), ReviewSubmission shell, 4 events, JPA entity |

---

## 2. ReviewCase Domain Methods（B3b 新增）

在現有 ReviewCase aggregate 上新增 2 個 methods。

### 2.1 startInitialReview(submissionId, scoreSheetVersionRef)

```java
public void startInitialReview(String submissionId, String scoreSheetVersionRef) {
    if (this.status != CaseStatus.PENDING_INITIAL_REVIEW) {
        throw new DomainStateConflictException("CASE_NOT_PENDING_INITIAL_REVIEW",
                "Case must be in PENDING_INITIAL_REVIEW to start. Current: " + this.status);
    }
    Objects.requireNonNull(submissionId);
    Objects.requireNonNull(scoreSheetVersionRef);
    this.latestInitialSubmissionId = submissionId;
    this.scoreSheetVersionRef = scoreSheetVersionRef;
    this.status = CaseStatus.INITIAL_REVIEW_IN_PROGRESS;
}
```

- Guard: `status == PENDING_INITIAL_REVIEW`
- 效果: → `INITIAL_REVIEW_IN_PROGRESS`，設定 `latestInitialSubmissionId` 和 `scoreSheetVersionRef`

### 2.2 completeInitialReview(totalScore)

```java
public void completeInitialReview(BigDecimal totalScore) {
    if (this.status != CaseStatus.INITIAL_REVIEW_IN_PROGRESS) {
        throw new DomainStateConflictException("CASE_NOT_IN_INITIAL_REVIEW",
                "Case must be in INITIAL_REVIEW_IN_PROGRESS to complete. Current: " + this.status);
    }
    Objects.requireNonNull(totalScore);
    this.finalScore = totalScore;
    this.status = CaseStatus.INITIAL_REVIEW_COMPLETED;
}
```

- Guard: `status == INITIAL_REVIEW_IN_PROGRESS`
- 效果: → `INITIAL_REVIEW_COMPLETED`，設定 finalScore
- `latestInitialSubmissionId` 已在 `startInitialReview()` 時設定，不重複設定
- 注意：不做品質線判定，留給後續 phase

---

## 3. AnswerItem Value Object

```java
package com.tzuchi.his.quality.inspection.domain.reviewcase.submission;

public class AnswerItem {
    private String itemId;
    private String answerValue;   // 原始答案（序列化為 string）
    private BigDecimal derivedScore;  // server-side 計算後的分數

    // factory: 前端傳入（無 derivedScore）
    public static AnswerItem ofRaw(String itemId, String answerValue);

    // factory: server 計算後
    public static AnswerItem ofScored(String itemId, String answerValue, BigDecimal derivedScore);

    // reconstitute from persistence
    public static AnswerItem reconstitute(String itemId, String answerValue, BigDecimal derivedScore);
}
```

- `answerValue` 統一為 String 型別，不做 polymorphic
- `derivedScore` 由 ScoreCalculationService 計算
- VO 是 immutable

---

## 4. ReviewSubmission Aggregate — Business Methods

擴充 B3a 建立的 ReviewSubmission shell（目前只有 getters）。

### 4.0 新增欄位

B3a shell 缺少以下欄位，B3b 需補上：

```java
private List<AnswerItem> answerSet;       // 作答內容（VO list）
private String attachmentRefsJson;        // 預留，B3b 不使用
```

### 4.1 Factory Methods

```java
// 建立初審草稿 — StartInitialReview 時呼叫
public static ReviewSubmission createInitialDraft(
        String submissionId, String caseId, String reviewerId,
        String scoreSheetVersionId) {
    // status = DRAFT
    // stage = INITIAL
    // revisionNo = 1
    // answerSet = empty list
    // totalScore = BigDecimal.ZERO
    // createdAt = now()
}

// 從持久化還原
public static ReviewSubmission reconstitute(
        String submissionId, String caseId, ReviewStage stage,
        String reviewerId, SubmissionStatus status, int revisionNo,
        List<AnswerItem> answerSet, BigDecimal totalScore,
        String comment, LocalDateTime submittedAt,
        String basedOnScoreSheetVersionId,
        String supersededBySubmissionId,
        String attachmentRefsJson,
        LocalDateTime createdAt, LocalDateTime updatedAt) {
}
```

### 4.2 Business Methods

```java
// 更新草稿 — SaveDraft 時呼叫（可多次）
public void updateDraft(List<AnswerItem> scoredAnswers, BigDecimal totalScore, String comment) {
    // guard: status == DRAFT
    // 更新 answerSet、totalScore、comment、updatedAt
}

// 正式提交
public void submit() {
    // guard: status == DRAFT
    // → status = SUBMITTED
    // submittedAt = now()
}
```

### 4.3 Domain Invariants

- `updateDraft()` 只能在 `DRAFT` 狀態
- `submit()` 只能在 `DRAFT` 狀態
- 已 `SUBMITTED` 不可再修改（append-only，新 revision 在 B3c）
- `submittedAt` 在 submit 後必填

---

## 5. ScoreCalculationService

放在 `application/reviewcase/` 作為 application-layer domain service。

```java
@Service
public class ScoreCalculationService {

    /**
     * 計算每個 answer 的 derivedScore 並回傳結果。
     * @param rawAnswers  前端傳入的原始答案（itemId + answerValue）
     * @param scoreSheet  已 published 的 ScoreSheetDefinition
     * @return ScoreResult 包含 scored answers + totalScore
     */
    public ScoreResult calculate(List<AnswerItem> rawAnswers, ScoreSheetDefinition scoreSheet);
}
```

### 5.1 ScoreResult

```java
public class ScoreResult {
    private List<AnswerItem> scoredAnswers;  // 每個 answer 帶 derivedScore
    private BigDecimal totalScore;           // Σ(derivedScore)
}
```

### 5.2 計分規則

| ScoreType | 公式 | 說明 |
|-----------|------|------|
| BOOLEAN | `"true".equals(answerValue) ? maxScore × weight : BigDecimal.ZERO` | 是/否判斷 |
| FIXED | `maxScore × weight` | 答題即得分，不看 answerValue |
| RANGE | `min(parseDecimal(answerValue), maxScore) × weight` | 0 ~ maxScore 範圍打分 |

- `totalScore = Σ(derivedScore)`
- 未作答的 item（不在 rawAnswers 中）→ derivedScore = 0，不加入 scoredAnswers
- `weight` 預設為 1.0

### 5.3 驗證規則

- rawAnswers 中的 itemId 必須存在於 scoreSheet 的 items 中（不存在 → 忽略或 throw）
- RANGE 的 answerValue 必須可 parse 為 decimal
- RANGE 的 answerValue 不得為負數

---

## 6. Application Use Cases

### 6.1 StartInitialReviewUseCase

```
@Service @Transactional
execute(ActorContext actor, String caseId)

1. actor.requireRole(DomainRole.REVIEWER)
2. 載入 ReviewCase（by caseId）
3. 驗證 actor.getUserId() == reviewCase.getReviewerAssignment().getReviewerId()
   → 不符 throw AccessDeniedException("Not the assigned reviewer")
4. 載入 ReviewProject（by reviewCase.getProjectId()）
   → 取得 boundScoreSheetId
5. 載入 ScoreSheetDefinition（by boundScoreSheetId）
   → 驗證 status == PUBLISHED，否則 throw DomainRuleViolationException
6. 建立 ReviewSubmission.createInitialDraft(UUID, caseId, actor.getUserId(), boundScoreSheetId)
7. reviewCase.startInitialReview(submissionId, boundScoreSheetId)
8. save reviewCase + save submission
9. publish InitialReviewStartedEvent(caseId, actor.getUserId(), submissionId)
10. return submissionId
```

### 6.2 SaveInitialReviewDraftUseCase

```
@Service @Transactional
execute(ActorContext actor, String caseId, List<AnswerInput> answers, String comment)
→ returns BigDecimal totalScore

1. actor.requireRole(DomainRole.REVIEWER)
2. 載入 ReviewCase
3. 驗證 actor.getUserId() == reviewCase.getReviewerAssignment().getReviewerId()
4. 驗證 reviewCase.status == INITIAL_REVIEW_IN_PROGRESS
   → 否則 throw DomainStateConflictException
5. 載入 ReviewSubmission（by reviewCase.getLatestInitialSubmissionId()）
6. 載入 ScoreSheetDefinition（by reviewCase.getScoreSheetVersionRef()）
   → ScoreSheetRepository.findById(scoreSheetVersionRef)
7. 用 ScoreCalculationService.calculate(rawAnswers, scoreSheet) 計算
8. submission.updateDraft(scoredAnswers, totalScore, comment)
9. save submission
10. publish ReviewDraftSavedEvent(caseId, submissionId, totalScore)
11. return totalScore
```

### 6.3 SubmitInitialReviewUseCase

```
@Service @Transactional
execute(ActorContext actor, String caseId)

1. actor.requireRole(DomainRole.REVIEWER)
2. 載入 ReviewCase
3. 驗證 actor.getUserId() == reviewCase.getReviewerAssignment().getReviewerId()
4. 驗證 reviewCase.status == INITIAL_REVIEW_IN_PROGRESS
5. 載入 ReviewSubmission（by reviewCase.getLatestInitialSubmissionId()）
6. 載入 ScoreSheetDefinition（by reviewCase.getScoreSheetVersionRef()）
7. 驗證所有 ALWAYS_REQUIRED items 都已作答
   → 未作答 throw DomainRuleViolationException("REQUIRED_ITEMS_NOT_ANSWERED")
8. 用 ScoreCalculationService 最終算分
9. submission.updateDraft(finalScoredAnswers, totalScore, comment) — 確保最新
10. submission.submit()
11. reviewCase.completeInitialReview(totalScore)
12. save submission + save reviewCase
13. publish ReviewSubmissionSubmittedEvent(caseId, submissionId, totalScore, reviewerId)
```

### 6.4 權限模型

| Use Case | Role Guard | Identity Guard |
|----------|-----------|----------------|
| StartInitialReview | `actor.requireRole(REVIEWER)` | `actor.userId == reviewerAssignment.reviewerId` |
| SaveInitialReviewDraft | `actor.requireRole(REVIEWER)` | `actor.userId == reviewerAssignment.reviewerId` |
| SubmitInitialReview | `actor.requireRole(REVIEWER)` | `actor.userId == reviewerAssignment.reviewerId` |

雙層驗證：先驗 role，再驗 identity（是否為被指派的審查醫師）。  
不符 role → `UnauthorizedException`；不符 identity → `AccessDeniedException`。

---

## 7. ReviewSubmission JPA Persistence

### 7.1 Table: review_submission

```sql
CREATE TABLE review_submission (
    submission_id       VARCHAR(64) PRIMARY KEY,
    case_id             VARCHAR(64) NOT NULL,
    stage               VARCHAR(20) NOT NULL,      -- INITIAL / REREVIEW
    reviewer_id         VARCHAR(64) NOT NULL,
    status              VARCHAR(20) NOT NULL,      -- DRAFT / SUBMITTED / SUPERSEDED
    revision_no         INT NOT NULL DEFAULT 1,
    answer_set_json     TEXT,                       -- JSON array of AnswerItem
    total_score         DECIMAL(10,2),
    comment             TEXT,
    submitted_at        TIMESTAMP,
    based_on_score_sheet_version_id VARCHAR(64),
    superseded_by_submission_id     VARCHAR(64),
    attachment_refs_json TEXT,                       -- 預留，B3b 不使用
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP,

    CONSTRAINT fk_submission_case FOREIGN KEY (case_id) REFERENCES review_case(case_id)
);
```

### 7.2 ReviewSubmissionEntity

JPA entity 與 domain model 的映射，包含 `toDomain()` 和 `toEntity()` 方法。

`answer_set_json` 使用 `@Convert(converter = AnswerSetJsonConverter.class)` 轉換 `List<AnswerItem>` ↔ JSON string。

### 7.3 AnswerSetJsonConverter

```java
@Converter
public class AnswerSetJsonConverter implements AttributeConverter<List<AnswerItem>, String> {
    // Jackson ObjectMapper
    // convertToDatabaseColumn: List<AnswerItem> → JSON string
    // convertToEntityAttribute: JSON string → List<AnswerItem>
}
```

### 7.4 ReviewSubmissionRepositoryImpl

```java
@Component
public class ReviewSubmissionRepositoryImpl implements ReviewSubmissionRepository {
    private final ReviewSubmissionJpaRepository jpaRepo;

    public ReviewSubmission findById(String submissionId);
    public void save(ReviewSubmission submission);
}
```

---

## 8. REST API Endpoints

### 8.1 POST /review-cases/{caseId}/start-initial-review

- Auth: api-key（已指派審查醫師）
- Request body: 無
- Response: `201 Created`
  ```json
  { "success": true, "data": { "submissionId": "sub-uuid-001" } }
  ```
- Errors: 403 (not assigned reviewer), 404 (case not found), 409 (wrong state)

### 8.2 POST /review-cases/{caseId}/initial-review/save-draft

- Auth: api-key（已指派審查醫師）
- Request body:
  ```json
  {
    "answers": [
      { "itemId": "Q001", "answerValue": "true" },
      { "itemId": "Q002", "answerValue": "85" }
    ],
    "comment": "optional reviewer comment"
  }
  ```
- Response: `200 OK`
  ```json
  { "success": true, "data": { "totalScore": 85.50 } }
  ```
- Errors: 400 (validation), 403, 404, 409

### 8.3 POST /review-cases/{caseId}/initial-review/submit

- Auth: api-key（已指派審查醫師）
- Request body: 無
- Response: `200 OK`
  ```json
  { "success": true, "data": null }
  ```
- Errors: 403, 404, 409, 422 (required items not answered)

---

## 9. Request / Response DTOs

### SaveInitialReviewDraftRequest

```java
public class SaveInitialReviewDraftRequest {
    @NotNull @Size(min = 1)
    private List<AnswerInput> answers;
    private String comment;  // optional
}

public class AnswerInput {
    @NotBlank
    private String itemId;
    @NotNull
    private String answerValue;
}
```

### SaveDraftResponse

```java
public class SaveDraftResponse {
    private BigDecimal totalScore;
}
```

### StartInitialReviewResponse

```java
public class StartInitialReviewResponse {
    private String submissionId;
}
```

---

## 10. Domain Events

| Event | Trigger | 欄位 |
|-------|---------|------|
| `InitialReviewStartedEvent` | startInitialReview | caseId, reviewerId, submissionId |
| `ReviewDraftSavedEvent` | saveDraft | caseId, submissionId, totalScore |
| `ReviewSubmissionSubmittedEvent` | submit | caseId, submissionId, totalScore, reviewerId |

所有 event 繼承現有 `DomainEvent` base class（extends Spring ApplicationEvent）。

---

## 11. ReviewSubmission Repository — Existing Interface

B3a 已定義完整介面，B3b **不需修改**，直接實作：

```java
public interface ReviewSubmissionRepository {
    void save(ReviewSubmission submission);
    Optional<ReviewSubmission> findById(String submissionId);
    List<ReviewSubmission> findByCaseIdAndStage(String caseId, ReviewStage stage);
    Optional<ReviewSubmission> findLatestByCaseIdAndStage(String caseId, ReviewStage stage);
}
```

B3b 主要使用 `save()` 和 `findById()`。`findByCaseIdAndStage` / `findLatestByCaseIdAndStage` 預留給 revision / query 使用。

---

## 12. ScoreSheetDefinition 查詢

B3b 的 use cases 需要載入 ScoreSheetDefinition 來計分。

**查詢路徑（StartInitialReview）**：

1. ReviewCase 持有 `projectId`
2. 載入 `ReviewProject` → 取得 `boundScoreSheetId`
3. 用 `ScoreSheetRepository.findById(boundScoreSheetId)` 載入 ScoreSheetDefinition
4. 驗證 `status == PUBLISHED`
5. 將 `boundScoreSheetId` 作為 `scoreSheetVersionRef` 設入 ReviewCase

**查詢路徑（SaveDraft / Submit）**：

1. ReviewCase 已有 `scoreSheetVersionRef`（startInitialReview 時設入）
2. 用 `ScoreSheetRepository.findById(scoreSheetVersionRef)` 載入

**所需 repository 注入**：

| Use Case | 需要的 Repositories |
|----------|-------------------|
| StartInitialReview | ReviewCaseRepository, ReviewProjectRepository, ScoreSheetRepository, ReviewSubmissionRepository |
| SaveDraft | ReviewCaseRepository, ScoreSheetRepository, ReviewSubmissionRepository |
| Submit | ReviewCaseRepository, ScoreSheetRepository, ReviewSubmissionRepository |

---

## 13. Testing Strategy

| 層 | 測試內容 | 預估數量 |
|----|---------|---------|
| Domain (ReviewSubmission) | createDraft, updateDraft, submit, guards, VO | ~8 tests |
| Domain (ReviewCase) | startInitialReview, completeInitialReview, guards | ~6 tests |
| Application (ScoreCalculationService) | BOOLEAN/FIXED/RANGE 計分、edge cases | ~8 tests |
| Application (3 use cases) | 成功 + 權限 + 狀態錯誤 + not found | ~15 tests |
| Infrastructure (JPA) | ReviewSubmission round-trip, JSON converter | ~4 tests |
| API integration | 3 endpoints × (success + error cases) | ~12 tests |
| **Total** | | **~53 tests** |

---

## 14. Design Decisions Summary

| Decision | 選擇 | 理由 |
|----------|------|------|
| answerValue 型別 | String（統一序列化） | JPA 友善、不需 polymorphic |
| answerSet 持久化 | JSON column（`answer_set_json TEXT`） | Submission 是 snapshot，不需查個別 item |
| 品質線判定 | 不納入 B3b | 專注初審作業流程，邊界清晰 |
| 計分位置 | ScoreCalculationService（application layer） | 跨 aggregate 資料，不適合放 domain |
| 計分規則 | BOOLEAN: maxScore×weight; FIXED: maxScore×weight; RANGE: min(val,max)×weight | 明確定義三種 ScoreType |
| 前端傳什麼 | 只傳 answerValue | Server-side 計分，前端不需知道計分邏輯 |
| SaveDraft 算分 | 每次都重算 | 前端即時看到分數 |
| Submission 建立時機 | startInitialReview 時建立 | 明確分離「開始」和「保存」 |
| Submission aggregate | 獨立 aggregate + 自己的 repo | 符合 authority doc |
| 附件 | 只預留欄位 | B3b 不做 |
| 操作 actor | 已指派審查醫師（非 HOSPITAL_MANAGER） | authority doc 明確要求 |
