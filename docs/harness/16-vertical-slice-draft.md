# 直向切片（抽選到初審提交）：程式與規格對不上的三處

狀態：草案，待負責人裁決（2026-11-21）。本文件不是律法：裁決後寫進 `decision-log.md`，與規格的修法 PR 一起生效。

## 一、背景

- 負責人 2026-11-21 選擇「先立法」：從抽選到初審提交的直向切片，先由規格的修法 PR 立法（`specs/sampling-batch/`、`specs/review-case/` 等，`spec_version: 2`，`law/coverage.txt` 對應的列改為 `legislated`），再由建構 PR 讓程式配合。修法 PR 不夾帶程式（P4）。
- `law/coverage.txt` 現況：切片會碰到的 `domain/sampling/`、`application/sampling/`、`domain/reviewcase/`、`application/reviewcase/`、`domain/common/`、`application/common/` 與三個 controller 都是 `grandfathered`。
- #118 的驗證把第二輪試寫對應用程式執行，35 個版本 2 的範例通過 4 個。原因是下面三處程式與試寫規格對不上，不是執行器（D-065 Evidence）。
- 本文件只處理這三處要「改規格還是改程式」。試寫已列的內容問題（Q-01～Q-05、R-5 等）不在本文件，正式立法時另外裁決。

## 二、總覽

| # | 對不上的地方 | 影響 | 建議 | 改哪一邊 |
|---|---|---|---|---|
| M-1 | `ListReviewCasesUseCase.execute` 的參數是 `criteria`，規格寫 `batchId` | 29 個範例停在準備步驟 | 規格維持攤平的輸入；程式把參數攤平 | 程式（建構 PR） |
| M-2 | 事件類別沒有 `batchId`、`caseId` 等屬性，只有 `entityId` 與 `detail`（JSON 字串） | 規格的 `with` 寫不到 | 規格照 `domain-events.md` 的欄位名稱；程式為切片用到的事件加上屬性 | 程式（建構 PR） |
| M-3 | `ExecuteSampling` 找不到專案時丟 `IllegalArgumentException`，沒有錯誤代碼 | 範例無法斷言代碼；HTTP 回 500 | 規格用 `PROJECT_NOT_FOUND`，標 `decided`；程式改丟 `ResourceNotFoundException` | 程式（建構 PR） |

三處的建議都是「規格照權威文件寫，程式配合」，與既有規則一致：`specs/README.md` 第十節「沒有對照表：程式要使用規格的名稱」。三處都不需要改格式或執行器（D-065 不必修）。

## 三、M-1：列出案件的輸入

### 查到的事實

| 來源 | 內容 |
|---|---|
| 程式 | `execute(ActorContext actor, ReviewCaseQueryCriteria criteria)`。`criteria` 有 `hospitalId`、`projectId`、`batchId`、`status`、`reviewerId`、`rereviewerId`、`medicalRecordType`、`page`、`size` |
| `api-spec.md` `GET /review-cases` | query params 與上面九個名稱相同 |
| 試寫 `review-case/usecases.yaml` | `input` 只有 `batchId`、`status`（切片需要的兩個） |
| 執行器（`specs/README.md` 第十節） | `input` 依**參數名稱**對到 `execute` 的參數；沒給的參數是 null；規格的名稱找不到同名參數，範例未通過 |
| 已立法的程式 | `application/project/`、`application/scoresheet/` 的 `execute` 都是攤平的參數（例如 `execute(actor, projectId, scoreSheetVersionId)`）。這是先例 |
| `application/reviewcase/`、`application/sampling/` | 有 10 支 `execute` 收 `Command`／`Criteria`／`cmd` 物件，不只這一支 |
| 草案第十三節 P-2 | 建議：立法後由建構 PR 把參數攤平；格式不必改 |

### 選項

| 選項 | 做法 | 優點 | 缺點 |
|---|---|---|---|
| **A. 程式攤平**（建議） | 規格輸入維持 `batchId`、`status`；建構 PR 把 `execute` 改成攤平的參數 | 與已立法的兩個 aggregate 一致；規格只用業務名稱；不改格式 | 建構 PR 要同時改 controller 組參數的地方；`page`、`size` 是 `int`，沒給時不能是 null |
| B. 規格包一層 | 規格輸入寫成 `criteria: ReviewCaseQuery`（`input_types`） | 程式不必改 | 把程式的包裝物件搬進規格，等於反向的對照表；`criteria` 不是 glossary 的詞 |
| C. 執行器支援「把輸入組成物件」 | 修 D-065：參數是物件時，依欄位名稱組裝 | 程式與規格都不必改 | 改律法來遷就未立法的程式；執行器多一條隱含的對照規則 |

### 建議：A

- 規格的輸入名稱照 `api-spec.md`。切片只列需要的欄位（`batchId`、`status`）；其餘七個不在本次規格裡，程式照樣保留參數，執行器傳 null。
- 建構 PR 的範圍：`ListReviewCasesUseCase.execute` 改成攤平的參數；`ReviewCaseController` 改成直接傳入；`page`、`size` 改成可以是 null 的型別，沒給時用現在的預設值（第 0 頁、每頁 20、上限 100）。
- 院區：規格不列 `hospitalId` 的輸入。非 SYSTEM_ADMIN 一律取自登入者（D-042，試寫的 RC-LST-01）。SYSTEM_ADMIN 指定院區（D-047）不在本次切片。
- 切片裡其他收物件的 `execute`（指派、開始初審、保存草稿、提交初審）同樣處理：建構 PR 依規格的輸入名稱攤平。規格的修法 PR 寫完後，逐支列出要改的簽章。

### 待負責人決定

- **M-1-1**：採 A？
- **M-1-2**：切片以外仍收物件的 `execute`（複審、結案、revision 等）這次不動，等各自立法時再攤平？（建議：不動。）

## 四、M-2：事件的欄位

### 查到的事實

| 來源 | 內容 |
|---|---|
| `DomainEvent`（`domain/common/`） | 欄位固定為 `eventId`、`occurredAt`、`actorUserId`、`actorUserName`、`hospitalId`、`entityType`、`entityId`、`action`、`detail`。各事件的內容放在 `detail`，是 `String.format` 組出來的 JSON 字串 |
| 30 個事件類別 | 29 個沒有自己的 getter，只有 `ScoreSheetBoundToProjectEvent` 有。問題不只抽選 |
| `detail` 的使用者 | `AuditTrailListener` 原樣存進稽核紀錄，`AuditTrailQueryAdapter` 原樣讀出。沒有其他程式解析它 |
| D-065 第 7 點與 Evidence | `with` 依屬性名稱部分比對，比對事件類別的屬性（getter） |
| `domain-events.md` | 8 個事件有「最低必要欄位」。切片用到的：§5.1 `SamplingBatchStarted`（`batchId`、`projectId`、`executionMode`）、§5.4 `SamplingBatchCompleted`（`batchId`、`projectId`、`populationCount`、`eligibleCount`、`excludedCount`、`selectedCount`）、§6.2 `ReviewCaseAssigned`（`caseId`、`assignedReviewerId`、`dueAt`）。§5.2、§5.3 只在發布條件提到欄位，沒有列最低必要欄位 |
| 試寫用到的 `with` | 抽選四個事件用 `batchId`（兩個另有 `projectId`）；`ReviewCaseAssigned`、`InitialReviewStarted`、`ReviewDraftSaved`、`ReviewSubmissionSubmitted`、`InitialReviewCompleted` 用 `caseId`（部分另有 `submissionId`、`assignedReviewerId`） |
| 程式的事件類別 | 切片的 9 個事件中，8 個有對應的 `XEvent`。`InitialReviewCompleted` 在 `src/main` 找不到（試寫 Q-03，另案裁決） |

### 選項

| 選項 | 做法 | 優點 | 缺點 |
|---|---|---|---|
| **A. 程式加屬性**（建議） | 規格的 `with` 照 `domain-events.md` 的欄位名稱；建構 PR 為切片用到的事件類別加上對應的欄位與 getter；`detail` 照舊組出，稽核不變 | 事件的業務欄位成為可以驗證的東西，對應 `domain-events.md` 第 13 節「應同步落地」；不改律法 | 建構 PR 要動 8 個事件類別；`detail` 與新欄位內容重複，要有測試確認兩者一致 |
| B. 執行器讀 `detail` | 修 D-065：`with` 找不到屬性時解析 `detail` 的 JSON | 程式不必改 | 律法依附稽核的實作細節；`detail` 是字串拼接，沒有跳脫，不是可靠的契約 |
| C. 規格寫 `entityId` | `with: { entityId: ..., detail: ... }` | 程式與律法都不必改 | `entityId`、`detail` 是實作的詞彙，不是 glossary 的詞；`detail` 只能整串比對，做不到部分比對 |

### 建議：A

- 規格的 `with` 只能用該事件在 `domain-events.md` 有寫到的欄位名稱。
- 沒有「最低必要欄位」的事件（§5.2、§5.3，以及 `InitialReviewStarted`、`ReviewDraftSaved` 等，需逐一核對）：規格只斷言發出事件的 aggregate 的 identity（`batchId`、`caseId`），標 `proposed`。等 `domain-events.md` 補上欄位後再改成 `decided`。
- 建構 PR 的範圍：只改切片的 8 個事件類別，加上規格用到的欄位；其餘 22 個不動。`DomainEvent` 的建構式與 `detail` 都不改，稽核紀錄不變。
- 不在本次範圍：`SamplingBatchCompleted` 的 `detail` 少了 `eligibleCount`、`excludedCount`（與 §5.4 不一致）。切片的規格不斷言這兩個欄位；要補的話另案。

### 待負責人決定

- **M-2-1**：採 A？
- **M-2-2**：沒有「最低必要欄位」的事件，規格只斷言 identity 並標 `proposed`？還是先修 `domain-events.md`，把切片用到的事件的欄位補齊再立法？（建議：前者，切片不必等文件。）
- **M-2-3**：`detail` 與新欄位重複，建構 PR 要不要改成由新欄位產生 `detail`？（建議：要。只有一個來源，不會不一致；稽核紀錄的內容要逐字不變，由測試確認。）

## 五、M-3：抽選時專案不存在

### 查到的事實

| 來源 | 內容 |
|---|---|
| 程式 `ExecuteSamplingUseCase` | `findById(projectId).orElseThrow(() -> new IllegalArgumentException(...))` |
| `GlobalExceptionHandler`（`legislated`，`specs/http-common.yaml`） | `IllegalArgumentException` 對到 500 `INTERNAL_ERROR`，記錄為「likely a programming error」；`ResourceNotFoundException` 對到 404 與它的 `errorCode` |
| 同一個專案的其他程式 | `FixInitialReviewUseCase`、`HospitalIdResolverImpl` 找不到專案時已經丟 `ResourceNotFoundException("PROJECT_NOT_FOUND", ...)` |
| `specs/review-project/usecases.yaml`（`legislated`） | 已定義 `PROJECT_NOT_FOUND`，category `not_found`，source 是 `api-spec.md` |
| `api-spec.md` | §7、§8.1 的錯誤表有 `PROJECT_NOT_FOUND`（404，projectId 不存在）。抽選端點（`POST /review-projects/{projectId}/sampling/execute`）的「業務限制」沒有列它 |
| 試寫 Q-04 | 假設 `PROJECT_NOT_FOUND`，標 `proposed` |

### 選項

| 選項 | 做法 | 說明 |
|---|---|---|
| **A. `PROJECT_NOT_FOUND`，`decided`**（建議） | 規格的 SB-G-02 用 `PROJECT_NOT_FOUND`，source 引用 `api-spec.md` 的錯誤表與 `specs/review-project/`；建構 PR 改丟 `ResourceNotFoundException("PROJECT_NOT_FOUND", ...)` | 代碼在本專案已有一致的意義；現在的 500 是缺陷，不是設計 |
| B. `PROJECT_NOT_FOUND`，維持 `proposed` | 規格照寫，範例列為不能當驗收依據 | 抽選端點的文件沒寫，保守；但這條範例會一直不算數 |
| C. 通用的 `NOT_FOUND` | 用 §4.1 的通用代碼 | 與已立法的 `review-project` 不一致 |

### 建議：A

- 理由：代碼、HTTP 狀態、意義都已經在 `api-spec.md` 與已立法的規格裡；抽選端點只是沒有重列。`IllegalArgumentException` 在本專案的處理器裡被當成程式錯誤，所以現況回 500 本身就違反 `http-common.yaml` 的意圖。
- 跨目錄：`sampling-batch/usecases.yaml` 的 `error_codes` 要自己列 `PROJECT_NOT_FOUND`（D-065 第 2 點：`then.code` 必須定義在 `when` 那支 use case 所在的目錄）。
- 建構 PR 的範圍：`ExecuteSamplingUseCase` 一行。
- 同步修正權威文件：`api-spec.md` 抽選端點補上錯誤表（`PROJECT_NOT_FOUND`、`HOSPITAL_SCOPE_MISMATCH`、`PROJECT_NOT_ACTIVE`）。這屬於規格的修法 PR，不屬於建構 PR。

### 順帶查到、不在本題

- 程式在專案沒有已發布版本時丟 `INVALID_STATE_TRANSITION`（409）。試寫沒有這條規則，`api-spec.md` 的「業務限制」有寫「採用 published version」。正式立法時要補一條規則與範例，代碼另外裁決。

### 待負責人決定

- **M-3-1**：採 A（`decided`）？
- **M-3-2**：`api-spec.md` 抽選端點的錯誤表放在規格的修法 PR 一起補？

## 六、裁決後的順序

```mermaid
flowchart LR
  R[負責人裁決 M-1～M-3] --> L[規格的修法 PR<br/>specs/ v2、coverage.txt、api-spec.md、decision-log、修法紀錄]
  L --> K[建構 PR<br/>攤平參數、事件屬性、PROJECT_NOT_FOUND]
  K --> G[範例由紅轉綠]
```

1. 規格的修法 PR：切片的範例一開始在三處是紅的。`./gradlew test` 的實際失敗必須與 `known-test-failures.txt` 完全相同，所以紅的範例要列進這份清單。這份清單是 `baseline`，只能縮短；新增等於同意紅燈存在，要在同一個修法 PR 裡由負責人核准，並註明對應的決策（清單檔頭）。
2. 建構 PR：照第三～五節的範圍改程式，把對應的範例移出已知失敗清單。每改一處，就有範例由紅轉綠，作為驗證。
3. 建構 PR 完成後，才算「時鐘與矩陣對應用程式驗過」（D-065 Evidence 的待驗證項目）。

還沒有查證的：

- 規格的修法 PR 會不會在這三處以外，還有程式對不上的地方。#118 那次執行，29 個範例停在 M-1，沒有走到後面的步驟。
- 紅的範例有多少個。#118 那次是 31 個未通過，但規格正式寫完後數字會變；要在修法 PR 裡實際跑過才知道。
- **M-0（待負責人決定）**：替代做法是把建構 PR 拆在前面、先改程式，讓規格的修法 PR 一進來就是綠的，不必新增已知失敗。但那樣就是在 `grandfathered` 範圍裡、沒有律法的情況下先改程式，與 2026-11-21 選的「先立法」相反。建議照本節的順序，接受暫時的已知失敗。
