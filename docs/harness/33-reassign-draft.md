# ReviewCase 切片 A：改派初審醫師、改派複審醫師

狀態：**已裁決**（2026-11-24）。負責人對 Z-1～Z-7 全部照建議裁決，並表示日後有必要再調整；條文是 `decision-log.md` 的 D-082，以那裡為準。下面保留起草時的版本（main = `bfd9cf8`）。本文件不是律法；題號 Z-1～Z-7 只在本文件使用。

負責人 2026-11-24 同意 ReviewCase 未立法範圍切成五個切片，順序 C＋B（D-080，已完成）→ **A 改派** → D 修正 → E 讀取。

## 一、範圍

| use case | 端點 | 角色 | 狀態條件（程式） | 事件（action） | 立法狀態 |
|---|---|---|---|---|---|
| `ReassignReviewCase` | `POST /review-cases/{caseId}/reassign-reviewer`，body `reviewerId`（`@NotBlank`）、`dueAt`（`@NotNull`，D-073 帶時區） | `HOSPITAL_MANAGER`，本院 | `PENDING_INITIAL_REVIEW`，否則 409 `CASE_NOT_PENDING_INITIAL_REVIEW` | `ReviewCaseReassigned`（`REASSIGNED`） | 未立法 |
| `ReassignRereviewer` | `POST /review-cases/{caseId}/reassign-rereviewer`，body `rereviewerId`（`@NotBlank`）、`dueAt`（`@NotNull`） | 同上 | `PENDING_REREVIEW`，否則 422 `CASE_NOT_ASSIGNABLE` | `RereviewerAssigned`（`REREVIEWER_ASSIGNED`，`detail` 帶 `"reassignment":true`） | use case 未立法；事件已立法（D-070） |

一併登記的未立法元件：`ReviewCaseReassignedEvent`、`command/ReassignRereviewerCommand.java`。依賴的 `RereviewerAssignmentHistoryPort`、`InitialReviewReaderPort` 已由指派複審醫師（D-069）使用。

## 二、查到的事實

**實測（2026-11-24，暫時的規格範例，走真實寫入與查詢路徑，跑完已還原，沒有提交）**：

| # | 情境 | 結果 |
|---|---|---|
| 1 | 待初審，改派給 u-r2 | 成功；稽核 `REASSIGNED`，`detail` `{"previousReviewerId":"u-r1","newReviewerId":"u-r2","dueAt":"2026-11-28T23:59:59Z"}` |
| 2 | 待初審，改派給原本的 u-r1 | 成功（等於只改期限，仍記 `REASSIGNED`） |
| 3 | 待初審，`reviewerId` 是空字串（直接執行 use case） | **成功**，案件的初審醫師變成空字串 |
| 4 | 待指派（`SELECTED_PENDING_ASSIGNMENT`）改派 | 409 `CASE_NOT_PENDING_INITIAL_REVIEW` |
| 5 | 複審中改派複審醫師（HTTP） | 422 `CASE_NOT_ASSIGNABLE` |
| 6 | 已指派 u-rr1，改派給 u-rr2（HTTP） | 成功；稽核 `REREVIEWER_ASSIGNED`，`detail` `{"rereviewerId":"u-rr2","reassignment":true}`（沒有 `dueAt`、沒有原本的複審醫師） |
| 7 | 改派複審醫師給初審醫師 u-r1 | 422 `REREVIEWER_SAME_AS_REVIEWER` |
| 8 | 待初審時改派複審醫師 | 422 `CASE_NOT_ASSIGNABLE` |
| 9 | 待複審但**還沒指派**複審醫師，改派 | **成功**，記成 `reassignment:true`，複審指派歷史記成「改派」 |
| 10 | 改派複審醫師給原本的 u-rr1 | 成功（歷史多一筆） |

**讀程式確認**：

- 改派後，原本的醫師不能開始初審／複審（`NOT_ASSIGNED_REVIEWER`、`NOT_ASSIGNED_REREVIEWER`）。
- `ReassignReviewCase.execute(actor, caseId, newReviewerId, dueAt)`：輸入名稱是 `newReviewerId`，HTTP 與指派用 `reviewerId`。
- `ReassignRereviewer.execute(actor, cmd)` 收 command，規格範例無法執行（同 D-080 的 `CloseWithoutRereview`）。
- 兩個改派在 domain 都沒有輸入檢查（指派有，D-067、D-069）；`dueAt` 是 null 也會成功，只靠 HTTP 的 `@NotNull` 擋。
- 改派複審醫師先查「讀過初審」（D-055），再查狀態；指派複審醫師也是這個順序（已立法）。
- `ReviewCaseReassignedEvent` 沒有 getter；`detail` 以 `String.format` 組成（ID 不含引號，實際上沒有跳脫問題）。
- 複審中不得改派複審醫師，D-055 已決定（「案件進入 `REREVIEW_IN_PROGRESS`、`CLOSED` 或 `CANCELLED` 之後，就不可能再有新的複審醫師」）；`permission-matrix.md` 第 9 節待確認第 2 點還沒標結案。
- 文件缺口：`state-machine.md` 轉移表沒有改派；`domain-events.md` 沒有 `ReviewCaseReassigned`；`api-spec.md` §10.4 沒有錯誤代碼。

## 三、Z-1：改派初審醫師的狀態錯誤

- **A（建議）：** 改成 422 `CASE_NOT_ASSIGNABLE`，與指派初審、指派／改派複審一致（D-069 第 2 點同樣的理由）；`CASE_NOT_PENDING_INITIAL_REVIEW` 停用。
- **B：** 維持 409 `CASE_NOT_PENDING_INITIAL_REVIEW`，規格新增這個代碼。

## 四、Z-2：輸入檢查

- **A（建議）：** 兩個改派都在狀態檢查之前檢查醫師與 `dueAt` 必填，空白回 400 `VALIDATION_ERROR`（同 D-067、D-069）。修正實測 #3。

## 五、Z-3：改派給同一個人

- **A（建議）：允許。** 這是唯一能只改期限的方式；事件照記（`REASSIGNED`，前後醫師相同；複審指派歷史多一筆）。
- **B：拒絕**（新增錯誤代碼）。要改期限就沒有入口。

## 六、Z-4：還沒指派複審醫師就改派

- **A（建議）：拒絕**，422 `NO_REREVIEWER_ASSIGNED`（開始複審已用的代碼）。改派要有原本的人；否則歷史記成「改派」不正確。
- **B：允許**（照程式），當成指派處理。

## 七、Z-5：改派複審醫師的事件

- **A（建議）：照現況。** 沿用 `RereviewerAssigned`（`REREVIEWER_ASSIGNED`），`detail` 帶 `"reassignment":true`；事件加上 `reassignment` 屬性（D-070 第 1 點留到改派立法時決定）。
- **B：** 新增 `RereviewerReassigned`（action `REREVIEWER_REASSIGNED`，帶原本與新的複審醫師），與初審側對稱。稽核與案件歷程讀取要跟著改。

## 八、Z-6：改派初審醫師的事件

- **建議：** `ReviewCaseReassigned` 帶 `caseId`、`previousReviewerId`、`newReviewerId`、`dueAt`（照現在的 `detail`）；`detail` 改以 JSON 序列化產生（同 D-080 第 5 點）。不帶分數，不列入 D-071 的遮罩清單。

## 九、Z-7：檢查順序與輸入名稱

- **建議：**
  - 改派複審醫師的順序：輸入 → 狀態 → 尚未指派（Z-4）→ 與初審醫師相同 → 讀過初審。狀態不對時先回 `CASE_NOT_ASSIGNABLE`，不因讀取紀錄回 `REREVIEWER_HAS_READ_INITIAL`。指派複審醫師（已立法）一併改成同樣順序。
  - use case 輸入名稱用 `reviewerId`、`rereviewerId`（與指派、HTTP 一致）；`ReassignRereviewer` 改收參數，`ReassignRereviewerCommand` 移除。
  - D-081 的範例 `restarts_with_a_new_draft_after_reopen_and_reassign` 隨之改用 `reviewerId`、移除 `needs`，由本切片的建構 PR 轉綠。
