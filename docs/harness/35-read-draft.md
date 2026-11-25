# ReviewCase 切片 E：讀取（案件詳情、提交明細、讀取授權、分派頁查詢）

狀態：**已裁決**（2026-11-25）。負責人對 V-1～V-11 全部照建議裁決；條文是 `decision-log.md` 的 D-084，以那裡為準。下面保留起草時的版本（main = `5cbc209`）。本文件不是律法；題號 V-1～V-11 只在本文件使用。

## 一、範圍

| use case | 端點 | 角色 | 立法狀態 |
|---|---|---|---|
| `GetReviewCaseDetail` | `GET /review-cases/{caseId}` | `HOSPITAL_MANAGER`、`REVIEWER`、`REREVIEWER`、`AUDITOR`、`SYSTEM_ADMIN` | 未立法 |
| `GetSubmissionDetail` | `GET /review-submissions/{submissionId}` | 同上 | 未立法 |
| `GetCaseAssignmentContext` | `GET /review-cases/{caseId}/assignment-context` | `HOSPITAL_MANAGER` | 未立法 |
| `ListCaseAssignmentCandidates` | `GET /review-cases/{caseId}/assignment-candidates?role=&keyword=` | `HOSPITAL_MANAGER` | 未立法 |

一併處理的元件：`ReviewReadAccessPolicy`、`CaseDetailMetadataMapper`、`InitialReviewReaderPort`、`RereviewerAssignmentHistoryPort`、`RereviewerAssignmentRecord`，以及 `query/` 裡這四個查詢用到的 view 與 port。`GetRevisionHistory` 已立法，但與本切片共用 `ReviewReadAccessPolicy`。

另有兩件從前面的切片留下來：D-083 第 10 點的三個欄位（V-9），以及 D-083 建構 PR 新增的 `RevisionChecks.java`（V-11）。

## 二、查到的事實

**實測（2026-11-25，暫時的規格範例，走真實寫入與查詢路徑，跑完已還原，沒有提交）**。除非另外註明，案件都在「複審已提交」（`rereview_submitted`：初審 20 分、複審 20 分）。

| # | 情境 | 結果 |
|---|---|---|
| 1 | 管理者讀案件詳情 | 成功；`projectionType = FULL`、`initialReviewScore = 20`、`finalScore = 20.00`；`finalConclusion = null`；輸出沒有 `rereviewRequired`、`updatedAt`、`returnReason`、`cancelReason` |
| 2 | 複審醫師讀案件詳情 | 成功；沒有 `initialReviewScore`、`finalScore`；`submissions` 裡初審那筆的 `totalScore = null`；`rereviewContext` 沒有 `initialTotalScore`、`finalScore`，但有 `initialReviewerId = u-r1` 與初審提交的 ID、版本、提交時間；`pageMode = REVISION`、`allowedActions = [REVISE_REREVIEW, CLOSE_REREVIEW]` |
| 3 | SYSTEM_ADMIN 讀案件詳情 | 成功；只有 metadata 白名單：`caseId`、`caseStatus`、`projectName`、`hospitalId`、`reviewerId`、`rereviewerId`、`updatedAt`、`latestRevisionSummary`（固定 `null`） |
| 4 | 同時有 REVIEWER 與 REREVIEWER 的人 | 403 `ACCESS_DENIED` |
| 5 | 沒有受指派、也沒有提交過的初審醫師 | 403 `ACCESS_DENIED` |
| 6 | 別院的管理者 | 403 `HOSPITAL_SCOPE_MISMATCH` |
| 7 | 案件不存在 | 404 `CASE_NOT_FOUND` |
| 8 | 還沒受派的複審醫師（`PENDING_REREVIEW`） | 403 `ACCESS_DENIED` |
| 9 | 被改派掉的前任複審醫師（沒有提交過） | 403 `ACCESS_DENIED` |
| 10 | u-rr1 曾受派複審，之後以 AUDITOR 身分讀案件詳情 | 403 `ACCESS_DENIED`（歷史複審醫師，D-055） |
| 11 | u-rr1 以 AUDITOR 身分先讀案件詳情（`PENDING_REREVIEW`），之後被指派為複審醫師 | 讀取成功；指派 422 `REREVIEWER_HAS_READ_INITIAL` |
| 12 | 初審醫師讀案件詳情（`INITIAL_REVIEW_COMPLETED`） | 成功；`initialReviewContext.pageMode = REVISION`、`allowedActions = [REVISE_INITIAL_REVIEW, FIX_INITIAL_REVIEW]`；也有 `rereviewContext`（`ROLE_DENIED`，內含 `initialTotalScore = 20`、`finalScore = 20`） |
| 13 | 稽核人員讀案件詳情 | 成功；`initialReviewScore = 20`；兩個 context 都是 `ROLE_DENIED` |
| 14 | 複審醫師讀結案後的案件詳情 | 成功；`rereviewContext.pageMode = READONLY_BLOCKED`、`blockedReason = CASE_ALREADY_CLOSED` |
| 15 | 複審醫師讀初審提交明細 | 成功；`answers`、`comment`、`totalScore` 是 `null`，`maskedFields = [answers, comment, totalScore, attachmentRefsJson]` |
| 16 | 複審醫師讀自己的複審提交明細 | 成功；`totalScore = 20`、`comment = 複審評語`，沒有 `maskedFields` |
| 17 | 另一位（沒受派的）複審醫師讀複審或初審提交明細 | 403 `ACCESS_DENIED` |
| 18 | 初審醫師讀複審提交明細 | 成功；`totalScore = 20`、評語都看得到 |
| 19 | 管理者讀初審提交明細 | 成功，完整內容 |
| 20 | SYSTEM_ADMIN 讀提交明細 | 成功；內容遮罩，但輸出還有 `caseId`、`createdAt` |
| 21 | 提交不存在 | 404 `SUBMISSION_NOT_FOUND` |
| 22 | 別院的管理者讀提交明細 | 403 `HOSPITAL_SCOPE_MISMATCH` |
| 23 | 分派頁：退回（`RETURNED`）的案件 | 成功；`pageMode = REASSIGN_INITIAL`、`allowedActions = [REASSIGN_REVIEWER]`、沒有 `blockedReason` |
| 24 | 照分派頁的 `allowedActions` 改派退回的案件 | 422 `CASE_NOT_ASSIGNABLE`（D-082：改派只在 `PENDING_INITIAL_REVIEW`） |
| 25 | 分派頁：已指派複審醫師（`PENDING_REREVIEW`） | 成功；`pageMode = MIXED_ASSIGNMENT`、`allowedActions = [REASSIGN_REREVIEWER]`、`effectiveDueAt` 是複審期限、`currentRereviewer.userId = u-rr1` |
| 26 | 稽核人員讀分派頁 | 403 `ROLE_DENIED` |
| 27 | 別院的管理者讀分派頁 | 403 `HOSPITAL_SCOPE_MISMATCH` |

**讀程式確認**：

- `ReviewReadAccessPolicy` 的檢查順序：多角色（403）→ 院區（SYSTEM_ADMIN 不限）→ 角色與指派／提交範圍 → 複審醫師直接放行（只給盲審投影）→ 固定案件版本 → 歷史身分（D-055）→ 寫入讀取者紀錄。多角色的檢查在院區之前，所以別院的雙角色使用者拿到 `ACCESS_DENIED`，不是 `HOSPITAL_SCOPE_MISMATCH`。已立法的 `GetRevisionHistory` 也一樣，它的規格把院區（RC-G-03）放在多角色（RC-RVH-01）之前，目前沒有範例涵蓋這個組合。
- 完整讀取會寫入讀取者紀錄，並遞增案件版本（D-055 第 5 點），所以兩個詳情查詢是可寫入的交易。案件詳情、提交明細、revision 歷程都會寫入。
- `ListCaseAssignmentCandidates` 的候選人 `blockedReason` 只有 `CANDIDATE_INACTIVE`、`INITIAL_ASSIGNMENT_NOT_ALLOWED`、`REREVIEW_ASSIGNMENT_NOT_ALLOWED`、`REREVIEWER_SAME_AS_REVIEWER`；讀過完整初審的人仍然顯示可選，選了之後指派才回 422 `REREVIEWER_HAS_READ_INITIAL`。候選人狀態的判斷與分派頁的 `allowedActions` 是兩份重複的程式（退回的案件也一樣列出 `REASSIGN_REVIEWER`）。
- 案件詳情對每一個角色都組出 `initialReviewContext` 與 `rereviewContext`（不是自己的角色就是 `ROLE_DENIED`）。複審醫師的那份已先遮罩，所以沒有初審分數；其他角色的 `rereviewContext` 內含初審分數與 `finalScore`，這些角色本來就看得到。
- 複審醫師的案件詳情 `projectionType` 仍是 `FULL`。D-033 規定「不預設新 projection enum」，所以沒有另外的值。
- D-083 第 10 點的三個欄位：
  - `rereviewRequired`：案件詳情沒有輸出；只在提交初審的 HTTP 回應出現，且一律是 `false`。已立法的 `SubmitInitialReview` 輸出沒有這個欄位。前端的型別有它。
  - `finalConclusion`：案件詳情與複審頁 bootstrap 都輸出，一律是 `null`。前端案件詳情頁會顯示它，`null` 時退回顯示 `FINAL`。
  - `ruleSnapshotRef`：沒有任何輸出。
  - 三者都有資料表欄位（`V1__baseline_schema.sql`）。正式環境還沒上線（D-045）。
- 稽核欄位 `updatedAt`（D-037）：SYSTEM_ADMIN 的案件詳情有輸出；其他角色沒有。`model.yaml` 的 `deferred_behaviors` 寫「本次範圍內沒有輸出它的查詢」，切片 E 立法後就有了。
- `api-spec.md` §10.2 的案件詳情只有 metadata／full 兩層與 `latestRereviewSubmissionId` 的範例，沒有逐欄的契約；§13.1 的提交明細有遮罩範例，但沒有 `attachmentRefsJson`。

## 三、讀取授權

### V-1：多角色與院區的順序

- **A（建議）：** 照 D-024 的順序（角色 → 載入 → 院區 → 規則），院區先於多角色。別院的雙角色使用者改回 `HOSPITAL_SCOPE_MISMATCH`。`GetRevisionHistory` 的程式一併改成符合它已立法的順序。
- **B：** 照程式，多角色先擋。`GetRevisionHistory` 的規格要改成多角色在院區之前。

### V-2：初審醫師看得到複審的內容

實測 #12、#18：初審醫師在案件詳情看得到複審分數與 `finalScore`，也能讀複審提交的分數與評語。D-033 只保護複審醫師（不得看初審）；permission-matrix §5 給 REVIEWER「受指派與本人提交範圍內的 FULL」。

- **A（建議）：** 照程式，立法為現行行為。
- **B：** 初審醫師看不到複審的分數、評語與 `finalScore`（新的遮罩規則，需要另外的理由）。

### V-3：複審醫師看得到初審醫師是誰

實測 #2：複審醫師的案件詳情有 `reviewerId`、`initialReviewerId`，以及初審提交的 ID、版本、提交時間，但沒有分數、答案、評語。D-033 列的是「答案、分數、評語與可反推的衍生資訊」，沒有包含身分；複審頁目前會顯示初審醫師。

- **A（建議）：** 照程式，身分與提交的 metadata 可見，分數、答案、評語、`finalScore` 不可見。
- **B：** 連初審醫師的身分也遮罩（需要另外裁決，並改複審頁）。

### V-4：SYSTEM_ADMIN 的提交明細

實測 #20：遮罩了內容，但輸出還有 `caseId`、`createdAt`。permission-matrix §5.1 的白名單是共用的 `caseId`、`caseStatus`、`projectName`、`hospitalId`、`reviewerId`、`rereviewerId`、`updatedAt`，加上 UI-014 的 `stage`、`revisionNo`、`submittedAt`、`submittedBy`、`isCurrentEffective`。

- **A（建議）：** 只輸出白名單裡有的欄位：`submissionId`、`caseId`、`stage`、`revisionNo`、`status`、`reviewerId`（即 `submittedBy`）、`submittedAt`；拿掉 `createdAt`。`maskedFields` 照舊。
- **B：** 照程式，把 `createdAt` 加進白名單。

## 四、分派頁

### V-5：退回的案件

實測 #23、#24：分派頁說可以改派，改派本身拒絕。退回的案件要先重開（D-080，`RETURNED` → `PENDING_INITIAL_REVIEW`），才能改派（D-082：改派只在 `PENDING_INITIAL_REVIEW`；D-081 是這個流程）。

- **建議：** `RETURNED` 的 `pageMode = READONLY_BLOCKED`、`blockedReason = CASE_NOT_ASSIGNABLE_IN_CURRENT_STATUS`、`allowedActions = []`；候選人清單相同（`INITIAL_ASSIGNMENT_NOT_ALLOWED`）。候選人與分派頁共用同一份可執行動作的判斷，不再各寫一份。

### V-6：讀過完整初審的候選人

- **A（建議）：** 複審醫師的候選人若在本案的初審讀取者紀錄中，`selectable = false`、`blockedReason = REREVIEWER_HAS_READ_INITIAL`，與指派的 422 一致。查詢不寫入紀錄，只讀。
- **B：** 照程式，候選人清單不檢查，選了才由指派拒絕。

## 五、規格與立法

### V-7：讀過完整初審就不得受派（D-055 第 5 點）

`model.yaml` 的 `deferred_behaviors` 與 `not_covered` 寫這條「範例做不出來」。實測 #11 證明以 `rr1_as_auditor_h1` 可以做出「先讀、後受派」。

- **建議：** `AssignRereviewer`、`ReassignRereviewer` 補上規則 `REREVIEWER_HAS_READ_INITIAL`（422，D-055 第 5 點）與範例；`deferred_behaviors` 與 `not_covered` 的對應項目移除。提交明細與 revision 歷程的讀取也會留下紀錄，各補一個範例。

### V-8：輸出欄位

- **建議：**
  - 案件詳情：FULL 投影照程式的欄位立法；複審醫師的投影以 `absent` 寫出不得出現的欄位（`initialReviewScore`、`finalScore`、初審提交的 `totalScore`、`rereviewContext.initialTotalScore`、`rereviewContext.finalScore`）；SYSTEM_ADMIN 的投影是第 3 項的白名單。
  - 兩個 context 的 `pageMode`、`blockedReason`、`allowedActions` 照程式立法（D-054 第 9 點的 `FIX_INITIAL_REVIEW`、D-032 結案後唯讀）。
  - 提交明細：遮罩時 `answers`、`comment`、`totalScore`、`attachmentRefsJson` 為 `null`，`maskedFields` 列出這四個；`api-spec.md` §13.1 補上 `attachmentRefsJson`。
  - `updatedAt`：SYSTEM_ADMIN 的案件詳情輸出它（D-037）；其他角色維持不輸出。`deferred_behaviors` 的 `updatedAt` 項目改寫。

### V-9：沒有被寫入的欄位（接 D-083 第 10 點）

- **A（建議）：** 移除 `rereviewRequired`、`finalConclusion`、`ruleSnapshotRef`：domain、讀取模型、HTTP 回應（提交初審的回應、案件詳情、複審頁 bootstrap）；以新的 Flyway migration 刪除三個欄位（D-045：正式環境還沒上線）。前端型別與案件詳情頁的 `finalConclusion` 顯示另開前端題目處理，後端先移除欄位，前端的 `null` 處理不受影響。`domain-model.md` 對應的欄位與 `completeRereview(…, finalConclusion)` 一併更新。
- **B：** 只從回應移除，保留 domain 與資料表。
- **C：** 不移除，留在 `deferred_behaviors`。

### V-10：登記範圍

- **建議：** 四個 use case 與第一節列的元件登記為已立法（`specs/review-case/`）。`query/` 裡只有這四個查詢用到的 view 與 port 一併登記；`ReviewCaseQueryCriteria`、`ReviewCaseSummary` 等列表用的，以及修正歷史用的 `RevisionHistoryEntry`、`RevisionHistoryView`，不在本切片。`interfaces/rest/ReviewCaseController.java` 仍是 grandfathered（留給 HTTP 介面的切片）。

### V-11：`RevisionChecks.java`

D-083 建構 PR（#159）新增，放修正初審與修正複審共用的必答題與沒有變更檢查（RC-RVI-08/09、RC-RVR-08/09），目前在 grandfathered 範圍。負責人 2026-11-25 決定在本切片的修法 PR 登記。

- **建議：** `law/coverage.txt` 登記為已立法（`specs/review-case/`）。

## 六、題目清單

本切片的修法 PR 一併把 `25-topic-backlog.md` 的 D-083 移到已完成。
