# Frontend-Backend Alignment Baseline

> **文件定位**：本文件是 UI-006 ~ UI-014 的前後端對齊基線。  
> **本輪更新重點**：關閉 B-001、將 F-002 正式轉為 deferred，並把 I-001 / I-002 / I-003 收斂成可拍板的 architecture options pack。  
> **Evidence 範圍**：僅採 source code、existing tests、現有 route/e2e mock；不把尚未落地的 shell integration 或外部系統契約包裝成已實作。

---

## 1. 狀態定義

| 符號 | 狀態 | 定義 |
|---|---|---|
| ✅ REAL | 真實實作 | FE/BE 已接通正式程式碼路徑 |
| ⚠️ PASSTHROUGH | 通過哨 | 結構存在，但邏輯仍為 `return true` 或未正式授權 |
| ⚠️ STUB-BACKED | Stub 資料來源 | API 已存在，但資料來源仍為 dev-only stub adapter |
| ⏸️ DEFERRED | 正式遞延 | 本輪明確不做，且 repo 已表達為 deferred，不再是假性 stub |
| 📦 DECISION PACK READY | 可決策 | 尚未實作，但已整理成可拍板的小問題集 |

---

## 2. 本輪決策輸入與落地結果

| ID | 決策 | 本輪結果 |
|---|---|---|
| B-001 | 採方案 A：新增 `GET /review-projects/{projectId}/sampling/execute-context` | **已落地**。UI-006 改為以單一 backend context 作 bootstrap |
| F-002 | 採方案 C：暫不做 batch detail | **已落地**。UI-007 detail action 改為明確 disabled + deferred 訊息 |
| I-001 | Route Guard 正式化不可直接實作 | **已收斂**為 decision pack |
| I-002 | hardcoded role 不可直接實作 | **已收斂**為 decision pack |
| I-003 | assignment candidate 正式資料源不可直接替換 | **已收斂**為 decision pack |

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
| `StubAssignmentCandidateLookupAdapter` | `services/quality-inspection-service/src/main/java/.../infrastructure/StubAssignmentCandidateLookupAdapter.java` | ⚠️ STUB-BACKED |

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
| Bootstrap context | `SamplingService.getExecuteContext(projectId)` | `GET /review-projects/{projectId}/sampling/execute-context` | ✅ REAL | B-001 已關閉 |
| Preview command | `POST /review-projects/{projectId}/sampling/preview` | `PreviewSamplingPopulationUseCase` | ✅ REAL | 舊合約未改語意 |
| Execute command | `POST /review-projects/{projectId}/sampling/execute` | `ExecuteSamplingUseCase` | ✅ REAL | 舊合約未改語意 |
| Page mode resolution | `SamplingExecutePageComponent.resolveMode()` | backend 提供 context flags | ✅ ALIGNED | 仍由 FE 做單點 mode resolution，未散落多 API 推導 |

**UI-006 新基線**

1. bootstrap 不再使用 fixture。
2. backend context 欄位維持最小集合：`projectId`、`projectName`、`hospitalId`、`ruleSummary`、`previewReady`、`executing`、`executeFailed`、`executedBatchId`、`blockedReason`、`previewStats`、`postExecuteSummary`。
3. 採保守推導：
   - `previewReady = false`：因 preview 結果未做 server-side persistence。
   - `executedBatchId = null`：不把「歷史已完成批次」誤當成「本次 page session 已完成」。
   - `executeFailed = true` 僅在最新批次為 `FAILED` 時回傳，並附最小 failure summary。
4. `ruleSummary` 直接由 backend 以專案版本、病歷類型、評分表、品質門檻組字串，避免 FE 再拆散判斷。

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
| UI-009 案件列表頁 | ✅ REAL（含 `projectionType`） | 仍有 I-002：worklist role hardcoded |
| UI-010 案件詳情頁 | ✅ REAL | `actorRole`、`projectionType`、`isOwner` 均由 backend 提供 |
| UI-011 案件分派頁 | ⚠️ STUB-BACKED | 命令已真實；candidate source 仍為 stub adapter |
| UI-012 初審作業頁 | ✅ REAL | bootstrap context 已嵌入 case detail |
| UI-013 複審作業頁 | ✅ REAL | CLOSED revision exception 已對齊 |
| UI-014 revision 歷程頁 | ✅ REAL | `hasExceptionEntry` 已由 backend 計算 |

---

## 5. Gap 分類（本輪後）

### 5.1 Closed

| ID | 頁面 / 範圍 | 描述 |
|---|---|---|
| B-001 | UI-006 | execute bootstrap context 改為 backend 專屬端點 |
| B-002 | UI-006 | preview command 已接真實 API |
| B-003 | UI-006 | execute command 已接真實 API |
| B-004 | UI-007 | batch list 已接真實 API |
| F-001 | UI-007 | retry command 已接真實 API |

### 5.2 Deferred

| ID | 頁面 / 範圍 | 狀態 | Deferred 原因 |
|---|---|---|---|
| F-002 | UI-007 batch detail | ⏸️ DEFERRED | UI spec 尚未定；本輪不做 route / drawer / modal |

### 5.3 Decision Pack Ready

| ID | 範圍 | 狀態 |
|---|---|---|
| I-001 | Route Guard 正式化 | 📦 DECISION PACK READY |
| I-002 | hardcoded role 移除 | 📦 DECISION PACK READY |
| I-003 | Assignment candidate 正式資料來源 | 📦 DECISION PACK READY |

---

## 6. Architecture Decision Pack

### 6.1 I-001 — Route Guard 正式化

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

#### 推薦方案

**推薦 A：Shell 直接注入 actor context，frontend 維持單一 actor context service。**

**理由**

1. 最接近 repo 既有 integration baseline：正式 login / user context 由 shell 提供。
2. 可重用 backend 已成熟的 actor 欄位語意，不必再發明第二套 bootstrap API。
3. guard / component 都能吃同一份 truth，避免 page-level 拼裝。

#### 待使用者拍板的最小問題集

1. shell 注入欄位是否固定為 `userId / userName / roles / hospitalId`？
2. frontend 是否允許在 standalone mode 用 mock actor provider 模擬同 shape contract？
3. `projectionType` 是否只走 page API，不進 shell actor contract？

---

### 6.2 I-002 — hardcoded role 移除

#### 現況

- 已確認 hardcoded role 主來源：`cases-worklist/case-list-page.component.ts`。
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

#### 推薦方案

**推薦 A：以 shell 提供 `roles[]`，frontend 明確定義 primary-role resolver。**

#### 建議 primary-role 規則

| 優先順序 | 角色 |
|---|---|
| 1 | `SYSTEM_ADMIN` |
| 2 | `HOSPITAL_MANAGER` |
| 3 | `AUDITOR` |
| 4 | `REREVIEWER` |
| 5 | `REVIEWER` |

> 這是**建議值**，不是本輪已拍板規則。

#### 待使用者拍板的最小問題集

1. 多角色使用者是否允許存在？若允許，是否接受上述 priority？
2. worklist / batch list 是否以 primary role 決定 default tab，或允許 UI 手動切 role 視角？
3. legacy alias（`MANAGER` / `ADMIN`）是暫時相容還是可直接移除？

---

### 6.3 I-003 — Assignment Candidate 正式資料來源

#### 現況

- `StubAssignmentCandidateLookupAdapter` 目前契約輸入：`hospitalId`、`role`、`keyword`。
- 目前來源輸出欄位：`userId`、`displayName`、`displayLabel`、`role`、`active`。
- application layer 再補：
  - `selectable`
  - `blockedReason`
- 關鍵結論：**資格過濾目前屬 application layer，不應直接下沉到外部來源。**

#### 方案比較

| 方案 | 可能來源 | 可提供欄位 | 搜尋能力 | reviewer / rereviewer 資格過濾落點 | 成本 | 風險 |
|---|---|---|---|---|---|---|
| A | HIS staff master | staffId、name、hospitalId、職類/科別、在職狀態 | 醫院內搜尋通常足夠 | application layer 保持現況；source 只回 directory entry | 中 | 可能缺少跨院/組織角色資訊 |
| B | HR org directory | employeeId、name、org unit、title、active | 組織樹 / keyword 通常較完整 | application layer；必要時另補 reviewer eligibility mapping | 中高 | 醫療審查角色未必直接存在 |
| C | IAM / AD / LDAP | account、displayName、group、active | account / group 搜尋 | application layer；group -> role mapping 另做 adapter | 中高 | 群組不一定能表達審查業務資格 |

#### 推薦方向

**推薦先以 A 或 B 類 directory source 取代 stub，但維持 `AssignmentCandidateDirectoryPort` 不變。**

**理由**

1. 現有 use case 已把 business eligibility 放在 application layer，替換 source 不必重寫 UI/command contract。
2. `AssignmentCandidateDirectoryPort` 已是穩定 seam，能讓 production source 漸進替換。
3. 直接用 IAM/LDAP 當唯一真相風險較高，因其 group 通常不等於審查業務資格。

#### 待使用者拍板的最小問題集

1. 正式 source 優先採 HIS staff master 還是 HR org directory？
2. reviewer / rereviewer 資格是否已存在於來源欄位，或需由本系統另做 mapping？
3. 搜尋範圍是否限同院，或允許跨組織候選？

---

## 7. Re-scan 結論

| 項目 | 本輪結論 |
|---|---|
| B-001 是否已關閉 | **是** |
| F-002 是否已正式轉為 deferred | **是** |
| UI-006 狀態是否改變 | **是**，由 PARTIAL / FIXTURE 轉為 REAL bootstrap |
| UI-007 detail gap 是否仍是含糊 stub | **否**，已改為明確 deferred |
| I-001 / I-002 / I-003 是否已從模糊 pending 轉為可決策方案包 | **是** |
| 是否發現新 gap | **無新增功能 gap**；僅確認 e2e 必須補 sampling mocks，已同步處理 |

---

## 8. Formal Closure Statement

本輪 gap closure cycle 的正式結論如下：

1. **B-001 已關閉**：UI-006 抽選執行頁現在以專屬 backend 端點 `GET /review-projects/{projectId}/sampling/execute-context` 作為唯一 bootstrap context 來源，不再依賴 frontend fixture 或多 API 自組。
2. **F-002 已正式遞延**：repo 已明確表達「batch detail 暫不做」，不再保留含糊 no-op stub；deferred 原因為 **UI spec 尚未定**。
3. **I-001 / I-002 / I-003 仍未實作，但已完成決策空間收斂**：三項均已整理成可供使用者快速拍板的 architecture options pack，後續可直接進入決策而非再做模糊盤點。
