# ReviewCase 規格的待裁決問題

本檔只列**目前還沒裁決**、而且規格裡有規則或欄位指向它的題目。`proposed` 規則的 `question` 指向這裡的標題。

- 題號沿用試寫（`docs/harness/samples/reviewcase-v2-trial/`）。Q-04 已由 D-066 第 3 點裁決；Q-01（後三列）、Q-02、Q-03、Q-05、Q-07 已由 D-067 裁決。Q-08 由 D-069 新增。
- 裁決後：先把決定寫進 `decision-log.md`，再把對應規則改成 `status: decided` 並補 `source`，最後刪除本檔的該題。三者在同一個修法 PR。

## 題目

### Q-08

複審事件的最低必要欄位。`domain-events.md` §6.6 `RereviewerAssigned`、§6.7 `RereviewStarted`、§6.8 `RereviewCompleted`、§6.9 `ReviewCaseClosed` 只有發布條件，沒有最低必要欄位。依 D-069（S-4，照 D-066 第 2 點的做法），規格只斷言發出事件的案件 `caseId`，標 `proposed`：`RC-ARR-S2`、`RC-STR-S3`、`RC-SRR-S3`、`RC-CLR-S2`。

要決定：這四個事件各帶哪些欄位（例如 `RereviewerAssigned` 的 `rereviewerId`、`dueAt`；`RereviewCompleted` 的 `submissionId`；`ReviewCaseClosed` 的 `submissionId`、結案路徑）。欄位會原樣進稽核紀錄，不得讓複審醫師經由任何讀取取得初審分數或 `finalScore`（D-033）。裁決後補進 `domain-events.md`，四條規則改成 `decided`。
