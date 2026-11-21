# ReviewCase 規格的待裁決問題

本檔只列**目前還沒裁決**、而且規格裡有規則或欄位指向它的題目。`proposed` 規則的 `question` 指向這裡的標題。

- 題號沿用試寫（`docs/harness/samples/reviewcase-v2-trial/`）。Q-04 已由 D-066 第 3 點裁決；Q-06 在 `../sampling-batch/OPEN-QUESTIONS.md`。
- 裁決後：先把決定寫進 `decision-log.md`，再把對應規則改成 `status: decided` 並補 `source`，最後刪除本檔的該題。三者在同一個修法 PR。

## 題目

### Q-01

**錯誤代碼：權威文件沒有寫的三種情況。**

前三種情況（指派時案件不是待指派、開始初審時案件不是待初審、保存草稿或提交初審的人不是草稿的醫師）已由 D-066 第 6 點裁決：照 `api-spec.md` §4.2，程式改用 `CASE_NOT_ASSIGNABLE`、`CASE_NOT_IN_INITIAL_REVIEW_STAGE`、`SUBMISSION_NOT_OWNED_BY_ACTOR`。剩下的是 `api-spec.md` 沒有代碼的三種：

| 情況 | `api-spec.md` §4.2 | 程式（2026-11-21） | 規格（`proposed`） |
|---|---|---|---|
| 開始初審的人不是受指派的醫師 | 表內沒有 | `NOT_ASSIGNED_REVIEWER` | `NOT_ASSIGNED_REVIEWER`（`RC-STI-01`） |
| 提交初審時案件不是初審中 | 表內沒有 | `CASE_NOT_IN_PROGRESS` | `CASE_NOT_IN_INITIAL_REVIEW_STAGE`（`RC-SUB-02`） |
| 提交初審時必答題沒有作答 | 表內沒有 | `REQUIRED_ITEMS_NOT_ANSWERED` | `REQUIRED_ITEMS_NOT_ANSWERED`（`RC-SUB-03`） |

- 第一、三列：規格與程式相同，範例已經是綠的（M-1 修好之後）。
- 第二列：規格借用 `api-spec.md` 為開始初審定義的代碼，與程式不同，範例是紅的。
- 裁決後把代碼補進 `api-spec.md` §4.2。

### Q-02

**指派時沒有給 `reviewerId` 或 `dueAt`。**

`api-spec.md` §10.3 說 `dueAt` 必填，state-machine §3.2 說 reviewer 必填，但都沒有寫被拒絕時的代碼。程式不檢查 `dueAt`；`reviewerId` 是 null 時丟出沒有錯誤代碼的例外。規格假設是 `VALIDATION_ERROR`。

### Q-03

**提交初審要不要發出 `InitialReviewCompleted`。**

`domain-events.md` §6.4 列了這個事件，§9.1 的流程也有它。程式提交初審時只發出 `ReviewSubmissionSubmitted`，整個 `src/main` 找不到 `InitialReviewCompleted`。

### Q-05

**SYSTEM_ADMIN 的 revision 歷程白名單有哪些欄位。**

| 來源 | 欄位 |
|---|---|
| `permission-matrix.md` §5.1（UI-014 的延伸欄位） | `stage`、`revisionNo`、`submittedAt`、`submittedBy`、`isCurrentEffective` |
| `api-spec.md` §13.2 的回應範例 | `submissionId`、`stage`、`revisionNo`、`status`、`submittedAt` |
| 程式（2026-11-20） | `submissionId`、`stage`、`reviewerId`、`status`、`revisionNo`、`submittedAt`、`supersededBySubmissionId` |

三邊都不一樣。規格的 `only` 照 `api-spec.md` §13.2。黑名單（沒有分數與評語）三邊一致，已經是 `decided`。

### Q-07

**`InitialReviewStarted`、`ReviewDraftSaved` 帶哪些欄位。**

`domain-events.md` §6.3、§7.1 沒有「最低必要欄位」。依 D-066 第 2 點，規格先只斷言發出者的 identity，標 `proposed`：

| 事件 | 發出者（`domain-events.md`） | 規格斷言的欄位 | 規則 |
|---|---|---|---|
| `InitialReviewStarted` | `ReviewCase`（§6.3） | `caseId` | `RC-STI-S3` |
| `ReviewDraftSaved` | `ReviewSubmission`（§7.1） | `submissionId` | `RC-DRF-S2` |

要裁決的是：這兩個事件的欄位清單。裁決後補進 `domain-events.md`，兩條規則改成 `decided`。

試寫第二輪的寫法不同：`InitialReviewStarted` 斷言 `caseId` 與 `submissionId`，`ReviewDraftSaved` 斷言 `caseId`。正式規格依 D-066 第 2 點改成只有發出者的 identity。
