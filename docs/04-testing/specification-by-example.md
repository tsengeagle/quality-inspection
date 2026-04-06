# Specification by Example v1

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

## EX-001 初審分數低於或等於品質線，案件直接結案

### Rule

當初審分數未超過品質線時，案件不進入複審，直接結案。

### Why it matters

這條規則直接決定：

* 哪些案件需要複審
* 哪些案件在初審完成後可直接結案
* state machine 中 `INITIAL_REVIEW_COMPLETED -> CLOSED` 的合法路徑

### Given

* 存在一個已抽選並完成初審指派的案件
* 案件目前狀態為 `INITIAL_REVIEW_IN_PROGRESS`
* actor 為被指派的初審醫師
* 專案品質線為 `80`
* 初審 submission 的總分為 `75`

### When

* 初審醫師送出初審結果

### Then

* submission 狀態變為 `SUBMITTED`
* 案件完成初審
* 案件不進入 `PENDING_REREVIEW`
* 案件狀態變為 `CLOSED`
* 產生：

  * `ReviewSubmissionSubmitted`
  * `InitialReviewCompleted`
  * `ReviewCaseClosed`

### Notes

* 本例驗證的是「直接結案」路徑
* 本例不處理 revision
* 本例不處理 detail visibility

### Mapping

```yaml
mapping:
  domain:
    - ReviewCase.completeInitialReview
    - CaseStatus CLOSED
    - qualityThreshold rule
  application:
    - SubmitInitialReviewUseCase
  api:
    - POST /review-submissions/{submissionId}/initial-review/submit
  tests:
    - TS-IR-006
```

---

## EX-002 初審分數高於品質線，案件必須進入複審

### Rule

當初審分數超過品質線時，案件必須進入複審。

### Why it matters

這條規則是整個複審流程的入口，如果模糊，會導致：

* 本該複審的案件沒有被升級
* 或所有案件都被錯誤地送去複審

### Given

* 案件狀態為 `INITIAL_REVIEW_IN_PROGRESS`
* actor 為被指派的初審醫師
* 品質線為 `80`
* 初審 submission 總分為 `88.5`

### When

* 初審醫師送出初審結果

### Then

* submission 狀態為 `SUBMITTED`
* 案件先完成初審
* 案件狀態轉為 `PENDING_REREVIEW`
* `rereviewRequired = true`
* 產生：

  * `ReviewSubmissionSubmitted`
  * `InitialReviewCompleted`
  * `ReviewCaseEscalatedToRereview`

### Notes

* 本例只固定「進入複審」的條件
* 不要求此時已經完成複審醫師指派

### Mapping

```yaml
mapping:
  domain:
    - ReviewCase.completeInitialReview
    - ReviewCaseEscalatedToRereview
  application:
    - SubmitInitialReviewUseCase
  api:
    - POST /review-submissions/{submissionId}/initial-review/submit
  tests:
    - TS-IR-007
```

---

## EX-003 複審完成後，最終分數以複審結果為準

### Rule

複審最終分數由複審醫師決定，案件於複審完成後結案。

### Why it matters

這條規則固定了：

* 初審分數不是最終 truth
* 一旦進入複審，final score 由複審 submission 決定

### Given

* 案件已進入複審
* 案件狀態為 `REREVIEW_IN_PROGRESS`
* actor 為已指派複審醫師
* 已存在初審提交
* 複審 submission 為合法草稿或待提交結果

### When

* 複審醫師送出複審結果，finalScore = `92`

### Then

* 複審 submission 狀態為 `SUBMITTED`
* 案件 finalScore = `92`
* 案件狀態變為 `CLOSED`
* 產生：

  * `ReviewSubmissionSubmitted`
  * `RereviewCompleted`
  * `ReviewCaseClosed`

### Notes

* 本例固定的是最終決定權，不是複審評分公式細節

### Mapping

```yaml
mapping:
  domain:
    - ReviewCase.completeRereview
    - CLOSED terminal state
  application:
    - SubmitRereviewUseCase
  api:
    - POST /review-submissions/{submissionId}/rereview/submit
  tests:
    - TS-RR-003
```

---

## 5. Revision Examples

---

## EX-004 原審查醫師可修正已提交初審結果

### Rule

已提交的初審結果，只能由原審查醫師在未結案前發起 revision。

### Why it matters

這條規則同時固定了：

* responsibility ownership
* revision actor 限制
* revision 與 overwrite 的差異

### Given

* 存在一筆 `INITIAL` 階段 submission
* submission 狀態為 `SUBMITTED`
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
    - POST /review-submissions/{submissionId}/initial-review/revise
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
    - POST /review-submissions/{submissionId}/initial-review/revise
  tests:
    - TS-RV-002
```

---

## EX-006 複審醫師可修正已提交複審結果

### Rule

已提交的複審結果，只能由複審醫師在未結案前發起 revision。

### Given

* 存在一筆 `REREVIEW` 階段 submission
* submission 狀態為 `SUBMITTED`
* 案件未 `CLOSED`
* 案件未 `CANCELLED`
* actor = 複審醫師

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
    - POST /review-submissions/{submissionId}/rereview/revise
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
    - POST /review-submissions/{id}/initial-review/revise
    - POST /review-submissions/{id}/rereview/revise
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
    - POST /review-cases/{caseId}/rereview/start
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

## EX-026 等於品質線時案件直接結案

### Rule
只有 **大於** 品質線時才進入複審；當初審分數 **等於** 品質線時，案件應直接結案。

### Why it matters
品質線邊界值最容易被實作寫錯成 `>= threshold` 進複審。  
若不單獨固定此例，主流程雖然有 `<= threshold` 與 `> threshold` 的描述，但邊界值仍可能在實作時漂移。

### Given
- 存在一個已抽選並完成初審指派的案件
- 案件狀態為 `INITIAL_REVIEW_IN_PROGRESS`
- actor 為被指派的初審醫師
- `qualityThreshold = 80`
- 初審 submission 的 `totalScore = 80`

### When
- 初審醫師送出初審結果

### Then
- submission 狀態為 `SUBMITTED`
- 案件不進入 `PENDING_REREVIEW`
- 案件狀態為 `CLOSED`
- 產生：
  - `ReviewSubmissionSubmitted`
  - `InitialReviewCompleted`
  - `ReviewCaseClosed`
- **不應** 產生 `ReviewCaseEscalatedToRereview`

### Notes
- 本例是 EX-001 / EX-002 的邊界值補強
- 本例不處理 revision

### Mapping
```yaml
mapping:
  domain:
    - ReviewCase.completeInitialReview
    - qualityThreshold boundary rule
  application:
    - SubmitInitialReviewUseCase
  api:
    - POST /review-submissions/{submissionId}/initial-review/submit
  tests:
    - TS-BD-001
````

---

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
    - GET /review-cases/{caseId}/revisions
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
    - GET /review-submissions
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

  * 初審直接結案
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
    - GET /review-submissions
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
    - GET /review-cases/{caseId}/revisions
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

| Case Status Before         | Actor                 | totalScore vs threshold | Expected Case Status After |
| -------------------------- | --------------------- | ----------------------- | -------------------------- |
| INITIAL_REVIEW_IN_PROGRESS | assigned reviewer     | `<= threshold`          | CLOSED                     |
| INITIAL_REVIEW_IN_PROGRESS | assigned reviewer     | `> threshold`           | PENDING_REREVIEW           |
| INITIAL_REVIEW_IN_PROGRESS | non-assigned reviewer | 任意                      | rejected                   |
| CLOSED                     | 任意                    | 任意                      | rejected                   |

---

## 10.2 Revision 可否成立

| Case Status                  | Submission Stage | Actor                 | Expected |
| ---------------------------- | ---------------- | --------------------- | -------- |
| not CLOSED and not CANCELLED | INITIAL          | original reviewer     | allowed  |
| not CLOSED and not CANCELLED | INITIAL          | non-original reviewer | rejected |
| not CLOSED and not CANCELLED | REREVIEW         | assigned rereviewer   | allowed  |
| not CLOSED and not CANCELLED | REREVIEW         | non-rereviewer        | rejected |
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
