# Frontend-Backend Alignment Baseline

> **文件定位**：本文件是 UI-006 ~ UI-014 的前後端對齊基線。  
> **本輪更新重點**：補記 execute-context 與 UI-007 deferred 現況，並將 B-001 / I-001 / I-002 / I-003 的狀態敘事校正為 limited-scope implemented、deferred、pending 與最小 source replacement。  
> **Evidence 範圍**：僅採 source code、existing tests、現有 route/e2e mock；不把尚未落地的 shell integration 或外部系統契約包裝成已實作。

---

## 1. 狀態定義

| 符號 | 狀態 | 定義 |
|---|---|---|
| ✅ REAL | 真實實作 | FE/BE 已接通正式程式碼路徑 |
| ⚠️ PASSTHROUGH | 通過哨 | 結構存在，但邏輯仍為 `return true` 或未正式授權 |
| ⚠️ STUB-BACKED | Stub 資料來源 | API 已存在，但資料來源仍為 dev-only stub adapter |
| ⏸️ DEFERRED | 正式遞延 | 本輪明確不做，且 repo 已表達為 deferred，不再是假性 stub |
| 🕓 DECISION-REQUIRED | 待決策 | 尚未實作，且正式方向仍待拍板 |

---

## 2. 本輪決策輸入與落地結果

| ID | 決策 | 本輪結果 |
|---|---|---|
| B-001 | 採方案 A：新增 `GET /review-projects/{projectId}/sampling/execute-context` | **limited-scope implemented**。API 與 bootstrap 接線已存在，但尚未宣稱為唯一 truth source |
| F-002 | 採方案 C：暫不做 batch detail | **deferred**。UI-007 detail action 改為明確 disabled + deferred 訊息 |
| I-001 | Route Guard 正式化：guard + directive 雙層策略 | **accepted**（D-029）— guard 待 actor context 注入後正式化；PASSTHROUGH 暫維持 |
| I-002 | hardcoded role 移除：RBAC 為正式來源 | **accepted**（D-030）— legacy grandfathering；新碼禁止 hardcoded role |
| I-003 | assignment candidate source replacement slice | **limited-scope implemented** |

---

## 3. Evidence Inventory 摘要

### 3.1 Frontend Services / Guards

| 類別 | 路徑 | 現況 |
|---|---|---|
| `SamplingService` | `frontends/quality-inspection-frontend/src/app/features/sampling/services/sampling.service.ts` | ✅ REAL — `getBatches()`、`previewSampling()`、`executeSampling()`、`retrySamplingBatch()`、`getExecuteContext()` 均為真實 HTTP |
| `CasesService` | `frontends/quality-inspection-frontend/src/app/features/cases/services/cases.service.ts` | ✅ REAL |
| `RoleGuard` | `frontends/quality-inspection-frontend/src/app/guards/role.guard.ts` | ⚠️ PASSTHROUGH |
| `ScopeGuard` | `frontends/quality-inspection-frontend/src/app/guards/scope.guard.ts` | ⚠️ PASSTHROUGH |
| `ProjectionGuard` | `frontends/quality-inspection-frontend/src/app/guards/projection.guard.ts` | ⚠️ PASSTHROUGH |

### 3.2 Backend Controllers / Sources

| 類別 | 路徑 | 現況 |
|---|---|---|
| `SamplingController` | `services/quality-inspection-service/src/main/java/.../interfaces/rest/SamplingController.java` | ✅ REAL — 含 preview / execute / list / detail / export / retry / **execute-context** |
| `ReviewCaseController` | `services/quality-inspection-service/src/main/java/.../interfaces/rest/ReviewCaseController.java` | ✅ REAL |
| `ApiKeyAuthFilter` + `TzJwtParser` | `services/quality-inspection-service/src/main/java/.../infrastructure/security/` | ✅ REAL — backend 已有 `userId / userName / roles / hospitalId` actor context 來源 |
| `HisStaffDirectoryAssignmentCandidateAdapter` | `services/quality-inspection-service/src/main/java/.../infrastructure/persistence/staffdirectory/HisStaffDirectoryAssignmentCandidateAdapter.java` | ✅ REAL — JPA-backed persisted staff directory source；local profile 以 seed 維持最小可運作資料 |

### 3.3 Validation Evidence

| 類型 | 指令 / 範圍 | 結果 |
|---|---|---|
| Backend targeted tests | `.\gradlew.bat test --tests ...SamplingControllerApiTest --tests ...SamplingUseCaseTest` | ✅ PASS |
| Frontend typecheck/build | `npm run typecheck` | ✅ PASS |
| Frontend unit tests | `npm run test:unit:ci -- --include src/app/features/sampling/**/*.spec.ts` | ✅ PASS |
| Frontend e2e smoke | `npm run e2e -- --grep "marks batch detail as deferred..."` | ✅ PASS |

---

## 4. Alignment Matrix

### 4.1 Sampling Cluster

#### UI-006 — 抽選執行頁（`/sampling/:projectId`）

| 能力項目 | FE 端 | BE 端 | 對齊狀態 | 備註 |
|---|---|---|---|---|
| Bootstrap context | `SamplingService.getExecuteContext(projectId)` | `GET /review-projects/{projectId}/sampling/execute-context` | ✅ REAL | limited-scope implemented；尚未宣稱為唯一 truth source |
| Preview command | `POST /review-projects/{projectId}/sampling/preview` | `PreviewSamplingPopulationUseCase` | ✅ REAL | 舊合約未改語意 |
| Execute command | `POST /review-projects/{projectId}/sampling/execute` | `ExecuteSamplingUseCase` | ✅ REAL | 舊合約未改語意 |
| Page mode resolution | `SamplingExecutePageComponent.resolveMode()` | backend 提供 context flags | ✅ ALIGNED | 仍由 FE 做單點 mode resolution，未散落多 API 推導 |

**UI-006 目前基線**

1. bootstrap 不再使用 fixture。
2. backend context 欄位維持最小集合：`projectId`、`projectName`、`hospitalId`、`ruleSummary`、`previewReady`、`executing`、`executeFailed`、`executedBatchId`、`blockedReason`、`previewStats`、`postExecuteSummary`。
3. 採保守推導：
   - `previewReady = false`：因 preview 結果未做 server-side persistence。
   - `executedBatchId = null`：不把「歷史已完成批次」誤當成「本次 page session 已完成」。
   - `executeFailed = true` 僅在最新批次為 `FAILED` 時回傳，並附最小 failure summary。
4. `ruleSummary` 直接由 backend 以專案版本、病歷類型、評分表、品質門檻組字串，避免 FE 再拆散判斷。
5. preview / execute 後的部分畫面狀態仍由 frontend local merge 維持，故 B-001 目前不寫成 fully closed。

#### UI-007 — 抽選批次列表頁（`/sampling/batches`）

| 能力項目 | FE 端 | BE 端 | 對齊狀態 | 備註 |
|---|---|---|---|---|
| 批次列表 | `SamplingService.getBatches()` | `GET /sampling-batches` | ✅ REAL | 已對齊 |
| Retry command | `retrySamplingBatch()` | `POST /sampling-batches/{batchId}/retry` | ✅ REAL | 已對齊 |
| Batch detail action | disabled `查看詳情（未開放）` + deferred note | `GET /sampling-batches/{batchId}` 存在但未綁 UI | ⏸️ DEFERRED | F-002 已正式遞延 |

**UI-007 新基線**

1. 不新增 `/sampling/batches/:batchId` route。
2. 不做 drawer / modal detail。
3. 不保留 no-op stub；改為 disabled action 與明確文案：**批次詳情已正式 deferred，原因為 UI spec 尚未定。**

---

### 4.2 Cases Cluster

| 頁面 | 現況 | 備註 |
|---|---|---|
| UI-007 抽選批次列表頁 | ✅ REAL（列表 / retry） | 仍有 I-002：batch-list role hardcoded |
| UI-009 案件列表頁 | ✅ REAL（含 `projectionType`） | 仍有 I-002：worklist role hardcoded |
| UI-010 案件詳情頁 | ✅ REAL | `actorRole`、`projectionType`、`isOwner` 均由 backend 提供 |
| UI-011 案件分派頁 | ✅ REAL（limited source replacement） | 命令與 query contract 維持不變；candidate lookup 已改走 persisted staff directory source |
| UI-012 初審作業頁 | ✅ REAL | bootstrap context 已嵌入 case detail |
| UI-013 複審作業頁 | ✅ REAL | CLOSED revision exception 已對齊 |
| UI-014 revision 歷程頁 | ✅ REAL | `hasExceptionEntry` 已由 backend 計算 |

---

## 5. Gap 分類（本輪後）

### 5.1 Limited-Scope Implemented

| ID | 頁面 / 範圍 | 描述 |
|---|---|---|
| B-001 | UI-006 | execute-context bootstrap endpoint 與前端接線已存在；但 previewStats 仍為 page-local session state，execute failure 仍保留 explicit fallback，故不寫成 fully closed |

### 5.2 Closed

| ID | 頁面 / 範圍 | 描述 |
|---|---|---|
| B-002 | UI-006 | preview command 已接真實 API |
| B-003 | UI-006 | execute command 已接真實 API |
| B-004 | UI-007 | batch list 已接真實 API |
| F-001 | UI-007 | retry command 已接真實 API |

### 5.3 Deferred

| ID | 頁面 / 範圍 | 狀態 | Deferred 原因 |
|---|---|---|---|
| F-002 | UI-007 batch detail | ⏸️ DEFERRED | UI spec 尚未定；本輪不做 route / drawer / modal |

### 5.4 Accepted（原 Decision-Required）

| ID | 範圍 | 狀態 | 決策 |
|---|---|---|---|
| I-001 | Route Guard 正式化 | ✅ ACCEPTED | D-029：guard（page-level）+ directive（component-level）；guard 待 actor context 注入後正式化 |
| I-002 | hardcoded role 移除 | ✅ ACCEPTED | D-030：RBAC 為正式來源；legacy grandfathering；新碼禁止 hardcoded role |
| I-003 | Assignment candidate source replacement slice | ✅ LIMITED-SCOPE IMPLEMENTED | 來源改為 persisted staff directory；application layer eligibility 不變 |

---

## 6. Architecture Decision Pack

### 6.1 I-001 — Route Guard 正式化

> **已拍板（D-029）**：guard（page-level）+ directive（component-level）雙層策略。以下保留歷史選項比較，供實作參考。

#### 現況

- `RoleGuard` / `ScopeGuard` / `ProjectionGuard` 全部是 `return true`。
- backend actor context 已存在：`userId`、`userName`、`roles`、`hospitalId`。
- frontend 尚無正式 actor context store / provider。

#### Guard 最小所需欄位

| 欄位 | 用途 | repo 現況 |
|---|---|---|
| `userId` | owner / audit trace | backend 已存在；frontend 未正式注入 |
| `roles` | route-level role guard | backend 已存在；frontend 未正式注入 |
| `hospitalId` | hospital scope guard | backend 已存在；frontend 未正式注入 |
| `projectionType` | metadata-only route gating（若做頁面級限制） | cases detail/list API 已存在；shell bootstrap 不存在 |

#### 方案比較

| 方案 | Context 來源 | FE 接入方式 | 對 guard / component 影響 | Standalone 影響 | 測試影響 | 成本 | 風險 |
|---|---|---|---|---|---|---|---|
| A | Shell 直接注入 actor context | shell adapter -> shared actor context service | guard 可直接正式化；component 少改 | 需 local mock provider | guard unit test + shell contract test | 中 | 依賴正式 shell contract |
| B | frontend 啟動時呼叫 `/me` 或 equivalent bootstrap API | app initializer / actor context store | guard 依 store 判斷；component 可共用 | standalone 可用 mock `/me` | 需補 bootstrap API mock | 中高 | repo 內目前沒有 `/me` 契約 |
| C | 延後 shell，先以 route resolver + page API 拼裝 | 每頁各自 resolver | guard 與 component 邏輯分裂 | standalone 容易做 | 測試分散 | 低 | 會把 auth truth 散落在 page routes，不推薦 |

#### 已拍板決策（D-029）

- **page-level gate**：Route guard（`RoleGuard` / `ScopeGuard` / `ProjectionGuard`）在 Angular route resolution 階段執行
- **component-level gate**：Directive 控制頁面內 UI 元素可見性
- 兩者共用同一 actor context 來源（shell 注入或 actor context store）
- Guard 目前仍為 PASSTHROUGH，待 actor context provider 完成注入後補正式化

#### 實作前提（尚待完成）

1. actor context provider 完成注入（shell 注入欄位：`userId / userName / roles / hospitalId`）
2. standalone mode 需對應 mock actor provider（shape 與正式相同）
3. `projectionType` 走 page API，不進 shell actor contract

---

### 6.2 I-002 — hardcoded role 移除

> **已拍板（D-030）**：RBAC 為正式來源；逐步移除；新碼禁止 hardcoded role；legacy grandfathering。以下保留現況描述與選項比較。

#### 現況

- 已確認 hardcoded role 現有範圍至少包含：`cases-worklist/case-list-page.component.ts`、`sampling/batch-list/sampling-batch-list-page.component.ts`。
- 該 hardcoded role 影響：
  - default tab
  - default status
  - mode label
  - 某些列表操作可見性
- `CaseDetailPageComponent` / `RevisionHistoryPageComponent` 已消費 backend `actorRole`，但仍容忍 legacy alias（如 `MANAGER` / `ADMIN`）。

#### 方案比較

| 方案 | 真實 role 來源 | 多角色處理 | Standalone 退化 | 成本 | 風險 |
|---|---|---|---|---|---|
| A | shell actor context.roles | FE 定一個 primary-role resolver + priority | standalone mock 同 shape roles | 中 | 需拍板 role priority |
| B | page API 回傳 `actorRole`，列表頁也補同欄位 | backend 決 primary role | standalone 需 mock page API | 中高 | 讓列表頁過度依賴單一 page API 回傳 |
| C | FE 自己從 token / local storage 解 role | FE 自算 | standalone 最容易 | 低 | 破壞 shell boundary，不推薦 |

#### 已拍板決策（D-030）

- RBAC（actor context `roles`）為正式 access policy 來源
- 新碼不得新增 hardcoded role 字串比對
- Legacy（`case-list-page`、`batch-list-page`）grandfathered，被修改時須同步改接 RBAC-backed 機制
- `MANAGER` / `ADMIN` legacy alias 在下次碰到該 component 時應清除

#### 建議 primary-role 規則（供 RBAC resolver 實作參考）

| 優先順序 | 角色 |
|---|---|
| 1 | `SYSTEM_ADMIN` |
| 2 | `HOSPITAL_MANAGER` |
| 3 | `AUDITOR` |
| 4 | `REREVIEWER` |
| 5 | `REVIEWER` |

> 此 priority 建議值配合 D-030 RBAC 策略使用；primary-role resolver 的具體實作在 guard / directive 落地時依此決定。

---

### 6.3 I-003 — Assignment Candidate Source Replacement Slice

#### 現況

- `HisStaffDirectoryAssignmentCandidateAdapter` 目前契約輸入仍為：`hospitalId`、`role`、`keyword`。
- 目前來源輸出欄位仍為：`userId`、`displayName`、`displayLabel`、`role`、`active`。
- application layer 持續補：
  - `selectable`
  - `blockedReason`
- 關鍵結論：**資格過濾仍屬 application layer，不下沉到 source。**

#### 本輪落地

- source adapter 改為單一 persisted staff directory source，責任限於 directory lookup 與欄位映射。
- local profile 以 seed 資料維持最小可運作來源，避免回退成 hardcoded stub component。
- `AssignmentCandidateDirectoryPort`、`AssignmentCandidateDirectoryEntry`、`AssignmentCandidateView`、`assignment-context`、`assignment-candidates` contract 皆未改 shape。
- 目前 repo 內仍無可驗證的外部 HIS / doctor master 接線 evidence，因此本輪不宣稱最終 upstream architecture 已完成。

#### 後續仍待演進範圍

1. persisted staff directory 如何與真正的 HIS staff master 或 doctor master 同步，repo 內仍無現成 evidence。
2. 若未來需要跨院 / 跨組織候選或多來源聚合，應另開 slice，不在本輪處理。
3. 若未來來源能提供 specialty / qualification 等欄位，可在不破壞 seam 的前提下擴充 entry 映射，但 business eligibility 仍應留在 application layer。

---

## 7. Re-scan 結論

| 項目 | 本輪結論 |
|---|---|
| B-001 是否已 fully closed | **否**；目前為 limited-scope implemented |
| F-002 是否已正式轉為 deferred | **是** |
| UI-006 狀態是否改變 | **是**，由 fixture bootstrap 轉為 execute-context bootstrap |
| UI-007 detail gap 是否仍是含糊 stub | **否**，已改為明確 deferred |
| I-001 / I-002 是否已 fully resolved | **I-001、I-002 已拍板**（D-029、D-030）；guard 正式化仍待 actor context 注入；legacy hardcoded role 清除仍在 grandfathering |
| I-003 是否已 fully resolved | **limited-scope implemented**；persisted staff directory 已接入；外部 HIS 同步仍待演進 |
| 是否仍有文件同步缺口 | **否**；本輪允許範圍內的 B-001 direct docs 已同步 |

---

## 8. Current Status Statement

本輪對齊盤點的保守結論如下：

1. **B-001 為 limited-scope implemented**：UI-006 已改接 `GET /review-projects/{projectId}/sampling/execute-context` 作為頁面 bootstrap API；但 `previewStats` 仍由 page-local session state 維持，execute failure 仍保留 explicit fallback，因此尚未收口成單一 truth source。
2. **F-002 為 deferred**：repo 已明確表達目前不開放 batch detail page / route / drawer / modal，前端 detail action 維持 disabled / 未開放。
3. **I-001 / I-002 已拍板（D-029 / D-030）**：策略方向已定稿。I-001 guard 正式化仍待 actor context provider 完成注入；I-002 legacy hardcoded role 處於 grandfathering，被修改時強制改接 RBAC。I-003 為 limited-scope implemented，外部 HIS 同步仍待演進。
