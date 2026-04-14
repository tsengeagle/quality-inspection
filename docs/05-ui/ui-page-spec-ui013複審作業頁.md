# UI Page Spec v1

## UI-013 複審作業頁

---

## 1. 頁面基本資訊

### Page ID
`UI-013`

### 頁面名稱
複審作業頁

### 頁面目的
提供已被指派的複審醫師，對單一案件執行以下作業：

- 開始複審
- 保存複審草稿
- 提交複審
- 在合法條件下發起複審 revision
- 查看與本次複審相關的初審摘要與複審版本摘要

### 頁面型態
`Action / Form / Final Decision Page`

### 對應 IA 模組
`案件作業`

### 對應主要角色
- `REREVIEWER`

### 對應次要查看角色
- 無。此頁以複審作業角色為主，不建議作為 manager / auditor 的主要查看頁。

### 對應 domain objects
- `ReviewCase`
- `ReviewSubmission`
- `ScoreSheetDefinition` [透過 snapshot 呈現]
- `ScoreSheetSnapshotRef`

### 對應主要 API
- `POST /review-cases/{caseId}/rereview/start`
- `POST /review-cases/{caseId}/rereview/draft`
- `POST /review-cases/{caseId}/submissions/{submissionId}/rereview/submit`
- `POST /review-cases/{caseId}/submissions/{submissionId}/rereview/revise`

---

## 2. 頁面責任範圍

本頁負責：

1. 呈現複審所需的案件摘要與初審摘要
2. 呈現複審使用的評分表快照
3. 讓複審醫師填寫答案、評語、附件
4. 保存複審草稿
5. 提交複審
6. 在合法條件下發起複審 revision
7. 呈現與自己相關的複審版本摘要
8. 清楚呈現最終分數與結案語意

本頁不負責：

- 指派 / 改派 reviewer 或 rereviewer
- 顯示完整 lifecycle 明細
- 顯示完整 audit trail
- 維護 project / score sheet 規則
- 顯示 revision 逐題差異比對詳表
- 代替案件詳情頁承載所有追溯資訊

---

## 3. 頁面核心定位

複審作業頁是「最終審查決策頁」。  
它和初審頁共享部分互動模型，但有三個關鍵差異：

1. 必須先看到初審摘要，才能進入複審表單
2. 複審提交後會直接決定 final score
3. 正常提交完成後，案件會進入 `CLOSED`

因此，這頁必須清楚區分：

- 初審摘要 [前置資訊]
- 複審填答 [本次作業主體]
- 最終分數 / 結案提示 [結果語意]
- revision 模式 [不覆寫舊版]

---

## 4. 進入條件與 guard

### 4.1 合法進入條件

使用者需同時滿足：

- actor = `REREVIEWER`
- actor 為案件已指派之 rereviewer
- case status 為以下之一：
  - `PENDING_REREVIEW`
  - `REREVIEW_IN_PROGRESS`
  - 合法可 revision 的未結案狀態

### 4.2 不合法進入情境

以下情境應拒絕或改為只讀提示：

- actor 不是該案件的 rereviewer
- case status = `CLOSED`
- case status = `CANCELLED`
- 案件尚未進入複審流程
- actor 嘗試進入他人的複審作業頁

### 4.3 UI 行為建議

- 不合法時不顯示可編輯表單
- 顯示明確提示，例如：
  - 此案件尚未進入複審流程
  - 你不是本案件的複審醫師
  - 案件已結案，不可再執行複審作業
  - 案件已取消，不可再執行複審作業

---

## 5. 頁面主要區塊

本頁建議分為 7 個區塊：

1. 頁面標題與案件摘要區
2. 狀態 / 模式提示區
3. 初審摘要區
4. 複審評分表作答區
5. 評語、附件與最終分數區
6. 提交與草稿操作區
7. 複審版本摘要區

---

## 5.1 頁面標題與案件摘要區

### 目的
讓複審醫師知道目前案件脈絡與自己所處任務。

### 建議內容
- caseId
- projectName
- batchId
- medicalRecordType
- dueAt
- case status
- projectVersionId
- scoreSheetVersionId
- assigned rereviewer

### 建議呈現
```text id="ih0pfx"
案件編號 | 專案 | 病歷類型 | 目前狀態 | 截止日 | 評分表版本 | 複審醫師
```

### 補充

* 初審詳細答案不在這區展開
* 詳細追溯仍以案件詳情頁為主

---

## 5.2 狀態 / 模式提示區

### 目的

讓使用者知道目前是在：

* 第一次進入複審
* 草稿續編
* revision 模式
* 不可操作狀態

### 建議模式

```yaml id="h2ghj1"
page_modes:
  - NEW_REREVIEW
  - EDIT_DRAFT
  - REVISION
  - REVISION_EXCEPTION_CLOSED
  - READONLY_BLOCKED
```

### 模式說明

#### `NEW_REREVIEW`

* 第一次開始複審
* 尚無複審 draft / submitted submission

#### `EDIT_DRAFT`

* 已存在複審 draft
* 使用者繼續編輯草稿

#### `REVISION`

* 基於已提交複審版本建立新 revision
* 必須明確標示不覆寫舊版

#### `READONLY_BLOCKED`

* 案件不允許複審操作
* 例如未進複審 / 已結案 / 已取消 / 非本人

#### `REVISION_EXCEPTION_CLOSED`

* 僅限原複審醫師
* UI mode label 顯示為 `例外修正（已結案）`
* 屬 revision exception，不構成 reopen

### 建議提示訊息

#### 複審草稿模式

* `目前為複審草稿編輯模式，尚未正式提交`

#### revision 模式

* `目前為複審修正模式，系統將建立新 revision，不會覆寫既有複審提交`

#### blocked 模式

* `案件目前不可執行複審作業`
* 使用 blocked 語意，不與 exception warning tone 混淆

---

## 5.3 初審摘要區

### 目的

複審不是從零開始，因此本區是複審頁最關鍵的前置資訊區。

### 必須顯示

* latest initial submissionId
* 初審 reviewer
* 初審 totalScore
* 初審 submittedAt
* initial revision count
* 是否因品質線進入複審
* 必要時顯示初審 comment 摘要 [依設計決策]

### 建議顯示方式

以唯讀摘要卡呈現，不與複審表單混在一起。

### 建議操作

* 返回案件詳情查看完整初審摘要
* 查看 revision 歷程 [非在本區直接展開逐題差異]

### 補充

這區的目的是讓複審醫師知道：

* 為什麼這案件會到我手上
* 初審結果是什麼
* 我現在是在做最終判定

---

## 5.4 複審評分表作答區

### 目的

承載複審表單，是本頁主要作業區。

### 內容來源

* 依 `basedOnScoreSheetSnapshot` 呈現
* 不直接使用 mutable score sheet draft
* 盡量與初審頁保持同一套題目結構，以降低學習成本

### 區塊結構建議

* section cards / accordion
* 每個 section 下列出 score items
* 每個 item 顯示：

  * title
  * input control
  * maxScore
  * required marker
  * derivedScore

### UI 原則

* 複審表單呈現上可與初審頁一致
* 但必須在區塊層面清楚標明「複審填答」
* revision 時預設帶入上一版複審答案

---

## 5.5 評語、附件與最終分數區

### 目的

承載複審補充說明與最終分數決策語意。

### 建議內容

* comment textarea
* attachment list
* upload control
* final score summary panel

### final score 顯示原則

* final score 不一定需要另外手填獨立欄位，若系統由答案計算，則應以摘要方式顯示
* 但 UI 必須清楚標明：

  * `此提交將作為最終分數依據`
  * `提交完成後案件將結案`

### 建議提示

* `複審提交完成後，案件將進入已結案狀態`
* `請確認本次複審結果與最終分數`

---

## 5.6 提交與草稿操作區

### 目的

清楚區分保存草稿、提交複審與提交複審修正。

### 建議按鈕

| 按鈕     | 用途                |
| ------ | ----------------- |
| 保存草稿   | 保存目前複審輸入          |
| 提交複審   | 正式提交複審並結案         |
| 提交複審修正 | revision 模式下的主要動作 |
| 返回案件詳情 | 返回 detail page    |

### 顯示規則

#### 草稿模式

* 顯示：

  * 保存草稿
  * 提交複審

#### revision 模式

* 顯示：

  * 保存草稿 [若允許 revision 草稿]
  * 提交複審修正

#### blocked 模式

* 不顯示提交類按鈕
* 顯示只讀提示與返回按鈕

### 重要提示

提交區需明確寫出：

* 複審提交完成後，案件將結案
* 複審修正會建立新 revision，不覆寫舊版

---

## 5.7 複審版本摘要區

### 目的

讓複審醫師理解自己目前基於哪一版複審資料在操作。

### 建議顯示內容

* current submissionId
* current revisionNo
* stage = `REREVIEW`
* latest submittedAt
* 若為 revision 模式：

  * base submissionId
  * base revisionNo

### 額外入口

* 查看 revision 歷程
* 返回案件詳情

---

## 6. 權限與可見性規則

### 6.1 可進入角色

* 僅 `REREVIEWER`
* 且必須為該案件指派 rereviewer

### 6.2 不可進入角色

* `HOSPITAL_MANAGER`
* `REVIEWER`
* `AUDITOR`
* `SYSTEM_ADMIN`

### 6.3 不可進入的處理建議

* manager / auditor / system admin 應從案件詳情頁查看摘要，不使用本頁
* 本頁應維持單一責任：複審作業

---

## 7. 狀態驅動 UI 規則

---

## 7.1 `PENDING_REREVIEW`

### 頁面行為

* 第一次進入時先觸發 `start rereview`
* 進入 `NEW_REREVIEW` 模式
* 顯示初審摘要 + 空白複審表單

### 可用操作

* 保存草稿
* 提交複審

---

## 7.2 `REREVIEW_IN_PROGRESS`

### 頁面行為

* 若已有 draft，進 `EDIT_DRAFT`
* 若已有 submitted 且尚未結案的合法 revision 情境，進 `REVISION`
* 否則維持 draft / edit 模式

### 可用操作

* 保存草稿
* 提交複審

---

## 7.3 `CLOSED`

### 頁面行為

* `REVISION_EXCEPTION_CLOSED`（限原複審醫師）或 `READONLY_BLOCKED`（其他人）
* 顯示：

  * 此案件已結案
  * 新複審提交不可執行
  * 允許以 revision 例外流程修正既有複審結果（不構成 reopen）
  * exception 提示採 warning / amber 語意；其他 readonly blocked 維持 blocked 語意
* 不顯示一般草稿 / 提交按鈕；僅顯示複審修正入口（合法責任人）

---

## 7.4 `CANCELLED`

### 頁面行為

* `READONLY_BLOCKED`
* 顯示：

  * 此案件已取消
  * 不可再審
* 不顯示草稿 / 提交按鈕

---

## 7.5 非複審流程狀態

例如：

* `PENDING_INITIAL_REVIEW`
* `INITIAL_REVIEW_IN_PROGRESS`
* `INITIAL_REVIEW_COMPLETED`

### 頁面行為

* `READONLY_BLOCKED`
* 顯示：

  * 此案件尚未進入複審流程
* 不顯示表單

---

## 8. revision 模式設計

### 8.1 進入 revision 模式的條件

需同時滿足：

* stage = `REREVIEW`
* actor = assigned rereviewer
* case status != `CANCELLED`
* 若 case status = `CLOSED`，僅允許例外 revision flow（D-021）
* 存在可作為 base 的 submitted rereview submission

### 8.2 revision 模式的 UI 表現

應額外顯示：

* `你正在修正已提交的複審結果`
* `系統將建立新 revision，不會覆寫原提交`
* `原複審版本編號`
* `新 revision 編號 [預估]`

### 8.3 revision 預設資料

* 預設帶入上一版 submitted rereview answers / comment / attachments
* 使用者在此基礎上修正

### 8.4 revision 提交結果

* 建立新 submissionId
* `revisionNo + 1`
* 舊版 submission -> `SUPERSEDED`
* case 主狀態不因 revision 自動 reopen

---

## 9. 頁面狀態設計

### 9.1 Loading

* skeleton header
* skeleton initial summary card
* skeleton form sections

### 9.2 Empty Form

* 僅在第一次開始複審時出現
* 顯示初審摘要 + 空白複審表單

### 9.3 Draft Loaded

* 顯示 draft badge
* 顯示最近草稿更新時間

### 9.4 Revision Mode

* 顯示 revision banner
* 顯示 base rereview submission 摘要

### 9.5 Forbidden

* 顯示無權限處理此案件
* 不顯示表單

### 9.6 Closed / Cancelled Blocked

* 顯示 terminal state 提示
* 顯示返回案件詳情按鈕
* 不顯示提交操作

---

## 10. 表單驗證建議

### 10.1 基本驗證

* 必填題目需完成作答
* 題型需符合 scoreType
* 附件格式 / 大小限制 [後續補]

### 10.2 提交前驗證

* 檢查 requiredRule
* 檢查 visibilityRule
* 檢查案件狀態仍合法
* 檢查 actor 仍是合法 rereviewer

### 10.3 revision 提交前額外驗證

* base submission stage = `REREVIEW`
* actor = assigned rereviewer
* case 未結案 / 未取消

---

## 11. 主要互動流程

### 11.1 第一次複審流程

```text id="j7zdrg"
案件詳情頁
  -> 點開始複審
  -> 進入複審作業頁
  -> 查看初審摘要
  -> 填寫複審表單
  -> 保存草稿 / 提交複審
  -> 回案件詳情頁
```

### 11.2 草稿續編流程

```text id="x6erqg"
案件詳情頁
  -> 點繼續複審
  -> 載入複審 draft
  -> 修改內容
  -> 保存草稿 / 提交複審
```

### 11.3 revision 流程

```text id="n4y5e5"
案件詳情頁
  -> 點查看 revision / 進入修正
  -> 進入複審作業頁 revision 模式
  -> 載入前版 submitted 複審內容
  -> 提交複審修正
  -> 建立新 revision
```

---

## 12. 對應 API 規劃

### 12.1 進入複審

* `POST /review-cases/{caseId}/rereview/start`

### 12.2 保存草稿

* `POST /review-cases/{caseId}/rereview/draft`

### 12.3 提交複審

* `POST /review-cases/{caseId}/submissions/{submissionId}/rereview/submit`

### 12.4 修正複審

* `POST /review-cases/{caseId}/submissions/{submissionId}/rereview/revise`

---

## 13. Request / Response 草稿

### 13.1 草稿保存 request

```json id="vxvwme"
{
  "answers": [
    {
      "itemId": "I1",
      "answerValue": true
    },
    {
      "itemId": "I2",
      "answerValue": 12
    }
  ],
  "comment": "複審中，初步確認",
  "attachments": [
    {
      "fileToken": "TMP-FILE-101"
    }
  ]
}
```

### 13.2 草稿保存 response

```json id="co0g3s"
{
  "success": true,
  "data": {
    "submissionId": "SUB-RR-001",
    "stage": "REREVIEW",
    "status": "DRAFT",
    "revisionNo": 1
  }
}
```

### 13.3 提交複審 response

```json id="uqnrea"
{
  "success": true,
  "data": {
    "submissionId": "SUB-RR-001",
    "status": "SUBMITTED",
    "caseId": "RC-001",
    "caseStatus": "CLOSED",
    "finalScore": 92.0
  }
}
```

### 13.4 revision response

```json id="abt5jj"
{
  "success": true,
  "data": {
    "oldSubmissionId": "SUB-RR-001",
    "newSubmissionId": "SUB-RR-002",
    "stage": "REREVIEW",
    "revisionNo": 2,
    "caseId": "RC-001"
  }
}
```

---

## 14. 頁面測試重點

本頁至少應驗證：

1. 只有指派 rereviewer 可進入
2. 尚未進入複審流程時是否正確阻擋
3. 初審摘要是否正確顯示
4. 草稿與 submitted 模式是否正確區分
5. revision 模式是否正確顯示
6. `CLOSED` / `CANCELLED` 是否正確阻擋
7. 提交複審後 case 是否正確進入 `CLOSED`
8. 非複審醫師不可 revision
9. revision 是否明確不覆寫舊版
10. final score 區塊是否清楚表達「最終判定」

---

## 15. 不屬於本頁處理的事項

* manager 指派 / 改派
* 初審完整填答
* revision 歷程完整差異比對
* audit trail 全量列表
* project / batch 詳細追溯頁

這些應由：

* `UI-011 案件分派頁`
* `UI-012 初審作業頁`
* `UI-014 Revision 歷程頁`
* `UI-016 稽核查詢頁`

負責。
