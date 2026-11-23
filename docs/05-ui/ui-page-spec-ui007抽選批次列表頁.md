# UI Page Spec v1

## UI-007 抽選批次列表頁

---

## 1. 頁面基本資訊

### Page ID
`UI-007`

### 頁面名稱
抽選批次列表頁

### 頁面目的
提供使用者查詢歷次抽選批次，並作為以下任務的入口：

- 查看歷次抽選結果
- 辨識成功 / 失敗批次
- 篩選特定 project / status / 時間區間
- 由 batch 導到案件列表
- 對 failed batch 發起 retry

### 頁面型態
`List / Execution Tracking`

### 對應 IA 模組
`抽選作業`

### 對應主要角色
- `HOSPITAL_MANAGER`
- `AUDITOR`
- `SYSTEM_ADMIN` [metadata only]

### 不建議作為主要使用角色
- `REVIEWER`
- `REREVIEWER`

### 對應 domain objects
- `SamplingBatch`
- `SamplingBatchSnapshot` [摘要層級]
- `ReviewProject` [摘要參照]

### 對應主要 API
- `GET /sampling-batches`
- `POST /sampling-batches/{batchId}/retry`

---

## 2. 頁面責任範圍

本頁負責：

1. 顯示抽選批次清單
2. 提供篩選、排序、分頁
3. 顯示每個 batch 的關鍵執行結果摘要
4. 區分 completed / failed / retry chain
5. 提供由 batch 導向案件列表的入口
6. 在合法條件下提供 retry 入口
7. 對尚未開放的 batch detail action 維持 disabled / 未開放表述

本頁不負責：

- 顯示單一 batch 的完整 message log
- 顯示完整 snapshot JSON
- 顯示 selected case 完整清單
- 執行正式抽選
- 修改 project 規則
- 做案件分派

---

## 3. 頁面核心定位

抽選批次列表頁是「抽選結果追蹤頁」。

它的核心任務不是做設定，而是回答：

1. 什麼時候跑過抽選
2. 用哪個 project / version 跑的
3. 跑得成不成功
4. 成功時抽了多少
5. 失敗時哪個 batch 可以 retry
6. 這批結果要往哪裡看下去

所以這頁應優先強化：

- 執行結果可讀性
- status 辨識
- retry chain 可追蹤性
- 從 batch 到 cases 的導頁效率

---

## 4. 進入條件與 guard

### 4.1 合法進入角色
- `HOSPITAL_MANAGER`
- `AUDITOR`
- `SYSTEM_ADMIN` [metadata only]

### 4.2 scope 規則
- manager：本院 batches
- auditor：授權院區 / 授權範圍 batches
- system admin：global metadata 視角

### 4.3 不合法角色
- `REVIEWER`
- `REREVIEWER`

### 4.4 UI 行為建議
- 無權限角色不應顯示本頁導航入口
- 若硬進頁，顯示 forbidden

---

## 5. 頁面主要區塊

本頁建議分為 5 個區塊：

1. 頁面標題與摘要列
2. 快速狀態 tab 區
3. 篩選器區
4. 批次列表表格區
5. 分頁與批次操作區

---

## 5.1 頁面標題與摘要列

### 目的
讓使用者知道目前看到的是抽選批次追蹤頁，而不是抽選執行頁。

### 建議內容
- 頁面標題：`抽選批次列表`
- 次標題：
  - manager：本院抽選批次追蹤
  - auditor：授權範圍批次查詢
  - sysadmin：抽選批次 metadata 監控

### 可選摘要資訊
- 目前查詢總筆數
- 當前篩選條件摘要
- 可 retry 批次數量 [manager 視角]

---

## 5.2 快速狀態 tab 區

### 目的
提供常用的 batch status 快速視角。

### 建議 tab

#### manager
- `全部批次`
- `成功`
- `失敗`
- `可重跑`

#### auditor
- `全部批次`
- `成功`
- `失敗`

#### system admin
- `全部批次`
- `失敗`

### 建議 tab 對應

```yaml id="ck13n7"
tab_filters:
  全部批次:
    batchStatus: []

  成功:
    batchStatus:
      - COMPLETED

  失敗:
    batchStatus:
      - FAILED

  可重跑:
    batchStatus:
      - FAILED
```

### 補充

* `可重跑` 本質上仍是 failed subset，但 UI 語意更貼近 manager 任務

---

## 5.3 篩選器區

### 目的

提供精準查詢。

### 建議篩選欄位

| 篩選欄位           | 類型                    | 備註                                                   |
| -------------- | --------------------- | ---------------------------------------------------- |
| project        | select / autocomplete | 依角色 scope                                            |
| hospital       | select                | manager 固定本院；auditor 依授權；sysadmin metadata           |
| batch status   | multi-select          | STARTED / COLLECTING / SAMPLING / COMPLETED / FAILED |
| execution mode | select                | MANUAL / SCHEDULED / RETRY                           |
| startedAt 區間   | datetime range        | 常用                                                   |
| finishedAt 區間  | datetime range        | 可選                                                   |
| retryOfBatchId | text / autocomplete   | 可選                                                   |
| batchId        | text                  | 精準查詢                                                 |

### 篩選區操作

* 查詢
* 重置
* 顯示目前 filter chips

---

## 5.4 批次列表表格區

### 目的

以高密度方式呈現 batch 執行結果。

### 表格欄位建議

| 欄位              | 說明                              | 顯示角色                         |
| --------------- | ------------------------------- | ---------------------------- |
| 批次編號            | batchId                         | 全角色                          |
| 專案名稱            | project                         | 全角色                          |
| project version | projectVersionId / versionNo 摘要 | 全角色                          |
| 執行模式            | MANUAL / SCHEDULED / RETRY      | 全角色                          |
| 狀態              | batchStatus                     | 全角色                          |
| 母體件數            | populationCount                 | 全角色                          |
| 符合件數            | eligibleCount                   | 全角色                          |
| 排除件數            | excludedCount                   | 全角色                          |
| 抽中件數            | selectedCount                   | 全角色                          |
| startedAt       | 開始時間                            | 全角色                          |
| finishedAt      | 結束時間                            | 全角色                          |
| retry 來源        | retryOfBatchId 摘要               | manager / auditor / sysadmin |
| 操作              | disabled detail / retry / 查看案件 | 依角色                          |

### 視覺重點

* `FAILED` 要有明顯失敗 badge
* `RETRY` 模式要清楚標示是重跑批次
* 若有 retry chain，可顯示 link icon 或 chain indicator

### system admin 視角

* 仍可看上述 metadata 類欄位
* 不進一步展開 business content

---

## 5.5 分頁與批次操作區

### 目的

支援列表操作與導頁。

### 建議內容

* 總筆數
* 每頁筆數
* 頁碼
* 排序控制
* 匯出目前列表 [若後續需要]

### 每列操作按鈕建議

| 按鈕   | 用途                             | 角色                           |
| ---- | ------------------------------ | ---------------------------- |
| 查看詳情 | 目前未開放；僅保留 disabled action 提示 | manager / auditor / sysadmin |
| 查看案件 | 進入案件列表並帶 batchId filter        | manager / auditor / sysadmin |
| 重跑   | 對 failed batch 建立新 batch retry | manager only                 |

### detail 目前狀態

* 目前不提供 batch detail page / route / drawer / modal
* 若頁面保留 detail action，應以 disabled / 未開放文案呈現
* 不以「現行可查看詳情」描述本頁能力

---

## 6. 狀態與操作對應

---

## 6.1 `COMPLETED`

### 顯示

* 顯示成功 badge
* 顯示統計完整數字

### 可用操作

* 查看案件

### 不可用操作

* 重跑 [本版不對 completed batch 提供 retry]

---

## 6.2 `FAILED`

### 顯示

* 顯示失敗 badge
* 顯示失敗提示摘要 [若 API 有提供]
* 顯示 retry chain 資訊 [若存在]

### manager 可用操作

* 重跑
* 查看案件 [若失敗前已產生部分資料，實作上可決定是否開放；本版預設不強調]

### auditor / sysadmin

* 僅可查看列表 metadata 與查看案件入口
* 不可重跑

---

## 6.3 `STARTED` / `COLLECTING_CANDIDATES` / `SAMPLING`

### 顯示

* 顯示進行中 badge
* 顯示 startedAt
* finishedAt 可能為空

### 可用操作

* 無 detail drill-down；維持列表追蹤

### 不可用操作

* 重跑
* 視為成功結果

---

## 6.4 `RETRY` execution mode

### 顯示

* 在 execution mode 欄位顯示 `RETRY`
* 顯示 `retryOfBatchId`

### 補充

* 讓使用者清楚這不是第一次執行，而是從某 failed batch 重跑而來

---

## 7. retry 操作規則

### 7.1 retry 按鈕顯示條件

需同時滿足：

* actor = `HOSPITAL_MANAGER`
* batchStatus = `FAILED`
* batch 屬於本院 scope

### 7.2 retry 按鈕不顯示情境

* actor 非 manager
* batch 非 failed
* batch 超出 scope
* batch 仍進行中

### 7.3 UI 提示

按下 retry 前，建議用 confirm modal 顯示：

* 原 batchId
* 這會建立新的 retry batch
* 不會覆寫舊 batch

### 建議確認文案

* `將以新的抽選批次重跑，原失敗批次會保留`

---

## 8. 權限與可見性規則

### 8.1 HOSPITAL_MANAGER

可看：

* 本院 batch 完整 metadata
* retry chain
* 統計結果

可做：

* 查看案件
* retry failed batch

### 8.2 AUDITOR

可看：

* 授權院區 / 範圍內 batch 完整 metadata
* status / 統計 / retry chain 摘要

不可做：

* retry
* execute sampling

### 8.3 SYSTEM_ADMIN

可看：

* global metadata
* status
* execution mode
* timestamps
* counts
* retry chain metadata

不可做：

* retry
* execute sampling
* 修改任何業務內容

---

## 9. 頁面狀態設計

### 9.1 Loading

* skeleton table
* skeleton filter bar

### 9.2 Empty

適用情境：

* 查無任何 batch
* 篩選後無結果

顯示：

* `目前沒有符合條件的抽選批次`
* 重置篩選按鈕

### 9.3 Ready

* 顯示列表與操作按鈕

### 9.4 Forbidden

* 無權限查看批次列表
* 不顯示 table

### 9.5 Metadata-only

* 對 system admin 不需額外拆頁
* 目前不提供 detail 進入行為；metadata-only 僅作用於列表顯示範圍

---

## 10. 主要互動流程

### 10.1 manager 追蹤抽選結果

```text id="xq2w2k"
抽選執行頁
  -> 正式執行抽選
  -> 成功後進入抽選批次列表頁
  -> 找到新 batch
  -> 點查看案件
```

### 10.2 manager 重跑失敗批次

```text id="1pmv7p"
抽選批次列表頁
  -> 切到失敗 / 可重跑 tab
  -> 點重跑
  -> confirm modal
  -> 建立新 retry batch
  -> 留在列表頁或導向新 batch 詳情
```

### 10.3 auditor 查詢歷次 batch

```text id="nthjlwm"
抽選批次列表頁
  -> 篩選 project / status / 時間
  -> 導向案件列表
```

---

## 11. 對應 API 規劃

### 11.1 batch list

* `GET /sampling-batches`

### 11.2 retry

* `POST /sampling-batches/{batchId}/retry`

### 11.3 下游導頁

* `GET /review-cases?batchId=...`

> `GET /sampling-batches/{batchId}` 目前存在 backend contract，但不作為本頁已開放的前端 detail flow。

---

## 12. Request / Response 草稿

### 12.1 batch list response

```json id="9v3e1s"
{
  "success": true,
  "data": [
    {
      "batchId": "BATCH-001",
      "projectId": "RP-001",
      "projectName": "2026 門診病歷品質審查",
      "projectVersionId": "RPV-003",
      "executionMode": "MANUAL",
      "status": "COMPLETED",
      "populationCount": 1200,
      "eligibleCount": 300,
      "excludedCount": 40,
      "selectedCount": 30,
      "retryOfBatchId": null,
      "startedAt": "2026-04-02T01:00:00Z",
      "finishedAt": "2026-04-02T01:01:12Z"
    },
    {
      "batchId": "BATCH-002",
      "projectId": "RP-001",
      "projectName": "2026 門診病歷品質審查",
      "projectVersionId": "RPV-003",
      "executionMode": "RETRY",
      "status": "FAILED",
      "populationCount": 1200,
      "eligibleCount": 0,
      "excludedCount": 0,
      "selectedCount": 0,
      "retryOfBatchId": "BATCH-001",
      "startedAt": "2026-04-03T01:00:00Z",
      "finishedAt": "2026-04-03T01:00:10Z"
    }
  ],
  "meta": {
    "page": 1,
    "size": 20,
    "total": 2
  }
}
```

### 12.2 retry response

```json id="sck94w"
{
  "success": true,
  "data": {
    "newBatchId": "BATCH-003",
    "retryOfBatchId": "BATCH-002"
  }
}
```

---

## 13. 頁面測試重點

本頁至少應驗證：

1. manager / auditor / sysadmin 的可見範圍是否正確
2. reviewer / rereviewer 不可進入
3. failed batch 是否正確顯示 retry 按鈕
4. completed batch 不顯示 retry
5. retry 後是否建立新 batch，不覆寫舊 batch
6. retry chain 資訊是否正確顯示
7. 篩選與排序是否正常
8. 由 batch 導向案件列表時是否正確帶入 batch filter

---

## 14. 不屬於本頁處理的事項

* 單一 batch 的 message log 詳細檢視
* 單一 batch 的 snapshot 詳細檢視
* selected case 明細清單
* 正式執行抽選
* project 規則修改

這些應由：

* `UI-008 抽選批次詳情頁`
* `UI-006 抽選執行頁`
* `UI-003 審查專案編輯頁`

負責。
