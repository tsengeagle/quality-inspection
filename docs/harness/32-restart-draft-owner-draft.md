# 重開後改派，新醫師開始初審會卡住

狀態：**已裁決**（2026-11-24）。負責人裁決先單獨修，只有同一位醫師才沿用草稿（Y-1 照建議）。條文是 `decision-log.md` 的 D-081，以那裡為準。下面保留起草時的版本（main = `ce5eae1`）。本文件不是律法；題號 Y-1 只在本文件使用。

## 一、背景

切片 A（改派）起草前實測時發現。D-080 第 3 點與 RC-STI-S4 規定「案件已有初審提交且它是 `DRAFT`」就沿用，沒有看提交的醫師。#153 照此實作。

## 二、查到的事實

**實測（2026-11-24，暫時的規格範例，跑完已還原，沒有提交）**：`answered_case`（u-r1 作答）→ 退回 → 重開 → `ReassignReviewCase` 改派給 u-r2 →

| 步驟 | 結果 |
|---|---|
| u-r2 `StartInitialReview` | 成功；輸出的 `submissionId` 是 u-r1 的提交 |
| `GetRevisionHistory`（`manager_h1`） | 只有一筆：`reviewerId = u-r1`、`DRAFT`、`revisionNo = 1`、`totalScore = 20`、`comment = 初審評語` |
| u-r2 `SaveInitialReviewDraft` | 拒絕 `SUBMISSION_NOT_OWNED_BY_ACTOR` |

- 案件停在 `INITIAL_REVIEW_IN_PROGRESS`：u-r2 寫不了這份提交；u-r1 已不是指派的醫師；改派只能在 `PENDING_INITIAL_REVIEW`（409 `CASE_NOT_PENDING_INITIAL_REVIEW`）。只剩取消；或由管理者再退回、重開、改派回 u-r1。
- #153 之前一律建立新提交，不會發生。
- u-r2 也看得到 u-r1 的作答與評語（同一份提交）。

## 三、Y-1：沿用的條件

- **A（建議）：同一位醫師才沿用。** 提交的醫師不是操作者時建立新提交，原本的提交留在修正歷史（與 #153 之前相同）。範圍小，不動改派。
- **B：改派時把舊草稿作廢。** 要新增提交狀態或作廢規則，牽動 `ReviewSubmission`（最後處理的共用元件），適合放到切片 A 一起討論。

### 規格

- RC-STI-S4 加上「它的醫師是操作者」；新增 RC-STI-S5（醫師不同時建立新的提交）。
- 新範例 `restarts_with_a_new_draft_after_reopen_and_reassign`，以 `ReassignReviewCase`（未立法）製造前提，列在 `needs`；切片 A 立法前不執行（負責人選 A），建構 PR 以整合測試驗證。
