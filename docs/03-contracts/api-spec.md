# 品質審查系統 API Spec v1

## 1. 文件目的

本文件定義品質審查系統的 HTTP API 契約，作為以下工作的依據：

- controller / endpoint 設計
- request / response DTO 設計
- authorization guard
- 前後端契約
- integration 與測試案例

本文件不重新定義 domain invariant。  
若 API 行為與下列文件衝突，應優先檢查：

1. 需求書
2. decision log
3. permission matrix
4. domain model
5. state machine
6. application service

---

## 2. API 設計原則

1. API 分為 command API 與 query API。
2. command API 以明確業務動作為主，不濫用 generic update。
3. 已發布版本、已提交結果、已結案案件，不提供直接覆寫型 API。
4. revision 必須用獨立 command API，不可用 update submission 取代。
5. 查詢 API 與內容可見性需遵守 permission matrix。
6. 系統管理員只可取得 metadata，不可取得完整作答明細。
7. 院方管理者可取得本院完整作答明細。
8. 稽核角色僅限授權院區 / 範圍。
9. API path 先以業務資源導向；是否拆分 controller 可於實作時再定。

---

## 3. 基本約定

### 3.1 Base Path

```text
/api/quality-inspection
```

### 3.2 Content Type

```text
Content-Type: application/json
Accept: application/json
```

### 3.3 時間格式

* ISO-8601
* 範例：`2026-04-02T14:10:00+08:00`

### 3.4 ID 命名

* `projectId`
* `scoreSheetId`
* `projectVersionId`
* `batchId`
* `caseId`
* `submissionId`

### 3.6 Metadata-only Projection Whitelist (O3-C)

`SYSTEM_ADMIN` 採 **shared base + page extension** 投影策略：

- shared base：`caseId`, `caseStatus`, `projectName`, `hospitalId`, `reviewerId`, `rereviewerId`, `updatedAt`
- page extension：
  - UI-009：`batchId`, `dueAt`
  - UI-010：`latestRevisionSummary`
  - UI-014：`stage`, `revisionNo`, `submittedAt`, `submittedBy`, `isCurrentEffective`

超出白名單欄位視為 FULL-only，不得回傳於 metadata-only 視角。

### 3.5 共用回應外殼

#### 成功

```json
{
  "success": true,
  "data": {},
  "meta": {}
}
```

#### 失敗

```json
{
  "success": false,
  "error": {
    "code": "CASE_ALREADY_CLOSED",
    "message": "案件已結案，不允許執行此操作",
    "details": {}
  }
}
```

---

## 4. 錯誤碼模型

### 4.1 通用錯誤碼

| code                   | http status | 說明                |
| ---------------------- | ----------: | ----------------- |
| `BAD_REQUEST`          |         400 | request 格式錯誤或欄位缺漏 |
| `UNAUTHORIZED`         |         401 | 未登入或憑證失效          |
| `FORBIDDEN`            |         403 | 無權限或超出資料範圍        |
| `NOT_FOUND`            |         404 | 資源不存在             |
| `CONFLICT`             |         409 | 與目前狀態衝突           |
| `UNPROCESSABLE_ENTITY` |         422 | 業務規則不允許           |
| `INTERNAL_ERROR`       |         500 | 未預期系統錯誤           |

### 4.2 業務錯誤碼

| code                               | http status | 說明                        |
| ---------------------------------- | ----------: | ------------------------- |
| `PROJECT_NOT_ACTIVE`               |         422 | 專案未啟用，不可抽選                |
| `PROJECT_VERSION_NOT_PUBLISHABLE`  |         422 | 專案版本不可發布                  |
| `SCORE_SHEET_NOT_PUBLISHABLE`      |         422 | 評分表版本不可發布                 |
| `BATCH_NOT_RETRYABLE`              |         422 | 批次不可重跑                    |
| `DUPLICATE_SELECTED_CASE_IN_BATCH` |         409 | 同批次重複抽中同一案件               |
| `CASE_NOT_ASSIGNABLE`              |         422 | 案件目前不可指派                  |
| `CASE_NOT_IN_INITIAL_REVIEW_STAGE` |         422 | 案件不在可開始初審狀態               |
| `CASE_NOT_IN_REREVIEW_STAGE`       |         422 | 案件不在可開始複審狀態               |
| `CASE_ALREADY_CLOSED`              |         409 | 案件已結案                     |
| `CASE_ALREADY_CANCELLED`           |         409 | 案件已取消                     |
| `REVISION_NOT_ALLOWED`             |         422 | 目前狀態不允許 revision          |
| `REVISION_ACTOR_MISMATCH`          |         403 | revision 發起者不是合法責任人       |
| `SUBMISSION_NOT_OWNED_BY_ACTOR`    |         403 | submission 不屬於目前 actor    |
| `SUBMISSION_STAGE_MISMATCH`        |         422 | submission stage 與 API 不符 |
| `INVALID_STATE_TRANSITION`         |         409 | 非法狀態轉移                    |
| `ROLE_DENIED`                      |         403 | 角色不符，不可執行此操作              |
| `HOSPITAL_SCOPE_MISMATCH`          |         403 | actor 所屬院所與案件/專案院所不符      |
| `VALIDATION_ERROR`                 |         400 | 欄位驗證失敗                    |

---

## 5. Authorization 與資料範圍

### 5.1 角色與 scope 原則

* `SYSTEM_ADMIN`

  * 僅 metadata 類查詢
  * 不可查看完整作答內容
  * 不可執行業務 command

* `HOSPITAL_MANAGER`

  * 可操作本院專案、抽選、分派
  * 可查看本院完整作答明細

* `REVIEWER`

  * 僅可對自己被指派案件做初審與初審 revision

* `REREVIEWER`

  * 僅可對自己被指派複審案件做複審與複審 revision

* `AUDITOR`

  * 僅能查詢授權院區 / 範圍內資料
  * 不可修改規則或審查結果

### 5.2 API 實作原則

* `403 ROLE_DENIED`

  * 角色不符，不可執行此操作（`RoleDeniedException`）

* `403 HOSPITAL_SCOPE_MISMATCH`

  * actor 所屬院所與案件/專案院所不符（所有 ReviewCase / SamplingBatch use case 均強制驗證）

* `403 FORBIDDEN`

  * 超出資料範圍
  * 內容明細不可見

---

## 6. 資源模型總覽

### 6.1 主要資源

* `review-projects`
* `score-sheets`
* `project-versions`
* `sampling-batches`
* `review-cases`
* `review-submissions`
* `audit-trails`
* `reports`

### 6.2 command 型 path 設計原則

使用業務動作 path，例如：

* `/activate`
* `/publish`
* `/assign-reviewer`
* `/submit`
* `/revise`

而非大量 generic `PUT /resource/{id}`。

---

## 7. Review Project APIs

---

## 7.1 建立審查專案

### `POST /review-projects`

#### 權限

* `HOSPITAL_MANAGER`

#### Request

```json
{
  "hospitalId": "H-01",
  "name": "2026 門診病歷品質審查",
  "medicalRecordTypes": ["OUTPATIENT"],
  "reviewTargetPolicy": {
    "type": "DOCTOR"
  },
  "dateCriteriaPolicy": {
    "fieldMapping": {
      "OUTPATIENT": "visitDate"
    }
  },
  "samplingPolicy": {
    "method": "RATIO",
    "ratio": 0.1
  },
  "stratificationPolicy": null,
  "excludedDepartments": ["D001", "D002"],
  "qualityThreshold": 80,
  "effectivePeriod": {
    "startAt": "2026-04-01T00:00:00+08:00",
    "endAt": "2026-12-31T23:59:59+08:00"
  }
}
```

#### Response

```json
{
  "success": true,
  "data": {
    "projectId": "RP-001",
    "status": "DRAFT"
  }
}
```

---

## 7.2 更新審查專案草稿

### `PUT /review-projects/{projectId}`

#### 權限

* `HOSPITAL_MANAGER`

#### 限制

* 僅允許未發布 / draft 狀態更新
* 已發布版本不可用此 API 直接覆寫

---

## 7.3 啟用審查專案

### `POST /review-projects/{projectId}/activate`

#### 權限

* `HOSPITAL_MANAGER`

---

## 7.4 停用審查專案

### `POST /review-projects/{projectId}/deactivate`

#### 權限

* `HOSPITAL_MANAGER`

---

## 7.5 發布專案版本

### `POST /review-projects/{projectId}/publish`

#### 權限

* `HOSPITAL_MANAGER`

#### Response

```json
{
  "success": true,
  "data": null
}
```

> **Implementation note:** 發布成功回傳 `null`，不回傳版本詳情。
> 如需取得版本資訊，使用 `GET /review-projects/{projectId}/version`。

---

## 7.6 綁定評分表

### `POST /review-projects/{projectId}/bind-score-sheet`

#### 權限

* `HOSPITAL_MANAGER`

#### Request

```json
{
  "scoreSheetId": "SS-001"
}
```

#### Response

```json
{
  "success": true,
  "data": null
}
```

---

## 7.7 查詢專案清單

### `GET /review-projects`

#### 權限

* `SYSTEM_ADMIN`
* `HOSPITAL_MANAGER`
* `AUDITOR`
* `REVIEWER` / `REREVIEWER` 僅限唯讀必要範圍

#### Query Params

* `hospitalId`
* `status`
* `keyword`
* `page`
* `size`

---

## 7.8 查詢專案詳情

### `GET /review-projects/{projectId}`

#### 權限

* 同上

---

## 8. Score Sheet APIs

---

## 8.1 建立評分表草稿

### `POST /score-sheets`

#### 權限

* `HOSPITAL_MANAGER`

#### Request

```json
{
  "projectId": "RP-001"
}
```

> **設計決策 (D-015)：** 採 create-then-update 模式。POST 僅需 `projectId` 建立空白草稿，取得 `scoreSheetId` 後再透過 PUT 更新內容。

#### Response — 201

```json
{
  "success": true,
  "data": {
    "scoreSheetId": "SS-001"
  }
}
```

#### Error

| code | HTTP | 說明 |
|------|------|------|
| `PROJECT_NOT_FOUND` | 404 | projectId 不存在 |
| `PROJECT_NOT_ACTIVE` | 422 | project 非 ACTIVE 狀態 |

---

## 8.2 更新評分表草稿

### `PUT /score-sheets/{scoreSheetId}`

#### 權限

* `HOSPITAL_MANAGER`

#### 限制

* 僅 draft / 未發布版本可更新

#### Request — 六相位 updateDraft command

```json
{
  "name": "門診病歷評分表",
  "sections": [
    {
      "sectionId": "S1",
      "title": "完整性",
      "displayOrder": 1,
      "items": [
        {
          "itemId": "I1",
          "itemCode": "record-complete",
          "title": "病歷完整性",
          "scoreType": "FIXED",
          "maxScore": 20,
          "weight": 1.0,
          "requiredRule": "ALWAYS_REQUIRED",
          "conditionExpression": null,
          "displayOrder": 1
        }
      ]
    }
  ]
}
```

> **欄位說明：**
> - `requiredRule`: enum string — `ALWAYS_REQUIRED` / `OPTIONAL` / `CONDITIONAL`
> - `conditionExpression`: 僅在 `requiredRule = CONDITIONAL` 時有值
> - `visibilityRule`: 延後至 B3/B4（見 D-014），本版不支援

#### Response — 200

```json
{
  "success": true,
  "data": null
}
```

#### Error

| code | HTTP | 說明 |
|------|------|------|
| `SCORE_SHEET_NOT_FOUND` | 404 | scoreSheetId 不存在 |
| `ALREADY_PUBLISHED` | 409 | 已發布版本不可更新 |
| `DUPLICATE_SECTION_ID` | 422 | section id 重複 |
| `DUPLICATE_ITEM_ID` | 422 | item id 重複 |
| `DUPLICATE_ITEM_CODE` | 422 | item code 重複 |

---

## 8.3 發布評分表版本

### `POST /score-sheets/{scoreSheetId}/publish-version`

#### 權限

* `HOSPITAL_MANAGER`

---

## 8.4 查詢評分表詳情

### `GET /score-sheets/{scoreSheetId}`

#### 權限

* `HOSPITAL_MANAGER`
* `AUDITOR`
* 其他角色依 scope 唯讀

---

## 9. Sampling APIs

---

## 9.1 取得抽選執行頁 bootstrap context

### `GET /review-projects/{projectId}/sampling/execute-context`

#### 權限

* `HOSPITAL_MANAGER`

#### 用途

提供 UI-006 抽選執行頁載入時所需的最小 bootstrap context。此端點回傳專案摘要、blocked reason 與最新批次衍生狀態，供頁面決定初始 mode 與首屏顯示；不定義 preview / execute session state 的最終責任歸屬，也不應被解讀為完整 preview/session persistence contract。

#### Response

```json
{
  "success": true,
  "data": {
    "projectId": "RP-001",
    "projectName": "2026 門診病歷品質審查",
    "hospitalId": "H001",
    "ruleSummary": "專案版本：v3\n病歷類型：OUTPATIENT\n評分表：SS-001\n品質門檻：85",
    "previewReady": false,
    "executing": false,
    "executeFailed": false,
    "executedBatchId": null,
    "blockedReason": null,
    "previewStats": null,
    "postExecuteSummary": null
  }
}
```

---

## 9.2 預估抽選母體

### `POST /review-projects/{projectId}/sampling/preview`

#### 權限

* `HOSPITAL_MANAGER`

#### Request

```json
{
  "mode": "MANUAL"
}
```

#### Response

```json
{
  "success": true,
  "data": {
    "populationCount": 1200,
    "eligibleCount": 300,
    "excludedCount": 40
  }
}
```

---

## 9.3 執行抽選

### `POST /review-projects/{projectId}/sampling/execute`

#### 權限

* `HOSPITAL_MANAGER`

#### Request

```json
{
  "mode": "MANUAL"
}
```

#### Response

```json
{
  "success": true,
  "data": {
    "batchId": "BATCH-001",
    "status": "COMPLETED",
    "selectedCount": 30
  }
}
```

#### 業務限制

* 專案需為 active
* 採用 published version
* 同批次內不得重複抽中同一 `sourceCaseKey`

---

## 9.4 重跑失敗批次

### `POST /sampling-batches/{batchId}/retry`

#### 權限

* `HOSPITAL_MANAGER`

#### Response

```json
{
  "success": true,
  "data": {
    "newBatchId": "BATCH-002",
    "retryOfBatchId": "BATCH-001"
  }
}
```

---

## 9.5 查詢批次清單

### `GET /sampling-batches`

#### 權限

* `HOSPITAL_MANAGER`
* `AUDITOR`
* `SYSTEM_ADMIN` 僅 metadata
* 其他角色唯讀必要範圍

#### Query Params

* `projectId`
* `hospitalId`
* `status`
* `startedFrom`
* `startedTo`
* `page`
* `size`

---

## 9.6 查詢批次詳情

### `GET /sampling-batches/{batchId}`

#### 權限

* `SYSTEM_ADMIN`：metadata only
* `HOSPITAL_MANAGER`
* `AUDITOR`：授權範圍內
* 其他角色依必要範圍唯讀

---

## 10. Review Case APIs

---

## 10.1 查詢案件清單

### `GET /review-cases`

#### 權限

* 全角色依 scope
* `SYSTEM_ADMIN` 僅 metadata

#### Query Params

* `hospitalId`
* `projectId`
* `batchId`
* `status`
* `reviewerId`
* `rereviewerId`
* `medicalRecordType`
* `page`
* `size`

---

## 10.2 查詢案件詳情

### `GET /review-cases/{caseId}`

#### 權限

* `HOSPITAL_MANAGER`：本院完整明細
* `REVIEWER` / `REREVIEWER`：合法範圍內
* `AUDITOR`：授權範圍內
* `SYSTEM_ADMIN`：metadata only

#### Response 分層

* metadata view
* full detail view

#### 新增欄位（GAP-13）

`data.latestRereviewSubmissionId` — 最新複審 submission ID，供前端導覽用；無複審時為 `null`。

```json
{
  "success": true,
  "data": {
    "caseId": "RC-001",
    "projectId": "RP-001",
    "status": "PENDING_REREVIEW",
    "reviewerId": "DOC-1001",
    "rereviewerId": "DOC-2001",
    "latestRereviewSubmissionId": "SUB-RR-001"
  }
}
```

---

## 10.3 指派初審醫師

### `GET /review-cases/{caseId}/assignment-context`

#### 權限

* `HOSPITAL_MANAGER`

#### 用途

提供 UI-011 manager command page 的 bootstrap context。此端點回傳 assignment mode、blocked reason、allowed actions 與目前 reviewer / rereviewer / due date truth；不承擔 change reason policy。

#### Response

```json
{
  "success": true,
  "data": {
    "caseId": "CASE-011",
    "projectId": "RP-001",
    "batchId": "BATCH-001",
    "hospitalId": "H001",
    "medicalRecordType": "OUTPATIENT",
    "status": "PENDING_REREVIEW",
    "pageMode": "MIXED_ASSIGNMENT",
    "blocked": false,
    "blockedReason": null,
    "allowedActions": ["ASSIGN_REREVIEWER"],
    "currentReviewer": {
      "userId": "reviewer-1",
      "displayName": "Dr. Reviewer One",
      "displayLabel": "reviewer-1 | Dr. Reviewer One",
      "assignedAt": "2026-04-01T10:00:00+08:00"
    },
    "currentRereviewer": null,
    "reviewerDueAt": "2026-04-10T23:59:59+08:00",
    "rereviewerDueAt": null,
    "effectiveDueAt": null
  }
}
```

### `GET /review-cases/{caseId}/assignment-candidates?role=REVIEWER|REREVIEWER&keyword=...`

#### 權限

* `HOSPITAL_MANAGER`

#### 用途

提供 UI-011 reviewer / rereviewer 候選人清單。回傳的是正式候選人 contract；候選資料來源可依環境由不同 adapter 提供。

#### Response

```json
{
  "success": true,
  "data": [
    {
      "userId": "reviewer-1",
      "displayName": "Dr. Reviewer One",
      "displayLabel": "reviewer-1 | Dr. Reviewer One",
      "role": "REVIEWER",
      "active": true,
      "selectable": true,
      "blockedReason": null
    }
  ]
}
```

### `POST /review-cases/{caseId}/assign-reviewer`

#### 權限

* `HOSPITAL_MANAGER`

#### Request

```json
{
  "reviewerId": "DOC-1001",
  "dueAt": "2026-04-10T23:59:59+08:00"
}
```

`dueAt` 為正式 command contract 的必填欄位。UI-011 可顯示 due date summary，但更細的 helper / validation policy 若未在 authority 定稿，不得由 frontend 自行發明。

---

## 10.4 改派初審醫師

### `POST /review-cases/{caseId}/reassign-reviewer`

#### 權限

* `HOSPITAL_MANAGER`

#### Request

```json
{
  "reviewerId": "DOC-1002",
  "dueAt": "2026-04-12T23:59:59+08:00"
}
```

不包含 `reason` 欄位。若後續需要 change reason，必須先由 authority 文件定稿。

---

## 10.5 指派複審醫師

### `POST /review-cases/{caseId}/assign-rereviewer`

#### 權限

* `HOSPITAL_MANAGER`

#### Request

```json
{
  "rereviewerId": "DOC-2001",
  "dueAt": "2026-04-20T23:59:59+08:00"
}
```

`dueAt` 為必填。此處僅收斂 command requiredness，不宣稱更深的 due date business validation 已定稿。

---

## 10.6 改派複審醫師

### `POST /review-cases/{caseId}/reassign-rereviewer`

#### 權限

* `HOSPITAL_MANAGER`

#### Request

```json
{
  "rereviewerId": "DOC-2002",
  "dueAt": "2026-04-22T23:59:59+08:00"
}
```

不包含 `reason` 欄位。若需改派原因，應先更新 accepted decision / UI page spec / API contract。

---

## 10.7 取消案件

### `POST /review-cases/{caseId}/cancel`

#### 權限

* `HOSPITAL_MANAGER`

#### Request

```json
{
  "reason": "案件資料不完整，取消審查"
}
```

---

## 10.8 退回初審

### `POST /review-cases/{caseId}/return-initial-review`

#### 權限

* 待你後續定義更細角色；目前建議 `HOSPITAL_MANAGER` 或流程規則允許的控制角色
* 若尚未定稿，可先標記 implementation pending

#### Request

```json
{
  "reason": "需補充病歷資料"
}
```

---

## 11. Initial Review APIs

---

## 11.1 開始初審

### `POST /review-cases/{caseId}/initial-review/start`

#### 權限

* `REVIEWER`
* 且必須為已指派 reviewer

---

## 11.2 保存初審草稿

### `POST /review-cases/{caseId}/initial-review/draft`

#### 權限

* `REVIEWER`
* 且必須為已指派 reviewer

#### Request

```json
{
  "answers": [
    {
      "itemId": "I1",
      "answerValue": true
    }
  ],
  "comment": "初步檢視完成",
  "attachments": [
    {
      "fileToken": "TMP-FILE-001"
    }
  ]
}
```

#### Response

```json
{
  "success": true,
  "data": {
    "submissionId": "SUB-INIT-001",
    "status": "DRAFT",
    "stage": "INITIAL"
  }
}
```

---

## 11.3 提交初審

D-031：提交不等於固定；未固定不得自動／手工進複審或結案。流向依固定結果判斷，不把本回應當作已固定或已進複審。

### `POST /review-cases/{caseId}/submissions/{submissionId}/initial-review/submit`

#### 權限

* `REVIEWER`
* 且必須為 submission 所屬初審醫師

#### Response

```json
{
  "success": true,
  "data": {
    "submissionId": "SUB-INIT-001",
    "status": "SUBMITTED",
    "caseId": "RC-001",
    "caseStatus": "INITIAL_REVIEW_COMPLETED",
    "totalScore": 88.5
  }
}
```

---

## 11.4 修正已提交初審

### `POST /review-cases/{caseId}/submissions/{submissionId}/initial-review/revise`

#### 權限

* `REVIEWER`
* 且必須為原審查醫師

#### 限制

* 初審結果不得已固定（D-031）
* 案件不得為 `CLOSED`
* 案件不得為 `CANCELLED`
* 不可覆寫舊 submission
* 建立新 revision

#### Request

```json
{
  "answers": [
    {
      "itemId": "I1",
      "answerValue": false
    }
  ],
  "comment": "修正初審結果",
  "attachments": []
}
```

#### Response

```json
{
  "success": true,
  "data": {
    "oldSubmissionId": "SUB-INIT-001",
    "newSubmissionId": "SUB-INIT-002",
    "revisionNo": 2,
    "stage": "INITIAL",
    "caseId": "RC-001"
  }
}
```

---

## 12. Re-review APIs

---

## 12.1 開始複審

### `POST /review-cases/{caseId}/rereview/start`

#### 權限

* `REREVIEWER`
* 且必須為已指派複審醫師

---

## 12.2 保存複審草稿

### `POST /review-cases/{caseId}/rereview/draft`

#### 權限

* `REREVIEWER`
* 且必須為已指派複審醫師

---

## 12.3 提交複審

### `POST /review-cases/{caseId}/submissions/{submissionId}/rereview/submit`

#### 權限與結果（D-032／033）

* REREVIEWER 且為合法 submission 提交者，初審已固定；保留 role／hospital scope／ownership guard。
* 提交成功形成 SUBMITTED 複審結果，不自動固定或 CLOSED。
* 合法提交且無其他既有阻擋條件即具結案資格；資格不等於已結案。原複審提交者另行決定正式結案時間。
* 系統仍依 D-012／020 計算 finalScore；回應複審醫師時不得回傳可反推初審分數的 finalScore、preview 或等價衍生資訊。
* 本節撤銷原 `caseStatus: CLOSED` 與向複審醫師回傳 finalScore 的成功範例。複審已提交未結案的狀態表示／精確 response schema 為 unknown，本輪不捏造 enum／DTO。

## 12.4 修正已提交複審

### `POST /review-cases/{caseId}/submissions/{submissionId}/rereview/revise`

#### 權限

* `REREVIEWER`
* 且必須為複審醫師本人

#### 限制

* 結果不得已固定，案件不得為 `CLOSED`（D-032 取代 D-021 例外）
* 案件不得為 `CANCELLED`
* 不可覆寫舊 submission
* 建立新 revision

#### Request

```json
{
  "answers": [
    {
      "itemId": "I1",
      "answerValue": false
    }
  ],
  "comment": "修正複審結果"
}
```

#### Response

```json
{
  "success": true,
  "data": {
    "oldSubmissionId": "SUB-RR-001",
    "newSubmissionId": "SUB-RR-002",
    "revisionNo": 2,
    "stage": "REREVIEW",
    "caseId": "RC-001"
  }
}
```

---

## 13. Submission Query APIs

---

## 13.1 查詢 submission 詳情

### `GET /review-cases/{caseId}/submissions/{submissionId}`

#### 權限

* `HOSPITAL_MANAGER`：本院完整明細
* `REVIEWER` / `REREVIEWER`：合法範圍內
* `AUDITOR`：授權範圍內
* `SYSTEM_ADMIN`：metadata only

#### D-018 Masking 行為（GAP-15）

依 D-033，當 actor 為同案複審醫師時，不論是否已提交、revision、固定或 CLOSED，
初審的以下欄位回傳 `null`，且 response 包含 `maskedFields`：

```json
{
  "success": true,
  "data": {
    "submissionId": "SUB-INIT-001",
    "stage": "INITIAL",
    "answers": null,
    "comment": null,
    "totalScore": null,
    "maskedFields": ["answers", "comment", "totalScore"]
  }
}
```

複審提交後及 CLOSED 後均維持遮罩，不得解除。finalScore／preview／diff 等可反推初審分數的衍生資訊亦不得提供（D-033）；此規則同樣適用 §10 案件、§13.2 歷程、§14 稽核、§15 報表及 command 回應。其他角色合法存取政策不變。

---

## 13.2 查詢 revision 歷程

### `GET /review-cases/{caseId}/revisions`

#### 權限

* `HOSPITAL_MANAGER`
* `AUDITOR`
* `REVIEWER` / `REREVIEWER`：合法範圍內
* `SYSTEM_ADMIN`：metadata only

#### Response

```json
{
  "success": true,
  "data": [
    {
      "submissionId": "SUB-INIT-001",
      "stage": "INITIAL",
      "revisionNo": 1,
      "status": "SUPERSEDED",
      "submittedAt": "2026-04-02T14:10:00+08:00"
    },
    {
      "submissionId": "SUB-INIT-002",
      "stage": "INITIAL",
      "revisionNo": 2,
      "status": "SUBMITTED",
      "submittedAt": "2026-04-02T15:00:00+08:00"
    }
  ]
}
```

---

## 14. Audit APIs

---

## 14.1 查詢案件生命週期

### `GET /review-cases/{caseId}/lifecycle`

#### 權限

* `HOSPITAL_MANAGER`
* `AUDITOR`
* `REVIEWER` / `REREVIEWER`：合法範圍內
* `SYSTEM_ADMIN`：metadata only

---

## 14.2 查詢稽核軌跡

### `GET /audit-trails`

#### 權限

* `SYSTEM_ADMIN`
* `HOSPITAL_MANAGER`
* `AUDITOR`

#### Query Params

* `aggregateType`
* `aggregateId`
* `hospitalId`
* `eventName`
* `from`
* `to`
* `page`
* `size`

---

## 15. Report APIs

---

## 15.1 Dashboard Summary

### `GET /reports/dashboard-summary`

#### 權限

* `HOSPITAL_MANAGER`
* `AUDITOR`
* `SYSTEM_ADMIN`：metadata / aggregated only

#### Query Params

* `hospitalId`
* `projectId`
* `from`
* `to`

---

## 15.2 匯出統計報表

### `POST /reports/export`

#### 權限

* `HOSPITAL_MANAGER`
* `AUDITOR`
* `SYSTEM_ADMIN`：metadata / aggregated only

#### Request

```json
{
  "reportType": "REVIEW_COMPLETION",
  "filters": {
    "hospitalId": "H-01",
    "from": "2026-04-01T00:00:00+08:00",
    "to": "2026-04-30T23:59:59+08:00"
  }
}
```

---

## 16. Response View 分級

### 16.1 Metadata View

適用：

* `SYSTEM_ADMIN`
* 或其他僅需摘要視角的場景

```json
{
  "caseId": "RC-001",
  "projectId": "RP-001",
  "status": "PENDING_REREVIEW",
  "reviewerId": "DOC-1001",
  "rereviewerId": "DOC-2001",
  "submittedAt": "2026-04-02T14:10:00+08:00"
}
```

### 16.2 Full Detail View

適用：

* `HOSPITAL_MANAGER`
* 合法範圍內的 `REVIEWER` / `REREVIEWER`
* 授權範圍內的 `AUDITOR`

```json
{
  "caseId": "RC-001",
  "projectId": "RP-001",
  "status": "PENDING_REREVIEW",
  "answers": [
    {
      "itemId": "I1",
      "answerValue": true,
      "derivedScore": 20
    }
  ],
  "comment": "內容完整",
  "attachments": [
    {
      "fileId": "FILE-001",
      "fileName": "evidence.pdf"
    }
  ]
}
```

---

## 17. 與 Domain 文件的對應關係

### 17.1 API 不可違反的規則

* 不可用 update API 覆寫已提交 submission
* 不可在結果固定、`CLOSED` 或 `CANCELLED` 後 revision；CLOSED 亦不得 reopen（D-031／032）
* revision 不等於 reopen
* 初審 / 複審 revision actor 不可混用
* 抽選執行必須使用正式 published version

### 17.2 應由哪一層保護

* API guard：角色 / scope / input
* application service：orchestration / ownership / command legality
* domain model：state / invariant / consistency

---

## 18. 後續待補項目

本版仍有以下項目尚未細化：

1. OpenAPI schema 正式化
2. enum 與 DTO 完整欄位定義
3. page / sort / filter 統一格式
4. attachment upload API
5. `return-initial-review` 的最終角色授權
6. optimistic locking / idempotency 設計
7. metadata view 與 full detail view 的正式 schema 分離

```

---

這份 `API Spec v1` 已經可以當作下一步的主文件。  
最合理的後續順序是：

1. 補 `Decision Log v1`
2. 補 `Persistence Model v1`
3. 再把這份 API spec 壓成 OpenAPI YAML

如果你要，我下一則直接幫你做 **Persistence Model v1**。
```

## 19. 固定、正式結案與 response 保護（D-031～D-033）

本輪只調整既有 API 的業務契約，不新增 endpoint／DTO／欄位／error code。

| 業務操作 | 必要契約 |
|---|---|
| 初審固定 | 與提交分離；僅目前有效初審結果的合法提交醫師（DD-03）；binding UNKNOWN，不能假定現有 submit API 已支援 |
| 進入／開始複審 | 初審已固定；閾值與 manager 手工派發規則不變 |
| 無需複審的正式結案 | 初審已固定，由院方管理者操作；未固定拒絕 |
| 複審 revision | 合法原提交者，已提交且未固定、未 CLOSED／CANCELLED；append-only，重算加權分數 |
| 複審正式結案 | 目前有效合法複審結果的提交醫師另行 Close Command；既有角色／scope／ownership 與資格成立；同時固定；獨立 binding UNKNOWN（D-034） |
| 固定後／CLOSED revision 或 CLOSED reopen | 拒絕，不寫新 revision／成功事件 |

新的拒絕條件之精確 HTTP／error code mapping unknown，保留既有已確認的 role／scope 錯誤。契約行為變更需後續相容性與實作對齊，本文件不宣稱程式已支援。
複審醫師任何階段不得取得初審答案／分數／評語，亦不得從 command ack、list/detail、revision history/compare、audit、report/export、前端 cache 取得可反推值。finalScore 數值仍在 domain 依原公式計算；所有廣義 FULL view／共用回應範例都須先套用 D-033 角色限制。精確衍生欄位清單與多角色 policy 合成 unknown。

## DD-03 初審固定的語意契約（binding UNKNOWN）

沿用 `/api/quality-inspection/**`、`api-key` 與既有 actor context；固定為與 submit 分離的操作，不新增路徑或暗示既有 submit 已支援。

| 項目 | 必要保證 |
|---|---|
| 授權 | 僅目前有效 INITIAL 結果的合法提交醫師；既有角色、院所 scope、ownership 全部適用；僅有 manager／指派／歷史提交身份不可代行 |
| 輸入語意 | 明確辨識 case 與欲固定的有效 submission，執行時驗證仍有效；具體欄位及版本 token UNKNOWN |
| 成功 | 固定該有效結果且可追溯操作者、時間、submission；不自動結案；依 D-031 流向且遵守 D-033 response 保護 |
| 拒絕 | 無合法提交、DRAFT／SUPERSEDED、actor／scope 不符、terminal case、過期版本不得成功固定；無新 revision 或成功事件 |
| 併發 | 固定與 revision 不可同時對同一結果成功；不得將舊版請求悄悄改為固定新版；進複審／結案以已固定有效結果為準 |
| 重試 | 重複固定、遺失成功回應後重試的冪等／衝突策略 UNKNOWN；不可據此改變已固定內容或重複推進流程 |

既有 role／hospital scope 錯誤維持 §3 的 403；既有通用 CONFLICT 為 409。固定新增拒絕條件的精確 HTTP／error code mapping、schema 與 binding 為 UNKNOWN，未建立可直接呼叫的完成契約。相容性與 consumer 對齊尚未驗證。


## D-034 複審獨立 Close Command 對齊

以下為語意契約，不指定 HTTP method、endpoint path、request／response schema：

| 操作／情境 | 必要行為 |
|---|---|
| 不需複審結案 | 初審已固定，由符合既有 scope 的 HOSPITAL_MANAGER 另行操作；提交／固定均不自動結案 |
| 複審 submit | 初審已固定，合法醫師正式提交有效結果；不自動固定或 CLOSED，不隱含 Close Command |
| 複審 Close Command | 明確辨識 case 與欲結案的有效 REREVIEW submission；執行時重驗 REREVIEWER、scope、ownership、合法有效提交及既有阻擋條件；僅該有效結果合法提交醫師可另行操作 |
| 成功 | 同時固定尚未固定的有效複審結果並 CLOSED，保存一致有效引用及原公式計分；不新增 manager 簽核或獨立複審固定操作 |
| 未提交／非法目標 | 不存在、DRAFT、SUPERSEDED、跨案、錯 stage 或過期 submission 拒絕；不偷偷改用新版 |
| 非法操作者 | 非有效結果提交者、僅指派／manager／複審角色／歷史提交身份，或 scope 不符，不能據此結案 |
| terminal／競爭 | CLOSED／CANCELLED 不得新結案；revision 先完成則舊版 close 拒絕，close 先完成則 revision 拒絕 |
| 失敗 | 不留下部分固定／CLOSED／成功事件；拒絕回應亦不得洩漏初審資訊 |
| 永久盲審 | submit／revision／close ack、錯誤、查詢、歷程、報表／匯出及快取均適用 D-033；CLOSED 不解除，finalScore 等可反推資訊不得向同案複審醫師提供 |

沿用正式 base path 與 api-key／actor context baseline。精確 binding、schema、版本 token、錯誤細碼／新增拒絕條件的 HTTP mapping、重複 close／網路重試回應、多角色授權合成與 consumer 相容性方案均 UNKNOWN；既有通用 role／scope 與 conflict 契約不因此重新定義。不得以 UNKNOWN 放出受限資料或重複推進 CLOSED。


## D-035 歷史身份 Fail-closed 讀取契約

Submission Detail、Case Detail、Revision History：完整初審內容讀取新增 D-035 前置條件。Role／scope 符合但歷史身份 Proven historical 或 UNKNOWN 時，使用既有 HTTP 403、ACCESS_DENIED 與錯誤外殼；不得回傳受限內容、成功資料或默默降級。這包括合法 Manager／Auditor、無需複審案件及無可靠排除證明的初審 REVIEWER。非法 Role／院所等既有拒絕仍優先執行；合法 REREVIEWER 本人複審內容及 SYSTEM_ADMIN 白名單投影不因此擴張。路徑／schema 不變，無新的 completeness token。


## D-036 — UI-010 metadata 八欄與 Lifecycle 延後邊界

UI-010 SYSTEM_ADMIN metadata-only 僅含 `caseId`, `caseStatus`, `projectName`, `hospitalId`, `reviewerId`, `rereviewerId`, `updatedAt`, `latestRevisionSummary`。`lifecycleLatestStage` 暫移除，JSON 必須不存在，不能回 null；目前狀態使用 `caseStatus`。

`updatedAt`、`latestRevisionSummary` 的值契約與實作仍未完成（UNKNOWN），目前 null 不表示已完成。不得加入 FULL-only、敏感巢狀資訊或因新增 DTO getter 自動擴大白名單。

既有 Lifecycle Query／API 與 EX-039／TS-TR-005 完整關鍵路徑需求保留；新增 Milestone／Latest Stage 投影延後，未取消需求。D-035 Fail-closed、D-033 永久盲審及既有 Role／Scope 不變。


## D-037 — updatedAt 契約

updatedAt 採案件層級持久化 UTC 時間點，ISO 8601 輸出；涵蓋 C／相關 S／兩種 Draft Save，建案初始化；缺乏可信歷史的 legacy 為 null。No-change 禁止寫入或宣稱新變更成功，但現有 API 沒有專用 No-change 契約，exception／code／HTTP status 均待決，本輪不新增 HTTP 行為。八欄及授權不變；實作尚未完成。


## D-038 — No-change API 契約補充

D-038 補足 D-037 原待決回應：完全無業務變更的 Draft Save 回 `409 Conflict`／`NO_CHANGES`，沿用既有 error envelope，`success=false` 且不得帶成功儲存結果。不得寫入 Submission／Case.updatedAt、發布 Draft Save Event 或新增 Audit。所有原 Guards 先執行；真實變更保留合法儲存能力。D-033／035／036 與 Lifecycle 延後不變。

目前完整 Draft 比較未實作；attachments 未由 Controller 傳入 Draft UseCase，答案順序、null／空字串、數值 scale 與正規化等語意 UNKNOWN。測試先限於同 API 成功保存後完全相同 payload；不得據此宣稱全部業務相等情形已定義。
