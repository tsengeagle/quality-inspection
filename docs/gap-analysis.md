# 實作 Gap Analysis v1

## 1. 文件目的

本文件根據設計文件（`docs/01-requirements/`、`docs/02-ddd/`、`docs/03-contracts/`）逐層審查現有實作，
列出偏離設計的具體 gap，供後續實作補齊時使用。

**審查基準日期**：2026-04-07  
**審查範圍**：B1 + B2 已實作範圍（Project Configuration、Sampling、ScoreSheet），以及整體架構層

---

## 2. 總結（嚴重程度分級）

| 嚴重程度 | 說明 | 數量 |
|---|---|---|
| 🔴 CRITICAL | 已違反 decision log 的明確決策，或影響資料正確性 | 3 |
| 🟡 MAJOR | 設計明確要求但完全缺漏的功能或欄位 | 12 |
| 🟢 MINOR | 實作細節偏差，不影響核心邏輯 | 8 |

---

## 3. CRITICAL Gaps

### GAP-001：D-013 violation — Retry 使用新 snapshot 而非原始 snapshot 🔴

**文件依據**：`decision-log.md` D-013 — "retry 沿用原 batch snapshot，不採用重新抓取的 published version"

**問題描述**：  
`RetrySamplingBatchUseCase.execute()` 原本在 retry 時，以當下的 project 版本資訊重新建構 `RuleSnapshot`：
```java
// 錯誤的舊版實作
RuleSnapshot snapshot = new RuleSnapshot(
        "project=" + projectId + ",version=" + project.getCurrentVersion().getVersionNumber(),
        "default-sampling-policy"
);
```
此行為違反 D-013。retry 應沿用 `previousBatch.getRuleSnapshot()`，讓 retry 的 snapshot 與原失敗批次完全一致，保持追溯性。

**修正狀態**：✅ **已修正**  
已修改為：
```java
// D-013: retry reuses the original batch's rule snapshot, not a newly built one.
RuleSnapshot snapshot = previousBatch.getRuleSnapshot();
```

---

### GAP-002：ScoreSheetDefinition 缺少跨 section 的 itemId / code 唯一性檢查 🔴

**文件依據**：`domain-model.md` — "No duplicate itemId within the same version"、"No duplicate code within the same version"

**問題描述**：  
設計要求同一版本內 `itemId` 與 `code` 必須唯一，但原始 `ScoreSheetDefinition.addSection()` 只檢查 `sectionId` 是否重複，並未檢查新加入 section 的 items 是否與現有 sections 的 items 衝突。  
`ScoreSheetSection.addItem()` 雖有 section 內部的唯一性檢查，但無法攔截跨 section 的重複。

**修正狀態**：✅ **已修正**  
`ScoreSheetDefinition.addSection()` 現在會在 section 加入時，逐一比對新 section 的所有 `item.itemId` 與 `item.code` 是否已存在於其他 sections。

---

### GAP-003：API Base Path 與 api-spec.md 不符 🔴

**文件依據**：`api-spec.md` §3.1 — Base Path = `/api/quality-review`

**問題描述**：  
`application.yml` 設定的 server context path 為 `/api/quality-inspection`，而 api-spec.md 定義的 base path 為 `/api/quality-review`。  
實際路徑（如 `/api/quality-inspection/review-projects`）與設計契約（`/api/quality-review/review-projects`）不符，
會影響前後端整合及前後端契約一致性。

**修正狀態**：⚠️ 尚未修正，需確認是否有意識的本地化差異或文件勘誤

---

## 4. MAJOR Gaps

### GAP-004：ReviewProject 缺少大量設計欄位（policy、effective period、生命週期操作）🟡

**文件依據**：`domain-model.md` §ReviewProject、`persistence-model.md` §5.1-5.3、`application-service.md`

**缺漏清單**：

| 項目 | 設計要求 | 現況 |
|---|---|---|
| `policies` | 內含 sampling policy、quality threshold、effective period | 未實作 |
| `effective_start_at` / `effective_end_at` | 有效期控制，批次只能在有效期內執行 | 未實作 |
| `quality_threshold` | 決定是否需要複審的閾值 | 未實作 |
| `ActivateReviewProjectUseCase` | 激活 project | 未實作 |
| `DeactivateReviewProjectUseCase` | 停用 project | 未實作（domain 已有 `deactivate()` 方法） |
| `UpdateReviewProjectDraftUseCase` | 修改草稿版本 | 未實作 |
| `BindScoreSheetToProjectUseCase` | 將評分表版本綁定至 project | 未實作 |
| `project-version` 獨立資料表 | 設計要求 project 與版本分離（version history） | 合併在同一 entity |

---

### GAP-005：ScoreSheetDefinition 缺少版本歷程分離及多版本管理 🟡

**文件依據**：`persistence-model.md` §5.4-5.7、`domain-model.md` §ScoreSheetDefinition

**缺漏清單**：

| 項目 | 設計要求 | 現況 |
|---|---|---|
| `score_sheet` + `score_sheet_version` 分離 | 主表 + 版本表分離，保留版本歷程 | 單一 entity 無版本歷程 |
| `UpdateScoreSheetDraftUseCase` | 修改評分表草稿 | 未實作 |
| `checksum` 欄位 | 版本完整性驗證 | 未實作 |
| 版本號唯一約束 | `(score_sheet_id, version_no)` unique | 未實作 |

---

### GAP-006：ReviewCase 缺少所有後續生命週期欄位與行為 🟡

**文件依據**：`domain-model.md` §ReviewCase、`state-machine.md`、`persistence-model.md` §5.12

**缺漏清單**：

| 項目 | 設計要求 | 現況 |
|---|---|---|
| `reviewerId` / `rereviewerId` | 初審 / 複審指派人 | 未實作 |
| `due_at` | 審查截止日 | 未實作 |
| `rereview_required` | 是否需要複審 | 未實作 |
| `final_score` / `final_conclusion` | 最終分數 / 結論 | 未實作 |
| `ruleSnapshotRef` / `scoreSheetVersionRef` | D-011 version + snapshot 追溯 | 未實作 |
| `closed_at` / `cancelled_at` | terminal state 時間戳 | 未實作 |
| 狀態轉移方法 | assign、startReview、submitReview、close、cancel 等 | 未實作（只有 createFromSampling） |
| RETURNED 狀態 | state machine 定義此狀態 | `CaseStatus.RETURNED` 存在但無對應轉移邏輯 |

---

### GAP-007：ReviewSubmission Aggregate 完全未實作 🟡

**文件依據**：`domain-model.md` §ReviewSubmission、`state-machine.md` §submission 子狀態機、`decision-log.md` D-001～D-006

**缺漏清單**：

- `ReviewSubmission` domain class（含 DRAFT / SUBMITTED / SUPERSEDED 子狀態機）
- submission 的 revision append-only 邏輯
- 所有 B3 use cases（起審、儲存草稿、提交、複審、revision 等）
- submission persistence（`review_submission`、`review_submission_answer`、`review_submission_snapshot`、`review_submission_attachment` 表）

> 注：B3 stage 尚未排入實作計畫，但此為設計的核心 gap。

---

### GAP-008：Authorization / Permission 完全未實作 🟡

**文件依據**：`permission-matrix.md`、`api-spec.md` §5

**缺漏清單**：

- 無 Spring Security / 任何授權框架配置
- Controller 無任何 role guard（任何人都可呼叫所有 API）
- 無 `api-key` header 驗證（gateway-layer 替代方案亦未設置）
- 資料範圍過濾（hospital scope、self-assigned scope）未實作
- 角色 SYSTEM_ADMIN / HOSPITAL_MANAGER / REVIEWER / REREVIEWER / AUDITOR 無任何落地

---

### GAP-009：persistence model 與設計的 schema 有大量欄位缺漏 🟡

**文件依據**：`persistence-model.md`

以下 table/欄位設計已明確，但 entity 尚未體現：

| Entity | 缺漏欄位 |
|---|---|
| `ReviewProjectEntity` | `effective_start_at`, `effective_end_at`, `created_by`, `created_at`, `updated_by`, `updated_at`, `current_version_id` |
| `SamplingBatchEntity` | `project_version_id`（FK 到 review_project_version）|
| `ReviewCaseEntity` | `project_version_id`, `department_id`, `reviewer_id`, `rereviewer_id`, `due_at`, `rereview_required`, `final_score`, `final_conclusion`, `rule_snapshot_checksum`, `score_sheet_version_id`, `closed_at`, `cancelled_at` |

缺漏的 table（尚無對應 entity）：
- `review_project_version`（project 版本資料表）
- `review_project_version_policy`（版本 policy JSON）
- `score_sheet_version`（評分表版本資料表）
- `sampling_batch_snapshot`（batch 的 immutable rule snapshot JSON）
- `sampling_batch_message_log`（批次執行訊息）
- `review_case_snapshot_ref`（案件層級 snapshot 追溯）
- `hospital`、`department`、`system_user`、`user_role_assignment`（master data）
- `audit_trail`、`outbox_event`（audit/event 追溯）

---

### GAP-010：Query API / GET endpoints 完全缺漏 🟡

**文件依據**：`api-spec.md` §7-§14

現有 controller 只有 write command。以下 GET endpoints 均缺漏：

- `GET /review-projects`（列表）
- `GET /review-projects/{projectId}`（詳情）
- `GET /score-sheets/{scoreSheetId}`（詳情）
- `GET /sampling-batches`（列表）
- `GET /sampling-batches/{batchId}`（詳情）
- `GET /review-cases`（列表，支援角色 scope filter）
- `GET /review-cases/{caseId}`（詳情，支援 scope-aware view）
- `GET /review-cases/{caseId}/revisions`（revision history）
- `GET /review-cases/{caseId}/lifecycle`（lifecycle history）
- `GET /audit-trails`（稽核查詢）
- `GET /reports/dashboard-summary`（報表）

---

### GAP-011：Domain Events 未實作 🟡

**文件依據**：`domain-events.md`、`application-service.md` §Transaction Boundaries

已定義但尚未發布的 domain events（抽例）：
- `ReviewProjectCreated`
- `ReviewProjectVersionPublished`
- `SamplingBatchStarted` / `SamplingBatchCompleted` / `SamplingBatchFailed`
- `ReviewCaseCreated`
- `ReviewCaseAssigned`
- `InitialReviewSubmitted`
- `RereviewSubmitted`
- `ReviewCaseClosed`
- `ReviewSubmissionRevised`

並無 Outbox / EventPublisher 機制落地。

---

### GAP-012：GlobalExceptionHandler 的業務錯誤碼映射不完整 🟡

**文件依據**：`api-spec.md` §4.2

現況：只有 `IllegalArgumentException → BAD_REQUEST` 和 `IllegalStateException → CONFLICT`。

未映射的業務錯誤碼（應回傳對應的 HTTP status 與 code 字串）：

| 業務錯誤碼 | HTTP Status | 觸發情境 |
|---|---|---|
| `PROJECT_NOT_ACTIVE` | 422 | 抽選時 project 未啟用 |
| `PROJECT_VERSION_NOT_PUBLISHABLE` | 422 | 版本不可發布 |
| `SCORE_SHEET_NOT_PUBLISHABLE` | 422 | 評分表不可發布 |
| `BATCH_NOT_RETRYABLE` | 422 | 非 FAILED 批次 retry |
| `DUPLICATE_SELECTED_CASE_IN_BATCH` | 409 | 同批次重複抽中 |
| `CASE_NOT_ASSIGNABLE` | 422 | 案件不可指派 |
| `CASE_ALREADY_CLOSED` | 409 | 案件已結案 |
| `CASE_ALREADY_CANCELLED` | 409 | 案件已取消 |
| `REVISION_NOT_ALLOWED` | 422 | 狀態不允許 revision |
| `REVISION_ACTOR_MISMATCH` | 403 | revision 發起者不是合法人 |
| `SUBMISSION_NOT_OWNED_BY_ACTOR` | 403 | submission 不屬於 actor |
| `INVALID_STATE_TRANSITION` | 409 | 非法狀態轉移 |
| `UNAUTHORIZED` | 401 | 未登入 |
| `FORBIDDEN` | 403 | 無權限 |
| `NOT_FOUND` | 404 | 資源不存在 |
| `INTERNAL_ERROR` | 500 | 未預期錯誤 |

---

### GAP-013：SamplingBatch.fail() 未保存 reason 訊息 🟡

**文件依據**：`persistence-model.md` §5.10 (`sampling_batch_message_log`)

現況：`SamplingBatch.fail(String reason)` 接受 reason 參數但直接丟棄，不儲存。  
設計要求批次執行訊息應保存到 `sampling_batch_message_log` 以供後續查詢。

---

### GAP-014：Frontend 完全未實作業務功能 🟡

**文件依據**：`docs/05-ui/` 各頁 page spec

現況：Frontend 只有 shell / core / shared 骨架，沒有任何業務 feature module、page、service、NgRx store。

缺漏的頁面：

| Page ID | 頁面名稱 |
|---|---|
| UI006 | 抽選執行頁 |
| UI007 | 抽選批次列表頁 |
| UI009 | 案件列表頁 |
| UI010 | 案件詳情頁 |
| UI011 | 案件分派頁 |
| UI012 | 初審作業頁 |
| UI013 | 複審作業頁 |
| UI014 | Revision 歷程頁 |

---

### GAP-015：RuleSnapshot 未以 JSON blob 方式持久化 🟡

**文件依據**：`persistence-model.md` §5.9 (`sampling_batch_snapshot`)

設計要求 `SamplingBatch` 的 rule snapshot 應儲存為獨立的 `sampling_batch_snapshot` 表（含 `snapshot_json`、`checksum`、`schema_version`）。

現況：`RuleSnapshot` 只有兩個字串欄位（`projectVersionInfo`、`samplingPolicyInfo`），
並直接以兩欄存入 `sampling_batches` 主表（`rule_snapshot_project`、`rule_snapshot_policy`），
無法保存完整的 snapshot 結構，且缺少 checksum 及 schema 版本管理。

---

## 5. MINOR Gaps

### GAP-016：Table 名稱與 persistence-model.md 定義不符 🟢

| Entity 使用名稱 | persistence-model.md 定義名稱 |
|---|---|
| `review_projects` | `review_project` |
| `review_cases` | `review_case` |
| `sampling_batches` | `sampling_batch` |
| `score_sheet_definitions` | `score_sheet` / `score_sheet_version` |

---

### GAP-017：ReviewProjectEntity 的版本資訊未分離 🟢

`ReviewProjectEntity` 將 `version_number` 和 `version_published` 直接存在 project 主表，
而設計的 `review_project_version` 要求獨立資料表，以保存版本歷程（每次 publish 均留記錄）。

---

### GAP-018：ApiResponse 缺少 `meta` 欄位填充 🟢

`ApiResponse` 有 `meta` 欄位的型別定義，但現有所有回應均未填充 `meta`（`meta` 恆為 null）。  
`api-spec.md` 定義 meta 欄位可承載分頁資訊等 metadata。

---

### GAP-019：ScoreSheetDefinition.addSection() 未驗證 section 內 items 的 maxScore 非負 🟢

`ScoreItem` 建構子已有 `maxScore < 0` 的檢查，但此檢查只在物件建構時觸發。  
ScoreSheetDefinition / ScoreSheetSection 本身沒有額外重驗，若有人繞過建構子則無法攔截。

> 注：在 Java 封閉環境內此風險較低，但對公開 API 而言仍建議防禦性驗證。

---

### GAP-020：ReviewProject.activate() 只能在有 published version 後才能呼叫，但無 API 端點 🟢

`ReviewProject.activate()` 方法已存在 domain 層，但 `ReviewProjectController` 未提供 `/activate` 端點。  
因此目前無法透過 API 將 project 狀態從 DRAFT / DEACTIVATED 改為 ACTIVE，
實際上讓所有抽選操作均無法進行（因為 `ExecuteSamplingUseCase` 要求 project.status == ACTIVE）。

---

### GAP-021：ReviewProject.deactivate() 有 domain 方法但缺 API 端點 🟢

同 GAP-020，`deactivate()` domain 方法存在但 controller 缺少對應端點 `/deactivate`。

---

### GAP-022：ReviewProject.prepareDraftVersion() 無對應 API 🟢

`ReviewProject.prepareDraftVersion()` 為允許下一次版本修改的方法（publish 之後準備新草稿），
但 controller 未暴露此操作。

---

### GAP-023：SelectedCaseRef.hospitalId 來源依賴 stub 但未型別保護 🟢

**文件依據**：`persistence-model.md` §5.11 — `hospital_id` 應 FK 至 `hospital`

現況：`StubCandidateSourceAdapter` 回傳的 `SelectedCaseRef.hospitalId` 是任意字串，  
無法驗證是否為合法的 hospital_id。正式 adapter 應接入 hospital master data 驗證。

---

## 6. 實作建議優先序

以下為建議補齊的優先序，供後續 sprint 規劃參考：

### 立即處理（已修正或本次 PR）
- ✅ GAP-001：D-013 violation（已修正）
- ✅ GAP-002：ScoreSheetDefinition 跨 section 唯一性（已修正）
- ⚠️ GAP-003：API base path 確認

### 短期補齊（B2 完整化）
- GAP-020 / GAP-021 / GAP-022：ReviewProject activate / deactivate / prepareDraft API endpoint
- GAP-012：GlobalExceptionHandler 業務錯誤碼映射
- GAP-018：ApiResponse meta 填充
- GAP-013：SamplingBatch fail reason 持久化

### 中期補齊（B3 啟動前必要基礎）
- GAP-008：Authorization / Permission Guard
- GAP-004：ReviewProject policies + effective period
- GAP-006：ReviewCase 完整欄位與狀態機
- GAP-009：Persistence schema 對齊（關鍵欄位補齊）
- GAP-010：Query API / GET endpoints

### 長期補齊（B3/B4 stage）
- GAP-007：ReviewSubmission aggregate + 所有 B3 use cases
- GAP-011：Domain Events + Outbox
- GAP-005：ScoreSheet 版本歷程分離
- GAP-014：Frontend 業務功能
- GAP-015：RuleSnapshot JSON 持久化

---

## 7. 不在 Gap 範圍的事項

以下設計上已明確標示為「後續演進」或「B3/B4 stage」的事項，不計入 gap，僅備查：

- B3 Bounded Context（Assignment and Initial Review）
- B4 Bounded Context（Audit and Reporting）
- Optimistic locking / idempotency key
- Attachment domain / 檔案儲存契約
- Query materialized view / read model projection
- Correction workflow

---

## 8. 審查參考依據

| 文件 | 用途 |
|---|---|
| `docs/01-requirements/decision-log.md` | Decisions D-001 ~ D-013 |
| `docs/02-ddd/domain-model.md` | Aggregate invariants |
| `docs/02-ddd/state-machine.md` | Status values + transitions |
| `docs/02-ddd/permission-matrix.md` | Role / scope rules |
| `docs/02-ddd/application-service.md` | Use case boundaries |
| `docs/03-contracts/api-spec.md` | HTTP contract + error codes |
| `docs/03-contracts/persistence-model.md` | Schema + constraints |
