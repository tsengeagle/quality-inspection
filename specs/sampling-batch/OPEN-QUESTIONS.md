# SamplingBatch 規格的待裁決問題

本檔只列**目前還沒裁決**、而且規格裡有規則指向它的題目。`proposed` 規則的 `question` 指向這裡的標題。

- 題號沿用試寫（`docs/harness/samples/reviewcase-v2-trial/`）。Q-01～Q-03、Q-05 在 `../review-case/OPEN-QUESTIONS.md`。
- 試寫時的 Q-04（抽選時專案不存在的代碼）已由 D-066 第 3 點裁決為 `PROJECT_NOT_FOUND`。
- 裁決後：先把決定寫進 `decision-log.md`，再把對應規則改成 `status: decided` 並補 `source`，最後刪除本檔的該題。三者在同一個修法 PR。

## 題目

### Q-06

**`SamplingCandidatesPrepared`、`ReviewCasesSelected` 帶哪些欄位。**

`domain-events.md` §5.2、§5.3 沒有「最低必要欄位」，只在發布條件提到 `populationCount`、`eligibleCount`、`excludedCount`。依 D-066 第 2 點，規格先只斷言發出者的 identity（`batchId`），標 `proposed`（`SB-EXE-S4`）。

要裁決的是：這兩個事件的欄位清單。裁決後補進 `domain-events.md`，`SB-EXE-S4` 改成 `decided`。

程式（2026-11-21）：這兩個事件類別沒有自己的屬性，內容在 `detail`（JSON 字串）：`SamplingCandidatesPrepared` 是 `populationCount`、`eligibleCount`、`excludedCount`；`ReviewCasesSelected` 是 `selectedCount`。
