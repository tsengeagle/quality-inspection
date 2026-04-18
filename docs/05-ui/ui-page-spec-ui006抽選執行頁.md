# UI Page Spec v1

## UI-006 抽選執行頁

---

## 1. 頁面基本資訊

### Page ID
`UI-006`

### 頁面名稱
抽選執行頁

### 頁面目的
提供院方管理者對單一審查專案執行以下操作：

- 查看本次抽選的專案與版本摘要
- 預估抽選母體
- 查看符合 / 排除 / 預估抽中統計
- 正式執行抽選
- 在完成後導向抽選批次結果

### 頁面型態
`Action / Preview + Execute`

### 對應 IA 模組
`抽選作業`

### 對應主要角色
- `HOSPITAL_MANAGER`

### 不建議作為主要使用角色
- `REVIEWER`
- `REREVIEWER`
- `AUDITOR`
- `SYSTEM_ADMIN`

### 對應 domain objects
- `ReviewProject`
- `ProjectVersion`
- `SamplingBatch`

### 對應主要 API
- `GET /review-projects/{projectId}/sampling/execute-context`
- `POST /review-projects/{projectId}/sampling/preview`
- `POST /review-projects/{projectId}/sampling/execute`

---

## 2. 頁面責任範圍

本頁負責：

1. 載入頁面時先取得 execute-context bootstrap 資訊
1. 呈現本次抽選所依據的專案與版本摘要
2. 顯示抽樣方式、分層依據、排除規則等關鍵資訊
3. 讓 manager 先做預估
4. 顯示 preview 統計結果
5. 讓 manager 在確認後正式執行抽選
6. 執行成功後導向 batch 結果視角

本頁不負責：

- 維護 project / score sheet 規則
- 修改專案設定
- 查看歷次 batch 詳細歷程
- 執行 batch retry
- 顯示 selected case 完整清單
- 直接做案件分派

---

## 3. 頁面核心定位

抽選執行頁是 manager 的「正式抽選前確認頁」。

它必須清楚區分兩種操作：

1. `Preview`
2. `Execute`

這兩者的語意不同：

- preview 只計算、不建 batch
- execute 會建立正式 batch 與案件來源參照

因此這頁不能只做成一顆「開始抽選」按鈕。  
它至少要讓使用者先知道：

- 現在用的是哪個專案版本
- 抽樣方式是什麼
- 預估母體有多少
- 排除後剩多少
- 正式執行之後會產生什麼結果

頁面初始化時，應先向 `GET /review-projects/{projectId}/sampling/execute-context` 取得 bootstrap context，再決定首屏狀態與可見區塊。現階段此端點僅作為頁面 bootstrap context 來源；preview / execute 後的 session state ownership 仍待後續收斂。當前實作下，`previewStats` 仍屬 page-local session state，execute failure 也仍允許 explicit fallback summary 補足短暫 UI 狀態。

---

## 4. 進入條件與 guard

### 4.1 合法進入條件
使用者需同時滿足：

- actor = `HOSPITAL_MANAGER`
- 專案屬於 actor 可管理的 hospital scope
- 專案可被查看

### 4.2 可 preview 條件
需同時滿足：

- project 存在
- project 狀態可供抽選
- 至少有可用的 published project version

### 4.3 可 execute 條件
需同時滿足：

- actor = `HOSPITAL_MANAGER`
- project = active
- 存在 published project version
- preview 成功或系統可直接計算執行條件
- 無 blocking validation error

### 4.4 不合法進入情境
以下情境應顯示 blocked state：

- actor 非 `HOSPITAL_MANAGER`
- project 不屬於本院 scope
- project 未啟用
- 尚無 published version
- 必要規則配置不完整

### 4.5 UI 行為建議
- project 不可抽選時，不顯示 execute primary button
- 顯示具體 blocked reason，而不是單純按鈕 disabled 無說明

---

## 5. 頁面主要區塊

本頁建議分為 6 個區塊：

1. 頁面標題與專案摘要區
2. 抽選規則摘要區
3. preview 操作區
4. preview 統計結果區
5. execute 確認區
6. 執行結果與導頁區

---

## 5.1 頁面標題與專案摘要區

### 目的
讓 manager 明確知道這次抽選是對哪個專案執行。

### 建議內容
- projectName
- projectId
- hospitalId
- project status
- current projectVersionId
- versionNo
- effectivePeriod
- scoreSheetVersionId
- qualityThreshold

### 建議呈現
```text id="08a2nn"
專案名稱 | 專案編號 | 院區 | 專案狀態 | 版本號 | 生效期間 | 評分表版本 | 品質線
```

### 補充

* 若 project 未 active 或無 published version，要在這區明顯顯示 warning
* 本區首屏資料以 execute-context 回傳欄位為準

---

## 5.2 抽選規則摘要區

### 目的

讓 manager 在執行前再次確認規則基線。

### 建議內容

* medicalRecordTypes
* reviewTargetPolicy 摘要
* dateCriteriaPolicy 摘要
* samplingPolicy 摘要
* stratificationPolicy 摘要
* excludedDepartments 摘要

### 顯示重點

* 抽樣方式：

  * `RATIO`
  * `STRATIFIED`
* 若為 `STRATIFIED`，需顯示分層依據
* 若有排除科別，需顯示已套用

### UI 原則

* 這裡顯示的是「執行依據摘要」
* 若要改規則，應導回專案編輯頁，而不是在本頁直接改

### 建議操作

* 查看專案詳情
* 查看版本檢視頁

---

## 5.3 preview 操作區

### 目的

先做母體預估，不直接建 batch。

### 建議內容

* 執行模式提示：

  * `MANUAL`
* preview 說明文字：

  * 本操作僅預估，不建立正式抽選批次

### 建議按鈕

* `預估母體`
* `重新預估`

### 按鈕顯示規則

* project 可抽選時才顯示
* 若 project blocked，隱藏或 disabled 並搭配原因提示

### 補充

* preview 完成後才顯示 execute 主區塊，體驗上更合理
* 但是否強制一定先 preview，再 execute，可由後端策略決定
* UI 第一版建議導向先 preview 再 execute

---

## 5.4 preview 統計結果區

### 目的

把抽選前的重要數量資訊顯示出來，降低 manager 對正式執行的不確定感。

### 建議內容

* populationCount
* eligibleCount
* excludedCount
* selectedCount [若 preview 能計算]
* warning / info messages

### 建議視覺結構

四張 summary cards：

```text id="0lv4hb"
母體件數 | 符合件數 | 排除件數 | 預估抽中件數
```

### 額外顯示

* 若無符合件數，顯示：

  * 本次條件下沒有可抽選案件
* 若 eligibleCount 過低，顯示提示
* 若 selectedCount = 0，execute 按鈕應 disabled

### 補充

* 本區是執行決策關鍵
* 不應只用 toast 一閃而過
* 當前畫面上的 preview 統計不等於 backend persisted truth；本輪實作仍允許以 page-local session state 暫存

---

## 5.5 execute 確認區

### 目的

在正式執行前做最後確認。

### 建議內容

* 本次將使用的 project version
* 本次為 `MANUAL` 執行
* 執行後將建立正式 `SamplingBatch`
* 執行後將建立案件來源參照
* 執行後不可把 preview 視為正式結果

### 建議按鈕

* `正式執行抽選`
* `返回專案詳情`

### 建議確認文案

* `確認後將建立正式抽選批次與案件來源資料`
* `若需調整規則，請先返回專案設定`

### 可選互動

* confirm modal：

  * 顯示關鍵統計
  * 顯示 project version 與 score sheet version
  * 再次確認

---

## 5.6 執行結果與導頁區

### 目的

正式執行成功後，提供使用者清楚的下一步。

### 成功後應顯示

* batchId
* batch status
* selectedCount
* post-execute summary [若 bootstrap context 已回傳]
* 導向按鈕：

  * 查看抽選批次列表

### 現況限制

* batch detail page / route 尚未落地，故不以「查看抽選批次詳情」作為現行完成動線
* 前往案件列表的成功導頁尚無 repo 內明確落地 evidence，不列為現行完成動線
* `startedAt / finishedAt` 目前不視為 execute success response 的穩定輸出；若需顯示，屬 pending confirmation

### 失敗後應顯示

* 執行失敗訊息
* 若後端提供 message log 摘要，可顯示
* 提供：

  * 返回專案詳情
  * 稍後前往批次列表查看失敗紀錄

---

## 6. 狀態與模式設計

本頁不是 case lifecycle 頁，但仍需要清楚 UI mode。

### 建議模式

```yaml id="9117te"
page_modes:
  - READY_FOR_PREVIEW
  - PREVIEW_COMPLETED
  - EXECUTING
  - EXECUTE_SUCCESS
  - EXECUTE_FAILED
  - READONLY_BLOCKED
```

### 模式說明

#### `READY_FOR_PREVIEW`

* 專案可用
* 尚未做 preview

#### `PREVIEW_COMPLETED`

* 已取得統計
* 可正式執行
* 此狀態可由當前頁面的 preview session 建立，不等於 backend 已持久化相同狀態

#### `EXECUTING`

* 正在執行抽選
* 顯示 loading / progress state

#### `EXECUTE_SUCCESS`

* batch 已成功建立
* 畫面進入 success 仍以 execute 後重新讀取的 bootstrap context 為準，不以 execute command response 單獨承擔完整頁面恢復

#### `EXECUTE_FAILED`

* 抽選失敗
* 顯示錯誤訊息與下一步建議
* 若 backend 最新批次失敗態尚未即時反映，頁面可先用 explicit fallback summary 顯示失敗

#### `READONLY_BLOCKED`

* project 不可執行抽選
* 例如 inactive / no published version

---

## 7. 狀態驅動 UI 規則

---

## 7.1 project = active 且有 published version

### 頁面行為

* 可做 preview
* 可做 execute
* 顯示完整規則摘要

---

## 7.2 project 非 active

### 頁面行為

* `READONLY_BLOCKED`
* 顯示：

  * 專案未啟用，不可執行抽選
* 不顯示 execute primary action

---

## 7.3 project 無 published version

### 頁面行為

* `READONLY_BLOCKED`
* 顯示：

  * 尚無正式版本，不可執行抽選
* 提供返回專案詳情 / 發布版本入口

---

## 7.4 preview 結果 `eligibleCount = 0`

### 頁面行為

* 顯示 empty result
* execute disabled
* 顯示：

  * 本次條件下無可抽選案件

---

## 7.5 execute 進行中

### 頁面行為

* 鎖定按鈕
* 顯示 loading
* 避免重複提交

---

## 7.6 execute 成功

### 頁面行為

* 顯示 success banner
* 顯示 batchId
* 顯示導向按鈕

---

## 7.7 execute 失敗

### 頁面行為

* 顯示 error banner
* 顯示簡要錯誤訊息
* 不自動當場重跑
* 導向批次列表 / 專案詳情

---

## 8. 權限與可見性規則

### 8.1 可進入角色

* 僅 `HOSPITAL_MANAGER`

### 8.2 不可進入角色

* `REVIEWER`
* `REREVIEWER`
* `AUDITOR`
* `SYSTEM_ADMIN`

### 8.3 scope 規則

* manager 僅可操作本院 project
* 不可跨院執行抽選

---

## 9. 主要互動流程

### 9.1 正常抽選流程

```text id="y7k27e"
審查專案詳情頁
  -> 進入抽選執行頁
  -> 讀取 execute-context bootstrap
  -> 查看規則摘要
  -> 預估母體
  -> 查看 preview 統計
  -> 正式執行抽選
  -> 成功後前往抽選批次列表頁
```

### 9.2 專案 blocked 流程

```text id="vsu06i"
審查專案詳情頁
  -> 進入抽選執行頁
  -> 發現專案未啟用或無正式版本
  -> 返回專案詳情 / 專案設定
```

### 9.3 execute 失敗流程

```text id="5r5unl"
抽選執行頁
  -> 正式執行抽選
  -> 執行失敗
  -> 顯示失敗訊息
  -> 前往抽選批次列表查看失敗批次
```

---

## 10. 對應 API 規劃

### 10.1 bootstrap context

* `GET /review-projects/{projectId}/sampling/execute-context`

### 10.2 preview

* `POST /review-projects/{projectId}/sampling/preview`

### 10.3 execute

* `POST /review-projects/{projectId}/sampling/execute`

### 10.4 導頁相關

* `GET /sampling-batches`

> `GET /sampling-batches/{batchId}` 與 `GET /review-cases?batchId=...` 目前不作為本頁已落地成功動線的必要前提。

---

## 11. Request / Response 草稿

### 11.1 preview request

```json id="w6n3sg"
{
  "mode": "MANUAL"
}
```

### 11.2 preview response

```json id="ubw2r8"
{
  "success": true,
  "data": {
    "populationCount": 1200,
    "eligibleCount": 300,
    "excludedCount": 40,
    "selectedCount": 30
  }
}
```

### 11.3 execute request

```json id="6z7i6f"
{
  "mode": "MANUAL"
}
```

### 11.4 execute success response

```json id="rzg2g7"
{
  "success": true,
  "data": {
    "batchId": "BATCH-001",
    "status": "COMPLETED",
    "selectedCount": 30
  }
}
```

> 若成功後需顯示 `startedAt` / `finishedAt`，目前屬 pending confirmation，不在本頁現行穩定 contract 內。

### 11.5 execute failed response

```json id="07lnfh"
{
  "success": false,
  "error": {
    "code": "SAMPLING_EXECUTION_FAILED",
    "message": "抽選執行失敗，請至批次列表查看詳細訊息",
    "details": {
      "projectId": "RP-001"
    }
  }
}
```

---

## 12. 頁面狀態設計

### 12.1 Loading

* 專案摘要 skeleton
* 規則摘要 skeleton
* 統計區 skeleton

### 12.2 Ready

* 可 preview
* execute 區未啟用或待 preview

### 12.3 Preview Completed

* 顯示統計結果
* execute primary button 啟用

### 12.4 Executing

* primary button loading
* 防止重複點擊

### 12.5 Success

* 顯示 success banner
* 顯示 batchId 與導向按鈕

### 12.6 Failed

* 顯示 error banner
* 顯示導向至批次列表的入口

### 12.7 Blocked

* 顯示 project 不可抽選原因
* 隱藏或 disabled execute

---

## 13. 頁面測試重點

本頁至少應驗證：

1. 只有 manager 可進入
2. inactive project 是否正確 blocked
3. 無 published version 是否正確 blocked
4. preview 成功是否不建立正式 batch
5. execute 成功是否建立正式 batch
6. preview 統計是否正確呈現
7. eligibleCount = 0 時 execute 是否正確 disabled
8. execute 進行中是否防重複提交
9. 成功後是否正確導向 batch / case 視角
9. 成功後是否正確導向批次列表
10. 失敗時是否提供清楚下一步

---

## 14. 不屬於本頁處理的事項

* batch retry
* batch message log 詳細檢視
* selected cases 明細瀏覽
* 案件分派
* 抽選規則修改

這些應由：

* `UI-007 抽選批次列表頁`
* `UI-011 案件分派頁`
* `UI-003 審查專案編輯頁`

> `UI-008 抽選批次詳情頁` 目前未落地，不列為本頁現行依賴。

負責。
