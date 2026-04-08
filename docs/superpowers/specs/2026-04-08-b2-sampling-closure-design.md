# B2 Sampling Closure Design Spec

## 1. 背景

B2 (Sampling) 核心 write-side 已實作完成：
- SamplingBatch aggregate + state machine + invariants
- ExecuteSamplingUseCase / PreviewSamplingPopulationUseCase / RetrySamplingBatchUseCase
- 3 個 write API endpoints + 30 tests

本次收斂目標：補齊 B2 所有 remaining items，使 Sampling bounded context 可封版。

---

## 2. 修正項目

### S1: RuleSnapshot 簡化版

**問題：** `RuleSnapshot` 目前是 stub（只有 2 個 String），不符合 D-011 traceability 需求。

**策略：** 簡化版 — 存 version IDs + key params 的結構化欄位，完整 snapshot_json 序列化留到 B5。

**設計：**

```java
public class RuleSnapshot {
    private final String projectVersionId;     // e.g. "PV-001-v2"
    private final int projectVersionNumber;
    private final String scoreSheetVersionId;  // e.g. "SS-001-v1"
    private final String samplingPolicy;       // e.g. "MANUAL" or future policy description
    private final String snapshotSummary;      // JSON string of key params for traceability

    // constructor, getters
}
```

**影響：**
- `RuleSnapshot.java` — 重寫欄位
- `ExecuteSamplingUseCase.java` — 建構 snapshot 時傳入真實版本資訊
- `RetrySamplingBatchUseCase.java` — 沿用前次 snapshot（不變）
- `SamplingBatch.java` — 無變動（已接受 RuleSnapshot）
- `SamplingBatchEntity.java` (infrastructure) — 新增欄位映射
- 所有 test 中建構 RuleSnapshot 的地方

---

### S2: Sampling Domain Events

**問題：** domain-events.md §5 定義了 5 個 sampling events，但 codebase 無任何 event class。

**策略：** 建立 5 個 event class，沿用 B1 的 DomainEvent 繼承模式。

**新增 event classes（`domain/sampling/event/` package）：**

| Event Class | entityType | action | 關鍵 payload |
|-------------|-----------|--------|-------------|
| `SamplingBatchStartedEvent` | SamplingBatch | STARTED | batchId, projectId, executionMode |
| `SamplingCandidatesPreparedEvent` | SamplingBatch | CANDIDATES_PREPARED | batchId, populationCount, eligibleCount, excludedCount |
| `ReviewCasesSelectedEvent` | SamplingBatch | CASES_SELECTED | batchId, selectedCount |
| `SamplingBatchCompletedEvent` | SamplingBatch | COMPLETED | batchId, projectId, selectedCount, populationCount |
| `SamplingBatchFailedEvent` | SamplingBatch | FAILED | batchId, failureReason |

**設計模式：** 與 B1 event 一致 — extends `DomainEvent`，constructor 接受 `String actorUserId, String actorUserName` + 業務欄位。

```java
public class SamplingBatchStartedEvent extends DomainEvent {
    public SamplingBatchStartedEvent(Object source, String actorUserId, String actorUserName,
                                      String hospitalId, String batchId, String projectId,
                                      String executionMode) {
        super(source, actorUserId, actorUserName,
              hospitalId, "SamplingBatch", batchId, "STARTED",
              String.format("{\"projectId\":\"%s\",\"executionMode\":\"%s\"}", projectId, executionMode));
    }
}
```

---

### S3: Event Emit 整合

**問題：** ExecuteSamplingUseCase 和 RetrySamplingBatchUseCase 未發布 domain events。

**策略：** 注入 `DomainEventPublisher`，在適當時機 publish events。

**ExecuteSamplingUseCase emit 順序：**
1. `SamplingBatch.start()` 後 → `SamplingBatchStartedEvent`
2. `batch.recordCandidateCount()` 後 → `SamplingCandidatesPreparedEvent`
3. `batch.selectCases()` 後 → `ReviewCasesSelectedEvent`
4. `batch.complete()` 後 → `SamplingBatchCompletedEvent`

**RetrySamplingBatchUseCase emit 順序：** 同上（retry 也是完整的 batch lifecycle）

**失敗場景：**
- 若 use case catch exception 做 `batch.fail(reason)` → `SamplingBatchFailedEvent`
- 目前 execute/retry 沒有 try-catch fail path（直接拋出），所以暫不 emit `SamplingBatchFailedEvent`
- 未來加入 fail handling 時再 emit

**PreviewSamplingPopulationUseCase：** 不 emit（preview 不建立 batch）

---

### S4: SamplingBatchQueryPort + Read Models

**策略：** 建 hexagonal port interface + read model DTOs。

**Port interface:**

```java
public interface SamplingBatchQueryPort {
    Page<SamplingBatchSummary> findBatches(SamplingBatchQueryCriteria criteria);
    Optional<SamplingBatchDetailView> findDetailById(String batchId);
}
```

**Read models:**

```java
// List item
public class SamplingBatchSummary {
    private String batchId;
    private String projectId;
    private String projectName;       // denormalized for display
    private ExecutionMode executionMode;
    private BatchStatus status;
    private int populationCount;
    private int eligibleCount;
    private int selectedCount;
    private String retryOfBatchId;
    private Instant startedAt;
    private Instant finishedAt;
}

// Detail view
public class SamplingBatchDetailView {
    private String batchId;
    private String projectId;
    private String projectName;
    private ExecutionMode executionMode;
    private BatchStatus status;
    private int populationCount;
    private int eligibleCount;
    private int excludedCount;
    private int selectedCount;
    private String retryOfBatchId;
    private String failureReason;
    private Instant startedAt;
    private Instant finishedAt;
    private RuleSnapshotSummary ruleSnapshot;
    private List<SelectedCaseRefView> selectedCases;
}

// Snapshot summary for display
public class RuleSnapshotSummary {
    private String projectVersionId;
    private int projectVersionNumber;
    private String scoreSheetVersionId;
    private String samplingPolicy;
}

// Selected case for display
public class SelectedCaseRefView {
    private String sourceCaseKey;
    private String hospitalId;
    private String medicalRecordType;
}
```

**Query criteria:**

```java
public class SamplingBatchQueryCriteria {
    private String projectId;
    private BatchStatus status;
    private ExecutionMode executionMode;
    private int page;     // 0-based
    private int size;     // default 20
}
```

**Pagination:**

```java
public class Page<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
```

---

### S5: ListSamplingBatchesUseCase

```java
@Service
public class ListSamplingBatchesUseCase {
    private final SamplingBatchQueryPort queryPort;

    public Page<SamplingBatchSummary> execute(ActorContext actor,
                                               SamplingBatchQueryCriteria criteria) {
        actor.requireAnyRole(DomainRole.HOSPITAL_MANAGER, DomainRole.AUDITOR, DomainRole.SYSTEM_ADMIN);
        return queryPort.findBatches(criteria);
    }
}
```

**注意：** 角色 scope filtering（SYSTEM_ADMIN 只看 metadata）留到 B6 (Permission-aware Queries)。

---

### S6: GetSamplingBatchDetailUseCase

```java
@Service
public class GetSamplingBatchDetailUseCase {
    private final SamplingBatchQueryPort queryPort;

    public SamplingBatchDetailView execute(ActorContext actor, String batchId) {
        actor.requireAnyRole(DomainRole.HOSPITAL_MANAGER, DomainRole.AUDITOR, DomainRole.SYSTEM_ADMIN);
        return queryPort.findDetailById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("SamplingBatch", batchId));
    }
}
```

---

### S7: ExportSamplingBatchResultUseCase（骨架）

```java
@Service
public class ExportSamplingBatchResultUseCase {
    private final SamplingBatchQueryPort queryPort;

    public SamplingBatchExportResult execute(ActorContext actor, String batchId) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        SamplingBatchDetailView detail = queryPort.findDetailById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("SamplingBatch", batchId));
        return new SamplingBatchExportResult(detail);
    }
}
```

`SamplingBatchExportResult` 包裝 detail view，實際 CSV/Excel 格式轉換留 B7。

---

### S8: Infrastructure + Controller + Tests

#### S8a: SamplingBatchQueryAdapter

JPA-based implementation of `SamplingBatchQueryPort`:
- `findBatches()` — JPQL with dynamic criteria + pagination
- `findDetailById()` — join fetch selected cases + snapshot

#### S8b: SamplingBatchEntity 更新

現有 `SamplingBatchEntity` 需新增 RuleSnapshot 欄位：
- `projectVersionId: varchar`
- `projectVersionNumber: int`
- `scoreSheetVersionId: varchar`
- `samplingPolicy: varchar`
- `snapshotSummary: varchar(2000)` (JSON string)

#### S8c: CandidateSourcePort stub adapter 完善

現有 `StubCandidateSourceAdapter` 維持不變，已足夠。確認 `@Component` 註解正確。

#### S8d: SamplingController 新增 2 個 GET endpoints

```java
@GetMapping("/sampling-batches")
public ResponseEntity<ApiResponse<Page<SamplingBatchSummary>>> listBatches(
        @CurrentActor ActorContext actor,
        @RequestParam(required = false) String projectId,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String executionMode,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) { ... }

@GetMapping("/sampling-batches/{batchId}")
public ResponseEntity<ApiResponse<SamplingBatchDetailView>> getBatchDetail(
        @CurrentActor ActorContext actor,
        @PathVariable String batchId) { ... }
```

#### S8e: Tests

新增測試：

| Test Class | 測試項目 |
|------------|---------|
| `SamplingBatchTest` | RuleSnapshot 新欄位建構驗證 |
| `SamplingUseCaseTest` | Event emit 驗證（verify DomainEventPublisher.publish called） |
| `SamplingUseCaseTest` | ListBatches + GetDetail use case |
| `SamplingControllerApiTest` | GET /sampling-batches + GET /sampling-batches/{batchId} endpoints |
| `SamplingControllerApiTest` | Export endpoint (骨架) |

---

## 3. 不在本次範圍

- 完整 snapshot_json 序列化 + checksum（留 B5）
- 角色 scope filtering (SYSTEM_ADMIN metadata only)（留 B6）
- CSV/Excel 實際格式轉換（留 B7）
- CandidateSourcePort 真實 HIS adapter（留整合期）
- Cross-batch 重複檢查（D-007 明確為同批次限制）

---

## 4. ActorContext.requireAnyRole()

`ActorContext` 已有 `requireAnyRole(DomainRole... roles)` — 無需新增。S5/S6/S7 直接使用。

---

## 5. 執行順序

1. **S1**: RuleSnapshot 簡化版（domain + infrastructure）
2. **S2**: 5 個 Sampling domain event classes
3. **S3**: Event emit 整合到 3 個 use cases + DomainEventPublisher 注入
4. **S4**: SamplingBatchQueryPort + read models + Page + criteria
5. **S5**: ListSamplingBatchesUseCase
6. **S6**: GetSamplingBatchDetailUseCase
7. **S7**: ExportSamplingBatchResultUseCase（骨架）
8. **S8**: SamplingBatchQueryAdapter + Controller endpoints + Tests
9. **Final**: 全量 test 驗證

---

## 6. Decision log 影響

本次需新增的 decision：無（D-011 已存在，D-013 已存在）。
RuleSnapshot 簡化版是 D-011 的漸進實作，不需要新 decision。
