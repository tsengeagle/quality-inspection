# D-069 留下的兩件：複審的提交事件與必答題檢查

狀態：**已裁決**（2026-11-23）。負責人對 V-1～V-5 全部照建議裁決，條文是 `decision-log.md` 的 D-076，以那裡為準。本文件不是律法，下面保留起草時的版本（main = `3411657`）。題號 V-1～V-5 只在本文件使用。

## 一、背景

D-069「不在本裁決」留下兩件，題目清單排在下一題：

- `domain-events.md` §7.1、§7.2 的 `ReviewDraftSaved`、`ReviewSubmissionSubmitted` 寫的是「某一審查階段」的事件，程式在複審沒有發出。規格的提交複審範例因此只能用 `emits_include`，不斷言完整的事件清單。
- 提交複審沒有必答題檢查。初審有（D-067，`REQUIRED_ITEMS_NOT_ANSWERED`）。

## 二、查到的事實

**文件怎麼寫**
- §7.1 `ReviewDraftSaved`：「某一審查階段草稿已保存」；補充寫「是否發布此事件，可視系統是否需要草稿稽核追蹤決定」。最低必要欄位是 `submissionId`、`caseId`；草稿分數不是必要欄位（D-067 第 6 點）。
- §7.2 `ReviewSubmissionSubmitted`：「某一審查階段的提交已正式成立」。最低必要欄位有 `stage`、`reviewerId`、`totalScore`。
- `api-spec.md` §4.2：`REQUIRED_ITEMS_NOT_ANSWERED`（422）的說明是「提交初審時有必答題沒有作答（D-067）」。

**程式現況**

| | 初審 | 複審 |
|---|---|---|
| 保存草稿 | 發 `ReviewDraftSaved`（稽核 `DRAFT_SAVED`） | 不發任何事件 |
| 提交 | 發 `ReviewSubmissionSubmitted`（稽核 `SUBMITTED`）與 `InitialReviewCompleted` | 只發 `RereviewCompleted`（稽核 `REREVIEW_SUBMITTED`） |
| 必答題 | 檢查 `ALWAYS_REQUIRED` 的題目，缺就回 `REQUIRED_ITEMS_NOT_ANSWERED` | 不檢查 |

- 兩個事件類別都沒有 `stage`。`ReviewSubmissionSubmittedEvent` 的 `detail` 是 `{"caseId","totalScore","reviewerId"}`；`ReviewDraftSavedEvent` 的 `detail` 是 `{"caseId","totalScore"}`。
- 複審草稿的分數不是輸入，提交時才由系統計算（D-069 第 7 點）。保存草稿時草稿上的分數是舊值或 0。
- 提交複審的答案：請求有帶 `answers` 就用它，沒有帶就用草稿的。初審只用草稿的。
- 必答題檢查只看 `ALWAYS_REQUIRED`；`CONDITIONAL` 的題目在初審也沒有檢查。

**稽核的遮罩**
- `ScoredDetailMask`（D-071）只遮 `entityType` 是 `ReviewCase` 的五個 action。
- `DRAFT_SAVED`、`SUBMITTED` 的 `entityType` 是 `ReviewSubmission`，`detail` 帶 `totalScore`，不在遮罩範圍。也就是說，初審的這兩種紀錄現在就帶著沒有遮罩的初審分數。
- `ListAuditTrails` 的角色是 `SYSTEM_ADMIN`、`HOSPITAL_MANAGER`、`AUDITOR`；D-071 的起因正是同一個使用者可能以 `AUDITOR` 讀取、之後受派為複審醫師。

**律法涵蓋**
- `SaveRereviewDraft`、`SubmitRereview` 已立法（D-069），有規則 `RC-RDR-*`、`RC-SRR-*`。
- 修正已提交的初審與複審（`ReviseInitialReview`、`ReviseRereview`）是 `grandfathered`，不在本題。

**還沒有查證的**
- `ListAuditTrails` 與 `GetCaseLifecycle` 實際上會不會回傳 `ReviewSubmission` 的紀錄。上面「初審分數沒有遮罩」是從程式讀出來的推論，沒有實際打過。
- D-071 當時是刻意不納入 `ReviewSubmission` 的紀錄，還是漏掉。

## 三、V-1：保存複審草稿要不要發 `ReviewDraftSaved`

### 選項

- **A（建議）：發，與初審一致。** §7.1 寫的是「某一審查階段」，照字面複審也算。欄位是 `submissionId`、`caseId`。規格的 `SaveRereviewDraft` 加上 `emits`。
- **B：不發，把 §7.1 改成只適用初審。** 複審的草稿就沒有稽核紀錄。

### 選 A 時的細節

`detail` 的 `totalScore`：複審草稿沒有有意義的分數。建議寫 `null`（格式不變，仍是 `{"caseId":"…","totalScore":null}`），不寫舊值或 0，避免稽核紀錄出現看起來像分數的假值。

### 待負責人決定

A／B？選 A 的話，`totalScore` 寫 `null` 可以嗎？

## 四、V-2：提交複審要不要發 `ReviewSubmissionSubmitted`

### 選項

- **A（建議）：發，與初審一致。** 提交複審發兩個事件：`ReviewSubmissionSubmitted` 與 `RereviewCompleted`，與初審（`ReviewSubmissionSubmitted` 與 `InitialReviewCompleted`）對稱。規格的 `SubmitRereview` 把 `emits` 寫完整，範例從 `emits_include` 改成 `emits`。
- **B：不發，把 §7.2 改成只適用初審。** 提交複審只有 `RereviewCompleted`。

### 選 A 時的細節

- §7.2 的最低必要欄位有 `stage`，但程式的事件沒有這個欄位，初審也沒有。建議事件加上 `stage` 的取得方法（`INITIAL`／`REREVIEW`），`detail` 逐字不變（D-066 第 2 點）。
- 稽核會多一筆 `SUBMITTED`，`detail` 帶複審的 `totalScore`。見 V-3。

### 待負責人決定

A／B？`stage` 要不要一起補？

## 五、V-3：`ReviewSubmission` 的稽核紀錄沒有遮罩

這是查證時發現的，不在 D-069 留下的兩件裡。

- 現況：初審的 `DRAFT_SAVED`、`SUBMITTED` 帶初審分數，不在 D-071 的遮罩範圍。
- V-1、V-2 選 A 之後，複審也會寫入這兩種紀錄；`SUBMITTED` 帶複審分數。

### 選項

- **A（建議）：另開一題，排在本題之後。** 先查證兩個出口會不會回傳這些紀錄，再決定要不要擴大 D-071 的遮罩。遮罩現在以案件編號判斷，`ReviewSubmission` 的紀錄記的是提交編號，要多一層對照，不是加一行就能解決。本題照 V-1、V-2 進行，不擴大遮罩。
- **B：併入本題。** 範圍變大，而且動到已立法的 D-071。
- **C：本題先不做 V-1、V-2**，等遮罩的題目裁決後再做，避免先多寫入帶分數的紀錄。

### 待負責人決定

A／B／C？

## 六、V-4：提交複審的必答題檢查

### 選項

- **A（建議）：與初審相同。** 評分表有 `ALWAYS_REQUIRED` 的題目沒有作答，回 `REQUIRED_ITEMS_NOT_ANSWERED`（422，`rule_violation`）。
  - 檢查的是這次提交實際採用的答案：請求有帶 `answers` 就看它，沒有帶就看草稿。
  - 檢查的順序放在狀態、指派、提交歸屬的檢查之後，計分之前，與初審相同。
  - `api-spec.md` §4.2 的說明改成「提交初審或複審時」。
- **B：複審不檢查。** 維持現狀，在規格明文寫出，並說明理由。

### 待負責人決定

A／B？

## 七、V-5：不在本題的

### 建議

下列維持現狀，不在本題處理：

- `CONDITIONAL` 的必答規則：初審與複審都沒有檢查，屬於評分表規則的題目。
- 修正已提交的初審與複審（`ReviseInitialReview`、`ReviseRereview`）：還沒有立法，它們發不發這兩個事件、有沒有必答題檢查，等修正立法時處理。

### 待負責人決定

同意嗎？

## 八、裁決後的順序

1. **修法 PR**：
   - `decision-log.md` 新增一條；
   - `specs/review-case/usecases.yaml`：`SaveRereviewDraft`、`SubmitRereview` 的 `emits` 與新規則；`examples.yaml` 新增與修改範例，移除 `not_covered` 的對應項目；
   - `domain-events.md` §7.1、§7.2，`api-spec.md` §4.2；
   - 一開始是紅的範例加進 `known-test-failures.txt`（D-066 第 5 點）；
   - 本草稿標為已裁決，更新題目清單。
2. **建構 PR**：兩支 use case 與兩個事件類別；移除 `known-test-failures.txt` 的項目。
