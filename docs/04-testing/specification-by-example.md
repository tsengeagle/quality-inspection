# Specification by Example v1

> D-031～D-033 對齊：所有 revision 成功 fixture 均須對應結果未固定；複審 fixture 的初審必已固定。所有共用 FULL view、完整歷程與分數示例均依角色套用 D-033：同案複審醫師不得取得初審內容或可反推的衍生資訊；其他角色政策不變。新驗證映射 EX-041～052 ↔ TS-DA-001～012。


## 1. 文件目的

本文件用於以具體例子定義品質審查系統的核心業務規則，作為以下工作的共同對齊基礎：

- 業務規則澄清
- DDD 設計驗證
- acceptance criteria 對齊
- test scenario 設計
- API / application service / domain test 映射

本文件的角色不是：
- 取代需求書
- 取代 domain model
- 取代 test scenarios
- 取代 API spec

本文件的角色是：

> 以具體、可驗證、可討論的例子，把重要規則固定下來。

---

## 2. 使用原則

1. 每個 example 都應固定一條或一組高度相關的規則。
2. example 優先描述業務事實，而不是 HTTP 細節。
3. 每個 example 都應可映射到：
   - domain rule
   - application service
   - API acceptance
   - test scenario
4. 若後續規則變更，應優先更新 example，再回頭同步其他規格文件。
5. 同一條規則若存在成功路徑與拒絕路徑，應盡量同時有 example 與 counter-example。

---

## 3. Example Template

每個 example 使用以下結構：

```yaml
example_template:
  id: EX-xxx
  title: 規則主題
  rule: 要固定的業務規則
  why_it_matters: 為何這條規則需要被明確固定
  given:
    - 前置條件
  when:
    - 觸發動作
  then:
    - 預期結果
  notes:
    - 補充說明
  mapping:
    domain:
      - 對應 aggregate / invariant / state
    application:
      - 對應 use case
    api:
      - 對應 API
    tests:
      - 對應 test-scenarios.md
```

---

## 4. 主流程 Examples

---

## EX-001 初審固定且不需複審時由 manager 結案

### Rule

初審提交不固定或結案；固定後依既有規則不需複審時由 manager 正式結案（D-031）。

### Given

初審分數 75、品質線 80；合法原提交者；同院 manager。

### When

先提交，再確認初審固定事實成立，manager 正式結案。

### Then

提交後 INITIAL_REVIEW_COMPLETED、無 ReviewCaseClosed；固定前 manager 結案拒絕。固定後無需複審，manager 結案才 CLOSED 並產生 ReviewCaseClosed。

### Mapping

* Authority: D-031／D-032／D-033（依適用規則）；D-004／005／012／020 保留。
* Tests: TS-IR-006、TS-DA-003

## EX-002 初審固定後超過品質線自動進複審

### Rule

初審固定是進複審必要條件，> threshold 自動進複審；手工派發同樣不得繞過固定（D-031）。

### Given

初審分數 88.5、品質線 80；原提交者合法提交。

### When

提交後先嘗試進複審，再確認初審固定事實成立。

### Then

未固定保持初審完成，不產生升級事件；固定後依規則進 PENDING_REREVIEW、rereviewRequired=true，產生 ReviewCaseEscalatedToRereview。固定僅由目前有效初審結果的合法提交醫師執行（DD-03）；API UNKNOWN。

### Mapping

* Authority: D-031／D-032／D-033（依適用規則）；D-004／005／012／020 保留。
* Tests: TS-IR-007、TS-DA-001

## EX-003 複審提交計算加權分數但不結案

### Rule

D-012／020 公式不變；D-032 提交與結案分離；D-033 不向複審醫師暴露加權結果。

### Given

固定初審 80、複審 92，發布權重各 0.5；初審已固定、複審提交者合法。

### When

複審醫師正式提交。

### Then

SUBMITTED；系統內 finalScore=86，未 CLOSED、未固定、無 ReviewCaseClosed；原提交者具資格但尚未正式結案。回應複審醫師不含可反推初審的 finalScore。

### Mapping

* Authority: D-031／D-032／D-033（依適用規則）；D-004／005／012／020 保留。
* Tests: TS-RR-003、TS-DA-004、TS-DA-010

## 5. Revision Examples

---

## EX-004 原審查醫師可修正已提交初審結果

### Rule

已提交的初審結果，只能由原提交者在初審未固定且未結案／未取消前發起 revision。

### Why it matters

這條規則同時固定了：

* responsibility ownership
* revision actor 限制
* revision 與 overwrite 的差異

### Given

* 存在一筆 `INITIAL` 階段 submission
* submission 狀態為 `SUBMITTED`
* 初審結果未固定
* 該案件尚未 `CLOSED`
* 該案件尚未 `CANCELLED`
* actor = 原審查醫師

### When

* actor 發起初審 revise 動作

### Then

* 建立新的 submission record
* 新 submission `revisionNo = 舊版 + 1`
* 舊 submission 狀態變為 `SUPERSEDED`
* 案件主狀態不因 revision 自動改變
* 產生 `ReviewSubmissionRevised`

### Notes

* revision 不得覆寫舊 submission
* revision 不等於 reopen

### Mapping

```yaml
mapping:
  domain:
    - ReviewSubmission.reviseFromSubmitted
    - initial revision actor guard
  application:
    - ReviseInitialReviewUseCase
  api:
    - POST /review-cases/{caseId}/initial-review/submissions/{submissionId}/revise
  tests:
    - TS-RV-001
    - TS-RV-007
    - TS-RV-008
```

---

## EX-005 非原審查醫師不可修正初審結果

### Rule

初審 revision 不能由非原審查醫師發起。

### Why it matters

若不固定，manager、其他 reviewer 或 sysadmin 可能被實作成可代修，破壞責任邊界。

### Given

* 存在一筆 `INITIAL` 階段 submission，狀態為 `SUBMITTED`
* 案件未結案
* actor 不是原審查醫師

### When

* actor 嘗試發起初審 revise

### Then

* 系統拒絕請求
* 不建立新 revision
* 不改變舊 submission
* 不產生 `ReviewSubmissionRevised`

### Notes

* manager / sysadmin / auditor 都屬於 counter-example actor

### Mapping

```yaml
mapping:
  domain:
    - initial revision actor guard
  application:
    - ReviseInitialReviewUseCase
  api:
    - POST /review-cases/{caseId}/initial-review/submissions/{submissionId}/revise
  tests:
    - TS-RV-002
```

---

## EX-006 複審醫師可修正已提交複審結果

### Rule

已提交的複審結果，只能由合法原提交者在複審未固定且未結案／未取消前發起 revision。

### Given

* 存在一筆 `REREVIEW` 階段 submission
* submission 狀態為 `SUBMITTED`
* 初審已固定，複審結果未固定
* 案件未 `CLOSED`
* 案件未 `CANCELLED`
* actor = 合法原複審提交者

### When

* actor 發起複審 revise

### Then

* 建立新的複審 submission
* 舊 submission 變為 `SUPERSEDED`
* 案件主狀態不自動回退
* 產生 `ReviewSubmissionRevised`

### Mapping

```yaml
mapping:
  domain:
    - rereview revision actor guard
  application:
    - ReviseRereviewUseCase
  api:
    - POST /review-cases/{caseId}/rereview/submissions/{submissionId}/revise
  tests:
    - TS-RV-003
```

---

## EX-007 結案後不可 revision

### Rule

案件一旦進入 `CLOSED`，任何 stage 的 revision 都不允許。

### Why it matters

這條規則固定 terminal state 的真正含義，避免 revision 被誤當作 reopen。

### Given

* 案件狀態為 `CLOSED`
* 存在一筆已提交 submission
* actor 為原 reviewer 或複審醫師

### When

* actor 嘗試發起 revise

### Then

* 系統拒絕
* 不建立新 revision
* 舊 submission 不變
* 不產生 `ReviewSubmissionRevised`

### Notes

* 本例不處理 administrative correction workflow
* 若未來有例外流程，需另開新 decision 與新 example

### Mapping

```yaml
mapping:
  domain:
    - CLOSED terminal state
    - revision guard
  application:
    - ReviseInitialReviewUseCase
    - ReviseRereviewUseCase
  api:
    - POST /review-cases/{caseId}/initial-review/submissions/{submissionId}/revise
    - POST /review-cases/{caseId}/rereview/submissions/{submissionId}/revise
  tests:
    - TS-RV-005
```

---

## EX-008 已取消案件不可 revision

### Rule

案件進入 `CANCELLED` 後，不得再產生 revision。

### Given

* 案件狀態為 `CANCELLED`
* 存在已提交 submission

### When

* actor 嘗試發起 revise

### Then

* 請求被拒絕
* 不建立新 revision
* 不產生 `ReviewSubmissionRevised`

### Mapping

```yaml
mapping:
  domain:
    - CANCELLED terminal state
  application:
    - ReviseInitialReviewUseCase
    - ReviseRereviewUseCase
  api:
    - revise APIs
  tests:
    - TS-RV-006
```

---

## 6. Sampling Examples

---

## EX-009 執行抽選時，系統建立 batch、snapshot 與案件

### Rule

正式抽選不是單純查詢，而是建立正式批次、保存 snapshot，並產生待審案件。

### Why it matters

這條規則固定了抽選的交易語意，避免被做成一次性查詢而無法追溯。

### Given

* actor = `HOSPITAL_MANAGER`
* 專案為 active
* 專案有 published version
* 病歷母體可正常取得

### When

* manager 執行正式抽選

### Then

* 建立 `SamplingBatch`
* 建立 `SamplingBatchSnapshot`
* 建立 `selected case refs`
* 建立對應 `ReviewCase`
* 產生：

  * `SamplingBatchStarted`
  * `ReviewCasesSelected`
  * `SamplingBatchCompleted`

### Mapping

```yaml
mapping:
  domain:
    - SamplingBatch
    - RuleSnapshot
  application:
    - ExecuteSamplingUseCase
  api:
    - POST /review-projects/{projectId}/sampling/execute
  tests:
    - TS-SM-002
```

---

## EX-010 同批次不得重複抽中同一來源案件

### Rule

在同一個 `SamplingBatch` 中，同一 `sourceCaseKey` 不得被重複抽中。

### Why it matters

這條規則是目前對避免重複抽選的正式收斂邊界：只限制同批次，不擴張到跨批次。

### Given

* 某次抽選執行中
* 抽樣邏輯嘗試兩次選中相同 `sourceCaseKey`

### When

* 系統寫入 selected case refs

### Then

* 系統拒絕重複資料
* 該 batch 不接受 duplicate selected case
* 若暴露到 API，應回衝突錯誤

### Notes

* 本例只限制同 batch
* 不處理跨 batch duplicate

### Mapping

```yaml
mapping:
  domain:
    - SamplingBatch invariant
  application:
    - ExecuteSamplingUseCase
  api:
    - sampling execute
  persistence:
    - unique(batch_id, source_case_key)
  tests:
    - TS-SM-004
```

---

## EX-011 不同批次可以再次抽中相同來源案件

### Rule

重複抽選限制不跨批次。

### Given

* 某來源案件曾在 batch A 被抽中
* batch B 為新的合法抽選批次

### When

* batch B 抽中相同 `sourceCaseKey`

### Then

* 系統允許建立 batch B 的 selected case ref
* 不因歷史 batch A 阻擋

### Mapping

```yaml
mapping:
  domain:
    - same-batch-only duplicate rule
  application:
    - ExecuteSamplingUseCase
  tests:
    - TS-SM-005
```

---

## EX-012 failed batch retry 會建立新 batch，而非覆寫舊 batch

### Rule

抽選失敗後的 retry 必須建立新批次，並保留與原批次的關聯。

### Given

* 存在一個 `FAILED` 狀態的 batch

### When

* manager 執行 retry

### Then

* 建立新的 batch
* 新 batch 的 `retryOfBatchId = oldBatchId`
* 舊 batch 保留原狀
* 不發生把 failed batch 改成 completed

### Mapping

```yaml
mapping:
  domain:
    - SamplingBatch retry relation
  application:
    - RetrySamplingBatchUseCase
  api:
    - POST /sampling-batches/{batchId}/retry
  tests:
    - TS-SM-006
```

---

## 7. Visibility / Scope Examples

---

## EX-013 院方管理者可查看本院完整作答明細

### Rule

`HOSPITAL_MANAGER` 可查看本院案件的完整 answers、comments 與附件資訊。

### Given

* actor = `HOSPITAL_MANAGER`
* 查詢的案件屬於 actor 所屬院區

### When

* actor 查詢 case detail 或 submission detail

### Then

* 系統回 full detail view
* 可見：

  * answers
  * comments
  * attachments metadata
* 不應被降級成 metadata only

### Mapping

```yaml
mapping:
  permission:
    - hospital manager full detail
  api:
    - GET /review-cases/{caseId}
    - GET /review-submissions/{submissionId}
  tests:
    - TS-PM-001
```

---

## EX-014 系統管理員只能看到 metadata

### Rule

`SYSTEM_ADMIN` 僅能查詢 metadata，不得查看完整作答內容。

### Given

* actor = `SYSTEM_ADMIN`
* 查詢一筆存在的 case / submission

### When

* actor 查詢 detail API

### Then

* 系統回 metadata view
* 不含：

  * answers
  * full comments
  * attachment content
* 只含 id、status、timestamps、assigned actors 等摘要欄位

### Mapping

```yaml
mapping:
  permission:
    - system admin metadata only
  api:
    - GET /review-cases/{caseId}
    - GET /review-submissions/{submissionId}
  tests:
    - TS-PM-003
```

---

## EX-015 稽核角色只能查授權院區

### Rule

`AUDITOR` 不具預設跨院全域查詢權，只能查授權院區或授權範圍。

### Given

* actor = `AUDITOR`
* actor 有院區 A 授權
* actor 無院區 B 授權

### When

* actor 分別查詢 A 與 B 的案件資料

### Then

* A 可查
* B 被拒絕或查無資料
* 系統不得默認 auditor 具 global scope

### Mapping

```yaml
mapping:
  permission:
    - auditor authorized_hospitals_only
  api:
    - list/detail queries
  tests:
    - TS-PM-004
```

---

## 8. Traceability Examples

---

## EX-016 案件必須可追溯其 project、batch、version 與 snapshot

### Rule

每一筆審查案件都必須可追溯其來源審查專案、抽選批次、規則版本與評分版本。

### Given

* 已存在一筆由抽選建立的案件

### When

* 系統查詢該案件的追溯資訊

### Then

* 可追溯：

  * projectId
  * batchId
  * projectVersionId
  * scoreSheetVersionId
  * snapshot checksum / snapshot ref

### Why it matters

若這條規則不成立，版本不可污染與 audit 要求都無法落地。

### Mapping

```yaml
mapping:
  domain:
    - RuleSnapshotRef
    - ScoreSheetVersionRef
  persistence:
    - review_case
    - review_case_snapshot_ref
  tests:
    - TS-TR-001
```

---

## EX-017 submission 必須可追溯當時使用的評分表 snapshot

### Rule

submission 的總分與答案，必須能回溯到當時實際使用的 score sheet snapshot。

### Given

* 已存在一筆 submission

### When

* 查詢 submission 的計分依據

### Then

* 可找到：

  * basedOnScoreSheetVersionId
  * score sheet snapshot checksum
  * snapshot 內容或 ref

### Mapping

```yaml
mapping:
  domain:
    - basedOnScoreSheetSnapshot
  persistence:
    - review_submission_snapshot
  tests:
    - TS-TR-002
```

---

## 9. 補強 Examples

---

## EX-018 已發布版本不可直接覆寫

### Rule

正式發布的 project version 或 score sheet version 不得以一般 update 方式直接覆寫。

### Why it matters

若已發布版本可被原地修改，則既有案件與批次的規則基礎會被污染，破壞版本治理與追溯要求。

### Given

* 已存在一個 published project version
* actor = `HOSPITAL_MANAGER`

### When

* actor 嘗試用一般 update 動作直接修改已發布版本內容

### Then

* 系統拒絕請求
* 已發布 version 不被改寫
* 若需要調整，必須建立新 draft / 新 version

### Mapping

```yaml
mapping:
  domain:
    - published version immutability
  application:
    - UpdateReviewProjectDraftUseCase
    - PublishReviewProjectVersionUseCase
  api:
    - PUT /review-projects/{projectId}
    - publish version APIs
  tests:
    - TS-PV-004
```

---

## EX-019 專案未啟用不得抽選

### Rule

只有 active project 才可執行正式抽選。

### Given

* actor = `HOSPITAL_MANAGER`
* project status != active

### When

* actor 執行 sampling execute

### Then

* 系統拒絕
* 不建立 batch
* 不建立 case

### Mapping

```yaml
mapping:
  domain:
    - ReviewProject active guard
  application:
    - ExecuteSamplingUseCase
  api:
    - POST /review-projects/{projectId}/sampling/execute
  tests:
    - TS-SM-003
```

---

## EX-020 未分派案件不可開始初審

### Rule

案件若尚未完成初審指派，不得進入初審中。

### Given

* case status = `SELECTED_PENDING_ASSIGNMENT`

### When

* actor 嘗試開始初審

### Then

* 系統拒絕
* case status 不變

### Mapping

```yaml
mapping:
  domain:
    - ReviewCase assignment guard
  application:
    - StartInitialReviewUseCase
  api:
    - POST /review-cases/{caseId}/initial-review/start
  tests:
    - TS-NG-001
```

---

## EX-021 未進入複審流程不可開始複審

### Rule

只有 `PENDING_REREVIEW` 案件才能開始複審。

### Given

* case status != `PENDING_REREVIEW`

### When

* actor 嘗試開始複審

### Then

* 系統拒絕
* case status 不變

### Mapping

```yaml
mapping:
  domain:
    - ReviewCase rereview state guard
  application:
    - StartRereviewUseCase
  api:
    - POST /review-cases/{caseId}/start-rereview
  tests:
    - TS-NG-002
```

---

## EX-022 不可使用未發布版本執行正式抽選

### Rule

正式抽選必須使用 published project version，不可直接使用 draft version。

### Given

* project 僅有 draft version

### When

* actor 執行 sampling execute

### Then

* 系統拒絕
* 不建立 batch

### Mapping

```yaml
mapping:
  domain:
    - published version only for batch execution
  application:
    - ExecuteSamplingUseCase
  api:
    - POST /review-projects/{projectId}/sampling/execute
  tests:
    - TS-NG-003
```

---

## EX-023 revise API 必須與 submission stage 一致

### Rule

`INITIAL` stage submission 只能走 initial-review revise API；`REREVIEW` stage submission 只能走 rereview revise API。

### Given

* submission stage = `REREVIEW`

### When

* actor 呼叫 initial-review revise API

### Then

* 系統拒絕
* 不建立新 revision

### Mapping

```yaml
mapping:
  application:
    - ReviseInitialReviewUseCase
    - ReviseRereviewUseCase
  api:
    - revise APIs
  tests:
    - TS-NG-004
```

---

## EX-024 系統不提供 generic update 來覆寫已提交 submission

### Rule

已提交 submission 的變更只能透過 revise command，不可透過 generic update API 覆寫。

### Given

* 已存在 submitted submission

### When

* actor 嘗試用 update 語義修改 submission

### Then

* 系統拒絕，或該 API 根本不存在
* 只允許 revise command

### Mapping

```yaml
mapping:
  domain:
    - append-only revision
  api:
    - no generic update submission API
  tests:
    - TS-NG-005
```

---

## EX-025 Dashboard / Report 查詢也必須遵守可見性與授權範圍

### Rule

dashboard 與報表查詢不因為是 aggregated query 就自動略過角色與 scope 邊界。

### Given

* actor = `SYSTEM_ADMIN` 或 `AUDITOR`

### When

* actor 查 dashboard summary 或 export report

### Then

* `SYSTEM_ADMIN` 僅能看 aggregated / metadata
* `AUDITOR` 僅能查授權院區 / 範圍

### Mapping

```yaml
mapping:
  permission:
    - system admin metadata only
    - auditor authorized scope
  api:
    - GET /reports/dashboard-summary
    - POST /reports/export
  tests:
    - TS-RP-001
    - TS-RP-002
    - TS-RP-003
```

## 9A. 高價值邊界與治理 Examples

---

## EX-026 初審分數等於品質線仍須固定及 manager 結案

### Rule

等於品質線不自動進複審，也不因提交或固定自動結案（D-031）。

### Given

初審分數=品質線=80。

### When

合法提交，確認初審固定，再由 manager 在不需複審時結案。

### Then

提交後初審完成、無升級／結案事件；固定後不強制複審；manager 結案才 CLOSED。

### Mapping

* Authority: D-031／D-032／D-033（依適用規則）；D-004／005／012／020 保留。
* Tests: TS-BD-001、TS-DA-003

## EX-027 第二次 revision 後應形成完整 superseded 鏈

### Rule

revision 採 append-only model。
當同一 submission 進行第二次 revision 時，系統應建立新的 revision 記錄，並維持完整的 superseded 鏈。

### Why it matters

如果只驗一次 revision，仍可能漏掉：

* revisionNo 遞增是否正確
* latest submission pointer 是否正確
* 中間 revision 是否正確轉為 `SUPERSEDED`
* 舊資料是否被誤覆寫

### Given

* 存在同一案件同一 stage 的三代提交鏈可能性
* revision 1 已為歷史版本
* revision 2 目前為 `SUBMITTED`
* 被修訂結果未固定
* case 狀態不是 `CLOSED`
* case 狀態不是 `CANCELLED`
* actor 為合法責任人

  * `INITIAL` 時為原審查醫師
  * `REREVIEW` 時為複審醫師

### When

* actor 對 revision 2 再次發起 revise

### Then

* 建立 revision 3
* revision 1 維持 `SUPERSEDED`
* revision 2 轉為 `SUPERSEDED`
* revision 3 狀態為 `SUBMITTED`
* case 主狀態不因 revision 自動改變
* 產生新的 `ReviewSubmissionRevised`

### Notes

* 本例固定 revision chain 的完整性
* 本例不處理 CLOSED / CANCELLED 的拒絕路徑

### Mapping

```yaml
mapping:
  domain:
    - ReviewSubmission.reviseFromSubmitted
    - append-only revision chain
  application:
    - ReviseInitialReviewUseCase
    - ReviseRereviewUseCase
  api:
    - revise APIs
  tests:
    - TS-RV-009
```

---

## EX-028 新版發布後，既有案件仍沿用原 version / snapshot

### Rule

新 project version 或 score sheet version 發布後，不得污染既有 batch、case、submission。
既有案件仍應沿用建立當時的 version 與 snapshot。

### Why it matters

這條規則是版本治理與可追溯性的核心。
若新版本發布會影響既有案件，則：

* 歷史資料不可重現
* audit 失真
* score / rule 判定基礎漂移

### Given

* project version 1 已發布
* score sheet version 1 已發布
* batch A 以 version 1 建立 case A
* case A 已存在 submission A
* 後續又發布：

  * project version 2
  * score sheet version 2

### When

1. 查詢 case A 的追溯資訊
2. 查詢 submission A 的計分依據
3. 再建立新的 batch B / case B

### Then

* case A 仍引用 version 1 / snapshot 1
* submission A 仍引用 score sheet snapshot 1
* batch B / case B 才會使用 version 2 / snapshot 2
* 新版本發布不改寫 case A / submission A 的歷史紀錄

### Notes

* 本例驗證「新版本不污染舊資料」
* 本例不處理 publish 權限本身

### Mapping

```yaml
mapping:
  domain:
    - version reference + immutable snapshot
  application:
    - PublishReviewProjectVersionUseCase
    - PublishScoreSheetVersionUseCase
    - ExecuteSamplingUseCase
  persistence:
    - review_case_snapshot_ref
    - review_submission_snapshot
  tests:
    - TS-VP-001
```

---

## EX-029 同一 detail endpoint 對不同角色回不同視圖

### Rule

同一個 detail endpoint 必須依角色與 scope 回傳不同視圖，而不是只有 allow / deny：

* `HOSPITAL_MANAGER`：本院 full detail
* `SYSTEM_ADMIN`：metadata only
* `AUDITOR`：授權範圍內 detail，未授權則拒絕

### Why it matters

如果只驗證是否能查，容易漏掉最重要的 response masking 規則。
這條規則直接約束 API response shape，而不只是權限判定。

### Given

* 存在同一筆 case / submission 資料
* 該資料屬於 hospital A
* manager 屬於 hospital A
* system admin 為平台角色
* auditor 對 hospital A 未授權，或只對其他 hospital 授權

### When

1. manager 查詢 detail API
2. system admin 查詢同一 API
3. auditor 查詢同一 API

### Then

* manager：

  * 成功
  * 回傳 full detail
  * 含 answers / comments / attachments metadata
* system admin：

  * 成功
  * 僅回 metadata view
  * 不含 answers / full comments / attachment content
* auditor：

  * 若未授權則拒絕或查無資料
  * 不得預設具有 global scope

### Notes

* 本例固定的是同 endpoint 的角色差異
* 本例不驗 reviewer / rereviewer 自己案件視角

### Mapping

```yaml
mapping:
  permission:
    - hospital manager full detail
    - system admin metadata only
    - auditor authorized scope
  api:
    - GET /review-cases/{caseId}
    - GET /review-submissions/{submissionId}
  tests:
    - TS-PM-006
```

---

## EX-030 retry 沿用原 batch snapshot，而非最新 published version

### Rule

retry 的語意是重做原失敗執行，因此必須沿用原 batch snapshot。
若要吃新規則，應重新執行新的正式抽選，而不是 retry。

### Why it matters

若 retry 會自動改用最新 version，則：

* retry 與 re-execute 語意混淆
* 舊 batch 與新 batch 難以比較
* traceability 失真
* retry 結果受規則漂移污染

### Given

* batch A 狀態為 `FAILED`
* batch A 使用 project version 1 / snapshot 1
* batch A 失敗後，又發布了 project version 2

### When

* manager 對 batch A 執行 retry

### Then

* 建立新的 batch B
* batch B 的 `retryOfBatchId = batch A`
* batch B 沿用與 batch A 相同的 snapshot 基礎
* batch B **不應** 使用 version 2 的 snapshot

---

## EX-031 Hospital scope mismatch → 403 HOSPITAL_SCOPE_MISMATCH

### Rule

所有處理 ReviewCase / SamplingBatch 的 use case，若 actor 所屬院所與資源院所不一致，
必須拒絕操作並回傳 `403 HOSPITAL_SCOPE_MISMATCH`。

### Why it matters

此為 defense-in-depth 層的 access guard。即使 actor 持有有效 JWT 且角色正確，
若不屬於案件所在院所，仍不可存取或操作。

### Given

* 存在 ReviewCase RC-001，所屬院所為 `H-01`
* actor 為 `HOSPITAL_MANAGER`，角色驗證通過
* actor 所屬院所為 `H-02`（與案件不同）

### When

* actor 嘗試對 RC-001 執行任意 command（如指派初審醫師）

### Then

* 回傳 `403`
* error code 為 `HOSPITAL_SCOPE_MISMATCH`
* 案件不受影響

### Notes

* 此規則適用所有 ReviewCase 與 SamplingBatch use case
* 驗證順序：`requireRole` → load aggregate → `requireHospitalScope`

### Mapping

```yaml
mapping:
  application:
    - AssignReviewCaseUseCase
    - StartInitialReviewUseCase
    - SubmitInitialReviewUseCase
    - SubmitRereviewUseCase
    - (所有 ReviewCase / SamplingBatch use case)
  api:
    - 任何 /review-cases/{caseId}/... command endpoint
  tests:
    - TS-GAP-01-001
```

---

## EX-032 D-033：複審提交前仍無法取得初審資訊

### Rule

同案複審醫師永久不可取得初審答案、分數、評語及可反推的衍生資訊；application/view 遮罩不改 domain 值（D-033）。

### Given

初審已固定；actor 為同案複審醫師；分別涵蓋複審未提交、已提交、revision、已固定、CLOSED 的狀況。

### When

查詢初審 detail、case/list、history/compare、audit/report/export，並檢查 submit/revision/close 回應與前端快取。

### Then

初審 answers/comment/totalScore 遮罩，保留既有 maskedFields 慣例；不含可反推初審的 finalScore/preview/diff。提交與結案不解除；其他角色依既有政策。

### Mapping

* Authority: D-031／D-032／D-033（依適用規則）；D-004／005／012／020 保留。
* Tests: TS-DA-009、TS-DA-012

## EX-033 D-033：複審提交後、revision 與結案後仍無法取得初審資訊

### Rule

同案複審醫師永久不可取得初審答案、分數、評語及可反推的衍生資訊；application/view 遮罩不改 domain 值（D-033）。

### Given

初審已固定；actor 為同案複審醫師；分別涵蓋複審未提交、已提交、revision、已固定、CLOSED 的狀況。

### When

查詢初審 detail、case/list、history/compare、audit/report/export，並檢查 submit/revision/close 回應與前端快取。

### Then

初審 answers/comment/totalScore 遮罩，保留既有 maskedFields 慣例；不含可反推初審的 finalScore/preview/diff。提交與結案不解除；其他角色依既有政策。

### Mapping

* Authority: D-031／D-032／D-033（依適用規則）；D-004／005／012／020 保留。
* Tests: TS-DA-009、TS-DA-012

* batch A 保持 `FAILED`
* retry 不覆寫原 batch

### Notes

* 本例固定 retry semantics
* 若未來要支援「用新規則重跑」，應另立新 command 與新 example

### Mapping

```yaml
mapping:
  decision:
    - D-013
  domain:
    - SamplingBatch retry semantics
  application:
    - RetrySamplingBatchUseCase
  persistence:
    - sampling_batch.retry_of_batch_id
    - sampling_batch_snapshot
  tests:
    - TS-SM-007
```

---

## 9B. 高風險一致性與查詢治理 Examples

---

## EX-031 revision history 查詢必須正確標示 latest 與 superseded

### Rule
revision history 查詢不只要列出歷史版本，還必須正確標示：
- 哪些版本為 `SUPERSEDED`
- 哪個版本是目前 latest / effective submission

### Why it matters
如果 revision history 只靠時間排序或僅列 submission 清單，而沒有明確 latest / superseded 語義，會導致：
- 使用者誤判哪一版是有效版本
- 稽核時無法穩定還原版本鏈
- 最新版 pointer 與查詢結果不一致

### Given
- 同一 case / stage 已存在 revision 1、2、3
- revision 1、2 已為 `SUPERSEDED`
- revision 3 為目前有效版本

### When
- 查詢 revision history

### Then
- revision history 應穩定輸出完整版本鏈
- revision 1、2 應標示為 `SUPERSEDED`
- revision 3 應標示為 latest / current effective submission
- latest submission pointer 與 history 查詢結果一致

### Notes
- 本例固定的是 query 語意，不是 revision 建立流程本身
- 本例不處理角色 masking 差異

### Mapping
```yaml
mapping:
  domain:
    - append-only revision chain
  application:
    - GetRevisionHistoryUseCase
  api:
    - GET /review-cases/{caseId}/revision-history
  tests:
    - TS-RV-009
    - TS-RV-010
```

---

## EX-032 project version 與 score sheet version 綁定必須一致

### Rule

正式抽選與後續案件 / 提交所使用的 score sheet version，必須來自 project version 當時正式綁定的版本，不可在執行時臨時混搭。

### Why it matters

如果 project version 與 score sheet version 在 execution 時被動態拼裝，會導致：

* 同一 project version 對應多個不一致的計分基礎
* case / submission 的追溯性失真
* published version 的治理失去意義

### Given

* project version 1 綁定 score sheet version 1
* project version 2 綁定 score sheet version 2
* 系統同時存在多個已發布版本

### When

* 以 project version 1 建立 batch / case / submission

### Then

* case 只能引用 project version 1 對應的 score sheet version 1
* submission 只能引用 score sheet snapshot 1
* 不可在 submission 時跳成 version 2
* snapshot checksum 與 project version 1 對應一致

### Notes

* 本例固定的是 version binding consistency
* 本例不處理 publish 權限

### Mapping

```yaml id="1dwde9"
mapping:
  domain:
    - project version binding
    - score sheet version reference consistency
  application:
    - ExecuteSamplingUseCase
    - SubmitInitialReviewUseCase
    - SubmitRereviewUseCase
  persistence:
    - review_project_version
    - review_case_snapshot_ref
    - review_submission_snapshot
  tests:
    - TS-VP-002
```

---

## EX-033 list API 也必須遵守 scope 與 metadata / detail 邊界

### Rule

list API 不得因為是清單查詢，就放寬 detail API 已定義的 scope 與 visibility 規則。

### Why it matters

常見錯誤是：

* detail API 有遮罩
* list API 卻直接把不該外露的欄位帶出
* 或 list API 默認跨 scope 查詢

這會造成 permission matrix 只在 detail 層有效，整體行為不一致。

### Given

* 系統存在多筆跨院區案件
* 包含不同 reviewer / rereviewer / status
* 有 manager、sysadmin、auditor、reviewer 等不同角色

### When

* 不同角色查詢 case list API

### Then

* manager：僅可見本院資料
* sysadmin：僅可見 metadata 欄位集
* auditor：僅可見授權院區資料
* reviewer：僅可見自己合法範圍資料
* list API 不得外露 detail-only 欄位

### Notes

* 本例固定 list query 的一致性
* 本例不處理單筆 detail payload

### Mapping

```yaml id="vjlwmv"
mapping:
  permission:
    - hospital manager full detail scope
    - system admin metadata only
    - auditor authorized scope
    - reviewer self scope
  api:
    - GET /review-cases
  tests:
    - TS-PM-007
```

---

## EX-034 export / report 不得繞過 detail visibility 規則

### Rule

若某角色在 detail API 看不到完整內容，則不應能透過 export / report 取得等價的敏感細節。

### Why it matters

報表與匯出常被當成旁路，若未套用相同的 visibility / scope 規則，會形成：

* sysadmin 透過 export 看到原本 detail API 看不到的資料
* auditor 匯出未授權院區的敏感內容
* detail 與 report 行為不一致

### Given

* system admin 僅能查看 metadata
* auditor 僅能查看授權院區
* manager 可查看本院完整資料
* 報表資料中可推得 detail content

### When

* 各角色執行 dashboard summary 或 export report

### Then

* manager：取得本院合法範圍報表
* sysadmin：僅取得 aggregated / metadata 級別輸出
* auditor：僅取得授權院區內容
* 未授權範圍請求被拒絕
* report / export 不得成為繞過 detail visibility 的旁路

### Notes

* 本例固定 reporting visibility consistency
* 不處理檔案格式細節

### Mapping

```yaml id="r8fgsx"
mapping:
  permission:
    - system admin metadata only
    - auditor authorized scope
    - hospital manager full detail scope
  api:
    - GET /reports/dashboard-summary
    - POST /reports/export
  tests:
    - TS-RP-004
    - TS-RP-002
    - TS-RP-003
```

---

## EX-035 submission 附件關聯應隨 revision 正確保留與隔離

### Rule

revision 是新的 submission 版本，因此附件關聯必須屬於對應版本，不可用 revision 覆寫舊版附件集合。

### Why it matters

如果附件關聯跟著 revision 被原地修改，會導致：

* 舊版 submission 的審計證據被污染
* revision history 難以準確還原
* 附件與 submission version 的對應關係失真

### Given

* 已存在一筆 submitted submission，含 attachment A
* case 未結案
* 合法責任人發起 revision
* 新 revision 帶 attachment B

### When

* 建立新 revision
* 查詢舊版與新版 submission detail

### Then

* 舊版 submission 仍只關聯 attachment A
* 新版 submission 關聯 attachment B
* 不發生舊版附件集合被覆寫
* revision history 可分辨不同版本附件

### Notes

* 本例固定附件關聯與 revision 的隔離
* 本例不處理檔案上傳技術細節

### Mapping

```yaml id="xg0tc0"
mapping:
  domain:
    - append-only revision
  application:
    - ReviseInitialReviewUseCase
    - ReviseRereviewUseCase
  persistence:
    - review_submission_attachment
  tests:
    - TS-AT-001
```

---

## EX-036 核心事件與持久化結果必須一致

### Rule

domain event 只能在業務事實成立後發布；不能出現：

* event 已產生，但對應資料未正確持久化
* 資料已變更，但應有 event 缺失

### Why it matters

若事件與持久化結果不一致，會導致：

* audit trail 失真
* outbox / downstream projection 錯誤
* 追溯鏈與實際資料不一致

### Given

* 選定一條核心流程：

  * 初審固定且無需複審時由 manager 正式結案
  * 初審進複審
  * 複審完成
  * revision 成功
  * sampling 成功

### When

* 執行該流程
* 查詢 event / audit 與持久化結果

### Then

* `InitialReviewCompleted` 存在時，submission 與 case 狀態已正確持久化
* `ReviewCaseClosed` 存在時，case 已為 `CLOSED`
* `ReviewSubmissionRevised` 存在時，新 revision 已存在、舊版已 `SUPERSEDED`
* `SamplingBatchCompleted` 存在時，batch / snapshot / selected case 已存在
* 不應存在「event 有、資料沒有」或「資料有、event 缺失」的情形

### Notes

* 本例固定 event / persistence consistency
* 不處理 message broker 與 outbox 技術細節

### Mapping

```yaml id="xtlb7r"
mapping:
  domain:
    - domain event publication semantics
  application:
    - core use cases
  persistence:
    - transaction + audit + outbox consistency
  tests:
    - TS-EV-001
```

---

## 9C. 查詢一致性與報表口徑 Examples

---

## EX-037 查詢條件切換不得突破既有 scope

### Rule
查詢 API 的 filter 條件只能在既有授權範圍內縮小或整理資料，不得成為突破 scope 的手段。

### Why it matters
如果使用者可以藉由切換 `hospitalId`、`projectId`、`reviewerId`、`status` 或複合條件來看到原本無權查看的案件，則 permission matrix 只在表面成立，實際查詢面會洩漏資料。

### Given
- 系統存在跨院區、多專案、多 reviewer 的案件資料
- actor 僅有部分院區或部分案件範圍權限
- actor 可使用 list API 的查詢條件

### When
- actor 以不同 `hospitalId`、`projectId`、`reviewerId`、`status` 組合反覆查詢 list API

### Then
- 查詢結果永遠不超出 actor 的合法 scope
- 未授權範圍資料要嘛被過濾掉，要嘛直接拒絕
- 不因排序、分頁或複合條件而洩漏未授權案件

### Notes
- 本例固定的是 query scope consistency
- 本例不處理單筆 detail payload

### Mapping
```yaml
mapping:
  permission:
    - scope-bound query behavior
  api:
    - GET /review-cases
  tests:
    - TS-QR-001
    - TS-PM-007
```

---

## EX-038 結案案件的查詢結果不得暗示仍可操作

### Rule

當案件已進入 `CLOSED` 狀態時，查詢結果不得暗示該案件仍可 revision、reopen 或重新提交。

### Why it matters

terminal state 若只在 command 層生效，而 query 層仍顯示「可操作」，會造成：

* 使用者誤判系統仍允許後續動作
* 前端與後端規則不一致
* terminal state 規則在產品層面不完整

### Given

* 存在一筆 `CLOSED` 案件
* 該案件至少有一筆已提交 submission
* actor 具有合法查詢權限

### When

* actor 查詢：

  * case detail
  * lifecycle
  * revision history
  * action metadata 或可操作提示欄位（若有）

### Then

* 查詢結果明確呈現案件已結案
* revision history 可查，但不得暗示仍可 revise
* lifecycle 應顯示終止狀態
* 若 API 提供 action hints / flags，應標示不可操作

### Notes

* 本例固定的是 query semantics，而不是 revise command 拒絕本身
* revise command 的拒絕已由 revision 類 example 處理

### Mapping

```yaml id="ecqqes"
mapping:
  domain:
    - CLOSED terminal state
  api:
    - GET /review-cases/{caseId}
    - GET /review-cases/{caseId}/lifecycle
    - GET /review-cases/{caseId}/revision-history
  tests:
    - TS-QR-002
```

---

## EX-039 case lifecycle 查詢必須呈現完整關鍵路徑

### Rule

case lifecycle 查詢不應只顯示最終狀態，而應能呈現關鍵業務路徑，例如：

* 指派
* 退回
* 重新待初審
* 初審完成
* 升級複審
* 複審完成
* 結案

### Why it matters

若 lifecycle 只顯示最後狀態，則：

* 稽核無法理解案件如何走到目前狀態
* 管理者無法重建處理路徑
* 追溯能力名存實亡

### Given

* 存在一筆經過多個關鍵節點的案件
* 至少經歷：

  * assign
  * return
  * reopen to pending initial review
  * initial review completed
  * escalated to rereview
  * rereview completed
  * closed

### When

* actor 查詢 case lifecycle

### Then

* lifecycle 應呈現完整關鍵節點
* 不得只回最終 `CLOSED`
* 各節點應具可辨識的業務語意，而不是只有低階技術事件流水

### Notes

* 本例固定的是 lifecycle query 的業務完整性
* 本例不要求 UI timeline 呈現樣式

### Mapping

```yaml id="3brfh0"
mapping:
  api:
    - GET /review-cases/{caseId}/lifecycle
  tests:
    - TS-TR-005
```

---

## EX-040 dashboard / export 的統計口徑必須與交易資料一致

### Rule

dashboard summary 與 export report 的統計結果，必須與實際交易資料一致；不能因 read model、projection、masking 或 scope 處理而改變統計口徑。

### Why it matters

如果報表與交易資料不一致，系統即使 detail 與 lifecycle 正確，也會在管理決策與稽核上失真。
尤其審查完成率、複審率、評分分布等統計，是需求中的正式能力，而不是附屬資訊。

### Given

* 已有可人工驗證的小樣本資料集
* 含：

  * direct close cases
  * rereview cases
  * overdue / completed cases
* dashboard 與 export 共享同一組查詢條件

### When

1. 查 dashboard summary
2. 匯出統計報表
3. 與交易資料人工比對

### Then

* completed count 與交易資料一致
* rereview count 與交易資料一致
* completion rate 與 rereview rate 一致
* score distribution 與交易資料一致
* 同一組 filter 條件下，dashboard 與 export 的統計口徑一致

### Notes

* 本例固定的是統計口徑一致性
* 本例不處理圖表樣式與報表排版格式

### Mapping

```yaml id="k1h4qm"
mapping:
  api:
    - GET /reports/dashboard-summary
    - POST /reports/export
  tests:
    - TS-RP-005
    - TS-RP-004
```

---

## 10. Decision Table 附錄

---

## 10.1 初審提交後的案件流向

| 事實／操作 | 條件 | 結果 |
|---|---|---|
| 合法初審提交 | 任意 threshold，未固定 | INITIAL_REVIEW_COMPLETED；不結案／進複審 |
| 初審固定後自動評估 | score > threshold | PENDING_REREVIEW |
| 初審固定後評估 | score ≤ threshold 或 null | 不強制複審；manager 可手工派複審或在不需複審時結案 |
| manager 手工派複審／不需複審時結案 | 初審未固定 | rejected |
| 提交／revision | CLOSED／CANCELLED | rejected |

固定操作授權依 DD-03，僅目前有效初審結果的合法提交醫師；表示 UNKNOWN，不由本表補造。

## 10.2 Revision 可否成立

| Case Status                  | Submission Stage | Actor                 | Expected |
| ---------------------------- | ---------------- | --------------------- | -------- |
| not CLOSED and not CANCELLED，初審未固定 | INITIAL | original submitter | allowed |
| not CLOSED and not CANCELLED | INITIAL          | non-original reviewer | rejected |
| not CLOSED and not CANCELLED，複審未固定 | REREVIEW | original submitter | allowed |
| not CLOSED and not CANCELLED | REREVIEW         | non-rereviewer        | rejected |
| 結果已固定                   | 對應 stage | 任意 | rejected |
| CLOSED                       | 任意               | 任意                    | rejected |
| CANCELLED                    | 任意               | 任意                    | rejected |

---

## 11. 與測試文件的關係

本文件與 `test-scenarios.md` 的差異：

* `specification-by-example.md`

  * 固定共同理解
  * 聚焦規則與例子
  * 先描述業務行為

* `test-scenarios.md`

  * 轉成測試情境
  * 更偏驗證執行
  * 更偏測試覆蓋與測試層級分工

---

## 12. 後續使用方式

本文件建議用來驅動：

1. acceptance criteria refinement
2. domain test naming
3. application service integration tests
4. API acceptance tests
5. regression suite 選例

## EX-041 初審未固定不得進入複審

### Rule

D-031。

### Given

初審合法提交且分數 > threshold，尚未固定。

### When

嘗試自動進複審、manager 手工派複審及開始複審。

### Then

均拒絕／不推進，不發布升級或開始事件；確認固定事實後自動依 > 分支進 PENDING_REREVIEW。固定由目前有效初審結果的合法提交醫師執行（DD-03）。

### Mapping

* Test: TS-DA-001
* Domain / application / API 契約層級依 test-scenarios 對映；未定 binding 不造新 endpoint。

## EX-042 初審固定前後 revision 邊界

### Rule

D-031、D-004、D-005。

### Given

初審 SUBMITTED、未結案；原提交者。

### When

固定前 revision；確認初審固定後再次 revision。

### Then

固定前建立新版本、舊版 SUPERSEDED、Case 主狀態不變；固定後拒絕且無新版本／成功事件。非原提交者固定前亦拒絕。

### Mapping

* Test: TS-DA-002
* Domain / application / API 契約層級依 test-scenarios 對映；未定 binding 不造新 endpoint。

## EX-043 無需複審案件的固定及結案 guard

### Rule

D-031。

### Given

初審分數分別 <、= threshold 或 threshold null；manager 同院。

### When

提交後未固定先嘗試結案，再確認固定且不需複審，由 manager 結案。

### Then

未固定拒絕；固定不自動結案；manager 正式結案才 CLOSED。非 manager 不可代替；固定後可保留既有 manager 手工派複審選項。

### Mapping

* Test: TS-DA-003
* Domain / application / API 契約層級依 test-scenarios 對映；未定 binding 不造新 endpoint。

## EX-044 複審提交不自動結案

### Rule

D-032。

### Given

初審已固定；合法 assigned rereviewer 與 draft。

### When

正式提交複審。

### Then

SUBMITTED，未固定、未 CLOSED；具資格但未結案；只有提交／完成事實，無 ReviewCaseClosed。提交後案件維持 `REREVIEW_IN_PROGRESS`、最新複審提交為 `SUBMITTED`（D-053）。

### Mapping

* Test: TS-DA-004
* Domain / application / API 契約層級依 test-scenarios 對映；未定 binding 不造新 endpoint。

## EX-045 不具結案資格不得正式結案

### Rule

D-032。

### Given

尚無合法複審提交，或仍有既有阻擋條件（例如 CANCELLED）。

### When

原複審醫師嘗試正式結案。

### Then

拒絕，案件及固定事實不被改為成功結案，無 ReviewCaseClosed。不新增分數門檻或額外審批。

### Mapping

* Test: TS-DA-005
* Domain / application / API 契約層級依 test-scenarios 對映；未定 binding 不造新 endpoint。

## EX-046 僅原複審提交者可正式結案

### Rule

D-032／034、D-023、D-024。

### Given

合法有效複審提交且無其他既有阻擋條件。

### When

以獨立 fixture 讓非有效結果提交者、僅 manager、僅 assigned rereviewer、其他 rereviewer、僅歷史版本提交者及跨院 actor 嘗試 Close Command；再由目前有效結果的合法原提交者另行操作（D-034）。

### Then

不合法 actor／scope 均拒絕且無固定／結案寫入或成功事件；合法原提交者可自行決定時間，另行 Close Command 才轉 CLOSED 且同時固定複審結果。manager 不作複審簽核；submit 本身不結案（D-034）。

### Mapping

* Test: TS-DA-006
* Domain / application / API 契約層級依 test-scenarios 對映；未定 binding 不造新 endpoint。

## EX-047 複審提交後結案前可修訂

### Rule

D-032、D-004、D-005。

### Given

初審固定，複審已合法提交但未固定、未 CLOSED／CANCELLED。

### When

原提交者 revision，再正式結案；另測非原提交者 revision。

### Then

合法 revision append-only、更新有效版本及加權分數，Case 主狀態不變；非原提交者拒絕。結案使用目前有效結果且同時固定。

### Mapping

* Test: TS-DA-007
* Domain / application / API 契約層級依 test-scenarios 對映；未定 binding 不造新 endpoint。

## EX-048 CLOSED 禁止所有 revision 與 reopen

### Rule

D-032、D-006。

### Given

案件 CLOSED；另有 CANCELLED 對照；原初審／複審提交者。

### When

嘗試兩階段 revision、新提交及 CLOSED reopen。

### Then

全部拒絕；狀態及歷史不變，無 ReviewSubmissionRevised 或其他成功事件；UI 無例外修正入口。

### Mapping

* Test: TS-DA-008
* Domain / application / API 契約層級依 test-scenarios 對映；未定 binding 不造新 endpoint。

## EX-049 全生命週期永久盲審與反推防護

### Rule

D-033。

### Given

同案複審醫師，分別處於未提交、已提交、revision、已固定及 CLOSED。

### When

檢查 list/detail/history/compare、audit/report/export、submit/revision/close ack、cache 與入口切換。

### Then

任何出口均不可取得初審答案／分數／評語，亦無 finalScore／preview／diff 等可反推值；應用層 response 遮罩，非僅 DOM 隱藏。已知複審分數與權重仍不能解出初審分數。

### Mapping

* Test: TS-DA-009
* Domain / application / API 契約層級依 test-scenarios 對映；未定 binding 不造新 endpoint。

## EX-050 加權公式與版本不變性

### Rule

D-012、D-020、D-032、D-033。

### Given

固定初審80、複審92，權重各0.5；另測合法0.4／0.6。

### When

提交、將複審 revision 為90、正式結案；嘗試修改已發布權重。

### Then

系統內分數依序86、85；0.4／0.6初次87.2；結案不改公式或初審結果。非法權重和／非正權重拒絕，發布後不可改；複審醫師不可讀加權值，manager 本院依原政策可讀。

### Mapping

* Test: TS-DA-010
* Domain / application / API 契約層級依 test-scenarios 對映；未定 binding 不造新 endpoint。

## EX-051 事件、固定與狀態一致性

### Rule

D-031、D-032。

### Given

合法初審／複審流程及各拒絕流程。

### When

逐步提交初審、確認固定、提交複審、revision、正式結案，對照持久化事實與事件。

### Then

提交初審不發升級／結案事件；固定後需複審才依規則升級。複審提交不發結案事件；revision 新舊版本一致；結案時已 CLOSED 且結果固定才 ReviewCaseClosed。拒絕／rollback 不留下成功事實或成功事件；固定事件技術名稱 unknown。

### Mapping

* Test: TS-DA-011
* Domain / application / API 契約層級依 test-scenarios 對映；未定 binding 不造新 endpoint。

## EX-052 其他角色資料政策不變

### Rule

D-008、D-009、D-010、D-033。

### Given

本院 manager、原 reviewer、授權 auditor、system admin 與同案 rereviewer。

### When

同一初審資料與衍生結果透過合法 query/export 路徑存取，另測跨院／未授權範圍。

### Then

其他角色依既有政策：manager 本院完整、reviewer 合法範圍、auditor 授權院區、sysadmin metadata-only；非法 scope 拒絕。同案 rereviewer 所有階段仍遮罩。

### Mapping

* Test: TS-DA-012
* Domain / application / API 契約層級依 test-scenarios 對映；未定 binding 不造新 endpoint。

## EX-053 固定成功與有效提交者授權

### Rule
DD-03 決策 1、2、5；D-031。

### Given
同院 REVIEWER A 合法提交有效初審 S2，S1 已 SUPERSEDED；案件未固定、未結案、未取消。

### When
A 固定 S2；各自使用獨立 fixture，讓僅有 manager 身份的 M、僅被指派但未提交的 B、僅有歷史提交責任的 C、跨院 actor 嘗試固定；另對無提交、DRAFT、SUPERSEDED 結果嘗試固定。

### Then
A 成功固定 S2，保存 A、固定時間及 S2 關聯，答案／分數／評語不變。其餘請求拒絕，不改有效結果、不留下成功固定／流向／事件。固定後 A 直接改結果或 revision 亦拒絕。若合法資料不允許不同版本有不同提交者，不製造成功 revision 繞過 guard；以授權驗證 fixture 驗證歷史身份不足。

### Mapping
TS-DD03-001；Domain / application / API / persistence 契約驗證。binding UNKNOWN，不創造 endpoint。


## EX-054 固定與 revision／流向的併發

### Rule
DD-03 決策 2、3、5；D-004／005／031。

### Given
A 為有效 S1 合法提交醫師；未固定、未結案、未取消。分別準備 > threshold 與 ≤ threshold／null 案件。

### When
交錯執行固定 S1 與合法 revision；分別控制 revision 先完成、固定先完成；並交錯進複審／manager 結案請求。

### Then
revision 先完成時 S2 有效、S1 SUPERSEDED，舊版固定不可成功；後續必須重新驗證有效結果及 actor 才可固定 S2。固定先完成則 revision 拒絕且無新版本。固定前進複審／結案拒絕；固定後 > 分支按既有規則進複審；≤／null 不自動結案，manager 符合不需複審條件才可結案。不可固定舊版卻使用新版分數，不可同一結果固定與 revision 同時成功。

### Mapping
TS-DD03-002；Domain / application / API / persistence 契約驗證。binding UNKNOWN，不創造 endpoint。


## EX-055 固定追溯、失敗與重試邊界

### Rule
DD-03 決策 5；D-031／033。

### Given
A 對合法有效 S2 固定；可於後續保存或流向處理注入失敗。

### When
分別驗證成功、保存追溯／流向失敗回復、成功後重新查詢，以及回應遺失後重試。

### Then
成功後可還原固定時的 A、時間、S2 與 case，歷史不可被之後 assignment 或 latest 指標替代；失敗不留下部分成功固定／流向／成功事件。重試不得改寫固定內容或重複推進流向；精確重試回應及冪等策略 UNKNOWN，相關斷言待定案，不宣稱此情境可完整自動執行。查詢及 ack 遵守 D-033。

### Mapping
TS-DD03-003；Domain / application / API / persistence 契約驗證。binding UNKNOWN，不創造 endpoint。


## EX-056 提交、複審與結案正反向回歸

### Rule
D-031／032／033；DD-03 決策 2～4。

### Given
各自獨立的合法 INITIAL／REREVIEW draft；正確 actor／院所／scope，複審 fixture 的初審已固定。

### When
合法醫師提交；非所屬提交者、跨院、非法狀態、CLOSED／CANCELLED 各嘗試提交。再驗證無需複審的 manager 結案及複審原提交者結案；以未固定初審、無合法複審提交、錯誤 actor 與 terminal case 各嘗試對應操作。

### Then
合法初審提交成為有效 SUBMITTED，不固定／不進複審／不結案；合法複審提交不固定／不結案。非法提交拒絕且無新有效結果／成功事件。無需複審時僅 manager 可於初審固定後結案；有複審時僅合法有效複審結果原提交者具資格才可正式結案並同時固定。其餘對應結案請求拒絕，不 CLOSED。複審固定前合法 revision 成功、固定或 CLOSED 後拒絕，沿用 EX-045～047，不改計分與盲審。

### Mapping
TS-DD03-004；Domain / application / API / persistence 契約驗證。binding UNKNOWN，不創造 endpoint。


## EX-057 過期或非法複審 submission 不得結案

### Rule
D-034；D-031／032／033、DD-03 保留。

### Given
初審已固定；合法複審醫師 A 原提交 S1，合法 revision 後 S1 為 SUPERSEDED、S2 為目前有效 SUBMITTED。另準備不存在、DRAFT、跨案及 INITIAL stage 目標。

### When
A 分別以 S1 與各非法目標另行執行 Close Command。

### Then
執行時驗證有效版本及 case／stage；各非法目標均拒絕，不把 S1 請求自動換成 S2。
案件不 CLOSED，S2 及既有固定初審不變；不新增複審固定或成功結案事件。

### Mapping
TS-DC-001；Domain / Application / API / Persistence。binding、回應與錯誤代碼見 D-053；版本／鎖定／交易技術 UNKNOWN（D-045）。


## EX-058 terminal 不得產生新的結案轉移

### Rule
D-034；D-031／032／033、DD-03 保留。

### Given
各自獨立的 CLOSED 與 CANCELLED 案件，以及可識別的既有複審結果。

### When
合法原複審提交者、manager 及其他醫師分別嘗試新的 Close Command、revision、新提交；對 CLOSED 另嘗試 reopen。

### Then
CLOSED／CANCELLED 不得產生新的結案轉移、revision 或新提交；CLOSED 不得 reopen。
既有分數、有效引用、固定事實、terminal 與時間不被覆寫，不產生新的成功事件。已成功 close 後再次結案回 `CASE_TERMINAL`，不視為成功（D-053 第 6 點）。

### Mapping
TS-DC-002；Domain / Application / API / Persistence。binding、回應與錯誤代碼見 D-053；版本／鎖定／交易技術 UNKNOWN（D-045）。


## EX-059 Close Command 與複審 revision 競爭

### Rule
D-034；D-031／032／033、DD-03 保留。

### Given
初審已固定；S1 是 A 的目前有效合法 SUBMITTED 複審，未固定、未 terminal，無其他既有阻擋條件。

### When
分別控制兩種順序：revision S1→S2 先完成後執行指向 S1 的 close；close S1 先完成後執行 revision。

保留上述兩個循序情境，另以各自獨立的相同初始 fixture 驗證以下真正重疊的情境。Close 與 Revision 均由合法 A 發起，明確以同一 S1 為目標，且雙方均已讀取 S1 為目前有效、未固定、非 terminal 的結果並開始處理；在任一方完成前，另一方也已開始，不能以第一方完成後才啟動第二方替代。

1. 重疊情境 R：先發出 Close 請求、再發出 Revision 請求，使兩者處理重疊且均達上述共同起點；之後控制 Revision S1→S2 先成功提交並完成，Close 才嘗試完成其成功提交。
2. 重疊情境 C：先發出 Revision 請求、再發出 Close 請求，使兩者處理重疊且均達上述共同起點；之後控制 Close S1 先成功提交並完成，Revision 才嘗試完成其成功提交。

此處「成功提交並完成」指該操作的業務變更已一致持久化，不是醫師 submit 複審結果，也不是請求送達或回應返回順序。須分別觀察請求發出順序與成功持久化順序；勝負依後者及執行時有效性判斷，不以先發請求或共同起點的舊讀取授權落敗方成功。不指定鎖定、隔離、交易實作或協調技術。


### Then
revision 先完成則舊版 close 拒絕、S2 保持有效且案件未 CLOSED；close 先完成則 S1 固定並 CLOSED、revision 拒絕，不產生 S2。
不得固定過期結果或同時讓同一結果的 close 與 revision 成功；有效引用、計分、固定及成功事件與勝出結果一致。

重疊情境 R：Revision 勝出，S1 為 SUPERSEDED 且指向 S2，S2 是唯一有效 SUBMITTED 複審；Close 雖已讀過有效 S1，完成時仍須拒絕過期目標，不得改結 S2。案件有效引用為 S2，系統內分數依原固定初審、S2 分數及既有發布版本權重重算；S1／S2 均未因本次競爭被固定，案件維持原非 terminal 主狀態、未 CLOSED，無新結案時間。僅 Revision 成功事件可留下，不得留下 Close 成功、ReviewCaseClosed 或成功固定事件（若有對應事件）。

重疊情境 C：Close 勝出，S1 保持唯一有效 SUBMITTED 複審並固定，案件有效及結案引用均為 S1；系統內分數依原固定初審、S1 分數及既有發布版本權重計算，案件 CLOSED 且結案時間與固定事實一致保存。Revision 即使已讀過未固定 S1 或算出 S2 分數，也必須拒絕；不得持久留下 S2、把 S1 標為 SUPERSEDED、更換有效引用或寫入 S2 分數。僅 Close 成功事件（含 ReviewCaseClosed，以及若有對應的成功固定事件）可留下，不得留下 Revision 成功事件。

兩種情境均須在雙方結束後重新查詢持久化結果與成功事件，確認沒有落敗方的部分寫入、事件或延遲覆寫；既有固定初審及其分數不變。不得同時成功，不以 response shaping 改寫 domain 分數；對複審醫師的回應與查詢仍遵守 D-033。


### Mapping
TS-DC-003；Application / Persistence integration / API。binding、回應與錯誤代碼見 D-053；版本／鎖定／交易技術 UNKNOWN（D-045）。


## EX-060 複審結案失敗不得部分成功

### Rule
D-034；D-031／032／033、DD-03 保留。

### Given
初審已固定；A 有目前有效合法 SUBMITTED 複審 S1，未固定、非 terminal 且具結案資格；另以獨立 fixture 驗證正常成功。

### When
A 另行 Close Command；分別在固定保存、案件／計分保存及成功事件處理注入失敗，再執行獨立正常成功對照。

### Then
失敗不得留下部分固定、CLOSED 或成功事件；成功對照須同時固定 S1 並 CLOSED。
原固定初審輸入、發布版本權重及公式不變；可於重新查詢確認一致結果。失敗注入點與交易／事件技術 UNKNOWN，待技術契約確定後轉成可執行測試，不假稱已具備故障注入能力。

### Mapping
TS-DC-004；Application / Persistence / Event integration。binding、回應與錯誤代碼見 D-053；版本／鎖定／交易技術 UNKNOWN（D-045）。


## EX-061 複審 Close Command 不解除永久盲審

### Rule
D-034；D-031／032／033、DD-03 保留。

### Given
同案複審醫師 A 已合法提交有效結果，具結案資格；含初審答案、分數、評語及可反推初審分數的加權結果。

### When
A 結案前查詢，另行 Close Command 成功後查看 ack、詳情、列表、歷程／compare、可達報表／匯出與快取；另驗拒絕 close 的回應。

### Then
各階段及成功／拒絕出口均不向 A 提供初審答案、分數、評語或可反推值；CLOSED 不解除盲審。
finalScore、preview、diff 等衍生資訊遵守 D-033；domain 原始分數及加權計算不因遮罩改寫。不以角色／入口切換繞過同案限制；多角色合成技術仍 UNKNOWN。

### Mapping
TS-DC-005；Application / API / Frontend / Acceptance。binding、回應與錯誤代碼見 D-053；版本／鎖定／交易技術 UNKNOWN（D-045）。


## EX-062 缺失歷史不得授權完整初審內容

### Rule
D-035、D-033；保留 D-024／028。

### Given
A 指派後未提交即被合法改派 B；僅 A 或全部指派 Audit 缺失；A 以同院 Manager／Auditor 查詢三條路徑，另含 CLOSED。

### When
執行既有 Submission Detail、Case Detail、Revision History 查詢，重新讀取持久化快照。

### Then
可靠排除能力不足，身份為 UNKNOWN；拒絕完整讀取，403 ACCESS_DENIED，無成功資料或靜默降級；Case／Submission／Audit 與事件不變。

### Mapping
TS-FC-001；Application Unit、HTTP／JPA integration。不存在可信負面證明 provider，不偽造 completeness marker。


## EX-063 正向及損壞證據的拒絕

### Rule
D-035、D-033；保留 D-024／028。

### Given
A 的正向歷史仍存在（含只缺 B），或 A 的證據 JSON 損壞；以完整讀者身份查詢。

### When
執行既有 Submission Detail、Case Detail、Revision History 查詢，重新讀取持久化快照。

### Then
正向身份不得讀初審；損壞不能作負面證明，同樣拒絕，無資料及成功事件變更。

### Mapping
TS-FC-002；Application Unit、HTTP／JPA integration。不存在可信負面證明 provider，不偽造 completeness marker。


## EX-064 合法完整讀者的收緊是預期行為

### Rule
D-035、D-033；保留 D-024／028。

### Given
合法同院 Manager／Auditor 或初審本人查詢；包含測試中未曾複審／不需複審案件，但無可靠負面身份證明。

### When
執行既有 Submission Detail、Case Detail、Revision History 查詢，重新讀取持久化快照。

### Then
仍為 UNKNOWN，403 ACCESS_DENIED 是 D-035 接受的結果；不把 fixture 已知過去、目前狀態或空 Audit 當作 Proven non-historical。

### Mapping
TS-FC-003；Application Unit、HTTP／JPA integration。不存在可信負面證明 provider，不偽造 completeness marker。


## EX-065 本人複審與 metadata 保留有限讀取

### Rule
D-035、D-033；保留 D-024／028。

### Given
合法 B 為目前複審醫師或合法本人提交者；或合法 SYSTEM_ADMIN 以原 scope 查詢 metadata。

### When
執行既有 Submission Detail、Case Detail、Revision History 查詢，重新讀取持久化快照。

### Then
B 在原 scope 內可讀本人複審內容，初審及衍生值持續隱藏；metadata 僅白名單，不新增完整內容授權或修改原始資料。

### Mapping
TS-FC-004；Application Unit、HTTP／JPA integration。不存在可信負面證明 provider，不偽造 completeness marker。


## EX-066 UI-010 metadata 暫移除 Latest Stage

### Rule

依 D-036，SYSTEM_ADMIN 案件詳情投影精確八欄；Lifecycle 業務需求保留，新 Milestone／Latest Stage 投影延後。

### Given

合法 scope 的 SYSTEM_ADMIN 與含 FULL-only 資料的案件；DTO 可能新增 getter。

### When

查詢 UI-010 案件詳情，包含既有 bootstrap 資料的底層 view。

### Then

JSON 僅含 `caseId`, `caseStatus`, `projectName`, `hospitalId`, `reviewerId`, `rereviewerId`, `updatedAt`, `latestRevisionSummary`。`lifecycleLatestStage` 不存在（不是 null）；無 FULL-only 或敏感 nested objects，新增 getter 不擴大白名單。狀態由 `caseStatus` 表達；保留兩欄值契約及實作 UNKNOWN。

既有 Lifecycle API 與 EX-039 不變，不以此例宣稱完整生命週期已實作。Mapping：TS-MD-001。


## EX-067 案件 updatedAt 與無變更 Draft

### Given / When

合法案件建立、Case-only 更新、相關 Submission Draft／Submit／Revision；或送出完全相同 Draft，包含操作者／scope／狀態／引用不合法的對照。

### Then

依 D-037，建案及真實變更持久化 UTC 時間，API ISO 8601；無變更先驗 Guards，禁止 C／S save、時間變動、新 Draft Event／Audit 與新儲存成功宣稱。失敗交易回復資料與時間；並行時間不倒退且時間機制不覆寫業務資料。無可信 legacy 時間保留 null；metadata 八欄、永久盲審及 Fail-closed 保留。

No-change HTTP status／code 待決；schema／migration／併發技術待決，不以自創測試替身代替真實驗證。Mapping：TS-MD-002。


## EX-068 Draft 相同 payload 不得回成功儲存

### Given

合法 Initial／Rereview Draft 經真實 API 成功保存有變更的答案與評語（Rereview 含既有 totalScore），建立可查詢的持久化基準。

### When / Then

重送完全相同 payload，回 409／NO_CHANGES，success=false，無成功 data；重新查詢 Case／Submission／Audit 與事件數量均不變，無新增 entity update／insert。相同內容若 scope／role／owner／terminal／有效引用非法，須先回原 Guard 錯誤，不能被 NO_CHANGES 取代。真實有變更仍成功保存。

附件與非完全相同資料正規化未定，不以本例推定；Mapping：TS-MD-003。
