# Domain Event Catalog v2

## 1. 文件目的

本文件定義品質審查系統中的 domain event catalog，作為以下設計與實作的依據：

- aggregate 完成某業務事實後應發布哪些 domain event
- event 與狀態轉移之間的關係
- audit / outbox / integration event 轉換的上游來源
- read model projection 的業務語意基礎

本文件只處理 **domain event**。  
不處理以下事項：

- notification event
- analytics event
- export event
- UI event
- integration event payload contract

若某事件的核心意義是「通知別的系統」而非「domain 事實成立」，則不應列入本文件。

---

## 2. 設計原則

本系統 domain event 遵守以下原則：

1. domain event 描述的是「業務事實已成立」。
2. domain event 應由 aggregate 狀態改變或業務決策成立後發布。
3. 非法狀態轉移不得發布 domain event。
4. revision event 只會發生於未固定的合法提交結果，且案件未結案、未取消（D-031／032）。
5. `ReviewCaseClosed` 發生後，不應再有新的 `ReviewSubmissionRevised` event。
6. domain event 不等於 integration event；是否對外通知，由 application layer 或 outbox 決定。
7. event catalog 應與 domain model、state machine、一致。

---

## 3. Domain Event 分類總覽

本系統 domain event 分為五類：

1. Project and Version Events
2. Sampling Events
3. Review Case Lifecycle Events
4. Submission Events
5. Revision Events

---

## 4. Project and Version Events

---

## 4.1 `ReviewProjectCreated`

### Emitted By
- `ReviewProject`

### 業務意義
建立新的審查專案草稿或初始專案實體。

### 發布條件
- `ReviewProject.create(...)` 成功完成
- `projectId` 已成立

### 最低必要欄位
```json
{
  "eventName": "ReviewProjectCreated",
  "projectId": "RP-001",
  "hospitalId": "H-01",
  "occurredAt": "2026-04-02T10:00:00+08:00"
}
```

---

## 4.2 `ReviewProjectActivated`

### Emitted By

* `ReviewProject`

### 業務意義

審查專案進入可供正式抽選使用的啟用狀態。

### 發布條件

* `activate()` 成功
* 專案狀態由非啟用轉為啟用

---

## 4.3 `ReviewProjectDeactivated`

### Emitted By

* `ReviewProject`

### 業務意義

審查專案停用，不再供新批次使用。

### 發布條件

* `deactivate()` 成功

---

## 4.4 `ReviewProjectVersionPublished`

### Emitted By

* `ReviewProject`

### 業務意義

某個正式 project version 已發布，可供新批次採用。

### 發布條件

* `publishNewVersion(...)` 成功
* 已建立正式 version reference 與 snapshot 關聯

### 補充

* 此事件不代表既有批次 / 案件重新套用新版
* 舊批次與舊案件仍維持原 version / snapshot 參照

---

## 4.5 `ScoreSheetDraftCreated`

### Emitted By

* `ScoreSheetDefinition`

### 業務意義

建立新的評分表草稿。

### 發布條件

* `createDraft(...)` 成功

---

## 4.5a `ScoreSheetDraftUpdated`

### Emitted By

* `ScoreSheetDefinition`

### 業務意義

評分表草稿的章節或題目已更新（D-050 第 5 點）。

### 發布條件

* 六相位的更新命令成功

---

## 4.6 `ScoreSheetVersionPublished`

### Emitted By

* `ScoreSheetDefinition`

### 業務意義

某個正式評分表版本已發布，可供新專案版本或新案件採用。

### 發布條件

* `publishVersion()` 成功

---

## 4.7 `ScoreSheetFrozen`

### Emitted By

* `ScoreSheetDefinition`

### 業務意義

評分表版本被凍結，不再允許修改。

### 發布條件

* `freezeVersion()` 成功

---

## 5. Sampling Events

---

## 5.1 `SamplingBatchStarted`

### Emitted By

* `SamplingBatch`

### 業務意義

一次抽選批次正式開始執行。

### 發布條件

* `SamplingBatch.start(...)` 成功
* `batchId` 已成立

### 最低必要欄位

```json
{
  "eventName": "SamplingBatchStarted",
  "batchId": "BATCH-001",
  "projectId": "RP-001",
  "executionMode": "SCHEDULED",
  "occurredAt": "2026-04-02T02:00:00+08:00"
}
```

---

## 5.2 `SamplingCandidatesPrepared`

### Emitted By

* `SamplingBatch`

### 業務意義

抽選母體與符合件數已完成蒐集與統計，可進入抽樣階段。

### 發布條件

* 候選母體已建立
* `populationCount` / `eligibleCount` / `excludedCount` 已可確定

### 最低必要欄位（D-067）

```json
{
  "eventName": "SamplingCandidatesPrepared",
  "batchId": "BATCH-001",
  "populationCount": 1200,
  "eligibleCount": 300,
  "excludedCount": 40,
  "occurredAt": "2026-04-02T02:10:00+08:00"
}
```

---

## 5.3 `ReviewCasesSelected`

### Emitted By

* `SamplingBatch`

### 業務意義

抽樣完成，已產生抽中案件參照集合。

### 發布條件

* `selectCases(...)` 成功
* 同批次內未違反 duplicate sourceCaseKey invariant

### 補充

* 本事件代表「被選中」
* 不等於每個 `ReviewCase` 實體都已完成建立
* 實際 case entity 建立後，仍可再發 `ReviewCaseCreated`

### 最低必要欄位（D-067）

```json
{
  "eventName": "ReviewCasesSelected",
  "batchId": "BATCH-001",
  "selectedCount": 30,
  "occurredAt": "2026-04-02T02:10:00+08:00"
}
```

---

## 5.4 `SamplingBatchCompleted`

### Emitted By

* `SamplingBatch`

### 業務意義

一次抽選批次完成，統計、規則快照與 selected case refs 已固定。

### 發布條件

* `complete(...)` 成功
* 批次進入 `COMPLETED`

### 最低必要欄位

```json
{
  "eventName": "SamplingBatchCompleted",
  "batchId": "BATCH-001",
  "projectId": "RP-001",
  "populationCount": 1200,
  "eligibleCount": 300,
  "excludedCount": 40,
  "selectedCount": 30,
  "occurredAt": "2026-04-02T02:20:00+08:00"
}
```

---

## 5.5 `SamplingBatchFailed`

### Emitted By

* `SamplingBatch`

### 業務意義

抽選批次執行失敗，需保留失敗原因與後續 retry 線索。

### 發布條件

* `fail(reason)` 成功
* 批次進入 `FAILED`

---

## 6. Review Case Lifecycle Events

---

## 6.1 `ReviewCaseCreated`

### Emitted By

* `ReviewCase`

### 業務意義

某一抽中案件已正式建立為可追蹤的審查案件。

### 發布條件

* `createFromSelection(...)` 成功
* `caseId` 已成立

---

## 6.2 `ReviewCaseAssigned`

### Emitted By

* `ReviewCase`

### 業務意義

案件完成初審醫師指派。

### 發布條件

* `assignReviewer(...)` 成功

### 最低必要欄位

```json
{
  "eventName": "ReviewCaseAssigned",
  "caseId": "RC-001",
  "assignedReviewerId": "DOC-1001",
  "dueAt": "2026-04-10T23:59:59+08:00",
  "occurredAt": "2026-04-02T10:30:00+08:00"
}
```

---

## 6.3 `InitialReviewStarted`

### Emitted By

* `ReviewCase`

### 業務意義

案件已正式進入初審中。

### 發布條件

* `startInitialReview()` 成功
* 狀態轉為 `INITIAL_REVIEW_IN_PROGRESS`

### 最低必要欄位（D-067）

```json
{
  "eventName": "InitialReviewStarted",
  "caseId": "RC-001",
  "submissionId": "SUB-INIT-001",
  "reviewerId": "DOC-123",
  "occurredAt": "2026-04-02T02:10:00+08:00"
}
```

---

## 6.4 `InitialReviewCompleted`

### Emitted By

* `ReviewCase`

### 業務意義

初審提交已完成，案件可進行品質線判定。

### 發布條件

* `completeInitialReview(...)` 成功
* 初審 submission 為 `SUBMITTED`
* 狀態轉為 `INITIAL_REVIEW_COMPLETED`；提交不等於固定，事件不代表可立即進複審或結案

### 最低必要欄位

```json
{
  "eventName": "InitialReviewCompleted",
  "caseId": "RC-20260402-0001",
  "projectId": "RP-001",
  "submissionId": "SUB-INIT-001",
  "reviewerId": "DOC-123",
  "totalScore": 88.5,
  "qualityThreshold": 80,
  "occurredAt": "2026-04-02T14:10:00+08:00"
}
```

---

## 6.5 `ReviewCaseEscalatedToRereview`

### Emitted By

* `ReviewCase`

### 業務意義

案件因初審超過品質線而進入複審流程。

### 發布條件

* `InitialReviewCompleted` 後
* 初審結果已固定且 `totalScore > qualityThreshold`
* 狀態轉為 `PENDING_REREVIEW`

### 最低必要欄位

```json
{
  "eventName": "ReviewCaseEscalatedToRereview",
  "caseId": "RC-20260402-0001",
  "projectId": "RP-001",
  "triggeredBySubmissionId": "SUB-INIT-001",
  "initialScore": 88.5,
  "qualityThreshold": 80,
  "occurredAt": "2026-04-02T14:10:01+08:00"
}
```

---

## 6.6 `RereviewerAssigned`

### Emitted By

* `ReviewCase`

### 業務意義

案件完成複審醫師指派。

### 發布條件

* `assignRereviewer(...)` 成功

---

## 6.7 `RereviewStarted`

### Emitted By

* `ReviewCase`

### 業務意義

案件已正式進入複審中。

### 發布條件

* `startRereview()` 成功
* 狀態轉為 `REREVIEW_IN_PROGRESS`

---

## 6.8 `RereviewCompleted`

### Emitted By

* `ReviewCase`

### 業務意義

複審提交完成，系統按 D-012／020 計算加權分數；此事實不代表固定或正式結案（D-032）。

### 發布條件

* `completeRereview(...)` 成功
* 複審 submission 為 `SUBMITTED`

---

## 6.9 `ReviewCaseClosed`

### Emitted By

* `ReviewCase`

### 業務意義

案件正式結案。

### 發布條件

* 無需複審時，初審已固定，由院方管理者正式結案；或
* 複審合法提交且無既有阻擋條件，由目前有效合法複審結果的提交醫師另行執行 Close Command，同時固定尚未固定的複審結果（D-032／034）
* `completeRereview(...)` 本身不再觸發結案事件
* 狀態轉為 `CLOSED`

### 補充

* `ReviewCaseClosed` 發布後，不應再有新的 `ReviewSubmissionRevised` event
* `CLOSED` 後不得 reopen
* `CLOSED` 後不得 revision

---

## 6.10 `ReviewCaseReturned`

### Emitted By

* `ReviewCase`

### 業務意義

案件於初審中被退回。

### 發布條件

* `returnToInitialReview(reason)` 成功
* 狀態轉為 `RETURNED`

---

## 6.11 `ReviewCaseCancelled`

### Emitted By

* `ReviewCase`

### 業務意義

案件被取消，不再進入審查流程。

### 發布條件

* `cancel(reason)` 成功
* 狀態轉為 `CANCELLED`

### 補充

* `CANCELLED` 後不得再審
* `CANCELLED` 後不得 revision

---

## 7. Submission Events

---

## 7.1 `ReviewDraftSaved`

### Emitted By

* `ReviewSubmission`

### 業務意義

某一審查階段草稿已保存。

### 發布條件

* `saveDraft(...)` 成功

### 補充

* draft saved 不代表案件主狀態一定改變
* 是否發布此事件，可視系統是否需要草稿稽核追蹤決定

### 最低必要欄位（D-067）

```json
{
  "eventName": "ReviewDraftSaved",
  "submissionId": "SUB-INIT-001",
  "caseId": "RC-001",
  "occurredAt": "2026-04-02T02:10:00+08:00"
}
```

草稿分數不列為必要欄位（D-067 第 6 點）。

---

## 7.2 `ReviewSubmissionSubmitted`

### Emitted By

* `ReviewSubmission`

### 業務意義

某一審查階段的提交已正式成立。

### 發布條件

* `submit()` 成功
* submission 狀態轉為 `SUBMITTED`

### 最低必要欄位

```json
{
  "eventName": "ReviewSubmissionSubmitted",
  "submissionId": "SUB-INIT-001",
  "caseId": "RC-001",
  "stage": "INITIAL",
  "reviewerId": "DOC-123",
  "totalScore": 88.5,
  "occurredAt": "2026-04-02T14:10:00+08:00"
}
```

---

## 7.3 `ReviewAttachmentAdded`

### Emitted By

* `ReviewSubmission`

### 業務意義

提交新增附件關聯。

### 發布條件

* `attachFile(...)` 成功

### 補充

* 此事件描述的是 attachment association 完成
* 不等於檔案內容傳輸事件

---

## 8. Revision Events

---

## 8.1 `ReviewSubmissionRevised`

### Emitted By

* `ReviewSubmission`

### 業務意義

已提交審查結果被合法修正，並建立新 revision。

### 發布條件

* 原 submission 必須已為 `SUBMITTED`
* 目前有效結果不得已固定（D-031／032）
* 關聯案件不得為 `CLOSED`
* 關聯案件不得為 `CANCELLED`
* 新 revision submission 建立成功
* 舊版 submission 標記為 `SUPERSEDED`

### 補充約束

* `INITIAL` stage revision 僅可由原審查醫師觸發
* `REREVIEW` stage revision 僅可由複審醫師觸發
* revision 不改變案件主狀態
* revision 不等於 reopen
* `ReviewCaseClosed` 發布後，不應再出現新的 `ReviewSubmissionRevised`

### 最低必要欄位

```json
{
  "eventName": "ReviewSubmissionRevised",
  "oldSubmissionId": "SUB-INIT-001",
  "newSubmissionId": "SUB-INIT-002",
  "caseId": "RC-001",
  "stage": "INITIAL",
  "revisionNo": 2,
  "triggeredBy": "DOC-123",
  "occurredAt": "2026-04-02T15:00:00+08:00"
}
```

---

## 9. 事件關聯與典型流程

---

## 9.1 初審固定且無需複審時的 manager 結案流程

```text
ReviewCaseCreated -> ReviewCaseAssigned -> InitialReviewStarted
-> ReviewSubmissionSubmitted -> InitialReviewCompleted
-> 初審固定業務事實（事件技術名稱 unknown）
-> 依固定結果判定無需複審 -> manager 正式結案 -> ReviewCaseClosed
```
初審分數 ≤ threshold 或 threshold 為 null 不強制複審；manager 仍可在固定後手工派複審（D-019／031）。初審提交與固定均不自動結案。

## 9.2 初審進入複審流程

```text
ReviewSubmissionSubmitted(INITIAL) -> InitialReviewCompleted
-> 初審固定業務事實（事件技術名稱 unknown）
-> ReviewCaseEscalatedToRereview（固定初審分數 > threshold，自動）
-> RereviewerAssigned -> RereviewStarted
-> ReviewSubmissionSubmitted(REREVIEW) -> RereviewCompleted
-> 可選：ReviewSubmissionRevised（複審未固定／未結案）
-> 原複審提交者正式結案，同時固定複審結果 -> ReviewCaseClosed
```
手工派複審同樣需要固定初審；現有 `ReviewCaseEscalatedToRereview` 的閾值事件不得偽裝為手工派發。固定後才可自動推進，不得在初審提交時提前發布升級事件。
複審提交後不立即發出 ReviewCaseClosed；資格不是事件已結案。

## 9.3 未結案 revision 流程

```text
ReviewSubmissionSubmitted
-> ReviewSubmissionRevised
```

### 補充

* 這裡描述的是 submission revision 的業務事實
* 不代表案件主狀態改變
* 不代表 reopen

---

## 10. Domain Event 與 Integration Event 的邊界

本文件只列 domain event。
以下不應直接列為 domain event：

* `ReviewerNotificationRequested`
* `RereviewReminderRequested`
* `BatchCompletedForAnalytics`
* `AuditExportRequested`
* `DashboardProjectionRefreshRequested`

這些若需要，應由：

* application service
* event handler
* outbox publisher

根據 domain event 再轉出。

### 例子

* `InitialReviewCompleted`

  * 是 domain event

* `RereviewNotificationRequested`

  * 是 integration / notification event
  * 不應直接列在本文件

---

## 11. 發布與處理原則

### 11.1 發布原則

* 僅當業務事實成立時發布
* 非法狀態轉移不得發布
* 若 transaction rollback，則對應 event 不應視為已成立

### 11.2 處理原則

* domain event 處理器不得破壞 aggregate invariant
* 對外副作用建議經由 outbox / integration event 執行
* read model 更新可基於 domain event 進行，但不應反向決定 domain truth

---

## 12. 不屬於本文件的事項

以下議題不在本文件定義範圍內：

* 哪個角色可以觸發哪個 command
* 哪個 API endpoint 發布哪些事件
* event bus / message broker 技術選型
* integration event payload schema
* analytics / dashboard projection schema
* attachment storage format

這些屬於：

* permission matrix
* application service
* API spec
* persistence / outbox design
* technical architecture

---

## 13. 後續落地要求

本文件定義的 domain event 應同步落地於：

* aggregate method 完成後的 event 收集
* transaction commit 後的 outbox 寫入
* audit trail 標記
* read model projection 上游來源

若與其他文件衝突，應優先檢查：

1. 需求書
2. decision log
3. domain model
4. state machine
5. application service
6. domain events
7. API / persistence / integration 實作

## 14. 固定事實與事件一致性（D-031／032／033）

初審提交、初審固定、複審提交、正式結案為不同業務事實。固定需可追溯，但本輪不新增事件名稱／payload；固定事實的事件技術表示為 unknown。
ReviewCaseClosed 僅在成功轉 CLOSED 且複審路徑已同時固定結果後成立；任何非法提交、固定後 revision、未具資格或 actor 不符的結案均不得產生成功事件。CLOSED 之後不得出現合法 ReviewSubmissionRevised。
事件使用 domain 正確分數，不改 D-012／020；事件衍生的使用者可見 audit／projection 仍須依 D-033 防止洩漏初審與可反推分數。固定與結案的交易、事件發布技術及相容性細節 unknown，不在本輪設計。

### DD-03 固定追溯

固定必須可追溯操作者、時間及當時有效 submission；事件技術名稱／payload／發布機制仍為 UNKNOWN。不得將固定追溯要求誤寫為已存在的新事件。拒絕及 rollback 不產生成功固定或後續流向的成功事件；可見 projection 仍遵守 D-033。


## D-034 複審獨立 Close Command 對齊

複審提交只形成提交／完成的業務事實，不發布 ReviewCaseClosed；由有效結果合法提交醫師的獨立 Close Command 成功後才形成結案事實。未提交、過期目標、非法 actor／scope、terminal、競爭落敗或 rollback 均不得留下新的成功結案／固定事件。技術事件名稱／payload 擴充、交易發布機制及重試去重方式 UNKNOWN；不新增已定案技術事件。audit／歷程出口仍須套用 D-033。
