# B2 Sampling — Design Spec

> Created: 2026-04-07
> Phase: B2 封版
> Scope: SamplingBatch 權限補完 + Query/Export API + hospitalId
> Depends on: B0 底座, B1 封版

---

## 1. Goal

完成 SamplingBatch 的所有 B2 use case：

- 既有 Command 加入完整權限 guard + hospitalId
- 新增 Query API（列表 + 詳情）
- 新增 Export API（JSON 格式）
- Domain Event 發布 + Audit Trail 記錄
- 完整測試覆蓋

---

## 2. SamplingBatch Domain 變更

### 2.1 新增欄位

- `hospitalId` (String, not null) — 建立時從關聯的 ReviewProject 取得

### 2.2 建立流程變更

`ExecuteSamplingUseCase` 在建立 SamplingBatch 時：
1. 載入 ReviewProject
2. 取得 `project.getHospitalId()`
3. 傳入 SamplingBatch 建構

### 2.3 Persistence 變更

- `SamplingBatchEntity` — 新增 `hospital_id` column (VARCHAR(50), NOT NULL)
- `SamplingBatchRepositoryImpl` — mapper 更新（domain ↔ entity 雙向）
- `SamplingBatchJpaRepository` — 新增查詢方法

---

## 3. 既有 Command 權限補完

### 3.1 ExecuteSamplingUseCase
- 加入 `actor.requireRole(HOSPITAL_MANAGER)`
- 加入 `actor.requireHospitalScope(project.getHospitalId())`
- 發布 `SamplingBatchExecutedEvent`（含 COMPLETED 或 FAILED 子事件）

### 3.2 PreviewSamplingPopulationUseCase
- 加入 `actor.requireRole(HOSPITAL_MANAGER)`
- 加入 `actor.requireHospitalScope(project.getHospitalId())`
- 不發布 domain event（preview 是唯讀操作）

### 3.3 RetrySamplingBatchUseCase
- 加入 `actor.requireRole(HOSPITAL_MANAGER)`
- 載入 batch → `actor.requireHospitalScope(batch.getHospitalId())`
- 發布 `SamplingBatchRetriedEvent`

---

## 4. 新增 Use Cases

### 4.1 ListSamplingBatchesUseCase

- Input: projectId (必填), status (選填), startDate/endDate (選填), page, size
- 權限:
  - HOSPITAL_MANAGER: 驗證 project.hospitalId == actor.hospitalId
  - SYSTEM_ADMIN: 允許存取（不限 hospitalId）
- Output: paginated list of batch summaries (batchId, projectId, status, startedAt, finishedAt, selectedCount, hospitalId)

### 4.2 GetSamplingBatchDetailUseCase

- Input: batchId
- 權限:
  - HOSPITAL_MANAGER: `requireHospitalScope(batch.hospitalId)`
  - SYSTEM_ADMIN: 允許存取
- Output: full batch detail including selectedCases list, ruleSnapshot, failureReason

### 4.3 ExportSamplingBatchResultUseCase

- Input: batchId
- 權限: `actor.requireRole(HOSPITAL_MANAGER)` + `requireHospitalScope(batch.hospitalId)`
- Output: JSON response containing:
  - batch metadata (batchId, projectId, status, startedAt, finishedAt, counts)
  - complete selectedCases list
  - ruleSnapshot
- B2 範圍只做 JSON endpoint，Excel/CSV 匯出延到 B7 Reporting

---

## 5. API Endpoints

| Endpoint | HTTP | Role | Scope Guard |
|---|---|---|---|
| `POST /sampling-batches/execute` | Execute | HM | requireHospitalScope(project) |
| `POST /sampling-batches/preview` | Preview | HM | requireHospitalScope(project) |
| `POST /sampling-batches/{id}/retry` | Retry | HM | requireHospitalScope(batch) |
| `GET /sampling-batches` | List | HM+SA | auto-filter (projectId required) |
| `GET /sampling-batches/{id}` | Detail | HM+SA | scope check |
| `GET /sampling-batches/{id}/export` | Export JSON | HM | requireHospitalScope |

HM = HOSPITAL_MANAGER, SA = SYSTEM_ADMIN

### 5.1 GET /sampling-batches Query Params

| Param | Type | Required | Description |
|---|---|---|---|
| projectId | String | Yes | 關聯的 ReviewProject ID |
| status | String | No | BatchStatus enum value |
| startDate | ISO date | No | startedAt >= |
| endDate | ISO date | No | startedAt <= |
| page | int | No | default 0 |
| size | int | No | default 20 |

---

## 6. Domain Events

B2 application services 發布以下 events：

- `SamplingBatchExecutedEvent` — batch 建立並執行（含最終狀態 COMPLETED/FAILED）
- `SamplingBatchRetriedEvent` — retry 建立新 batch

所有 events 繼承 `DomainEvent` base class，由 `AuditTrailListener` 自動記錄。

---

## 7. JPA Repository 查詢方法

```java
// SamplingBatchJpaRepository 新增
Page<SamplingBatchEntity> findByProjectIdAndHospitalId(
    String projectId, String hospitalId, Pageable pageable);

Page<SamplingBatchEntity> findByProjectId(
    String projectId, Pageable pageable);

Page<SamplingBatchEntity> findByProjectIdAndStatus(
    String projectId, BatchStatus status, Pageable pageable);

Page<SamplingBatchEntity> findByProjectIdAndHospitalIdAndStatus(
    String projectId, String hospitalId, BatchStatus status, Pageable pageable);
```

實際查詢方法依 Spring Data JPA naming convention 或 `@Query` 實作。

---

## 8. 測試策略

| 測試類別 | 驗證內容 |
|---|---|
| Domain unit test | SamplingBatch hospitalId 設定 |
| Use case unit test | 權限 guard 正常 + 異常 |
| Use case unit test | List/Detail/Export 正常流程 |
| Integration test | real DB 驗完整流程 + hospitalId 過濾 |
| API test | 既有 endpoint 加權限驗證 |
| API test | 新 endpoint 正常 + 異常 |
| Scope guard test | 跨院存取被拒絕 |
| Query filter test | projectId + status + dateRange + 分頁 |

---

## 9. 不包含

- ReviewCase 完整生命週期（B3）
- Excel/CSV 匯出（B7）
- Cross-batch duplicate 檢查（per-batch only，已確認）
- SamplingBatch CANCELLED 狀態操作（若有需要延後）
