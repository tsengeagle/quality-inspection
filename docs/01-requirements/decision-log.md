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

## 憲法草案

> 本節是全專案原則，位階高於本檔所有個案裁決（D-xxx）。後續裁決不得牴觸本節；要牴觸，必須先以新的裁決修改本節的條文。本節預定提升為獨立的憲法文件，提升之前以這裡為準（D-072）。
>
> 條文以「第 N 條」編號，與 D-xxx 分開。頒布與修改都以個案裁決記錄，條文旁註明出處；條文只放在本節，不在其他地方重複。

### 第一條　日期時間的表示（D-072 頒布）

1. 凡是表示某一時刻的值，跨出程式邊界時一律使用 ISO-8601 延伸格式，帶時區位移（`Z` 或 `±hh:mm`），而且秒必須寫出。程式邊界包括 HTTP 請求與回應、事件的欄位、稽核 `detail`、匯出、規格範例，以及資料庫以外的任何儲存或傳遞。
2. 輸出一律正規化為 UTC（`Z`）。輸入接受任何位移；不帶時區的輸入一律拒絕（`VALIDATION_ERROR`）。
3. 程式內部以時間點表示（Java 用 `Instant`）。業務上需要「當地日期」的地方，一律以 `Asia/Taipei` 換算，換算的位置必須在裁決中寫明。
4. 只有日期、沒有時刻的值（`YYYY-MM-DD`）不受第 1 點約束，但必須由裁決明文指定，並寫明以哪個時區解讀。

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

`accepted`（部分取代）

> 部分被 D-058 取代：路徑樣式改為 stage 在前（`/review-cases/{caseId}/{stage}/...`），提交命令的路徑不帶 `submissionId`，並保留 `GET /review-submissions/{submissionId}`。保留 case-rooted、command 一律使用 `POST`。下列為歷史決策原文。

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

* ~~退回初審的最終授權角色~~（D-080 第 6 點：`HOSPITAL_MANAGER`，本院）
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

`accepted`（2026-11-12，依使用者確認）；2026-11-16 依使用者裁決補上正式資料庫、migration 工具與衝突錯誤代碼，解除 BLOCKED

### Decision

案件的併發保護採版本欄位的樂觀鎖：以過期資料寫入時，後寫者失敗，不得覆寫先寫者的資料。取代 `persistence-model.md`「IMP-02A-MD-03」提案中的悲觀鎖做法；該提案其餘內容（`updatedAt` 欄位、Clock、legacy 保留 null、必要的整合測試）仍為待審提案。

2026-11-16 補充（使用者裁決）：

1. **正式資料庫**：Oracle Database 19c 以上。
2. **migration 工具**：Flyway，版本跟隨 Spring Boot 2.7.18 管理的 8.5.13。已查證該版免費版的 Oracle 支援下限是 12.2（`flyway-core` 8.5.13 `OracleDatabase.ensureSupported`：12.2 以下需 Enterprise 版；19 以上只提示建議升級 Flyway），19c 在範圍內，不需付費。Flyway 10 以上需要 Java 17，後端目前是 Java 11，升級 Flyway 主版本要和升級 Java 一起處理。
3. **衝突的錯誤代碼**：樂觀鎖失敗回 `409 CONFLICT`（沿用既有通用代碼，不新增）。
4. **開發與測試**：繼續用 H2，改用 Oracle 相容模式，由 Flyway 以同一組 migration 建立 schema；Hibernate 不再自動建表。migration 只用 H2 Oracle 相容模式與 Oracle 19c 都能執行的語法。
   - 測試：`ddl-auto=validate`，方言用測試專用的 `H2OracleCompatibleDialect`（`src/test/java/.../infrastructure/persistence/`）。它產生的 SQL 與 `Oracle12cDialect` 完全相同，只調整兩件事：不在啟動時查 H2 沒有的 `ALL_SEQUENCES`（本系統沒有 sequence，自動編號是 identity 欄位）；`validate` 時把 `NUMERIC`／`DECIMAL` 視為與整數、布林型別等價，其餘型別比對不放寬。
   - local 與正式環境：方言用 `Oracle12cDialect`，`ddl-auto=none`，schema 只由 Flyway 管理。不做 `validate` 的原因：Oracle 的 JDBC 把 `NUMBER(19,0)`、`NUMBER(1,0)` 回報為 `NUMERIC`／`DECIMAL`，Hibernate 5 的 `validate` 期望 `BIGINT`、`BIT`，一律判為不符（2026-11-16 在 PR #49 以 H2 Oracle 相容模式實測，真的 Oracle 回報的型別相同）。
     - 更正（2026-11-17）：上句「真的 Oracle 回報的型別相同」是由 H2 實測推論的，沒有在真的 Oracle 上驗證。PR #55 之後在本機 Oracle XE 21c 實測：`Oracle12cDialect` 加 `ddl-auto=validate` 對 V1～V5 建立的 schema 驗證通過；刪掉一個欄位時則正確失敗（`Schema-validation: missing column`），可證實 validate 確實有執行。所以「真的 Oracle 一律判為不符」不成立。local 與正式環境仍維持 `ddl-auto=none`；是否改為 `validate` 需另行裁決，並應在 19c 上確認。
5. **Oracle 的限制**：空字串會被當成 NULL，程式不得依賴空字串與 NULL 的差別；19c 沒有 boolean 型別，布林值用 `NUMBER(1)`；物件名稱上限 128 字元；沒有 `TEXT` 型別，長文字用 `CLOB`（現有 entity 有 `columnDefinition = "TEXT"`，第 2 步修正）；`VARCHAR2(n)` 預設以位元組計長度，UTF-8 中文一字 3 位元組，migration 一律寫 `VARCHAR2(n CHAR)`；自動編號用 `GENERATED BY DEFAULT AS IDENTITY`；表名與欄位名不得使用 Oracle 保留字（Oracle SQL Language Reference 的「Oracle SQL Reserved Words」），現有的只有 `review_submission.comment` 這一個，改為 `comment_text`（只改欄位名，Java 欄位與 API 欄位不變）。

   2026-11-16 以 H2 2.1.214 實測（Flyway 第 1 步）：Oracle 相容模式接受 `VARCHAR2(n CHAR)`、`CLOB`、`NUMBER(1,0)`、`GENERATED BY DEFAULT AS IDENTITY`、`OFFSET … FETCH NEXT`，空字串也會當成 NULL；但不接受 Hibernate Oracle 方言自動建表產生的 `GENERATED AS IDENTITY`。一般模式不接受 `VARCHAR2(n CHAR)`；H2 方言產生的分頁 `LIMIT` 在 Oracle 相容模式會失敗。因此 Oracle 相容模式、Oracle 方言與「schema 由 Flyway 建立」三者要一起切換，不能分開。H2 的 Oracle 相容模式接受名為 `COMMENT` 的欄位，所以保留字的問題測試資料庫抓不到，要另外用測試檢查。另外有 19 個測試類別會換掉資料庫連線：17 個明寫 `@AutoConfigureTestDatabase`，2 個 `@DataJpaTest`（`ReviewCaseEntityRoundTripTest`、`ScoreSheetLegacyDataTest`）預設也會替換；切換時全部要改成不替換（`replace = NONE`）。Flyway 8.5.13 會警告 H2 2.1.214 比它測試過的 2.1.210 新，目前不影響使用。
6. **落地順序**：`build.gradle` 與 `src/test/resources/application-test.yml` 是律法（gate），修改它們要走修法 PR，而修法 PR 不能夾帶程式或 migration（P4），所以導入 Flyway 分三步（2026-11-16 依第 5 點的實測修正）：
   1. 修法 PR（PR #48）：`build.gradle` 加 Flyway 相依；`application-test.yml` 先停用 Flyway（`spring.flyway.enabled: false`），測試仍用一般 H2 與 Hibernate 建表。local profile 會啟用 Flyway，但還沒有 migration，只建立 Flyway 自己的紀錄表。
   2. 建構 PR（PR #49）：加入第一版 migration（以現有 entity 對應的 schema 為準，依第 5 點的 Oracle 寫法）；entity 的 `TEXT` 改為 `CLOB`；第 5 點列出的 19 個測試類別改為不替換資料庫（`@AutoConfigureTestDatabase(replace = NONE)`）、拿掉個別測試的 `ddl-auto=create-drop` 覆寫（與目前的測試設定相同，不影響結果）；`review_submission.comment` 欄位改名為 `comment_text`；加一個整合測試，以 H2 Oracle 相容模式、Oracle 方言、Flyway 建 schema、`ddl-auto=validate` 啟動並執行基本讀寫；加一個測試，從 Hibernate 的對應資料取出所有表名與欄位名，與 Oracle 保留字清單比對，有重疊就失敗；local profile 改為 H2 Oracle 相容模式、`Oracle12cDialect`、Flyway 建 schema、`ddl-auto=none`（見第 4 點）。
   3. 修法 PR（PR #50）：`application-test.yml` 一次切換為 H2 Oracle 相容模式、測試方言 `H2OracleCompatibleDialect`（見第 4 點）、啟用 Flyway、`ddl-auto=validate`，全部測試改用 Flyway 建的 schema。同時拿掉連線字串的 `DB_CLOSE_DELAY=-1`：改成 `validate` 後 Hibernate 不再刪表，而每個測試 context 用一個隨機名稱的資料庫，保留到 JVM 結束會累積記憶體；拿掉後，context 關閉、連線池釋放時資料庫就會關閉。

   之後依序以建構 PR 加入：(a) 案件版本欄位與 D-037 的 `updatedAt`（PR #51，V2）；(b) 下方的評分表參照與 `boundScoreSheetId` 改名（PR #52，V3）；(c) D-054 初審固定的欄位（PR #53，V4）。這些只改 `src/main` 與 migration，不動律法。三項都已完成；之後 D-055 的複審指派歷史與初審讀取者以 PR #55（V5）加入。

### Pending

- CI 無法在 Oracle 19c 上執行 migration，只驗證 H2 Oracle 相容模式。上線前要在院內的 Oracle 19c 實際執行一次 migration，這是已知的剩餘風險。
  - 2026-11-16 在本機以 Docker 的 Oracle XE 21c（21.3.0.0.0）、`Oracle12cDialect` 驗證過一次（PR #55）：V1～V5 依序套用成功，Hibernate schema validate 通過，D-055 與 D-054 的併發測試全部通過。這縮小了風險，但不取代 19c 的驗證（官方 19c 映像需要 Oracle 帳號，這個環境也無法連到其映像站）。

依 2026-11-16 負責人裁決（PR #42 審查），導入 migration 時一併處理評分表參照（已在 PR #52 完成，見下方各點的結果）：

- PR #42 起，專案綁定的評分表、案件的評分表參照、初審提交依據的評分表，存的都是評分表版本 id（D-050 第 1 點；由評分表 id 與版本號組成，沒有新增欄位），載入時只接受版本 id。
- 若屆時有以評分表 id 儲存的既有資料，要轉換成版本 id；目前只用記憶體資料庫，沒有這類資料，所以 PR #42 沒有加相容轉換。PR #52 導入時正式資料庫仍無資料（D-045：正式資料庫自 Flyway V1 起尚未上線），因此不需要轉換。
- 欄位名稱（專案的 `boundScoreSheetId`）沿用舊名、內容是版本 id；改名與資料庫欄位調整在那時一起做。PR #52 已完成：程式改名為 `scoreSheetVersionRef`，資料庫欄位以 V3 改為 `score_sheet_version_ref`，API 回應欄位為 `scoreSheetVersionId`。


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

## D-051 — REVIEWER／REREVIEWER 不能讀取專案與抽選批次

### Status

`accepted`（2026-11-15，依使用者裁決 `specs/review-project/OPEN-QUESTIONS.md` Q-12，採選項 A）

### Decision

`REVIEWER`、`REREVIEWER` 不能使用專案列表、專案詳情、抽選批次列表、抽選批次詳情，一律以 `ROLE_DENIED` 拒絕。他們審查所需的資訊從被指派的案件取得：案件詳情帶專案名稱，評分依據來自案件的 snapshot（D-011）。

`api-spec.md` §7.7、§7.8、§9.5、§9.6 原本寫的「唯讀必要範圍」／「依必要範圍唯讀」改為明確不開放。

### Relationship / Evidence

- 補足 D-043 的 Pending（「唯讀必要範圍」未定，在定義前不開放）：維持不開放，並把它定為正式決定。
- 與 `permission-matrix.md` §5 一致：這兩個角色的資料範圍是被指派給自己、或自己提交過的案件。
- `permission-matrix.md` §4 的 UI-007 抽選批次列表頁原本給這兩個角色 `R`，與本決策衝突，改為 `N`。前端路由（`sampling-routing.module.ts`）與後端本來就不允許這兩個角色，所以只有文件過時。
- 理由：
  - 目前沒有任何審查醫師的畫面需要這些介面。
  - 專案詳情含品質線與權重；初審醫師若知道品質線，可能把分數打在品質線附近，影響是否進入複審（D-019）。這和 D-033 盲審要防止的問題同類。
- 其他選項：
  - 只開放自己案件所屬的專案、並限縮欄位：需要跨聚合的判斷與專用投影，目前沒有使用情境。
  - 開放同院區全部：超出「只看自己的案件」的範圍。
- 現行程式已經拒絕這兩個角色，不需要修改。
- 日後若有審查醫師專用的專案總覽畫面，需要新的裁決。

### Mapping

`specs/review-project/`：`GetReviewProjectDetail` 的讀取角色（原 `actor.open` 指向 Q-12，已移除）。

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

---

## D-053 — 複審結案命令的 API、固定的表示方式與錯誤代碼

### Status

`accepted`（2026-11-15，依使用者裁決，第 1～8 點全部採建議）

### Decision

補足 D-032、D-034 列為 UNKNOWN 的技術契約：

1. **API**：`POST /review-cases/{caseId}/rereview/submissions/{submissionId}/close`，不需要請求內容。路徑上的 `submissionId` 是要結案的有效複審結果（D-034 第 4 點：明確辨識結案目標，不得偷偷改用新版）。
2. **回應**：`{ caseId, caseStatus: "CLOSED", submissionId }`。不回傳 `finalScore` 或任何可反推初審分數的資訊（D-033）。
3. **固定的表示方式**：案件變成 `CLOSED`，就代表複審結果已固定，不另外新增欄位。有效版本是結案當下案件的 `latestRereviewSubmissionId`；結案時間記在既有的 `closedAt`，操作者記在稽核事件。`CLOSED` 之後本來就禁止所有 revision、新提交與 reopen（D-032），固定的效果由此達成。
4. **提交後、結案前的狀態**：案件維持 `REREVIEW_IN_PROGRESS`，最新的複審提交為 `SUBMITTED`。不新增案件狀態（D-032）。`finalScore` 在提交與 revision 時依 D-012／D-020 計算並保存，只是不回傳給複審醫師。
5. **錯誤代碼**：全部沿用既有代碼，不新增。檢查順序依 D-024：角色 → 載入 → 院區 → 領域規則。

   | 情況 | 代碼 |
   |---|---|
   | 不是 `REREVIEWER` | `ROLE_DENIED` |
   | 案件或提交不存在 | 既有的資源不存在代碼 |
   | 院區不符 | `HOSPITAL_SCOPE_MISMATCH` |
   | 案件已是 `CLOSED` 或 `CANCELLED` | `CASE_TERMINAL` |
   | 案件不在 `REREVIEW_IN_PROGRESS` | `CASE_NOT_IN_REREVIEW_STAGE`（D-069 第 3 點修改，原為 `CASE_NOT_REREVIEW_IN_PROGRESS`） |
   | 結案目標不屬於本案、不是複審階段，或不是目前有效版本（已被取代、過期） | `CONFLICT` |
   | 結案目標仍是草稿 | `SUBMISSION_NOT_SUBMITTED` |
   | 操作者不是這份有效結果的提交者 | `NOT_SUBMISSION_OWNER` |

6. **重複結案**：已 `CLOSED` 的案件再次結案，回 `CASE_TERMINAL`，不視為成功。前端收到時重新整理畫面。
7. **結案與 revision**：依路徑上的 `submissionId` 檢查是否仍是目前有效版本。revision 已先完成，舊版的結案就回 `CONFLICT`；結案已先完成，revision 依既有規則以 `CASE_TERMINAL` 拒絕。兩者真正同時發生的競爭，要靠 D-045 的樂觀鎖才能完全擋住；D-045 的落地仍被擋住（正式資料庫與 migration 工具未定），這是已知的剩餘風險。
8. **前端**：複審頁在「最新的複審提交為 `SUBMITTED`，且登入者是它的提交者」時顯示「結案」按鈕；結案成功後顯示已結案，畫面不顯示 `finalScore`。

### Relationship / Evidence

- 補足 D-032（複審已提交未結案的狀態表示、獨立結案 binding、固定的持久化）與 D-034（Contract Boundary 列出的 binding、schema、錯誤代碼、重複 close 回應）的 UNKNOWN。D-032、D-034 的業務規則不變。
- 第 2 點與 D-033 一致。現行的複審提交回應會把 `finalScore` 回給複審醫師，違反 D-033；`RereviewApiTest#fullRereviewFlow` 還預期這個欄位存在。這兩處在實作時一併修正，不需要另外裁決。
- 第 3 點不新增欄位，因此不受 D-045「正式資料庫與 migration 工具未定前不得新增欄位」限制。
- 第 5 點的代碼都是現有複審提交與複審 revision 已在使用的代碼。
- 2026-11-22 依 D-069 第 3 點修改第 5 點：案件不在 `REREVIEW_IN_PROGRESS` 改回 `CASE_NOT_IN_REREVIEW_STAGE`（422），與初審的 `CASE_NOT_IN_INITIAL_REVIEW_STAGE` 對稱；`CASE_NOT_REREVIEW_IN_PROGRESS`（409）停用。其餘代碼不變。
- 不在本決策範圍：初審的「固定」（D-031、DD-03）在程式裡同樣尚未實作，另案處理。

### Mapping

`docs/03-contracts/api-spec.md` 的「D-034 複審獨立 Close Command 對齊」；`docs/02-ddd/state-machine.md` 的複審提交與結案轉移；已知失敗清單上的 `SubmitRereviewUseCaseTest#execute_submitMustNotCloseCase_D032`、`RereviewApiTest#fullRereviewFlow`。


---

## D-054 — 初審固定的 API、記錄方式與錯誤代碼

### Status

`accepted`（2026-11-16，依使用者裁決：只加欄位、不新增案件狀態；其餘採建議）

### Decision

補足 D-031、DD-03 列為 UNKNOWN 的技術契約：

1. **API**：`POST /review-cases/{caseId}/initial-review/submissions/{submissionId}/fix`，不需要請求內容。路徑上的 `submissionId` 是要固定的有效初審結果（DD-03 第 5 點：明確辨識固定目標，不得偷偷改用新版）。路徑形式與 D-053 的複審結案一致。
2. **記錄方式**：在案件上新增三個欄位：固定時間 `initialFixedAt`、固定者 `initialFixedBy`、固定的提交 `initialFixedSubmissionId`。**不新增案件狀態**。「已固定」的判斷是 `initialFixedAt` 有值。欄位依 D-045 第 6 點的順序，在導入 Flyway 的三步完成之後才新增。
3. **流向的判斷時點**：提交初審時不再判斷品質線，案件一律停在 `INITIAL_REVIEW_COMPLETED`。固定成功時才依 D-031 判斷：分數 > 品質線就進 `PENDING_REREVIEW`；≤ 品質線或品質線為 null，留在 `INITIAL_REVIEW_COMPLETED`，等管理者手工派複審或結案。用來判斷的分數是被固定那份提交的分數。
4. **回應**：`{ caseId, caseStatus, submissionId, fixedAt }`。不回傳品質線。
5. **固定之後**：初審 revision 拒絕；管理者手工派複審與不經複審結案，都要求初審已固定。
6. **錯誤代碼**：全部沿用既有代碼，不新增。檢查順序依 D-024：角色 → 載入 → 院區 → 領域規則。

   | 情況 | 代碼 |
   |---|---|
   | 不是 `REVIEWER` | `ROLE_DENIED` |
   | 案件或提交不存在 | `CASE_NOT_FOUND`／`SUBMISSION_NOT_FOUND` |
   | 院區不符 | `HOSPITAL_SCOPE_MISMATCH` |
   | 案件已是 `CLOSED` 或 `CANCELLED` | `CASE_TERMINAL` |
   | 初審已固定（重複固定） | `INVALID_STATE_TRANSITION` |
   | 案件不在 `INITIAL_REVIEW_COMPLETED` | `CASE_NOT_INITIAL_REVIEW_COMPLETED` |
   | 固定目標不屬於本案、不是初審階段，或不是目前有效版本（已被取代） | `CONFLICT` |
   | 固定目標仍是草稿 | `SUBMISSION_NOT_SUBMITTED` |
   | 操作者不是這份有效結果的提交者 | `NOT_SUBMISSION_OWNER` |
   | 已固定後再 revision 初審；未固定就派複審或不經複審結案 | `INVALID_STATE_TRANSITION` |
   | 與 revision 同時寫入，樂觀鎖失敗 | `CONFLICT`（D-045 第 3 點） |

7. **重複固定**：回 `INVALID_STATE_TRANSITION`，不視為成功，不改變已固定的內容，也不再次推進流向。前端收到時重新整理畫面。
8. **事件與稽核**：固定成功發布初審固定事件，稽核紀錄記下操作者、時間與 `submissionId`，不記分數。分數 > 品質線時另外發布既有的進複審事件。
9. **前端**：案件詳情的 `allowedActions` 在「案件為 `INITIAL_REVIEW_COMPLETED`、尚未固定、登入者是目前有效初審結果的提交者」時加入 `FIX_INITIAL_REVIEW`；初審頁依此顯示「固定」按鈕。

### Relationship / Evidence

- 補足 D-031（method、API、狀態／儲存表示）與 DD-03 Contract Boundary 列出的 UNKNOWN（binding、request／response schema、錯誤細碼、重複固定、狀態／儲存表示、鎖定、既有資料遷移）。D-031、DD-03 的業務規則不變。
- DD-03 Contract Boundary 寫的「不新增 enum、aggregate、endpoint 或 DB schema」，原意是該輪只定業務規則、不決定技術契約（同段把狀態／儲存表示列為後續定案的 UNKNOWN）。本決策新增一個 endpoint 與三個案件欄位，不新增 enum 與 aggregate，部分取代該句。
- 第 3 點改變現行程式：`SubmitInitialReviewUseCase` 目前在提交時就依品質線進複審（D-019 的舊行為），與 D-031 不符，實作時一併修正。
- 第 6 點的代碼都已在現有初審、複審、結案的程式中使用。目標不是目前有效版本時回 `CONFLICT`，與 D-053 第 5 點相同。
- 現行只有記憶體資料庫，沒有既有資料需要轉換。
- 固定與 revision 真正同時發生的競爭，要等 D-045 的版本欄位落地才能完全擋住；在那之前依 `submissionId` 檢查是否仍是目前有效版本。

### Mapping

`docs/03-contracts/api-spec.md` 的「DD-03 初審固定的語意契約」；`docs/02-ddd/state-machine.md` 的初審提交與固定轉移；`docs/03-contracts/persistence-model.md` 的「DD-03 固定持久化與併發保證」。


---

## D-055 — 以複審指派歷史證明「不是歷史複審醫師」（補足 D-035）

### Status

`accepted`（2026-11-16，依使用者裁決：方案 A，專用的複審指派歷史）

### Decision

補足 D-035 第 2 點「現有來源尚不能產生可信的 Proven non-historical」：

1. **複審指派歷史**：新增只能新增、不能修改或刪除的紀錄，每次指派或改派複審醫師各一筆，內容為案件、複審醫師、指派時間、操作者、類別（指派／改派）。與案件的指派變更在同一個交易寫入；任何一方失敗，整筆回滾。
2. **完整性**：案件上記一個「複審指派歷史完整」旗標。導入這份歷史之後建立的案件為完整；導入之前已存在的案件為不完整。正式環境在導入前沒有任何資料（D-045：正式資料庫自 Flyway V1 起，尚未上線），所以正式資料全部是完整的。不得事後把不完整的案件改為完整，也不得補造歷史紀錄。
3. **判斷規則**（`ReviewReadAccessPolicy`）：
   - 登入者是目前的複審醫師，或在歷史中出現過 → Proven historical（與現行相同，拒絕完整初審讀取）。
   - 案件歷史完整，且登入者不在歷史中 → Proven non-historical，可在既有 role／scope／ownership 範圍內完整讀取。
   - 案件歷史不完整 → UNKNOWN，拒絕（與現行相同）。
   - 不再以稽核紀錄判斷身份；稽核紀錄維持原用途。
4. **寫入者**：目前只有指派複審醫師、改派複審醫師兩個 use case 會變更複審醫師，兩者都必須寫入歷史。之後新增任何會變更複審醫師的入口，也必須寫入；以測試確保每一個寫入點都有對應的歷史紀錄。
5. **初審讀取者紀錄**（2026-11-16 依使用者裁決補上，回應 PR #54 審查）：D-035 原本全面拒絕，等於附帶保證「複審醫師不可能事先讀過初審」。開放讀取後，必須另外擋住「先讀過完整初審、之後才被指派或改派為同案複審醫師」的情況，否則違反 D-033，而且事後無法補救。
   - 每次准許某人讀取某案的完整初審內容（案件詳情、初審提交明細、revision 歷程中未遮罩的初審內容），就新增一筆只增不改的「初審讀取者」紀錄（案件、讀取者、時間），必須在回應送出前 commit；寫入失敗就拒絕這次讀取（fail-closed）。同一人同一案只需一筆。
   - 指派或改派複審醫師時，若對象在該案的初審讀取者紀錄中，回 `422 REREVIEWER_HAS_READ_INITIAL`（新增的錯誤代碼），與既有的 `REREVIEWER_SAME_AS_REVIEWER` 並列。
   - 讀取與指派要互斥：讀取寫入紀錄時一併以 D-045 的案件版本檢查（強制遞增版本，不推進 updatedAt，因為讀取不是業務更新），與同時進行的指派或改派只能有一方成功。
6. **匯出的逐案分數**（2026-11-16 依使用者裁決補上，回應 PR #54 第二次審查）：`POST /reports/export` 逐案輸出初審分數與 finalScore，卻不經過第 5 點的讀取者紀錄，可以用來繞過指派限制。處理方式是遮罩，不記錄讀取者：
   - 複審指派與改派只能在 `PENDING_REREVIEW` 進行；案件進入 `REREVIEW_IN_PROGRESS`、`CLOSED` 或 `CANCELLED` 之後，就不可能再有新的複審醫師。
   - 下列任一情況，匯出的逐案 `initialScore`、`finalScore`（以及之後新增的任何可反推初審分數的逐案值）一律輸出 `null`，並以 maskedFields 標示：
     - 案件狀態還不是這三個之一；
     - 匯出者是該案目前的複審醫師，或出現在該案的複審指派歷史中（D-033 結案後也不解除）；
     - 該案的複審指派歷史不完整（無法證明匯出者不是歷史複審醫師，依 D-035 fail-closed）。
   - 以上都不成立時照常輸出。
   - 匯出不寫入讀取者紀錄，也不遞增案件版本。要看尚未進入複審的案件分數，請從案件詳情讀取（會留讀取者紀錄）。
   - SYSTEM_ADMIN 依既有的 metadata／aggregated-only 政策（D-033、api-spec 15.2），任何狀態都不取得逐案分數；目前程式會輸出，由建構 PR 一併修正。
   - 之後新增任何會輸出逐案初審分數或可反推值的出口，必須擇一套用第 5 點（記錄讀取者）或本點（遮罩）。
7. **不變的部分**：D-033 永久盲審；REREVIEWER 只讀本人複審內容、初審永遠遮罩；SYSTEM_ADMIN 只看 metadata；D-024 院區範圍；多角色同時持有時仍拒絕（未裁決，維持 D-035 第 5 點）；錯誤回應仍為 403 `ACCESS_DENIED`。

### Relationship / Evidence

- 部分取代 D-035 第 2 點的「現有來源尚不能產生可信的 Proven non-historical」與「Consequences」中「受保護的完整讀取可能全面遭拒」：歷史完整的案件可以證明負面身份。D-035 的 fail-closed 原則（UNKNOWN 一律拒絕、不得默默降級、不得補造完整性）不變。
- 影響：HOSPITAL_MANAGER、AUDITOR、REVIEWER（本案指派或提交的初審醫師）在案件歷史完整時恢復完整讀取，因此初審頁（含 D-054 的 FIX_INITIAL_REVIEW）與管理者明細可以使用。
- 第 2 點的「不得補造」依據 D-035 第 2 點；完整性來自「從案件建立起每一次指派都被記錄」，不是事後宣告。
- 第 6 點只改匯出的輸出內容，不改匯出的權限、院區範圍與統計欄位；統計摘要（`DashboardSummary`）只有件數與比率，不含分數，不受影響。
- 第 5 點新增錯誤代碼 `REREVIEWER_HAS_READ_INITIAL`（422），其餘錯誤代碼沿用。讀取會產生寫入（讀取者紀錄），屬於醫療資訊系統常見的存取紀錄；紀錄只用於指派限制，不輸出給一般 API。
- 這份歷史之後也可以用於生命週期查詢的讀取範圍（2026-11-16 決定暫緩，待需要時裁決），本決策不包含該項。

### Mapping

`docs/02-ddd/permission-matrix.md` 與 `docs/03-contracts/api-spec.md` 的「D-035 歷史身份 Fail-closed 讀取契約」、api-spec 的指派／改派複審醫師與錯誤代碼表；`docs/03-contracts/persistence-model.md`；實作以建構 PR 落地（Flyway migration、指派兩個 use case、`ReviewReadAccessPolicy`、三個完整讀取入口的讀取者紀錄、匯出逐案分數遮罩）。

---

## D-056 — 匯出請求的格式以程式為準

### Status

`accepted`（2026-11-17，依負責人裁決）

### Decision

1. `POST /reports/export` 的請求條件直接放在最外層：`reportType`（必填）、`hospitalId`、`from`、`to`，不包在 `filters` 物件中。api-spec 15.2 依此修正，與程式的 `ExportRequest` 一致。
2. 院區範圍不變，依 D-042、D-047。
3. `from`、`to` 的現況如實記載：程式會接收，但逐案資料列與統計摘要都沒有依時間過濾。要依哪個時間欄位過濾（例如抽選時間、指派時間或結案時間）尚未裁決，本決策不包含。（已由 D-057 取代：以案件建立時間過濾、必填、分頁。）

### Relationship / Evidence

- 落差在 PR #57 審閱時發現：api-spec 15.2 的請求範例使用 `filters` 物件，程式則是最外層欄位（`ExportRequest`）；`ExportQueryService.queryRows` 與 `DashboardQueryService` 都只使用院區與專案條件。
- 只修正文件，不改程式行為。

### Mapping

`docs/03-contracts/api-spec.md` 15.2「匯出統計報表」的 Request。

---

## D-057 — 匯出依案件建立時間過濾、時間區間必填、逐案資料列分頁

### Status

`accepted`（2026-11-17，依負責人裁決：(1) 以案件建立時間過濾；(2) `from`、`to` 改為必填；(3) 改用分頁。細節採建議）

### Decision

1. **時間欄位**：以案件建立時間 `review_case.created_at` 過濾。案件建立（抽選產生案件）時寫入，UTC 時間點，之後不得修改。此欄位以 Flyway 新增（D-045 第 6 點），可為 NULL；導入前已存在的案件為 NULL，不回填，也不會落在任何時間區間內（正式環境導入前沒有資料，D-045）。
2. **區間必填**：`POST /reports/export` 的 `from`、`to` 改為必填，格式為 ISO-8601 時間點。區間為含起不含迄（`from` ≤ `created_at` < `to`），相鄰期間不會重複計算。缺少任一個，或 `from` 不早於 `to`，回 `400 BAD_REQUEST`。
3. **分頁**：逐案資料列改為分頁，取代原本最多 10,000 筆的隱性截斷。
   - 請求加 `page`（0 起算，預設 0）與 `size`（預設 100，上限 1,000；超過以 1,000 計）；回應加 `page`、`size`（實際使用的值）、`totalElements`、`totalPages`。
   - 非正值比照現有列表 API 的處理方式正規化，不回錯誤：`page` 小於 0 以 0 計；`size` 小於或等於 0 以預設值 100 計。
   - 排序固定為 `caseId` 遞增，分頁結果穩定。
   - 統計摘要 `summary` 以整個過濾範圍計算，不受分頁影響，與 `totalElements` 對應同一個母體。
4. **統計摘要與儀表板**：匯出的 `summary` 套用同一個時間區間。`GET /reports/dashboard-summary` 的 `from`、`to` 維持選填；兩個都給時套用同一個規則（含起不含迄，`from` 不早於 `to` 回 `400 BAD_REQUEST`），只給其中一個回 `400 BAD_REQUEST`，都不給時不依時間過濾（與現行相同）。
5. **逐案資料列**的 `createdAt` 改為案件建立時間（原本一律為 `null`）。
6. **不變**：院區範圍（D-042、D-047）、逐案分數遮罩（D-055 第 6 點，以每一列判斷）、`reportType` 只支援 `REVIEW_COMPLETION`。

### Relationship / Evidence

- 取代 D-056 第 3 點（「`from`、`to` 會接收但沒有作用、依哪個時間欄位過濾尚未裁決」）；D-056 第 1、2 點不變。
- 原因（PR #61 之後的分析）：不依時間過濾時，報表的完成率、逾期率會變成歷年累計而使用者無從察覺；資料超過 10,000 筆後逐案資料列被靜默截斷、與統計摘要不一致；每次匯出都帶出全院歷年的每案醫師代號與分數，不符合資料最小化。
- UI-017 報表匯出頁的主要內容已列「日期區間」；`persistence-model.md` 的 `review_case` 已列 `created_at`，本決策將其落地。
- 第 3、4 點的預設值（`size` 預設 100、上限 1,000；儀表板只給一端回 400）是建議值，與現有列表 API 的分頁形狀（`page`、`size`、`totalElements`、`totalPages`）一致，上限放寬是因為匯出的用途是批次取資料。

### Mapping

`docs/03-contracts/api-spec.md` 15.1、15.2；`docs/03-contracts/persistence-model.md` 5.12；實作以建構 PR 落地（V6 migration、案件建立時寫入時間、匯出與儀表板的過濾與驗證、分頁）。

---

## D-058 — API 路徑以程式為準：修正 D-022 的路徑樣式

### Status

`accepted`（2026-11-18，依負責人裁決：採 `docs/review/api-spec-path-reconciliation-2026-11-18.md` 第五節的選項 A）

### Decision

1. **路徑樣式。** Review workflow 的命令與查詢以案件為根、stage 在前：
   - 對案件「目前」的提交或 submission 下命令：`/review-cases/{caseId}/{stage}/{action}`，例如 `POST /review-cases/{caseId}/initial-review/submit`。
   - 對特定 submission 下命令：`/review-cases/{caseId}/{stage}/submissions/{submissionId}/{action}`，例如 `.../initial-review/submissions/{submissionId}/revise`、`.../rereview/submissions/{submissionId}/close`（D-053）、`.../initial-review/submissions/{submissionId}/fix`（D-054）。
   - command 一律使用 `POST`（沿用 D-022）。
2. **提交命令的 `submissionId`。**
   - 初審提交 `POST /review-cases/{caseId}/initial-review/submit`：路徑與 body 都不帶 `submissionId`，由案件取得目前的 submission。
   - 複審提交 `POST /review-cases/{caseId}/rereview/submit`：路徑不帶，request body 的 `submissionId` 為必填（`SubmitRereviewRequest`，`@NotBlank`），用來確認提交的是目前版本。
3. **submission 詳情。** 保留 `GET /review-submissions/{submissionId}`，撤銷 D-022 對它的廢除。院區與角色檢查經由 submission 所屬的案件進行（`GetSubmissionDetailUseCase` 以所屬案件呼叫 `ReviewReadAccessPolicy.requireAccess`），D-022「以 caseId 強化院區驗證」的目的由此滿足。
4. **api-spec 依程式改寫的 9 支。** 8.3 `POST /score-sheets/{scoreSheetId}/publish`、10.8 `POST /review-cases/{caseId}/initial-review/return`、11.3、11.4、12.1 `POST /review-cases/{caseId}/start-rereview`、12.3、12.4、13.1、13.2 `GET /review-cases/{caseId}/revision-history`。只改路徑，以及第 2 點的 `submissionId` 位置與複審提交的 request body；各節的其他內容（權限、限制、其他欄位）維持原文，本決策沒有重新核對。
5. **api-spec 補上 5 支只存在程式的端點，以程式為準。**（連同第 6 點的 2 支，共補上 7 支。）
   - `POST /review-cases/{caseId}/close-without-rereview`、`POST /review-cases/{caseId}/send-to-rereview`：限 `HOSPITAL_MANAGER`，本院；案件須為 `INITIAL_REVIEW_COMPLETED` 且初審已固定（D-031）。
   - `POST /review-cases/{caseId}/reopen`：限 `HOSPITAL_MANAGER`，本院；`RETURNED` → `PENDING_INITIAL_REVIEW`。只用於退回初審之後，不是重開 `CLOSED` 案件（D-005、D-032 不受影響）。
   - `GET /sampling-batches/{batchId}/export`：限 `HOSPITAL_MANAGER`，本院（依所屬專案的院區）。
   - `GET /score-sheets/{scoreSheetId}/version`：角色與院區依 D-050 第 7 點。
6. **評分表列表與章節查詢，依 D-050 第 7 點，不以程式為準。** `GET /score-sheets`、`GET /score-sheets/{scoreSheetId}/sections` 的路徑以程式為準，但回應依 D-050 第 7 點：列表回傳 `{ items }`、章節回傳 `{ scoreSheetId, sections }`。現行程式直接回傳陣列，與 D-050 不一致，列為待修的程式缺口，由後續的建構 PR 處理，本決策不改程式。（已由 #85 修正：列表回傳 `{ items }`、章節回傳 `{ scoreSheetId, sections }`，見 `ScoreSheetController`；2026-11-19 更正本點的現況描述，裁決內容不變。）
7. **不納入本決策：`GET /review-cases/{caseId}/rereview-revisions`。** `GetRereviewRevisionHistoryUseCase` 沒有角色檢查（程式註解寫「Any authenticated role can query — visibility enforced at API/UI layer」，但 API 層沒有檢查），院區範圍內的任何角色都能讀到複審的 `totalScore` 與 `comment`，包括只應取得 metadata 的 `SYSTEM_ADMIN`。這是權限缺口，不能以程式為準寫進 api-spec；是否保留這支端點、該開放哪些角色，需另行裁決。目前沒有前端使用。（已由 D-059 裁決：移除。）

### Relationship / Evidence

- 差異的來源與比對方法見 `docs/review/api-spec-path-reconciliation-2026-11-18.md`（PR #76）：以腳本比對 api-spec、後端 controller、前端 service，前端呼叫的 27 個端點後端都有。
- D-022 的理由「實作已以 case-rooted paths 通過測試」與現在的實作不符；之後的 D-053、D-054 已採用 stage 在前的樣式。本決策讓裁決、api-spec 與程式一致，做法與 D-056「匯出請求格式以程式為準」相同。
- 第 6 點：D-050 的位階高於程式現況，所以 api-spec 依 D-050；D-050 的 Relationship 本來就記載「現行程式與本決策的差異……尚未修改」。
- 第 7 點：2026-11-12 的現況快照已列出「`GetRereviewRevisionHistoryUseCase` 沒有角色檢查」，本次確認仍成立。

### Mapping

- `docs/03-contracts/api-spec.md`：8.3、10.8、11.3、11.4、12.1、12.3、12.4、13.1、13.2，以及新增的 8.5～8.7、9.7、10.9～10.11。
- 同步路徑字串（只改路徑與參數欄）：
  - `docs/04-testing/test-scenarios.md`、`docs/04-testing/specification-by-example.md`
  - `docs/05-ui/ui-page-inventory.md`、UI-010、UI-012、UI-013、UI-014 頁面規格
  - `docs/06-frontend/ui-api-binding-matrix.md`、`frontend-design-roadmap.md`、`ui-view-model-spec.md`
- 更新路徑樣式的規則：`docs/06-frontend/frontend-test-scenarios.md` 的 G-01／G-02、`docs/07-design/ui-design-decisions.md` 的 O2-A；`docs/06-frontend/frontend-review-gate-pending-items.md` 的 O2 分析加註以 D-058 為準。
- 不改：`docs/superpowers/`、`docs/review/` 的舊計畫與報告（歷史紀錄）；`docs/06-frontend/` 的 `ready-pages-*`、`wave-backend-dependency-inventory.md`、`sampling-route-module-map-addendum.md` 等規劃與交接文件，其中多數路徑是當時提議、程式從未實作的端點，不屬於本次的路徑差異；UI-014 §11.3「可後補」的 compare API。
- 程式不變；第 6 點的回應格式與第 7 點的權限缺口由後續 PR 處理。（第 6 點已由 #85 處理，第 7 點已由 D-059 處理。）

---

## D-059 — 移除 `GET /review-cases/{caseId}/rereview-revisions`

### Status

`accepted`（2026-11-18，依負責人裁決：移除。處理 D-058 第 7 點）

### Decision

1. **移除端點。** 刪除 `GET /review-cases/{caseId}/rereview-revisions` 與其 use case（`GetRereviewRevisionHistoryUseCase`、`GetRereviewRevisionHistoryQuery`）。不提供轉址或別名；移除後呼叫該路徑視為不存在的資源。
2. **複審歷程一律經 `revision-history`。** `GET /review-cases/{caseId}/revision-history`（api-spec 13.2）已回傳初審與複審兩個階段的歷程，並經 `ReviewReadAccessPolicy` 檢查：角色、院區、指派範圍、多重角色拒絕、歷史複審醫師排除與讀取紀錄（D-055 第 5 點）、內容遮罩（`SYSTEM_ADMIN` 只取得 metadata；REREVIEWER 只看得到自己的複審內容）。
3. **以測試鎖定 `revision-history` 對複審資料的保護。** 建構 PR 須包含下列測試，且移除前先確認它們成立：
   - `SYSTEM_ADMIN` 讀到的複審 `totalScore`、`comment` 為 `null`（D-010）。
   - 同院、未被指派到本案的 REVIEWER 被拒絕。
   - 同院、不是本案複審醫師的 REREVIEWER 被拒絕。
   - 多重角色帳號被拒絕。
4. **不在本決策範圍。** `revision-history` 的 `hasExceptionEntry` 仍依 D-021「CLOSED 後原複審醫師可 revision」的例外計算，該例外已被 D-032 撤銷；如何處理此旗標另行決定。

### Relationship / Evidence

- **缺口**：`GetRereviewRevisionHistoryUseCase` 只檢查院區（`requireReadScope`），程式註解寫「Any authenticated role can query — visibility enforced at API/UI layer」，但 API 層沒有檢查；前端也沒有使用這支端點。
- **證據（2026-11-18，本機以 JDK 17 執行，測試未提交）**：以第 3 點的四個情境為預期，
  - 對 `rereview-revisions`：4 個全部失敗。`SYSTEM_ADMIN` 取得複審分數 3（預期 `null`）；未指派的 REVIEWER、其他 REREVIEWER、多重角色帳號都沒有被拒絕。
  - 對 `revision-history`：4 個全部通過。
- **來源**：B4 設計（`docs/superpowers/specs/2026-04-10-b4-rereview-operations-design.md` §6.9）要求「Apply visibility rules per permission matrix」，實作沒有做。B3c 設計的 `GetRevisionHistoryUseCase` 從第一版起就讀出案件所有階段的 submission，複審歷程一直是它的子集。2026-11-09（D-035、D-036）與 2026-11-16（D-055）的權限加固只套用在 `revision-history`；2026-11-12（D-042、D-047）只為 `rereview-revisions` 補了院區檢查。
- **權限矩陣**只有 `viewRevisionHistory (UI-014)` 一列，沒有複審歷程的獨立權限，這支端點的存取規則沒有出處。
- 另立一支複審專用端點的原因，設計文件沒有記載；若是為了 D-021 的 CLOSED 複審 revision 例外，該例外已被 D-032 撤銷。

### Mapping

- `docs/03-contracts/api-spec.md` 13.2：說明改為「已依 D-059 移除」。
- `docs/harness/05-current-state.md` 第五節第 4a 項、第七節第 1 項。
- 程式由後續建構 PR 落地：先寫「該路徑已不存在」與第 3 點的測試，再刪除 controller 方法、use case、query 與其測試，並更新 `RereviewApiTest`、`ResourceNotFoundErrorCodeApiTest` 中對這支端點的呼叫。

---

## D-060 — 未立法的問題側先停下來：問題側涵蓋清單

### Status

`accepted`（2026-11-18，依負責人裁決：採「擋新增」；問題側包含前端規格，但先完成後端 harness，前端之後再納入）

### Decision

1. **問題側涵蓋清單。** 新增 `law/coverage.txt`（屬於 `law/`，類別是 gate）。清單把問題側的程式路徑對應到兩種狀態：
   - `legislated`（已立法）：指向 `specs/` 底下的規格目錄，規格目錄必須存在。
   - `grandfathered`（未立法但暫准）：本裁決生效時已存在、但沒有可執行律法的範圍。
2. **問題側的邊界。** 目前只含後端：`domain`、`application`、`interfaces`（HTTP 契約）、`reporting`。`infrastructure` 與建置設定不算。前端頁面行為與前端規格也屬於問題側，但依負責人裁決，待後端 harness 完成後另行納入，納入時另立裁決或修訂本裁決。
3. **判決（擋新增）。** `law-check` 對每個 PR 判斷問題側裡新增或修改的檔案：
   - 符合 `legislated` 的範圍：照常判決。
   - 符合 `grandfathered` 的範圍：通過，但報告列為「未立法範圍的修改」。在 GitHub 上另外顯示為警告。
   - 不符合任何一行：**未通過**。這是未立法的新範圍，要先立法，或以修法把它列進清單（需要簽章）。
   - 刪除程式不擋。清單一律讀合併目標分支（base）上的版本，PR 不能在同一個 PR 裡替自己的新範圍開門。
4. **給 AI 的指示。** 動到 `grandfathered` 或清單以外的問題側之前，AI 先停下來，向負責人說明「這裡沒有可執行的律法」，並請負責人決定：先立法、以修法列為暫准，或在暫准範圍內照現有權威文件實作。寫在 `AGENTS.md` 與 `CLAUDE.md`。
5. **暫准範圍只減不增。** 每立法一個 aggregate，就把它從 `grandfathered` 改為 `legislated`。把新範圍加為 `grandfathered` 需要修法紀錄與簽章，並在紀錄的 `reason` 說明為什麼不先立法。

### Relationship / Evidence

- **起因**：2026-11-18 發現的 3 個漏網缺陷（`docs/harness/12-escaped-defects.md` E-01～E-03）都落在可執行律法照不到的地方。原本計畫先拿少量 aggregate 驗證 harness，之後的實作逐漸延伸到未立法的範圍，但沒有任何機制讓這件事被注意到。
- **為什麼不直接擋所有未立法範圍**：生效時問題側共 256 個檔案，已立法只有 50 個（review-project、score-sheet），全擋會讓所有工作停擺。做法與 ArchUnit 的凍結基線相同：既有的列為暫准，只擋新增。
- **擋不到的**：
  - 已立法 aggregate 的介面層缺口：範例只執行到 use case 層（E-03）。為了讓提醒照得到，這些 aggregate 的 controller 與 dto 列為暫准。
  - 修改裁決後，暫准範圍內的程式漂移（E-02）。這要靠擴大立法範圍。
  - 暫准範圍內的新增檔案只提醒、不擋。
- **實作**：判決程式 `harness-core/law_check.py` 0.4.0，以清冊的 `@coverage` 指示啟用；測試在 `harness-core/tests/test_law_check.py` 的 `CoverageTest`（13 個）。

### Mapping

- `law/coverage.txt`、`law/manifest.txt`（`@coverage`）、`law/README.md`
- `harness-core/law_check.py`、`harness-core/tests/test_law_check.py`、`harness-core/README.md`、`harness-core/VERSION`
- `AGENTS.md`、`CLAUDE.md`
- `docs/harness/03-harness-plan.md` 第六節、`docs/harness/12-escaped-defects.md`、`docs/harness/05-current-state.md` 第七節

---

## D-061 — HTTP 契約升格為律法：觀察式比對與閘門

### Status

`accepted`（2026-11-18，依負責人裁決：HTTP 契約採選項 B「另立機器可讀的契約」、比對採 B-1「觀察」、比對器放在 `acceptance/`；草案的 Q-1～Q-6 全部採建議選項。草案與裁決紀錄見 `docs/harness/14-d061-draft.md`）

### Decision

1. **契約的位置。** 共用層（回應外殼、錯誤分類對應的狀態碼、不屬於任何 use case 的錯誤代碼）放在 `specs/http-common.yaml`，跨 aggregate 只寫一次。aggregate 層（端點與回應的 data 形狀）放在 `specs/<aggregate>/http.yaml`。端點屬於它呼叫的 use case 所在的規格目錄，不看 URL 前綴。
2. **格式。** 規格格式 v1 第三節規則 6（不出現實作詞彙）只適用於 `model.yaml`、`usecases.yaml`、`examples.yaml`。`http.yaml` 與 `http-common.yaml` 用來寫 HTTP 契約，可以出現路徑與狀態碼。格式寫在 `specs/README.md` 第七節，由 `acceptance/http/` 的 `HttpContractFilesTest` 檢查，不重複寫進 `spec_lint`。
3. **比對方式（B-1 觀察）。** 測試期間經過 MockMvc 的每一個回應，都用契約比對：外殼、成功狀態碼、data 的形狀（物件是封閉的）、錯誤代碼的分類對應的狀態碼；本文必須是單一 JSON 文件。涵蓋的要求：
   - 每個端點至少觀察到一次成功回應。
   - 每個陣列至少觀察到一次非空。
   - data 形狀裡可為 null 的欄位，null 與非 null 都要觀察到（W-7）。共用層的外殼沒有可為 null 的欄位：錯誤回應的 `message` 一律要有值（api-spec §3.5）。
4. **閘門。** `check` 的 test 閘門在下列任一情況未通過：
   - `specs/` 的契約有任何問題：載入問題、回應不符，以及完整執行時的涵蓋不足。有篩選條件的執行只判每一筆回應，涵蓋不判。
   - 完整執行時，沒有本次執行產生的報告。報告第一行是 Gradle 這次給的執行識別碼，所以觀察器被關掉、或只留下舊報告，都算未通過。
   - `spec-draft/` 的契約只列出、不擋。`build.gradle` 把 `specs/` 與 `spec-draft/` 都宣告為 test 的輸入。
   - 本裁決生效時立刻擋（Q-6）：生效時現行程式的報告問題為 0，沒有要凍結的已知失敗。
5. **涵蓋清單。** 下列範圍由暫准改為已立法：
   - `dto/ApiResponse.java`、`GlobalExceptionHandler.java`，指向 `specs/http-common.yaml`。
   - score-sheet 的 5 個回應 dto，指向 `specs/score-sheet/`。

   修訂 D-060 第 1 點：`legislated` 可以指向規格目錄，也可以指向規格檔案（`law_check.py` 0.5.0）。
6. **不在本裁決範圍內的。**
   - **請求端**（Q-3）：`ScoreSheetController` 與請求 dto 仍列為暫准。請求端之後與律法擁有的 HTTP 請求（W-10）一起設計。
   - **操作者的傳遞**：`ActorContextArgumentResolver`、`CurrentActor` 不在契約內，仍列為暫准。
   - **其他 aggregate**（Q-1）：本裁決只做 score-sheet。review-project 另以修法補上 `http.yaml`，不必另立裁決。
7. **401。** 未認證一律回 401 與 `UNAUTHORIZED`（D-023、api-spec §4.1），缺少與無效的差別只寫在 `message`（Q-5，#96 已修正程式）。
8. **暫准只減不增**（D-060 第 5 點）。之後每立法一個 aggregate，就同時寫它的 `http.yaml`，並把它的回應 dto 改為已立法。

### Relationship / Evidence

- **起因**：E-03（`docs/harness/12-escaped-defects.md`）。score-sheet 已立法，範例全過，回應形狀卻不符 D-050 第 7 點達 7 個月。D-060 只能提醒，擋不到。
- **試做**：#93、#95（`docs/harness/13-http-contract-options.md` 第九節）：
  - 把 #85 之前的寫法放回去，列出 6 個問題。
  - 不打 HTTP 時，列出 7 個端點都沒有成功觀察。
  - 現行程式的報告問題為 0。
- **生效前的準備**：
  - #96：401 改回 `UNAUTHORIZED`。試做的報告在修正前就列出 `MISSING_API_KEY`、`INVALID_TOKEN` 沒有定義。
  - #97：補上建構者測試，讓 `scoreSheetVersionId`、`conditionExpression` 兩種值都被觀察到。
- **擋不到的**：
  - 不經過 MockMvc 的呼叫：會判為沒有觀察。
  - 值的正確性：由 use case 範例負責。
  - 請求端（W-9）、律法擁有的 HTTP 請求（W-10）。

### Mapping

- `specs/http-common.yaml`、`specs/score-sheet/http.yaml`、`specs/README.md`、`spec-draft/README.md`
- `services/quality-inspection-service/src/test/java/com/tzuchi/his/quality/inspection/acceptance/http/`、`services/quality-inspection-service/build.gradle`
- `law/coverage.txt`、`harness-core/law_check.py`、`harness-core/tests/test_law_check.py`、`harness-core/README.md`、`harness-core/VERSION`
- `docs/harness/13-http-contract-options.md`、`docs/harness/14-d061-draft.md`、`docs/harness/12-escaped-defects.md`

---

## D-062 — 專案列表與版本查詢：回應形狀與讀取角色

### Status

`accepted`（2026-11-19，依負責人裁決：列表改成 `{ items }`；查詢參數只寫 `hospitalId`；版本查詢加開 `AUDITOR`）

### Decision

1. **列表回傳 `{ items }`。** `GET /review-projects` 的 `data` 是 `{ items }`，每一項的欄位與專案詳情相同。與評分表列表（D-050 第 7 點）一致。
2. **列表的查詢參數只有 `hospitalId`。** 只有 `SYSTEM_ADMIN` 可用來縮小範圍；其他角色只列出自己院區的專案，輸入的 `hospitalId` 不採用（D-042、D-047）。api-spec §7.7 的 `status`、`keyword`、`page`、`size` 尚未實作，不寫進規格與契約，另立題目。
3. **版本查詢的讀取角色。** `GET /review-projects/{projectId}/version` 的讀取角色為 `SYSTEM_ADMIN`、`HOSPITAL_MANAGER`、`AUDITOR`，院區範圍依 D-042、D-047；`REVIEWER`／`REREVIEWER` 不開放（D-051）。與專案詳情（D-043）、評分表的版本查詢（D-050 第 7 點）一致。現行程式拒絕 `AUDITOR`，是程式缺口。
4. **版本查詢的輸出。** 規格的輸出依 `model.yaml` 的 `ProjectVersion`：`versionNo`、`published`。HTTP 回應的欄位名稱在補上 `http.yaml` 時決定。（負責人 2026-11-19 決定：HTTP 欄位維持程式現況 `versionNumber`、`published`，前端目前沒有使用，改名沒有效益。）
5. **順序。** 先以本修法寫入規格與範例（一開始紅的範例凍結進已知失敗清單），再以建構 PR 修改程式，最後以修法補上 `specs/review-project/http.yaml` 的兩個端點。列表的契約若在程式修改前寫入，D-061 的閘門會立刻擋下。

### Relationship / Evidence

- 起因：D-061 第 6 點補上 review-project 的 `http.yaml` 時，這兩支端點的 use case 不在 `usecases.yaml`，負責人 2026-11-19 決定另立題目（#100）。
- 現行程式：`ReviewProjectController` 的列表直接回傳陣列；`GetReviewProjectVersionUseCase` 只允許 `SYSTEM_ADMIN`、`HOSPITAL_MANAGER`，回傳的 `ProjectVersion` 屬性名稱是 `versionNumber`。
- 前端目前沒有呼叫這兩支端點（只有 sampling 使用 `/review-projects` 底下的路徑），改形狀不影響前端。

### Mapping

- `specs/review-project/usecases.yaml`：`ListReviewProjects`、`GetReviewProjectVersion`
- `specs/review-project/examples.yaml`、`services/quality-inspection-service/known-test-failures.txt`
- `docs/03-contracts/api-spec.md` §7.7、§7.9
- 後續：建構 PR 修改程式；修法補上 `specs/review-project/http.yaml` 與 `law/coverage.txt`

## D-063 — 規格格式 v1.1：封閉結構、分辨不了情境的範例、引用完整性

### Status

`accepted`（2026-11-20，依負責人對 Q-1、Q-2 的裁決：照現狀補完整的欄位表，另標出待改寫的項目；A 組直接套用 v1 的檔案）

### Decision

1. **適用範圍。**
   - 適用於 `specs/` 與 `--report-only` 草稿的 `model.yaml`、`usecases.yaml`、`examples.yaml`。
   - 檔案仍寫 `spec_version: 1`。
   - `http.yaml`、`http-common.yaml` 不變：它們的形狀已經是封閉的（D-061）。
2. **封閉結構。**
   - `specs/README.md` 第八節為三種檔案補完整的欄位表，列出每一層允許的欄位與型別。
   - 欄位表照現有規格的寫法收錄：現有規格用到的欄位全部列入，不因收錄而改寫任何規格。
   - 表上沒有的欄位、型別不符的值都是問題，擋下。
   - 沒寫的欄位不檢查；寫了但值是 null，也是問題。只有任意值的欄位（範例的輸入與預期輸出、屬性的 `initial`）可以是 null。
   - 任何一層都可以有 `note`。它是自由文字，不參與判決。
   - 欄位表內建在 `harness-core/spec_lint.py`，不讀專案的 README，以保持 `harness-core/` 可攜。README 的欄位表是給人看的同一份內容，由人工維持一致；不一致時以檢查程式為準。
3. **收錄但待改寫的四項。** 這四項列入欄位表並標為「待改寫」。v1.1 照樣接受，在下一次規格格式的修法中改寫：
   1. command 的 `success.output`（清單）與 query 的 `usecases[].output`（帶 `fields` 的物件）合併成一種寫法。
   2. `usecases[].input_note`、`invariants[].enforced_by` 併入 `note`。
   3. `attributes[].range` 改寫成有 id 的規則。它目前沒有任何範例或程式在驗證；範圍的依據待 D-046 確認。
   4. review-project 的 `not_covered` 寫了 `rules: [emits]`，`emits` 不是規則 id。改成 `item: emits`，與 score-sheet 一致。
4. **分辨不了情境的範例。**
   - 範例的 `given` 依 fixture 展開之後，若兩個範例的 `given`、`when` 完全相同：
     - `then` 不同，判為問題，擋下。
     - `then`、`observe` 也相同，只列出：可能是刻意的重複。
   - 比較時忽略 `note`。
   - `${名稱}` 依字面比較。變數名稱不同、實質相同的兩個範例，檢查不出來。
5. **引用完整性。** 下列引用都要核對，不符就擋下：
   - `guard_exceptions[].guard`：必須是同一支 use case `guards` 裡的 id。
   - `transitions[].by`：必須是同目錄 `usecases.yaml` 的 use case。
   - `transitions[].from`、`to`：都要有。
   - 轉移的狀態與 `terminal_states`：必須都是同一個列舉的值。有轉移或 `terminal_states`，就必須有 `enums`。
   - `not_covered[].rules`：等第 3 點第 4 項改寫後才核對。
6. **回歸樣本。** `docs/harness/10-spec-format.md` 第五節的兩份試寫草稿，放進 `harness-core/tests/fixtures/spec-format-trial/` 當測試資料：
   - 嚴格版：v1 判它零問題。v1.1 要以第 4 點擋下它的兩個空洞範例。
   - 擴充版：試寫結果二的前 7 列都要被擋下。
     - 第 1 列「一條規則兩個結果」，v1 已經擋下。
     - 其餘 6 列由第 2 點擋下。
     - 第 8 列（實作詞彙）不在本裁決的範圍，屬於格式 v2 候選第 8 項。
7. **順序。** 本裁決用一個修法 PR 完成：`decision-log.md`、`specs/README.md`、`harness-core/spec_lint.py` 與它的測試。
   - 執行器不動：v1.1 沒有新的語意。
   - 現有規格在這個 PR 上必須維持零問題。若出現問題，就是真的缺陷，另案處理，不在這個 PR 裡放寬欄位表。

### Relationship / Evidence

- **起因**：`docs/harness/10-spec-format.md` 第五節。
  - 試寫的擴充版用了 6 種不存在的欄位，`spec_lint` 一個也沒發現。
  - 嚴格版的兩個範例只差在 `then`，也檢查不出來。
- **盤點**：`docs/harness/15-spec-format-v2-draft.md` 第二節（2026-11-20）。README 描述了規則與範例，但 `model.yaml`、`usecases.yaml` 的結構沒有欄位表。
- **實作時的驗證**（2026-11-20）：
  - 現有兩份規格在 v1.1 下零問題。
  - 嚴格版另外被抓出 4 個 v1 靜默接受的結構錯誤：`enums`、`input` 寫成對應表，`success` 寫成清單。
  - 逐一拿掉三項新檢查，各有測試變紅。
  - #109 的獨立審查指出兩個漏洞：值是 null 的欄位繞過型別檢查；沒寫 `enums` 時，狀態的核對被靜默略過。修正在 #109 合併後才推上，所以另以後續的修法 PR 補上（修法紀錄 `2026-11-20-d063-review-fixes`）。
- **相關裁決**：本裁決是格式 v2 草案的 A 組。B 組（ReviewCase 需要的寫法）、C 組（外部資源協定）待 Q-3～Q-9 裁決後另立。

### Mapping

- `specs/README.md` 第六、八節
- `harness-core/spec_lint.py`、`harness-core/tests/test_spec_lint.py`、`harness-core/tests/fixtures/spec-format-trial/`
- 後續：第 3 點的四項改寫；格式 v2.0（B 組）、v2.1（C 組）另立裁決

## D-064 — 規格格式 v1.2：專案設定檔，harness-core 不再寫死本專案的操作者、錯誤分類與 guard 順序

### Status

`accepted`（2026-11-20，依負責人對格式 v2 草案第十二節三點的裁決：設定檔用 `specs/spec-profile.yaml`；沒有設定檔不給預設值；不核對 `status_by_category`）

### Decision

1. **專案設定檔。**
   - 新增 `specs/spec-profile.yaml`，寫下規格格式裡屬於專案的部分。`spec_lint` 從規格目錄讀它，與 `forbidden-vocabulary.txt` 相同。
   - 這個檔案在 `specs/` 底下，依律法清冊屬於規格類，修改要修法紀錄。
   - 檔案不存在、讀不進來、缺少下面任何一項或內容不合法，`spec_lint` 判為無法評估，不使用預設值。預設值會讓本專案的假設以另一種形式留在 `harness-core/`。
   - 設定檔本身也是封閉結構：`kind: spec-profile`、`profile_version: 1`，加上第 2～4 點的四項。
2. **操作者的欄位（G-07）。**
   - `actor_fields`：`examples.yaml` 的 `actors` 每一項允許的欄位與型別。
   - `usecase_actor_fields`：`usecases.yaml` 的 `actor` 允許的欄位與型別。
   - 型別只有 `str`（字串）、`str_list`（字串清單）兩種；兩項都不可以是空的。
   - 欄位表的其他部分不變，仍然封閉：設定檔沒列的欄位照樣擋下。
   - 本專案照現狀：`actor_fields` 是 `user`、`roles`（字串清單）、`hospital`；`usecase_actor_fields` 是 `roles`（字串清單）。
3. **錯誤分類（G-03）。**
   - `error_categories`：`error_codes[].category` 允許的值，不可以是空的，不可重複。
   - 本專案照現狀列五種：`denied`、`not_found`、`invalid_input`、`conflict`、`rule_violation`。
   - `specs/README.md` 規則 7 改寫成「`category` 只能是設定檔列出的值」；「分類對應 HTTP 狀態碼」的說明移到第七節（HTTP 契約）。
   - 不核對 `http-common.yaml` 的 `status_by_category` 是否涵蓋每一種分類：那是 D-061 的範圍。
4. **guard 的順序（G-04）。**
   - `specs/README.md` 規則 5 改寫成「`guards` 的順序就是檢查的順序」，順序的依據改由設定檔的 `guard_order` 引用，不再寫死 D-024。
   - `guard_order` 有 `text`（必填）與 `source`（`decision` 的清單）。`source` 的裁決必須存在於決策紀錄。
   - `spec_lint` 不檢查順序，與現狀相同；`guard_order` 只給人看。本專案寫「角色 → 載入 → 院區範圍 → 領域規則」，出處 D-024。
5. **不變的部分。**
   - 規格檔仍寫 `spec_version: 1`，現有規格一個字都不改。
   - 執行器（`acceptance/`）不動：它本來就是本專案的程式，依本專案的操作者建立使用者；它只讀含 `examples.yaml` 的目錄，不受新檔案影響。
   - 範例的語法、引用完整性、分辨不了情境的範例，都與 D-063 相同。
6. **驗證。**
   - 現有規格在 v1.2 下維持零問題。
   - 另一個專案的設定檔（操作者 `program`、`user`，分類含 `device_error`）接受自己的寫法，並擋下它沒列的欄位與分類；本專案的設定檔仍然擋下 `program`。
   - 拿掉設定檔、拿掉其中任何一項、或內容不合法，判為無法評估。
   - `harness-core/VERSION` 升為 0.7.0。
7. **順序。** 一個修法 PR：`decision-log.md`、`specs/README.md`、`specs/spec-profile.yaml`、`harness-core/spec_lint.py` 與它的測試、`harness-core/VERSION`、修法紀錄。不夾帶程式（P4）。

### Relationship / Evidence

- **起因**：`docs/harness/15-spec-format-v2-draft.md` 第十一節的缺口二。依 Q-9，卡片規格放在 iccard 的 repo 並安裝 `harness-core/`；G-07 不改，iccard 寫 `program` 就被擋。
- **起草時多查到的一處**：use case 的 `actor` 也寫死只能有 `roles`，一併納入第 2 點。
- **前例**：D-063 第 2 點要求欄位表內建在 `harness-core/` 以保持可攜。本裁決把欄位表裡屬於專案的部分移出來，其餘照舊內建。決策紀錄的位置讀律法清冊的 `@decision-log`、禁用詞彙讀 `specs/forbidden-vocabulary.txt`，都是同樣的做法。
- **實作時的驗證**（2026-11-20）：
  - 現有兩份規格零問題。
  - harness-core 測試 147 個全部通過（需要 PyYAML）。
  - 把錯誤分類改回寫死、或把操作者欄位改回寫死，新的測試各有 2 個變紅。
- **相關裁決**：D-024（guard 順序，內容不變，改由設定檔引用）、D-061（`status_by_category`）、D-063（欄位表）。

### Mapping

- `specs/spec-profile.yaml`、`specs/README.md` 第二、三、六、七、八、九節
- `harness-core/spec_lint.py`、`harness-core/tests/test_spec_lint.py`、`harness-core/VERSION`
- 後續：缺口一的語法（v2.1 前補進格式 v2 草案第四節）；格式 v2.0、v2.1

---

## D-065 — 規格格式 v2.0：輸出斷言、清單路徑、時鐘、矩陣、事件、轉移、跨目錄引用、最小的埠

### Status

`accepted`（2026-11-20，依負責人對格式 v2 草案 Q-6、Q-7、Q-8、Q-10～Q-20 的裁決，以及對 D-065 草案三點的裁決：`local_time_zone` 用 `Asia/Taipei`；現有兩份規格先不升版；`spec_lint` 與執行器分成兩個修法 PR）

### Decision

1. **版本。**
   - 本裁決的寫法只在 `spec_version: 2` 的目錄生效。同一個 `specs/` 裡，版本 1 與 2 的目錄可以並存。
   - 同一個目錄的檔案版本必須相同。
   - 版本 2 的目錄只有 `model.yaml` 是必要的：一個 aggregate 可以只有狀態機，改變它的 use case 定義在別的目錄。
   - 版本 1 的目錄照 D-063、D-064，一個字都不必改，判決結果也不變。現有的 `specs/review-project/`、`specs/score-sheet/` 這次不升版。
   - `http.yaml`、`http-common.yaml` 不變（D-061）。
2. **斷言的欄位。** `then`、`observe` 的 `expect`、具名斷言共用一組欄位：`result`、`code`、`output`、`absent`、`only`、`use_expect`；`then` 另外可以有 `emits`、`emits_include`。
   - `then` 比對 `when` 的輸出。
   - `result` 是 `REJECTED` 時，只能有 `code`。
   - `then.code` 必須定義在 `when` 那支 use case 所在目錄的 `error_codes`。
3. **路徑。** `.`、`a.b`、`a[*]`、`a[k=v]`（只有等於；多個條件用逗號；值可以是 `${名稱}`）、`a[0]`。
   - `output` 的鍵、`absent`、`only` 可以用 `a.b`、`a[*]`、`a[k=v]`；`only` 另外可以用 `.`。
   - `a[0]` 只能用在 `save`；`save` 不能用 `[*]`、`[k=v]`。
   - `[*]`、`[k=v]` 選不到任何一筆時，斷言不成立。
   - `output` 裡直接寫的清單維持版本 1 的語意：筆數相同、一對一配對。
4. **`absent` 與 `only`。**
   - `absent`：選到的屬性不存在或為 null。
   - `only`：選到的每一個物件，值不是 null 的屬性都必須在清單裡。
   - JSON 的鍵是否存在，仍由 HTTP 契約的封閉形狀負責（D-061）。
5. **時間。**
   - 型別詞彙加 `datetime`。範例裡寫 ISO 8601 加時區；沒有時區的時間是問題。比對的是時間點。
   - 範例可以寫 `clock: { start }`；步驟可以寫 `advance`（ISO 8601 期間，只能前進）。
   - `spec-profile.yaml` 的 `profile_version: 2` 多兩項必填：`clock_start`、`local_time_zone`。
   - `profile_version: 1` 仍然接受。但只要有版本 2 的規格而設定檔是版本 1，就判為無法評估，不使用預設值（D-064 第 1 點）。
   - 本專案：`local_time_zone` 是 `Asia/Taipei`，`clock_start` 是 `2026-11-19T17:00:00+08:00`。
   - 執行器提供固定的時鐘：起始值是範例的 `clock.start` 或設定檔的 `clock_start`，時區是 `local_time_zone`。
6. **具名斷言與矩陣。**
   - `examples.yaml` 加 `expectations`：名稱 → 第 2 點的欄位（不含事件）。具名斷言不能再引用具名斷言。
   - 範例可以有 `matrix`：`columns`、`key`、`rows`。`key` 必填。
   - 展開後的名稱是 `<name>.<key 的值，用 . 連接>`。`key` 那幾欄的值只能用英數字與 `_` `-`，組合不得重複。
   - 欄的值代入範例裡的 `${欄名}`。欄名與 `save` 的變數名稱不得相同。
   - 版本 2 的範例名稱在同一個檔案裡不得重複。
7. **事件。**
   - 事件 `X` 對到類別 `XEvent`。斷言的事件必須在 `when` 那支 use case 的 `success.emits` 有宣告。
   - `emits` 是完整的清單，不比順序；`emits_include` 是部分比對。`with` 依屬性名稱部分比對。
   - `then.result` 是 `REJECTED` 時，執行器一律確認沒有發出任何事件。
   - 範例斷言了宣告為 `proposed` 的事件，列為還不能當驗收依據。
8. **轉移。**
   - `transitions` 的每一條必須至少被一個範例的 `basis` 引用，否則擋下。其餘規則仍然只列出。
   - 轉移可以有 `when`。同一個 `from`、同一支 use case 的多條轉移，`when` 必須不同。`from` 可以等於 `to`。
   - 加 `deferred_transitions`：`from`、`to`、`by`、`when`、`note`。`from`、`to` 核對列舉；`by` 已經在規格裡的不得留在這裡。
9. **跨目錄的引用。**
   - fixture、use case、轉移的 `by`、埠，都可以寫 `<目錄>.<名稱>`。型別的列舉名稱也可以這樣寫；檢查程式不核對型別名稱，與版本 1 相同。
   - 別的目錄的 use case 寫 `<目錄>.<名稱>`，不必列在 `needs`。`needs` 只留給還沒立法的 use case。
   - 擋下的規格只能引用擋下的規格；草稿可以引用兩者。
10. **輸出的結構。**
    - 加 `output_types`。命令與查詢的輸出都寫 `output.fields`；`success.output` 不再接受。
    - 欄位對誰可見不寫進結構。
11. **最小的埠。**
    - `model.yaml` 可以宣告 `ports`：`name`、`term`、`operations`（`name`、`returns`）。埠與操作的名稱受禁用詞彙檢查。
    - 步驟可以寫 `port: <埠>.<操作>` 加 `returns`。其他的埠設定與斷言屬於 v2.1。
    - 埠 `X` 對到介面 `XPort`，操作對到同名的方法。只有規格宣告過的埠可以被替換。
    - 範例執行到的埠沒有設定 `returns`，範例未通過。
12. **情境相同的範例（部分取代 D-063 第 4 點）。**
    - 兩個範例的 `given`、`when`（與 `clock`）相同時，`result` 或 `code` 不同才擋下。
    - 其餘的斷言不同，只列出。
    - 版本 1 的目錄仍照 D-063 第 4 點的條文檢查；它的 `then` 只有 `result` 與 `code`，結果相同。
13. **用 use case 建立不出來的前提。** 不寫範例，列在 `not_covered`，由建構者的白箱測試負責。不提供直接寫入資料的寫法，也不把系統自己的儲存宣告成埠。
14. **D-063 第 3 點的四項待改寫。**
    - 版本 2 的目錄不接受 `success.output`、`input_note`、`invariants[].enforced_by`、`attributes[].range`；`not_covered[].rules` 核對是規則 id。
    - 現有的兩份規格仍是版本 1，照舊接受。它們在升到版本 2 時改寫。
15. **回歸樣本。** 格式 v2 草案第二輪的試寫放進 `harness-core/tests/fixtures/spec-format-v2-trial/`：
    - 原樣必須零問題。
    - 逐項改壞，每一種都要被擋下。
16. **順序。**
    - 本裁決與第一個修法 PR 一起生效：`decision-log.md`、`specs/README.md`、`specs/spec-profile.yaml`、`harness-core/spec_lint.py` 與測試、`harness-core/VERSION`（0.8.0）、修法紀錄。
    - 第二個修法 PR：執行器。要做到第 2～7、9、11 點：輸出斷言與路徑、時鐘、矩陣展開、事件擷取、跨目錄的 fixture、埠的替身。
    - **第二個修法 PR 合併之前，`specs/` 不得有版本 2 的規格**：檢查過得了，但沒有執行器能跑。
    - 之後才是規格的修法 PR（從抽選到初審提交的直向切片）與建構 PR。修法 PR 不夾帶程式（P4）。

### Relationship / Evidence

- **起因**：漏網紀錄 E-01、E-02 的根本回應是把 ReviewCase 立法，而格式版本 1 寫不出它的讀取遮罩、時間、狀態機與事件。
- **兩輪試寫**：`docs/harness/samples/reviewcase-v2-trial/`。第一輪照格式 v2 草案原本的第四節，找出 12 個缺口；第二輪用本裁決的寫法重寫成直向切片。經過與理由見 `docs/harness/15-spec-format-v2-draft.md` 第十三、十四節。
- **起草後補上的四處**（草案第十四節沒有，實作時加的）：
  - 第 1 點：版本 2 的目錄只有 `model.yaml` 是必要的。試寫的 `review-submission/` 就是這種目錄。
  - 第 2 點：`then.code` 要核對。版本 1 不核對 `then` 的代碼。
  - 第 5 點：`profile_version: 1` 仍然接受。這樣安裝 `harness-core/` 的其他專案不必為了升級而改設定檔，用到版本 2 才需要。
  - 第 6 點：版本 2 的範例名稱不得重複。矩陣展開後的名稱是身分，重複就分不出是哪一個。
- **部分取代** D-063 第 4 點（第 12 點）。D-063 第 3 點的四項待改寫由第 14 點處理。
- **D-064**：`spec-profile.yaml` 增加 `profile_version: 2`。本專案的設定檔升為 2。
- **實作時的驗證**（2026-11-20）：
  - 現有兩份規格零問題（82 條規則）。
  - 第二輪試寫用正式的 `spec_lint`（`--report-only`）檢查：三個目錄 37 條規則，零問題。
  - `test_spec_lint.py` 110 個測試全部通過，其中 41 個是這次新增的；原有的 69 個只改了一個（設定檔版本的訊息）。
  - 執行器沒有動，也還不能執行版本 2 的規格。
- **還沒有驗證的**：第 2～7、11 點的語意要等執行器實作才知道做不做得到。執行器做不到的部分，回頭修本裁決。
- **第二個修法 PR：執行器**（2026-11-20）。第 2～7、9、11 點都做得到，條文不必修。第 16 點「第二個修法 PR 合併之前，`specs/` 不得有版本 2 的規格」的限制，隨這個 PR 合併而解除。
  - 實作時補上的做法（條文沒有寫，寫在 `specs/README.md` 第十節「執行（版本 2）」）：
    - 別的目錄的 fixture，步驟用那個目錄的 `actors`。
    - `use_expect` 與直接寫的欄位並存時，直接寫的蓋過具名斷言。與 `spec_lint` 相同。
    - `when` 的 `save` 先存，`then` 才比對：`then` 的 `output` 與事件可以引用 `when` 存的變數。
    - 事件只算 `when` 執行期間發出的。`with` 比對事件類別的屬性（getter）。
    - 埠只在版本 2 的範例期間被替換；版本 1 的範例用原本的實作。被替換的埠，程式必須依介面注入。
    - 輸入的時間只在 `execute` 的參數本身是 `LocalDateTime` 時換算成當地時間；物件或清單裡面的時間、埠的 `returns` 裡的時間不換算。
  - 驗證：
    - 現有兩份規格（版本 1）的 82 個範例全部通過，與修改前相同。
    - 執行器的測試新增 49 個（`ExampleRunnerV2Test` 37 個、`AcceptanceWorldTest` 12 個），原有的 25 個沒有改。
    - 把執行器改壞七種（`absent`、`only`、被拒絕時的事件、`emits` 的筆數、時間點、沒設定的埠、選不到任何一筆），每一種都有測試變紅。
    - 把第二輪試寫與現有的 `review-project`、`score-sheet` 放在一起，對應用程式執行（不進 `specs/`）：應用程式帶著埠的替身啟動；`CandidateSource.queryCandidates` 回傳設定的值；`ExecuteSampling` 的四個事件都擷取到。35 個版本 2 的範例通過 4 個，其餘未通過的原因是程式與試寫的規格對不上，不是執行器。
  - 那次執行看到、要由規格的修法 PR 或建構 PR 處理的：
    - `ListReviewCasesUseCase.execute` 的參數是 `criteria`，規格寫 `batchId`（試寫紀錄的 P-2）。29 個範例停在這裡，所以時鐘與矩陣只有執行器自己的測試驗過，還沒有對應用程式驗過。
    - 抽選的四個事件類別沒有 `batchId`、`projectId` 屬性，只有 `DomainEvent` 的 `entityId` 與 `detail`（JSON 字串）。規格的 `with` 寫不到它們。
    - `ExecuteSampling` 找不到專案時丟 `IllegalArgumentException`，沒有錯誤代碼。
  - 還管不到的：`DomainEvent.occurredAt` 直接取系統時間（第 5 點留下的那一處），`clock`、`advance` 影響不到它。

### Mapping

- `specs/README.md` 第二、五、六、九、十節；`specs/spec-profile.yaml`
- `harness-core/spec_lint.py`、`harness-core/tests/test_spec_lint.py`、`harness-core/tests/fixtures/spec-format-v2-trial/`、`harness-core/README.md`、`harness-core/VERSION`
- 執行器：`services/quality-inspection-service/src/test/java/.../acceptance/`（`ExampleRunner`、`TreePath`、`ExampleWorld`、`SpringExampleWorld`、`AcceptanceClock`、`AcceptancePorts`、`AcceptanceWorldConfiguration`、`SpecFiles`）
- 後續：直向切片的規格；格式 v2.1（C 組）

## D-066 — 直向切片（抽選到初審提交）立法；程式與規格對不上的三處依規格修正

### Status

`accepted`（2026-11-21，依負責人對 `docs/harness/16-vertical-slice-draft.md` M-0～M-3 的裁決：全部照草案的建議；以及對規格修法 PR 兩點的裁決：試寫 Q-01 的前三列照 `api-spec.md`，涵蓋清單逐檔列出）

### Decision

1. **立法的範圍。** 從抽選到初審提交的直向切片，以格式 v2.0（D-065）寫成三個目錄：
   - `specs/sampling-batch/`：`ExecuteSampling`。
   - `specs/review-case/`：`ListReviewCases`、`AssignReviewCase`、`StartInitialReview`、`SaveInitialReviewDraft`、`SubmitInitialReview`、`GetRevisionHistory`。
   - `specs/review-submission/`：提交的狀態機，改變它的 use case 定義在 `review-case/`。
   - 內容來自格式 v2 草案第二輪試寫（`docs/harness/samples/reviewcase-v2-trial/round-2/`），依第 2～4、6 點修改。試寫列出的內容問題（Q-01 的後三列、Q-02、Q-03、Q-05）不在本裁決，對應的規則仍是 `proposed`，題目留在各目錄的 `OPEN-QUESTIONS.md`。
   - `law/coverage.txt`：只把範例直接執行的程式列為 `legislated`，用逐檔的列寫在目錄那一列之後：上面七支 use case 的類別，以及第 2 點的八個事件類別。aggregate 本身（`domain/sampling/`、`domain/reviewcase/`）與其他 use case 仍是 `grandfathered`：它們的其他轉移還沒有立法。
2. **事件的欄位（M-2）。**
   - 規格的 `with` 只用該事件在 `docs/02-ddd/domain-events.md` 寫到的欄位名稱。
   - `domain-events.md` 有「最低必要欄位」的事件，斷言列為 `decided`：`SamplingBatchStarted`、`SamplingBatchCompleted`（`batchId`、`projectId`）、`ReviewCaseAssigned`（`caseId`、`assignedReviewerId`）、`ReviewSubmissionSubmitted`（`caseId`、`submissionId`）。
   - 沒有「最低必要欄位」的事件，只斷言發出者的 identity，標 `proposed`：`SamplingCandidatesPrepared`、`ReviewCasesSelected`（`batchId`，Q-06）、`InitialReviewStarted`（`caseId`，Q-07）、`ReviewDraftSaved`（`submissionId`，Q-07）。不必等 `domain-events.md` 補齊。
   - 建構 PR：切片的八個事件類別加上規格用到的欄位與取得方法；`detail` 改由這些欄位產生，只有一個來源。稽核紀錄的內容必須逐字不變，由測試確認。`DomainEvent` 的建構式與其他 22 個事件類別不動。
3. **抽選時專案不存在（M-3）。**
   - 錯誤代碼是 `PROJECT_NOT_FOUND`，`decided`。代碼、意義與分類和 `specs/review-project/` 的 `RP-G-02` 相同。
   - `docs/03-contracts/api-spec.md` §9.3 補上檢查的錯誤表（`ROLE_DENIED`、`PROJECT_NOT_FOUND`、`HOSPITAL_SCOPE_MISMATCH`、`PROJECT_NOT_ACTIVE`）。
   - 建構 PR：`ExecuteSamplingUseCase` 找不到專案時丟出帶 `PROJECT_NOT_FOUND` 的 `ResourceNotFoundException`。現在丟的 `IllegalArgumentException` 被共用的例外處理當成程式錯誤，回 500。
4. **use case 的輸入（M-1）。**
   - 規格的輸入名稱照 `api-spec.md`，攤平成個別的參數。`ListReviewCases` 只列切片需要的 `batchId`、`status`。這是既有規則「程式要使用規格的名稱」（`specs/README.md` 第四節）的結果，格式與執行器不改。
   - 建構 PR：切片裡收物件的 `execute` 依規格的輸入名稱攤平，controller 跟著改。`ListReviewCases` 規格沒列的參數照樣保留，沒給時是 null；`page`、`size` 沒給時用現在的預設值（第 0 頁、每頁 20、上限 100）。
   - 切片以外仍收物件的 `execute`（複審、結案、修正等）這次不動，等各自立法時再攤平。
5. **一開始是紅的範例（M-0）。**
   - 本裁決與規格一起生效時，程式還沒照第 2～4 點修改，對應的範例是紅的。這些範例在同一個修法 PR 加進 `known-test-failures.txt`，對應的決策是本裁決。
   - 建構 PR 每修好一處，就把變綠的範例從清單刪除（只刪行免修法）。
   - 不採「先改程式、讓規格一進來就是綠的」：那是在 `grandfathered` 範圍、沒有律法的情況下先改程式，與「先立法」相反。
6. **錯誤代碼照 `api-spec.md`（試寫 Q-01 的前三列）。** 指派時案件不是待指派是 `CASE_NOT_ASSIGNABLE`；開始初審時案件不是待初審是 `CASE_NOT_IN_INITIAL_REVIEW_STAGE`；保存草稿或提交初審的人不是草稿的醫師是 `SUBMISSION_NOT_OWNED_BY_ACTOR`。建構 PR 把程式的 `CASE_NOT_PENDING_ASSIGNMENT`、`CASE_NOT_PENDING_INITIAL_REVIEW`、`NOT_SUBMISSION_OWNER` 改成這三個。`NOT_SUBMISSION_OWNER` 在 D-054 第 6 點（固定初審）的用法不變。`api-spec.md` 沒有代碼的另外三種情況仍是 Q-01，`proposed`。
7. **涵蓋清單的粒度。** 照第 1 點：逐檔列出範例直接執行的程式，aggregate 目錄仍是 `grandfathered`。不把整個目錄列為 `legislated`，那會誇大涵蓋的範圍。
8. **順序。** 本裁決、三個規格目錄、`law/coverage.txt`、`known-test-failures.txt`、`api-spec.md` §9.3、`specs/README.md` 在同一個修法 PR。之後才是建構 PR（第 2～4、6 點）。修法 PR 不夾帶程式（P4）。

### Relationship / Evidence

- **起因**：D-065 的執行器修法 PR（#118）把第二輪試寫對應用程式執行，35 個版本 2 的範例通過 4 個；三個原因（M-1～M-3）見 D-065 的 Evidence。
- **草案與查證**：`docs/harness/16-vertical-slice-draft.md`。三處的程式現況、權威文件、選項與理由都在那裡。
- **與試寫的差別**：試寫的 Q-04 由第 3 點裁決；`InitialReviewStarted` 試寫斷言 `caseId` 與 `submissionId`、`ReviewDraftSaved` 試寫斷言 `caseId`，依第 2 點改成只有發出者的 identity；新增 `SB-EXE-S3`、`SB-EXE-S4`、`RC-ASG-S3`、`RC-STI-S3`、`RC-DRF-S2`、`RC-SUB-S3` 六條規則承接事件的斷言；跨目錄的 fixture 改用真的 `specs/review-project/`（試寫用的是簡化版）。
- **D-060**：本裁決把切片列為已立法。沒列進來的範圍照舊是暫准。
- **D-065**：不必修。第 2～4 點都在現有格式與執行器能表達的範圍內。
- **立法時的驗證**（2026-11-21，本機，JDK 17）：
  - `spec_lint`（與 CI 相同的參數）：五個目錄零問題。
  - 後端完整測試：1095 個測試方法，失敗 34 個，與已知失敗清單完全相同，建置通過。修改前是 1057 個、失敗 0；新增的 38 個都是驗收範例（抽選 6 個、審查案件 32 個）。
  - 34 個紅的範例：M-1 有 31 個（全部停在準備步驟的 `ListReviewCases`）、M-2 有 2 個、M-3 有 1 個。
  - 為了知道 M-1 後面還有什麼，在本機暫時把 `ListReviewCases` 攤平再執行一次（實驗，未提交，已還原）：34 個裡剩 15 個紅。原因：M-2 有 6 個、M-2 加 Q-03 有 1 個、D-066 第 6 點有 4 個、Q-01 有 1 個、Q-02 有 1 個、Q-05 有 1 個、M-3 有 1 個；每一個的原因寫在 `known-test-failures.txt`。矩陣的 10 列全部通過，所以矩陣已經對應用程式驗過。
  - 還沒有對應用程式驗過的：時鐘。斷言 `assignedAt`、`submittedAt` 的兩個範例在 `then` 的事件就停了，沒有走到 `observe`。
- **第 6 點的影響**：已知失敗清單裡「M-1 之後還有 Q-01」的 5 個範例，有 4 個由第 6 點的建構 PR 修好；`rejects_submit_of_a_review_already_submitted` 要等 Q-01 第二列裁決。

### Mapping

- `specs/sampling-batch/`、`specs/review-case/`、`specs/review-submission/`、`specs/README.md` 第一節
- `law/coverage.txt`、`services/quality-inspection-service/known-test-failures.txt`
- `docs/03-contracts/api-spec.md` §9.3
- 後續：建構 PR（第 2～4、6 點）；Q-01（後三列）、Q-02、Q-03、Q-05～Q-07 的裁決

## D-067 — 直向切片的六題待裁決：錯誤代碼、指派的輸入檢查、初審完成事件、SYSTEM_ADMIN 的歷程欄位、四個事件的欄位

### Status

`accepted`（2026-11-22，負責人對 `docs/harness/17-slice-open-questions-draft.md` 十項待決全部照建議裁決）

### Decision

1. **錯誤代碼（試寫 Q-01 的後三列）。**
   - 開始初審的人不是受指派的醫師：`NOT_ASSIGNED_REVIEWER`，分類 `denied`（403），照程式。
   - 提交初審時案件不是初審中：`CASE_NOT_IN_INITIAL_REVIEW_STAGE`，分類 `rule_violation`（422），與開始初審共用。`api-spec.md` §4.2 的說明放寬為「案件不在該初審操作要求的狀態」。建構 PR 把 `SubmitInitialReviewUseCase` 的 `CASE_NOT_IN_PROGRESS`（409）改成這個代碼。
   - 提交初審時必答題沒有作答：`REQUIRED_ITEMS_NOT_ANSWERED`，分類 `rule_violation`（422），照程式。
   - 三個代碼都補進 `api-spec.md` §4.2。
   - 不在本裁決：保存草稿時案件不是初審中（程式回 `CASE_NOT_IN_PROGRESS`，規格沒有這條規則），以及複審的 `NOT_ASSIGNED_REREVIEWER` 是 422、初審的 `NOT_ASSIGNED_REVIEWER` 是 403 的不一致。
2. **指派時沒有給 `reviewerId` 或 `dueAt`（Q-02）。**
   - 回 `VALIDATION_ERROR`（400），由 domain 檢查，不依賴 HTTP 層的註記。依據 `api-spec.md` §10.3（`dueAt` 必填）、`state-machine.md` §3.2（要有 reviewer），先例是 `ReviewProject` 的 `DomainValidationException("VALIDATION_ERROR")`。
   - 建構 PR：`ReviewCase.assignReviewer` 在狀態檢查之前，對 null 的 `reviewerId`、`dueAt` 丟 `DomainValidationException("VALIDATION_ERROR")`。`reassignReviewer` 不在切片內，不動。
3. **提交初審發出 `InitialReviewCompleted`（Q-03）。**
   - 照 `domain-events.md` §6.4、§9.1、§9.2：提交初審成功後，除了 `ReviewSubmissionSubmitted`，再發出 `InitialReviewCompleted`。前者是 submission 的事實，後者是案件的事實。
   - 欄位照 §6.4：`caseId`、`projectId`、`submissionId`、`reviewerId`、`totalScore`、`qualityThreshold`。`qualityThreshold` 取自專案目前的版本，沒有品質線時是 null（D-019）。規格斷言 `caseId`、`submissionId`（`RC-SUB-S4`）。
   - 建構 PR：新增 `domain/reviewcase/event/InitialReviewCompletedEvent.java`，由 `SubmitInitialReviewUseCase` 在 `completeInitialReview` 之後發出。稽核的 `detail` 帶分數，已由 D-033 的遮罩涵蓋。`law/coverage.txt` 在本裁決的修法 PR 先把這個檔案列為 `legislated`。
   - 建構 PR 同時確認稽核頁與案件生命週期頁多出「初審完成」一筆的顯示，只確認，不改設計。
4. **SYSTEM_ADMIN 的 revision 歷程欄位（Q-05）。**
   - 照 `permission-matrix.md` §5.1 的白名單（共用欄位加 UI-014 的延伸欄位）。程式現有的七個欄位都在範圍內：`submissionId`、`stage`、`revisionNo`、`status`、`submittedAt`、`reviewerId`、`supersededBySubmissionId`。對應：`submittedBy` 是 `reviewerId`；`isCurrentEffective` 由 `status` 與 `supersededBySubmissionId` 表示；`submissionId` 是每一筆的 identity。
   - `api-spec.md` §13.2 的回應是不分角色的範例，不是 SYSTEM_ADMIN 的白名單；補一句 SYSTEM_ADMIN 看到的欄位。程式不改。
5. **`SamplingCandidatesPrepared`、`ReviewCasesSelected` 的欄位（Q-06）。**
   - `SamplingCandidatesPrepared`：`batchId`、`populationCount`、`eligibleCount`、`excludedCount`。`ReviewCasesSelected`：`batchId`、`selectedCount`。補進 `domain-events.md` §5.2、§5.3。
   - 建構 PR：照 D-066 第 2 點的做法加欄位與取得方法，`detail` 逐字不變。
6. **`InitialReviewStarted`、`ReviewDraftSaved` 的欄位（Q-07）。**
   - `InitialReviewStarted`：`caseId`、`submissionId`、`reviewerId`。`ReviewDraftSaved`：`submissionId`、`caseId`。草稿分數不列為必要欄位：它會一直變，又是要遮罩的資料；程式的 `detail` 照舊帶它，稽核不變。補進 `domain-events.md` §6.3、§7.1。
   - 建構 PR：同第 5 點。
7. **一開始是紅的範例。** 照 D-066 第 5 點：本裁決與規格一起生效時，第 1～3、5、6 點的程式還沒改，對應的範例在同一個修法 PR 加進 `known-test-failures.txt`，由建構 PR 逐一移除。第 4 點的範例在修法 PR 就變綠，從清單刪除。
8. **順序。** 本裁決、規格、`domain-events.md`、`api-spec.md`、`law/coverage.txt`、`known-test-failures.txt` 在同一個修法 PR；之後是建構 PR（第 1～3、5、6 點）。修法 PR 不夾帶程式（P4）。

### Relationship / Evidence

- **起因**：D-066 的建構 PR（#121）之後，已知失敗清單剩 4 個範例，各在等 Q-01（第二列）、Q-02、Q-03、Q-05 的裁決；Q-06、Q-07 決定四個事件的斷言能不能升為 `decided`。
- **草案與查證**：`docs/harness/17-slice-open-questions-draft.md`（main = `922f256` 的程式現況、權威文件、選項與理由）。
- **D-066**：第 2 點的「沒有最低必要欄位的事件只斷言 identity」，本裁決第 5、6 點補上欄位後不再適用於這四個事件。
- **D-033**：第 3、6 點的分數由既有的稽核遮罩涵蓋。

### Mapping

- `specs/review-case/`、`specs/sampling-batch/`（規則改為 `decided`，`OPEN-QUESTIONS.md` 清空）
- `docs/02-ddd/domain-events.md` §5.2、§5.3、§6.3、§7.1
- `docs/03-contracts/api-spec.md` §4.2、§13.2
- `law/coverage.txt`、`services/quality-inspection-service/known-test-failures.txt`
- 後續：建構 PR（第 1～3、5、6 點）

---

## D-068 — 直向切片的後續四題：草稿的狀態錯誤、複審的指派錯誤、批次完成事件的計數、未發布版本的抽選

### Status

`accepted`（2026-11-22，負責人對 `docs/harness/18-slice-followup-questions-draft.md` 四題全部照建議裁決）

### Decision

1. **保存初審草稿時案件不是初審中（F-1）。**
   - 回 `CASE_NOT_IN_INITIAL_REVIEW_STAGE`，分類 `rule_violation`（422），與開始初審、提交初審共用（理由同 D-067 第 1 點）。`api-spec.md` §4.2 的說明加上「保存初審草稿」。
   - 規格：新增 `RC-DRF-03` 與範例 `rejects_draft_before_review_started`（已指派、還沒開始初審）。
   - 建構 PR：`SaveInitialReviewDraftUseCase` 的 `CASE_NOT_IN_PROGRESS`（409）改成這個代碼與 `DomainRuleViolationException`。退回（`ReviewCase.returnToInitialReview`）的 `CASE_NOT_IN_PROGRESS` 不在本裁決，等退回立法。
2. **複審的 `NOT_ASSIGNED_REREVIEWER`（F-2）。**
   - 改成分類 `denied`（403），與 D-067 第 1 點的 `NOT_ASSIGNED_REVIEWER` 一致。依據 `permission-matrix.md` §6、§10（actor = assigned rereviewer 是權限條件）與 `test-scenarios.md` TS-RR-004（期望 403）。補進 `api-spec.md` §4.2。
   - 範圍是暫准（`application/reviewcase/`、`domain/reviewcase/` 的複審部分），沒有規格。負責人裁決在暫准範圍內照上述權威文件修改，由單元測試確認。
   - 建構 PR：`SaveRereviewDraftUseCase`、`SubmitRereviewUseCase`、`ReviseRereviewUseCase` 改丟 `AccessDeniedException("NOT_ASSIGNED_REREVIEWER")`；`StartRereviewUseCase` 在呼叫 `ReviewCase.startRereview` 之前做同樣的檢查（照 `StartInitialReviewUseCase` 的做法），domain 的檢查保留為防線。
   - 不在本裁決：修正已提交的複審要不要改用 `REVISION_ACTOR_MISMATCH`（`test-scenarios.md` TS-RV-004），等複審立法。
3. **`SamplingBatchCompleted` 的計數（F-3）。**
   - 照 `domain-events.md` §5.4：事件帶 `populationCount`、`eligibleCount`、`excludedCount`、`selectedCount`。
   - 規格：新增 `SB-EXE-S5` 與範例 `completed_event_carries_the_counts`。
   - 建構 PR：`SamplingBatchCompletedEvent` 加 `eligibleCount`、`excludedCount` 參數與四個計數的取得方法，兩個發出者（`ExecuteSamplingUseCase`、`RetrySamplingBatchUseCase`）傳入。`detail` 仍由欄位產生，改成五個鍵：原有的 `projectId`、`selectedCount`、`populationCount` 順序不變，`eligibleCount`、`excludedCount` 接在後面。這是 D-066 第 2 點「`detail` 逐字不變」對這個事件的例外；舊的稽核紀錄不動，`detail` 沒有程式解析。
4. **專案沒有已發布的版本時執行抽選（F-4）。**
   - 不立規則。`ReviewProject.activate` 要求已發布的版本，唯一能讓啟用中的專案出現未發布版本的 `prepareDraftVersion` 沒有呼叫者，這個情況現在走不到，範例也做不出來。
   - `ExecuteSamplingUseCase` 的 `INVALID_STATE_TRANSITION` 檢查保留為防線。啟用中的專案準備新版本之後，抽選要拒絕還是用最後一個已發布的版本（`api-spec.md` §9.3「採用 published version」），等 `prepareDraftVersion` 立法時一起決定。
5. **一開始是紅的範例。** 照 D-066 第 5 點：第 1、3 點的範例在本裁決的修法 PR 加進 `known-test-failures.txt`，由建構 PR 移除。
6. **順序。** 本裁決、規格、`api-spec.md`、`known-test-failures.txt` 在同一個修法 PR；之後是建構 PR（第 1～3 點）。修法 PR 不夾帶程式（P4）。

### Relationship / Evidence

- **起因**：D-067 第 1 點明列不在該裁決的兩件事（第 1、2 點），以及 D-067 建構 PR 時查到的兩件事（第 3、4 點）。
- **草案與查證**：`docs/harness/18-slice-followup-questions-draft.md`（main = `e14d6aa` 的程式現況、權威文件、選項與理由）。
- **D-066**：第 3 點是其第 2 點「`detail` 逐字不變」對 `SamplingBatchCompleted` 的例外。
- **D-060**：第 2 點在暫准範圍內照權威文件修改，由負責人裁決。

### Mapping

- `specs/review-case/`（`RC-DRF-03`）、`specs/sampling-batch/`（`SB-EXE-S5`）
- `docs/03-contracts/api-spec.md` §4.2
- `services/quality-inspection-service/known-test-failures.txt`
- 後續：建構 PR（第 1～3 點）

---

## D-069 — 複審切片：初審固定到複審結案的立法範圍、狀態錯誤代碼、事件名稱

### Status

`accepted`（2026-11-22，負責人對 `docs/harness/19-rereview-slice-draft.md` 裁決：S-1、S-2、S-4～S-6 照建議；S-3 不照建議，改成 422 並修 D-053）

### Decision

1. **範圍（S-1）。** 立法七支 use case：`FixInitialReview`（含分數大於品質線時自動進複審）、`SendToRereview`、`AssignRereviewer`、`StartRereview`、`SaveRereviewDraft`、`SubmitRereview`、`CloseRereview`。送複審前必須固定初審（D-054），開始複審前必須指派複審醫師，所以前兩支與指派一起納入。
   - 不納入，維持暫准：改派複審醫師、修正已提交的初審與複審、不經複審結案、取消、退回與重開、讀取遮罩。
   - `law/coverage.txt` 把七支 use case 與它們發出的事件類別列為 `legislated`。
2. **指派複審醫師、開始複審的狀態錯誤（S-2）。**
   - 指派複審醫師時案件不是 `PENDING_REREVIEW`：`CASE_NOT_ASSIGNABLE`，分類 `rule_violation`（422），與指派初審醫師相同。`api-spec.md` §4.2 的說明加上「指派複審醫師」。
   - 開始複審時案件不是 `PENDING_REREVIEW`：`CASE_NOT_IN_REREVIEW_STAGE`（422，見第 3 點）。
   - 開始複審時還沒有指派：`NO_REREVIEWER_ASSIGNED`（422）；指派的複審醫師是本案初審醫師：`REREVIEWER_SAME_AS_REVIEWER`（422，D-017）。兩者照程式，補進 §4.2。
   - 指派複審醫師時 `rereviewerId` 或 `dueAt` 沒有給：`VALIDATION_ERROR`（`api-spec.md` §10.5「`dueAt` 為必填」），與指派初審醫師（D-067）相同。
   - 建構 PR：`ReviewCase.assignRereviewer`、`reassignRereviewer`、`startRereview` 的 `CASE_NOT_PENDING_REREVIEW`（409）改成上述代碼與 `DomainRuleViolationException`。改派不在立法範圍，但共用同一個檢查，代碼一起變，行為不變；`CASE_NOT_PENDING_REREVIEW` 停用。指派複審醫師補上輸入的必填檢查。
3. **草稿、提交、結案的狀態錯誤（S-3）。**
   - 開始複審、保存複審草稿、提交複審、複審結案時案件不在該操作要求的狀態，共用 `CASE_NOT_IN_REREVIEW_STAGE`，分類 `rule_violation`（422）。§4.2 的說明放寬為「案件不在該複審操作要求的狀態」，與初審的 `CASE_NOT_IN_INITIAL_REVIEW_STAGE`（D-067、D-068）對稱。
   - 修 D-053 第 5 點：原本的 `CASE_NOT_REREVIEW_IN_PROGRESS`（409）停用。`api-spec.md` 結案端點的錯誤表一併改。
   - 建構 PR：`SaveRereviewDraftUseCase`、`SubmitRereviewUseCase`、`CloseRereviewUseCase`，以及 `ReviewCase.completeRereview`、`closeAfterRereview` 改成這個代碼與 `DomainRuleViolationException`。
   - 不變：`SUBMISSION_NOT_DRAFT`、`SUBMISSION_NOT_SUBMITTED`、`CONFLICT`、`NOT_SUBMISSION_OWNER`、`CASE_TERMINAL` 照 D-053、D-054。`SUBMISSION_NOT_DRAFT`、`SUBMISSION_NOT_SUBMITTED`（409）補進 §4.2。
4. **事件的名稱與欄位（S-4）。**
   - 規格用 `domain-events.md` 的名稱，程式改類別名稱：`CaseEscalatedToRereviewEvent` → `ReviewCaseEscalatedToRereviewEvent`、`RereviewSubmittedEvent` → `RereviewCompletedEvent`、`RereviewCaseClosedEvent` → `ReviewCaseClosedEvent`。不經複審結案的 `CaseClosedWithoutRereviewEvent` 不在範圍，不動。
   - 稽核紀錄不變：稽核存的是建構式寫死的 `action` 與 `detail`，不存類別名稱（2026-11-22 查證，見草案第六節）。改名時 `action` 與 `detail` 逐字不變，照 D-066 第 2 點由測試確認。`GET /audit-trails` 的 `eventName` 實際比對 `action`，它的值要不要對齊 `domain-events.md`，等稽核查詢立法。
   - `ReviewCaseEscalatedToRereview` 照 §6.5 的最低必要欄位：`caseId`、`projectId`、`triggeredBySubmissionId`、`initialScore`、`qualityThreshold`。§6.5 補上「管理者手工送複審也發布本事件」；手工時 `triggeredBySubmissionId`、`qualityThreshold` 是 null。
   - `InitialReviewFixed` 補進 `domain-events.md`（§6.12）：`caseId`、`submissionId`，不帶分數（D-054 第 8 點）。
   - 其他四個事件（`RereviewerAssigned`、`RereviewStarted`、`RereviewCompleted`、`ReviewCaseClosed`）文件沒有最低必要欄位：規格只斷言 `caseId`，標 `proposed`（`OPEN-QUESTIONS.md` Q-08）。
   - 建構 PR：三個類別改名；`ReviewCaseEscalatedToRereviewEvent`、`InitialReviewFixedEvent` 加上述欄位與取得方法；其他四個事件加 `caseId` 的取得方法。
5. **盲審與讀過初審的人（S-5）。**
   - 提交複審、複審結案的輸出不得有 `finalScore` 與初審分數（D-033、D-053 第 2 點），規格以 `absent` 斷言。
   - 讀過完整初審的人不得受派（D-055 第 5 點）延後到讀取遮罩立法：範例做不出「讀過、之後才受派」的狀態。`REREVIEWER_SAME_AS_REVIEWER`（D-017）納入。
6. **品質線與權重取自專案目前的版本（S-6）。** 不立規則，併入 D-068 第 4 點，等專案版本管理立法時一起決定（案件要不要記專案版本）。
7. **輸入。** 七支 use case 的輸入照規格的名稱攤平（D-066 M-1-2）。保存複審草稿的輸入沒有分數：分數在提交時由系統依評分表計算，與初審相同。
8. **一開始是紅的範例。** 照 D-066 第 5 點，本裁決的修法 PR 把紅的範例加進 `known-test-failures.txt`，由建構 PR 移除。
9. **順序。** 本裁決、規格、`api-spec.md`、`domain-events.md`、`law/coverage.txt`、`known-test-failures.txt` 在同一個修法 PR；之後是建構 PR。修法 PR 不夾帶程式（P4）。

### Relationship / Evidence

- **起因**：D-066～D-068 立法了抽選到初審提交；負責人 2026-11-22 同意下一個切片是送複審到結案。
- **草案與查證**：`docs/harness/19-rereview-slice-draft.md`（main = `b351b03` 的程式現況、權威文件、選項與理由；含事件類別與稽核的查證）。
- **D-053**：第 3 點修改其第 5 點的一個代碼。
- **D-066**：第 4 點照其第 2 點（事件欄位、`detail` 逐字不變）；第 7 點照其 M-1-2（攤平輸入）。
- **D-068**：第 6 點併入其第 4 點。
- **不在本裁決**：`domain-events.md` §7.1、§7.2 的 `ReviewDraftSaved`、`ReviewSubmissionSubmitted` 寫的是提交（不分階段）的事件，程式在複審沒有發出；規格的提交複審範例用 `emits_include`，不斷言完整的事件清單。提交複審沒有必答題檢查（初審有，D-067）。兩者等複審的後續題裁決。

### Mapping

- `specs/review-case/`（`RC-T-04`～`RC-T-09`、`RC-G-04`、`RC-FIX-*`、`RC-SND-*`、`RC-ARR-*`、`RC-STR-*`、`RC-RDR-*`、`RC-SRR-*`、`RC-CLR-*`、`OPEN-QUESTIONS.md` Q-08）、`specs/review-submission/`（`RS-T-03`、`RS-T-04`）
- `docs/03-contracts/api-spec.md` §4.2、D-034 複審獨立 Close Command 對齊
- `docs/02-ddd/domain-events.md` §6.5、§6.12
- `law/coverage.txt`、`services/quality-inspection-service/known-test-failures.txt`
- 後續：建構 PR

---

## D-070 — 四個複審事件的最低必要欄位（Q-08）

### Status

`accepted`（2026-11-22，負責人對 `docs/harness/20-q08-rereview-event-fields-draft.md` 的 E-1～E-4（含 E-3-2）全部照建議裁決）

### Decision

1. **`RereviewerAssigned`（E-1）**：`caseId`、`rereviewerId`、`dueAt`。名稱用 `rereviewerId`，與指派的輸入、`CaseSummary` 一致。改派也發這個事件、傳入同樣的三個欄位；改派要不要另帶 `reassignment`，等改派立法。
2. **`RereviewStarted`（E-2）**：`caseId`、`submissionId`、`rereviewerId`，照 `InitialReviewStarted`（D-067）。
3. **`RereviewCompleted`（E-3）**：`caseId`、`submissionId`、`rereviewerId`、`rereviewScore`、`finalScore`。`finalScore` 依 D-012／D-020 加權；它與 `rereviewScore`、權重可以反推初審分數，靠既有的稽核遮罩保護（持有 `REREVIEWER` 的人拿不到 `detail`），與 `domain-events.md` §14 一致。複審醫師的命令輸出仍不得有 `finalScore`（D-033、`RC-SRR-S4`）。
   - 規格以範例斷言加權（E-3-2）：RANGE 題目的專案，初審 5 分、手工送複審、複審 15 分，權重預設 0.50／0.50，`finalScore` = 10.00。`deferred_behaviors` 與 `not_covered` 的「`finalScore` 加權沒有範例」刪除。
4. **`ReviewCaseClosed`（E-4，複審路徑）**：`caseId`、`submissionId`（結案時固定的複審提交，D-053 第 3 點）。不加結案路徑欄位；兩條結案路徑要不要合成一個類別，等不經複審結案立法。
5. **作法**：照 D-066 第 2 點與 D-069 第 4 點，事件類別以取得方法提供欄位，`action` 與 `detail` 逐字不變，稽核紀錄不變。
6. **一開始是紅的範例**：照 D-066 第 5 點列進 `known-test-failures.txt`，由建構 PR 移除。
7. **順序**：本裁決、`domain-events.md`、規格、`known-test-failures.txt` 在同一個修法 PR；之後是建構 PR（四個事件類別加欄位，發出者與改派傳入）。修法 PR 不夾帶程式（P4）。

### Relationship / Evidence

- **起因**：D-069 第 4 點把四個事件的欄位列為 `proposed`（`specs/review-case/OPEN-QUESTIONS.md` Q-08）。
- **草案與查證**：`docs/harness/20-q08-rereview-event-fields-draft.md`（main = `9766a59`；含稽核讀取遮罩的查證：`GetCaseLifecycleUseCase`、`ListAuditTrailsUseCase`）。
- **不在本裁決**：稽核的 `detail` 帶初審分數，卻不算 D-055 第 5 點的「讀過完整初審」，不留下讀取者紀錄（草案第七節）；另案處理。D-069 留下的兩件（複審不發 `ReviewDraftSaved`、`ReviewSubmissionSubmitted`；提交複審沒有必答題檢查）也不在本裁決。

### Mapping

- `docs/02-ddd/domain-events.md` §6.6～§6.9
- `specs/review-case/`（`RC-ARR-S2`、`RC-STR-S3`、`RC-SRR-S3`、`RC-CLR-S2` 改為 `decided`；fixture `weighted_rereview_answered`；`OPEN-QUESTIONS.md` 刪除 Q-08）
- `services/quality-inspection-service/known-test-failures.txt`
- 後續：建構 PR

---

## D-071 — 稽核的 `detail` 依 D-055 第 6 點遮罩

### Status

`accepted`（2026-11-22，負責人對 `docs/harness/21-audit-detail-initial-reader-draft.md` 的 A-1～A-3 全部照建議裁決）

### Decision

1. **帶分數的 action**：`ReviewCase` 的稽核紀錄中，下列 action 的 `detail` 帶有初審分數或可反推的值：`INITIAL_REVIEW_COMPLETED`（`totalScore`、`qualityThreshold`）、`CASE_ESCALATED_TO_REREVIEW`（修正初審路徑的 `score`、`threshold`）、`REREVIEW_SUBMITTED`、`REREVIEW_REVISED`（`rereviewScore`、`finalScore`）、`CASE_CLOSED_WITHOUT_REREVIEW`（`finalScore`）。之後新增會帶分數的 action 必須加進這份清單，並以測試檢查。
2. **遮罩（A-1 選 B）**：`GET /review-cases/{caseId}/lifecycle` 與 `GET /audit-trails` 是 D-055 第 6 點所說的出口，套用遮罩，不記錄讀取者。只要符合下列任一條件，帶分數的那一列 `detail` 輸出 `null`：
   - 案件**目前**的狀態不是 `REREVIEW_IN_PROGRESS`、`CLOSED`、`CANCELLED`。判斷看的是讀取當下，不是事件發生時。
   - 讀者是該案目前的複審醫師，或出現在該案的複審指派歷史中。這條看使用者，不看當下持有的角色。
   - 該案的複審指派歷史不完整。

   三個條件都不成立時照常輸出。其他 action 的 `detail` 不受影響。讀取不寫入初審讀取者紀錄，也不遞增案件版本。在指派之前要看分數，從案件詳情讀（會留下紀錄）。
3. **粒度（A-2 選 A）**：遮罩時整個 `detail` 輸出 `null`，不只拿掉分數鍵，也不加 `maskedFields`。
4. **沿用的規則**：持有 `REREVIEWER` 的人（不論同時持有哪些角色），在兩個出口都拿不到任何 `detail`（D-033）。
5. **`SYSTEM_ADMIN`（A-3）**：在 `/audit-trails` 也拿不到任何 `detail`，與生命週期查詢一致，依 D-033 與 api-spec 15.2 的「只看 metadata」。
6. **規格的輸入與輸出**：`ListAuditTrails` 的輸入改成平的參數（`aggregateType`、`aggregateId`、`hospitalId`、`eventName`、`from`、`to`、`page`、`size`），照 D-069 的作法。`GetCaseLifecycle` 的輸出以 `entries` 包住清單，因為範例的路徑不能選取最外層的清單。HTTP 回應的格式不變，由 controller 展開。
7. **一開始是紅的範例**：照 D-066 第 5 點列進 `known-test-failures.txt`，由建構 PR 移除。
8. **順序**：本裁決、api-spec、規格、`law/coverage.txt`（`application/audit/` 改成 `legislated`）、`known-test-failures.txt` 在同一個修法 PR，之後才是建構 PR。修法 PR 不夾帶程式（P4）。

### Relationship / Evidence

- **起因**：D-070 草稿第七節。`GetCaseLifecycleUseCase` 與 `ListAuditTrailsUseCase` 會把帶分數的 `detail` 交給 `HOSPITAL_MANAGER`、`AUDITOR`、`REVIEWER`，卻不留下讀取者紀錄。讀者若之後才取得 `REREVIEWER` 角色並受派，就繞過了 D-055 第 5 點。
- **草案與查證**：`docs/harness/21-audit-detail-initial-reader-draft.md`（main = `956bfe3`）。
- 補足 D-055 第 6 點最後一項（「之後新增任何會輸出逐案初審分數或可反推值的出口，必須擇一套用」）。這兩個出口在 D-055 之前就已存在，當時沒有列入。
- **不在本裁決**：D-069 留下的兩件（複審不發 `ReviewDraftSaved`、`ReviewSubmissionSubmitted`；提交複審沒有必答題檢查）；生命週期查詢找不到案件時的行為（現在回空清單）。

### Mapping

- `docs/03-contracts/api-spec.md` §14.1、§14.2
- `specs/review-case/`（`GetCaseLifecycle`：`RC-LCY-01`～`RC-LCY-04`；`ListAuditTrails`：`RC-AUD-01`～`RC-AUD-03`；output type `AuditEntry`；actor `rr1_as_auditor_h1`）
- `law/coverage.txt`：`application/audit/` → `legislated specs/review-case/`
- `services/quality-inspection-service/known-test-failures.txt`
- 後續：建構 PR

---

## D-072 — 頒布憲法草案第一條：日期時間的表示

### Status

`accepted`（2026-11-22，負責人對 `docs/harness/22-constitution-datetime-draft.md` 的 T-1～T-5 全部照建議裁決）

### Decision

1. **憲法草案（T-2）**：在本檔「4. 決策清單」之前新增「憲法草案」一節。該節位階高於所有個案裁決，後續裁決不得牴觸，要牴觸必須先修改條文。該節預定提升為獨立的憲法文件。
2. **第一條（T-1 選 A）**：條文見「憲法草案」第一條。輸出一律是 UTC `Z`，不帶時區的輸入一律拒絕。
3. **`dueAt` 的意思**：負責人確認，需求中的 `dueAt` 是一個確切的時刻，不是日期。所以它適用第一條第 1～3 點，不適用第 4 點。
4. **違憲清單（T-3 選 A）**：下列是頒布時已知的違憲處。修正之前暫時容許，但不得擴大；新寫的程式與規格必須合憲；每修正一處，就在本條註明修正的 PR。建議依下列順序修正，各項的細節由各自的裁決或建構 PR 決定：
   1. 稽核 `detail`：`ASSIGNED`（`ReviewCaseAssignedEvent`）、`REASSIGNED`（`ReviewCaseReassignedEvent`）的 `dueAt` 以 `LocalDateTime.toString()` 寫入，不帶時區，秒數是 0 時還會省略。
   2. HTTP 的輸入：指派、改派初審醫師與複審醫師的 `dueAt` 接受不帶時區的值。修正前要先確認前端送出的格式。
   3. HTTP 的輸出：案件列表、詳情、指派畫面的 view，以及匯出的 `closedAt`，時間以 `LocalDateTime` 輸出，不帶時區。
   4. domain 與 persistence：`ReviewCase` 的 `dueAt`、`assignedAt`、`closedAt`、`cancelledAt`、`rereviewerDueAt`、`rereviewerAssignedAt` 是 `LocalDateTime`。這六個值代表台北的牆上時刻，改成 `Instant` 時以 `Asia/Taipei` 換算。正式資料庫還沒有資料（D-045）。
   5. `specs/spec-profile.yaml` 的 `local_time_zone`：用來解讀不帶時區的時間。改成只用於第一條第 3、4 點的業務換算。
   6. 文件：`domain-events.md` 的時間範例帶 `+08:00`，改成 UTC `Z`。`api-spec.md` 其他章節的時間範例也一樣，隨相關的 PR 修改。
5. **稽核 `detail`（T-4）**：為了合憲而修改 `detail` 的時間表示，不受 D-066 第 2 點、D-069 第 4 點「`detail` 逐字不變」的限制；其他內容仍然逐字不變。已寫入的稽核紀錄只增不改，不補改，同一種 action 會有新舊兩種格式並存。
6. **D-071 的建構 PR（T-5 選 A）**：
   - 一併修正違憲清單第 1 項的 `ASSIGNED`：`dueAt` 以 `Asia/Taipei` 換成時間點，輸出 UTC，例如 `"dueAt":"2026-11-26T23:59:59Z"`。
   - D-071 規格的兩個範例已經是這個值，不用修改。
   - `REASSIGNED` 屬於改派，還沒有立法，留在清單上。
7. **本次的修改**：只改文件，包括本檔、`api-spec.md` §3.3、`api-spec.md` 14.1，以及 `docs/harness/22`。程式不動（P4）。

### Relationship / Evidence

- **起因**：D-071 建構 PR 的兩個範例，預期 `ASSIGNED` 的 `detail` 帶 `Z`，程式寫出的不帶時區。查證後發現，專案沒有全專案層級的時間規定。`api-spec.md` §3.3 只寫「ISO-8601」，`spec-profile.yaml` 的 `local_time_zone` 反而容許不帶時區的時間。
- **草案與查證**：`docs/harness/22-constitution-datetime-draft.md`（main = `ee21979`）。共有 42 個檔案使用 `LocalDateTime`，沒有任何檔案使用 `OffsetDateTime` 或 `ZonedDateTime`。
- **與既有裁決的關係**：
  - D-037、D-054、D-057 的 UTC 時間點已經合憲。
  - D-066、D-069 的「`detail` 不變」由第 5 點加上例外。
- **還沒有查證的**：前端送出 `dueAt` 的格式（違憲清單第 2 項）。

### Mapping

- `docs/01-requirements/decision-log.md`「憲法草案」第一條
- `docs/03-contracts/api-spec.md` §3.3（改成引用第一條）、14.1（新舊格式並存）
- 後續：D-071 建構 PR（違憲清單第 1 項的 `ASSIGNED`）；違憲清單其餘各項

---

## D-073 — 違憲清單第 2 項：HTTP 收進來的 `dueAt` 必須帶時區

### Status

`accepted`（2026-11-22，負責人對 `docs/harness/23-dueat-input-timezone-draft.md` 的 I-1～I-5 全部照建議裁決）

### Decision

1. **範圍**：指派與改派初審醫師、複審醫師的四個端點（`assign-reviewer`、`reassign-reviewer`、`assign-rereviewer`、`reassign-rereviewer`），請求本文中的 `dueAt`。
2. **後端（I-2 選 A）**：四個 request DTO 的 `dueAt` 改成帶時區的型別（`OffsetDateTime`）。controller 以 `Asia/Taipei` 換成牆上時刻（`LocalDateTime`）再交給 use case，use case 的簽名不變，因此 `specs/review-case/` 不用改。這個換算的位置依憲法第一條第 3 點寫明於此；等違憲清單第 4 項把 domain 改成 `Instant` 時，再一起移除。
3. **拒絕（I-3）**：請求本文中的時間不帶時區、無法讀成時間點時，回 400 `VALIDATION_ERROR`，外殼依 `specs/http-common.yaml`。其他無法讀取的請求本文怎麼處理，本裁決不決定，維持現狀。`VALIDATION_ERROR` 加進 `http-common.yaml` 的 `transport_codes`，因為這個拒絕發生在進入 use case 之前。
4. **前端（I-1 選 A）**：指派頁面把 `datetime-local` 的牆上時刻當成台北時間，送出時加上 `+08:00`，例如 `"2026-11-26T23:59:00+08:00"`；不依賴瀏覽器的時區。前端必須與後端在同一個建構 PR 修改，否則指派會失敗。
5. **未立法的範圍（I-4）**：`ReviewCaseController`、四個 DTO，以及改派的兩個 use case 都是 `grandfathered`（D-060）。依負責人裁決，本項在暫准範圍內照憲法第一條實作。本項只改輸入格式，不改業務規則；改派的業務規則等改派立法時處理。
6. **前端顯示（I-5）**：不在本裁決，屬於違憲清單第 3 項。
7. **可執行性**：HTTP 契約只比對回應形狀，不涵蓋請求端（`specs/README.md` 第七節第 8 點）；use case 範例也看不到 JSON 的讀取。因此第 3 點目前沒有規格範例，由建構 PR 的 API 測試保證（含「不帶時區的值回 400 `VALIDATION_ERROR`」）。要讓規格能表達請求端，屬於另一個題目。
8. **違憲清單的註記**：D-072 第 4 點第 1 項的 `ASSIGNED` 已由 PR #134 修正（`REASSIGNED` 仍在清單上）；第 2 項由本裁決的建構 PR 修正。D-072 的條文不改，以本點為註記。
9. **順序**：本裁決、`http-common.yaml`、`api-spec.md`、草稿在同一個修法 PR；之後是前後端一起改的建構 PR。修法 PR 不夾帶程式（P4）。

### Relationship / Evidence

- **依據**：憲法草案第一條第 2、3 點；D-072 第 3 點（`dueAt` 是確切的時刻）、第 4 點第 2 項。
- **草案與查證**：`docs/harness/23-dueat-input-timezone-draft.md`（main = `f94254a`）。前端的 `normalizeDueAtInput` 只補秒數，送出不帶時區的值；`GlobalExceptionHandler` 沒有處理 `HttpMessageNotReadableException`。
- **與草案不同**：草案 I-3 寫「在 `http-common.yaml` 補規則與範例」。查證後發現 HTTP 契約不涵蓋請求端，只能補 `transport_codes`，所以改由第 7 點處理。
- **還沒有查證的**：Jackson 遇到不帶時區的字串時丟出的確切例外；前端測試與 demo 資料是否依賴不帶時區的格式。

### Mapping

- `specs/http-common.yaml`（`transport_codes` 加 `VALIDATION_ERROR`）
- `docs/03-contracts/api-spec.md` §10.3、§10.5（`dueAt` 必須帶時區）
- 後續：建構 PR（四個 DTO、`ReviewCaseController`、`GlobalExceptionHandler`、前端指派頁面、API 測試）

---

## D-074 — 規格格式 v2.1：請求端的範例

### Status

`accepted`（2026-11-22，負責人對 `docs/harness/24-request-side-examples-draft.md` 的 R-1～R-8 全部照建議裁決，並同意第 7 點新增的院區覆寫檢查）

### Decision

1. **範圍（R-1 選 A）**：請求端的範例只寫 use case 範例看不到的行為：
   - 請求本文的讀取（格式、型別、時區）；
   - 認證；
   - 錯誤代碼對到的 HTTP 狀態碼與外殼。

   業務規則仍只寫在 use case 範例，同一條規則不在兩層重複。
2. **寫法（R-2 選 A）**：寫在同一份 `examples.yaml`，`when` 新增 `http` 步驟（`method`、`path`、`body`、`body_raw`、`query`）。`given` 沿用 fixture。目錄有 `http.yaml` 時，`method` 與 `path` 必須是其中的端點。
3. **請求本文（R-3）**：
   - `body` 照常檢查，包括 D-065 第 5 點「時間要帶時區」；
   - `body_raw` 原封不動送出，不受該條約束，也不檢查內容，只能用在被拒絕的範例，或沒有外殼的回應；
   - `body` 與 `body_raw` 擇一。
4. **`then`（R-4）**：
   - 寫法沿用 use case 範例。`code` 必須在本目錄的 `error_codes` 或 `http-common.yaml` 的 `transport_codes`。
   - 狀態碼不寫，由執行器依 `http-common.yaml` 推得並檢查。
   - 沒有外殼的回應寫成 `then: { status, body: empty }`，只能與 `body_raw` 一起用。
5. **操作者（R-5）**：
   - 執行器依操作者產生 `api-key`（JWT）。角色經 `spec-profile.yaml` 新增的 `role_claims` 轉換。
   - 查證：程式以 `DomainRole.valueOf(role_name.toUpperCase())` 轉換，五個角色一對一；`user_role` 是清單，可以表示多個角色。
   - 新增 `as: anonymous`，只能用在 `http` 步驟。
6. **版本與 `spec_lint`（R-6）**：
   - 版本是格式 v2.1，檔案仍是 `spec_version: 2`，說明在 `specs/README.md` 第十一節。
   - `given`、`observe`、fixture 不允許 `http` 步驟。
   - `http` 步驟納入「情境相同的範例」的比較。
7. **院區覆寫（查證後新增，負責人同意）**：
   - `TzJwtParser` 在 `quality-inspection.hospital-id`（環境變數 `QUALITY_INSPECTION_HOSPITAL_ID`）有值時，會忽略 JWT 的院區。
   - 執行器在跑 `http` 範例之前必須確認這個設定是空的；有值就停下來（執行器的問題，不是範例失敗），不讓操作者的院區被悄悄換掉。
   - 查證時本機與 CI 都沒有設定。
8. **執行器（R-7）**：
   - `AcceptanceExamplesTest` 加上 MockMvc，`http` 步驟以 MockMvc 送出。
   - 查證：
     - 加上 MockMvc 之後，現有 174 個範例的名稱與結果不變，執行時間在量測誤差內（約 3.7～4.0 秒）。
     - 這個測試本來就有獨立的 Spring context，不會多建一個。
   - 事件沿用同步擷取：回應不是 2xx 就算被拒絕，被拒絕時不得有事件（D-065 第 7 點）。
   - 以 MockMvc 實測：
     - 不帶時區回 400，沒有事件；
     - 成功回 200，有 `ReviewCaseAssigned`；
     - 業務規則拒絕回 422，沒有事件。
9. **順序（比照 D-065）**：
   1. 修法 PR 一：本裁決、`specs/README.md` 第十一節、`spec_lint` 與它的測試、`spec-profile.yaml` 的 `role_claims`。這個 PR 合併之後、修法 PR 二合併之前，`specs/` 不得出現 `http` 步驟。
   2. 修法 PR 二：執行器與它的測試，以及 `specs/review-case/` 的第一批範例（D-073 的四個端點不帶時區回 `VALIDATION_ERROR`、其他讀不進來的本文維持現狀）。範例應該直接是綠的。
   3. 建構 PR：依 R-8 刪除 `DueAtTimeZoneApiTest` 中與範例重複的部分；換算結果要看資料庫，這一部分保留。
10. **D-072 違憲清單的註記**：第 2 項已由 PR #136 修正（D-073 的建構 PR）。D-072 的條文不改，以本點為註記。

### Relationship / Evidence

- **起因**：D-073 第 7 點。「不帶時區回 400」沒有可以執行的規格，由不在律法清冊的 JUnit 測試保證，而且 `given` 是手工存 entity。
- **草案與查證**：`docs/harness/24-request-side-examples-draft.md`（main = `04bbdb0`），第十二節是三項查證的紀錄。
- **與既有裁決的關係**：
  - 延伸 D-065（格式 v2.0）與 D-061（HTTP 契約）。HTTP 契約仍只比對回應形狀；`http` 步驟的回應也會被它觀察到。
  - `role_claims` 依 D-064，專案的部分放在設定檔。

### Mapping

- `specs/README.md` 第十一節；`specs/spec-profile.yaml` 的 `role_claims`
- `harness-core/spec_lint.py`（`http` 步驟、`body_raw`、沒有外殼的 `then`、`transport_codes`、`role_claims`），`harness-core/tests/test_spec_lint.py`（`SpecLintHttpStepTest`）
- `docs/harness/25-topic-backlog.md`（題目清單）
- 後續：修法 PR 二（執行器與範例）、建構 PR

---

## D-075 — 違憲清單第 3～6 項：時間改成時間點（`Instant`）

### Status

`accepted`（2026-11-23，負責人對 `docs/harness/26-datetime-instant-draft.md` 的 N-1～N-8 全部照建議裁決；並說明目前沒有正式環境，也沒有需要保留資料的環境）

### Decision

1. **範圍與順序（N-1 選 A）**：違憲清單第 3 項（HTTP 的輸出）與第 4 項（domain 與 persistence）一起做，從 domain 改起。domain、persistence、use case、view、匯出的時間全部改成 `Instant`，HTTP 輸出因此是 UTC `Z`。D-073 第 2 點放在 controller 的換算（以 `Asia/Taipei` 換成牆上時刻）一併移除：DTO 的 `OffsetDateTime` 直接換成 `Instant` 交給 use case。
2. **補充違憲清單（N-2 選 A）**：D-072 第 4 點第 4 項漏列 `ReviewSubmission` 的 `createdAt`、`submittedAt`、`updatedAt`。這三個值同樣是 `LocalDateTime`，也以不帶時區的格式輸出（`submittedAt`、`initialSubmittedAt`）。補進清單，這次一起改。
3. **更正 D-072 第 4 點第 4 項的說法**：
   - 該項寫「這六個值代表台北的牆上時刻」。查證後只有 `dueAt`、`rereviewerDueAt` 是（由 controller 以 `Asia/Taipei` 換算）。
   - `assignedAt`、`rereviewerAssignedAt`、`closedAt`、`cancelledAt`，以及第 2 點的三個值，是 `LocalDateTime.now(clock)`，而時鐘是 `Clock.systemDefaultZone()`，所以是伺服器 JVM 時區的牆上時刻。
   - D-072 的條文不改，以本點為更正。
4. **既有資料與時鐘（N-3）**：
   - 不轉換既有資料，不新增 migration。欄位型別（`timestamp`）不改，`hibernate.jdbc.time_zone: UTC` 不改；改成 `Instant` 之後，同一欄存的是 UTC。
   - 負責人說明：目前沒有正式環境，也沒有需要保留資料的環境。本機與 demo 的資料庫重建。
   - 時鐘改成 `Clock.systemUTC()`。「現在」一律以 `Instant.now(clock)` 取得。
5. **前端顯示（N-4 選 A；D-073 第 6 點留下的題目）**：
   - 畫面上的時間固定以 `Asia/Taipei` 顯示，輸入框也以台北時間填入；送出時照 D-073 第 4 點加上 `+08:00`。不依賴瀏覽器的時區。
   - 換算的位置（憲法第一條第 3 點）：前端一個共用的函式，所有時間的顯示與輸入框都經過它。
   - 報表匯出頁的 `createdAt` 原本寫「以使用者瀏覽器的本地時間顯示」，一併改成固定 `Asia/Taipei`。
   - 前端必須與後端在同一個建構 PR 修改，否則畫面上的時間會差 8 小時。
6. **匯出的 `closedAt`（N-5 選 A）**：
   - `POST /reports/export` 回應的 `closedAt` 改成 `Instant`，輸出 UTC `Z`，與同一列的 `createdAt` 一致。
   - CSV 由前端產生：`closedAt` 與 `createdAt` 一樣寫入 ISO-8601 UTC，欄位標題從「結案時間（伺服器時間）」改成「結案時間」。
7. **`local_time_zone` 與執行器（N-6 選 A；違憲清單第 5 項）**：建構 PR 合併之後，另開修法 PR 收緊：
   - 執行器移除替不帶時區的時間所做的兩處換算（輸入的 `SpringUseCaseInvoker.localTime`、輸出的 `ExampleRunner.instantOf`）；程式輸出不帶時區的時間時，範例直接失敗。
   - `local_time_zone` 只用來決定範例時鐘的時區，`specs/README.md` 第九、十節照改。
   - 順序必須在建構 PR 之後，否則現有範例會全部失敗。
8. **文件（N-7；違憲清單第 6 項）**：
   - `domain-events.md`、`api-spec.md`、`docs/05-ui/` 的事件與回應範例改成 UTC `Z`，換算成同一個時刻。
   - 請求的範例保留 `+08:00`：輸入接受任何位移（第一條第 2 點），前端依 D-073 以台北時間送出。
   - `ui-page-spec-ui017` 依第 5、6 點修改 `createdAt`、`closedAt` 的呈現。
9. **未立法的範圍與 `REASSIGNED`（N-8）**：
   - 改派、修正、不經複審結案、取消、退回都是 `grandfathered`（D-060）。依負責人裁決，在暫准範圍內照憲法第一條實作。只改時間的型別與格式，不改業務規則。
   - `ReviewCaseReassignedEvent` 的 `dueAt` 改成 `Instant` 之後，稽核 `detail` 的 `dueAt` 是 UTC `Z`，違憲清單第 1 項的 `REASSIGNED` 一併修正（D-072 第 5 點已容許）。
10. **規格**：`specs/` 的時間型別是 `datetime`，範例都已經寫帶時區的值，預期不用修改，建構之後應維持通過。這是推論，建構 PR 驗證；若有範例需要修改，另開修法 PR，不在建構 PR 裡改。
11. **順序**：
    1. 修法 PR 一：本裁決、文件範例、`docs/harness/26` 標為已裁決、更新題目清單。程式不動（P4）。
    2. 建構 PR：後端與前端一起改。
    3. 修法 PR 二：第 7 點的收緊。
12. **違憲清單的註記**：建構 PR 合併後，D-072 第 4 點第 1 項的 `REASSIGNED`、第 3 項、第 4 項（含第 2 點補充的三個值）、第 6 項修正完成；第 5 項由修法 PR 二完成。D-072 的條文不改，以本點為註記，PR 編號於合併後補記在題目清單。

### Relationship / Evidence

- **依據**：憲法草案第一條；D-072 第 4 點的違憲清單；D-073 第 2、6 點。
- **草案與查證**：`docs/harness/26-datetime-instant-draft.md`（main = `edb7f61`）。`src/main` 有 39 個檔案使用 `LocalDateTime`；時鐘是 `Clock.systemDefaultZone()`；前端把時間當成字串顯示（取前 16 個字元），沒有換算時區；匯出是 JSON 回應，CSV 由前端產生。
- **與草案不同**：草案 N-5 寫「匯出檔的格式還沒有查證」。查證後 CSV 由前端產生（`ui-page-spec-ui017` §5.2），所以第 6 點多了 CSV 的欄位標題；報表匯出頁 `createdAt` 的顯示方式併入第 5 點。
- **與既有裁決的關係**：
  - 更正並補充 D-072 第 4 點第 4 項（第 2、3 點）。
  - 取代 D-073 第 2 點中「controller 換成牆上時刻、use case 的簽名不變」的做法；D-073 其餘各點不變。
  - D-045（正式資料庫沒有資料）是第 4 點不轉換資料的前提。
- **還沒有查證的**：`Instant` 經 Jackson 輸出時的小數位數；demo 資料與前端測試是否依賴不帶時區的格式。

### Mapping

- `docs/02-ddd/domain-events.md`、`docs/03-contracts/api-spec.md`（§3.3 與各回應範例）、`docs/05-ui/` 六份頁面規格
- `docs/harness/26-datetime-instant-draft.md`、`docs/harness/25-topic-backlog.md`
- 後續：建構 PR（`domain/reviewcase/`、`application/reviewcase/`、`infrastructure/persistence/`、`interfaces/rest/`、`reporting/`、`SystemClockConfig`、前端的時間顯示與 CSV）；修法 PR 二（`acceptance/`、`specs/README.md`、`specs/spec-profile.yaml` 的註解）

---

## D-076 — 複審的提交事件與必答題檢查（D-069 留下的兩件）

### Status

`accepted`（2026-11-23，負責人對 `docs/harness/27-rereview-submission-events-draft.md` 的 V-1～V-5 全部照建議裁決）

### Decision

1. **保存複審草稿發 `ReviewDraftSaved`（V-1 選 A）**：
   - 與初審一致，照 `domain-events.md` §7.1「某一審查階段」的字面。欄位是 `submissionId`、`caseId`。
   - 稽核 `detail` 的格式不變（`{"caseId":"…","totalScore":…}`）；複審草稿沒有有意義的分數（D-069 第 7 點），`totalScore` 寫 `null`，不寫舊值或 0。
2. **提交複審發 `ReviewSubmissionSubmitted`（V-2 選 A）**：
   - 提交複審發兩個事件：`ReviewSubmissionSubmitted` 與 `RereviewCompleted`，與提交初審（`ReviewSubmissionSubmitted` 與 `InitialReviewCompleted`）對稱。
   - 事件補上 §7.2 要求的 `stage`（`INITIAL`／`REREVIEW`），初審一併補。稽核的 `action`、`detail` 逐字不變（D-066 第 2 點）。
   - 規格把 `SubmitRereview` 的事件清單寫完整；D-069 留下的 `not_covered` 項目移除。
3. **`ReviewSubmission` 的稽核紀錄沒有遮罩（V-3 選 A）**：
   - 查證時發現：`DRAFT_SAVED`、`SUBMITTED` 的 `detail` 帶 `totalScore`，`entityType` 是 `ReviewSubmission`，不在 D-071 的遮罩範圍（D-071 只列 `ReviewCase` 的 action）。
   - 另開一題，排在本題之後：先查證兩個查詢出口會不會回傳這些紀錄，再決定要不要擴大遮罩。本題不擴大遮罩。
   - 第 1、2 點實作之後，複審也會寫入這兩種紀錄，`SUBMITTED` 帶複審的分數。負責人知悉並同意先照本裁決進行。
4. **提交複審的必答題檢查（V-4 選 A）**：
   - 評分表有 `ALWAYS_REQUIRED` 的題目沒有作答時，回 `REQUIRED_ITEMS_NOT_ANSWERED`（422，`rule_violation`），與初審（D-067）相同。
   - 檢查的是這次提交採用的答案：輸入有 `answers` 就看它（包括空的清單），沒有帶就看草稿。
   - 順序在狀態、指派、提交歸屬的檢查之後，計分之前。
5. **不在本裁決（V-5）**：
   - `CONDITIONAL` 的必答規則：初審與複審都沒有檢查，屬於評分表規則的題目。
   - 修正已提交的初審與複審（`ReviseInitialReview`、`ReviseRereview`）還沒有立法；它們發不發這兩個事件、有沒有必答題檢查，等修正立法時處理。
6. **一開始是紅的範例**：照 D-066 第 5 點，本裁決的修法 PR 把紅的範例加進 `known-test-failures.txt`，由建構 PR 移除。
7. **順序**：本裁決、規格、`domain-events.md`、`api-spec.md`、`known-test-failures.txt` 在同一個修法 PR；之後是建構 PR。修法 PR 不夾帶程式（P4）。

### Relationship / Evidence

- **起因**：D-069「不在本裁決」留下的兩件；題目清單的下一題。
- **草案與查證**：`docs/harness/27-rereview-submission-events-draft.md`（main = `3411657`）。程式現況：保存複審草稿不發事件；提交複審只發 `RereviewCompleted`，沒有必答題檢查；兩個事件類別都沒有 `stage`。
- **與既有裁決的關係**：
  - 補完 D-069（複審切片）；必答題檢查比照 D-067。
  - 第 2 點照 D-066 第 2 點（`detail` 逐字不變）。
  - 第 3 點是 D-071 的後續題，D-071 的條文不改。
- **還沒有查證的**：`ListAuditTrails` 與 `GetCaseLifecycle` 會不會回傳 `ReviewSubmission` 的紀錄；D-071 當時是刻意不納入還是漏掉。

### Mapping

- `specs/review-case/usecases.yaml`（`RC-SUB-S5`、`RC-RDR-S2`、`RC-SRR-09`、`RC-SRR-S5`，`SaveRereviewDraft` 與 `SubmitRereview` 的 `emits`）、`specs/review-case/examples.yaml`（6 個新範例，移除一項 `not_covered`）
- `docs/02-ddd/domain-events.md` §7.1、§7.2；`docs/03-contracts/api-spec.md` §4.2
- `services/quality-inspection-service/known-test-failures.txt`（5 個）
- `docs/harness/25-topic-backlog.md`（新增「`ReviewSubmission` 稽核紀錄的遮罩」「報表的日期篩選固定以台北解讀」）
- 後續：建構 PR（`SaveRereviewDraftUseCase`、`SubmitRereviewUseCase`、`ReviewDraftSavedEvent`、`ReviewSubmissionSubmittedEvent`）

## D-077 — 報表的日期篩選固定以台北解讀

### Status

`accepted`（2026-11-24，負責人對 `docs/harness/28-report-date-filter-taipei-draft.md` 的 V-1～V-5 全部照建議裁決；V-4 的測試時區用 `UTC`）

### Decision

1. **日期欄位的意義與送出轉換（V-1 選 A）**：
   - 報表匯出頁（UI-017）與儀表板（UI-015）的起日、迄日是 `Asia/Taipei` 的日曆日期（`YYYY-MM-DD`），不依瀏覽器的時區。這是憲法草案第一條第 4 點要求的時區指定。
   - `from` ＝ 起日 00:00 `+08:00`；`to` ＝ 迄日**次日** 00:00 `+08:00`。兩者以 UTC（`Z`）送出，維持 D-057 的含起不含迄，畫面上的迄日含當天。
   - 例：起日 2026-10-01、迄日 2026-10-31 → `from=2026-09-30T16:00:00Z`、`to=2026-10-31T16:00:00Z`。
   - 換算的位置（憲法草案第一條第 3 點）：前端的 `features/reports/report-export/export-date-range.ts`。後端只收時間點，不做日期換算；HTTP 契約不變。
2. **預設區間（V-2）**：「上一個整月」以台北的今天計算。例：瀏覽器在 UTC、時間是 2026-11-30T17:00:00Z（台北已是 2026-12-01 01:00）時，預設區間是 2026-11-01～2026-11-30。
3. **範圍（V-3）**：
   - UI-015 與 UI-017 共用同一個換算。
   - CSV 檔名的日期是畫面上的台北日期，檔名格式不變。
   - 後端不改；`reporting/` 與 `ReportController.java` 在 `law/coverage.txt` 維持 `grandfathered`。
   - 建構 PR 一併修正 `report-export.service.ts` 中 `closedAt` 過時的註解（D-075 之後是 `Instant`）。
4. **測試（V-4）**：
   - 單元測試的預期值寫死 UTC 字串，不再以瀏覽器時區的 `new Date(...)` 計算；加上第 2 點的跨月例子，以注入的時鐘固定「今天」。
   - 單元測試與 e2e 在 `UTC` 時區執行（Karma 以 `TZ=UTC`，Playwright 以 `timezoneId: 'UTC'`），使不在 UTC+8 的瀏覽器也被測到。具體作法在建構 PR 決定，以 Windows 與 Linux 的 CI 都能跑為準。
5. **不在本裁決（V-5）**：
   - 後端 `reporting` 的立法。
   - `ReviewSubmission` 稽核紀錄的遮罩（D-076 第 3 點），照題目清單排在本題之後。
   - 其他頁面沒有只有日期的輸入（查證：前端只有這兩頁共四個 `type="date"` 欄位）；指派頁的 `dueAt` 是時刻，已由 D-073 處理。
6. **順序**：本裁決與 UI 文件在同一個修法 PR；之後是建構 PR。修法 PR 不夾帶程式（P4）。

### Relationship / Evidence

- **起因**：題目清單待辦第 1 項；負責人 2026-11-23 決定固定成 `Asia/Taipei`。
- **草案與查證**：`docs/harness/28-report-date-filter-taipei-draft.md`（main = `a18fbb7`）。程式現況：`toExportRange` 與 `previousFullMonth` 以瀏覽器本地時間換算；後端 `from`、`to` 已是 `Instant`，以 `from ≤ createdAt < to` 過濾。
- **與既有裁決的關係**：
  - 補上 D-057 沒有寫的畫面日期時區；D-057 的條文不改。
  - 依憲法草案第一條第 3、4 點；前端以固定 `+08:00` 換算，與 D-073 第 4 點、D-075 第 5 點相同（台北沒有日光節約時間）。
- **還沒有查證的**：無。

### Mapping

- `docs/05-ui/ui-page-spec-ui017報表匯出頁.md` §4、§5 檔名、§8 第 1 項；`docs/05-ui/ui-page-spec-ui015儀表板頁.md` §4、§8 第 1 項
- `docs/harness/25-topic-backlog.md`（本題移到進行中；D-075 修法 PR 二補上 #142；D-076 移到已完成）
- 後續：建構 PR（`export-date-range.ts` 與其單元測試、Karma 與 Playwright 的時區設定、`report-export.service.ts` 的註解）

## D-078 — `ReviewSubmission` 稽核紀錄的遮罩

### Status

`accepted`（2026-11-24，負責人對 `docs/harness/29-review-submission-audit-mask-draft.md` 的 V-1～V-5 全部照建議裁決；V-2 選 A；並要求立法前掃過其他種類的 `detail`）

### Decision

1. **範圍（V-1）**：`ReviewSubmission` 的 `DRAFT_SAVED`、`SUBMITTED` 列為帶分數的紀錄，加進 D-071 第 1 點的清單；初審與複審一律套用（複審的 `DRAFT_SAVED` 是 `null`，一併套用）。`REVISED` 不帶分數，不列。
2. **找到所屬案件（V-2 選 A）**：由提交編號查案件編號，一次查一頁；查不到就遮罩（D-035 fail-closed）。不解析 `detail` 的字串。
3. **遮罩條件（V-3）**：與 RC-LCY-02 相同，以所屬案件判斷；初審與複審的分數都照這個條件。遮罩的方式與 D-071 相同：整個 `detail` 變成 `null`，其他欄位照常。
4. **生命週期（V-4）**：`GetCaseLifecycle` 維持只回傳 `ReviewCase` 的紀錄。本裁決只影響 `ListAuditTrails`。
5. **不在本裁決（V-5）**：
   - `AuditController` 的立法（`grandfathered`）；HTTP 介面不變。
   - 稽核的其他 `entityType`：立法前已掃過全部事件類別，不帶分數（草稿 29 第七節的表）。`CANCELLED`、`RETURNED` 的 `reason` 是自由文字，不屬於遮罩的範圍。
   - 已寫入的紀錄不改；遮罩在讀取時套用。正式資料庫還沒有資料（D-045）。
6. **一開始是紅的範例**：照 D-066 第 5 點加進 `known-test-failures.txt`，由建構 PR 移除。
7. **順序**：本裁決、規格、`known-test-failures.txt` 在同一個修法 PR；之後是建構 PR。修法 PR 不夾帶程式（P4）。

### Relationship / Evidence

- **起因**：D-076 第 3 點另開的一題。
- **草案與查證**：`docs/harness/29-review-submission-audit-mask-draft.md`（main = `2fd49e6`）。
  - `GetCaseLifecycle` 只查 `entityType = ReviewCase`，不會回傳提交紀錄；`ListAuditTrails` 會回傳，`ScoredDetailMask` 只看 `ReviewCase`。
  - 2026-11-24 以暫時的單元測試實測：`AUDITOR`、`HOSPITAL_MANAGER` 讀到 `SUBMITTED`、`DRAFT_SAVED` 的 `totalScore` 沒有遮罩（同一頁的 `ReviewCase` 帶分數紀錄有遮罩）。
  - D-071 判斷是漏掉：草稿 21 只列了專案與評分表不帶分數。
- **與既有裁決的關係**：擴大 D-071 第 1 點的清單，D-071 其他條文不改；遮罩條件沿用 D-055 第 6 點、D-071 第 2 點。

### Mapping

- `specs/review-case/usecases.yaml`（RC-AUD-02 的條件）、`specs/review-case/examples.yaml`（3 個新範例）
- `services/quality-inspection-service/known-test-failures.txt`（2 個）
- `docs/harness/25-topic-backlog.md`（本題移到進行中；D-077 移到已完成）
- 後續：建構 PR（`ScoredDetailMask`、提交編號 → 案件編號的查詢）

## D-079 — 初審草稿的稽核紀錄帶院區

### Status

`accepted`（2026-11-24，負責人表示補院區、已寫入的紀錄不回填，並指示範圍不大就直接開修法 PR；`docs/harness/30-initial-draft-audit-hospital-draft.md` 的 W-1～W-3 照建議，W-3 選 A）

### Decision

1. **補院區（W-1）**：初審保存草稿的 `ReviewDraftSaved` 帶所屬案件的院區，與複審草稿相同。院區人員以 `ListAuditTrails` 讀得到這類紀錄；`detail` 的分數照 D-078 遮罩。
2. **一般規則（W-3 選 A）**：稽核紀錄的院區一律是所屬案件（或專案、批次）的院區，不得是空字串或 `null`；之後新增的事件也適用。本裁決時只有初審草稿不符合（草稿 30 的掃描）。
3. **已寫入的紀錄（W-2）**：不回填。正式資料庫沒有資料（D-045，負責人確認）。
4. **規格**：D-078 的兩個範例（`audit_trails_mask_submission_rows_before_rereview_for_an_auditor`／`_for_a_manager`）恢復檢查 `DRAFT_SAVED`。它們一開始是紅的，照 D-066 第 5 點加進 `known-test-failures.txt`，由建構 PR 移除。
5. **順序**：本裁決、規格、`known-test-failures.txt`、`domain-events.md` 在同一個修法 PR；之後是建構 PR。修法 PR 不夾帶程式（P4）。

### Relationship / Evidence

- **起因**：D-078 建構時發現（草稿 29 的更正、#148）。
- **查證**：`docs/harness/30-initial-draft-audit-hospital-draft.md`（main = `2eb643d`）。`SaveInitialReviewDraftUseCase` 兩處傳 `""`；`src/main/java` 其他 `publish(new …)` 都有院區。
- **與既有裁決的關係**：補足 D-078 第 1 點在初審草稿上的實際效果；D-078 條文不改。

### Mapping

- `specs/review-case/examples.yaml`（2 個範例恢復檢查 `DRAFT_SAVED`）
- `services/quality-inspection-service/known-test-failures.txt`（2 個）
- `docs/02-ddd/domain-events.md`（7.1 的最低必要欄位）
- 後續：建構 PR（`SaveInitialReviewDraftUseCase` 改傳 `reviewCase.getHospitalId()`）

## D-080 — ReviewCase 切片：退回、重開、取消、不經複審結案

### Status

`accepted`（2026-11-24，負責人對 `docs/harness/31-case-return-cancel-close-draft.md` 的 X-1～X-8 全部照建議裁決）

### Decision

1. **可取消的狀態（X-1 選 A）**：除了 `CLOSED`、`CANCELLED` 以外的狀態都可以取消，包括 `RETURNED`、`INITIAL_REVIEW_COMPLETED`、`REREVIEW_IN_PROGRESS`。取消已終止的案件回 409 `CASE_TERMINAL`。取消不處理進行中的提交；案件終止後不得再審、不得 revision（D-006）。
2. **重開的事件（X-2 選 A）**：重開發 `ReviewCaseReopened`（稽核 action `REOPENED`），帶 `caseId`、`reviewerId`、`dueAt`；不再發 `ReviewCaseAssigned`。不帶分數，不列入 D-071 的遮罩清單。
3. **退回後的草稿（X-3 選 A）**：退回時進行中的初審提交維持 `DRAFT`。重開後開始初審，案件已有初審提交且它是 `DRAFT` 時沿用這份提交，不建立新的；作答與評語保留，`InitialReviewStarted` 帶的是原本的 `submissionId`。這一點修改已立法的 `StartInitialReview`（RC-STI-S2 限定為還沒有初審提交時，新增 RC-STI-S4）。
4. **重開前不要求改派（X-4 選 A）**：重開沿用原本的初審醫師與期限；要換人或改期限，重開後以改派處理。`state-machine.md` 第 7 節待確認第 2 點以此結案。
5. **理由（X-5）**：退回與取消的稽核 `detail` 以 JSON 序列化產生，格式維持 `{"reason":"…"}`，理由含引號、反斜線、換行時仍是合法 JSON。use case 拒絕空白的理由（400 `VALIDATION_ERROR`），與 HTTP 的 `@NotBlank` 一致。
6. **授權（X-6）**：退回、重開、取消、不經複審結案都限 `HOSPITAL_MANAGER`，且案件為本院。`decision-log.md` 第 7 節「退回初審的最終授權角色」以此結案。
7. **不經複審結案的事件（X-7 選 A）**：維持獨立的事件 `CaseClosedWithoutRereview`（稽核 action `CASE_CLOSED_WITHOUT_REREVIEW`，`detail` 是 `{"finalScore":…}`，D-071 遮罩），以 action 區分結案路徑；不發 `ReviewCaseClosed`。
8. **錯誤代碼（X-8）**：退回時不是初審中回 409 `CASE_NOT_IN_PROGRESS`；重開時不是 `RETURNED` 回 409 `CASE_NOT_RETURNED`；不經複審結案沿用 `CASE_NOT_INITIAL_REVIEW_COMPLETED`、`INVALID_STATE_TRANSITION`。D-068 第 1 點留給退回立法的 `CASE_NOT_IN_PROGRESS` 以此結案（維持 409）。
9. **事件欄位**：`ReviewCaseReturned`、`ReviewCaseCancelled` 帶 `caseId`、`reason`；`CaseClosedWithoutRereview` 帶 `caseId`。
10. **一開始是紅的範例**：照 D-066 第 5 點加進 `known-test-failures.txt`，由建構 PR 移除。
11. **順序**：本裁決、規格、`law/coverage.txt`、`known-test-failures.txt`、文件在同一個修法 PR；之後是建構 PR。修法 PR 不夾帶程式（P4）。

### Relationship / Evidence

- **起因**：題目清單「還沒立法的 ReviewCase 範圍」。負責人 2026-11-24 同意切成五個切片（C → B → A → D → E，C 與 B 合併為本切片）；共用元件跟著第一個用到它的切片，`ReviewCase.java`、`ReviewSubmission.java` 最後處理。
- **草案與查證**：`docs/harness/31-case-return-cancel-close-draft.md`（main = `4b768bd`）。2026-11-24 以暫時的規格範例實測：重開記成 `ASSIGNED`、理由 `a"b` 寫成 `{"reason":"a"b"}`、退回後重開再開始初審留下兩筆 `DRAFT`（都是 `revisionNo = 1`）。
- **與既有裁決的關係**：沿用 D-006（`CANCELLED` 終止）、D-031／D-054（不經複審結案須初審已固定）、D-071（`CASE_CLOSED_WITHOUT_REREVIEW` 的遮罩）。修改已立法的 `StartInitialReview`（第 3 點）。

### Mapping

- `specs/review-case/usecases.yaml`（四個 use case、`RC-STI-S4`、兩個錯誤代碼）、`model.yaml`（`RC-T-10`～`RC-T-19`，原本的 `deferred_transitions` 移除）、`examples.yaml`（兩個 fixture、16 個範例）
- `law/coverage.txt`（四個 use case、`CloseWithoutRereviewCommand`、三個既有事件；`ReviewCaseReopenedEvent` 由建構 PR 新增，修法 PR 先登記路徑）
- `services/quality-inspection-service/known-test-failures.txt`
- `docs/02-ddd/state-machine.md`（§3.2、§3.5、第 7 節）、`docs/02-ddd/domain-events.md`（§6.9～§6.11、新增 §6.13、§6.14）、`docs/02-ddd/permission-matrix.md`（§6）、`docs/03-contracts/api-spec.md`（§10.7、§10.8、§10.11）
- 後續：建構 PR。除了第 2、3、5、9 點，`CloseWithoutRereviewUseCase.execute` 改收 `caseId`（規格範例以參數名稱呼叫 use case），`CloseWithoutRereviewCommand` 隨之移除

## D-081 — 重開後改派，新醫師開始初審建立新的提交（D-080 第 3 點的更正）

### Status

`accepted`（2026-11-24，負責人裁決：先單獨修，只有同一位醫師才沿用草稿；`docs/harness/32-restart-draft-owner-draft.md` 的 Y-1 照建議）

### Decision

1. **沿用的條件（Y-1）**：開始初審時，案件已有初審提交且它是 `DRAFT`，**而且它的醫師是操作者**，才沿用這份提交（RC-STI-S4 加上醫師條件）。
2. **醫師不同時**：建立一份新的初審提交（同 RC-STI-S2），案件改指新提交；原本的提交維持 `DRAFT`、留在修正歷史，不作廢、不刪除（RC-STI-S5）。這與 D-080 之前的行為相同。
3. **D-080 的其餘點不變**：第 3 點「退回時進行中的初審提交維持 `DRAFT`」、第 4 點「重開不要求改派」照舊。
4. **一開始是紅的範例**：`restarts_with_a_new_draft_after_reopen_and_reassign` 照 D-066 第 5 點加進 `known-test-failures.txt`。它以未立法的改派製造前提，列在 `needs`，所以在切片 A 立法 `ReassignReviewCase` 之前不會執行，由切片 A 的建構 PR 移除。本裁決的建構 PR 另以整合測試驗證「重開後改派，新醫師開始初審並保存草稿」。
5. **順序**：本裁決、規格、`known-test-failures.txt`、文件在同一個修法 PR；之後是建構 PR。修法 PR 不夾帶程式（P4）。

### Relationship / Evidence

- **起因**：切片 A（改派）起草前的實測。D-080 的建構 PR（#153）照 RC-STI-S4 實作後，「作答 → 退回 → 重開 → 改派給 u-r2 → u-r2 開始初審」拿到 u-r1 的 `DRAFT`（`reviewerId = u-r1`，作答與評語是 u-r1 的），u-r2 保存草稿回 `SUBMISSION_NOT_OWNED_BY_ACTOR`。初審中不能改派、也不能重開，案件只能取消。
- **查證**：`docs/harness/32-restart-draft-owner-draft.md`（main = `ce5eae1`）。以暫時的規格範例走真實寫入路徑實測，跑完還原，沒有提交。
- **與既有裁決的關係**：更正 D-080 第 3 點與 RC-STI-S4 的適用範圍；D-080 條文不改，以本裁決為註記。`ReassignReviewCase` 仍未立法（切片 A），範例只用它製造前提。

### Mapping

- `specs/review-case/usecases.yaml`（RC-STI-S4 加上醫師條件、新增 RC-STI-S5）、`examples.yaml`（1 個範例）
- `services/quality-inspection-service/known-test-failures.txt`（1 個）
- `docs/03-contracts/api-spec.md`（§10.8）、`docs/02-ddd/domain-events.md`（§6.10）
- 後續：建構 PR（`StartInitialReviewUseCase` 沿用前比對提交的醫師與操作者，加整合測試）；範例由切片 A 的建構 PR 轉綠

## D-082 — ReviewCase 切片 A：改派初審醫師、改派複審醫師

### Status

`accepted`（2026-11-24，負責人對 `docs/harness/33-reassign-draft.md` 的 Z-1～Z-7 全部照建議裁決，並表示日後有必要再調整）

### Decision

1. **改派初審醫師的狀態錯誤（Z-1 選 A）**：案件不是 `PENDING_INITIAL_REVIEW` 時回 422 `CASE_NOT_ASSIGNABLE`，與指派初審醫師、指派／改派複審醫師一致；`CASE_NOT_PENDING_INITIAL_REVIEW` 停用。
2. **輸入檢查（Z-2 選 A）**：兩個改派都在狀態檢查之前檢查醫師與 `dueAt` 必填，空白也算沒給，回 400 `VALIDATION_ERROR`（同 D-067、D-069）。
3. **改派給同一個人（Z-3 選 A）**：允許，用來只改期限；照樣發出事件，改派複審醫師照樣寫入複審指派歷史（D-055）。
4. **還沒指派複審醫師就改派（Z-4 選 A）**：拒絕，422 `NO_REREVIEWER_ASSIGNED`。
5. **改派複審醫師的事件（Z-5 選 A）**：沿用 `RereviewerAssigned`（`REREVIEWER_ASSIGNED`），帶 `caseId`、`rereviewerId`、`dueAt`，另帶 `reassignment`（改派為 `true`，指派為 `false`）；稽核 `detail` 照舊是 `{"rereviewerId":…,"reassignment":true}`。D-070 第 1 點留下的問題以此結案。
6. **改派初審醫師的事件（Z-6）**：`ReviewCaseReassigned`（`REASSIGNED`）帶 `caseId`、`previousReviewerId`、`newReviewerId`、`dueAt`；稽核 `detail` 以 JSON 序列化產生，格式維持 `{"previousReviewerId":…,"newReviewerId":…,"dueAt":…}`。不帶分數，不列入 D-071 的遮罩清單。
7. **檢查順序與輸入名稱（Z-7）**：
   - 改派複審醫師依序檢查：輸入 → 狀態 → 尚未指派 → 與初審醫師相同 → 讀過初審（D-055 第 5 點）。指派複審醫師（已立法）一併改成「輸入 → 狀態 → 與初審醫師相同 → 讀過初審」，狀態不對時先回 `CASE_NOT_ASSIGNABLE`。讀過初審的範例仍等讀取遮罩立法（`model.yaml` 的 `deferred_behaviors`）。
   - use case 的輸入名稱用 `reviewerId`、`rereviewerId`，與指派和 HTTP 一致；`ReassignRereviewer` 改收參數，`ReassignRereviewerCommand` 移除。
   - D-081 的範例 `restarts_with_a_new_draft_after_reopen_and_reassign` 改用 `reviewerId`、移除 `needs`，由本裁決的建構 PR 轉綠。
8. **複審中不得改派複審醫師**：D-055 已決定；`permission-matrix.md` 第 9 節待確認第 2 點以此結案。
9. **一開始是紅的範例**：照 D-066 第 5 點加進 `known-test-failures.txt`，由建構 PR 移除。
10. **順序**：本裁決、規格、`law/coverage.txt`、`known-test-failures.txt`、文件在同一個修法 PR；之後是建構 PR。修法 PR 不夾帶程式（P4）。

### Relationship / Evidence

- **起因**：題目清單「還沒立法的 ReviewCase 範圍」切片 A。
- **草案與查證**：`docs/harness/33-reassign-draft.md`（main = `bfd9cf8`）。以暫時的規格範例走真實寫入與查詢路徑實測：空白的初審醫師會被接受；還沒指派複審醫師也能改派並記成改派；改派初審醫師的狀態錯誤是 409 `CASE_NOT_PENDING_INITIAL_REVIEW`。
- **與既有裁決的關係**：沿用 D-017、D-055、D-067、D-069、D-070、D-073、D-075；修改已立法的 `AssignRereviewer` 檢查順序（第 7 點）。

### Mapping

- `specs/review-case/usecases.yaml`（兩個 use case）、`model.yaml`（`RC-T-20`、`RC-T-21`）、`examples.yaml`（改派的範例；D-081 範例改輸入名稱、移除 `needs`）
- `law/coverage.txt`（兩個 use case、`ReviewCaseReassignedEvent`、`ReassignRereviewerCommand`）
- `services/quality-inspection-service/known-test-failures.txt`
- `docs/02-ddd/state-machine.md`（§3.2）、`docs/02-ddd/domain-events.md`（§6.6、新增 §6.15）、`docs/02-ddd/permission-matrix.md`（第 9 節）、`docs/03-contracts/api-spec.md`（§10.4、§10.6）
- 後續：建構 PR

## D-083 — ReviewCase 切片 D：修正已提交的初審與複審，並立法 `ReviewCase.java`

### Status

`accepted`（2026-11-24，負責人決定把 `ReviewCase.java` 提早到本切片立法；對 `docs/harness/34-revise-and-reviewcase-draft.md` 的 U-1～U-10 全部照建議裁決）

### Decision

1. **必答題（U-1 選 A）**：修正與提交相同，輸入的 `answers` 裡評分表有必答的題目沒有作答時，回 422 `REQUIRED_ITEMS_NOT_ANSWERED`（D-067、D-076）。
2. **沒有變更（U-2 選 A）**：`answers` 與 `comment` 都和被修正的提交相同時，回 409 `NO_CHANGES`（比照 D-038）。
3. **稽核紀錄（U-3 選 A）**：修正複審也發 `ReviewSubmissionRevised`（`ReviewSubmission` 的 `REVISED`，不帶分數），與修正初審對稱；`RereviewRevised`（`REREVIEW_REVISED`，帶分數，D-071 遮罩）照舊。修正初審不另寫 `ReviewCase` 的紀錄。
4. **目標不對（U-4）**：提交不屬於本案或階段不對，兩個修正一律回 409 `CONFLICT`（D-054 第 6 點）；`SUBMISSION_NOT_INITIAL` 與修正初審的 400 請求錯誤停用。
5. **事件與輸出（U-5）**：
   - `ReviewSubmissionRevised` 帶 `caseId`、`oldSubmissionId`、`newSubmissionId`、`stage`、`revisionNo`；`RereviewRevised` 帶 `caseId`、`oldSubmissionId`、`newSubmissionId`、`rereviewScore`、`finalScore`。兩者稽核 `detail` 格式不變，以 JSON 序列化產生。
   - 輸出 `{ newSubmissionId, oldSubmissionId, revisionNo, totalScore, status, stage, caseId }`；修正的人只看得到自己階段的分數（D-033），修正複審的輸出沒有 `finalScore` 與初審分數。
6. **輸入與 ID（U-6）**：`ReviseRereview` 改收參數（`caseId`、`submissionId`、`answers`、`comment`），`answers` 與修正初審同型別；`ReviseRereviewCommand` 移除。新提交 ID 一律是 UUID。
7. **檢查順序**：
   - 修正初審：角色 → 載入 → 院區 → 案件未終止 → 提交存在 → 屬於本案且是初審 → 操作者是提交者 → `SUBMITTED` → 目前有效 → 未固定 → 必答題 → 有變更。
   - 修正複審：角色 → 載入 → 院區 → 案件未終止 → 受指派的複審醫師 → 提交存在 → 屬於本案且是複審 → 操作者是提交者 → `SUBMITTED` → 目前有效 → 必答題 → 有變更。
8. **`ReviewCase.java` 立法（U-7、U-8）**：`ReviewCase.java`、`CaseStatus.java`、`ReviewerAssignment.java`、`ReviewCaseRepository.java` 登記為已立法（`specs/review-case/`）。`model.yaml` 補上不變式 RC-INV-03（`finalScore` 的計算，D-012、D-020、D-032）、RC-INV-04（固定後不得修正初審；終止後不得修正、提交或再審）、RC-INV-05（初審醫師與複審醫師不同人，D-017）。`domain/reviewcase/` 目錄那一列仍是 grandfathered（`submission/` 等最後處理）。
9. **目前有效的複審提交（U-9）**：記錄複審修正改由 domain 方法檢查與執行（舊提交是目前有效的、重算 `finalScore`、改指新提交），對應修正初審的 `recordInitialReviewRevision`；`setLatestRereviewSubmissionId`、`updateFinalScore` 不再公開。行為不變。
10. **沒有被寫入的欄位（U-10 選 A）**：`rereviewRequired`、`finalConclusion`、`ruleSnapshotRef` 不在本次立法範圍，列進 `deferred_behaviors`，等讀取（切片 E）決定是否移除。
11. **加權取專案目前的版本**：維持 D-068 第 4 點的已知問題（`deferred_behaviors`），修正複審與提交複審相同。
12. **一開始是紅的範例**：照 D-066 第 5 點加進 `known-test-failures.txt`，由建構 PR 移除。
13. **順序**：本裁決、規格、`law/coverage.txt`、`known-test-failures.txt`、文件在同一個修法 PR；之後是建構 PR。修法 PR 不夾帶程式（P4）。

### Relationship / Evidence

- **起因**：題目清單「還沒立法的 ReviewCase 範圍」切片 D；負責人 2026-11-24 在 #157 合併後決定 `ReviewCase.java` 提早到本切片，避免建構 PR 一再依先例修改未立法的檔案。
- **草案與查證**：`docs/harness/34-revise-and-reviewcase-draft.md`（main = `623552a`）。以暫時的規格範例走真實寫入與查詢路徑實測：必答題沒答、沒有變更都能修正成功；修正複審沒有 `REVISED` 紀錄；分數重算正確。
- **與既有裁決的關係**：沿用 D-006、D-012、D-017、D-020、D-031、D-032、D-033、D-038、D-053、D-054、D-067、D-068、D-071、D-076。

### Mapping

- `specs/review-case/usecases.yaml`（`ReviseInitialReview`、`ReviseRereview`）、`model.yaml`（RC-INV-03～05、`deferred_behaviors`）、`examples.yaml`（修正的範例；`not_covered` 的理由更新）
- `law/coverage.txt`（兩個 use case、`ReviseRereviewCommand`、`RevisionResult`、兩個事件、`ReviewCase.java`、`CaseStatus.java`、`ReviewerAssignment.java`、`ReviewCaseRepository.java`）
- `services/quality-inspection-service/known-test-failures.txt`
- `docs/02-ddd/domain-events.md`（§8.1、新增 §8.2）、`docs/03-contracts/api-spec.md`（§11.4、§12.4）
- 後續：建構 PR

## D-084 — ReviewCase 切片 E：讀取（案件詳情、提交明細、讀取授權、分派頁查詢）

### Status

`accepted`（2026-11-25，負責人對 `docs/harness/35-read-draft.md` 的 V-1～V-11 全部照建議裁決）

### Decision

1. **多角色與院區的順序（V-1 選 A）**：照 D-024 的順序（角色 → 載入 → 院區 → 規則），院區先於多角色。別院的雙角色使用者回 `HOSPITAL_SCOPE_MISMATCH`。案件詳情、提交明細與已立法的修正歷史（RC-RVH-01）共用這個順序；修正歷史的程式由建構 PR 改成符合它的規格。
2. **初審醫師看得到複審的內容（V-2 選 A）**：照程式。初審醫師在案件詳情看得到複審分數與 `finalScore`，也能讀複審提交的分數與評語。D-033 只保護複審醫師。
3. **複審醫師看得到初審醫師是誰（V-3 選 A）**：照程式。複審醫師的案件詳情有 `reviewerId`、`rereviewContext.initialReviewerId`，以及初審提交的 ID、版本、提交時間；沒有初審的答案、分數、評語，也沒有 `finalScore`（D-033）。
4. **SYSTEM_ADMIN 的提交明細（V-4 選 A）**：只輸出 permission-matrix §5.1 白名單裡有的欄位：`submissionId`、`caseId`、`stage`、`revisionNo`、`status`、`reviewerId`（`submittedBy`）、`submittedAt`，以及 `maskedFields`；拿掉 `createdAt`。
5. **退回的案件（V-5）**：分派頁在 `RETURNED` 是 `READONLY_BLOCKED`、`blockedReason = CASE_NOT_ASSIGNABLE_IN_CURRENT_STATUS`、`allowedActions` 是空的；候選人清單相同（`INITIAL_ASSIGNMENT_NOT_ALLOWED`）。退回的案件要先重開（D-080），才能改派（D-082）。候選人與分派頁共用同一份可執行動作的判斷。
6. **讀過完整初審的候選人（V-6 選 A）**：複審醫師的候選人若在本案的初審讀取者紀錄中，`selectable = false`、`blockedReason = REREVIEWER_HAS_READ_INITIAL`，與指派的 422 一致。候選人查詢只讀，不寫入紀錄。
7. **讀過完整初審就不得受派（V-7）**：`AssignRereviewer`、`ReassignRereviewer` 立法 `REREVIEWER_HAS_READ_INITIAL`（422，D-055 第 5 點），在其他檢查都通過之後（D-082 第 7 點）。以另一個角色登入的同一個使用者先讀、後受派，做為範例；案件詳情、提交明細、修正歷史的讀取都會留下紀錄。
8. **輸出欄位（V-8）**：
   - 案件詳情：完整投影（HOSPITAL_MANAGER、AUDITOR、REVIEWER）照程式的欄位；複審醫師的盲審投影沒有 `initialReviewScore`、`finalScore`、初審提交的 `totalScore`、`rereviewContext.initialTotalScore`、`rereviewContext.finalScore`；SYSTEM_ADMIN 只有 metadata 白名單（`caseId`、`caseStatus`、`projectName`、`hospitalId`、`reviewerId`、`rereviewerId`、`updatedAt`、`latestRevisionSummary`）。
   - 兩個 context（`initialReviewContext`、`rereviewContext`）的 `pageMode`、`blockedReason`、`allowedActions` 照程式立法（D-054 第 9 點的 `FIX_INITIAL_REVIEW`、D-032 結案後唯讀）。
   - 提交明細：遮罩時 `answers`、`comment`、`totalScore`、`attachmentRefsJson` 是 `null`，`maskedFields` 依序列出這四個。
   - `updatedAt`（D-037）：只有 SYSTEM_ADMIN 的案件詳情輸出它；其他角色不輸出。
9. **沒有被寫入的欄位（V-9 選 A，接 D-083 第 10 點）**：移除 `rereviewRequired`、`finalConclusion`、`ruleSnapshotRef`：domain、讀取模型、HTTP 回應（提交初審的回應、案件詳情、複審頁 bootstrap），並以新的 Flyway migration 刪除三個欄位（D-045：正式環境還沒上線）。前端型別與案件詳情頁的 `finalConclusion` 顯示另開前端題目處理。
10. **登記範圍（V-10）**：`GetReviewCaseDetailUseCase`、`GetSubmissionDetailUseCase`、`GetCaseAssignmentContextUseCase`、`ListCaseAssignmentCandidatesUseCase`、`ReviewReadAccessPolicy`、`CaseDetailMetadataMapper`、`InitialReviewReaderPort`、`RereviewerAssignmentHistoryPort`、`RereviewerAssignmentRecord`，以及 `query/` 裡這四個查詢用到的 view 與 port，登記為已立法（`specs/review-case/`）。`ReviewCaseController.java` 仍是 grandfathered（留給 HTTP 介面的切片）。
11. **`RevisionChecks.java`（V-11）**：D-083 建構 PR（#159）新增的共用檢查（RC-RVI-08/09、RC-RVR-08/09），登記為已立法。
12. **範例做不到的部分**：候選人清單（人員目錄的埠無法在範例裡替換，輸出是最外層的清單）、複審醫師讀別人的複審提交（做不出前提）、三個欄位的移除（執行器把 `null` 與沒有欄位視為相同），列進 `not_covered`，由建構 PR 以單元測試驗證。
13. **一開始是紅的範例**：照 D-066 第 5 點加進 `known-test-failures.txt`，由建構 PR 移除。
14. **順序**：本裁決、規格、`law/coverage.txt`、`known-test-failures.txt`、文件在同一個修法 PR；之後是建構 PR。修法 PR 不夾帶程式（P4）。

### Relationship / Evidence

- **起因**：題目清單「還沒立法的 ReviewCase 範圍」的最後一個切片 E。
- **草案與查證**：`docs/harness/35-read-draft.md`（main = `5cbc209`）。以暫時的規格範例走真實寫入與查詢路徑實測：退回的案件分派頁可改派而改派本身拒絕、讀過初審的候選人仍可選、「先讀、後受派」做得出範例、別院的雙角色使用者拿到 `ACCESS_DENIED`、SYSTEM_ADMIN 的提交明細多出 `createdAt`。
- **與既有裁決的關係**：沿用 D-010、D-017、D-024、D-032、D-033、D-035、D-037、D-045、D-047、D-054、D-055、D-058、D-080、D-082、D-083。第 9 點完成 D-083 第 10 點留下的題目。

### Mapping

- `specs/review-case/usecases.yaml`（`GetReviewCaseDetail`、`GetSubmissionDetail`、`GetCaseAssignmentContext`、`ListCaseAssignmentCandidates`；RC-ARR-04、RC-RRR-05；RC-RVH-01 的順序；錯誤代碼 `REREVIEWER_HAS_READ_INITIAL`）、`model.yaml`（RC-INV-06、`deferred_behaviors`）、`examples.yaml`（讀取的範例、兩個 actor；`not_covered` 更新）
- `law/coverage.txt`（第 10、11 點的檔案）
- `services/quality-inspection-service/known-test-failures.txt`
- `docs/03-contracts/api-spec.md`（§10.2、§10.3、§13.1）、`docs/02-ddd/domain-model.md`（ReviewCase 的欄位）
- 後續：建構 PR；前端移除 `finalConclusion`、`rereviewRequired` 的型別與顯示另列題目
