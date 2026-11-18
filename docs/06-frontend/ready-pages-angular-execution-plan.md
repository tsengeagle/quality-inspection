# Ready Pages — Angular Frontend Implementation Execution Plan

> D-032 規格變更（2026-11-18 補註）：下文涉及 O1-C、CLOSED 例外修正入口（UI-014 C22「前往複審修正（例外）」、`hasExceptionEntry`、UI-013 `CLOSED_REVISION_EXCEPTION` 模式及其 amber 提示）的內容，已被 D-032 取代，僅保留歷史依據，不作現行可實作規則。CLOSED 不得 revision／reopen；UI-013、UI-014 在 CLOSED 只顯示唯讀提示。應用程式已對齊：UI-013 CLOSED 唯讀（PR #39）；UI-014 移除 `hasExceptionEntry` 與 C22 例外入口（PR #83）。C22 的其他導頁設計（例如返回案件詳情）不在 D-032 撤銷範圍，目前應用程式沒有實作 C22。

> **文件定位**：本文件是 `ready-pages-implementation-handoff.md` 與 `p2-implementation-handoff.md` 的
> **Angular 執行計畫轉化**。它不重新定義設計決策，而是將已固定的 page truth 轉換成
> Angular 前端可直接執行的任務邊界、架構骨架、STUB/deferred 分割與測試先行順序。
>
> 上游 authority 衝突時，以 `ready-pages-implementation-handoff.md` 為主。
> 本文件不是 implementation 過程中的自行添加／降級 deferred 的授權。

---

## 1. 文件目的

本文件回答以下 8 個 Angular 實作問題：

1. **Feature module 邊界**：哪些頁面在同一個 lazy-loaded module？哪些必須隔離？
2. **容器 / 呈現元件邊界**：Container 持有什麼？Presentational 接收哪些 input？
3. **Shared component 重用**：哪些跨頁 shared component 需要先建立？
4. **Action / navigate 邊界**：哪些 CTA 必須實作？哪些是 STUB？
5. **Placeholder region**：哪些 region 只需佔位，不需實作互動細節？
6. **Deferred 邊界**：哪些中欄不在本輪實作範圍？
7. **Test-first 順序**：每頁的測試先行優先序為何？
8. **Pending confirmation**：哪些問題需先確認才能開始實作？

---

## 2. Authority Sources 來源表

| 優先度 | 文件 | 用途 |
|---|---|---|
| 1 | `docs/01-requirements/decision-log.md` | §d089 O1-C exception、§d095 O3-C projection、D-013 retry text |
| 2 | `docs/02-ddd/state-machine.md` | case status transitions、terminal state |
| 3 | `docs/02-ddd/permission-matrix.md` | role-to-action mapping |
| 4 | `docs/03-contracts/api-spec.md` | endpoint contracts、query params |
| 5 | `docs/06-frontend/ready-pages-implementation-handoff.md` | ★ 主要 handoff authority（8 頁） |
| 6 | `docs/06-frontend/p2-implementation-handoff.md` | UI-009/011/012 詳細規格 |
| 7 | `docs/06-frontend/sampling-route-module-map-addendum.md` | Sampling cluster routing 補充（BLOCKING resolved） |
| 8 | `docs/06-frontend/ui-route-module-map.md` | Cases cluster route/module/guard mapping |
| 9 | `docs/07-design/ui-visual-baseline.md` | P1 C01~C04 shared region 渲染順序與 tone 基線 |
| 10 | `docs/07-design/wireframes/` | 各頁 first-pass wireframe（形狀依據） |
| 11 | `docs/06-frontend/ui-component-map.md` | C01~C22 component 分類與 ownership |
| 12 | `docs/05-ui/ui-layout-archetypes.md` | A1~A5 / SA1/SA2 archetype 定義 |

---

## 3. Ready Pages Inventory

| Page ID | 頁面 | 叢集 | Archetype | Route | Feature Module | 本文位置 | Main Caution |
|---|---|---|---|---|---|---|---|
| UI-006 | 抽選執行頁 | Sampling | SA1 Action/Execute | `/sampling/:projectId` | `features/sampling/sampling-execute/` | §5.1 | 6 modes 不可做成 6 routes |
| UI-007 | 抽選批次列表頁 | Sampling | SA2 List/Tracking | `/sampling/batches` | `features/sampling/batch-list/` | §5.2 | `[查看詳情]` 全程 STUB |
| UI-009 | 案件列表頁 | Cases | A1 Work Queue | `/cases` | `features/cases/cases-worklist/` | §5.3 | tab ≠ sub-page；metadata-only ≠ 獨立 route |
| UI-010 | 案件詳情頁 | Cases | A2 Detail/Control Center | `/cases/:caseId` | `features/cases/case-detail/` | §5.4 | 純 navigate-only；不得有任何 form submit |
| UI-011 | 案件分派頁 | Cases | A3 Action/Assignment | `/cases/:caseId/assign` | `features/cases/cases-assignment-command/` | §5.5 | reviewer/rereviewer section 必須實體分開 |
| UI-012 | 初審作業頁 | Cases | A4 Action/Review Form | `/cases/:caseId/initial-review` | `features/cases/cases-initial-review/` | §5.6 | REVISION ≠ edit existing；C16 不可移動 |
| UI-013 | 複審作業頁 | Cases | A4 Action/Review Form | `/cases/:caseId/rereview` | `features/cases/cases-rereview/` | §5.7 | CLOSED exception ≠ READONLY_BLOCKED |
| UI-014 | Revision 歷程頁 | Cases | A5 History/Traceability | `/cases/:caseId/revisions` | `features/cases/revision-history/` | §5.8 | C22 = navigate-only；不得有 fixed bottom bar |

**明確排除**：UI-008（page spec 不存在；[查看詳情] 全程 STUB；route 不得預留）。

---

## 4. Cross-Page Implementation Strategy

### 4.1 Feature Module Strategy

#### 4.1.1 叢集隔離規則（硬規則）

```
features/
  sampling/                ← Sampling 叢集（UI-006, UI-007）
    sampling-execute/      ← UI-006
    batch-list/            ← UI-007
  cases/                   ← Cases 叢集（UI-009~UI-014）
    cases-worklist/        ← UI-009
    case-detail/           ← UI-010
    cases-assignment-command/ ← UI-011
    cases-initial-review/  ← UI-012
    cases-rereview/        ← UI-013
    revision-history/      ← UI-014
```

**硬規則**：  
- `features/sampling/` 與 `features/cases/` **必須完全隔離**；不可互相引用 service。  
- 唯一合法的跨叢集溝通是 Angular Router navigate：UI-007 → `/cases?batchId={batchId}`。  
- 不可在 cases module 中引用任何 sampling service；反之亦然。

#### 4.1.2 `app-routing.module.ts` 兩個 lazy-load entries

```typescript
// app-routing.module.ts — 在現有 { path: '', redirectTo: 'shell' } 之後新增：
{
  path: 'sampling',
  loadChildren: () =>
    import('./features/sampling/sampling.module').then(m => m.SamplingModule)
},
{
  path: 'cases',
  loadChildren: () =>
    import('./features/cases/cases.module').then(m => m.CasesModule)
}
```

#### 4.1.3 SamplingRoutingModule child route 順序（必須遵守）

```typescript
// SamplingRoutingModule：batches literal 必須在 :projectId wildcard 之前
const routes: Routes = [
  { path: 'batches', component: SamplingBatchListPageComponent },   // ← 先 literal
  { path: ':projectId', component: SamplingExecutePageComponent },  // ← 再 wildcard
  { path: '', redirectTo: 'batches', pathMatch: 'full' }            // optional default
];
```

**原因**：若 `':projectId'` 在前，`/sampling/batches` 會被誤判為 `projectId='batches'`。

#### 4.1.4 CasesRoutingModule routes

```typescript
const routes: Routes = [
  { path: '',                   component: CaseListPageComponent },
  { path: ':caseId',            component: CaseDetailPageComponent },
  { path: ':caseId/assign',     component: CaseAssignmentPageComponent },
  { path: ':caseId/initial-review', component: InitialReviewPageComponent },
  { path: ':caseId/rereview',   component: RereviewPageComponent },
  { path: ':caseId/revisions',  component: RevisionHistoryPageComponent }
];
```

---

### 4.2 Shared vs Feature-Scoped Component 策略

#### Cases 叢集 Shared Components（C01~C04）

這些 component 必須**先建立**，因為所有 Cases 頁面均依賴。

| Component | ID | 使用頁 | 渲染順序 |
|---|---|---|---|
| `CaseSummaryHeaderComponent` | C01 | UI-009~014（全部） | 固定頂部，始終顯示 |
| `StatusOrBlockedBannerComponent` | C02 | UI-010~014 | 在 C01 之後，C03 之前；條件出現 |
| `ModeIndicatorStripComponent` | C03 | UI-009~014 | 在 C02 之後；始終顯示 |
| `ProjectionNoticeBannerComponent` | C04 | UI-009/010/014 | 嵌入 C01 inline；不獨立 banner row |

**C01~C04 渲染順序（固定，來自 ui-visual-baseline.md §5.1）**：
```
C01（含 C04 inline） → C02（條件） → C03
```

#### A4 家族 Shared Components（C15/C16/C17）

這些 component 被 UI-012 和 UI-013 共用，**在實作 UI-012/013 前先建立**。

| Component | ID | 使用頁 | 規則 |
|---|---|---|---|
| `ReviewFormSummaryStripComponent` | C15 | UI-012/013 | 接收 mode input；stateless |
| `ReviewCommandBarComponent` | C16 | UI-012/013 | **viewport-fixed bottom**；依 mode 切換 label；不可移位 |
| `ReviewVersionSummaryRegionComponent` | C17 | UI-012/013 | REVISION mode 顯示 basedOn reference |

#### Feature-Scoped Components

| 頁面 | Feature-scoped 範圍 | Component |
|---|---|---|
| UI-009 | `cases-worklist` | C05 CaseWorkQueueFilterBar / C06 CaseWorkQueueTable / C07 CaseRowActionSet |
| UI-010 | `case-detail` | C08 CaseControlCenterNavZone / C09 InitialReviewSummaryRegion / C10 RereviewSummaryRegion / C11 LifecycleProgressRegion |
| UI-011 | `cases-assignment-command` | C12 AssignmentModeBanner / C13 ReviewerAssignmentSection / C14 RereviewerAssignmentSection / C23（STUB slot） |
| UI-014 | `revision-history` | C20 RevisionTimelineRegion / C21 RevisionCompareSummaryRegion / C22 RevisionNavigationActionZone |

---

### 4.3 Route / Query State Strategy

| 政策 | 規則 |
|---|---|
| Tab state | 以 query param 呈現（`?tab=`）；tab ≠ sub-page component；tab = query preset |
| Worklist filter 保持 | `tab / filters / pagination / sort` 在 back navigation 時 restore |
| Mode 來源 | **永遠**由 container 計算（role + caseState + ownership + exceptionConditions）；**不從 URL param 讀取** |
| Cross-cluster navigate | UI-007 → UI-009：`router.navigate(['/cases'], { queryParams: { batchId } })` |
| UI-006→UI-007 navigate | `router.navigate(['/sampling/batches'])` （EXECUTE_SUCCESS 後） |
| UI-014→UI-013 navigate | `router.navigate(['/cases', caseId, 'rereview'])` （CLOSED exception entry）；navigate-only；不是 submit |
| STUB navigate | 不綁定任何推測 route；以 no-op handler 或 disabled 呈現 |

---

### 4.4 Guard 架構策略

#### Guard 分層（不可跳躍）

```
route-level:
  1. RoleGuard           ← 最外層：角色檢查
  2. ScopeGuard          ← hospitalId scope 比對
  3. ProjectionGuard     ← METADATA_ONLY vs FULL 決定

page-mode-level（container 內計算）:
  4. status check        ← case status 是否允許此頁
  5. ownership check     ← command 頁：REVIEWER/REREVIEWER ownership
  6. exception check     ← CLOSED exception 路徑條件（UI-013/014）

action-level:
  7. action_policy       ← per-action in-flight 檢查
```

**本輪 Guard 實作策略**：先以 passthrough 骨架建立 `RoleGuard` / `ScopeGuard` / `ProjectionGuard`；守衛邏輯於 API 整合時補完。UI-013 的 CLOSED exception 3 條件邏輯需實作於 route resolver 或 guard。

---

### 4.5 Must-Not-Invent Rules（禁止清單）

以下項目在本輪**絕對不得自行推斷或實作**：

| 禁止項目 | 原因 |
|---|---|
| UI-008 route / component / lazy-load entry | page spec 不存在；route 不得預留 |
| Shell nav entry key / MFE federation remote name | 無 repo evidence；shell integration 未定稿 |
| `[查看詳情]` 真實 route target（UI-007） | STUB 邊界；UI-008 unready |
| UI-006 EXECUTE_SUCCESS `[前往案件列表]` fixed batchId param | handoff 標記 provisional；未最終固定 |
| OQ-S01：4th preview stat card selectedCount（UI-006） | backend response 是否含此欄未確認 |
| OQ-S04：SYSADMIN SamplingBatch metadata 精確 whitelist（UI-007） | pending confirmation |
| OQ-S05：REVIEWER/REREVIEWER UI-007 access policy | pending confirmation；現行視為 deny |
| Candidate picker 深層互動規格（UI-011） | prototype 只確認 slot 位置；非 picker spec |
| Score sheet item-level form controls（UI-012/013） | 本輪 spec 範圍外 |
| Attachment upload/preview/delete 互動（UI-012/013） | 後續 component planning |
| C16 button label for `CLOSED_REVISION_EXCEPTION`（UI-013） | OQ §1；UX writing 未定 |
| D-013 retry modal 文案以外的任何 retry 語義說明（UI-007） | D-013 fixed text 是唯一 truth |
| Compare version selection UI 互動細節（UI-014） | OQ §3；interaction spec 未定 |
| C24 RevisionHistoryJumpEntry 置放位置（UI-010） | OQ §1；待 prototype 定案 |
| `EXECUTING` 動態 animation spec（UI-006） | 視覺 baseline 層；deferred |
| UI-006 6 modes → 6 independent routes | handoff 明確禁止 |

---

## 5. Per-Page Execution Plan

---

### 5.1 UI-006 抽選執行頁

#### A. Page Identity

| 項目 | 值 |
|---|---|
| Archetype | SA1 Action/Execute（Sampling 叢集專屬） |
| Route | `/sampling/:projectId` |
| Feature Module | `features/sampling/sampling-execute/` |
| Lazy-load boundary | SamplingModule |
| Guard | `RoleGuard`（HOSPITAL_MANAGER only）+ `ScopeGuard`（projectId scope） |

#### B. Fixed Truths（已固定，不可更改）

- 整個執行流程在**同一 page component** 切換 6 個 mode；**不可做成 6 個獨立 routes**。
- 6 modes：`READY_FOR_PREVIEW` / `PREVIEW_COMPLETED` / `EXECUTING` / `EXECUTE_SUCCESS` / `EXECUTE_FAILED` / `READONLY_BLOCKED`。
- `READONLY_BLOCKED` 有 4 類 blocked reason（project 非 ACTIVE、同版本已有 COMPLETED 批次、project CLOSED、permission revoked）；blocked reason banner **不可省略**。
- `SamplingExecuteConfirmZone` 的 confirm modal 是第二確認關卡；不可省略。
- `[前往抽選批次列表]` from `EXECUTE_SUCCESS` → `/sampling/batches`（FIXED）。

#### C. Angular Boundary

```
features/sampling/sampling-execute/
  SamplingExecutePageComponent     ← Container（owns mode, API calls, region visibility）
  SamplingRuleSummaryComponent     ← Presentational（mode + ruleParams inputs）
  SamplingPreviewStatsComponent    ← Presentational（previewStats input）
  SamplingExecuteConfirmModal      ← Modal（open: boolean input; confirmed/cancelled outputs）
  SamplingPostExecuteComponent     ← Presentational（batchId, timing inputs）
```

**Container** 持有：`SamplingExecuteMode` state、`ruleParams`、`previewStats`、`batchId`、`blockedReason`、`loading`、`error`。  
**Presentational** 一律 stateless，接收 input，emit 事件。

Mode state 接線：

```typescript
interface SamplingExecutePageState {
  mode: SamplingExecuteMode;
  ruleParams: SamplingRuleSnapshot | null;
  previewStats: SamplingPreviewStats | null;
  batchId: string | null;
  blockedReason: SamplingBlockedReason | null; // 4 categories
  loading: boolean;
  error: ApiError | null;
}
```

#### D. Implement Now

| 項目 | 基礎 |
|---|---|
| SA1 6-mode state machine（R1~R6 region 可視邏輯） | handoff §2.1.5 mode table |
| `READONLY_BLOCKED` 4 class reason banner | handoff §2.1.5 |
| `SamplingPreviewStatsRegion`（3 confirmed cards：populationCount / eligibleCount / excludedCount） | OQ-S01 resolved 前只做 3 cards |
| `SamplingExecuteConfirmModal`（structure；confirm/cancel flow） | modal 結構先做；copy = PLACEHOLDER |
| `SamplingPostExecuteRegion`（success 骨架；timing fields = STUB） | R6 success state 骨架 |
| Preview CTA → POST `/api/quality-inspection/sampling-batches/preview` → `PREVIEW_COMPLETED` | api-spec |
| Execute CTA → confirm modal → POST `/api/quality-inspection/sampling-batches` → `EXECUTING` → SUCCESS/FAILED | api-spec |
| `EXECUTE_FAILED`：[重新預估] re-enabled；前次 stats 保留 | mode grammar |
| `[前往抽選批次列表]` from `EXECUTE_SUCCESS` → `/sampling/batches` | FIXED cross-page navigate |

#### E. STUB Only

| 項目 | 說明 |
|---|---|
| `SamplingPageHeader`（header region） | sampling cluster component map 尚未建立；以 placeholder div/component 占位 |
| Post-execute timing fields（`startedAt` / `finishedAt`） | OQ-S02：欄位來源未確認 |
| `EXECUTE_SUCCESS` `[前往案件列表]` 的 batchId query param | handoff 標記 provisional；先以 `/cases` navigate（無 param） |

#### F. Deferred

| 項目 | 說明 |
|---|---|
| 4th preview stat card（selectedCount） | OQ-S01 resolved 後才實作 |
| `EXECUTING` animation / 進度轉場 | 視覺 baseline 層 |
| Confirm modal 精確文案 copy | UX writing 決定後替換 placeholder |

#### G. Test-First Notes

1. Route Guard：非 HOSPITAL_MANAGER → deny（redirect / 403）
2. Mode resolver 正確性：各 mode transition 驅動正確 region 可視性（特別是 READY→PREVIEW_COMPLETED→EXECUTING）
3. `READONLY_BLOCKED`：4 類 blocked reason 各別呈現正確 banner text
4. Preview CTA → `POST /preview` → stats 更新 → mode = `PREVIEW_COMPLETED`
5. Execute CTA → confirm modal → [確認] → `POST /sampling-batches` → `EXECUTING` → `EXECUTE_SUCCESS` / `EXECUTE_FAILED`
6. `EXECUTE_FAILED`：[重新預估] re-enabled；前次 previewStats 保留

#### H. Pending Confirmation

- **OQ-S01（HIGH，BLOCKING 4th card）**：`/sampling-batches/preview` response 是否含 `selectedCount`？
- `SamplingPageHeader` 精確 component API（需 sampling cluster component map）。

---

### 5.2 UI-007 抽選批次列表頁

#### A. Page Identity

| 項目 | 值 |
|---|---|
| Archetype | SA2 List/Execution Tracking（Sampling 叢集專屬） |
| Route | `/sampling/batches` |
| Feature Module | `features/sampling/batch-list/`（與 UI-006 共用 `SamplingModule`） |
| Lazy-load boundary | SamplingModule |
| Guard | `RoleGuard`（MANAGER / AUDITOR / SYSADMIN；REVIEWER/REREVIEWER = deny 現行）+ `ScopeGuard` |

#### B. Fixed Truths

- Role tab preset 在 component init 決定；不可讓 AUDITOR 看到 `[可重跑]` tab。
- SYSADMIN stats columns 採**保守 MASKED** 策略（OQ-S04 resolved 前）。
- `[重跑]` confirm modal text **必須符合 D-013 fixed semantic**：「將以新的抽選批次重跑，原失敗批次會保留」；不可更改語義。
- `[查看詳情]` 全程 STUB；不得綁任何推測 route。
- `[查看案件]` → `/cases?batchId={batchId}`（FIXED cross-cluster navigate）。

#### C. Angular Boundary

```
features/sampling/batch-list/
  SamplingBatchListPageComponent   ← Container（role, tab state, filter, data load）
  BatchStatusTabComponent          ← Presentational（tabs input; tabChange emit）
  BatchFilterBarComponent          ← Presentational（filters input; filterChange emit）
  BatchListTableComponent          ← Presentational（batches input; rowAction emit）
  BatchRowActionsComponent         ← Presentational（batch input; action emit）
  RetryConfirmModalComponent       ← Modal（batch: SamplingBatchSummary | null input; confirmed emit）
```

#### D. Implement Now

| 項目 | 基礎 |
|---|---|
| MANAGER tab preset：[全部批次] [成功] [失敗] [可重跑] | handoff §2.2.3 Role Tab Preset 矩陣 |
| AUDITOR tab preset：[全部批次] [成功] [失敗] | 矩陣 |
| SYSADMIN tab preset：[全部批次] [失敗] | 矩陣 |
| SYSADMIN stats columns MASKED（populationCount / eligibleCount / excludedCount / selectedCount） | O3-C projection + 保守做法 |
| MANAGER `[重跑]` row action → `RetryConfirmModal`（D-013 text） | D-013 FIXED |
| `[查看案件]` row action → `/cases?batchId={batchId}` | cross-cluster FIXED |
| Forbidden state（V6）/ Empty state（V4）/ Error state | ui-layout-archetypes |
| Paging zone（page / pageSize / totalCount） | standard |

#### E. STUB Only

| 項目 | 說明 |
|---|---|
| `[查看詳情]` row action | UI-008 page spec 不存在；action button 存在但 no-op（`console.warn` 或 disabled）；**不得寫死任何 route** |
| `BatchListPageHeader`（header region） | sampling cluster component map 尚未建立；placeholder |

#### F. Deferred

| 項目 | 說明 |
|---|---|
| 批次即時狀態 refresh / polling | api-spec 無 SSE；deferred |
| 匯出功能 | api-spec 無匯出 endpoint |
| `retryOfBatchId` chain display | 資料欄位未在 api-spec 確認 |
| SYSADMIN column 精確完整 whitelist | OQ-S04 resolved 後補完整欄位 |
| REVIEWER/REREVIEWER access 確認 | OQ-S05 pending |

#### G. Test-First Notes

1. Guard：REVIEWER/REREVIEWER（現行 deny）→ 403
2. Role → tab preset matrix：MANAGER 有 [可重跑]；AUDITOR 無；SYSADMIN 只有 [全部批次][失敗]
3. MANAGER FAILED row：[重跑] visible；confirm modal text 符合 D-013 FIXED 語意
4. SYSADMIN：stats columns MASKED；timing/metadata columns visible；無 [查看案件] / [重跑]
5. `[查看詳情]` → STUB placeholder（不測試真實 navigation）
6. `[查看案件]` → navigate to `/cases?batchId={batchId}`

#### H. Pending Confirmation

- **OQ-S04（HIGH）**：SYSADMIN SamplingBatch metadata whitelist 精確欄位清單。
- **OQ-S05（MID）**：REVIEWER/REREVIEWER 是否有合法訪問路徑。
- `BatchListPageHeader` component API（需 sampling cluster component map）。

---

### 5.3 UI-009 案件列表頁

#### A. Page Identity

| 項目 | 值 |
|---|---|
| Archetype | A1 Work Queue |
| Route | `/cases` |
| Feature Module | `features/cases/cases-worklist/` |
| Lazy-load boundary | cases-worklist boundary |
| Guard | `RoleGuard` + `ScopeGuard` + `ProjectionGuard` |

#### B. Fixed Truths

- 頁面是 **single-page work queue + role-default preset**；不是 dashboard，不是 A2 control center。
- `tab` = query param preset；tab ≠ sub-page component；tab 切換不做頁面導頁。
- `metadata-only`（SYSADMIN）= **同一 route 同一 projection**；不是獨立 route `/cases/meta`。
- 所有 action 均為 **row-level navigate-only**；無頁面級 command submit。
- Role-default tab preset 在 component init 決定。

#### C. Angular Boundary

```
features/cases/cases-worklist/
  CaseListPageComponent              ← Container（query state, role projection, data）
  CaseSummaryHeaderComponent         ← shared C01（list 版本骨架）
  ProjectionNoticeBannerComponent    ← shared C04（SYSADMIN 專有 inline in C01）
  ModeIndicatorStripComponent        ← shared C03
  CaseWorkQueueFilterBarComponent    ← feature-scoped C05
  CaseWorkQueueTableComponent        ← feature-scoped C06
  CaseRowActionSetComponent          ← feature-scoped C07
```

Container 持有：query state（tab / filters / pagination / sort）、role projection、data、loading / error。  
Presentational stateless；emit 事件（tabChange / filterChange / rowAction）。

#### D. Implement Now

| 項目 | 基礎 |
|---|---|
| Role-default tab preset（MANAGER / REVIEWER / REREVIEWER / AUDITOR / SYSADMIN） | ui-route-module-map §7 role entry table |
| O3-C metadata-only same-route projection（SYSADMIN） | O3-C decision；shared base + UI-009 extension whitelist |
| C04 SYSADMIN metadata-only projection inline indicator | C04 嵌入 C01 inline；不獨立 banner row |
| Row-level actions：navigate to detail / assign / initial-review / rereview / revisions | action visibility = role + case state + scope |
| Forbidden（C02）/ Empty / Error state in table zone | 標準 state handling |
| Query state persistence（tab/filters/sort/pagination in URL query） | ui-route-module-map §8 |
| Back navigation context restore | restore on back navigation = true |

#### E. STUB Only

| 項目 | 說明 |
|---|---|
| Bulk action / export | api-spec 無對應 endpoint |
| Advanced filter | 不在本輪 spec 範圍 |

#### F. Deferred

（本頁無明確 deferred 項目；bulk action 已列為 STUB）

#### G. Test-First Notes

1. Route Guard：scope mismatch → forbidden
2. SYSADMIN：O3-C metadata-only projection；C04 visible in C01；FULL-only fields absent
3. Role tab preset：MANAGER 看到 manager_default；REVIEWER 看到 reviewer_default preset
4. Action visibility：case status + role + ownership 正確驅動行動可見性
5. Row navigate：點擊案件 → `/cases/:caseId`
6. Tab 切換 = query param 更新，不是頁面跳轉

#### H. Pending Confirmation

- UI-009 SYSADMIN extension whitelist 精確欄位（O3-C shared base + UI-009 extension）。
- 詳見 `p2-implementation-handoff.md` §9.2。

---

### 5.4 UI-010 案件詳情頁

#### A. Page Identity

| 項目 | 值 |
|---|---|
| Archetype | A2 Detail/Control Center |
| Route | `/cases/:caseId` |
| Feature Module | `features/cases/case-detail/` |
| Lazy-load boundary | detail-center boundary |
| Guard | `RoleGuard` + `ScopeGuard` + `ProjectionGuard` |

#### B. Fixed Truths

- UI-010 是 **A2 control center**；不是 command page；**不得有任何 form 提交 / save / submit 行為**。
- 所有 CTA 均為 navigate action（前往 assign / initial-review / rereview / revisions）。
- `metadata-only`（SYSADMIN `METADATA_CONTROL_CENTER` mode）= 同一 route；不是獨立 route。
- CLOSED exception 的 revision 入口在 **UI-014**（C22），不在 UI-010；不可在 UI-010 新增 revision entry button。
- Modes：`FULL_CONTROL_CENTER` / `ROLE_LIMITED` / `METADATA_CONTROL_CENTER` / `READONLY_TERMINAL`。

#### C. Angular Boundary

```
features/cases/case-detail/
  CaseDetailPageComponent              ← Container（resolves mode from role+state）
  CaseSummaryHeaderComponent           ← shared C01
  StatusOrBlockedBannerComponent       ← shared C02（條件）
  ModeIndicatorStripComponent          ← shared C03
  ProjectionNoticeBannerComponent      ← shared C04（METADATA mode）
  CaseControlCenterNavComponent        ← feature-scoped C08（navigate CTAs）
  InitialReviewSummaryComponent        ← feature-scoped C09
  RereviewSummaryComponent             ← feature-scoped C10（條件）
  LifecycleProgressComponent           ← feature-scoped C11
  // C24 = DEFERRED placeholder（置放位置待 prototype 定案）
```

Container 持有：mode resolution（role + caseState）、case detail data、available navigate actions。  
無任何 form state；Container 不持有 command form。

ViewModel：

```typescript
interface CaseDetailViewModel {
  mode: CaseDetailMode; // FULL_CONTROL_CENTER | ROLE_LIMITED | METADATA_CONTROL_CENTER | READONLY_TERMINAL
  caseContext: CaseContextViewModel;
  initialReviewSummary: ReviewSummaryViewModel | null;
  rereviewSummary: ReviewSummaryViewModel | null;
  availableNavActions: NavigateActionConfig[];  // derived from mode + status
  metadataOnly: boolean;
  blockedReason?: string;
}
```

#### D. Implement Now

| 項目 | 基礎 |
|---|---|
| 4-mode resolution（FULL / ROLE_LIMITED / METADATA / TERMINAL） | role + case status 映射 |
| `FULL_CONTROL_CENTER`：完整 navigate CTAs（依 case status 動態縮減） | permission-matrix |
| `METADATA_CONTROL_CENTER`（SYSADMIN）：C04 banner；metadata whitelist fields；無 command CTA | O3-C projection |
| `READONLY_TERMINAL`（CLOSED/CANCELLED 無例外）：C02 terminal notice；CTAs hidden | state-machine terminal state |
| `ROLE_LIMITED`（REVIEWER/REREVIEWER）：initial-review / rereview 入口依 status + ownership 動態 | permission-matrix |
| C09 InitialReviewSummaryRegion | 有初審 submission 時顯示 |
| C11 LifecycleProgressRegion | 所有 mode |
| Query context preserve on back navigation | return to worklist with filter context |

#### E. STUB Only

| 項目 | 說明 |
|---|---|
| C24 RevisionHistoryJumpEntry | 置放位置待 prototype（OQ §1）；保留 placeholder slot |

#### F. Deferred

| 項目 | 說明 |
|---|---|
| C10/C11 ROLE_LIMITED section visibility 細則 | 採最小收斂：REVIEWER=`C09+C11`、REREVIEWER=`C09+C10+C11`；不擴張為 command rule |
| C03 `METADATA_CONTROL_CENTER` 精確文案 | OQ §3；「metadata-only」是否直接外露 |
| O3-C UI-010 extension whitelist 精確欄位 | shared base + UI-010 specific 欄位 |

#### G. Test-First Notes

1. Route Guard：scope mismatch → forbidden
2. Mode resolution：各 role + case status 組合 → 正確 mode
3. `METADATA_CONTROL_CENTER`：C04 visible in C01；FULL-only fields absent；無 command CTA
4. `READONLY_TERMINAL`：C02 shown；所有 command CTAs hidden
5. `ROLE_LIMITED`：REVIEWER/REREVIEWER 各自 section 可見性
6. Navigate CTA：CLOSED + non-exception → assign/review CTAs hidden
7. CLOSED exception 入口不在本頁：確認本頁無「複審修正」按鈕

#### H. Pending Confirmation

- C10/C11 `ROLE_LIMITED` 可見性以最小前端收斂固定：REVIEWER 不顯示 C10，REREVIEWER 可見 C10；兩者均保留 C09 + C11。
- C24 最終置放位置（OQ §1；待 prototype 驗證）。
- O3-C UI-010 extension whitelist 已由 `api-spec.md` §3.6 與 `permission-matrix.md` §5.1 固定，Wave 3 可直接依 whitelist 落地。

---

### 5.5 UI-011 案件分派頁

#### A. Page Identity

| 項目 | 值 |
|---|---|
| Archetype | A3 Action/Assignment |
| Route | `/cases/:caseId/assign` |
| Feature Module | `features/cases/cases-assignment-command/` |
| Lazy-load boundary | isolated command boundary（與 case-detail 隔離） |
| Guard | `RoleGuard`（HOSPITAL_MANAGER hard guard）+ `ScopeGuard` + status guard |

#### B. Fixed Truths

- **Reviewer 與 Rereviewer section 必須實體分開**（C12 / C13）；不可共用同一 generic input component。
- C14 ScopeAwarePickerComponent 的位置由 prototype 確認；深層 picker 互動（search / modal / autocomplete）= STUB。
- Modes：`ASSIGN_INITIAL` / `REASSIGN_INITIAL` / `MIXED_ASSIGNMENT` / `READONLY_BLOCKED`。
- Page-level submit CTA（A3 特徵）位於頁面上方；不是 viewport-fixed bottom bar。

#### C. Angular Boundary

```
features/cases/cases-assignment-command/
  CaseAssignmentPageComponent      ← Container（mode, assignment form state, API）
  CaseSummaryHeaderComponent       ← shared C01
  StatusOrBlockedBannerComponent   ← shared C02（BLOCKED mode）
  ModeIndicatorStripComponent      ← shared C03
  AssignmentModeBannerComponent    ← feature-scoped C12
  ReviewerAssignmentSection        ← feature-scoped C13（reviewer input + C14 picker slot）
  RereviewerAssignmentSection      ← feature-scoped C14（rereviewer input + C14 picker slot）
  // C23 = STUB slot（due-date helper placeholder）
  // Page-level submit + back zone 位於 C01/C02/C03 之後
```

Container 持有：`AssignmentMode` state、`reviewerId`、`rerevieewerId`、blocked reason、loading。  
不共享 `cases-detail` module 的 state。

Assignment form state：

```typescript
interface AssignmentFormState {
  reviewerId: string | null;
  rerevieewerId: string | null;
}
// mode: ASSIGN_INITIAL | REASSIGN_INITIAL | MIXED_ASSIGNMENT | READONLY_BLOCKED
```

#### D. Implement Now

| 項目 | 基礎 |
|---|---|
| 4 modes resolution（ASSIGN / REASSIGN / MIXED / BLOCKED） | state-machine + permission |
| C12 AssignmentModeBanner（mode → banner text） | A3 handoff §6.2 |
| C13 ReviewerAssignmentSection（reviewer input）— C14 picker slot 骨架 | section separation |
| C14 RereviewerAssignmentSection（rereviewer input）— C14 picker slot 骨架 | section separation |
| Page-level submit CTA + back/cancel | A3 grammar |
| `READONLY_BLOCKED`：C02 banner；所有 input disabled；submit hidden | blocked grammar |
| Guard：非 HOSPITAL_MANAGER → 403 | role hard guard |
| API：`POST /api/quality-inspection/review-cases/:caseId/assign` | api-spec |

#### E. STUB Only

| 項目 | 說明 |
|---|---|
| C14 picker 深層互動（search / autocomplete / modal / 清單分頁） | prototype 只確認 slot 位置；non-spec this round |
| C23 due-date helper region | STUB slot；no interaction spec |
| Change reason 欄位 | 正式 requirement 與資料約束未定 |

#### F. Deferred

（C23 / picker 深層已列 STUB；本頁無其他 deferred 項目）

#### G. Test-First Notes

1. Guard：非 HOSPITAL_MANAGER → deny（403 / redirect）
2. Mode resolution：case status + existing assignment → 正確 mode
3. C13 / C14 section 實體分離：reviewer input ≠ rereviewer input（不共用同一 element）
4. `READONLY_BLOCKED`：C02 visible；submit hidden；input disabled
5. Submit flow：ASSIGN_INITIAL → `POST /assign` → success → navigate（back or detail）
6. C23 slot：present as placeholder；無互動測試

#### H. Pending Confirmation

- Picker 候選人 API endpoint 的 scope filtering（hospitalId scope）。
- C23 due-date helper 完整欄位規則（後續 spec）。
- Picker 深層互動 spec（後續 component planning）。

---

### 5.6 UI-012 初審作業頁

#### A. Page Identity

| 項目 | 值 |
|---|---|
| Archetype | A4 Action/Review Form |
| Route | `/cases/:caseId/initial-review` |
| Feature Module | `features/cases/cases-initial-review/` |
| Lazy-load boundary | isolated command boundary |
| Guard | `RoleGuard`（REVIEWER ownership）+ `ScopeGuard` + status guard + ownership guard |

#### B. Fixed Truths

- Modes：`NEW` / `EDIT_DRAFT` / `REVISION` / `READONLY_BLOCKED`。
- **REVISION = append-only（不是 edit existing submission）**；domain truth；不可在 implementation 被稀釋。
- C16 ReviewCommandBar = **viewport-fixed bottom**；不可移到其他位置；main form 區必須預留底部 padding 以避免被 C16 遮蓋。
- Optional query param `?intent=draft|revision` 用於模式提示；不是 mode 的唯一來源（mode 由 container 計算）。

#### C. Angular Boundary

```
features/cases/cases-initial-review/
  InitialReviewPageComponent         ← Container（mode, draft state, API）
  CaseSummaryHeaderComponent         ← shared C01
  StatusOrBlockedBannerComponent     ← shared C02
  ModeIndicatorStripComponent        ← shared C03
  ReviewFormSummaryStripComponent    ← shared C15（A4）
  ReviewFormMainAreaComponent        ← page-owned（score items = STUB region）
  CommentaryAttachmentZone           ← page-owned（placeholder）
  ReviewVersionSummaryRegionComponent ← shared C17（REVISION mode: basedOn ref）
  ReviewCommandBarComponent          ← shared C16（viewport-fixed bottom; labels per mode）
```

Draft state：

```typescript
interface InitialReviewDraftState {
  mode: A4InitialMode; // NEW | EDIT_DRAFT | REVISION | READONLY_BLOCKED
  draftId?: string;
  basedOnSubmissionId?: string;  // REVISION mode
  commandConfig: CommandBarConfig; // derived from mode
}
```

#### D. Implement Now

| 項目 | 基礎 |
|---|---|
| 4 modes resolution（NEW / EDIT_DRAFT / REVISION / BLOCKED） | ownership + status |
| C15 ReviewFormSummaryStrip（mode input） | A4 shared |
| C16 ReviewCommandBar（viewport-fixed bottom；label 依 mode）| A4 shared；fixed position |
| C17 ReviewVersionSummaryRegion（REVISION mode：basedOn ref） | A4 shared |
| Main form area 骨架（score sheet placeholder region） | region exists；內容 = STUB |
| Commentary / attachment zone 骨架 | region exists；deep interaction = STUB |
| Guard：ownership miss → deny | ownership guard |
| REVISION append-only：C17 shows basedOn；`POST /submit` creates new revision（not update） | domain truth |
| `READONLY_BLOCKED`：C02 banner；C16 hidden；form disabled | blocked grammar |
| Draft save：`POST /draft`（NEW / EDIT_DRAFT） | api-spec |
| Submit initial：`POST /submit`（NEW → `INITIAL_REVIEW_COMPLETED`） | api-spec |
| Submit revision：`POST /submit`（REVISION）| api-spec |

#### E. STUB Only

| 項目 | 說明 |
|---|---|
| Score sheet item-level form controls（題型、input 元件） | 本輪 spec 範圍外；region = placeholder |
| Attachment deep interaction（上傳、預覽、刪除、錯誤） | 後續 component planning |

#### F. Deferred

（score controls / attachment 已列 STUB；本頁無其他 deferred 項目）

#### G. Test-First Notes

1. Guard：ownership mismatch → deny
2. REVISION mode：append-only；C17 shows basedOn reference；submit creates new record（not update）
3. C16 command bar：label 隨 mode 正確切換（[儲存草稿] / [提交初審] / [修正後提交] 等）
4. Draft save / restore：`POST /draft` → reload → EDIT_DRAFT mode
5. `READONLY_BLOCKED`：C02 visible；C16 hidden；form non-interactable
6. C16 fixed bottom：main form 未被 C16 遮蓋（layout padding 正確）

#### H. Pending Confirmation

- Draft save API endpoint 確認（api-spec 是否完整定義 PATCH / PUT / POST）。
- Score sheet item-level form controls spec（後續 spec 層）。
- Attachment 互動 spec（後續 component planning）。

---

### 5.7 UI-013 複審作業頁

#### A. Page Identity

| 項目 | 值 |
|---|---|
| Archetype | A4 Action/Review Form（rereview variant） |
| Route | `/cases/:caseId/rereview` |
| Feature Module | `features/cases/cases-rereview/` |
| Lazy-load boundary | isolated command boundary |
| Guard | `RoleGuard`（REREVIEWER ownership）+ `ScopeGuard` + status guard + `ExceptionGuard` |

#### B. Fixed Truths

- Modes：`NEW` / `EDIT_DRAFT` / `REVISION` / `CLOSED_REVISION_EXCEPTION` / `READONLY_BLOCKED`。
- **`CLOSED_REVISION_EXCEPTION` ≠ `READONLY_BLOCKED`**；兩者視覺語意完全不同（amber/warning vs blocked）。
- **3 exception conditions**（全部成立才是 exception）：
  1. `case.status == CLOSED`
  2. `actor == original_rereviewer`
  3. `has_base_rereview_submission`
- Mode **絕對不從 URL param 決定**；由 route resolver / guard 計算 exception conditions。
- `CLOSED_REVISION_EXCEPTION`：REVISE enabled；**NEW SUBMIT disabled**。
- UI-013 的 CLOSED exception 邏輯**不繼承 UI-012**；兩頁業務語意獨立。
- C18 `InitialSummaryReferenceRegion`（read-only reference）= page-owned；**不得 upgrade 為 shared**。
- C16 ReviewCommandBar = viewport-fixed bottom；同 UI-012。

#### C. Angular Boundary

```
features/cases/cases-rereview/
  RereviewPageComponent                ← Container（resolves all modes incl. exception）
  CaseSummaryHeaderComponent           ← shared C01（含 exception context 顯示）
  StatusOrBlockedBannerComponent       ← shared C02（amber tone for exception；blocked tone for BLOCKED）
  ModeIndicatorStripComponent          ← shared C03（exception label = `例外修正（已結案）`）
  ReviewFormSummaryStripComponent      ← shared C15（A4; rereview-focused）
  RereviewFormRegionComponent          ← page-owned（score items = STUB）
  CommentaryAttachmentZone             ← page-owned（placeholder）
  ReviewVersionSummaryRegionComponent  ← shared C17（REVISION: basedOn ref）
  InitialSummaryReferenceComponent     ← page-owned C18（read-only）
  FinalDecisionSummaryComponent        ← page-owned C19（已依 D-033 移除，PR #45）
  ReviewCommandBarComponent            ← shared C16（viewport-fixed bottom）
```

ViewModel：

```typescript
interface RereviewPageViewModel {
  mode: A4RereviewMode; // NEW | EDIT_DRAFT | REVISION | CLOSED_REVISION_EXCEPTION | READONLY_BLOCKED
  caseContext: CaseContextViewModel;
  currentSubmission: RereviewSubmissionViewModel | null;
  basedOnSubmission: RereviewSubmissionViewModel | null;
  commandConfig: CommandBarConfig;
  isExceptionMode: boolean;
}
```

#### D. Implement Now

| 項目 | 基礎 |
|---|---|
| 5 modes resolution（含 CLOSED_REVISION_EXCEPTION exception path） | O1-C decision-log §d089 |
| Exception conditions：3 conditions 全部成立 → `CLOSED_REVISION_EXCEPTION` | O1-C |
| `CLOSED_REVISION_EXCEPTION`：C02 = amber warning tone；C03 label = `例外修正（已結案）`；REVISE enabled；NEW SUBMIT disabled | O1-C + D-013 |
| `READONLY_BLOCKED`（CLOSED + 非 exception）：C02 = blocked tone（與 amber 明確區分） | visual-baseline §4.5 |
| C15/C16/C17 A4 shared reuse（同 UI-012 pattern） | A4 family |
| C18 InitialSummaryReferenceRegion（read-only；不 upgrade 為 shared） | page-owned |
| C19 FinalDecisionSummaryComponent 骨架（無 live calc） | 已依 D-033 移除（PR #45）：複審頁不得顯示最終分數與最終結論 |
| Guard：ownership guard before exception guard（不可跳過 ownership）| guard hierarchy §3.5 |
| API：`POST /api/quality-inspection/review-cases/:caseId/rereview/submit` | api-spec |

#### E. STUB Only

| 項目 | 說明 |
|---|---|
| Score sheet item-level form controls | 本輪 spec 範圍外 |
| Attachment deep interaction | 後續 component planning |
| C16 button label for `CLOSED_REVISION_EXCEPTION` | OQ §1；UX writing 決定後替換 PLACEHOLDER |
| Revision draft save | api-spec 需確認；先以 STUB/pending 處理 |

#### F. Deferred

| 項目 | 說明 |
|---|---|
| C19 live score calculation | OQ §2；submit-time vs live calc 未定 |
| C02/C03 同時出現時的 priority | OQ §3（exception 同時 blocked 的邊緣情況） |

#### G. Test-First Notes

1. Guard：非 rereviewer ownership → deny
2. **CLOSED + original rereviewer + has base submission → `CLOSED_REVISION_EXCEPTION`**（最高優先驗證）
3. CLOSED + 非 original rereviewer → `READONLY_BLOCKED`
4. `CLOSED_REVISION_EXCEPTION`：C02 amber tone；C03 label = `例外修正（已結案）`；REVISE enabled；NEW SUBMIT hidden/disabled
5. Standard REVISION mode（非 CLOSED）：append-only；C17 shows basedOn reference
6. C16 command bar：label 隨 mode 切換
7. C18 = read-only（不可 edit）；C19 = 靜態骨架（無計算）

#### H. Pending Confirmation

- C16 button label for `CLOSED_REVISION_EXCEPTION`（UX writing OQ §1）。
- Revision draft save API 可行性（api-spec 需更新）。
- C19 live score calculation timing（OQ §2）。

---

### 5.8 UI-014 Revision 歷程頁

#### A. Page Identity

| 項目 | 值 |
|---|---|
| Archetype | A5 History/Traceability |
| Route | `/cases/:caseId/revisions` |
| Feature Module | `features/cases/revision-history/` |
| Lazy-load boundary | history boundary |
| Guard | `RoleGuard` + `ScopeGuard` + `ProjectionGuard` |

#### B. Fixed Truths

- UI-014 是 **timeline + navigate-only action zone**；**無任何 form submission**。
- C22 CLOSED exception entry = **navigate-only**（`routerLink` 或 `router.navigate` to UI-013）；**不是 submit button**；不得有 fixed bottom bar 類似 C16。
- C22 視覺語意必須與 C16 command bar 顯著不同（warning tone outlined secondary；非 primary CTA）。
- METADATA_HISTORY（SYSADMIN）：C21 嚴格服從 O3-C whitelist（`stage / revisionNo / submittedAt / submittedBy / currentEffective`）；FULL-only fields = `—`。
- 每個 stage 的 timeline card 必須明確標示 current effective badge vs superseded badge。

#### C. Angular Boundary

```
features/cases/revision-history/
  RevisionHistoryPageComponent      ← Container（resolves mode + exception conditions）
  CaseSummaryHeaderComponent        ← shared C01
  StatusOrBlockedBannerComponent    ← shared C02（exception：amber hint；terminal：info）
  ModeIndicatorStripComponent       ← shared C03
  ProjectionNoticeBannerComponent   ← shared C04（METADATA_HISTORY mode）
  RevisionTimelineComponent         ← feature-scoped C20（timeline cards per stage）
  RevisionCompareSummaryComponent   ← feature-scoped C21（FULL vs METADATA_ONLY variants）
  RevisionNavActionZoneComponent    ← feature-scoped C22（C22 navigate-only；exception = conditional）
```

Container 持有：mode resolution（role + exception conditions）、revision history data、stage tab state。  
C20 = primarily presentational（timeline cards）。  
C22 recives mode + exception entry config；emits navigate action；**never emits submit**。

ViewModel：

```typescript
interface RevisionHistoryViewModel {
  mode: A5Mode; // FULL_HISTORY | METADATA_HISTORY | READONLY_TERMINAL
  hasExceptionEntry: boolean; // CLOSED + original rereviewer + has base submission
  stages: RevisionStageViewModel[];
  stageTabs: StageTabConfig;   // ALL / INITIAL_ONLY / REREVIEW_ONLY
  metadataOnly: boolean;
  selectedComparison?: [RevisionRef, RevisionRef]; // compare = DEFERRED
}
```

#### D. Implement Now

| 項目 | 基礎 |
|---|---|
| 3 modes resolution（FULL_HISTORY / METADATA_HISTORY / READONLY_TERMINAL） | role + case status |
| Exception conditions → `hasExceptionEntry: true` → C22 shows navigate link | O1-C decision; 3 conditions |
| C20 RevisionTimeline：per-stage current effective marker + superseded marker | A5 spec |
| C21 RevisionCompareSummaryRegion：  FULL variant（complete）+ METADATA_ONLY variant（O3-C whitelist；FULL-only = `—`） | O3-C projection |
| C21 METADATA_ONLY：compare notice visible（「詳細差異內容不適用於此視角」）| HR-1 |
| C04 ProjectionNoticeBanner in C01 inline（METADATA_HISTORY only） | C04 inline pattern |
| C22 CLOSED exception entry：warning tone outlined navigate link → UI-013 | O1-C；navigate-only |
| Stage tabs（ALL / INITIAL_ONLY / REREVIEW_ONLY）作為 query param | query state persistence |
| Guard：scope mismatch → forbidden | |

#### E. STUB Only

（本頁無 STUB 項目；compare version interaction = DEFERRED）

#### F. Deferred

| 項目 | 說明 |
|---|---|
| Compare version selection 互動（如何選兩版進行比較） | OQ §3；interaction spec 層未定 |
| Stage tab 預設切換策略（route resolver vs component init） | OQ §2 |
| C20 timeline card hover / expand | 靜態骨架 |
| C03 METADATA_HISTORY mode strip 精確文案 | OQ §4；UX writing 未定 |
| effective version badge design token（色彩/視覺 token） | 視覺 baseline 層 |

#### G. Test-First Notes

1. Guard：scope mismatch → forbidden
2. `FULL_HISTORY`：complete revision data；effective version badge visible per stage；superseded badge present
3. `METADATA_HISTORY`（SYSADMIN）：C21 only shows O3-C whitelist；FULL-only fields = `—`；compare notice visible；C04 visible
4. CLOSED + original rereviewer + base submission → C22 shows navigate link to UI-013（warning tone outlined）
5. CLOSED 無例外條件 → C22 無例外入口（READONLY_TERMINAL）
6. C22 elements：emit navigate only；**never emit submit**；無 fixed bottom bar

#### H. Pending Confirmation

- Compare version selection interaction spec（OQ §3）仍 deferred；本波僅固定為 non-interactive summary。
- Stage tab pre-selection 採 page spec 建議作為 frontend default；query param 仍優先，未將此預設提升為 authority truth。
- C21 METADATA_HISTORY O3-C whitelist 已由 `api-spec.md` §3.6 與 `permission-matrix.md` §5.1 固定，Wave 3 可直接依 whitelist 落地。

---

## 6. Recommended Implementation Order

### 第一批：Foundation（前置必做）

| 步驟 | 工作 | 理由 |
|---|---|---|
| F1 | 在 `app-routing.module.ts` 新增 `sampling` + `cases` lazy-load entries | 所有頁面的前置前提 |
| F2 | 建立 `SamplingModule` + `SamplingRoutingModule` skeleton（含 child route order） | UI-006/007 前置 |
| F3 | 建立 `CasesModule` + `CasesRoutingModule` skeleton（含 6 child routes） | UI-009~014 前置 |
| F4 | 建立 `RoleGuard` / `ScopeGuard` / `ProjectionGuard` passthrough 骨架 | 所有 route 前置；guard 邏輯後補 |

### 第二批：Shared Components（最高重用；先建立）

| 步驟 | 工作 | 理由 |
|---|---|---|
| S1 | `CaseSummaryHeaderComponent`（C01）含 C04 inline slot | UI-009~014 全部依賴 |
| S2 | `StatusOrBlockedBannerComponent`（C02）—— blocked / exception / info 3 tones | UI-010~014 依賴；UI-013 exception amber 必須實作 |
| S3 | `ModeIndicatorStripComponent`（C03）| UI-009~014 全部依賴 |
| S4 | `ProjectionNoticeBannerComponent`（C04 inline） | UI-009/010/014 依賴 |
| S5 | `ReviewFormSummaryStripComponent`（C15）| UI-012/013 A4 family 依賴 |
| S6 | `ReviewCommandBarComponent`（C16；viewport-fixed bottom）| UI-012/013 A4 family 依賴 |
| S7 | `ReviewVersionSummaryRegionComponent`（C17） | UI-012/013 A4 REVISION mode 依賴 |

### 第三批：Cases 頁面（read-only 先行）

| 步驟 | 工作 | 理由 |
|---|---|---|
| P1 | UI-009 案件列表頁（A1）| 最簡單；無 form；是所有 cases 頁面的 navigate hub |
| P2 | UI-010 案件詳情頁（A2）| navigate-only；無 form；建立後 UI-009 row navigate 可驗證 |
| P3 | UI-014 Revision 歷程頁（A5）| navigate-only；無 form；O1-C exception pattern 可先驗證 |

### 第四批：Sampling 頁面

| 步驟 | 工作 | 理由 |
|---|---|---|
| P4 | UI-007 抽選批次列表頁（SA2）| list-only；cross-cluster navigate 可驗證 |
| P5 | UI-006 抽選執行頁（SA1）| 最複雜（6-mode + modal + API POST）；在 UI-007 後做 |

### 第五批：Command 頁面（複雜度依序）

| 步驟 | 工作 | 理由 |
|---|---|---|
| P6 | UI-011 案件分派頁（A3）| form + submit；但無 revision logic；較 A4 單純 |
| P7 | UI-012 初審作業頁（A4）| A4 form + modes；無 exception path；先做 |
| P8 | UI-013 複審作業頁（A4 + exception）| 最複雜；CLOSED exception logic 置最後 |

### 跨批次注意事項

- **Frontend test foundation gate 是後續所有波次的共同前置**：在 `npm run typecheck`、`npm run test:unit:ci`、`npm run test:e2e` 尚未同時為綠燈前，不得開始 Wave 3 / Wave 4 / Wave 5 的任何功能實作。
- **test-first 是執行規則，不只是建議**：每一個 wave 必須先定義該 wave 的 unit / e2e 驗證，再開始功能；若該 wave 的 exit gate 未通過，不得進入下一個 wave。
- **若 e2e 再度回到 partial**（包含 `No tests found`、discovery 失效、smoke route 失敗），允許進行的工作僅限 foundation repair；Wave 3 / Wave 4 / Wave 5 一律停止。
- F1~F4 必須全部完成後，P1~P8 才能開始。
- S1~S7 可以與 F1~F4 平行進行，也可以在 P 批次進行中按需建立。
- P3（UI-014）應在 P8（UI-013）之前完成，因為 C22 navigate to UI-013 是 P3 的 implement-now 項目。

---

## 7. Open Issues / Pending Confirmation

### 7.1 Sampling 叢集

| 編號 | 問題 | Impact | 影響頁面 |
|---|---|---|---|
| OQ-S01 | `/sampling-batches/preview` response 是否含 `selectedCount`？ | 4th preview stat card 是否顯示 | UI-006 |
| OQ-S02 | Post-execute timing fields（`startedAt` / `finishedAt`）來源 | R6 timing STUB → implement now | UI-006 |
| OQ-S04 | SYSADMIN SamplingBatch metadata whitelist 精確欄位 | MASKED → 完整 whitelist | UI-007 |
| OQ-S05 | REVIEWER/REREVIEWER 是否可進入 `/sampling/batches` | guard 策略 | UI-007 |
| S-BLOCK-1 | UI-008 page spec 何時定稿 | [查看詳情] STUB → real navigate | UI-007 |

### 7.2 Cases 叢集

| 編號 | 問題 | Impact | 影響頁面 |
|---|---|---|---|
| OQ-C01 | C10/C11 ROLE_LIMITED（REVIEWER/REREVIEWER）RereviewSummary 可見性細則 | section visibility | UI-010 |
| OQ-C02 | C24 RevisionHistoryJumpEntry 最終置放位置 | 佔位 → real layout | UI-010 |
| OQ-C03 | O3-C UI-010 extension whitelist 精確欄位 | 已由 `api-spec.md` §3.6 + `permission-matrix.md` §5.1 關閉；不再阻塞 Wave 3 | UI-010 |
| OQ-C04 | O3-C UI-009 extension whitelist 精確欄位 | metadata projection fields | UI-009 |
| OQ-C05 | O3-C UI-014 extension whitelist 精確欄位 | 已由 `api-spec.md` §3.6 + `permission-matrix.md` §5.1 關閉；不再阻塞 Wave 3 | UI-014 |
| OQ-C06 | UI-013 C16 button label for `CLOSED_REVISION_EXCEPTION` | PLACEHOLDER → real label | UI-013 |
| OQ-C07 | UI-013 revision draft save API 可行性 | STUB → implement | UI-013 |
| OQ-C08 | UI-013 C19 live score calculation timing（submit-time vs live） | C19 骨架 → live calc | UI-013 |
| OQ-C09 | UI-014 compare version selection interaction spec | DEFERRED → interaction spec | UI-014 |
| OQ-C10 | UI-014 stage tab pre-selection strategy | DEFERRED → strategy | UI-014 |

### 7.4 Wave 3 Closure Pass Update

- Frontend Wave 3 foundation gate 已由實測 evidence 關閉：`npm run typecheck`、`npm run test:unit:ci`、`npm run test:e2e` 均已通過，且 e2e 不再是 `No tests found`。
- OQ-C03（UI-010 metadata extension whitelist）已由 authority 文件固定，改為 closed。
- OQ-C05（UI-014 metadata extension whitelist）已由 authority 文件固定，改為 closed。
- OQ-C01、OQ-C02、OQ-C09、OQ-C10 仍保留，但僅屬 Wave 3 non-blocking / deferred，不阻塞 UI-010、UI-014 的 test-first skeleton。

### 7.3 Cross-Cutting

| 編號 | 問題 | Impact |
|---|---|---|
| CC-01 | shell navigation entry key 正式命名（「抽選作業」/ 「案件管理」） | shell navigation registration |
| CC-02 | MFE federation remote name（`sampling` / `quality-inspection`） | remote entry configuration |
| CC-03 | UI-006 EXECUTE_SUCCESS `[前往案件列表]` 是否以 `batchId` 作為 query param（provisional） | navigate target confirmation |
| CC-04 | Cases cluster back-navigation context restore strategy（`/cases?batchId=...` 返回 `/sampling/batches`） | cross-cluster query state |

---

## 8. Final Delivery Split

### 8.1 Implement Now（本輪必做）

| 類別 | 項目 |
|---|---|
| Foundation | `app-routing.module.ts` lazy-load entries × 2；`SamplingModule` + `SamplingRoutingModule`（含 route order）；`CasesModule` + `CasesRoutingModule`；Guard stubs |
| Shared（C01~C04） | `CaseSummaryHeaderComponent`（C01）、`StatusOrBlockedBannerComponent`（C02, 3 tones）、`ModeIndicatorStripComponent`（C03）、`ProjectionNoticeBannerComponent`（C04 inline） |
| A4 Shared（C15~C17） | `ReviewFormSummaryStripComponent`（C15）、`ReviewCommandBarComponent`（C16 viewport-fixed）、`ReviewVersionSummaryRegionComponent`（C17） |
| UI-006 | SA1 6-mode state machine；3 confirmed preview stat cards；execute flow（preview→confirm modal→execute）；READONLY_BLOCKED 4 reason banner；post-execute regions |
| UI-007 | SA2 role tab preset matrix（3 roles）；SYSADMIN MASKED stats；[重跑] D-013 modal；[查看案件] cross-cluster navigate |
| UI-009 | A1 work queue；role-default tab preset；O3-C metadata-only same-route；row-level navigate actions |
| UI-010 | A2 control center；4-mode resolution；navigate CTAs；metadata/terminal modes |
| UI-011 | A3 modes；C12/C13/C14 section separation；page-level submit；BLOCKED handling |
| UI-012 | A4 modes；C15/C16/C17 reuse；append-only REVISION；form region skeleton；BLOCKED handling |
| UI-013 | A4 + CLOSED_REVISION_EXCEPTION mode；3 exception conditions；amber vs blocked visual distinction；C15/C16/C17 reuse |
| UI-014 | A5 history；effective/superseded badges；METADATA_HISTORY O3-C whitelist；C22 navigate link（exception conditional） |

### 8.2 STUB Only（存在但不實作互動）

| 項目 | 頁面 |
|---|---|
| `[查看詳情]` row action button（no-op；not bound to any route） | UI-007 |
| `SamplingPageHeader` / `BatchListPageHeader`（placeholder div） | UI-006/007 |
| C14 picker 深層互動（slot 存在；no interaction） | UI-011 |
| C23 due-date helper region（slot 存在；no interaction） | UI-011 |
| Main review form score item-level region（placeholder region） | UI-012/013 |
| Commentary / attachment zone（placeholder） | UI-012/013 |
| UI-013 C16 button label for exception（PLACEHOLDER text） | UI-013 |
| C24 RevisionHistoryJumpEntry（slot 存在；位置待定案）| UI-010 |

### 8.3 Deferred（本輪不做；等待後續 spec 或決策）

| 項目 | 觸發條件 | 頁面 |
|---|---|---|
| 4th preview stat card（selectedCount） | OQ-S01 resolved | UI-006 |
| Post-execute timing fields | OQ-S02 resolved | UI-006 |
| SYSADMIN 完整 whitelist（非 MASKED） | OQ-S04 resolved | UI-007 |
| EXECUTING animation | 視覺 baseline 定案 | UI-006 |
| Confirm modal copy | UX writing | UI-006 |
| Real-time batch refresh | API spec 新增 streaming | UI-007 |
| C10/C11 ROLE_LIMITED 精確規則 | OQ-C01 closed by minimal frontend convergence | UI-010 |
| C24 final placement | OQ-C02 prototype validation | UI-010 |
| Score item-level form controls | form controls spec | UI-012/013 |
| Attachment upload/preview/delete | component planning | UI-012/013 |
| C19 live score calculation | OQ-C08 resolved | UI-013 |
| Compare version selection UI | OQ-C09 interaction spec | UI-014 |
| Stage tab pre-selection strategy | OQ-C10 downgraded to frontend-default/non-blocking | UI-014 |
| Cross-cluster back-navigation context restore | CC-04 strategy | UI-007/009 |

---

*版本*：v1.0 — 基於 `ready-pages-implementation-handoff.md` v1.0 + `p2-implementation-handoff.md` + `sampling-route-module-map-addendum.md` + `ui-route-module-map.md` + `ui-visual-baseline.md` + decision-log §d089/§d095/D-013。
