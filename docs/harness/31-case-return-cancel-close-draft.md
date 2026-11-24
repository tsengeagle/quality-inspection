# ReviewCase 切片：退回、重開、取消、不經複審結案

狀態：**已裁決**（2026-11-24）。負責人對 X-1～X-8 全部照建議裁決；條文是 `decision-log.md` 的 D-080，以那裡為準。下面保留起草時的版本（main = `4b768bd`）。本文件不是律法；題號 X-1～X-8 只在本文件使用。

負責人 2026-11-24 同意 ReviewCase 未立法範圍切成五個切片，順序 C → B → A → D → E，C 與 B 合併為本切片；共用元件跟著第一個用到它的切片立法，`ReviewCase.java`、`ReviewSubmission.java` 最後處理。

## 一、範圍

| use case | 端點 | 角色 | 狀態條件（程式） | 事件（action） | 立法狀態 |
|---|---|---|---|---|---|
| `ReturnInitialReview` | `POST /review-cases/{caseId}/initial-review/return`，body `reason`（`@NotBlank`） | `HOSPITAL_MANAGER`，本院 | `INITIAL_REVIEW_IN_PROGRESS`，否則 409 `CASE_NOT_IN_PROGRESS` | `ReviewCaseReturned`（`RETURNED`） | 未立法 |
| `ReopenInitialReview` | `POST /review-cases/{caseId}/reopen`，無 body | 同上 | `RETURNED`，否則 409 `CASE_NOT_RETURNED` | `ReviewCaseAssigned`（`ASSIGNED`） | 事件已立法 |
| `CancelReviewCase` | `POST /review-cases/{caseId}/cancel`，body `reason`（`@NotBlank`） | 同上 | 不是 `CLOSED`／`CANCELLED`，否則 409 `CASE_TERMINAL` | `ReviewCaseCancelled`（`CANCELLED`） | 未立法 |
| `CloseWithoutRereview` | `POST /review-cases/{caseId}/close-without-rereview`，無 body | 同上 | `INITIAL_REVIEW_COMPLETED` 且初審已固定，否則 409 `CASE_NOT_INITIAL_REVIEW_COMPLETED`／`INVALID_STATE_TRANSITION` | `CaseClosedWithoutRereview`（`CASE_CLOSED_WITHOUT_REREVIEW`，`detail` 是 `{"finalScore":…}`，已由 D-071 遮罩） | 未立法 |

四個 use case 都有單元測試；`ReviewCaseApiTest` 有取消、退回、重開的 HTTP 測試。`specs/` 沒有任何地方提到它們。

這個切片用到的未立法元件只有 `command/CloseWithoutRereviewCommand.java`（只給不經複審結案用），一併登記；其餘依賴是 `ReviewCaseRepository`、`DomainEventPublisher`、`ActorContext`、`Clock`，以及 domain 的 `ReviewCase`（照負責人裁決最後處理）。

## 二、查到的事實

**實測（2026-11-24，暫時的規格範例，跑完已還原，沒有提交）**：`answered_case` → 退回（理由 `a"b`）→ 重開 → 開始初審，以 `HOSPITAL_MANAGER` 查。

稽核紀錄（新到舊）：

| action | `detail` |
|---|---|
| `INITIAL_REVIEW_STARTED` | `{"reviewerId":"u-r1","submissionId":"c2f8…"}`（新提交） |
| `ASSIGNED` | `{"reviewerId":"u-r1","dueAt":"2026-11-26T23:59:59Z"}` ← 重開 |
| `RETURNED` | `{"reason":"a"b"}` ← **不是合法的 JSON** |
| `INITIAL_REVIEW_STARTED` | `{"reviewerId":"u-r1","submissionId":"5ecb…"}`（舊提交） |
| `ASSIGNED` | `{"reviewerId":"u-r1","dueAt":"2026-11-26T23:59:59Z"}` ← 原本的指派 |

修正歷史（`GetRevisionHistory`）：

| 提交 | status | revisionNo | totalScore | comment |
|---|---|---|---|---|
| 舊 | `DRAFT` | 1 | 20 | 初審評語 |
| 新 | `DRAFT` | 1 | 0 | null |

由此確認：

1. **重開記成 `ASSIGNED`**，內容和原本的指派完全相同。稽核軌跡分不出「重開」和「指派」。`test-scenarios.md` TS-TR-005 要求生命週期「可分辨至少 assign、return、reopen to pending initial review」。
2. **退回與取消的 `detail` 沒有跳脫**：`ReviewCaseReturnedEvent`、`ReviewCaseCancelledEvent` 用 `String.format("{\"reason\":\"%s\"}")` 組字串。理由含 `"` 或 `\` 時寫入的不是合法 JSON。
3. **退回後舊草稿留在 `DRAFT`**：退回不處理進行中的提交；重開後開始初審建立新提交，`latestInitialSubmissionId` 改指新提交。舊提交沒有作廢，也沒有東西指向它，但仍出現在修正歷史裡；兩筆都是 `revisionNo = 1`。

**讀文件確認**：

4. **可取消的狀態，文件與程式不一致**。`state-machine.md` §3.2 的轉移表只列 `SELECTED_PENDING_ASSIGNMENT`、`PENDING_INITIAL_REVIEW`、`INITIAL_REVIEW_IN_PROGRESS`、`PENDING_REREVIEW` 可取消；程式是「不是 `CLOSED`／`CANCELLED` 都可以」，所以 `RETURNED`、`INITIAL_REVIEW_COMPLETED`、`REREVIEW_IN_PROGRESS` 也能取消。D-006 只規定 `CANCELLED` 是終止狀態。
5. **退回的授權角色沒有定案**：`decision-log.md` 第 7 節「本版尚未納入的議題」列了「退回初審的最終授權角色」；`api-spec.md` §10.8 寫「待你後續定義更細角色；目前建議 `HOSPITAL_MANAGER`」。程式是 `HOSPITAL_MANAGER`。`permission-matrix.md` §6 沒有取消、退回、重開這三列。
6. **重開前要不要改派**：`state-machine.md` 第 7 節待確認第 2 點「`RETURNED` 後是否必須 reassign reviewer 才能 reopen」還沒有決定。程式不要求，重開後沿用原本的初審醫師與期限（期限可能已過）。重開後案件是 `PENDING_INITIAL_REVIEW`，可以用改派（切片 A）換人或改期限。
7. **不經複審結案的事件**：`domain-events.md` 6.9 寫不經複審結案也發 `ReviewCaseClosed`，並註明「不經複審結案的欄位、要不要帶結案路徑，等不經複審結案立法時決定」。程式發的是另一個事件 `CaseClosedWithoutRereview`，action 是 `CASE_CLOSED_WITHOUT_REREVIEW`，已在 D-071 的遮罩清單裡。
8. **錯誤代碼**：`CASE_TERMINAL`、`INVALID_STATE_TRANSITION`、`CASE_NOT_INITIAL_REVIEW_COMPLETED` 已在規格的 `error_codes`（409 `conflict`）。`CASE_NOT_IN_PROGRESS`、`CASE_NOT_RETURNED` 還沒有。D-068 第 1 點把退回的 `CASE_NOT_IN_PROGRESS` 留到退回立法時處理。

**沒有查證的**：取消時進行中的提交（初審或複審的 `DRAFT`、`SUBMITTED`）。程式不處理，案件終止後所有寫入都被擋，推論不會造成錯誤，但沒有實測。

## 三、X-1：可取消的狀態

- **A（建議）：照程式。** 除了 `CLOSED`、`CANCELLED` 以外都可以取消；`state-machine.md` 的轉移表補上 `RETURNED`、`INITIAL_REVIEW_COMPLETED`、`REREVIEW_IN_PROGRESS`。理由：管理者在任何未結案的時點都可能需要終止案件（例如病歷資料有誤），D-006 也只限制終止之後。
- **B：照文件。** 程式收窄，這三個狀態取消時拒絕。

## 四、X-2：重開的事件

- **A（建議）：新增事件 `ReviewCaseReopened`，action `REOPENED`。** `detail` 帶 `reviewerId`、`dueAt`（與 `ASSIGNED` 相同的欄位）。滿足 TS-TR-005。不帶分數，不列入遮罩清單。
- **B：維持 `ASSIGNED`。** 改 TS-TR-005，接受以「`RETURNED` 之後的 `ASSIGNED`」辨認重開。

## 五、X-3：退回後的舊草稿

- **A（建議）：重開後開始初審，沿用原本的草稿。** `StartInitialReview` 在案件已有初審提交且它還是 `DRAFT` 時不建立新提交，`latestInitialSubmissionId` 不變；初審醫師接著改原本的作答。修正歷史只有一筆。
- **B：退回時把草稿標成作廢。** 需要新的提交狀態或欄位（目前只有 `DRAFT`、`SUBMITTED`、`SUPERSEDED`，`SUPERSEDED` 必須指向新版）。
- **C：維持現狀。** 規格固定「舊草稿留在 `DRAFT`、修正歷史看得到」。

A 改的是已立法的 `StartInitialReview`；它的既有範例（第一次開始初審）不受影響，要新增一個重開後再開始的範例。

## 六、X-4：重開前要不要改派

- **A（建議）：不要求。** 重開沿用原本的初審醫師與期限；要換人或改期限，重開後用改派（切片 A 立法）。解決 `state-machine.md` 待確認第 2 點。
- **B：重開時必須帶新的期限**（或初審醫師）。改 API 的請求格式。

## 七、X-5：理由的 `detail`

- **建議：** 退回與取消的 `detail` 以 JSON 序列化產生，理由含 `"`、`\`、換行時仍是合法 JSON；欄位與格式（`{"reason":"…"}`）不變。規格新增含 `"` 的範例。
- 理由本身：HTTP 請求要求不是空白（`@NotBlank`）；use case 只檢查不是 `null`。建議 use case 也拒絕空白（與 HTTP 一致），錯誤代碼見 X-8。

## 八、X-6：退回的授權角色

- **建議：** 定為 `HOSPITAL_MANAGER`，本院（與程式相同）。結束 `decision-log.md` 第 7 節的這一項；`api-spec.md` §10.8 與 `permission-matrix.md` §6 補上取消、退回、重開三列（都只有 `HOSPITAL_MANAGER`）。

## 九、X-7：不經複審結案的事件

- **A（建議）：維持獨立的事件 `CaseClosedWithoutRereview`（action `CASE_CLOSED_WITHOUT_REREVIEW`）。** 結案路徑由 action 區分；`detail` 維持 `{"finalScore":…}`（D-071 遮罩）。`domain-events.md` 6.9 改寫：複審路徑發 `ReviewCaseClosed`，不經複審發 `CaseClosedWithoutRereview`，並新增一節。
- **B：改發 `ReviewCaseClosed`**，加一個結案路徑欄位。要改 action，D-071 的遮罩清單與既有稽核紀錄的 action 都會變。

## 十、X-8：錯誤代碼

**建議：** 維持程式現有的代碼與分類，補進規格的 `error_codes`：

| 情況 | 代碼 | 分類 |
|---|---|---|
| 退回時不是初審中 | `CASE_NOT_IN_PROGRESS` | 409 `conflict` |
| 重開時不是 `RETURNED` | `CASE_NOT_RETURNED` | 409 `conflict` |
| 取消已終止的案件 | `CASE_TERMINAL` | 409 `conflict`（已有） |
| 不經複審結案時狀態不對／初審未固定 | `CASE_NOT_INITIAL_REVIEW_COMPLETED`／`INVALID_STATE_TRANSITION` | 409 `conflict`（已有） |
| 理由空白（X-5） | 沿用規格現有的輸入驗證代碼 | 400 |

D-068 第 1 點把草稿的狀態錯誤改成 422；退回是管理者對案件狀態的操作，與重開、取消、結案同屬狀態衝突，建議維持 409。

## 十一、裁決後的順序

1. **修法 PR**：
   - `decision-log.md` 新增 D-080；
   - `specs/review-case/usecases.yaml`：四個 use case、規則、錯誤代碼、新事件（X-2）；`examples.yaml`：成功、各種拒絕、稽核紀錄（含 `"` 的理由、重開事件）、X-3 的重開後再開始；
   - 一開始是紅的範例加進 `known-test-failures.txt`；
   - `law/coverage.txt`：四個 use case、`CloseWithoutRereviewCommand` 與 `ReviewCaseReturned`、`ReviewCaseCancelled`、`CaseClosedWithoutRereview`（與 X-2 的新事件）登記為已立法；
   - `state-machine.md`（X-1、X-4）、`domain-events.md`（X-2、X-7）、`api-spec.md` §10.7、§10.8、§10.11、`permission-matrix.md` §6（X-6）；
   - 本草稿標為已裁決；題目清單、README 索引。
2. **建構 PR**：依裁決修改（預計：重開事件、`detail` 序列化、理由空白、`StartInitialReview` 沿用草稿）；移除 `known-test-failures.txt` 的項目。
