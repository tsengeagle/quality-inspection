# sampling-route-module-map-addendum.md

## 文件狀態
- **類型**：Addendum（補件）
- **Layer**：Layer 2 Repo-Aligned Frontend Bridge
- **範圍**：Sampling cluster — UI-006 / UI-007
- **用途**：補充 `ui-route-module-map.md` 未涵蓋的 Sampling cluster，解除 `ready-pages-implementation-handoff.md` 中標注的 BLOCKING item

---

## 1. Document Purpose

本文件是 `ui-route-module-map.md` 的最小必要補件。

現行 `ui-route-module-map.md` 的範圍宣告為：

> 範圍：僅 cases 主幹（UI-009~UI-014）

這造成 Sampling cluster（UI-006 / UI-007）在 `ready-pages-implementation-handoff.md` 將其列為 ready page 後，**仍缺少正式 route / feature module 落點**，是前端 skeleton 開始前的阻塞點。

本文件的目的：

1. 補出 Sampling cluster 的 feature module 邊界
2. 固定 UI-006 / UI-007 的 route path 提案
3. 明確定義 cross-cluster navigation contract
4. 標示 STUB/deferred 邊界（UI-008 未 ready）
5. 區分已固定與尚待確認的決策

本文件**不**：

- 重寫或取代 `ui-route-module-map.md`
- 定義 UI-008 page spec
- 發明 shell / MFE / federation contract
- 展開新的 API contract

---

## 2. Authority Sources Used

| 文件 | 用途 |
|------|------|
| `docs/06-frontend/ready-pages-implementation-handoff.md` §2.1 / §2.2 | UI-006 / UI-007 handoff；route 建議來源；blocking item 確認 |
| `docs/06-frontend/ui-route-module-map.md` | Cases cluster route 命名風格參照；guard 邊界模式 |
| `docs/07-design/wireframes/ui006-sampling-execute-first-pass.md` | UI-006 region map、mode set、cross-cluster CTA |
| `docs/07-design/wireframes/ui007-batch-list-first-pass.md` | UI-007 region map、row action grammar、cross-cluster navigation 明確標注 |
| `docs/05-ui/ui-page-spec-ui006抽選執行頁.md` | API 錨點：`POST /review-projects/{projectId}/sampling/...`；page role |
| `docs/05-ui/ui-page-spec-ui007抽選批次列表頁.md` | 對應 IA 模組「抽選作業」；page role |
| `docs/05-ui/ui-information-architecture.md` | IA 模組結構：「抽選作業」為 top-level nav 模組 |
| `docs/03-contracts/api-spec.md` §9 | API path 確認：`/review-projects/{projectId}/sampling/preview|execute`、`GET /sampling-batches`、`GET /review-cases?batchId=` |
| `frontends/quality-inspection-frontend/src/app/app-routing.module.ts` | 現行 Angular routing skeleton 確認（目前空骨架） |
| `frontends/quality-inspection-frontend/src/app/features/` | 現行 features 資料夾確認（空）；`features/sampling/` 尚未建立 |

---

## 3. Why This Addendum Exists

### 3.1 Present-state gap

`ready-pages-implementation-handoff.md` 明確標注以下 BLOCKING items：

**UI-006（§2.1.2 / §2.1.10 / §2.1.12）**：

```
Sampling cluster route path（BLOCKING）：實作前必須先定義 sampling route module。
```

**UI-007（§2.2.2 / §2.2.10 / §2.2.12）**：

```
Sampling cluster route path（BLOCKING）：實作前需先定義。
```

### 3.2 Root cause

`ui-route-module-map.md` 明確宣告其範圍為 **cases 主幹（UI-009~UI-014）**，Sampling cluster route 從未被正式定義於任何 frontend bridge 文件中。

### 3.3 What this addendum resolves

- Sampling cluster feature module 邊界（固定）
- UI-006 / UI-007 route path 提案（固定，標明根據）
- Cross-cluster navigation contract（固定部分；存在未定項）
- STUB 邊界（UI-008）的表達方式
- 在 Angular skeleton 可安全開始實作前需知道的全部 routing 決策

---

## 4. Sampling Cluster Feature Boundary

### 4.1 結論

**Sampling 應有獨立的 feature module：`features/sampling/`**。

根據：

- `ready-pages-implementation-handoff.md` §2.1.6 明確標注：`features/sampling/sampling-execute/`
- `ready-pages-implementation-handoff.md` §2.2.6 明確標注：`features/sampling/batch-list/`
- 兩個 handoff section 均明確表示：「Route: lazy-loaded under `features/sampling/`（與 UI-006 共用同一 feature module）」

### 4.2 Cluster 邊界

| 叢集 | Feature module | 包含頁面 | 路由根 |
|------|---------------|----------|--------|
| Sampling cluster | `features/sampling/` | UI-006、UI-007（UI-008 STUB） | `/sampling/` |
| Cases cluster | `features/cases/` | UI-009～UI-014 | `/cases/` |

**兩者不得合併為單一 feature module。** handoff 文件已在兩個 cluster 之間建立明確邊界；UI-007 `[查看案件]` 是 cross-cluster navigation，不是 intra-module navigation。

### 4.3 Sampling cluster 成員

| Page | ID | Feature sub-module |
|------|----|--------------------|
| 抽選執行頁 | UI-006 | `sampling-execute` |
| 抽選批次列表頁 | UI-007 | `batch-list` |
| 抽選批次詳情頁 | UI-008 | STUB（page spec 不存在；不得實作） |

---

## 5. Route Decisions

### 5.1 Fixed Route Decisions

以下決策有足夠 authority source evidence，可作為前端 skeleton 基礎。

#### UI-006：`/sampling/:projectId`

**根據**：

1. `api-spec.md` §9.1 / §9.2 明確確認 API 路徑為 `/review-projects/{projectId}/sampling/preview` 與 `/review-projects/{projectId}/sampling/execute`，表示 UI-006 必須以 `projectId` 為錨點。
2. `ui-page-spec-ui006.md` §1 標注頁面責任為「對**單一**審查專案執行」，確認 projectId 是頁面必要 route param。
3. Cases cluster route 命名原則（`ui-route-module-map.md`）採用 `feature-root/identifier` 格式：`/cases/:caseId`、`/cases/:caseId/revisions`。Sampling cluster 對等：`/sampling/:projectId`。
4. handoff §2.1.2 提案：「/projects/:projectId/sampling 或 /sampling/:projectId」；本 addendum 選擇 `/sampling/:projectId` 以維持與 cases cluster 的頂層路由一致性（不做 `/projects/...` 嵌套）。

#### UI-007：`/sampling/batches`

**根據**：

1. `api-spec.md` §9.4 `GET /sampling-batches` 無 projectId 必要路徑參數，表示 UI-007 是 cross-project 批次列表，不依賴單一 project。
2. handoff §2.2.2 直接提案：「建議 /sampling/batches」。
3. 與 UI-006 `/sampling/:projectId` 同屬 `/sampling/` 路由根，符合單一 feature module 結構。
4. Cases cluster 類比：`/cases` 是列表；`/sampling/batches` 對等於 cases 的列表入口。

#### `[查看案件]` cross-cluster navigation contract

**根據**：

`/cases?batchId={batchId}`

1. wirefame `ui007-batch-list-first-pass.md` §4 V1 明確：`[查看案件] → navigate to UI-009 with ?batchId={batchId}`
2. `api-spec.md` §10.1 `GET /review-cases` 明確支援 `batchId` 作為 query param。
3. `ui-route-module-map.md` 確認 UI-009 route 為 `/cases`，query params 為 filter 用途（persist: tab / filters / pagination）。
4. `ready-pages-implementation-handoff.md` §2.2.9 test-first order 明確：`[查看案件] → navigate '/cases?batchId={batchId}'`。

### 5.2 Deferred Route Decisions

以下決策在本 addendum 中暫不最終定稿。

#### UI-006 EXECUTE_SUCCESS `[前往案件列表]` 的目標

- 建議目標：`/cases?batchId={batchId}`（batchId 在 execute response 回傳）
- 根據：邏輯一致性（顯示本次抽選產生的案件）；api-spec 支援 `batchId` filter
- **狀態**：Addendum-level provisional decision（尚未在 handoff 中明確固定）

#### UI-007 `[查看詳情]` 的目標（見 §8 STUB 邊界）

- **狀態**：STUB — UI-008 page spec 不存在；route 不得定義

### 5.3 Pending Confirmation

| 項目 | 原因 | 對實作的影響 |
|------|------|------------|
| shell navigation entry key for 「抽選作業」 | shell integration contract 尚未有 repo evidence | 不影響 feature module / route 定義；影響 shell nav link target |
| MFE federation key / remote name for sampling module | 無 MFE contract evidence | 不影響本輪 skeleton；影響正式 MFE 整合時 |
| OQ-S05：REVIEWER/REREVIEWER 是否可通過 `/sampling/batches` 的 route guard | permission-matrix 無 sampling cluster 角色 R 的最終決策 | 現行保守策略：deny |
| OQ-S04：SYSADMIN SamplingBatch metadata whitelist 精確欄位 | permission-matrix 未列出 | 不影響 route 定義；影響 UI-007 column masking |

---

## 6. Proposed Route Map

| Page | Page ID | Feature module | Proposed route | Route params | Query params | Status | Notes |
|------|---------|---------------|----------------|-------------|-------------|--------|-------|
| 抽選執行頁 | UI-006 | `sampling-execute` (under `features/sampling/`) | `/sampling/:projectId` | `projectId` | — | **Fixed** | projectId 錨點；API `/review-projects/{projectId}/sampling/...` 確認；6 mode 在同一 route 內切換，不得拆成多 route |
| 抽選批次列表頁 | UI-007 | `batch-list` (under `features/sampling/`) | `/sampling/batches` | — | `projectId?`, `status?`, `hospitalId?`, `page?`, `size?` | **Fixed** | Cross-project list；query params 與 `GET /sampling-batches` query params 對應；tab state 以 `tab` query param 持久化 |
| 抽選批次詳情頁 | UI-008 | （未建立） | STUB — 不得定義 | — | — | **STUB** | page spec 不存在；`[查看詳情]` action present but no real route target；前端 skeleton 留 action slot，不綁 route |

---

## 7. Cross-Cluster Navigation Rules

| From page | From action | To page | Navigation contract | Status | Notes |
|-----------|-------------|---------|---------------------|--------|-------|
| UI-007 | `[查看案件]` | UI-009 | `router.navigate(['/cases'], { queryParams: { batchId: row.batchId } })` | **Fixed** | wireframe、handoff、api-spec 三方一致；適用 COMPLETED / FAILED rows；SYSADMIN 的 metadata-only 投影不提供 `[查看案件]`（handoff §2.2.5） |
| UI-006 (EXECUTE_SUCCESS) | `[前往抽選批次列表]` | UI-007 | `router.navigate(['/sampling/batches'])` | **Fixed** | wireframe §4 V4 明確；batchId 不傳入（列表頁自行過濾） |
| UI-006 (EXECUTE_SUCCESS) | `[前往案件列表]` | UI-009 | `router.navigate(['/cases'], { queryParams: { batchId: batchId } })` | **Provisional** | 語意上應傳入 execute response 的 batchId；api-spec 支援；尚未在 handoff 中明確固定 |
| UI-007 | `[查看詳情]` | UI-008 | **STUB — 不得實作真實 navigation** | **STUB** | UI-008 page spec 不存在；見 §8 |
| UI-009（接收 batchId context） | back navigation | UI-007 | preserve `batchId` query param for return context | **Deferred** | Cases cluster query state restore 策略尚未擴展至 from-sampling context |

---

## 8. STUB / Placeholder Boundaries

### 8.1 UI-008 在本輪的處理原則

UI-008（抽選批次詳情頁）的 page spec 不存在，明確排除於本批次 ready pages 之外（`ready-pages-implementation-handoff.md` 明確說明）。

前端在建立 skeleton 時的處理規則：

| 項目 | 規則 |
|------|------|
| `[查看詳情]` row action in UI-007 | **必須呈現**（button/link 需要存在，符合 wireframe 和 handoff 要求） |
| `[查看詳情]` action target | **STUB**：不綁任何真實 route |
| STUB 的 Angular 實作方式 | 選項 A：`(click)="onViewDetailStub(batch)"` → `console.warn` or no-op；選項 B：display action slot but disable with `[disabled]="true"` + tooltip「功能開發中」；任一皆可，但不得寫死 `/sampling/batches/:batchId` 或任何推測 route |
| UI-008 route 是否預留 in router | **否**：不得在 `app-routing.module.ts` 或 `SamplingRoutingModule` 中預留任何 `batches/:batchId` route，以免後續 page spec 定稿時產生衝突 |
| action slot 測試 | handoff §2.2.9 test-first order 明確：`[查看詳情] → STUB placeholder（不測試真實 navigation）` |

### 8.2 `SamplingPageHeader` 共用邊界的 STUB 狀態

`SamplingPageHeader` 在 UI-006 / UI-007 均標注為「sampling 叢集級共用，結構佔位」（handoff §2.1.4 / §2.2.4）。

前端建立 skeleton 時：

- 以 placeholder div/component 存在，不強行定義最終 shared component API
- Sampling cluster component map 尚未建立；待後續補充

---

## 9. Implementation Notes for Angular

### 9.1 前端現在可以做的事

| 工作項目 | 依據 | 備注 |
|----------|------|------|
| 建立 `features/sampling/` 資料夾與 `SamplingModule`（或 standalone 根） | handoff §2.1.6 / §2.2.6；本文件 §4 | `SamplingRoutingModule` + lazy-load entry |
| 在 `app-routing.module.ts` 新增 `path: 'sampling'` lazy-load entry | 本文件 §5.1 | `loadChildren: () => import('./features/sampling/sampling.module').then(m => m.SamplingModule)` |
| 在 `SamplingRoutingModule` 定義 child routes：`{ path: ':projectId', component: SamplingExecutePageComponent }` | UI-006 `/sampling/:projectId`（本文件 §5.1） | |
| 在 `SamplingRoutingModule` 定義 child routes：`{ path: 'batches', component: SamplingBatchListPageComponent }` | UI-007 `/sampling/batches`（本文件 §5.1） | 注意 `batches` 是 literal segment，排在 `:projectId` 之前以避免 catch-all 衝突 |
| 建立 `RoleGuard` / `ScopeGuard` placeholder for sampling routes | handoff §2.1.2 / §2.2.2 guard boundary | 先以 passthrough 實作；守衛邏輯後補 |
| UI-007 `[查看案件]` 導向 `/cases?batchId={batchId}` | 本文件 §7 Fixed | `router.navigate(['/cases'], { queryParams: { batchId: batch.batchId } })` |
| UI-006 EXECUTE_SUCCESS `[前往抽選批次列表]` 導向 `/sampling/batches` | 本文件 §7 Fixed | |
| UI-007 `[查看詳情]` 實作為 action slot（STUB，不綁 route） | 本文件 §8 | |

### 9.2 前端不可做的事（本輪）

| 禁止項目 | 原因 |
|----------|------|
| 定義 `/sampling/batches/:batchId` route | UI-008 無 page spec；route 不得提前定義 |
| 把 UI-006 的 6 個 Mode 實作成 6 個獨立 route | handoff §2.1.11 明確禁止 |
| 為 sampling cluster 定義 shell navigation entry point / MFE federation key | 無 repo evidence |
| `[查看詳情]` 綁任何推測 route target | STUB 邊界，見 §8 |
| 在 sampling route 中加入 cross-cluster navigation logic to cases cluster（讓 cases cluster redirect 回 sampling） | cases cluster 的 back navigation 策略不在本輪 addendum 範圍 |

### 9.3 Route 衝突預防注意事項

`SamplingRoutingModule` 的 child route 順序：

```typescript
const routes: Routes = [
  { path: 'batches', component: SamplingBatchListPageComponent },  // ← 必須在 :projectId 之前
  { path: ':projectId', component: SamplingExecutePageComponent },
  { path: '', redirectTo: 'batches', pathMatch: 'full' }           // optional: default to list
];
```

`batches` 是 literal segment，必須排在 wildcard `:projectId` 之前，否則 `/sampling/batches` 會被誤解為 projectId=`batches`。

---

## 10. Final Decision Summary

### 10.1 現在可以正式交給前端的 routing 決策

| 決策 | 內容 |
|------|------|
| Sampling feature module | `features/sampling/`，獨立 lazy-loaded module |
| UI-006 route | `/sampling/:projectId` |
| UI-007 route | `/sampling/batches` |
| Sampling routing 入口 | `app-routing.module.ts` 中 `path: 'sampling'` lazy-load entry |
| Child route 順序 | `batches`（literal）排在 `:projectId`（wildcard）之前 |
| `[查看案件]` cross-cluster navigation | `/cases?batchId={batchId}` |
| `[前往抽選批次列表]` from UI-006 | `/sampling/batches` |
| `[查看詳情]` 在本輪 | STUB action slot，不綁 route |
| UI-008 route | **不得定義**（本輪） |

### 10.2 仍未定稿、不可自行補完的 routing 問題

| 問題 | 原因 | 何時可解決 |
|------|------|----------|
| UI-008 route（抽選批次詳情） | page spec 不存在 | UI-008 正式進入 ready 流程後 |
| shell nav 「抽選作業」的正式 entry point key | shell integration contract 未定稿 | MFE / shell integration 正式規格確認後 |
| MFE federation remote name for sampling module | 無 evidence | MFE 架構確認後 |
| OQ-S05：REVIEWER/REREVIEWER 進 `/sampling/batches` 是否 deny | permission-matrix 無最終決策 | permission-matrix Sampling cluster 補充後 |
| UI-006 EXECUTE_SUCCESS `[前往案件列表]` 是否正式以 `batchId` 作為 query param | 尚未在 handoff 明確固定 | 下一輪 handoff refinement 確認 |
| Cases cluster back-navigation context（from `/cases?batchId=...` 返回 `/sampling/batches`） | 跨 cluster query state restore 策略未定義 | cases cluster 實作規格確認後 |

---

## 附錄：Addendum-level Assumption 清單

以下為本文件在 evidence 不完整情況下，基於現有 authority 所作的 **addendum-level provisional decisions**，而非 confirmed truth。後續若有矛盾 evidence 出現，應優先更新本文件：

| Assumption | 依據 | Confidence |
|-----------|------|-----------|
| Sampling 不嵌套在 `/projects/:projectId/...` 下，而以 `/sampling/` 為獨立根 | Cases cluster pattern（`/cases/` 非 `/review-cases/`）；IA 顯示「抽選作業」為 top-level 模組；handoff 建議「/sampling/:projectId」 | 高 |
| UI-006 EXECUTE_SUCCESS `[前往案件列表]` 傳入 `batchId` | 邏輯一致性；api-spec 支援；但 handoff 未明確固定 | 中（provisional） |
| `cases` cluster 的 `/cases?batchId=` filter 接受 batchId 並限制顯示範圍 | api-spec §10.1 `GET /review-cases` 的 query params 包含 `batchId`；`ui-route-module-map.md` 確認 `/cases` query persist filters | 高 |
