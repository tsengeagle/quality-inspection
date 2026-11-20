# 待裁決：ReviewCase 薄切片（試寫）

試寫時查到權威文件與程式不一致、或權威文件沒有寫的地方。這些是內容的問題，不是格式的缺口；格式的缺口見 `../README.md`。

正式立法前要先裁決，否則對應的規則只能是 `proposed`，範例不能當驗收依據。

### Q-01

**錯誤代碼：權威文件寫的和程式用的不一樣。**

| 情況 | `api-spec.md` §4.2 | 程式（2026-11-20） |
|---|---|---|
| 指派時案件不是待指派 | `CASE_NOT_ASSIGNABLE`（422） | `CASE_NOT_PENDING_ASSIGNMENT`（409 類的例外） |
| 開始初審時案件不是待初審 | `CASE_NOT_IN_INITIAL_REVIEW_STAGE`（422） | `CASE_NOT_PENDING_INITIAL_REVIEW` |
| 提交初審的人不是草稿的醫師 | `SUBMISSION_NOT_OWNED_BY_ACTOR`（403） | `NOT_SUBMISSION_OWNER` |
| 開始初審的人不是受指派的醫師 | 表內沒有 | `NOT_ASSIGNED_REVIEWER` |
| 提交初審時案件不是初審中 | 表內沒有 | `CASE_NOT_IN_PROGRESS` |
| 提交初審時必答題沒有作答 | 表內沒有 | `REQUIRED_ITEMS_NOT_ANSWERED` |

- 前三列：試寫照權威文件寫成 `decided`。照這樣立法，程式的三個代碼都要改，範例一開始會是紅的。
- 後三列：權威文件沒有代碼，試寫標 `proposed`。`RC-SUB-02` 暫時借用 `CASE_NOT_IN_INITIAL_REVIEW_STAGE`。
- `NOT_SUBMISSION_OWNER` 在 D-054 第 6 點（固定初審）是已裁決的代碼。要不要讓提交初審也用它、並修正 `api-spec.md`，是這一題的一部分。

要裁決的是：每一列以哪一邊為準。

### Q-02

**指派時沒有給 `reviewerId` 或 `dueAt`。**

`api-spec.md` §10.3 說 `dueAt` 必填，state-machine §3.2 說 reviewer 必填，但都沒有寫被拒絕時的代碼。程式目前不檢查 `dueAt`；`reviewerId` 是 null 時丟出沒有錯誤代碼的例外。試寫假設是 `VALIDATION_ERROR`。

### Q-03

**提交初審要不要發出 `InitialReviewCompleted`。**

`domain-events.md` §6.4 列了這個事件，§9.1 的流程也有它。程式提交初審時只發出 `ReviewSubmissionSubmitted`，整個 `src/main` 找不到 `InitialReviewCompleted`。

格式 v2 第 7 項的 `emits_only: true` 會讓這個差異變成紅燈。要裁決的是：補上事件，還是修正 `domain-events.md`。
