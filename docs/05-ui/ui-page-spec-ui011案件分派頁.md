# UI Page Spec v1

## UI-011 案件分派頁

---

## 1. 頁面基本資訊

### Page ID
`UI-011`

### 頁面名稱
案件分派頁

### 頁面目的
提供院方管理者對單一案件執行以下指派類操作：

- 指派初審醫師
- 改派初審醫師
- 指派複審醫師
- 改派複審醫師
- 設定 / 更新案件截止日

### 頁面型態
`Action / Assignment / Control Page`

### 對應 IA 模組
`案件作業`

### 對應主要角色
- `HOSPITAL_MANAGER`

### 對應次要查看角色
- 無。此頁以 manager 執行指派為主，不作為一般查詢頁。

### 對應 domain objects
- `ReviewCase`
- `ReviewerAssignment`
- `RereviewerAssignment`

### 對應主要 API
- `POST /review-cases/{caseId}/assign-reviewer`
- `POST /review-cases/{caseId}/reassign-reviewer`
- `POST /review-cases/{caseId}/assign-rereviewer`
- `POST /review-cases/{caseId}/reassign-rereviewer`

---

## 2. 頁面責任範圍

本頁負責：

1. 呈現案件目前的指派狀態
2. 呈現 reviewer / rereviewer 可選名單
3. 提供指派與改派操作
4. 提供 due date 設定
5. 依案件狀態決定哪些指派操作合法
6. 清楚區分「指派」與「改派」

本頁不負責：

- 查看完整 submission 作答內容
- 執行初審 / 複審填答
- 發起 revision
- 修改 project / rule / score sheet
- 顯示完整 audit trail

---

## 3. 頁面核心定位

案件分派頁是 manager 的「任務分流控制頁」。  
它的價值不在於資訊量大，而在於：

- 在正確時機把案件交給正確的人
- 不讓錯誤角色進入錯誤流程
- 讓 case state 與 assignment state 對齊

因此本頁設計要特別避免：

- 把 reviewer / rereviewer 指派混成同一個模糊操作
- 在不合法狀態下仍顯示可操作按鈕
- 讓 manager 誤以為分派後就能代替醫師繼續作業

---

## 4. 進入條件與 guard

### 4.1 合法進入條件
使用者需同時滿足：

- actor = `HOSPITAL_MANAGER`
- case 屬於 actor 可管理之 hospital scope
- case 狀態允許分派或改派

### 4.2 不合法進入情境
以下情境應拒絕或顯示只讀 blocked state：

- actor 不是 `HOSPITAL_MANAGER`
- case 已 `CLOSED`
- case 已 `CANCELLED`
- case 超出 actor 的 hospital scope
- case 狀態不允許目前欲執行的 assignment action

### 4.3 UI 行為建議
- 非 manager 不應進此頁
- 若案件已結案 / 已取消，頁面應顯示 blocked banner，而非開放輸入

---

## 5. 頁面主要區塊

本頁建議分為 5 個區塊：

1. 頁面標題與案件摘要區
2. 狀態 / 指派模式提示區
3. 初審指派區
4. 複審指派區
5. 提交與取消操作區

---

## 5.1 頁面標題與案件摘要區

### 目的
讓 manager 先確認這是哪一筆案件，再進行分派。

### 建議內容
- caseId
- projectName
- batchId
- medicalRecordType
- hospitalId
- caseStatus
- current reviewer
- current rereviewer
- dueAt

### 建議呈現
```text id="7h9wq0"
案件編號 | 專案 | 病歷類型 | 目前狀態 | 初審醫師 | 複審醫師 | 截止日
```

### 補充

* 此區只放任務決策需要的摘要
* 詳細追溯資訊仍以案件詳情頁為主

---

## 5.2 狀態 / 指派模式提示區

### 目的

說明目前頁面是在做哪種指派，並明確告知哪些操作可用。

### 建議模式

```yaml id="hcm8xv"
page_modes:
  - ASSIGN_INITIAL
  - REASSIGN_INITIAL
  - ASSIGN_REREVIEW
  - REASSIGN_REREVIEW
  - MIXED_ASSIGNMENT
  - READONLY_BLOCKED
```

### 模式說明

#### `ASSIGN_INITIAL`

* 尚未有 reviewer
* 目前可做初審指派

#### `REASSIGN_INITIAL`

* 已有 reviewer
* 目前允許改派 reviewer

#### `ASSIGN_REREVIEW`

* 案件已進入待複審
* 尚未有 rereviewer
* 目前可做複審指派

#### `REASSIGN_REREVIEW`

* 已有 rereviewer
* 目前允許改派 rereviewer

#### `MIXED_ASSIGNMENT`

* 某些案件狀態下，同頁可能同時要顯示 reviewer 與 rereviewer 的既有資訊，但只有其中一種 action 合法
* 例如 reviewer 已固定，但 rereviewer 尚待指派

#### `READONLY_BLOCKED`

* 案件狀態不允許任何指派操作
* 例如 `CLOSED`, `CANCELLED`

### 建議提示訊息

#### 初審指派

* `此案件尚未分派初審醫師，分派後將進入待初審流程`

#### 初審改派

* `此案件已分派初審醫師，可重新指定新的初審醫師`

#### 複審指派

* `此案件已進入複審流程，請指派複審醫師`

#### 複審改派

* `此案件已分派複審醫師，可重新指定新的複審醫師`

#### blocked

* `此案件目前不可進行分派或改派`

---

## 5.3 初審指派區

### 目的

讓 manager 對 reviewer 進行指派或改派。

### 建議內容

* 目前初審醫師摘要
* reviewer 選擇器
* dueAt 設定
* 指派原因 / 備註 [pending clarification，不屬本版正式 command contract]

### reviewer 選擇器需求

* 支援搜尋
* 顯示：

  * 醫師姓名
  * 醫師代碼
  * 所屬院區 / 科別 [若可取得]
* 僅顯示可作為 reviewer 的合法人員
* 本版 contract 只固定候選人最小欄位與 selectability；候選資料來源在目前 repo 階段可由 dev-only stub adapter 提供

### 可用操作

* 指派初審醫師
* 改派初審醫師

### 顯示規則

* `SELECTED_PENDING_ASSIGNMENT`：主要顯示初審指派
* `PENDING_INITIAL_REVIEW`：可改派
* `RETURNED`：可重新分派或改派
* 其他狀態通常不應顯示初審指派輸入元件，或只讀顯示既有資訊

---

## 5.4 複審指派區

### 目的

讓 manager 對 rereviewer 進行指派或改派。

### 建議內容

* 目前複審醫師摘要
* rereviewer 選擇器
* dueAt 設定
* 指派原因 / 備註 [pending clarification，不屬本版正式 command contract]

### rereviewer 選擇器需求

* 支援搜尋
* 顯示：

  * 醫師姓名
  * 醫師代碼
  * 所屬院區 / 科別 [若可取得]
* 僅顯示可作為 rereviewer 的合法人員
* 本版 contract 只固定候選人最小欄位與 selectability；候選資料來源在目前 repo 階段可由 dev-only stub adapter 提供

### 可用操作

* 指派複審醫師
* 改派複審醫師

### 顯示規則

* `PENDING_REREVIEW`：主要顯示複審指派
* `REREVIEW_IN_PROGRESS`：通常不建議在進行中任意改派，若允許需額外規則；本版先傾向保守，不開此操作
* `CLOSED` / `CANCELLED`：只讀

---

## 5.5 提交與取消操作區

### 目的

提供明確的 command 提交入口。

### 建議按鈕

| 按鈕     | 用途               |
| ------ | ---------------- |
| 提交初審指派 | 建立 reviewer 指派   |
| 提交初審改派 | 更新 reviewer 指派   |
| 提交複審指派 | 建立 rereviewer 指派 |
| 提交複審改派 | 更新 rereviewer 指派 |
| 返回案件詳情 | 返回 detail page   |

### UI 原則

* 不同 command 不應共用一個模糊的「儲存」按鈕
* 按鈕名稱應反映真實意圖，例如：

  * `提交初審指派`
  * `提交複審改派`

### blocked 模式

* 不顯示提交按鈕
* 只顯示返回案件詳情

---

## 6. 狀態驅動 UI 規則

---

## 6.1 `SELECTED_PENDING_ASSIGNMENT`

### 頁面行為

* 主要是初審指派模式
* reviewer 選擇器開放
* rereviewer 區只顯示不可用提示

### 可用操作

* 指派初審醫師

---

## 6.2 `PENDING_INITIAL_REVIEW`

### 頁面行為

* 顯示現有 reviewer
* 允許初審改派
* 不開放複審指派

### 可用操作

* 改派初審醫師
* 更新截止日

---

## 6.3 `INITIAL_REVIEW_IN_PROGRESS`

### 頁面行為

* 以只讀為主
* 是否允許改派 reviewer 需額外決策；本版先保守，不建議開放
* 顯示提示：

  * 案件已進入初審中，不建議改派

### 可用操作

* 預設不提供改派
* 僅返回案件詳情

---

## 6.4 `INITIAL_REVIEW_COMPLETED`

### 頁面行為

* 初審已完成；進入複審前仍必須確認已固定（D-031）
* 若尚未進入複審，不顯示 reviewer 指派
* 以只讀為主

### 可用操作

* 預設不提供指派類操作

---

## 6.5 `PENDING_REREVIEW`

### 頁面行為

* reviewer 區只讀顯示
* rereviewer 區開放指派或改派

### 可用操作

* 指派複審醫師
* 改派複審醫師 [若已有 rereviewer]

---

## 6.6 `REREVIEW_IN_PROGRESS`

### 頁面行為

* 以只讀為主
* 是否允許複審中改派需另決策；本版先保守不開放

### 可用操作

* 預設不提供改派

---

## 6.7 `RETURNED`

### 頁面行為

* 可重新處理初審 assignment
* 顯示退回原因摘要 [若可取得]

### 可用操作

* 指派初審醫師
* 改派初審醫師
* 更新截止日

---

## 6.8 `CLOSED`

### 頁面行為

* `READONLY_BLOCKED`
* 顯示：

  * 此案件已結案
  * 不可再分派或改派

### 可用操作

* 無

---

## 6.9 `CANCELLED`

### 頁面行為

* `READONLY_BLOCKED`
* 顯示：

  * 此案件已取消
  * 不可再分派或改派

### 可用操作

* 無

---

## 7. 權限與可見性規則

### 7.1 可進入角色

* 僅 `HOSPITAL_MANAGER`

### 7.2 不可進入角色

* `REVIEWER`
* `REREVIEWER`
* `AUDITOR`
* `SYSTEM_ADMIN`

### 7.3 scope 規則

* manager 僅可操作本院案件
* 不可跨院對他院案件做分派 / 改派

---

## 8. 欄位與輸入規格

---

## 8.1 reviewer 選擇器

### 類型

* searchable select
* remote autocomplete [若人員數多]

### 顯示欄位

* 醫師姓名
* 醫師代碼
* 科別 [若可取得]
* 所屬院區 [若可取得]

### 驗證

* 必填
* 必須是可用 reviewer

---

## 8.2 rereviewer 選擇器

### 類型

* searchable select
* remote autocomplete

### 顯示欄位

* 醫師姓名
* 醫師代碼
* 科別 [若可取得]
* 所屬院區 [若可取得]

### 驗證

* 在複審指派情境中為必填
* 必須是可用 rereviewer

---

## 8.3 dueAt 欄位

### 類型

* datetime picker

### 驗證

* 必填
* 本版正式 contract 只收斂「assign / reassign command 必須提供 dueAt」
* 是否不可早於目前時間、是否需額外 upper bound、是否允許沿用舊 dueAt，皆屬後續 policy clarification
* assignment context 可同時提供 `reviewerDueAt`、`rereviewerDueAt`、`effectiveDueAt` 供頁面摘要呈現

---

## 8.4 備註 / 原因欄位 [pending clarification]

### 用途

* 若後續 authority 決定需要，可讓 manager 補充改派原因
* 本版不將其視為 assign / reassign command 的正式必填或正式 request 欄位

### 類型

* textarea

### 狀態

* 待 authority 釐清後再決定是否建立正式 contract
* 本版先標記為 pending，不要求 backend / frontend 實作 request payload

---

## 9. 頁面狀態設計

### 9.1 Loading

* skeleton summary
* skeleton form fields

### 9.2 Ready

* 顯示可操作區
* 按當前案件狀態載入對應模式

### 9.3 Blocked

* 顯示不可分派原因
* 隱藏所有提交型按鈕

### 9.4 Forbidden

* 顯示無權限訊息
* 不顯示輸入元件

### 9.5 Submit Success

* 顯示成功 toast / banner
* 可導回案件詳情
* 可留在頁面看更新後的 assignment 摘要

---

## 10. 主要互動流程

### 10.1 初審指派流程

```text id="k9q00b"
案件詳情頁
  -> 點指派初審
  -> 進入案件分派頁
  -> 選擇 reviewer 與 dueAt
  -> 提交初審指派
  -> 返回案件詳情頁
```

### 10.2 初審改派流程

```text id="v4txu1"
案件詳情頁
  -> 點改派初審
  -> 進入案件分派頁
  -> 更換 reviewer
  -> 提交初審改派
```

### 10.3 複審指派流程

```text id="vwqdel"
案件詳情頁
  -> 案件進入待複審
  -> 點指派複審
  -> 進入案件分派頁
  -> 選擇 rereviewer
  -> 提交複審指派
```

---

## 11. 對應 API 規劃

### 11.1 指派初審醫師

* `POST /review-cases/{caseId}/assign-reviewer`

### 11.2 改派初審醫師

* `POST /review-cases/{caseId}/reassign-reviewer`

### 11.3 指派複審醫師

* `POST /review-cases/{caseId}/assign-rereviewer`

### 11.4 改派複審醫師

* `POST /review-cases/{caseId}/reassign-rereviewer`

---

## 12. Request / Response 草稿

### 12.1 初審指派 request

```json id="wmjlwm"
{
  "reviewerId": "DOC-1001",
  "dueAt": "2026-04-10T23:59:59+08:00",
  "note": "依科別分派"
}
```

### 12.2 初審指派 response

```json id="wo08vx"
{
  "success": true,
  "data": {
    "caseId": "RC-001",
    "reviewerId": "DOC-1001",
    "dueAt": "2026-04-10T15:59:59Z",
    "caseStatus": "PENDING_INITIAL_REVIEW"
  }
}
```

### 12.3 複審指派 request

```json id="m8dr1s"
{
  "rereviewerId": "DOC-2001",
  "dueAt": "2026-04-12T23:59:59+08:00",
  "note": "進入複審流程"
}
```

### 12.4 複審指派 response

```json id="muff8s"
{
  "success": true,
  "data": {
    "caseId": "RC-001",
    "rereviewerId": "DOC-2001",
    "caseStatus": "PENDING_REREVIEW"
  }
}
```

---

## 13. 頁面測試重點

本頁至少應驗證：

1. 只有 manager 可進入
2. 不同 case status 下的可操作模式是否正確
3. `CLOSED` / `CANCELLED` 是否正確阻擋
4. reviewer / rereviewer 選擇器是否只顯示合法名單
5. 指派與改派按鈕文字是否清楚區分
6. 複審尚未開始時才能做正常複審指派
7. scope 是否正確限制於本院案件

---

## 14. 不屬於本頁處理的事項

* reviewer / rereviewer 真正執行審查
* revision 歷程比對
* lifecycle 詳細時間軸
* project / batch 追溯詳情
* 報表與 audit trail 全量列表

這些應由：

* `UI-012 初審作業頁`
* `UI-013 複審作業頁`
* `UI-014 Revision 歷程頁`
* `UI-016 稽核查詢頁`

負責。

## D-031～D-033 生命週期與資料可見性對齊

以下為 Decision Log D-031～D-033 在本頁的行為映射；領域與權限 authority 仍為 state-machine／permission-matrix。

自動／手工進複審均以初審已固定為必要條件（D-031），本頁不得以 assignment 繞過。複審提交與結案分離不授權 manager 代替原複審提交者結案（D-032）。其他 assignment 與 scope 規則保留；資料顯示適用 D-033。

驗證映射：EX-041～052／TS-DA-001～012。其他角色已接受的資料政策不變；不以規格更新宣稱程式已落地。
