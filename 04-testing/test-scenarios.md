# Test Scenarios v1

## 1. 文件目的

本文件定義品質審查系統的核心測試情境，作為以下工作的驗證基礎：

- domain rule 驗證
- application service 驗證
- API acceptance 驗證
- regression 測試
- UAT / SIT / integration 測試案例整理

本文件不取代單元測試程式碼，也不取代 OpenAPI contract test。  
它的角色是把已定稿的業務規則與設計決策，轉成可驗證情境。

---

## 2. 測試設計原則

1. 優先驗證不可違反的 domain invariant。
2. 優先驗證 state transition 與 terminal state。
3. 優先驗證 revision 與 version / snapshot 追溯。
4. 權限測試需同時驗證：
   - 可否操作
   - 可否看到完整內容
   - scope 是否正確
5. 若需求書、decision log 與 API spec 同時涉及同一規則，至少要有一個 acceptance scenario 覆蓋。

---

## 3. 測試分組

本文件將測試情境分為：

1. Project / Version
2. Sampling
3. Assignment and Initial Review
4. Re-review
5. Revision
6. Permission / Visibility
7. Traceability / Audit
8. Reporting / Query
9. Negative / Conflict Cases

---

## 4. Project / Version Scenarios

---

## TS-PV-001 建立審查專案草稿成功

### Objective
驗證院方管理者可建立審查專案草稿。

### Preconditions
- actor = `HOSPITAL_MANAGER`
- hospital 存在
- 必填欄位完整

### Steps
1. 呼叫 `POST /review-projects`
2. 提供合法專案資料

### Expected
- 回傳 `success=true`
- 產生 `projectId`
- `status = DRAFT`
- audit trail 記錄 `ReviewProjectCreated`

---

## TS-PV-002 非院方管理者不可建立審查專案

### Objective
驗證角色限制。

### Preconditions
- actor = `REVIEWER` 或 `SYSTEM_ADMIN`

### Steps
1. 呼叫 `POST /review-projects`

### Expected
- 回傳 `403 FORBIDDEN`
- 不建立任何 project

---

## TS-PV-003 發布專案版本成功

### Objective
驗證 draft / 可發布版本可被正式發布。

### Preconditions
- actor = `HOSPITAL_MANAGER`
- 專案存在
- 綁定合法 score sheet version
- 專案處於可發布狀態

### Steps
1. 呼叫 `POST /review-projects/{projectId}/publish-version`

### Expected
- 建立 `projectVersionId`
- `versionNo` 遞增
- `ReviewProjectVersionPublished` event 成立
- 舊 version 不被覆寫

---

## TS-PV-004 已發布版本不可直接 update 覆寫

### Objective
驗證正式版本不可原地修改。

### Preconditions
- 專案已有 published version

### Steps
1. 嘗試以一般 update API 直接修改已發布版本內容

### Expected
- 回傳 `409 CONFLICT` 或 `422 UNPROCESSABLE_ENTITY`
- 已發布 version 不被改寫
- 若要修改，必須建立新 draft / 新 version

---

## 5. Sampling Scenarios

---

## TS-SM-001 預估母體成功

### Objective
驗證 preview sampling 可回傳母體 / 符合 / 排除統計，且不建立正式 batch。

### Preconditions
- actor = `HOSPITAL_MANAGER`
- 專案為 active
- project version 已發布

### Steps
1. 呼叫 `POST /review-projects/{projectId}/sampling/preview`

### Expected
- 回傳 population / eligible / excluded count
- 不建立 `sampling_batch`
- 不建立 `review_case`

---

## TS-SM-002 執行抽選成功

### Objective
驗證正式抽選可建立 batch 與案件。

### Preconditions
- actor = `HOSPITAL_MANAGER`
- 專案為 active
- 使用 published version
- 母體可成功取得

### Steps
1. 呼叫 `POST /review-projects/{projectId}/sampling/execute`

### Expected
- 建立 `sampling_batch`
- 建立 `sampling_batch_snapshot`
- 建立 `sampling_batch_selected_case`
- 建立對應 `review_case`
- 回傳 `batchId`
- `SamplingBatchCompleted` event 成立

---

## TS-SM-003 專案未啟用不得抽選

### Objective
驗證 inactive project 不可執行抽選。

### Preconditions
- actor = `HOSPITAL_MANAGER`
- project status != active

### Steps
1. 呼叫 `POST /review-projects/{projectId}/sampling/execute`

### Expected
- 回傳 `422 PROJECT_NOT_ACTIVE`
- 不建立 batch
- 不建立 case

---

## TS-SM-004 同批次不得重複抽中同一 sourceCaseKey

### Objective
驗證 duplicate sampling rule 僅限制同批次。

### Preconditions
- actor = `HOSPITAL_MANAGER`
- 某次抽選結果中，抽樣邏輯嘗試兩次選到相同 `sourceCaseKey`

### Steps
1. 執行抽選
2. 模擬同 batch 內重複插入相同 `sourceCaseKey`

### Expected
- 因 `(batch_id, source_case_key)` unique constraint 或 domain invariant 失敗
- 回傳 `409 DUPLICATE_SELECTED_CASE_IN_BATCH`
- 該 batch 不應 silently 接受重複資料

---

## TS-SM-005 不同批次可再次抽中相同 sourceCaseKey

### Objective
驗證 duplicate rule 不跨 batch。

### Preconditions
- 前一 batch 已抽中某 `sourceCaseKey`
- 新 batch 合法執行

### Steps
1. 在新 batch 再次抽中相同 `sourceCaseKey`

### Expected
- 允許建立新 batch selected case
- 不因歷史 batch 被阻擋

---

## TS-SM-006 失敗批次重跑會建立新 batch

### Objective
驗證 retry 不覆寫舊 batch。

### Preconditions
- 某 batch 狀態 = `FAILED`

### Steps
1. 呼叫 `POST /sampling-batches/{batchId}/retry`

### Expected
- 建立新 `batchId`
- 新 batch `retryOfBatchId = old batchId`
- 舊 batch 保留原錯誤紀錄
- 不覆寫舊 batch 為 completed

---

## 6. Assignment and Initial Review Scenarios

---

## TS-IR-001 指派初審醫師成功

### Objective
驗證 manager 可指派 reviewer。

### Preconditions
- actor = `HOSPITAL_MANAGER`
- case status = `SELECTED_PENDING_ASSIGNMENT`

### Steps
1. 呼叫 `POST /review-cases/{caseId}/assign-reviewer`

### Expected
- case status -> `PENDING_INITIAL_REVIEW`
- reviewerId / dueAt 被寫入
- `ReviewCaseAssigned` event 成立

---

## TS-IR-002 非 manager 不可指派初審醫師

### Objective
驗證角色限制。

### Preconditions
- actor = `REVIEWER` 或 `SYSTEM_ADMIN`

### Steps
1. 呼叫 assign reviewer API

### Expected
- 回傳 `403 FORBIDDEN`

---

## TS-IR-003 已指派初審醫師可開始初審

### Objective
驗證合法 reviewer 可開始初審。

### Preconditions
- actor = assigned `REVIEWER`
- case status = `PENDING_INITIAL_REVIEW`

### Steps
1. 呼叫 `POST /review-cases/{caseId}/initial-review/start`

### Expected
- case status -> `INITIAL_REVIEW_IN_PROGRESS`
- `InitialReviewStarted` event 成立

---

## TS-IR-004 非被指派 reviewer 不可開始初審

### Objective
驗證 ownership guard。

### Preconditions
- actor = 其他 reviewer
- case status = `PENDING_INITIAL_REVIEW`

### Steps
1. 呼叫 start initial review

### Expected
- 回傳 `403 FORBIDDEN`

---

## TS-IR-005 保存初審草稿成功

### Objective
驗證 draft save 不應視為正式提交。

### Preconditions
- actor = assigned reviewer
- case status = `INITIAL_REVIEW_IN_PROGRESS`

### Steps
1. 呼叫 `POST /review-cases/{caseId}/initial-review/draft`

### Expected
- 建立或更新 `review_submission`
- submission status = `DRAFT`
- case 主狀態仍維持 `INITIAL_REVIEW_IN_PROGRESS`

---

## TS-IR-006 提交初審且低於品質線，直接結案

### Objective
驗證初審直接結案路徑。

### Preconditions
- actor = assigned reviewer
- case status = `INITIAL_REVIEW_IN_PROGRESS`
- totalScore <= qualityThreshold

### Steps
1. 提交初審

### Expected
- submission status = `SUBMITTED`
- case status 先完成初審，再進入 `CLOSED`
- 不建立待複審狀態
- `ReviewCaseClosed` event 成立

---

## TS-IR-007 提交初審且高於品質線，進入複審

### Objective
驗證進複審路徑。

### Preconditions
- actor = assigned reviewer
- totalScore > qualityThreshold

### Steps
1. 提交初審

### Expected
- submission status = `SUBMITTED`
- case status -> `INITIAL_REVIEW_COMPLETED`
- 隨後 -> `PENDING_REREVIEW`
- `ReviewCaseEscalatedToRereview` event 成立

---

## TS-IR-008 初審中可退回

### Objective
驗證退回流程。

### Preconditions
- case status = `INITIAL_REVIEW_IN_PROGRESS`

### Steps
1. 執行 return initial review command

### Expected
- case status -> `RETURNED`
- 必須有 reason
- `ReviewCaseReturned` event 成立

---

## 7. Re-review Scenarios

---

## TS-RR-001 指派複審醫師成功

### Objective
驗證 manager 可指派 rereviewer。

### Preconditions
- actor = `HOSPITAL_MANAGER`
- case status = `PENDING_REREVIEW`

### Steps
1. 呼叫 `POST /review-cases/{caseId}/assign-rereviewer`

### Expected
- rereviewerId 寫入
- `RereviewerAssigned` event 成立

---

## TS-RR-002 已指派複審醫師可開始複審

### Objective
驗證合法 rereviewer 可開始複審。

### Preconditions
- actor = assigned `REREVIEWER`
- case status = `PENDING_REREVIEW`

### Steps
1. 呼叫 `POST /review-cases/{caseId}/rereview/start`

### Expected
- case status -> `REREVIEW_IN_PROGRESS`
- `RereviewStarted` event 成立

---

## TS-RR-003 複審提交後結案

### Objective
驗證複審提交與 final score 決定權。

### Preconditions
- actor = assigned rereviewer
- case status = `REREVIEW_IN_PROGRESS`

### Steps
1. 提交複審

### Expected
- submission status = `SUBMITTED`
- final score 由複審 submission 決定
- case status -> `CLOSED`
- `RereviewCompleted` 與 `ReviewCaseClosed` event 成立

---

## TS-RR-004 非複審醫師不可提交複審

### Objective
驗證複審 ownership guard。

### Preconditions
- actor != assigned rereviewer

### Steps
1. 呼叫複審提交 API

### Expected
- 回傳 `403 FORBIDDEN`

---

## 8. Revision Scenarios

---

## TS-RV-001 原審查醫師可修正已提交初審

### Objective
驗證初審 revision 正常路徑。

### Preconditions
- actor = original reviewer
- submission stage = `INITIAL`
- submission status = `SUBMITTED`
- case status != `CLOSED`
- case status != `CANCELLED`

### Steps
1. 呼叫 `POST /review-submissions/{submissionId}/initial-review/revise`

### Expected
- 建立新 submission
- 新 submission `revisionNo = old + 1`
- 舊 submission status -> `SUPERSEDED`
- `ReviewSubmissionRevised` event 成立
- case 主狀態不因 revision 自動改變

---

## TS-RV-002 非原審查醫師不可修正初審

### Objective
驗證初審 revision actor guard。

### Preconditions
- actor != original reviewer
- stage = `INITIAL`

### Steps
1. 呼叫初審 revise API

### Expected
- 回傳 `403 REVISION_ACTOR_MISMATCH`

---

## TS-RV-003 複審醫師可修正已提交複審

### Objective
驗證複審 revision 正常路徑。

### Preconditions
- actor = assigned rereviewer
- submission stage = `REREVIEW`
- case status != `CLOSED`
- case status != `CANCELLED`

### Steps
1. 呼叫 `POST /review-submissions/{submissionId}/rereview/revise`

### Expected
- 建立新 revision
- 舊 submission -> `SUPERSEDED`
- case 主狀態不自動回退
- `ReviewSubmissionRevised` event 成立

---

## TS-RV-004 非複審醫師不可修正複審

### Objective
驗證複審 revision actor guard。

### Preconditions
- actor != assigned rereviewer

### Steps
1. 呼叫複審 revise API

### Expected
- 回傳 `403 REVISION_ACTOR_MISMATCH`

---

## TS-RV-005 結案後不可 revision

### Objective
驗證 `CLOSED` terminal rule。

### Preconditions
- case status = `CLOSED`

### Steps
1. 呼叫任一 revise API

### Expected
- 回傳 `409 CASE_ALREADY_CLOSED` 或 `422 REVISION_NOT_ALLOWED`
- 不建立新 revision
- 不產生 `ReviewSubmissionRevised`

---

## TS-RV-006 取消後不可 revision

### Objective
驗證 `CANCELLED` terminal rule。

### Preconditions
- case status = `CANCELLED`

### Steps
1. 呼叫 revise API

### Expected
- 回傳 `409 CASE_ALREADY_CANCELLED` 或 `422 REVISION_NOT_ALLOWED`
- 不建立新 revision

---

## TS-RV-007 revision 不得覆寫舊 submission

### Objective
驗證 append-only model。

### Preconditions
- 存在已提交 submission
- actor 合法

### Steps
1. 發起 revision
2. 檢查 DB / response

### Expected
- 舊 submission 內容不變
- 新 submission 另立新 id
- `revisionNo` 遞增
- 舊版 status = `SUPERSEDED`

---

## TS-RV-008 revision 不等於 reopen

### Objective
驗證 revision 不改變 case lifecycle。

### Preconditions
- case status = `PENDING_REREVIEW` 或其他未結案狀態
- 發起合法 revision

### Steps
1. 執行 revision

### Expected
- 不產生 reopen 行為
- case 主狀態維持原狀
- 只新增 submission revision

---

## 9. Permission / Visibility Scenarios

---

## TS-PM-001 manager 可查看本院完整作答明細

### Objective
驗證 HOSPITAL_MANAGER 的 full detail view。

### Preconditions
- actor = `HOSPITAL_MANAGER`
- case 屬於 actor 所屬 hospital

### Steps
1. 查詢 case detail
2. 查詢 submission detail

### Expected
- 可取得 answers / comments / attachments metadata
- 不被 metadata-only 遮罩

---

## TS-PM-002 manager 不可查看他院完整明細

### Objective
驗證 manager scope。

### Preconditions
- actor = `HOSPITAL_MANAGER`
- case 屬於其他 hospital

### Steps
1. 查詢 detail API

### Expected
- 回傳 `403 FORBIDDEN` 或查無資料（依統一策略）
- 不可取得 full detail

---

## TS-PM-003 system admin 只能看 metadata

### Objective
驗證 SYSTEM_ADMIN 資料遮罩。

### Preconditions
- actor = `SYSTEM_ADMIN`

### Steps
1. 查詢 case detail
2. 查詢 submission detail

### Expected
- 僅回 metadata view
- 不含 answers / full comments / attachment content

---

## TS-PM-004 auditor 僅能查授權院區

### Objective
驗證 AUDITOR scope。

### Preconditions
- actor = `AUDITOR`
- 具有授權院區 A
- 無授權院區 B

### Steps
1. 查詢 A 院區案件
2. 查詢 B 院區案件

### Expected
- A 可查
- B 回 `403` 或查無資料
- 不具預設 global scope

---

## TS-PM-005 reviewer 只能看自己合法範圍內案件

### Objective
驗證 reviewer scope。

### Preconditions
- actor = `REVIEWER`
- 有 case A 指派給自己
- case B 指派給其他 reviewer

### Steps
1. 查 A detail
2. 查 B detail

### Expected
- A 可查
- B 不可查或僅極小 metadata（依實作策略）
- 不可取得他人 submission 明細

---

## 10. Traceability / Audit Scenarios

---

## TS-TR-001 case 可追溯 batch / project version / score sheet version

### Objective
驗證案件追溯能力。

### Preconditions
- 已完成抽選並建立 case

### Steps
1. 查 case detail / lifecycle / DB
2. 追查其 batch / projectVersion / scoreSheetVersion / snapshot checksum

### Expected
- 可找到完整關聯
- case 可追溯：
  - project
  - batch
  - project version
  - score sheet version
  - snapshot reference

---

## TS-TR-002 submission 可追溯 score sheet snapshot

### Objective
驗證提交可重現計分基礎。

### Preconditions
- 已有 submission

### Steps
1. 查 submission
2. 查 submission snapshot

### Expected
- submission 對應一份 immutable score sheet snapshot
- 可取得 checksum / snapshot schema version

---

## TS-TR-003 revision 歷程完整保留

### Objective
驗證 revision history。

### Preconditions
- 已對同一 submission 做至少一次 revision

### Steps
1. 查 `GET /review-cases/{caseId}/revisions`

### Expected
- 顯示 revisionNo 1, 2, ...
- 舊版保留
- 舊版狀態 = `SUPERSEDED`
- 最新版 = `SUBMITTED`

---

## TS-TR-004 batch retry 保留原批次與關聯

### Objective
驗證 failed batch retry traceability。

### Preconditions
- 有 failed batch，後續執行 retry

### Steps
1. 查舊 batch
2. 查新 batch

### Expected
- 舊 batch 保留 failed 記錄
- 新 batch 有 `retryOfBatchId`
- 不發生原紀錄被覆寫

---

## 11. Reporting / Query Scenarios

---

## TS-RP-001 dashboard summary 可依 hospital / project / date 查詢

### Objective
驗證 summary query 正常。

### Preconditions
- 有多筆 case / submission / batch 資料

### Steps
1. 呼叫 dashboard summary API

### Expected
- 可回傳：
  - 審查完成率
  - 逾期率
  - 評分分布
  - 複審率

---

## TS-RP-002 system admin 查 dashboard 只能看 aggregated / metadata

### Objective
驗證 system admin 的查詢視角。

### Preconditions
- actor = `SYSTEM_ADMIN`

### Steps
1. 查 dashboard
2. 查 detail 類 query

### Expected
- dashboard summary 可查
- 不可看到完整作答細節

---

## TS-RP-003 auditor 匯出統計報表受授權院區限制

### Objective
驗證 auditor export scope。

### Preconditions
- actor = `AUDITOR`
- 有授權院區範圍

### Steps
1. 匯出授權院區報表
2. 匯出未授權院區報表

### Expected
- 授權範圍成功
- 未授權範圍被拒絕

---

## 12. Negative / Conflict Scenarios

---

## TS-NG-001 非法狀態轉移：未分派就開始初審

### Objective
驗證 domain/state guard。

### Preconditions
- case status = `SELECTED_PENDING_ASSIGNMENT`

### Steps
1. 呼叫 start initial review

### Expected
- 回傳 `409 INVALID_STATE_TRANSITION` 或 `422`
- case 狀態不變

---

## TS-NG-002 非法狀態轉移：未進入複審就開始複審

### Objective
驗證 rereview state guard。

### Preconditions
- case status != `PENDING_REREVIEW`

### Steps
1. 呼叫 start rereview

### Expected
- 拒絕
- 狀態不變

---

## TS-NG-003 使用未發布 project version 執行正式抽選

### Objective
驗證 publish guard。

### Preconditions
- project 只有 draft version

### Steps
1. 執行 sampling execute

### Expected
- 回傳 `422 PROJECT_NOT_ACTIVE` 或等價錯誤
- 不建立 batch

---

## TS-NG-004 submission stage 與 revise API 不符

### Objective
驗證 stage-specific revision API guard。

### Preconditions
- submission stage = `REREVIEW`

### Steps
1. 呼叫 initial-review revise API

### Expected
- 回傳 `422 SUBMISSION_STAGE_MISMATCH`

---

## TS-NG-005 嘗試 update 已提交 submission 取代 revision

### Objective
驗證 API 與 model 不提供覆寫路徑。

### Preconditions
- 已存在 submitted submission

### Steps
1. 嘗試呼叫 generic update submission API（若實作不存在則直接驗證無此 API）
2. 或嘗試以 update 語義覆寫提交

### Expected
- 不存在此 API 或被拒絕
- 系統只接受 revise command

---

## 13. 測試資料建議

### 建議至少準備
- 2 個 hospital
- 3 個 department
- 1 個 active project
- 1 個 inactive project
- 1 個 published score sheet version
- 1 組 reviewer / rereviewer / manager / auditor / sysadmin
- 至少 1 個直接結案 case
- 至少 1 個進複審 case
- 至少 1 個 failed batch + retry case
- 至少 1 組 revision case

---

## 14. 測試層級建議對照

| 類型 | 適合覆蓋的 scenario |
|---|---|
| Domain Unit Test | state guard, invariant, revision rule, duplicate-in-batch |
| Application Service Test | actor ownership, orchestration, publish flow, retry flow |
| API Integration Test | auth, response shape, error code, masking |
| Persistence Test | unique key, FK, append-only revision, snapshot persistence |
| Acceptance Test | 端到端流程：抽選 → 分派 → 初審 → 複審 / 結案 |

---

## 15. 最低必跑清單

若要建立最小可信回歸集，至少必跑以下案例：

```yaml id="gzbj4g"
minimum_regression_suite:
  - TS-PV-003
  - TS-SM-002
  - TS-SM-004
  - TS-IR-007
  - TS-RR-003
  - TS-RV-001
  - TS-RV-005
  - TS-PM-001
  - TS-PM-003
  - TS-TR-001
  - TS-TR-003
  - TS-NG-001
```

---

## 16. 後續落地要求

本文件中的 scenario 應同步轉換為：

* unit test case
* application service test
* API integration test
* regression suite
* UAT checklist

若後續規則變更，應優先檢查：

1. `decision-log.md`
2. `domain-model.md`
3. `state-machine.md`
4. `permission-matrix.md`
5. `api-spec.md`
6. `persistence-model.md`
7. `test-scenarios.md`
