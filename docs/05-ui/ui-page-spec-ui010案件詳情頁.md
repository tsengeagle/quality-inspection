# UI Page Spec v1

## UI-010 案件詳情頁

---

## 1. 頁面基本資訊

### Page ID
`UI-010`

### 頁面名稱
案件詳情頁

### 頁面目的
提供單一案件的完整資訊整合視圖，作為以下任務的中心頁面：

- 理解案件目前狀態
- 查看案件來源與追溯資訊
- 查看初審 / 複審摘要
- 查看 revision / lifecycle 入口
- 依角色與狀態決定下一步操作
- 作為分派、初審、複審、歷程檢視的中繼頁

### 頁面型態
`Detail / Control Center`

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
- `ReviewSubmission`
- `SamplingBatch`
- `ReviewProject`
- `ScoreSheetVersion`
- `RuleSnapshotRef`

### 對應主要 API
- `GET /review-cases/{caseId}`
- `GET /review-cases/{caseId}/revision-history`
- `GET /review-cases/{caseId}/lifecycle`

---

## 2. 頁面責任範圍

本頁負責：

1. 呈現單一案件的主資訊
2. 呈現案件來源與追溯資訊
3. 呈現案件目前所處狀態
4. 呈現初審 / 複審摘要
5. 呈現 revision / lifecycle 入口
6. 顯示依角色與狀態合法的操作入口

本頁不負責：

- 直接填寫完整初審表單
- 直接填寫完整複審表單
- 直接做 revision 差異比對詳表
- 直接做抽選重跑
- 直接維護 project / score sheet 規則

---

## 3. 頁面核心定位

案件詳情頁是「單一案件的控制台」。  
它應同時回答使用者四個問題：

1. 這個案件是什麼
2. 它從哪裡來
3. 現在走到哪一步
4. 我現在可以做什麼

因此這頁的資訊結構應優先支援：

- 狀態判讀
- 流程判讀
- 角色判讀
- 追溯判讀

而不是只把所有欄位平鋪。

---

## 4. 頁面主要區塊

本頁建議分為 7 個區塊：

1. 頁面標題與案件主摘要列
2. 狀態與操作區
3. 來源與追溯區
4. 初審摘要區
5. 複審摘要區
6. Revision / Lifecycle 入口區
7. 角色提示與限制說明區

---

## 4.1 頁面標題與案件主摘要列

### 目的
讓使用者一進頁面就能快速定位這筆案件。

### 建議內容
- `caseId`
- project 名稱
- batchId
- 病歷類型
- 院區
- 當前狀態 badge
- dueAt
- reviewer / rereviewer 摘要
- view mode 標記：
  - `FULL`
  - `METADATA_ONLY`
  - metadata-only 在複合狀態下以 header subtitle / badge / inline indicator 呈現，不與主狀態 banner 競爭

### 建議版面
上方一列摘要卡，固定呈現：

```text id="oqj6my"
案件編號 | 專案 | 批次 | 病歷類型 | 狀態 | 截止日 | 初審醫師 | 複審醫師
```

---

## 4.2 狀態與操作區

### 目的

根據案件狀態與角色，給出目前可用的合法操作。

### 顯示原則

* 按鈕顯示以 `caseStatus + role + assignment + visibility` 決定
* terminal state 要明確顯示不可操作原因
* 不可讓使用者誤以為 `revision = reopen`

### 建議內容

* 目前狀態說明
* 下一步說明
* 主要 action buttons
* 狀態提示 message

### 建議 action button 區

#### manager

* 指派 / 改派初審醫師
* 指派 / 改派複審醫師
* 取消案件
* 查看 revision 歷程

#### reviewer

* 開始初審
* 繼續初審
* 查看自己的 revision 歷程

#### rereviewer

* 開始複審
* 繼續複審
* 查看自己的 revision 歷程

#### auditor

* 查看 revision 歷程
* 查看 lifecycle

#### system admin

* 查看 metadata
* 查看 lifecycle metadata

---

## 4.3 來源與追溯區

### 目的

支援你系統非常重視的可追溯性。

### 必須顯示

* `projectId`
* `projectName`
* `projectVersionId`
* `batchId`
* `scoreSheetVersionId`
* `ruleSnapshotChecksum`
* `medicalRecordKey`
* `medicalRecordType`
* `hospitalId`
* `departmentId` [若有]

### 補充

這區是案件詳情頁和一般 CRUD 詳情頁最不同的地方。
因為它不是單純展示資料來源，而是讓使用者知道：

* 這筆案件是由哪個批次抽來的
* 套用的是哪個 project version
* 用的是哪個 score sheet version
* 未來 audit / debugging 要從哪裡追

### 建議操作

* 點 project version / batchId 可導向對應詳情頁
* system admin 在這區仍可看 metadata

---

## 4.4 初審摘要區

### 目的

摘要顯示初審目前狀況，而不直接進入完整表單。

### 建議顯示內容

* 是否已有初審 submission
* latest initial submissionId
* 初審 submission status
* totalScore
* submittedAt
* reviewer
* 是否已被 superseded
* 若有 revision，顯示 revision count

### 角色差異

* manager / auditor：可看摘要
* reviewer：可看自己的摘要與操作入口
* rereviewer：僅可看不洩漏初審答案／分數／評語的流程 metadata（D-033）
* system admin：metadata only，不顯示完整答案內容

### 建議操作

* 進入初審作業頁 [合法角色]
* 查看 revision 歷程

---

## 4.5 複審摘要區

### 目的

顯示複審目前狀況與最終分數決策資訊。

### 建議顯示內容

* 是否已進複審
* rereviewRequired
* latest rereview submissionId
* 複審 submission status
* finalScore
* submittedAt
* rereviewer
* revision count

### 顯示邏輯

* 若案件尚未進複審，可顯示「尚未進入複審」
* 若案件已進複審，顯示完整摘要
* 若案件已結案，需清楚顯示 final score 來源

### 建議操作

* 進入複審作業頁 [合法角色]
* 查看 revision 歷程

---

## 4.6 Revision / Lifecycle 入口區

### 目的

將歷程與追溯入口集中，避免把詳情頁塞太滿。

### 建議內容

* revision count
* lifecycle event count
* 最近一次 revision / state transition 時間
* 最近一次操作人

### 建議操作

* 查看 revision 歷程
* 查看 lifecycle
* 查看 audit trail [有權限角色]

### 補充

這區可以先做成摘要卡 + 入口，不必一開始就把完整時間軸攤在主頁。

---

## 4.7 角色提示與限制說明區

### 目的

降低誤操作與認知錯誤。

### 建議顯示場景

#### 當案件為 `CLOSED`

顯示：

* 此案件已結案
* 不可再 revision
* 不可 reopen
* 僅可查看歷程

#### 當案件為 `CANCELLED`

顯示：

* 此案件已取消
* 不可再進入審查流程
* 不可 revision

#### 當使用者為 system admin

顯示：

* 目前為 metadata only 視角
* metadata 欄位採 shared base + page extension whitelist（不可回傳 full answers/comments/attachments）
* 完整作答內容已遮罩

#### 當使用者非合法責任人

顯示：

* 目前角色僅可查看，不可操作

---

## 5. 角色視角差異

---

## 5.1 HOSPITAL_MANAGER 視角

### 可看

* 本院完整案件資訊
* 初審 / 複審摘要
* revision 摘要
* lifecycle 摘要
* 追溯資訊

### 可做

* 指派 / 改派
* 取消案件
* 進 revision 歷程
* 進 lifecycle 檢視

### 不可做

* 代替 reviewer / rereviewer 直接提交審查
* 代為發起 revision

---

## 5.2 REVIEWER 視角

### 可看

* 自己合法範圍內案件資訊
* 初審摘要
* 必要的複審狀態摘要
* 追溯摘要

### 可做

* 開始 / 繼續初審
* 查看 revision 歷程
* 若合法，發起初審 revision

### 不可做

* 指派 reviewer / rereviewer
* 進行複審提交
* 查看超出合法範圍內容

---

## 5.3 REREVIEWER 視角

### 可看

* 自己合法範圍內案件資訊
* 初審固定的流程事實，不含初審答案／分數／評語或可反推值
* 本人複審摘要，不含可反推初審分數的 finalScore
* 追溯摘要

### 可做

* 開始 / 繼續複審
* 若合法，發起複審 revision
* 查看 revision 歷程

### 不可做

* 指派 reviewer / rereviewer
* 發起初審 revision

---

## 5.4 AUDITOR 視角

### 可看

* 授權範圍內完整案件詳情
* revision 摘要
* lifecycle 摘要
* 追溯資訊

### 可做

* 查看 revision 歷程
* 查看 lifecycle
* 查看 audit trail

### 不可做

* 指派
* 提交
* revision
* 取消案件

---

## 5.5 SYSTEM_ADMIN 視角

### 可看

* metadata
* status
* actor ids
* timestamps
* batch / project / version 參照
* lifecycle metadata

### 不可看

* 完整 answers
* full comments
* attachment content
* full detail review content

### 可做

* 無業務操作
* 僅 metadata 層追蹤與導航

---

## 6. 操作按鈕矩陣

| 按鈕             | manager | reviewer | rereviewer | auditor |        sysadmin |
| -------------- | ------: | -------: | ---------: | ------: | --------------: |
| 查看 revision 歷程 |       Y |   Y 合法範圍 |     Y 合法範圍 |       Y | Y metadata only |
| 查看 lifecycle   |       Y |   Y 合法範圍 |     Y 合法範圍 |       Y | Y metadata only |
| 指派初審           |       Y |        N |          N |       N |               N |
| 改派初審           |       Y |        N |          N |       N |               N |
| 指派複審           |       Y |        N |          N |       N |               N |
| 改派複審           |       Y |        N |          N |       N |               N |
| 開始初審           |       N |    Y 僅本人 |          N |       N |               N |
| 繼續初審           |       N |    Y 僅本人 |          N |       N |               N |
| 開始複審           |       N |        N |      Y 僅本人 |       N |               N |
| 繼續複審           |       N |        N |      Y 僅本人 |       N |               N |
| 取消案件           |       Y |        N |          N |       N |               N |

---

## 7. 狀態驅動 UI 規則

### 7.1 `SELECTED_PENDING_ASSIGNMENT`

* 顯示：待分派
* manager 可見：指派初審
* reviewer / rereviewer 無作業按鈕

### 7.2 `PENDING_INITIAL_REVIEW`

* 顯示：待初審
* assigned reviewer 可見：開始初審
* manager 可見：改派

### 7.3 `INITIAL_REVIEW_IN_PROGRESS`

* 顯示：初審中
* assigned reviewer 可見：繼續初審
* manager 只能看摘要，不可代填

### 7.4 `INITIAL_REVIEW_COMPLETED`

* 顯示：初審完成
* 若尚未轉複審，顯示等待判定 / 已完成摘要
* 不顯示 reviewer 作業按鈕

### 7.5 `PENDING_REREVIEW`

* 顯示：待複審
* manager 可見：指派 / 改派複審
* assigned rereviewer 可見：開始複審

### 7.6 `REREVIEW_IN_PROGRESS`

* 顯示：複審中
* assigned rereviewer 可見：繼續複審

### 7.7 `RETURNED`

* 顯示：已退回
* manager 可視流程重新分派
* reviewer 視實作可見退回原因
* 不直接顯示 submit action

### 7.8 `CLOSED`

* 顯示：已結案
* 不顯示任何審查作業按鈕
* 顯示 final score
* 顯示不可 revision 提示

### 7.9 `CANCELLED`

* 顯示：已取消
* 不顯示任何審查作業按鈕
* 顯示不可再審提示

---

## 8. 主要資訊卡設計建議

### 8.1 案件摘要卡

* caseId
* project
* batch
* status
* dueAt
* reviewer
* rereviewer

### 8.2 追溯卡

* projectVersionId
* scoreSheetVersionId
* batchId
* snapshot checksum

### 8.3 初審摘要卡

* latestInitialSubmissionId
* totalScore
* submittedAt
* revision count

### 8.4 複審摘要卡

* latestRereviewSubmissionId
* finalScore
* submittedAt
* revision count

### 8.5 歷程入口卡

* revision count
* lifecycle count
* 最近事件時間

---

## 9. 頁面狀態設計

### 9.1 Loading

* skeleton cards
* skeleton summary sections

### 9.2 Not Found

* 案件不存在
* 提供返回列表頁按鈕

### 9.3 Forbidden

* 無權限查看此案件
* 顯示簡潔權限提示
* 不顯示敏感摘要

### 9.4 Metadata-only View

* 顯示 metadata banner
* 隱藏 answers / full comments / attachment content
* 仍可顯示 status / ids / timestamps / actor ids

---

## 10. 頁面互動流程

### 10.1 manager 典型流程

```text id="z1udpt"
案件詳情頁
  -> 查看狀態與來源
  -> 指派或改派
  -> 回案件詳情頁
  -> 查看初審 / 複審摘要
```

### 10.2 reviewer 典型流程

```text id="p4p7d7"
案件詳情頁
  -> 查看案件摘要與目前狀態
  -> 開始初審 / 繼續初審
  -> 回案件詳情頁
  -> 查看初審摘要 / revision 入口
```

### 10.3 rereviewer 典型流程

```text id="5bcz31"
案件詳情頁
  -> 查看初審摘要
  -> 開始複審 / 繼續複審
  -> 回案件詳情頁
  -> 查看 final score / revision 入口
```

### 10.4 auditor 典型流程

```text id="w0fm0r"
案件詳情頁
  -> 查看來源與追溯
  -> 查看 revision 歷程
  -> 查看 lifecycle
```

---

## 11. 對應 API 規劃

### 11.1 主資料

* `GET /review-cases/{caseId}`

### 11.2 revision 摘要 / 歷程

* `GET /review-cases/{caseId}/revision-history`

### 11.3 lifecycle

* `GET /review-cases/{caseId}/lifecycle`

### 11.4 下游作業頁入口

* 初審作業頁 API 由初審頁負責
* 複審作業頁 API 由複審頁負責
* 分派 command 由分派頁負責

---

## 12. Response View 建議

### 12.1 Full Detail View

```json id="s5t3go"
{
  "caseId": "RC-001",
  "projectId": "RP-001",
  "projectName": "2026 門診病歷品質審查",
  "projectVersionId": "RPV-003",
  "batchId": "BATCH-001",
  "scoreSheetVersionId": "SSV-002",
  "ruleSnapshotChecksum": "abc123",
  "medicalRecordType": "OUTPATIENT",
  "hospitalId": "H-01",
  "departmentId": "D001",
  "caseStatus": "PENDING_REREVIEW",
  "dueAt": "2026-04-10T15:59:59Z",
  "reviewerId": "DOC-1001",
  "rereviewerId": "DOC-2001",
  "latestInitialSubmission": {
    "submissionId": "SUB-INIT-001",
    "status": "SUBMITTED",
    "totalScore": 88.5,
    "submittedAt": "2026-04-02T06:10:00Z",
    "revisionCount": 2
  },
  "latestRereviewSubmission": null,
  "viewMode": "FULL"
}
```

### 12.2 Metadata-only View

```json id="4rlo1t"
{
  "caseId": "RC-001",
  "caseStatus": "PENDING_REREVIEW",
  "projectName": "品質審查專案",
  "hospitalId": "H001",
  "reviewerId": "DOC-1001",
  "rereviewerId": "DOC-2001",
  "updatedAt": null,
  "latestRevisionSummary": null
}
```

兩個 null 僅表達目前值契約／實作未完成（UNKNOWN），不作完成度承諾。

---

## 13. 測試重點

本頁至少應驗證：

1. 不同角色看到的 detail view 是否正確
2. metadata only 與 full detail 是否正確切換
3. terminal state 下按鈕是否正確隱藏 / disabled
4. manager 不可代 reviewer / rereviewer 執行作業
5. reviewer / rereviewer 只能對自己合法案件看到作業入口
6. lifecycle / revision 入口是否在合法角色下可見
7. 追溯資訊是否完整呈現

---

## 14. 不屬於本頁處理的事項

* 完整初審填答與提交
* 完整複審填答與提交
* revision 差異逐題比對
* audit trail 全量列表
* project / batch version 詳細頁

這些應由：

* 初審作業頁
* 複審作業頁
* Revision 歷程頁
* 稽核查詢頁
* 版本檢視頁

負責。

## D-031～D-033 生命週期與資料可見性對齊

以下為 Decision Log D-031～D-033 在本頁的行為映射；領域與權限 authority 仍為 state-machine／permission-matrix。

本頁維持 control center／導頁責任。初審未固定不得導向可執行複審或無需複審的 manager 結案；複審已提交不等於 CLOSED；正式結案限原複審提交者且具資格，操作 binding 見 D-053。CLOSED 僅唯讀歷程，無例外修正入口（D-031／032）。複審醫師的所有摘要、finalScore、history 導頁均遵守 D-033。

驗證映射：EX-041～052／TS-DA-001～012。其他角色已接受的資料政策不變；不以規格更新宣稱程式已落地。


## D-036 — UI-010 metadata 八欄與 Lifecycle 延後邊界

UI-010 SYSTEM_ADMIN metadata-only 僅含 `caseId`, `caseStatus`, `projectName`, `hospitalId`, `reviewerId`, `rereviewerId`, `updatedAt`, `latestRevisionSummary`。`lifecycleLatestStage` 暫移除，JSON 必須不存在，不能回 null；目前狀態使用 `caseStatus`。

`updatedAt`、`latestRevisionSummary` 的值契約與實作仍未完成（UNKNOWN），目前 null 不表示已完成。不得加入 FULL-only、敏感巢狀資訊或因新增 DTO getter 自動擴大白名單。

既有 Lifecycle Query／API 與 EX-039／TS-TR-005 完整關鍵路徑需求保留；新增 Milestone／Latest Stage 投影延後，未取消需求。D-035 Fail-closed、D-033 永久盲審及既有 Role／Scope 不變。
