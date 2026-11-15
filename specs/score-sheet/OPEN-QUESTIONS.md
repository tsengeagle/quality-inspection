# ScoreSheetDefinition：裁決紀錄、程式現況與未決事項

- 2026-11-15：Q-16～Q-23 全部裁決，寫在 `decision-log.md` D-050。所有規則都是 `decided`，目前沒有待裁決的題目。草稿時期的題目原文、選項與建議見本檔的 git 歷史。
- 2026-11-15：由 `spec-draft/score-sheet/` 升格到 `specs/score-sheet/`。升格時一併完成：
  1. 綁定（`BindScoreSheetToProject`，D-050 第 8 點）加進 `specs/review-project/usecases.yaml`，規則 `RP-BND-01`～`RP-BND-03`、`RP-BND-S1`、`RP-BND-S2`，範例 6 個。
  2. `specs/review-project/examples.yaml` 的 `bound_project` 改用評分表的 use case 準備（先加一個題目再發布，`SS-PUB-02`），拿掉 `input_pending`。
  3. `specs/README.md` 第五節補上 `list<T>` 與 `input_types`。
  4. 與程式不一致的範例加進 `known-test-failures.txt`。

## 程式現況（給建構 PR 參考，不是規格）

下列差異讓範例未通過，列在已知失敗清單。建構 PR 修正後，從清單刪除變綠的範例。

| 項目 | 規格 | 程式 |
|---|---|---|
| 版本 id | 發布回傳 `scoreSheetVersionId`（D-050 第 1 點） | 沒有 |
| 更新的輸入 | 六個具名輸入 | 一個命令物件；執行器依參數名稱綁定，要改成六個參數 |
| 重複章節 | `DUPLICATE_SECTION_ID` | `INVALID_STATE_TRANSITION` |
| 重複題目 id、代碼（跨章節、同章節新增、同章節修改） | 分類 `rule_violation` | 代碼正確，分類是 409 類的例外。同章節的檢查與代碼已由 PR #29 修正 |
| 欄位驗證（D-050 第 4 點） | `VALIDATION_ERROR` | 程式錯誤（500）；非 CONDITIONAL 帶 conditionExpression 被接受 |
| 查詢的檢查順序 | 角色 → 載入 → 院區（D-024） | 詳情、版本兩支已由 PR #29 修正；章節查詢本來就是這個順序 |
| 讀取角色（D-050 第 7 點） | `SYSTEM_ADMIN`、`HOSPITAL_MANAGER`、`AUDITOR` | 拒絕 `AUDITOR`；章節查詢只允許 `HOSPITAL_MANAGER` |
| 列表的院區參數 | `SYSTEM_ADMIN` 可指定院區（D-047） | 不支援 |
| 輸出欄位 | `versionNo`；章節與列表回物件 | `version`；回清單 |
| 版本查詢 | 含 `scoreSheetVersionId`（未發布時 null） | 只有版本號與是否已發布 |
| 綁定（D-050 第 8 點） | 綁定版本、檢查版本存在、屬於本專案、已發布 | 綁定 `scoreSheetId`，都不檢查 |

## 不指向規則、但仍未決的事

| 事項 | 出處 | 影響 |
|---|---|---|
| 建立新的草稿版本 | D-050 第 1 點、D-040 Pending | 發布後要改評分表，目前只能建立另一份評分表 |
| 「已發布」與「已被採用」：領域模型寫已發布**且已被採用**的版本不可修改 | `domain-model.md` §5.2 | 本規格採 api-spec §8.2 的「已發布即不可修改」（SS-INV-01） |
| 凍結（`freezeVersion`、`ScoreSheetFrozen`）與 FROZEN 狀態 | `domain-model.md` §5.2、`domain-events.md` §4.7 | 沒有定義與發布的差別；本次不處理 |
| 三種 `scoreType` 的意義、`weight` 的數值範圍 | `domain-model.md` §5.2、D-050 第 4 點 | 屬於計分的規格 |
| 發布時的 snapshot 與 checksum | D-011、`persistence-model.md` §5.5 | 屬於抽選與提交的規格 |
| `visibilityRule` | D-014 | 延後至 B3/B4 |
