# 違憲清單第 3～6 項：時間改成時間點（`Instant`）

狀態：**已裁決**（2026-11-23）。負責人對 N-1～N-8 全部照建議裁決，並說明目前沒有正式環境、也沒有需要保留資料的環境。條文是 `decision-log.md` 的 D-075，以那裡為準。本文件不是律法，下面保留起草時的版本（main = `edb7f61`）。題號 N-1～N-8 只在本文件使用。

**修法時的調整**：CSV 由前端產生（`ui-page-spec-ui017` §5.2），所以 D-075 第 6 點多了 CSV 的欄位標題；報表匯出頁 `createdAt` 的顯示方式併入第 5 點；請求的文件範例保留 `+08:00`（第 8 點）。

## 一、背景

- 憲法草案第一條：跨出程式邊界的時間一律帶時區、輸出 UTC `Z`；程式內部用 `Instant`；需要當地時間的地方以 `Asia/Taipei` 換算，位置要在裁決中寫明。
- D-072 第 4 點的違憲清單還剩：
  - 第 1 項的 `REASSIGNED`（稽核 `detail` 的 `dueAt`）；
  - 第 3 項：HTTP 輸出的時間不帶時區；
  - 第 4 項：`ReviewCase` 的六個 `LocalDateTime`；
  - 第 5 項：`spec-profile.yaml` 的 `local_time_zone` 用來解讀不帶時區的時間；
  - 第 6 項：文件的時間範例帶 `+08:00`。
- 題目清單（`25-topic-backlog.md`）把這幾項排在 D-074 之後。D-074 的三個 PR（#137、#138、#139）都已合併。

## 二、查到的事實

**`LocalDateTime` 的分布**（`src/main` 共 39 個檔案）

| 層 | 內容 |
|---|---|
| domain | `ReviewCase`：`dueAt`、`closedAt`、`cancelledAt`、`rereviewerDueAt`；兩個 `ReviewerAssignment` 的 `assignedAt` |
| domain | **`ReviewSubmission`：`createdAt`、`submittedAt`、`updatedAt`**（違憲清單沒有列） |
| domain 事件 | `ReviewCaseAssignedEvent`、`ReviewCaseReassignedEvent`、`RereviewerAssignedEvent` 的 `dueAt` |
| application | 指派與改派四支 use case 的 `dueAt` 參數；`ReassignRereviewerCommand` |
| application view | `ReviewCaseSummary`、`ReviewCaseDetailView`、`AssignmentBootstrapView`、`AssignmentCaseSnapshotView`、`AssignmentAssigneeSummaryView`、`RereviewBootstrapView`、`ReviewSubmissionRefView`、`RevisionHistoryEntry`、`SubmissionDetailView`、`SubmissionSummaryItem` |
| persistence | `ReviewCaseEntity`（6 欄）、`ReviewSubmissionEntity`（3 欄） |
| interfaces | `SubmissionSummaryDto.submittedAt`；`ReviewCaseController.toTaipeiWallTime`（D-073 的換算） |
| reporting | `ExportRow.closedAt`、`ExportCaseRecord.closedAt` |

測試有 52 個檔案用到 `LocalDateTime`。

**「現在」是怎麼取的**
- `SystemClockConfig` 提供的是 `Clock.systemDefaultZone()`。
- `assignedAt`、`closedAt`、`cancelledAt`，以及提交的 `createdAt`、`submittedAt`、`updatedAt`，都是 `LocalDateTime.now(clock)`。
- 所以這些值是**伺服器 JVM 時區的牆上時刻**，不一定是台北。D-072 第 4 點第 4 項寫「這六個值代表台北的牆上時刻」，只有在伺服器設成 `Asia/Taipei` 時才成立。伺服器若是 UTC，同一筆案件的 `dueAt`（controller 換成台北）與 `assignedAt`（UTC）會差 8 小時，而輸出看不出來。
- 正式環境的 JVM 時區還沒有查證。

**資料庫**
- 欄位都是 `timestamp`（不帶時區），`hibernate.jdbc.time_zone: UTC`。
- 同一張表裡已經有 `Instant` 欄位（`created_at`、`initial_fixed_at`、稽核的 `occurred_at`），以 UTC 存取。
- 欄位型別不用改；`LocalDateTime` 改成 `Instant` 之後，同一欄存的值會從牆上時刻變成 UTC。
- 正式資料庫還沒有資料（D-045）。

**規格與執行器**
- `specs/` 的時間型別是 `datetime`，範例都已經寫帶時區的值（`spec_lint` 會擋不帶時區的）。
- 執行器有兩處替不帶時區的時間換算，都依 `local_time_zone`：
  - 輸入：`SpringUseCaseInvoker.localTime`，`execute` 的參數是 `LocalDateTime` 時換成當地時間；
  - 輸出：`ExampleRunner.instantOf`，程式輸出不帶時區時當成當地時間。
- `local_time_zone` 另外用來決定範例時鐘的時區（`AcceptanceClock`）。
- 所以程式改成 `Instant` 之後，現有範例預期不用改，也應該維持綠的。這是推論，還沒有實際驗證。

**前端**
- 時間都當成字串處理，沒有換算時區：
  - 案件列表：`displayDate` 把 `T` 換成空白、取前 16 個字元；
  - 指派頁、修訂歷程、版本摘要：直接顯示字串；
  - 指派頁的輸入框：`toDateTimeLocalInput` 取字串前 16 個字元。
- 後端輸出改成 `Z` 之後，這些地方會把 UTC 當成當地時間顯示，差 8 小時。前後端必須同一個建構 PR 一起改。

**文件**
- 帶 `+08:00` 的範例：`domain-events.md` 19 處、`api-spec.md` 14 處、`docs/05-ui/` 五份頁面規格共 16 處。

**律法涵蓋（D-060）**
- `domain/reviewcase/`、`application/reviewcase/`、`reporting/`、`ReviewCaseController`、`interfaces/rest/dto/` 是 `grandfathered`。
- 其中已立法的：`AssignReviewCaseUseCase`、`AssignRereviewerUseCase` 等有規格的 use case，以及 `ReviewCaseAssignedEvent` 等事件。
- 改派、修正、不經複審結案、取消、退回還沒有立法，這次會動到它們的時間型別。

**還沒有查證的**
- 正式環境的 JVM 時區。
- `Instant` 經 Jackson 輸出時的小數位數（既有的 `Instant` 欄位已經在輸出，推測沒有問題）。
- demo 資料與前端測試是否依賴不帶時區的格式。

## 三、N-1：第 3 項與第 4 項的順序

### 選項

- **A（建議）：一起做，從 domain 改起。** domain、persistence、use case、view 全部改成 `Instant`，HTTP 輸出自然變成 `Z`，第 3 項不用另外處理。D-073 放在 controller 的換算（`toTaipeiWallTime`）一併移除。
- **B：先做第 3 項，在輸出的邊界換算。** view 維持 `LocalDateTime`，輸出時換成 `Z`。之後做第 4 項時這些換算要再拆掉，而且「現在」取的是 JVM 時區的問題還在，換算可能是錯的。

### 待負責人決定

A／B？

## 四、N-2：`ReviewSubmission` 的三個時間

違憲清單第 4 項只列了 `ReviewCase` 的六個值，沒有列 `ReviewSubmission` 的 `createdAt`、`submittedAt`、`updatedAt`。它們同樣以 `LocalDateTime` 輸出（`submittedAt`、`initialSubmittedAt`）。

### 選項

- **A（建議）：補進違憲清單，這次一起改。** 同一批 view 同時含案件與提交的時間，只改一半會讓同一個回應裡兩種格式並存。
- **B：這次只改清單上的六個值**，提交的時間另開一題。

### 待負責人決定

A／B？

## 五、N-3：既有資料與時鐘

### 建議

- 不寫資料轉換：正式資料庫沒有資料（D-045）；本機與 demo 的資料庫重建。
- 欄位型別不改，不新增 migration。
- `SystemClockConfig` 改成 `Clock.systemUTC()`。改成 `Instant.now(clock)` 之後時區已經不影響結果，這只是避免之後有人再用 `LocalDateTime.now(clock)`。
- 在裁決裡更正 D-072 第 4 點第 4 項的說法：只有 `dueAt`、`rereviewerDueAt` 是台北的牆上時刻，其餘四個值是 JVM 時區的牆上時刻。

### 待負責人決定

同意不轉換既有資料嗎？正式環境以外，有沒有需要保留資料的環境？

## 六、N-4：前端怎麼顯示

D-073 第 6 點把前端顯示留到這一項。

### 選項

- **A（建議）：固定以 `Asia/Taipei` 顯示。** 收到 `Z` 的值，換成台北時間再顯示與填入輸入框；送出時照 D-073 加 `+08:00`。與 D-073 的 I-1、憲法第一條第 3 點一致，不依賴瀏覽器時區。
- **B：依瀏覽器時區顯示。** 使用者的電腦時區設錯時，顯示與送出會不一致（送出固定是台北）。

換算的位置（憲法第一條第 3 點要求寫明）：前端一個共用的函式，所有顯示與輸入框都經過它。

### 待負責人決定

A／B？

## 七、N-5：匯出的 `closedAt`

憲法第一條第 1 點把匯出列為程式邊界，所以照條文是 UTC `Z`。

### 選項

- **A（建議）：照條文輸出 UTC `Z`。** 查證：匯出是 `POST /reports/export` 的 JSON 回應（`ApiResponse<ExportResult>`），不是後端產生的檔案；同一列的 `createdAt` 已經是 `Instant`。所以只是讓 `closedAt` 與它一致。
- **B：匯出改成台北時間並帶 `+08:00`。** 牴觸第一條第 2 點「輸出一律正規化為 UTC」，要先修改條文。

### 待負責人決定

A／B？

## 八、N-6：`local_time_zone` 與執行器（第 5 項）

程式改成 `Instant` 之後，執行器替不帶時區的時間做的兩處換算就用不到了。

### 選項

- **A（建議）：建構 PR 合併之後，另開一個修法 PR 收緊。**
  - 執行器移除兩處換算；程式輸出不帶時區的時間時，範例直接失敗。
  - `local_time_zone` 只剩「範例時鐘的時區」這個用途，`specs/README.md` 第九、十節照改。
  - 順序必須在建構 PR 之後，否則現有範例會全部變紅。
- **B：維持現狀。** 之後有人再寫出不帶時區的輸出，範例不會發現。

### 待負責人決定

A／B？

## 九、N-7：文件（第 6 項）

### 建議

- `domain-events.md`、`api-spec.md`、`docs/05-ui/` 的時間範例一律改成 UTC `Z`，換算成同一個時刻（不是只換字尾）。
- 放在第一個修法 PR，與裁決一起。
- `api-spec.md` 的回應範例若有不帶時區的時間，一併改正。

### 待負責人決定

`docs/05-ui/` 的頁面規格也一起改嗎？（D-072 只點名前兩份。）

## 十、N-8：未立法範圍與 `REASSIGNED`

- 這次會動到 `grandfathered` 範圍裡改派、修正、不經複審結案、取消、退回的時間型別。只改型別與格式，不改業務規則。
- `ReviewCaseReassignedEvent` 的 `dueAt` 改成 `Instant` 之後，稽核 `detail` 會自動變成 `Z`，違憲清單第 1 項的 `REASSIGNED` 就一起修正了。D-072 第 5 點已經允許為了合憲修改 `detail` 的時間表示。

### 待負責人決定

同意在暫准範圍內照憲法第一條實作（比照 D-073 第 5 點）嗎？`REASSIGNED` 一起修正嗎？

## 十一、裁決後的順序

1. **修法 PR 一**：
   - `decision-log.md` 新增一條裁決，並註記違憲清單各項的修正；
   - 文件範例（N-7）；
   - 本草稿標為已裁決；
   - 更新 `25-topic-backlog.md`（D-074 移到已完成）。

   規格範例預期不用改。
2. **建構 PR**：後端全部改成 `Instant`，前端顯示（N-4），同一個 PR。範例應該維持綠的。
3. **修法 PR 二**：收緊執行器與 `local_time_zone` 的說明（N-6）。
