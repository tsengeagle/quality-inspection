# UI Page Inventory v1

## 1. 文件目的

本文件用於盤點品質審查系統需要的 UI 功能頁，作為以下工作的基礎：

- UI 資訊架構設計
- 頁面導航規劃
- 畫面規格設計
- 前端模組切分
- 權限導向的頁面可見性規劃

本文件只定義：
- 頁面名稱
- 頁面目的
- 主要角色
- 主要內容
- 主要操作
- 對應 domain / API

本文件不處理：
- wireframe
- 欄位逐項規格
- 元件樣式
- 畫面版型細節
- 前端技術實作方式

---

## 2. 設計原則

1. 頁面切分應優先依業務流程與角色任務切分，而不是依資料表切分。
2. 不同角色可共用同一頁，但可見內容與可操作按鈕可不同。
3. 若某頁同時承擔過多責任，應拆成：
   - 列表頁
   - 詳情頁
   - 作業頁
4. 會改變 domain 狀態的操作，應有明確對應的作業頁或 command 區塊。
5. revision、terminal state、metadata only 等規則，必須反映在頁面責任設計中。

---

## 3. 頁面分群總覽

本系統目前建議分為 5 組 UI 模組：

1. 專案與規則管理
2. 抽選作業
3. 案件與審查作業
4. 查詢與報表
5. 平台與稽核輔助頁

---

## 4. 頁面清單總表

| Page ID | 頁面名稱 | 模組 | 主要角色 | 頁面型態 |
|---|---|---|---|---|
| UI-001 | 審查專案列表頁 | 專案與規則管理 | HOSPITAL_MANAGER, SYSTEM_ADMIN, AUDITOR | List |
| UI-002 | 審查專案詳情頁 | 專案與規則管理 | HOSPITAL_MANAGER, AUDITOR | Detail |
| UI-003 | 審查專案編輯頁 | 專案與規則管理 | HOSPITAL_MANAGER | Edit |
| UI-004 | 評分表管理頁 | 專案與規則管理 | HOSPITAL_MANAGER | Edit / Manage |
| UI-005 | 版本檢視頁 | 專案與規則管理 | HOSPITAL_MANAGER, AUDITOR | Detail |
| UI-006 | 抽選執行頁 | 抽選作業 | HOSPITAL_MANAGER | Action |
| UI-007 | 抽選批次列表頁 | 抽選作業 | HOSPITAL_MANAGER, AUDITOR, SYSTEM_ADMIN | List |
| UI-008 | 抽選批次詳情頁 | 抽選作業 | HOSPITAL_MANAGER, AUDITOR, SYSTEM_ADMIN | Detail |
| UI-009 | 案件列表頁 | 案件與審查作業 | HOSPITAL_MANAGER, REVIEWER, REREVIEWER, AUDITOR, SYSTEM_ADMIN | List |
| UI-010 | 案件詳情頁 | 案件與審查作業 | HOSPITAL_MANAGER, REVIEWER, REREVIEWER, AUDITOR, SYSTEM_ADMIN | Detail |
| UI-011 | 案件分派頁 | 案件與審查作業 | HOSPITAL_MANAGER | Action |
| UI-012 | 初審作業頁 | 案件與審查作業 | REVIEWER | Action |
| UI-013 | 複審作業頁 | 案件與審查作業 | REREVIEWER | Action |
| UI-014 | Revision 歷程頁 | 案件與審查作業 | HOSPITAL_MANAGER, REVIEWER, REREVIEWER, AUDITOR | History |
| UI-015 | Dashboard 頁 | 查詢與報表 | HOSPITAL_MANAGER, AUDITOR, SYSTEM_ADMIN | Summary |
| UI-016 | 稽核查詢頁 | 查詢與報表 | AUDITOR, HOSPITAL_MANAGER, SYSTEM_ADMIN | Query |
| UI-017 | 報表匯出頁 | 查詢與報表 | HOSPITAL_MANAGER, AUDITOR, SYSTEM_ADMIN | Action |

---

## 5. 頁面詳細盤點

---

## UI-001 審查專案列表頁

### 頁面目的
提供審查專案的查詢、篩選與入口管理。

### 主要角色
- `HOSPITAL_MANAGER`
- `AUDITOR`
- `SYSTEM_ADMIN` [metadata 視角]

### 主要內容
- 專案名稱
- 所屬院區
- 狀態
- 生效期間
- 當前版本
- 最近更新時間

### 主要操作
- 建立新專案 [manager]
- 進入專案詳情
- 進入專案編輯 [草稿 / 未發布]
- 篩選與搜尋

### 對應 domain / API
- `ReviewProject`
- `GET /review-projects`
- `POST /review-projects`

---

## UI-002 審查專案詳情頁

### 頁面目的
查看審查專案的完整設定摘要與版本資訊。

### 主要角色
- `HOSPITAL_MANAGER`
- `AUDITOR`

### 主要內容
- 專案基本資料
- 適用病歷類型
- 審查對象
- 日期條件
- 抽樣方式
- 分層依據
- 排除科別
- 品質線
- 生效期間
- 當前版本摘要
- 綁定評分表摘要

### 主要操作
- 啟用 / 停用專案 [manager]
- 進入編輯頁 [manager]
- 發布新版本 [manager]
- 進入版本檢視頁

### 對應 domain / API
- `ReviewProject`
- `GET /review-projects/{projectId}`
- `POST /review-projects/{projectId}/activate`
- `POST /review-projects/{projectId}/deactivate`
- `POST /review-projects/{projectId}/publish`

---

## UI-003 審查專案編輯頁

### 頁面目的
建立或編輯審查專案草稿。

### 主要角色
- `HOSPITAL_MANAGER`

### 主要內容
- 專案基本欄位
- 適用病歷類型
- 審查對象設定
- 日期條件設定
- 抽樣方式設定
- 分層依據設定
- 排除科別
- 品質線
- 生效期間

### 主要操作
- 儲存草稿
- 驗證必填
- 離開前提醒未儲存變更

### 對應 domain / API
- `ReviewProject`
- `POST /review-projects`
- `PUT /review-projects/{projectId}`

---

## UI-004 評分表管理頁

### 頁面目的
維護評分表草稿與版本。

### 主要角色
- `HOSPITAL_MANAGER`

### 主要內容
- 評分表基本資訊
- 章節清單
- 題目清單
- 分數 / 權重
- 必填規則
- 顯示規則
- 版本資訊

### 主要操作
- 建立評分表
- 新增 / 編輯章節
- 新增 / 編輯題目
- 儲存草稿
- 發布版本

### 對應 domain / API
- `ScoreSheetDefinition`
- `POST /score-sheets`
- `PUT /score-sheets/{scoreSheetId}`
- `POST /score-sheets/{scoreSheetId}/publish`

---

## UI-005 版本檢視頁

### 頁面目的
查看 project version 與 score sheet version 的正式版本資訊。

### 主要角色
- `HOSPITAL_MANAGER`
- `AUDITOR`

### 主要內容
- project version 清單
- score sheet version 清單
- 版本號
- 發布時間
- checksum / snapshot 參照摘要
- 目前生效版本標記

### 主要操作
- 切換查看不同版本
- 進入版本詳情
- 比較版本摘要 [後續可擴充]

### 對應 domain / API
- `ReviewProjectVersion`
- `ScoreSheetVersion`
- 建議後續補 version query API

---

## UI-006 抽選執行頁

### 頁面目的
執行正式抽選前的預估與正式執行。

### 主要角色
- `HOSPITAL_MANAGER`

### 主要內容
- 專案摘要
- 本次抽選模式
- 母體預估統計
- 符合 / 排除 / 預估抽中數

### 主要操作
- 預估母體
- 正式執行抽選
- 查看執行結果摘要

### 對應 domain / API
- `SamplingBatch`
- `POST /review-projects/{projectId}/sampling/preview`
- `POST /review-projects/{projectId}/sampling/execute`

---

## UI-007 抽選批次列表頁

### 頁面目的
查看歷次抽選批次與執行結果。

### 主要角色
- `HOSPITAL_MANAGER`
- `AUDITOR`
- `SYSTEM_ADMIN` [metadata only]

### 主要內容
- batchId
- project
- 執行模式
- status
- population / eligible / excluded / selected
- startedAt / finishedAt
- retry 關聯摘要

### 主要操作
- 查詢與篩選
- 進入批次詳情
- 對 failed batch 執行 retry [manager]

### 對應 domain / API
- `SamplingBatch`
- `GET /sampling-batches`
- `POST /sampling-batches/{batchId}/retry`

---

## UI-008 抽選批次詳情頁

### 頁面目的
查看某次抽選批次的完整執行資訊。

### 主要角色
- `HOSPITAL_MANAGER`
- `AUDITOR`
- `SYSTEM_ADMIN` [metadata only]

### 主要內容
- batch 基本資訊
- 規則快照摘要
- 執行統計
- selected cases 摘要
- message log
- retry 關聯資訊

### 主要操作
- retry failed batch [manager]
- 匯出抽選結果
- 導向案件列表

### 對應 domain / API
- `SamplingBatch`
- `GET /sampling-batches/{batchId}`

---

## UI-009 案件列表頁

### 頁面目的
提供各角色查看案件清單與進入後續作業。

### 主要角色
- `HOSPITAL_MANAGER`
- `REVIEWER`
- `REREVIEWER`
- `AUDITOR`
- `SYSTEM_ADMIN` [metadata only]

### 主要內容
- caseId
- project
- batch
- status
- reviewer / rereviewer
- dueAt
- medicalRecordType
- hospital
- finalScore [依權限 / 狀態顯示]

### 主要操作
- 查詢與篩選
- 進入案件詳情
- manager 進入分派頁
- reviewer / rereviewer 進入作業頁

### 對應 domain / API
- `ReviewCase`
- `GET /review-cases`

---

## UI-010 案件詳情頁

### 頁面目的
提供案件的統一查看中心，整合來源、狀態、指派、提交、revision 與生命周期資訊。

### 主要角色
- `HOSPITAL_MANAGER`
- `REVIEWER`
- `REREVIEWER`
- `AUDITOR`
- `SYSTEM_ADMIN` [metadata only]

### 主要內容
- 案件基本資料
- 來源 batch / project
- project version / score sheet version
- 當前狀態
- reviewer / rereviewer
- 初審摘要
- 複審摘要
- revision history 入口
- lifecycle 摘要

### 主要操作
- manager：指派 / 改派 / 取消
- reviewer：開始初審 / 繼續草稿 / 查看合法 revision
- rereviewer：開始複審 / 繼續草稿 / 查看合法 revision
- auditor：查看歷程
- system admin：metadata only

### 對應 domain / API
- `ReviewCase`
- `ReviewSubmission`
- `GET /review-cases/{caseId}`
- `GET /review-cases/{caseId}/revision-history`
- `GET /review-cases/{caseId}/lifecycle`

---

## UI-011 案件分派頁

### 頁面目的
進行 reviewer / rereviewer 指派與截止日設定。

### 主要角色
- `HOSPITAL_MANAGER`

### 主要內容
- 案件摘要
- 當前指派資訊
- reviewer 選擇器
- rereviewer 選擇器
- dueAt 設定

### 主要操作
- 指派初審醫師
- 改派初審醫師
- 指派複審醫師
- 改派複審醫師

### 對應 domain / API
- `ReviewCase`
- `POST /review-cases/{caseId}/assign-reviewer`
- `POST /review-cases/{caseId}/reassign-reviewer`
- `POST /review-cases/{caseId}/assign-rereviewer`
- `POST /review-cases/{caseId}/reassign-rereviewer`

---

## UI-012 初審作業頁

### 頁面目的
讓 reviewer 執行初審草稿、提交與合法 revision。

### 主要角色
- `REVIEWER`

### 主要內容
- 案件摘要
- 評分表
- 作答區
- 評語區
- 附件區
- revision 提示
- 提交結果摘要

### 主要操作
- 保存草稿
- 提交初審
- 發起初審 revision [未結案且為原審查醫師]
- 查看自己歷次初審版本

### 對應 domain / API
- `ReviewSubmission`
- `POST /review-cases/{caseId}/initial-review/start`
- `POST /review-cases/{caseId}/initial-review/draft`
- `POST /review-cases/{caseId}/initial-review/submit`
- `POST /review-cases/{caseId}/initial-review/submissions/{submissionId}/revise`

---

## UI-013 複審作業頁

### 頁面目的
讓 rereviewer 執行複審草稿、提交與合法 revision。

### 主要角色
- `REREVIEWER`

### 主要內容
- 案件摘要
- 初審摘要
- 評分表
- 作答區
- 評語區
- 附件區
- final score 區
- revision 提示

### 主要操作
- 保存草稿
- 提交複審
- 發起複審 revision [未結案且為複審醫師]
- 查看自己歷次複審版本

### 對應 domain / API
- `ReviewSubmission`
- `POST /review-cases/{caseId}/start-rereview`
- `POST /review-cases/{caseId}/rereview/draft`
- `POST /review-cases/{caseId}/rereview/submit`
- `POST /review-cases/{caseId}/rereview/submissions/{submissionId}/revise`

---

## UI-014 Revision 歷程頁

### 頁面目的
集中查看 submission revision 歷程。

### 主要角色
- `HOSPITAL_MANAGER`
- `REVIEWER`
- `REREVIEWER`
- `AUDITOR`

### 主要內容
- stage
- revisionNo
- submissionId
- submittedAt
- status
- triggeredBy
- 新舊版本差異摘要

### 主要操作
- 查看 revision timeline
- 比對不同 revision 版本
- 查看目前有效版本

### 對應 domain / API
- `ReviewSubmission`
- `GET /review-cases/{caseId}/revision-history`

---

## UI-015 Dashboard 頁

### 頁面目的
提供審查狀況的摘要監控。

### 主要角色
- `HOSPITAL_MANAGER`
- `AUDITOR`
- `SYSTEM_ADMIN` [aggregated / metadata only]

### 主要內容
- 審查完成率
- 逾期率
- 評分分布
- 複審率
- 條件篩選器

### 主要操作
- 切換篩選條件
- 進入相關清單頁
- 匯出對應報表 [可連至報表頁]

### 對應 domain / API
- dashboard read model
- `GET /reports/dashboard-summary`

---

## UI-016 稽核查詢頁

### 頁面目的
提供操作軌跡、事件歷程與案件生命周期查詢。

### 主要角色
- `AUDITOR`
- `HOSPITAL_MANAGER`
- `SYSTEM_ADMIN` [metadata only]

### 主要內容
- aggregate type
- aggregate id
- event name
- actor
- occurredAt
- hospital scope
- lifecycle 事件列表

### 主要操作
- 查詢 audit trail
- 查詢案件 lifecycle
- 篩選事件類型 / 時間區間

### 對應 domain / API
- `audit_trail`
- `GET /audit-trails`
- `GET /review-cases/{caseId}/lifecycle`

---

## UI-017 報表匯出頁

### 頁面目的
提供條件式報表匯出。

### 主要角色
- `HOSPITAL_MANAGER`
- `AUDITOR`
- `SYSTEM_ADMIN` [aggregated / metadata only]

### 主要內容
- 報表類型
- 匯出條件
- 日期區間
- scope 摘要
- 匯出狀態

### 主要操作
- 選擇報表類型
- 設定查詢條件
- 執行匯出

### 對應 domain / API
- reporting read model
- `POST /reports/export`

---

## 6. 第一階段建議優先頁面

若要先做 MVP，建議第一階段優先以下頁面：

```yaml id="gf7v6g"
phase_1_pages:
  - UI-001 審查專案列表頁
  - UI-003 審查專案編輯頁
  - UI-004 評分表管理頁
  - UI-006 抽選執行頁
  - UI-007 抽選批次列表頁
  - UI-009 案件列表頁
  - UI-010 案件詳情頁
  - UI-011 案件分派頁
  - UI-012 初審作業頁
  - UI-013 複審作業頁
````

### Phase 1 理由

* 已涵蓋主要業務主線：

  * 專案設定
  * 抽選
  * 案件分派
  * 初審
  * 複審

---

## 7. 第二階段建議頁面

```yaml id="vu2159"
phase_2_pages:
  - UI-002 審查專案詳情頁
  - UI-005 版本檢視頁
  - UI-008 抽選批次詳情頁
  - UI-014 Revision 歷程頁
  - UI-015 Dashboard 頁
  - UI-016 稽核查詢頁
  - UI-017 報表匯出頁
```

### Phase 2 理由

* 偏管理深化、稽核深化、報表深化
* 對第一階段主流程不是 blocker，但對治理與營運很重要

---

## 8. 角色與頁面對應摘要

| 角色               | 主要使用頁                                                                                  |
| ---------------- | -------------------------------------------------------------------------------------- |
| SYSTEM_ADMIN     | UI-001, UI-007, UI-008, UI-009, UI-010, UI-015, UI-016, UI-017 [metadata only]         |
| HOSPITAL_MANAGER | UI-001 ~ UI-011, UI-014, UI-015, UI-016, UI-017                                        |
| REVIEWER         | UI-009, UI-010, UI-012, UI-014                                                         |
| REREVIEWER       | UI-009, UI-010, UI-013, UI-014                                                         |
| AUDITOR          | UI-001, UI-002, UI-005, UI-007, UI-008, UI-009, UI-010, UI-014, UI-015, UI-016, UI-017 |

---

## 9. 後續應補文件

本文件完成後，建議下一步補：

1. `ui-information-architecture.md`
2. `ui-page-spec.md`
3. `ui-state-actions-matrix.md`

### 建議順序

先做：

* `案件列表頁`
* `案件詳情頁`
* `初審作業頁`

因為這三頁最能定住整個系統的 UI 主幹。

---

## 10. 不屬於本文件的事項

以下不在本文件定義範圍內：

* 欄位逐項定義
* component 細節
* form validation 細節
* loading / empty / error state 細節
* wireframe
* 視覺樣式
* 前端技術選型

---

## 11. 維護原則

1. 若新增新頁面，應先更新本文件。
2. 若某頁功能責任擴張過大，應先在本文件上拆分，再進入畫面細規格。
3. 若某頁僅因角色不同而呈現不同內容，優先保留同頁，避免不必要頁面複製。
