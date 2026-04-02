# Application Service List v2

## 1. 文件目的

本文件定義品質審查系統的 application service 邊界與 use case 清單，作為以下設計與實作的依據：

- command orchestration
- aggregate 載入與協作
- transaction boundary
- 外部整合呼叫時機
- audit / outbox / integration event 發布點

本文件只處理 **application layer 應如何協調 domain objects 與外部資源**。  
不在此文件內重新定義 domain invariant、狀態機、權限矩陣或 query projection；這些應由：

- domain model
- state machine
- permission matrix
- read model / reporting spec

負責。

---

## 2. 設計原則

本系統 application service 遵守以下原則：

1. application service 負責 orchestration，不直接承載核心業務規則。
2. domain rule 必須由 aggregate methods 與 invariant 保護。
3. application service 可協調多個 aggregate，但不得繞過 aggregate 邊界直接改寫內部狀態。
4. revision 不得以覆寫舊 submission 方式實作，必須走新 revision record。
5. `CLOSED` 與 `CANCELLED` 案件不得進行 revision orchestration。
6. revision 不等於 reopen，不構成重新開案流程。
7. actor 權限驗證應在 application service 與 authorization guard 層共同執行。
8. 系統管理員不直接介入院內抽選、分派、審查、複審業務流程。

---

## 3. Application Layer 責任範圍

### 3.1 應由 application service 負責的事

- 載入一個或多個 aggregate
- 開啟與提交 transaction
- 協調 aggregate 方法呼叫順序
- 組裝外部查詢或匯入結果
- 呼叫 HIS 同步查詢或批次匯入 adapter
- 寫 audit log / outbox
- 發布 integration event
- 轉換 command / DTO 與 domain object
- 執行必要的 actor / scope / policy guard

### 3.2 不應由 application service 負責的事

- 直接判定案件是否進複審
- 直接改寫 aggregate 內部欄位跳過 domain method
- 直接覆寫已提交 submission
- 直接修改已發布版本
- 直接把 permission matrix 寫成硬編碼 scattered if-else
- 在 application service 中自行定義狀態機規則取代 domain model / state machine

---

## 4. Use Case 分類

本系統 application service 分為以下幾組：

1. Project Configuration
2. Sampling
3. Assignment and Initial Review
4. Re-review
5. Revision and Case Control
6. Read / Audit Trigger Supporting Services
7. Platform Administration Boundary

---

## 5. Project Configuration Use Cases

這組 use case 負責審查專案、評分表與版本發布。

| Use Case | 輸入 | 主要動作 |
|---|---|---|
| `CreateReviewProjectUseCase` | project draft | 建立審查專案草稿 |
| `UpdateReviewProjectDraftUseCase` | projectId, updated draft | 更新未發布專案草稿 |
| `ActivateReviewProjectUseCase` | projectId | 啟用專案 |
| `DeactivateReviewProjectUseCase` | projectId | 停用專案 |
| `PublishReviewProjectVersionUseCase` | projectId | 發布專案版本 |
| `CreateScoreSheetDraftUseCase` | projectId | 建立評分表草稿 |
| `UpdateScoreSheetDraftUseCase` | scoreSheetId, updated draft | 更新評分表草稿 |
| `PublishScoreSheetVersionUseCase` | scoreSheetId | 發布評分表版本 |
| `BindScoreSheetToProjectUseCase` | projectId, scoreSheetVersionId | 綁定評分表版本至專案 |

### 5.1 orchestration 重點

- 驗證 actor 是否為 `HOSPITAL_MANAGER`
- 載入 `ReviewProject` / `ScoreSheetDefinition`
- 僅允許 draft / 未發布版本被更新
- 發布時建立正式 version 與 snapshot 參照
- 寫入 audit 與 outbox

---

## 6. Sampling Use Cases

這組 use case 負責抽選執行、批次建立與失敗重跑。

| Use Case | 輸入 | 主要動作 |
|---|---|---|
| `ExecuteSamplingUseCase` | projectId, mode | 讀取有效專案版本、查母體、套條件、抽樣、建批次、建案件 |
| `RetrySamplingBatchUseCase` | previousBatchId | 以 retry 模式建立新批次並重跑 |
| `PreviewSamplingPopulationUseCase` | projectId | 預估母體 / 符合 / 排除件數，不正式建批次 |
| `ExportSamplingBatchResultUseCase` | batchId | 匯出抽選結果 |
| `GetSamplingBatchDetailUseCase` | batchId | 查詢批次詳情與執行訊息 |

### 6.1 orchestration 重點

#### `ExecuteSamplingUseCase`
- 驗證 actor 是否為 `HOSPITAL_MANAGER`
- 載入 `ReviewProject`
- 取得 published project version 與 rule snapshot
- 呼叫病歷來源 adapter：
  - 同步查詢模式
  - 批次匯入模式
- 建立 `SamplingBatch`
- 依 `SamplingPolicy` 執行抽樣
- 建立 `ReviewCase`
- 寫 audit / outbox / integration event

#### `RetrySamplingBatchUseCase`
- 不得覆寫舊 batch
- 必須建立新 `SamplingBatch`
- `retryOfBatchId` 必填
- 若前一批次非 `FAILED`，應由 domain / policy 決定是否拒絕

### 6.2 Sampling 特別限制

- application service 不應自行硬寫品質線規則
- application service 不應負責跨批次去重規則定義
- 同批次重複抽選限制由 `SamplingBatch` invariant 保護

---

## 7. Assignment and Initial Review Use Cases

這組 use case 負責案件分派、初審開始、草稿保存與提交。

| Use Case | 輸入 | 主要動作 |
|---|---|---|
| `AssignReviewCaseUseCase` | caseId, reviewerId, dueAt | 指派初審醫師與期限 |
| `ReassignReviewCaseUseCase` | caseId, reviewerId, dueAt | 改派初審醫師 |
| `StartInitialReviewUseCase` | caseId, reviewerId | 驗證指派關係後進入初審中 |
| `SaveInitialReviewDraftUseCase` | caseId, reviewerId, answers, comment, attachments | 保存初審草稿 |
| `SubmitInitialReviewUseCase` | caseId, submissionId | 正式提交初審，交由 domain 決定是否進複審 |
| `ReturnInitialReviewUseCase` | caseId, actorId, reason | 將初審中案件退回 |
| `CancelReviewCaseUseCase` | caseId, actorId, reason | 取消尚未結案案件 |

### 7.1 orchestration 重點

#### `AssignReviewCaseUseCase`
- 驗證 actor 是否為 `HOSPITAL_MANAGER`
- 載入 `ReviewCase`
- 呼叫 `assignReviewer(...)`
- 寫 audit

#### `StartInitialReviewUseCase`
- 驗證 actor 是否為已指派初審醫師
- 載入 `ReviewCase`
- 呼叫 `startInitialReview()`

#### `SaveInitialReviewDraftUseCase`
- 驗證 actor 是否為已指派初審醫師
- 載入 `ReviewCase`
- 載入當前有效 `ScoreSheetSnapshot`
- 建立或更新 `ReviewSubmission` draft
- 不改變案件主狀態，除非 state machine 另有要求

#### `SubmitInitialReviewUseCase`
- 驗證 actor 與 submission 所有權
- 載入 `ReviewCase`
- 載入 `ReviewSubmission`
- 呼叫 `submit()`
- 呼叫 `ReviewCase.completeInitialReview(...)`
- 是否進入複審由 domain 根據品質線決定
- application service 不得自行寫死判斷邏輯

---

## 8. Re-review Use Cases

這組 use case 負責複審指派、複審開始、草稿保存與提交。

| Use Case | 輸入 | 主要動作 |
|---|---|---|
| `AssignRereviewerUseCase` | caseId, rereviewerId, dueAt | 指派複審醫師 |
| `ReassignRereviewerUseCase` | caseId, rereviewerId, dueAt | 改派複審醫師 |
| `StartRereviewUseCase` | caseId, rereviewerId | 驗證指派關係後進入複審中 |
| `SaveRereviewDraftUseCase` | caseId, rereviewerId, answers, comment, attachments | 保存複審草稿 |
| `SubmitRereviewUseCase` | caseId, submissionId | 正式提交複審並結案 |

### 8.1 orchestration 重點

#### `AssignRereviewerUseCase`
- 驗證 actor 是否為 `HOSPITAL_MANAGER`
- 載入 `ReviewCase`
- 呼叫 `assignRereviewer(...)`

#### `StartRereviewUseCase`
- 驗證 actor 是否為已指派複審醫師
- 載入 `ReviewCase`
- 呼叫 `startRereview()`

#### `SaveRereviewDraftUseCase`
- 驗證 actor 是否為已指派複審醫師
- 載入 `ReviewCase`
- 建立或更新 `REREVIEW` stage draft submission

#### `SubmitRereviewUseCase`
- 驗證 actor 與 submission 所有權
- 載入 `ReviewCase`
- 載入 `ReviewSubmission`
- 呼叫 `submit()`
- 呼叫 `ReviewCase.completeRereview(...)`
- 最終分數由複審醫師提交結果決定
- 正常完成後進入 `CLOSED`

---

## 9. Revision and Case Control Use Cases

這組 use case 負責未結案案件的 revision，以及案件控制命令。

本版不再使用語意過寬的 generic revision 描述，改為依審查階段拆開。

| Use Case | 輸入 | 主要動作 |
|---|---|---|
| `ReviseInitialReviewUseCase` | submissionId, reviewerId, revisedAnswers, comment, attachments | 驗證未結案且 reviewer 為原審查醫師後建立新 revision |
| `ReviseRereviewUseCase` | submissionId, rereviewerId, revisedAnswers, comment, attachments | 驗證未結案且 rereviewer 為複審醫師後建立新 revision |
| `GetRevisionHistoryUseCase` | caseId | 查詢 revision 歷程 |
| `GetCaseLifecycleUseCase` | caseId | 查詢案件生命週期與狀態變化 |

### 9.1 `ReviseInitialReviewUseCase`

#### 主要責任
- 驗證 submission stage 為 `INITIAL`
- 驗證案件未 `CLOSED`
- 驗證案件未 `CANCELLED`
- 驗證 actor 為原審查醫師
- 建立新 revision submission
- 將舊版標記為 `SUPERSEDED`
- 不改變案件主狀態

#### 明確禁止
- 不得覆寫既有 submission record
- 不得用 revision 模擬 reopen
- 不得由 `HOSPITAL_MANAGER`、`SYSTEM_ADMIN`、`AUDITOR` 代為發起

---

### 9.2 `ReviseRereviewUseCase`

#### 主要責任
- 驗證 submission stage 為 `REREVIEW`
- 驗證案件未 `CLOSED`
- 驗證案件未 `CANCELLED`
- 驗證 actor 為複審醫師
- 建立新 revision submission
- 將舊版標記為 `SUPERSEDED`
- 不改變案件主狀態

#### 明確禁止
- 不得覆寫既有 submission record
- 不得由非複審醫師發起
- 不得於結案後執行

---

### 9.3 Revision 共通限制

```yaml id="b3g9cs"
revision_orchestration_rules:
  - revision 僅適用未結案且未取消案件
  - revision 必須建立新 submission record
  - revision 不改變案件主狀態
  - revision 不等於 reopen
  - revision actor 必須與審查階段責任人一致
```

---

## 10. Read / Audit Trigger Supporting Services

這組 use case 不改變 domain state，主要提供查詢支援或 read model / audit 觸發協調。

| Use Case                           | 輸入                    | 主要動作                   |
| ---------------------------------- | --------------------- | ---------------------- |
| `GetReviewProjectDetailUseCase`    | projectId             | 查詢專案與版本資訊              |
| `GetReviewCaseDetailUseCase`       | caseId                | 查詢案件詳情                 |
| `GetReviewSubmissionDetailUseCase` | submissionId          | 查詢提交詳情                 |
| `GetAuditTrailUseCase`             | aggregateId           | 查詢操作軌跡與異動歷程            |
| `ExportReviewReportUseCase`        | filter                | 匯出報表                   |
| `GetDashboardSummaryUseCase`       | hospitalScope, filter | 查詢審查完成率 / 複審率 / 逾期率等摘要 |

### 10.1 說明

* 這些 use case 雖屬 application layer，但以 query / export orchestration 為主
* 權限範圍由 permission matrix 決定
* 內容遮罩與 metadata only 規則不得在此忽略

---

## 11. Platform Administration Boundary

這組明確說明 system admin 能做什麼、不能做什麼。

| Use Case                         | SYSTEM_ADMIN 是否可用 | 說明                      |
| -------------------------------- | ----------------: | ----------------------- |
| `ManageAccountsAndRolesUseCase`  |                 Y | 平台帳號 / 角色 / 權限治理        |
| `GetOperationalMetadataUseCase`  |                 Y | 查看執行狀態、批次 metadata、錯誤訊息 |
| `TriggerBusinessSamplingUseCase` |                 N | 不直接執行院內業務抽選             |
| `AssignReviewCaseUseCase`        |                 N | 不直接分派初審 / 複審            |
| `ReviseInitialReviewUseCase`     |                 N | 不可代修                    |
| `ReviseRereviewUseCase`          |                 N | 不可代修                    |
| `ViewFullAnswerContentUseCase`   |                 N | 不可查看完整評分內容              |

### 11.1 原則

SYSTEM_ADMIN 的 application service 邊界只到平台治理與 operational metadata，不進入業務內容操作。

---

## 12. Transaction Boundary 建議

### 12.1 單一交易內適合完成的操作

* 指派 reviewer / rereviewer
* submission draft save
* submission submit + case 狀態更新
* revision 建立 + 舊版標記 `SUPERSEDED`
* batch 建立 + selected case refs 寫入

### 12.2 可用 outbox / 非同步處理的操作

* 通知發送
* 報表彙總更新
* dashboard projection 更新
* analytics / audit downstream sync
* integration event 發布後的跨系統副作用

---

## 13. Integration / Adapter 協作點

application service 可透過下列 adapter 協作：

* `MedicalRecordSourceAdapter`

  * 查詢病歷母體
  * 批次匯入病歷資料

* `NotificationAdapter`

  * 待審通知
  * 複審通知
  * 提醒通知

* `AuditTrailWriter`

  * 記錄操作軌跡
  * 記錄 revision 與版本發布事件

* `OutboxPublisher`

  * 寫入 integration event outbox

### 13.1 原則

* adapter 只做外部整合
* 業務規則仍由 domain 決定
* application service 不得把 adapter 結果直接當作 domain truth 而跳過 aggregate 驗證

---

## 14. 失敗與例外處理原則

### 14.1 應由 application service 拒絕的情境

* actor 與角色不符
* actor 不在合法資料範圍內
* revision 嘗試作用於 `CLOSED` / `CANCELLED` 案件
* revision actor 與原責任醫師不一致
* 嘗試提交不屬於自己的 submission
* 嘗試使用未發布版本執行正式抽選

### 14.2 應由 domain 拒絕的情境

* 非法狀態轉移
* 重複 revisionNo
* 非法 batch 內重複抽選
* 已發布版本被原地修改
* 未分派案件開始初審 / 複審

---

## 15. 不屬於本文件的事項

以下議題不在本文件定義範圍內：

* 哪個角色可以看完整答案
* 哪個 endpoint 對應哪個 HTTP method / path
* score sheet snapshot JSON 結構
* persistence schema 與索引設計
* domain event payload 細節
* UI 頁面流程與按鈕顯示

這些屬於：

* permission matrix
* API spec
* domain model
* domain events
* persistence model
* frontend spec

---

## 16. 後續落地要求

本文件定義的 use case 邊界應同步落地於：

* command handler / service class
* authorization guard
* aggregate invocation sequence
* transaction demarcation
* audit / outbox / integration event 發布點

若與其他文件衝突，應優先檢查：

1. 需求書
2. decision log
3. permission matrix
4. domain model
5. state machine
6. application service
7. API / persistence / UI 實作
