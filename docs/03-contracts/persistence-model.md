# Persistence Model v1

## 1. 文件目的

本文件定義品質審查系統的 persistence model，作為以下工作的依據：

- relational schema 設計
- aggregate 與 table 的映射
- version / snapshot / revision 的持久化策略
- constraint / unique key / foreign key 設計
- read model 與 transaction model 的邊界

本文件不重新定義 domain rule。  
若與以下文件衝突，應優先檢查：

1. 需求書
2. decision log
3. domain model
4. state machine
5. application service
6. API spec

---

## 2. 設計原則

1. 交易模型以 aggregate 邊界為核心，不以單純 CRUD 表為核心。
2. 已發布版本不可原地修改，需以 version record 持久化。
3. 已提交 submission 不可覆寫，revision 採 append-only。
4. 批次 / 案件 / submission 均需可追溯其 version / snapshot。
5. 同批次內不得重複抽中同一 `sourceCaseKey`。
6. 結果固定後及 `CLOSED` / `CANCELLED` 後不允許 revision（D-031／032），此規則主要由 application/domain guard 保護；DB 僅做輔助約束。
7. query / report / audit 可建立 read model，不必與 transaction table 完全同構。

---

## 3. 資料分層

本系統 persistence 分成四層：

1. Master Data
2. Transactional Domain Data
3. Version / Snapshot Data
4. Audit / Read Model Data

---

## 4. Master Data

## 4.1 `hospital`

### 用途
院區主檔。

### 主要欄位
```yaml id="u2n0v9"
hospital:
  hospital_id: varchar PK
  hospital_code: varchar unique
  hospital_name: varchar
  status: varchar
  created_at: datetime
  updated_at: datetime
```

---

## 4.2 `department`

### 用途

科別主檔。

### 主要欄位

```yaml id="nnoq2f"
department:
  department_id: varchar PK
  hospital_id: varchar FK -> hospital.hospital_id
  department_code: varchar
  department_name: varchar
  status: varchar
  created_at: datetime
  updated_at: datetime
```

### 約束

* unique: `(hospital_id, department_code)`

---

## 4.3 `system_user`

### 用途

使用者主檔。
實務上可對接既有 IAM / SSO；此表僅表示 persistence 參照需求。

```yaml id="vzl6dd"
system_user:
  user_id: varchar PK
  account: varchar unique
  display_name: varchar
  hospital_id: varchar nullable
  status: varchar
  created_at: datetime
  updated_at: datetime
```

---

## 4.4 `user_role_assignment`

### 用途

角色配置。

```yaml id="t0gl4s"
user_role_assignment:
  assignment_id: bigint PK
  user_id: varchar FK -> system_user.user_id
  role_code: varchar
  scope_type: varchar
  scope_value: varchar nullable
  created_at: datetime
```

### 備註

* 這是 authorization persistence，不是 domain aggregate 核心。

---

## 5. Transactional Domain Data

---

## 5.1 `review_project`

### 對應 aggregate

* `ReviewProject`

### 用途

審查專案主表，保存當前專案主體。

```yaml id="n9w9q1"
review_project:
  project_id: varchar PK
  hospital_id: varchar FK -> hospital.hospital_id
  project_name: varchar
  status: varchar
  current_version_id: varchar nullable
  effective_start_at: datetime
  effective_end_at: datetime
  created_by: varchar
  created_at: datetime
  updated_by: varchar
  updated_at: datetime
```

### 備註

* `current_version_id` 指向目前正式使用中的 published version
* 專案本體與 version 拆開，不把所有規則直接塞主表

---

## 5.2 `review_project_version`

### 對應 aggregate 內 entity

* `ProjectVersion`

### 用途

保存專案正式版本。

```yaml id="n10f0m"
review_project_version:
  project_version_id: varchar PK
  project_id: varchar FK -> review_project.project_id
  version_no: int
  version_status: varchar
  score_sheet_version_id: varchar nullable
  quality_threshold: decimal(10,2)
  effective_start_at: datetime
  effective_end_at: datetime
  published_by: varchar nullable
  published_at: datetime nullable
  checksum: varchar
  created_at: datetime
```

### 約束

* unique: `(project_id, version_no)`

### 備註

* 此表保存正式受控版本
* 抽選與案件不只會存 `project_version_id`，還會再存 snapshot 參照

---

## 5.3 `review_project_version_policy`

### 用途

保存專案版本的 policy 組件，可考慮 JSON 或拆表。

```yaml id="l03iyi"
review_project_version_policy:
  policy_id: bigint PK
  project_version_id: varchar FK -> review_project_version.project_version_id
  policy_type: varchar
  policy_json: json
  created_at: datetime
```

### `policy_type` 範例

* `REVIEW_TARGET_POLICY`
* `DATE_CRITERIA_POLICY`
* `SAMPLING_POLICY`
* `STRATIFICATION_POLICY`
* `REREVIEW_POLICY`
* `EXCLUDED_DEPARTMENTS`

### 備註

* 若之後發現某些 policy 需要高頻查詢，可再拆正規化表
* v1 可先以 JSON 為主，保留彈性

---

## 5.4 `score_sheet`

### 對應 aggregate

* `ScoreSheetDefinition`

### 用途

評分表主表。

```yaml id="is5vr9"
score_sheet:
  score_sheet_id: varchar PK
  project_id: varchar FK -> review_project.project_id
  score_sheet_name: varchar
  current_version_id: varchar nullable
  created_by: varchar
  created_at: datetime
  updated_by: varchar
  updated_at: datetime
```

---

## 5.5 `score_sheet_version`

### 用途

評分表版本主表。

```yaml id="ns73xx"
score_sheet_version:
  score_sheet_version_id: varchar PK
  score_sheet_id: varchar FK -> score_sheet.score_sheet_id
  version_no: int
  version_status: varchar
  checksum: varchar
  published_by: varchar nullable
  published_at: datetime nullable
  created_at: datetime
```

### 約束

* unique: `(score_sheet_id, version_no)`

---

## 5.6 `score_sheet_section`

### 用途

評分表版本下的章節。

```yaml id="af7i9u"
score_sheet_section:
  id: bigint PK (surrogate, JPA auto-generated)
  score_sheet_version_id: varchar FK -> score_sheet_version.score_sheet_version_id
  section_id: varchar (business key)
  title: varchar
  display_order: int
```

### 約束

* unique: `(score_sheet_version_id, section_id)`

### 備註

* `id` 為 JPA surrogate PK，用於管理 section→items parent-child 關聯
* `section_id` 為業務 key，由前端或 domain 指定，用於 API 與 domain logic

---

## 5.7 `score_sheet_item`

### 用途

評分表版本下的題目。

```yaml id="jlwm6m"
score_sheet_item:
  id: bigint PK (surrogate, JPA auto-generated)
  section_id: bigint FK -> score_sheet_section.id (surrogate FK for parent-child)
  item_id: varchar (business key)
  item_code: varchar
  title: varchar
  score_type: varchar
  max_score: decimal(10,2)
  weight: decimal(10,4)
  required_rule: varchar
  condition_expression: varchar nullable
  display_order: int
```

### 約束

* unique: `(score_sheet_version_id, item_id)` — 透過 section 間接關聯，domain 層強制
* unique: `(score_sheet_version_id, item_code)` — 透過 section 間接關聯，domain 層強制

> **注意：** 上述 uniqueness 由 domain layer aggregate invariant 強制（error code: `DUPLICATE_ITEM_ID` / `DUPLICATE_ITEM_CODE`），目前無 DB-level composite unique constraint，因跨 table 複合索引需額外設計。

### 備註

* `section_id` FK 參照 `score_sheet_section.id` (surrogate PK)，非 business key
* `required_rule` 為 enum string: `ALWAYS_REQUIRED`, `OPTIONAL`, `CONDITIONAL`
* `condition_expression` 僅在 `required_rule = CONDITIONAL` 時有值
* `visibility_rule` 欄位保留但尚未實作，待 B3/B4 定義具體行為後再加入（見 decision-log）

---

## 5.8 `sampling_batch`

### 對應 aggregate

* `SamplingBatch`

### 用途

抽選批次主表。

```yaml id="m6u1cw"
sampling_batch:
  batch_id: varchar PK
  project_id: varchar FK -> review_project.project_id
  project_version_id: varchar FK -> review_project_version.project_version_id
  execution_mode: varchar
  batch_status: varchar
  population_count: int
  eligible_count: int
  excluded_count: int
  selected_count: int
  retry_of_batch_id: varchar nullable FK -> sampling_batch.batch_id
  snapshot_checksum: varchar
  started_at: datetime
  finished_at: datetime nullable
  created_by: varchar
  created_at: datetime
```

### 備註

* 主表保留統計與核心參照
* 詳細 snapshot 另放 snapshot 表

---

## 5.9 `sampling_batch_snapshot`

### 用途

保存 batch 實際採用的 immutable rule snapshot。

```yaml id="nzi1lh"
sampling_batch_snapshot:
  snapshot_id: bigint PK
  batch_id: varchar unique FK -> sampling_batch.batch_id
  snapshot_schema_version: int
  snapshot_json: json
  checksum: varchar
  created_at: datetime
```

### 備註

* `batch_id` unique，代表每個 batch 只有一份正式 snapshot

---

## 5.10 `sampling_batch_message_log`

### 用途

批次執行訊息。

```yaml id="gomc8u"
sampling_batch_message_log:
  log_id: bigint PK
  batch_id: varchar FK -> sampling_batch.batch_id
  level: varchar
  message: text
  logged_at: datetime
```

---

## 5.11 `sampling_batch_selected_case`

### 用途

記錄批次抽中的案件來源參照。

```yaml id="0i3td1"
sampling_batch_selected_case:
  selected_case_row_id: bigint PK
  batch_id: varchar FK -> sampling_batch.batch_id
  source_case_key: varchar
  hospital_id: varchar FK -> hospital.hospital_id
  medical_record_type: varchar
  created_at: datetime
```

### 關鍵約束

* unique: `(batch_id, source_case_key)`

### 說明

* 這個 unique key 對應「同批次不得重複抽中同一案件」的 persistence guard。

---

## 5.12 `review_case`

### 對應 aggregate

* `ReviewCase`

### 用途

案件主表。

```yaml id="humlq9"
review_case:
  case_id: varchar PK
  batch_id: varchar FK -> sampling_batch.batch_id
  project_id: varchar FK -> review_project.project_id
  project_version_id: varchar FK -> review_project_version.project_version_id
  hospital_id: varchar FK -> hospital.hospital_id
  medical_record_key: varchar
  medical_record_type: varchar
  department_id: varchar nullable FK -> department.department_id
  review_target_type: varchar
  review_target_value: varchar
  reviewer_id: varchar nullable FK -> system_user.user_id
  rereviewer_id: varchar nullable FK -> system_user.user_id
  case_status: varchar
  due_at: datetime nullable
  rereview_required: boolean
  final_score: decimal(10,2) nullable
  final_conclusion: text nullable
  rule_snapshot_checksum: varchar
  score_sheet_version_id: varchar FK -> score_sheet_version.score_sheet_version_id
  latest_initial_submission_id: varchar nullable
  latest_rereview_submission_id: varchar nullable
  closed_at: datetime nullable
  cancelled_at: datetime nullable
  created_at: datetime
  updated_at: datetime
```

### 建議索引

* index: `(project_id, case_status)`
* index: `(hospital_id, case_status)`
* index: `(reviewer_id, case_status)`
* index: `(rereviewer_id, case_status)`
* index: `(batch_id)`

### 備註

* `closed_at` / `cancelled_at` 可協助查詢與 guard
* 真正禁止 revision 仍由 application/domain 控制，不建議只依 DB trigger 實作

---

## 5.13 `review_case_snapshot_ref`

### 用途

案件層級的 snapshot / version 參照資料。

```yaml id="1xf87u"
review_case_snapshot_ref:
  case_snapshot_ref_id: bigint PK
  case_id: varchar unique FK -> review_case.case_id
  batch_id: varchar FK -> sampling_batch.batch_id
  project_version_id: varchar FK -> review_project_version.project_version_id
  batch_snapshot_checksum: varchar
  score_sheet_version_id: varchar FK -> score_sheet_version.score_sheet_version_id
  quality_threshold: decimal(10,2)
  created_at: datetime
```

### 備註

* 可視情況與 `review_case` 合併部分欄位
* 這裡保留明確追溯層，方便審計

---

## 5.14 `review_submission`

### 對應 aggregate

* `ReviewSubmission`

### 用途

submission 主表，一筆記錄即一個 revision 版本。

```yaml id="usjlwm"
review_submission:
  submission_id: varchar PK
  case_id: varchar FK -> review_case.case_id
  stage: varchar
  reviewer_id: varchar FK -> system_user.user_id
  submission_status: varchar
  revision_no: int
  total_score: decimal(10,2)
  comment_text: text nullable
  submitted_at: datetime nullable
  superseded_by_submission_id: varchar nullable FK -> review_submission.submission_id
  based_on_score_sheet_version_id: varchar FK -> score_sheet_version.score_sheet_version_id
  score_sheet_snapshot_checksum: varchar
  created_at: datetime
  updated_at: datetime
```

### 關鍵約束

* unique: `(case_id, stage, revision_no)`

### 備註

* append-only model：revision 建新列
* 舊版被新 revision 取代後，可設 `submission_status=SUPERSEDED`

---

## 5.15 `review_submission_answer`

### 用途

保存 submission 各題答案。

```yaml id="jynq0e"
review_submission_answer:
  answer_row_id: bigint PK
  submission_id: varchar FK -> review_submission.submission_id
  item_id: varchar
  answer_value_text: text nullable
  answer_value_number: decimal(18,4) nullable
  answer_value_boolean: boolean nullable
  answer_value_json: json nullable
  derived_score: decimal(10,2) nullable
  created_at: datetime
```

### 約束

* unique: `(submission_id, item_id)`

### 備註

* 多型答案先以多欄位 + json 混合方式保存
* 之後若題型收斂可再簡化

---

## 5.16 `review_submission_snapshot`

### 用途

保存 submission 當下實際使用的 score sheet snapshot。

```yaml id="zfqx0w"
review_submission_snapshot:
  submission_snapshot_id: bigint PK
  submission_id: varchar unique FK -> review_submission.submission_id
  snapshot_schema_version: int
  snapshot_json: json
  checksum: varchar
  created_at: datetime
```

### 備註

* 每個 submission 一份正式 snapshot
* 保證未來可重現計分依據

---

## 5.17 `review_submission_attachment`

### 用途

submission 關聯附件。

```yaml id="cemh7d"
review_submission_attachment:
  attachment_row_id: bigint PK
  submission_id: varchar FK -> review_submission.submission_id
  file_id: varchar
  file_name: varchar
  content_type: varchar
  file_size: bigint nullable
  created_at: datetime
```

### 備註

* 二進位檔案不建議直接存在交易表
* 實際檔案可存 object storage / 文件系統 / ECM

---

## 6. Audit / Read Model Data

---

## 6.1 `audit_trail`

### 用途

記錄操作與事件軌跡。

```yaml id="bv8bja"
audit_trail:
  audit_id: bigint PK
  aggregate_type: varchar
  aggregate_id: varchar
  event_name: varchar
  actor_id: varchar nullable
  hospital_id: varchar nullable
  metadata_json: json
  occurred_at: datetime
```

### 備註

* 這是 audit / tracing 表，不是 domain truth

---

## 6.2 `outbox_event`

### 用途

保存待發送 integration event。

```yaml id="u3qygm"
outbox_event:
  outbox_id: bigint PK
  aggregate_type: varchar
  aggregate_id: varchar
  domain_event_name: varchar
  payload_json: json
  publish_status: varchar
  created_at: datetime
  published_at: datetime nullable
```

---

## 6.3 `review_case_read_model`

可選。
用於 dashboard / 列表查詢優化，不作為交易真實來源。

```yaml id="vqjpe5"
review_case_read_model:
  case_id: varchar PK
  hospital_id: varchar
  project_id: varchar
  batch_id: varchar
  case_status: varchar
  reviewer_id: varchar nullable
  rereviewer_id: varchar nullable
  due_at: datetime nullable
  rereview_required: boolean
  final_score: decimal(10,2) nullable
  latest_initial_submission_id: varchar nullable
  latest_rereview_submission_id: varchar nullable
  updated_at: datetime
```

---

## 7. Table 與 Aggregate 對照

| Aggregate / Entity     | 主要 Table                                                                                                      |
| ---------------------- | ------------------------------------------------------------------------------------------------------------- |
| `ReviewProject`        | `review_project`                                                                                              |
| `ProjectVersion`       | `review_project_version`, `review_project_version_policy`                                                     |
| `ScoreSheetDefinition` | `score_sheet`, `score_sheet_version`, `score_sheet_section`, `score_sheet_item`                               |
| `SamplingBatch`        | `sampling_batch`, `sampling_batch_snapshot`, `sampling_batch_message_log`, `sampling_batch_selected_case`     |
| `ReviewCase`           | `review_case`, `review_case_snapshot_ref`                                                                     |
| `ReviewSubmission`     | `review_submission`, `review_submission_answer`, `review_submission_snapshot`, `review_submission_attachment` |

---

## 8. 關鍵資料約束

### 8.1 同批次禁止重複抽中

* table: `sampling_batch_selected_case`
* unique: `(batch_id, source_case_key)`

### 8.2 同案件同階段 revisionNo 唯一

* table: `review_submission`
* unique: `(case_id, stage, revision_no)`

### 8.3 專案版本號唯一

* table: `review_project_version`
* unique: `(project_id, version_no)`

### 8.4 評分表版本號唯一

* table: `score_sheet_version`
* unique: `(score_sheet_id, version_no)`

### 8.5 每個 batch / submission 只綁一份正式 snapshot

* `sampling_batch_snapshot.batch_id` unique
* `review_submission_snapshot.submission_id` unique

---

## 9. 狀態與 revision 的持久化策略

### 9.1 Case terminal state

`review_case.case_status` 為：

* `CLOSED`
* `CANCELLED`

時，不允許新 revision。

### 9.2 DB 層保護策略

v1 建議：

* 主要靠 application service + domain invariant 保護
* DB 層只做必要 constraint 與索引
* 不急著上 trigger，避免把業務規則散落到資料庫

### 9.3 為什麼不先用 trigger 鎖死

因為：

* revision / reopen / correction 未來可能演進
* 過早把規則寫死在 DB trigger，會讓版本治理困難

---

## 10. JSON vs 正規化策略

### 建議原則

* 高治理、穩定結構、需查詢關聯的資料：正規化表
* 高彈性、低頻查詢、追溯用快照：JSON
* v1 優先採混合式

### 適合 JSON 的資料

* `review_project_version_policy.policy_json`
* `sampling_batch_snapshot.snapshot_json`
* `review_submission_snapshot.snapshot_json`
* 某些複雜答案結構的 `answer_value_json`

### 適合正規化的資料

* project / version / batch / case / submission 主體
* score sheet section / item
* selected case
* submission answer

---

## 11. 與權限模型的邊界

以下內容不建議直接塞進交易表作為 domain 欄位：

* system admin metadata only 規則
* hospital manager 是否可看完整作答
* auditor 授權院區範圍

這些應由：

* IAM / RBAC / ABAC
* query scope filter
* response masking
* read model view policy

處理，而不是塞進 `review_case` 或 `review_submission`。

---

## 12. 索引建議

### `review_case`

* `(hospital_id, case_status)`
* `(project_id, case_status)`
* `(reviewer_id, case_status)`
* `(rereviewer_id, case_status)`
* `(batch_id)`

### `review_submission`

* `(case_id, stage, revision_no)`
* `(reviewer_id, stage)`
* `(submitted_at)`

### `sampling_batch`

* `(project_id, batch_status, started_at)`
* `(retry_of_batch_id)`

### `audit_trail`

* `(aggregate_type, aggregate_id, occurred_at)`
* `(event_name, occurred_at)`

---

## 13. 後續演進方向

### v2 可能要補的內容

1. optimistic locking 欄位
2. idempotency key table
3. attachment domain 與檔案儲存契約
4. read model / dashboard projection schema
5. query materialized view
6. correction workflow 專用資料模型（若未來納入）

---

## 14. 不屬於本文件的事項

以下不在本文件定義範圍內：

* controller / API path
* DTO schema
* authorization 實作細節
* domain event payload 最終格式
* UI query shape
* OpenAPI 正式 YAML

---

## 15. 落地要求

本文件定義的 persistence model 應同步落地於：

* entity / repository 設計
* migration script
* unique / FK / index 設計
* snapshot persistence
* revision persistence
* outbox / audit persistence

若與其他文件衝突，應優先檢查：

1. 需求書
2. decision log
3. domain model
4. state machine
5. application service
6. API spec
7. persistence model

## 16. 固定與正式結案語意對齊（D-031～D-033）

沿用 §5.12 ReviewCase、§5.14 ReviewSubmission 與既有 aggregate／table 邊界，不在本輪新增 table、column、enum 或 migration。

* 初審 SUBMITTED 不等於固定；固定後禁止 revision，複審及無需複審的 manager 結案必須引用已固定初審結果。
* 複審 SUBMITTED 不等於 CLOSED 或固定；未固定、未結案時合法原提交者可 append-only revision，保留 revisionNo／SUPERSEDED 鏈與有效版本關聯。
* 正式結案與固定尚未固定的複審結果須保持業務一致性；CLOSED 後不得寫任何 revision 或 reopen。CANCELLED terminal 不變。
* finalScore 依固定初審結果、最新合法複審結果與已發布版本權重按 D-012／020 計算；固定不是改權重或改 snapshot。
* read model／audit／export 向同案複審醫師輸出時，永久排除初審答案／分數／評語及可反推初審分數的衍生值（D-033）；不修改 domain 原始資料，也不改其他角色政策。
* 固定的儲存表示、複審已提交未結案狀態表示、併發／交易設計、既有資料遷移為 unknown；本輪不把新增規則宣稱為已存在 DB 保證。

## DD-03 固定持久化與併發保證

必須可持久追溯固定的 case、當時有效 INITIAL submission、操作者及時間；不得以目前指派醫師、查詢時間或可變動的 latest 指標重建歷史固定事實。保留 append-only revision／SUPERSEDED 鏈，固定後不得覆寫結果或換有效版本。
有效版本驗證、固定事實、追溯資料與依固定結果產生的流向須維持一致；不得出現固定已成功但追溯缺漏、固定舊版卻採新版分數、或固定後又成功 revision。revision 先完成則舊版固定不可成功；固定先完成則 revision 不可成功。失敗不得留下部分成功事實或成功事件。
以上是待實作的可觀察一致性契約；實體 table／column、時間表示、鎖定／版本控制、交易邊界／隔離與發布機制、migration／舊資料補值／相容性、重複固定策略均為 UNKNOWN。不新增 DB 設計、不執行 migration、不宣稱現有 DB 已保證。


## D-034 複審獨立 Close Command 對齊

複審 SUBMITTED 與正式 CLOSED 須可區別；Close Command 只可引用執行當下合法有效 REREVIEW submission，保留既有 append-only／SUPERSEDED 鏈。成功結案須一致保存有效版本引用、尚未固定結果的固定事實、原公式計分及 CLOSED；既有固定初審輸入不可被更換。不得只由目前 assignment 或歷史提交推定結案資格。
revision 先完成則舊版 close 不得成功；close 先完成則不能再寫 revision；保存或事件處理失敗不得留下部分成功結案／固定及成功事件。此為可觀察一致性要求，不宣稱現有 DB 已保證。
欄位／table／constraint、鎖定／版本／交易與事件技術、提交後狀態表示、close 重試持久化、legacy 補正／遷移均 UNKNOWN；不得以 submittedAt／closedAt 或指派者補造未確認歷史。read model 與 audit 出口遵守 D-033，包括結案後的初審及衍生資訊遮罩。本次不改 DB、不執行 migration。


## D-037 — updatedAt 契約

案件層級持久化 updatedAt 與業務變更同交易，rollback 不留時間；並行不倒退且 touch 不得造成資料遺失，不要求 commit 排序。欄位／型別、migration、Clock、併發技術待決；legacy 無可信歷史保留 null。現有 schema 不具備案件欄位，本輪不得新增 schema 或 migration。


## IMP-02A-MD-03 — updatedAt 最小技術設計（PROPOSED，待審，未實作）

> D-045（2026-11-12）：併發保護改採樂觀鎖。下方「建議最小併發機制：案件父列交易鎖」一節已被取代，僅供參考；其餘內容仍為待審提案。

### 現有能力與部署邊界

ReviewCase／ReviewCaseEntity 無通用時間欄位；ReviewCaseRepositoryImpl.toEntity 每次建立完整 Entity 再 save，不能拿過期 Domain 快照只為 touch 而整筆覆寫。Submission 已有 LocalDateTime updatedAt，但會在相同 Draft Save 重設，不能作案件可信來源。ReviewCaseJpaRepository 僅繼承 JpaRepository，無 lock／version／時間更新方法。

application.yml 的共用 ddl-auto=none、local H2 memory profile 為 update；測試使用既有 create-drop H2。build.gradle 未宣告 Flyway／Liquibase，repo 未確認 migration 工具、正式資料庫及 timestamp 精度。現有能力不足以執行下述機制驗證；本輪不增加欄位、DDL 或 migration。H2 僅能驗證未來本地機制，不能代替未知正式 DB 驗收。

### Domain／Entity／DB／Query

建議 ReviewCase 與 ReviewCaseEntity 使用 nullable Instant updatedAt，Repository 重建／保存均映射，DB 欄位候選名 updated_at。UTC SQL 型別／JDBC 行為依實際 DB 與 provider 驗證，不預先指定跨 DB DDL。注入共享 Clock.systemUTC 作操作時間來源，避免依 JVM default timezone；JSON 使用既有 Jackson Java time 支援輸出可解析的 UTC ISO 8601（Z 或 +00:00）。Query 從可信持久化欄位映射到既有八欄投影，不新增欄位、不得讀取時補 now()。

createFromSampling 的正式建案及 retry 新案件，與 local seed 的新增案件均初始化；重建 legacy 不初始化。案件時間與業務資料同交易，第一次成功真實更新可由 null 轉 UTC 值；無變更的 legacy Draft 仍維持 null。舊資料不回填不可靠的 batch／audit／submission 最大時間。

### 建議最小併發機制：案件父列交易鎖

1. 每個既有案件寫入入口先驗角色，再在同一既有交易以 PESSIMISTIC_WRITE 取得該 ReviewCase 列並讀取新鮮狀態，之後驗 scope／owner／terminal／有效引用；不得沿用鎖前已讀的過期快照。這是待審的 Repository 查詢擴充，不宣稱目前已存在。
2. 需要更新 Submission 時，統一先鎖 Case、再讀／改相關 Submission；Initial／Rereview Draft 的全部 Guards 完成後才比較內容。無變更拋 NO_CHANGES 的既有 409 類型路徑，無 Case／S save、時間 touch 或事件。讀取列鎖不是業務更新。
3. 真實變更時候選時間取共享 UTC Clock；持有父列鎖下設定 max(目前可信 updatedAt, 候選時間)，null 則用候選值。時間可相等，不要求嚴格遞增／commit 排序；時鐘倒退亦不倒退。
4. Case-only：保存新鮮且已鎖定的 Case 業務值與時間。Submission-only：保存 S 並在同交易保存同一新鮮 C 的時間；不得從舊快照建立整筆 Case 來 touch。Submit／Revision 涉及多筆 S／C 使用同一操作候選時間，統一一次案件時間推進。
5. 鎖維持至 commit／rollback。任何業務 save、時間保存或 Audit listener 失敗都撤銷同交易資料與時間；不得 REQUIRES_NEW、after-commit 補時間或吞掉例外。鎖等待／deadlock 的現有錯誤映射與 timeout 尚 UNKNOWN，不新增未接受 HTTP 規則。
6. 所有同案 C／S writer 必須遵循同一順序，包括指派／改派、Return／Reopen／Cancel／Close／Send、Start／Draft／Submit／Revision，以及以後新增入口。新建案無既有父列，用插入交易初始化。local seed 只新增缺少案件，須明確交易化；repo 外 SQL／其他 writer 不遵守時不能提供保證。

此機制依靠序列化同案 writer 與重新讀取避免時間 touch 遺失其他欄位，不只靠 max()；沒有納入全部 writer 就不成立。若改採單欄原子 SQL touch，仍無法單獨解決既有完整 Case merge 的 stale overwrite，不能宣稱同等保證。僅增加 @Version 也需完整版本映射及衝突行為，不是本輪既有能力。

### Migration／Legacy

待正式 DB 與 migration 管理方式確認後，最小部署是 nullable 欄位、無預設 now()、無全表歷史回填，再發布完整映射／writer 整合。舊版本 writer 在滾動部署期間可能不維護時間，須停寫切換或另定相容部署窗口；不假定加欄位即可達成全路徑保證。若未確認，migration 落地 BLOCKED。

### 必要整合測試與可執行性

- 現在可執行：兩條 Draft HTTP No-change Red、changed-save、Guard 優先、原資料 rollback 對照及既有盲審／八欄回歸。
- 待欄位／機制就緒：獨立交易重新查詢案件時間；逐個 writer 對應；建案／legacy；業務、Case 時間、Audit 寫入失敗後三者共同回復。
- 兩個連線／獨立交易，以 latch／barrier 控制交錯：A 保持父列鎖，B 開始同案更新；A commit 後 B 必須讀最新資料；分別測 A／B commit 及 A rollback，驗證最終業務欄位、S chain、時間及 Audit。測不同候選時間與倒退 Clock、相等值、null 首次更新。
- 重現舊快照覆寫：A 修改合法指派／狀態，B 僅修改 S／時間，B 完成不得撤回 A 欄位；測 Case-only、Submission-only 與跨入口組合。不得以只有時間 max 通過代替資料不遺失。
- 驗 no-change 在鎖後 Guards 優先且零 update／insert；驗鎖失敗不產生成功事件／時間。正式 DB 須重跑相同競爭案例，H2 PASS 不代表正式 PASS。

目前沒有案件時間欄位或鎖查詢，以上 timestamp／併發整合測試尚不能對正式機制執行，明確 BLOCKED；本節不是 accepted technical decision 或已完成實作。
