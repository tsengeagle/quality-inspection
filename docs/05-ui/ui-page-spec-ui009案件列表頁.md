# UI Page Spec v1

## UI-009 案件列表頁

---

## 1. 頁面基本資訊

### Page ID
`UI-009`

### 頁面名稱
案件列表頁

### 頁面目的
提供各角色查看其可存取範圍內的案件清單，並作為以下任務的主要入口：

- 查看案件目前狀態
- 追蹤案件進度
- 進入案件詳情
- 進入案件分派
- 進入初審作業
- 進入複審作業

### 頁面型態
`List / Work Queue`

### 對應 IA 模組
`案件作業`

### 對應主要角色
- `HOSPITAL_MANAGER`
- `REVIEWER`
- `REREVIEWER`
- `AUDITOR`
- `SYSTEM_ADMIN` [metadata only]

### 對應 domain objects
- `ReviewCase`
- `ReviewSubmission` [摘要層級]
- `SamplingBatch` [摘要參照]
- `ReviewProject` [摘要參照]

### 對應主要 API
- `GET /review-cases`

---

## 2. 頁面責任範圍

本頁負責：

1. 呈現案件清單
2. 提供角色導向的預設工作視角
3. 提供查詢、篩選、排序與分頁
4. 顯示案件目前狀態與關鍵摘要
5. 提供進入下游頁面的入口

本頁不負責：

- 顯示完整 submission 作答內容
- 執行初審 / 複審填答
- 顯示完整 revision 差異
- 直接修改案件內容
- 直接執行 revision

---

## 3. 角色視角與預設入口

---

## 3.1 HOSPITAL_MANAGER 視角

### 預設目的
- 追蹤案件整體進度
- 找待分派案件
- 找待複審案件
- 找已結案案件
- 進入案件分派與詳情

### 建議預設 tab
- `待分派`
- `待複審`
- `全部案件`

---

## 3.2 REVIEWER 視角

### 預設目的
- 找到自己待處理的初審案件
- 繼續初審中案件
- 查看自己已提交的案件摘要

### 建議預設 tab
- `待初審`
- `初審中`
- `我的案件`

---

## 3.3 REREVIEWER 視角

### 預設目的
- 找到自己待處理的複審案件
- 繼續複審中案件
- 查看自己已完成的複審案件摘要

### 建議預設 tab
- `待複審`
- `複審中`
- `我的案件`

---

## 3.4 AUDITOR 視角

### 預設目的
- 查詢授權院區內案件
- 依狀態、專案、批次、日期篩選
- 進入詳情與 lifecycle / revision 檢視

### 建議預設 tab
- `全部案件`

---

## 3.5 SYSTEM_ADMIN 視角

### 預設目的
- 查看平台層 metadata
- 追蹤異常與狀態分布
- 進入 metadata 視角的案件詳情

### 建議預設 tab
- `全部案件`

### 限制
- 僅 metadata 視角
- 不可看到完整作答內容
- 不顯示業務操作按鈕

---

## 4. 頁面主要區塊

案件列表頁建議分成 5 個區塊：

1. 頁面標題與摘要列
2. 狀態 / 工作 tab 區
3. 篩選器區
4. 案件列表表格區
5. 分頁與批次操作區

---

## 4.1 頁面標題與摘要列

### 目的
- 告知目前頁面用途
- 顯示目前角色視角
- 顯示目前查詢範圍摘要

### 建議內容
- 頁面標題：`案件列表`
- 次標題：
  - manager：本院案件總覽
  - reviewer：我的初審工作清單
  - rereviewer：我的複審工作清單
  - auditor：授權範圍案件查詢
  - sysadmin：案件 metadata 監控

### 可選摘要資訊
- 當前查詢條件摘要
- 目前結果總數
- 使用中的資料範圍標記

---

## 4.2 狀態 / 工作 tab 區

### 目的
讓使用者快速切到常用工作視角，而不是每次重新手動篩選。

### tab 設計原則
- tab 本質是預設 filter
- 不同角色可見不同 tab
- tab 不應取代進階篩選器

### 建議 tab

#### manager
- `待分派`
- `待初審`
- `待複審`
- `已結案`
- `全部案件`

#### reviewer
- `待初審`
- `初審中`
- `我的案件`

#### rereviewer
- `待複審`
- `複審中`
- `我的案件`

#### auditor
- `全部案件`
- `已結案`
- `已取消`

#### system admin
- `全部案件`

### tab 與狀態對應建議

```yaml id="x8d6yw"
tab_filters:
  待分派:
    caseStatus:
      - SELECTED_PENDING_ASSIGNMENT

  待初審:
    caseStatus:
      - PENDING_INITIAL_REVIEW

  初審中:
    caseStatus:
      - INITIAL_REVIEW_IN_PROGRESS

  待複審:
    caseStatus:
      - PENDING_REREVIEW

  複審中:
    caseStatus:
      - REREVIEW_IN_PROGRESS

  已結案:
    caseStatus:
      - CLOSED

  已取消:
    caseStatus:
      - CANCELLED
```

---

## 4.3 篩選器區

### 目的

提供精準查詢。

### 建議篩選欄位

| 篩選欄位   | 類型                    | 備註                                 |
| ------ | --------------------- | ---------------------------------- |
| 關鍵字    | text                  | caseId / 病歷 key / project 名稱       |
| 院區     | select                | 依角色 scope 控制                       |
| 專案     | select                | 可搜尋                                |
| 批次     | select / autocomplete | 可選                                 |
| 案件狀態   | multi-select          | 支援多選                               |
| 病歷類型   | multi-select          | OUTPATIENT / INPATIENT / EMERGENCY |
| 初審醫師   | select                | manager / auditor 可用               |
| 複審醫師   | select                | manager / auditor 可用               |
| 到期日區間  | date range            | dueAt                              |
| 建立時間區間 | date range            | createdAt                          |

### 角色差異

* reviewer / rereviewer 不一定需要全部篩選欄位
* system admin 不應顯示內容導向篩選，只保留 metadata 導向欄位即可

### 篩選區操作

* 查詢
* 重置
* 保留目前 tab 條件
* 顯示目前套用中的 filter chips

---

## 4.4 案件列表表格區

### 目的

以高密度但可讀的方式呈現案件關鍵資訊。

### 表格欄位建議

| 欄位     | 說明                | 顯示角色                                      |
| ------ | ----------------- | ----------------------------------------- |
| 案件編號   | caseId            | 全角色                                       |
| 專案名稱   | project           | 全角色                                       |
| 批次編號   | batchId           | 全角色                                       |
| 病歷類型   | medicalRecordType | 全角色                                       |
| 院區     | hospital          | manager / auditor / sysadmin              |
| 初審醫師   | reviewer          | manager / auditor / sysadmin / rereviewer |
| 複審醫師   | rereviewer        | manager / auditor / sysadmin              |
| 狀態     | caseStatus        | 全角色                                       |
| 截止日    | dueAt             | 全角色                                       |
| 最終分數摘要 | finalScore        | 非 sysadmin，且依資料可見性策略                      |
| 更新時間   | updatedAt         | 全角色                                       |
| 操作     | action buttons    | 依角色與狀態                                    |

### 顯示規則補充

* `SYSTEM_ADMIN` 不顯示完整內容相關欄位
* `REVIEWER` / `REREVIEWER` 只需看到與自己任務相關欄位
* `AUDITOR` 可依授權範圍看完整清單資訊，但不顯示修改按鈕

### 狀態欄呈現建議

用明確 badge：

* 已抽中待分派
* 待初審
* 初審中
* 初審完成
* 待複審
* 複審中
* 已結案
* 已退回
* 已取消

---

## 4.5 分頁與批次操作區

### 目的

支援列表型頁面的基本控制。

### 建議內容

* 總筆數
* 每頁筆數選擇
* 頁碼切換
* 排序欄位選擇
* 匯出目前清單 [僅有權限角色]

---

## 5. 操作按鈕規格

本頁的核心不是直接改資料，而是提供進入下游任務頁的正確入口。

### 5.1 每列操作按鈕建議

| 按鈕          | 用途              | 角色                       |
| ----------- | --------------- | ------------------------ |
| 查看詳情        | 進入案件詳情頁         | 全角色                      |
| 指派 / 改派     | 進入案件分派頁         | manager                  |
| 開始初審        | 進入初審作業頁         | assigned reviewer        |
| 繼續初審        | 進入初審作業頁         | assigned reviewer        |
| 開始複審        | 進入複審作業頁         | assigned rereviewer      |
| 繼續複審        | 進入複審作業頁         | assigned rereviewer      |
| 查看 revision | 進入 revision 歷程頁 | manager / auditor / 合法角色 |

---

## 5.2 操作按鈕顯示原則

### manager

* 永遠可見：查看詳情
* 狀態符合時可見：指派 / 改派
* 不顯示初審 / 複審作業按鈕

### reviewer

* 只有自己被指派案件可見：開始初審 / 繼續初審
* 非自己案件不顯示作業按鈕

### rereviewer

* 只有自己被指派複審案件可見：開始複審 / 繼續複審

### auditor

* 只顯示查看型按鈕
* 不顯示 command 按鈕

### system admin

* 只顯示 metadata 查看入口
* 不顯示任何業務 command 按鈕

---

## 6. 狀態與操作對應

### 6.1 manager 視角

| 狀態                          | 可用操作       |
| --------------------------- | ---------- |
| SELECTED_PENDING_ASSIGNMENT | 查看詳情, 指派   |
| PENDING_INITIAL_REVIEW      | 查看詳情, 改派   |
| INITIAL_REVIEW_IN_PROGRESS  | 查看詳情       |
| INITIAL_REVIEW_COMPLETED    | 查看詳情       |
| PENDING_REREVIEW            | 查看詳情, 指派複審 |
| REREVIEW_IN_PROGRESS        | 查看詳情       |
| CLOSED                      | 查看詳情       |
| RETURNED                    | 查看詳情, 改派   |
| CANCELLED                   | 查看詳情       |

---

### 6.2 reviewer 視角

| 狀態                         | 可用操作             |
| -------------------------- | ---------------- |
| PENDING_INITIAL_REVIEW     | 查看詳情, 開始初審       |
| INITIAL_REVIEW_IN_PROGRESS | 查看詳情, 繼續初審       |
| INITIAL_REVIEW_COMPLETED   | 查看詳情             |
| CLOSED                     | 查看詳情             |
| RETURNED                   | 查看詳情             |
| 其他                         | 查看詳情或不可見，依 scope |

---

### 6.3 rereviewer 視角

| 狀態                   | 可用操作             |
| -------------------- | ---------------- |
| PENDING_REREVIEW     | 查看詳情, 開始複審       |
| REREVIEW_IN_PROGRESS | 查看詳情, 繼續複審       |
| CLOSED               | 查看詳情             |
| 其他                   | 查看詳情或不可見，依 scope |

---

## 7. 資料可見性規則

本頁雖是列表頁，仍需反映 metadata only 與 full scope 的差異。

### 7.1 SYSTEM_ADMIN

* 只看 metadata
* 不顯示完整內容導向欄位
* 不顯示 full detail action

### 7.2 HOSPITAL_MANAGER

* 可看本院完整列表摘要
* 可看 reviewer / rereviewer / 狀態 / 最終分數摘要

### 7.3 REVIEWER / REREVIEWER

* 只看自己合法範圍內案件
* 不應看到不屬於自己工作的敏感內容清單

### 7.4 AUDITOR

* 只看授權院區 / 範圍
* 可看完整清單摘要
* 不顯示修改類按鈕

---

## 8. 頁面狀態設計

### 8.1 Loading

顯示 skeleton table 或 loading spinner。

### 8.2 Empty State

當沒有符合條件案件時，顯示：

* 無資料提示
* 目前篩選條件摘要
* 重置篩選按鈕

### 8.3 Error State

顯示：

* 查詢失敗訊息
* 重試按鈕

### 8.4 Forbidden State

若超出資料範圍：

* 顯示無權限提示
* 不顯示敏感內容

---

## 9. 頁面互動流程

### 9.1 manager 常見流程

```text
案件列表頁
  -> 篩選待分派
  -> 點選案件
  -> 進入案件分派頁
```

### 9.2 reviewer 常見流程

```text
案件列表頁
  -> 查看待初審 tab
  -> 點選開始初審
  -> 進入初審作業頁
```

### 9.3 rereviewer 常見流程

```text
案件列表頁
  -> 查看待複審 tab
  -> 點選開始複審
  -> 進入複審作業頁
```

### 9.4 auditor 常見流程

```text
案件列表頁
  -> 篩選特定專案 / 狀態
  -> 查看詳情
  -> 進入 revision 歷程 / lifecycle
```

---

## 10. 查詢參數建議

對應 `GET /review-cases`，本頁建議使用：

```yaml id="vbnll2"
query_params:
  keyword: string
  hospitalId: string
  projectId: string
  batchId: string
  statuses: string[]
  medicalRecordTypes: string[]
  reviewerId: string
  rereviewerId: string
  dueFrom: datetime
  dueTo: datetime
  createdFrom: datetime
  createdTo: datetime
  page: int
  size: int
  sortBy: string
  sortDirection: asc|desc
```

---

## 11. Response View 建議

### 11.1 List Row DTO

```json id="p0cs3r"
{
  "caseId": "RC-001",
  "projectId": "RP-001",
  "projectName": "2026 門診病歷品質審查",
  "batchId": "BATCH-001",
  "medicalRecordType": "OUTPATIENT",
  "hospitalId": "H-01",
  "reviewerId": "DOC-1001",
  "rereviewerId": "DOC-2001",
  "caseStatus": "PENDING_REREVIEW",
  "dueAt": "2026-04-10T23:59:59+08:00",
  "finalScore": null,
  "updatedAt": "2026-04-02T14:10:00+08:00",
  "viewMode": "FULL"
}
```

### 11.2 Metadata-only Row DTO

```json id="7xvghr"
{
  "caseId": "RC-001",
  "projectId": "RP-001",
  "batchId": "BATCH-001",
  "caseStatus": "PENDING_REREVIEW",
  "reviewerId": "DOC-1001",
  "rereviewerId": "DOC-2001",
  "dueAt": "2026-04-10T23:59:59+08:00",
  "updatedAt": "2026-04-02T14:10:00+08:00",
  "viewMode": "METADATA_ONLY"
}
```

---

## 12. 與其他頁面的關係

### 上游頁

* Dashboard 頁
* 抽選批次詳情頁
* 導航入口

### 下游頁

* 案件詳情頁
* 案件分派頁
* 初審作業頁
* 複審作業頁
* Revision 歷程頁

---

## 13. 不屬於本頁處理的事項

* submission 完整內容呈現
* revision 差異比對細節
* 初審 / 複審表單填寫
* attachment 操作
* lifecycle 詳細時間軸

這些應由：

* 案件詳情頁
* 初審作業頁
* 複審作業頁
* Revision 歷程頁

負責。

---

## 14. 測試重點

本頁至少應驗證：

1. 不同角色預設 tab 是否正確
2. scope 過濾是否正確
3. metadata only 與 full view 是否正確
4. 不同狀態的操作按鈕是否正確顯示
5. reviewer / rereviewer 是否只看到自己合法案件
6. manager 是否可看到本院完整清單摘要
7. auditor 是否受授權院區限制

## D-031～D-033 生命週期與資料可見性對齊

以下為 Decision Log D-031～D-033 在本頁的行為映射；領域與權限 authority 仍為 state-machine／permission-matrix。

列表的 action hints 必須依 D-031／032 區分已提交、已固定及已正式結案；不得把複審提交顯示為 CLOSED。依 D-033 套用角色 projection，列表不能洩漏初審內容或可反推的加權值。

驗證映射：EX-041～052／TS-DA-001～012。其他角色已接受的資料政策不變；不以規格更新宣稱程式已落地。
