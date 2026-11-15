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

本版保留 accepted 決策及其 superseded 歷史；部分取代以 Status 下的範圍說明為準。

---

## 4. 決策清單

---

## D-001 結案後不允許 revision（修訂 - B4）

### Status

`accepted`（部分取代）

> 部分被 D-032 取代：撤銷 CLOSED 複審 revision 例外；保留 CLOSED terminal、禁止新提交與 reopen。下列為歷史決策原文。

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

`superseded`

> `superseded` by D-033：撤銷複審完成後可查看初審內容的規則；改為全生命週期盲審。下列為歷史原文。

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

`accepted`（部分取代）

> 部分被 D-031 取代：閾值判斷與自動進複審延至初審固定後；手工派複審與無需複審的手工結案均要求初審固定。保留 > threshold、≤ threshold／null 分支與 manager 責任。下列為歷史原文。

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

`superseded`

> `superseded` by D-032：CLOSED 後不再有複審 revision 例外。下列為歷史原文。

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

## D-022 — API Path Model: Case-Rooted Sub-Resource Style

### Status

`accepted`

### Category

`api-design`

### Decision

所有 review workflow command endpoint 採用 case-rooted sub-resource path 風格：
`/review-cases/{caseId}/submissions/{submissionId}/{stage}/action`。
所有 command method 使用 `POST`。舊的 `/review-submissions/{submissionId}/...` paths 廢除。

### Rationale

* GAP-04 發現 api-spec.md（submission-rooted）與 implementation（case-rooted）不一致
* 實作已有 78+ tests 以 case-rooted paths 通過，零 consumer migration 成本
* case-rooted 風格使 caseId 成為必要路徑參數，強化 hospital scope 驗證
* 一致性優先

### Affected Artifacts

* `api-spec.md` — 11.3、11.4、12.3、12.4、13.1 更新為 case-rooted paths

### Consequences

* frontend 從第一天就以統一路徑開發
* `GET /review-submissions/{submissionId}` → `GET /review-cases/{caseId}/submissions/{submissionId}`
* `POST /review-submissions/{submissionId}/...` → `POST /review-cases/{caseId}/submissions/{submissionId}/...`

---

## D-023 — 角色驗證失敗使用 ROLE_DENIED error code

### Status

`accepted`

### Category

`error-handling`

### Decision

HTTP 403 因角色不符觸發時，使用獨立 error code `ROLE_DENIED`（對應 `RoleDeniedException`），
而非混用 `UNAUTHORIZED` 或 generic `FORBIDDEN`。

### Rationale

* GAP-06 發現 `UnauthorizedException` 命名容易與 HTTP 401（未認證）混淆
* `ROLE_DENIED` 語意更精確，對應「已認證但角色不允許」
* 統一 error code 有助前端精確錯誤處理

### Affected Artifacts

* `api-spec.md` — Section 4.1、5.2 更新
* `application-service.md` — 例外類別名稱

### Consequences

* `UnauthorizedException` 改名為 `RoleDeniedException`
* 所有 403 role 驗證失敗一律回傳 `ROLE_DENIED`
* `UNAUTHORIZED`（401）維持原語意（未登入 / 憑證失效）

---

## D-024 — 所有 use case 強制 hospital scope guard

### Status

`accepted`

### Category

`permission-and-domain-guard`

### Decision

所有處理 `ReviewCase` 或 `SamplingBatch` 的 use case，
在 `requireRole` 與 aggregate 載入後，必須執行 `requireHospitalScope` 驗證，
確保 actor 所屬院所與案件/批次的院所一致。
違反時回傳 `403 HOSPITAL_SCOPE_MISMATCH`。

### Rationale

* GAP-01 發現部分 use case 缺乏 hospital scope 驗證，存在跨院資料存取風險
* hospital scope guard 是 defense-in-depth 層：即使 JWT 有效，仍需確保 actor 在正確院所範圍內
* 此為 access policy，不應寫成 domain invariant

### Affected Artifacts

* `application-service.md` — 所有 ReviewCase / SamplingBatch use case 的 orchestration 描述
* `api-spec.md` — Section 4.2、5.2 新增 `HOSPITAL_SCOPE_MISMATCH`
* `specification-by-example.md` — 新增 EX-031

### Consequences

* use case orchestration 標準順序：`requireRole` → load aggregate → `requireHospitalScope` → identity guard → domain method
* 違反 hospital scope 一律回傳 `403 HOSPITAL_SCOPE_MISMATCH`

---

## D-025 — ReviewCase 新增 latestRereviewSubmissionId

### Status

`accepted`

### Category

`domain-model-extension`

### Decision

`ReviewCase` aggregate 新增 `latestRereviewSubmissionId` 欄位，
在 `GET /review-cases/{caseId}` response 中回傳，供前端直接導覽至最新複審 submission。

### Rationale

* GAP-13 發現前端需要此欄位才能導覽至複審作業頁，而目前無法從案件詳情直接取得
* 此欄位語意明確（最新複審 submission）、不改變 domain invariant
* 由 use case 在 rereview submission 建立時同步更新

### Affected Artifacts

* `domain-model.md` — ReviewCase 新增欄位
* `persistence-model.md` — review_case 新增 column
* `api-spec.md` — Section 10.2 新增欄位說明

### Consequences

* `latestRereviewSubmissionId` 在無複審時為 `null`
* 建立 rereview submission 的 use case 負責更新此欄位

---

## D-026 — D-018 遮罩在 application layer 實作（view level）

### Status

`accepted`（部分取代）

> 部分被 D-033 取代：撤銷「無 SUBMITTED 複審才遮罩」條件；保留 application/view layer 遮罩及 maskedFields 慣例。下列為歷史原文。

### Category

`permission-and-visibility`

### Decision

D-018（複審期間複審醫師看不到初審答案與分數）的遮罩邏輯，
在 application layer 的 `GetSubmissionDetailUseCase` 中根據 actor 角色與案件查詢狀態執行，
而非在 domain layer 實作。遮罩為 view-level 操作，不改變 domain state。

判斷條件：actor 為 `REREVIEWER` 且案件無 `SUBMITTED` 複審 submission 時，
將 `answers`、`comment`、`totalScore` 設為 `null`，並於 response 加入 `maskedFields`。

### Rationale

* 遮罩是 response shaping / access policy，不是 domain invariant
* 放在 application layer 可清楚區分「能不能看到資料」與「資料本身是否合法」
* domain layer 的 `ReviewSubmission` 不應因 actor 視角不同而改變其值

### Affected Artifacts

* `application-service.md` — `GetSubmissionDetailUseCase` 說明
* `api-spec.md` — Section 13.1 新增 masking 行為說明
* `specification-by-example.md` — 新增 EX-032、EX-033

### Consequences

* `maskedFields` 僅在有遮罩時出現於 response，無遮罩時不帶此欄位
* 前端必須根據 `maskedFields` 存在與否決定顯示行為

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


---

## D-027 — CLOSED 後複審修正入口策略（O1-C）

### Status

`superseded`

> `superseded` by D-032：取消 CLOSED 例外修正入口；UI-014 仍為閱讀與合法導頁頁面，不直接提交 revision。下列為歷史原文。

### Category

`frontend-workflow-entry`

### Decision

當 case status = `CLOSED` 時：

- `UI-014` 允許對「原複審醫師」顯示條件式「前往複審修正」導頁入口
- 真正的修正 command 僅在 `UI-013` 執行（例外修正模式）
- 不允許 reopen，不允許新的複審提交

### Rationale

* 對齊 D-021 的 domain 例外與 command page 責任分層
* history page 保持閱讀/導頁定位，不直接提交

### Affected Artifacts

* `ui-page-spec-ui013複審作業頁.md`
* `ui-page-spec-ui014revision歷程頁.md`
* `ui-state-actions-matrix.md`
* `frontend-test-scenarios.md`

### Consequences

* CLOSED 下「新提交」維持 blocked
* CLOSED 下「複審修正」僅限合法責任人，且為 append-only

---

## D-028 — Metadata-only 欄位白名單採 shared base + page extension（O3-C）

### Status

`accepted`

### Category

`permission-and-visibility`

### Decision

`SYSTEM_ADMIN` / metadata-only 視角採：

- shared base whitelist
- per-page extension whitelist（UI-009/UI-010/UI-014）

metadata-only 為 projection，不是獨立頁，不得取得 FULL-only 欄位。

### Rationale

* 在一致性與頁面差異需求間取得平衡
* 可直接支援前端 view-model projection 與 section visibility

### Affected Artifacts

* `permission-matrix.md`
* `api-spec.md`
* `ui-view-model-spec.md`
* `ui-api-binding-matrix.md`

### Consequences

* 白名單成為 metadata-only mapper 的唯一來源
* FULL-only 欄位不得在 metadata-only response 出現

---

## D-029 — Frontend Access Control Strategy: Guard + Directive

### Status

`accepted`

### Category

`frontend-access-control`

### Decision

Frontend 正式採用 **guard + directive** 雙層 access control 策略：

- **Route guard**（`guards/`）負責 **page-level gate**：在 Angular route resolution 階段，以 actor context（`roles`、`hospitalId`）決定頁面是否可進入。
- **Directive**（component-level）負責 **page-internal / component-level gate**：在頁面內根據 actor context 控制個別 UI 元素（按鈕、操作欄、區塊）的可見性與可用性。

兩者共用同一份 actor context 來源，不得各自維護獨立 role truth。

### Rationale

* 分層清晰：guard 不管 component 細節，directive 不管路由切換
* 避免 guard 過度膨脹或 component 自行實作隱式路由攔截
* 對應 I-001 的 pending decision，現已拍板

### Affected Artifacts

* `frontends/quality-inspection-frontend/src/app/guards/` — RoleGuard、ScopeGuard、ProjectionGuard 改為正式實作（目前為 PASSTHROUGH，待 actor context 注入完成後補實）
* `frontends/quality-inspection-frontend/src/app/` — 後續新增 directive 應放入 shared 或對應 feature module
* `frontend.instructions.md` — 補記 access control placement rule
* `frontend-backend-alignment-baseline.md` — I-001 狀態改為 accepted

### Consequences

* `RoleGuard` / `ScopeGuard` / `ProjectionGuard` 目前仍為 PASSTHROUGH；正式化前提為 actor context provider 已完成注入
* 新開發功能不得在 component 內自寫路由跳轉邏輯作為 page-level gate 替代
* component-level 可見性控制統一走 directive，不得硬寫在 template `*ngIf="role === 'xxx'"`

---

## D-030 — Frontend Role Context: RBAC as Formal Source (I-002)

### Status

`accepted`

### Category

`frontend-role-context`

### Decision

Frontend 正式採用 **RBAC**（Role-Based Access Control，以 actor context 的 `roles` 欄位為來源）作為 access policy 的正式來源：

1. **逐步移除 hardcoded role**：既有 hardcoded role（如 `'REVIEWER'`、`'MANAGER'`、`'ADMIN'` 字串）應在涉及修改時優先改接 RBAC-backed guard 或 directive。
2. **新碼禁止新增 hardcoded role**：任何新實作不得引入新的 hardcoded role 字串比對。
3. **Legacy grandfathering**：目前已確認的 hardcoded role 範圍（`cases-worklist/case-list-page.component.ts`、`sampling/batch-list/sampling-batch-list-page.component.ts`）在未被主動修改前，允許暫時維持現況。被修改時，應同步改接 RBAC-backed 機制。

### Rationale

* 消除 frontend role 的 single source of truth 模糊性
* 長期維護性：角色新增 / 異動時只改 actor context provider，不改各頁面
* 對應 I-002 的 pending decision，現已拍板

### Affected Artifacts

* `frontends/quality-inspection-frontend/src/app/features/cases/cases-worklist/case-list-page.component.ts` — 後續修改時應改接 RBAC-backed guard / directive
* `frontends/quality-inspection-frontend/src/app/features/sampling/sampling-batch-list/sampling-batch-list-page.component.ts` — 同上
* `frontend.instructions.md` — 補記 RBAC 優先原則與 hardcoded role 禁止規則
* `frontend-backend-alignment-baseline.md` — I-002 狀態改為 accepted

### Consequences

* 既有 legacy hardcoded role 不強制立即重構，但一旦碰到該 component 就必須改
* 新增任何 access control 邏輯必須以 guard 或 directive 配合 actor context 實作
* `CaseDetailPageComponent` / `RevisionHistoryPageComponent` 已消費 backend `actorRole` 的做法符合本規則，可保留；但 `MANAGER` / `ADMIN` legacy alias 在下次碰到時應同步清除


---

## D-031 — 初審提交、固定與後續流向分離

### Status

`accepted` — 使用者本輪確認。

### Category

`domain-lifecycle-and-revision`

### Decision

* 初審提交與固定為不同業務事實；提交本身不代表固定、進入複審或結案。
* 原提交者可 revision 合法已提交且未固定的初審結果；固定後禁止 revision。
* 初審固定是進入複審的必要條件。系統以固定的初審結果依既有規則判斷：分數 > qualityThreshold 自動進入複審；≤ threshold 或 threshold 為 null 不強制複審，保留 manager 手工派複審選項。
* 手工派複審亦不得繞過初審固定；複審始終建立在已固定初審結果之上。
* 不需複審的案件，只能由院方管理者在初審固定後正式結案；提交、固定本身都不自動結案。

### Supersession / Preserved Rules

部分取代 D-019 的判斷／流向時機及無需複審的結案前置條件；保留其閾值比較、null 分支及 manager 手工派複審責任。
D-002 原責任人 revision、D-004 append-only、D-005 revision 不等於 reopen、D-006 CANCELLED terminal 保留。

### Rationale / Consequences

固定界定初審可修訂窗口，避免複審建立於仍可變動的結果。固定與評分表／ProjectVersion 的 immutable snapshot 是不同事實。
ReviewCase 保護進複審與結案條件；ReviewSubmission 保護提交與 revision。沿用既有 aggregate 邊界，不新增 aggregate。
初審固定授權原列 unknown，已由 DD-03 accepted 補齊；method 名稱、事件技術名稱、API、狀態／儲存表示仍為 UNKNOWN。

### Affected Artifacts

Requirements / glossary、domain-model、state-machine、domain-events、application-service、permission-matrix、API / persistence、examples / test scenarios、UI-010～014 與 frontend bridge。

---

## D-032 — 複審提交與正式結案分離，結案固定結果

### Status

`accepted` — 使用者本輪確認。

### Category

`domain-lifecycle-and-revision`

### Decision

* 複審正式提交不自動 CLOSED，也不自動固定結果。
* 合法複審提交完成且不存在其他既有阻擋條件時，即具備結案資格；資格不等於已結案。不新增分數門檻、額外審批或其他未確認條件。
* 由原複審提交者決定正式結案時間；只有指派角色、manager 身份或曾提交歷史版本，不足以代替目前合法結果的提交責任驗證。
* 已提交且未固定、未 CLOSED／CANCELLED 的複審結果，可由合法原提交者 append-only revision；revision 不直接改變 Case 主狀態。
* 正式結案同時固定尚未固定的複審結果；不得出現已 CLOSED 而結果仍可修訂的成功結果。
* CLOSED 後禁止所有 revision、新提交與 reopen。CANCELLED 保持 terminal。
* D-012／D-020 不變：finalScore = initialReviewScore × initialReviewWeight + rereviewScore × rereviewWeight；默認各 0.5、權重和為 1、均 > 0 且 ≤ 1，發布版本權重不變；複審 revision 後依相同固定初審結果與權重重算。

### Supersession / Preserved Rules

取代 D-021、D-027 的 CLOSED 複審 revision 及入口例外；部分取代 D-001 B4 的例外內容，保留 terminal 與 no-reopen。
取代 state-machine、application-service 等下游「複審提交即結案」語意。
D-003 合法原責任人、D-004、D-005、D-012、D-020、D-023／024 的角色及院所 scope guard 保留。

### Rationale / Consequences

提交、具備結案資格、正式結案是不同業務事實；ReviewCase 保護正式結案，application service 協調提交、有效版本、分數與固定的一致性。
不在本輪新增 CaseStatus／SubmissionStatus、aggregate、endpoint 或 DB 欄位。複審已提交未結案的狀態表示、獨立結案 binding、固定的持久化／併發機制，以及是否提供獨立複審固定操作均為 unknown。
既有資料遷移與 API／事件相容性需後續評估；文件更新不代表實作已對齊。

### Affected Artifacts

D-031 所列相關文件；另含歷程／報表及 UI-013／014 的 CLOSED 操作政策。

---

## D-033 — 複審全生命週期盲審與衍生資訊防洩漏

### Status

`accepted` — 使用者本輪確認。

### Category

`permission-and-visibility`

### Decision

* 複審醫師始終不可取得同案初審答案、分數、評語；提交後、revision 期間、固定後、CLOSED 後均不解除。
* 防止 finalScore 等衍生資訊間接洩漏初審分數。已知複審分數與非零權重時，加權 finalScore 可反推出初審分數，故不得向該複審醫師提供可推回初審資訊的 finalScore、preview、分數差異或等價衍生資訊。
* 規則涵蓋查詢、submit／revision／close 回應、列表／詳情／歷程／compare、報表／匯出與前端快取；不得只隱藏畫面而在 response 暴露值。
* 不改變 manager 本院完整明細、reviewer 合法範圍、auditor 授權院區、system admin metadata-only 等既有政策。複審醫師不可藉視圖／入口切換繞過同案盲審。
* 遮罩仍在 application / view shaping 與 authorization boundary，不能修改 domain 原始分數；maskedFields 慣例保留。

### Supersession / Preserved Rules

取代 D-018 的時段限制及提交後解除遮罩；部分取代 D-026 的條件，保留 application/view layer 位置及既有 maskedFields 慣例。
D-008／009／010、D-012／020、D-017 與 D-028～030 不變；權重計算與資料可見性分開處理。

### Rationale / Consequences

永久盲審避免提交後修訂受到初審資訊影響；最終分數仍由系統按原公式計算與保存。
不預設新 projection enum、DTO、error code 或資料欄位；精確欄位表示、跨角色 actor 的授權合成方式與衍生欄位安全清單為 unknown，實作不得以 unknown 為理由放出受限資料。

### Affected Artifacts

Requirements / glossary、permission-matrix、application-service、API、persistence read model 說明、examples / test scenarios、UI-009～014 與 frontend projection／API binding／測試文件。

## DD-03 — 初審固定操作者與有效結果追溯（Gate 1）

### Status

`accepted` — 2026-11-04，使用者明確確認。

### Decision

1. 只有目前有效初審結果的合法提交醫師可固定該結果；僅有管理者、指派醫師身份或提交過歷史版本不足以授權。沿用既有角色、院所 scope 與 ownership guard。
2. 固定前依既有合法規則 append-only revision；固定後不得修改或 revision。
3. 自動／手工進複審前必須固定初審；需複審與否沿用 D-031 的 >、≤、null 分支。
4. 不需複審時，初審固定後仍須由 HOSPITAL_MANAGER 正式結案；提交及固定均不自動結案。
5. 固定必須可追溯操作者、時間與當時有效 submission；固定與 revision 競爭不得固定過期版本、遺失追溯或讓固定結果被改寫。

### Supersession / Preserved Rules

補齊 D-031 原未定的固定授權及追溯要求；D-031 的其他流程、D-032 複審結案、D-033 永久盲審及其他 accepted 決策不變。保留既有 superseded 原文與歷程。DD-03 不是 D-003（複審 revision）。

### Contract Boundary / UNKNOWN

沿用 ReviewCase／ReviewSubmission、Spring Boot／Gradle 與既有 API baseline；不新增 enum、aggregate、endpoint 或 DB schema。固定 binding、request／response schema、錯誤細碼、重複固定／網路重試語意、狀態／儲存表示、鎖定／版本與交易／事件發布技術、既有資料遷移／相容性方案均為 UNKNOWN。這些項目需後續定案，不代表實作已具備。

### Decision / Evidence Mapping

| 決策 | 規格落點 | Example / Scenario |
|---|---|---|
| 合法有效提交者固定與拒絕冒用 | permission-matrix §10；domain-model DD-03；API DD-03 | EX-053 / TS-DD03-001 |
| 固定前 revision、固定後不可變 | state-machine §4、DD-03；persistence DD-03 | EX-042、054 / TS-DA-002、TS-DD03-002 |
| 複審前置條件 | state-machine §3；application-service §17 | EX-041、054 / TS-DA-001、TS-DD03-002 |
| 無需複審的管理者結案 | requirements §13；permission-matrix §10 | EX-043 / TS-DA-003 |
| 操作者／時間／有效版本追溯與併發 | application、API、persistence DD-03 | EX-054、055 / TS-DD03-002、003 |
| 提交成功／拒絕與既有流程回歸 | API §11、12；D-031／032 | EX-056 / TS-DD03-004；EX-044～047 / TS-DA-004～007 |


## D-034 — 兩條結案路徑與複審獨立 Close Command

### Status

`accepted` — 2026-11-04，使用者明確確認；本決策不是 DD-03 的重新編號。

### Category

`domain-lifecycle-and-permission`

### Decision

1. 不需複審的案件：初審固定後，由符合既有院所 scope 的 HOSPITAL_MANAGER 另行正式結案；初審提交與固定均不自動 CLOSED。
2. 需要複審的案件：先固定初審，再由合法複審醫師執行複審並正式提交有效複審結果。合法提交且不存在其他既有阻擋條件即具結案資格，不新增分數門檻、管理者簽核或額外審批。
3. 具結案資格後，由合法複審醫師另行執行 Close Command，提交不自動造成 CLOSED。「合法」沿用 D-032：須為目前有效合法複審結果的提交責任醫師，並符合既有 REREVIEWER、hospital scope 與 ownership guard；僅有指派、manager 身份、複審角色或歷史版本提交紀錄不足以授權。不新增代理結案權限。
4. Close Command 執行時須重新驗證有效 submission；DRAFT、SUPERSEDED、過期目標、跨案／錯 stage、錯誤 actor／scope、CLOSED／CANCELLED 不得產生新的成功結案。不得把舊版請求默默套用新版。
5. 複審正式結案同時固定尚未固定的有效複審結果；固定、有效版本引用、分數與 CLOSED 須一致。close 與 revision 競爭時，revision 先完成則舊版 close 拒絕；close 先完成則 revision 拒絕。失敗不得留下部分結案／固定或成功事件。CLOSED 後禁止所有 revision、新提交與 reopen。
6. 複審醫師永久不可取得同案初審答案、分數、評語，包含結案後；D-033 的 finalScore 等衍生資訊防洩漏與全部資料出口保護不變。

### Relationship / Preserved Rules

* D-031：保留初審提交／固定分離、固定後才進複審、原 >／≤／null 閾值分支，以及無需複審時 manager 結案責任。
* D-032：確認並明文化獨立 Close Command；保留有效結果原提交者、結案資格、結案同時固定、未固定且非 terminal 時合法 append-only revision、原加權公式與發布版本權重。本次不擴張結案權限，也不恢復 CLOSED revision 例外。
* D-033：完整保留永久盲審與衍生資訊防洩漏，Close Command 不構成解除遮罩的理由。
* DD-03：保留初審固定操作者、有效結果追溯及固定／revision 競爭要求；不授予 manager 代固定權限。
* 本決策為上述 accepted 決策的確認與契約補充，不撤銷其規則；既有 superseded 原文與紀錄保留。

### Contract Boundary / UNKNOWN

Close Command 是獨立業務意圖，不代表本輪已決定 HTTP method／endpoint path、request／response schema、版本 token、error code、提交後狀態表示、固定儲存表示、交易／鎖定／事件發布技術、重複 close／網路重試回應、legacy 資料相容性或遷移方案；以上均為 UNKNOWN。terminal 不允許新的結案轉移，已成功 close 的重放回應語意仍 UNKNOWN。本次不新增獨立複審固定操作、管理者簽核、framework、aggregate 或 enum；不宣稱實作已對齊。

### Decision / Evidence Mapping

| 決策 | 規格落點 | Example / Scenario |
|---|---|---|
| 不需複審由 manager 結案 | requirements §13；permission-matrix §10 | EX-043 / TS-DA-003；EX-056 / TS-DD03-004 |
| 複審提交與 Close Command 分離 | application-service §17；API D-034 | EX-044、046 / TS-DA-004、006 |
| 未提交與錯誤 actor 拒絕 | permission-matrix §10；API D-034 | EX-045、046 / TS-DA-005、006 |
| 過期目標與 terminal 拒絕 | state-machine、API D-034 | EX-057、058 / TS-DC-001、002 |
| close／revision 與失敗一致性 | domain-model、persistence D-034 | EX-059、060 / TS-DC-003、004 |
| 結案後永久盲審 | permission-matrix §10；API D-034 | EX-061 / TS-DC-005；既有 EX-049～052 / TS-DA-009～012 |


## D-035 — 歷史複審身份 UNKNOWN 的完整初審讀取採 Fail-closed

### Status

`accepted` — 2026-11-06，使用者確認方案 A。

### Decision

1. 歷史複審身份不能可靠排除時，不得提供受限初審答案、分數、評語或可反推的衍生資訊。UNKNOWN 必須拒絕完整讀取，不得當作「沒有歷史身份」，也不得默默降級 response。
2. 區分 Proven historical、Proven non-historical、UNKNOWN。匹配的指派／提交事實可證明正向身份；空 Audit、部分缺失、損壞或沒有命中某醫師，不構成負面身份證明。現有來源尚不能產生可信的 Proven non-historical；不得補造完整性標記、legacy 歷史或保留政策。
3. 完整初審讀取仍須先通過既有 Role、hospital scope 與 assignment／ownership 範圍；它們是必要條件，不取代新增的歷史身份安全條件。已證明歷史身份與 UNKNOWN 均使用既有拒絕機制；只有可靠排除後才可能進入原有完整讀取分支。
4. 接受合法 Manager／Auditor 的完整查詢暫時受限，包含測試中未受指派及無需複審案件；目前狀態、rereviewRequired=false 與測試 fixture 的已知過去都不是完整歷史證明。同一安全前置條件不得因 REVIEWER 本人、其他完整讀取角色或入口切換而省略；相關初審本人查詢亦可能被拒絕。
5. 合法 REREVIEWER 仍可在既有 scope 內讀取自己的複審內容；初審永久遮罩，提交／CLOSED／歷史改派均不解除。SYSTEM_ADMIN 繼續既有 metadata-only 白名單與院所 scope；不新增完整內容權限。未定多角色合成不放寬。
6. 本次僅落實 Submission Detail、Case Detail、Revision History 的共同讀取邊界；不新增 API、schema、migration、角色或事件保留機制，不改原始資料、公式、審查流程或 Audit。

### Relationship / Supersession

補充 D-033 的 UNKNOWN 拒絕策略；對 D-008／D-009 與 permission matrix 的完整明細授權增加安全前置條件，部分取代「角色及院所符合即完整讀取」的無條件解讀，保留其角色與 scope 限制。D-024、D-028 的既有院所／metadata 白名單、D-031／032／034、DD-03、原始決策與 superseded 紀錄均保留。此決策不宣稱 Audit 歷史完整。

### Consequences / UNKNOWN

現有來源無法證明負面身份，故本次受保護的完整讀取可能全面遭拒；這是已接受的保守行為，不代表角色或院所錯誤。使用既有 AccessDeniedException／403 ACCESS_DENIED，不新增錯誤回應 schema。恢復完整讀取所需的可靠完整性／legacy 契約仍 UNKNOWN；不以假資料或測試 mock 宣稱已具備。D-032 Red Test 保留，IMP-02B 與 metadata 三個未定值不在本次範圍。

### Mapping

EX-062～065 / TS-FC-001～004；對應既有 78 個 completeness HTTP/JPA 案例、讀取授權 Unit/API 回歸。


---

## D-036 — UI-010 metadata 暫移除 lifecycleLatestStage，Lifecycle 新投影延後

### Status

`accepted`（2026-11-09，依使用者確認）

### Decision

僅部分調整 D-028 的 UI-010 page extension：暫時移除 `lifecycleLatestStage`，欄位不得出現在 metadata-only JSON，包含不得以 null 保留。UI-010 以既有 `caseStatus` 表達目前案件狀態，不將它冒充完整 Lifecycle／Milestone。

UI-010 SYSTEM_ADMIN metadata-only 正式白名單精確為八欄：`caseId`, `caseStatus`, `projectName`, `hospitalId`, `reviewerId`, `rereviewerId`, `updatedAt`, `latestRevisionSummary`。shared base 與其他頁 extension 不變；FULL-only、敏感 nested objects 及未來新增 DTO getter 均不得自動加入。

`updatedAt`、`latestRevisionSummary` 保留，但值契約與實作尚未完成（UNKNOWN）；本次不補值來源，不宣稱 IMP-02A 全面完成。

### Supersession / Compatibility

部分 supersede D-028 的 UI-010 extension 中 `lifecycleLatestStage`；保留 D-028 原文、shared base + page extension 策略及其他頁白名單。這是已確認的 response 欄位移除；repo 引用盤點未發現 Angular 或外部 consumer 的實際依賴，repo 外使用情形 UNKNOWN。

既有 Lifecycle Query、`GET /review-cases/{caseId}/lifecycle` 與 EX-039／TS-TR-005 完整生命週期需求保留；新增 Milestone／Latest Stage 投影實作延後，未取消業務需求。D-024 scope、D-033 永久盲審、D-035 Fail-closed 不變。不修改 State Machine、Domain Event、Audit 或 schema。

### Affected Artifacts / Verification

Glossary、Permission Matrix、API、UI-010、Frontend ViewModel、EX-066／TS-MD-001 與 metadata 序列化／HTTP exact-key tests 同步；兩個保留欄位的值完成度仍為 UNKNOWN。


---

## D-037 — 案件 updatedAt 的持久化、無變更與一致性契約

### Status

`accepted`（依使用者 IMP-02A-MD-02 確認，2026-11-12 再次確認；本輪僅 Authority 與 Red Tests）

### Decision

1. `updatedAt` 表示案件相關業務資料最後更新時間，採案件層級持久化，涵蓋 ReviewCase 與相關 ReviewSubmission；案件建立成功即設定初始值。
2. Initial／Rereview Draft Save 均在範圍內。Draft 業務內容完全未變時禁止儲存：不得寫入 Case／Submission、更新時間、新增 Draft Save Event／Audit，亦不得宣稱新的變更已儲存成功。
3. 既有 role／scope／status／owner／有效引用 Guard 不得被無變更判斷旁路。
4. 時間採統一 UTC 時間點；API 使用 ISO 8601。時間與業務資料由同一交易維護，rollback 不得保留更新時間。
5. 最小併發保證為時間不倒退，且時間更新機制不得造成業務資料遺失；不要求精確代表 commit 順序。
6. 無可信歷史時間的 legacy 案件保留 null，不推測或補造。

### Relationship / Supersession

部分補足 D-036 對 updatedAt 的待定業務契約，保留原決策及八欄 whitelist。實作仍未完成；latestRevisionSummary 的值契約仍 UNKNOWN，Lifecycle Milestone／Latest Stage 延後不變。D-031～035、永久盲審、Fail-closed、原 Guards 與 IMP-01 checkpoint 不因本決策放寬；不修復 D-032。

### Pending Technical Contracts / BLOCKED

No-change 尚無既有專用 Domain／HTTP 錯誤契約。最小建議是明確、可辨識的無變更拒絕結果，沿用既有錯誤外殼；具體 exception／error code／HTTP status 待使用者決定，不在本輪新增映射或測試固定狀態碼。內容正規化、答案順序、null／空字串、數值 scale 等非完全相同情形仍 UNKNOWN。

案件時間欄位／型別、migration／legacy 部署、Clock、原子更新或競爭保護技術未決。現行 schema 無案件時間欄位，現行 Repository 無相關併發機制；不能以 mock、假欄位或僅檢查缺少 getter 宣稱 rollback／時間單調性／資料不遺失已驗證。須在後續契約與實作就緒後補齊真實 JPA 競爭驗證。

### Mapping

EX-067／TS-MD-002；本輪保留現有測試，新增真實 UseCase／JPA／HTTP Red 與 Guard／rollback 對照。測試通過範圍不得擴張為 updatedAt 已完成。


---

## D-038 — Draft No-change 採 409／NO_CHANGES

### Status

`accepted`（2026-11-10，依使用者 IMP-02A-MD-03 確認）

### Decision

Draft Save 業務內容沒有實際變化時，HTTP 回傳 `409 Conflict`，`error.code=NO_CHANGES`，沿用既有 API error envelope，`success=false` 且不得有成功儲存結果。不得更新 Submission、Case.updatedAt 或新增 Draft Save Event／Audit。原有 role／scope／owner／terminal／有效引用等 Guard 必須先執行；有實際變更時原合法儲存能力保留。

### Relationship / Supersession

補足 D-037 的 No-change 回應待決項，保留 D-037 原文與全部 updatedAt 決策。不修改 D-033／035／036、八欄 metadata、Lifecycle 延後及 D-032 已知缺口。此決策未宣稱完整 Draft 比較、持久化或併發已實作。

### Evidence / Remaining UNKNOWN

現有 DomainStateConflictException 已映射 409，可作最小沿用途徑（技術建議，非本決策指定 exception 類別）。現有兩條 Draft API 尚直接回成功結果，無 NO_CHANGES guard。AnswerItem.equals 只覆蓋單筆 item／answer／derivedScore，沒有完整 Draft 比較；Request attachments 未進入 Draft Command／UseCase。附件、順序、正規化與數值 scale 等比較仍 UNKNOWN，不將忽略欄位推定為等價。

### Mapping

EX-068／TS-MD-003：以同 API 成功儲存後重送相同 payload 的 HTTP／JPA Red，以及既有 Guard 優先與 changed-save 對照驗證。updatedAt 持久化／並行設計位於 persistence-model.md，僅為待審技術提案。


---

## D-039 — 審查專案的狀態、啟用與停用

### Status

`accepted`（2026-11-12，依使用者裁決 spec-draft Q-01～Q-04）

### Decision

1. 審查專案的狀態為 `DRAFT`、`ACTIVE`、`DEACTIVATED`。
2. 啟用：`DRAFT` 或 `DEACTIVATED` 可轉為 `ACTIVE`，前置條件是目前版本已發布；未發布時拒絕，`409 INVALID_STATE_TRANSITION`。
3. 停用：只有 `ACTIVE` 可轉為 `DEACTIVATED`。`DEACTIVATED` 不是終點，可再啟用。
4. 草稿不是正式文件，沒有停用的問題：對 `DRAFT` 停用一律拒絕，`409 INVALID_STATE_TRANSITION`。
5. 對已是 `ACTIVE` 的專案啟用、對已是 `DEACTIVATED` 的專案停用：回成功，不改狀態，不發 event，不新增稽核紀錄。

### Relationship / Evidence

既有文件與決策沒有禁止再啟用；`domain-events.md` §4.2「由非啟用轉為啟用」與本決策一致。現行程式把 `DEACTIVATED` 當終點，且重複啟用仍發 event，與本決策不符，尚未修改。`state-machine.md` 尚未補上專案狀態機。

### Mapping

spec-draft：`RP-T-01`～`RP-T-03`、`RP-ACT-01`、`RP-ACT-03`、`RP-DEA-01`、`RP-DEA-02`。


---

## D-040 — 審查專案的建立、草稿更新與版本

### Status

`accepted`（2026-11-12，依使用者裁決 spec-draft Q-05～Q-08、Q-13、Q-14）

### Decision

1. `hospitalId` 取自登入者，請求不帶。
2. `name` 必填且不可空白；同院區的專案名稱可以重複，唯一識別是 `projectId`。`medicalRecordTypes` 至少一個。`description` 選填。
3. 更新草稿時，`name`、`description`、`medicalRecordTypes` 由輸入整組取代。
4. 上述三個欄位屬於版本內容。目前版本已發布後再更新一律拒絕，`409 INVALID_STATE_TRANSITION`。
5. `versionNo` 從 1 開始；發布不遞增；建立下一個草稿版本時才遞增。
6. 欄位格式不合法回 `400 VALIDATION_ERROR`；發布時權重不符合 D-020 回 `422 PROJECT_VERSION_NOT_PUBLISHABLE`。
7. 草稿更新成功要發布 `ReviewProjectDraftUpdated` event。

### Pending

「建立新草稿版本」的 use case 另案處理，尚未設計。對已發布版本再次發布的錯誤代碼未定（現行測試預期 409）。品質線與權重的輸入管道見 spec-draft Q-10，未決。

### Mapping

spec-draft：`RP-CRT-01`、`RP-CRT-02`、`RP-CRT-S2`、`RP-CRT-S3`、`RP-UPD-01`～`RP-UPD-03`、`RP-UPD-S1`、`RP-UPD-S2`、`RP-PUB-03`、`RP-PUB-S2`。TS-PV-003、TS-PV-004 已同步。


---

## D-041 — 專案綁定的是評分表版本，發布前必須已綁定

### Status

`accepted`（2026-11-12，依使用者裁決 spec-draft Q-09）

### Decision

專案綁定的對象是評分表「版本」（`scoreSheetVersionId`），不是評分表本體。發布專案版本時必須已綁定，否則拒絕，`422 PROJECT_VERSION_NOT_PUBLISHABLE`。啟用不再另外檢查綁定。

### Relationship / Evidence

與 `application-service.md` §5、TS-PV-003 及 D-011（案件可追溯到評分表版本）一致。`api-spec.md` §7.6 的請求欄位已改為 `scoreSheetVersionId`。現行程式綁定的是 `scoreSheetId`，且在啟用時才檢查，尚未修改。

### Pending

綁定時是否要求該版本存在且已發布，未定。

### Mapping

spec-draft：`RP-PUB-02`、`ProjectVersion.scoreSheetVersionRef`。


---

## D-042 — 院區範圍一律取自登入者，並適用於審查專案與查詢

### Status

`accepted`（2026-11-12，依使用者裁決 gap 報告 R-02 與 spec-draft Q-11）

### Decision

1. D-024 的院區範圍檢查擴及 `ReviewProject`，以及所有列表、查詢、匯出類的 use case。
2. 範圍判斷所用的院區一律取自登入者；client 傳入的院區參數不得用來決定或擴大可見範圍。

### Relationship / Evidence

補足 D-024（原條文只寫 `ReviewCase` 與 `SamplingBatch`）。gap 報告 2026-11-11 R-02 列出 9 支未檢查的 use case，其中列表類端點的院區來自選填的請求參數，尚未修改。

### Pending

`SYSTEM_ADMIN` 是否不受院區限制，未定。api-gateway 是否另有一層院區檢查，未確認。

### Mapping

spec-draft：`RP-G-03`。


---

## D-043 — 審查專案詳情的讀取角色與欄位

### Status

`accepted`（2026-11-12，依使用者裁決 spec-draft Q-12）

### Decision

`SYSTEM_ADMIN`、`HOSPITAL_MANAGER`、`AUDITOR` 可以查詢專案詳情。回傳欄位為 `projectId`、`hospitalId`、`name`、`description`、`medicalRecordTypes`、`status`、`versionNo`、`versionPublished`、`qualityThreshold`、`initialReviewWeight`、`rereviewWeight`。

### Relationship / Evidence

現行程式拒絕 `AUDITOR`，與本決策不符，尚未修改。

### Pending

`REVIEWER`／`REREVIEWER` 的「唯讀必要範圍」未定，在定義前不開放。


---

## D-044 — Draft 無變更的判定方式

### Status

`accepted`（2026-11-12，依使用者確認）

### Decision

1. 答案以題目 id 對應後比較，不看順序。
2. 數值以數值比較，不看小數位數。
3. 文字欄位的 null 與空字串視為相同。
4. 附件在 Controller 尚未把它傳入 Draft use case 之前，不納入比較。
5. 前端收到 `NO_CHANGES` 時視為「已是最新」，不顯示錯誤。

### Relationship

補足 D-038 標為 UNKNOWN 的比較語意。附件接上之後的比較方式另行決定。


---

## D-045 — 案件的併發保護採樂觀鎖

### Status

`accepted`（2026-11-12，依使用者確認）；落地 BLOCKED

### Decision

案件的併發保護採版本欄位的樂觀鎖：以過期資料寫入時，後寫者失敗，不得覆寫先寫者的資料。取代 `persistence-model.md`「IMP-02A-MD-03」提案中的悲觀鎖做法；該提案其餘內容（`updatedAt` 欄位、Clock、legacy 保留 null、必要的整合測試）仍為待審提案。

### Pending / BLOCKED

正式資料庫與 migration 工具未定，在決定之前不得新增欄位或 migration。衝突時的錯誤代碼與 HTTP 狀態未定。


---

## D-046 — 品質線：草稿可為空，發布時必填，範圍為整數 1～10

### Status

`accepted`（2026-11-12，依使用者裁決 spec-draft Q-10）；範圍為暫定值

### Decision

1. 品質線（`qualityThreshold`）是建立專案與更新草稿的選填輸入；草稿階段可以是 null。
2. 發布專案版本時品質線必填；為 null 時拒絕，`422 PROJECT_VERSION_NOT_PUBLISHABLE`。
3. 品質線的值是 1 到 10 的整數（暫定）；超出範圍或不是整數回 `400 VALIDATION_ERROR`。

### Relationship / Evidence

保留 D-019、D-031 的「threshold 為 null 不強制複審」分支，不廢止。但本決策生效後，新發布的版本一定有品質線，該分支只會出現在本決策之前建立的資料。現行程式沒有任何 use case 能設定品質線，正常操作建立的專案品質線都是 null，尚未修改。

### Pending

- 權重（`initialReviewWeight`、`rereviewWeight`）的輸入管道與範圍未定：使用者表示「權重也一致」，但 D-020 規定兩個權重相加為 1.0 且各自大於 0、小於等於 1，與「整數 1～10」不相容，待釐清。
- 初審總分的量尺未定。`api-spec.md` §7.1 與 `domain-events.md` 的範例品質線是 80，與 1～10 不一致，範例尚未修改。

### Mapping

spec-draft：`RP-CRT-03`、`RP-UPD-04`、`RP-PUB-04`、`ProjectVersion.qualityThreshold`。


---

## D-047 — SYSTEM_ADMIN 讀取不受院區限制；權重與品質線走同一個輸入管道

### Status

`accepted`（2026-11-12，依使用者裁決）

### Decision

1. `SYSTEM_ADMIN` 的讀取（查詢、列表、匯出）一律跨院區，不做院區範圍檢查。列表查詢時可以用請求參數指定院區來縮小範圍，不指定則看全部。
2. 其他角色的讀取範圍一律是登入者自己的院區（D-042）；請求帶的院區參數不採用。
3. 寫入命令的角色與院區檢查不變。
4. 權重（`initialReviewWeight`、`rereviewWeight`）與品質線走同一個輸入管道：建立專案與更新草稿時可填，發布時依 D-020 驗證。數值規則維持 D-020（相加為 1.0，各自大於 0、小於等於 1），不是整數 1～10。

### Relationship / Evidence

補足 D-042、D-043、D-046 的 Pending。`SYSTEM_ADMIN` 看到的內容仍受 D-036 的 metadata-only 白名單與 D-033 盲審限制，本決策只放寬院區範圍。案件讀取原本對 `SYSTEM_ADMIN` 也檢查院區，已依本決策放寬。權重的輸入管道尚未實作。

### Mapping

spec-draft：`RP-G-03`、`CreateReviewProject` 與 `UpdateReviewProjectDraft` 的權重輸入。


---

## D-048 — 後端採 DDD 加 CQRS：查詢經過 application 的查詢埠，讀取模型不重用寫入側的 entity

### Status

`accepted`（2026-11-14，依使用者裁決）

### Decision

1. 後端的讀寫分離做到「同一個資料庫、同一組資料表、程式路徑分開」這一級。不建立獨立的讀取表或讀取資料庫。
2. 查詢一律經過查詢埠：埠與回傳的 view 定義在 `application`，實作（adapter）在 `infrastructure`。查詢處理器不得直接存取持久層。
3. 讀取模型使用自己的投影（view），不得重用寫入側的 JPA entity 作為查詢結果的型別。
4. `reporting` 不再是獨立的讀取層，其內容逐步搬進 `application` 的查詢側。搬遷完成前的過渡規則：
   - `reporting` 對 `application` 只能依賴 `application.common`。
   - `infrastructure` 不得依賴 `reporting`。
5. 角色檢查與輸入驗證可以用統一的切面處理；院區範圍必須是查詢條件的明確欄位，由登入者決定（D-042、D-047），不得只靠切面隱含套用。

### Relationship / Evidence

- 現況已有六個查詢 adapter 採用第 2 點的寫法（`ReviewCaseQueryPort` 等介面在 `application`，`*QueryAdapter` 在 `infrastructure`）。
- `reporting` 的兩支服務直接使用 `EntityManager` 與 `ReviewCaseEntity`，不符合第 2、3 點；這是既有違規，已凍結在架構規則的基線裡。
- 2026-11-14 以測試確認這兩支服務沒有套用 D-042 的院區範圍，同日修正。這是「讀取側另走一條路徑時，範圍規則容易漏掉」的實例，也是第 2、5 點的理由。
- 本決策補上架構規則 ARCH-01、ARCH-04 的依據：兩條規則的依賴方向原本是依各層責任推定的。

### Pending

- `reporting` 搬進 `application` 的時程與套件命名未定。
- 下列規則尚未寫成閘門：查詢側不得依賴寫入用的 repository 或命令 use case；命令側不得依賴查詢的 view；查詢處理器一律是唯讀交易；每個查詢處理器都必須帶授權註解並接收範圍條件；原生 SQL 只能出現在 adapter。
- 統一的授權與驗證切面尚未設計。盲審遮罩（D-033、D-036）不適用切面，維持明確的 policy。

### Mapping

架構規則：ARCH-01、ARCH-04（分層依賴方向）、ARCH-05（`reporting` 只能依賴 `application.common`）。

---

## D-049 — 已發布版本再次發布的錯誤代碼；更新草稿時沒給的權重回到預設

### Status

`accepted`（2026-11-15，依使用者裁決 `specs/review-project/OPEN-QUESTIONS.md` Q-08、Q-15）

### Decision

1. 對目前版本已發布的專案再次發布，拒絕，錯誤代碼 `INVALID_STATE_TRANSITION`（衝突）。這是狀態問題，不是內容不足以發布；與已發布後更新草稿（D-040 第 4 點）一致。
2. 更新草稿時，有給的權重等於輸入；沒給的權重回到預設 0.50（D-020）。權重不在更新時驗證，發布時才依 D-020 驗證。

### Relationship / Evidence

- 第 1 點補足 D-040 的 Pending「對已發布版本再次發布的錯誤代碼未定」。現行測試 `ReviewProjectApiTest.publishVersion_alreadyPublished_shouldReturn409` 預期的就是衝突，程式不需要改。
- 第 2 點補足 D-047 第 4 點沒寫的情況。理由：結果只由這次的輸入決定，與建立時（沒給的權重是 0.50）一致，也符合 D-040 第 3 點「整組取代」的精神。選項「保留原值」會讓更新變成部分修改；選項「null」會讓每次更新都必須給權重，否則發布一定被拒絕。
- 權重的輸入管道尚未實作（D-047），第 2 點目前沒有對應的程式。

### Mapping

`specs/review-project/`：`RP-PUB-01`、`RP-UPD-S3`。

---

## D-050 — 評分表的版本、建立、更新、發布與查詢

### Status

`accepted`（2026-11-15，依使用者裁決 `spec-draft/score-sheet/OPEN-QUESTIONS.md` Q-16～Q-23，全部採建議；Q-23 的兩個錯誤代碼另行選定）

### Decision

1. **版本的識別（Q-16）。** 評分表版本有自己的 `scoreSheetVersionId`，發布時產生並回傳，版本查詢也回傳（未發布時為 null）；專案綁定的就是這個 id（D-041）。本次只有第一版，「建立新的草稿版本」未定。
2. **建立草稿（Q-17）。** 不檢查專案狀態；同一專案可以有多份評分表，被採用的是專案綁定的那個版本。
3. **錯誤代碼（Q-18）。** 更新已發布的版本、再次發布：`INVALID_STATE_TRANSITION`（衝突）。指向不存在的章節、題目：`SECTION_NOT_FOUND`、`ITEM_NOT_FOUND`（規則違反；評分表本身存在，不用「找不到」類）。重複的章節、題目 id、題目代碼依 api-spec §8.2：`DUPLICATE_SECTION_ID`、`DUPLICATE_ITEM_ID`、`DUPLICATE_ITEM_CODE`（規則違反）。
4. **欄位驗證（Q-19）。** 必填欄位為 null 或空白、章節標題空白、`maxScore` 小於 0、`CONDITIONAL` 沒有 `conditionExpression`、非 `CONDITIONAL` 卻有 `conditionExpression`：一律 `VALIDATION_ERROR`。題目沒有輸入 `weight` 時是 1；`weight` 的數值範圍本次不限制，由計分的規格處理。
5. **更新命令（Q-20）。** 採 D-015 的六相位命令，依序：刪除章節 → 刪除題目 → 章節改名 → 修改題目 → 新增章節（含題目）→ 新增題目到既有章節，後面的相位看得到前面的結果。每個相位內先驗證該操作的輸入（第 4 點），再查找章節或題目，最後檢查重複。修改題目是整筆取代。六個相位都空時成功、沒有改變（D-038 的 `NO_CHANGES` 只適用提交草稿）。更新成功發出 `ScoreSheetDraftUpdated`。評分表名稱與 `displayOrder` 本次不納入；順序以清單順序表示。
6. **發布條件（Q-21）。** 版本內沒有任何題目時拒絕，`SCORE_SHEET_NOT_PUBLISHABLE`；有題目時容許空章節。
7. **查詢（Q-22）。** 詳情、章節、版本、列表四支查詢的讀取角色為 `SYSTEM_ADMIN`、`HOSPITAL_MANAGER`、`AUDITOR`，院區範圍依 D-042、D-047。D-010 的「完整評分內容」指提交的分數與答案，不包括評分表的題目定義，所以 `SYSTEM_ADMIN` 可以讀章節與題目。`REVIEWER`／`REREVIEWER` 不開放（審查時用案件的 snapshot）。章節查詢回傳 `{ scoreSheetId, sections }`，列表回傳 `{ items }`。
8. **綁定（Q-23）。** `BindScoreSheetToProject` 屬於 `ReviewProject`，在評分表規格升格時一起加進 `specs/review-project/`。檢查依序：角色 → 專案存在 → 院區 → 專案目前版本未發布（否則 `INVALID_STATE_TRANSITION`）→ 評分表版本存在且屬於這個專案（否則 `SCORE_SHEET_NOT_FOUND`，不透露其他專案的版本是否存在）→ 評分表版本已發布（否則 `INVALID_STATE_TRANSITION`）。成功時專案目前版本的評分表版本參照等於輸入，重新綁定會取代。

### Relationship / Evidence

- 第 1 點落實 D-041，與 D-011、`persistence-model.md` §5.5 一致。第 8 點補足 D-041 的 Pending（綁定時版本是否要存在且已發布）。
- 第 2 點取代 `api-spec.md` §8.1 的 `PROJECT_NOT_ACTIVE`：要求專案已啟用會卡死（專案發布前必須已綁定，RP-PUB-02；啟用前必須已發布，RP-ACT-03）。
- 第 3 點的 `INVALID_STATE_TRANSITION` 與 RP-UPD-01、D-049 第 1 點一致，取代 api-spec §8.2 的 `ALREADY_PUBLISHED`。
- 第 5 點以 D-015 為準，修正 api-spec §8.2 的整份取代範例。
- 現行程式與本決策的差異（沒有版本 id、欄位驗證回 500、重複章節的代碼、`AUDITOR` 被拒、綁定不檢查評分表等）列在規格草稿的「程式現況」，尚未修改。

### Mapping

`spec-draft/score-sheet/`：`SS-CRT-01`、`SS-CRT-02`、`SS-UPD-01～12`、`SS-UPD-S1`、`SS-UPD-S3`、`SS-UPD-S4`、`SS-PUB-01～03`、`SS-PUB-S2`、四支查詢的角色與輸出。第 8 點在升格時對應到 `specs/review-project/` 的綁定規則。

---

## D-052 — 登記備援核准金鑰的修法（PR #33）以人工審查取代簽章，破例合併

### Status

`accepted`（2026-11-15，依負責人明確指示「#33 破例合併」）

### Decision

PR #33 在 `law/allowed_signers` 為負責人 `tsengeagle` 新增第二把公鑰（resident FIDO2 金鑰，指紋 `SHA256:8hyCboa8G5+Kuzbg/2XELz5LKqawGnhaUOZULqHPpHI`）。這個 PR 不附核准簽章，`law-check` 在紅燈下合併，由負責人的人工審查取代簽章。

破例只限這一次，條件是：

1. 原本登記的金鑰沒有遺失。它是非 resident 金鑰，把手檔只在負責人暫時拿不到的舊電腦上，而且沒有任何備份可以還原。
2. 新公鑰由負責人本人在新電腦產生。指紋由負責人親自核對，並以 `ssh-keygen -K` 從實體金鑰取回、確認相同。
3. 這次修法只新增負責人自己的金鑰，不刪除、不修改任何既有的金鑰，也不改動判決程式與清冊。
4. 合併前由負責人逐行審查 PR 的差異。

之後的所有修法恢復簽章，第一筆就是 PR #28。

### Relationship / Evidence

- 這是 2026-11-14「不留繞過權限」裁決（`docs/harness/07-github-settings.md` 第一節）的例外。它也不屬於同一文件第四節的 SOP：那個 SOP 只適用於檢查本身壞掉。所以這次的依據是本裁決，不是 SOP。
- 性質和 2026-11-14 第一次登記金鑰（修法紀錄 `2026-11-14-enable-signed-approval`）相同：沒有可用的簽章，由負責人親自核對公鑰。差別是這次原本的金鑰仍然存在。
- GitHub 免費帳號的私有 repo 不會強制執行分支保護，所以紅燈下合併在技術上可行。防線是事後偵測：合併後 `main` 上的 `law-check (post-merge)` 會判定未通過，這是預期中的，記在 `07-github-settings.md` 的例外紀錄。
- 已知的風險：同一個人連續幾天拿不到舊電腦，就可能想再破例一次。為避免這種情況，條件 1 寫死為「原本的金鑰仍然存在、只是暫時拿不到、而且沒有備份」。日後若再發生，這筆裁決不能直接援用，要另外裁決。
- 備援金鑰和原本的金鑰在同一支實體金鑰上：它防的是換電腦，不防金鑰遺失。第二支實體金鑰仍然沒有登記。

### Mapping

`law/allowed_signers`、`law/amendments/2026-11-15-backup-approval-key.json`、`docs/harness/07-github-settings.md` 的例外紀錄。
