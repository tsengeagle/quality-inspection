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

## D-001 結案後不允許 revision（修訂 - B4）

### Status

`accepted`

### Category

`domain-lifecycle`

### Decision

案件一旦進入 `CLOSED` 狀態，不允許狀態轉移或重開。然而，**複審 revision（分數修正）允許在 CLOSED 案件上進行**，因為：
1. revision 不改變案件狀態（案件仍保持 CLOSED）
2. revision 不構成 reopen
3. revision 只修正分數，不改變生命週期
4. 最終分數會被重新計算

初審 revision 在 CLOSED 案件上仍然禁止。

### Rationale

* 複審自動結案案件，修正分數的唯一時機是結案後
* 追加 revision 是 append-only，不改變案件狀態
* CLOSED 狀態應防止 reopen，但不應防止「已結案案件的分數微調」
* 此例外安全且合理

### Affected Artifacts

* `domain-model.md`
* `state-machine.md`
* `application-service.md`
* `api-spec.md`
* `persistence-model.md`
* `test-scenarios.md`

### Consequences

* `CLOSED` 後不可 reopen 或進行狀態轉移
* `CLOSED` 後不可新的初審提交或複審提交
* `CLOSED` 後**允許複審 revision**（例外規則，見 D-021）
* 初審 revision 在 CLOSED 上仍禁止

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

## D-012 複審最終分數（修訂 - B4）

### Status

`accepted`

### Category

`domain-rule`

### Decision

案件的最終分數為「初審分數」與「複審分數」的加權平均值，而非單純由複審分數決定。

默認權重配置：initialReviewWeight = 0.5, rereviewWeight = 0.5（各佔 50%）。

權重可按 ProjectVersion 配置，但保持一致性。

### Rationale

* 複審的角色不是「推翻」初審，而是「確認與調整」
* 加權平均反映初審與複審的共同決策權
* 允許業務按不同專案調整權重配置，支援差異化品質政策
* 明確定義「最終分數」計算公式，避免後續解釋衝突

### Affected Artifacts

* `domain-model.md`
* `state-machine.md`
* `application-service.md`
* `permission-matrix.md`
* `api-spec.md`
* `persistence-model.md`
* `test-scenarios.md`

### Consequences

* finalScore = initialReviewScore × initialReviewWeight + rereviewScore × rereviewWeight
* `ProjectVersion` 需存儲 `initialReviewWeight`, `rereviewWeight`
* 權重在版本發布時固定，不可事後改變
* 複審完成後，系統自動計算最終分數（不需複審醫師手工輸入）
* 複審 revision 時也需按相同公式重新計算最終分數

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

## D-014 visibilityRule 延後至 B3/B4 定義與實作

### Status
`accepted`

### Category
`scoresheet-design`

### Decision
ScoreSheetItem 的 `visibilityRule`（題目可見性規則）延後至 B3/B4 階段，待案件審查流程明確後再定義具體行為與持久化方式。B1b 階段不實作此欄位。

### Rationale
- B1b 範圍是 ScoreSheetDefinition CRUD + Query，尚無案件審查流程
- `visibilityRule` 的語意依賴審查者角色、案件狀態等 B3/B4 才定義的行為
- 過早實作會造成 schema 與行為不一致
- 延後不影響 B1b 功能完整性

### Affected Artifacts
- `persistence-model.md` — `visibility_rule` 欄位標記為 reserved/未實作
- `domain-model.md` — ScoreSheetItem 暫不包含 visibilityRule
- `api-spec.md` — ScoreSheet API response 不含 visibilityRule

### Consequences
- B1b persistence model 不建立 `visibility_rule` 欄位
- B3/B4 階段需新增 decision 定義 visibilityRule 行為後再實作
- 若業務緊急需要，須回到 decision-log 提出新 decision

---

## D-015 ScoreSheet 採 create-then-update 模式

### Status
`accepted`

### Category
`scoresheet-design`

### Decision
ScoreSheetDefinition 的建立採 create-then-update 模式：`POST /score-sheets` 只需 `{ projectId }` 即可建立空白草稿，後續透過 `PUT /score-sheets/{id}` 更新內容（6 相位 command）。

### Rationale
- 前端可先建立草稿取得 ID，再逐步填寫 sections 與 items
- 避免單次 POST 需提交完整結構（sections + items），降低前端複雜度
- 與 ReviewProject 的 create-then-update 模式一致
- 支援自動存檔 / 暫存場景

### Affected Artifacts
- `api-spec.md` — § 8.1 POST 只需 `{ projectId }`，§ 8.2 PUT 承載完整 updateDraft command
- `persistence-model.md` — 新建時僅有 header，sections/items 在 update 時寫入

### Consequences
- 新建 ScoreSheet 的 API response 只回傳 `scoreSheetId`
- 前端 UI 需先 POST 建立 → 再 PUT 更新內容
- 若未來需支援一次性 POST 完整結構，須新增 decision 或另開 endpoint

---

## D-016 Persistence model 使用 JPA surrogate key

### Status
`accepted`

### Category
`persistence-design`

### Decision
所有 JPA entity 的 primary key 使用 surrogate key（`bigint` auto-generated），business key 保留為 unique column。parent-child 關係的 FK 參照 surrogate PK。

### Rationale
- JPA `@GeneratedValue` 慣用做法，簡化 ORM mapping
- 業務鍵（如 `sectionId`, `itemId`）仍保留為 unique constraint，不影響業務查詢
- surrogate FK 比 composite business FK 更高效、更容易 index
- 原 persistence-model.md 使用 varchar PK 是設計期草案，實作時選擇 JPA 慣用方式更務實

### Affected Artifacts
- `persistence-model.md` — score_sheet_section 和 score_sheet_item 改為 `id: bigint PK (surrogate)`
- 現有 JPA entity 已使用此模式，本決策為追認

### Consequences
- 所有 child entity FK 參照 parent surrogate PK
- business key 作為 unique constraint 保留查詢與驗證能力
- API 層不暴露 surrogate key，仍以 business key 為外部識別

---

## D-017 複審醫師不可為同案的初審醫師

### Status

`accepted`

### Category

`permission-and-domain-guard`

### Decision

任何案件的複審醫師不可為該案的初審醫師。

### Rationale

* 複審的語意是「獨立審查」，而非「同人自我檢查」
* 防止初審與複審由同一醫師操作，確保品質檢查有效性
* 符合內部稽核基本原則

### Affected Artifacts

* `permission-matrix.md`
* `domain-model.md`
* `application-service.md`
* `api-spec.md`
* `test-scenarios.md`

### Consequences

* `AssignRereviewUseCase` 需驗證 assigned_doctor ≠ original_initial_reviewer
* assignment API 應在指派複審前檢查此條件
* 若違反，API 應回傳明確 error（如 `SELF_REVIEW_NOT_ALLOWED`）

---

## D-018 複審期間複審醫師看不到初審答案與分數

### Status

`accepted`

### Category

`permission-and-visibility`

### Decision

複審醫師在進行複審時，不可看到初審的原始答案、評語與分數。複審醫師應獨立評分，複審完成後才可查看初審內容作為參考。

### Rationale

* 複審的核心是「獨立評估」，若提前暴露初審結果，易造成偏見
* 保證複審的獨立性與可信度
* 符合品質管理的盲評原則

### Affected Artifacts

* `permission-matrix.md`
* `api-spec.md`
* `domain-model.md`
* `test-scenarios.md`

### Consequences

* `GetReviewCaseUseCase` 需有不同視角：
  - `initial_reviewer` 視角：可看初審內容
  - `rereview_doctor` 視角（複審中）：隱藏初審分數與答案
  - `rereview_doctor` 視角（複審後）：可看初審內容
* review case detail API 需按 actor 與 case status 動態遮蔽字段
* UI 層應不顯示初審內容直到複審提交完成

---

## D-019 自動閾值評估與自動進入複審

### Status

`accepted`

### Category

`state-machine-and-automation`

### Decision

當初審分數 > quality_threshold 時，案件自動進入 `PENDING_REREVIEW` 狀態。

當初審分數 ≤ threshold 或 threshold 為 null 時，案件進入 `INITIAL_REVIEW_COMPLETED` 狀態；院方管理者可選擇手工關閉或手工派發複審。

### Rationale

* 達不到品質閾值的案件應自動進入複審以保障品質
* threshold ≤ score 的案件不強制複審，但保留人工干預空間
* 避免複審人力浪費，同時保留靈活性

### Affected Artifacts

* `state-machine.md`
* `application-service.md`
* `domain-model.md`
* `api-spec.md`
* `test-scenarios.md`

### Consequences

* `SubmitInitialReviewUseCase` 需在 submission 成功後觸發閾值檢查
* 若 score > threshold：case state 自動轉為 `PENDING_REREVIEW`
* 若 score ≤ threshold 或 threshold == null：case state 轉為 `INITIAL_REVIEW_COMPLETED`
* 從 `INITIAL_REVIEW_COMPLETED` 有手工 command 進入 `PENDING_REREVIEW`
* `INITIAL_REVIEW_COMPLETED` 也可手工進入 `CLOSED`

---

## D-020 計分權重與不變性

### Status

`accepted`

### Category

`domain-rule`

### Decision

ProjectVersion 中存儲 `initialReviewWeight` 與 `rereviewWeight`，默認各 0.5。

約束條件：`initialReviewWeight + rereviewWeight = 1.0`，且兩者均 > 0 並 ≤ 1。

權重在 ProjectVersion 發布時固定不變，不可事後修改。

### Rationale

* 權重決定最終分數，應與版本綁定，支持版本間的可對比性
* 發布後不可修改，否則會改變同版本內已結案案件的分數回溯
* 明確約束保證計算公式的合法性

### Affected Artifacts

* `domain-model.md`
* `persistence-model.md`
* `api-spec.md`
* `test-scenarios.md`

### Consequences

* `ProjectVersion` aggregate 新增 `initialReviewWeight`, `rereviewWeight` 字段
* publish API 時應驗證權重和 = 1.0 且均 > 0
* published version 的權重欄位設為 immutable
* 若業務要改權重，必須建立新版本

---

## D-021 複審 Revision 允許在 CLOSED 案件上（D-001 例外）

### Status

`accepted`

### Category

`revision-exception`

### Decision

複審 revision（修正複審分數）允許在案件已進入 `CLOSED` 狀態後進行。這是 D-001「結案後不允許 revision」規則的例外。

### Rationale

* 複審自動結案案件，若禁止複審 revision，複審醫師發現錯誤後無法修正
* revision 不改變案件狀態（案件仍保持 CLOSED），不構成 reopen
* revision 是 append-only，新版本 submission 清晰記錄修正歷史
* CLOSED 狀態的防護是「防止狀態轉移」和「防止新提交」，不應防止「已提交結果的分數微調」
* 此例外安全且合理

### Affected Artifacts

* `state-machine.md`
* `domain-model.md`
* `application-service.md`
* `api-spec.md`
* `test-scenarios.md`

### Consequences

* `ReviseRereviewUseCase` 需允許在 case status = `CLOSED` 時觸發
* 初審 revision 在 `CLOSED` 上仍禁止（不包含在本例外內）
* revision 後重新計算 finalScore（見 D-020）
* revision submission 仍遵循 D-004 append-only 原則

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

  - D-014 visibilityRule 延後
    influences:
      - D-015 create-then-update
      - persistence-model.md
      - B3/B4 scoresheet item visibility

  - D-015 ScoreSheet create-then-update
    influences:
      - api-spec.md POST/PUT score-sheets
      - frontend create flow

  - D-016 JPA surrogate key
    influences:
      - persistence-model.md
      - all JPA entity mapping
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
