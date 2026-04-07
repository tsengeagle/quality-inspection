# B1 Review Project Configuration — Design Spec

> Created: 2026-04-07
> Phase: B1 封版
> Scope: ReviewProject + ScoreSheetDefinition 所有 Command + Query 完成
> Depends on: B0 底座

---

## 1. Goal

完成 ReviewProject 與 ScoreSheetDefinition 兩個 aggregate 的所有 B1 use case：

- 所有 Command API（CRUD + 狀態操作 + 綁定）
- 所有 Query API（列表 + 詳情 + 版本歷史）
- 完整權限 guard（role + hospital scope）
- Domain Event 發布 + Audit Trail 記錄
- 完整測試覆蓋

---

## 2. ReviewProject Command 補完

### 2.1 已實作（需修改）

**CreateReviewProjectUseCase**
- 移除 request body 中的 `hospitalId` 欄位
- 改從 `actor.requireHospitalId()` 取得
- 加入 `actor.requireRole(HOSPITAL_MANAGER)`
- 發布 `ReviewProjectCreatedEvent`

**CreateReviewProjectRequest DTO**
- 移除 `hospitalId` 欄位
- 只保留 `name`, `medicalRecordTypes`

### 2.2 需修改 Domain

**ReviewProject aggregate 新增欄位：**
- `boundScoreSheetId` (String, nullable)
- `description` (String, nullable)
- `status` — 確認既有 ProjectStatus 是否已有 DRAFT/ACTIVE/DEACTIVATED

**ReviewProject aggregate 新增方法：**
- `updateDraft(String name, String description, List<MedicalRecordType> types)` — 前置：DRAFT 狀態
- `activate()` — 前置：有 published version + boundScoreSheetId != null
- `deactivate()` — 前置：ACTIVE 狀態
- `bindScoreSheet(String scoreSheetId)` — 前置：DRAFT 狀態

**ReviewProjectEntity JPA**
- 新增 `bound_score_sheet_id` column
- 新增 `description` column（若尚未存在）

### 2.3 新增 Use Cases

#### UpdateReviewProjectDraftUseCase
- Input: projectId, name, description, medicalRecordTypes
- 權限: `actor.requireRole(HOSPITAL_MANAGER)` + `actor.requireHospitalScope(project.getHospitalId())`
- 前置: project 在 DRAFT 狀態
- 發布: `ReviewProjectDraftUpdatedEvent`

#### ActivateReviewProjectUseCase
- Input: projectId
- 權限: `actor.requireRole(HOSPITAL_MANAGER)` + `actor.requireHospitalScope(project.getHospitalId())`
- 前置: project 有 published version + boundScoreSheetId != null
- 發布: `ReviewProjectActivatedEvent`

#### DeactivateReviewProjectUseCase
- Input: projectId
- 權限: `actor.requireRole(HOSPITAL_MANAGER)` + `actor.requireHospitalScope(project.getHospitalId())`
- 前置: project 在 ACTIVE 狀態
- 發布: `ReviewProjectDeactivatedEvent`

#### BindScoreSheetToProjectUseCase
- Input: projectId, scoreSheetId
- 權限: `actor.requireRole(HOSPITAL_MANAGER)` + `actor.requireHospitalScope(project.getHospitalId())`
- 前置: project 在 DRAFT 狀態
- 驗證: scoreSheetId 必須是 published 狀態，且 hospitalId 為全局(null) 或 actor 的醫院
- 發布: `ScoreSheetBoundToProjectEvent`

---

## 3. ScoreSheetDefinition Command 補完

### 3.1 已實作（需修改）

**CreateScoreSheetDraftUseCase**
- hospitalId 邏輯: HOSPITAL_MANAGER → `actor.requireHospitalId()`; SYSTEM_ADMIN → `null`
- 加入 `actor.requireAnyRole(HOSPITAL_MANAGER, SYSTEM_ADMIN)`
- 發布 `ScoreSheetDraftCreatedEvent`

### 3.2 Domain 變更

**ScoreSheetDefinition aggregate 新增欄位：**
- `hospitalId` (String, nullable — null 表示全局)

**ScoreSheetDefinition aggregate 新增方法：**
- `updateMetadata(String name, String description)` — 前置：DRAFT 狀態
- `removeSection(String sectionId)` — 前置：DRAFT 狀態
- `reorderSections(List<String> sectionIds)` — 前置：DRAFT 狀態
- `updateSection(String sectionId, String name, BigDecimal weight)` — 前置：DRAFT 狀態

**ScoreSheetSection 新增方法：**
- `removeItem(String itemId)`
- `reorderItems(List<String> itemIds)`
- `updateItem(String itemId, String name, BigDecimal maxScore)`

**ScoreSheetDefinitionEntity JPA**
- 新增 `hospital_id` column

### 3.3 ScoreSheet 權限 Guard

```java
void requireScoreSheetAccess(ActorContext actor, ScoreSheetDefinition sheet) {
    if (actor.hasRole(SYSTEM_ADMIN)) {
        // SYSTEM_ADMIN: 可操作全局(null) + 自己醫院的
        if (sheet.getHospitalId() != null) {
            actor.requireHospitalScope(sheet.getHospitalId());
        }
        // hospitalId == null (全局) → 允許
    } else if (actor.hasRole(HOSPITAL_MANAGER)) {
        // HOSPITAL_MANAGER: 只能操作自己醫院的
        if (sheet.getHospitalId() == null) {
            throw new AccessDeniedException("Hospital manager cannot operate on global score sheet");
        }
        actor.requireHospitalScope(sheet.getHospitalId());
    } else {
        throw new AccessDeniedException("Insufficient role");
    }
}
```

### 3.4 新增 Use Cases

#### UpdateScoreSheetDraftUseCase
- Input: scoreSheetId, name, description
- 權限: requireScoreSheetAccess
- 前置: DRAFT 狀態
- 發布: `ScoreSheetDraftUpdatedEvent`

#### ManageScoreSheetSectionsUseCase (或拆為獨立 use case)
- Section CRUD: addSection, removeSection, updateSection, reorderSections
- Item CRUD: addItem, removeItem, updateItem, reorderItems
- 權限: requireScoreSheetAccess
- 前置: DRAFT 狀態

> 實作時可選擇拆成多個小 use case 或一個 orchestrator，由 plan 階段決定。

---

## 4. Query APIs

### 4.1 ReviewProject Query

#### GET /api/quality-inspection/review-projects
- Params: `status` (optional), `name` (optional, fuzzy), `page`, `size`
- HOSPITAL_MANAGER: 自動過濾 `actor.hospitalId`
- SYSTEM_ADMIN: 回傳全部（B1 先不做 metadata 區分）
- Response: paginated list `{ content: [...], page, size, totalElements, totalPages }`

#### GET /api/quality-inspection/review-projects/{id}
- HOSPITAL_MANAGER: `requireHospitalScope(project.hospitalId)`
- SYSTEM_ADMIN: 允許存取
- Response: full project detail + current version + boundScoreSheetId

#### GET /api/quality-inspection/review-projects/{id}/versions
- 同上權限
- Response: list of ProjectVersion

### 4.2 ScoreSheet Query

#### GET /api/quality-inspection/score-sheets
- Params: `status` (optional), `name` (optional, fuzzy), `page`, `size`
- HOSPITAL_MANAGER: 只看 `hospitalId = actor.hospitalId`
- SYSTEM_ADMIN: 看 `hospitalId IS NULL` (全局) + `hospitalId = actor.hospitalId`
- Response: paginated list

#### GET /api/quality-inspection/score-sheets/{id}
- HOSPITAL_MANAGER: `requireHospitalScope(sheet.hospitalId)`
- SYSTEM_ADMIN: 全局 + 自己醫院
- Response: full detail with sections and items

#### GET /api/quality-inspection/score-sheets/{id}/versions
- 同上權限
- Response: version history

---

## 5. API Endpoint 與 Permission Guard 總表

| Endpoint | HTTP | Role | Scope Guard |
|---|---|---|---|
| `POST /review-projects` | Create | HM | hospitalId from actor |
| `PUT /review-projects/{id}` | UpdateDraft | HM | requireHospitalScope |
| `POST /review-projects/{id}/versions/publish` | Publish | HM | requireHospitalScope |
| `POST /review-projects/{id}/activate` | Activate | HM | requireHospitalScope |
| `POST /review-projects/{id}/deactivate` | Deactivate | HM | requireHospitalScope |
| `POST /review-projects/{id}/bind-score-sheet` | Bind | HM | requireHospitalScope |
| `GET /review-projects` | List | HM+SA | auto-filter |
| `GET /review-projects/{id}` | Detail | HM+SA | scope check |
| `GET /review-projects/{id}/versions` | Versions | HM+SA | scope check |
| `POST /score-sheets` | Create | HM+SA | hospitalId logic |
| `PUT /score-sheets/{id}` | UpdateDraft | HM+SA | scoreSheetAccess |
| `POST /score-sheets/{id}/sections` | AddSection | HM+SA | scoreSheetAccess |
| `DELETE /score-sheets/{id}/sections/{sId}` | RemoveSection | HM+SA | scoreSheetAccess |
| `PUT /score-sheets/{id}/sections/reorder` | Reorder | HM+SA | scoreSheetAccess |
| `PUT /score-sheets/{id}/sections/{sId}` | UpdateSection | HM+SA | scoreSheetAccess |
| `POST /score-sheets/{id}/sections/{sId}/items` | AddItem | HM+SA | scoreSheetAccess |
| `DELETE /score-sheets/{id}/sections/{sId}/items/{iId}` | RemoveItem | HM+SA | scoreSheetAccess |
| `PUT /score-sheets/{id}/sections/{sId}/items/reorder` | ReorderItems | HM+SA | scoreSheetAccess |
| `PUT /score-sheets/{id}/sections/{sId}/items/{iId}` | UpdateItem | HM+SA | scoreSheetAccess |
| `POST /score-sheets/{id}/versions/publish` | Publish | HM+SA | scoreSheetAccess |
| `GET /score-sheets` | List | HM+SA | auto-filter |
| `GET /score-sheets/{id}` | Detail | HM+SA | scope check |
| `GET /score-sheets/{id}/versions` | Versions | HM+SA | scope check |

HM = HOSPITAL_MANAGER, SA = SYSTEM_ADMIN

---

## 6. Domain Events

B1 application services 發布以下 events：

- `ReviewProjectCreatedEvent`
- `ReviewProjectDraftUpdatedEvent`
- `ReviewProjectVersionPublishedEvent`
- `ReviewProjectActivatedEvent`
- `ReviewProjectDeactivatedEvent`
- `ScoreSheetBoundToProjectEvent`
- `ScoreSheetDraftCreatedEvent`
- `ScoreSheetDraftUpdatedEvent`
- `ScoreSheetVersionPublishedEvent`

所有 events 繼承 `DomainEvent` base class，由 `AuditTrailListener` 自動記錄到 audit_trail 表。

---

## 7. 測試策略

| 測試類別 | 驗證內容 |
|---|---|
| Domain unit test | aggregate invariant — activate 前置、DRAFT guard、bind 限制 |
| Domain unit test | ScoreSheet section/item CRUD、reorder |
| Use case unit test | 每個 use case mock repo 測正常 + 異常 |
| Integration test | real DB 驗完整流程 |
| API test | MockMvc + TestTokenFactory 驗 endpoint + 權限 |
| Scope guard test | 跨院存取被拒絕 (H001 vs H002) |
| Role guard test | 錯誤 role 被拒絕 (403) |
| Query filter test | 分頁、status 過濾、name 模糊搜尋 |

---

## 8. 不包含

- SYSTEM_ADMIN metadata-only response（延後定義）
- ReviewProject 跨院操作
- ScoreSheet 跨醫院操作（SYSTEM_ADMIN 只能全局 + 自己醫院）
- ReviewCase 完整生命週期（B3）
