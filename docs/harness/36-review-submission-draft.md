# 切片 C：`ReviewSubmission` 本體、計分，以及 ReviewCase 的收尾

狀態：**已裁決**（2026-11-25）。負責人對 T-1～T-14 全部照建議裁決；條文是 `decision-log.md` 的 D-085，以那裡為準（T-13 實測後只補得出 RC-FIX-04、RC-CLR-07，見 D-085 第 14 點）。下面保留起草時的版本（main = `d80939e`）。本文件不是律法；題號 T-1～T-14 只在本文件使用。

負責人 2026-11-25 同意問題側其餘範圍的順序：C（本切片）→ A SamplingBatch → B 報表 → D、E HTTP 介面與共用元件 → 專案版本管理與未裁決事項。本切片的修法 PR 一併把這個順序寫進題目清單。

## 一、範圍

| 範圍 | 檔案 | 立法狀態 |
|---|---|---|
| `ReviewSubmission` 本體 | `domain/reviewcase/submission/` 的 `ReviewSubmission.java`、`AnswerItem.java`、`DraftContent.java`、`ReviewStage.java`、`SubmissionStatus.java`、`ReviewSubmissionRepository.java` | 未立法。`specs/review-submission/model.yaml` 只有狀態與四個轉移，修正造成的 `SUBMITTED → SUPERSEDED` 還列在 `deferred_transitions` |
| 計分 | `application/reviewcase/ScoreCalculationService.java`、`ScoreResult.java` | 未立法。初審草稿、提交初審、提交複審、兩個修正都用它 |
| ReviewCase 的收尾 | `CloseRereviewResult`、`FixInitialReviewResult`、`SaveDraftResult`、`StartInitialReviewResult`、`SubmitInitialReviewResult`、`SubmitRereviewResult`、`dto/AnswerInput`、`query/ReviewCaseQueryCriteria`、`query/ReviewCaseSummary`、`query/RevisionHistoryEntry`、`query/RevisionHistoryView` | 未立法，但它們屬於的 use case 已立法 |
| 已立法範圍的缺口 | `GetRevisionHistory` 對複審醫師的可見規則；D-083 之後補得出來的範例 | — |

## 二、查到的事實

**實測（2026-11-25，暫時的規格範例，走真實寫入與查詢路徑，跑完已還原，沒有提交）**。計分的情境用 `low_score_completed_case`（題目 I1 是 RANGE、滿分 20、權重 1），以修正初審觸發計分。

| # | 情境 | 結果 |
|---|---|---|
| 1 | RANGE 作答 `abc` | `IllegalArgumentException`。HTTP 由 `GlobalExceptionHandler` 回 **500 `INTERNAL_ERROR`** |
| 2 | RANGE 作答 `-1` | 同上，**500** |
| 3 | RANGE 作答 `99`（滿分 20） | 成功，`totalScore = 20`：**默默截到滿分** |
| 4 | RANGE 作答 `5.555` | 成功，輸出 `totalScore = 5.555`；資料表 `total_score` 是 `number(10,2)`，存進去是兩位小數，**輸出與保存不一致** |
| 5 | 作答含評分表沒有的題目 `X9` | 成功；`X9` **默默丟掉**，保存的答案只有 I1 |
| 6 | 同一題作答兩次（`3`、`4`） | 成功，`totalScore = 7`：**重複計分**，可以超過滿分 |
| 7 | 作答沒有 `answerValue` | `NullPointerException`，HTTP **500** |
| 8 | FIXED 題作答 `false` | 成功，`totalScore = 20`：FIXED 不看作答內容 |
| 9 | 初審草稿不帶任何作答 | 成功，`totalScore = 0` |
| 10 | 初審草稿剛建立時的明細 | `totalScore = 0`、`answers = []`、沒有 `submittedAt` |
| 11 | 複審草稿剛建立時的明細 | **沒有 `totalScore`**（`null`）、`answers = []` |
| 12 | 連續修正兩次 | 第二次的 `revisionNo = 3` |
| 13 | 複審醫師讀修正歷史 | 成功；初審那筆的 `totalScore`、`comment` 是 `null`，自己的複審那筆完整 |
| 14 | 初審醫師讀修正歷史（複審已提交） | 成功；兩筆都完整，包括複審的分數與評語（與 D-084 第 2 點一致） |

**讀程式確認**：

- 計分（`ScoreCalculationService`）：
  - BOOLEAN：作答剛好是 `"true"` 得 滿分 × 權重，其他一律 0（`"TRUE"`、`"yes"` 也是 0）。
  - FIXED：不論作答，得 滿分 × 權重。
  - RANGE：作答轉成數字；不是數字或小於 0 丟 `IllegalArgumentException`；大於滿分截到滿分；得 作答 × 權重。
  - 評分表沒有的題目跳過，「Unknown itemId — ignore per spec」的註解出自 `docs/superpowers/plans/2026-04-08-b3b-initial-review.md`（實作計畫，不是權威文件）。
  - 總分是各題相加，沒有四捨五入；`finalScore` 才四捨五入到兩位小數（RC-INV-03）。
  - **權威文件沒有計分的條文**：decision-log 只有權重（D-020）與題目設定的欄位驗證（D-061 等），`domain-model.md` 只列出三種 `scoreType`。
- 必答題的檢查只看 `ALWAYS_REQUIRED`；`CONDITIONAL` 沒有任何檢查（`conditionExpression` 的求值沒有定義，D-014 延後 `visibilityRule`）。
- `ReviewSubmission`：
  - 建立：初審草稿有 `basedOnScoreSheetVersionId`、`totalScore = 0`、`revisionNo = 1`；複審草稿**沒有** `basedOnScoreSheetVersionId`（複審計分改用案件的 `scoreSheetVersionRef`）、`totalScore = null`，建立時也不檢查 ID 與醫師是否為 `null`。
  - 只有 `DRAFT` 能保存與提交（`SUBMISSION_NOT_DRAFT`），提交後不能回到草稿。
  - 修正（`createRevision`）：舊的必須是 `SUBMITTED`；新的直接是 `SUBMITTED`，`revisionNo` 加 1，案件、階段、醫師、評分表版本照舊；舊的改為 `SUPERSEDED` 並指向新的（`markSuperseded`）。
  - `attachmentRefsJson`：**沒有任何行為寫入**，只由 `reconstitute` 讀回；修正時也不帶到新的提交。HTTP 請求的附件沒有傳進 use case（D-044 第 4 點）。附件的上傳與儲存策略還沒定案。
- `DraftContent`：已實作 D-044 的比較規則（不看順序、數值以數值比較、`null` 與空字串相同、不比附件），草稿與修正的「沒有變更」都用它。
- `GetRevisionHistory` 的規格（RC-VIS-01、03、05）沒有寫複審醫師與「初審醫師看複審」的可見規則；`model.yaml` 的 `deferred_behaviors` 記著「revision 歷程對複審醫師的遮罩還沒有規則」。

## 三、計分

### T-1：RANGE 的作答不是數字或小於 0

- **A（建議）：** 400 `VALIDATION_ERROR`，在狀態、指派、提交歸屬的檢查之後，必答題之前。不寫入。
- **B：** 照程式（500）。

### T-2：作答沒有 `answerValue`

- **A（建議）：** 400 `VALIDATION_ERROR`，與 T-1 同一個位置。`itemId` 沒有給也一樣。
- **B：** 照程式（500）。

### T-3：RANGE 的作答大於滿分

- **A（建議）：** 400 `VALIDATION_ERROR`，不默默截斷。
- **B：** 照程式，截到滿分。

### T-4：小數位數

- **A（建議）：** RANGE 的作答最多兩位小數，超過回 400 `VALIDATION_ERROR`。題目分數（作答 × 權重）與總分四捨五入到兩位小數（HALF_UP），與 `finalScore` 相同，輸出與保存一致。
- **B：** 不限制作答，只把題目分數與總分四捨五入到兩位小數。

### T-5：評分表沒有的題目

- **A（建議）：** 400 `VALIDATION_ERROR`。
- **B：** 照程式，默默丟掉。

### T-6：同一題作答兩次

- **A（建議）：** 400 `VALIDATION_ERROR`。
- **B：** 照程式，重複計分。

### T-7：三種題目的計分

- **建議（照程式，補上作答檢查）：**
  - BOOLEAN：作答只能是 `true` 或 `false`（其他回 400 `VALIDATION_ERROR`）；`true` 得 滿分 × 權重，`false` 得 0。
  - FIXED：作答就得 滿分 × 權重，作答內容不影響分數。
  - RANGE：得 作答 × 權重（作答在 0 到滿分之間，T-1、T-3、T-4）。
  - 總分是各題分數相加；沒有作答的題目不計分。
- `CONDITIONAL` 的必答與 `conditionExpression` 不在本次範圍，列進 `deferred_behaviors`（D-014）。

## 四、`ReviewSubmission`

### T-8：狀態轉移與不變式

- **建議：** `specs/review-submission/model.yaml` 立法：
  - 轉移：建立 `DRAFT`（開始初審、開始複審）；`DRAFT → DRAFT`（保存）；`DRAFT → SUBMITTED`（提交）；`SUBMITTED → SUPERSEDED`（兩個修正，同時建立一份新的 `SUBMITTED`）。`deferred_transitions` 的兩列改為轉移。
  - 不變式：只有 `DRAFT` 能保存與提交；`SUBMITTED`、`SUPERSEDED` 的內容不再改變；修正建立的提交 `revisionNo` = 被修正的 + 1，案件、階段、醫師、評分表版本照舊；`SUPERSEDED` 一定指向取代它的提交；`SUPERSEDED` 是終點。

### T-9：草稿的分數

實測 #10、#11：初審草稿 `totalScore = 0`（保存時計分），複審草稿 `null`（D-069 第 7 點：複審草稿不計分，提交時才計分）。

- **A（建議）：** 照程式立法。
- **B：** 複審草稿也設成 0，與初審一致。

### T-10：複審提交的評分表版本

- **A（建議）：** 開始複審時把案件的 `scoreSheetVersionRef` 記到複審草稿的 `basedOnScoreSheetVersionId`，修正照舊帶過去；計分仍用案件的版本（兩者相同），行為不變。初審與複審的提交都能追溯到評分表版本。
- **B：** 照程式，複審提交沒有評分表版本。

### T-11：附件

- **建議：** `attachmentRefsJson` 沒有任何行為寫入，但附件是規劃中的功能（permission-matrix §5、D-008 的完整明細包含附件），與 D-084 第 9 點的三個欄位不同：不移除，列進 `deferred_behaviors`，等附件的上傳與儲存策略裁決。修正時是否帶到新的提交也一併延後。

## 五、ReviewCase 的收尾

### T-12：修正歷史的可見規則

- **建議：** `GetRevisionHistory` 補上（照程式，實測 #13、#14）：
  - 複審醫師：初審的每一筆沒有 `totalScore`、`comment`；自己的複審那幾筆完整（D-033）。
  - 初審醫師：複審的分數與評語可見（D-084 第 2 點）。
  - `deferred_behaviors` 的對應項目移除。

### T-13：補得出來的範例

D-083 立法修正之後，下列 `not_covered` 的前提做得出來（先提交、再修正，舊的提交就是「已被取代」或「不是目前有效」）：

- **建議：** 補範例並移除 `not_covered` 的對應項目：RC-FIX-04、RC-FIX-05（固定的目標不是目前有效的初審）、RC-RVI-06、RC-RVR-07（修正不是目前有效的提交）、RC-RDR-08、RC-SRR-08、RC-CLR-07（複審草稿、提交、結案的目標不是目前有效的複審）。做不出來的保留在 `not_covered`，理由逐項寫明。

### T-14：登記範圍

- **建議：** 第一節的檔案全部登記為已立法：`domain/reviewcase/submission/` 的六個檔案登記到 `specs/review-submission/`；計分與 ReviewCase 的收尾登記到 `specs/review-case/`。登記完成後，`domain/reviewcase/`、`application/reviewcase/` 兩個目錄列底下就沒有暫准的檔案；目錄列保留（新增的檔案仍要先立法，D-060）。

## 六、題目清單

本切片的修法 PR 一併：

- D-084 移到已完成。
- 把負責人 2026-11-25 同意的順序寫進待辦：C（本切片）→ A SamplingBatch → B 報表 → D、E HTTP 介面與共用元件 → 專案版本管理與未裁決事項（逾期、多角色的讀取授權、`latestRevisionSummary`）。
