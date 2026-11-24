# `ReviewSubmission` 稽核紀錄的遮罩

狀態：**已裁決**（2026-11-24）。負責人對 V-1～V-5 全部照建議裁決（V-2 選 A），並要求立法前掃過其他種類的 `detail`（結果在第七節）。條文是 `decision-log.md` 的 D-078，以那裡為準。本文件不是律法，下面保留起草時的版本（main = `2fd49e6`）。題號 V-1～V-5 只在本文件使用。

## 一、背景

- D-076 第 3 點：`DRAFT_SAVED`、`SUBMITTED` 的 `detail` 帶 `totalScore`，`entityType` 是 `ReviewSubmission`，不在 D-071 的遮罩範圍。D-076 決定另開一題：先查證兩個查詢出口會不會回傳，再決定要不要擴大遮罩。
- D-076 實作之後（#144），複審也寫入這兩種紀錄，`SUBMITTED` 帶複審的分數。

## 二、查到的事實

**兩個出口**

| 出口 | 會不會回傳 `ReviewSubmission` 的紀錄 | 根據 |
|---|---|---|
| `GetCaseLifecycle` | **不會** | 只查 `entityType = ReviewCase`、`entityId = caseId`（`AuditTrailQueryAdapter.findLifecycleByEntityId`） |
| `ListAuditTrails` | **會** | `aggregateType` 選填，不填就不過濾種類；只以院區、action、時間過濾 |

**證據鏈（`ListAuditTrails`）**
1. `SubmitInitialReviewUseCase`、`SubmitRereviewUseCase` 發 `ReviewSubmissionSubmittedEvent`，帶 `hospitalId`；保存草稿發 `ReviewDraftSavedEvent`，同樣帶 `hospitalId`。
2. `AuditTrailListener` 把所有 `DomainEvent` 寫進 `audit_trail`，包括 `hospitalId`。
3. `ListAuditTrails` 以操作者的院區範圍過濾，所以這些紀錄查得到。
4. `ScoredDetailMask.scored()` 只看 `entityType = ReviewCase` 的五個 action，`ReviewSubmission` 的紀錄直接放行。
5. 持有 `REREVIEWER` 或 `SYSTEM_ADMIN` 的人每一筆都沒有 `detail`（RC-AUD-01），不受影響。**`HOSPITAL_MANAGER` 與不持有 `REREVIEWER` 的 `AUDITOR` 會拿到分數。**

**實測（2026-11-24，暫時的單元測試，跑完已刪除）**：同一頁放三筆紀錄，遮罩在找不到案件時應該 fail-closed。

| 操作者 | `ReviewCase/INITIAL_REVIEW_COMPLETED` | `ReviewSubmission/SUBMITTED` | `ReviewSubmission/DRAFT_SAVED` |
|---|---|---|---|
| `AUDITOR` | `null`（已遮罩） | `{"caseId":"c1","totalScore":85,"reviewerId":"r1"}` | `{"caseId":"c1","totalScore":80}` |
| `HOSPITAL_MANAGER` | `null`（已遮罩） | 同上，沒有遮罩 | 同上，沒有遮罩 |

這是 use case 層級的實測，沒有走 HTTP 與資料庫；第 1～3 步是讀程式確認的。

**`ReviewSubmission` 的三種紀錄**

| action | `detail` | 帶分數嗎 |
|---|---|---|
| `DRAFT_SAVED` | `{"caseId","totalScore"}` | 初審帶草稿分數；複審是 `null`（D-076 第 1 點） |
| `SUBMITTED` | `{"caseId","totalScore","reviewerId"}` | 帶；初審與複審都有 |
| `REVISED` | `{"caseId","oldSubmissionId","stage","revisionNo"}` | 不帶 |

**D-071 當時是不是刻意不納入**
- 草稿 21 第 84 行：「稽核的其他 `entityType`（專案、評分表）不帶初審分數，不受影響」。只列了專案與評分表，沒有提到 `ReviewSubmission`。
- D-071 第 1 點要求「之後新增會帶分數的 action 必須加進這份清單」。
- 判斷是**漏掉**，不是刻意排除。這是從文件推論的，沒有其他紀錄。

**規格與律法**
- `application/audit/` 已立法，對應 `specs/review-case/`。RC-AUD-02 寫的是「某一筆是 **ReviewCase** 帶分數的 action」，規格本身也沒有涵蓋。
- `AuditController.java` 是 `grandfathered`；本題不需要動它。
- 由提交編號找案件：`ReviewSubmissionRepository` 只有單筆的 `findById`，沒有批次查詢。遮罩現在以 `AuditCaseStateQuery.findStates(caseIds)` 一次查一頁的案件。

## 三、V-1：範圍

### 建議

- `ReviewSubmission` 的 `DRAFT_SAVED`、`SUBMITTED` 列為帶分數的 action，加進 D-071 第 1 點的清單。
- 不分初審或複審，一律套用。複審的 `DRAFT_SAVED` 雖然是 `null`，一起套用比較簡單，也不會因為之後改寫而漏掉。
- `REVISED` 不帶分數，不列。

### 待負責人決定

同意嗎？

## 四、V-2：怎麼找到所屬案件

遮罩條件是以案件的狀態與複審指派歷史判斷的，所以要先從紀錄找到案件。

### 選項

- **A（建議）：由提交編號查案件編號。** 新增一個批次查詢（提交編號 → 案件編號），與 `AuditCaseStateQuery` 同樣一次查一頁。查不到就遮罩（D-035 fail-closed）。
- **B：解析 `detail` 裡的 `caseId`。** 不用多查一次，但把遮罩綁在 `detail` 的字串格式上；格式逐字不變（D-066 第 2 點）只保證寫入，不保證拿來當查詢的依據。
- **C：`ReviewSubmission` 的紀錄一律遮罩分數，不看案件。** 最簡單，但比 `ReviewCase` 的紀錄更嚴：結案後 `ReviewCase` 的分數看得到，提交紀錄的分數卻看不到，兩邊不一致。

### 待負責人決定

A／B／C？

## 五、V-3：遮罩條件

### 建議

- 與 RC-LCY-02 相同，以所屬案件判斷：案件還可能再指派複審醫師、或讀者是現任或曾任的複審醫師、或指派歷史不完整，就遮罩。三個條件都不成立時照常輸出。
- 初審分數與複審分數都照這個條件。這與 `ReviewCase` 的 `REREVIEW_SUBMITTED`（帶 `rereviewScore`）已列在清單裡一致。
- 遮罩的方式與現在相同：整個 `detail` 變成 `null`，其他欄位照常。

### 待負責人決定

同意嗎？

## 六、V-4：生命週期

### 建議

`GetCaseLifecycle` 維持只回傳 `ReviewCase` 的紀錄，不在本題。要不要把提交紀錄也列進案件的生命週期，是另一個需求問題。

### 待負責人決定

同意嗎？

## 七、V-5：不在本題的

### 建議

- `AuditController` 的立法（`grandfathered`）。
- 稽核的其他 `entityType`：不帶分數，不遮罩。2026-11-24 立法前全部掃過（見下表）。

**掃描結果**：稽核紀錄只由 `AuditTrailListener` 寫入（把每個 `DomainEvent` 原樣寫入），沒有其他直接寫入的地方。所有事件類別的 `detail`：

| `entityType` | action | `detail` 的鍵 | 帶分數 |
|---|---|---|---|
| `ReviewProject` | `CREATED`、`DRAFT_UPDATED`、`ACTIVATED`、`DEACTIVATED`、`VERSION_PUBLISHED` | 沒有 `detail`（`null`） | 否 |
| `ReviewProject` | `SCORE_SHEET_BOUND` | `scoreSheetId` | 否 |
| `ScoreSheetDefinition` | `DRAFT_CREATED`、`DRAFT_UPDATED`、`VERSION_PUBLISHED` | `projectId` | 否 |
| `SamplingBatch` | `STARTED` | `projectId`、`executionMode` | 否 |
| `SamplingBatch` | `CANDIDATES_PREPARED` | `populationCount`、`eligibleCount`、`excludedCount` | 否（件數） |
| `SamplingBatch` | `CASES_SELECTED` | `selectedCount` | 否（件數） |
| `SamplingBatch` | `COMPLETED` | `projectId`、`selectedCount`、`populationCount`、`eligibleCount`、`excludedCount` | 否（件數） |
| `SamplingBatch` | `FAILED` | `failureReason` | 否 |
| `ReviewCase` | 五個帶分數的 action（D-071 第 1 點） | — | 是，已遮罩 |
| `ReviewCase` | 其他 action（`ASSIGNED`、`REASSIGNED`、`INITIAL_REVIEW_STARTED`、`INITIAL_REVIEW_FIXED`、`REREVIEWER_ASSIGNED`、`REREVIEW_STARTED`、`CASE_CLOSED_AFTER_REREVIEW`、`CANCELLED`、`RETURNED`） | 人員、提交編號、`dueAt`、`reason` | 否 |
| `ReviewSubmission` | `DRAFT_SAVED`、`SUBMITTED` | `caseId`、`totalScore`（`reviewerId`） | **是，本題** |
| `ReviewSubmission` | `REVISED` | `caseId`、`oldSubmissionId`、`stage`、`revisionNo` | 否 |

唯一的保留：`CANCELLED`、`RETURNED` 的 `reason` 是使用者輸入的自由文字，程式擋不住有人把分數寫進去。這不屬於遮罩的範圍。
- 既有的資料：遮罩是在讀取時套用，不改已寫入的紀錄；正式資料庫還沒有資料（D-045）。

### 待負責人決定

同意嗎？

## 八、裁決後的順序

1. **修法 PR**：
   - `decision-log.md` 新增 D-078（擴大 D-071 第 1 點的清單）；
   - `specs/review-case/usecases.yaml`：RC-AUD-02 的條件改寫（或新增一條規則）；`examples.yaml` 新增範例（`AUDITOR`、`HOSPITAL_MANAGER` 讀到 `SUBMITTED`、`DRAFT_SAVED` 時遮罩，以及結案後照常輸出）；
   - 一開始是紅的範例加進 `known-test-failures.txt`（D-066 第 5 點）；
   - 本草稿標為已裁決；題目清單（本題進行中、D-077 移到已完成）、README 索引。
2. **建構 PR**：`ScoredDetailMask` 與提交編號 → 案件編號的查詢；移除 `known-test-failures.txt` 的項目。
