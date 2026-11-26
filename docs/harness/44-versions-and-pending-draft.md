# 專案版本管理與未裁決事項

狀態：**已裁決**（2026-11-26）。負責人裁決：全部照建議。版本管理、多角色、`latestRevisionSummary` 的條文是 D-094，逾期是 D-095，以 `decision-log.md` 為準。下面保留起草時的版本（main = `8a5e26b`）。本文件不是律法；題號只在本文件使用。

題目清單待辦第 1 題。負責人 2026-11-26 先定的方向：

| 件 | 方向 |
|---|---|
| 專案版本管理 | 先不開放改版：發布後專案與評分表都不再改，要改就建新專案 |
| 逾期 | 重新定義後立法：看目前階段的期限 |
| 多角色的讀取授權 | 維持拒絕，寫成定案 |
| `latestRevisionSummary` | 從 SYSTEM_ADMIN 的白名單移除 |

本草稿把四件的細節列成待裁決。四件互不相關，第五節建議分兩批。

## 一、專案版本管理（V-1～V-3）

### 查到的事實（讀程式與規格，沒有實測）

1. **「發布後不能改」已經立法**：專案的更新、綁定評分表、發布在 `currentVersion.published = true` 時都拒絕（`specs/review-project/usecases.yaml` 第 128、174、204 行）；評分表的更新、發布在 `PUBLISHED` 時拒絕（`specs/score-sheet/usecases.yaml` 第 140、234 行）；一個專案只能有一份評分表（第 107 行），而且只有 `DRAFT` 的專案能建立評分表（第 102 行）。
2. **沒有被呼叫的程式**：`ReviewProject.prepareDraftVersion()`（第 255 行）與 `ProjectVersion.nextDraft()`（第 87 行）只有單元測試在用（`ReviewProjectTest`、`ProjectVersionTest`、`ReviewProjectIntegrationTest`）。D-068 第 4 點把 `ExecuteSamplingUseCase` 的「專案目前的版本未發布」檢查留作防線，等 `prepareDraftVersion` 立法時再決定。
3. **待立法清單裡跟版本有關的**：
   - `score-sheet/model.yaml`：`freezeVersion`（`FROZEN` 狀態未定義）、`snapshotForSubmission`、建立新的草稿版本。
   - `review-case/model.yaml`：品質線與權重取自專案目前的版本，不是案件建立時的版本（D-069）。
   - `sampling-batch/model.yaml`：快照沒有被任何行為使用，案件的評分表版本、品質線與權重取自專案目前的版本（D-068 第 4 點、D-086 第 8 點）。
4. **前端**：沒有「準備新版本」或「建立新版本」的入口。

### V-1　「先不開放改版」怎麼寫進律法

- **A（建議）**：裁決寫明「專案與評分表發布後不再改版；要改就建立新的專案」。第 1 項的規則已經涵蓋行為，不加新規則。`score-sheet` 的三項待立法改成本裁決「不做」，從 `deferred_behaviors` 移除。`review-case` 的那一項改寫成定案：因為發布後的版本不會再變，「目前的版本」就是案件建立時的版本，兩者沒有差別。範例做不出差別，所以不加範例。
- B：同 A，另外在 review-case 加一條不變式規則（「計分用的品質線與權重等於案件建立時專案已發布的版本」），列進 `not_covered`。

### V-2　沒有被呼叫的程式

- **A（建議）**：建構 PR 刪除 `prepareDraftVersion`、`nextDraft` 與它們的單元測試。抽選的「版本未發布」檢查保留作防線。
- B：保留，加註「D-094 不開放改版」。

### V-3　抽選的快照

- **A（建議）**：`sampling-batch` 的「快照沒有被任何行為使用」改寫理由：不開放改版之後，快照不需要用來鎖定版本；快照的內容與保存方式仍等 D-011（留在 `deferred_behaviors`）。
- B：既然不需要，連快照一起不做。

## 二、逾期（O-1～O-6）

### 查到的事實（讀程式，沒有實測）

1. 現在的算法（`GetDashboardSummaryUseCase.java` 第 79 行）：`dueAt < 現在` 而且狀態不是 `CLOSED`（`CANCELLED` 已經不算進任何件數，D-087 第 4 點）。`dueAt` 是初審期限；複審期限另存在 `rereviewer_due_at`，沒有用到。「現在」取注入的時鐘（D-092 第 5 點）。
2. 狀態與誰在等：

   | 狀態 | 在等誰 | 期限 |
   |---|---|---|
   | `SELECTED_PENDING_ASSIGNMENT` | 管理者指派 | 沒有 |
   | `PENDING_INITIAL_REVIEW`、`INITIAL_REVIEW_IN_PROGRESS` | 初審醫師 | 初審期限 |
   | `RETURNED`（從 `INITIAL_REVIEW_IN_PROGRESS` 退回） | 管理者重開 | 沒有新的期限 |
   | `INITIAL_REVIEW_COMPLETED` | 管理者固定或送複審 | 沒有 |
   | `PENDING_REREVIEW` | 管理者指派複審，或複審醫師開始 | 指派之後才有複審期限 |
   | `REREVIEW_IN_PROGRESS` | 複審醫師 | 複審期限 |
   | `CLOSED`、`CANCELLED` | — | — |

3. 重開（`RETURNED` → `PENDING_INITIAL_REVIEW`）不改初審期限。
4. 匯出的 `summary` 是同一個範圍的儀表板摘要（RC-RPT-12，D-087 第 8 點），所以跟著改。
5. 權威文件只有「逾期率」這個名詞（規格草案第 220、392 行），沒有定義。`review-case/examples.yaml` 的 `not_covered` 也寫「權威文件沒有已裁決的逾期規則」。

### O-1　定義

- **A（建議）**：逾期 = 案件在初審階段（`PENDING_INITIAL_REVIEW`、`INITIAL_REVIEW_IN_PROGRESS`）而初審期限已過，或在複審階段（`PENDING_REREVIEW`、`REREVIEW_IN_PROGRESS`）、已指派複審醫師，而複審期限已過。其他狀態都不算逾期。
- B：同 A，另外把 `RETURNED`、`INITIAL_REVIEW_COMPLETED`、還沒指派複審的 `PENDING_REREVIEW` 也算（看初審期限）。

### O-2　邊界

- **A（建議）**：期限「早於」現在才算逾期，剛好等於不算（與現況相同）。

### O-3　重開後的期限

- **A（建議）**：維持現況，重開不改期限；期限已過的案件重開後立刻算逾期，管理者可以用改派給新的期限。
- B：重開時要輸入新的期限（新增輸入與規則）。

### O-4　比率

- **A（建議）**：`overdueRate` = 逾期件數 ÷ `totalCases`，`totalCases` 是 0 時是 0（與 D-087 第 6 點的其他比率相同）。

### O-5　範例

- **A（建議）**：每一種狀態至少一個範例，用 `advance` 讓期限過去；加一個剛好等於期限的範例。修法 PR 時凍結，建構 PR 解凍。
- B：只寫幾個代表性的範例。

### O-6　其他出口

逾期只出現在儀表板與匯出的 `summary`。其他地方（案件清單、詳情）不顯示逾期，本題不新增。

## 三、多角色的讀取授權（M-1）

### 查到的事實

1. 規格已經立法為拒絕：例如 `RC-RVH-01`「操作者同時具備兩個以上的角色 → `ACCESS_DENIED`」（`status: decided`，來源 D-035、D-055、D-084）；案件詳情、提交明細同樣。
2. 沒有定案的是說法：D-035 第 5 點寫「未定多角色合成不放寬」，D-055 第 7 點寫「多角色同時持有時仍拒絕（未裁決，維持 D-035 第 5 點）」，`permission-matrix.md` 第 185 行寫「多角色 actor 的技術授權合成 unknown」，程式的訊息是 `Multi-role read authorization is pending confirmation`（`ReviewReadAccessPolicy.java` 第 57 行）。
3. 稽核的兩個查詢不拒絕多角色，而是只要持有 `REREVIEWER` 或 `SYSTEM_ADMIN` 就整批遮罩（RC-LCY-01、RC-AUD-01）。

### M-1　寫成定案

- **A（建議）**：裁決寫明「同時持有兩個以上角色的使用者，讀案件內容（詳情、提交明細、修正歷史）一律拒絕；這是定案，不是暫時的作法」。`permission-matrix.md` 改寫第 185 行；建構 PR 把程式訊息改成不帶「pending」。稽核的遮罩不變。行為沒有變更，不加範例。

## 四、`latestRevisionSummary`（L-1、L-2）

### 查到的事實

1. 出現在：`RC-DTV-04` 的白名單、`specs/review-case/http.yaml` 第 139 行（`'null'`）、`api-spec.md`（第 76、997、2060、2062 行）、D-036、D-037 的八欄白名單、程式 `CaseDetailMetadataView.java` 第 29 行。前端沒有使用。
2. **實測**（本機，用完已還原）：在 `http.yaml` 的案件詳情加一種沒有這個欄位的形狀，完整執行測試，閘門擋下：「`GET /review-cases/{caseId}`（metadata-without-summary）：沒有觀察到這種形狀的成功回應」。HTTP 契約是封閉的對應表，多一個欄位、少一個欄位都不符；涵蓋要求每種形狀都觀察到。HTTP 契約也沒有已知失敗的清單。
3. 所以**移除一個欄位沒有「修法 PR → 建構 PR」的路**：修法 PR 先改契約，程式還在輸出這個欄位，契約不符；建構 PR 先改程式，契約還要求這個欄位，同樣不符。修法 PR 不能夾帶程式（P4）。

### L-1　移除的範圍

- **A（建議）**：`RC-DTV-04` 的白名單、`http.yaml`、`api-spec.md`、程式一起移除；D-036、D-037 的八欄改成七欄（以本裁決更正）。範例 `admin_reads_case_metadata_only` 改用 `only` 列出七欄。

### L-2　怎麼過渡

| 選項 | 作法 | 代價 |
|---|---|---|
| **A（建議）** | 先修閘門：HTTP 契約的形狀可以標「建構中」（例如 `pending: D-094`），涵蓋不檢查這種形狀、但回應仍要符合其中一種。用法：修法 PR 加一種標「建構中」的新形狀；建構 PR 改程式並拿掉標記；最後一個修法 PR 刪掉舊的形狀 | 改的是閘門（`acceptance/http/`，律法），要另一組修法 PR；之後任何欄位的增刪都能用 |
| B | 不移除，回到「維持 null，寫明未定」 | 與方向不同 |
| C | 由負責人裁決一次例外：同一個 PR 改契約與程式 | `law-check` 是必要檢查，P4 會擋，合併不了 |

## 五、分批

四件互不相關，建議分兩批：

1. **第一批（D-094）：版本管理、多角色、`latestRevisionSummary`**。大多只改文件；程式只有刪除沒用的程式碼與改訊息。`latestRevisionSummary` 看 L-2：選 A 時先做閘門的修法，再照 L-2 A 的順序；選 B 時只改文件。
2. **第二批（D-095）：逾期**。新的規則與範例（凍結）→ 建構 PR。

## 六、建議總表

| 題 | 建議 |
|---|---|
| V-1 | A：裁決寫明不改版；三項待立法改成不做；review-case 那一項改寫成定案 |
| V-2 | A：刪除沒有被呼叫的程式，抽選的防線保留 |
| V-3 | A：快照留在待立法，改寫理由 |
| O-1 | A：看目前階段的期限 |
| O-2 | A：早於現在才算 |
| O-3 | A：重開不改期限 |
| O-4 | A：與其他比率相同 |
| O-5 | A：每種狀態一個範例，加邊界 |
| M-1 | A：寫成定案，改訊息 |
| L-1 | A：全部移除，白名單改成七欄 |
| L-2 | A：先讓閘門支援「建構中」的形狀 |
| 分批 | 兩批 |
