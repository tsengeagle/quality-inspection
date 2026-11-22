# 憲法草案第一條：日期時間的表示

狀態：**已裁決**（2026-11-22）。負責人對 T-1～T-5 全部照建議裁決，條文是 `decision-log.md`「憲法草案」第一條與 D-072，以那裡為準。本文件不是律法，下面保留起草時的版本（main = `ee21979`）。

**裁決時補充**：負責人確認 `dueAt` 在需求中是一個確切的時刻，不是日期（第八節「還沒有查證的」第二項就此解決）。題號 T-1～T-5 只在本文件使用。

## 一、背景

- D-071 的建構 PR 有兩個範例沒有通過。範例預期 `ASSIGNED` 的 `detail` 裡是 `"dueAt":"2026-11-26T23:59:59Z"`，程式實際寫出 `"2026-11-27T07:59:59"`，不帶時區。
- 查證之後發現，專案沒有全專案層級的時間規定：
  - `api-spec.md` §3.3 只寫「ISO-8601」，附一個帶 `+08:00` 的範例。
  - `specs/spec-profile.yaml` 的 `local_time_zone` 反而容許不帶時區的時間。
- 負責人裁決（2026-11-22）：
  - 在 `decision-log.md` 新增一節「憲法草案」，註明未來會提升為獨立的憲法。
  - 第一條先定時間格式，並貫穿全專案。

## 二、查到的事實

**程式有兩套時間表示。**

| 表示方式 | 用在哪裡 | 輸出的樣子 |
|---|---|---|
| `Instant`（時間點） | `updatedAt`（D-037）、`createdAt`（D-057）、`initialFixedAt`（D-054）、事件的 `occurredAt`、稽核紀錄、抽選批次、讀取者紀錄、複審指派歷史 | `2026-11-22T12:33:50.715216Z` |
| `LocalDateTime`（不帶時區） | `ReviewCase` 的 `dueAt`、`assignedAt`、`closedAt`、`cancelledAt`、`rereviewerDueAt`、`rereviewerAssignedAt`；案件詳情、列表、指派畫面的 view；匯出的 `closedAt`；指派與改派的 request | `2026-11-27T07:59:59`；秒數是 0 時會省略，例如 `2026-12-01T17:00` |

- **涉及的檔案數**：
  - `LocalDateTime`：42 個檔案（application/reviewcase 27、domain/reviewcase 6、interfaces/rest 5、infrastructure 2、application/common 1、reporting 1）。
  - `OffsetDateTime`、`ZonedDateTime`：0 個。
- **HTTP 本身就不符合 §3.3。** 指派和改派的 request 收不帶時區的 `dueAt`：API 測試送的是 `"2026-12-31T23:59:00"`。案件列表、詳情回傳的 `dueAt`、`assignedAt` 也不帶時區。D-071 的範例會在案件列表通過，是因為執行器依 `local_time_zone: Asia/Taipei` 把兩邊都換成時間點再比對，HTTP 回應本身並沒有帶時區。
- **`LocalDateTime` 代表的意思沒有寫在任何文件裡。** 從執行器的換算和範例來看，它是台北時間的牆上時刻。`application.yml` 設了 `hibernate.jdbc.time_zone: UTC`，讓它在每一台機器上存取都一樣，但這不改變它不帶時區的性質。
- **帶時間的 `detail`**：
  - `ASSIGNED`：`ReviewCaseAssignedEvent`，`dueAt` 的寫法是 `LocalDateTime.toString()`。
  - `REASSIGNED`：`ReviewCaseReassignedEvent`，寫法相同。
  - 其他 `detail` 不帶時間。
- **前端**：Angular 送出和顯示時間的方式還沒有查證（見第八節）。
- **治理面**：`decision-log.md` 是最高權威（`repo-hard-rules.md` 第 1 節），但它目前只有個案裁決，沒有「後續裁決不得牴觸」的條文。

## 三、T-1：條文內容

### 建議

> **憲法第一條（日期時間）**
> 1. 凡是表示某一時刻的值，跨出程式邊界時一律使用 ISO-8601 延伸格式，帶時區位移（`Z` 或 `±hh:mm`），而且秒必須寫出。程式邊界包括 HTTP 請求與回應、事件的欄位、稽核 `detail`、匯出、規格範例、資料庫以外的任何儲存或傳遞。
> 2. 輸出一律正規化為 UTC（`Z`）。輸入接受任何位移；不帶時區的輸入一律拒絕（`VALIDATION_ERROR`）。
> 3. 程式內部以時間點表示（Java 用 `Instant`）。業務上需要「當地日期」的地方（例如「某日結束前」），一律以 `Asia/Taipei` 換算，換算的位置必須在裁決中寫明。
> 4. 只有日期、沒有時刻的值（`YYYY-MM-DD`）不受第 1 點約束，但必須由裁決明文指定，並寫明以哪個時區解讀。

### 選項

- **A（建議）：輸出一律 UTC `Z`。** 與 D-037、`Instant` 預設的序列化結果一致，程式最單純。顯示成台北時間是前端的工作。
- **B：輸出一律 `+08:00`。** 與 api-spec、domain-events 的範例一致，人讀起來方便，但 `Instant` 需要額外設定序列化。
- **C：只規定要帶時區，位移不限。** 最寬鬆，但同一個時刻可以有多種寫法，字串比對（例如稽核 `detail`）就不穩定。

### 待負責人決定

A／B／C？第 2 點拒絕不帶時區的輸入是否同意？

## 四、T-2：憲法節的結構與位階

### 建議

- 在 `decision-log.md` 開頭（第一條裁決之前）新增一節 `## 憲法草案`。內容包括：
  - 前言：「本節是全專案原則，位階高於本檔所有個案裁決。後續裁決不得牴觸；要牴觸，必須先修改本節的條文。本節預定提升為獨立的憲法文件。」
  - 條文編號用「第 N 條」，與 D-xxx 分開。
- 本條以一個個案裁決（D-072）頒布。D-072 記錄起因與決策；條文本身只放在「憲法草案」這一節，不在兩處重複。
- 之後修改條文時，同樣以新的 D-xxx 記錄，條文旁註明「由 D-xxx 修改」。

### 待負責人決定

同意這個結構嗎？前言的措辭要不要調整？

## 五、T-3：現有違憲的處理

違反本條的程式與文件已經不少（第二節）。

### 選項

- **A（建議）：違憲清單，分批修正。** D-072 列出目前已知的違憲處，修正的順序另行裁決。在修好之前：
  - 新寫的程式與規格必須合憲；
  - 既有的違憲處暫時容許，但不得擴大；
  - 每修正一處，就從清單上刪除。

  做法比照 D-060 的 `grandfathered`。
- **B：一次修完。** `LocalDateTime` 遍及 42 個檔案，還涉及 HTTP 契約、前端與資料庫，一個 PR 做不完，審查風險高。

### 建議的清單與順序

1. 稽核 `detail` 的 `ASSIGNED`、`REASSIGNED`：`dueAt` 改成帶時區（見 T-4）。
2. HTTP 的輸入（指派、改派的 `dueAt`）：拒絕不帶時區的值。需要先確認前端送出的格式。
3. HTTP 的輸出：view 的時間改成時間點，輸出 UTC `Z`。
4. domain 與 persistence：`ReviewCase` 的六個 `LocalDateTime` 改成 `Instant`。要寫 migration，把既有值依 `Asia/Taipei` 換算；正式環境還沒上線，D-045 說正式資料庫從 V1 起沒有資料。
5. `spec-profile.yaml` 的 `local_time_zone`：改成只用於第 3、4 點的業務換算，不再用來解讀不帶時區的時間。
6. 文件：api-spec §3.3 改成引用憲法第一條，範例改成 UTC；domain-events 的範例比照修改。

### 待負責人決定

A／B？順序是否同意？

## 六、T-4：稽核 `detail` 與 D-066／D-069「`detail` 不變」

### 事實

- D-066 第 2 點和 D-069 第 4 點要求重構時 `action`、`detail` 逐字不變，目的是不讓重構改動稽核紀錄。
- 合憲的 `detail` 必須帶時區，所以 `ASSIGNED`、`REASSIGNED` 的 `detail` 一定會改變。

### 建議

- D-072 明文規定：為了合憲而修改 `detail` 的時間表示，不受「`detail` 不變」限制。其他內容仍然逐字不變。
- 已經寫入的稽核紀錄不補改，因為稽核紀錄只增不改。修正之後，同一種 action 會有新舊兩種格式並存。這一點要寫進 api-spec 14.1、14.2。
- `ASSIGNED` 的 `detail` 依第 3 點換算：`dueAt` 是台北的牆上時刻，以 `Asia/Taipei` 換成時間點，輸出 UTC。以 D-071 的範例來說，就是 `"dueAt":"2026-11-26T23:59:59Z"`。這正是 D-071 範例原本寫的值。

### 待負責人決定

同意例外與新舊格式並存嗎？

## 七、T-5：D-071 那兩個範例（即先前的 Q-2）

依 T-4 的換算，範例預期的 `"2026-11-26T23:59:59Z"` 是合憲的值，錯的是程式。所以**規格不用改**。

### 選項

- **A（建議）：D-071 的建構 PR 一併修正 `ASSIGNED` 的 `detail`。** 也就是第五節清單的第 1 項。
  - 這要等 D-072 合併之後才能做。
  - `REASSIGNED` 屬於改派，還沒有立法，留在違憲清單。
  - `SliceEventPropertiesTest` 的預期改成帶時區。
- **B：D-071 的建構 PR 先不動 `detail`，把兩個範例列進已知失敗清單。** `detail` 另開 PR 修。這要多一次修法（新增已知失敗）。

### 待負責人決定

A／B？

## 八、裁決後的順序

1. **修法 PR（D-072）**：
   - `decision-log.md` 新增「憲法草案」一節（第一條）與 D-072，包含違憲清單；
   - api-spec §3.3 改成引用第一條；
   - 本草稿標為已裁決。

   程式不動。
2. **D-071 建構 PR**（T-5 選 A 時）：
   - 原有的遮罩程式；
   - `ReviewCaseAssignedEvent` 的 `detail` 改成帶時區。
3. **之後**：違憲清單第 2～6 項，各自另開裁決或 PR。

### 還沒有查證的

- 前端送出 `dueAt` 的格式（Angular 的 date picker 或 `toISOString()`）。這會決定清單第 2 項會不會影響前端。
- `dueAt` 的業務意思：它是「某日結束前」（日期），還是一個確切的時刻？如果是日期，依第一條第 4 點，可能應該改成 `YYYY-MM-DD`，而不是時間點。這要另案裁決，本條不決定。
- 匯出的 `closedAt`（`LocalDateTime`）與 `createdAt`（`Instant`）放在同一列，格式不一致。屬於清單第 3 項。
