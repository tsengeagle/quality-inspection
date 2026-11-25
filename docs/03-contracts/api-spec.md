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

* 依 `decision-log.md`「憲法草案」第一條（D-072）：ISO-8601 延伸格式，必須帶時區、寫出秒；輸出一律 UTC，例如 `2026-04-02T06:10:00Z`；不帶時區的輸入回 `VALIDATION_ERROR`。
* 回應與事件的時間範例都是 UTC（D-075 第 8 點）。請求的範例保留 `+08:00`：輸入接受任何位移，前端依 D-073 以台北時間送出。
* 目前程式仍有違憲處（D-072 第 4 點的清單，D-075 更正並補充），修正之前以該清單為準；D-075 的建構 PR 合併後，時間一律以 `Instant` 表示。

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
| `SCORE_SHEET_NOT_FOUND`            |         404 | 評分表或評分表版本不存在（D-050）     |
| `DUPLICATE_SECTION_ID`             |         422 | 評分表章節 id 重複（D-050）        |
| `DUPLICATE_ITEM_ID`                |         422 | 評分表題目 id 重複（D-050）        |
| `DUPLICATE_ITEM_CODE`              |         422 | 評分表題目代碼重複（D-050）         |
| `SECTION_NOT_FOUND`                |         422 | 更新命令指向不存在的章節（D-050）     |
| `ITEM_NOT_FOUND`                   |         422 | 更新命令指向不存在的題目（D-050）     |
| `BATCH_NOT_RETRYABLE`              |         422 | 批次不可重跑                    |
| `SAMPLING_BATCH_NOT_FOUND`         |         404 | 批次不存在（D-086）              |
| `DUPLICATE_SELECTED_CASE_IN_BATCH` |         409 | 同批次重複抽中同一案件               |
| `CASE_NOT_ASSIGNABLE`              |         422 | 案件目前不可指派（指派初審醫師；指派複審醫師時案件不是 `PENDING_REREVIEW`，D-069） |
| `REREVIEWER_HAS_READ_INITIAL`      |         422 | 指派或改派的複審醫師曾讀過本案完整初審（D-055） |
| `CASE_NOT_IN_INITIAL_REVIEW_STAGE` |         422 | 案件不在該初審操作要求的狀態（開始初審、保存初審草稿、提交初審；D-067、D-068） |
| `NOT_ASSIGNED_REVIEWER`            |         403 | 開始初審的人不是受指派的初審醫師（D-067） |
| `REQUIRED_ITEMS_NOT_ANSWERED`      |         422 | 提交初審或複審時有必答題沒有作答（D-067、D-076）   |
| `CASE_NOT_IN_REREVIEW_STAGE`       |         422 | 案件不在該複審操作要求的狀態（開始複審、保存複審草稿、提交複審、複審結案；D-069） |
| `NO_REREVIEWER_ASSIGNED`           |         422 | 開始複審時案件還沒有指派複審醫師（D-069） |
| `REREVIEWER_SAME_AS_REVIEWER`      |         422 | 指派的複審醫師是本案的初審醫師（D-017、D-069） |
| `SUBMISSION_NOT_DRAFT`             |         409 | 保存或提交的複審提交已不是草稿（D-069） |
| `SUBMISSION_NOT_SUBMITTED`         |         409 | 固定或結案的目標仍是草稿（D-053、D-054） |
| `NOT_ASSIGNED_REREVIEWER`          |         403 | 操作者不是受指派的複審醫師（開始、保存草稿、提交、修正複審；D-068） |
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

發布成功時 `data` 為 `null`，不回傳版本詳情。版本資訊由 `GET /review-projects/{projectId}/version` 取得。

---

## 7.6 綁定評分表

### `POST /review-projects/{projectId}/bind-score-sheet`

#### 權限

* `HOSPITAL_MANAGER`

#### Request

```json
{
  "scoreSheetVersionId": "SSV-001"
}
```

#### 檢查（D-050 第 8 點）

| code | HTTP | 說明 |
|------|------|------|
| `PROJECT_NOT_FOUND` | 404 | projectId 不存在 |
| `INVALID_STATE_TRANSITION` | 409 | 專案目前的版本已發布；或評分表版本尚未發布 |
| `SCORE_SHEET_NOT_FOUND` | 404 | 評分表版本不存在，或屬於另一個專案 |

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
* `REVIEWER` / `REREVIEWER` 不開放（D-051）

#### Query Params

* `hospitalId`：只有 `SYSTEM_ADMIN` 可用來縮小範圍；其他角色只列出自己院區的專案（D-062 第 2 點）
* `status`、`keyword`、`page`、`size`：尚未實作，不在規格與契約內（D-062 第 2 點）

#### Response（D-062 第 1 點）

```json
{
  "success": true,
  "data": {
    "items": [
      { "projectId": "RP-001", "hospitalId": "H-01", "name": "門診品質審查", "status": "DRAFT" }
    ]
  }
}
```

每一項的欄位與 §7.8 專案詳情相同。

---

## 7.8 查詢專案詳情

### `GET /review-projects/{projectId}`

#### 權限

* 同上

---

## 7.9 查詢專案版本

### `GET /review-projects/{projectId}/version`

#### 權限（D-062 第 3 點）

* `SYSTEM_ADMIN`（不受院區限制）
* `HOSPITAL_MANAGER`、`AUDITOR`：本院
* `REVIEWER` / `REREVIEWER` 不開放（D-051）

#### Response

```json
{
  "success": true,
  "data": { "versionNumber": 1, "published": false }
}
```

欄位名稱維持程式現況 `versionNumber`（D-062 第 4 點）。

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

> 不檢查專案狀態；同一專案可以有多份評分表（D-050 第 2 點）。原本的 `PROJECT_NOT_ACTIVE` 已移除：要求專案已啟用會使第一份評分表無法建立。

---

## 8.2 更新評分表草稿

### `PUT /score-sheets/{scoreSheetId}`

#### 權限

* `HOSPITAL_MANAGER`

#### 限制

* 僅 draft / 未發布版本可更新

#### Request — 六相位 updateDraft command（D-015、D-050 第 5 點）

```json
{
  "removeSectionIds": [],
  "removeItemIds": [],
  "updateSections": [{ "sectionId": "S1", "title": "完整性" }],
  "updateItems": [],
  "addSections": [
    {
      "sectionId": "S2",
      "title": "正確性",
      "items": [
        {
          "itemId": "I2",
          "code": "diagnosis-correct",
          "title": "診斷正確",
          "scoreType": "FIXED",
          "maxScore": 20,
          "weight": 1,
          "requiredRule": "ALWAYS_REQUIRED",
          "conditionExpression": null
        }
      ]
    }
  ],
  "addItems": [
    {
      "targetSectionId": "S1",
      "itemId": "I3",
      "code": "signature",
      "title": "簽章",
      "scoreType": "BOOLEAN",
      "maxScore": 5,
      "requiredRule": "OPTIONAL"
    }
  ]
}
```

> **欄位說明：**
> - 相位依序執行：刪除章節 → 刪除題目 → 章節改名 → 修改題目 → 新增章節（含題目）→ 新增題目到既有章節；後面的相位看得到前面的結果。每個相位內先驗證輸入，再查找章節或題目，最後檢查重複。被拒絕時整個命令沒有任何改變
> - `updateItems` 整筆取代題目的欄位；沒有輸入 `weight` 時是 1
> - `requiredRule`: enum string — `ALWAYS_REQUIRED` / `OPTIONAL` / `CONDITIONAL`
> - `conditionExpression`: `requiredRule = CONDITIONAL` 時必填，其他時候必須沒有值
> - 六個相位都空時成功、沒有改變
> - 評分表名稱與 `displayOrder` 本版不納入，順序以清單順序表示（D-050 第 5 點）
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
| `INVALID_STATE_TRANSITION` | 409 | 已發布版本不可更新（D-050 第 3 點） |
| `VALIDATION_ERROR` | 400 | 必填欄位空白、`maxScore` 小於 0、`conditionExpression` 與 `requiredRule` 不符 |
| `SECTION_NOT_FOUND` | 422 | 指向不存在的章節 |
| `ITEM_NOT_FOUND` | 422 | 指向不存在的題目 |
| `DUPLICATE_SECTION_ID` | 422 | section id 重複 |
| `DUPLICATE_ITEM_ID` | 422 | item id 重複 |
| `DUPLICATE_ITEM_CODE` | 422 | item code 重複 |

---

## 8.3 發布評分表版本

### `POST /score-sheets/{scoreSheetId}/publish`

路徑依 D-058（原寫 `publish-version`，程式為 `publish`）。

#### 權限

* `HOSPITAL_MANAGER`

#### Response

```json
{
  "success": true,
  "data": {
    "scoreSheetVersionId": "SSV-001"
  }
}
```

回傳的 `scoreSheetVersionId` 用於綁定（§7.6，D-050 第 1 點）。

#### Error

| code | HTTP | 說明 |
|------|------|------|
| `SCORE_SHEET_NOT_FOUND` | 404 | scoreSheetId 不存在 |
| `INVALID_STATE_TRANSITION` | 409 | 已發布 |
| `SCORE_SHEET_NOT_PUBLISHABLE` | 422 | 版本內沒有任何題目；有題目時容許空章節 |

---

## 8.4 查詢評分表詳情

### `GET /score-sheets/{scoreSheetId}`

#### 權限

* `SYSTEM_ADMIN`（不受院區限制，D-047）
* `HOSPITAL_MANAGER`
* `AUDITOR`

評分表的章節、版本與列表查詢採相同角色；`REVIEWER`／`REREVIEWER` 不開放（D-050 第 7 點）。

---

## 8.5 查詢評分表列表

### `GET /score-sheets`

#### 權限

與 8.4 相同（D-050 第 7 點）。院區依 D-042、D-047：`SYSTEM_ADMIN` 可用 `hospitalId` 縮小範圍，其他角色一律為本院。

#### Query Params

* `hospitalId`（選填，只有 `SYSTEM_ADMIN` 有作用）

#### Response（依 D-050 第 7 點）

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "scoreSheetId": "SS-001",
        "projectId": "PRJ-001",
        "status": "DRAFT",
        "version": 1,
        "sectionCount": 3,
        "itemCount": 12
      }
    ]
  }
}
```

---

## 8.6 查詢評分表章節

### `GET /score-sheets/{scoreSheetId}/sections`

#### 權限

與 8.4 相同（D-050 第 7 點）。`SYSTEM_ADMIN` 可以讀章節與題目定義（D-050 第 7 點：D-010 的「完整評分內容」不包括題目定義）。

#### Response（依 D-050 第 7 點）

```json
{
  "success": true,
  "data": {
    "scoreSheetId": "SS-001",
    "sections": [
      {
        "sectionId": "SEC-1",
        "title": "病歷完整性",
        "items": [
          {
            "itemId": "I1",
            "code": "A01",
            "title": "主訴是否記載",
            "scoreType": "BOOLEAN",
            "maxScore": 10,
            "weight": 1,
            "requiredRule": "ALWAYS_REQUIRED",
            "conditionExpression": null
          }
        ]
      }
    ]
  }
}
```

---

## 8.7 查詢評分表版本

### `GET /score-sheets/{scoreSheetId}/version`

#### 權限

與 8.4 相同（D-050 第 7 點）。

#### Response

```json
{
  "success": true,
  "data": {
    "version": 1,
    "published": true,
    "scoreSheetVersionId": "SSV-001"
  }
}
```

未發布時 `scoreSheetVersionId` 為 `null`（D-050 第 1 點）。本 API 只回傳目前版本，不是版本清單。

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

#### 檢查（D-086 第 9 點）

依序：角色 → `PROJECT_NOT_FOUND`（404）→ 院區 `HOSPITAL_SCOPE_MISMATCH`（403）→ `PROJECT_NOT_ACTIVE`（422）。不建立批次、不發事件。目前母體來源回傳的每一筆都算合格，`excludedCount` 是 0（D-086 第 6 點）。

規格：`specs/sampling-batch/`。

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

#### 檢查（D-066 第 3 點）

依序：角色 → 載入 → 院區範圍 → 領域規則（D-024）。

| code | HTTP | 說明 |
|------|------|------|
| `ROLE_DENIED` | 403 | 操作者不是 `HOSPITAL_MANAGER` |
| `PROJECT_NOT_FOUND` | 404 | projectId 不存在 |
| `HOSPITAL_SCOPE_MISMATCH` | 403 | 操作者的院區與專案的院區不同 |
| `PROJECT_NOT_ACTIVE` | 422 | 專案未啟用 |

規格：`specs/sampling-batch/`。

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

#### 檢查（D-086 第 1～3 點）

依序：角色 → 載入 → 院區範圍 → 領域規則（D-024）。

| code | HTTP | 說明 |
|------|------|------|
| `ROLE_DENIED` | 403 | 操作者不是 `HOSPITAL_MANAGER` |
| `SAMPLING_BATCH_NOT_FOUND` | 404 | batchId 不存在 |
| `HOSPITAL_SCOPE_MISMATCH` | 403 | 操作者的院區與批次所屬專案的院區不同 |
| `BATCH_NOT_RETRYABLE` | 422 | 批次不是 `FAILED` |

目前沒有任何行為會產生 `FAILED` 的批次（抽選中途出錯時不留下批次），所以成功的重跑走不到；失敗的記錄與重跑的成功路徑等母體來源的介接定案（D-086 第 1、2 點）。

規格：`specs/sampling-batch/`。

---

## 9.5 查詢批次清單

### `GET /sampling-batches`

#### 權限

* `HOSPITAL_MANAGER`
* `AUDITOR`
* `SYSTEM_ADMIN` 僅 metadata
* `REVIEWER` / `REREVIEWER` 不開放（D-051）

#### Query Params

* `projectId`
* `hospitalId`
* `status`
* `executionMode`
* `page`
* `size`

`startedFrom`、`startedTo` 尚未實作（D-086 第 11 點）。

不是 `SYSTEM_ADMIN` 只看得到本院的批次，`hospitalId` 不採用；`SYSTEM_ADMIN` 可用 `hospitalId` 縮小範圍。依開始時間由新到舊；`size` 預設 20、上限 100。

規格：`specs/sampling-batch/`。

---

## 9.6 查詢批次詳情

### `GET /sampling-batches/{batchId}`

#### 權限

* `SYSTEM_ADMIN`：metadata only
* `HOSPITAL_MANAGER`
* `AUDITOR`：授權範圍內
* `REVIEWER` / `REREVIEWER` 不開放（D-051）

`SYSTEM_ADMIN` 的回應沒有 `selectedCases`（抽中的每一筆病歷）；計數、執行方式、狀態、時間與規則快照照給（D-086 第 12 點）。批次不存在回 `SAMPLING_BATCH_NOT_FOUND`（404）。

規格：`specs/sampling-batch/`。

---

## 9.7 匯出抽選批次結果

### `GET /sampling-batches/{batchId}/export`

依 D-058 第 5 點，以程式為準補上。

#### 權限

* `HOSPITAL_MANAGER`，且批次所屬專案為本院

#### Response

```json
{
  "success": true,
  "data": {
    "detail": { "...": "與 9.6 批次詳情相同的內容" }
  }
}
```

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

#### 投影與授權（D-084）

規格：`specs/review-case/usecases.yaml` 的 `GetReviewCaseDetail`（RC-DET、RC-DTV、RC-DTC）。

* 檢查順序：角色 → 案件存在 → 院區（SYSTEM_ADMIN 不限）→ 多角色（403 `ACCESS_DENIED`）→ 指派／提交範圍 → 歷史複審身分（D-035、D-055）。別院的雙角色使用者回 `HOSPITAL_SCOPE_MISMATCH`。
* 完整投影（HOSPITAL_MANAGER、AUDITOR、REVIEWER）：`projectionType = FULL`，有 `initialReviewScore`、`finalScore`、每一筆提交的 `totalScore`；寫入初審讀取者紀錄（D-055 第 5 點）。初審醫師也看得到複審的分數與 `finalScore`。
* 複審醫師的盲審投影：沒有 `initialReviewScore`、`finalScore`；初審提交的 `totalScore` 是 `null`；`rereviewContext` 沒有 `initialTotalScore`、`finalScore`。初審醫師的身分與初審提交的 ID、版本、提交時間可見。
* SYSTEM_ADMIN：只有 `caseId`、`caseStatus`、`projectName`、`hospitalId`、`reviewerId`、`rereviewerId`、`updatedAt`、`latestRevisionSummary`（目前固定 `null`）。
* 沒有 `rereviewRequired`、`finalConclusion`（D-084 第 9 點移除）。

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

依狀態（D-084，規格 RC-ACX-01～06）：

| status | pageMode | allowedActions | blockedReason |
|---|---|---|---|
| `SELECTED_PENDING_ASSIGNMENT` | `ASSIGN_INITIAL` | `ASSIGN_REVIEWER` | — |
| `PENDING_INITIAL_REVIEW` | `REASSIGN_INITIAL` | `REASSIGN_REVIEWER` | — |
| `PENDING_REREVIEW` | `MIXED_ASSIGNMENT` | 未指派 `ASSIGN_REREVIEWER`；已指派 `REASSIGN_REREVIEWER` | — |
| `RETURNED`、`INITIAL_REVIEW_IN_PROGRESS`、`INITIAL_REVIEW_COMPLETED`、`REREVIEW_IN_PROGRESS` | `READONLY_BLOCKED` | 空 | `CASE_NOT_ASSIGNABLE_IN_CURRENT_STATUS`（`RETURNED` 要先重開） |
| `CLOSED`、`CANCELLED` | `READONLY_BLOCKED` | 空 | `CASE_ALREADY_CLOSED`、`CASE_ALREADY_CANCELLED` |

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
      "assignedAt": "2026-04-01T02:00:00Z"
    },
    "currentRereviewer": null,
    "reviewerDueAt": "2026-04-10T15:59:59Z",
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

`blockedReason`（D-084，規格 RC-ACD-01～04）：`CANDIDATE_INACTIVE`；這個角色的指派不在分派頁的 `allowedActions` 裡時 `INITIAL_ASSIGNMENT_NOT_ALLOWED`、`REREVIEW_ASSIGNMENT_NOT_ALLOWED`；複審候選人是初審醫師時 `REREVIEWER_SAME_AS_REVIEWER`；複審候選人讀過本案的完整初審時 `REREVIEWER_HAS_READ_INITIAL`（查詢只讀，不寫入紀錄）。

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

`dueAt` 為正式 command contract 的必填欄位（D-073：`dueAt` 必須帶時區，例如 `2026-04-10T23:59:59+08:00`；不帶時區回 400 `VALIDATION_ERROR`。）UI-011 可顯示 due date summary，但更細的 helper / validation policy 若未在 authority 定稿，不得由 frontend 自行發明。

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

#### 限制（D-082）

* `reviewerId`、`dueAt` 必填（空白也算沒給），否則 `400 VALIDATION_ERROR`；在狀態檢查之前
* 案件須為 `PENDING_INITIAL_REVIEW`，否則 `422 CASE_NOT_ASSIGNABLE`
* 可改派給原本的醫師（只改期限）；改派後原本的醫師不能開始初審

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

`dueAt` 為必填（D-073：`dueAt` 必須帶時區，例如 `2026-04-10T23:59:59+08:00`；不帶時區回 400 `VALIDATION_ERROR`。）此處僅收斂 command requiredness，不宣稱更深的 due date business validation 已定稿。

對象曾讀過本案完整初審內容時回 `422 REREVIEWER_HAS_READ_INITIAL`（D-055 第 5 點，D-033 永久盲審）。

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

對象曾讀過本案完整初審內容時回 `422 REREVIEWER_HAS_READ_INITIAL`（D-055 第 5 點，D-033 永久盲審）。

#### 限制（D-082）

依序檢查：

* `rereviewerId`、`dueAt` 必填（空白也算沒給），否則 `400 VALIDATION_ERROR`
* 案件須為 `PENDING_REREVIEW`（複審中不得改派，D-055），否則 `422 CASE_NOT_ASSIGNABLE`
* 須已指派複審醫師，否則 `422 NO_REREVIEWER_ASSIGNED`
* 不得是本案的初審醫師，否則 `422 REREVIEWER_SAME_AS_REVIEWER`
* 不得讀過本案完整初審，否則 `422 REREVIEWER_HAS_READ_INITIAL`
* 可改派給原本的複審醫師（只改期限）；改派後原本的複審醫師不能開始複審

---

## 10.7 取消案件

### `POST /review-cases/{caseId}/cancel`

#### 權限

* `HOSPITAL_MANAGER`，且案件為本院

#### 限制（D-080）

* 案件不是 `CLOSED`／`CANCELLED`，否則 `409 CASE_TERMINAL`；其他狀態都可以取消
* `reason` 不得空白，否則 `400 VALIDATION_ERROR`

#### Request

```json
{
  "reason": "案件資料不完整，取消審查"
}
```

---

## 10.8 退回初審

### `POST /review-cases/{caseId}/initial-review/return`

路徑依 D-058（原寫 `return-initial-review`）。

#### 權限

* `HOSPITAL_MANAGER`，且案件為本院（D-080 第 6 點）

#### 限制（D-080）

* 案件須為 `INITIAL_REVIEW_IN_PROGRESS`，否則 `409 CASE_NOT_IN_PROGRESS`
* `reason` 不得空白，否則 `400 VALIDATION_ERROR`
* 進行中的初審提交維持 `DRAFT`；重開後由同一位醫師開始初審時沿用它，改派後由新醫師開始時建立新的提交（D-081）

#### Request

```json
{
  "reason": "需補充病歷資料"
}
```

---

## 10.9 不送複審直接結案

### `POST /review-cases/{caseId}/close-without-rereview`

依 D-058 第 5 點，以程式為準補上。

#### 權限

* `HOSPITAL_MANAGER`，且案件為本院

#### 限制

* 案件須為 `INITIAL_REVIEW_COMPLETED`，否則 `409 CASE_NOT_INITIAL_REVIEW_COMPLETED`
* 初審須已固定（D-031）
* 結案後 `finalScore` 為初審分數，案件為 `CLOSED`

#### Request

無 body。

---

## 10.10 送複審

### `POST /review-cases/{caseId}/send-to-rereview`

依 D-058 第 5 點，以程式為準補上。

#### 權限

* `HOSPITAL_MANAGER`，且案件為本院

#### 限制

* 案件須為 `INITIAL_REVIEW_COMPLETED`，否則 `409 CASE_NOT_INITIAL_REVIEW_COMPLETED`
* 初審須已固定（D-031）
* 成功後案件為 `PENDING_REREVIEW`，接著以 10.5 指派複審醫師

#### Request

無 body。

---

## 10.11 重新開放初審

### `POST /review-cases/{caseId}/reopen`

依 D-058 第 5 點，以程式為準補上。只用於 10.8 退回初審之後，不是重開 `CLOSED` 案件（D-005、D-032）。

#### 權限

* `HOSPITAL_MANAGER`，且案件為本院

#### 限制

* 案件須為 `RETURNED`，否則 `409 CASE_NOT_RETURNED`
* 成功後清除退回原因，案件為 `PENDING_INITIAL_REVIEW`
* 沿用原本的初審醫師與期限；要換人或改期限，重開後改派（D-080 第 4 點）
* 稽核紀錄是 `REOPENED`，不是 `ASSIGNED`（D-080 第 2 點）

#### Request

無 body。

---

## 11. Initial Review APIs

---

### 作答的檢查與計分（D-085）

保存初審草稿、提交初審、提交複審、修正初審、修正複審都會計分；計分前先檢查作答，不合法回 `400 VALIDATION_ERROR`，不寫入（規格 RC-INV-07、RC-INV-08）：

* 每一筆都有 `itemId` 與 `answerValue`；`itemId` 是評分表的題目；同一題只作答一次。
* BOOLEAN：作答是 `true` 或 `false`；`true` 得 滿分 × 權重，`false` 得 0。
* RANGE：作答是 0 到滿分之間的數字，最多兩位小數；得 作答 × 權重。
* FIXED：作答內容不檢查，得 滿分 × 權重。
* 題目分數與總分四捨五入到兩位小數（HALF_UP）；總分是各題相加，沒有作答的題目不計分。
* 位置在狀態、指派、提交歸屬的檢查之後，必答題（與沒有變更）之前。複審草稿保存時不計分，不合法的作答在提交複審時才擋下。

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

### `POST /review-cases/{caseId}/initial-review/submit`

路徑依 D-058：不帶 `submissionId`，由案件取得目前的初審 submission；request 沒有 body。

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

### `POST /review-cases/{caseId}/initial-review/submissions/{submissionId}/revise`

路徑依 D-058（stage 在前）。

#### 權限

* `REVIEWER`
* 且必須為原審查醫師

#### 限制

* 初審結果不得已固定（D-031）
* 案件不得為 `CLOSED`
* 案件不得為 `CANCELLED`
* 不可覆寫舊 submission
* 建立新 revision

#### 錯誤（D-083）

* 案件已終止 `409 CASE_TERMINAL`；提交找不到 `404 SUBMISSION_NOT_FOUND`
* 提交不屬於本案或不是初審、不是目前有效的初審結果 `409 CONFLICT`
* 操作者不是提交者 `403 NOT_SUBMISSION_OWNER`；提交不是 `SUBMITTED` `409 SUBMISSION_NOT_SUBMITTED`；初審已固定 `409 INVALID_STATE_TRANSITION`
* 必答題沒有作答 `422 REQUIRED_ITEMS_NOT_ANSWERED`；`answers` 與 `comment` 都沒有變更 `409 NO_CHANGES`

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
    "totalScore": 3,
    "status": "SUBMITTED",
    "stage": "INITIAL",
    "caseId": "RC-001"
  }
}
```

---

## 12. Re-review APIs

---

## 12.1 開始複審

### `POST /review-cases/{caseId}/start-rereview`

路徑依 D-058（原寫 `rereview/start`）。

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

### `POST /review-cases/{caseId}/rereview/submit`

路徑依 D-058：路徑不帶 `submissionId`，改放在 request body，且為必填（用來確認提交的是目前版本）。

#### Request

```json
{
  "submissionId": "SUB-RR-001",
  "answers": [
    {
      "itemId": "I1",
      "answerValue": true
    }
  ],
  "comment": "複審意見"
}
```

`submissionId` 必填（`@NotBlank`），缺少時回 `400`。

#### 權限與結果（D-032／033）

* REREVIEWER 且為合法 submission 提交者，初審已固定；保留 role／hospital scope／ownership guard。
* 提交成功形成 SUBMITTED 複審結果，不自動固定或 CLOSED。
* 合法提交且無其他既有阻擋條件即具結案資格；資格不等於已結案。原複審提交者另行決定正式結案時間。
* 系統仍依 D-012／020 計算 finalScore；回應複審醫師時不得回傳可反推初審分數的 finalScore、preview 或等價衍生資訊。
* 本節撤銷原 `caseStatus: CLOSED` 與向複審醫師回傳 finalScore 的成功範例。複審已提交未結案時，案件維持 `REREVIEW_IN_PROGRESS`、最新複審提交為 `SUBMITTED`，不新增 enum（D-053）；回應不得含 `finalScore`（D-033）。其餘精確 response schema 仍為 unknown，本輪不捏造 DTO。

## 12.4 修正已提交複審

### `POST /review-cases/{caseId}/rereview/submissions/{submissionId}/revise`

路徑依 D-058（stage 在前）。

#### 權限

* `REREVIEWER`
* 且必須為複審醫師本人

#### 限制

* 結果不得已固定，案件不得為 `CLOSED`（D-032 取代 D-021 例外）
* 案件不得為 `CANCELLED`
* 不可覆寫舊 submission
* 建立新 revision

#### 錯誤（D-083）

* 案件已終止 `409 CASE_TERMINAL`；不是受指派的複審醫師 `403 NOT_ASSIGNED_REREVIEWER`；提交找不到 `404 SUBMISSION_NOT_FOUND`
* 提交不屬於本案或不是複審、不是目前有效的複審結果 `409 CONFLICT`
* 操作者不是提交者 `403 NOT_SUBMISSION_OWNER`；提交不是 `SUBMITTED` `409 SUBMISSION_NOT_SUBMITTED`
* 必答題沒有作答 `422 REQUIRED_ITEMS_NOT_ANSWERED`；`answers` 與 `comment` 都沒有變更 `409 NO_CHANGES`
* 回應沒有 `finalScore` 與初審分數（D-033）

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
    "totalScore": 5,
    "status": "SUBMITTED",
    "stage": "REREVIEW",
    "caseId": "RC-001"
  }
}
```

---

## 13. Submission Query APIs

---

## 13.1 查詢 submission 詳情

### `GET /review-submissions/{submissionId}`

路徑依 D-058 第 3 點：保留 submission 為根的查詢路徑，撤銷 D-022 對它的廢除。院區與角色檢查經由 submission 所屬的案件進行。

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
    "attachmentRefsJson": null,
    "maskedFields": ["answers", "comment", "totalScore", "attachmentRefsJson"]
  }
}
```

SYSTEM_ADMIN 也遮罩這四個欄位，且只輸出 permission-matrix §5.1 白名單裡有的欄位：`submissionId`、`caseId`、`stage`、`revisionNo`、`status`、`reviewerId`（`submittedBy`）、`submittedAt`、`maskedFields`，沒有 `createdAt`（D-084 第 4 點）。初審醫師可讀複審提交的完整內容（D-084 第 2 點）。

複審提交後及 CLOSED 後均維持遮罩，不得解除。finalScore／preview／diff 等可反推初審分數的衍生資訊亦不得提供（D-033）；此規則同樣適用 §10 案件、§13.2 歷程、§14 稽核、§15 報表及 command 回應。其他角色合法存取政策不變。

---

## 13.2 查詢 revision 歷程

### `GET /review-cases/{caseId}/revision-history`

路徑依 D-058（原寫 `revisions`）。複審歷程也由本 API 提供。原本的複審專用端點 `GET /review-cases/{caseId}/rereview-revisions` 沒有角色檢查，已依 D-059 移除。

#### 權限

* `HOSPITAL_MANAGER`
* `AUDITOR`
* `REVIEWER` / `REREVIEWER`：合法範圍內
* `SYSTEM_ADMIN`：metadata only。每一筆只有 `submissionId`、`stage`、`revisionNo`、`status`、`submittedAt`、`reviewerId`、`supersededBySubmissionId`，依 `permission-matrix.md` §5.1（D-067 第 4 點）

#### Response

下面是回應的範例，不分角色，不是任何角色的欄位清單。

```json
{
  "success": true,
  "data": [
    {
      "submissionId": "SUB-INIT-001",
      "stage": "INITIAL",
      "revisionNo": 1,
      "status": "SUPERSEDED",
      "submittedAt": "2026-04-02T06:10:00Z"
    },
    {
      "submissionId": "SUB-INIT-002",
      "stage": "INITIAL",
      "revisionNo": 2,
      "status": "SUBMITTED",
      "submittedAt": "2026-04-02T07:00:00Z"
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

#### `detail` 遮罩（D-071）

* 持有 `REREVIEWER` 或 `SYSTEM_ADMIN`：所有紀錄都不含 `detail`。
* 帶分數的 action（`INITIAL_REVIEW_COMPLETED`、`CASE_ESCALATED_TO_REREVIEW`、`REREVIEW_SUBMITTED`、`REREVIEW_REVISED`、`CASE_CLOSED_WITHOUT_REREVIEW`）：案件目前的狀態不是 `REREVIEW_IN_PROGRESS`／`CLOSED`／`CANCELLED`、讀者是現任或歷史複審醫師，或複審指派歷史不完整時，該筆 `detail` 為 `null`。
* 讀取不寫入初審讀取者紀錄。
* `detail` 的時間表示依 D-072 第 5 點：新紀錄帶時區（UTC），修正之前已寫入的紀錄不補改，新舊格式並存。

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

#### `detail` 遮罩（D-071）

* `SYSTEM_ADMIN` 或持有 `REREVIEWER`：所有紀錄都不含 `detail`。
* `ReviewCase` 帶分數的 action：逐列套用 14.1 的條件，符合時該筆 `detail` 為 `null`。

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

`from`、`to` 為選填的 ISO-8601 時間點（D-057 第 4 點）：兩個都給時，只統計案件建立時間 `from` ≤ `createdAt` < `to` 的案件，`from` 不早於 `to` 回 `400 BAD_REQUEST`；只給其中一個回 `400 BAD_REQUEST`；都不給時不依時間過濾。

---

## 15.2 匯出統計報表

### `POST /reports/export`

#### 權限

* `HOSPITAL_MANAGER`
* `AUDITOR`
* `SYSTEM_ADMIN`：metadata / aggregated only

#### 逐案分數遮罩（D-055 第 6 點）

* 案件狀態不是 `REREVIEW_IN_PROGRESS`、`CLOSED`、`CANCELLED` 時（之後仍可能指派或改派複審醫師），逐案 `initialScore`、`finalScore` 輸出 `null`，並列入該列的 `maskedFields`。
* 匯出者是該案目前的複審醫師或出現在複審指派歷史中，或該案歷史不完整時，同樣輸出 `null` 並列入 `maskedFields`（D-033 結案後不解除，D-035 fail-closed）。
* 以上都不成立時照常輸出。
* `SYSTEM_ADMIN` 任何狀態都不取得逐案分數。
* 匯出不寫入初審讀取者紀錄，也不遞增案件版本。

#### Response（分頁欄位、逐案列與遮罩相關的欄位）

逐案資料列 `rows` 為當頁內容，依 `caseId` 遞增排序；`totalElements`、`totalPages` 與統計摘要 `summary` 都以整個過濾範圍計算，不受分頁影響（D-057 第 3 點）。

```json
{
  "success": true,
  "data": {
    "reportType": "REVIEW_COMPLETION",
    "page": 0,
    "size": 100,
    "totalElements": 2,
    "totalPages": 1,
    "rows": [
      {
        "caseId": "RC-001",
        "createdAt": "2026-04-02T01:00:00Z",
        "status": "CLOSED",
        "reviewerId": "DOC-1001",
        "rereviewerId": "DOC-2001",
        "initialScore": 92,
        "finalScore": 86,
        "maskedFields": []
      },
      {
        "caseId": "RC-002",
        "createdAt": "2026-04-03T01:00:00Z",
        "status": "PENDING_REREVIEW",
        "reviewerId": "DOC-1002",
        "rereviewerId": null,
        "initialScore": null,
        "finalScore": null,
        "maskedFields": ["initialScore", "finalScore"]
      }
    ],
    "summary": {}
  }
}
```

`maskedFields` 一律存在；沒有遮罩時為空陣列。被遮罩的欄位輸出 `null`，前端不得把 `null` 當成 0 分。

#### Request

```json
{
  "reportType": "REVIEW_COMPLETION",
  "hospitalId": "H001",
  "from": "2026-03-31T16:00:00Z",
  "to": "2026-04-30T16:00:00Z",
  "page": 0,
  "size": 100
}
```

條件直接放在最外層，不包在 `filters` 物件中（D-056：以程式為準，對應 `ExportRequest`）。

| 欄位 | 必填 | 說明 |
|---|---|---|
| `reportType` | 是 | 目前只支援 `REVIEW_COMPLETION`；其他值回 `400 BAD_REQUEST`。 |
| `hospitalId` | 否 | 院區範圍依 D-042、D-047：只有 `SYSTEM_ADMIN` 可以用它縮小範圍，其他角色一律採登入者的院區，忽略此參數。 |
| `from`、`to` | 是 | ISO-8601 時間點（D-057）。以案件建立時間過濾，含起不含迄：`from` ≤ `createdAt` < `to`。缺少任一個，或 `from` 不早於 `to`，回 `400 BAD_REQUEST`。建立時間為 NULL 的案件（導入前已存在）不會出現在任何區間。 |
| `page` | 否 | 0 起算，預設 0；小於 0 以 0 計。 |
| `size` | 否 | 預設 100，上限 1,000；超過以 1,000 計，小於或等於 0 以 100 計。回應中的 `page`、`size` 為實際使用的值。 |

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
  "submittedAt": "2026-04-02T06:10:00Z"
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

原輪只調整既有 API 的業務契約，不新增 endpoint／DTO／欄位／error code。其後 D-053（複審結案）與 D-054（初審固定）各新增一個 endpoint，錯誤代碼都沿用既有代碼。

| 業務操作 | 必要契約 |
|---|---|
| 初審固定 | 與提交分離；僅目前有效初審結果的合法提交醫師（DD-03）；binding、回應與錯誤代碼見「DD-03 初審固定的語意契約」一節（D-054） |
| 進入／開始複審 | 初審已固定；閾值與 manager 手工派發規則不變 |
| 無需複審的正式結案 | 初審已固定，由院方管理者操作；未固定拒絕 |
| 複審 revision | 合法原提交者，已提交且未固定、未 CLOSED／CANCELLED；append-only，重算加權分數 |
| 複審正式結案 | 目前有效合法複審結果的提交醫師另行 Close Command；既有角色／scope／ownership 與資格成立；同時固定；binding、回應與錯誤代碼見「D-034 複審獨立 Close Command 對齊」一節（D-053） |
| 固定後／CLOSED revision 或 CLOSED reopen | 拒絕，不寫新 revision／成功事件 |

複審正式結案的拒絕條件與錯誤代碼已由 D-053 定案（見「D-034 複審獨立 Close Command 對齊」一節），初審固定的已由 D-054 定案（見「DD-03 初審固定的語意契約」一節），保留既有已確認的 role／scope 錯誤。契約行為變更需後續相容性與實作對齊，本文件不宣稱程式已支援。
複審醫師任何階段不得取得初審答案／分數／評語，亦不得從 command ack、list/detail、revision history/compare、audit、report/export、前端 cache 取得可反推值。finalScore 數值仍在 domain 依原公式計算；所有廣義 FULL view／共用回應範例都須先套用 D-033 角色限制。精確衍生欄位清單與多角色 policy 合成 unknown。

## DD-03 初審固定的語意契約（binding 已由 D-054 定案）

> D-054（2026-11-16）補足本節的 binding UNKNOWN。下方表格為 DD-03 原有的語意保證，仍然有效；表格前後原本標為 UNKNOWN 的項目（路徑、輸入欄位、重試策略、錯誤代碼）以本段為準。
>
> - `POST /review-cases/{caseId}/initial-review/submissions/{submissionId}/fix`，不需要請求內容；角色 `REVIEWER`，且必須是該有效初審結果的提交者。
> - 成功回應 `{ caseId, caseStatus, submissionId, fixedAt }`；`caseStatus` 為 `PENDING_REREVIEW`（分數 > 品質線）或 `INITIAL_REVIEW_COMPLETED`。
> - 錯誤代碼依 decision-log D-054 第 6 點：`ROLE_DENIED`、`CASE_NOT_FOUND`、`SUBMISSION_NOT_FOUND`、`HOSPITAL_SCOPE_MISMATCH`、`CASE_TERMINAL`、`INVALID_STATE_TRANSITION`（重複固定；固定後 revision；未固定就派複審或結案）、`CASE_NOT_INITIAL_REVIEW_COMPLETED`、`CONFLICT`（目標不是本案目前有效的初審結果；樂觀鎖失敗）、`SUBMISSION_NOT_SUBMITTED`、`NOT_SUBMISSION_OWNER`。
> - 提交初審（§11）不再依品質線進複審，案件一律停在 `INITIAL_REVIEW_COMPLETED`。
> - 案件詳情的 `allowedActions` 在可固定時加入 `FIX_INITIAL_REVIEW`。
> - 初審已固定時，案件詳情的 `initialReviewContext.pageMode` 為 `READONLY_BLOCKED`。`blockedReason` 依序判斷，取第一個成立的：`ROLE_DENIED`（不是 `REVIEWER`）→ `NOT_ASSIGNED_REVIEWER`（不是本案初審醫師）→ `CASE_ALREADY_CLOSED` → `CASE_ALREADY_CANCELLED` → `INITIAL_REVIEW_FIXED`。所以只有通過角色與指派檢查、且案件尚未結案或取消時，已固定才回 `INITIAL_REVIEW_FIXED`；已固定後又結案或取消的案件，回的是終態代碼。


沿用 `/api/quality-inspection/**`、`api-key` 與既有 actor context；固定為與 submit 分離的操作，路徑見上方 D-054，既有 submit 不負責固定。

| 項目 | 必要保證 |
|---|---|
| 授權 | 僅目前有效 INITIAL 結果的合法提交醫師；既有角色、院所 scope、ownership 全部適用；僅有 manager／指派／歷史提交身份不可代行 |
| 輸入語意 | 明確辨識 case 與欲固定的有效 submission，執行時驗證仍有效；以路徑上的 `caseId`、`submissionId` 辨識（D-054） |
| 成功 | 固定該有效結果且可追溯操作者、時間、submission；不自動結案；依 D-031 流向且遵守 D-033 response 保護 |
| 拒絕 | 無合法提交、DRAFT／SUPERSEDED、actor／scope 不符、terminal case、過期版本不得成功固定；無新 revision 或成功事件 |
| 併發 | 固定與 revision 不可同時對同一結果成功；不得將舊版請求悄悄改為固定新版；進複審／結案以已固定有效結果為準 |
| 重試 | 重複固定（含遺失成功回應後的重試）回 `INVALID_STATE_TRANSITION`，不視為成功（D-054 第 7 點）；不可據此改變已固定內容或重複推進流程 |

既有 role／hospital scope 錯誤維持 §3 的 403；既有通用 CONFLICT 為 409。錯誤代碼、schema 與 binding 已由 D-054 定案（見本節開頭）；程式尚未實作，相容性與 consumer 對齊尚未驗證。


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

沿用正式 base path 與 api-key／actor context baseline。以下 binding 依 D-053：

### `POST /review-cases/{caseId}/rereview/submissions/{submissionId}/close`

#### 權限

* `REREVIEWER`，且為該有效複審結果的提交者

#### Request

不需要請求內容。路徑上的 `submissionId` 是要結案的有效複審結果。

#### Response

```json
{
  "success": true,
  "data": {
    "caseId": "C-001",
    "caseStatus": "CLOSED",
    "submissionId": "SUB-RR-002"
  }
}
```

不回傳 `finalScore` 或任何可反推初審分數的資訊（D-033）。

#### Error（沿用既有代碼，依 D-024 的檢查順序）

| code | HTTP | 說明 |
|------|------|------|
| `ROLE_DENIED` | 403 | 不是 `REREVIEWER` |
| `HOSPITAL_SCOPE_MISMATCH` | 403 | 院區不符 |
| `CASE_TERMINAL` | 409 | 案件已是 `CLOSED` 或 `CANCELLED`（含重複結案） |
| `CASE_NOT_IN_REREVIEW_STAGE` | 422 | 案件不在複審中（D-069 修改，原為 `409 CASE_NOT_REREVIEW_IN_PROGRESS`） |
| `CONFLICT` | 409 | 結案目標不屬於本案、不是複審階段，或不是目前有效版本 |
| `SUBMISSION_NOT_SUBMITTED` | 409 | 結案目標仍是草稿 |
| `NOT_SUBMISSION_OWNER` | 403 | 不是這份有效結果的提交者 |

結案成功時，案件 `CLOSED` 即代表複審結果已固定，不另設欄位；有效版本為結案當下的 `latestRereviewSubmissionId`。複審提交與複審 revision 的回應同樣不得含 `finalScore`（D-033）。結案與 revision 同時發生的競爭，在 D-045 的樂觀鎖落地前是已知的剩餘風險。版本 token、多角色授權合成與 consumer 相容性方案仍為 UNKNOWN；既有通用 role／scope 與 conflict 契約不因此重新定義。


## D-035 歷史身份 Fail-closed 讀取契約

Submission Detail、Case Detail、Revision History：完整初審內容讀取新增 D-035 前置條件。Role／scope 符合但歷史身份 Proven historical 或 UNKNOWN 時，使用既有 HTTP 403、ACCESS_DENIED 與錯誤外殼；不得回傳受限內容、成功資料或默默降級。這包括合法 Manager／Auditor、無需複審案件及無可靠排除證明的初審 REVIEWER。非法 Role／院所等既有拒絕仍優先執行；合法 REREVIEWER 本人複審內容及 SYSTEM_ADMIN 白名單投影不因此擴張。路徑／schema 不變，無新的 completeness token。

> D-055（2026-11-16）：以只增不改的複審指派歷史提供負面身份證明。案件歷史完整（導入後建立的案件）且登入者不在歷史中，即為 Proven non-historical；歷史不完整仍為 UNKNOWN 並拒絕。HOSPITAL_MANAGER、AUDITOR、REVIEWER 在歷史完整的案件恢復完整讀取。每次准許完整初審讀取都記錄讀取者（回應前 commit，寫入失敗即拒絕），讀過的人不得再被指派或改派為同案複審醫師（`422 REREVIEWER_HAS_READ_INITIAL`）。匯出不記錄讀取者，改為在案件仍可能指派複審、匯出者是現任或歷史複審醫師、或歷史不完整時遮罩逐案分數（見 15.2）。其餘規則不變。


## D-036 — UI-010 metadata 八欄與 Lifecycle 延後邊界

UI-010 SYSTEM_ADMIN metadata-only 僅含 `caseId`, `caseStatus`, `projectName`, `hospitalId`, `reviewerId`, `rereviewerId`, `updatedAt`, `latestRevisionSummary`。`lifecycleLatestStage` 暫移除，JSON 必須不存在，不能回 null；目前狀態使用 `caseStatus`。

`updatedAt`、`latestRevisionSummary` 的值契約與實作仍未完成（UNKNOWN），目前 null 不表示已完成。不得加入 FULL-only、敏感巢狀資訊或因新增 DTO getter 自動擴大白名單。

既有 Lifecycle Query／API 與 EX-039／TS-TR-005 完整關鍵路徑需求保留；新增 Milestone／Latest Stage 投影延後，未取消需求。D-035 Fail-closed、D-033 永久盲審及既有 Role／Scope 不變。


## D-037 — updatedAt 契約

updatedAt 採案件層級持久化 UTC 時間點，ISO 8601 輸出；涵蓋 C／相關 S／兩種 Draft Save，建案初始化；缺乏可信歷史的 legacy 為 null。No-change 禁止寫入或宣稱新變更成功，回應見 D-038。八欄及授權不變；實作尚未完成。


## D-038 — No-change API 契約補充

無變更的 Draft Save 回 `409 Conflict`／`NO_CHANGES`。完整內容見 `decision-log.md` D-038；比較方式見 D-044。
