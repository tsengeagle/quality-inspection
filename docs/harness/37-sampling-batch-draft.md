# 切片 A：SamplingBatch 其餘部分（重跑、預覽、查詢與匯出、`SamplingBatch` 本體、規則快照）

狀態：**已裁決**（2026-11-25）。負責人裁決：S-5 改為加大替身的母體來源（不改抽選的規則），其餘照建議；條文是 `decision-log.md` 的 D-086，以那裡為準。下面保留起草時的版本（main = `42fa5fe`），加上裁決後實跑範例的一則補充（事實 11）。本文件不是律法；題號 S-1～S-16 只在本文件使用。

負責人 2026-11-25 同意的順序：C（D-085，已完成）→ **A（本切片）** → B 報表 → D、E HTTP 介面與共用元件 → 專案版本管理與未裁決事項。

## 一、範圍

| 範圍 | 檔案 | 立法狀態 |
|---|---|---|
| `SamplingBatch` 本體 | `domain/sampling/` 的 `SamplingBatch.java`、`BatchStatus.java`、`ExecutionMode.java`、`RuleSnapshot.java`、`SelectedCaseRef.java`、`SamplingBatchRepository.java`、`event/SamplingBatchFailedEvent.java` | 未立法。`specs/sampling-batch/model.yaml` 只有狀態列舉；四個轉移都在 `deferred_transitions` |
| 重跑 | `application/sampling/RetrySamplingBatchUseCase.java` | 未立法（D-013 是權威文件的裁決，沒有規格） |
| 預覽與執行頁 | `PreviewSamplingPopulationUseCase`、`SamplingPreviewResult`、`GetSamplingExecuteContextUseCase`、`SamplingExecuteContextView` | 未立法 |
| 查詢與匯出 | `ListSamplingBatchesUseCase`、`GetSamplingBatchDetailUseCase`、`ExportSamplingBatchResultUseCase`、`SamplingBatchQueryPort`、`SamplingBatchQueryCriteria`、`SamplingBatchSummary`、`SamplingBatchDetailView`、`RuleSnapshotSummary`、`SelectedCaseRefView`、`SamplingBatchExportResult` | 未立法 |
| 母體來源 | `CandidateSourcePort` | 規格有 port 的宣告，檔案未登記 |
| 已立法範圍的缺口 | `ExecuteSampling`：母體 0 筆、同批次重複、跨批次重複、排除條件 | `model.yaml` 的 `deferred_behaviors` |

不在本切片：`SamplingController` 與 HTTP 的請求、回應格式（D、E 切片，D-061 Q-3）。查到的 HTTP 落差列在第七節，供那個切片使用。

## 二、查到的事實

**本節全部是讀程式與既有測試確認的，沒有實測**（規格範例的執行器目前只接了 `ExecuteSampling`）。如果負責人要先實測再裁決，請指定題號。

**失敗與重跑**

1. **沒有任何程式會產生 `FAILED` 的批次。** `SamplingBatch.fail` 沒有呼叫者；`SamplingBatchFailedEvent` 沒有發出者。
2. `ExecuteSampling` 與重跑整支都在一個交易裡：中途出錯（例如母體來源丟例外）整個回滾，**不留下批次**，也沒有事件。
3. 因此重跑的成功路徑（對 `FAILED` 批次重跑）**從外面走不到**；既有的單元測試與 API 測試是直接把 `FAILED` 的批次放進儲存庫。
4. 重跑走得到的部分：
   - 批次不存在：`IllegalArgumentException`，HTTP **500**。
   - **沒有院區檢查**：只要求操作者有院區，不比對批次所屬專案的院區。別院的管理員對本院的批次重跑，得到的是 `BATCH_NOT_RETRYABLE`（批次是 `COMPLETED` 時），不是 `HOSPITAL_SCOPE_MISMATCH`。
   - 批次不是 `FAILED`：`BATCH_NOT_RETRYABLE`（422）。
   - 專案不存在：`IllegalArgumentException`，HTTP **500**；專案不是 `ACTIVE`：`PROJECT_NOT_ACTIVE`。
5. D-013（重跑沿用原批次的快照）：程式把原批次的 `RuleSnapshot` 複製到新批次。但母體查詢用的是**專案目前的**院區與病歷類型，**沒有任何行為讀快照的內容**。所以「沿用快照」目前只影響新批次上記錄的版本號。
6. `ExecutionMode` 有 `MANUAL`、`SCHEDULED`、`RETRY`；`SCHEDULED` 沒有任何程式使用。`ExecuteSampling` 沒有 `mode` 輸入，一律 `MANUAL`；HTTP 請求本文的 `mode` 沒有讀取。

**執行抽選**

7. **沒有抽樣**：母體來源回傳的每一筆都選中、各建一個案件。`excludedCount` 固定 0，`eligibleCount = populationCount = selectedCount`。`api-spec.md` 建立專案（`POST /review-projects`）的請求範例裡有 `samplingPolicy`（比例）、`excludedDepartments`，但專案的 domain 與 HTTP 請求都沒有 `samplingPolicy` 這個欄位，抽選也不讀排除條件。
8. 母體來源（`CandidateSourcePort`）在正式環境的實作是 `StubCandidateSourceAdapter`：每個專案固定回傳 5 筆，`sourceCaseKey` 是 `CASE-{projectId}-{1..5}`。真正的介接還沒有。
9. 母體 0 筆：批次 `COMPLETED`，四個計數都是 0，不建立案件，四個事件照發。
10. 母體回傳重複的 `sourceCaseKey`：`DUPLICATE_SELECTED_CASE_IN_BATCH`（409），整個回滾（D-007）。
11. 同一個專案執行兩次：建立兩個批次；因為不做跨批次排重（D-007），**同樣的 5 個 `sourceCaseKey` 會再建立 5 個案件**。（裁決後補充：寫範例時實跑確認。在**同一個交易**裡抽選兩次會撞到 `(batch_id, source_case_key)` 的唯一約束而失敗，因為抽中病歷的資料列先以沒有批次的狀態寫入；分開的交易不會。見 D-086 第 17 點。）
12. 案件的院區取自母體那一筆的 `hospitalId`，不檢查是否等於專案的院區。

**規則快照（D-011）**

13. `RuleSnapshot` 有五個欄位：
    - `projectVersionId`：組出來的字串 `{projectId}-v{版本號}`，不是任何資料的 ID。
    - `projectVersionNumber`：專案目前版本的版本號。
    - `scoreSheetVersionId`：專案綁定的評分表版本；沒有綁定時寫 `"UNBOUND"`。發布時已檢查綁定（D-041），啟用中的專案走不到 `"UNBOUND"`。
    - `samplingPolicy`：固定 `"MANUAL"`。
    - `snapshotSummary`：固定 `null`。
14. `persistence-model.md` §5.9 的 `sampling_batch_snapshot`（`snapshot_json`）沒有實作；五個欄位存在 `sampling_batch` 資料表。
15. 快照沒有被任何行為使用：案件的評分表版本在開始初審時取專案當時的版本；品質線與權重取專案目前的版本（D-068 第 4 點、D-083 第 11 點的已知問題）。

**預覽與執行頁**

16. 預覽：角色 → 載入專案 → 院區 → `PROJECT_NOT_ACTIVE`；回傳三個計數；不建立批次、不發事件。專案不存在是 `IllegalArgumentException`，HTTP **500**。沒有「未發布版本」的檢查。
17. 執行頁（`GetSamplingExecuteContext`）：
    - `previewReady` 固定 `false`、`previewStats` 固定 `null`。
    - `executing` 實際上永遠是 `false`（批次在同一個交易裡完成，查不到進行中的批次）。
    - `blockedReason`：版本未發布 `PROJECT_VERSION_NOT_PUBLISHED`，否則專案不是 `ACTIVE` 時 `PROJECT_NOT_ACTIVE`，否則 `null`。
    - `executeFailed`、`executedBatchId`、`postExecuteSummary` 由專案最新的批次（依 `startedAt`）推出；`executeFailed` 因為第 1 點永遠是 `false`。
    - `ruleSummary`、`postExecuteSummary` 的 `title`、`description` 是後端組出來的中文顯示文字。

**查詢與匯出**

18. 列表：`HOSPITAL_MANAGER`、`AUDITOR`、`SYSTEM_ADMIN`；非 `SYSTEM_ADMIN` 只看本院，請求的 `hospitalId` 忽略；`SYSTEM_ADMIN` 可用 `hospitalId` 縮小範圍。篩選 `projectId`、`status`、`executionMode`；依 `startedAt` 新到舊；`size` 預設 20、上限 100。
    - `api-spec.md` §9.5 列的 `startedFrom`、`startedTo` **程式沒有**；程式有的 `executionMode` 文件沒有。
19. 詳情：同樣三個角色；角色 → 批次不存在 `SAMPLING_BATCH_NOT_FOUND`（404）→ 院區（`SYSTEM_ADMIN` 跨院）。
20. **`SYSTEM_ADMIN` 拿到的詳情與其他角色完全相同**，包含 `selectedCases`（每一筆的 `sourceCaseKey`、院區、病歷類型）。`api-spec.md` §9.5、§9.6 寫的是「僅 metadata」。
21. 匯出：只有 `HOSPITAL_MANAGER`，本院；回傳的是詳情外面包一層 `detail`，沒有檔案格式；**不發事件**，稽核查不到誰匯出。

**架構基線**

22. `ExecuteSampling`、重跑用 `UUID.randomUUID()` 產生 ID（ARCH-02 基線 4 行）；預覽、重跑丟 `IllegalArgumentException`（ARCH-03 基線 3 行）。

## 三、失敗與重跑

### S-1：失敗的批次

- **A（建議）：** 照現況，不立失敗的規則。抽選中途出錯時不留下批次（事實 2）；`FAILED`、`SamplingBatchFailed`、`failureReason` 列進 `deferred_behaviors`，等母體來源的真正介接（事實 8）定案時一起決定失敗要怎麼記錄。理由：現在唯一會失敗的地方是還沒介接的外部系統，失敗的種類與要不要留紀錄都還無從判斷。
- **B：** 現在立法失敗路徑：母體來源出錯時保存一個 `FAILED` 的批次、發 `SamplingBatchFailed`、回一個錯誤代碼。建構 PR 要把批次的保存移到獨立的交易。

### S-2：D-013 與重跑的成功路徑

- **A（建議）：** D-013 不改。成功路徑（`FAILED` → 新批次、`retryOfBatchId`、沿用快照）跟著 S-1 延後，列進 `deferred_behaviors`，註明事實 5：快照目前不影響母體查詢。程式保留。
- **B：** 現在立法成功路徑，範例做不出前提，以單元測試驗證（比照 D-085 第 16 點）。缺點：條文描述的是從外面走不到的行為。

### S-3：重跑走得到的檢查

- **A（建議）：** 立法並修正，順序：角色 → 批次不存在 `SAMPLING_BATCH_NOT_FOUND`（404，現在 500）→ 院區 `HOSPITAL_SCOPE_MISMATCH`（現在沒有檢查）→ 批次不是 `FAILED` `BATCH_NOT_RETRYABLE`（422）。專案不存在改 `PROJECT_NOT_FOUND`（防線）。前三項做得出範例。
- **B：** 照程式（500、沒有院區檢查）。

## 四、執行抽選的補齊

### S-4：母體 0 筆

- **A（建議）：** 照程式：批次 `COMPLETED`、計數都是 0、不建立案件、四個事件照發。
- **B：** 拒絕並回錯誤代碼，不留下批次。

### S-5：重複

- **建議（照程式與 D-007）：**
  - 母體回傳同一個 `sourceCaseKey` 兩次：`DUPLICATE_SELECTED_CASE_IN_BATCH`（409），不留下批次與案件。
  - 跨批次不排重：同一個專案再執行一次，同樣的病歷會再建立案件（事實 11）。補一個範例把這件事寫明。
- 請負責人確認第二點是要的行為。D-007 的理由是「先採最小必要限制」；在母體來源是固定 5 筆的現在，每按一次執行就多 5 個重複的案件。

### S-6：抽樣與排除條件

- **A（建議）：** 照程式立法「母體來源回傳的每一筆都選中」（SB-EXE-S2 已經這樣寫），加上 `excludedCount = 0`、`eligibleCount = populationCount`。抽樣比例、排除科別、分層列進 `deferred_behaviors`，等母體來源的介接與專案版本管理的切片。
- **B：** 現在裁決抽樣規則。需要先定 `samplingPolicy` 存在哪裡（專案目前沒有這個欄位）。

### S-7：`SamplingBatch` 的不變式

- **建議（照程式）：**
  - 計數不得為負（`INVALID_METRIC_VALUE`）；`selectedCount ≤ eligibleCount`。
  - `COMPLETED`、`FAILED` 是終點，內容不再改變。
  - 同一批次內 `sourceCaseKey` 不重複。
  - `retryOfBatchId` 只有 `RETRY` 的批次有。
  - 內部的三個轉移（`STARTED → COLLECTING_CANDIDATES → SAMPLING → COMPLETED`）留在 `deferred_transitions`：它們是 `ExecuteSampling` 內部的步驟，從外面只看得到 `COMPLETED`。
- `ExecutionMode.SCHEDULED`：沒有任何程式使用。排程抽選出現在需求草案與 `domain-model.md`，是規劃中的功能，建議保留列舉值並列進 `deferred_behaviors`。

### S-8：規則快照

- **A（建議）：** 立法有意義的兩項：批次記下執行當時專案的版本號與評分表版本，完成後不變（D-011）。其餘：
  - `projectVersionId`（組出來的字串）照程式保留，條文註明它是版本號的另一種寫法。
  - `samplingPolicy`（固定 `"MANUAL"`）、`snapshotSummary`（固定 `null`）沒有任何行為寫入有意義的值。建議**不移除**，列進 `deferred_behaviors`：它們是 S-6 的抽樣規則與 `sampling_batch_snapshot` 的位置。
  - `"UNBOUND"` 保留為防線（走不到）。
- **B：** 移除 `samplingPolicy`、`snapshotSummary`（比照 D-084 第 9 點），要加資料表的遷移。
- 不論選哪個：快照沒有被任何行為使用（事實 15）是專案版本管理切片的題目，本切片只註記。

## 五、預覽與執行頁

### S-9：預覽

- **A（建議）：** 立法並修正：角色 → `PROJECT_NOT_FOUND`（現在 500）→ 院區 → `PROJECT_NOT_ACTIVE`；回傳三個計數；不建立批次、不發事件。
- **B：** 照程式（500）。

### S-10：執行頁

- **A（建議）：** 立法有行為的欄位：專案的摘要欄位、`blockedReason` 的兩個值與先後、`executedBatchId`（最新的批次是 `COMPLETED` 時）。其餘列進 `deferred_behaviors`，不立內容：
  - 固定值的 `previewReady`、`previewStats`、`executing`、`executeFailed`（事實 17）。
  - 顯示文字 `ruleSummary`、`postExecuteSummary`。
- **B：** 全部照程式立法，包括固定值與中文文字。
- **C：** 移除固定值的欄位。會動到前端，建議留給 D、E 切片。

## 六、查詢與匯出

### S-11：列表

- **A（建議）：** 照程式立法（事實 18）。`api-spec.md` §9.5 補上 `executionMode`；`startedFrom`、`startedTo` 從文件移到 `deferred_behaviors`。
- **B：** 實作時間篩選（時區規則照 D-073、D-077）。

### S-12：`SYSTEM_ADMIN` 看到的內容

- **A（建議）：** `SYSTEM_ADMIN` 的詳情不含 `selectedCases`（個別病歷的識別），其餘計數、版本、時間是 metadata，照給。與 RC-INV-02（`SYSTEM_ADMIN` 只取得 metadata）一致。列表本來就沒有個別病歷。
- **B：** 照程式，`SYSTEM_ADMIN` 看到完整的詳情；`api-spec.md` 改掉「僅 metadata」。

### S-13：詳情

- **建議（照程式）：** 角色 → `SAMPLING_BATCH_NOT_FOUND` → 院區（`SYSTEM_ADMIN` 跨院）。輸出：批次的狀態、執行方式、四個計數、`retryOfBatchId`、`failureReason`、起訖時間、快照的四個欄位、`selectedCases`（S-12）。

### S-14：匯出

- **A（建議）：** 照程式立法：只有 `HOSPITAL_MANAGER`、本院、內容與詳情相同。檔案格式列進 `deferred_behaviors`。
- **B：** A 加上發一個稽核事件，記錄誰匯出了哪個批次。請負責人判斷抽選名單的匯出需不需要留紀錄。
- **C：** 移除匯出：內容與詳情完全相同，沒有獨立的行為。

## 七、登記與其他

### S-15：架構基線

- **建議：** 建構 PR 一併把 `UUID.randomUUID()` 改成 `IdGenerator`、`IllegalArgumentException` 改成對應的例外（S-3、S-9），ARCH-02、ARCH-03 的基線縮短 7 行。

### S-16：登記範圍

- **建議：** 第一節的檔案全部登記到 `specs/sampling-batch/`。`SamplingBatchFailedEvent` 跟著 S-1：選 A 時仍登記（列舉值與事件類別保留），條文在 `deferred_behaviors`。登記後 `domain/sampling/`、`application/sampling/` 兩個目錄列底下沒有暫准的檔案；目錄列保留。

### 留給 D、E 切片的 HTTP 落差（只記錄，不在本切片裁決）

- 執行抽選的回應只有 `batchId`；`api-spec.md` §9.3 寫 `batchId`、`status`、`selectedCount`。
- 重跑的回應是 `batchId`；`api-spec.md` §9.4 寫 `newBatchId`、`retryOfBatchId`。
- 預覽、執行的請求本文 `mode` 沒有讀取。
- 列表的 `status`、`executionMode` 不合法時回 `BAD_REQUEST`。

## 八、題目清單

本切片的修法 PR 一併：

- C（D-085）移到已完成；A 標為進行中。
- 新增延後的題目：母體來源的介接（失敗的記錄、抽樣與排除、S-1、S-2、S-6）。
