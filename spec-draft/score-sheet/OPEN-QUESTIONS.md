# ScoreSheetDefinition 規格草稿：裁決紀錄與升格待辦

- 狀態：**草稿，不是 authority**。權威來源仍是 `docs/01-requirements` 到 `docs/04-testing`。
- 建立日期：2026-11-15，由 Claude Code 依現有文件與程式轉換。
- 範圍：評分表的 3 支命令（建立、更新、發布）與 4 支查詢。綁定（`BindScoreSheetToProject`）屬於 `ReviewProject`，見「升格時一起處理」。
- 2026-11-15：Q-16～Q-23 全部裁決，寫在 `decision-log.md` D-050。所有規則都是 `decided`，目前沒有待裁決的題目。草稿時期的題目原文、選項與建議見本檔的 git 歷史。

## 升格時一起處理

升格（`spec-draft/score-sheet/` → `specs/score-sheet/`）的修法 PR 要同時做下列幾件事：

1. **把綁定加進 `specs/review-project/usecases.yaml`**（D-050 第 8 點）。合約如下，規則 id 在升格時編號：

   | 檢查（依序） | 結果 |
   |---|---|
   | 角色不是 `HOSPITAL_MANAGER` | `ROLE_DENIED`（RP-G-01） |
   | 專案不存在 | `PROJECT_NOT_FOUND`（RP-G-02） |
   | 院區不符 | `HOSPITAL_SCOPE_MISMATCH`（RP-G-03） |
   | 專案目前的版本已發布 | `INVALID_STATE_TRANSITION` |
   | 評分表版本不存在，或屬於另一個專案 | `SCORE_SHEET_NOT_FOUND` |
   | 評分表版本尚未發布 | `INVALID_STATE_TRANSITION` |
   | 成功 | 專案目前版本的 `scoreSheetVersionRef` = 輸入；重新綁定會取代 |

2. **更新 `specs/review-project/examples.yaml` 的 `bound_project` fixture**：`CreateScoreSheetDraft` 的輸入是 `{ projectId }`，發布前要先用 `UpdateScoreSheetDraft` 加至少一個題目（`SS-PUB-02`）。拿掉 `input_pending`。
3. **在 `specs/README.md` 第五節補上格式缺口**（見下節）。
4. **已知失敗清單**：升格後執行器會執行這些範例，與程式不一致的範例要加進 `known-test-failures.txt`（見「程式現況」）。

## 格式

本草稿用到格式 v1 沒有定義的寫法：

| 寫法 | 用在 | 意思 |
|---|---|---|
| `list<T>` | 章節、題目、更新命令的各相位 | 有順序、可重複的清單。範例比對時執行器不比順序（第四節「執行」） |
| `input_types` | `usecases.yaml` | 輸入裡的物件結構，用名稱引用 |

## 程式現況（給建構 PR 參考，不是規格）

| 項目 | 規格 | 程式 |
|---|---|---|
| 版本 id | 發布回傳 `scoreSheetVersionId`（D-050 第 1 點） | 沒有 |
| 更新的輸入 | 六個具名輸入 | 一個命令物件；執行器依參數名稱綁定，要改成六個參數 |
| 重複章節 | `DUPLICATE_SECTION_ID` | `INVALID_STATE_TRANSITION` |
| 重複題目 id、代碼：跨章節 | 分類 `rule_violation` | 409 類的例外 |
| 重複題目 id、代碼：同章節新增 | `DUPLICATE_ITEM_ID`、`DUPLICATE_ITEM_CODE` | 章節自己的檢查丟程式錯誤（500） |
| 重複題目代碼：同章節修改 | `DUPLICATE_ITEM_CODE`（SS-INV-03） | **不檢查**：唯一性檢查排除了題目所在的章節，取代時也不檢查，會存下重複的代碼。不只是錯誤分類，唯一性檢查本身要補 |
| 欄位驗證（D-050 第 4 點） | `VALIDATION_ERROR` | 程式錯誤（500）；非 CONDITIONAL 帶 conditionExpression 被接受 |
| `GetScoreSheetDetail` 的檢查順序 | 角色 → 載入 → 院區（D-024） | 先載入再檢查角色：不存在的 id 對無權角色回 `SCORE_SHEET_NOT_FOUND` 而不是 `ROLE_DENIED` |
| 讀取角色（D-050 第 7 點） | `SYSTEM_ADMIN`、`HOSPITAL_MANAGER`、`AUDITOR` | 拒絕 `AUDITOR`；章節查詢只允許 `HOSPITAL_MANAGER` |
| 列表的院區參數 | `SYSTEM_ADMIN` 可指定院區（D-047） | 不支援 |
| 輸出欄位 | `versionNo`；章節與列表回物件 | `version`；回清單 |
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
