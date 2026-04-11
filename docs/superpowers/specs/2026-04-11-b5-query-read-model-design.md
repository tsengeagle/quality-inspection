# B5 — Backend Query / Read Model Design Spec

## 1. Overview

### 1.1 Goal

Implement the remaining 7 query/read endpoints defined in `api-spec.md` that have not yet been built. This completes the backend query surface for case listing, case detail, submission detail, audit trail, lifecycle, dashboard, and export.

### 1.2 Scope

| Group | Endpoints | Description |
|-------|-----------|-------------|
| B5a | `GET /review-cases`, `GET /review-cases/{caseId}`, `GET /review-submissions/{submissionId}` | Case + Submission queries |
| B5b | `GET /review-cases/{caseId}/lifecycle`, `GET /audit-trails` | Audit / Lifecycle queries |
| B5c | `GET /reports/dashboard-summary`, `POST /reports/export` | Dashboard + Export |

### 1.3 Non-Goals

- Response view 分級（metadata vs full detail）— 後續做
- 細粒度 scope 過濾（REVIEWER 只看自己的、AUDITOR 範圍限制）— 後續做
- Export 檔案產生（Excel/CSV）— 這次只回 JSON 數據
- 前端頁面
- Command side 任何修改

### 1.4 Authority References

- `docs/03-contracts/api-spec.md` §10, §13, §14, §15, §16
- `docs/02-ddd/application-service.md` §10
- `docs/02-ddd/permission-matrix.md`
- Existing query patterns: `SamplingBatchQueryPort`, `ReviewProjectQueryPort`, `ScoreSheetQueryPort`

---

## 2. Technical Strategy

### 2.1 Query Architecture

Follow the established `QueryPort` pattern:

```
Controller → UseCase → QueryPort (interface) → Adapter (JPA) → DB
```

- **B5a / B5b**: QueryPort interfaces in `application/` layer, adapters in `infrastructure/` layer
- **B5c**: Dashboard/Export services in `reporting/` layer with JPQL aggregation queries

### 2.2 Pagination

Use the existing `page`/`size` parameter pattern with `Page<T>`:
- `page` defaults to 0, `size` defaults to 20, max 100
- Consistent with `SamplingController.listBatches()` and `ListSamplingBatchesUseCase`

### 2.3 Authorization

Basic role check only (this iteration):
- Verify actor has a legal role for the endpoint
- Do NOT implement response view splitting (metadata vs full detail)
- Do NOT implement fine-grained scope filtering

### 2.4 DTO Conventions

- Application layer: `*Summary` (list items), `*DetailView` (single detail), `*Entry` (audit)
- Interface layer: `*Response` (REST DTOs)
- Criteria: `*QueryCriteria`

---

## 3. B5a — Case + Submission Queries

### 3.1 `GET /review-cases` — List Review Cases

#### API Contract (from api-spec.md §10.1)

```
GET /review-cases?hospitalId=&projectId=&batchId=&status=&reviewerId=&rereviewerId=&medicalRecordType=&page=0&size=20
```

**Roles**: All roles (basic role check)

#### New Classes

| Layer | Class | Location |
|-------|-------|----------|
| application | `ReviewCaseQueryPort` | `application/reviewcase/query/` |
| application | `ReviewCaseQueryCriteria` | `application/reviewcase/query/` |
| application | `ReviewCaseSummary` | `application/reviewcase/query/` |
| application | `ListReviewCasesUseCase` | `application/reviewcase/` |
| infrastructure | `ReviewCaseQueryAdapter` | `infrastructure/persistence/reviewcase/` |
| interfaces | `ReviewCaseListResponse` | `interfaces/rest/dto/` |

#### ReviewCaseQueryCriteria Fields

```java
String hospitalId;       // optional filter
String projectId;        // optional filter
String batchId;          // optional filter
String status;           // optional filter (CaseStatus enum)
String reviewerId;       // optional filter
String rereviewerId;     // optional filter
String medicalRecordType; // optional filter
int page;                // default 0
int size;                // default 20, max 100
```

#### ReviewCaseSummary Fields

```java
String caseId;
String projectId;
String batchId;
String hospitalId;
String status;           // CaseStatus name
String reviewerId;
String rereviewerId;
String medicalRecordType;
String sourceCaseKey;
Instant createdAt;
Instant assignedAt;
Instant dueAt;
```

#### ListReviewCasesUseCase

```java
public Page<ReviewCaseSummary> execute(ActorContext actor, ReviewCaseQueryCriteria criteria) {
    actor.requireAnyRole("HOSPITAL_MANAGER", "REVIEWER", "REREVIEWER", "AUDITOR", "SYSTEM_ADMIN");
    return queryPort.findCases(criteria);
}
```

### 3.2 `GET /review-cases/{caseId}` — Case Detail

#### API Contract (from api-spec.md §10.2)

```
GET /review-cases/{caseId}
```

**Roles**: All roles (basic role check)

#### New Classes

| Layer | Class | Location |
|-------|-------|----------|
| application | `ReviewCaseDetailView` | `application/reviewcase/query/` |
| application | `GetReviewCaseDetailUseCase` | `application/reviewcase/` |
| interfaces | `ReviewCaseDetailResponse` | `interfaces/rest/dto/` |

#### ReviewCaseDetailView Fields

```java
String caseId;
String projectId;
String batchId;
String hospitalId;
String status;
String reviewerId;
String rereviewerId;
String medicalRecordType;
String sourceCaseKey;
BigDecimal initialScore;
BigDecimal rereviewScore;
BigDecimal finalScore;
Instant createdAt;
Instant assignedAt;
Instant dueAt;
Instant closedAt;
Instant cancelledAt;
List<SubmissionSummaryItem> submissions;  // latest submissions per stage
```

#### SubmissionSummaryItem (nested)

```java
String submissionId;
String stage;           // INITIAL or REREVIEW
int revisionNo;
String status;          // DRAFT, SUBMITTED, SUPERSEDED
Instant submittedAt;
```

#### GetReviewCaseDetailUseCase

```java
public ReviewCaseDetailView execute(ActorContext actor, String caseId) {
    actor.requireAnyRole("HOSPITAL_MANAGER", "REVIEWER", "REREVIEWER", "AUDITOR", "SYSTEM_ADMIN");
    return queryPort.findDetailById(caseId)
            .orElseThrow(() -> new ResourceNotFoundException("ReviewCase", caseId));
}
```

### 3.3 `GET /review-submissions/{submissionId}` — Submission Detail

#### API Contract (from api-spec.md §13.1)

```
GET /review-submissions/{submissionId}
```

**Roles**: All roles (basic role check)

#### New Classes

| Layer | Class | Location |
|-------|-------|----------|
| application | `SubmissionQueryPort` | `application/reviewcase/query/` |
| application | `SubmissionDetailView` | `application/reviewcase/query/` |
| application | `GetSubmissionDetailUseCase` | `application/reviewcase/` |
| infrastructure | `SubmissionQueryAdapter` | `infrastructure/persistence/submission/` |
| interfaces | `SubmissionController` (new) | `interfaces/rest/` |
| interfaces | `SubmissionDetailResponse` | `interfaces/rest/dto/` |

#### SubmissionDetailView Fields

```java
String submissionId;
String caseId;
String stage;            // INITIAL or REREVIEW
int revisionNo;
String status;           // DRAFT, SUBMITTED, SUPERSEDED
String reviewerId;
List<AnswerDetail> answers;
String comment;
List<AttachmentRef> attachments;
BigDecimal totalScore;
Instant createdAt;
Instant submittedAt;
```

#### AnswerDetail (nested)

```java
String itemId;
Object answerValue;      // boolean or numeric
BigDecimal derivedScore;
```

#### AttachmentRef (nested)

```java
String fileId;
String fileName;
```

### 3.4 ReviewCaseQueryPort Interface

```java
public interface ReviewCaseQueryPort {
    Page<ReviewCaseSummary> findCases(ReviewCaseQueryCriteria criteria);
    Optional<ReviewCaseDetailView> findDetailById(String caseId);
}
```

### 3.5 SubmissionQueryPort Interface

```java
public interface SubmissionQueryPort {
    Optional<SubmissionDetailView> findDetailById(String submissionId);
}
```

### 3.6 Infrastructure Adapters

#### ReviewCaseQueryAdapter

- Implements `ReviewCaseQueryPort`
- Uses existing `ReviewCaseJpaEntity` + JPA Specification or JPQL for dynamic filtering
- Joins with submission table for `ReviewCaseDetailView.submissions`

#### SubmissionQueryAdapter

- Implements `SubmissionQueryPort`
- Uses existing `ReviewSubmissionJpaEntity`
- Maps answers from JSON column

### 3.7 Controller Changes

**ReviewCaseController** — add 2 GET endpoints:

```java
@GetMapping
public ResponseEntity<ApiResponse<Page<ReviewCaseSummary>>> listCases(
        @CurrentActor ActorContext actor,
        @RequestParam(required = false) String hospitalId,
        @RequestParam(required = false) String projectId,
        @RequestParam(required = false) String batchId,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String reviewerId,
        @RequestParam(required = false) String rereviewerId,
        @RequestParam(required = false) String medicalRecordType,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size);

@GetMapping("/{caseId}")
public ResponseEntity<ApiResponse<ReviewCaseDetailView>> getCaseDetail(
        @CurrentActor ActorContext actor,
        @PathVariable String caseId);
```

**SubmissionController** (new controller at `/review-submissions`):

```java
@GetMapping("/{submissionId}")
public ResponseEntity<ApiResponse<SubmissionDetailView>> getSubmissionDetail(
        @CurrentActor ActorContext actor,
        @PathVariable String submissionId);
```

---

## 4. B5b — Audit / Lifecycle Queries

### 4.1 `GET /audit-trails` — Audit Trail Query

#### API Contract (from api-spec.md §14.2)

```
GET /audit-trails?aggregateType=&aggregateId=&hospitalId=&eventName=&from=&to=&page=0&size=20
```

**Roles**: SYSTEM_ADMIN, HOSPITAL_MANAGER, AUDITOR

#### New Classes

| Layer | Class | Location |
|-------|-------|----------|
| application | `AuditTrailQueryPort` | `application/audit/` |
| application | `AuditTrailQueryCriteria` | `application/audit/` |
| application | `AuditTrailEntry` | `application/audit/` |
| application | `ListAuditTrailsUseCase` | `application/audit/` |
| infrastructure | `AuditTrailQueryAdapter` | `infrastructure/audit/` |
| interfaces | `AuditController` (new) | `interfaces/rest/` |
| interfaces | `AuditTrailResponse` | `interfaces/rest/dto/` |

#### AuditTrailQueryCriteria Fields

```java
String aggregateType;    // optional (maps to entityType)
String aggregateId;      // optional (maps to entityId)
String hospitalId;       // optional
String eventName;        // optional (maps to action)
Instant from;            // optional
Instant to;              // optional
int page;                // default 0
int size;                // default 20, max 100
```

#### AuditTrailEntry Fields

```java
String id;
String entityType;
String entityId;
String action;
String actorUserId;
String actorUserName;
String hospitalId;
Instant occurredAt;
String detail;
```

#### AuditTrailQueryAdapter

- Extends `AuditTrailJpaRepository` with custom query methods
- Uses JPA Specification for dynamic filtering on `AuditTrailEntity`

### 4.2 `GET /review-cases/{caseId}/lifecycle` — Case Lifecycle

#### API Contract (from api-spec.md §14.1)

```
GET /review-cases/{caseId}/lifecycle
```

**Roles**: HOSPITAL_MANAGER, AUDITOR, REVIEWER/REREVIEWER (in scope), SYSTEM_ADMIN (metadata only)

#### Design

`GetCaseLifecycleUseCase` delegates to `AuditTrailQueryPort` with fixed criteria:
- `aggregateType = "ReviewCase"`
- `aggregateId = caseId`
- Sorted by `occurredAt` ascending
- No pagination (lifecycle is typically a bounded list)

#### New Classes

| Layer | Class | Location |
|-------|-------|----------|
| application | `GetCaseLifecycleUseCase` | `application/audit/` |

Uses `AuditTrailQueryPort` — no separate port needed.

#### Controller

Add to `ReviewCaseController`:

```java
@GetMapping("/{caseId}/lifecycle")
public ResponseEntity<ApiResponse<List<AuditTrailEntry>>> getCaseLifecycle(
        @CurrentActor ActorContext actor,
        @PathVariable String caseId);
```

---

## 5. B5c — Dashboard + Export

### 5.1 `GET /reports/dashboard-summary` — Dashboard

#### API Contract (from api-spec.md §15.1)

```
GET /reports/dashboard-summary?hospitalId=&projectId=&from=&to=
```

**Roles**: HOSPITAL_MANAGER, AUDITOR, SYSTEM_ADMIN

#### New Classes

| Layer | Class | Location |
|-------|-------|----------|
| reporting | `DashboardQueryService` | `reporting/` |
| reporting | `DashboardSummary` | `reporting/` |
| reporting | `DashboardQueryCriteria` | `reporting/` |
| infrastructure | `DashboardQueryAdapter` | `reporting/` (inner, or infrastructure) |
| interfaces | `ReportController` (new) | `interfaces/rest/` |
| interfaces | `DashboardSummaryResponse` | `interfaces/rest/dto/` |

#### DashboardQueryCriteria Fields

```java
String hospitalId;   // optional
String projectId;    // optional
Instant from;        // optional
Instant to;          // optional
```

#### DashboardSummary Fields

```java
long totalCases;
long closedCases;
long rereviewedCases;
long overdueCases;
double completionRate;   // closedCases / totalCases
double rereviewRate;     // rereviewedCases / totalCases
double overdueRate;      // overdueCases / totalCases
```

#### DashboardQueryService

```java
@Service
public class DashboardQueryService {
    public DashboardSummary getSummary(ActorContext actor, DashboardQueryCriteria criteria) {
        actor.requireAnyRole("HOSPITAL_MANAGER", "AUDITOR", "SYSTEM_ADMIN");
        // JPQL aggregation queries
    }
}
```

#### Dashboard Calculation Logic

```sql
-- Total cases (with optional filters)
SELECT COUNT(*) FROM review_case WHERE [filters]

-- Closed cases
SELECT COUNT(*) FROM review_case WHERE status = 'CLOSED' AND [filters]

-- Rereviewed cases (cases that entered rereview at any point)
SELECT COUNT(*) FROM review_case WHERE rereviewer_id IS NOT NULL AND [filters]

-- Overdue cases (past due and not completed)
SELECT COUNT(*) FROM review_case
WHERE due_at < CURRENT_TIMESTAMP
AND status NOT IN ('CLOSED', 'CANCELLED')
AND [filters]
```

Rates are computed in Java: `closedCases / (double) totalCases` etc., with totalCases=0 → rate=0.0 guard.

### 5.2 `POST /reports/export` — Export Data

#### API Contract (from api-spec.md §15.2)

```
POST /reports/export
{
  "reportType": "REVIEW_COMPLETION",
  "filters": { "hospitalId": "...", "from": "...", "to": "..." }
}
```

**Roles**: HOSPITAL_MANAGER, AUDITOR, SYSTEM_ADMIN

#### Design Decision

This iteration returns JSON data only (no file generation). The response wraps the query result in the standard `ApiResponse` envelope.

#### New Classes

| Layer | Class | Location |
|-------|-------|----------|
| reporting | `ExportQueryService` | `reporting/` |
| reporting | `ExportResult` | `reporting/` |
| reporting | `ExportCriteria` | `reporting/` |
| interfaces | `ExportRequest` | `interfaces/rest/dto/` |
| interfaces | `ExportResponse` | `interfaces/rest/dto/` |

#### ExportCriteria Fields

```java
String reportType;      // "REVIEW_COMPLETION"
String hospitalId;      // optional
Instant from;           // optional
Instant to;             // optional
```

#### ExportResult Fields

```java
String reportType;
List<ExportRow> rows;
DashboardSummary summary;  // reuse dashboard summary
```

#### ExportRow (for REVIEW_COMPLETION)

```java
String caseId;
String projectId;
String hospitalId;
String status;
String reviewerId;
String rereviewerId;
BigDecimal initialScore;
BigDecimal rereviewScore;
BigDecimal finalScore;
Instant createdAt;
Instant closedAt;
```

#### ExportQueryService

```java
@Service
public class ExportQueryService {
    public ExportResult export(ActorContext actor, ExportCriteria criteria) {
        actor.requireAnyRole("HOSPITAL_MANAGER", "AUDITOR", "SYSTEM_ADMIN");
        if (!"REVIEW_COMPLETION".equals(criteria.getReportType())) {
            throw new BadRequestException("Unsupported reportType: " + criteria.getReportType());
        }
        // Query case data + compute summary
    }
}
```

---

## 6. Testing Strategy

### 6.1 Unit Tests (Use Case / Service level)

| Test Class | Scope |
|------------|-------|
| `ListReviewCasesUseCaseTest` | Filter combinations, pagination, role check, empty result |
| `GetReviewCaseDetailUseCaseTest` | Found, not found, role check |
| `GetSubmissionDetailUseCaseTest` | Found, not found, role check |
| `ListAuditTrailsUseCaseTest` | Filter combinations, pagination, role check |
| `GetCaseLifecycleUseCaseTest` | Found events, empty events, role check |
| `DashboardQueryServiceTest` | Rate calculations, zero-total guard, filter passthrough |
| `ExportQueryServiceTest` | Valid reportType, invalid reportType, filter passthrough |

Test double pattern: InMemory implementations of QueryPort interfaces (consistent with existing test patterns).

### 6.2 API Integration Tests (Controller level)

| Test Class | Endpoints |
|------------|-----------|
| `ReviewCaseQueryApiTest` | `GET /review-cases`, `GET /review-cases/{caseId}` |
| `SubmissionQueryApiTest` | `GET /review-submissions/{submissionId}` |
| `AuditApiTest` | `GET /audit-trails`, `GET /review-cases/{caseId}/lifecycle` |
| `ReportApiTest` | `GET /reports/dashboard-summary`, `POST /reports/export` |

Integration test pattern: `@SpringBootTest` + `@AutoConfigureMockMvc` + H2 (consistent with existing API tests).

### 6.3 Not Tested This Iteration

- Response view 分級
- Fine-grained scope filtering per role
- File export generation

---

## 7. Implementation Order

### Phase B5a — Case + Submission Queries

1. `ReviewCaseQueryPort` + `ReviewCaseQueryCriteria` + `ReviewCaseSummary`
2. `ReviewCaseDetailView` + `SubmissionSummaryItem`
3. `ListReviewCasesUseCase` + unit test
4. `GetReviewCaseDetailUseCase` + unit test
5. `SubmissionQueryPort` + `SubmissionDetailView` + `AnswerDetail` + `AttachmentRef`
6. `GetSubmissionDetailUseCase` + unit test
7. `ReviewCaseQueryAdapter` (infrastructure)
8. `SubmissionQueryAdapter` (infrastructure)
9. Controller endpoints + REST DTOs
10. API integration tests

### Phase B5b — Audit / Lifecycle

11. `AuditTrailQueryPort` + `AuditTrailQueryCriteria` + `AuditTrailEntry`
12. `ListAuditTrailsUseCase` + unit test
13. `GetCaseLifecycleUseCase` + unit test
14. `AuditTrailQueryAdapter` (extend existing JPA repo)
15. `AuditController` + REST DTOs
16. Lifecycle endpoint on ReviewCaseController
17. API integration tests

### Phase B5c — Dashboard + Export

18. `DashboardQueryCriteria` + `DashboardSummary`
19. `DashboardQueryService` + unit test
20. `ExportCriteria` + `ExportResult` + `ExportRow`
21. `ExportQueryService` + unit test
22. Reporting infrastructure (JPQL queries)
23. `ReportController` + REST DTOs
24. API integration tests

### Final

25. Full test suite run + verification

---

## 8. Intentional Deviations from api-spec.md

| # | Deviation | Reason |
|---|-----------|--------|
| 1 | Response view 分級 (metadata vs full detail) 不做 | 本次 scope 限定為 basic role check，後續迭代補 |
| 2 | Scope 過濾（REVIEWER 只看自己、AUDITOR 範圍限制）不做 | 後續迭代補 |
| 3 | Export 不產生檔案（只回 JSON） | 本次 scope 只做數據查詢 |
| 4 | Dashboard 的「複審率」用 `rereviewer_id IS NOT NULL` 判斷 | 最直接的判斷依據，與是否實際完成複審有差異但符合本次精度 |

---

## 9. Files Changed Summary

### New Files (~35)

**application/reviewcase/query/** (6 files):
- `ReviewCaseQueryPort.java`
- `ReviewCaseQueryCriteria.java`
- `ReviewCaseSummary.java`
- `ReviewCaseDetailView.java`
- `SubmissionQueryPort.java`
- `SubmissionDetailView.java`

**application/reviewcase/** (3 use cases):
- `ListReviewCasesUseCase.java`
- `GetReviewCaseDetailUseCase.java`
- `GetSubmissionDetailUseCase.java`

**application/audit/** (5 files):
- `AuditTrailQueryPort.java`
- `AuditTrailQueryCriteria.java`
- `AuditTrailEntry.java`
- `ListAuditTrailsUseCase.java`
- `GetCaseLifecycleUseCase.java`

**reporting/** (6 files):
- `DashboardQueryService.java`
- `DashboardSummary.java`
- `DashboardQueryCriteria.java`
- `ExportQueryService.java`
- `ExportResult.java`
- `ExportCriteria.java`

**infrastructure/** (3 adapters):
- `ReviewCaseQueryAdapter.java`
- `SubmissionQueryAdapter.java`
- `AuditTrailQueryAdapter.java`

**interfaces/rest/** (3 new controllers):
- `SubmissionController.java`
- `AuditController.java`
- `ReportController.java`

**interfaces/rest/dto/** (~6 DTOs):
- `ReviewCaseListResponse.java`
- `ReviewCaseDetailResponse.java`
- `SubmissionDetailResponse.java`
- `AuditTrailResponse.java`
- `DashboardSummaryResponse.java`
- `ExportRequest.java`

**test/** (~11 test files):
- 7 unit tests + 4 API integration tests

### Modified Files

- `ReviewCaseController.java` — add 3 GET endpoints (list, detail, lifecycle)

---

## 10. Risk & Mitigation

| Risk | Mitigation |
|------|------------|
| ReviewCase JPA entity 欄位可能不完整（缺某些 query 需要的欄位） | 實作 adapter 時檢查，必要時補欄位 |
| Submission answers 存為 JSON，查詢映射可能需要特殊處理 | 使用 JPA converter 或 native query |
| Dashboard JPQL 聚合性能 | 本階段數據量小可接受，後續可加 materialized view |
| 現有 controller 新增 GET 可能路由衝突 | 確認 `@RequestMapping` path 不衝突 |
