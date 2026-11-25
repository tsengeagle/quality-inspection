# ReviewCase 切片 D：修正已提交的初審與複審，並立法 `ReviewCase.java`

狀態：**已裁決**（2026-11-24）。負責人對 U-1～U-10 全部照建議裁決；條文是 `decision-log.md` 的 D-083，以那裡為準。下面保留起草時的版本（main = `623552a`）。本文件不是律法；題號 U-1～U-10 只在本文件使用。

負責人 2026-11-24 決定把 `ReviewCase.java` 提早到本切片一起立法（原本排在切片 E 之後）。

## 一、範圍

| use case | 端點 | 角色 | 程式的條件 | 事件（action） | 立法狀態 |
|---|---|---|---|---|---|
| `ReviseInitialReview` | `POST /review-cases/{caseId}/initial-review/submissions/{submissionId}/revise`，body `answers`、`comment` | `REVIEWER`，本院，提交的醫師 | 案件未終止；提交是本案、`INITIAL`、`SUBMITTED`、目前有效；初審**未固定** | `ReviewSubmissionRevised`（`ReviewSubmission` 的 `REVISED`） | 未立法 |
| `ReviseRereview` | `POST /review-cases/{caseId}/rereview/submissions/{submissionId}/revise` | `REREVIEWER`，本院，受指派且是提交者 | 案件未終止；提交是本案、`REREVIEW`、`SUBMITTED`、目前有效 | `RereviewRevised`（`ReviewCase` 的 `REREVIEW_REVISED`，帶分數，D-071 遮罩） | 未立法 |

一併處理的元件：`command/ReviseRereviewCommand.java`、`RevisionResult.java`、`RereviewRevisedEvent.java`、`submission/event/ReviewSubmissionRevisedEvent.java`；以及 `domain/reviewcase/` 的 `ReviewCase.java`、`CaseStatus.java`、`ReviewerAssignment.java`、`ReviewCaseRepository.java`。

## 二、查到的事實

**實測（2026-11-24，暫時的規格範例，走真實寫入與查詢路徑，跑完已還原，沒有提交）**：

| # | 情境 | 結果 |
|---|---|---|
| 1 | 初審完成、未固定，原醫師修正（RANGE 題 5 → 3） | 成功；輸出 `totalScore = 3`；修正歷史：舊的 `SUPERSEDED`（`supersededBySubmissionId` 指新的）、新的 `SUBMITTED`、`revisionNo = 2` |
| 2 | 初審已固定後修正 | 409 `INVALID_STATE_TRANSITION` |
| 3 | 別的醫師修正 | 403 `NOT_SUBMISSION_OWNER` |
| 4 | 答案與原本相同 | **成功**，多一個 revision |
| 5 | 答案是空的（必答題沒答） | **成功**；提交初審會拒絕 `REQUIRED_ITEMS_NOT_ANSWERED`（D-067） |
| 6 | 用已被取代的舊提交修正 | 409 `SUBMISSION_NOT_SUBMITTED`（舊提交是 `SUPERSEDED`） |
| 7 | 初審中（草稿）修正 | 409 `SUBMISSION_NOT_SUBMITTED` |
| 8 | 提交 ID 不存在 | 404 `SUBMISSION_NOT_FOUND` |
| 9 | 案件已取消 | 409 `CASE_TERMINAL` |
| 10 | 複審已提交，修正（加權 0.5／0.5，初審 5、複審改 5） | 成功；稽核 `REREVIEW_REVISED` `{"newSubmissionId":"sub-rev-…","oldSubmissionId":"…","rereviewScore":5,"finalScore":5.00}`；案件維持 `REREVIEW_IN_PROGRESS` |
| 11 | 複審草稿修正 | 409 `SUBMISSION_NOT_SUBMITTED` |
| 12 | 用初審的提交 ID 修正複審 | 409 `CONFLICT` |
| 13 | 複審答案是空的 | **成功**；提交複審會拒絕（D-076） |
| 14 | 已結案 | 409 `CASE_TERMINAL` |

稽核紀錄：

- 修正初審只寫 `ReviewSubmission` 的 `REVISED`（`{"caseId","oldSubmissionId","stage":"INITIAL","revisionNo":2}`，不帶分數），`ReviewCase` 沒有紀錄，雖然案件的 `finalScore` 變了。
- 修正複審只寫 `ReviewCase` 的 `REREVIEW_REVISED`，**沒有** `ReviewSubmission` 的 `REVISED`。兩邊不對稱。

**讀程式確認**：

- 修正初審：提交不是本案時丟 `BadRequestException`（400，代碼是一般的請求錯誤）；不是 `INITIAL` 時 422 `SUBMISSION_NOT_INITIAL`（規格沒有這個代碼）。修正複審對同樣的情況都回 409 `CONFLICT`。
- 修正複審收 command（`ReviseRereviewCommand`），規格範例無法執行；新提交 ID 前綴 `sub-rev-`，其他提交都是純 UUID。
- 修正複審的加權取專案**目前**的版本，與提交複審相同（D-068 第 4 點的已知問題，`deferred_behaviors`）。
- `ReviseRereview` 以 `setLatestRereviewSubmissionId`（公開的 setter）改指新提交，「目前有效」的檢查在 application 層；修正初審則由 domain 的 `recordInitialReviewRevision` 檢查。`updateFinalScore` 也是公開的，只有 domain 自己用。
- 兩個事件類別都沒有 getter；`RereviewRevised` 的 `detail` 在 use case 以 `String.format` 組成。
- `api-spec.md` §11.4、§12.4 的回應範例沒有 `totalScore`、`status`；程式有回。

**`ReviewCase.java` 盤點**：

- 狀態轉移全部已在 `model.yaml`（RC-T-01～21）；每個公開的行為方法都對應到已立法或本切片的 use case。
- 從來沒有被行為方法寫入的欄位：`rereviewRequired`（D-054 後進複審改在固定時判斷，提交初審的回應永遠是 `false`）、`finalConclusion`、`ruleSnapshotRef`。只由 `reconstitute` 從資料庫讀回。
- `model.yaml` 的 `invariants` 只有一條（SYSTEM_ADMIN 的讀取），沒有寫 `finalScore` 的計算與「固定後不得修正初審」等規則；這些在 use case 的規則與範例裡。

## 三、修正（切片 D）

### U-1：修正時的必答題

- **A（建議）：** 與提交相同，必答題沒答回 422 `REQUIRED_ITEMS_NOT_ANSWERED`（D-067、D-076）。修正直接成為 `SUBMITTED`，不檢查會留下不完整的有效結果。
- **B：** 照程式，不檢查。

### U-2：沒有變更的修正

- **A（建議）：** 答案與評語都和原本相同時拒絕，409 `NO_CHANGES`（比照 D-038 的草稿）。
- **B：** 照程式，允許，多一個 revision。

### U-3：稽核紀錄

- **A（建議）：** 修正複審也發 `ReviewSubmissionRevised`（`REVISED`，不帶分數），與修正初審對稱；`REREVIEW_REVISED` 照舊（帶分數、遮罩）。修正初審不另寫 `ReviewCase` 的紀錄。
- **B：** 照程式。

### U-4：提交不屬於本案或階段不對

- **建議：** 兩個修正一律 409 `CONFLICT`（D-054 第 6 點「目標不是本案目前有效的結果」）；`SUBMISSION_NOT_INITIAL` 與 400 的請求錯誤停用。

### U-5：事件與輸出

- **建議：**
  - `ReviewSubmissionRevised` 帶 `caseId`、`oldSubmissionId`、`newSubmissionId`、`stage`、`revisionNo`；`RereviewRevised` 帶 `caseId`、`oldSubmissionId`、`newSubmissionId`、`rereviewScore`、`finalScore`。兩者 `detail` 格式不變，以 JSON 序列化產生。
  - 輸出 `{ newSubmissionId, oldSubmissionId, revisionNo, totalScore, status, stage, caseId }`（照程式）；`api-spec.md` 補上 `totalScore`、`status`。修正的人只看得到自己階段的分數，不違反 D-033。

### U-6：輸入名稱與 ID

- **建議：** `ReviseRereview` 改收參數（`caseId`、`submissionId`、`answers`、`comment`），`answers` 與修正初審同型別；`ReviseRereviewCommand` 移除。新提交 ID 一律是 UUID（去掉 `sub-rev-`）。

## 四、`ReviewCase.java`

### U-7：登記範圍

- **建議：** `ReviewCase.java`、`CaseStatus.java`、`ReviewerAssignment.java`、`ReviewCaseRepository.java` 登記為已立法（`specs/review-case/`）。`domain/reviewcase/` 目錄那一列仍是 grandfathered，涵蓋 `submission/`（`ReviewSubmission.java` 照原順序最後處理）。

### U-8：規格補上不變式

- **建議：** `model.yaml` 的 `invariants` 補上（都有既有裁決）：
  - `finalScore`：複審前等於有效初審分數；複審提交或修正後 = 初審分數 × 初審權重 + 複審分數 × 複審權重，兩位小數（D-012、D-020、D-032）。
  - 初審固定後不得修正初審（D-031、D-054）；`CLOSED`、`CANCELLED` 後不得修正或再審（D-006、D-032）。
  - 案件的初審醫師與複審醫師不是同一人（D-017）。

### U-9：「目前有效的複審提交」移進 domain

- **建議：** 新增 domain 方法記錄複審修正（檢查舊提交是目前有效的、重算 `finalScore`、改指新提交），對應修正初審的 `recordInitialReviewRevision`；`setLatestRereviewSubmissionId`、`updateFinalScore` 不再公開。行為不變。

### U-10：沒有被寫入的欄位

- **A（建議）：** `rereviewRequired`、`finalConclusion`、`ruleSnapshotRef` 不在本次立法範圍，列進 `deferred_behaviors`，等讀取（切片 E）決定要不要從回應與資料表移除。
- **B：** 本切片一併移除（牽動資料表與回應，範圍變大）。
