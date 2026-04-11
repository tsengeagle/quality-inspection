# Design–Implementation Gap Report

> 產出時間：2026-04-11  
> 盤點範圍：後端 Java 實作 B1–B4 + B5b 審計追蹤  
> 排除範圍：B5a 查詢、B5c 報表/匯出、前端

---

## 1. Authority Sources Used

| 文件 | 路徑 |
|------|------|
| Decision Log | `docs/01-requirements/decision-log.md` |
| Glossary | `docs/01-requirements/glossary.md` |
| Context Map | `docs/02-ddd/context-map.md` |
| Domain Model | `docs/02-ddd/domain-model.md` |
| State Machine | `docs/02-ddd/state-machine.md` |
| Permission Matrix | `docs/02-ddd/permission-matrix.md` |
| Application Service | `docs/02-ddd/application-service.md` |
| Domain Events | `docs/02-ddd/domain-events.md` |
| API Spec | `docs/03-contracts/api-spec.md` |
| Persistence Model | `docs/03-contracts/persistence-model.md` |
| Specification by Example | `docs/04-testing/specification-by-example.md` |
| Test Scenarios | `docs/04-testing/test-scenarios.md` |

---

## 2. Design Artifact Inventory

共 12 份設計文件，覆蓋需求、DDD、契約、測試四大區。

- **需求區**：decision-log（21 筆 D-001–D-021）、glossary
- **DDD 區**：domain-model（5 aggregate）、state-machine（ReviewCase 8 states + ReviewSubmission 3 states）、permission-matrix（5 角色）、application-service（~31 use cases）、domain-events（~25 event 定義）、context-map（4 bounded contexts）
- **契約區**：api-spec（~50 endpoints）、persistence-model（~18 tables）
- **測試區**：specification-by-example（40 examples EX-001–EX-040）、test-scenarios（60+ scenarios across 9 categories）

---

## 3. Implementation Inventory

### 3.1 Domain Layer

| Package | 主要產出 |
|---------|---------|
| `domain.project` | ReviewProject, ProjectVersion, ProjectStatus + 6 events |
| `domain.scoresheet` | ScoreSheetDefinition, ScoreSection, ScoreItem, ScoreSheetStatus + 3 events |
| `domain.sampling` | SamplingBatch, SelectedCaseRef, RuleSnapshot, SamplingStatus + 5 events |
| `domain.reviewcase` | ReviewCase, CaseStatus, ReviewerAssignment + 10 events |
| `domain.reviewcase.submission` | ReviewSubmission, SubmissionStatus, ReviewStage, AnswerItem + 4 events |

### 3.2 Application Layer

| Package | Use Case 數量 |
|---------|-------------|
| `application.project` | ~11（含 query） |
| `application.scoresheet` | ~8（含 query） |
| `application.sampling` | ~5（含 query） |
| `application.reviewcase` | ~25（含 B3 initial + B4 rereview + revision） |
| `application.audit` | 2（B5b） |

### 3.3 Interface Layer

| Controller | 掛載路徑 |
|-----------|---------|
| ReviewProjectController | `/review-projects` |
| ScoreSheetController | `/score-sheets` |
| SamplingController | `/sampling-batches` |
| ReviewCaseController | `/review-cases` |
| SubmissionController | `/submissions` |
| AuditController | `/audit` |
| ReportController | `/reports` |

### 3.4 Infrastructure Layer

- Persistence：5 aggregate 對應 JPA entities
- Security：ApiKeyAuthFilter + TzJwtParser + ActorContext
- Event：SpringDomainEventPublisher
- Audit：AuditTrailListener（@EventListener）
- Adapter：StubCandidateSourceAdapter

### 3.5 測試

- 約 78 個測試檔，涵蓋 domain / application / interfaces 三層
- 使用 InMemory repository + CapturingDomainEventPublisher test doubles

---

## 4. Alignment Summary

以下僅總結有差異的項目。**對齊良好（aligned）的項目不逐一列出**。

### 4.1 良好對齊的面向（摘要）

- ✅ ReviewProject / ScoreSheetDefinition lifecycle 與 state machine 一致
- ✅ SamplingBatch invariants（duplicate guard、immutability after completion）
- ✅ ReviewSubmission append-only revision model（createRevision + markSuperseded）
- ✅ CaseStatus terminal states（CLOSED / CANCELLED）正確 guard
- ✅ D-001–D-007 decision compliance 大致對齊
- ✅ D-011–D-013, D-016–D-017, D-019–D-020 decision compliance 對齊
- ✅ 5 個 DomainRole enum 與 permission-matrix 一致
- ✅ Authentication infrastructure（ApiKeyAuthFilter + TzJwtParser + ActorContext）
- ✅ Audit trail listener 正確捕捉所有 DomainEvent
- ✅ Bounded context 責任在 domain package 中正確表達
- ✅ Glossary 核心術語（Sampling, Submission, Terminal State）一致

### 4.2 有差異的面向摘要

| 面向 | partially_aligned | missing_in_impl | implemented_but_undocumented | conflict |
|------|:-----------------:|:----------------:|:----------------------------:|:--------:|
| Domain Events | 1 | 5 | 4 | — |
| Hospital Scope Guard | 7 | — | — | — |
| API Path / Method | 6 | — | 1 | — |
| Request/Response Payload | — | 2 | — | 3 |
| Error Code | — | — | 1 | 1 |
| Persistence Model | 3 | 9 | — | 1 |
| Application Service Inventory | — | — | 5 | — |
| Domain Invariant (Revision Actor) | 1 | — | — | — |
| Domain Model (Entity/Field) | 2 | — | — | — |
| ScoreSheet Freeze Mechanism | — | 1 | — | — |
| Spec-by-Example / Decision | — | — | — | 1 |
| Test Scenario Coverage | — | 5 | — | — |
| D-018 Visibility Masking | — | 1 | — | — |

---

## 5. Detailed Findings

### 5.1 Hospital Scope Guard 缺失（安全性）

**Status**: `partially_aligned`

**Summary**：permission-matrix.md 明確定義 HOSPITAL_MANAGER 只能操作本院專案、REVIEWER 只能存取本院 + 自身指派案件，但多個 use case 只做了 role check 而未呼叫 `requireHospitalScope()`。

**已有 scope check 的 use cases**：AssignReviewCaseUseCase, ReassignReviewCaseUseCase, CancelReviewCaseUseCase, ReturnInitialReviewUseCase, ReopenInitialReviewUseCase 及所有 project/scoresheet use cases

**缺少 scope check 的 use cases**：

| Use Case | 角色 | 缺少的 guard |
|----------|------|-------------|
| ExecuteSamplingUseCase | HOSPITAL_MANAGER | `requireHospitalScope(project.getHospitalId())` |
| AssignRereviewerUseCase | HOSPITAL_MANAGER | `requireHospitalScope(reviewCase.getHospitalId())` |
| SendToRereviewUseCase | HOSPITAL_MANAGER | `requireHospitalScope(reviewCase.getHospitalId())` |
| CloseWithoutRereviewUseCase | HOSPITAL_MANAGER | `requireHospitalScope(reviewCase.getHospitalId())` |
| StartInitialReviewUseCase | REVIEWER | `requireHospitalScope(reviewCase.getHospitalId())` |
| SaveInitialReviewDraftUseCase | REVIEWER | `requireHospitalScope(reviewCase.getHospitalId())` |
| SubmitInitialReviewUseCase | REVIEWER | `requireHospitalScope(reviewCase.getHospitalId())` |

> **Note**：REVIEWER use cases 有做 identity check（actor == submission owner），但未做 hospital 層級的隔離。若 user ID 跨院共用則構成安全漏洞。

**Design source**: permission-matrix.md §4.2, §6.4; application-service.md §3.1, §6.1, §7.1
**Implementation source**: 上述各 UseCase.java
**Recommended action**: `update_code`
**Target artifact**: 上述 7 個 UseCase — 加入 `actor.requireHospitalScope(...)` 

---

### 5.2 Domain Events 缺失

**Status**: `missing_in_implementation`

domain-events.md 定義的事件與實作的事件對比：

| 設計事件名 | 實作狀態 | 備註 |
|-----------|---------|------|
| ReviewCaseCreated | ❌ 未實作 | domain-events.md §6.1 |
| InitialReviewCompleted | ❌ 未實作 | domain-events.md §6.4，completeInitialReview() 未發事件 |
| RereviewCompleted | ❌ 未實作 | domain-events.md §6.8 |
| ReviewCaseClosed | ❌ 未實作 | domain-events.md §6.9，只有 CaseClosedWithoutRereviewEvent |
| ScoreSheetFrozen | ❌ 未實作 | domain-events.md §4.7，freezeVersion() 整體缺失 |
| ReviewAttachmentAdded | ❌ 未實作 | domain-events.md §7.3，attachFile() 未實作 |

**Design source**: domain-events.md §4.7, §6.1, §6.4, §6.8, §6.9, §7.3
**Recommended action**: `update_code`（新增缺失事件類別與發布邏輯）
**Target artifact**: `domain/reviewcase/event/`, `domain/scoresheet/event/`

---

### 5.3 Domain Events 未入文件

**Status**: `implemented_but_undocumented`

以下事件存在於實作但未記錄在 domain-events.md：

| 實作事件類別 | 備註 |
|------------|------|
| ReviewProjectDraftUpdatedEvent | 專案草稿更新時發布 |
| ScoreSheetDraftUpdatedEvent | 評分表草稿更新時發布 |
| ScoreSheetBoundToProjectEvent | 評分表綁定至專案時發布 |
| ReviewDraftSavedEvent | 審查草稿儲存時發布 |
| ReviewSubmissionRevisedEvent | submission revision 時發布（spec 有 ReviewSubmissionRevised 但列在不同位置） |

**Design source**: domain-events.md 未收錄
**Recommended action**: `update_spec`
**Target artifact**: `docs/02-ddd/domain-events.md`

---

### 5.4 API Path 不一致

**Status**: `partially_aligned` / `conflict`

| 操作 | api-spec.md 定義路徑 | 實作路徑 | 差異 |
|------|---------------------|---------|------|
| Save Initial Draft | `POST .../initial-review/draft` | `POST .../initial-review/save-draft` | 路徑名稱不同 |
| Submit Initial Review | `POST /review-submissions/{submissionId}/initial-review/submit` | `POST /review-cases/{caseId}/initial-review/submit` | 資源根不同（submission vs case） |
| Revise Initial Review | `POST /review-submissions/{submissionId}/initial-review/revise` | `POST /review-cases/{caseId}/submissions/{submissionId}/revise` | 資源根與路徑結構不同 |
| Return Initial Review | `POST .../return-initial-review` | `POST .../return` | 路徑名稱不同 |
| Save Rereview Draft | `POST .../rereview/draft` | `PUT .../rereview-draft` | HTTP method 和路徑都不同 |
| Submit Rereview | `POST /review-submissions/{submissionId}/rereview/submit` | `POST /review-cases/{caseId}/submit-rereview` | 資源根不同 |
| Revise Rereview | `POST /review-submissions/{submissionId}/rereview/revise` | `POST /review-cases/{caseId}/revise-rereview` | 資源根不同 |

**Design source**: api-spec.md §10.8, §11.2–§11.4, §12.2–§12.4
**Implementation source**: ReviewCaseController.java
**Recommended action**: `update_code` 或 `update_spec`（需決定以哪邊為準）
**Target artifact**: `api-spec.md` 或 `ReviewCaseController.java`

---

### 5.5 Request / Response Payload 不一致

**Status**: `conflict`

| 操作 | 差異 |
|------|------|
| Save Initial Draft Request | api-spec 有 `attachments` 欄位，實作無 |
| Revise Initial Review Request | api-spec 有 `attachments` 欄位，實作無 |
| Submit Initial Review Response | api-spec 定義 submissionId/caseStatus/totalScore 等欄位，實作回傳 null |
| Submit Rereview Response | api-spec 定義 submissionId/finalScore 等欄位，實作回傳 null |
| Revision Result Response | 缺少 `oldSubmissionId` 欄位 |

**Design source**: api-spec.md §11.2–§11.4, §12.3–§12.4
**Implementation source**: ReviewCaseController.java, SaveInitialReviewDraftRequest.java, RevisionResultResponse.java
**Recommended action**: `update_code`
**Target artifact**: 各 Request/Response DTO

---

### 5.6 Error Code 映射衝突

**Status**: `conflict`

| 問題 | 設計 | 實作 |
|------|------|------|
| UnauthorizedException HTTP status | api-spec 定義 401 | GlobalExceptionHandler 回傳 403 FORBIDDEN |
| VALIDATION_ERROR code | api-spec 未定義 | GlobalExceptionHandler 回傳 `VALIDATION_ERROR` |

**Design source**: api-spec.md §4.1
**Implementation source**: GlobalExceptionHandler.java:38–39, :66
**Recommended action**: `update_code`（UnauthorizedException → 401）+ `update_spec`（收錄 VALIDATION_ERROR）
**Target artifact**: `GlobalExceptionHandler.java`, `api-spec.md`

---

### 5.7 Persistence Model 缺失（結構性差異）

**Status**: `missing_in_implementation`

persistence-model.md 定義但實作中不存在的 table / entity：

| 設計 Table | 實作狀態 | 說明 |
|-----------|---------|------|
| `review_project_version` | ❌ 版本嵌入主 entity | spec 要求獨立 version 表 |
| `review_project_version_policy` | ❌ 不存在 | policy snapshot 持久化 |
| `score_sheet_version` | ❌ 版本嵌入主 entity | spec 要求獨立 version 表 |
| `review_submission_answer` | ❌ 用 JSON 替代 | spec 要求正規化表 |
| `review_submission_snapshot` | ❌ 不存在 | 不可變 snapshot + checksum |
| `review_submission_attachment` | ❌ 用 JSON 替代 | spec 要求獨立表 |
| `sampling_batch_snapshot` | ❌ 用 String 欄位替代 | spec 要求獨立表 |
| `sampling_batch_message_log` | ❌ 不存在 | 執行日誌 |
| `review_case_snapshot_ref` | ❌ 嵌入主 entity | spec 要求獨立表 |
| `audit_trail` | ❌ JPA entity 不存在 | B5b listener 存在但無 entity |
| `outbox_event` | ❌ 不存在 | transactional outbox pattern |

**補充差異**：

| 問題 | 說明 |
|------|------|
| ScoreSheetSection FK | spec 指向 `score_sheet_version.id`，實作指向 `score_sheet.id` |
| ReviewCaseEntity 缺欄位 | 缺少 `department_id`, `medical_record_key`, `review_target_type/value` |
| ReviewCaseEntity 缺審計欄位 | 缺少 `created_at`, `updated_at` |
| ReviewSubmissionEntity 缺 unique constraint | 缺少 `(case_id, stage, revision_no)` unique |
| ScoreItemEntity 缺 unique constraint | 缺少 `(scoreSheetVersionId, itemId)` unique |
| 全面缺少 `@Index` | spec 定義的查詢索引均未實作 |

**Design source**: persistence-model.md §5.2–§6.2, §12
**Recommended action**: `update_code` 或 `update_spec`（需決定簡化策略是否可接受）
**Target artifact**: persistence entities 或 `persistence-model.md`

---

### 5.8 Application Service 未入文件

**Status**: `implemented_but_undocumented`

以下 use cases 存在於實作但未記錄在 application-service.md：

| Use Case | 備註 |
|----------|------|
| ReopenInitialReviewUseCase | app-service §9.3 說 "revision ≠ reopen"，但此 use case 存在且未被文件化 |
| SendToRereviewUseCase | 手動觸發複審升級，未文件化 |
| CloseWithoutRereviewUseCase | 無需複審直接結案，未文件化 |
| ReassignRereviewerUseCase | 複審醫師重新指派，未文件化 |
| GetRereviewRevisionHistoryUseCase | 複審 revision 歷程，未文件化 |

**Design source**: application-service.md
**Recommended action**: `update_spec`
**Target artifact**: `docs/02-ddd/application-service.md`

---

### 5.9 Revision Actor Guard 缺失（Domain 層）

**Status**: `partially_aligned`

domain-model.md §8.2 規定：
- 初審 revision 僅原審查醫師可發起
- 複審 revision 僅複審醫師可發起

實作中 `ReviewSubmission.createRevision()` **未接收 actorId 參數**，因此 domain 層不檢查 actor 身份。目前此檢查由 application layer use case 執行（ReviseInitialReviewUseCase / ReviseRereviewUseCase），但不在 aggregate invariant 內。

**Design source**: domain-model.md §8.2
**Implementation source**: ReviewSubmission.createRevision()
**Recommended action**: `pending_confirmation`（需確認是否接受 application layer guard 即可，或需下放至 domain）
**Target artifact**: ReviewSubmission.java 或 domain-model.md

---

### 5.10 ScoreSheet Freeze 機制缺失

**Status**: `missing_in_implementation`

domain-model.md §5.2 定義 `freezeVersion()` 方法與 FROZEN 狀態，domain-events.md §4.7 定義 `ScoreSheetFrozen` 事件。實作中 ScoreSheetStatus 只有 DRAFT / PUBLISHED，無 FROZEN 狀態或 freeze 方法。

**Design source**: domain-model.md §5.2, domain-events.md §4.7
**Implementation source**: ScoreSheetStatus.java, ScoreSheetDefinition.java
**Recommended action**: `update_code` 或 `update_spec`（需確認 freeze 機制是否仍為必要）
**Target artifact**: ScoreSheetDefinition.java 或 domain-model.md

---

### 5.11 MedicalRecordIdentity Value Object 缺失

**Status**: `partially_aligned`

domain-model.md §7.1 列出 MedicalRecordIdentity 為 ReviewCase aggregate 下的 entity。實作中使用扁平欄位（`sourceCaseKey`, `medicalRecordType`, `hospitalId`），未抽出為獨立 value object。

**Design source**: domain-model.md §7.1
**Implementation source**: ReviewCase.java
**Recommended action**: `pending_confirmation`（需確認是否需要抽出 VO 或 spec 過度設計）
**Target artifact**: ReviewCase.java 或 domain-model.md

---

### 5.12 ReviewCase 缺少 latestRereviewSubmissionId

**Status**: `partially_aligned`

domain-model.md §5.4 定義 ReviewCase 應有 `latestInitialSubmissionId` 和 `latestRereviewSubmissionId`。實作只有 `latestInitialSubmissionId`，無 `latestRereviewSubmissionId`。

**Design source**: domain-model.md §5.4
**Implementation source**: ReviewCase.java
**Recommended action**: `update_code`
**Target artifact**: ReviewCase.java

---

### 5.13 EX-003 與 D-012 衝突（文件自身不一致）

**Status**: `conflict`

specification-by-example.md EX-003 描述「複審最終分數由複審醫師決定」暗示 finalScore = rereviewScore。但 decision-log.md D-012 明確規定 finalScore = initialScore × weight + rereviewScore × weight（加權平均）。實作遵循 D-012。

**Design source**: specification-by-example.md EX-003; decision-log.md D-012
**Implementation source**: ScoreCalculationService.java
**Recommended action**: `update_examples`
**Target artifact**: `docs/04-testing/specification-by-example.md` — 修正 EX-003 以反映加權平均

---

### 5.14 D-018 複審答案遮蔽未實作

**Status**: `missing_in_implementation`

D-018 規定複審醫師進行複審時不可看到初審原始答案、評語與分數。目前未找到任何 dynamic visibility masking 的實作或測試。

**Design source**: decision-log.md D-018
**Implementation source**: 未找到對應實作
**Recommended action**: `update_code`
**Target artifact**: 需在 query/detail 層實作角色 + 狀態的動態遮蔽（與 B5a query 相關，可能在後續階段處理）

---

### 5.15 D-021 複審 Revision 允許 CLOSED — 已實作但文件不完全同步

**Status**: `partially_aligned`

D-021 在 decision-log.md 已記錄。ReviseRereviewUseCase 程式碼有 D-021 註解。但 application-service.md §9.2 仍寫「revision 僅適用未結案且未取消案件」，尚未同步此例外。permission-matrix.md §6.6 也顯示「結案後 revision: N」。

**Design source**: decision-log.md D-021; application-service.md §9.2; permission-matrix.md §6.6
**Implementation source**: ReviseRereviewUseCase.java:30
**Recommended action**: `update_spec`
**Target artifact**: `application-service.md`, `permission-matrix.md` — 同步 D-021 例外規則

---

### 5.16 測試場景覆蓋缺口

**Status**: `missing_in_implementation`

以下 spec example / test scenario 缺少對應測試：

| 項目 | 類型 | 說明 |
|------|------|------|
| D-018 複審答案遮蔽 | Decision | 未實作 masking 也未有測試 |
| D-001 exception CLOSED rereview revision | Decision | 有實作但缺專門測試驗證 CLOSED 狀態下的複審 revision |
| TS-PM-003A 系統管理員 metadata masking | Test Scenario | 未有明確驗證 response 遮蔽的測試 |
| TS-AT-001 附件 revision 隔離 | Test Scenario | EX-035 定義但無對應測試 |
| EX-029 同端點三角色不同 view | Spec Example | 無統一場景測試 |
| TS-NG-001–TS-NG-005 負面案例 | Test Scenario | test-scenarios.md §9 定義但對應測試未明確標識 |
| TS-PM-004A 稽核員 scope + detail view | Test Scenario | scope + detail view 組合未測 |

**Recommended action**: `update_tests`
**Target artifact**: 各對應測試類別

---

### 5.17 Context Map 模組結構風格差異

**Status**: `partially_aligned`

context-map.md 建議以 bounded context 為頂層目錄（`project-config/`, `sampling/`, `review-execution/`）。實作採用 layer-first（`domain/`, `application/`, `infrastructure/`）+ 子 package 對應 context 的結構。

此為風格差異而非錯誤，domain package 內確實按 context 分組（`project`, `sampling`, `reviewcase`, `scoresheet`）。

**Recommended action**: `update_spec`（在 context-map.md 加註說明實際採用的模組風格）
**Target artifact**: `docs/02-ddd/context-map.md`

---

### 5.18 Glossary「Rereview」拼寫

**Status**: `partially_aligned`

glossary.md 同時使用「Re-review」和「Rereview」。程式碼統一使用 camelCase `rereview`（無連字號）。建議在 glossary 中明確標示技術命名規範。

**Recommended action**: `update_spec`
**Target artifact**: `docs/01-requirements/glossary.md`

---

## 6. Gap Classification Table

| gap_id | area | design_source | implementation_source | status | summary | evidence | recommended_action | target_artifact | note |
|--------|------|---------------|----------------------|--------|---------|----------|--------------------|-----------------|------|
| GAP-01 | Hospital Scope | permission-matrix §4.2, §6.4 | 7 個 UseCase 缺 requireHospitalScope | `partially_aligned` | HOSPITAL_MANAGER / REVIEWER use cases 缺少院區隔離 | 比對 requireHospitalScope 呼叫清單 | `update_code` | 7 個 UseCase.java | 安全性風險 — 高優先 |
| GAP-02 | Domain Events | domain-events.md §6.1,§6.4,§6.8,§6.9,§4.7,§7.3 | 6 個事件未實作 | `missing_in_implementation` | ReviewCaseCreated, InitialReviewCompleted, RereviewCompleted, ReviewCaseClosed, ScoreSheetFrozen, ReviewAttachmentAdded 缺失 | grep Event.java 比對 | `update_code` | domain event classes | freeze/attachment 需先確認是否仍需要 |
| GAP-03 | Domain Events | domain-events.md 未收錄 | 4 個事件在 code 存在 | `implemented_but_undocumented` | DraftUpdated / BoundToProject / DraftSaved events 未入文件 | event class listing | `update_spec` | domain-events.md | |
| GAP-04 | API Path | api-spec.md §10.8,§11.2–§11.4,§12.2–§12.4 | ReviewCaseController.java | `partially_aligned` | 7 個端點路徑 / HTTP method 與 spec 不一致 | 逐條比對 | `update_code` 或 `update_spec` | api-spec.md 或 Controller | 需決定哪邊為準 |
| GAP-05 | Request/Response | api-spec.md §11.2–§12.4 | DTO classes | `conflict` | attachments 缺失、submit response 回 null、revision response 缺 oldSubmissionId | 欄位比對 | `update_code` | Request/Response DTOs | |
| GAP-06 | Error Code | api-spec.md §4.1 | GlobalExceptionHandler:38 | `conflict` | UnauthorizedException → 403（應為 401）；VALIDATION_ERROR 未定義 | status code 比對 | `update_code` + `update_spec` | GlobalExceptionHandler, api-spec.md | |
| GAP-07 | Persistence | persistence-model.md §5.2–§6.2 | persistence entities | `missing_in_implementation` | 11 個 table/entity 未實作（version 分離、snapshot、attachment、outbox 等） | entity listing 比對 | `update_code` 或 `update_spec` | persistence layer | 需決定是否簡化 persistence 策略 |
| GAP-08 | Persistence Constraints | persistence-model.md §5.7,§5.14,§12 | JPA entities | `missing_in_implementation` | unique constraints 和 indexes 未加 | annotation 搜尋 | `update_code` | JPA entities | |
| GAP-09 | App Service Inventory | application-service.md | 5 個 UseCase 未文件化 | `implemented_but_undocumented` | Reopen, SendToRereview, CloseWithoutRereview, ReassignRereviewer, GetRereviewRevisionHistory | UseCase listing | `update_spec` | application-service.md | |
| GAP-10 | Revision Actor | domain-model.md §8.2 | ReviewSubmission.createRevision() | `partially_aligned` | domain 層 createRevision() 未接收 actorId，guard 在 app layer | code review | `pending_confirmation` | ReviewSubmission 或 domain-model.md | 需確認是否接受 |
| GAP-11 | ScoreSheet Freeze | domain-model.md §5.2, domain-events.md §4.7 | ScoreSheetDefinition.java | `missing_in_implementation` | FROZEN 狀態 / freezeVersion() / ScoreSheetFrozen event 全部缺失 | status enum 比對 | `update_code` 或 `update_spec` | ScoreSheetDefinition 或 domain-model.md | 需確認是否仍需要 |
| GAP-12 | Domain Model | domain-model.md §7.1 | ReviewCase.java | `partially_aligned` | MedicalRecordIdentity VO 未抽出 | field 比對 | `pending_confirmation` | ReviewCase 或 domain-model.md | |
| GAP-13 | Domain Model | domain-model.md §5.4 | ReviewCase.java | `partially_aligned` | 缺少 latestRereviewSubmissionId 欄位 | field grep | `update_code` | ReviewCase.java | |
| GAP-14 | Spec Consistency | EX-003 vs D-012 | ScoreCalculationService.java | `conflict` | EX-003 暗示複審分數直接決定 finalScore，D-012 規定加權平均 | 文件交叉比對 | `update_examples` | specification-by-example.md | 文件自身不一致 |
| GAP-15 | Decision Compliance | decision-log.md D-018 | 無對應實作 | `missing_in_implementation` | 複審答案遮蔽未實作 | 搜尋 masking 邏輯 | `update_code` | query/detail 層 | 與 B5a 相關 |
| GAP-16 | Spec Sync | decision-log.md D-021 | ReviseRereviewUseCase:30 | `partially_aligned` | D-021 已實作但 app-service.md / permission-matrix 未同步 | 文件交叉比對 | `update_spec` | application-service.md, permission-matrix.md | |
| GAP-17 | Test Coverage | spec-by-example, test-scenarios | test files | `missing_in_implementation` | 7 個 scenario/example 缺少對應測試 | test file listing | `update_tests` | test classes | |
| GAP-18 | Context Map | context-map.md §5.1 | package structure | `partially_aligned` | 模組結構風格差異（layer-first vs context-first） | package listing | `update_spec` | context-map.md | 非錯誤，僅風格 |
| GAP-19 | Glossary | glossary.md | code naming | `partially_aligned` | Rereview vs Re-review 拼寫不統一 | text 搜尋 | `update_spec` | glossary.md | 低優先 |
| GAP-20 | Persistence | persistence-model.md §5.12 | ReviewCaseEntity | `partially_aligned` | 缺少 department_id, medical_record_key, review_target, created_at, updated_at | column 比對 | `update_code` | ReviewCaseEntity.java | |

---

## 7. Recommended Minimal Follow-up Plan

### 🔴 高優先（安全性 / 資料正確性）

1. **GAP-01**：補齊 7 個 use case 的 `requireHospitalScope()` guard
2. **GAP-06**：修正 UnauthorizedException → HTTP 401

### 🟠 中優先（設計 vs 實作對齊）

3. **GAP-04 + GAP-05**：**先做決定**——API path 以 spec 為準還是以實作為準？決定後統一。建議更新 `decision-log.md` 記錄此決策，再統一修改。
4. **GAP-07 + GAP-08**：**先做決定**——persistence 是否需要完整分離 version/snapshot/attachment 表？還是接受 JSON embedded 策略？若接受簡化策略，需更新 `persistence-model.md`。
5. **GAP-14**：修正 EX-003 以符合 D-012 加權平均規則（文件自身修正）
6. **GAP-16**：同步 D-021 到 application-service.md 和 permission-matrix.md

### 🟡 低優先（補完 / 文件化）

7. **GAP-02**：缺失 domain events — 先確認 freeze / attachment 機制是否仍需要，再決定補或刪
8. **GAP-03 + GAP-09**：將未文件化的 events / use cases 補入 spec
9. **GAP-11**：ScoreSheet freeze 機制 — 確認是否仍為需求
10. **GAP-13**：補 latestRereviewSubmissionId 欄位
11. **GAP-17**：補齊缺少的測試
12. **GAP-15**：D-018 答案遮蔽 — 可於 B5a query 階段一併處理

### ⚪ 確認後再決定

13. **GAP-10**：Revision actor guard 放在 domain 層還是 app 層？
14. **GAP-12**：MedicalRecordIdentity 是否需要抽出為 VO？
15. **GAP-18 + GAP-19**：context-map 模組風格 + glossary 拼寫，低風險文件修正
