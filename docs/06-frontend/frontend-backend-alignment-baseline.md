# Frontend–Backend Alignment Baseline

> **文件定位**：本文件是 UI-006 ~ UI-014 的 **前後端對齊基線**（Authoritative Baseline）。  
> 所有結論均來自實際 source code 讀取，不依賴文件描述或假設。  
> 本文件取代 `wave-backend-dependency-inventory.md` 作為現行 wave-boundary 的 alignment truth。  
>
> 基線採集日期：2025-07（初版）→ 更新於本次掃描（see Git log）  
> Evidence 來源：source code scan（不含 docs 推論；docs 只作舊版對比參考）

---

## 1. 狀態定義

| 符號 | 狀態 | 定義 |
|------|------|------|
| ✅ REAL | 真實實作 | FE 呼叫 HttpClient 或 BE 有完整 controller → usecase → port 鏈結 |
| ⚠️ FIXTURE | Fixture 替身 | FE 使用 `of(hardcoded)` / 本地 array；無 HttpClient 呼叫 |
| ⚠️ PASSTHROUGH | 通過哨 | 邏輯存在但 `return true`；無實質驗證 |
| ⚠️ STUB-BACKED | Stub 資料來源 | 端點存在且 FE 已接通，但 BE 資料來源為 dev-only stub adapter |
| ❌ ABSENT | 缺失 | 端點不存在、use case 未建立或 FE 未消費 |

---

## 2. Evidence Inventory 摘要

### 2.1 Frontend Angular Services

| Service | 路徑 | HTTP 狀態 | Citation |
|---------|------|----------|----------|
| `CasesService` | `features/cases/services/cases.service.ts` | ✅ REAL — `HttpClient` 注入，所有方法呼叫真實端點 | L1-50 (imports), L450-800 (methods) |
| `SamplingService` | `features/sampling/services/sampling.service.ts` | ✅ REAL (partial) — `HttpClient` 注入；`getBatches()`/`previewSampling()`/`executeSampling()`/`retrySamplingBatch()` 均已接通真實端點；`getExecuteContext()` 仍為 fixture pending B-001 架構決策 | 本次 gap closure scan |

### 2.2 Frontend Route Guards

| Guard | 路徑 | 狀態 | Citation |
|-------|------|------|----------|
| `RoleGuard` | `guards/role.guard.ts` | ⚠️ PASSTHROUGH — `return true` | L6-8 |
| `ScopeGuard` | `guards/scope.guard.ts` | ⚠️ PASSTHROUGH — `return true` | L6-8 |
| `ProjectionGuard` | `guards/projection.guard.ts` | ⚠️ PASSTHROUGH — `return true` | L6-8 |

三個 Guard 均以 `canActivate(): boolean { return true }` 實作，並有 comment 標記「formal logic 實作於 post-Wave 1」。  
**根本原因**：Shell integration gate — 正式 actor context 由 MFE shell 注入；standalone mode 繞過。

### 2.3 Backend Controllers（確認存在）

| Controller | 路徑 | 端點 |
|-----------|------|------|
| `ReviewCaseController` | `interfaces/rest/ReviewCaseController.java` | GET /review-cases, GET /{caseId}, GET /{caseId}/assignment-context, GET /{caseId}/assignment-candidates, GET /{caseId}/revision-history, GET /{caseId}/lifecycle, POST /assign-reviewer, /reassign-reviewer, /assign-rereviewer, /reassign-rereviewer, /initial-review/start, /initial-review/draft, /initial-review/submit, /initial-review/submissions/{id}/revise, /start-rereview, /rereview/draft, /rereview/submit, /rereview/submissions/{id}/revise, /close-without-rereview, /send-to-rereview, /cancel, /initial-review/return, /reopen |
| `SamplingController` | `interfaces/rest/SamplingController.java` | GET /sampling-batches, GET /sampling-batches/{batchId}, GET /sampling-batches/{batchId}/export, POST /review-projects/{projectId}/sampling/preview, POST /review-projects/{projectId}/sampling/execute, POST /sampling-batches/{batchId}/retry |
| `SubmissionController` | `interfaces/rest/SubmissionController.java` | GET /review-submissions/{submissionId} |

### 2.4 Key Capability Fields（Backend 端計算結果）

| 欄位 | 計算位置 | Citation |
|------|---------|----------|
| `projectionType` (list) | `ListReviewCasesUseCase.execute()` — `SYSTEM_ADMIN` → `METADATA_ONLY`，其餘 → `FULL` | `ListReviewCasesUseCase.java:31-35` |
| `projectionType` (detail) | `GetReviewCaseDetailUseCase.execute()` — 同上邏輯 | `GetReviewCaseDetailUseCase.java:38` |
| `actorRole` | `GetReviewCaseDetailUseCase.execute()` — 依 DomainRole priority 計算字串 | `GetReviewCaseDetailUseCase.java:40-47` |
| `isOwner` | `GetReviewCaseDetailUseCase.execute()` — `userId == reviewerId OR rereviewerId` | `GetReviewCaseDetailUseCase.java:49` |
| `initialReviewContext` | `GetReviewCaseDetailUseCase.buildInitialReviewContext()` — `pageMode`, `hasDraft`, `allowedActions`, `blockedReason` 等 | `GetReviewCaseDetailUseCase.java:54-97` |
| `rereviewContext` | `GetReviewCaseDetailUseCase.buildRereviewContext()` — 包含 `isClosedRevisionException` | `GetReviewCaseDetailUseCase.java:99-155` |
| `hasExceptionEntry` | `GetRevisionHistoryUseCase` → `RevisionHistoryView.isHasExceptionEntry()` → `RevisionHistoryResponse` constructor 第三參數 | `ReviewCaseController.java:284-295` |

### 2.5 E2E Test Strategy

| 檔案 | 策略 | 覆蓋 |
|------|------|------|
| `e2e/src/app.e2e.spec.ts` | Playwright route-fulfill mock | UI-009, UI-010, UI-014, UI-006, UI-007 smoke |
| `e2e/src/wave5-routes.e2e.spec.ts` | Playwright route-fulfill mock | UI-011 (assignment ASSIGN/REASSIGN/MIXED modes), UI-012 (initial-review revision) |

兩份 E2E 均使用 route-fulfill 攔截，**非真實 backend 整合**。  
Wave 5 E2E mock 的 shape 已對齊目前 BE 回應結構（`projectionType`, `actorRole`, `isOwner`, `initialReviewContext`, `hasExceptionEntry` 均出現在 mock body）。

---

## 3. 前後端 Alignment 矩陣

### 3.1 Sampling Cluster

#### UI-006 — 抽選執行頁（`/sampling/:projectId`）

| 能力項目 | FE 端 | BE 端 | 對齊狀態 | Gap 分類 |
|---------|-------|-------|----------|---------|
| 載入執行 context | `samplingService.getExecuteContext(projectId)` → `of({...})` fixture（TODO B-001）| `GET /review-projects/{projectId}` 或衍生端點（**仍待架構決策**） | ❌ MISALIGNED | **Blocker** — B-001 pending user decision |
| 預估母體（preview） | `openPreviewStub()` → 呼叫 `samplingService.previewSampling(projectId)` → `POST /review-projects/{projectId}/sampling/preview` ✅ REAL | `POST /review-projects/{projectId}/sampling/preview` ✅ REAL | ✅ ALIGNED | — |
| 執行抽選 | `onConfirmModalConfirm()` → 呼叫 `samplingService.executeSampling(projectId)` → `POST /review-projects/{projectId}/sampling/execute` ✅ REAL | `POST /review-projects/{projectId}/sampling/execute` ✅ REAL | ✅ ALIGNED | — |
| 模式切換（6 modes） | `resolveMode(context)` → 依 context 欄位計算，邏輯正確 | N/A（純前端 mode 計算） | ✅ N/A | Documentation Debt |

**UI-006 整體狀態：PARTIAL WIRED**  
Preview/Execute 命令已接通真實 HTTP（B-002/B-003 CLOSED）。  
**剩餘 gap**：`getExecuteContext()` 仍為 fixture — `getExecuteContext` 對應的 backend 端點不存在，需架構決策後補齊（B-001 pending user decision）。

---

#### UI-007 — 抽選批次列表頁（`/sampling/batches`）

| 能力項目 | FE 端 | BE 端 | 對齊狀態 | Gap 分類 |
|---------|-------|-------|----------|---------|
| 批次列表資料 | `samplingService.getBatches(criteria)` → `GET /sampling-batches` ✅ REAL；返回 `SamplingBatchListResult` | `GET /sampling-batches?projectId&status&page&size` ✅ REAL | ✅ ALIGNED | — |
| 批次詳情（查看詳情） | no-op stub — `onViewDetailStub()` 維持 no-op（F-002 pending user decision） | `GET /sampling-batches/{batchId}` ✅ REAL | ⚠️ MISALIGNED | Functional Gap — F-002 pending user decision |
| 重跑批次（retry） | `confirmRetry(batchId)` → 呼叫 `samplingService.retrySamplingBatch(batchId)` → `POST /sampling-batches/{batchId}/retry` ✅ REAL | `POST /sampling-batches/{batchId}/retry` ✅ REAL | ✅ ALIGNED | — |

**UI-007 整體狀態：PARTIAL WIRED**  
`getBatches()` 已接通真實 HTTP（B-004 CLOSED）；retry 命令已接通（F-001 CLOSED）。  
**剩餘 gap**：F-002（批次詳情路由）pending user decision。

---

### 3.2 Cases Cluster

#### UI-009 — 案件列表頁（`/cases`）

| 能力項目 | FE 端 | BE 端 | 對齊狀態 | Gap 分類 |
|---------|-------|-------|----------|---------|
| 案件列表資料 | `casesService.getCases(criteria)` → `GET /review-cases` ✅ REAL | `GET /review-cases` → `ListReviewCasesUseCase` ✅ REAL | ✅ ALIGNED | — |
| `projectionType` | `CaseSummaryItem.projectionType` → mapped from `ReviewCaseSummary.projectionType` | `ListReviewCasesUseCase` 計算並注入 `projectionType` ✅ | ✅ ALIGNED | — |
| Role-based tab / filter | `CaseListPageComponent.role` — 目前 **hardcoded** `'REVIEWER'` | N/A（shell 應注入） | ⚠️ Integration Debt | Shell integration gate |
| `showBusinessCommands` | `projectionType === 'FULL'` 計算 | `projectionType` 由 BE 提供 | ✅ ALIGNED | — |
| 分頁 / filter params | HttpParams 建構 → `status`, `reviewerId`, `projectId`, `page`, `size` | `ReviewCaseQueryCriteria` 對應欄位 | ✅ ALIGNED | — |
| Route guards | `RoleGuard + ScopeGuard + ProjectionGuard` canActivate | — | ⚠️ PASSTHROUGH | Integration Debt — shell gate |

**UI-009 整體狀態：WIRED**  
FE 已接通真實 HTTP，`projectionType` 對齊。  
唯一遞延：`role` hardcoded `'REVIEWER'`（應由 shell context 注入）。

---

#### UI-010 — 案件詳情頁（`/cases/:caseId`）

| 能力項目 | FE 端 | BE 端 | 對齊狀態 | Gap 分類 |
|---------|-------|-------|----------|---------|
| 案件詳情資料 | `casesService.getCaseDetail(caseId)` → `GET /review-cases/{caseId}` ✅ REAL | `GetReviewCaseDetailUseCase` → `ReviewCaseDetailView.enrich(...)` ✅ REAL | ✅ ALIGNED | — |
| `projectionType` | `CaseDetailViewModel.projectionType` ← mapped from API | `GetReviewCaseDetailUseCase` 計算並 enrich ✅ | ✅ ALIGNED | — |
| `actorRole` | `CaseDetailViewModel.actorRole` ← mapped from API `ReviewCaseDetailDto.actorRole` | `GetReviewCaseDetailUseCase` 依 DomainRole priority 計算 ✅ | ✅ ALIGNED | — |
| `isOwner` | `CaseDetailViewModel.isOwner` ← mapped from API | `GetReviewCaseDetailUseCase` 計算 ✅ | ✅ ALIGNED | — |
| `initialReviewContext` | `CaseDetailViewModel.initialReviewContext` ← mapped from `InitialReviewBootstrapDto` | `GetReviewCaseDetailUseCase.buildInitialReviewContext()` ✅ | ✅ ALIGNED | — |
| `rereviewContext` | `CaseDetailViewModel.rereviewContext` ← mapped from `RereviewBootstrapDto` | `GetReviewCaseDetailUseCase.buildRereviewContext()` ✅ | ✅ ALIGNED | — |
| 4-mode resolution | `CaseDetailPageComponent._resolveMode()` | 由 BE 提供 `projectionType`, `actorRole`, `isOwner` | ✅ ALIGNED | — |
| 導航 CTA（無 form submit） | navigate-only（`router.navigate`） | N/A | ✅ N/A | — |

**UI-010 整體狀態：WIRED**  
所有 key capability 欄位均已對齊；FE mapper 已實作（`mapCaseDetail`）。

---

#### UI-011 — 案件分派頁（`/cases/:caseId/assign`）

| 能力項目 | FE 端 | BE 端 | 對齊狀態 | Gap 分類 |
|---------|-------|-------|----------|---------|
| Assignment context bootstrap | `casesService.getAssignmentContext(caseId)` → `GET /review-cases/{caseId}/assignment-context` ✅ REAL | `GetCaseAssignmentContextUseCase` ✅ REAL | ✅ ALIGNED | — |
| 候選人列表（reviewer） | `casesService.getAssignmentCandidates(caseId, 'REVIEWER', keyword)` ✅ REAL | `ListCaseAssignmentCandidatesUseCase` → `StubAssignmentCandidateLookupAdapter` ⚠️ STUB-BACKED | ⚠️ STUB-BACKED | Integration Debt — dev-only stub adapter |
| 候選人列表（rereviewer） | 同上，role = `'REREVIEWER'` ✅ REAL | 同上 ⚠️ STUB-BACKED | ⚠️ STUB-BACKED | Integration Debt |
| `pageMode` 4 modes | `CaseAssignmentPageComponent.resolveMode()` ← from `AssignmentContextViewModel.pageMode` | `AssignmentBootstrapView.pageMode` ✅ REAL | ✅ ALIGNED | — |
| 指派初審醫師 | `casesService.assignReviewer(caseId, payload)` → `POST /review-cases/{caseId}/assign-reviewer` ✅ REAL | `AssignReviewCaseUseCase` ✅ REAL | ✅ ALIGNED | — |
| 重新指派初審醫師 | `casesService.reassignReviewer(caseId, payload)` ✅ REAL | `ReassignReviewCaseUseCase` ✅ REAL | ✅ ALIGNED | — |
| 指派複審醫師 | `casesService.assignRereviewer(caseId, payload)` ✅ REAL | `AssignRereviewerUseCase` ✅ REAL | ✅ ALIGNED | — |
| 重新指派複審醫師 | `casesService.reassignRereviewer(caseId, payload)` ✅ REAL | `ReassignRereviewerUseCase` ✅ REAL | ✅ ALIGNED | — |
| `dueAt` required | `AssignReviewerPayload`, `AssignRereviewerPayload` 均含 `dueAt` | `AssignReviewerRequest.dueAt`, `AssignRereviewerRequest.dueAt` 均存在 | ✅ ALIGNED | — |

**UI-011 整體狀態：WIRED**（Integration Debt：候選人 stub）  
命令端全部 REAL；候選人端點合約正確，但資料來源為 `StubAssignmentCandidateLookupAdapter`（production 前需替換）。

---

#### UI-012 — 初審作業頁（`/cases/:caseId/initial-review`）

| 能力項目 | FE 端 | BE 端 | 對齊狀態 | Gap 分類 |
|---------|-------|-------|----------|---------|
| 頁面 bootstrap context | `casesService.getCaseDetail(caseId)` → `detail.initialReviewContext` ✅ REAL | `GetReviewCaseDetailUseCase.buildInitialReviewContext()` 嵌入 detail response ✅ REAL | ✅ ALIGNED | — |
| `pageMode` 4 modes（NEW / EDIT_DRAFT / REVISION / READONLY_BLOCKED） | `InitialReviewPageComponent.applyCaseDetail()` ← `bootstrap.pageMode` | `buildInitialReviewContext()` 計算 `pageMode` ✅ | ✅ ALIGNED | — |
| `allowedActions` | `bootstrap.allowedActions` | `resolveInitialAllowedActions(pageMode)` ✅ | ✅ ALIGNED | — |
| 載入 submission 詳情 | `casesService.getSubmissionDetail(submissionId)` → `GET /review-submissions/{id}` ✅ REAL | `SubmissionController` ✅ REAL | ✅ ALIGNED | — |
| 開始初審 | `casesService.startInitialReview(caseId)` → `POST /initial-review/start` ✅ REAL | `StartInitialReviewUseCase` ✅ REAL | ✅ ALIGNED | — |
| 儲存草稿 | `casesService.saveInitialReviewDraft(caseId, payload)` ✅ REAL | `SaveInitialReviewDraftUseCase` ✅ REAL | ✅ ALIGNED | — |
| 提交初審 | `casesService.submitInitialReview(caseId)` ✅ REAL | `SubmitInitialReviewUseCase` ✅ REAL | ✅ ALIGNED | — |
| 修正初審（REVISION mode） | `casesService.reviseInitialReview(caseId, submissionId, payload)` ✅ REAL | `ReviseInitialReviewUseCase` ✅ REAL | ✅ ALIGNED | — |

**UI-012 整體狀態：WIRED**  
Bootstrap context 嵌入 case detail 回應，無需獨立端點。所有命令對齊。

---

#### UI-013 — 複審作業頁（`/cases/:caseId/rereview`）

| 能力項目 | FE 端 | BE 端 | 對齊狀態 | Gap 分類 |
|---------|-------|-------|----------|---------|
| 頁面 bootstrap context | `casesService.getCaseDetail(caseId)` → `detail.rereviewContext` ✅ REAL | `GetReviewCaseDetailUseCase.buildRereviewContext()` 嵌入 detail ✅ REAL | ✅ ALIGNED | — |
| `pageMode` 5 modes（NEW / EDIT_DRAFT / REVISION / REVISION_EXCEPTION_CLOSED / READONLY_BLOCKED） | `RereviewPageComponent.applyCaseDetail()` ← `bootstrap.pageMode` | `buildRereviewContext()` 計算，含 `isClosedRevisionException` ✅ | ✅ ALIGNED | — |
| CLOSED revision exception | `RereviewMode = 'CLOSED_REVISION_EXCEPTION'` | `isClosedRevisionException = CLOSED + userId == rereviewerId + hasSubmittedBase` ✅ | ✅ ALIGNED | — |
| 開始複審 | `casesService.startRereview(caseId)` → `POST /start-rereview` ✅ REAL | `StartRereviewUseCase` ✅ REAL | ✅ ALIGNED | — |
| 儲存複審草稿 | `casesService.saveRereviewDraft(caseId, payload)` ✅ REAL | `SaveRereviewDraftUseCase` ✅ REAL | ✅ ALIGNED | — |
| 提交複審 | `casesService.submitRereview(caseId, payload)` ✅ REAL | `SubmitRereviewUseCase` ✅ REAL | ✅ ALIGNED | — |
| 修正複審（REVISION / CLOSED_EXCEPTION） | `casesService.reviseRereview(caseId, submissionId, payload)` ✅ REAL | `ReviseRereviewUseCase` ✅ REAL | ✅ ALIGNED | — |

**UI-013 整體狀態：WIRED**  
CLOSED revision exception 邏輯於 FE 與 BE 均已對齊。

---

#### UI-014 — Revision 歷程頁（`/cases/:caseId/revisions`）

| 能力項目 | FE 端 | BE 端 | 對齊狀態 | Gap 分類 |
|---------|-------|-------|----------|---------|
| 歷程資料 | `casesService.getRevisionHistory(caseId)` → `forkJoin([getCaseDetail, GET /revision-history])` ✅ REAL | `GET /review-cases/{caseId}/revision-history` → `GetRevisionHistoryUseCase` ✅ REAL | ✅ ALIGNED | — |
| `hasExceptionEntry` | `RevisionHistoryViewModel.hasExceptionEntry` ← mapped from `RevisionHistoryResponseDto.hasExceptionEntry` | `RevisionHistoryResponse(view.getCaseId(), dtos, view.isHasExceptionEntry())` ✅ REAL | ✅ ALIGNED | — |
| `actorRole`（tab recommendation） | `RevisionHistoryViewModel.actorRole` ← from case detail `actorRole` | `GetReviewCaseDetailUseCase` 計算 ✅ | ✅ ALIGNED | — |
| 3-mode（FULL_HISTORY / METADATA_HISTORY / READONLY_TERMINAL） | `RevisionHistoryPageComponent._resolveMode()` | `projectionType`, `actorRole` 由 BE 提供 | ✅ ALIGNED | — |
| C22 navigate-to-rereview（exception entry） | `hasExceptionEntry` ← true → 顯示 outlined warning button | `hasExceptionEntry` 由 `GetRevisionHistoryUseCase` 計算 ✅ | ✅ ALIGNED | — |

**UI-014 整體狀態：WIRED**  
所有 key capability 對齊；`hasExceptionEntry` 已在 BE 計算並在 FE 消費。

---

## 4. Key Capability Matrix（跨頁彙整）

| 欄位 | UI-006 | UI-007 | UI-009 | UI-010 | UI-011 | UI-012 | UI-013 | UI-014 |
|------|--------|--------|--------|--------|--------|--------|--------|--------|
| `projectionType` | N/A | N/A | ✅ ALIGNED | ✅ ALIGNED | N/A | N/A | N/A | ✅ ALIGNED (via detail) |
| `actorRole` | N/A | N/A | ⚠️ hardcoded role | ✅ ALIGNED | N/A | N/A | N/A | ✅ ALIGNED |
| `isOwner` | N/A | N/A | N/A | ✅ ALIGNED | N/A | N/A | N/A | N/A |
| `initialReviewContext` | N/A | N/A | N/A | ✅ ALIGNED | N/A | ✅ ALIGNED | N/A | N/A |
| `rereviewContext` | N/A | N/A | N/A | ✅ ALIGNED | N/A | N/A | ✅ ALIGNED | N/A |
| `hasExceptionEntry` | N/A | N/A | N/A | N/A | N/A | N/A | N/A | ✅ ALIGNED |
| `pageMode` (assignment) | N/A | N/A | N/A | N/A | ✅ ALIGNED | N/A | N/A | N/A |
| `pageMode` (initial-review) | N/A | N/A | N/A | N/A | N/A | ✅ ALIGNED | N/A | N/A |
| `pageMode` (rereview) | N/A | N/A | N/A | N/A | N/A | N/A | ✅ ALIGNED | N/A |
| Route Guard 授權 | ⚠️ PASSTHROUGH | ⚠️ PASSTHROUGH | ⚠️ PASSTHROUGH | ⚠️ PASSTHROUGH | ⚠️ PASSTHROUGH | ⚠️ PASSTHROUGH | ⚠️ PASSTHROUGH | ⚠️ PASSTHROUGH |

---

## 5. 與 wave-backend-dependency-inventory.md 的差異（舊版 vs 現況）

> 舊版文件採集基線：2025-07。以下逐條對比。

| 舊版描述 | 舊版判斷 | 現況判斷 | 變化原因 |
|---------|---------|---------|---------|
| Wave 3：`CasesService` 三個方法均為 `of(null)` / `of([])` stub | ❌ STUBBED | ✅ REAL | 已換成 HttpClient 實作 |
| Wave 3：`projectionType` / `actorRole` / `isOwner` 欄位不存在於 BE | ❌ GAP | ✅ ALIGNED | `GetReviewCaseDetailUseCase` 和 `ListReviewCasesUseCase` 均已計算並返回 |
| Wave 3：`hasExceptionEntry` 欄位不存在 | ❌ GAP | ✅ ALIGNED | `GetRevisionHistoryUseCase` 已計算；`RevisionHistoryResponse` 含第三參數 |
| Wave 3：revision history 返回 domain objects | ❌ DESIGN GAP | ✅ RESOLVED | `ReviewCaseController` 已將 `RevisionHistoryView` 轉成 `RevisionHistoryResponse` DTO |
| Wave 3：SYSTEM_ADMIN projection 未實作 | ❌ GAP | ✅ ALIGNED | `ListReviewCasesUseCase` + `GetReviewCaseDetailUseCase` 均依 `DomainRole.SYSTEM_ADMIN` 設定 `projectionType` |
| Wave 5：UI-012 / UI-013 bootstrap context「無對應 backend 端點」 | ❌ GAP | ✅ ALIGNED | Bootstrap context 嵌入 `GetReviewCaseDetailUseCase` response（`initialReviewContext` / `rereviewContext`） |
| Wave 5：UI-012 / UI-013 component 無 service call | ❌ NO CALL | ✅ REAL | 兩個 component 均呼叫 `casesService.getCaseDetail()` 取得 bootstrap |
| Wave 4：UI-006 / UI-007 SamplingService 為 fixture | ⚠️ FIXTURE | ⚠️ FIXTURE | **不變** — SamplingService 仍為 fixture，尚未接通 |
| Wave 4：`SamplingExecuteContext` 無對應 BE 端點 | ⚠️ 待確認 | ❌ ABSENT | `SamplingController` 無 getExecuteContext 端點；仍為 gap |

---

## 6. Gap 分類清單

### ✅ Closed（本次 gap closure cycle 關閉）

| ID | Page | 描述 | 關閉方式 |
|----|------|------|---------|
| B-002 | UI-006 | `openPreviewStub()` 已替換為呼叫 `samplingService.previewSampling(projectId)` | FE code change |
| B-003 | UI-006 | `onConfirmModalConfirm()` 已替換為呼叫 `samplingService.executeSampling(projectId)`，含 success/error handler | FE code change |
| B-004 | UI-007 | `SamplingService.getBatches()` 已替換為呼叫 `GET /sampling-batches`，返回 `SamplingBatchListResult` | FE code change |
| F-001 | UI-007 | `confirmRetry()` 已替換為呼叫 `samplingService.retrySamplingBatch(batchId)`，成功後 reload list | FE code change |

### Blocker（仍開放 — 待決策）

| ID | Page | 描述 | 影響 |
|----|------|------|------|
| B-001 | UI-006 | `SamplingService.getExecuteContext()` 仍為 fixture；`SamplingController` 無對應端點；需架構決策（選項 A: 新增 BE 端點 / 選項 B: FE 組合查詢） | 執行頁 projectName/hospitalId/blockedReason 無法取得真實資料 |

### Functional Gap（仍開放 — 待決策）

| ID | Page | 描述 | 影響 |
|----|------|------|------|
| F-002 | UI-007 | 查看詳情為 no-op stub；`GET /sampling-batches/{batchId}` 端點已就緒但無 routing；待 UI-008 規格或路由方案決定 | 批次詳情無法顯示 |

### Integration Debt（合約正確，資料或整合層待補）

| ID | Page | 描述 | 影響 |
|----|------|------|------|
| I-001 | All | 三個 Route Guard 均為 PASSTHROUGH；role / scope / projection 授權不執行 | Shell 整合完成前無法強制授權 |
| I-002 | UI-009, 010 | `CaseListPageComponent.role` hardcoded `'REVIEWER'`；正式需由 shell context 注入 | 其他 role 的 tab/filter default 不正確 |
| I-003 | UI-011 | `StubAssignmentCandidateLookupAdapter` 為 dev-only；assignment candidate 資料為合成 | Production 前必須替換為真實 HIS 資料來源 |

### Validation Gap（邏輯已對齊，但缺乏整合測試覆蓋）

| ID | Page | 描述 |
|----|------|------|
| V-001 | UI-009~014 | E2E 使用 route-fulfill mock，非真實 backend integration test；BE 邏輯正確性未被 E2E 驗證 |
| V-002 | UI-012/013 | `blockedReason` 種類（ROLE_DENIED、NOT_ASSIGNED_REVIEWER 等）未被前端 E2E 全部覆蓋 |
| V-003 | UI-014 | `hasExceptionEntry = true` 的完整路徑（CLOSED + originalRereviewer + baseSubmission）未有整合測試 |

### Documentation Debt（實作正確，文件未更新）

| ID | 描述 |
|----|------|
| D-001 | `wave-backend-dependency-inventory.md` 描述 Wave 3/5 gap 均已不再成立，但文件未更新 — 本文件取代之 |
| D-002 | Wave 5 E2E spec 中 `pageMode = 'REVISION_EXCEPTION_CLOSED'` vs BE 計算的 `'REVISION_EXCEPTION_CLOSED'` — 字串對齊，無問題；但 api-spec.md 中的描述需確認一致 |

---

## 7. Formal Closure Statement

| Page | Alignment 狀態 | 接通前置條件 |
|------|--------------|-------------|
| UI-006 | **PARTIAL WIRED** — Preview/Execute 命令已接通（B-002/B-003 CLOSED）；`getExecuteContext()` 仍為 fixture | B-001 (pending decision) |
| UI-007 | **PARTIAL WIRED** — 批次列表/retry 已接通（B-004/F-001 CLOSED）；批次詳情 no-op | F-002 (pending decision) |
| UI-009 | **WIRED** — `projectionType` 對齊；shell 注入 role 後 I-001/I-002 可關閉 | I-001, I-002 |
| UI-010 | **WIRED** — 所有 key capability 對齊；shell 整合後可關閉 I-001 | I-001 |
| UI-011 | **WIRED** — 合約對齊；I-003 為 pre-production 必填 | I-001, I-003 |
| UI-012 | **WIRED** — bootstrap 嵌入 detail，所有命令對齊 | I-001 |
| UI-013 | **WIRED** — CLOSED exception 邏輯已對齊 | I-001 |
| UI-014 | **WIRED** — `hasExceptionEntry` 完全對齊 | I-001 |

---

*本文件由 source code scan 產生。如需更新，請重新掃描對應 service / controller / usecase 並更新本節的 Citation 欄位。*
