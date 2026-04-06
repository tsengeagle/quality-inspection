# Decision Log v1

## 1. 文件目的

本文件用於記錄品質審查系統在 DDD 設計過程中，已明確定稿的關鍵決策。

本文件的用途：

- 作為後續 domain model、state machine、API spec、persistence model、測試案例的共用 authority
- 避免同一議題在不同文件中被重複推翻或各自解讀
- 協助後續實作、code review、規格變更評估時快速定位既有決策

本文件不重新描述完整需求，也不取代：
- 需求書
- domain model
- state machine
- permission matrix
- API spec
- persistence model

---

## 2. 使用原則

1. 本文件只記錄「已決定」事項，不記錄尚未決定的 brainstorming。
2. 若某決策會影響多份文件，應以本文件為索引，再同步更新相關設計文件。
3. 若後續要推翻既有決策，應新增新版本 decision，而不是直接靜默覆蓋。
4. 若某項規則僅屬權限 / scope 問題，不應誤寫成 domain invariant。
5. 若某項規則已進入 decision log，後續 API / persistence / implementation 不應自行另解。

---

## 3. 決策狀態定義

```yaml
decision_status:
  - proposed
  - accepted
  - superseded
  - rejected
```

本版只收錄 `accepted` 決策。

---

## 4. 決策清單

---

## D-001 結案後不允許 revision

### Status

`accepted`

### Category

`domain-lifecycle`

### Decision

案件一旦進入 `CLOSED` 狀態，不允許再建立任何 submission revision。

### Rationale

* 結案應作為正式終止點
* 避免 revision 造成 reopen / 反覆翻案
* 簡化 state machine、API 與 persistence 邊界
* 保持 audit trail 清楚

### Affected Artifacts

* `domain-model.md`
* `state-machine.md`
* `application-service.md`
* `api-spec.md`
* `persistence-model.md`
* `test-scenarios.md`

### Consequences

* `CLOSED` 後不可 revision
* `CLOSED` 後不可 reopen
* `CLOSED` 後不可重新提交初審或複審
* 若未來需要例外修正，必須另開 administrative correction workflow，本版不納入

---

## D-002 初審 revision 僅限原審查醫師

### Status

`accepted`

### Category

`permission-and-domain-guard`

### Decision

已提交初審結果的 revision，只能由原審查醫師發起。

### Rationale

* 修正應由原責任人負責
* 避免行政角色直接改寫專業審查結果
* 使責任歸屬與 audit trail 一致

### Affected Artifacts

* `permission-matrix.md`
* `domain-model.md`
* `application-service.md`
* `api-spec.md`
* `test-scenarios.md`

### Consequences

* `ReviseInitialReviewUseCase` 需驗證 actor 是否為原審查醫師
* 非原審查醫師不得代修
* manager / sysadmin / auditor 不可代為修正初審

---

## D-003 複審 revision 僅限複審醫師

### Status

`accepted`

### Category

`permission-and-domain-guard`

### Decision

已提交複審結果的 revision，只能由複審醫師發起。

### Rationale

* 複審最終分數由複審醫師決定
* 後續修正仍應維持相同責任歸屬
* 避免最終結論被非責任人修改

### Affected Artifacts

* `permission-matrix.md`
* `domain-model.md`
* `application-service.md`
* `api-spec.md`
* `test-scenarios.md`

### Consequences

* `ReviseRereviewUseCase` 需驗證 actor 是否為複審醫師
* 非複審醫師不得代修複審結果

---

## D-004 revision 採 append-only，不覆寫舊提交

### Status

`accepted`

### Category

`revision-model`

### Decision

revision 必須建立新的 submission record，舊版保留並標記為 `SUPERSEDED`，不得直接覆寫既有提交。

### Rationale

* 需求要求已送出結果不得直接覆寫
* 需保留修正紀錄供稽核
* 有利於版本追蹤與責任釐清

### Affected Artifacts

* `domain-model.md`
* `application-service.md`
* `domain-events.md`
* `api-spec.md`
* `persistence-model.md`
* `test-scenarios.md`

### Consequences

* `review_submission` 需有 `revision_no`
* 舊提交不可被 update 成新版內容
* 新版建立後，舊版轉為 `SUPERSEDED`

---

## D-005 revision 不等於 reopen

### Status

`accepted`

### Category

`state-machine`

### Decision

revision 不構成案件 reopen，也不應直接改變案件主狀態。

### Rationale

* submission revision 與案件生命週期應分離
* 避免單純修正提交內容時意外回退案件狀態
* 降低流程複雜度

### Affected Artifacts

* `state-machine.md`
* `application-service.md`
* `api-spec.md`
* `test-scenarios.md`

### Consequences

* revision 成立時，預設不改變 `ReviewCase.case_status`
* 若未來要支援 correction workflow，需另立流程

---

## D-006 `CANCELLED` 為 terminal state

### Status

`accepted`

### Category

`state-machine`

### Decision

案件一旦進入 `CANCELLED`，不得再審、不得 revision、不得 reopen。

### Rationale

* 取消案件應終止正常審查流程
* 避免取消後又回到一般審查流程，造成語意混亂

### Affected Artifacts

* `domain-model.md`
* `state-machine.md`
* `api-spec.md`
* `test-scenarios.md`

### Consequences

* `CANCELLED` 後不可初審 / 複審 / revision
* 僅保留查詢與稽核能力

---

## D-007 重複抽選限制範圍為同批次

### Status

`accepted`

### Category

`sampling-rule`

### Decision

同一案件的重複抽選限制範圍為「同批次」，不延伸到跨批次、跨週期或同專案永久排除。

### Rationale

* 現階段先採最小必要限制
* 避免過早引入跨批次排重規則與歷史查詢成本
* 保留後續依業務需要升級規則的空間

### Affected Artifacts

* `domain-model.md`
* `api-spec.md`
* `persistence-model.md`
* `test-scenarios.md`

### Consequences

* `sampling_batch_selected_case` 需有 `(batch_id, source_case_key)` unique constraint
* 不做 cross-batch duplicate exclusion
* 不做 cycle-level duplicate exclusion

---

## D-008 院方管理者可查看本院完整作答明細

### Status

`accepted`

### Category

`permission-and-visibility`

### Decision

`HOSPITAL_MANAGER` 可查看本院案件的完整作答明細、評語與附件資訊。

### Rationale

* 院方管理者需負責追蹤、管理、稽核本院審查品質
* 若僅能看摘要，難以實質管理案件品質

### Affected Artifacts

* `permission-matrix.md`
* `api-spec.md`
* query / read model 設計
* `test-scenarios.md`

### Consequences

* manager detail API 應回 full detail view
* scope 限制為 hospital only
* 此規則屬 access policy，不寫成 domain invariant

---

## D-009 稽核角色僅限授權院區 / 授權範圍

### Status

`accepted`

### Category

`permission-and-scope`

### Decision

`AUDITOR` 不具預設跨院全域查詢權，只能查詢授權院區或授權範圍內資料。

### Rationale

* 需求要求區分跨院與院內資料可見範圍
* 稽核角色不應預設擁有全域可見性

### Affected Artifacts

* `permission-matrix.md`
* `api-spec.md`
* query scope filter
* `test-scenarios.md`

### Consequences

* 稽核查詢需帶入授權 scope 過濾
* 不得預設以 global 查詢所有院區資料

---

## D-010 系統管理員僅能查看 metadata

### Status

`accepted`

### Category

`permission-and-visibility`

### Decision

`SYSTEM_ADMIN` 僅能查看平台治理與操作 metadata，不可查看完整評分內容、評語全文與附件內容。

### Rationale

* system admin 角色屬平台治理，不屬院內業務審查角色
* 符合最小權限原則
* 避免平台治理角色接觸不必要的業務內容

### Affected Artifacts

* `permission-matrix.md`
* `api-spec.md`
* query / response masking 設計
* `test-scenarios.md`

### Consequences

* metadata view 與 full detail view 需分離
* system admin 查詢 detail API 時只能回 metadata 視角

---

## D-011 抽選與案件需保留正式 version + snapshot 追溯

### Status

`accepted`

### Category

`versioning-and-traceability`

### Decision

批次、案件、submission 均需保存正式 version reference 與 immutable snapshot 參照，以支援追溯與重現。

### Rationale

* 需求要求案件與批次可追溯規則版本與評分版本
* 僅存 version id 不足以完全重現當時判定
* 僅存 snapshot 又缺正式受控版本關聯

### Affected Artifacts

* `domain-model.md`
* `persistence-model.md`
* `api-spec.md`
* `test-scenarios.md`

### Consequences

* 採 `version reference + immutable snapshot` 雙軌
* `sampling_batch_snapshot`
* `review_case_snapshot_ref`
* `review_submission_snapshot`
  皆需存在

---

## D-012 複審最終分數由複審醫師決定

### Status

`accepted`

### Category

`domain-rule`

### Decision

案件進入複審後，最終分數由複審醫師提交結果決定。

### Rationale

* 直接對應需求規則
* 明確劃分初審與複審決策權限

### Affected Artifacts

* `domain-model.md`
* `state-machine.md`
* `application-service.md`
* `permission-matrix.md`
* `api-spec.md`
* `test-scenarios.md`

### Consequences

* 初審結果只能決定是否進入複審
* 複審完成後 final score 以複審 submission 為準

---

## D-013 retry 沿用原 batch snapshot，不使用後續新發布 version

### Status
`accepted`

### Category
`sampling-retry-semantics`

### Decision
當 `FAILED` batch 執行 retry 時，新的 retry batch 必須沿用原 batch 的 snapshot 基礎，不得改用 retry 發生當下後續新發布的 project version 或 score sheet version。

### Rationale
- retry 的語意是「重做原失敗執行」，而不是「以新規則重新跑一次抽選」
- 若 retry 自動改用新 version，會造成：
  - retry 與 re-execute 語意混淆
  - 舊 batch 與新 batch 難以比較
  - traceability 失真
  - retry 結果受版本漂移污染
- 若業務要使用新規則重跑，應走新的正式抽選，而不是 retry

### Affected Artifacts
- `specification-by-example.md`
- `test-scenarios.md`
- `domain-model.md`
- `application-service.md`
- `api-spec.md`
- `persistence-model.md`

### Consequences
- `RetrySamplingBatchUseCase` 應以原 batch snapshot 作為 retry 基礎
- retry 建立的新 batch 應保留：
  - 新 `batchId`
  - `retryOfBatchId = oldBatchId`
  - 與原 batch 相同或等價的 snapshot 參照
- retry batch **不應** 因為之後有新發布 version，而自動改吃新規則
- 若要使用最新 published version，應重新執行新的正式抽選流程，而非 retry

### Non-Goals
- 本決策不處理 batch 原始失敗原因的分類
- 本決策不定義「重新正式抽選」的 command 細節
- 本決策不代表 retry 必須重用完全相同的技術執行環境；它只固定業務規則基礎必須一致

---

## 5. 已收斂但不列為 domain invariant 的決策

下列事項雖已決定，但屬 access / view / scope，不應寫成 domain invariant：

```yaml
non_domain_invariant_decisions:
  - HOSPITAL_MANAGER 可查看本院完整作答明細
  - AUDITOR 僅限授權院區 / 授權範圍
  - SYSTEM_ADMIN 僅能查看 metadata
```

這些應由：

* permission matrix
* query scope filter
* response masking
* authorization guard

落地。

---

## 6. 決策相依關係

```yaml id="j5j5w0"
decision_dependencies:
  - D-001 結案後不允許 revision
    influences:
      - D-004 append-only revision
      - D-005 revision 不等於 reopen
      - API revise endpoints
      - state terminal rules

  - D-002 初審 revision actor
    influences:
      - permission matrix
      - ReviseInitialReviewUseCase
      - revise initial API guard

  - D-003 複審 revision actor
    influences:
      - permission matrix
      - ReviseRereviewUseCase
      - revise rereview API guard

  - D-007 同批次重複抽選限制
    influences:
      - sampling invariant
      - selected case unique key
      - batch retry behavior
```

---

## 7. 本版尚未納入的議題

以下議題目前未列入已接受決策，若後續需要，應新增 decision：

* 退回初審的最終授權角色
* 是否需要 administrative correction workflow
* attachment upload / storage 最終策略
* optimistic locking / idempotency 設計
* dashboard read model 物化策略
* integration event 命名與 payload 正式契約

---

## 8. 後續落地要求

本文件中的 accepted decision，應同步反映到：

* `permission-matrix.md`
* `domain-model.md`
* `state-machine.md`
* `application-service.md`
* `domain-events.md`
* `api-spec.md`
* `persistence-model.md`
* `test-scenarios.md`

若後續某份文件與本文件衝突，應先以本文件檢查該規則是否已被正式接受。
