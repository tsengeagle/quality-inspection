# B3a Assignment & Case Management Design Spec

## 1. 背景

B0–B2 已完成：
- B0: 工程基礎（DDD base、event publishing、audit trail、JWT security）
- B1a: ReviewProject aggregate（9 use cases、完整 CRUD + 狀態管理 + versioning）
- B1b: ScoreSheetDefinition aggregate（6 command + 4 query use cases）
- B2: Sampling context（ExecuteSampling、RetrySampling、Preview + 3 query use cases + domain events + infrastructure）

現有 ReviewCase aggregate 為 B2 建立的最小版本（caseId、batchId、projectId、hospitalId、sourceCaseKey、medicalRecordType、status），由 `SamplingBatch.createFromSelection()` 產生，缺少 assignment、cancel、return 等 lifecycle methods。

本次目標：擴充 ReviewCase aggregate 以支援案件分派管理（assign、reassign、cancel、return、reopen），建立 ReviewSubmission 基本框架，為 B3b（初審作業）奠基。

---

## 2. 範圍

### 2.1 本次包含

- ReviewCase aggregate 欄位擴充（一次加完到 B3c 所需的所有初審相關欄位）
- ReviewerAssignment value object
- 5 個 domain methods（assignReviewer、reassignReviewer、cancel、returnToInitialReview、reopenInitialReview）
- 4 個 domain events（ReviewCaseAssigned、ReviewCaseReassigned、ReviewCaseCancelled、ReviewCaseReturned）
- 5 個 application use cases
- ReviewSubmission 基本框架（domain model + enum + repository interface，不含 business methods）
- ReviewCaseEntity JPA 擴充
- ReviewCaseRepositoryImpl 擴充（findById）
- ReviewCaseController + 5 個 API endpoints
- 完整測試（domain + application + API + infrastructure）

### 2.2 本次不包含

- StartInitialReview、SaveDraft、Submit（B3b）
- Revision workflow（B3c）
- ReviewSubmission JPA persistence（B3b）
- 複審相關欄位與 methods（B4）
- 前端 feature modules
- Query use cases（案件列表、案件詳情）— 待需求確認後再規劃

### 2.3 B3 sub-phase 總覽

| Sub-phase | 範圍 | 狀態 |
|-----------|------|------|
| B3a | 案件分派管理（assign、reassign、cancel、return、reopen） | **本次** |
| B3b | 初審作業（start、save draft、submit、品質線判定） | 待規劃 |
| B3c | 初審 revision | 待規劃 |

---

## 3. ReviewCase Aggregate 擴充

### 3.1 新增欄位

以 domain-model.md §5.4 為準，一次加完到 B3c 所需的初審相關欄位。複審欄位（rereviewerAssignment、latestRereviewSubmissionId）留給 B4。

```java
// 現有欄位（保留不動）
private String caseId;
private String batchId;
private String projectId;
private String hospitalId;
private String sourceCaseKey;
private MedicalRecordType medicalRecordType;
private CaseStatus status;

// B3a 新增 — assignment 相關
private ReviewerAssignment reviewerAssignment;    // 初審指派
private LocalDateTime dueAt;                      // 初審期限
private String cancelReason;                      // 取消原因
private String returnReason;                      // 退回原因
private LocalDateTime cancelledAt;                // 取消時間
private LocalDateTime closedAt;                   // 結案時間

// B3a 新增 — B3b/B3c 預留欄位（nullable，暫不使用）
private boolean rereviewRequired;                 // 是否需複審
private BigDecimal finalScore;                    // 最終分數
private String finalConclusion;                   // 最終結論
private String latestInitialSubmissionId;          // 最新初審提交 ID
private String ruleSnapshotRef;                   // 規則快照參照
private String scoreSheetVersionRef;              // 評分表版本參照
```

### 3.2 ReviewerAssignment Value Object

```java
package com.tzuchi.his.quality.inspection.domain.reviewcase;

public class ReviewerAssignment {
    private final String reviewerId;
    private final LocalDateTime assignedAt;

    public ReviewerAssignment(String reviewerId, LocalDateTime assignedAt) {
        Objects.requireNonNull(reviewerId, "reviewerId must not be null");
        Objects.requireNonNull(assignedAt, "assignedAt must not be null");
        this.reviewerId = reviewerId;
        this.assignedAt = assignedAt;
    }

    // getters, equals, hashCode
}
```

### 3.3 Domain Methods（B3a 實作）

```java
// 1. 指派初審醫師
// Guard: status == SELECTED_PENDING_ASSIGNMENT
// Post: status → PENDING_INITIAL_REVIEW, reviewerAssignment 設定
public void assignReviewer(String reviewerId, LocalDateTime dueAt)

// 2. 改派初審醫師
// Guard: status == PENDING_INITIAL_REVIEW
// Post: reviewerAssignment 更新, dueAt 更新
public void reassignReviewer(String newReviewerId, LocalDateTime dueAt)

// 3. 取消案件
// Guard: status 非 CLOSED 且非 CANCELLED
// Post: status → CANCELLED, cancelReason 設定, cancelledAt 設定
public void cancel(String reason)

// 4. 退回初審
// Guard: status == INITIAL_REVIEW_IN_PROGRESS
// Post: status → RETURNED, returnReason 設定
public void returnToInitialReview(String reason)

// 5. 重新開啟初審
// Guard: status == RETURNED
// Post: status → PENDING_INITIAL_REVIEW, returnReason 清除
public void reopenInitialReview()
```

### 3.4 Domain Methods（B3b/B3c 預留，本次不實作）

```java
// B3b
public void startInitialReview()
public void completeInitialReview(String submissionId, BigDecimal totalScore, BigDecimal threshold)
public void closeWithoutRereview(BigDecimal finalScore, String finalConclusion)

// B3c
public void assertRevisionAllowed(ReviewStage stage, String actorId)
```

### 3.5 狀態轉移（B3a 範圍）

| From | Trigger | To | Guard |
|------|---------|-----|-------|
| SELECTED_PENDING_ASSIGNMENT | assignReviewer | PENDING_INITIAL_REVIEW | reviewerId 必填 |
| PENDING_INITIAL_REVIEW | reassignReviewer | PENDING_INITIAL_REVIEW | newReviewerId 必填 |
| SELECTED_PENDING_ASSIGNMENT | cancel | CANCELLED | reason 必填 |
| PENDING_INITIAL_REVIEW | cancel | CANCELLED | reason 必填 |
| INITIAL_REVIEW_IN_PROGRESS | cancel | CANCELLED | reason 必填 |
| RETURNED | cancel | CANCELLED | reason 必填 |
| INITIAL_REVIEW_IN_PROGRESS | returnToInitialReview | RETURNED | reason 必填 |
| RETURNED | reopenInitialReview | PENDING_INITIAL_REVIEW | — |

**cancel 的狀態約束：** CLOSED 和 CANCELLED 不可再 cancel（throw DomainStateConflictException）。INITIAL_REVIEW_COMPLETED、PENDING_REREVIEW、REREVIEW_IN_PROGRESS 在 B3a 時允許 cancel（這些狀態是 B3b/B4 的前置，但 cancel 語意上是「取消尚未結案案件」，適用所有非 terminal 狀態）。

### 3.6 既有相容性

- `createFromSampling()` 工廠方法保留不動
- 新增欄位全部 nullable/default，不影響 B2 建立流程
- `ReviewCaseRepository.save()` 和 `saveAll()` 保留

---

## 4. ReviewSubmission 基本框架

B3a 建立 ReviewSubmission 的 domain model 框架，不實作 business methods（留 B3b/B3c）。

### 4.1 ReviewSubmission Domain Class

```java
package com.tzuchi.his.quality.inspection.domain.reviewcase.submission;

public class ReviewSubmission {
    private String submissionId;
    private String caseId;
    private ReviewStage stage;              // INITIAL / REREVIEW
    private String reviewerId;
    private SubmissionStatus status;        // DRAFT / SUBMITTED / SUPERSEDED
    private int revisionNo;
    private BigDecimal totalScore;
    private String comment;
    private LocalDateTime submittedAt;
    private String basedOnScoreSheetVersionId;
    private String supersededBySubmissionId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected ReviewSubmission() { /* persistence */ }

    // getters only — business methods 留 B3b/B3c
}
```

### 4.2 Enums

```java
// ReviewStage.java
public enum ReviewStage {
    INITIAL,
    REREVIEW
}

// SubmissionStatus.java
public enum SubmissionStatus {
    DRAFT,
    SUBMITTED,
    SUPERSEDED
}
```

### 4.3 ReviewSubmissionRepository Interface

```java
public interface ReviewSubmissionRepository {
    void save(ReviewSubmission submission);
    Optional<ReviewSubmission> findById(String submissionId);
    List<ReviewSubmission> findByCaseIdAndStage(String caseId, ReviewStage stage);
    Optional<ReviewSubmission> findLatestByCaseIdAndStage(String caseId, ReviewStage stage);
}
```

### 4.4 Package 位置

`domain/reviewcase/submission/` — 作為 ReviewCase aggregate 的子 package。

---

## 5. Domain Events

### 5.1 新增 4 個 Event Classes

放在 `domain/reviewcase/event/` package，沿用 B1/B2 的 `DomainEvent` 繼承模式。

```java
// ReviewCaseAssignedEvent
// entityType: "ReviewCase", action: "ASSIGNED"
// payload: caseId, reviewerId, dueAt
public class ReviewCaseAssignedEvent extends DomainEvent {
    public ReviewCaseAssignedEvent(Object source, String actorUserId, String actorUserName,
                                    String hospitalId, String caseId,
                                    String reviewerId, LocalDateTime dueAt) { ... }
}

// ReviewCaseReassignedEvent
// entityType: "ReviewCase", action: "REASSIGNED"
// payload: caseId, previousReviewerId, newReviewerId, dueAt
public class ReviewCaseReassignedEvent extends DomainEvent {
    public ReviewCaseReassignedEvent(Object source, String actorUserId, String actorUserName,
                                      String hospitalId, String caseId,
                                      String previousReviewerId, String newReviewerId,
                                      LocalDateTime dueAt) { ... }
}

// ReviewCaseCancelledEvent
// entityType: "ReviewCase", action: "CANCELLED"
// payload: caseId, reason
public class ReviewCaseCancelledEvent extends DomainEvent {
    public ReviewCaseCancelledEvent(Object source, String actorUserId, String actorUserName,
                                     String hospitalId, String caseId, String reason) { ... }
}

// ReviewCaseReturnedEvent
// entityType: "ReviewCase", action: "RETURNED"
// payload: caseId, reason
public class ReviewCaseReturnedEvent extends DomainEvent {
    public ReviewCaseReturnedEvent(Object source, String actorUserId, String actorUserName,
                                    String hospitalId, String caseId, String reason) { ... }
}
```

### 5.2 Reopen 事件

ReopenInitialReview 不建立獨立事件。Reopen 使案件回到 PENDING_INITIAL_REVIEW 狀態，在 use case 層發布 `ReviewCaseAssignedEvent`（語意：案件重新回到待初審狀態，延續原指派）。

---

## 6. Application Use Cases

### 6.1 共同模式

- 放在 `application/reviewcase/` package
- @Service annotation
- Constructor injection
- ActorContext 作為第一個參數
- actor.requireRole(HOSPITAL_MANAGER) 做角色驗證
- actor.requireHospitalScope(reviewCase.getHospitalId()) 做 hospital scope guard（ReviewCase 自帶 hospitalId，不需 HospitalIdResolver）
- DomainEventPublisher 發布事件
- ReviewCaseRepository 持久化

### 6.2 ReviewCaseRepository 擴充

```java
public interface ReviewCaseRepository {
    void save(ReviewCase reviewCase);
    void saveAll(List<ReviewCase> cases);
    Optional<ReviewCase> findById(String caseId);        // 新增
}
```

### 6.3 AssignReviewCaseUseCase

```java
@Service
public class AssignReviewCaseUseCase {
    // 依賴: ReviewCaseRepository, DomainEventPublisher, HospitalIdResolver

    public void execute(ActorContext actor, String caseId, String reviewerId, LocalDateTime dueAt) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        ReviewCase reviewCase = repository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("ReviewCase", caseId));

        String hospitalId = reviewCase.getHospitalId();
        actor.requireHospitalScope(hospitalId);   // scope guard

        reviewCase.assignReviewer(reviewerId, dueAt);
        repository.save(reviewCase);
        eventPublisher.publish(new ReviewCaseAssignedEvent(...));
    }
}
```

### 6.4 ReassignReviewCaseUseCase

```java
@Service
public class ReassignReviewCaseUseCase {
    public void execute(ActorContext actor, String caseId, String newReviewerId, LocalDateTime dueAt) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        ReviewCase reviewCase = repository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("ReviewCase", caseId));
        actor.requireHospitalScope(reviewCase.getHospitalId());

        String previousReviewerId = reviewCase.getReviewerAssignment() != null
                ? reviewCase.getReviewerAssignment().getReviewerId() : null;

        reviewCase.reassignReviewer(newReviewerId, dueAt);
        repository.save(reviewCase);
        eventPublisher.publish(new ReviewCaseReassignedEvent(..., previousReviewerId, newReviewerId, ...));
    }
}
```

### 6.5 CancelReviewCaseUseCase

```java
@Service
public class CancelReviewCaseUseCase {
    public void execute(ActorContext actor, String caseId, String reason) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        ReviewCase reviewCase = repository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("ReviewCase", caseId));
        actor.requireHospitalScope(reviewCase.getHospitalId());

        reviewCase.cancel(reason);
        repository.save(reviewCase);
        eventPublisher.publish(new ReviewCaseCancelledEvent(...));
    }
}
```

### 6.6 ReturnInitialReviewUseCase

```java
@Service
public class ReturnInitialReviewUseCase {
    public void execute(ActorContext actor, String caseId, String reason) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        ReviewCase reviewCase = repository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("ReviewCase", caseId));
        actor.requireHospitalScope(reviewCase.getHospitalId());

        reviewCase.returnToInitialReview(reason);
        repository.save(reviewCase);
        eventPublisher.publish(new ReviewCaseReturnedEvent(...));
    }
}
```

### 6.7 ReopenInitialReviewUseCase

```java
@Service
public class ReopenInitialReviewUseCase {
    public void execute(ActorContext actor, String caseId) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        ReviewCase reviewCase = repository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("ReviewCase", caseId));
        actor.requireHospitalScope(reviewCase.getHospitalId());

        reviewCase.reopenInitialReview();
        repository.save(reviewCase);
        // 發布 ReviewCaseAssignedEvent，語意：案件重新回到待初審
        eventPublisher.publish(new ReviewCaseAssignedEvent(...));
    }
}
```

---

## 7. Infrastructure

### 7.1 ReviewCaseEntity JPA 擴充

現有 `ReviewCaseEntity` 新增所有欄位的 column mapping。

```java
// 新增欄位
@Column(name = "reviewer_id")
private String reviewerId;

@Column(name = "reviewer_assigned_at")
private LocalDateTime reviewerAssignedAt;

@Column(name = "due_at")
private LocalDateTime dueAt;

@Column(name = "cancel_reason")
private String cancelReason;

@Column(name = "return_reason")
private String returnReason;

@Column(name = "cancelled_at")
private LocalDateTime cancelledAt;

@Column(name = "closed_at")
private LocalDateTime closedAt;

@Column(name = "rereview_required")
private boolean rereviewRequired;

@Column(name = "final_score", precision = 10, scale = 2)
private BigDecimal finalScore;

@Column(name = "final_conclusion")
private String finalConclusion;

@Column(name = "latest_initial_submission_id")
private String latestInitialSubmissionId;

@Column(name = "rule_snapshot_ref")
private String ruleSnapshotRef;

@Column(name = "score_sheet_version_ref")
private String scoreSheetVersionRef;
```

### 7.2 ReviewCaseRepositoryImpl 擴充

新增 `findById()` 方法，從 JPA entity 轉回 domain object：

```java
@Override
public Optional<ReviewCase> findById(String caseId) {
    return jpaRepository.findById(caseId)
            .map(this::toDomain);
}
```

Domain ↔ Entity 轉換需處理 ReviewerAssignment value object 的拆合（entity 用 flat columns，domain 用 value object）。

### 7.3 ReviewSubmission JPA

**B3a 不建立 ReviewSubmission 的 JPA entity/mapping。** ReviewSubmission 在 B3a 只有 domain model + repository interface，不 persist。B3b 實作初審作業時再建立 JPA mapping。

---

## 8. REST API

### 8.1 ReviewCaseController

新建 `interfaces/rest/ReviewCaseController.java`。

```java
@RestController
@RequestMapping("/api/quality-inspection/review-cases")
public class ReviewCaseController {
    // 注入 5 個 use cases
}
```

### 8.2 Endpoints

| Method | Path | Use Case | Auth |
|--------|------|----------|------|
| POST | `/{caseId}/assign-reviewer` | AssignReviewCaseUseCase | HOSPITAL_MANAGER |
| POST | `/{caseId}/reassign-reviewer` | ReassignReviewCaseUseCase | HOSPITAL_MANAGER |
| POST | `/{caseId}/cancel` | CancelReviewCaseUseCase | HOSPITAL_MANAGER |
| POST | `/{caseId}/return` | ReturnInitialReviewUseCase | HOSPITAL_MANAGER |
| POST | `/{caseId}/reopen` | ReopenInitialReviewUseCase | HOSPITAL_MANAGER |

### 8.3 Request DTOs

```java
// AssignReviewerRequest
public class AssignReviewerRequest {
    private String reviewerId;     // required
    private LocalDateTime dueAt;   // required
}

// ReassignReviewerRequest
public class ReassignReviewerRequest {
    private String reviewerId;     // required
    private LocalDateTime dueAt;   // required
}

// CancelReviewCaseRequest
public class CancelReviewCaseRequest {
    private String reason;         // required
}

// ReturnReviewCaseRequest
public class ReturnReviewCaseRequest {
    private String reason;         // required
}
```

### 8.4 Response

所有 endpoints 使用 `ApiResponse.ok(null)` — command endpoints 不需要 response body。

### 8.5 Error Mapping

沿用 GlobalExceptionHandler：
- DomainStateConflictException → 409（狀態不允許的操作，如 cancel 已 CLOSED 的案件）
- DomainRuleViolationException → 422（domain 規則違反）
- ResourceNotFoundException → 404
- AccessDeniedException → 403
- BadRequestException → 400

---

## 9. 測試策略

### 9.1 Domain 單元測試

擴充現有 `ReviewCaseTest.java`：

| 測試案例 | 驗證重點 |
|---------|---------|
| assignReviewer 成功 | 狀態 → PENDING_INITIAL_REVIEW, reviewerAssignment 設定 |
| assignReviewer 從非 SELECTED_PENDING_ASSIGNMENT 失敗 | DomainStateConflictException |
| reassignReviewer 成功 | reviewer 更新, dueAt 更新 |
| reassignReviewer 從非 PENDING_INITIAL_REVIEW 失敗 | DomainStateConflictException |
| cancel 從各合法狀態成功 | 狀態 → CANCELLED, reason 記錄 |
| cancel 從 CLOSED 失敗 | DomainStateConflictException |
| cancel 從 CANCELLED 失敗 | DomainStateConflictException |
| returnToInitialReview 成功 | 狀態 → RETURNED, reason 記錄 |
| returnToInitialReview 從非 INITIAL_REVIEW_IN_PROGRESS 失敗 | DomainStateConflictException |
| reopenInitialReview 成功 | 狀態 → PENDING_INITIAL_REVIEW |
| reopenInitialReview 從非 RETURNED 失敗 | DomainStateConflictException |

### 9.2 Application 層測試

每個 use case 一個 test class，使用：
- `InMemoryReviewCaseRepository`（新建 test fixture）
- `CapturingDomainEventPublisher`（沿用）
- Lambda `HospitalIdResolver`（沿用模式）

| Test Class | 驗證重點 |
|-----------|---------|
| AssignReviewCaseUseCaseTest | role guard, scope guard, domain delegation, event publish |
| ReassignReviewCaseUseCaseTest | role guard, scope guard, previousReviewerId 擷取 |
| CancelReviewCaseUseCaseTest | role guard, scope guard, 各狀態取消 |
| ReturnInitialReviewUseCaseTest | role guard, scope guard |
| ReopenInitialReviewUseCaseTest | role guard, scope guard, reopen event |

### 9.3 API 整合測試

`ReviewCaseApiTest.java`：
- @SpringBootTest + TestRestTemplate
- TestTokenFactory 產生 HOSPITAL_MANAGER / 其他角色 token
- 驗證 5 個 endpoints 的成功路徑與 error 路徑
- 驗證 HTTP status code + ApiResponse structure

### 9.4 Infrastructure 測試

擴充或新增 ReviewCase JPA 測試：
- 新欄位 persist + load 驗證
- 向後相容（舊資料無新欄位時不壞）

---

## 10. 實作階段

| Stage | 內容 | 依賴 |
|-------|------|------|
| S1 | ReviewCase aggregate 欄位擴充 + ReviewerAssignment VO + 5 個 domain methods + domain tests | — |
| S2 | ReviewSubmission 基本框架（domain class + enums + repository interface） | — |
| S3 | 4 個 domain event classes | S1 |
| S4 | 5 個 application use cases + InMemoryReviewCaseRepository test fixture + application tests | S1, S3 |
| S5 | Infrastructure（ReviewCaseEntity JPA 擴充 + ReviewCaseRepositoryImpl findById） | S1 |
| S6 | ReviewCaseController + DTOs + API tests | S4, S5 |

S1 和 S2 可平行。S3 依賴 S1。S4 依賴 S1 + S3。S5 依賴 S1。S6 依賴 S4 + S5。

---

## 11. 與既有 code 的相容性

### 11.1 B2 Sampling 相容

- `SamplingBatch.createFromSelection()` 呼叫 `ReviewCase.createFromSampling()` — 保留不動
- 新增欄位全部 nullable/default — 不影響 B2 既有建立流程
- `ReviewCaseRepository.save()` / `saveAll()` — 保留，JPA entity 新增欄位有 default

### 11.2 ReviewCaseEntity 向後相容

- JPA entity 新增的 column 全部 nullable
- H2 test database 使用 `spring.jpa.hibernate.ddl-auto=update`（或 create-drop）— auto schema
- 無需手動 migration script（H2 + Hibernate DDL auto）

---

## 12. 設計決策記錄

| 決策 | 理由 |
|------|------|
| ReviewCase 欄位一次加完到 B3c | 避免重複修改同一 aggregate，但不含 B4 複審欄位 |
| ReviewSubmission B3a 只建框架 | B3a 不需要 persist submission，但 domain model 先到位 |
| ReopenInitialReview 獨立 use case | reopen 不一定伴隨 reassign，可能延續原指派 |
| Reopen 不建立獨立 event | reopen 使案件回到 PENDING_INITIAL_REVIEW，用 ReviewCaseAssignedEvent 語意足夠 |
| ReviewCaseReassigned 獨立 event | first assignment 與 reassignment 語意不同，分開有助 audit trail |
| Return 時 draft submission 自然保留 | authority docs 未定義 return 時要處理 draft，最小改動原則 |
| cancel 適用所有非 terminal 狀態 | 語意：「取消尚未結案案件」，CLOSED/CANCELLED 為 terminal state |

---

## 13. 不在本次範圍

- StartInitialReview、SaveInitialReviewDraft、SubmitInitialReview（B3b）
- ReviseInitialReview、GetRevisionHistory（B3c）
- 複審相關（B4）
- ReviewCase query use cases（案件列表、詳情）— 待確認後另行規劃
- 前端 feature modules
- Notification integration
- SYSTEM_ADMIN / AUDITOR 的案件查詢（metadata-only access）
