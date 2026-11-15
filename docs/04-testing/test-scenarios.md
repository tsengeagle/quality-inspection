# Test Scenarios v1

> D-031～D-033 對齊：所有 revision 成功 fixture 均須對應結果未固定；複審 fixture 的初審必已固定。所有共用 FULL view、完整歷程與分數示例均依角色套用 D-033：同案複審醫師不得取得初審內容或可反推的衍生資訊；其他角色政策不變。新驗證映射 EX-041～052 ↔ TS-DA-001～012。


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
- 回傳 `403 ROLE_DENIED`
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
1. 呼叫 `POST /review-projects/{projectId}/publish`

### Expected
- 目前版本成為已發布
- `versionNo` 不變（D-040）
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
- 回傳 `409 INVALID_STATE_TRANSITION`（D-040）
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

## TS-SM-002A 正式抽選會完整發布核心 batch / selection 事件

### Objective
驗證正式抽選不只建立資料，也完整形成核心 domain event。

### Preconditions
- actor = `HOSPITAL_MANAGER`
- 專案為 active
- published version 存在
- 母體可正常取得

### Steps
1. 執行正式抽選

### Expected
- 產生 `SamplingBatchStarted`
- 產生 `ReviewCasesSelected`
- 產生 `SamplingBatchCompleted`
- 三者的 `batchId` 一致
- `selectedCount` 與 selected case refs 一致

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

## TS-SM-007 retry 必須沿用原 batch snapshot，而非最新 version

### Objective
驗證 retry 的語意是重做原失敗執行，因此必須沿用原 batch snapshot，而不是吃到後續新發布版本。

### Preconditions
- batch A 狀態 = `FAILED`
- batch A 使用 project version 1 / snapshot 1
- batch A 失敗後，又發布了 project version 2
- actor = `HOSPITAL_MANAGER`

### Steps
1. 呼叫 `POST /sampling-batches/{batchId}/retry`
2. 取得新 batch B
3. 查詢 batch A 與 batch B 的 snapshot / version 參照

### Expected
- 建立新的 batch B
- batch B 的 `retryOfBatchId = batch A`
- batch B 沿用與 batch A 相同的 snapshot 基礎
- batch B **不應** 使用 version 2 的 snapshot
- batch A 維持 `FAILED`
- retry 不覆寫原 batch
- retry 之後的 batch start / complete 或 fail 事件屬於 batch B，而非 batch A

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

## TS-IR-006 初審固定且不需複審時由 manager 結案

### Authority
D-031；EX-001、EX-043。
### Preconditions
合法初審提交者；分數 < threshold；同院 manager。
### Steps
提交後先嘗試未固定結案，再確認初審固定且不需複審，由 manager 正式結案。
### Expected
提交只形成 SUBMITTED／初審完成；未固定結案拒絕，固定本身不結案；manager 正式結案才 CLOSED／ReviewCaseClosed。詳 TS-DA-003。

## TS-IR-007 初審固定後高於品質線進入複審

### Authority
D-031；EX-002、EX-041。
### Preconditions
合法初審提交者，score > threshold。
### Steps
提交後確認未固定不得進複審；再以已合法固定的事實驗證後續流向。
### Expected
提交只初審完成；固定後自動 PENDING_REREVIEW／ReviewCaseEscalatedToRereview；未固定的自動／手工進複審拒絕。詳 TS-DA-001。

## TS-BD-001 等於品質線須固定後由 manager 結案

### Authority
D-031；EX-026、EX-043。
### Preconditions
score=threshold=80。
### Steps
提交、確認固定、不需複審時由 manager 結案。
### Expected
提交不 CLOSED、無升級／結案事件；固定後不強制複審；manager 正式結案才 CLOSED。另測 null 分支；詳 TS-DA-003。

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

## TS-RR-003 複審提交計分但不結案

### Authority
D-032、D-012／020／033；EX-003、EX-044、EX-050。
### Preconditions
初審已固定，actor 為合法 assigned rereviewer。
### Steps
正式提交複審。
### Expected
SUBMITTED、未固定、未 CLOSED，具資格不等於結案；RereviewCompleted 成立但無 ReviewCaseClosed。系統按原公式加權，複審醫師回應不洩漏 finalScore。詳 TS-DA-004／010。

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
- 被修訂結果未固定
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

## TS-RV-003 原複審提交者可修正已提交且未固定複審

### Objective
驗證複審 revision 正常路徑。

### Preconditions
- actor = assigned rereviewer
- submission stage = `REREVIEW`
- 被修訂結果未固定
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
- 未固定的合法 submitted 結果、未 CLOSED／CANCELLED；初審一旦已進複審必已固定，不得再修初審（D-031）
- 發起合法 revision

### Steps
1. 執行 revision

### Expected
- 不產生 reopen 行為
- case 主狀態維持原狀
- 只新增 submission revision

---

## TS-RV-009 第二次 revision 後應形成完整 superseded 鏈

### Objective
驗證多次 revision 時，submission revision 鏈保持 append-only，且 latest 版本認定正確。

### Preconditions
- 被修訂結果未固定
- case status != `CLOSED`
- case status != `CANCELLED`
- 已存在：
  - revision 1 = `SUPERSEDED`
  - revision 2 = `SUBMITTED`
- actor 為合法責任人
  - `INITIAL` 時為 original reviewer
  - `REREVIEW` 時為 assigned rereviewer

### Steps
1. actor 對 revision 2 再次發起 revise
2. 查詢 revision history
3. 查詢 latest submission pointer

### Expected
- 建立 revision 3
- revision 1 維持 `SUPERSEDED`
- revision 2 轉為 `SUPERSEDED`
- revision 3 狀態 = `SUBMITTED`
- case 主狀態不自動改變
- latest submission pointer 指向 revision 3
- 產生新的 `ReviewSubmissionRevised`
- 舊資料不被覆寫

---

## TS-RV-010 revision history 查詢必須正確標示 latest 與 superseded

### Objective
驗證 revision history 查詢結果不只列出歷史版本，還能正確標示：
- 哪些版本為 `SUPERSEDED`
- 哪個版本是目前 latest / effective submission

### Preconditions
- 同一 case / stage 已存在 revision 1、2、3
- revision 1、2 狀態 = `SUPERSEDED`
- revision 3 狀態 = `SUBMITTED`
- latest submission pointer 指向 revision 3
- actor 具有合法查詢權限

### Steps
1. 呼叫 `GET /review-cases/{caseId}/revisions`
2. 比對 revision history 與 latest submission pointer
3. 驗證排序與狀態標示

### Expected
- revision history 穩定輸出完整版本鏈
- revision 1、2 顯示 `SUPERSEDED`
- revision 3 顯示 latest / current effective submission
- latest submission pointer 與 revision history 一致
- 不因時間排序或資料查詢順序造成 latest 判定漂移

### Non-goals
- 不驗 revision 建立流程本身
- 不驗不同角色的 masking 差異

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

## TS-PM-001A manager 查詢 case / submission detail 時應取得 full detail view

### Objective
驗證 manager 的 detail response shape，而不只驗證可否查詢。

### Preconditions
- actor = `HOSPITAL_MANAGER`
- 資料屬於本院
- submission 含 answers / comment / attachments

### Steps
1. 查詢 case detail
2. 查詢 submission detail

### Expected
- response 含 answers
- response 含 full comments
- response 含 attachments metadata
- 不被降級成 metadata only

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
- 回傳 `403 FORBIDDEN` 或查無資料
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

## TS-PM-003A system admin 查詢 detail 時應被遮罩為 metadata view

### Objective
驗證 sysadmin 的 response masking。

### Preconditions
- actor = `SYSTEM_ADMIN`
- 存在 case / submission detail

### Steps
1. 查詢 case detail
2. 查詢 submission detail

### Expected
- response 不含 answers
- response 不含 full comments
- response 不含 attachment content
- 僅含 id / status / timestamps / assigned actors 等 metadata

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

## TS-PM-004A auditor 查詢 detail 時同時受 scope 與 detail view 規則限制

### Objective
驗證 auditor 不僅有院區授權限制，且在授權範圍內可取得合法 detail。

### Preconditions
- actor = `AUDITOR`
- 有院區 A 授權
- 無院區 B 授權

### Steps
1. 查 A 的 case / submission detail
2. 查 B 的 case / submission detail

### Expected
- A 成功
- B 被拒絕或查無資料
- 不可默認 global scope

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
- B 不可查或僅極小 metadata
- 不可取得他人 submission 明細

---

## TS-PM-006 同一 detail API 對不同角色回不同視圖

### Objective
驗證同一個 detail endpoint 會依角色與 scope 回傳不同 response shape，而不只是單純 allow / deny。

### Preconditions
- 存在同一筆 case / submission
- 該資料屬於 hospital A
- manager 屬於 hospital A
- system admin 為平台角色
- auditor 對 hospital A 未授權，或只對其他 hospital 授權

### Steps
1. `HOSPITAL_MANAGER` 查 `/review-cases/{caseId}`
2. `SYSTEM_ADMIN` 查同一 endpoint
3. `AUDITOR` 查同一 endpoint

### Expected
- manager：
  - 200
  - response 含 answers
  - response 含 comments
  - response 含 attachments metadata
- system admin：
  - 200
  - 僅回 metadata view
  - 不含 answers
  - 不含 full comments
  - 不含 attachment content
- auditor：
  - 若未授權，403 或查無資料
  - 不得預設具有 global scope

### Notes
- 本例固定的是同 endpoint 的角色視圖差異
- 不驗 reviewer / rereviewer 自己案件視角

---

## TS-PM-007 list API 也必須遵守 scope 與 metadata / detail 邊界

### Objective
驗證 list API 不會因為是清單查詢，就放寬 detail API 已定義的 scope 與 visibility 規則。

### Preconditions
- 系統存在多筆跨院區案件
- 包含不同 reviewer / rereviewer / case status
- actor 分別為：
  - `HOSPITAL_MANAGER`
  - `SYSTEM_ADMIN`
  - `AUDITOR`
  - `REVIEWER`

### Steps
1. manager 查 case list API
2. sysadmin 查同一 list API
3. auditor 查授權內與未授權院區資料
4. reviewer 查自己與他人案件

### Expected
- manager：僅可見本院資料
- sysadmin：僅可見 metadata 欄位集
- auditor：僅可見授權院區資料
- reviewer：僅可見自己合法範圍資料
- list API 不得外露 detail-only 欄位
- list API 不得默認擴張 scope

### Non-goals
- 不驗單筆 detail payload
- 不驗 export

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

## TS-TR-002A submission snapshot 追溯需同時可驗 version、checksum 與 snapshot ref / content

### Objective
驗證 submission 的計分依據可被完整追溯。

### Preconditions
- 已存在一筆 submission
- submission 綁定 score sheet snapshot

### Steps
1. 查 submission detail
2. 查 snapshot ref 或 snapshot content

### Expected
- 可找到 `basedOnScoreSheetVersionId`
- 可找到 snapshot checksum
- 可找到 snapshot schema version
- 可取得 snapshot ref 或內容

---

## TS-VP-001 新版發布後既有案件仍沿用原 version / snapshot

### Objective
驗證新 version 發布後，不污染既有案件與既有 submission 的規則基礎。

### Preconditions
- project version 1 已發布
- score sheet version 1 已發布
- batch A 使用 version 1 建立 case A
- case A 已存在 submission A
- 後續又發布：
  - project version 2
  - score sheet version 2

### Steps
1. 查詢 case A 的追溯資訊
2. 查詢 submission A 的 snapshot 資訊
3. 再建立新的 batch B / case B
4. 查詢 batch B / case B 的 version / snapshot

### Expected
- case A 仍引用 version 1 / snapshot 1
- submission A 仍引用 score sheet snapshot 1
- batch B / case B 才使用 version 2 / snapshot 2
- 新版本發布不改寫 case A / submission A 的歷史紀錄
- case A 與 submission A 的 checksum / version ref 維持不變

---

## TS-VP-002 project version 與 score sheet version 綁定必須一致

### Objective
驗證正式抽選與後續案件 / 提交所使用的 score sheet version，必須來自 project version 當時正式綁定的版本，不可在執行時臨時混搭。

### Preconditions
- project version 1 綁定 score sheet version 1
- project version 2 綁定 score sheet version 2
- 系統同時存在多個已發布版本
- actor = `HOSPITAL_MANAGER`

### Steps
1. 以 project version 1 執行抽選並建立 batch / case
2. 對該 case 建立 submission
3. 查詢 case 與 submission 的 version / snapshot 參照

### Expected
- case 只能引用 project version 1 對應的 score sheet version 1
- submission 只能引用 score sheet snapshot 1
- 不可在 submission 時跳成 version 2
- snapshot checksum 與 project version 1 對應一致

### Non-goals
- 不驗 publish 流程本身
- 不驗新版本發布後的污染問題，該情境由 TS-VP-001 處理

---

## TS-AT-001 submission 附件關聯應隨 revision 正確保留與隔離

### Objective
驗證 submission attachment 關聯在 revision 後不會污染舊版，也不會遺失新版本關聯。

### Preconditions
- 已存在 submitted submission，關聯 attachment A
- case 未 `CLOSED`
- case 未 `CANCELLED`
- actor 為合法責任人
- 新 revision 將關聯 attachment B

### Steps
1. 發起 revision
2. 查詢舊版 submission detail
3. 查詢新版 submission detail
4. 查 revision history

### Expected
- 舊版 submission 仍只關聯 attachment A
- 新版 submission 關聯 attachment B
- 不發生舊版附件集合被覆寫
- revision history 可分辨不同版本附件關聯
- revision 建立後，舊版 status = `SUPERSEDED`，新版 status = `SUBMITTED`

### Non-goals
- 不驗實際檔案上傳機制
- 不驗 object storage / ECM 技術細節

---

## TS-AT-002 同一案件不同階段附件不得交叉污染

### Objective

驗證 INITIAL 與 REREVIEW 兩個 stage 的附件關聯彼此隔離，不可因同一 case 而互相混用。

### Primary Rule

submission 附件關聯屬於 submission version 與 stage，不應因同 case 多階段存在而混淆。需求書已明定評分作業支援附件上傳，且需保留初審與複審紀錄。

### Authority

```yaml id="ixev9w"
authority:
  requirement:
    - "8.6 評分規則與評分作業管理"
    - "8.7 複審管理"
  examples:
    - EX-035
```

### Level

```yaml id="0o3b7u"
level:
  primary: L4
  secondary:
    - L3
```

### Actors

* `REVIEWER`
* `REREVIEWER`

### Preconditions

* 同一 case 已有：

  * INITIAL submission with attachment A
  * REREVIEW submission with attachment B

### Steps

1. 查 initial submission detail
2. 查 rereview submission detail
3. 查 revision history / attachments relation

### Expected

* INITIAL 只顯示 attachment A
* REREVIEW 只顯示 attachment B
* 不會因同 case 而混出跨 stage 附件
* revision / stage query 能穩定分辨附件歸屬

### Non-goals

* 不驗檔案內容本身
* 不驗實際檔案上傳流程

---

## TS-EV-001 核心事件與持久化結果必須一致，不可只有 event 沒有資料或反之

### Objective
驗證關鍵流程中，domain event、audit 與持久化結果彼此一致。

### Preconditions
- 選定一條核心流程：
  - 初審固定且無需複審時由 manager 正式結案
  - 初審進複審
  - 複審完成
  - revision 成功
  - sampling 成功
- 對應 persistence 與 audit / event 查詢能力可用

### Steps
1. 執行選定流程
2. 查詢 domain event / audit trail
3. 查詢持久化結果
4. 比對 event 與資料一致性

### Expected
- `InitialReviewCompleted` 存在時，對應 submission 與 case 狀態已正確寫入
- `ReviewCaseClosed` 存在時，case 已為 `CLOSED`
- `ReviewSubmissionRevised` 存在時，新 revision 已存在、舊版已 `SUPERSEDED`
- `SamplingBatchCompleted` 存在時，batch / snapshot / selected case 已存在
- 不應存在：
  - event 有、資料沒有
  - 資料有、事件缺失
  - audit trail 與 domain 事實互相矛盾

### Non-goals
- 不驗 message broker / outbox 發送重試細節
- 不驗外部訂閱者行為

---

## TS-EV-002 失敗流程不得產生成功事件或成功審計語意

### Objective

驗證當流程失敗時，不會留下誤導性的成功事件或成功狀態 audit。

### Primary Rule

domain event 應只在業務事實成立後發布；若 transaction 失敗，不應留下成功語意。這與 domain event catalog 的發布原則一致。

### Authority

```yaml id="w42g8e"
authority:
  examples:
    - EX-036
```

### Level

```yaml id="6y0rjp"
level:
  primary: L2
  secondary:
    - L4
```

### Actors

* 視流程而定

### Preconditions

* 選定一條會在中途失敗的流程，例如：

  * sampling execute fail
  * submit initial fail
  * revise fail due to guard
* audit / event persistence 可查

### Steps

1. 執行失敗流程
2. 查詢 event / audit / persistence

### Expected

* 不應出現成功事件，例如：

  * 不應有 `SamplingBatchCompleted`
  * 不應有 `ReviewCaseClosed`
  * 不應有 `ReviewSubmissionRevised`
* 不應留下與實際結果矛盾的成功 audit 語意
* 失敗狀態 / 失敗訊息應與資料現況一致

### Non-goals

* 不驗 message broker 發送失敗補償
* 不驗監控告警系統

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

## TS-TR-005 case lifecycle 查詢必須完整呈現退回、改派、複審與結案路徑

### Objective

驗證 lifecycle query 不只顯示最終狀態，而要保留關鍵路徑事件。

### Primary Rule

系統需保留案件分派、改派、退回、初審、複審、結案等完整歷程。

### Authority

```yaml id="o4g813"
authority:
  requirement:
    - "8.9 稽核與歷程追蹤"
  examples:
    - EX-016
    - EX-036
```

### Level

```yaml id="6ir8i9"
level:
  primary: L3
  secondary:
    - L5
```

### Actors

* `HOSPITAL_MANAGER`
* `AUDITOR`

### Preconditions

* 存在一筆經過：

  * 指派
  * 退回
  * 重新待初審
  * 初審完成
  * 進複審
  * 複審完成
  * 結案
    的案件

### Steps

1. 查 `GET /review-cases/{caseId}/lifecycle`

### Expected

* 可看見完整關鍵節點，而非只看最終 `CLOSED`
* 可分辨至少：

  * assign
  * return
  * reopen to pending initial review
  * initial review completed
  * escalated to rereview
  * rereview completed
  * closed

### Non-goals

* 不要求 lifecycle 顯示所有低階技術事件
* 不驗 UI timeline 呈現樣式

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

## TS-RP-004 export / report 不得繞過 detail visibility 規則

### Objective
驗證 export / report 不能成為繞過 detail visibility 與 scope 規則的旁路。

### Preconditions
- actor 分別為：
  - `HOSPITAL_MANAGER`
  - `SYSTEM_ADMIN`
  - `AUDITOR`
- 報表資料中可推得 detail content
- system admin 僅能看 metadata
- auditor 僅授權部分院區

### Steps
1. manager 匯出本院報表
2. sysadmin 匯出相同主題報表
3. auditor 匯出授權 / 未授權院區報表

### Expected
- manager：可取得本院合法範圍報表
- sysadmin：僅取得 aggregated / metadata 級別輸出
- auditor：僅能匯出授權院區內容
- 未授權範圍匯出被拒絕
- export / report 不得外露 detail API 原本不允許的敏感欄位

### Non-goals
- 不驗 dashboard UI 呈現
- 不驗檔案格式細節

---

## TS-RP-005 統計報表中的案件數、複審率、完成率需與交易資料一致

### Objective

驗證報表計算結果與實際交易資料一致，不可因 read model / projection 偏差而失真。

### Primary Rule

系統需提供審查完成率、複審率、評分分布與統計報表，且這些指標應建立在可追溯、可驗證的交易資料之上。

### Authority

```yaml id="3bqzsl"
authority:
  requirement:
    - "8.8 查詢、統計與報表"
  examples:
    - EX-034
    - EX-036
```

### Level

```yaml id="jlwm1u"
level:
  primary: L5
  secondary:
    - L3
```

### Actors

* `HOSPITAL_MANAGER`
* `AUDITOR`
* `SYSTEM_ADMIN`（aggregated only）

### Preconditions

* 已有可人工計算的小樣本資料集
* 包含：

  * direct close cases
  * rereview cases
  * overdue / completed cases

### Steps

1. 查 dashboard summary
2. 匯出統計報表
3. 與交易資料人工比對

### Expected

* completed count、rereview count、completion rate、rereview rate、score distribution 與交易資料一致
* aggregated output 不因 masking / scope 規則而算錯
* 同一 filter 條件下 dashboard 與 export 的統計口徑一致

### Non-goals

* 不驗圖表前端呈現
* 不驗報表檔案格式排版

---

## TS-QR-001 查詢條件切換時不得跨 scope 洩漏案件

### Objective

驗證查詢 API 在切換 `hospitalId`、`projectId`、`reviewerId`、`status` 等 filter 時，不會因條件組合而跨出既定 scope 洩漏資料。

### Primary Rule

使用者僅可存取其權限範圍內的院區與案件資料；查詢條件不應成為繞過 scope 的手段。

### Authority

```yaml id="1p0y6x"
authority:
  requirement:
    - "11.1 權限與安全"
  examples:
    - EX-033
    - EX-034
```

### Level

```yaml id="vq1l9w"
level:
  primary: L3
  secondary:
    - L5
```

### Actors

* `HOSPITAL_MANAGER`
* `AUDITOR`
* `REVIEWER`

### Preconditions

* 系統存在跨院區、多專案、多 reviewer 的案件資料
* actor 僅有部分院區 / 部分案件 scope

### Steps

1. 以合法 scope 查詢 list API
2. 切換不同 `hospitalId` / `projectId` / `reviewerId` 組合查詢
3. 嘗試用排序、分頁、複合條件查詢未授權範圍資料

### Expected

* 查詢結果永遠不超出 actor scope
* 不因複合條件或分頁造成未授權資料洩漏
* 未授權範圍要嘛被過濾掉，要嘛直接拒絕，策略需一致

### Non-goals

* 不驗單筆 detail payload
* 不驗報表匯出

---

## TS-QR-002 已結案案件的查詢結果不得仍顯示為可操作狀態

### Objective

驗證查詢 / detail / lifecycle API 對 `CLOSED` 案件的輸出，不會暗示仍可 revision、reopen 或重新提交。

### Primary Rule

`CLOSED` 為 terminal state；結案後不允許 revision，且查詢面不應傳遞錯誤可操作訊號。

### Authority

```yaml id="xufk7u"
authority:
  requirement:
    - "10. 狀態需求"
    - "11.2 可追溯性"
  decision_log:
    - D-001
  examples:
    - EX-007
```

### Level

```yaml id="yjlwmx"
level:
  primary: L3
  secondary:
    - L2
```

### Actors

* `HOSPITAL_MANAGER`
* `REVIEWER`
* `REREVIEWER`
* `AUDITOR`

### Preconditions

* 存在 `CLOSED` 案件
* 存在至少一筆 submitted submission

### Steps

1. 查 case detail
2. 查 lifecycle
3. 查 revision history
4. 查 UI / API 可操作欄位或 action metadata（若有）

### Expected

* case status = `CLOSED`
* revision history 可查，但不可標示仍可 revise
* lifecycle 顯示終止狀態
* 若 API 有 action hints / flags，應顯示不可操作

### Non-goals

* 不驗真正發起 revise command 的拒絕，那已由 revision 類 scenario 覆蓋

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
1. 嘗試呼叫 generic update submission API
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
- 至少 1 個初審固定且無需複審、manager 正式結案的 case
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

```yaml id="c2n1hz"
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

## TS-DA-001 初審未固定不得進入複審

### Objective
驗證 初審未固定不得進入複審。

### Authority
D-031；specification-by-example.md EX-041。

### Level
Domain / application specification；涉及資料出口者另含 API integration / frontend / acceptance。未執行實作測試。

### Preconditions
初審合法提交且分數 > threshold，尚未固定。

### Steps
嘗試自動進複審、manager 手工派複審及開始複審。

### Expected
均拒絕／不推進，不發布升級或開始事件；確認固定事實後自動依 > 分支進 PENDING_REREVIEW。固定由目前有效初審結果的合法提交醫師執行（DD-03）。

### Non-goals
不新增 API、error code、資料欄位、CaseStatus 或未確認結案條件；初審固定授權依 DD-03；未知 binding 不由本測試定案。

## TS-DA-002 初審固定前後 revision 邊界

### Objective
驗證 初審固定前後 revision 邊界。

### Authority
D-031、D-004、D-005；specification-by-example.md EX-042。

### Level
Domain / application specification；涉及資料出口者另含 API integration / frontend / acceptance。未執行實作測試。

### Preconditions
初審 SUBMITTED、未結案；原提交者。

### Steps
固定前 revision；確認初審固定後再次 revision。

### Expected
固定前建立新版本、舊版 SUPERSEDED、Case 主狀態不變；固定後拒絕且無新版本／成功事件。非原提交者固定前亦拒絕。

### Non-goals
不新增 API、error code、資料欄位、CaseStatus 或未確認結案條件；初審固定授權依 DD-03；未知 binding 不由本測試定案。

## TS-DA-003 無需複審案件的固定及結案 guard

### Objective
驗證 無需複審案件的固定及結案 guard。

### Authority
D-031；specification-by-example.md EX-043。

### Level
Domain / application specification；涉及資料出口者另含 API integration / frontend / acceptance。未執行實作測試。

### Preconditions
初審分數分別 <、= threshold 或 threshold null；manager 同院。

### Steps
提交後未固定先嘗試結案，再確認固定且不需複審，由 manager 結案。

### Expected
未固定拒絕；固定不自動結案；manager 正式結案才 CLOSED。非 manager 不可代替；固定後可保留既有 manager 手工派複審選項。

### Non-goals
不新增 API、error code、資料欄位、CaseStatus 或未確認結案條件；初審固定授權依 DD-03；未知 binding 不由本測試定案。

## TS-DA-004 複審提交不自動結案

### Objective
驗證 複審提交不自動結案。

### Authority
D-032／034；specification-by-example.md EX-044。

### Level
Domain / application specification；涉及資料出口者另含 API integration / frontend / acceptance。未執行實作測試。

### Preconditions
初審已固定；合法 assigned rereviewer 與 draft。

### Steps
正式提交複審。

### Expected
SUBMITTED，未固定、未 CLOSED；具資格但未結案；只有提交／完成事實，無 ReviewCaseClosed。提交後案件維持 `REREVIEW_IN_PROGRESS`、最新複審提交為 `SUBMITTED`（D-053）。

### Non-goals
不新增 API、error code、資料欄位、CaseStatus 或未確認結案條件；初審固定授權依 DD-03；未知 binding 不由本測試定案。

## TS-DA-005 不具結案資格不得正式結案

### Objective
驗證 不具結案資格不得正式結案。

### Authority
D-032／034；specification-by-example.md EX-045。

### Level
Domain / application specification；涉及資料出口者另含 API integration / frontend / acceptance。未執行實作測試。

### Preconditions
尚無合法複審提交，或仍有既有阻擋條件（例如 CANCELLED）。

### Steps
原複審醫師嘗試正式結案。

### Expected
拒絕，案件及固定事實不被改為成功結案，無 ReviewCaseClosed。不新增分數門檻或額外審批。

### Non-goals
不新增 API、error code、資料欄位、CaseStatus 或未確認結案條件；初審固定授權依 DD-03；未知 binding 不由本測試定案。

## TS-DA-006 僅原複審提交者可正式結案

### Objective
驗證 僅原複審提交者可正式結案。

### Authority
D-032／034、D-023、D-024；specification-by-example.md EX-046。

### Level
Domain / application specification；涉及資料出口者另含 API integration / frontend / acceptance。未執行實作測試。

### Preconditions
合法有效複審提交且無其他既有阻擋條件。

### Steps
以獨立 fixture 讓非有效結果提交者、僅 manager、僅 assigned rereviewer、其他 rereviewer、僅歷史版本提交者及跨院 actor 嘗試 Close Command；再由目前有效結果的合法原提交者另行操作（D-034）。

### Expected
不合法 actor／scope 均拒絕且無固定／結案寫入或成功事件；合法原提交者可自行決定時間，另行 Close Command 才轉 CLOSED 且同時固定複審結果。manager 不作複審簽核；submit 本身不結案（D-034）。

### Non-goals
不新增 API、error code、資料欄位、CaseStatus 或未確認結案條件；初審固定授權依 DD-03；未知 binding 不由本測試定案。

## TS-DA-007 複審提交後結案前可修訂

### Objective
驗證 複審提交後結案前可修訂。

### Authority
D-032、D-004、D-005；specification-by-example.md EX-047。

### Level
Domain / application specification；涉及資料出口者另含 API integration / frontend / acceptance。未執行實作測試。

### Preconditions
初審固定，複審已合法提交但未固定、未 CLOSED／CANCELLED。

### Steps
原提交者 revision，再正式結案；另測非原提交者 revision。

### Expected
合法 revision append-only、更新有效版本及加權分數，Case 主狀態不變；非原提交者拒絕。結案使用目前有效結果且同時固定。

### Non-goals
不新增 API、error code、資料欄位、CaseStatus 或未確認結案條件；初審固定授權依 DD-03；未知 binding 不由本測試定案。

## TS-DA-008 CLOSED 禁止所有 revision 與 reopen

### Objective
驗證 CLOSED 禁止所有 revision 與 reopen。

### Authority
D-032、D-006；specification-by-example.md EX-048。

### Level
Domain / application specification；涉及資料出口者另含 API integration / frontend / acceptance。未執行實作測試。

### Preconditions
案件 CLOSED；另有 CANCELLED 對照；原初審／複審提交者。

### Steps
嘗試兩階段 revision、新提交及 CLOSED reopen。

### Expected
全部拒絕；狀態及歷史不變，無 ReviewSubmissionRevised 或其他成功事件；UI 無例外修正入口。

### Non-goals
不新增 API、error code、資料欄位、CaseStatus 或未確認結案條件；初審固定授權依 DD-03；未知 binding 不由本測試定案。

## TS-DA-009 全生命週期永久盲審與反推防護

### Objective
驗證 全生命週期永久盲審與反推防護。

### Authority
D-033；specification-by-example.md EX-049。

### Level
Domain / application specification；涉及資料出口者另含 API integration / frontend / acceptance。未執行實作測試。

### Preconditions
同案複審醫師，分別處於未提交、已提交、revision、已固定及 CLOSED。

### Steps
檢查 list/detail/history/compare、audit/report/export、submit/revision/close ack、cache 與入口切換。

### Expected
任何出口均不可取得初審答案／分數／評語，亦無 finalScore／preview／diff 等可反推值；應用層 response 遮罩，非僅 DOM 隱藏。已知複審分數與權重仍不能解出初審分數。

### Non-goals
不新增 API、error code、資料欄位、CaseStatus 或未確認結案條件；初審固定授權依 DD-03；未知 binding 不由本測試定案。

## TS-DA-010 加權公式與版本不變性

### Objective
驗證 加權公式與版本不變性。

### Authority
D-012、D-020、D-032、D-033；specification-by-example.md EX-050。

### Level
Domain / application specification；涉及資料出口者另含 API integration / frontend / acceptance。未執行實作測試。

### Preconditions
固定初審80、複審92，權重各0.5；另測合法0.4／0.6。

### Steps
提交、將複審 revision 為90、正式結案；嘗試修改已發布權重。

### Expected
系統內分數依序86、85；0.4／0.6初次87.2；結案不改公式或初審結果。非法權重和／非正權重拒絕，發布後不可改；複審醫師不可讀加權值，manager 本院依原政策可讀。

### Non-goals
不新增 API、error code、資料欄位、CaseStatus 或未確認結案條件；初審固定授權依 DD-03；未知 binding 不由本測試定案。

## TS-DA-011 事件、固定與狀態一致性

### Objective
驗證 事件、固定與狀態一致性。

### Authority
D-031、D-032；specification-by-example.md EX-051。

### Level
Domain / application specification；涉及資料出口者另含 API integration / frontend / acceptance。未執行實作測試。

### Preconditions
合法初審／複審流程及各拒絕流程。

### Steps
逐步提交初審、確認固定、提交複審、revision、正式結案，對照持久化事實與事件。

### Expected
提交初審不發升級／結案事件；固定後需複審才依規則升級。複審提交不發結案事件；revision 新舊版本一致；結案時已 CLOSED 且結果固定才 ReviewCaseClosed。拒絕／rollback 不留下成功事實或成功事件；固定事件技術名稱 unknown。

### Non-goals
不新增 API、error code、資料欄位、CaseStatus 或未確認結案條件；初審固定授權依 DD-03；未知 binding 不由本測試定案。

## TS-DA-012 其他角色資料政策不變

### Objective
驗證 其他角色資料政策不變。

### Authority
D-008、D-009、D-010、D-033；specification-by-example.md EX-052。

### Level
Domain / application specification；涉及資料出口者另含 API integration / frontend / acceptance。未執行實作測試。

### Preconditions
本院 manager、原 reviewer、授權 auditor、system admin 與同案 rereviewer。

### Steps
同一初審資料與衍生結果透過合法 query/export 路徑存取，另測跨院／未授權範圍。

### Expected
其他角色依既有政策：manager 本院完整、reviewer 合法範圍、auditor 授權院區、sysadmin metadata-only；非法 scope 拒絕。同案 rereviewer 所有階段仍遮罩。

### Non-goals
不新增 API、error code、資料欄位、CaseStatus 或未確認結案條件；初審固定授權依 DD-03；未知 binding 不由本測試定案。

## TS-DD03-001 固定成功與有效提交者授權

### Authority
DD-03 決策 1、2、5；D-031。 specification-by-example.md EX-053。

### Level
Domain guard / application integration / API contract / persistence consistency；文件情境，未執行實作測試。

### Preconditions
同院 REVIEWER A 合法提交有效初審 S2，S1 已 SUPERSEDED；案件未固定、未結案、未取消。

### Steps
A 固定 S2；各自使用獨立 fixture，讓僅有 manager 身份的 M、僅被指派但未提交的 B、僅有歷史提交責任的 C、跨院 actor 嘗試固定；另對無提交、DRAFT、SUPERSEDED 結果嘗試固定。

### Expected
A 成功固定 S2，保存 A、固定時間及 S2 關聯，答案／分數／評語不變。其餘請求拒絕，不改有效結果、不留下成功固定／流向／事件。固定後 A 直接改結果或 revision 亦拒絕。若合法資料不允許不同版本有不同提交者，不製造成功 revision 繞過 guard；以授權驗證 fixture 驗證歷史身份不足。

### Pending
固定 binding、schema、精確錯誤 mapping 與交易／併發技術 UNKNOWN；重試結果待決策，不以測試自行定案。


## TS-DD03-002 固定與 revision／流向的併發

### Authority
DD-03 決策 2、3、5；D-004／005／031。 specification-by-example.md EX-054。

### Level
Domain guard / application integration / API contract / persistence consistency；文件情境，未執行實作測試。

### Preconditions
A 為有效 S1 合法提交醫師；未固定、未結案、未取消。分別準備 > threshold 與 ≤ threshold／null 案件。

### Steps
交錯執行固定 S1 與合法 revision；分別控制 revision 先完成、固定先完成；並交錯進複審／manager 結案請求。

### Expected
revision 先完成時 S2 有效、S1 SUPERSEDED，舊版固定不可成功；後續必須重新驗證有效結果及 actor 才可固定 S2。固定先完成則 revision 拒絕且無新版本。固定前進複審／結案拒絕；固定後 > 分支按既有規則進複審；≤／null 不自動結案，manager 符合不需複審條件才可結案。不可固定舊版卻使用新版分數，不可同一結果固定與 revision 同時成功。

### Pending
固定 binding、schema、精確錯誤 mapping 與交易／併發技術 UNKNOWN；重試結果待決策，不以測試自行定案。


## TS-DD03-003 固定追溯、失敗與重試邊界

### Authority
DD-03 決策 5；D-031／033。 specification-by-example.md EX-055。

### Level
Domain guard / application integration / API contract / persistence consistency；文件情境，未執行實作測試。

### Preconditions
A 對合法有效 S2 固定；可於後續保存或流向處理注入失敗。

### Steps
分別驗證成功、保存追溯／流向失敗回復、成功後重新查詢，以及回應遺失後重試。

### Expected
成功後可還原固定時的 A、時間、S2 與 case，歷史不可被之後 assignment 或 latest 指標替代；失敗不留下部分成功固定／流向／成功事件。重試不得改寫固定內容或重複推進流向；精確重試回應及冪等策略 UNKNOWN，相關斷言待定案，不宣稱此情境可完整自動執行。查詢及 ack 遵守 D-033。

### Pending
固定 binding、schema、精確錯誤 mapping 與交易／併發技術 UNKNOWN；重試結果待決策，不以測試自行定案。


## TS-DD03-004 提交、複審與結案正反向回歸

### Authority
D-031／032／033；DD-03 決策 2～4。 specification-by-example.md EX-056。

### Level
Domain guard / application integration / API contract / persistence consistency；文件情境，未執行實作測試。

### Preconditions
各自獨立的合法 INITIAL／REREVIEW draft；正確 actor／院所／scope，複審 fixture 的初審已固定。

### Steps
合法醫師提交；非所屬提交者、跨院、非法狀態、CLOSED／CANCELLED 各嘗試提交。再驗證無需複審的 manager 結案及複審原提交者結案；以未固定初審、無合法複審提交、錯誤 actor 與 terminal case 各嘗試對應操作。

### Expected
合法初審提交成為有效 SUBMITTED，不固定／不進複審／不結案；合法複審提交不固定／不結案。非法提交拒絕且無新有效結果／成功事件。無需複審時僅 manager 可於初審固定後結案；有複審時僅合法有效複審結果原提交者具資格才可正式結案並同時固定。其餘對應結案請求拒絕，不 CLOSED。複審固定前合法 revision 成功、固定或 CLOSED 後拒絕，沿用 EX-045～047，不改計分與盲審。

### Pending
固定 binding、schema、精確錯誤 mapping 與交易／併發技術 UNKNOWN；重試結果待決策，不以測試自行定案。


## TS-DC-001 過期或非法複審 submission 不得結案

### Objective
驗證過期或非法複審 submission 不得結案。

### Authority
D-034；specification-by-example.md EX-057；保留 D-031／032／033 與 DD-03。

### Level
Domain / Application / API / Persistence；本次僅設計，未執行程式測試。

### Preconditions
初審已固定；合法複審醫師 A 原提交 S1，合法 revision 後 S1 為 SUPERSEDED、S2 為目前有效 SUBMITTED。另準備不存在、DRAFT、跨案及 INITIAL stage 目標。

### Steps
A 分別以 S1 與各非法目標另行執行 Close Command。

### Primary Assertion
執行時驗證有效版本及 case／stage；各非法目標均拒絕，不把 S1 請求自動換成 S2。

### Secondary Assertions
案件不 CLOSED，S2 及既有固定初審不變；不新增複審固定或成功結案事件。

### Expected
由 application／domain guard 及一致性契約保護；拒絕不產生新的成功結案／固定或越權資料。API binding、回應與錯誤代碼見 D-053，具體斷言在建構時補上；交易與併發技術機制仍 UNKNOWN（D-045）。

### Non-goals
不新增複審固定操作、管理者簽核、分數門檻、endpoint、enum 或 DB 設計；不決定重複 close 回應或 legacy 遷移。


## TS-DC-002 terminal 不得產生新的結案轉移

### Objective
驗證terminal 不得產生新的結案轉移。

### Authority
D-034；specification-by-example.md EX-058；保留 D-031／032／033 與 DD-03。

### Level
Domain / Application / API / Persistence；本次僅設計，未執行程式測試。

### Preconditions
各自獨立的 CLOSED 與 CANCELLED 案件，以及可識別的既有複審結果。

### Steps
合法原複審提交者、manager 及其他醫師分別嘗試新的 Close Command、revision、新提交；對 CLOSED 另嘗試 reopen。

### Primary Assertion
CLOSED／CANCELLED 不得產生新的結案轉移、revision 或新提交；CLOSED 不得 reopen。

### Secondary Assertions
既有分數、有效引用、固定事實、terminal 與時間不被覆寫，不產生新的成功事件。已成功 close 後再次結案回 `CASE_TERMINAL`，不視為成功（D-053 第 6 點）。

### Expected
由 application／domain guard 及一致性契約保護；拒絕不產生新的成功結案／固定或越權資料。API binding、回應與錯誤代碼見 D-053，具體斷言在建構時補上；交易與併發技術機制仍 UNKNOWN（D-045）。

### Non-goals
不新增複審固定操作、管理者簽核、分數門檻、endpoint、enum 或 DB 設計；不決定重複 close 回應或 legacy 遷移。


## TS-DC-003 Close Command 與複審 revision 競爭

### Objective
驗證Close Command 與複審 revision 競爭。

### Authority
D-034；specification-by-example.md EX-059；保留 D-031／032／033 與 DD-03。

### Level
Application / Persistence integration / API；本次僅設計，未執行程式測試。

### Preconditions
初審已固定；S1 是 A 的目前有效合法 SUBMITTED 複審，未固定、未 terminal，無其他既有阻擋條件。

### Steps
分別控制兩種順序：revision S1→S2 先完成後執行指向 S1 的 close；close S1 先完成後執行 revision。

保留上述兩個循序情境，另以各自獨立的相同初始 fixture 驗證以下真正重疊的情境。Close 與 Revision 均由合法 A 發起，明確以同一 S1 為目標，且雙方均已讀取 S1 為目前有效、未固定、非 terminal 的結果並開始處理；在任一方完成前，另一方也已開始，不能以第一方完成後才啟動第二方替代。

1. 重疊情境 R：先發出 Close 請求、再發出 Revision 請求，使兩者處理重疊且均達上述共同起點；之後控制 Revision S1→S2 先成功提交並完成，Close 才嘗試完成其成功提交。
2. 重疊情境 C：先發出 Revision 請求、再發出 Close 請求，使兩者處理重疊且均達上述共同起點；之後控制 Close S1 先成功提交並完成，Revision 才嘗試完成其成功提交。

此處「成功提交並完成」指該操作的業務變更已一致持久化，不是醫師 submit 複審結果，也不是請求送達或回應返回順序。須分別觀察請求發出順序與成功持久化順序；勝負依後者及執行時有效性判斷，不以先發請求或共同起點的舊讀取授權落敗方成功。不指定鎖定、隔離、交易實作或協調技術。


### Primary Assertion
revision 先完成則舊版 close 拒絕、S2 保持有效且案件未 CLOSED；close 先完成則 S1 固定並 CLOSED、revision 拒絕，不產生 S2。

### Secondary Assertions
不得固定過期結果或同時讓同一結果的 close 與 revision 成功；有效引用、計分、固定及成功事件與勝出結果一致。

重疊情境 R：Revision 勝出，S1 為 SUPERSEDED 且指向 S2，S2 是唯一有效 SUBMITTED 複審；Close 雖已讀過有效 S1，完成時仍須拒絕過期目標，不得改結 S2。案件有效引用為 S2，系統內分數依原固定初審、S2 分數及既有發布版本權重重算；S1／S2 均未因本次競爭被固定，案件維持原非 terminal 主狀態、未 CLOSED，無新結案時間。僅 Revision 成功事件可留下，不得留下 Close 成功、ReviewCaseClosed 或成功固定事件（若有對應事件）。

重疊情境 C：Close 勝出，S1 保持唯一有效 SUBMITTED 複審並固定，案件有效及結案引用均為 S1；系統內分數依原固定初審、S1 分數及既有發布版本權重計算，案件 CLOSED 且結案時間與固定事實一致保存。Revision 即使已讀過未固定 S1 或算出 S2 分數，也必須拒絕；不得持久留下 S2、把 S1 標為 SUPERSEDED、更換有效引用或寫入 S2 分數。僅 Close 成功事件（含 ReviewCaseClosed，以及若有對應的成功固定事件）可留下，不得留下 Revision 成功事件。

兩種情境均須在雙方結束後重新查詢持久化結果與成功事件，確認沒有落敗方的部分寫入、事件或延遲覆寫；既有固定初審及其分數不變。不得同時成功，不以 response shaping 改寫 domain 分數；對複審醫師的回應與查詢仍遵守 D-033。


### Expected
由 application／domain guard 及一致性契約保護；拒絕不產生新的成功結案／固定或越權資料。API binding、回應與錯誤代碼見 D-053，具體斷言在建構時補上；交易與併發技術機制仍 UNKNOWN（D-045）。

### Non-goals
不新增複審固定操作、管理者簽核、分數門檻、endpoint、enum 或 DB 設計；不決定重複 close 回應或 legacy 遷移。


## TS-DC-004 複審結案失敗不得部分成功

### Objective
驗證複審結案失敗不得部分成功。

### Authority
D-034；specification-by-example.md EX-060；保留 D-031／032／033 與 DD-03。

### Level
Application / Persistence / Event integration；本次僅設計，未執行程式測試。

### Preconditions
初審已固定；A 有目前有效合法 SUBMITTED 複審 S1，未固定、非 terminal 且具結案資格；另以獨立 fixture 驗證正常成功。

### Steps
A 另行 Close Command；分別在固定保存、案件／計分保存及成功事件處理注入失敗，再執行獨立正常成功對照。

### Primary Assertion
失敗不得留下部分固定、CLOSED 或成功事件；成功對照須同時固定 S1 並 CLOSED。

### Secondary Assertions
原固定初審輸入、發布版本權重及公式不變；可於重新查詢確認一致結果。失敗注入點與交易／事件技術 UNKNOWN，待技術契約確定後轉成可執行測試，不假稱已具備故障注入能力。

### Expected
由 application／domain guard 及一致性契約保護；拒絕不產生新的成功結案／固定或越權資料。API binding、回應與錯誤代碼見 D-053，具體斷言在建構時補上；交易與併發技術機制仍 UNKNOWN（D-045）。

### Non-goals
不新增複審固定操作、管理者簽核、分數門檻、endpoint、enum 或 DB 設計；不決定重複 close 回應或 legacy 遷移。


## TS-DC-005 複審 Close Command 不解除永久盲審

### Objective
驗證複審 Close Command 不解除永久盲審。

### Authority
D-034；specification-by-example.md EX-061；保留 D-031／032／033 與 DD-03。

### Level
Application / API / Frontend / Acceptance；本次僅設計，未執行程式測試。

### Preconditions
同案複審醫師 A 已合法提交有效結果，具結案資格；含初審答案、分數、評語及可反推初審分數的加權結果。

### Steps
A 結案前查詢，另行 Close Command 成功後查看 ack、詳情、列表、歷程／compare、可達報表／匯出與快取；另驗拒絕 close 的回應。

### Primary Assertion
各階段及成功／拒絕出口均不向 A 提供初審答案、分數、評語或可反推值；CLOSED 不解除盲審。

### Secondary Assertions
finalScore、preview、diff 等衍生資訊遵守 D-033；domain 原始分數及加權計算不因遮罩改寫。不以角色／入口切換繞過同案限制；多角色合成技術仍 UNKNOWN。

### Expected
由 application／domain guard 及一致性契約保護；拒絕不產生新的成功結案／固定或越權資料。API binding、回應與錯誤代碼見 D-053，具體斷言在建構時補上；交易與併發技術機制仍 UNKNOWN（D-045）。

### Non-goals
不新增複審固定操作、管理者簽核、分數門檻、endpoint、enum 或 DB 設計；不決定重複 close 回應或 legacy 遷移。


## TS-FC-001 缺失歷史不得授權完整初審內容

### Authority
D-035、D-033；EX-062。

### Level
Application Unit / HTTP / JPA integration。

### Preconditions
A 指派後未提交即被合法改派 B；僅 A 或全部指派 Audit 缺失；A 以同院 Manager／Auditor 查詢三條路徑，另含 CLOSED。

### Steps
對三條既有讀取路徑執行查詢，核對 HTTP JSON、資料／Audit 與事件前後快照。

### Primary Assertion
可靠排除能力不足，身份為 UNKNOWN；拒絕完整讀取，403 ACCESS_DENIED，無成功資料或靜默降級；Case／Submission／Audit 與事件不變。

### Secondary Assertions
Role／院所／ownership 不放寬；CLOSED 不解除；D-032 既有 Red Test 保留。

### Non-goals
不建立歷史完整性 provider、schema／migration／保留政策；不補造 Audit；不處理 IMP-02B、metadata 三欄值或 Close 流程。


## TS-FC-002 正向及損壞證據的拒絕

### Authority
D-035、D-033；EX-063。

### Level
Application Unit / HTTP / JPA integration。

### Preconditions
A 的正向歷史仍存在（含只缺 B），或 A 的證據 JSON 損壞；以完整讀者身份查詢。

### Steps
對三條既有讀取路徑執行查詢，核對 HTTP JSON、資料／Audit 與事件前後快照。

### Primary Assertion
正向身份不得讀初審；損壞不能作負面證明，同樣拒絕，無資料及成功事件變更。

### Secondary Assertions
Role／院所／ownership 不放寬；CLOSED 不解除；D-032 既有 Red Test 保留。

### Non-goals
不建立歷史完整性 provider、schema／migration／保留政策；不補造 Audit；不處理 IMP-02B、metadata 三欄值或 Close 流程。


## TS-FC-003 合法完整讀者的收緊是預期行為

### Authority
D-035、D-033；EX-064。

### Level
Application Unit / HTTP / JPA integration。

### Preconditions
合法同院 Manager／Auditor 或初審本人查詢；包含測試中未曾複審／不需複審案件，但無可靠負面身份證明。

### Steps
對三條既有讀取路徑執行查詢，核對 HTTP JSON、資料／Audit 與事件前後快照。

### Primary Assertion
仍為 UNKNOWN，403 ACCESS_DENIED 是 D-035 接受的結果；不把 fixture 已知過去、目前狀態或空 Audit 當作 Proven non-historical。

### Secondary Assertions
Role／院所／ownership 不放寬；CLOSED 不解除；D-032 既有 Red Test 保留。

### Non-goals
不建立歷史完整性 provider、schema／migration／保留政策；不補造 Audit；不處理 IMP-02B、metadata 三欄值或 Close 流程。


## TS-FC-004 本人複審與 metadata 保留有限讀取

### Authority
D-035、D-033；EX-065。

### Level
Application Unit / HTTP / JPA integration。

### Preconditions
合法 B 為目前複審醫師或合法本人提交者；或合法 SYSTEM_ADMIN 以原 scope 查詢 metadata。

### Steps
對三條既有讀取路徑執行查詢，核對 HTTP JSON、資料／Audit 與事件前後快照。

### Primary Assertion
B 在原 scope 內可讀本人複審內容，初審及衍生值持續隱藏；metadata 僅白名單，不新增完整內容授權或修改原始資料。

### Secondary Assertions
Role／院所／ownership 不放寬；CLOSED 不解除；D-032 既有 Red Test 保留。

### Non-goals
不建立歷史完整性 provider、schema／migration／保留政策；不補造 Audit；不處理 IMP-02B、metadata 三欄值或 Close 流程。


## TS-MD-001 UI-010 SYSTEM_ADMIN 八欄 exact-key 投影

### Authority

D-036／EX-066；D-028 僅 UI-010 extension 部分被取代。

### Given / When

合法院所 scope 的 SYSTEM_ADMIN 查詢案件詳情；底層 view 含分數、評語、bootstrap 等 FULL-only 資料，並模擬未來新增 getter。

### Expected

- HTTP 200，data keys 精確為 `caseId`, `caseStatus`, `projectName`, `hospitalId`, `reviewerId`, `rereviewerId`, `updatedAt`, `latestRevisionSummary`。
- `lifecycleLatestStage` 不存在，不得以 null 代替；無額外欄位、敏感巢狀內容或 getter 洩漏。
- `caseStatus` 保留正確值；另兩個保留欄位值契約與實作仍 UNKNOWN，不補來源。
- Case／Submission／Audit 不變；既有盲審、D-035、院所拒絕與 Lifecycle API 測試保留。
- EX-039／TS-TR-005 完整生命週期需求保留，新增 Milestone／Latest Stage 實作延後；D-032 已知 Red 保留。

### Test Level

Mapper 序列化 unit、MockMvc HTTP exact-key、JPA／H2 snapshot 與既有 Lifecycle 回歸。


## TS-MD-002 updatedAt 持久化與 No-change Red Test 矩陣

Authority：D-037／EX-067。

| 情境 | 預期／驗證邊界 |
|---|---|
| 新建案 | 真實 createFromSampling／Repository 後 API 回非 null UTC 時間 |
| Initial／Rereview Draft 實變 | 真實 UseCase 後持久化案件時間及 API 非 null |
| Initial／Rereview 完全相同 Draft | 無 C／S 寫入、時間／Event／Audit 不變；拒絕回應形式待決 |
| 相同內容但非法 Guard | role／scope／status／owner／有效引用拒絕，資料事件不變 |
| Submit／Revision／Case-only | 成功後案件時間更新，業務資料保留 |
| rollback | 交易內業務變更後故障，重新查詢資料與時間未改 |
| 並行／時間倒退／資料不遺失 | 真實不同交易交錯，要求時間不倒退與非目標資料不被 touch 覆寫；目前案件 schema／機制不足，BLOCKED，不能聲稱已覆蓋 |
| legacy null | 真實既有案件無可信時間，metadata 仍 null，不補造 |
| HTTP／授權 | D-036 八欄、D-035／D-033 保持；D-032 已知失敗獨立執行 |

具體 No-change exception／error code／HTTP status 尚未決定，不在測試發明。UTC 型別／持久化路徑就緒後須補真實 rollback 與並行驗證；目前僅資料 rollback 對照不算案件時間 rollback PASS。


## TS-MD-003 No-change HTTP／JPA Red

Authority：D-038／EX-068；補足 TS-MD-002 的回應待決項。

- Initial 與 Rereview 先成功保存，再重送相同 payload：409、error.code=NO_CHANGES、success=false、data 缺失或 null。
- assertAll 同時驗證回應、重新查詢持久化快照、Hibernate entity update／insert 數與 Domain Event／Audit，不能因第一個狀態碼失敗而跳過資料副作用斷言。
- 同 payload 的 role／scope／owner／terminal／有效引用非法：保留原錯誤，且無資料／事件變動。
- 有變更 payload：200 與成功結果、實際答案及評語保存；Rereview 另驗 totalScore。沿用真實 Controller／UseCase／Repository／H2，不用 mock。
- attachments 傳遞及完整 Draft 比較仍 BLOCKED；UTC 持久化、rollback 與並行機制尚不存在，另依 persistence-model 待審提案補驗，不用 mock 宣稱 PASS。
- 保留前輪 Red、D-032 已知失敗與 D-035／D-033／D-036 回歸。
