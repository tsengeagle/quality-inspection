# 題目清單

負責人核准的立法題目與順序（2026-11-22 起）。本文件不是律法，只是工作順序；每一題仍要照「草稿 → 裁決 → 修法 PR → 建構 PR」的流程。負責人可以隨時調整順序。

## 進行中

1. **ReviewCase 切片 E：讀取（案件詳情、提交明細、讀取授權、分派頁查詢）**（D-084）。修法 PR 之後是建構 PR。

## 待辦（依序）

1. **還沒立法的 ReviewCase 範圍**（`law/coverage.txt` 的 `grandfathered`）。負責人 2026-11-24 同意切成五個切片，順序 C＋B（已完成）→ A 改派（已完成）→ D 修正（已完成）→ E 讀取（進行中）；共用元件跟著第一個用到它的切片，`ReviewSubmission.java` 最後處理。負責人 2026-11-24 決定 `ReviewCase.java` 提早到切片 D 立法。
2. **前端移除 `finalConclusion`、`rereviewRequired`**（D-084 第 9 點）：後端的建構 PR 移除欄位之後，前端的型別（`cases.service.ts`）與案件詳情頁的 `finalConclusion` 顯示一併移除。前端任務從 `.ai/governance/repo-governance-instruction.md` 進入。
3. **更正 D-083 第 10 點與 D-084 第 9 點的事實描述**：兩者說 `rereviewRequired`、`finalConclusion`、`ruleSnapshotRef`「沒有任何行為寫入」，但 `rereviewRequired` 在固定初審、分數超過品質線時被 `fixInitialReview` 寫成 `true`（D-084 建構 PR 查到）。沒有任何地方讀它做判斷，移除不改變行為；條文的理由應改為「沒有任何行為讀取」。在下一個修法 PR 以註記更正。
4. **後端 JDK 定為 17**：`build.gradle` 宣告 `sourceCompatibility = '11'`，但 D-075 之後 `Instant` 解析 `+08:00` 需要 JDK 12 以上，JDK 11 上 `assigns_a_reviewer` 失敗。負責人已決定與 CI 一致，定為 17（含 CLAUDE.md、`.github/copilot-instructions.md` 等文件）。

## 已完成

| 題目 | 裁決 | PR |
|---|---|---|
| 稽核 `detail` 的遮罩 | D-071 | #132、#134 |
| 憲法草案第一條：日期時間 | D-072 | #133 |
| 違憲清單第 1 項的 `ASSIGNED` | D-072 第 6 點 | #134 |
| 違憲清單第 2 項：HTTP 收進來的 `dueAt` 帶時區 | D-073 | #135、#136 |
| 規格格式 v2.1：請求端的範例 | D-074 | #137、#138、#139 |
| 違憲清單第 1 項的 `REASSIGNED`、第 3、4、6 項：時間改成 `Instant`（含 `ReviewSubmission` 的三個時間） | D-075 | #140、#141 |
| 違憲清單第 5 項：`local_time_zone` 只用於範例時鐘，執行器不再解讀不帶時區的時間 | D-075 第 7 點 | #142 |
| D-069 留下的兩件：複審的提交事件與必答題檢查 | D-076 | #143、#144 |
| 報表的日期篩選固定以台北解讀 | D-077 | #145、#146 |
| `ReviewSubmission` 稽核紀錄的遮罩 | D-078 | #147、#148、#149 |
| 初審草稿的稽核紀錄帶院區 | D-079 | #150、#151 |
| ReviewCase 切片：退回、重開、取消、不經複審結案 | D-080 | #152、#153 |
| 重開後改派，新醫師開始初審建立新的提交 | D-081 | #154、#155 |
| ReviewCase 切片 A：改派初審醫師、改派複審醫師 | D-082 | #156、#157 |
| ReviewCase 切片 D：修正已提交的初審與複審，並立法 `ReviewCase.java` | D-083 | #158、#159 |

D-072 第 4 點的違憲清單到此全部修正完成（D-075 第 12 點）。
