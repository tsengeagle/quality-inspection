# Glossary v1

## 1. 文件目的

本文件用於定義品質審查系統的共用詞彙，作為以下文件的共同語言基礎：

- 需求書
- decision log
- domain model
- state machine
- application service
- domain events
- API spec
- persistence model
- test scenarios

本文件的目的是消除語意歧義，避免同一名詞在不同文件中有不同意思。

---

## 2. 使用原則

1. 本文件優先定義業務語意，而不是技術實作細節。
2. 若同一術語同時有業務語意與技術語意，應優先以業務語意為主，技術語意作為補充。
3. 若後續需要新增名詞，應先補入本文件，再擴散到其他設計文件。
4. 本文件不負責授權規則、狀態轉移或 API path；這些應由其他文件定義。

---

## 3. 詞彙分類

本文件將術語分為：

1. 業務核心詞彙
2. 流程與狀態詞彙
3. 規則與版本詞彙
4. 審查與提交詞彙
5. 權限與可見性詞彙
6. DDD / 技術設計詞彙

---

## 4. 業務核心詞彙

---

## 審查專案 [ReviewProject]

### 定義
品質審查活動的管理主體，用來定義抽選條件、評分規則、審查醫師群、複審規則與執行範圍。

### 補充
- 系統中的抽選、分派、評分與複審，都是掛在審查專案之下。
- 在 domain model 中，`ReviewProject` 是核心 aggregate root 之一。

---

## 院區 [Hospital]

### 定義
系統中的資料歸屬單位。  
審查專案、案件、使用者角色、查詢範圍與授權範圍都可能以院區作為邊界。

### 補充
- 權限中的 `HOSPITAL` scope 即以院區為基礎。
- 稽核角色目前不具預設跨院全域權。

---

## 科別 [Department]

### 定義
院區下的業務科別主檔，可作為：
- 排除條件
- 分層抽選依據
- 案件屬性之一

---

## 病歷類型 [MedicalRecordType]

### 定義
本系統目前支援的病歷分類。

### 目前值
```yaml
MedicalRecordType:
  - OUTPATIENT
  - INPATIENT
  - EMERGENCY
```

### 補充

* 日期條件會依病歷類型套用不同欄位。

---

## 審查對象 [ReviewTarget]

### 定義

審查規則關注的管理維度，例如醫師、病例類型或未來擴充的其他對象。

### 補充

* 在專案設定中以 `ReviewTargetPolicy` 表示。
* 在案件中以 `reviewTargetRef` 表示具體對象參照。

---

## 5. 流程與狀態詞彙

---

## 抽選 [Sampling]

### 定義

根據專案規則，自病歷母體中篩選出待審查案件的過程。

### 補充

* 抽選不是單純查詢，而是正式建立批次與案件的業務行為。
* 抽選使用正式 published project version。

---

## 抽選批次 [SamplingBatch]

### 定義

一次抽選執行的正式記錄，包含執行模式、統計結果、規則快照、訊息記錄與抽中案件參照。

### 補充

* `SamplingBatch` 是 aggregate root。
* 失敗重跑不覆寫舊批次，而是建立新批次並透過 `retryOfBatchId` 關聯。

---

## 案件 [ReviewCase]

### 定義

由抽選結果產生、可被分派、初審、複審、結案與追蹤的審查單位。

### 補充

* 案件是審查流程的主要生命週期承載者。
* `ReviewCase` 是 aggregate root。

---

## 初審 [Initial Review]

### 定義

案件由審查醫師進行的第一階段評分作業。

### 補充

* 初審提交不自動結案或進複審；固定後依品質線決定流向，不需複審時由院方管理者結案（D-031）。
* 初審修正只能由原審查醫師發起。

---

## 複審 [Re-review / Rereview]

### 定義

案件符合條件後，由複審醫師進行的第二階段審查作業。

### 補充

* 複審結果由合法複審提交者負責；系統依 D-012／020 將固定初審與複審分數加權計算，不是直接取複審分數（D-032）。
* 複審修正只能由複審醫師發起。

---

## 結案 [Closed]

### 定義

案件流程正式結束的狀態。

### 補充

* `CLOSED` 是 terminal state。
* 結案後不可 reopen。
* 結案後不允許 revision。

---

## 取消 [Cancelled]

### 定義

案件被終止，不再進入審查流程的狀態。

### 補充

* `CANCELLED` 是 terminal state。
* 取消後不可再審。
* 取消後不可 revision。

---

## 退回 [Returned]

### 定義

案件在初審進行中被退回，等待重新進入待初審狀態。

### 補充

* 退回不等於取消。
* 退回後仍可重新進入初審流程。

---

## 終止狀態 [Terminal State]

### 定義

一旦進入後，案件不再允許正常審查流程命令的狀態。

### 本系統目前的 terminal states

```yaml
terminal_states:
  - CLOSED
  - CANCELLED
```

---

## 6. 規則與版本詞彙

---

## 品質線 [QualityThreshold]

### 定義

判斷案件是否需進入複審的分數門檻。

### 補充

* 初審固定後，若固定分數超過品質線，案件必須進入複審（D-031）；未固定不得進入。
* `QualityThreshold` 是 value object，不是 entity。

---

## 規則 [Rule]

### 定義

用來控制抽選、分派、評分、複審與判定的業務規範集合。

### 補充

* 規則不一定是一張表，也不一定是單一欄位。
* 在專案版本層可能拆成多種 policy。

---

## Policy

### 定義

某一類規則的結構化表示，例如：

* `ReviewTargetPolicy`
* `DateCriteriaPolicy`
* `SamplingPolicy`
* `RereviewPolicy`

### 補充

* policy 是規則的技術建模方式。
* policy 不一定是 aggregate root，通常是 value object 或 policy object。

---

## 專案版本 [ProjectVersion]

### 定義

審查專案在某一時點正式發布的規則版本。

### 補充

* 新批次只能使用正式 published project version。
* 已被採用的 project version 不可原地修改。

---

## 評分表版本 [ScoreSheetVersion]

### 定義

評分表於某一時點的正式發布版本。

### 補充

* 案件與 submission 需可追溯使用的評分表版本。
* 已被採用的 score sheet version 不可原地修改。

---

## 快照 [Snapshot]

### 定義

某一時點實際採用規則內容的不可變記錄，用來支援追溯與重現。

### 補充

* 本系統採 `version reference + immutable snapshot` 雙軌策略。
* 批次、案件、submission 都會保存 snapshot 參照或內容。

---

## Immutable Snapshot

### 定義

一旦被批次、案件或 submission 採用後，不可再修改的快照。

### 補充

* 若規則要變更，應建立新版本 / 新快照，而不是修改既有快照。

---

## 7. 審查與提交詞彙

---

## 評分表 [ScoreSheetDefinition]

### 定義

定義評分章節、題目、分數、權重、必填規則與顯示規則的結構。

### 補充

* 在 domain 中，`ScoreSheetDefinition` 是 aggregate root。
* 它不是單純的畫面表單，而是正式業務規則的一部分。

---

## 題目 [ScoreItem]

### 定義

評分表中的最小評分單位。

### 補充

* `ScoreItem` 有 `itemId`、`code`、`title`、`scoreType`、`maxScore`、`weight` 等資訊。
* `ScoreItem` 是 entity，但不是 aggregate root。

---

## 章節 [Section]

### 定義

評分表中的題目分組單位。

### 補充

* 用來表達構面、區塊或顯示群組。

---

## 提交 [Submission]

### 定義

某案件在某審查階段的一次實際送出結果。

### 補充

* submission 可為草稿、已提交、或被後續 revision 取代。
* `ReviewSubmission` 是 aggregate root。

---

## 草稿 [Draft]

### 定義

尚未正式送出的 submission 狀態。

### 補充

* 草稿可保存，但不代表正式審查完成。

---

## 已提交 [Submitted]

### 定義

已正式送出的 submission 狀態。

### 補充

* `SUBMITTED` 不可原地覆寫。
* 若需修正，必須建立 revision。

---

## 被取代 [Superseded]

### 定義

某筆已提交 submission 因後續 revision 而不再作為目前有效版本，但仍保留歷史紀錄的狀態。

---

## 修正 [Revision]

### 定義

對已提交 submission 的正式修正行為，會建立新的 submission version，而非覆寫舊版。

### 補充

* revision 只允許對未固定的合法已提交結果，在未結案、未取消案件上由合法原提交者發起（D-031／032）
* 初審 revision 僅原審查醫師
* 複審 revision 僅複審醫師
* revision 不等於 reopen

---

## append-only revision model

### 定義

submission 一旦提交後，只能新增新版本，不可直接覆寫原版本的修正模式。

### 補充

* 舊版本保留
* 新版本遞增 `revisionNo`
* 舊版轉為 `SUPERSEDED`

---

## 附件 [Attachment]

### 定義

submission 關聯的補充檔案資訊。

### 補充

* attachment association 是 domain 事實
* 實體檔案本身通常不直接存於交易表

---

## 8. 權限與可見性詞彙

---

## 操作權限 [Operation Permission]

### 定義

某角色是否可執行某個 command 或業務動作。

### 例子

* 是否可發起抽選
* 是否可提交初審
* 是否可修正複審

---

## 資料範圍 [Data Scope]

### 定義

某角色可存取資料的邊界。

### 本系統常用 scope

* `GLOBAL_METADATA`
* `HOSPITAL`
* `SELF_ASSIGNED`
* `AUTHORIZED_HOSPITALS_ONLY`

---

## 內容可見性 [Content Visibility]

### 定義

某角色即使可查詢某筆資料，是否可看到完整內容。

### 例子

* system admin 可看 metadata，但不可看完整作答
* manager 可看本院完整作答
* auditor 可看授權範圍內完整內容

---

## Metadata View

### 定義

僅包含執行狀態、識別欄位、時間、角色參照等摘要資訊的資料視圖。

### 補充

* 不包含完整 answers / full comments / attachment content

---

## Full Detail View

### 定義

包含完整作答、評語、附件資訊等業務內容的資料視圖。

---

## 9. DDD / 技術設計詞彙

---

## Bounded Context

### 定義

具有清楚語意邊界與模型邏輯的設計範圍。

### 本系統目前主要 contexts

* Review Project Configuration
* Sampling
* Assignment and Review
* Audit and Reporting

---

## Aggregate

### 定義

一組需要一起維護一致性的 domain object 邊界，並由 aggregate root 對外提供操作入口。

### 補充

* aggregate 不是「比較大的 entity」
* 它是交易一致性邊界

---

## Aggregate Root

### 定義

aggregate 對外唯一應被直接操作的根物件。

### 本系統例子

* `ReviewProject`
* `ScoreSheetDefinition`
* `SamplingBatch`
* `ReviewCase`
* `ReviewSubmission`

---

## Entity

### 定義

具有持續 identity 的 domain object。

### 本系統例子

* `ScoreItem`
* `Section`
* `ReviewerAssignment`

---

## Value Object

### 定義

以值與語意為主，不以獨立 identity 為主的物件。

### 本系統例子

* `QualityThreshold`
* `EffectivePeriod`
* `RuleSnapshotRef`
* `ScoreSheetVersionRef`

---

## Domain Invariant

### 定義

無論從哪個入口進入系統，都不可被破壞的業務約束。

### 本系統例子

* 初審超過品質線必須進複審
* 結案後不可 revision
* 同批次不得重複抽中同一案件

---

## Domain Event

### 定義

描述某個業務事實已成立的事件。

### 本系統例子

* `ReviewProjectVersionPublished`
* `SamplingBatchCompleted`
* `InitialReviewCompleted`
* `ReviewSubmissionRevised`

---

## Integration Event

### 定義

用於通知其他系統或非同步處理的事件，不等於 domain 事實本身。

### 補充

* 不直接列入 domain event catalog
* 通常由 application layer / outbox 轉出

---

## State Machine

### 定義

定義某個 domain object 狀態如何合法轉移的模型。

### 補充

* 本系統最核心的是 `ReviewCase` state machine
* submission 也有自己的子狀態機

---

## Application Service

### 定義

負責 orchestration 的應用層服務。

### 補充

* 負責載入 aggregate、開 transaction、協調外部系統
* 不應直接承載核心業務規則

---

## Read Model

### 定義

為查詢、報表、dashboard 而建立的資料投影，不作為交易真實來源。

---

## Outbox

### 定義

保存待發送 integration event 的持久化模式，用於可靠發布事件。

---

## 10. 容易混淆的詞彙對照

| 詞彙 A            | 詞彙 B              | 差異                                              |
| --------------- | ----------------- | ----------------------------------------------- |
| Revision        | Reopen            | revision 是新 submission version；reopen 是案件生命週期重開 |
| Domain Event    | Integration Event | 前者是業務事實；後者是對外通知                                 |
| Aggregate       | Entity            | aggregate 是一致性邊界；entity 是有 identity 的物件         |
| Metadata View   | Full Detail View  | 前者是摘要；後者含完整作答內容                                 |
| Project Version | Snapshot          | 前者是正式版本；後者是實際採用內容的不可變記錄                         |

---

## 11. 不屬於本文件的事項

以下不在本文件定義範圍內：

* 哪個角色可以做什麼
* API endpoint 命名
* DTO 結構
* persistence schema
* error code
* 測試案例步驟

這些應由：

* permission matrix
* api spec
* persistence model
* test scenarios

負責。

---

## 12. 後續維護原則

1. 新增重要業務名詞時，先更新 glossary。
2. 若某名詞在不同文件中出現歧義，以 glossary 為優先對照來源。
3. 若後續設計演進導致定義改變，應同步更新：

   * `glossary.md`
   * `decision-log.md`
   * 受影響的設計文件

## 固定與結案資格（D-031～D-033）

* 提交：形成正式審查結果，不等於固定或正式結案。
* 初審固定：已提交初審結果停止接受 revision 的業務事實，是進入複審及無需複審時 manager 結案的必要條件；操作授權角色與狀態／欄位表示 unknown。
* 結案資格：合法複審提交完成且無其他既有阻擋條件，不代表自動結案；不新增額外條件。
* 正式結案：複審路徑由原複審提交者決定時間，同時固定尚未固定的複審結果；CLOSED 禁止所有 revision 與 reopen。
* 全生命週期盲審：複審醫師始終不可取得初審答案、分數、評語或可反推初審分數的衍生資訊；不影響系統依 D-012／020 計算加權分數。
