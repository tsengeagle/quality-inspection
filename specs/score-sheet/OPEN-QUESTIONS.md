# ScoreSheetDefinition：裁決紀錄、程式現況與未決事項

- 2026-11-15：Q-16～Q-23 全部裁決，寫在 `decision-log.md` D-050。所有規則都是 `decided`，目前沒有待裁決的題目。草稿時期的題目原文、選項與建議見本檔的 git 歷史。
- 2026-11-15：由 `spec-draft/score-sheet/` 升格到 `specs/score-sheet/`。升格時一併完成：
  1. 綁定（`BindScoreSheetToProject`，D-050 第 8 點）加進 `specs/review-project/usecases.yaml`，規則 `RP-BND-01`～`RP-BND-03`、`RP-BND-S1`、`RP-BND-S2`，範例 6 個。
  2. `specs/review-project/examples.yaml` 的 `bound_project` 改用評分表的 use case 準備（先加一個題目再發布，`SS-PUB-02`），拿掉 `input_pending`。
  3. `specs/README.md` 第五節補上 `list<T>` 與 `input_types`。
  4. 與程式不一致的範例加進 `known-test-failures.txt`。

## 程式現況（給建構 PR 參考，不是規格）

升格時列出的差異已全部修正，`specs/` 的驗收範例全部通過，已知失敗清單裡沒有評分表的範例。

| 項目 | 修正 |
|---|---|
| 更新的輸入改為六個具名輸入；欄位驗證回 `VALIDATION_ERROR`；重複章節回 `DUPLICATE_SECTION_ID`；重複題目與「不能發布」改為規則違反類；檢查順序依本規格 | PR #41 |
| 讀取角色加入 `AUDITOR`；`SYSTEM_ADMIN` 不受院區限制；輸出欄位 `versionNo`，章節與列表回物件 | PR #41 |
| 發布回傳 `scoreSheetVersionId`、版本查詢含 `scoreSheetVersionId`；綁定檢查版本存在、屬於本專案、已發布；評審流程以版本 id 載入評分表 | PR #42 |

版本 id 的格式由實作決定（評分表 id 加版本號，沒有新增欄位）；既有資料的轉換與欄位改名見 `decision-log.md` D-045 的 Pending。

## 不指向規則、但仍未決的事

| 事項 | 出處 | 影響 |
|---|---|---|
| 建立新的草稿版本 | D-050 第 1 點、D-040 Pending | 發布後要改評分表，目前只能建立另一份評分表 |
| 「已發布」與「已被採用」：領域模型寫已發布**且已被採用**的版本不可修改 | `domain-model.md` §5.2 | 本規格採 api-spec §8.2 的「已發布即不可修改」（SS-INV-01） |
| 凍結（`freezeVersion`、`ScoreSheetFrozen`）與 FROZEN 狀態 | `domain-model.md` §5.2、`domain-events.md` §4.7 | 沒有定義與發布的差別；本次不處理 |
| 三種 `scoreType` 的意義、`weight` 的數值範圍 | `domain-model.md` §5.2、D-050 第 4 點 | 屬於計分的規格 |
| 發布時的 snapshot 與 checksum | D-011、`persistence-model.md` §5.5 | 屬於抽選與提交的規格 |
| `visibilityRule` | D-014 | 延後至 B3/B4 |
