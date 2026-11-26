# review-project、score-sheet 的事件驗證

狀態：**已裁決**（2026-11-25）。負責人裁決：E-1～E-7 全部照建議；條文是 `decision-log.md` 的 D-090，以那裡為準。下面保留起草時的版本（main = `cf2a0b6`）。本文件不是律法；題號 E-1～E-7 只在本文件使用。

題目清單第 5 題（D-089 第 7 點）。兩個目錄在 D-089 升級到格式 v2 之後，範例可以用 `emits` 驗證事件；目前兩個目錄的 `not_covered` 都寫「事件不由範例驗證」。

## 一、範圍

| use case | 規格宣告的事件 | 成功的範例 |
|---|---|---|
| `CreateReviewProject` | `ReviewProjectCreated` | 2 |
| `UpdateReviewProjectDraft` | `ReviewProjectDraftUpdated` | 2 |
| `PublishReviewProjectVersion` | `ReviewProjectVersionPublished` | 1 |
| `ActivateReviewProject` | `ReviewProjectActivated` | 3 |
| `DeactivateReviewProject` | `ReviewProjectDeactivated` | 2 |
| `BindScoreSheetToProject` | （沒有宣告） | 1 |
| `CreateScoreSheetDraft` | `ScoreSheetDraftCreated` | 3 |
| `UpdateScoreSheetDraft` | `ScoreSheetDraftUpdated` | 4 |
| `PublishScoreSheetVersion` | `ScoreSheetVersionPublished` | 2 |

查詢的 use case 不發事件，不在範圍內。

## 二、查到的事實

**本節第 1～4 項是實測的**：本機在上表 20 個成功的範例加上 `emits: []`（斷言不發事件），跑完整測試，從失敗訊息讀出實際發出的事件；用完已還原。其餘是讀程式與文件。

1. 每個成功的範例都剛好發出一個事件，名稱與規格宣告的一致。
2. **沒有變更的成功也發事件**：
   - `activating_an_active_project_changes_nothing`（RP-ACT 的 `no_change`）發出 `ReviewProjectActivated`。
   - `deactivating_a_deactivated_project_changes_nothing` 發出 `ReviewProjectDeactivated`。
   - `accepts_an_empty_update`（SS-UPD-12，`no_change: true`）發出 `ScoreSheetDraftUpdated`。

   規格寫 `no_change: true`，但稽核紀錄會多一筆「已啟用」「已更新」。
3. **綁定評分表發出 `ScoreSheetBoundToProject`**，規格沒有宣告，`domain-events.md` 也沒有。它的 `detail` 是 `{"scoreSheetId":"<版本 id>"}`：鍵名是 `scoreSheetId`，值卻是評分表**版本**的 id（例如 `…:v1`），與 `BindScoreSheetToProject` 的輸入 `scoreSheetVersionId` 不同名。
4. **事件沒有規格詞彙的屬性**。九個事件類別都只有 `DomainEvent` 的共用屬性：`entityType`、`entityId`、`action`、`detail`（JSON 字串）、`hospitalId`、`actorUserId`、`actorUserName`、`eventId`、`occurredAt`。專案的 id 在 `entityId`；評分表事件的 `projectId` 只在 `detail` 的 JSON 裡。review-case 的事件類別另外提供規格詞彙的 getter（例如 `ReviewCaseCancelledEvent.getCaseId()` 回傳 `entityId`），範例的 `with` 才能寫 `caseId`。
5. `domain-events.md` §4 只有 `ReviewProjectCreated` 寫了最低必要欄位（`projectId`、`hospitalId`、`occurredAt`）；其他事件只有發布條件。`ReviewProjectDraftUpdated` 不在 `domain-events.md`。
6. **所有事件的 `occurredAt` 取系統時間**（`DomainEvent` 的 `Instant.now()`），不是應用程式注入的時鐘；範例的 `clock`、`advance` 控制不到。這是跨所有 aggregate 的現況，不只這兩個目錄（D-087 第 5 點記過資料庫時間的同類問題）。

## 三、問題與選項

### E-1：驗證到什麼程度

- **A（建議）：** 每個會發事件的 use case，至少一個成功的範例寫 `emits`，`with` 驗證事件屬於哪一筆資料、哪個院區（事實 4 的作法見 E-2）。沒有變更的情況另見 E-3。
- **B：** 只驗證事件名稱（`emits: [{ event: X }]`），不寫 `with`。不需要改程式，但驗不到事件是不是對到正確的專案。

### E-2：事件的屬性（事實 4）

- **A（建議）：** 比照 review-case：事件類別補上規格詞彙的 getter，範例的 `with` 用規格的名稱。
  - 專案的五個事件：`projectId`（回傳 `entityId`）。
  - 評分表的三個事件：`scoreSheetId`（回傳 `entityId`）、`projectId`。
  - 只是新增 getter，事件的內容與稽核紀錄不變。要一個建構 PR。
- **B：** `with` 直接寫 `entityId`、`hospitalId`。不用改程式，但 `entityId` 是實作的詞彙，違反「範例使用規格的詞彙」。

### E-3：沒有變更的成功也發事件（事實 2）

- **A（建議）：** 照程式立法：沒有變更也發事件，範例寫 `emits`。稽核紀錄看得到「有人按了啟用」，不算錯。
- **B：** 沒有變更時不發事件，與 `no_change: true` 一致。是行為變更，要建構 PR；稽核紀錄會少掉這幾筆。

### E-4：綁定評分表的事件（事實 3）

- **A（建議）：** 照程式在 `BindScoreSheetToProject` 的 `success.emits` 宣告 `ScoreSheetBoundToProject`，`domain-events.md` 補一節。`with` 用 E-2 的作法補 `projectId`、`scoreSheetVersionId` 的 getter（回傳現在 `detail` 裡的值）；`detail` 的鍵名 `scoreSheetId` 照舊，不改稽核紀錄的格式。
- **B：** A，另外把 `detail` 的鍵名改成 `scoreSheetVersionId`。稽核紀錄的格式會改變，已存的紀錄與新紀錄不一致。

### E-5：`domain-events.md`（事實 5）

- **建議：** 補上 `ReviewProjectDraftUpdated`、`ScoreSheetBoundToProject` 兩節；每個事件的「最低必要欄位」照 E-2 的 getter 寫。文件在 authority 的第 4 層（`docs/02-ddd/`），規格與它一致。

### E-6：`occurredAt`（事實 6）

- **A（建議）：** 本題不處理。跨所有 aggregate，另立題目「事件的發生時間改用注入的時鐘」，與 D-087 第 5 點的資料庫時間一起看。
- **B：** 本題一併改 `DomainEvent`。會影響所有 aggregate 的事件與稽核紀錄的時間，範圍超出本題。

### E-7：分批與題目清單

- **建議：** 修法 PR（規格、範例、`domain-events.md`；用到新 getter 的範例一開始是紅的，凍結）→ 建構 PR（getter）。兩個目錄 `not_covered` 的「事件不由範例驗證」刪除。題目清單：本題移到已完成；E-6 選 A 時新增「事件的發生時間」。
