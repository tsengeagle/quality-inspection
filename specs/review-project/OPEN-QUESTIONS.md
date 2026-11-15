# ReviewProject 規格的待裁決問題

本檔只列**目前還沒裁決**、而且規格裡有規則或欄位指向它的題目。`proposed` 規則的 `question` 指向這裡的標題。

- 已裁決的題目寫在 `docs/01-requirements/decision-log.md`：Q-01～Q-14 的大部分在 D-039～D-043、D-046、D-047；Q-08 的錯誤代碼與 Q-15 在 D-049（2026-11-15）；Q-12 在 D-051（2026-11-15）。
- 草稿時期的完整題目原文與程式現況，見升格前的 `spec-draft/review-project/OPEN-QUESTIONS.md`（git 歷史）。
- 裁決後：先把決定寫進 `decision-log.md`，再把對應規則改成 `status: decided` 並補 `source`，最後刪除本檔的該題。三者在同一個修法 PR。

## 題目

目前沒有待裁決的題目。Q-12（`REVIEWER`／`REREVIEWER` 能不能讀專案詳情）於 2026-11-15 裁決為不開放，見 D-051。

## 不指向規則、但仍未決的事

| 事項 | 出處 | 影響 |
|---|---|---|
| 初審總分的量尺。文件範例的品質線是 80，與 D-046 的整數 1～10 不一致 | D-046 Pending | 品質線範圍是暫定值；`RP-CRT-03`、`RP-UPD-04` 的範圍可能要改 |
| 綁定時是否要求評分表版本存在且已發布 | D-041 Pending | 評分表規格轉換時處理；`bound_project` fixture 目前假設綁定的是已發布的版本 |
| 「建立新草稿版本」的 use case | D-040 Pending | `RP-INV-01` 說要調整必須建立新版本，但目前沒有這支 use case |
