# Frontend-Blocking Gap Resolution Design

> Date: 2026-04-12
> Scope: GAP-01, GAP-04, GAP-05, GAP-06, GAP-13, GAP-15
> Goal: Eliminate design–implementation gaps that would harm frontend development

---

## 1. Problem Statement

The `design-implementation-gap.md` report identified 20 gaps between design documents and backend implementation. Six of these gaps directly threaten frontend development because they affect:

- API endpoint paths (frontend doesn't know which URL to call)
- Request/response payload shapes (frontend doesn't know what to send/receive)
- Error code semantics (frontend error handling would be incorrect)
- Security scope guards (frontend could receive data it shouldn't)
- Missing domain fields (frontend queries would lack necessary data)
- Visibility masking (frontend wouldn't know which fields to hide)

This design addresses these six gaps in priority order.

---

## 2. Scope

| Gap ID | Title | Category |
|--------|-------|----------|
| GAP-04 | API Path / Method Divergence | decision required → update code + spec |
| GAP-06 | Error Code / HTTP Status Divergence | unacceptable → update code + spec |
| GAP-05 | Request / Response Payload Divergence | unacceptable → update code |
| GAP-01 | Hospital Scope Guard Missing | security → update code |
| GAP-13 | Missing `latestRereviewSubmissionId` | domain field → update code |
| GAP-15 | D-018 Rereview Answer Masking | decision compliance → update code |

**Out of scope**: GAP-02 (domain events), GAP-07/08 (persistence model), GAP-09 (app service docs), GAP-10–12 (pending confirmation), GAP-14 (spec consistency), GAP-16–20 (lower priority).

---

## 3. Processing Order

1. GAP-04 → decides the API path model (everything else depends on this)
2. GAP-06 → error codes are independent, fix early
3. GAP-05 → payload shapes depend on GAP-04 path decisions
4. GAP-01 → security guards, independent
5. GAP-13 → domain field addition, independent
6. GAP-15 → query-layer masking, independent

---

## 4. GAP-04: API Path Unification

### 4.1 Decision

**D-022**: The review workflow API adopts a **case-rooted sub-resource** path model. All command endpoints use `/review-cases/{caseId}/{stage}/action` style. Submit/revise endpoints no longer use submission as the resource root. All HTTP command methods are `POST`.

This decision should be recorded in `docs/01-requirements/decision-log.md` as D-022.

**Rationale**:
- Current backend implementation (controllers + 78 tests + API tests) is already case-rooted
- No frontend code exists yet that depends on either path model (confirmed by strengthening report)
- Changing code to match spec (Option A) would cause massive test churn with no consumer benefit
- The existing implementation paths have internal style inconsistencies that need unification regardless

### 4.2 Path Mapping

All paths are relative to `/review-cases/{caseId}`.

| Operation | Current Implementation Path | Unified Path | Change |
|-----------|---------------------------|--------------|--------|
| Start Initial Review | `POST .../start-initial-review` | `POST .../initial-review/start` | rename |
| Save Initial Draft | `POST .../initial-review/save-draft` | `POST .../initial-review/draft` | rename |
| Submit Initial Review | `POST .../initial-review/submit` | `POST .../initial-review/submit` | no change |
| Revise Initial Review | `POST .../submissions/{sid}/revise` | `POST .../initial-review/submissions/{sid}/revise` | restructure |
| Return Initial Review | `POST .../return` | `POST .../initial-review/return` | add stage prefix |
| Save Rereview Draft | `PUT .../rereview-draft` | `POST .../rereview/draft` | change method + path |
| Submit Rereview | `POST .../submit-rereview` | `POST .../rereview/submit` | rename |
| Revise Rereview | `POST .../revise-rereview` | `POST .../rereview/submissions/{sid}/revise` | restructure |

### 4.3 Old Path Handling

Old paths are **deleted** (not deprecated or redirected). No known consumer exists.

### 4.4 Unchanged Endpoints

- `SubmissionController` (`GET /submissions/{submissionId}`) — query endpoint, not in GAP-04 scope
- All project, score-sheet, and sampling endpoints — not affected

---

## 5. GAP-06: Error Code Alignment

### 5.1 Exception Rename

| Before | After |
|--------|-------|
| `UnauthorizedException` class | `RoleDeniedException` class |
| Error code `UNAUTHORIZED` | Error code `ROLE_DENIED` |
| HTTP 403 | HTTP 403 (unchanged) |

`AccessDeniedException` (hospital scope mismatch) remains unchanged:
- Error code: `ACCESS_DENIED`
- HTTP: 403

### 5.2 GlobalExceptionHandler Changes

```java
// Before
@ExceptionHandler(UnauthorizedException.class)
→ ApiResponse.error("UNAUTHORIZED", ...)

// After
@ExceptionHandler(RoleDeniedException.class)
→ ApiResponse.error("ROLE_DENIED", ...)
```

### 5.3 VALIDATION_ERROR

`VALIDATION_ERROR` (returned by `MethodArgumentNotValidException` handler for HTTP 400) is added to the api-spec error code table during spec sync. No code change needed.

### 5.4 Impact

- ~30+ test files reference `UnauthorizedException` → mechanical find-and-replace to `RoleDeniedException`
- All API tests asserting error code `UNAUTHORIZED` → assert `ROLE_DENIED`
- `ActorContext.requireRole()` / `requireAnyRole()` → throw `RoleDeniedException`

### 5.5 Final Error Code Table

| Error Code | HTTP Status | Trigger |
|------------|-------------|---------|
| `ROLE_DENIED` | 403 | Actor lacks required role |
| `ACCESS_DENIED` | 403 | Hospital scope mismatch |
| `VALIDATION_ERROR` | 400 | Bean validation failure |
| `BAD_REQUEST` | 400 | Other bad request |
| `NOT_FOUND` | 404 | Resource not found |
| `STATE_CONFLICT` | 409 | Domain state conflict |
| `DOMAIN_RULE_VIOLATION` | 422 | Domain rule violated |
| `INTERNAL_ERROR` | 500 | Unexpected error |

401 Unauthorized is returned directly by `ApiKeyAuthFilter` (no api-key or invalid api-key), not through exception handler.

---

## 6. GAP-05: Request/Response Payload Alignment

### 6.1 Draft Request — Add `attachments`

Add `attachments` field (`List<AttachmentRef>`) to:
- `SaveInitialReviewDraftRequest`
- `SaveRereviewDraftRequest`
- `ReviseRereviewRequest` (and initial review revise request)

`AttachmentRef` is a simple DTO: `{ fileId, fileName, fileType }`.

Backend use cases accept the field but **do not persist it yet** (attachment persistence is out of scope — GAP-02/GAP-07). The field flows through to the use case as an empty list placeholder.

### 6.2 Save Draft Response — Structured Object

Replace minimal `{ totalScore }` response with:

```json
{
  "submissionId": "...",
  "status": "DRAFT",
  "stage": "INITIAL_REVIEW" | "REREVIEW",
  "totalScore": 85.5
}
```

### 6.3 Submit Response — Structured Object

Replace `null` response with:

**Initial Review Submit Response**:
```json
{
  "submissionId": "...",
  "status": "SUBMITTED",
  "caseId": "...",
  "caseStatus": "INITIAL_REVIEW_COMPLETED",
  "totalScore": 85.5,
  "rereviewRequired": true
}
```

**Rereview Submit Response**:
```json
{
  "submissionId": "...",
  "status": "SUBMITTED",
  "caseId": "...",
  "caseStatus": "REREVIEW_COMPLETED",
  "finalScore": 88.0
}
```

### 6.4 Revise Response — Add Missing Fields

Add `oldSubmissionId`, `stage`, `caseId` to `RevisionResultResponse`:

```json
{
  "newSubmissionId": "...",
  "oldSubmissionId": "...",
  "revisionNo": 2,
  "status": "DRAFT",
  "stage": "INITIAL_REVIEW" | "REREVIEW",
  "caseId": "...",
  "totalScore": 85.5
}
```

### 6.5 Use Case Return Type Changes

Current use cases that return `void` or minimal data need to return richer result objects. This means:
- `SubmitInitialReviewUseCase` → return `SubmitInitialReviewResult`
- `SubmitRereviewUseCase` → return `SubmitRereviewResult`
- `SaveInitialReviewDraftUseCase` → return `SaveDraftResult` (may already exist partially)
- `SaveRereviewDraftUseCase` → return `SaveDraftResult`
- Revise use cases → return `RevisionResult` (may already exist partially)

---

## 7. GAP-01: Hospital Scope Guard

### 7.1 Use Cases to Fix

| Use Case | Actor Role | Guard |
|----------|-----------|-------|
| `ExecuteSamplingUseCase` | HOSPITAL_MANAGER | `actor.requireHospitalScope(project.getHospitalId())` |
| `AssignRereviewerUseCase` | HOSPITAL_MANAGER | `actor.requireHospitalScope(reviewCase.getHospitalId())` |
| `SendToRereviewUseCase` | HOSPITAL_MANAGER | `actor.requireHospitalScope(reviewCase.getHospitalId())` |
| `CloseWithoutRereviewUseCase` | HOSPITAL_MANAGER | `actor.requireHospitalScope(reviewCase.getHospitalId())` |
| `StartInitialReviewUseCase` | REVIEWER | `actor.requireHospitalScope(reviewCase.getHospitalId())` |
| `SaveInitialReviewDraftUseCase` | REVIEWER | `actor.requireHospitalScope(reviewCase.getHospitalId())` |
| `SubmitInitialReviewUseCase` | REVIEWER | `actor.requireHospitalScope(reviewCase.getHospitalId())` |

### 7.2 Implementation

Add `actor.requireHospitalScope(...)` call at the start of each use case's `execute()` method, after role check and before any business logic. Throws `AccessDeniedException` on mismatch.

### 7.3 Tests

Add a negative test case to each affected use case test: actor with correct role but wrong hospital → expect `AccessDeniedException`.

---

## 8. GAP-13: Add `latestRereviewSubmissionId`

### 8.1 Domain Change

Add `latestRereviewSubmissionId` field to `ReviewCase` entity (nullable String).

### 8.2 Maintenance Points

The field is set when:
- A rereview submission is created (start rereview)
- A rereview revision is created (revise rereview)

### 8.3 Persistence

Add corresponding column to `ReviewCaseEntity` JPA mapping.

### 8.4 Query Impact

The field should be included in `ReviewCaseDetailView` and relevant query responses.

---

## 9. GAP-15: D-018 Rereview Answer Masking

### 9.1 Rule

D-018: A rereview reviewer must not see the initial review's original answers, comments, and scores while conducting rereview.

### 9.2 Trigger Condition

Masking is active when **all** of the following are true:
- Actor's role is rereview reviewer for this case
- The rereview has **not yet been submitted** (no rereview submission with status SUBMITTED exists)

Once the rereview is submitted, masking is lifted.

### 9.3 Masked Fields

When masking is active, the following fields are set to `null` in the response:
- Initial review answers
- Initial review comments
- Initial review score(s)

### 9.4 Response Behavior

- Use `@JsonInclude(JsonInclude.Include.NON_NULL)` on the response DTO to omit null fields
- Add a `maskedFields` list (e.g., `["initialAnswers", "initialComments", "initialScore"]`) to the response so the frontend can distinguish "masked" from "absent"

### 9.5 Implementation Location

`GetSubmissionDetailUseCase` (or equivalent query use case in application layer). The masking logic checks `ActorContext` role and rereview submission status, then nullifies the relevant fields before returning.

---

## 10. Cross-Cutting Concerns

### 10.1 Commit Strategy

One commit per gap, plus a final spec sync commit:

1. `fix(gap-04): unify API paths to case-rooted sub-resource style`
2. `fix(gap-06): rename UnauthorizedException to RoleDeniedException with ROLE_DENIED`
3. `fix(gap-05): align request/response payloads with API spec`
4. `fix(gap-01): add requireHospitalScope to 7 use cases`
5. `fix(gap-13): add latestRereviewSubmissionId to ReviewCase`
6. `fix(gap-15): implement D-018 rereview answer masking`
7. `docs: sync decision-log, api-spec, spec-by-example, test-scenarios`

### 10.2 Testing Strategy

- **Update existing tests**: path URLs, error codes, response assertions
- **Add contract tests**: each modified endpoint gets at least one happy-path + error-path test verifying response shape

### 10.3 Spec Sync (Final Commit)

Documents to update:

| Document | Changes |
|----------|---------|
| `docs/01-requirements/decision-log.md` | Add D-022 (API path model decision) |
| `docs/03-contracts/api-spec.md` | Update paths, error code table, add `VALIDATION_ERROR` |
| `docs/04-testing/specification-by-example.md` | Update endpoint path references |
| `docs/04-testing/test-scenarios.md` | Update endpoint path references |
| `docs/02-ddd/application-service.md` | Sync D-021 exception (GAP-16 piggyback) |

### 10.4 Code-First Approach

All code changes and tests are completed first. Spec sync happens as the final step after all code changes pass.

---

## 11. Risk Assessment

| Risk | Mitigation |
|------|-----------|
| GAP-04 path changes break existing API tests | Mechanical find-and-replace; tests are updated in the same commit |
| GAP-06 rename touches 30+ test files | Automated refactoring; all are mechanical name changes |
| GAP-05 use case return types change | New result DTOs are additive; existing domain logic unchanged |
| GAP-01 scope guard breaks valid test scenarios | Tests that use correct hospital continue to pass; only cross-hospital tests fail (as expected) |
| GAP-15 masking introduces false negatives | `maskedFields` flag prevents frontend from misinterpreting null as "no data" |

---

## 12. What This Design Does NOT Cover

- Attachment persistence (GAP-02, GAP-07) — DTOs accept `attachments` but do not persist
- Persistence model alignment (GAP-07, GAP-08) — out of scope
- Missing domain events (GAP-02) — out of scope
- ScoreSheet freeze mechanism (GAP-11) — out of scope
- EX-003 vs D-012 spec consistency (GAP-14) — out of scope
- Context map style (GAP-18) / glossary spelling (GAP-19) — out of scope
