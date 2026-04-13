# UI Page Spec v1

## UI-012 初審作業頁

---

## 1. 頁面基本資訊

### Page ID
`UI-012`

### 頁面名稱
初審作業頁

### 頁面目的
提供已被指派的初審醫師，對單一案件執行以下作業：

- 開始初審
- 保存初審草稿
- 提交初審
- 在合法條件下發起初審 revision
- 查看自己的初審版本摘要

### 頁面型態
`Action / Form / Command Page`

### 對應 IA 模組
`案件作業`

### 對應主要角色
- `REVIEWER`

### 對應次要查看角色
- 無。此頁以作業角色為主，不建議作為 manager / auditor 的主要查看頁。

### 對應 domain objects
- `ReviewCase`
- `ReviewSubmission`
- `ScoreSheetDefinition` [透過 snapshot 呈現]
- `ScoreSheetSnapshotRef`

### 對應主要 API
- `POST /review-cases/{caseId}/initial-review/start`
- `POST /review-cases/{caseId}/initial-review/draft`
- `POST /review-cases/{caseId}/submissions/{submissionId}/initial-review/submit`
- `POST /review-cases/{caseId}/submissions/{submissionId}/initial-review/revise`

---

## 2. 頁面責任範圍

本頁負責：

1. 呈現初審所需最小必要案件資訊
2. 呈現初審使用的評分表快照
3. 讓 reviewer 填寫答案、評語、附件
4. 保存草稿
5. 提交初審
6. 在合法條件下發起 revision
7. 呈現與自己相關的初審版本摘要

本頁不負責：

- 顯示完整 lifecycle 詳表
- 顯示 full audit trail
- 處理 reviewer 指派 / 改派
- 直接執行複審
- 顯示跨角色完整內容視角
- 進行 project / score sheet 規則編輯

---

## 3. 頁面核心定位

初審作業頁是「reviewer 執行審查命令」的頁面，而不是一般資料編輯頁。

這頁必須明確反映以下幾件事：

1. 草稿 != 正式提交
2. 提交會影響案件主流程
3. revision != 編輯舊資料
4. revision != reopen
5. 結案後不可 revision
6. 只有原審查醫師可修正初審

因此，畫面語意必須清楚區分：

- 草稿模式
- 已提交版本
- revision 模式
- 不可操作狀態

---

## 4. 進入條件與 guard

### 4.1 合法進入條件

使用者需同時滿足：

- actor = `REVIEWER`
- actor 為案件已指派之 reviewer
- case status 為以下之一：
  - `PENDING_INITIAL_REVIEW`
  - `INITIAL_REVIEW_IN_PROGRESS`
  - 合法可 revision 的未結案狀態

### 4.2 不合法進入情境

以下情境應拒絕或改為只讀提示：

- actor 不是該案件的 reviewer
- case status = `CLOSED`
- case status = `CANCELLED`
- actor 嘗試進入他人的初審作業頁
- case 已進入純複審階段且不允許初審 side command

### 4.3 UI 行為建議

- 不合法時不直接顯示空白表單
- 顯示明確提示，例如：
  - 無權限處理此案件
  - 案件已結案，不可再執行初審作業
  - 案件已取消，不可再執行初審作業

---

## 5. 頁面主要區塊

本頁建議分為 6 個區塊：

1. 頁面標題與案件摘要區
2. 狀態 / 模式提示區
3. 評分表作答區
4. 評語與附件區
5. 提交與草稿操作區
6. 初審版本摘要區

---

## 5.1 頁面標題與案件摘要區

### 目的
讓 reviewer 在作業時始終知道自己正在審哪一筆案件。

### 建議內容
- caseId
- projectName
- batchId
- medicalRecordType
- dueAt
- case status
- projectVersionId
- scoreSheetVersionId

### 建議呈現
頁面上方固定摘要條：

```text id="z0b8sw"
案件編號 | 專案 | 病歷類型 | 目前狀態 | 截止日 | 評分表版本
```

### 補充

* 此區只放必要資訊
* 詳細追溯資訊仍以案件詳情頁為主，不在作業頁過度展開

---

## 5.2 狀態 / 模式提示區

### 目的

讓使用者知道目前是在：

* 初次填寫
* 草稿續編
* revision 模式
* 不可操作狀態

### 建議模式

```yaml id="pvsbzc"
page_modes:
  - NEW_INITIAL_REVIEW
  - EDIT_DRAFT
  - REVISION
  - READONLY_BLOCKED
```

### 模式說明

#### `NEW_INITIAL_REVIEW`

* 第一次開始初審
* 尚無 submission 或尚未產生草稿

#### `EDIT_DRAFT`

* 已存在 draft submission
* 使用者繼續編輯草稿

#### `REVISION`

* 以既有 `SUBMITTED` submission 為基礎建立新 revision
* 必須清楚標示這不是修改舊資料

#### `READONLY_BLOCKED`

* 目前不允許操作
* 例如案件已結案 / 已取消 / 非本人

### 建議提示訊息

#### 草稿模式

* `目前為草稿編輯模式，尚未正式提交`

#### revision 模式

* `目前為初審修正模式，系統將建立新 revision，不會覆寫既有提交`

#### blocked 模式

* `案件目前不可執行初審作業`

---

## 5.3 評分表作答區

### 目的

承載評分表填答，是本頁核心表單區。

### 內容來源

* 依 `basedOnScoreSheetSnapshot` 呈現
* 不應直接用 mutable score sheet draft 呈現

### 區塊結構建議

* section accordion / section cards
* 每個 section 下列出 score items
* 每個 item 顯示：

  * title
  * scoreType
  * required hint
  * input control
  * 即時計分摘要 [若適用]

### 題型支援

依目前設計，至少支援：

* boolean
* fixed score
* range / number
* 後續可擴充 object / structured answer

### 欄位建議

每題至少包含：

| 欄位              | 說明     |
| --------------- | ------ |
| itemId          | 隱藏識別   |
| title           | 題目名稱   |
| input control   | 作答輸入元件 |
| required marker | 必填提示   |
| maxScore        | 顯示上限   |
| derivedScore    | 計分摘要   |

### UI 原則

* 題目與分數顯示需穩定
* 不可因後續 score sheet 變更影響本頁內容
* revision 時預設帶入前版答案，供使用者修正

---

## 5.4 評語與附件區

### 目的

承載非結構化補充內容。

### 建議內容

* comment textarea
* attachment list
* upload control

### UI 行為

* 草稿可先保存 comment 與附件
* submission 時一併提交
* revision 模式可新增或調整附件關聯
* 若附件內容管理獨立，這裡至少要呈現已關聯檔案清單

### 備註

* 附件區的真正 upload 協定可在後續 API / file storage 設計補
* 本頁先只定 interaction 層需求

---

## 5.5 提交與草稿操作區

### 目的

清楚區分 command 操作，避免使用者混淆「儲存」與「提交」。

### 建議按鈕

| 按鈕          | 用途                       |
| ----------- | ------------------------ |
| 保存草稿        | 保存目前輸入，不變更正式提交狀態         |
| 提交初審        | 正式提交初審                   |
| 發起 revision | 僅在 revision 模式時顯示為主要提交語意 |
| 返回案件詳情      | 返回 detail page           |
| 重置未保存變更     | 可選                       |

### 顯示規則

#### 草稿模式

* 顯示：

  * 保存草稿
  * 提交初審

#### revision 模式

* 顯示：

  * 保存草稿 [可選，若允許 revision 草稿]
  * 提交 revision
* 不應只寫「儲存」，應明確寫成：

  * `提交初審修正`

#### blocked 模式

* 所有提交型按鈕 disabled / hidden
* 僅保留返回或查看提示

### 重要提示

提交區必須清楚說明：

* 提交後會影響案件狀態
* revision 會建立新版本，不覆寫舊版

---

## 5.6 初審版本摘要區

### 目的

讓 reviewer 在作業頁內快速知道自己目前基於哪一版作業，不必跳到 revision 歷程頁才看得到。

### 建議顯示內容

* current mode
* current submissionId
* stage = `INITIAL`
* current revisionNo
* latest submittedAt
* 若為 revision 模式，顯示：

  * base submissionId
  * base revisionNo

### 額外入口

* 查看 revision 歷程
* 返回案件詳情

---

## 6. 權限與可見性規則

### 6.1 可進入角色

* 僅 `REVIEWER`
* 且必須為該案件指派 reviewer

### 6.2 不可進入角色

* `HOSPITAL_MANAGER`
* `REREVIEWER`
* `AUDITOR`
* `SYSTEM_ADMIN`

### 6.3 不可進入的處理建議

* 不建議讓 manager / auditor 用只讀模式直接看作業頁
* 他們應從案件詳情頁或 revision 歷程頁看摘要
* 作業頁應盡量保持單一責任：給 reviewer 做事

---

## 7. 狀態驅動 UI 規則

---

## 7.1 `PENDING_INITIAL_REVIEW`

### 頁面行為

* 第一次進入時先觸發 `start initial review`
* 進入 `NEW_INITIAL_REVIEW` 模式
* 顯示空白或預設答案表單

### 可用操作

* 保存草稿
* 提交初審

---

## 7.2 `INITIAL_REVIEW_IN_PROGRESS`

### 頁面行為

* 若已有 draft，進 `EDIT_DRAFT`
* 若已有 submitted 且可合法 revision，進 `REVISION` 前置判斷
* 否則維持 draft / edit 模式

### 可用操作

* 保存草稿
* 提交初審

---

## 7.3 `INITIAL_REVIEW_COMPLETED`

### 頁面行為

* 原則上不再作為一般初審填答頁入口
* 若系統允許未結案前以 reviewer 進行合法 revision，才可進 `REVISION` 模式
* 否則只顯示不可操作提示

### 可用操作

* 依 revision 合法性決定
* 不可當成一般 draft edit 繼續操作

---

## 7.4 `PENDING_REREVIEW`

### 頁面行為

* 不應允許一般初審續編
* 僅在你既有 decision 下，若未結案且屬原審查醫師合法 revision，才可進 revision 模式
* 否則顯示只讀提示並導回案件詳情

---

## 7.5 `REREVIEW_IN_PROGRESS`

### 頁面行為

* 同上，僅在合法 revision 情境下可進
* 不可讓 reviewer 誤以為還能正常重做初審

---

## 7.6 `CLOSED`

### 頁面行為

* `READONLY_BLOCKED`
* 顯示：

  * 已結案
  * 不可 revision
* 不顯示草稿 / 提交按鈕

---

## 7.7 `CANCELLED`

### 頁面行為

* `READONLY_BLOCKED`
* 顯示：

  * 已取消
  * 不可再審
* 不顯示草稿 / 提交按鈕

---

## 8. revision 模式設計

這一段非常重要，避免把 revision 做成「編輯原資料」。

### 8.1 進入 revision 模式的條件

需同時滿足：

* stage = `INITIAL`
* actor = original reviewer
* case status != `CLOSED`
* case status != `CANCELLED`
* 存在可作為 base 的 submitted submission

### 8.2 revision 模式的 UI 表現

應額外顯示：

* `你正在修正已提交的初審結果`
* `系統將建立新 revision，不會覆寫原提交`
* `原提交版本編號`
* `新 revision 編號 [預估]`

### 8.3 revision 預設資料

* 預設帶入上一版 submitted answers / comment / attachments
* 使用者在此基礎上修改

### 8.4 revision 提交結果

* 建立新 submissionId
* `revisionNo + 1`
* 舊版 submission -> `SUPERSEDED`

---

## 9. 頁面狀態設計

### 9.1 Loading

* skeleton header
* skeleton section cards
* submit bar disabled

### 9.2 Empty Form

* 僅在首次初審時出現
* 顯示空白評分表

### 9.3 Draft Loaded

* 顯示 draft badge
* 顯示最近草稿更新時間

### 9.4 Revision Mode

* 顯示 revision banner
* 顯示 base submission 摘要

### 9.5 Forbidden

* 顯示無權限處理此案件
* 不顯示表單內容

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
* 檢查 visibilityRule 對應題目
* 檢查案件狀態仍合法
* 檢查 actor 仍是合法 reviewer

### 10.3 revision 提交前額外驗證

* base submission stage = `INITIAL`
* actor = original reviewer
* case 未結案 / 未取消

---

## 11. 主要互動流程

### 11.1 初次初審流程

```text id="t1o9qj"
案件詳情頁
  -> 點開始初審
  -> 進入初審作業頁
  -> 填寫表單
  -> 保存草稿 / 提交初審
  -> 回案件詳情頁
```

### 11.2 草稿續編流程

```text id="wnr3kn"
案件詳情頁
  -> 點繼續初審
  -> 載入 draft
  -> 修改內容
  -> 保存草稿 / 提交初審
```

### 11.3 revision 流程

```text id="pk4zo0"
案件詳情頁
  -> 點查看 revision / 進入修正
  -> 進入初審作業頁 revision 模式
  -> 載入前版 submitted 內容
  -> 提交初審修正
  -> 建立新 revision
```

---

## 12. 對應 API 規劃

### 12.1 進入初審

* `POST /review-cases/{caseId}/initial-review/start`

### 12.2 保存草稿

* `POST /review-cases/{caseId}/initial-review/draft`

### 12.3 提交初審

* `POST /review-cases/{caseId}/submissions/{submissionId}/initial-review/submit`

### 12.4 修正初審

* `POST /review-cases/{caseId}/submissions/{submissionId}/initial-review/revise`

---

## 13. Request / Response 草稿

### 13.1 草稿保存 request

```json id="eokuqr"
{
  "answers": [
    {
      "itemId": "I1",
      "answerValue": true
    },
    {
      "itemId": "I2",
      "answerValue": 10
    }
  ],
  "comment": "初步檢視完成",
  "attachments": [
    {
      "fileToken": "TMP-FILE-001"
    }
  ]
}
```

### 13.2 草稿保存 response

```json id="oq0s20"
{
  "success": true,
  "data": {
    "submissionId": "SUB-INIT-001",
    "stage": "INITIAL",
    "status": "DRAFT",
    "revisionNo": 1
  }
}
```

### 13.3 提交初審 response

```json id="9bjs3x"
{
  "success": true,
  "data": {
    "submissionId": "SUB-INIT-001",
    "status": "SUBMITTED",
    "caseId": "RC-001",
    "caseStatus": "INITIAL_REVIEW_COMPLETED",
    "totalScore": 88.5,
    "rereviewRequired": true
  }
}
```

### 13.4 revision response

```json id="kicp6q"
{
  "success": true,
  "data": {
    "oldSubmissionId": "SUB-INIT-001",
    "newSubmissionId": "SUB-INIT-002",
    "stage": "INITIAL",
    "revisionNo": 2,
    "caseId": "RC-001"
  }
}
```

---

## 14. 頁面測試重點

本頁至少應驗證：

1. 只有指派 reviewer 可進入
2. draft 與 submitted 模式是否正確區分
3. revision 模式是否正確顯示
4. revision 是否明確提示不覆寫舊版
5. `CLOSED` / `CANCELLED` 是否正確阻擋
6. 提交後 case status 是否依規則轉移
7. 超過品質線時是否正確導向複審邏輯
8. 非原 reviewer 不可 revision
9. form 驗證是否與 score sheet snapshot 對齊

---

## 15. 不屬於本頁處理的事項

* 複審最終分數決定
* manager 指派 / 改派
* revision 歷程完整差異對照
* audit trail 明細
* project / batch 詳細追溯頁

這些應由：

* `UI-011 案件分派頁`
* `UI-013 複審作業頁`
* `UI-014 Revision 歷程頁`
* `UI-016 稽核查詢頁`

負責。
