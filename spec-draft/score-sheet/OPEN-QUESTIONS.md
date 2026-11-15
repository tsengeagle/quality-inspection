# ScoreSheetDefinition 規格草稿的待裁決問題

- 狀態：**草稿，不是 authority**。權威來源仍是 `docs/01-requirements` 到 `docs/04-testing`。
- 建立日期：2026-11-15，由 Claude Code 依現有文件與程式轉換。
- 範圍：評分表的 3 支命令（建立、更新、發布）與 4 支查詢。綁定（`BindScoreSheetToProject`）屬於 `ReviewProject`，見 Q-23。
- 題號接續 `specs/review-project/OPEN-QUESTIONS.md`（到 Q-15），避免兩份規格的題號混淆。
- 裁決後：先把決定寫進 `decision-log.md`，再把對應規則改成 `status: decided` 並補 `source`，最後刪除本檔的該題。

## 總表

| 題號 | 問題 | 指向的規則 | 建議 |
|---|---|---|---|
| Q-16 | 評分表版本的識別：專案綁定的 `scoreSheetVersionId` 從哪裡來 | `ScoreSheetVersion.scoreSheetVersionId`、`SS-PUB-S2`、發布的輸出 | A：版本有自己的 id，發布時回傳；「建立新版本」暫不做 |
| Q-17 | 建立草稿的前提：專案狀態、同一專案可否有多份 | `SS-CRT-01`、`SS-CRT-02` | 都不限制 |
| Q-18 | 更新與發布的錯誤代碼 | `SS-UPD-01～03`、`SS-PUB-01`、`SECTION_NOT_FOUND`、`ITEM_NOT_FOUND` | 已發布一律 `INVALID_STATE_TRANSITION`；保留兩個 NOT_FOUND 代碼 |
| Q-19 | 題目與章節欄位的輸入驗證 | `SS-UPD-04～08`、`SS-UPD-S3`、`ScoreItem.weight` | 全部 `VALIDATION_ERROR`；weight 預設 1 |
| Q-20 | 更新命令的形式與語意 | `SS-UPD-12`、`SS-UPD-S1`、`SS-UPD-S4`、`ScoreSheetDraftUpdated` | 六相位（D-015），依程式的相位順序 |
| Q-21 | 發布的條件 | `SS-PUB-02`、`SS-PUB-03` | 至少一個題目；容許空章節 |
| Q-22 | 查詢的讀取角色與輸出 | 4 支查詢的 `actor.open` 與 `output` | 比照 D-043 |
| Q-23 | 綁定的規則放在哪裡、要檢查什麼 | 無（屬於 `ReviewProject`） | 升格時加進 `specs/review-project/` |

## 題目

### Q-16

**評分表版本的識別：專案綁定的 `scoreSheetVersionId` 從哪裡來。**

- 已定：專案綁定的是評分表「版本」（D-041）；案件與提交要能追溯評分表版本（D-011）。`persistence-model.md` §5.4、§5.5 把評分表與評分表版本分成兩層，版本有 `score_sheet_version_id` 與 `version_no`。
- 現況：程式的評分表沒有版本層。`version` 固定是 1，沒有版本 id，也沒有「建立新版本」；發布不回傳任何值。綁定 use case 收的是 `scoreSheetId`。
- 影響：`specs/review-project` 有 12 個範例等這一題（`bound_project` fixture 用 `PublishScoreSheetVersion` 的輸出 `scoreSheetVersionId` 綁定）。

| 選項 | 內容 | 代價 |
|---|---|---|
| **A（建議）** | 版本有自己的 `scoreSheetVersionId`，發布時產生並回傳。本次只有「第一版」；「建立新的草稿版本」列為未決（比照 D-040 Pending） | 程式要加版本 id；與 D-041、persistence-model 一致 |
| B | 一份評分表就是一個版本，`scoreSheetId` 兼任版本 id；要改版就建立一份新的評分表 | 程式改最少，但要修改 D-041 與 api-spec §7.6，並放棄 `versionNo` 遞增的意義 |
| C | 綁定 `scoreSheetId` 加 `versionNo` | 兩個欄位才能定位一個版本，追溯時容易只存一半 |

### Q-17

**建立評分表草稿的前提。**

1. **專案狀態（`SS-CRT-01`）。** `api-spec.md` §8.1 寫專案不是 `ACTIVE` 時回 `PROJECT_NOT_ACTIVE`。但這會卡死：專案發布前必須已綁定評分表版本（RP-PUB-02），啟用前必須已發布（RP-ACT-03）——要求 `ACTIVE` 等於第一份評分表永遠建不出來。程式不檢查專案狀態。
   - 建議：不檢查。建立草稿不改變專案；專案狀態在綁定時才有關係（Q-23）。同時刪除 api-spec §8.1 的 `PROJECT_NOT_ACTIVE`。
   - 其他選項：只允許 `DRAFT`（停用的專案不能準備新評分表）。
2. **同一專案多份評分表（`SS-CRT-02`）。** 文件沒寫。程式允許；`persistence-model.md` §5.4 的 `project_id` 沒有唯一約束。
   - 建議：允許。哪一份被採用由綁定決定。

### Q-18

**更新與發布的錯誤代碼。**

| 規則 | 情況 | api-spec | 程式 | 建議 |
|---|---|---|---|---|
| `SS-UPD-01` | 更新已發布的版本 | `ALREADY_PUBLISHED`（409，§4.2 沒有這個代碼） | `INVALID_STATE_TRANSITION` | `INVALID_STATE_TRANSITION`，與 RP-UPD-01 一致 |
| `SS-PUB-01` | 再次發布 | 沒寫 | `INVALID_STATE_TRANSITION` | `INVALID_STATE_TRANSITION`，與 D-049 對專案的裁決一致 |
| `SS-UPD-02` | 指向不存在的章節 | 沒寫 | `SECTION_NOT_FOUND` | 保留，分類 `rule_violation`。不用 `not_found`：評分表本身存在，對外的狀態碼不該跟 `SCORE_SHEET_NOT_FOUND` 一樣 |
| `SS-UPD-03` | 指向不存在的題目 | 沒寫 | `ITEM_NOT_FOUND` | 同上 |

另外，重複的章節、題目 id、題目代碼（`SS-UPD-09～11`）的代碼已由 api-spec §8.2 定義，本草稿標為 `decided`，分類依 api-spec 是 `rule_violation`（422）；程式目前對重複章節回 `INVALID_STATE_TRANSITION`，對重複題目回 409 類，見「程式現況」。

### Q-19

**題目與章節欄位的輸入驗證。**

- 已定：`maxScore` 不為負（SS-INV-04）；`CONDITIONAL` 時 `conditionExpression` 有值、其他時候沒有值（SS-INV-06）。
- 未定：違反時回什麼。程式在這些情況丟出程式錯誤（對外變成 500），只有重複類有錯誤代碼。
- 建議：下列全部回 `VALIDATION_ERROR`：

  | 規則 | 情況 |
  |---|---|
  | `SS-UPD-04` | 必填欄位為 null 或空白 |
  | `SS-UPD-05` | 章節 title 為 null 或空白 |
  | `SS-UPD-06` | `maxScore` 小於 0 |
  | `SS-UPD-07` | `CONDITIONAL` 卻沒有 `conditionExpression` |
  | `SS-UPD-08` | 不是 `CONDITIONAL` 卻有 `conditionExpression`（程式目前接受並保存） |

- `weight`（`SS-UPD-S3`）：沒有輸入時是 1。這只見於程式；api-spec §8.2 的範例是 `1.0`。另外 `weight` 能不能是 0 或負數，文件沒寫；建議本次不限制，等計分的規格處理。

### Q-20

**更新命令的形式與語意。**

1. **形式。** D-015 定案為「六相位 command」，程式也是；但 `api-spec.md` §8.2 的請求範例是整份取代（含 `name`、`sections`、`displayOrder`）。建議以 D-015 為準，修正 api-spec §8.2 的範例。
2. **相位順序（`SS-UPD-S1`）。** 依程式：刪除章節 → 刪除題目 → 章節改名 → 修改題目 → 新增章節 → 新增題目。後面的相位看得到前面的結果，所以「先刪 I1、再新增 I1」可以成功（範例 `removal_runs_before_addition`）。
3. **修改題目是整筆取代（`SS-UPD-S4`）。** 沒給的欄位不保留舊值（例如 `weight` 回到 1）。與 RP-UPD-S1 的整組取代一致。
4. **空命令（`SS-UPD-12`）。** 六個相位都空時成功、沒有改變。D-038 的 `NO_CHANGES` 是提交草稿的規則，建議不套用到評分表。
5. **事件。** 程式發出 `ScoreSheetDraftUpdated`，`domain-events.md` 沒有這個事件。建議補進 domain-events。
6. **`name` 與 `displayOrder`。** api-spec §8.2 與 persistence-model §5.4、§5.6、§5.7 有，領域模型與程式都沒有。建議本次不納入（列在 `model.yaml` 的 `deferred_attributes`）；章節與題目的順序以清單順序表示，見「格式」。

### Q-21

**發布的條件。**

- 已定：有 `SCORE_SHEET_NOT_PUBLISHABLE` 這個代碼（api-spec §4.2），但沒有寫什麼情況不可發布。`domain-model.md` §5.2 只寫「必填規則與顯示規則必須可計算」。
- 程式：沒有章節，或所有章節都沒有題目時拒絕；有題目時，空的章節可以發布。
- 建議：照程式（`SS-PUB-02` 拒絕、`SS-PUB-03` 成功）。其他選項：空章節也拒絕。

### Q-22

**查詢的讀取角色與輸出。**

| 查詢 | api-spec | 程式 | 建議 |
|---|---|---|---|
| `GetScoreSheetDetail` | §8.4：`HOSPITAL_MANAGER`、`AUDITOR`、其他角色依 scope 唯讀 | `SYSTEM_ADMIN`、`HOSPITAL_MANAGER` | `SYSTEM_ADMIN`、`HOSPITAL_MANAGER`、`AUDITOR`（比照 D-043） |
| `GetScoreSheetSections` | 沒寫 | `HOSPITAL_MANAGER` | 同上 |
| `GetScoreSheetVersion` | 沒寫 | `SYSTEM_ADMIN`、`HOSPITAL_MANAGER` | 同上 |
| `ListScoreSheetSummaries` | 沒寫 | `SYSTEM_ADMIN`、`HOSPITAL_MANAGER` | 同上 |

- `SYSTEM_ADMIN` 能不能看章節與題目：D-010 寫它「不可查看完整評分內容」。建議解讀為提交的分數與答案，不包括評分表的題目定義；若不同意，`GetScoreSheetSections` 拿掉 `SYSTEM_ADMIN`。
- `REVIEWER`／`REREVIEWER`：審查時看到的評分表來自案件的 snapshot，不走這幾支查詢。建議不開放（與 Q-12 同樣未定義範圍）。
- 輸出：`GetScoreSheetSections` 回 `{ scoreSheetId, sections }`、`ListScoreSheetSummaries` 回 `{ items }`。執行器的 `expect.output` 只比對物件的欄位，回傳清單本身無法比對。

### Q-23

**綁定（`BindScoreSheetToProject`）的規則。**

綁定改變的是專案，屬於 `ReviewProject`，但目前不在 `specs/review-project/usecases.yaml`。`specs/review-project` 的 12 個範例要等它、`CreateScoreSheetDraft`、`PublishScoreSheetVersion` 三支都進到 `specs/` 才能執行。

建議在本草稿升格的同一個 PR，把下列合約加進 `specs/review-project/usecases.yaml`：

| 檢查（依序） | 結果 | 依據 |
|---|---|---|
| 角色不是 `HOSPITAL_MANAGER` | `ROLE_DENIED` | RP-G-01 |
| 專案不存在 | `PROJECT_NOT_FOUND` | RP-G-02 |
| 院區不符 | `HOSPITAL_SCOPE_MISMATCH` | RP-G-03 |
| 專案目前的版本已發布 | `INVALID_STATE_TRANSITION` | RP-INV-01。程式檢查的是專案 status = DRAFT，啟用後的專案草稿版本也會被擋（D-040 Pending 的新版本問題） |
| 評分表版本不存在 | 待定（建議 `SCORE_SHEET_NOT_FOUND`） | D-041 Pending |
| 評分表版本尚未發布 | 待定（建議 `PROJECT_VERSION_NOT_PUBLISHABLE` 以外的新代碼，或 `INVALID_STATE_TRANSITION`） | D-041 Pending |
| 評分表屬於另一個專案 | 待定（建議拒絕） | 文件沒寫；程式不檢查，可以綁到別院區專案的評分表 |
| 成功 | 專案目前版本的 `scoreSheetVersionRef` = 輸入；重新綁定會取代 | D-041 |

## 格式

本草稿用到格式 v1 沒有定義的寫法，升格時要在 `specs/README.md` 第五節補上（同一個修法 PR）：

| 寫法 | 用在 | 意思 |
|---|---|---|
| `list<T>` | 章節、題目、更新命令的各相位 | 有順序、可重複的清單。範例比對時執行器不比順序（第四節「執行」） |
| `input_types` | `usecases.yaml` | 輸入裡的物件結構，用名稱引用 |

## 程式現況（給建構 PR 參考，不是規格）

| 項目 | 規格 | 程式 |
|---|---|---|
| 版本 id | 發布回傳 `scoreSheetVersionId`（Q-16） | 沒有 |
| 更新的輸入 | 六個具名輸入 | 一個命令物件；執行器依參數名稱綁定，要改成六個參數 |
| 重複章節 | `DUPLICATE_SECTION_ID` | `INVALID_STATE_TRANSITION` |
| 重複題目 id、代碼 | 分類 `rule_violation` | 409 類的例外 |
| 欄位驗證（Q-19） | `VALIDATION_ERROR` | 程式錯誤（500）；非 CONDITIONAL 帶 conditionExpression 被接受 |
| `GetScoreSheetDetail` 的檢查順序 | 角色 → 載入 → 院區（D-024） | 先載入再檢查角色：不存在的 id 對無權角色回 `SCORE_SHEET_NOT_FOUND` 而不是 `ROLE_DENIED` |
| 讀取角色（Q-22） | 含 `AUDITOR` | 拒絕 `AUDITOR` |
| 列表的院區參數 | `SYSTEM_ADMIN` 可指定院區（D-047） | 不支援 |
| 輸出欄位 | `versionNo`；Sections 與 List 回物件 | `version`；回清單 |
| 綁定（Q-23） | 綁定版本、檢查版本 | 綁定 `scoreSheetId`，不檢查評分表是否存在、是否已發布、屬於哪個專案 |

## 不指向規則、但仍未決的事

| 事項 | 出處 | 影響 |
|---|---|---|
| 「已發布」與「已被採用」：領域模型寫已發布**且已被採用**的版本不可修改 | `domain-model.md` §5.2 | 本草稿採 api-spec §8.2 的「已發布即不可修改」（SS-INV-01） |
| 凍結（`freezeVersion`、`ScoreSheetFrozen`）與 FROZEN 狀態 | `domain-model.md` §5.2、`domain-events.md` §4.7 | 沒有定義與發布的差別；本次不處理 |
| 三種 `scoreType` 的意義（例如 BOOLEAN 的 `maxScore`） | `domain-model.md` §5.2 | 屬於計分的規格 |
| 發布時的 snapshot 與 checksum | D-011、`persistence-model.md` §5.5 | 屬於抽選與提交的規格 |
| `visibilityRule` | D-014 | 延後至 B3/B4 |
