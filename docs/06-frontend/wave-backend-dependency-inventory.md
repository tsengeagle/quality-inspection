# Wave 0–5 跨波次 Backend 依賴盤點 & Closure 排序建議

> ⚠️ **SUPERSEDED — 本文件已被取代**  
> 本文件的 Wave 3 / Wave 5 gap 描述均基於 2025-07 的採集基線，其中大多數 gap 已在後續實作中關閉。  
> **現行 authority 請使用：`docs/06-frontend/frontend-backend-alignment-baseline.md`**  
> 本文件僅保留作為歷史脈絡參考。Wave 3/5 的舊版 gap 描述與現況的差異，已詳列於新文件第 5 節。  
>
> 本文件為 Phase A（逐波依賴盤點）+ Phase B（Backend Closure 排序建議）。  
> 目的：區分「純前端遞延」與「真正的 backend gap」，不宣稱 controller 存在即代表 page-friendly contract 就緒。  
> 基線：Evidence gathered from source code reading (backend + frontend) on 2025-07.

---

## Phase A — Wave 0~5 逐波依賴盤點

---

### Wave 0 — Foundation（Auth + Routing Shell）

#### 前端依賴

| 依賴類型 | 後端能力 | 現況 |
|---------|---------|------|
| Auth 解析 | ApiKeyAuthFilter → TzJwtParser → ActorContext | ✅ REAL |
| JWT 格式 | `api-key` header → parse `tz-user` claim（double-encoded JSON） | ✅ REAL |
| ActorContext 注入 | `@CurrentActor` via `ActorContextArgumentResolver` | ✅ REAL |
| Route Guard 授權 | RoleGuard / ScopeGuard / ProjectionGuard | ⚠️ PASSTHROUGH |

#### Gap 描述

- Auth 基礎設施完整，JWT 讀取與 `ActorContext` 建構均為真實實作。
- 三個 Angular Route Guard 全部 `return true`（postwave comment 標記為「post-Wave 1」）。
- **Guard 啟動的 blocker 不是 backend，而是 shell 整合**：Angular Guard 需要 shell 注入已認證的 actor context（role/scope/projectionType），目前以 standalone mode 繞過。

#### 結論

**純前端遞延（shell integration gate）。** Backend auth 不是 blocker。

---

### Wave 1 — Route Structure

#### 前端依賴

| 依賴類型 | 後端能力 | 現況 |
|---------|---------|------|
| Feature module lazy routing | Angular lazy-load boundary | ✅ REAL |
| `/sampling` → SamplingModule | 路由結構 | ✅ REAL |
| `/cases` → CasesModule | 路由結構 | ✅ REAL |
| CasesRoutingModule 6 routes | 路由結構 | ✅ REAL |
| SamplingRoutingModule 3 routes | 路由結構 | ✅ REAL |

#### Gap 描述

無 backend gap。Route structure 為純前端架構決策。Guards 依然 passthrough（繼承 Wave 0 遞延）。

#### 結論

**無 backend gap。** 純前端完成。

---

### Wave 2 — Shared Components

#### 前端依賴

| 依賴類型 | 後端能力 | 現況 |
|---------|---------|------|
| C01 CaseStatusChip | 無（純 UI） | ✅ 純前端 |
| C02 CaseBanner | 無（純 UI） | ✅ 純前端 |
| C03 ModeLabel / PageHeader | 無（純 UI） | ✅ 純前端 |
| C04 ProjectionNotice | 無（純 UI） | ✅ 純前端 |

#### Gap 描述

共用 component 為純前端 UI 元件，無 backend 依賴。`features/cases/components/` 目錄已存在。

#### 結論

**無 backend gap。** 純前端。

---

### Wave 3 — Cases Read-Only（UI-009 / UI-010 / UI-014）

#### 前端依賴（完整列表）

| 依賴類型 | 對應 API | 現況 |
|---------|---------|------|
| 案件列表資料 | `GET /review-cases` → `Page<ReviewCaseSummary>` | ⚠️ STUB |
| 案件詳情資料 | `GET /review-cases/{caseId}` → `ReviewCaseDetailView` | ⚠️ STUB |
| 修訂歷程資料 | `GET /review-cases/{caseId}/revision-history`（或等效端點） | ⚠️ STUB |
| 單一 submission 詳情 | `GET /review-submissions/{submissionId}` → `SubmissionDetailView` | ✅ REAL（but not wired） |
| Role-aware mode resolution | `projectionType`、`allowedActions`、`role`、`isOwner` | ❌ **GAP — 欄位不存在** |
| SYSTEM_ADMIN metadata projection | 欄位遮蔽（whitelist）| ❌ **GAP — QueryAdapter 未實作** |

#### Angular Service 現況

```
CasesService.getCases()           → return of([]);     // TODO: real HTTP
CasesService.getCaseDetail()      → return of(null);   // TODO: real HTTP
CasesService.getRevisionHistory() → return of(null);   // TODO: real HTTP
```

**所有 Wave 3 API 呼叫均為 stub。**

#### Backend API 現況

| 端點 | Controller | UseCase | QueryPort | 實作 |
|-----|-----------|---------|----------|------|
| `GET /review-cases` | ✅ ReviewCaseController | ✅ ListReviewCasesUseCase | ✅ ReviewCaseQueryAdapter (JPA) | ✅ REAL |
| `GET /review-cases/{caseId}` | ✅ ReviewCaseController | ✅ GetReviewCaseDetailUseCase | ✅ ReviewCaseQueryAdapter (JPA) | ✅ REAL |
| `GET /review-cases/{caseId}/lifecycle` | ✅ ReviewCaseController | ✅ GetCaseLifecycleUseCase | — | ✅ REAL |
| Revision history（by caseId） | ✅ ReviewCaseController | ✅ GetRevisionHistoryUseCase | ReviewSubmissionRepository | ⚠️ 返回 domain objects |
| `GET /review-submissions/{id}` | ✅ SubmissionController | ✅ GetSubmissionDetailUseCase | ✅ SubmissionQueryAdapter | ✅ REAL |

#### Contract Mismatch（Page-Friendly Query Gap）

以下列出 frontend component 需要但 backend 目前未提供的欄位：

**`ReviewCaseDetailView` — 目前有的欄位：**
```
caseId, projectId, batchId, hospitalId, status,
reviewerId, rereviewerId, medicalRecordType, sourceCaseKey,
initialReviewScore, finalScore, finalConclusion,
createdAt, assignedAt, dueAt, closedAt, cancelledAt,
List<SubmissionSummaryItem> submissions,
latestRereviewSubmissionId
```

**`CaseDetailPageComponent._resolveMode()` 需要但 backend 未提供：**
```
d.projectionType    → 'FULL' | 'METADATA_ONLY'   ❌ 欄位不存在
d.role              → actor 在此案件的角色         ❌ 欄位不存在
d.isOwner           → 是否為 assigned reviewer    ❌ 欄位不存在
```

**`RevisionHistoryPageComponent` 需要：**
```
vm.hasExceptionEntry  → CLOSED + originalRereviewer + baseSubmission   ❌ 欄位不存在
```

**`ReviewCaseSummary` — 目前有的欄位：**
```
caseId, projectId, batchId, hospitalId, status,
reviewerId, rereviewerId, medicalRecordType, sourceCaseKey,
createdAt, assignedAt, dueAt
```

**`CaseListPageComponent` 需要但 backend 未提供：**
```
mode（或足以計算 mode 的 projectionType 欄位）   ❌ 欄位不存在
allowedActions / nextActionHint                   ❌ 欄位不存在
```

#### 修訂歷程 DTO Gap

```java
// GetRevisionHistoryUseCase.execute() 返回：
List<ReviewSubmission>   ← 這是 domain aggregate，不是 DTO

// 前端需要 RevisionHistoryViewModel：
RevisionHistoryViewModel {
  entries: RevisionEntry[]
  hasExceptionEntry: boolean   // CLOSED + originalRereviewer + baseSubmission 組合計算
}
```

`GetRevisionHistoryUseCase` 返回 domain objects，缺乏到 view-model 的轉換。Controller 直接序列化 domain aggregate 是一個設計問題。

#### SYSTEM_ADMIN Projection Gap

- `api-spec.md §3.3`（O3-C）明確定義 SYSTEM_ADMIN metadata-only whitelist
- `GetReviewCaseDetailUseCase.execute()` 直接返回 `ReviewCaseDetailView`，無 role-based field masking
- 只有 `SubmissionDetailView` 有 `maskedFields` 設計，`ReviewCaseDetailView` 無對應機制

#### Wave 3 結論

**FULLY STUBBED — 無法在不修正 query contract 的前提下接通 backend。**

主要阻礙：
1. `ReviewCaseDetailView` 缺少 `projectionType`、`role`（actor role in context of this case）、`isOwner`
2. Revision history 端點返回 domain objects，不是 page-ready DTO
3. `CasesService` 三個方法全部為 `of(null)` / `of([])`（Angular HTTP 未接通）
4. SYSTEM_ADMIN projection 未實作

---

### Wave 4 — Sampling（UI-006 / UI-007）

#### 前端依賴

| 依賴類型 | 對應 API | 現況 |
|---------|---------|------|
| 批次清單 | `GET /sampling-batches` | ⚠️ FIXTURE |
| 執行 context | `GET /review-projects/{projectId}` 或推導 | ⚠️ FIXTURE（hardcoded） |
| 預估母體 | `POST /review-projects/{projectId}/sampling/preview` | ✅ REAL（未接通） |
| 執行抽選 | `POST /review-projects/{projectId}/sampling/execute` | ✅ REAL（未接通） |
| 重跑批次 | `POST /sampling-batches/{batchId}/retry` | ✅ REAL（未接通） |
| 批次詳情 | `GET /sampling-batches/{batchId}` | ✅ REAL（未接通） |

#### Angular Service 現況

```typescript
// sampling.service.ts
const BATCH_FIXTURES: SamplingBatchSummaryItem[] = [
  { batchId: 'BATCH-FAILED-001', status: 'FAILED', canRetry: true, ... },
  { batchId: 'BATCH-COMPLETED-001', status: 'COMPLETED', selectedCount: 24, ... }
];

getBatches(criteria)    → filters BATCH_FIXTURES        // FIXTURE
getExecuteContext(id)   → returns hardcoded project data // FIXTURE（hardcoded）
```

#### Backend API 現況

| 端點 | 實作 |
|-----|------|
| `POST /sampling/preview` | ✅ REAL（但資料來自 StubCandidateSourceAdapter） |
| `POST /sampling/execute` | ✅ REAL（同上） |
| `POST /sampling-batches/{batchId}/retry` | ✅ REAL |
| `GET /sampling-batches` | ✅ REAL |
| `GET /sampling-batches/{batchId}` | ✅ REAL |

#### StubCandidateSourceAdapter

```java
/**
 * Stub implementation of CandidateSourcePort for local development.
 * In production, this would be replaced by an adapter calling HIS or external data sources.
 */
```

返回 5 筆 hardcoded `CASE-{projectId}-{1..5}` candidates。
- 功能性正確（合法的資料格式），但**資料來源為合成資料**。
- 不阻擋 Wave 4 Angular 接通，但代表在 production 前必須替換。

#### Contract Mismatch

- `SamplingBatchSummaryItem.canRetry`（前端）→ 前端從 `status === 'FAILED'` 計算，後端 `SamplingBatchSummary` 是否有對應欄位待確認
- `SamplingExecuteContext`（前端 hardcoded）的欄位結構需與 `ReviewProjectDetailView` 對應

#### Wave 4 結論

**BACKEND READY。前端 service HTTP wiring 是遞延（fixture）。**

阻礙為純前端：`SamplingService` 替換 fixture → 真實 HttpClient 呼叫。  
`StubCandidateSourceAdapter` 是資料品質 gap，不是 Wave 4 接通的阻礙。

---

### Wave 5 — Cases Commands（UI-011 / UI-012 / UI-013）

> 2026-04 更新：本節原始結論建基於 2025-07 的盤點。UI-011 已完成 Slice 3 backend closure 與 limited integration；以下保留 UI-012 / UI-013 gap，但 UI-011 不再屬於「page bootstrap 完全無 source」狀態。

#### 前端依賴

| 依賴類型 | 對應 API | 現況 |
|---------|---------|------|
| UI-011 assignment bootstrap context | `GET /review-cases/{caseId}/assignment-context` | ✅ REAL |
| UI-011 assignment candidates | `GET /review-cases/{caseId}/assignment-candidates` | ✅ REAL（source = stub-backed dev-only adapter） |
| 頁面 bootstrap context（初審） | 需要 `InitialReviewContext` | ❌ **GAP — 無對應 backend 端點** |
| 頁面 bootstrap context（複審） | 需要 `RereviewContext` | ❌ **GAP — 無對應 backend 端點** |
| 指派初審醫師 | `POST /assign-reviewer` | ✅ REAL（未接通） |
| 開始初審 | `POST /initial-review/start` | ✅ REAL（未接通） |
| 保存初審草稿 | `POST /initial-review/draft` | ✅ REAL（未接通） |
| 提交初審 | `POST /submissions/{id}/initial-review/submit` | ✅ REAL（未接通） |
| 修正初審 | `POST /submissions/{id}/initial-review/revise` | ✅ REAL（未接通） |
| 開始複審 | `POST /rereview/start` | ✅ REAL（未接通） |
| 保存複審草稿 | `POST /rereview/draft` | ✅ REAL（未接通） |
| 提交複審 | `POST /submissions/{id}/rereview/submit` | ✅ REAL（未接通） |
| 修正複審 | `POST /submissions/{id}/rereview/revise` | ✅ REAL（未接通） |

#### Angular Component 現況

```typescript
// InitialReviewPageComponent.ngOnInit()
this.caseId = this.route.snapshot.paramMap.get('caseId') ?? 'CASE-WAVE5';
this.applyContext(this.createDefaultContext(this.caseId));  // NO service call

// RereviewPageComponent.ngOnInit()
this.caseId = this.route.snapshot.paramMap.get('caseId') ?? 'CASE-WAVE5';
this.applyContext(this.createDefaultContext(this.caseId));  // NO service call
```

兩個 command page 均從 `createDefaultContext(caseId)` 取得資料 — 硬編碼預設值，**無任何 HTTP 呼叫**。

#### Page Bootstrap Context Gap

`InitialReviewPageComponent.resolveMode()` 需要：

```typescript
interface InitialReviewContext {
  caseId: string;
  status: string;          // 案件狀態
  blockedReason: string | null;
  hasDraft: boolean;       // 此 reviewer 是否有 DRAFT submission
  hasSubmittedBase: boolean; // 是否已有 SUBMITTED submission（→ REVISION mode）
  basedOnVersion?: SubmissionRef;
}
```

`RereviewPageComponent.resolveMode()` 需要：

```typescript
interface RereviewContext {
  status: string;
  blockedReason: string | null;
  hasDraft: boolean;
  hasSubmittedBase: boolean;
  isClosedRevisionException: boolean;  // status == CLOSED + 是 originalRereviewer + 有 submitted rereview
  initialSubmissionId: string;
  initialReviewerName: string;
  initialTotalScore: string;
  finalDecisionSummary: string;
}
```

以上欄位均**無法從現有 `ReviewCaseDetailView` 直接取得**：

| 欄位 | 推導需要的資訊 | 目前 backend 狀態 |
|-----|--------------|-----------------|
| `hasDraft` | 查詢此 caseId 下 actor 的 DRAFT submission | `SubmissionSummaryItem` 有 status，但 `ReviewCaseDetailView` 沒有 actor-context filter |
| `hasSubmittedBase` | 查詢此 caseId 下 SUBMITTED submission | 同上 |
| `isClosedRevisionException` | status==CLOSED + actor==originalRereviewer + 有 SUBMITTED rereview | 需要 business logic 計算 |
| `initialSubmissionId` | rereview page 需要顯示初審 submission 的 ID | `submissions` 列表有，但需 stage filter |
| `initialTotalScore` / `initialReviewerName` | 需要 join submission 資料 | `ReviewCaseDetailView` 有 `initialReviewScore` 但無 reviewer name |

#### B3 Command Object Gap（次要）

`application/reviewcase/command/` 目錄下只有 B4 command objects：
```
AssignRereviewerCommand, CloseWithoutRereviewCommand, ReassignRereviewerCommand,
ReviseRereviewCommand, SaveRereviewDraftCommand, SendToRereviewCommand,
StartRereviewCommand, SubmitRereviewCommand
```

B3 use cases（AssignReviewCase、StartInitialReview 等）**傳遞 primitives 而非 command objects**。  
這是 consistency gap，不是功能 blocker，但代表 B3 use cases 缺乏明確的 command value object。

#### UI-011 Contract Clarification / Closure（2026-04）

- `assignment-context` 已提供 `pageMode`、`blocked`、`blockedReason`、`allowedActions`、`currentReviewer`、`currentRereviewer`、`reviewerDueAt`、`rereviewerDueAt`、`effectiveDueAt`
- `assignment-candidates` 已提供最小正式候選人 contract：`userId`、`displayName`、`displayLabel`、`role`、`active`、`selectable`、`blockedReason`
- 候選資料來源目前為 `StubAssignmentCandidateLookupAdapter`，只可視為 dev-only / limited-ready，不可宣稱 production-ready
- `change reason` 尚未形成正式 command contract；UI-011 僅可標示 pending clarification
- `dueAt` 的正式 contract 目前只收斂到 command requiredness；更深的 helper / validation policy 仍待 authority 定稿

#### Wave 5 結論

**UI-011：limited integration 與最小 contract closure 已成立；UI-012 / UI-013 page bootstrap context 仍是後續 backend gap。**

---

## Phase B — Backend Closure 排序建議

---

### 跨波次依賴叢集分析

```
Cluster A ─ Auth / Guard 執行   → Wave 0      → Blocker: shell 整合（非 backend）
Cluster B ─ Case query contract → Wave 3 + 5  → Backend gap：缺少 page context 欄位
Cluster C ─ Revision history DTO → Wave 3     → Backend gap：domain object 直接序列化
Cluster D ─ Sampling HTTP wiring → Wave 4     → 純 Angular service 替換（最快 win）
Cluster E ─ Page bootstrap context → Wave 5  → Backend gap：初審/複審 context 欄位
```

#### 依賴鏈圖

```
Wave 3 可接通 ← Cluster B（projectionType + role + isOwner in DetailView）
                Cluster C（RevisionHistoryView DTO）
                Angular service HTTP wiring

Wave 4 可接通 ← Cluster D（Angular service 替換 fixture）

Wave 5 可接通 ← Cluster B（Wave 3 prerequisite）
                Cluster E（hasDraft / hasSubmittedBase / isClosedRevisionException bootstrap）
                Angular service command wiring

Guard 執行   ← Cluster A（shell 提供 actor context）
```

---

### Slice 1 — 最高槓桿（同時解鎖 Wave 3 + Wave 4）

#### 後端工作

**B-S1-1. 為 `ReviewCaseDetailView` 新增 page-friendly context 欄位**

```java
// 在 GetReviewCaseDetailUseCase.execute() 中由 actor + case 計算後填入
String projectionType;   // "FULL" | "METADATA_ONLY" — actor role 決定
String actorRole;        // actor 在此案件的 domain role（用於前端 mode resolution）
boolean isOwner;         // actor == reviewerId 或 rereviewerId
```

設計選項：
- Option A（推薦）：在 `GetReviewCaseDetailUseCase.execute()` 中，根據 actor role 計算後直接包入 `ReviewCaseDetailView`
- Option B：定義新的 `ReviewCasePageContextView` wrapper，包含原 `ReviewCaseDetailView` + page context

**B-S1-2. 修正 Revision History 端點返回 DTO（而非 domain objects）**

```java
// 目前
public List<ReviewSubmission> execute(...)

// 應改為
public RevisionHistoryView execute(...)

// RevisionHistoryView 結構
RevisionHistoryView {
  List<RevisionEntry> entries;
  boolean hasExceptionEntry;   // CLOSED + originalRereviewer + base submission 計算
}
```

**B-S1-3. 在 `ListReviewCasesUseCase` 結果中加入 projectionType（可選但有助 Wave 3 list page）**

```java
// 若 SYSTEM_ADMIN，在 ReviewCaseSummary 中設定 projectionType = "METADATA_ONLY"
// 並移除非 whitelist 欄位
```

#### 前端工作（Slice 1 中 Angular-only 快速 win）

**F-S1-1. Wave 4 HTTP wiring（與後端 Slice 1 平行進行）**

```typescript
// 替換 sampling.service.ts fixture → HttpClient
getBatches(criteria): Observable<SamplingBatchSummaryItem[]> {
  return this.http.get<Page<SamplingBatchSummaryItem>>(`/api/sampling-batches`, { params: ... })
    .pipe(map(page => page.content));
}
```

Backend 端點已就緒，這是純前端改動，可與後端工作平行。

#### Slice 1 預期產出

- Wave 3 `CaseDetailPageComponent._resolveMode()` 可從真實 `ReviewCaseDetailView` 取得 `projectionType`、`actorRole`、`isOwner`
- Wave 3 `RevisionHistoryPageComponent` 可取得 `hasExceptionEntry`
- Wave 4 `SamplingService` 接通真實 backend，fixture 退役
- `CasesService.getCaseDetail()` / `getCases()` 可替換 stub → HttpClient

---

### Slice 2 — 次優先（解鎖 Wave 5）

#### 後端工作

**B-S2-1. 提供 Wave 5 page bootstrap context**

選項 A：在 `ReviewCaseDetailView` 新增 actor-aware computed 欄位：

```java
boolean hasDraft;              // actor 在此 case 的 DRAFT submission
boolean hasSubmittedBase;      // actor 在此 case 的 SUBMITTED submission
boolean isClosedRevisionException;  // CLOSED + actor==originalRereviewer + submitted rereview
String initialSubmissionId;    // 最新初審 submission ID（rereview page 導覽用）
String initialReviewerName;    // 供複審頁顯示
String initialTotalScore;      // 供複審頁顯示
```

選項 B（更乾淨）：定義專用端點：

```
GET /review-cases/{caseId}/review-context    → InitialReviewContextView
GET /review-cases/{caseId}/rereview-context  → RereviewContextView
```

**B-S2-2. `GetReviewCaseDetailUseCase` 加入 SYSTEM_ADMIN projection masking**

```java
// 若 actor.hasRole(SYSTEM_ADMIN) → 返回 metadata-only 欄位集合
// 遵循 api-spec.md §3.3 whitelist
```

#### 前端工作

**F-S2-1. `InitialReviewPageComponent.ngOnInit()` 替換 `createDefaultContext()` → service call**

```typescript
this.casesService.getInitialReviewContext(this.caseId).subscribe(ctx => {
  this.applyContext(ctx);
});
```

**F-S2-2. `RereviewPageComponent.ngOnInit()` 同上**

#### Slice 2 預期產出

- Wave 5 `InitialReviewPageComponent` 可從真實 context 解析 mode（NEW / EDIT_DRAFT / REVISION / READONLY_BLOCKED）
- Wave 5 `RereviewPageComponent` 可解析 mode（包含 CLOSED_REVISION_EXCEPTION）
- Wave 5 command endpoints 可接通（button action → service → HTTP → backend command use case）

---

### Slice 3 — 後期（polish / production readiness）

| 項目 | 說明 | 優先度 |
|-----|------|--------|
| `StubCandidateSourceAdapter` 替換 | 接通真實 HIS upstream | Production gate |
| Route Guard 啟動 | RoleGuard / ScopeGuard / ProjectionGuard 實作 | Shell 整合後 |
| B3 command objects 補齊 | AssignReviewCaseCommand 等 | Consistency（non-blocking） |
| Reporting / Dashboard 接通 | DashboardQueryService 已 real，Angular service 未接通 | 獨立 wave |
| Export 功能接通 | ExportQueryService 已存在，Angular 未接通 | 獨立 wave |

---

### Contract 釐清事項（Slice 1 實作前需決策）

| # | 問題 | 選項 | 影響 |
|---|-----|-----|-----|
| C-1 | 由誰計算 `projectionType`？ | A: backend 計算並回傳；B: frontend 從 actor role 推導 | A 為 single source of truth，推薦 A |
| C-2 | 由誰計算 `mode`/`allowedActions`？ | A: backend enrichment；B: frontend 從 status+role 計算 | A 避免 permission matrix 邏輯外漏到 Angular，推薦 A |
| C-3 | Revision history DTO：flat 還是 by-stage 結構？ | A: flat，含 `stage` 欄位；B: `{ initialEntries, rereviewEntries }` | B 與前端 stageTab 設計對齊，但需確認 controller 端 |
| C-4 | Wave 5 bootstrap：擴充 DetailView 還是新端點？ | A: 欄位直接加入 DetailView；B: 專用 context 端點 | B 語意更清晰（page bootstrap vs. data query 分離） |
| C-5 | `ReviewCaseSummary` 是否也需 `projectionType`？ | A: 需要；B: 由 ActorContext 在 list page 計算 | CaseListPage 的 `showBusinessCommands` 目前由 projectionType 決定 |

#### 已確認的設計決策

以下決策已定，不需再重新討論：

1. **C1**：`projectionType` 由後端計算
2. **C2**：`actorRole` / `isOwner` / mode-related truth 由後端計算，不交給 frontend 推論
3. **C3**：Revision history 採 **flat DTO 結構**
4. **C4**：先擴充 `ReviewCaseDetailView`，不新增專用 context endpoint
5. **C5**：`ReviewCaseSummary` 也補 `projectionType`

---

### 最終建議

**最高槓桿的單一 backend closure**：

> 在 `GetReviewCaseDetailUseCase.execute()` 中，根據 actor context 計算並填入 `projectionType`、`actorRole`、`isOwner` 到 `ReviewCaseDetailView`，並將 `GetRevisionHistoryUseCase` 改為返回 `RevisionHistoryView` DTO（含 `hasExceptionEntry`）。

此單一改動可：
1. 解鎖 Wave 3 `CaseDetailPageComponent` 4-mode resolution（無需前端自行計算 permission matrix）
2. 解鎖 Wave 3 `RevisionHistoryPageComponent` METADATA_HISTORY mode 與 exception entry 偵測
3. 提供 Wave 5 page bootstrap 的基礎（`isOwner` + `status` 可推導 `hasDraft` 需求）
4. 避免將 permission matrix 業務邏輯複製到 Angular frontend

Wave 4 HTTP wiring（fixture → HttpClient）是 Angular-only 的快速 win，可在後端 Slice 1 工作的同時平行推進。

---

*Evidence sources: source code reading 2025-07（ReviewCaseController.java, ReviewCaseDetailView.java, ReviewCaseSummary.java, ReviewCaseQueryAdapter.java, GetReviewCaseDetailUseCase.java, GetRevisionHistoryUseCase.java, CaseDetailPageComponent.ts, CaseListPageComponent.ts, RevisionHistoryPageComponent.ts, InitialReviewPageComponent.ts, RereviewPageComponent.ts, CasesService.ts, SamplingService.ts, route guards, app-routing.module.ts, api-spec.md §3–12)*
