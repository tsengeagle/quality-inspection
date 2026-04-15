# Ready Pages Frontend Task Breakdown

## 1. 文件目的

本文件將 `ready-pages-angular-execution-plan.md`、`ready-pages-implementation-handoff.md`、
`p2-implementation-handoff.md` 與 `sampling-route-module-map-addendum.md` 的設計結論，
轉換成前端工程可直接分派、可逐步交付、可獨立驗證的任務清單。

每個任務均包含：completion boundary（什麼算做完）、out_of_scope（什麼不在本任務範圍）、
acceptance_criteria（如何驗收）與 blockers（哪些 pending confirmation 會影響交付）。

**文件用途：**
- 作為前端工程師的實作分派依據
- 作為 reviewer/QA 的驗收基準
- 作為 pending OQ 阻斷影響範圍的追蹤記錄

---

## 2. Authority Sources Used

本文件的所有任務邊界均來自以下 authority sources（依優先序）：

| 優先序 | 文件 | 採用內容 |
|--------|------|---------|
| 1 | `docs/01-requirements/decision-log.md` | D-027/D-028 決策（CLOSED exception、metadata-only whitelist）|
| 2 | `docs/02-ddd/permission-matrix.md` | 頁面存取、scope、action permission |
| 3 | `docs/02-ddd/state-machine.md` | case lifecycle、terminal state、revision guard |
| 4 | `docs/03-contracts/api-spec.md` | HTTP API 契約、endpoint list、error codes |
| 5 | `docs/05-ui/ui-layout-archetypes.md` | A1/A3/A4 archetype 定義 |
| 6 | `docs/05-ui/ui-page-spec-ui006抽選執行頁.md` 等各頁 spec | 各頁 region 與 action truth |
| 7 | `docs/06-frontend/ui-route-module-map.md` | Cases cluster route/guard |
| 8 | `docs/06-frontend/sampling-route-module-map-addendum.md` | Sampling cluster route（補充）|
| 9 | `docs/06-frontend/ready-pages-angular-execution-plan.md` | 8 頁完整 Angular 計畫（含 §6 波次、§7 OQs、§8 交付邊界）|
| 10 | `docs/06-frontend/ready-pages-implementation-handoff.md` | 各頁 handoff（含 §3 cross-page notes）|
| 11 | `docs/06-frontend/p2-implementation-handoff.md` | UI-009/011/012 handoff detail |
| 12 | `docs/06-frontend/ui-component-map.md` | shared/feature-scoped/page-owned 分類 |

---

## 3. Breakdown Strategy

### 3.1 任務分類

| 類型 | 說明 | 系列 |
|------|------|------|
| Foundation | Angular module/routing/guard 骨架 | F-series |
| Shared | 跨頁共用 component（C01–C17 family） | S-series |
| Sampling | Sampling cluster 頁面（UI-006/007） | SP-series |
| Cases | Cases cluster 頁面（UI-009~014） | C-series |
| Placeholder | STUB 與佔位結構 | PH-series |
| Verification | 跨頁驗收與邊界驗核 | V-series |

### 3.2 STUB vs Deferred 語意

**STUB（本輪必須存在，但不可互動）：** 結構必須出現（可視、可定位），但功能尚未實作。
例：`[查看詳情]` 按鈕存在但點按無效；picker slot 存在但無 dropdown。

**Deferred（本輪完全不做）：** 結構也不須建立；必要時僅留 TODO comment。
例：4th preview stat card（等 OQ-S01 解答）；SYSADMIN batch metadata whitelist columns。

### 3.3 Must-Not-Invent（16 條全域禁止規則）

以下事項任何任務均不得自行發明：

1. UI-008 route/component/lazy-load entry（page spec 不存在）
2. Shell nav entry key / MFE federation remote name（無 repo evidence）
3. `[查看詳情]` 真實 route target（UI-007）— 必須 STUB
4. UI-006 EXECUTE_SUCCESS `[前往案件列表]` 的 batchId param（Provisional OQ CC-03）
5. OQ-S01: 4th preview stat card selectedCount
6. OQ-S04: SYSADMIN SamplingBatch metadata whitelist
7. OQ-S05: REVIEWER/REREVIEWER UI-007 access policy
8. Candidate picker 深層互動規格（UI-011）
9. Score sheet item-level form controls 詳規（UI-012/013）
10. Attachment upload/preview/delete interaction 詳規（UI-012/013）
11. C16 button label for CLOSED_REVISION_EXCEPTION（UI-013）
12. Retry modal 文案以外的確認流程（UI-007）
13. Compare version selection UI interaction（UI-014）
14. C24 RevisionHistoryJumpEntry 最終位置（UI-010）
15. EXECUTING animation spec（UI-006）
16. SamplingPageHeader / BatchListPageHeader component API

### 3.4 不可混淆的邊界規則

- **Mode 永遠由 container** 根據（role + caseState + ownership + exceptionConditions）計算
- **Mode 絕不從 URL params** 讀取
- **Mode 絕不在 presentational component 內部決定**
- **Cluster 隔離：** `features/sampling/` 與 `features/cases/` 不得有任何跨模組 service import
- **唯一合法跨 cluster 呼叫：** UI-007 → UI-009 的 `router.navigate(['/cases'], { queryParams: { batchId } })`

---

## 4. Task Overview Table

| Task ID | 標題 | 類型 | 頁面 | 依賴 |
|---------|------|------|------|------|
| F-01 | App Routing 加入兩個 lazy-load 入口 | Foundation | 全域 | — |
| F-02 | Sampling Feature Module 骨架 | Foundation | UI-006/007 | F-01 |
| F-03 | Cases Feature Module 骨架 | Foundation | UI-009~014 | F-01 |
| F-04 | Route Guard 通道骨架（三組） | Foundation | 全域 | F-02, F-03 |
| S-01 | CaseSummaryHeaderComponent（含 C04 inline slot） | Shared | UI-009~014 | F-03 |
| S-02 | StatusOrBlockedBannerComponent（三種 tone） | Shared | UI-010~014 | F-03 |
| S-03 | ModeIndicatorStripComponent | Shared | UI-009~014 | F-03 |
| S-04 | ProjectionNoticeBannerComponent（C04） | Shared | UI-009/010/014 | F-03 |
| S-05 | ReviewFormSummaryStripComponent（C15） | Shared | UI-012/013 | F-03 |
| S-06 | ReviewCommandBarComponent（C16，viewport-fixed） | Shared | UI-012/013 | F-03 |
| S-07 | ReviewVersionSummaryRegionComponent（C17） | Shared | UI-012/013 | F-03 |
| SP-01 | UI-007 SamplingBatchListPageComponent 骨架 | Sampling | UI-007 | F-02, S-01, S-02, S-03 |
| SP-02 | UI-007 BatchList 子 component 群（BatchStatusTab / FilterBar / Table / RowActions） | Sampling | UI-007 | SP-01 |
| SP-03 | UI-007 RetryConfirmModal（固定文案 D-013） | Sampling | UI-007 | SP-01 |
| SP-04 | UI-006 SamplingExecutePageComponent 骨架（6-mode） | Sampling | UI-006 | F-02, S-01, S-02, S-03 |
| SP-05 | UI-006 SamplingRuleSummary + SamplingPreviewStats（前 3 card） | Sampling | UI-006 | SP-04 |
| SP-06 | UI-006 SamplingPostExecuteRegion + BLOCKED 4-reason banner | Sampling | UI-006 | SP-04 |
| SP-07 | UI-006 SamplingExecuteConfirmModal 結構 | Sampling | UI-006 | SP-04 |
| C-01 | UI-009 CaseListPageComponent 骨架 + role-default preset | Cases | UI-009 | F-03, S-01, S-03, S-04 |
| C-02 | UI-009 WorkQueue 子 component 群（FilterBar / Table / RowActionSet） | Cases | UI-009 | C-01 |
| C-03 | UI-010 CaseDetailPageComponent 骨架（4-mode resolution） | Cases | UI-010 | F-03, S-01, S-02, S-03, S-04 |
| C-04 | UI-010 CaseControlCenter 子 component 群（NavComponent / InitialSummary / LifecycleProgress） | Cases | UI-010 | C-03 |
| C-05 | UI-011 CaseAssignmentPageComponent 骨架（4-mode，A3 submit zone） | Cases | UI-011 | F-03, S-01, S-02, S-03 |
| C-06 | UI-011 AssignmentModeBanner + ReviewerAssignmentSection + RereviewerAssignmentSection | Cases | UI-011 | C-05 |
| C-07 | UI-012 InitialReviewPageComponent 骨架（4-mode，A4 grammar） | Cases | UI-012 | F-03, S-01, S-02, S-03, S-05, S-06, S-07 |
| C-08 | UI-012 ReviewFormMainArea + CommentaryAttachmentZone 骨架 | Cases | UI-012 | C-07 |
| C-09 | UI-013 RereviewPageComponent 骨架（5-mode，含 CLOSED_REVISION_EXCEPTION） | Cases | UI-013 | F-03, S-01, S-02, S-03, S-05, S-06, S-07, C-07 |
| C-10 | UI-013 InitialSummaryReferenceRegion（C18）+ FinalDecisionSummaryComponent（C19 skeleton） | Cases | UI-013 | C-09 |
| C-11 | UI-014 RevisionHistoryPageComponent 骨架（3-mode）+ exception entry | Cases | UI-014 | F-03, S-01, S-02, S-03, S-04 |
| C-12 | UI-014 RevisionTimeline / RevisionCompareSummary / RevisionNavActionZone | Cases | UI-014 | C-11 |
| PH-01 | [查看詳情] no-op button STUB（UI-007） | Placeholder | UI-007 | SP-02 |
| PH-02 | SamplingPageHeader / BatchListPageHeader placeholder div | Placeholder | UI-006/007 | F-02 |
| PH-03 | C14 RereviewerAssignmentSection picker slot（UI-011） | Placeholder | UI-011 | C-06 |
| PH-04 | C23 AssignmentDueDateHelper placeholder slot（UI-011） | Placeholder | UI-011 | C-06 |
| PH-05 | Score item-level region placeholder（UI-012/013） | Placeholder | UI-012/013 | C-08, C-10 |
| PH-06 | Commentary/attachment zone placeholder（UI-012/013） | Placeholder | UI-012/013 | C-08, C-10 |
| PH-07 | C16 command label PLACEHOLDER for CLOSED exception（UI-013） | Placeholder | UI-013 | C-09 |
| PH-08 | C24 RevisionHistoryJumpEntry slot（UI-010） | Placeholder | UI-010 | C-04 |
| V-01 | Route smoke test（8 routes 可解析；UI-008 不存在） | Verification | 全域 | F-02, F-03 |
| V-02 | Guard passthrough 驗證 | Verification | 全域 | F-04 |
| V-03 | Cluster isolation 驗證（無跨 cluster import） | Verification | 全域 | SP-01~SP-07, C-01~C-12 |
| V-04 | Mode resolution contract 審核（mode 來源一致性） | Verification | 全域 | C-03, C-05, C-07, C-09, C-11 |
| V-05 | Must-not-invent 16 條 audit | Verification | 全域 | 所有任務完成後 |
| V-06 | STUB boundary 驗收（STUB 存在但不可互動） | Verification | 全域 | 所有 PH- 任務完成後 |

---

## 5. Foundation Tasks（F-series）

---

### F-01 — App Routing 加入兩個 lazy-load 入口

```yaml
task_id: F-01
title: App Routing 加入兩個 lazy-load 入口
type: Foundation
pages: [全域]
depends_on: []
parallelizable: false
```

**Scope：**
修改 `app-routing.module.ts`，加入兩個 lazy-load route entry：
```typescript
{ path: 'sampling', loadChildren: () => import('./features/sampling/sampling.module').then(m => m.SamplingModule) }
{ path: 'cases',    loadChildren: () => import('./features/cases/cases.module').then(m => m.CasesModule) }
```
現有 `{ path: '', redirectTo: 'shell', pathMatch: 'full' }` entry 保持不變。

**Out of Scope：**
- 不建立 SamplingModule / CasesModule 的實際內容（由 F-02/F-03 負責）
- 不定義 UI-008 相關 entry（page spec 不存在）
- 不修改 shell routing 或其他現有 route

**Deliverables：**
- 修改後的 `app-routing.module.ts`

**Acceptance Criteria：**
1. `app-routing.module.ts` 有兩個新 lazy-load entry（`sampling` 與 `cases`）
2. `loadChildren` 指向正確的 module 路徑（`features/sampling/sampling.module` 和 `features/cases/cases.module`）
3. 現有 redirectTo entry 未被移除
4. TypeScript compile 無錯誤（即使 target module 尚未建立，可先使用 placeholder）
5. UI-008 entry 不存在

**Blockers/Pending Confirmation：** CC-02（MFE federation remote name）— 不影響 routing module 定義本身。

---

### F-02 — Sampling Feature Module 骨架

```yaml
task_id: F-02
title: Sampling Feature Module 骨架
type: Foundation
pages: [UI-006, UI-007]
depends_on: [F-01]
parallelizable: false（F-01 必須先完成）
```

**Scope：**
建立以下骨架（空 component class + empty template）：
```
features/sampling/
  sampling.module.ts          ← SamplingModule（含 RouterModule.forChild）
  sampling-routing.module.ts  ← SamplingRoutingModule
  sampling-execute/
    sampling-execute-page.component.ts/.html/.scss  ← UI-006
  batch-list/
    sampling-batch-list-page.component.ts/.html/.scss  ← UI-007
```

**Routes（順序不可改變，batches 必須在 :projectId 之前）：**
```typescript
const routes: Routes = [
  { path: 'batches',    component: SamplingBatchListPageComponent },  // UI-007
  { path: ':projectId', component: SamplingExecutePageComponent },    // UI-006
  { path: '',           redirectTo: 'batches', pathMatch: 'full' }
];
```

**Out of Scope：**
- 不建立 UI-006/UI-007 的任何 child component（由 SP-series 負責）
- 不加入 guard（由 F-04 負責）
- 不定義 UI-008 的任何 route

**Deliverables：**
- `sampling.module.ts`, `sampling-routing.module.ts`
- `sampling-execute-page.component.ts/html/scss`
- `sampling-batch-list-page.component.ts/html/scss`

**Acceptance Criteria：**
1. `/sampling/batches` 可解析到 `SamplingBatchListPageComponent`
2. `/sampling/project-001`（任意 projectId）可解析到 `SamplingExecutePageComponent`
3. `/sampling` 自動 redirect 到 `/sampling/batches`
4. 路由順序正確（`batches` literal 在 `:projectId` 前）
5. compile 無錯誤，component 顯示空白頁面無 runtime error

**Blockers/Pending Confirmation：** 無。

---

### F-03 — Cases Feature Module 骨架

```yaml
task_id: F-03
title: Cases Feature Module 骨架
type: Foundation
pages: [UI-009, UI-010, UI-011, UI-012, UI-013, UI-014]
depends_on: [F-01]
parallelizable: true（可與 F-02 平行）
```

**Scope：**
建立以下骨架（空 component class + empty template）：
```
features/cases/
  cases.module.ts
  cases-routing.module.ts
  cases-worklist/
    case-list-page.component.ts/.html/.scss         ← UI-009
  case-detail/
    case-detail-page.component.ts/.html/.scss       ← UI-010
  cases-assignment-command/
    case-assignment-page.component.ts/.html/.scss   ← UI-011
  cases-initial-review/
    initial-review-page.component.ts/.html/.scss    ← UI-012
  cases-rereview/
    rereview-page.component.ts/.html/.scss          ← UI-013
  revision-history/
    revision-history-page.component.ts/.html/.scss  ← UI-014
```

**Routes：**
```typescript
const routes: Routes = [
  { path: '',                       component: CaseListPageComponent },       // UI-009
  { path: ':caseId',                component: CaseDetailPageComponent },     // UI-010
  { path: ':caseId/assign',         component: CaseAssignmentPageComponent }, // UI-011
  { path: ':caseId/initial-review', component: InitialReviewPageComponent },  // UI-012
  { path: ':caseId/rereview',       component: RereviewPageComponent },       // UI-013
  { path: ':caseId/revisions',      component: RevisionHistoryPageComponent } // UI-014
];
```

**Out of Scope：**
- 不建立任何 child component（由 C-series 負責）
- 不加入 guard（由 F-04 負責）

**Deliverables：**
- `cases.module.ts`, `cases-routing.module.ts`
- 6 個 page component 骨架（ts/html/scss）

**Acceptance Criteria：**
1. 6 條 case routes 均可解析（smoke test）
2. `/cases` → `CaseListPageComponent`
3. `/cases/case-001` → `CaseDetailPageComponent`
4. `/cases/case-001/assign` → `CaseAssignmentPageComponent`
5. `/cases/case-001/initial-review` → `InitialReviewPageComponent`
6. `/cases/case-001/rereview` → `RereviewPageComponent`
7. `/cases/case-001/revisions` → `RevisionHistoryPageComponent`
8. compile 無錯誤，空頁面無 runtime error

**Blockers/Pending Confirmation：** 無。

---

### F-04 — Route Guard 通道骨架（三組）

```yaml
task_id: F-04
title: Route Guard 通道骨架（RoleGuard / ScopeGuard / ProjectionGuard）
type: Foundation
pages: [全域]
depends_on: [F-02, F-03]
parallelizable: true（可與 S-series 平行）
```

**Scope：**
建立三個 passthrough guard（永遠 return true 的骨架），並在 routes 上加入 `canActivate`：
- `RoleGuard`：outermost route-level、角色檢查
- `ScopeGuard`：hospitalId scope 比對
- `ProjectionGuard`：METADATA_ONLY vs FULL 投影判斷

所有 guard 目前實作為 passthrough（return `true`），以確保開發期間不阻斷任何路由。
後續正式實作時換置這三個 guard 的邏輯即可。

**Guard 層次（文件固定順序，不可跳過）：**
```
1. RoleGuard         ← outermost
2. ScopeGuard
3. ProjectionGuard
4. status_guard      ← page-mode level（container 內部，不在本任務）
5. ownership_guard   ← container 內部
6. exception_guard   ← container 內部
7. action_policy_guard  ← action level
```
本任務只負責 1-3（route-level）。4-7 屬 container 內部邏輯，由各頁面任務負責。

**Out of Scope：**
- 不實作 guard 的實際邏輯（只需 passthrough）
- Status guard / ownership guard / exception guard 的 container 內部邏輯不在本任務
- token 驗證或 JWT 解析邏輯

**Deliverables：**
- `guards/role.guard.ts`
- `guards/scope.guard.ts`
- `guards/projection.guard.ts`（均為 passthrough）

**Acceptance Criteria：**
1. 三個 guard class 存在且 implement `CanActivate`
2. 三個 guard 均 return `true`（passthrough）
3. `canActivate: [RoleGuard, ScopeGuard, ProjectionGuard]` 加入至 Sampling 與 Cases 頂層 route
4. 已有路由不因 guard 加入而導致任何 URL 解析失敗
5. compile 無錯誤

**Blockers/Pending Confirmation：** OQ-S05（REVIEWER/REREVIEWER sampling access）— 影響 guard 正式邏輯，不影響本輪 passthrough 骨架。

---

## 6. Shared Structure Tasks（S-series）

> S-series 可與 F-01~F-04 平行進行，但需要目標 module 骨架存在才能 declare。

---

### S-01 — CaseSummaryHeaderComponent（C01，含 C04 inline slot）

```yaml
task_id: S-01
title: CaseSummaryHeaderComponent（C01）含 C04 ProjectionNoticeBanner inline slot
type: Shared
pages: [UI-009, UI-010, UI-011, UI-012, UI-013, UI-014]
depends_on: [F-03]
parallelizable: true
```

**Scope：**
建立 `CaseSummaryHeaderComponent`（C01），固定在頁面頂部，顯示案件基本識別資訊：
- 案件 ID、病歷號、姓名、科別、入院日期等 case context 欄位
- 內部有 `<ng-content>` 或 conditional 插槽，讓 C04 `ProjectionNoticeBanner` 可 inline 呈現（不作獨立 row）

**固定渲染順序（整個 Cases cluster 遵守）：**
```
C01（含 C04 inline） → C02（conditional） → C03 → page content
```

**C04 inline 規則：**
- C04 只有 SYSADMIN + metadata-only projection 時顯示
- C04 放在 C01 內部，不放在 C01 外作獨立 banner row
- C04 的存在不影響 C01 的基本 layout

**Out of Scope：**
- C04 本身的 component 實作（由 S-04 負責）
- metadata-only 欄位遮蔽邏輯（由各頁面查詢 projection 後自行控制）
- case context 的 API 呼叫（由各頁面 container service 負責）

**Deliverables：**
- `components/case-summary-header/case-summary-header.component.ts/html/scss`
- 在 SharedModule（或 CasesModule）宣告並匯出

**Acceptance Criteria：**
1. Component 存在且可在所有 cases 頁面正常掛入
2. 顯示基本 case context 欄位（可使用 mock input data）
3. 有 C04 inline slot（`ng-content` select 或 `@Input() showProjectionNotice`）
4. C01 固定為頁面最上方的 case context zone
5. 單元測試：輸入 mock case data，驗證渲染正確

**Blockers/Pending Confirmation：** OQ-C03/C04/C05（各頁 O3-C projection whitelist）— 影響各頁欄位的顯示/遮蔽，不影響 C01 骨架建立。

---

### S-02 — StatusOrBlockedBannerComponent（C02，三種 tone）

```yaml
task_id: S-02
title: StatusOrBlockedBannerComponent（C02）含三種 tone 語意
type: Shared
pages: [UI-010, UI-011, UI-012, UI-013, UI-014]
depends_on: [F-03]
parallelizable: true
```

**Scope：**
建立 `StatusOrBlockedBannerComponent`（C02），呈現頁面層級的狀態/阻斷說明。
三種 tone 必須在視覺上明確區分（顏色/圖示不可相同）：
1. **blocked**：因 status/ownership/role 阻斷，無法執行命令
2. **exception-amber**：CLOSED_REVISION_EXCEPTION 特殊路徑（UI-013）
3. **info**：一般資訊提示

C02 呈現順序：在 C01 之後、C03 之前（conditional 顯示）。

**Out of Scope：**
- blocked 原因的計算邏輯（由各頁面 container 負責）
- 設計 token 細節（使用現有 portal51-lib 類別）

**Deliverables：**
- `components/status-or-blocked-banner/status-or-blocked-banner.component.ts/html/scss`
- `@Input() tone: 'blocked' | 'exception-amber' | 'info'`
- `@Input() message: string`

**Acceptance Criteria：**
1. 三種 tone 在視覺上可明確區分
2. blocked tone 與 exception-amber tone 外觀不相同（避免混淆）
3. 單元測試：三種 tone 各自渲染正確的 CSS class 或 icon
4. component 在 UI-013 中可正確呈現 `exception-amber` tone

**Blockers/Pending Confirmation：** 無。

---

### S-03 — ModeIndicatorStripComponent（C03）

```yaml
task_id: S-03
title: ModeIndicatorStripComponent（C03）
type: Shared
pages: [UI-009, UI-010, UI-011, UI-012, UI-013, UI-014]
depends_on: [F-03]
parallelizable: true
```

**Scope：**
建立 `ModeIndicatorStripComponent`（C03），顯示當前頁面的業務 mode label。
Mode label 為業務語意，不是技術狀態。

**已知的 mode 顯示值（摘要）：**
- Normal modes：`閱讀（唯讀）`、`指派中`、`初審中`、`複審修正中` 等
- UI-013 exception mode 特殊 label：`例外修正（已結案）`
- 其餘 mode label 由各頁面 container 依 mode resolution 結果輸入

**Out of Scope：**
- Mode 的計算邏輯（container 負責）

**Deliverables：**
- `components/mode-indicator-strip/mode-indicator-strip.component.ts/html/scss`
- `@Input() modeLabel: string`

**Acceptance Criteria：**
1. Component 顯示傳入的 `modeLabel` 字串
2. UI-013 exception mode 可正確顯示 `例外修正（已結案）`
3. Strip 在渲染順序中位於 C02 之後、page content 之前

**Blockers/Pending Confirmation：** 無。

---

### S-04 — ProjectionNoticeBannerComponent（C04）

```yaml
task_id: S-04
title: ProjectionNoticeBannerComponent（C04）
type: Shared
pages: [UI-009, UI-010, UI-014]
depends_on: [F-03]
parallelizable: true
```

**Scope：**
建立 `ProjectionNoticeBannerComponent`（C04），僅在 SYSADMIN metadata-only projection 時顯示。
放置位置：C01 內部 inline（非獨立 banner row）。

**Out of Scope：**
- projection 判斷邏輯（由各頁面 container 輸入 `@Input() show`)
- 各頁面的 metadata-only 欄位遮蔽邏輯

**Deliverables：**
- `components/projection-notice-banner/projection-notice-banner.component.ts/html/scss`
- `@Input() show: boolean`（當 false 時不渲染）

**Acceptance Criteria：**
1. `show=true` 時橫幅可見
2. `show=false` 時橫幅完全不渲染（`*ngIf` 或 `@if`）
3. 可正確放置於 C01 內部

**Blockers/Pending Confirmation：** OQ-C03/C04/C05（各頁 extension whitelist）— 不影響 C04 骨架。

---

### S-05 — ReviewFormSummaryStripComponent（C15）

```yaml
task_id: S-05
title: ReviewFormSummaryStripComponent（C15）
type: Shared
pages: [UI-012, UI-013]
depends_on: [F-03]
parallelizable: true
```

**Scope：**
建立 `ReviewFormSummaryStripComponent`（C15），A4 shared context strip，顯示評審摘要資訊。
不放答案欄位。接受 mode input，輸出對應的摘要呈現。

**Deliverables：**
- `components/review-form-summary-strip/review-form-summary-strip.component.ts/html/scss`

**Acceptance Criteria：**
1. Component 接受 mode input，呈現對應摘要（mock data 即可）
2. 不包含答案欄位或提交按鈕
3. 在 C03 之後、主作業區之前

**Blockers/Pending Confirmation：** 無。

---

### S-06 — ReviewCommandBarComponent（C16，viewport-fixed bottom）

```yaml
task_id: S-06
title: ReviewCommandBarComponent（C16）viewport-fixed bottom
type: Shared
pages: [UI-012, UI-013]
depends_on: [F-03]
parallelizable: true
```

**Scope：**
建立 `ReviewCommandBarComponent`（C16），**viewport-fixed bottom**（不可改成 static position）。
C16 是 A4 command grammar 的核心，必須對 save / submit / revise 命令的語意分離負責。

**關鍵約束（不可違反）：**
- C16 必須使用 CSS `position: fixed; bottom: 0`，不得隨頁面滾動
- 各頁面主作業區必須預留底部 padding，避免 C16 遮擋內容
- UI-013 CLOSED_REVISION_EXCEPTION mode 的 C16 button label 仍為 STUB（見 PH-07）

**Out of Scope：**
- button label 的實際文案（由各頁面 mode 決定；exception label 由 PH-07 處理）
- API 呼叫（由各頁面 container service 負責）

**Deliverables：**
- `components/review-command-bar/review-command-bar.component.ts/html/scss`
- viewport-fixed bottom 就定位

**Acceptance Criteria：**
1. C16 保持 `position: fixed; bottom: 0` 在所有視窗尺寸
2. 頁面主作業區底部有補白，不被 C16 遮擋
3. 接受 `@Input() primaryLabel: string`、`@Input() secondaryLabel?: string`
4. 接受 `@Input() disabled: boolean`（command bar 整體 disabled）

**Blockers/Pending Confirmation：** OQ-C06（C16 CLOSED exception button label）— 影響 label 文案，不影響 C16 component 骨架。

---

### S-07 — ReviewVersionSummaryRegionComponent（C17）

```yaml
task_id: S-07
title: ReviewVersionSummaryRegionComponent（C17）
type: Shared
pages: [UI-012, UI-013]
depends_on: [F-03]
parallelizable: true
```

**Scope：**
建立 `ReviewVersionSummaryRegionComponent`（C17），只在 REVISION mode 顯示。
顯示 basedOn submission 的版本參考資訊（append-only 語境）。

**Deliverables：**
- `components/review-version-summary-region/review-version-summary-region.component.ts/html/scss`

**Acceptance Criteria：**
1. Component 接受 `@Input() basedOnVersion: SubmissionRef` 型別
2. REVISION mode 時正確顯示；非 REVISION mode 時不渲染
3. 在主作業區上方顯示版本參考（mock data 可）

**Blockers/Pending Confirmation：** 無。

---

## 7. Cases Cluster Tasks（C-series）

---

### C-01 — UI-009 CaseListPageComponent 骨架（role-default preset）

```yaml
task_id: C-01
title: UI-009 CaseListPageComponent 骨架 + role-default preset
type: Cases
pages: [UI-009]
depends_on: [F-03, S-01, S-03, S-04]
parallelizable: true（Cases read-only wave，可與 SP-series 平行）
```

**Scope：**
建立 `CaseListPageComponent`（A1 work queue），完成以下 Angular skeleton：

**Page role（固定不可改）：** A1 work queue — 提供 role-default preset + filter + row-level navigate action。
不做 page-level command、不顯示 full submission 內容、不直接執行 revision。

**Mode resolution（必須由 container 執行）：**
- mode 根據 `(role + queryParams)` 計算，不從 URL 讀取
- metadata-only projection：同 route、同頁、同 archetype（不分成 metadata-only 子路由）

**Query state（tab 持久化）：**
- 當前 tab（work queue type）以 URL query params 持久化
- back navigation 必須 restore 原 query context（tab / filter / pagination / sort）

**Regions（implement-now）：**
- C01 CaseSummaryHeader（本頁使用列表 context 版本）
- C03 ModeIndicatorStrip
- C04 ProjectionNoticeBanner（metadata-only 時）
- C05 CaseWorkQueueFilterBar（骨架）
- C06 CaseWorkQueueTable（骨架）
- C07 CaseRowActionSet（骨架）
- paging zone
- table-level forbidden / empty / error state

**Action grammar：**
- 所有 action 均為 row-level navigate / read entry
- 頁面上不建立 submit / save / bulk command bar
- metadata-only 的 business command 必須 `hidden`（不只 disabled）

**API（implement-now）：**
```
GET /review-cases?role={role}&status={status}&page={page}&size={size}
    &hospitalId={hospitalId}&batchId={batchId}
```

**Out of Scope：**
- C05/C06/C07 的詳細互動實作（由 C-02 負責）
- bulk action / export（Deferred — 不在本輪）
- O3-C extension whitelist 欄位（OQ-C04 pending）

**Deliverables：**
- `cases-worklist/case-list-page.component.ts/html/scss`
- CaseListUIState interface
- `CasesService.getCases()` method stub

**Acceptance Criteria：**
1. `/cases` route 渲染 CaseListPageComponent
2. container 根據 role 設定 default tab/filter preset
3. filter / tab 狀態以 query params 持久化（`?tab=MY_QUEUE&status=PENDING`）
4. metadata-only mode 時 hidden business commands（單測驗證）
5. C01 / C03 / C04 按固定順序渲染
6. 單元測試：role=REVIEWER → default tab 為 REVIEWER queue

**Blockers/Pending Confirmation：**
- OQ-C04（UI-009 O3-C extension whitelist）— 影響 metadata-only 的具體欄位子集
- CC-04（back navigation query context restore 策略）

---

### C-02 — UI-009 WorkQueue 子 component 群

```yaml
task_id: C-02
title: UI-009 WorkQueue 子 component 群（FilterBar / Table / RowActionSet）
type: Cases
pages: [UI-009]
depends_on: [C-01]
parallelizable: false
```

**Scope：**
實作 UI-009 的三個 feature-scoped component：

**C05 CaseWorkQueueFilterBar：**
- 提供 role-aware filter chips（依 role 顯示對應的 status filter）
- 搜尋欄、date range filter 骨架
- emit filter change event 到 container

**C06 CaseWorkQueueTable：**
- 展示案件列表（columns：caseId、patientId、status、assignee、createDate 等）
- metadata-only 模式：只顯示 shared base whitelist 欄位
- 空狀態 / forbidden / error state

**C07 CaseRowActionSet：**
- 依 `(role, caseState, scope)` 決定 visible actions
- 動作清單：`[審查]`、`[複審]`、`[查看]`、`[指派]` 等 navigate actions
- metadata-only 的 business command → hidden（必須完全隱藏，非 disabled）

**固定規則（不可違反）：**
- 不要因為容易抽象而把 row action policy 變成 generic table action schema
- tab 切換不能實作成不同 page component
- A1 work queue 身分不可退化成 dashboard 或 generic table page

**Out of Scope：**
- bulk action / export（Deferred）
- prototype 中的示範 row 筆數不代表正式 pagination 規格

**Deliverables：**
- `cases-worklist/components/case-work-queue-filter-bar/`
- `cases-worklist/components/case-work-queue-table/`
- `cases-worklist/components/case-row-action-set/`

**Acceptance Criteria：**
1. 列表依 API 資料渲染（mock data 驗收）
2. metadata-only 時：business command row actions 完全 hidden
3. empty state 正確顯示
4. filter change 後 container 重新查詢
5. row navigate action 觸發正確路由（`/cases/:caseId` 等）

**Blockers/Pending Confirmation：** OQ-C04（UI-009 projection whitelist columns）。

---

### C-03 — UI-010 CaseDetailPageComponent 骨架（4-mode resolution）

```yaml
task_id: C-03
title: UI-010 CaseDetailPageComponent 骨架（4-mode resolution）
type: Cases
pages: [UI-010]
depends_on: [F-03, S-01, S-02, S-03, S-04]
parallelizable: true（Cases read-only wave）
```

**Scope：**

**Page role（固定）：** A2 control center — 案件生命週期的控制中心，展示 review context、lifecycle progress、可用命令導航。不是 review form。

**4-mode resolution（container 計算）：**

| Mode | 條件 |
|------|------|
| FULL_CONTROL_CENTER | Manager + FULL projection + active case |
| METADATA_CONTROL_CENTER | SYSADMIN + METADATA_ONLY projection |
| REVIEW_ACCESS | Reviewer/Rereviewer + FULL + owned case |
| READONLY_TERMINAL | Terminal state (CLOSED/CANCELLED) |

**Mode 計算規則（不可違反）：**
- Mode 由 container 計算，來源：`(role + caseState + ownership + projectionType)`
- Mode 絕不從 URL params 讀取

**Regions（implement-now）：**
- C01 CaseSummaryHeader（+C04 inline）
- C02 StatusOrBlockedBanner（conditional）
- C03 ModeIndicatorStrip
- C08 CaseControlCenterNavComponent（骨架）
- C09 InitialSummaryRegion（骨架）
- C10/C11 ReviewerAssignmentInfo + RereviewerAssignmentInfo（骨架）

**STUB：** C24 RevisionHistoryJumpEntry slot（見 PH-08）

**API（implement-now）：**
- `GET /review-cases/{caseId}`

**Out of Scope：**
- C10/C11 ROLE_LIMITED visibility rules（OQ-C01 Deferred）
- C24 最終位置（OQ-C02 Deferred）
- O3-C extension whitelist（OQ-C03 Deferred）

**Deliverables：**
- `case-detail/case-detail-page.component.ts/html/scss`
- CaseDetailViewModel interface
- `CasesService.getCaseDetail()` method stub

**Acceptance Criteria：**
1. Container 根據 role/state/ownership 輸出 4 個 mode 之一
2. METADATA_CONTROL_CENTER mode 時 C04 顯示，full-only 欄位隱藏
3. READONLY_TERMINAL mode 時 C02 顯示阻斷 banner
4. CLOSED case 不顯示 exception entry（CLOSED exception 只在 UI-013/014，不在 UI-010）
5. C01(+C04) → C02 → C03 → page content 渲染順序正確
6. 單元測試：4 種 mode 各自的 container 計算

**Blockers/Pending Confirmation：**
- OQ-C01（C10/C11 ROLE_LIMITED visibility rules）
- OQ-C02（C24 placement）
- OQ-C03（O3-C UI-010 extension whitelist）

---

### C-04 — UI-010 CaseControlCenter 子 component 群

```yaml
task_id: C-04
title: UI-010 CaseControlCenter 子 component 群
type: Cases
pages: [UI-010]
depends_on: [C-03]
parallelizable: false
```

**Scope：**
實作 UI-010 的 feature-scoped components：

**C08 CaseControlCenterNavComponent：**
- 提供案件生命週期各 stage 的導航連結
- 依 mode 決定可點按的 actions（FULL_CONTROL_CENTER 可操作；READONLY_TERMINAL 只能查看）

**C09 InitialSummaryRegion：**
- 顯示初審結果摘要（不含完整評分表內容）
- 骨架即可，data binding 到 API 回傳的 summary

**C10 ReviewerAssignmentInfo / C11 RereviewerAssignmentInfo：**
- 顯示已指派的審查人員資訊
- ROLE_LIMITED visibility 規則待 OQ-C01 解決前使用保守顯示（不建 ROLE_LIMITED 邏輯）

**Out of Scope：**
- C10/C11 ROLE_LIMITED visibility rules（Deferred，待 OQ-C01）
- C24（RevisionHistoryJumpEntry）由 PH-08 負責

**Deliverables：**
- `case-detail/components/case-control-center-nav/`
- `case-detail/components/initial-summary-region/`
- `case-detail/components/reviewer-assignment-info/`

**Acceptance Criteria：**
1. 導航連結依 caseId 組合正確路徑（`/cases/:caseId/initial-review` 等）
2. READONLY_TERMINAL mode 時 command actions disabled 或 hidden
3. 初審摘要以 mock data 顯示

**Blockers/Pending Confirmation：** OQ-C01（C10/C11 ROLE_LIMITED）。

---

### C-05 — UI-011 CaseAssignmentPageComponent 骨架（4-mode，A3 submit zone）

```yaml
task_id: C-05
title: UI-011 CaseAssignmentPageComponent 骨架（4-mode，A3 submit zone）
type: Cases
pages: [UI-011]
depends_on: [F-03, S-01, S-02, S-03]
parallelizable: false（波次 5，指令頁面）
```

**Scope：**

**Page role（固定）：** A3 assignment command page — manager 執行 assign/reassign 命令。
**不是 A4 review bar**，submit zone 是 page-level command 而非 viewport-fixed bottom bar。

**4-mode resolution（container 計算）：**

| Mode | 條件 |
|------|------|
| ASSIGN | Manager + PENDING case + no reviewer yet |
| REASSIGN | Manager + active case + existing reviewer |
| MIXED | Manager + case 需同時決定 reviewer 和 rereviewer |
| READONLY_BLOCKED | Non-manager / wrong status / scope mismatch |

**Guard（container 內部）：**
- status_guard（condition 4）→ ownership_guard（condition 5）順序不可跳過

**Action grammar：**
- submit label 須對應實際命令（初審指派、初審改派、複審指派等），不可固定文案
- READONLY_BLOCKED 時不保留可誤觸的 picker 或 submit CTA

**Regions（implement-now）：**
- C01 CaseSummaryHeader
- C02 StatusOrBlockedBanner（READONLY_BLOCKED 時）
- C03 ModeIndicatorStrip
- C12 AssignmentModeBanner（骨架）
- C13 ReviewerAssignmentSection（骨架）
- C14 RereviewerAssignmentSection（骨架，picker 為 STUB）
- page-level submit / back action zone

**API（implement-now）：**
- `GET /review-cases/{caseId}/assignment`
- `POST /review-cases/{caseId}/assignment`

**Out of Scope：**
- C14 picker 深層互動（由 PH-03 負責 STUB）
- C23 due date helper（由 PH-04 負責 STUB）
- Candidate picker 完整互動邏輯（Must-not-invent #8）

**Deliverables：**
- `cases-assignment-command/case-assignment-page.component.ts/html/scss`
- AssignmentPageViewModel interface
- `AssignmentService.getAssignment()` / `.submitAssignment()` method stubs

**Acceptance Criteria：**
1. Container 根據 role/state 輸出 4 個 mode 之一
2. READONLY_BLOCKED 時：C02 顯示、picker 與 submit zone 不渲染
3. Page-level submit zone（非 viewport-fixed）在 ASSIGN/REASSIGN/MIXED mode 可見
4. Submit label 隨 mode 變更（mock）
5. 單元測試：READONLY_BLOCKED → submit zone hidden

**Blockers/Pending Confirmation：** Candidate picker API（Must-not-invent #8）。

---

### C-06 — UI-011 AssignmentSection 子 component 群

```yaml
task_id: C-06
title: UI-011 AssignmentModeBanner + ReviewerAssignmentSection + RereviewerAssignmentSection
type: Cases
pages: [UI-011]
depends_on: [C-05]
parallelizable: false
```

**Scope：**

**C12 AssignmentModeBanner：**
- 顯示當前 assignment 動作語意（初審指派 / 初審改派 / 複審指派等）
- 由 container 注入 mode，C12 只負責顯示

**C13 ReviewerAssignmentSection：**
- reviewer 選人 section
- 顯示現有 reviewer name（如有）
- 選人入口（使用者可點按進入 picker — picker 本體為 STUB，見 PH-03）

**C14 RereviewerAssignmentSection：**
- rereviewer 選人 section
- 結構相同但語意分離（不合併到 C13）
- picker 互動為 STUB（見 PH-03）

**固定規則（不可違反）：**
- C13 與 C14 必須是兩個獨立 section，不可合併成單一 generic assignment form
- 原因：assignment grammar 的核心是 reviewer 與 rereviewer 語意分離

**Out of Scope：**
- Picker 深層互動（由 PH-03 負責 placeholder slot）
- C23 due date helper（由 PH-04 負責 placeholder slot）

**Deliverables：**
- `cases-assignment-command/components/assignment-mode-banner/`
- `cases-assignment-command/components/reviewer-assignment-section/`
- `cases-assignment-command/components/rereviewer-assignment-section/`

**Acceptance Criteria：**
1. C13 與 C14 為獨立 component，不合併
2. READONLY_BLOCKED mode 時兩個 section 均不渲染（由 C-05 container 控制）
3. 各 section 顯示現有 assignee 資訊（mock data）
4. Picker 入口（button slot）存在但互動為 STUB（點按無效）

**Blockers/Pending Confirmation：** PH-03 依賴本任務；PH-04 依賴本任務。

---

### C-07 — UI-012 InitialReviewPageComponent 骨架（4-mode，A4 grammar）

```yaml
task_id: C-07
title: UI-012 InitialReviewPageComponent 骨架（4-mode，A4 grammar）
type: Cases
pages: [UI-012]
depends_on: [F-03, S-01, S-02, S-03, S-05, S-06, S-07]
parallelizable: false（波次 5，依賴 S-05/S-06/S-07）
```

**Scope：**

**Page role（固定）：** A4 review command page — reviewer 執行 draft/submit/revise initial review。
**不承接 UI-013 的 CLOSED exception branch**。

**4-mode resolution（container 計算）：**

| Mode | 條件 |
|------|------|
| NEW | Reviewer + ownership + PENDING_INITIAL_REVIEW (no existing draft) |
| EDIT_DRAFT | Reviewer + ownership + DRAFT exists |
| REVISION | Reviewer + REVISION_REQUIRED + ownership |
| READONLY_BLOCKED | Non-owner / wrong status / CLOSED / CANCELLED |

**Guard（container 內部）：**
- status_guard（4）→ ownership_guard（5）順序不可跳過
- CLOSED exception 不在此頁（UI-013 專屬）

**Regions（implement-now）：**
- C01 CaseSummaryHeader
- C02 StatusOrBlockedBanner（READONLY_BLOCKED 時）
- C03 ModeIndicatorStrip
- C15 ReviewFormSummaryStrip
- main review form region（骨架，score item-level 為 STUB）
- commentary / attachment region（骨架，attachment 互動為 STUB）
- C17 ReviewVersionSummaryRegion（REVISION mode 時）
- C16 ReviewCommandBar（viewport-fixed bottom）

**API（implement-now）：**
```
GET /review-cases/{caseId}/initial-review/current-submission
POST /review-cases/{caseId}/initial-review/save-draft
POST /review-cases/{caseId}/initial-review
POST /review-cases/{caseId}/initial-review/revise       ← REVISION mode
```

**Out of Scope：**
- Score item-level controls（由 PH-05 負責 STUB）
- Attachment 深層互動（由 PH-06 負責 STUB）

**Deliverables：**
- `cases-initial-review/initial-review-page.component.ts/html/scss`
- InitialReviewPageViewModel interface / DraftState interface
- `InitialReviewService` stubs

**Acceptance Criteria：**
1. Container 根據 role/state/ownership 輸出 4 個 mode 之一
2. C16 viewport-fixed bottom 就定位（任何視窗尺寸）
3. 主作業區底部有補白（不被 C16 遮擋）
4. READONLY_BLOCKED mode 時 C16 command bar hidden / inert
5. REVISION mode 時 C17 顯示；NEW/EDIT_DRAFT mode 時隱藏
6. 單元測試：REVISION 語意為 append-only，不可改寫現有 submission
7. 單元測試：CLOSED case 不進入任何 command mode（必須 READONLY_BLOCKED）

**Blockers/Pending Confirmation：**
- OQ-C07（save draft API 型別確認，暫用 provisional）

---

### C-08 — UI-012 ReviewFormMainArea + CommentaryAttachmentZone 骨架

```yaml
task_id: C-08
title: UI-012 ReviewFormMainArea + CommentaryAttachmentZone 骨架
type: Cases
pages: [UI-012]
depends_on: [C-07]
parallelizable: false
```

**Scope：**
建立 UI-012 的兩個 page-owned region 骨架：

**Main Review Form Region：**
- 評分表主作業區域的 region 容器
- 內部 score item-level controls 為 STUB（留 placeholder div，見 PH-05）
- Region 邊界需清楚，以便後續 item-level component 插入

**Commentary / Attachment Region：**
- 評語輸入 + 附件 region 容器
- 文字輸入 textarea 骨架
- Attachment zone 為 STUB（留 placeholder div，見 PH-06）

**Out of Scope：**
- Score item-level form controls 詳規（Must-not-invent #9）
- Attachment upload/preview/delete 互動（Must-not-invent #10）

**Deliverables：**
- `cases-initial-review/components/review-form-main-area/`
- `cases-initial-review/components/commentary-attachment-zone/`

**Acceptance Criteria：**
1. 兩個 region 有清楚 DOM 邊界
2. Score item region 內有 placeholder（`<!-- score items deferred -->`）
3. Commentary textarea 可接收輸入（基本功能）
4. Attachment zone 有 placeholder（`<!-- attachment interaction deferred -->`）

**Blockers/Pending Confirmation：** 無（placeholder 確定做）。

---

### C-09 — UI-013 RereviewPageComponent 骨架（5-mode，含 CLOSED_REVISION_EXCEPTION）

```yaml
task_id: C-09
title: UI-013 RereviewPageComponent 骨架（5-mode，含 CLOSED_REVISION_EXCEPTION）
type: Cases
pages: [UI-013]
depends_on: [F-03, S-01, S-02, S-03, S-05, S-06, S-07, C-07]
parallelizable: false（波次 5，UI-014 P3 必須在 UI-013 P8 前）
```

**Scope：**

**Page role（固定）：** A4 rereview command page — rereviewer 執行 rereview / revise rereview；另有 CLOSED exception 路徑。

**5-mode resolution（container 計算）：**

| Mode | 條件 |
|------|------|
| NEW | Rereviewer + ownership + PENDING_REREVIEW (no draft) |
| EDIT_DRAFT | Rereviewer + ownership + DRAFT exists |
| REVISION | Rereviewer + ownership + REVISION_REQUIRED |
| CLOSED_REVISION_EXCEPTION | CLOSED + original rereviewer + has base submission |
| READONLY_BLOCKED | 任何不符上述條件者 |

**CLOSED_REVISION_EXCEPTION 三個必要條件（缺一不可）：**
1. `caseState === CLOSED`
2. `user === originalRereviewer`（不是一般 rereviewer）
3. `baseSubmission exists`（有 rereview submission 可作為 revision 基礎）

**Guard 執行順序（不可跳過 ownership）：**
```
status_guard(4) → ownership_guard(5) → exception_guard(6)
```
exception_guard(6) 必須在 ownership_guard(5) 之後執行。

**API（implement-now）：**
```
GET /review-cases/{caseId}/rereview/current-submission   ← O1-C1
POST /review-cases/{caseId}/rereview                     ← O1-C2
POST /review-cases/{caseId}/rereview/revise              ← O1-C3
GET /review-cases/{caseId}/initial-review/current-submission  ← C18 reference
```

**Out of Scope：**
- C18 InitialSummaryReferenceRegion 互動（由 C-10 負責）
- CLOSED exception 下的 draft save API（OQ-C07 Deferred）
- C16 exception mode label（PH-07 STUB）

**Deliverables：**
- `cases-rereview/rereview-page.component.ts/html/scss`
- RereviewPageViewModel interface
- `RereviewService` stubs

**Acceptance Criteria：**
1. Container 根據 role/state/ownership/exception 輸出 5 個 mode 之一
2. CLOSED_REVISION_EXCEPTION 需同時滿足三條件才啟動
3. exception_guard 在 ownership_guard 之後執行（不可跳過 ownership check）
4. CLOSED_REVISION_EXCEPTION mode 時 C02 顯示 `exception-amber` tone（見 S-02）
5. CLOSED_REVISION_EXCEPTION mode 時 C03 顯示 `例外修正（已結案）`（見 S-03）
6. 單元測試：只有 originalRereviewer（非其他 rereviewer）可進入 CLOSED_REVISION_EXCEPTION
7. 單元測試：CLOSED case 的 non-owner 進入 READONLY_BLOCKED（不進 exception）

**Blockers/Pending Confirmation：**
- OQ-C06（C16 CLOSED exception button label）— 見 PH-07 STUB
- OQ-C07（revision draft save API）— Deferred
- OQ-C08（C19 live score calc timing）— 見 C-10

---

### C-10 — UI-013 InitialSummaryReferenceRegion（C18）+ FinalDecisionSummaryComponent（C19 skeleton）

```yaml
task_id: C-10
title: UI-013 InitialSummaryReferenceRegion（C18）+ FinalDecisionSummaryComponent（C19 skeleton）
type: Cases
pages: [UI-013]
depends_on: [C-09]
parallelizable: false
```

**Scope：**

**C18 InitialSummaryReferenceRegion（page-owned，不得升級為 shared）：**
- 顯示初審結果作為複審時的參考
- 唯讀，僅顯示摘要（不顯示完整評分表）
- 此 component 是 UI-013 page-owned，絕對不得抽成 shared（需要特定複審情境語意）

**C19 FinalDecisionSummaryComponent（skeleton，live calc Deferred）：**
- 顯示最終決定摘要的結構骨架
- C19 的 live score calculation 為 Deferred（等 OQ-C08 解答）
- 本任務只建立 skeleton + static mock display

**Deliverables：**
- `cases-rereview/components/initial-summary-reference-region/`
- `cases-rereview/components/final-decision-summary/` (skeleton only)

**Acceptance Criteria：**
1. C18 以 read-only 方式顯示 initial review 摘要（mock data）
2. C18 未被抽成 shared component
3. C19 骨架存在，顯示 static mock data
4. C19 有 `<!-- live-calc: deferred, pending OQ-C08 -->` 標示

**Blockers/Pending Confirmation：** OQ-C08（C19 live score calculation timing）。

---

### C-11 — UI-014 RevisionHistoryPageComponent 骨架（3-mode）+ exception entry

```yaml
task_id: C-11
title: UI-014 RevisionHistoryPageComponent 骨架（3-mode）+ exception entry
type: Cases
pages: [UI-014]
depends_on: [F-03, S-01, S-02, S-03, S-04]
parallelizable: true（Cases read-only wave，波次 3）
```

**Scope：**

**Page role（固定）：** A5 revision history — submission 的歷史紀錄與版本比較（唯讀）。

**3-mode resolution（container 計算）：**

| Mode | 條件 |
|------|------|
| FULL_HISTORY | FULL projection + case with revision history |
| METADATA_HISTORY | METADATA_ONLY projection（SYSADMIN）|
| READONLY_TERMINAL | Terminal state（CLOSED/CANCELLED），可查看但不可命令 |

**Exception entry（hasExceptionEntry 條件）：**
- CLOSED + originalRereviewer + baseSubmission → C22 顯示「前往複審修正」navigate link
- C22 是 navigate-only（warning tone outlined button）；不是 C16-style command bar

**HR-1（強制規則）：**
METADATA_HISTORY mode 時 C21 顯示：`"詳細差異內容不適用於此視角"` 文字取代差異比較。

**HR-2（強制規則）：**
C22 必須是 outlined warning tone button；不可使用 viewport-fixed 或 command bar style。

**Query state（tab 持久化）：**
- stage tab（ALL / INITIAL_ONLY / REREVIEW_ONLY）以 query params 持久化

**API（implement-now）：**
```
GET /review-cases/{caseId}/revisions
```

**Deliverables：**
- `revision-history/revision-history-page.component.ts/html/scss`
- RevisionHistoryViewModel interface
- `CasesService.getRevisionHistory()` stub

**Acceptance Criteria：**
1. Container 輸出 3 個 mode 之一
2. METADATA_HISTORY mode：C21 顯示 HR-1 固定文字（非差異比較）
3. FULL_HISTORY mode：C21 顯示版本差異摘要（mock data）
4. hasExceptionEntry = true 時：C22 顯示 outlined warning navigate button
5. hasExceptionEntry = false 時：C22 不渲染
6. Stage tabs（ALL/INITIAL_ONLY/REREVIEW_ONLY）以 query params 持久化
7. 單元測試：hasExceptionEntry 三條件缺一不可

**Blockers/Pending Confirmation：**
- OQ-C05（UI-014 O3-C extension whitelist）
- OQ-C09（compare version selection interaction）— Deferred
- OQ-C10（stage tab pre-selection strategy）— Deferred

---

### C-12 — UI-014 RevisionTimeline / RevisionCompareSummary / RevisionNavActionZone

```yaml
task_id: C-12
title: UI-014 RevisionTimeline / RevisionCompareSummary / RevisionNavActionZone
type: Cases
pages: [UI-014]
depends_on: [C-11]
parallelizable: false
```

**Scope：**

**C20 RevisionTimelineComponent（presentational）：**
- 顯示 revision 時間軸（每個版本的時間戳、作者、action type）
- Presentational component（data 由 container 輸入）
- DEFERRED：hover/expand detail、badge design tokens

**C21 RevisionCompareSummaryComponent（FULL vs METADATA_ONLY variants）：**
- FULL 模式：顯示版本間差異摘要（mock data 骨架）
- METADATA_ONLY 模式：顯示 HR-1 固定文字 `"詳細差異內容不適用於此視角"`
- Compare version selection 互動為 Deferred（OQ-C09）

**C22 RevisionNavActionZoneComponent（navigate-only）：**
- 條件顯示（hasExceptionEntry 時）
- 只有 `[前往複審修正]` navigate link（`router.navigate(['/cases', caseId, 'rereview'])`）
- 使用 outlined + warning tone（HR-2 規則）
- 不是 command bar，不是 submit button

**Out of Scope：**
- C20 hover/expand 互動（Deferred）
- Compare version selection 互動（OQ-C09 Deferred）
- Stage tab pre-selection 策略（OQ-C10 Deferred）

**Deliverables：**
- `revision-history/components/revision-timeline/`
- `revision-history/components/revision-compare-summary/`
- `revision-history/components/revision-nav-action-zone/`

**Acceptance Criteria：**
1. C20 以靜態方式顯示 3+ 個版本節點（mock data）
2. C21 METADATA_ONLY mode：顯示 HR-1 固定文字
3. C21 FULL mode：顯示版本差異骨架（mock data 或 skeleton）
4. C22 只顯示 navigate button（不顯示 confirm / submit）
5. C22 使用 outlined warning tone（視覺可識別）
6. C22 點按後 navigate 至 `/cases/:caseId/rereview`

**Blockers/Pending Confirmation：** OQ-C09（compare interaction）— Deferred。

---

## 8. Sampling Cluster Tasks（SP-series）

---

### SP-01 — UI-007 SamplingBatchListPageComponent 骨架（role-tab matrix）

```yaml
task_id: SP-01
title: UI-007 SamplingBatchListPageComponent 骨架 + role-tab matrix
type: Sampling
pages: [UI-007]
depends_on: [F-02, S-01, S-02, S-03]
parallelizable: true（Sampling wave，可與 Cases read-only wave 平行）
```

**Scope：**

**Page role（固定）：** 跨專案抽選批次列表，依角色呈現不同 tab 內容。

**Role-tab matrix（固定）：**
| Role | 預設 tab | 可見 tab |
|------|----------|---------|
| ADMIN | 全部批次 | 全部批次 / 進行中 / 已完成 / 失敗 |
| MANAGER | 我管理的專案 | 我管理的專案 / 所有批次 |
| SYSADMIN | 全部批次（METADATA_ONLY 投影） | 全部批次 |
| REVIEWER/REREVIEWER | 保守策略：deny（OQ-S05 pending） | — |

**SYSADMIN 欄位遮蔽：**
批次 metadata whitelist 待 OQ-S04 確認；本輪保守策略：只顯示 shared base 欄位，deferred 欄位做 STUB。

**Query state（tab 持久化）：**
- 目前 tab 以 `?tab=ALL_BATCHES` 等 query params 持久化

**Regions（implement-now）：**
- C01 CaseSummaryHeader（sampling context 版本）
- C02 StatusOrBlockedBanner（blocked 時）
- C03 ModeIndicatorStrip
- role-based tab 切換
- BatchListPageHeader placeholder（見 PH-02）

**API（implement-now）：**
```
GET /sampling-batches?projectId={}&status={}&hospitalId={}&page={}&size={}
POST /sampling-batches/{batchId}/retry
```

**Out of Scope：**
- SYSADMIN metadata whitelist 精確欄位（OQ-S04 Deferred）
- REVIEWER/REREVIEWER access policy（OQ-S05 Deferred，保守策略：deny）

**Deliverables：**
- `batch-list/sampling-batch-list-page.component.ts/html/scss`
- BatchListUIState interface
- `SamplingService.getBatches()` / `.retryBatch()` method stubs

**Acceptance Criteria：**
1. ADMIN role：預設顯示「全部批次」tab
2. MANAGER role：預設顯示「我管理的專案」tab
3. SYSADMIN role：顯示批次列表（只有 base whitelist 欄位）
4. Tab state 以 query params 持久化
5. `[查看案件]` navigate 按鈕存在並觸發 `/cases?batchId={batchId}`

**Blockers/Pending Confirmation：**
- OQ-S04（SYSADMIN metadata whitelist）
- OQ-S05（REVIEWER/REREVIEWER access policy）

---

### SP-02 — UI-007 BatchList 子 component 群

```yaml
task_id: SP-02
title: UI-007 BatchStatusTab / BatchFilterBar / BatchListTable / BatchRowActions
type: Sampling
pages: [UI-007]
depends_on: [SP-01]
parallelizable: false
```

**Scope：**

**BatchStatusTab：** tab 列，依 role 顯示對應 tabs。

**BatchFilterBar：** 批次篩選列（projectId / status / date range 等）。

**BatchListTable：**
- 顯示抽選批次清單
- SYSADMIN mode：使用 base whitelist 欄位（MASKED 策略）
- FAILED 狀態的 retry CTA 顯示

**BatchRowActions：**
- `[查看案件]`：navigate 到 `/cases?batchId={batchId}`（COMPLETED / FAILED rows）
- `[查看詳情]`：STUB（見 PH-01）
- `[重試]`：FAILED rows 的 retry trigger

**固定規則：**
- D-013 fixed text：retry 確認訊息使用固定文案（見 SP-03）
- SYSADMIN 的 `[查看案件]` action：metadata-only 投影不提供（hidden）

**Deliverables：**
- `batch-list/components/batch-status-tab/`
- `batch-list/components/batch-filter-bar/`
- `batch-list/components/batch-list-table/`
- `batch-list/components/batch-row-actions/`

**Acceptance Criteria：**
1. FAILED row 顯示 `[重試]` 按鈕
2. `[查看案件]` 觸發 `router.navigate(['/cases'], { queryParams: { batchId } })`
3. `[查看詳情]` 存在但點按無效（STUB）
4. SYSADMIN 不顯示 `[查看案件]`
5. 單元測試：role-tab matrix 各 role 的 visible tabs 正確

**Blockers/Pending Confirmation：** OQ-S04（SYSADMIN 欄位詳細 whitelist）。

---

### SP-03 — UI-007 RetryConfirmModal（固定文案 D-013）

```yaml
task_id: SP-03
title: UI-007 RetryConfirmModal（D-013 固定文案）
type: Sampling
pages: [UI-007]
depends_on: [SP-01]
parallelizable: true（可與 SP-02 平行）
```

**Scope：**
建立 retry 確認 modal，使用 D-013 固定文案。
D-013 文案不可自行改寫，必須來自 decision-log.md 的固定決策。

**Deliverables：**
- `batch-list/components/retry-confirm-modal/`

**Acceptance Criteria：**
1. Modal 包含 D-013 規定的固定確認文案
2. 確認後呼叫 `POST /sampling-batches/{batchId}/retry`
3. 取消後 modal 關閉，無副作用
4. 單元測試：確認按鈕觸發 retry API call

**Blockers/Pending Confirmation：** 無（D-013 固定文案已決定）。

---

### SP-04 — UI-006 SamplingExecutePageComponent 骨架（6-mode）

```yaml
task_id: SP-04
title: UI-006 SamplingExecutePageComponent 骨架（6-mode state machine）
type: Sampling
pages: [UI-006]
depends_on: [F-02, S-01, S-02, S-03]
parallelizable: false（依賴 SP-01 wave 之後，但可與 Cases command wave 平行）
```

**Scope：**

**Page role（固定）：** 抽選執行頁，單一 project 的抽選流程控制。

**6-mode state machine（container 計算）：**

| Mode | 條件 |
|------|------|
| IDLE | 設定未完成，抽選尚未開始 |
| READY | 設定完成，等待執行 |
| PREVIEW | 預覽已產生，等待確認 |
| EXECUTING | 執行中（非同步）|
| EXECUTE_SUCCESS | 執行成功 |
| READONLY_BLOCKED | 4 種阻斷原因之一（見下方）|

**READONLY_BLOCKED 4 種原因（必須對應顯示）：**
1. 非 ADMIN/MANAGER role
2. 非本 project scope
3. SamplingBatch EXECUTING（其他批次執行中）
4. Project 已超過 sampling quota

**Regions（implement-now）：**
- C01 CaseSummaryHeader（project context 版本）
- C02 StatusOrBlockedBanner（READONLY_BLOCKED 時）
- C03 ModeIndicatorStrip
- SamplingPageHeader placeholder（見 PH-02）
- ProjectId 顯示 region

**API（implement-now）：**
```
GET /review-projects/{projectId}/sampling/config
POST /review-projects/{projectId}/sampling/preview
POST /review-projects/{projectId}/sampling/execute
GET /review-projects/{projectId}/sampling/status
```

**Out of Scope：**
- EXECUTING animation spec（Must-not-invent #15，Deferred）
- Confirm modal 的 non-fixed 文案部分（Must-not-invent）
- 4th preview stat card selectedCount（OQ-S01 Deferred）

**Deliverables：**
- `sampling-execute/sampling-execute-page.component.ts/html/scss`
- SamplingExecuteViewModel interface
- `SamplingService.getConfig()` / `.preview()` / `.execute()` / `.getStatus()` stubs

**Acceptance Criteria：**
1. Container 根據 role/scope/batchState/quota 輸出 6 個 mode 之一
2. READONLY_BLOCKED mode 時 C02 顯示 4 種原因中的對應原因
3. EXECUTE_SUCCESS 顯示兩個 CTA：`[前往抽選批次列表]` 和 `[前往案件列表]`
4. 單元測試：6 種 mode 的 container 計算
5. 單元測試：READONLY_BLOCKED 各原因對應正確 banner 文案

**Blockers/Pending Confirmation：**
- OQ-S01（selectedCount — 4th stat card）
- OQ-S02（post-execute timing fields）
- CC-03（EXECUTE_SUCCESS `[前往案件列表]` batchId param，Provisional）

---

### SP-05 — UI-006 SamplingRuleSummary + SamplingPreviewStats（前 3 card）

```yaml
task_id: SP-05
title: UI-006 SamplingRuleSummary + SamplingPreviewStats（前 3 card）
type: Sampling
pages: [UI-006]
depends_on: [SP-04]
parallelizable: false
```

**Scope：**
建立 PREVIEW mode 的主要展示 component：

**SamplingRuleSummaryComponent：**
- 顯示抽選規則摘要（projectId / rule type / rule params）
- 來自 `GET /review-projects/{projectId}/sampling/config` 回傳

**SamplingPreviewStatsComponent（前 3 card，第 4 card Deferred）：**
- Card 1：候選病歷數（eligible count）
- Card 2：抽選比例或數量
- Card 3：預計抽選結果數
- Card 4（Deferred）：selectedCount（OQ-S01 — 不建立）

**Deliverables：**
- `sampling-execute/components/sampling-rule-summary/`
- `sampling-execute/components/sampling-preview-stats/`

**Acceptance Criteria：**
1. 顯示前 3 個 stat card（mock data）
2. 第 4 card 完全不存在（`<!-- OQ-S01: deferred -->` 標示）
3. Rule summary 顯示規則 snapshot 摘要

**Blockers/Pending Confirmation：** OQ-S01（4th card selectedCount）。

---

### SP-06 — UI-006 SamplingPostExecuteRegion + BLOCKED 4-reason banner

```yaml
task_id: SP-06
title: UI-006 SamplingPostExecuteRegion + READONLY_BLOCKED 4-reason banner
type: Sampling
pages: [UI-006]
depends_on: [SP-04]
parallelizable: true（可與 SP-05 平行）
```

**Scope：**

**SamplingPostExecuteRegion（EXECUTE_SUCCESS mode）：**
- 顯示執行成功後的結果摘要
- 兩個 CTA：`[前往抽選批次列表]`（Fixed）和 `[前往案件列表]`（Provisional, CC-03）
- Post-execute timing fields（OQ-S02 Deferred）

**READONLY_BLOCKED 4-reason content（C02 feeding）：**
配合 C02 banner 呈現 4 種原因的具體說明文字。

**Deliverables：**
- `sampling-execute/components/sampling-post-execute-region/`
- READONLY_BLOCKED 各原因的 banner message 對應（可以 `@Input()` 傳入 reason enum）

**Acceptance Criteria：**
1. EXECUTE_SUCCESS mode 時兩個 CTA 可見且觸發正確 navigate
2. `[前往抽選批次列表]` → `router.navigate(['/sampling/batches'])`
3. `[前往案件列表]` → `router.navigate(['/cases'], { queryParams: { batchId } })`（Provisional）
4. 4 種 blocked reason 各自顯示對應 banner 文案
5. OQ-S02 timing fields 區域有 `<!-- timing: deferred, pending OQ-S02 -->` 標示

**Blockers/Pending Confirmation：**
- OQ-S02（post-execute timing fields）
- CC-03（`[前往案件列表]` batchId param 確認）

---

### SP-07 — UI-006 SamplingExecuteConfirmModal 結構

```yaml
task_id: SP-07
title: UI-006 SamplingExecuteConfirmModal 結構
type: Sampling
pages: [UI-006]
depends_on: [SP-04]
parallelizable: true（可與 SP-05/SP-06 平行）
```

**Scope：**
建立抽選執行確認 modal 結構。
Modal 呈現在 PREVIEW mode 使用者按下「執行抽選」後。

**固定文案部分：** 必須來自現有 page spec / wireframe 決策（不自行發明）。
**非固定文案部分（Must-not-invent）：** 若 page spec 或 wireframe 中無固定文案，留 `TODO: confirm copy` 標示。

**Deliverables：**
- `sampling-execute/components/sampling-execute-confirm-modal/`

**Acceptance Criteria：**
1. Modal 在 PREVIEW mode 可觸發
2. 確認後觸發 `POST /review-projects/{projectId}/sampling/execute`
3. 取消後 modal 關閉

**Blockers/Pending Confirmation：** Must-not-invent（confirm modal 文案不自行發明）。

---

## 9. Placeholder / STUB Preservation Tasks（PH-series）

---

### PH-01 — [查看詳情] no-op button STUB（UI-007）

```yaml
task_id: PH-01
title: "[查看詳情] no-op STUB（UI-007）"
type: Placeholder
pages: [UI-007]
depends_on: [SP-02]
parallelizable: true
```

**Scope：**
確保 `[查看詳情]` 按鈕在 UI-007 BatchRowActions 中：
- **可見**（用戶看得到）
- **點按無效**（click handler 空或 no-op）
- **不得綁定任何 route**（UI-008 page spec 不存在）

**禁止事項（Must-not-invent #3）：** 不得自行定義 `/sampling/batches/:batchId` 或任何 UI-008 目標 route。

**Acceptance Criteria：**
1. `[查看詳情]` 按鈕存在且可見
2. 點按後無任何導航發生
3. button 可附帶 `disabled` 樣式或 tooltip：「詳情頁開發中」
4. 無任何 route target 被定義

---

### PH-02 — SamplingPageHeader / BatchListPageHeader placeholder div

```yaml
task_id: PH-02
title: SamplingPageHeader / BatchListPageHeader placeholder div
type: Placeholder
pages: [UI-006, UI-007]
depends_on: [F-02]
parallelizable: true
```

**Scope：**
在 UI-006 和 UI-007 各建立一個 header placeholder div，標示 page header 位置。
不建立真實 SamplingPageHeader component（Must-not-invent #16：API 尚未定義）。

**Acceptance Criteria：**
1. UI-006 有 `<div class="page-header-placeholder"><!-- sampling-page-header: pending --></div>`
2. UI-007 有 `<div class="page-header-placeholder"><!-- batch-list-page-header: pending --></div>`
3. 兩個 div 不影響頁面其他佈局

---

### PH-03 — C14 RereviewerAssignmentSection picker slot（UI-011）

```yaml
task_id: PH-03
title: C14 picker slot STUB（UI-011）
type: Placeholder
pages: [UI-011]
depends_on: [C-06]
parallelizable: true
```

**Scope：**
在 C14 RereviewerAssignmentSection 與 C13 ReviewerAssignmentSection 中，
讓 picker 入口存在但互動為 STUB：
- 「選擇審查人員」/ 「選擇複審人員」按鈕可見
- 點按後無任何 modal/dropdown 展開
- Must-not-invent #8：不自行定義 picker 的搜尋或 autocomplete 規格

**Acceptance Criteria：**
1. 選人按鈕在 ASSIGN/REASSIGN mode 可見
2. 點按後無 modal/dropdown 開啟
3. READONLY_BLOCKED mode 時按鈕不渲染

---

### PH-04 — C23 AssignmentDueDateHelper placeholder slot（UI-011）

```yaml
task_id: PH-04
title: C23 due date helper placeholder slot（UI-011）
type: Placeholder
pages: [UI-011]
depends_on: [C-06]
parallelizable: true
```

**Scope：**
在 UI-011 assignment section 建立 C23 的可插拔 slot，現在只放 placeholder。
不實作任何 due date 欄位或驗證（Must-not-invent 衍伸）。

**Acceptance Criteria：**
1. `<div class="due-date-helper-slot"><!-- C23: deferred --></div>` 存在
2. Slot 不影響其他 section 佈局

---

### PH-05 — Score item-level region placeholder（UI-012/013）

```yaml
task_id: PH-05
title: Score item-level region placeholder（UI-012 + UI-013）
type: Placeholder
pages: [UI-012, UI-013]
depends_on: [C-08, C-10]
parallelizable: true
```

**Scope：**
在 UI-012 ReviewFormMainArea 和 UI-013 RereviewPageComponent 的主作業區，
留 score item-level 的 placeholder region：
```html
<!-- score-items: deferred, pending item-level control library spec -->
<div class="score-items-placeholder">評分項目（開發中）</div>
```

**Acceptance Criteria：**
1. Placeholder div 存在於 UI-012 和 UI-013 的主作業區
2. Placeholder 有明確文案說明（不是空白區域）
3. Placeholder 不影響 C15/C16/C17 的正常渲染

---

### PH-06 — Commentary/attachment zone placeholder（UI-012/013）

```yaml
task_id: PH-06
title: Commentary/attachment zone placeholder（UI-012 + UI-013）
type: Placeholder
pages: [UI-012, UI-013]
depends_on: [C-08, C-10]
parallelizable: true
```

**Scope：**
在 UI-012 CommentaryAttachmentZone 和 UI-013 的對應 region，
讓 commentary textarea 可用（基本）但 attachment zone 為 STUB：
- Commentary textarea：可輸入文字（基本功能即可）
- Attachment zone：僅有 placeholder div，must-not-invent #10

**Acceptance Criteria：**
1. Commentary textarea 可接收文字輸入
2. Attachment zone 有 placeholder（`<!-- attachment interaction: deferred, pending #10 -->`）
3. Attachment zone 無任何可互動的 upload button

---

### PH-07 — C16 command label PLACEHOLDER for CLOSED exception（UI-013）

```yaml
task_id: PH-07
title: C16 button label PLACEHOLDER for CLOSED_REVISION_EXCEPTION（UI-013）
type: Placeholder
pages: [UI-013]
depends_on: [C-09]
parallelizable: true
```

**Scope：**
在 UI-013 的 CLOSED_REVISION_EXCEPTION mode 下，C16 的 primary action button label
使用 PLACEHOLDER 文案（例如：`「複審修正（待定）」`）。

原因：OQ-C06（Must-not-invent #11）— 此 mode 下的 button label 尚未由 authority 確認。

**Acceptance Criteria：**
1. CLOSED_REVISION_EXCEPTION mode 下 C16 primary button 顯示 PLACEHOLDER 文案
2. 文案格式：`「動作名稱（待定）」` 或類似標示
3. 正常 mode（NEW/EDIT_DRAFT/REVISION）的 C16 label 不受影響

---

### PH-08 — C24 RevisionHistoryJumpEntry slot（UI-010）

```yaml
task_id: PH-08
title: C24 RevisionHistoryJumpEntry placeholder slot（UI-010）
type: Placeholder
pages: [UI-010]
depends_on: [C-04]
parallelizable: true
```

**Scope：**
在 UI-010 CaseDetailPage 的適當位置建立 C24 的 placeholder slot。
C24 的最終位置待 OQ-C02 確認，本輪只保留可插拔位置。

**Acceptance Criteria：**
1. `<div class="revision-history-jump-slot"><!-- C24: pending OQ-C02 --></div>` 存在
2. Slot 位置不影響 C08/C09/C10/C11 的正常佈局

---

## 10. Cross-Cutting Verification Tasks（V-series）

---

### V-01 — Route smoke test（8 routes 可解析；UI-008 不存在）

```yaml
task_id: V-01
title: Route smoke test
type: Verification
pages: [全域]
depends_on: [F-02, F-03]
parallelizable: false（建立後即可執行）
```

**驗收清單（8 routes）：**

| URL | Expected Component |
|-----|-------------------|
| `/sampling/batches` | SamplingBatchListPageComponent |
| `/sampling/:projectId` | SamplingExecutePageComponent |
| `/cases` | CaseListPageComponent |
| `/cases/:caseId` | CaseDetailPageComponent |
| `/cases/:caseId/assign` | CaseAssignmentPageComponent |
| `/cases/:caseId/initial-review` | InitialReviewPageComponent |
| `/cases/:caseId/rereview` | RereviewPageComponent |
| `/cases/:caseId/revisions` | RevisionHistoryPageComponent |

**必須不存在：**
- `/sampling/batches/:batchId`（UI-008）
- 任何 UI-008 相關 route

**Acceptance Criteria：**
1. 上表 8 個 URL 無一 404
2. `batches` literal route 在 `:projectId` 之前被匹配（`/sampling/batches` 不被誤匹配為 projectId = 'batches'）
3. 無 UI-008 相關 route 存在

---

### V-02 — Guard passthrough 驗證

```yaml
task_id: V-02
title: Guard passthrough 驗證（RoleGuard / ScopeGuard / ProjectionGuard 不阻斷任何路由）
type: Verification
pages: [全域]
depends_on: [F-04]
parallelizable: true
```

**Acceptance Criteria：**
1. 三個 guard 均 return `true`
2. 任何 URL 訪問不因 guard 返回 `false` 而被阻斷（開發期）
3. Guard 有 unit test：三個 guard 各自 return `true`

---

### V-03 — Cluster isolation 驗證（無跨 cluster service import）

```yaml
task_id: V-03
title: Cluster isolation 驗證
type: Verification
pages: [全域]
depends_on: [SP-01～SP-07, C-01～C-12]
parallelizable: false（所有 SP 和 C 任務完成後）
```

**驗收清單：**
1. `features/sampling/` 路徑下沒有任何 import 自 `features/cases/` 的 service 或 component
2. `features/cases/` 路徑下沒有任何 import 自 `features/sampling/` 的 service 或 component
3. 唯一合法跨 cluster 呼叫：`router.navigate(['/cases'], { queryParams: { batchId } })`（SP-02 的 `[查看案件]`）

**驗核方式：** `grep -r "features/cases" features/sampling/` 和反向搜尋，預期無 service/component import。

---

### V-04 — Mode resolution contract 審核

```yaml
task_id: V-04
title: Mode resolution contract 審核（mode 來源一致性）
type: Verification
pages: [全域]
depends_on: [C-03, C-05, C-07, C-09, C-11]
parallelizable: false
```

**驗收清單（逐頁容器審核）：**
1. **UI-010**：mode 來自 `(role + caseState + ownership + projectionType)` 計算，無 `activatedRoute.snapshot.queryParams.mode` 字樣
2. **UI-011**：mode 來自 container 計算，非 URL
3. **UI-012**：mode 來自 container，CLOSED exception 分支不存在（UI-013 專有）
4. **UI-013**：5-mode 計算在 container；exception_guard 在 ownership_guard 之後
5. **UI-014**：mode 來自 container，hasExceptionEntry 三條件各自被驗證

**違規型態（必須不存在）：**
- `this.mode = this.route.snapshot.queryParams['mode']`
- presentational component 內部有 mode 判斷 if/switch

---

### V-05 — Must-not-invent 16 條 audit

```yaml
task_id: V-05
title: Must-not-invent 16 條全域 audit
type: Verification
pages: [全域]
depends_on: [所有任務完成後]
parallelizable: false
```

**Audit checklist（依 §3.3 16 條）：**

- [ ] 1. UI-008 route/component 不存在
- [ ] 2. Shell nav entry key / MFE federation name 未硬寫（待 CC-01/CC-02）
- [ ] 3. `[查看詳情]` 無 route target（STUB no-op）
- [ ] 4. `[前往案件列表]` batchId param 已標示 Provisional（CC-03）
- [ ] 5. OQ-S01 4th stat card 不存在（Deferred）
- [ ] 6. OQ-S04 SYSADMIN whitelist 欄位未硬寫（Deferred）
- [ ] 7. OQ-S05 REVIEWER/REREVIEWER access 保守策略（deny，待 OQ-S05）
- [ ] 8. C14 picker 深層互動未實作（STUB slot）
- [ ] 9. Score item-level controls 未發明（Placeholder）
- [ ] 10. Attachment interaction 未發明（Placeholder）
- [ ] 11. C16 CLOSED exception label 為 PLACEHOLDER（PH-07）
- [ ] 12. D-013 retry 文案以外無自行發明
- [ ] 13. Compare version interaction 未實作（Deferred）
- [ ] 14. C24 位置為 placeholder slot（PH-08）
- [ ] 15. EXECUTING animation 未實作（Deferred）
- [ ] 16. SamplingPageHeader API 未自行發明（PH-02 placeholder）

---

### V-06 — STUB boundary 驗收

```yaml
task_id: V-06
title: STUB boundary 驗收（STUB 存在但不可互動）
type: Verification
pages: [全域]
depends_on: [所有 PH 任務完成後]
parallelizable: false
```

**驗收清單：**

| STUB 項目 | 必做（可見）| 必不做（不可互動）|
|-----------|------------|-----------------|
| PH-01 `[查看詳情]` | 按鈕可見 | 點按不導航 |
| PH-02 page header placeholder | div 存在 | 無 component 邏輯 |
| PH-03 picker slot | 選人按鈕可見 | 點按無 modal |
| PH-04 C23 due date slot | div 存在 | 無欄位輸入 |
| PH-05 score item placeholder | div 可見 | 無 form control |
| PH-06 attachment placeholder | div 可見 | 無 upload trigger |
| PH-07 C16 exception label | PLACEHOLDER 文案可見 | 非正式命令 |
| PH-08 C24 slot | div 存在 | 無 navigate action |

---

## 11. Recommended Execution Waves

### 波次規劃（建議執行順序）

```
Wave 1 — Foundation（必須最先完成，所有後續任務均依賴）
  F-01: App Routing lazy-load entries
  F-02: Sampling Feature Module 骨架
  F-03: Cases Feature Module 骨架          ← F-02 / F-03 可平行
  F-04: Route Guard 骨架（三組）

Wave 2 — Shared Components（可與 Wave 1 中的 F-01 後平行開始）
  S-01: CaseSummaryHeaderComponent（C01 +C04 inline slot）
  S-02: StatusOrBlockedBannerComponent（C02）
  S-03: ModeIndicatorStripComponent（C03）
  S-04: ProjectionNoticeBannerComponent（C04）
  S-05: ReviewFormSummaryStripComponent（C15）
  S-06: ReviewCommandBarComponent（C16, viewport-fixed）
  S-07: ReviewVersionSummaryRegionComponent（C17）
  PH-02: Header placeholder div（UI-006/007）
  ← S-01 ~ S-07 + PH-02 可完全平行

Wave 3 — Cases Read-only（依賴 Wave 2 之 S-01/S-02/S-03/S-04）
  Start gate:
    - `npm run typecheck` 綠燈
    - `npm run test:unit:ci` 綠燈，且已存在 shared component baseline（C01）與 cases container baseline（UI-009）
    - `npm run test:e2e` 綠燈，且 smoke 至少覆蓋 `/` 與 `/cases`
    - 先建立 Wave 3 目標頁（UI-009/UI-010/UI-014）的 unit 測試，再開始功能變更
  C-01: UI-009 CaseListPageComponent 骨架
  C-03: UI-010 CaseDetailPageComponent 骨架
  C-11: UI-014 RevisionHistoryPageComponent 骨架
  V-01: Route smoke test（可在此波次後執行）
  Exit gate:
    - `npm run typecheck`
    - `npm run test:unit:ci`
    - `npm run test:e2e`
    - Wave 3 新增或變更的 read-only route / mode / navigate-only 行為均有對應 unit 測試與 smoke 驗證
  ← C-01 / C-03 / C-11 可平行（但需等 S-01~S-04）

Wave 4 — Sampling（依賴 Wave 2 之 S-01/S-02/S-03；可與 Wave 3 平行）
  Start gate:
    - Wave foundation gate 持續為綠燈：`npm run typecheck`、`npm run test:unit:ci`、`npm run test:e2e`
    - Wave 4 先補 sampling route / role tab / placeholder 或 mode smoke 所需測試，再開始 UI-007 / UI-006 功能
    - e2e smoke 至少已可進入 `/sampling/batches`；若 sampling smoke route 不可進入，不得開始 Wave 4
  SP-01: UI-007 SamplingBatchListPageComponent 骨架
  SP-04: UI-006 SamplingExecutePageComponent 骨架
  Exit gate:
    - `npm run typecheck`
    - `npm run test:unit:ci`
    - `npm run test:e2e`
    - `/sampling/batches` 與 `/sampling/:projectId` 至少各有 1 個 smoke 驗證；sampling role/tab 或 mode baseline 已有 unit coverage
  ← SP-01 / SP-04 可平行

Wave 3+4 Extensions（依賴各自的 Wave 3/4 骨架）
  C-02: UI-009 WorkQueue 子 component 群
  C-04: UI-010 CaseControlCenter 子 component 群
  C-12: UI-014 RevisionTimeline / CompareSummary / NavActionZone
  SP-02: UI-007 BatchList 子 component 群
  SP-03: UI-007 RetryConfirmModal
  SP-05: UI-006 SamplingRuleSummary + PreviewStats
  SP-06: UI-006 PostExecuteRegion + BLOCKED banner
  SP-07: UI-006 ExecuteConfirmModal
  PH-01, PH-08: 對應 STUB
  ← 各依賴其 page 骨架

Wave 5 — Cases Command Pages（依賴 Wave 2 + Wave 3 + S-05/S-06/S-07；UI-014 須在 UI-013 前完成）
  Start gate:
    - Wave 3 與 Wave 4 exit gate 均已通過
    - `npm run typecheck`、`npm run test:unit:ci`、`npm run test:e2e` 仍維持綠燈
    - 先建立 command page 測試（guard / ownership / mode / command bar 可視狀態），再開始 UI-011 / UI-012 / UI-013 功能
    - 若 e2e 仍為 partial 或 smoke route 尚未穩定，Wave 5 不得開始
  C-05: UI-011 CaseAssignmentPageComponent 骨架
  C-07: UI-012 InitialReviewPageComponent 骨架（依賴 S-05/S-06/S-07）
  C-09: UI-013 RereviewPageComponent 骨架（依賴 C-07，因 A4 grammar 承接）
  Exit gate:
    - `npm run typecheck`
    - `npm run test:unit:ci`
    - `npm run test:e2e`
    - command page 的 route entry、guard、mode resolution、command region 可視條件都已有 unit coverage；至少 1 條 command page smoke route 可進入
  ← C-05 可與 C-07 平行，但 C-09 必須在 C-07 後

Wave 5 Extensions
  C-06: UI-011 AssignmentSection 子 component 群
  C-08: UI-012 ReviewFormMainArea + CommentaryAttachmentZone
  C-10: UI-013 InitialSummaryReference + FinalDecisionSummary
  PH-03, PH-04, PH-05, PH-06, PH-07: 對應 STUB

Wave 6 — Verification（所有任務完成後）
  V-02: Guard passthrough 驗證
  V-03: Cluster isolation 驗證
  V-04: Mode resolution contract 審核
  V-05: Must-not-invent 16 條 audit
  V-06: STUB boundary 驗收
```

### Wave test-first gate reset

| Wave | Pre-start required | Exit gate required | 若 gate 未過的處置 |
|---------|------|---------|-------------|
| Wave 3 | `npm run typecheck`、`npm run test:unit:ci`、`npm run test:e2e` 全部通過；shared component baseline + cases container baseline 已存在 | 同三個 command 全綠；read-only route / navigate-only / mode 變更均有 unit + smoke | 不得進入 Wave 4 |
| Wave 4 | Wave foundation gate 持續全綠；sampling smoke route 已可進入；sampling wave 測試先定義 | 同三個 command 全綠；`/sampling/batches`、`/sampling/:projectId` smoke 至少各 1 條 | 不得進入 Wave 5 |
| Wave 5 | Wave 3 + Wave 4 exit gate 已通過；command page 測試先定義；e2e 非 partial | 同三個 command 全綠；command page route / guard / ownership / mode / command region 有 unit coverage，且至少 1 條 smoke route 可進入 | 不得進入下一 wave |

補充規則：
- `required`：`npm run typecheck`、`npm run test:unit:ci`、`npm run test:e2e`
- `required`：每個 wave 的測試必須先寫，再做功能
- `required`：若 `npm run test:e2e` 回到 partial（例如 `No tests found`），只允許修補測試基礎，不允許繼續 Wave 3 / Wave 4 / Wave 5
- `recommended`：新增 route 時同步擴充 smoke route 清單；新增 mode 時同步補 mode resolver unit 測試

### 波次並行矩陣摘要

| Wave | 必要前置 | 可平行項目 |
|------|---------|-----------|
| Wave 1 | — | F-02 // F-03 |
| Wave 2 | F-01 | S-01~S-07 + PH-02 全部平行 |
| Wave 3 | S-01~S-04 | C-01 // C-03 // C-11 |
| Wave 4 | S-01~S-03 | SP-01 // SP-04（可與 Wave 3 平行）|
| Wave 5 | S-05/S-06/S-07 + C-11（P3）| C-05 // C-07；C-09 需等 C-07 |
| Wave 6 | 所有任務 | V-02~V-06 部分可平行 |

---

## 12. External Dependencies / Pending Confirmation Map

以下 pending item 目前阻斷或暫緩部分交付，須於實作前確認。

### Sampling 相關

| OQ ID | 問題 | 影響 | 建議臨時處理 |
|-------|------|------|------------|
| OQ-S01 | API response 是否含 `selectedCount` | UI-006 第 4 個 preview stat card | **Deferred（不建立）** |
| OQ-S02 | post-execute timing fields 來源（即時 / 異步 polling）| UI-006 EXECUTE_SUCCESS 時間顯示欄位 | Deferred，region 留 placeholder comment |
| OQ-S04 | SYSADMIN `SamplingBatch` metadata whitelist 精確欄位 | UI-007 SYSADMIN mode 欄位遮蔽 | 保守策略：只顯示 base whitelist，DEFERRED columns 不建 |
| OQ-S05 | REVIEWER / REREVIEWER 是否可通過 `/sampling/batches` route guard | UI-007 guard 邏輯 | 保守策略：deny，guard 實作時再補 |
| CC-03 | UI-006 EXECUTE_SUCCESS `[前往案件列表]` batchId query param 是否傳入 | UI-006 CTA navigate target | Provisional（目前傳 batchId，待確認） |

### Cases 相關

| OQ ID | 問題 | 影響 | 建議臨時處理 |
|-------|------|------|------------|
| OQ-C01 | C10/C11 ROLE_LIMITED visibility rules | UI-010 reviewer/rereviewer assignment info 欄位 | Deferred，現顯示 base info（不建 ROLE_LIMITED 邏輯）|
| OQ-C02 | C24 RevisionHistoryJumpEntry 最終位置 | UI-010 佈局 | Deferred，placeholder slot（PH-08）|
| OQ-C03 | O3-C UI-010 extension whitelist | UI-010 SYSADMIN 欄位 | Deferred，只顯示 base whitelist |
| OQ-C04 | O3-C UI-009 extension whitelist | UI-009 SYSADMIN 欄位 | Deferred，只顯示 base whitelist |
| OQ-C05 | O3-C UI-014 extension whitelist | UI-014 SYSADMIN 欄位 | Deferred，只顯示 base whitelist |
| OQ-C06 | C16 button label for CLOSED_REVISION_EXCEPTION mode | UI-013 example button text | STUB PLACEHOLDER（PH-07）|
| OQ-C07 | CLOSED_REVISION_EXCEPTION 下的 revision draft save API | UI-013 exception mode draft save | Deferred，exception mode 不提供 save draft |
| OQ-C08 | C19 FinalDecisionSummary live score calculation timing | UI-013 C19 即時計算 | Deferred，skeleton + static mock |
| OQ-C09 | compare version selection UI interaction | UI-014 版本選擇互動 | Deferred，無 compare selector |
| OQ-C10 | UI-014 stage tab pre-selection strategy | UI-014 tab 預選邏輯 | Deferred，預設 ALL tab |

### Cross-cutting 相關

| CC ID | 問題 | 影響 | 建議臨時處理 |
|-------|------|------|------------|
| CC-01 | Shell navigation entry key for「抽選作業」和「案件管理」 | MFE shell integration 的 nav link | 不影響 feature module / route；影響 shell integration（暫不實作）|
| CC-02 | MFE federation remote name for sampling / cases module | 正式 MFE 整合時的模組識別 | 不影響本輪 skeleton；影響正式 MFE 整合 |
| CC-04 | Cases cluster back navigation query context restore 策略 | UI-009 back navigation 從深頁回來的 filter/tab 還原 | Deferred，只保存基本 query params；完整 context restore 待確認 |

---

## 13. Final Delivery Boundary

### Implement Now（本輪確定交付）

| 分類 | 任務 |
|------|------|
| Foundation | F-01, F-02, F-03, F-04 |
| Shared | S-01, S-02, S-03, S-04, S-05, S-06, S-07 |
| Sampling | SP-01, SP-02, SP-03, SP-04, SP-05, SP-06, SP-07 |
| Cases | C-01, C-02, C-03, C-04, C-05, C-06, C-07, C-08, C-09, C-10, C-11, C-12 |
| Placeholder | PH-01, PH-02, PH-03, PH-04, PH-05, PH-06, PH-07, PH-08 |
| Verification | V-01, V-02, V-03, V-04, V-05, V-06 |

---

### STUB Only（本輪可見但不可互動）

| STUB 項目 | Task | 理由 |
|-----------|------|------|
| `[查看詳情]` no-op button | PH-01 | UI-008 page spec 不存在 |
| SamplingPageHeader / BatchListPageHeader div | PH-02 | Must-not-invent #16 |
| C14 picker slot | PH-03 | Must-not-invent #8 |
| C23 due date helper slot | PH-04 | Deferred |
| Score item region | PH-05 | Must-not-invent #9 |
| Commentary attachment zone | PH-06 | Must-not-invent #10 |
| C16 CLOSED exception label | PH-07 | OQ-C06（Must-not-invent #11）|
| C24 RevisionHistoryJump slot | PH-08 | OQ-C02（Deferred）|

---

### Deferred（本輪完全不做，不得留 TODO 之外的程式碼）

| 項目 | 原因 |
|------|------|
| UI-006 第 4 個 preview stat card | OQ-S01：API response 未確認 |
| UI-006 EXECUTE_SUCCESS timing fields | OQ-S02：資料來源未確認 |
| UI-006 EXECUTING animation | Must-not-invent #15：spec 不存在 |
| UI-007 SYSADMIN metadata 額外欄位 | OQ-S04：whitelist 未確認 |
| REVIEWER/REREVIEWER sampling access | OQ-S05：permission 未決 |
| UI-009 bulk action / export | Deferred scope |
| UI-010 C10/C11 ROLE_LIMITED visibility | OQ-C01：規則未確認 |
| UI-010 C24 最終位置 | OQ-C02：設計未決 |
| UI-009/010/014 O3-C extension whitelist 額外欄位 | OQ-C03/C04/C05 |
| UI-013 exception mode draft save | OQ-C07：API 未確認 |
| UI-013 C19 live score calculation | OQ-C08：timing 未決 |
| UI-014 compare version interaction | OQ-C09：互動規格未定 |
| UI-014 stage tab pre-selection | OQ-C10：策略未定 |
| Shell nav integration | CC-01/CC-02：MFE contract 無 evidence |
| Cases cluster 完整 context restore | CC-04：策略未定 |
