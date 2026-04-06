# Context Map v1

## 1. 文件目的

本文件用於定義品質審查系統的 bounded context 劃分、上下游關係、整合邊界與語意責任。

本文件的用途：

- 明確說明各 context 負責什麼、不負責什麼
- 協助後續模組化設計與 package / module 切分
- 避免 aggregate 與 service 穿透 context 邊界
- 協助決定 integration point、ACL、read model 與 ownership

本文件不重複描述 aggregate 內部欄位、API path 或資料表結構。  
這些應由：
- `domain-model.md`
- `api-spec.md`
- `persistence-model.md`

負責。

---

## 2. 設計原則

1. bounded context 依業務語意與一致性邊界切分，不依畫面或資料表切分。
2. 同一名詞在不同 context 中若語意不同，應以各 context 自身語意為準。
3. context 間交換正式依據時，應盡量使用：
   - version reference
   - snapshot reference
   - 明確 DTO / command / event
4. 下游 context 不得直接改寫上游 context 的內部規則。
5. read model / reporting 不應反向決定 transactional domain truth。

---

## 3. Bounded Context 清單

本系統目前採以下 4 個主要 bounded context。

| Context | 主要責任 | 核心 Aggregate |
|---|---|---|
| Review Project Configuration | 專案、規則、評分表、版本與發布 | `ReviewProject`, `ScoreSheetDefinition` |
| Sampling | 母體取得、條件套用、抽樣、批次建立、案件來源參照 | `SamplingBatch` |
| Assignment and Review | 案件生命週期、指派、初審、複審、revision | `ReviewCase`, `ReviewSubmission` |
| Audit and Reporting | 查詢、統計、報表、稽核、歷程投影 | 以 read model 為主 |

---

## 4. Context 詳細說明

---

## 4.1 Review Project Configuration Context

### 主要責任
- 建立與維護審查專案
- 維護抽選規則
- 維護評分表
- 管理專案版本與評分表版本
- 發布正式 version
- 提供 batch / case / submission 可追溯的 version 基礎

### 核心模型
- `ReviewProject`
- `ProjectVersion`
- `ScoreSheetDefinition`
- `ScoreSheetVersion`
- 各類 policy：
  - `ReviewTargetPolicy`
  - `DateCriteriaPolicy`
  - `SamplingPolicy`
  - `StratificationPolicy`
  - `RereviewPolicy`

### 不負責
- 不直接執行抽選
- 不直接管理案件生命週期
- 不直接保存 submission 結果
- 不直接產出 dashboard 報表

### 對外輸出
- Published project version
- Published score sheet version
- Immutable snapshot 生成依據

### 上下游角色
- 對 `Sampling` 為 upstream
- 對 `Assignment and Review` 間接為 upstream
- 對 `Audit and Reporting` 提供版本語意依據

---

## 4.2 Sampling Context

### 主要責任
- 根據 published project version 執行正式抽選
- 取得病歷母體
- 套用日期條件、排除規則、抽樣方法
- 建立 `SamplingBatch`
- 產生 selected case refs
- 建立 `ReviewCase` 的來源依據

### 核心模型
- `SamplingBatch`
- `RuleSnapshot`
- `SelectedCaseRef`

### 不負責
- 不定義專案規則內容
- 不維護評分表內容
- 不直接處理初審 / 複審提交流程
- 不決定最終分數

### 對外輸入
- 來自 `Review Project Configuration` 的 published version / snapshot basis
- 來自外部病歷來源的 candidate data

### 對外輸出
- `SamplingBatch`
- `SamplingBatchSnapshot`
- `ReviewCasesSelected`
- case 建立所需來源參照

### 上下游角色
- `Review Project Configuration` 是 upstream
- `Assignment and Review` 是 downstream
- `Audit and Reporting` 以它為事件 / read model 上游之一

---

## 4.3 Assignment and Review Context

### 主要責任
- 建立與管理案件生命週期
- 指派初審 / 複審醫師
- 管理初審、複審、revision
- 管理 terminal state
- 決定何時進入複審
- 決定結案與最終分數

### 核心模型
- `ReviewCase`
- `ReviewSubmission`

### 不負責
- 不重新定義抽選規則
- 不重新定義 project / score sheet 正式版本
- 不直接生成 dashboard / reporting projection

### 對外輸入
- 來自 `Sampling` 的 selected case source
- 來自 `Review Project Configuration` 的 version / snapshot 參照

### 對外輸出
- 案件生命周期事件
- submission 事件
- final score / conclusion
- revision 歷程

### 上下游角色
- `Sampling` 是直接 upstream
- `Review Project Configuration` 是規則意義上的 upstream
- `Audit and Reporting` 是 downstream

### 補充
這是本系統最核心的流程 context。  
`ReviewCase` state machine 與 `ReviewSubmission` revision model 都在此 context 內成立。

---

## 4.4 Audit and Reporting Context

### 主要責任
- 查詢案件、批次、版本與歷程
- 統計審查完成率、逾期率、評分分布、複審率
- 輸出報表
- 提供 dashboard read model
- 提供 audit trail 檢視能力

### 核心模型
- `review_case_read_model`
- `audit_trail`
- `dashboard_summary`
- 各類報表 projection

### 不負責
- 不作為交易真實來源
- 不回頭控制案件狀態
- 不回頭修改 submission / batch / project

### 對外輸入
- 來自前三個 context 的 domain event / read projection source

### 對外輸出
- 查詢 API
- 報表 API
- 稽核視圖
- dashboard 摘要

### 上下游角色
- 對其他三個 context 為 downstream
- 不應反向成為上游決策來源

---

## 5. 外部 Supporting Contexts

除了核心 4 個 context，本系統還依賴幾個外部 supporting context。

---

## 5.1 Medical Record Source Context [External]

### 角色
提供抽選所需的病歷母體資料。

### 與本系統關係
- 對 `Sampling` 為 upstream external source
- 不屬於本系統核心 domain

### 整合方式
- `MedicalRecordSourceAdapter`
- 同步查詢或批次匯入

### 補充
本系統不擁有病歷主資料 truth，只消費它。

---

## 5.2 Identity and Access Context [External / Supporting]

### 角色
提供：
- 帳號
- 角色
- 權限
- scope

### 與本系統關係
- 對 permission / authorization 為 upstream supporting context
- 不屬於審查核心 domain

### 補充
`SYSTEM_ADMIN`、`HOSPITAL_MANAGER`、`REVIEWER`、`REREVIEWER`、`AUDITOR` 的實際授權落地可由此 supporting context 提供。

---

## 5.3 Notification Context [External / Supporting]

### 角色
提供待審、複審、提醒通知能力。

### 與本系統關係
- 消費本系統事件
- 不屬於本系統核心 domain

### 補充
像 `ReviewerNotificationRequested`、`RereviewReminderRequested` 這類都應在 application / integration 層處理，而不是列為 core domain event。

---

## 6. Context Map 圖

```mermaid id="vrmn0z"
flowchart LR
    A[Review Project Configuration]
    B[Sampling]
    C[Assignment and Review]
    D[Audit and Reporting]

    E[Medical Record Source]
    F[Identity and Access]
    G[Notification]

    A --> B
    A --> C
    B --> C

    A --> D
    B --> D
    C --> D

    E --> B
    F --> A
    F --> C
    F --> D
    C --> G
````

---

## 7. Upstream / Downstream 關係

| Upstream                     | Downstream            | 關係說明                                              |
| ---------------------------- | --------------------- | ------------------------------------------------- |
| Review Project Configuration | Sampling              | Sampling 只能使用 published version 與其規則基線            |
| Review Project Configuration | Assignment and Review | 案件與 submission 需可追溯 project / score sheet version |
| Sampling                     | Assignment and Review | ReviewCase 建立依賴抽選結果                               |
| Review Project Configuration | Audit and Reporting   | 報表需理解版本與規則語意                                      |
| Sampling                     | Audit and Reporting   | 批次與抽選統計為報表上游                                      |
| Assignment and Review        | Audit and Reporting   | 案件與 submission 是查詢 / 稽核 / dashboard 的核心來源         |
| Medical Record Source        | Sampling              | 提供母體，不決定審查規則                                      |
| Identity and Access          | 各 context             | 提供角色與 scope，不提供審查 domain truth                    |
| Assignment and Review        | Notification          | 通知屬下游外部副作用                                        |

---

## 8. 關係型態判斷

### 8.1 Review Project Configuration -> Sampling

建議型態：**Published Language + Conformist**

#### 說明

* Sampling 直接接受 published project version 與 snapshot 基線
* Sampling 不應重解釋規則語意
* 若規則模型演進，Sampling 跟著上游語意調整

---

### 8.2 Sampling -> Assignment and Review

建議型態：**Customer / Supplier**

#### 說明

* Assignment and Review 依賴 Sampling 產生 case source
* 但案件生命週期由 Assignment and Review 自己掌控
* Assignment and Review 不應回頭控制 Sampling 的抽樣演算法

---

### 8.3 Assignment and Review -> Audit and Reporting

建議型態：**Published Language**

#### 說明

* 報表與稽核應基於明確事件 / read model / projection
* 不應直接穿透 aggregate 內部狀態去拼裝報表 truth

---

### 8.4 Medical Record Source -> Sampling

建議型態：**ACL [Anti-Corruption Layer]**

#### 說明

* 外部病歷來源不一定與本系統語意一致
* 應透過 adapter / mapping 轉成 Sampling 可理解的 candidate model

---

### 8.5 Identity and Access -> Core Contexts

建議型態：**Supporting Context**

#### 說明

* 提供角色與 scope
* 不應與 core domain 耦合為同一模型

---

## 9. Context 交界處的正式交換物

這一節很重要，因為它定義 context 之間應交換什麼，而不是直接共用內部 entity。

---

## 9.1 Review Project Configuration -> Sampling

### 應交換

* `projectId`
* `projectVersionId`
* published rule set
* snapshot basis
* scoreSheetVersionRef
* qualityThreshold

### 不應交換

* 直接共用 mutable project draft object
* 直接讓 Sampling 改 project 內部 state

---

## 9.2 Sampling -> Assignment and Review

### 應交換

* `batchId`
* `projectId`
* `projectVersionId`
* `sourceCaseKey`
* `medicalRecordType`
* `hospitalId`
* `ruleSnapshotRef`

### 不應交換

* 直接把 `SamplingBatch` 當成 `ReviewCase` 內部可修改子物件
* 讓 Assignment and Review 回頭修改 selected case 結果

---

## 9.3 Assignment and Review -> Audit and Reporting

### 應交換

* `caseId`
* `caseStatus`
* `submissionId`
* `stage`
* `revisionNo`
* `finalScore`
* `eventName`
* `occurredAt`

### 不應交換

* read model 直接回寫交易 aggregate
* 報表查詢直接成為案件狀態判定依據

---

## 10. 不應跨越的邊界

### 10.1 Sampling 不應直接決定案件最終狀態

原因：

* 抽選只決定案件來源，不負責審查生命週期

### 10.2 Assignment and Review 不應直接改 project rule

原因：

* 規則管理屬 Review Project Configuration

### 10.3 Audit and Reporting 不應回寫 domain truth

原因：

* 報表是 projection，不是 transaction source

### 10.4 Identity and Access 不應成為 domain rule 的唯一承載者

原因：

* 角色與 scope 可由 IAM 提供
* 但像 `CLOSED` 後不可 revision，仍是 domain rule，不是 IAM rule。

---

## 11. 建議的程式模組切分

若後續落在單一 repository 的 modular monolith，可考慮下列模組切分：

```text id="0f3j7d"
quality-review/
  project-config/
  sampling/
  review-execution/
  audit-reporting/
  shared-kernel/
  adapters/
```

### 說明

* `project-config` -> Review Project Configuration Context
* `sampling` -> Sampling Context
* `review-execution` -> Assignment and Review Context
* `audit-reporting` -> Audit and Reporting Context
* `shared-kernel` -> 共享 enum / id / error code / common contract
* `adapters` -> Medical Record Source / IAM / Notification 等整合點

---

## 12. Shared Kernel 建議範圍

可共用，但需保守控制：

### 建議可放入 shared kernel

* `ProjectId`, `BatchId`, `CaseId`, `SubmissionId`
* `MedicalRecordType`
* `CaseStatus`
* `BatchStatus`
* error code 常數
* audit metadata 結構
* snapshot checksum 型別

### 不建議放入 shared kernel

* `ReviewProject` aggregate
* `SamplingBatch` aggregate
* `ReviewCase` aggregate
* 各 context 專屬 service / repository / policy

---

## 13. 與其他文件的對應

| 文件                       | 與 context map 的關係                       |
| ------------------------ | --------------------------------------- |
| `glossary.md`            | 定義共用語言                                  |
| `domain-model.md`        | 定義各 context 內 aggregate 與 invariant     |
| `state-machine.md`       | 定義 Assignment and Review context 內核心狀態流 |
| `application-service.md` | 定義 context 內 / context 間 orchestration  |
| `domain-events.md`       | 定義跨 context projection 的業務事實            |
| `api-spec.md`            | 將各 context 對外暴露成 HTTP 契約                |
| `persistence-model.md`   | 定義 context 的持久化邊界                       |
| `decision-log.md`        | 記錄已確立的跨文件設計決策                           |

---

## 14. 不屬於本文件的事項

以下不在本文件定義範圍內：

* aggregate 欄位明細
* API endpoint path
* DB table schema
* 權限矩陣細節
* error code
* 測試步驟

---

## 15. 後續維護原則

1. 若新增新的 bounded context，必須先更新本文件。
2. 若某 context 的責任被重新切分，應同步更新：

   * `context-map.md`
   * `domain-model.md`
   * `application-service.md`
3. 若外部整合方式改變，例如病歷來源或 IAM 邊界調整，也應更新本文件中的 supporting context 描述。
