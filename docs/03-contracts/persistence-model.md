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
6. `CLOSED` / `CANCELLED` 後不允許 revision，此規則主要由 application/domain guard 保護；DB 僅做輔助約束。
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
  section_row_id: bigint PK
  score_sheet_version_id: varchar FK -> score_sheet_version.score_sheet_version_id
  section_id: varchar
  title: varchar
  display_order: int
```

### 約束

* unique: `(score_sheet_version_id, section_id)`

---

## 5.7 `score_sheet_item`

### 用途

評分表版本下的題目。

```yaml id="jlwm6m"
score_sheet_item:
  item_row_id: bigint PK
  score_sheet_version_id: varchar FK -> score_sheet_version.score_sheet_version_id
  section_id: varchar
  item_id: varchar
  item_code: varchar
  title: varchar
  score_type: varchar
  max_score: decimal(10,2)
  weight: decimal(10,4)
  required_rule_json: json
  visibility_rule_json: json
  display_order: int
```

### 約束

* unique: `(score_sheet_version_id, item_id)`
* unique: `(score_sheet_version_id, item_code)`

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
