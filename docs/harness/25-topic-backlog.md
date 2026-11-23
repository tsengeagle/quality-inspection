# 題目清單

負責人核准的立法題目與順序（2026-11-22 起）。本文件不是律法，只是工作順序；每一題仍要照「草稿 → 裁決 → 修法 PR → 建構 PR」的流程。負責人可以隨時調整順序。

## 進行中

1. **規格格式 v2.1：請求端的範例**（D-074）。修法 PR 一是格式與 `spec_lint`；之後是修法 PR 二（執行器與第一批範例），最後是建構 PR（刪除 `DueAtTimeZoneApiTest` 中重複的部分）。

## 待辦（依序）

2. **D-072 違憲清單其餘各項**：
   - 第 1 項剩下的 `REASSIGNED`（屬於改派，等改派立法）；
   - 第 3 項：HTTP 輸出的時間格式（view 與匯出的 `LocalDateTime`）；
   - 第 4 項：`ReviewCase` 的六個 `LocalDateTime` 改成 `Instant`；
   - 第 5 項：`spec-profile.yaml` 的 `local_time_zone`；
   - 第 6 項：文件範例改成 UTC。
3. **D-069 留下的兩件**：複審不發 `ReviewDraftSaved`、`ReviewSubmissionSubmitted`；提交複審沒有必答題檢查。
4. **還沒立法的 ReviewCase 範圍**（`law/coverage.txt` 的 `grandfathered`）：改派、修正、不經複審結案、取消、退回。

## 已完成

| 題目 | 裁決 | PR |
|---|---|---|
| 稽核 `detail` 的遮罩 | D-071 | #132、#134 |
| 憲法草案第一條：日期時間 | D-072 | #133 |
| 違憲清單第 1 項的 `ASSIGNED` | D-072 第 6 點 | #134 |
| 違憲清單第 2 項：HTTP 收進來的 `dueAt` 帶時區 | D-073 | #135、#136 |
