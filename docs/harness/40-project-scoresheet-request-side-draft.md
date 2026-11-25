# review-project、score-sheet 的格式 v2 與請求端

狀態：**已裁決**（2026-11-25）。負責人裁決：V-1～V-8 全部照建議（V-1、V-3、V-7 選 A）；條文是 `decision-log.md` 的 D-089，以那裡為準。下面保留起草時的版本（main = `f1d332e`）。本文件不是律法；題號 V-1～V-8 只在本文件使用。

題目清單第 5 題（D-088 第 13 點）。D-088 的 D-1、D-2、E 已完成（#168～#175），問題側暫准的只剩本題的 7 個檔案。

## 一、範圍

| 範圍 | 檔案 | 現況 |
|---|---|---|
| controller | `interfaces/rest/ReviewProjectController.java`、`ScoreSheetController.java` | 回應由 HTTP 契約涵蓋（D-061、D-062），請求端未立法 |
| 請求 dto | `dto/CreateReviewProjectRequest`、`UpdateReviewProjectRequest`、`BindScoreSheetRequest`、`CreateScoreSheetDraftRequest`、`UpdateScoreSheetDraftRequest` | 未立法。必填的註解已在 #172 拿掉，use case 都有對應的規則與範例（D-088 第 13 點） |
| 規格 | `specs/review-project/`、`specs/score-sheet/` 的三個檔案 | 格式 v1，不能寫 `http` 步驟（D-074 只適用 v2） |

## 二、查到的事實

**格式升級是實測的**：本機把兩個目錄六個檔案的 `spec_version` 改成 2，跑 `spec_lint` 與完整的 `./gradlew test`，用完已還原。其餘是讀程式，沒有實測。

**升級到 v2**

1. `spec_lint` 只擋下 7 個問題，都是 v2 拿掉的欄位（`specs/README.md` 第十節）：
   - `review-project/model.yaml`：`ProjectVersion.qualityThreshold` 的 `range: [1, 10]`（註解「暫定」）；`RP-INV-04` 的 `enforced_by`。
   - `review-project/usecases.yaml`：`CreateReviewProject` 的 `success.output`。
   - `score-sheet/usecases.yaml`：`CreateScoreSheetDraft`、`PublishScoreSheetVersion` 的 `success.output`；`UpdateScoreSheetDraft` 的 `input_note`。
   - `review-project/examples.yaml`：`not_covered` 的 `rules: [emits]`（`emits` 不是規則 id）。
2. 執行器照 v2 跑，兩個目錄 84 個範例全部通過，結果與 v1 相同。完整測試 1775 個，失敗 0。
3. `qualityThreshold` 的 1～10 已由 RP-CRT-03 寫成規則，拿掉 `range` 不會少掉行為；`enforced_by` 只是說明。
4. v1 的範例不驗證事件（兩個目錄的 `not_covered` 都寫了）；v2 可以用 `emits` 驗證。本題不補，見 V-7。

**請求端**

5. **本文的列舉在 controller 轉換**，不認得的值回 400 `BAD_REQUEST`（`BadRequestException`），在角色檢查之前：
   - `ReviewProjectController` 的 `medicalRecordTypes`（建立、更新）。
   - `ScoreSheetController` 的 `scoreType`、`requiredRule`（新增題目、更新題目、新增章節裡的題目）。沒給（null）時交給 use case 回 `VALIDATION_ERROR`（SS-UPD-04）。

   與 D-088 第 13 點修正前的查詢參數相同：沒有權限的角色帶不認得的值，得到 400 不是 403。
6. 兩個 `usecases.yaml` 都沒有 `BAD_REQUEST` 這個錯誤代碼。
7. `ReviewProjectController.createProject` 建立之後再呼叫 `GetReviewProjectDetail` 取得 `status` 放進回應（api-spec §7.1）。建立與讀取的角色都是 `HOSPITAL_MANAGER`，目前不會有建立成功、讀取被拒的情況。
8. D-2 時寫過的 7 個經過 HTTP 的成功範例（建立、更新、綁定專案，專案列表，建立、更新評分表，評分表列表），在 v1 時被 `spec_lint` 擋下才拿掉。升級後可以直接用。
9. `law/coverage.txt` 有一行註解還寫著 `CloseWithoutRereviewRequest`、`SendToRereviewRequest`「沒有使用，仍是暫准」，兩個檔案已在 #175 刪除。

## 三、問題與選項

### V-1：分批

- **A（建議）：** 視 V-3 而定。
  - V-3 選 A（行為變更）：修法 PR 一（升級、規則、範例，紅的凍結）→ 建構 PR → 修法 PR 二（登記）。
  - V-3 選 B（照程式）：一個修法 PR 做完（升級、範例、登記），不需要建構 PR。
- **B：** 升級與請求端分成兩份裁決。升級本身很小，分開沒有好處。

### V-2：升級的作法

- **建議：** 只做 `spec_lint` 要求的 7 處，不改其他寫法：
  - `range` 刪除（RP-CRT-03 已涵蓋）；`enforced_by` 刪除，內容移到註解。
  - `success.output` 改成 v2 的 `output.fields`；`input_note` 移到註解。
  - `not_covered` 的 `rules: [emits]` 改成 `item: 事件`。
  - 三個檔案的 `spec_version` 改成 2。範例的寫法不變，行為也不變（事實 2）。

### V-3：本文的列舉（事實 5）

- **A（建議）：** 比照 D-088 第 13 點（H-5）：轉換移進 use case，順序依 D-024（角色 → 載入 → 院區範圍 → 輸入），代碼維持 `BAD_REQUEST`。兩個 `usecases.yaml` 新增錯誤代碼 `BAD_REQUEST` 與對應的規則。行為變更：沒有權限的角色帶不認得的值，從 400 變成 403。規格的輸入型別不改，行為以 `http` 步驟的範例表達（與 H-5 相同）。
  - score-sheet 的 `scoreType`、`requiredRule` 在六相位命令裡（SS-UPD 的檢查順序）。不認得的值要放在相位裡的哪一步，建議與「沒給」（SS-UPD-04）同一步，代碼不同。
- **B：** 留在 controller，用 `http` 範例把現況（400 在 403 之前）寫成律法。不需要建構 PR，但與其他 controller 不一致。

### V-4：經過 HTTP 的成功範例（H-8）

- **建議：** 用事實 8 的 7 個範例，確認請求的欄位有傳到 use case。

### V-5：建立專案的回應（事實 7）

- **建議：** 照程式，不改。controller 組合兩支 use case 只是為了回應的形狀；若之後讀取的角色與建立不同，再另外處理。

### V-6：登記與註解

- **建議：** 兩個 controller 與 5 個請求 dto 登記為已立法（各自的規格目錄）；`dto/` 與 controller 的暫准列改成已立法或刪除，問題側不再有 `grandfathered` 的列。`law/coverage.txt` 檔頭的 `grandfathered` 說明保留（格式仍支援）。事實 9 的註解一併刪除。

### V-7：事件的驗證（事實 4）

- **A（建議）：** 本題不補。兩個目錄的 `not_covered` 維持「事件不由範例驗證」，另立題目。
- **B：** 升級時一併補上 `emits`。規格宣告的事件 review-project 有 5 種、score-sheet 有 3 種；程式另外在綁定評分表時發出 `ScoreSheetBoundToProject`，規格沒有宣告。要先確認每一種的欄位，範圍會變大。

### V-8：題目清單

- **建議：** 本題移到已完成；V-7 選 A 時新增「review-project、score-sheet 的事件驗證」；D-088 移到已完成（#168～#175）。
