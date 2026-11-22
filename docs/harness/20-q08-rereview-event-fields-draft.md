# Q-08：四個複審事件的最低必要欄位

狀態：**已裁決**（2026-11-22）。負責人對 E-1～E-4（含 E-3-2）全部照建議裁決，條文是 `decision-log.md` 的 D-070，以那裡為準。本文件不是律法，下面保留起草時的版本（main = `9766a59`）。題號 E-1～E-4 只在本文件使用。

## 一、背景

- D-069 第 4 點：`domain-events.md` §6.6～§6.9 的 `RereviewerAssigned`、`RereviewStarted`、`RereviewCompleted`、`ReviewCaseClosed` 只有發布條件，沒有最低必要欄位。規格只斷言 `caseId`，四條規則標 `proposed`（`RC-ARR-S2`、`RC-STR-S3`、`RC-SRR-S3`、`RC-CLR-S2`），題目是 `specs/review-case/OPEN-QUESTIONS.md` 的 Q-08。
- #129 之後，四個事件類別都有 `getCaseId()`，其他內容只在 `detail`（JSON 字串）。
- 做法照 D-066 第 2 點與 D-069 第 4 點：欄位用取得方法提供，`action` 與 `detail` 逐字不變，稽核紀錄不變。

## 二、總覽

| 題 | 事件 | 建議的欄位 | 程式現在的 `detail` |
|---|---|---|---|
| E-1 | `RereviewerAssigned` | `caseId`、`rereviewerId`、`dueAt` | `{"rereviewerId":…}`；改派另加 `"reassignment":true` |
| E-2 | `RereviewStarted` | `caseId`、`submissionId`、`rereviewerId` | `{"submissionId":…}` |
| E-3 | `RereviewCompleted` | `caseId`、`submissionId`、`rereviewerId`、`rereviewScore`、`finalScore` | `{"rereviewScore":…,"finalScore":…}` |
| E-4 | `ReviewCaseClosed`（複審路徑） | `caseId`、`submissionId` | `{"submissionId":…}` |

四題都只加取得方法，`detail` 不變。對照的是初審的三個事件：§6.2 `ReviewCaseAssigned`（`caseId`、`assignedReviewerId`、`dueAt`）、§6.3 `InitialReviewStarted`（`caseId`、`submissionId`、`reviewerId`，D-067）、§6.4 `InitialReviewCompleted`（`caseId`、`projectId`、`submissionId`、`reviewerId`、`totalScore`、`qualityThreshold`）。

## 三、E-1：`RereviewerAssigned`

### 查到的事實

| 來源 | 內容 |
|---|---|
| 發出者 | `AssignRereviewerUseCase:55`（`{"rereviewerId":"%s"}`）、`ReassignRereviewerUseCase:56`（`{"rereviewerId":"%s","reassignment":true}`）。改派不在立法範圍 |
| 對照 | §6.2 指派初審醫師用 `assignedReviewerId`、`dueAt` |
| 稽核的讀取 | `PermanentBlindReviewReadApiTest:292`、`:450` 依 `action = REREVIEWER_ASSIGNED` 找出指派紀錄 |

### 選項

| 選項 | 欄位名稱 | 說明 |
|---|---|---|
| **A. `rereviewerId`**（建議） | `caseId`、`rereviewerId`、`dueAt` | 與 `detail` 的鍵、`CaseSummary.rereviewerId`、指派的輸入名稱相同 |
| B. `assignedRereviewerId` | `caseId`、`assignedRereviewerId`、`dueAt` | 與 §6.2 的命名對稱；但規格其他地方都叫 `rereviewerId` |

- 改派要不要帶 `reassignment`：改派不在立法範圍，這次不決定。建構 PR 讓改派也傳入同樣的三個欄位，`detail` 照舊。

### 待負責人決定

- **E-1**：採 A？

## 四、E-2：`RereviewStarted`

- 程式的 `detail` 已有 `submissionId`。開始複審的操作者就是受指派的複審醫師（`RC-STR-01`），所以 `rereviewerId` 等於操作者。
- 照 §6.3（D-067）：`caseId`、`submissionId`、`rereviewerId`。

### 待負責人決定

- **E-2**：採這三個欄位？

## 五、E-3：`RereviewCompleted`

### 查到的事實

| 來源 | 內容 |
|---|---|
| 程式的 `detail` | `{"rereviewScore":%s,"finalScore":%s}`（`SubmitRereviewUseCase:143`） |
| D-033 | 複審醫師不得取得初審分數或可反推的資訊。`finalScore` 依 D-012／D-020 由初審與複審分數加權，知道 `rereviewScore`、`finalScore` 與權重，就能算出初審分數 |
| `domain-events.md` §14 | 「事件使用 domain 正確分數，不改 D-012／020；事件衍生的使用者可見 audit／projection 仍須依 D-033 防止洩漏」 |
| 稽核的讀取（2026-11-22 查證） | `GetCaseLifecycleUseCase:33`：持有 `REREVIEWER` 或 `SYSTEM_ADMIN` 的人拿不到 `detail`。`ListAuditTrailsUseCase:26`：持有 `REREVIEWER` 的人拿不到 `detail`。事件本身不會回給複審醫師 |
| 規格 | `deferred_behaviors` 與 `not_covered` 列了「複審提交的 `finalScore` 加權沒有範例」：沒有輸出 `finalScore` 的查詢 |

### 選項

| 選項 | 欄位 | 優點 | 缺點 |
|---|---|---|---|
| **A. 帶兩個分數**（建議） | `caseId`、`submissionId`、`rereviewerId`、`rereviewScore`、`finalScore` | 與 `detail`、§14 一致；規格可以用 `with` 斷言 `finalScore`，D-012／D-020 的加權第一次有範例 | 事件帶有可反推初審分數的值；靠稽核讀取的遮罩保護（現況已是如此） |
| B. 不帶分數 | `caseId`、`submissionId`、`rereviewerId` | 事件的屬性不含敏感值 | `detail` 仍帶兩個分數，屬性與 `detail` 不一致；加權仍然沒有範例 |

### 建議：A

- 規格：`RC-SRR-S3` 改成帶五個欄位，`decided`；範例 `rereview_completed_carries_the_case` 斷言 `finalScore`。目前的 fixture 初審 20 分、複審 20 分、權重 0.50／0.50，`finalScore` 是 20.00，看不出加權。建議範例改用 `low_score_completed_case` 的 RANGE 題目專案：初審 5 分、手工送複審、複審作答 15 分，`finalScore` = 5 × 0.50 + 15 × 0.50 = 10.00。
- `deferred_behaviors` 與 `not_covered` 的「`finalScore` 加權沒有範例」刪除。

### 待負責人決定

- **E-3-1**：採 A？
- **E-3-2**：加權的範例改用 RANGE 題目的專案（初審 5、複審 15，`finalScore` 10.00）？

## 六、E-4：`ReviewCaseClosed`

### 查到的事實

| 來源 | 內容 |
|---|---|
| §6.9 | 兩條結案路徑共用一個事件：不經複審結案、複審結案 |
| 程式 | 複審結案是 `ReviewCaseClosedEvent`（`action = CASE_CLOSED_AFTER_REREVIEW`，`detail` 是 `{"submissionId":…}`）；不經複審結案是另一個類別 `CaseClosedWithoutRereviewEvent`（`CASE_CLOSED_WITHOUT_REREVIEW`），不在立法範圍 |
| D-053 第 3 點 | 結案即固定；有效版本是結案當下的 `latestRereviewSubmissionId` |

### 建議

- 欄位：`caseId`、`submissionId`（結案時固定的那份複審提交）。
- 不加「結案路徑」欄位：目前只有複審路徑立法。兩條路徑要不要合成一個類別、帶不帶路徑欄位，等不經複審結案立法時決定（那時 `submissionId` 是被固定的初審提交）。

### 待負責人決定

- **E-4**：採 `caseId`、`submissionId`，結案路徑延後？

## 七、順帶查到、不在本題

- **稽核的 `detail` 不算「讀過完整初審」。** D-055 第 5 點列的讀取是案件詳情、初審提交明細、revision 歷程中未遮罩的初審內容，只有這些會寫入讀取者紀錄。稽核的 `detail` 也帶初審分數（`InitialReviewCompleted` 的 `totalScore`、`ReviewCaseEscalatedToRereview` 的 `score`），`HOSPITAL_MANAGER`、`AUDITOR`、`REVIEWER` 在生命週期查詢拿得到，卻不留下讀取者紀錄。只要讀的人當時沒有 `REREVIEWER` 角色、之後才被指派為複審醫師，就繞過了 D-055 第 5 點。E-3 選 A 或 B 都不影響這件事。建議另案處理。
- **D-069 留下的兩件**（`ReviewDraftSaved`、`ReviewSubmissionSubmitted` 在複審沒有發出；提交複審沒有必答題檢查）不在本題。要一起裁決的話，可以併進這份草稿。

## 八、裁決後的順序

1. **規格的修法 PR**：`decision-log.md` 新增一條；`domain-events.md` §6.6～§6.9 補最低必要欄位；`specs/review-case/` 的四條規則改成 `decided`，範例的 `with` 補欄位（E-3-2 另加 fixture），`OPEN-QUESTIONS.md` 刪除 Q-08。紅的範例列進已知失敗清單。
2. **建構 PR**：四個事件類別加欄位與取得方法（建構式多收這些值），發出者傳入；改派一起傳入。`action`、`detail` 不變，`RereviewSliceEventPropertiesTest` 補上新欄位。改完清空已知失敗清單。

### 還沒有查證的

- E-3-2 的範例：手工送複審之後，複審的評分表是案件的 `scoreSheetVersionRef`，應該也是 RANGE 題目；要在修法 PR 裡實際跑過才確定。
