# UI Page Spec v1

## UI-014 Revision 歷程頁

---

## 1. 頁面基本資訊

### Page ID
`UI-014`

### 頁面名稱
Revision 歷程頁

### 頁面目的
提供單一案件下 submission revision 的查詢、追溯與版本理解能力，讓使用者可以：

- 查看初審 / 複審的 revision timeline
- 辨識目前有效版本
- 辨識哪些版本已被 superseded
- 查看每次 revision 的時間、提交者、階段、版本號
- 必要時比較不同 revision 之間的差異摘要

### 頁面型態
`History / Traceability / Comparison Page`

### 對應 IA 模組
`案件作業`

### 對應主要角色
- `HOSPITAL_MANAGER`
- `REVIEWER` [合法範圍]
- `REREVIEWER` [合法範圍]
- `AUDITOR`

### 不建議作為主要使用角色
- `SYSTEM_ADMIN`  
  system admin 若需進此頁，應為 metadata-only 視角，不作為主使用情境

### 對應 domain objects
- `ReviewSubmission`
- `ReviewCase`

### 對應主要 API
- `GET /review-cases/{caseId}/revisions`
- `GET /review-cases/{caseId}/submissions/{submissionId}` [供詳情 / 比對時補抓，case-rooted]

---

## 2. 頁面責任範圍

本頁負責：

1. 顯示案件下所有 revision 的清單與順序
2. 區分 `INITIAL` 與 `REREVIEW` 兩個 stage
3. 清楚標示 `SUBMITTED` 與 `SUPERSEDED`
4. 標示目前有效版本
5. 顯示每次 revision 的提交者、提交時間、revisionNo
6. 提供版本比對入口或差異摘要

本頁不負責：

- 直接發起 revision
- 直接編輯 submission
- 直接提交初審 / 複審
- 顯示完整 audit trail 全量事件
- 顯示完整案件所有追溯資訊
- 決定是否允許 revision

---

## 3. 頁面核心定位

這頁是「submission 版本歷史的閱讀頁」，不是「編輯頁」。

它的核心任務有三個：

1. 幫使用者理解**版本演進**
2. 幫使用者辨識**目前有效版本**
3. 幫使用者追蹤**責任人與時間點**

所以 UI 必須避免兩種錯誤：

- 讓使用者誤以為可以直接在歷程頁修改舊版
- 把 revision 清單畫成單純列表，卻看不出哪一版是現在有效版本

---

## 4. 進入條件與 guard

### 4.1 合法進入條件

使用者需同時滿足：

- 對該案件具有合法查閱權
- 角色屬於：
  - `HOSPITAL_MANAGER`
  - `AUDITOR`
  - `REVIEWER` [合法範圍]
  - `REREVIEWER` [合法範圍]
  - `SYSTEM_ADMIN` [metadata only, 非主情境]

### 4.2 不合法進入情境
以下情境應拒絕：

- actor 無案件查看權
- actor 超出 hospital / authorized scope
- reviewer / rereviewer 試圖查看不屬於自己合法範圍的案件 revision

### 4.3 UI 行為建議
- 無權限時顯示 forbidden，不顯示 revision 清單
- metadata-only 視角不顯示完整答案差異

---

## 5. 頁面主要區塊

本頁建議分為 5 個區塊：

1. 頁面標題與案件摘要區
2. revision 分組切換區
3. revision timeline 區
4. revision 詳細摘要 / 差異區
5. 操作與導頁區

---

## 5.1 頁面標題與案件摘要區

### 目的
讓使用者知道目前在看哪一筆案件的 revision 歷程。

### 建議內容
- caseId
- projectName
- caseStatus
- reviewer
- rereviewer
- latest initial revisionNo
- latest rereview revisionNo

### 建議呈現
```text id="3mwvqs"
案件編號 | 專案 | 狀態 | 初審醫師 | 複審醫師 | 最新初審版 | 最新複審版
```

### 補充

* 此區只放 revision 理解所需摘要
* 其他追溯資訊仍由案件詳情頁承載

---

## 5.2 revision 分組切換區

### 目的

讓使用者明確區分：

* 初審 revision
* 複審 revision

避免兩種 stage 混在一條時間軸裡造成閱讀困難。

### 建議模式

```yaml id="82f0m2"
group_modes:
  - ALL
  - INITIAL_ONLY
  - REREVIEW_ONLY
```

### 預設建議

* manager / auditor：`ALL`
* reviewer：`INITIAL_ONLY`
* rereviewer：`REREVIEW_ONLY`

### 切換方式

* tabs
* segmented control

### 顯示原則

* `ALL` 模式下仍需清楚標示 stage
* `INITIAL_ONLY` / `REREVIEW_ONLY` 可提供更聚焦的檢視

---

## 5.3 revision timeline 區

### 目的

以時間序與版本序呈現 revision 演進。

### 每筆 revision 至少顯示

* stage
* revisionNo
* submissionId
* submission status
* submittedAt
* triggeredBy / reviewerId
* 是否為目前有效版本
* 是否為 superseded 版本

### 建議時間軸排序

* 同 stage 內依 `revisionNo` 由小到大
* `ALL` 模式下依 `submittedAt` 或 `stage + revisionNo` 切組展示

### 建議標示方式

#### 當前有效版本

* `Current`
* 強調色 badge

#### 已被取代版本

* `Superseded`
* 次要色 badge

#### stage 標示

* `INITIAL`
* `REREVIEW`

### timeline 視覺建議

每個 revision card 至少包含：

```text id="ce5le6"
Stage | Revision No | Submission ID | Submitted At | Submitted By | Status | Current / Superseded
```

---

## 5.4 revision 詳細摘要 / 差異區

### 目的

讓使用者選取某個 revision 後，查看該版摘要，或比較兩版差異。

### 建議兩種檢視模式

```yaml id="a7nmph"
detail_modes:
  - SINGLE_REVISION_SUMMARY
  - REVISION_COMPARISON
```

---

### 5.4.1 單版摘要模式

#### 適用

* 一般 manager / auditor 快速閱讀
* reviewer / rereviewer 想確認某一版內容

#### 建議內容

* submissionId
* stage
* revisionNo
* status
* submittedAt
* submittedBy
* totalScore
* comment 摘要
* attachment 摘要
* 答案摘要 [依權限]

---

### 5.4.2 版本比對模式

#### 適用

* manager / auditor 做追溯
* reviewer / rereviewer 確認自己修正了什麼

#### 比對對象

* 相鄰 revision
* 使用者手動選兩版

#### 建議顯示內容

* base revisionNo
* target revisionNo
* 差異題目數
* 變更後 totalScore 差異
* comment 是否變更
* attachment 是否變更

### 差異呈現建議

先做「摘要差異」即可，不必第一版就做逐題細粒度 diff。

#### 差異摘要範例

```text id="q8dr0y"
- 題目變更：3 題
- 分數差異：88.5 -> 91.0
- 評語已更新
- 附件新增 1 筆
```

---

## 5.5 操作與導頁區

### 目的

提供從 revision 歷程回到正確上下文的入口。

### 建議按鈕

* 返回案件詳情
* 查看某版 submission 詳情 [若有對應 detail page / drawer]
* 比較與上一版差異
* 前往初審作業頁 [僅合法 reviewer，且僅當可 revision]
* 前往複審作業頁 [僅合法 rereviewer，且僅當可 revision]

### 顯示原則

* 本頁不直接顯示「提交 revision」按鈕
* 若要進行 revision，應導向作業頁的 revision 模式

---

## 6. 角色視角差異

---

## 6.1 HOSPITAL_MANAGER 視角

### 可看

* 本院案件 revision 歷程
* 初審與複審兩階段版本演進
* full detail 摘要與差異摘要

### 可做

* 查看 revision 歷程
* 比較版本差異
* 返回案件詳情

### 不可做

* 直接發起 revision
* 在本頁編輯 submission

---

## 6.2 REVIEWER 視角

### 可看

* 合法範圍內案件的 revision 歷程
* 以 `INITIAL` 為主的版本演進
* 自己提交與修正的初審版本

### 可做

* 查看自己的初審 revision 歷程
* 若案件未結案且合法，可由此導去初審作業頁 revision 模式

### 不可做

* 查看不屬於自己合法範圍的案件
* 發起複審 revision

---

## 6.3 REREVIEWER 視角

### 可看

* 合法範圍內案件的 revision 歷程
* 以 `REREVIEW` 為主的版本演進
* 自己提交與修正的複審版本

### 可做

* 查看自己的複審 revision 歷程
* 若複審結果未固定、案件未結案／未取消且 actor 為合法原提交者，可導去 revision 模式

### 不可做

* 發起初審 revision

---

## 6.4 AUDITOR 視角

### 可看

* 授權院區 / 範圍內完整 revision 歷程
* 初審與複審版本演進
* 差異摘要

### 可做

* 查詢
* 比較
* 導回案件詳情
* 進一步看 lifecycle

### 不可做

* 發起 revision
* 進入作業 command 頁做修改

---

## 6.5 SYSTEM_ADMIN 視角

### 可看

* metadata only revision list
* stage
* revisionNo
* submittedAt
* submittedBy
* currentEffective

### 不可看

* 完整答案摘要
* full comments
* attachment content
* 差異內容細節

### UI 原則

* 若 system admin 進入本頁，應以 metadata-only projection indicator 明確提示
* 若同時存在 terminal / exception / blocked 提示，projection indicator 應附著於 header 區；主 banner 仍由狀態提示承擔

---

## 7. revision timeline 設計規格

### 7.1 Timeline Row / Card 欄位

| 欄位           | 說明                     |
| ------------ | ---------------------- |
| stage        | INITIAL / REREVIEW     |
| revisionNo   | 版本號                    |
| submissionId | submission 識別          |
| status       | SUBMITTED / SUPERSEDED |
| submittedAt  | 提交時間                   |
| submittedBy  | 提交者                    |
| totalScore   | 分數摘要 [依權限]             |
| currentFlag  | 是否目前有效版本               |

### 7.2 視覺重點

* 最新有效版本要最明顯
* superseded 版本要可見，但視覺退一級
* stage 要清楚標色 / 標籤化

### 7.3 分數顯示

* manager / auditor / 合法 reviewer / rereviewer：可看 score 摘要
* system admin：不顯示 score 數值；若保留欄位僅以 `—` 佔位

---

## 8. revision 可操作性提示

這區很重要，因為本頁容易讓使用者誤解「能看歷程 = 能改」。

### 8.1 當案件 `CLOSED`

顯示：

* `案件已結案，revision 歷程唯讀，禁止所有修正與 reopen`（D-032）
* CLOSED 不顯示任何 revision 入口；未固定、未結案時才依原提交者 guard 導向作業頁（D-032）

### 8.2 當案件 `CANCELLED`

顯示：

* `案件已取消，revision 歷程僅供查閱`

### 8.3 當案件未結案，且當前角色為合法責任人

顯示：

* `若需修正，請從對應作業頁建立新 revision`
* 顯示導向作業頁按鈕

### 8.4 當角色不是合法責任人

顯示：

* `你可以查看歷程，但不可發起修正`

---

## 9. 頁面狀態設計

### 9.1 Loading

* skeleton timeline
* skeleton summary panel

### 9.2 Empty

適用情境：

* 尚無 revision
* 或尚無任何 submitted submission

顯示：

* `目前尚無 revision 歷程`
* 可顯示目前是否仍在 draft 階段

### 9.3 Ready

* 顯示 timeline + detail summary

### 9.4 Forbidden

* 無權限查看此案件 revision
* 不顯示 timeline

### 9.5 Metadata-only

* 顯示 metadata banner
* 差異與內容摘要受遮罩
* FULL-only 類別保留 label，值以 `—` 佔位
* compare 區顯示 `詳細差異內容不適用於此視角`

---

## 10. 主要互動流程

### 10.1 manager / auditor 查看歷程

```text id="v6sv4m"
案件詳情頁
  -> 點查看 revision 歷程
  -> 進入 revision 歷程頁
  -> 切換 INITIAL / REREVIEW
  -> 查看版本摘要 / 差異
```

### 10.2 reviewer 查看自己的初審歷程

```text id="vnxvop"
案件詳情頁
  -> 點查看 revision 歷程
  -> 預設切到 INITIAL
  -> 查看自己提交與修正的版本
  -> 若合法，導去初審作業頁 revision 模式
```

### 10.3 rereviewer 查看自己的複審歷程

```text id="szl1by"
案件詳情頁
  -> 點查看 revision 歷程
  -> 預設切到 REREVIEW
  -> 查看自己提交與修正的版本
  -> 若合法，導去複審作業頁 revision 模式
```

---

## 11. 對應 API 規劃

### 11.1 revision list

* `GET /review-cases/{caseId}/revisions`

### 11.2 單一 submission 詳細摘要 [可選]

* `GET /review-cases/{caseId}/submissions/{submissionId}`

### 11.3 差異摘要 API [可後補]

若後續需要伺服器端差異計算，可考慮：

* `GET /review-cases/{caseId}/submissions/{submissionId}/compare?with={otherSubmissionId}`

本版可先不強制要求。

---

## 12. Response / DTO 草稿

### 12.1 revision list response

```json id="6l8aa5"
{
  "success": true,
  "data": [
    {
      "submissionId": "SUB-INIT-001",
      "stage": "INITIAL",
      "revisionNo": 1,
      "status": "SUPERSEDED",
      "submittedAt": "2026-04-02T14:10:00+08:00",
      "submittedBy": "DOC-1001",
      "totalScore": 88.5,
      "isCurrent": false
    },
    {
      "submissionId": "SUB-INIT-002",
      "stage": "INITIAL",
      "revisionNo": 2,
      "status": "SUBMITTED",
      "submittedAt": "2026-04-02T15:00:00+08:00",
      "submittedBy": "DOC-1001",
      "totalScore": 91.0,
      "isCurrent": true
    }
  ]
}
```

### 12.2 comparison summary response [可選]

```json id="fg5mbi"
{
  "success": true,
  "data": {
    "baseSubmissionId": "SUB-INIT-001",
    "targetSubmissionId": "SUB-INIT-002",
    "changedItemCount": 3,
    "scoreDiff": {
      "from": 88.5,
      "to": 91.0
    },
    "commentChanged": true,
    "attachmentDiff": {
      "added": 1,
      "removed": 0
    }
  }
}
```

### 12.3 metadata-only revision row

```json id="pktm6y"
{
  "submissionId": "SUB-INIT-002",
  "stage": "INITIAL",
  "revisionNo": 2,
  "status": "SUBMITTED",
  "submittedAt": "2026-04-02T15:00:00+08:00",
  "submittedBy": "DOC-1001",
  "isCurrent": true,
  "viewMode": "METADATA_ONLY"
}
```

---

## 13. 頁面測試重點

本頁至少應驗證：

1. 初審與複審 revision 是否正確分組
2. `SUBMITTED` 與 `SUPERSEDED` 是否正確顯示
3. 目前有效版本是否正確標示
4. manager / auditor 是否可看完整 revision 摘要
5. reviewer / rereviewer 是否只看自己合法範圍
6. system admin 是否只看到 metadata
7. `CLOSED` / `CANCELLED` 是否正確阻擋 revision action 入口
8. 歷程頁是否不提供直接編輯能力

---

## 14. 不屬於本頁處理的事項

* 真正執行 revision
* 初審 / 複審表單填答
* full audit trail 全量事件列表
* 案件分派
* project / batch 詳細版本頁

這些應由：

* `UI-012 初審作業頁`
* `UI-013 複審作業頁`
* `UI-016 稽核查詢頁`
* `UI-011 案件分派頁`
* `UI-005 版本檢視頁`

負責。

## D-031～D-033 生命週期與資料可見性對齊

以下為 Decision Log D-031～D-033 在本頁的行為映射；領域與權限 authority 仍為 state-machine／permission-matrix。

保持閱讀／比較／合法導頁定位，不在歷程頁直接提交。D-031／032：只有未固定、未 CLOSED／CANCELLED 的合法原提交者可導向對應作業頁 revision；CLOSED 不提供例外入口或 reopen。D-033：複審醫師在 timeline／detail／compare／導頁及 cache 中始終不能取得初審內容或可反推的衍生分數，不能藉切換 stage tab 解除。

驗證映射：EX-041～052／TS-DA-001～012。其他角色已接受的資料政策不變；不以規格更新宣稱程式已落地。
