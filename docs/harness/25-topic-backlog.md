# 題目清單

負責人核准的立法題目與順序（2026-11-22 起）。本文件不是律法，只是工作順序；每一題仍要照「草稿 → 裁決 → 修法 PR → 建構 PR」的流程。負責人可以隨時調整順序。

## 進行中

1. **切片 D、E：HTTP 介面與共用元件**（D-088）。分三批：D-1 回應契約（修法 PR 一 → 建構 PR → 修法 PR 二，契約移到 `specs/`）→ D-2 請求端 → E 共用元件。

## 待辦（依序）

1. **問題側其餘的範圍**（`law/coverage.txt` 的 `grandfathered`）。負責人 2026-11-25 同意的順序：C `ReviewSubmission` 與計分（已完成，D-085）→ A SamplingBatch 其餘部分（已完成，D-086）→ B 報表與儀表板（已完成，D-087）→ D、E HTTP 介面與共用元件（進行中，D-088）→ 專案版本管理（score-sheet 的 `freezeVersion` 等、品質線與權重取自目前的版本）與未裁決事項（逾期、多角色的讀取授權、`latestRevisionSummary`），要負責人先定方向。逾期的現況（D-087 第 5 點）：儀表板的 `overdueCases` 只看初審期限、不看複審期限；初審準時提交但期限已過且還沒結案也算；「現在」取資料庫的時間，範例的時鐘控制不到。ReviewCase 的五個切片已完成（D-080～D-084）
2. **前端移除 `finalConclusion`、`rereviewRequired`**（D-084 第 9 點）：後端的建構 PR 移除欄位之後，前端的型別（`cases.service.ts`）與案件詳情頁的 `finalConclusion` 顯示一併移除。前端任務從 `.ai/governance/repo-governance-instruction.md` 進入。
3. **母體來源的介接**（D-086 第 1、2、6 點延後的部分）：抽選失敗怎麼記錄（`FAILED`、`SamplingBatchFailed`）、重跑的成功路徑（D-013）、抽樣比例與排除條件。要等真正的母體來源定案。
4. **HTTP 回應與 `api-spec.md` 的落差**（D-088 第 2 點，契約照程式）：執行抽選的回應只有 `batchId`（§9.3 寫 `batchId`、`status`、`selectedCount`）；重跑的回應是 `batchId`（§9.4 寫 `newBatchId`、`retryOfBatchId`）；預覽、執行的請求本文 `mode` 沒有讀取。程式一律輸出 null 的欄位：`rereviewContext.initialReviewerName`、執行頁的 `previewStats`（`previewReady` 一律是 false）。另外兩件之後再做：沒有回應 dto 的端點改成 dto（H-3 B）、統一 `Page` 與 `{ items }`（D-088 第 9 點）。
5. **後端 JDK 定為 17**：`build.gradle` 宣告 `sourceCompatibility = '11'`，但 D-075 之後 `Instant` 解析 `+08:00` 需要 JDK 12 以上，JDK 11 上 `assigns_a_reviewer` 失敗。負責人已決定與 CI 一致，定為 17（含 CLAUDE.md、`.github/copilot-instructions.md` 等文件）。

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
| ReviewCase 切片 E：讀取（案件詳情、提交明細、讀取授權、分派頁查詢） | D-084 | #160、#161 |
| 更正 D-083 第 10 點與 D-084 第 9 點的事實描述（`rereviewRequired` 其實有寫入，只是沒有讀取） | D-085 第 20 點 | 切片 C 的修法 PR |
| 切片 C：`ReviewSubmission` 本體、計分，以及 ReviewCase 的收尾 | D-085 | #162、#163 |
| 切片 A：SamplingBatch 其餘部分（重跑的檢查、預覽、執行頁、查詢與匯出、批次本體、規則快照） | D-086 | #164、#165 |
| 切片 B：報表與儀表板（入口改成 use case、件數不含已取消的案件、匯出的規則與遮罩） | D-087 | #166、#167 |

D-072 第 4 點的違憲清單到此全部修正完成（D-075 第 12 點）。
