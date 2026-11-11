# quality-inspection 後端 Gap 重新盤點

- 盤點日期：2026-11-11
- 對象：`~/source/quality-inspection`，HEAD `d84107f`（2026-11-09）加上工作目錄的未提交變更
- 範圍：後端 `services/quality-inspection-service`，對照 `docs/01-requirements` 到 `docs/04-testing`
- 前一版：`design-implementation-gap.md`（2026-04-11，之後有 65 個 commit）。前一版保留不動；本報告第 4 節標「未重查」的項目仍以前一版的內容為準。
- 產出方式：由 Claude Code 盤點，尚未經人工複核。

## 0. 方法與限制

| 面向 | 做法 | 可信度 |
|---|---|---|
| Domain event、use case、端點、資料表、錯誤代碼 | 從文件與程式碼各抽出清單，做集合比對 | 高（機械比對） |
| 狀態機、院區範圍、分層依賴 | 讀程式碼並對照決策原文 | 中高 |
| 決策 D-022 到 D-038 是否落地 | 以關鍵字搜尋加抽讀 | 中（只確認「存在」，沒驗證行為正確） |

沒有做的事：

- 沒有執行測試，不知道目前 574 個 `@Test` 是否全綠。
- 沒有逐欄比對請求與回應 DTO、aggregate 屬性、資料表欄位。
- 沒有逐條驗證 68 個 `EX-` 範例與 97 個 `TS-` 情境的行為，只查了 id 的追溯。
- 前端完全不在範圍內。
- 9 份權威文件有未提交的修改，另有 1 個未追蹤的測試檔；本報告以工作目錄現況為準。

## 1. 總表

| 面向 | 文件有 | 程式有 | 文件有、程式沒有 | 程式有、文件沒有 | 兩邊不一致 |
|---|---:|---:|---:|---:|---:|
| Domain event | 27 | 28 | 6 | 7 | 1（命名） |
| Use case | 36 具名 + 2 未命名 | 49 + 2 個查詢服務 | 2（未命名的固定、結案） | 15 | 4（命名或落點） |
| HTTP 端點 | 42 | 51 | 0 | 8 | 9（路徑） |
| 資料表 | 24（其中 4 張為外部主檔） | 11 | 11 | 2 | — |
| 業務錯誤代碼 | 19 | 約 40 | 6 | 28 | — |
| `EX-` / `TS-` 追溯 | 68 / 97 | 0 / 0 | 全部 | — | — |

## 2. 最嚴重的發現

### R-01 已接受的決策與程式行為相反（D-031、D-032、D-034、DD-03）

決策都是 `accepted`，但程式仍是舊行為：

| 決策 | 文件要求 | 程式現況 |
|---|---|---|
| D-031 | 初審「提交」不觸發進複審；要先「固定」，再依固定結果判斷 | `ReviewCase.completeInitialReview()` 在提交時直接比對門檻並轉 `PENDING_REREVIEW`（`ReviewCase.java:190-191`） |
| D-032 / D-034 | 複審「提交」不結案；由提交醫師另下 Close Command | `ReviewCase.completeRereview()` 直接設 `CLOSED`（`ReviewCase.java:279`） |
| DD-03 | 固定要記錄操作者、時間、當時有效的 submission | 程式裡找不到任何固定的概念 |

文件自己把 use case 名稱、API、狀態表示、交易與事件機制都標成 `UNKNOWN`。所以這不是單純漏做，而是「已決定、未設計、未實作」。

專案負責人確認（2026-11-11）：這一項是漏掉的，不在既有排程內。

### R-02 院區範圍檢查仍有缺口（D-024）

D-024 要求所有處理 `ReviewCase` 或 `SamplingBatch` 的 use case 都要 `requireHospitalScope`。原報告列的 7 支已補上，但以下 9 支沒有：

| Use case | 類型 | 現況 |
|---|---|---|
| `ReassignRereviewerUseCase` | 命令 | 只檢查角色 |
| `PreviewSamplingPopulationUseCase` | 查詢 | 只檢查角色 |
| `ExportSamplingBatchResultUseCase` | 查詢 | 只檢查角色 |
| `GetSamplingBatchDetailUseCase` | 查詢 | 只檢查角色 |
| `ListSamplingBatchesUseCase` | 查詢 | 只檢查角色，查詢條件沒有院區 |
| `ListReviewCasesUseCase` | 查詢 | 院區是 client 傳入的選填參數 |
| `ListAuditTrailsUseCase` | 查詢 | 院區是 client 傳入的選填參數 |
| `GetCaseLifecycleUseCase` | 查詢 | 只檢查角色 |
| `GetRereviewRevisionHistoryUseCase` | 查詢 | 沒有角色檢查也沒有院區檢查 |

列表類端點的 `hospitalId` 來自 `@RequestParam(required = false)`（`ReviewCaseController.java:114`、`AuditController.java`），不是從 actor 取得。

限制：我沒有實際發請求驗證，也沒有確認 api-gateway 是否另外擋了一層。

### R-03 規格範例與測試之間沒有任何追溯

`specification-by-example.md` 有 68 個 `EX-` id，`test-scenarios.md` 有 97 個 `TS-` id。測試程式碼裡引用到的數量是 0 和 0。測試引用的決策 id 只有 D-017、D-032、D-035、D-037 四個。

所以無法機械地回答「哪個範例有測試、哪個沒有」。

## 3. 各面向明細

### 3.1 Domain event

文件有、程式沒有（6，與原報告相同）：
`ScoreSheetFrozen`、`ReviewCaseCreated`、`InitialReviewCompleted`、`RereviewCompleted`、`ReviewCaseClosed`、`ReviewAttachmentAdded`

程式有、文件沒有（7，原報告是 5）：
`CaseClosedWithoutRereviewEvent`、`RereviewRevisedEvent`、`RereviewSubmittedEvent`、`ReviewCaseReassignedEvent`、`ReviewProjectDraftUpdatedEvent`、`ScoreSheetBoundToProjectEvent`、`ScoreSheetDraftUpdatedEvent`

命名不一致（1）：文件 `ReviewCaseEscalatedToRereview`，程式 `CaseEscalatedToRereviewEvent`

### 3.2 Use case

程式有、文件 `application-service.md` 沒有（15）：

- reviewcase：`CloseWithoutRereview`、`SendToRereview`、`ReopenInitialReview`、`GetCaseAssignmentContext`、`ListCaseAssignmentCandidates`、`GetRereviewRevisionHistory`、`ListReviewCases`
- project：`GetReviewProjectVersion`、`ListReviewProjects`
- sampling：`GetSamplingExecuteContext`、`ListSamplingBatches`
- scoresheet：`GetScoreSheetDetail`、`GetScoreSheetSections`、`GetScoreSheetVersion`、`ListScoreSheetSummaries`

命名或落點不一致（4）：

| 文件 | 程式 |
|---|---|
| `GetReviewSubmissionDetailUseCase` | `GetSubmissionDetailUseCase` |
| `GetAuditTrailUseCase` | `ListAuditTrailsUseCase` |
| `ExportReviewReportUseCase` | `reporting/ExportQueryService` |
| `GetDashboardSummaryUseCase` | `reporting/DashboardQueryService` |

文件有、程式沒有（2）：初審固定、複審 Close Command（見 R-01）

### 3.3 HTTP 端點

路徑不一致（9）。D-022 已決定採 case-rooted 風格：

| api-spec.md | 程式 |
|---|---|
| `POST /score-sheets/{id}/publish-version` | `POST /score-sheets/{id}/publish` |
| `POST /review-cases/{caseId}/return-initial-review` | `POST .../initial-review/return` |
| `POST .../submissions/{sid}/initial-review/submit` | `POST .../initial-review/submit`（沒有 `sid`） |
| `POST .../submissions/{sid}/initial-review/revise` | `POST .../initial-review/submissions/{sid}/revise` |
| `POST .../rereview/start` | `POST .../start-rereview` |
| `POST .../submissions/{sid}/rereview/submit` | `POST .../rereview/submit`（沒有 `sid`） |
| `POST .../submissions/{sid}/rereview/revise` | `POST .../rereview/submissions/{sid}/revise` |
| `GET /review-cases/{caseId}/submissions/{sid}` | `GET /review-submissions/{sid}`（不是 case-rooted） |
| `GET /review-cases/{caseId}/revisions` | `GET .../revision-history` 與 `GET .../rereview-revisions` |


程式有、文件沒有獨立條目（8）：
`POST /review-cases/{id}/reopen`、`.../close-without-rereview`、`.../send-to-rereview`、`GET /score-sheets`、`GET /score-sheets/{id}/version`、`GET /score-sheets/{id}/sections`、`GET /sampling-batches/{id}/export`、`GET /review-projects/{id}/version`（文件只在內文提到）

### 3.4 資料表

文件有、程式沒有 JPA entity（11，不含外部主檔）：

- 不算缺口的主檔 4：`hospital`、`department`、`system_user`、`user_role_assignment`。專案負責人確認（2026-11-11）由外部主檔提供。`persistence-model.md` §4 應加註這一點
- 版本與快照 8：`review_project_version`、`review_project_version_policy`、`score_sheet_version`、`sampling_batch_snapshot`、`sampling_batch_message_log`、`review_case_snapshot_ref`、`review_submission_snapshot`、`review_submission_attachment`
- 其他 3：`review_submission_answer`、`outbox_event`、`review_case_read_model`

程式有、文件沒有（2）：`his_staff_directory`、`review_project_record_types`

約束：全部 entity 只有 2 個唯一鍵約束，`@Index` 為 0，`@Version` 為 0（沒有樂觀鎖；D-034 與 DD-03 都提到結案與 revision 的競爭）。

Schema 管理：沒有遷移腳本；基礎設定 `ddl-auto: none`，`local` profile 用 H2 記憶體資料庫與 `ddl-auto: update`。

### 3.5 錯誤代碼

規格有、程式沒用到（6）：
`PROJECT_VERSION_NOT_PUBLISHABLE`、`REVISION_NOT_ALLOWED`、`REVISION_ACTOR_MISMATCH`、`SUBMISSION_NOT_OWNED_BY_ACTOR`、`SUBMISSION_STAGE_MISMATCH`、`NO_CHANGES`（D-038）

程式有、規格沒列（28）：
`CASE_NOT_FOUND`、`CASE_NOT_IN_INITIAL_REVIEW`、`CASE_NOT_IN_PROGRESS`、`CASE_NOT_INITIAL_REVIEW_COMPLETED`、`CASE_NOT_PENDING_ASSIGNMENT`、`CASE_NOT_PENDING_INITIAL_REVIEW`、`CASE_NOT_PENDING_REREVIEW`、`CASE_NOT_REREVIEW_IN_PROGRESS`、`CASE_NOT_RETURNED`、`CASE_TERMINAL`、`INVALID_METRIC_VALUE`、`INVALID_PROJECT_NAME`、`ITEM_NOT_FOUND`、`NO_BOUND_SCORE_SHEET`、`NO_REREVIEWER_ASSIGNED`、`NOT_ASSIGNED_REREVIEWER`、`NOT_ASSIGNED_REVIEWER`、`NOT_SUBMISSION_OWNER`、`REQUIRED_ITEMS_NOT_ANSWERED`、`REREVIEWER_SAME_AS_REVIEWER`、`SAMPLING_BATCH_NOT_FOUND`、`SCORE_SHEET_NOT_BOUND`、`SCORE_SHEET_NOT_PUBLISHED`、`SECTION_NOT_FOUND`、`SUBMISSION_NOT_DRAFT`、`SUBMISSION_NOT_FOUND`、`SUBMISSION_NOT_INITIAL`、`SUBMISSION_NOT_SUBMITTED`

寫法不一致：

- `ResourceNotFoundException` 有兩種用法：31 處傳錯誤代碼（`"CASE_NOT_FOUND"`），23 處傳實體名稱（`"ReviewCase"`）。
- application 與 domain 有 15 處 `IllegalArgumentException`、1 處 `IllegalStateException`，會被對應成 500 `INTERNAL_ERROR`。例：`PreviewSamplingPopulationUseCase` 找不到專案時回 500 而不是 404。

### 3.6 決策落地狀況（D-022 起）

| 決策 | 狀態 | 依據 |
|---|---|---|
| D-022 case-rooted API | 部分 | 見 3.3 |
| D-023 `ROLE_DENIED` | 已做 | 程式有該代碼 |
| D-024 院區範圍 | 部分 | 見 R-02 |
| D-025 `latestRereviewSubmissionId` | 已做 | `ReviewCase` 有對應欄位 |
| D-026 遮罩在 application 層 | 已做（未驗證正確性） | `GetSubmissionDetailUseCase`、`ReviewReadAccessPolicy` |
| D-031 / D-032 / D-034 / DD-03 | 未做 | 見 R-01 |
| D-033 / D-035 盲審與 fail-closed | 已做（未驗證正確性） | 最近兩個 commit、`PermanentBlindReviewReadApiTest` |
| D-036 移除 `lifecycleLatestStage` | 已做 | 主程式碼無此欄位，只出現在測試 |
| D-037 案件 `updatedAt` | 進行中 | 只有未追蹤的 `UpdatedAtContractIntegrationTest`；`ReviewCase` 沒有此欄位 |
| D-038 `NO_CHANGES` | 未做 | 主程式碼無此代碼 |

D-027 到 D-030 是前端或 UI 決策，沒有檢查。

### 3.7 程式結構

分層違規（3）：

| 位置 | 問題 |
|---|---|
| `application/reviewcase/ReviseInitialReviewUseCase.java:21` | application 引用 `interfaces.rest.BadRequestException` |
| `interfaces/rest/ActorContextArgumentResolver.java:4` | interfaces 引用 `infrastructure.security.ApiKeyAuthFilter` |
| `reporting/ExportQueryService.java:5` | reporting 直接引用 JPA entity `ReviewCaseEntity` |

其他：

- domain 與 application 有 12 個檔案直接呼叫 `Instant.now()`、`LocalDateTime.now()` 或 `UUID.randomUUID()`。
- 7 支 use case 用 `String.format` 手工拼 JSON 字串當 event 的 detail，值沒有跳脫。
- `domain/common/DomainEvent` 繼承 Spring 的 `ApplicationEvent`，是 domain 唯一依賴框架的檔案。

### 3.8 測試

80 個測試類別、574 個 `@Test`：application 50、interfaces 12、domain 9、infrastructure 7、reporting 2。其中 20 個類別用 `@SpringBootTest`，沒有 `@Disabled`。

## 4. 原報告 20 項的現況

| 原編號 | 主題 | 現況 |
|---|---|---|
| GAP-01 | 院區範圍 | 原 7 支已修；另有 9 支缺（R-02） |
| GAP-02 | 缺 6 個 event | 未變 |
| GAP-03 | 未入文件的 event | 從 5 個變 7 個 |
| GAP-04 | API 路徑 | D-022 已決定方向；仍有 9 處不一致 |
| GAP-05 | 請求與回應欄位 | 未重查；文件自己承認 attachments 沒有從 controller 傳入 |
| GAP-06 | 401 / 403 | 已修。401 由 `ApiKeyAuthFilter` 回，403 用於角色與範圍，`VALIDATION_ERROR` 已入規格 |
| GAP-07 | 缺資料表 | `audit_trail` 已有；其餘未變 |
| GAP-08 | 約束與索引 | 幾乎未變 |
| GAP-09 | 未入文件的 use case | 從 5 支變 15 支 |
| GAP-10 | revision 的身分檢查位置 | 未重查 |
| GAP-11 | ScoreSheet freeze | 未變（文件仍有 `freezeVersion()`，程式沒有） |
| GAP-12 | `MedicalRecordIdentity` | 未變 |
| GAP-13 | `latestRereviewSubmissionId` | 已修（D-025） |
| GAP-14 | EX-003 與 D-012 矛盾 | 未重查 |
| GAP-15 | D-018 遮罩 | 已實作（D-026、D-033、D-035） |
| GAP-16 | D-021 文件同步 | 未重查 |
| GAP-17 | 測試覆蓋 | 無法判定，見 R-03 |
| GAP-18、19 | 套件風格、詞彙拼法 | 未重查 |
| GAP-20 | `review_case` 欄位 | 未重查 |

