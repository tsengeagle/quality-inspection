# 直向切片的後續四題：草稿的狀態錯誤、複審的指派錯誤、批次完成事件的計數、未發布版本的抽選

狀態：**已裁決**（2026-11-22）。負責人對四題全部照建議裁決，條文是 `decision-log.md` 的 D-068，以那裡為準。本文件不是律法，下面保留起草時的版本。

## 一、背景

- D-066、D-067 立法了抽選到初審提交的直向切片，範例全部是綠的，`known-test-failures.txt` 是空的。
- D-067 第 1 點明列兩件事不在該裁決（F-1、F-2）。另外兩件是建構 PR 時順帶查到的（F-3、F-4）。
- 本文件逐題列出程式現況（2026-11-22，main = `e14d6aa`）、權威文件、選項與建議。題號 F-1～F-4 只在本文件使用。

## 二、總覽

| 題 | 問題 | 範圍 | 建議 | 要改哪裡 |
|---|---|---|---|---|
| F-1 | 保存初審草稿時案件不是初審中 | 已立法（`SaveInitialReviewDraftUseCase`） | `CASE_NOT_IN_INITIAL_REVIEW_STAGE`（422），新增規則與範例 | 規格；建構 PR 改程式 |
| F-2 | 複審的 `NOT_ASSIGNED_REREVIEWER` 是 422，初審的 `NOT_ASSIGNED_REVIEWER` 是 403 | 暫准（複審的四處） | 改成 403，與初審一致 | `api-spec.md`；建構 PR 改程式 |
| F-3 | `SamplingBatchCompleted` 缺 `eligibleCount`、`excludedCount` | 已立法（事件類別） | 照 `domain-events.md` §5.4 補上，`detail` 一併多兩個鍵 | 規格；建構 PR 改程式 |
| F-4 | 專案沒有已發布的版本時執行抽選 | 已立法（`ExecuteSamplingUseCase`） | 現在走不到；不立規則，等「啟用中專案準備新版本」立法時一起決定 | 只記在裁決 |

## 三、F-1：保存初審草稿時案件不是初審中

### 查到的事實

| 來源 | 內容 |
|---|---|
| 程式（`SaveInitialReviewDraftUseCase:70`） | 終態檢查之後，status 不是 `INITIAL_REVIEW_IN_PROGRESS` 時丟 `DomainStateConflictException("CASE_NOT_IN_PROGRESS")`（409） |
| 規格（`specs/review-case/usecases.yaml` 的 `SaveInitialReviewDraft`） | 只有 `RC-DRF-01`（不是草稿的醫師）、`RC-DRF-02`（沒有變更），沒有狀態規則 |
| `api-spec.md` §4.2 | 沒有 `CASE_NOT_IN_PROGRESS`。`CASE_NOT_IN_INITIAL_REVIEW_STAGE`（422）的說明是「案件不在該初審操作要求的狀態（開始初審、提交初審；D-067）」 |
| D-067 第 1 點 | 提交初審的同一種錯誤已改成 `CASE_NOT_IN_INITIAL_REVIEW_STAGE`（422），理由是狀態類代碼一律 422、開始與提交共用 |
| 其他用到 `CASE_NOT_IN_PROGRESS` 的地方 | `ReviewCase.returnToInitialReview`（退回）。退回不在切片內 |
| 前端 | 沒有依 `CASE_NOT_IN_PROGRESS` 或 409 處理草稿保存失敗（只認 409 的 `NO_CHANGES`，`cases.service.ts:799`） |

### 選項

| 選項 | 做法 | 優點 | 缺點 |
|---|---|---|---|
| **A. 沿用 `CASE_NOT_IN_INITIAL_REVIEW_STAGE`（422）**（建議） | 新增 `RC-DRF-03` 與範例；`api-spec.md` 的說明加上「保存初審草稿」；建構 PR 改程式的代碼與例外類別 | 初審的三個操作同一個代碼、同一個狀態碼；D-067 的理由同樣適用 | 要改已立法的 use case（一行） |
| B. 照程式 `CASE_NOT_IN_PROGRESS`（409） | 新增規則，代碼補進 `api-spec.md` | 不改程式 | 初審三個操作中，只有草稿回 409；與 D-067 的理由相反 |

### 建議：A

- 規則：`RC-DRF-03`，「案件的 status 不是 `INITIAL_REVIEW_IN_PROGRESS`」→ `CASE_NOT_IN_INITIAL_REVIEW_STAGE`。
- 範例：已指派、還沒開始初審的案件保存草稿。修法 PR 時是紅的，列進已知失敗清單；建構 PR 改完後刪除。
- 退回（`returnToInitialReview`）的 `CASE_NOT_IN_PROGRESS` 不動，等退回立法時處理。

### 待負責人決定

- **F-1**：採 A？

## 四、F-2：複審的 `NOT_ASSIGNED_REREVIEWER` 是 422

### 查到的事實

| 位置 | 程式 | 檢查在哪一層 |
|---|---|---|
| 開始複審 | `ReviewCase.startRereview` 丟 `DomainRuleViolationException("NOT_ASSIGNED_REREVIEWER")`（422） | domain；`StartRereviewUseCase` 沒有自己檢查 |
| 保存複審草稿 | `SaveRereviewDraftUseCase:56`，同上（422） | application |
| 提交複審 | `SubmitRereviewUseCase:78`，同上（422） | application |
| 修正已提交的複審 | `ReviseRereviewUseCase:81`，同上（422） | application |
| 開始初審（對照） | `StartInitialReviewUseCase:69` 丟 `AccessDeniedException("NOT_ASSIGNED_REVIEWER")`（403），在呼叫 domain 之前；domain 只檢查狀態 | application |

| 權威文件 | 內容 |
|---|---|
| D-067 第 1 點 | 初審：`NOT_ASSIGNED_REVIEWER`，分類 `denied`（403） |
| `permission-matrix.md` §6、§10 | 開始複審與提交複審都要求「actor = assigned rereviewer」，屬於權限條件 |
| `test-scenarios.md` TS-RR-004 | 非複審醫師提交複審：期望 `403` |
| `test-scenarios.md` TS-RV-004（非複審醫師修正複審） | 期望 `403 REVISION_ACTOR_MISMATCH` |
| `api-spec.md` §4.2 | 沒有 `NOT_ASSIGNED_REREVIEWER`；有 `REVISION_ACTOR_MISMATCH`（403，「revision 發起者不是合法責任人」） |

- 範圍：四處都在 `application/reviewcase/`、`domain/reviewcase/` 的暫准範圍，複審沒有規格。
- `AccessDeniedException` 在 `application/common/`，domain 不能用。開始複審要像開始初審一樣，在 use case 呼叫 domain 之前檢查。
- 前端沒有依這個代碼或 422 處理錯誤。案件詳情的 `blockedReason` 也叫 `NOT_ASSIGNED_REREVIEWER`（`GetReviewCaseDetailUseCase:214`），那是字串，不是例外，不受影響。
- 測試：`UpdatedAtContractIntegrationTest:344` 期望這個代碼；`ReviseRereviewUseCaseTest:188` 期望 `DomainRuleViolationException`。建構 PR 要跟著改。

### 選項

| 選項 | 做法 | 優點 | 缺點 |
|---|---|---|---|
| **A. 現在改成 403**（建議） | `api-spec.md` §4.2 補 `NOT_ASSIGNED_REREVIEWER`（403）；建構 PR：三處 application 改丟 `AccessDeniedException`，`StartRereviewUseCase` 在呼叫 domain 之前加同樣的檢查（domain 的檢查保留，當作防線） | 與 D-067、`permission-matrix.md`、TS-RR-004 一致；改動小，前端不受影響 | 在暫准範圍改程式，沒有規格範例保護，只靠單元測試 |
| B. 留到複審立法 | 只在裁決記下方向 | 現在不動暫准範圍 | 已知的不一致繼續存在到下一個切片 |

### 建議：A

- 修正已提交的複審要不要改用 `REVISION_ACTOR_MISMATCH`（`test-scenarios.md` 的期望）不在本題。本題只改狀態碼，代碼名稱等複審立法時一起決定。

### 待負責人決定

- **F-2**：採 A？

## 五、F-3：`SamplingBatchCompleted` 缺 `eligibleCount`、`excludedCount`

### 查到的事實

| 來源 | 內容 |
|---|---|
| `domain-events.md` §5.4 最低必要欄位 | `batchId`、`projectId`、`populationCount`、`eligibleCount`、`excludedCount`、`selectedCount` |
| 程式（`SamplingBatchCompletedEvent`） | 建構式只收 `selectedCount`、`populationCount`；欄位只有 `projectId`；`detail` 是 `{"projectId","selectedCount","populationCount"}` |
| 發出者 | `ExecuteSamplingUseCase:117`、`RetrySamplingBatchUseCase:118`。兩處手上都有 `eligibleCount`、`excludedCount`（剛傳給 `SamplingCandidatesPrepared`） |
| 規格 | `SB-EXE-S3` 只斷言 `batchId`、`projectId` |
| `detail` 的讀者 | 只有稽核：`AuditTrailListener` 原樣存、`AuditTrailQueryAdapter` 原樣回。沒有程式解析它。前端的計數取自批次本身，不取自稽核 |
| D-066 第 2 點 | 當時的建構 PR 要求 `detail` 逐字不變，並由 `SliceEventPropertiesTest` 確認 |

### 選項

| 選項 | 做法 | 優點 | 缺點 |
|---|---|---|---|
| **A. 補欄位，`detail` 多兩個鍵**（建議） | 建構式加兩個參數，加取得方法；`detail` 由欄位產生，改成六個值都帶；`SB-EXE-S3` 之外新增規則斷言計數 | 與 `domain-events.md` §5.4 一致；`detail` 仍然只有一個來源；稽核紀錄多了完整的統計 | 新的稽核紀錄格式與舊的不同（舊紀錄不動）；要明文例外於 D-066 第 2 點的「逐字不變」 |
| B. 補欄位與取得方法，`detail` 不變 | 同 A，但 `detail` 只帶原來三個值 | 稽核紀錄完全不變 | 事件與稽核的內容不一致；`detail` 不再只由欄位產生 |
| C. 不補 | 改 `domain-events.md`，把兩個計數從 §5.4 刪掉 | 不改程式 | 改權威文件去配合程式；完成事件少了批次的統計 |

### 建議：A

- 規則：`SB-EXE-S5`，「`SamplingBatchCompleted` 帶有 `populationCount`、`eligibleCount`、`excludedCount`、`selectedCount`」。範例沿用 `intermediate_events_carry_the_batch` 的資料（2／2／0、抽中 2），修法 PR 時是紅的，列進已知失敗清單。
- `detail` 的新格式：`{"projectId":…,"selectedCount":…,"populationCount":…,"eligibleCount":…,"excludedCount":…}`，原有三個鍵的順序不變，新的兩個接在後面。`SliceEventPropertiesTest` 的期望值跟著改。

### 待負責人決定

- **F-3**：採 A？

## 六、F-4：專案沒有已發布的版本時執行抽選

### 查到的事實

| 來源 | 內容 |
|---|---|
| 程式（`ExecuteSamplingUseCase:65`） | 在 `PROJECT_NOT_ACTIVE` 之後，目前的版本沒有發布時丟 `DomainStateConflictException("INVALID_STATE_TRANSITION")`（409） |
| `api-spec.md` §9.3 業務限制 | 「專案需為 active」「採用 published version」；檢查表只列到 `PROJECT_NOT_ACTIVE` |
| `ReviewProject.activate` | 目前的版本沒有發布時不能啟用（`INVALID_STATE_TRANSITION`） |
| `ReviewProject.prepareDraftVersion` | 唯一能讓已發布的專案再出現未發布版本的方法。**沒有任何呼叫者**，也不在 `specs/review-project/` |
| 執行器 | 範例只能經由 use case 建立狀態，做不出「啟用中、但目前版本未發布」的專案 |

- 所以這個檢查現在走不到。它真正要回答的問題是：啟用中的專案準備新版本草稿之後，抽選要拒絕，還是用最後一個已發布的版本（`api-spec.md` 寫的是「採用 published version」，比較像後者）。這是專案版本管理的問題，不是抽選的錯誤代碼問題。

### 選項

| 選項 | 做法 | 優點 | 缺點 |
|---|---|---|---|
| **A. 現在不立規則**（建議） | 裁決記下「走不到，程式的檢查保留為防線」；等 `prepareDraftVersion` 立法時一起決定 | 不為走不到的情況立法；不搶先回答版本管理的問題 | 程式有一個沒有規格的分支 |
| B. 立規則，照程式 | 新增 `SB-EXE-02` → `INVALID_STATE_TRANSITION`（409） | 程式的每個分支都有規則 | 寫不出範例；可能和日後的版本管理裁決衝突 |
| C. 立規則，改成 422 的新代碼 | 例如 `PROJECT_VERSION_NOT_PUBLISHED`，補進 `api-spec.md` | 與 `PROJECT_NOT_ACTIVE` 同類 | 同 B；還多一個新代碼 |

### 建議：A

### 待負責人決定

- **F-4**：採 A？

## 七、裁決後的順序

```mermaid
flowchart LR
  R[負責人裁決四題] --> L[規格的修法 PR<br/>decision-log、specs、api-spec.md、修法紀錄]
  L --> K[建構 PR<br/>F-1、F-2、F-3]
  K --> G[已知失敗清單清空]
```

1. **規格的修法 PR**：`decision-log.md` 新增一條（D-068）；`specs/review-case/` 加 `RC-DRF-03` 與範例；`specs/sampling-batch/` 加 `SB-EXE-S5` 與斷言；`api-spec.md` §4.2 改 `CASE_NOT_IN_INITIAL_REVIEW_STAGE` 的說明、補 `NOT_ASSIGNED_REREVIEWER`（403）。F-1、F-3 的兩個新範例列進已知失敗清單。
2. **建構 PR**：F-1 改 `SaveInitialReviewDraftUseCase`；F-2 改三處 application、`StartRereviewUseCase` 加檢查；F-3 改 `SamplingBatchCompletedEvent` 與兩個發出者。改完從清單刪掉兩行，清單回到空的。

### 還沒有查證的

- F-2：複審頁面在 403 與 422 的顯示是否不同。2026-11-22 查過前端只認 409 的 `CASE_TERMINAL`（`rereview-page.component.ts:532`），沒有依 422 處理；但沒有實際操作頁面。
- 兩個新範例在規格改完後實際的紅綠，要在修法 PR 裡跑過才知道。
