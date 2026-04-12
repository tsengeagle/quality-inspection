# Frontend-Blocking Gap Resolution Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Eliminate 6 design–implementation gaps (GAP-04, GAP-06, GAP-05, GAP-01, GAP-13, GAP-15) that block frontend development of the quality inspection system.

**Architecture:** Each gap is resolved in one atomic commit. API paths are unified to case-rooted sub-resource style (D-022). Exception naming aligns error codes with spec. Request/response payloads match api-spec.md. Hospital scope guards are added for defense-in-depth. A new domain field and query masking are added for rereview workflow. A final commit syncs all affected documentation.

**Tech Stack:** Java 11, Spring Boot 2.7.18, Gradle 7.6.4 (Groovy DSL), JUnit 5, Spring MockMvc, H2 (tests)

**Design Spec:** `docs/superpowers/specs/2026-04-12-frontend-blocking-gap-resolution-design.md`

**Build & Test Command:**
```bash
cd services/quality-inspection-service && .\gradlew clean test
```

**Path Aliases (used throughout this plan):**
- `SRC` = `services/quality-inspection-service/src/main/java/com/tzuchi/his/quality/inspection`
- `TEST` = `services/quality-inspection-service/src/test/java/com/tzuchi/his/quality/inspection`
- `PKG` = `com.tzuchi.his.quality.inspection`

---

## File Structure

### GAP-04: API Path Unification
- Modify: `SRC/interfaces/rest/ReviewCaseController.java` — 8 endpoint annotations
- Modify: `SRC/interfaces/rest/dto/ReviseRereviewRequest.java` — remove `submissionId` (now path variable)
- Modify: `TEST/interfaces/rest/InitialReviewApiTest.java` — update 9 test URLs
- Modify: `TEST/interfaces/rest/RevisionApiTest.java` — update 4 test URLs
- Modify: `TEST/interfaces/rest/RereviewApiTest.java` — update URLs + HTTP methods
- Modify: `TEST/interfaces/rest/ReviewCaseApiTest.java` — update return test URL

### GAP-06: Exception Rename
- Create: `SRC/application/common/RoleDeniedException.java`
- Delete: `SRC/application/common/UnauthorizedException.java`
- Modify: `SRC/interfaces/rest/GlobalExceptionHandler.java` — handler method
- Modify: `SRC/application/common/ActorContext.java` — throw references
- Modify: ~30 files referencing `UnauthorizedException` → `RoleDeniedException`

### GAP-05: Payload Alignment
- Create: `SRC/interfaces/rest/dto/AttachmentRef.java`
- Create: `SRC/application/reviewcase/SaveDraftResult.java`
- Create: `SRC/application/reviewcase/SubmitInitialReviewResult.java`
- Create: `SRC/application/reviewcase/SubmitRereviewResult.java`
- Create: `SRC/interfaces/rest/dto/SubmitInitialReviewResponse.java`
- Create: `SRC/interfaces/rest/dto/SubmitRereviewResponse.java`
- Modify: `SRC/interfaces/rest/dto/SaveDraftResponse.java` — add fields
- Modify: `SRC/interfaces/rest/dto/RevisionResultResponse.java` — add fields
- Modify: `SRC/application/reviewcase/RevisionResult.java` — add fields
- Modify: `SRC/interfaces/rest/dto/SaveInitialReviewDraftRequest.java` — add attachments
- Modify: `SRC/interfaces/rest/dto/ReviseInitialReviewRequest.java` — add attachments
- Modify: `SRC/interfaces/rest/dto/SaveRereviewDraftRequest.java` — add attachments
- Modify: `SRC/interfaces/rest/dto/ReviseRereviewRequest.java` — add attachments
- Modify: `SRC/application/reviewcase/SaveInitialReviewDraftUseCase.java` — return SaveDraftResult
- Modify: `SRC/application/reviewcase/SaveRereviewDraftUseCase.java` — return SaveDraftResult
- Modify: `SRC/application/reviewcase/SubmitInitialReviewUseCase.java` — return SubmitInitialReviewResult
- Modify: `SRC/application/reviewcase/SubmitRereviewUseCase.java` — return SubmitRereviewResult
- Modify: `SRC/application/reviewcase/ReviseRereviewUseCase.java` — return RevisionResult
- Modify: `SRC/interfaces/rest/ReviewCaseController.java` — map results to responses

### GAP-01: Hospital Scope Guard
- Modify: 7 use case files — add `actor.requireHospitalScope(...)`
- Modify: `SRC/application/reviewcase/SaveInitialReviewDraftUseCase.java` — add ReviewCaseRepository dependency
- Modify: corresponding test files — add scope guard negative tests

### GAP-13: latestRereviewSubmissionId
- Modify: `SRC/domain/reviewcase/ReviewCase.java` — new field + getter + reconstitute param
- Modify: `SRC/infrastructure/persistence/reviewcase/ReviewCaseEntity.java` — new column
- Modify: `SRC/infrastructure/persistence/reviewcase/ReviewCaseRepositoryImpl.java` — mapping
- Modify: `SRC/application/reviewcase/query/ReviewCaseDetailView.java` — new field
- Modify: use cases that create rereview submissions — set the field

### GAP-15: D-018 Rereview Answer Masking
- Modify: `SRC/application/reviewcase/GetSubmissionDetailUseCase.java` — add masking logic
- Modify: `SRC/application/reviewcase/query/SubmissionDetailView.java` — add `maskedFields`, make mutable
- Create: `TEST/application/reviewcase/GetSubmissionDetailUseCaseMaskingTest.java`

### Docs Sync (Final Commit)
- Modify: `docs/01-requirements/decision-log.md` — add D-022
- Modify: `docs/03-contracts/api-spec.md` — paths, error codes
- Modify: `docs/04-testing/specification-by-example.md` — path references
- Modify: `docs/04-testing/test-scenarios.md` — path references
- Modify: `docs/02-ddd/application-service.md` — exception naming

---

## Task 1: GAP-04 — Update Controller Endpoint Paths

**Files:**
- Modify: `SRC/interfaces/rest/ReviewCaseController.java`

- [ ] **Step 1: Update initial review endpoints (lines 191–221)**

Change these 4 annotations in `ReviewCaseController.java`:

```java
// Line 191: start-initial-review → initial-review/start
@PostMapping("/{caseId}/initial-review/start")

// Line 201: initial-review/save-draft → initial-review/draft
@PostMapping("/{caseId}/initial-review/draft")

// Line 215: initial-review/submit → NO CHANGE (already correct)

// Line 172: return → initial-review/return
@PostMapping("/{caseId}/initial-review/return")
```

- [ ] **Step 2: Update initial revise endpoint (line 225)**

Add `initial-review/` prefix to the revise path:

```java
// Line 225: submissions/{submissionId}/revise → initial-review/submissions/{submissionId}/revise
@PostMapping("/{caseId}/initial-review/submissions/{submissionId}/revise")
```

Method signature stays the same (already has `@PathVariable String submissionId`).

- [ ] **Step 3: Update rereview endpoints (lines 316–372)**

```java
// Line 316: PUT rereview-draft → POST rereview/draft
@PostMapping("/{caseId}/rereview/draft")
public ResponseEntity<ApiResponse<Void>> saveRereviewDraft(

// Line 336: submit-rereview → rereview/submit
@PostMapping("/{caseId}/rereview/submit")

// Line 355: revise-rereview → rereview/submissions/{submissionId}/revise
@PostMapping("/{caseId}/rereview/submissions/{submissionId}/revise")
public ResponseEntity<ApiResponse<Void>> reviseRereview(
        @CurrentActor ActorContext actor,
        @PathVariable String caseId,
        @PathVariable String submissionId,
        @RequestBody @Valid ReviseRereviewRequest request) {
    ReviseRereviewCommand cmd = new ReviseRereviewCommand();
    cmd.setCaseId(caseId);
    cmd.setSubmissionId(submissionId);  // from path, not request body
    if (request.getAnswers() != null) {
        cmd.setAnswers(request.getAnswers().stream()
                .filter(Objects::nonNull)
                .map(a -> AnswerItem.ofRaw(a.getItemId(), a.getAnswerValue()))
                .collect(Collectors.toList()));
    }
    cmd.setComment(request.getComment());
    reviseRereviewUseCase.execute(actor, cmd);
    return ResponseEntity.ok(ApiResponse.ok(null));
}
```

---

## Task 2: GAP-04 — Update ReviseRereviewRequest DTO

**Files:**
- Modify: `SRC/interfaces/rest/dto/ReviseRereviewRequest.java`

- [ ] **Step 1: Remove submissionId from request body**

`submissionId` now comes from the path variable. Remove the field from the DTO:

```java
package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import java.util.List;

public class ReviseRereviewRequest {

    private List<SaveInitialReviewDraftRequest.AnswerInputDto> answers;
    private String comment;

    public List<SaveInitialReviewDraftRequest.AnswerInputDto> getAnswers() { return answers; }
    public void setAnswers(List<SaveInitialReviewDraftRequest.AnswerInputDto> answers) { this.answers = answers; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
```

---

## Task 3: GAP-04 — Update API Test URLs

**Files:**
- Modify: `TEST/interfaces/rest/InitialReviewApiTest.java`
- Modify: `TEST/interfaces/rest/RevisionApiTest.java`
- Modify: `TEST/interfaces/rest/RereviewApiTest.java`
- Modify: `TEST/interfaces/rest/ReviewCaseApiTest.java`

- [ ] **Step 1: Update InitialReviewApiTest.java**

Find and replace these URL patterns:

| Old URL fragment | New URL fragment |
|-----------------|-----------------|
| `/start-initial-review` | `/initial-review/start` |
| `/initial-review/save-draft` | `/initial-review/draft` |

Tests affected: `startInitialReview_*` (4 tests), `saveDraft_*` (4 tests).

Example change for line 197:
```java
// Before
mockMvc.perform(post(BASE + "/c1/start-initial-review")
// After
mockMvc.perform(post(BASE + "/c1/initial-review/start")
```

Example change for line 241:
```java
// Before
mockMvc.perform(post(BASE + "/c1/initial-review/save-draft")
// After
mockMvc.perform(post(BASE + "/c1/initial-review/draft")
```

- [ ] **Step 2: Update RevisionApiTest.java**

Find and replace:

| Old URL fragment | New URL fragment |
|-----------------|-----------------|
| `/submissions/sub-1/revise` | `/initial-review/submissions/sub-1/revise` |

Tests affected: `revise_*` (3 tests), `fullRoundTrip_*` (1 test).

Example change for line 158:
```java
// Before
mockMvc.perform(post(BASE + "/c1/submissions/sub-1/revise")
// After
mockMvc.perform(post(BASE + "/c1/initial-review/submissions/sub-1/revise")
```

- [ ] **Step 3: Update RereviewApiTest.java**

Find and replace:

| Old | New | Note |
|-----|-----|------|
| `put(BASE + "/c1/rereview-draft")` | `post(BASE + "/c1/rereview/draft")` | Method PUT→POST |
| `post(BASE + "/c1/submit-rereview")` | `post(BASE + "/c1/rereview/submit")` | |
| `post(BASE + "/c1/revise-rereview")` | `post(BASE + "/c1/rereview/submissions/" + submissionId + "/revise")` | Add submissionId to path |

For the revise-rereview test, the `submissionId` variable needs to be extracted from the preceding submit response or from test setup. Look for the existing `submissionId` variable used in the revise body — move it to the URL instead.

Also remove `submissionId` from the JSON body of revise-rereview requests.

- [ ] **Step 4: Update ReviewCaseApiTest.java**

Find and replace:

| Old URL fragment | New URL fragment |
|-----------------|-----------------|
| `"/c1/return"` | `"/c1/initial-review/return"` |

Tests affected: `returnCase_*` (2 tests).

---

## Task 4: GAP-04 — Run Tests and Commit

- [ ] **Step 1: Run all tests**

```bash
cd services/quality-inspection-service && .\gradlew clean test
```

Expected: All tests pass.

- [ ] **Step 2: Commit**

```bash
cd services/quality-inspection-service
git add -A
git commit -m "fix(gap-04): unify API paths to case-rooted sub-resource style

D-022: Adopt case-rooted sub-resource path model.

Path changes:
- start-initial-review → initial-review/start
- initial-review/save-draft → initial-review/draft
- return → initial-review/return
- submissions/{sid}/revise → initial-review/submissions/{sid}/revise
- PUT rereview-draft → POST rereview/draft
- submit-rereview → rereview/submit
- revise-rereview → rereview/submissions/{sid}/revise

Old paths deleted (no known consumers).

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 5: GAP-06 — Create RoleDeniedException and Delete UnauthorizedException

**Files:**
- Create: `SRC/application/common/RoleDeniedException.java`
- Delete: `SRC/application/common/UnauthorizedException.java`

- [ ] **Step 1: Create RoleDeniedException**

```java
package com.tzuchi.his.quality.inspection.application.common;

/**
 * Thrown when the actor lacks the required role(s) for an operation.
 * Maps to HTTP 403 with error code ROLE_DENIED.
 */
public class RoleDeniedException extends RuntimeException {

    private final String errorCode;

    public RoleDeniedException(String message) {
        super(message);
        this.errorCode = "ROLE_DENIED";
    }

    public String getErrorCode() {
        return errorCode;
    }
}
```

- [ ] **Step 2: Delete UnauthorizedException**

```bash
cd services/quality-inspection-service
git rm src/main/java/com/tzuchi/his/quality/inspection/application/common/UnauthorizedException.java
```

---

## Task 6: GAP-06 — Update GlobalExceptionHandler and ActorContext

**Files:**
- Modify: `SRC/interfaces/rest/GlobalExceptionHandler.java`
- Modify: `SRC/application/common/ActorContext.java`

- [ ] **Step 1: Update GlobalExceptionHandler (lines 36–40)**

Replace the `UnauthorizedException` handler:

```java
// Before (lines 36-40):
@ExceptionHandler(UnauthorizedException.class)
public ResponseEntity<ApiResponse<Void>> handle(UnauthorizedException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error("UNAUTHORIZED", ex.getMessage()));
}

// After:
@ExceptionHandler(RoleDeniedException.class)
public ResponseEntity<ApiResponse<Void>> handle(RoleDeniedException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage()));
}
```

Also update the import: `UnauthorizedException` → `RoleDeniedException`.

- [ ] **Step 2: Update ActorContext (lines 29–34, 40–46)**

Replace `UnauthorizedException` with `RoleDeniedException`:

```java
// Line 31 (in requireRole):
throw new RoleDeniedException(
        "Actor does not have required role: " + required);

// Line 44 (in requireAnyRole):
throw new RoleDeniedException(
        "Actor does not have any of the required roles");
```

Update the import: `UnauthorizedException` → `RoleDeniedException`.

---

## Task 7: GAP-06 — Rename All References Across Codebase

**Files:**
- All files importing/referencing `UnauthorizedException`

- [ ] **Step 1: Find all references**

```bash
cd services/quality-inspection-service
grep -r "UnauthorizedException" --include="*.java" -l src/
```

Expected: ~30+ files.

- [ ] **Step 2: Mechanical rename in all source and test files**

For each file found, replace:
- `import ...UnauthorizedException;` → `import ...RoleDeniedException;`
- `UnauthorizedException.class` → `RoleDeniedException.class`
- `UnauthorizedException` (in type references) → `RoleDeniedException`
- `new UnauthorizedException(` → `new RoleDeniedException(`

Also update any API test assertions checking error code:
- `"UNAUTHORIZED"` → `"ROLE_DENIED"` (only where it appears as error code in test assertions for role-denied scenarios, NOT in test method names)

Search for these assertions:
```bash
grep -rn '"UNAUTHORIZED"' --include="*.java" src/test/
```

Example: `SamplingControllerApiTest.java` line 113:
```java
// Before
.andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
// After
.andExpect(jsonPath("$.error.code").value("ROLE_DENIED"));
```

---

## Task 8: GAP-06 — Run Tests and Commit

- [ ] **Step 1: Verify no remaining references**

```bash
cd services/quality-inspection-service
grep -r "UnauthorizedException" --include="*.java" src/
```

Expected: No results.

- [ ] **Step 2: Run all tests**

```bash
cd services/quality-inspection-service && .\gradlew clean test
```

Expected: All tests pass.

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "fix(gap-06): rename UnauthorizedException to RoleDeniedException

- Error code changes: UNAUTHORIZED → ROLE_DENIED
- RoleDeniedException has errorCode field (like AccessDeniedException)
- GlobalExceptionHandler uses ex.getErrorCode() instead of hardcoded string
- All test assertions updated to expect ROLE_DENIED

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 9: GAP-05 — Create Application-Layer Result DTOs

**Files:**
- Create: `SRC/application/reviewcase/SaveDraftResult.java`
- Create: `SRC/application/reviewcase/SubmitInitialReviewResult.java`
- Create: `SRC/application/reviewcase/SubmitRereviewResult.java`
- Modify: `SRC/application/reviewcase/RevisionResult.java`

- [ ] **Step 1: Create SaveDraftResult**

```java
package com.tzuchi.his.quality.inspection.application.reviewcase;

import java.math.BigDecimal;

/**
 * Result of saving a review draft — returned to the caller.
 */
public class SaveDraftResult {

    private final String submissionId;
    private final String status;
    private final String stage;
    private final BigDecimal totalScore;

    public SaveDraftResult(String submissionId, String status, String stage, BigDecimal totalScore) {
        this.submissionId = submissionId;
        this.status = status;
        this.stage = stage;
        this.totalScore = totalScore;
    }

    public String getSubmissionId() { return submissionId; }
    public String getStatus() { return status; }
    public String getStage() { return stage; }
    public BigDecimal getTotalScore() { return totalScore; }
}
```

- [ ] **Step 2: Create SubmitInitialReviewResult**

```java
package com.tzuchi.his.quality.inspection.application.reviewcase;

import java.math.BigDecimal;

/**
 * Result of submitting an initial review.
 */
public class SubmitInitialReviewResult {

    private final String submissionId;
    private final String status;
    private final String caseId;
    private final String caseStatus;
    private final BigDecimal totalScore;
    private final boolean rereviewRequired;

    public SubmitInitialReviewResult(String submissionId, String status,
                                      String caseId, String caseStatus,
                                      BigDecimal totalScore, boolean rereviewRequired) {
        this.submissionId = submissionId;
        this.status = status;
        this.caseId = caseId;
        this.caseStatus = caseStatus;
        this.totalScore = totalScore;
        this.rereviewRequired = rereviewRequired;
    }

    public String getSubmissionId() { return submissionId; }
    public String getStatus() { return status; }
    public String getCaseId() { return caseId; }
    public String getCaseStatus() { return caseStatus; }
    public BigDecimal getTotalScore() { return totalScore; }
    public boolean isRereviewRequired() { return rereviewRequired; }
}
```

- [ ] **Step 3: Create SubmitRereviewResult**

```java
package com.tzuchi.his.quality.inspection.application.reviewcase;

import java.math.BigDecimal;

/**
 * Result of submitting a rereview.
 */
public class SubmitRereviewResult {

    private final String submissionId;
    private final String status;
    private final String caseId;
    private final String caseStatus;
    private final BigDecimal finalScore;

    public SubmitRereviewResult(String submissionId, String status,
                                 String caseId, String caseStatus,
                                 BigDecimal finalScore) {
        this.submissionId = submissionId;
        this.status = status;
        this.caseId = caseId;
        this.caseStatus = caseStatus;
        this.finalScore = finalScore;
    }

    public String getSubmissionId() { return submissionId; }
    public String getStatus() { return status; }
    public String getCaseId() { return caseId; }
    public String getCaseStatus() { return caseStatus; }
    public BigDecimal getFinalScore() { return finalScore; }
}
```

- [ ] **Step 4: Add missing fields to RevisionResult**

Add `oldSubmissionId`, `stage`, `caseId` to existing `RevisionResult.java`:

```java
package com.tzuchi.his.quality.inspection.application.reviewcase;

import java.math.BigDecimal;

/**
 * Result of a revision operation — returned to the caller with new submission details.
 */
public class RevisionResult {

    private final String newSubmissionId;
    private final String oldSubmissionId;
    private final int revisionNo;
    private final BigDecimal totalScore;
    private final String status;
    private final String stage;
    private final String caseId;

    public RevisionResult(String newSubmissionId, String oldSubmissionId,
                          int revisionNo, BigDecimal totalScore,
                          String status, String stage, String caseId) {
        this.newSubmissionId = newSubmissionId;
        this.oldSubmissionId = oldSubmissionId;
        this.revisionNo = revisionNo;
        this.totalScore = totalScore;
        this.status = status;
        this.stage = stage;
        this.caseId = caseId;
    }

    public String getNewSubmissionId() { return newSubmissionId; }
    public String getOldSubmissionId() { return oldSubmissionId; }
    public int getRevisionNo() { return revisionNo; }
    public BigDecimal getTotalScore() { return totalScore; }
    public String getStatus() { return status; }
    public String getStage() { return stage; }
    public String getCaseId() { return caseId; }
}
```

---

## Task 10: GAP-05 — Create/Update Interface-Layer Response DTOs

**Files:**
- Modify: `SRC/interfaces/rest/dto/SaveDraftResponse.java`
- Create: `SRC/interfaces/rest/dto/SubmitInitialReviewResponse.java` (replace existing minimal one)
- Create: `SRC/interfaces/rest/dto/SubmitRereviewResponse.java`
- Modify: `SRC/interfaces/rest/dto/RevisionResultResponse.java`

- [ ] **Step 1: Expand SaveDraftResponse**

Replace current `SaveDraftResponse` (which only has `totalScore`):

```java
package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import java.math.BigDecimal;

public class SaveDraftResponse {

    private final String submissionId;
    private final String status;
    private final String stage;
    private final BigDecimal totalScore;

    public SaveDraftResponse(String submissionId, String status,
                              String stage, BigDecimal totalScore) {
        this.submissionId = submissionId;
        this.status = status;
        this.stage = stage;
        this.totalScore = totalScore;
    }

    public String getSubmissionId() { return submissionId; }
    public String getStatus() { return status; }
    public String getStage() { return stage; }
    public BigDecimal getTotalScore() { return totalScore; }
}
```

- [ ] **Step 2: Create SubmitInitialReviewResponse**

The file `StartInitialReviewResponse.java` already exists (for start endpoint). Create a **new** file for submit:

```java
package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import java.math.BigDecimal;

public class SubmitInitialReviewResponse {

    private final String submissionId;
    private final String status;
    private final String caseId;
    private final String caseStatus;
    private final BigDecimal totalScore;
    private final boolean rereviewRequired;

    public SubmitInitialReviewResponse(String submissionId, String status,
                                        String caseId, String caseStatus,
                                        BigDecimal totalScore, boolean rereviewRequired) {
        this.submissionId = submissionId;
        this.status = status;
        this.caseId = caseId;
        this.caseStatus = caseStatus;
        this.totalScore = totalScore;
        this.rereviewRequired = rereviewRequired;
    }

    public String getSubmissionId() { return submissionId; }
    public String getStatus() { return status; }
    public String getCaseId() { return caseId; }
    public String getCaseStatus() { return caseStatus; }
    public BigDecimal getTotalScore() { return totalScore; }
    public boolean isRereviewRequired() { return rereviewRequired; }
}
```

- [ ] **Step 3: Create SubmitRereviewResponse**

```java
package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import java.math.BigDecimal;

public class SubmitRereviewResponse {

    private final String submissionId;
    private final String status;
    private final String caseId;
    private final String caseStatus;
    private final BigDecimal finalScore;

    public SubmitRereviewResponse(String submissionId, String status,
                                    String caseId, String caseStatus,
                                    BigDecimal finalScore) {
        this.submissionId = submissionId;
        this.status = status;
        this.caseId = caseId;
        this.caseStatus = caseStatus;
        this.finalScore = finalScore;
    }

    public String getSubmissionId() { return submissionId; }
    public String getStatus() { return status; }
    public String getCaseId() { return caseId; }
    public String getCaseStatus() { return caseStatus; }
    public BigDecimal getFinalScore() { return finalScore; }
}
```

- [ ] **Step 4: Expand RevisionResultResponse**

Add `oldSubmissionId`, `stage`, `caseId`:

```java
package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import java.math.BigDecimal;

public class RevisionResultResponse {

    private final String newSubmissionId;
    private final String oldSubmissionId;
    private final int revisionNo;
    private final BigDecimal totalScore;
    private final String status;
    private final String stage;
    private final String caseId;

    public RevisionResultResponse(String newSubmissionId, String oldSubmissionId,
                                   int revisionNo, BigDecimal totalScore,
                                   String status, String stage, String caseId) {
        this.newSubmissionId = newSubmissionId;
        this.oldSubmissionId = oldSubmissionId;
        this.revisionNo = revisionNo;
        this.totalScore = totalScore;
        this.status = status;
        this.stage = stage;
        this.caseId = caseId;
    }

    public String getNewSubmissionId() { return newSubmissionId; }
    public String getOldSubmissionId() { return oldSubmissionId; }
    public int getRevisionNo() { return revisionNo; }
    public BigDecimal getTotalScore() { return totalScore; }
    public String getStatus() { return status; }
    public String getStage() { return stage; }
    public String getCaseId() { return caseId; }
}
```

---

## Task 11: GAP-05 — Add AttachmentRef and Update Request DTOs

**Files:**
- Create: `SRC/interfaces/rest/dto/AttachmentRef.java`
- Modify: `SRC/interfaces/rest/dto/SaveInitialReviewDraftRequest.java`
- Modify: `SRC/interfaces/rest/dto/ReviseInitialReviewRequest.java`
- Modify: `SRC/interfaces/rest/dto/SaveRereviewDraftRequest.java`
- Modify: `SRC/interfaces/rest/dto/ReviseRereviewRequest.java`

- [ ] **Step 1: Create AttachmentRef DTO**

```java
package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

public class AttachmentRef {

    private String fileId;
    private String fileName;
    private String fileType;

    public AttachmentRef() {}

    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
}
```

- [ ] **Step 2: Add attachments to all 4 request DTOs**

Add to each request DTO (after existing fields):

```java
private List<AttachmentRef> attachments;

public List<AttachmentRef> getAttachments() { return attachments; }
public void setAttachments(List<AttachmentRef> attachments) { this.attachments = attachments; }
```

Files to modify:
1. `SaveInitialReviewDraftRequest.java` — add after `comment` field
2. `ReviseInitialReviewRequest.java` — add after `comment` field
3. `SaveRereviewDraftRequest.java` — add after `comment` field
4. `ReviseRereviewRequest.java` — add after `comment` field (which was already modified in Task 2)

---

## Task 12: GAP-05 — Update Use Cases to Return Rich Results

**Files:**
- Modify: `SRC/application/reviewcase/SaveInitialReviewDraftUseCase.java`
- Modify: `SRC/application/reviewcase/SaveRereviewDraftUseCase.java`
- Modify: `SRC/application/reviewcase/SubmitInitialReviewUseCase.java`
- Modify: `SRC/application/reviewcase/SubmitRereviewUseCase.java`
- Modify: `SRC/application/reviewcase/ReviseInitialReviewUseCase.java`
- Modify: `SRC/application/reviewcase/ReviseRereviewUseCase.java`

- [ ] **Step 1: Update SaveInitialReviewDraftUseCase return type**

Change `public BigDecimal execute(...)` → `public SaveDraftResult execute(...)`.

Replace the two return statements:

```java
// In the empty-answers branch (around line 71):
return new SaveDraftResult(
        submission.getSubmissionId(),
        submission.getStatus().name(),
        submission.getStage().name(),
        BigDecimal.ZERO);

// In the normal branch (around line 98):
return new SaveDraftResult(
        submission.getSubmissionId(),
        submission.getStatus().name(),
        submission.getStage().name(),
        result.getTotalScore());
```

- [ ] **Step 2: Update SaveRereviewDraftUseCase return type**

Change `public void execute(...)` → `public SaveDraftResult execute(...)`.

Add return at the end of the method (after `submissionRepository.save(submission)`):

```java
return new SaveDraftResult(
        submission.getSubmissionId(),
        submission.getStatus().name(),
        submission.getStage().name(),
        submission.getTotalScore());
```

- [ ] **Step 3: Update SubmitInitialReviewUseCase return type**

Change `public void execute(...)` → `public SubmitInitialReviewResult execute(...)`.

Add return at the end of the method (after event publishing, around line 148):

```java
return new SubmitInitialReviewResult(
        submission.getSubmissionId(),
        submission.getStatus().name(),
        caseId,
        reviewCase.getStatus().name(),
        result.getTotalScore(),
        reviewCase.isRereviewRequired());
```

- [ ] **Step 4: Update SubmitRereviewUseCase return type**

Change `public void execute(...)` → `public SubmitRereviewResult execute(...)`.

Add return at the end of the method (after event publishing, around line 106):

```java
return new SubmitRereviewResult(
        submission.getSubmissionId(),
        submission.getStatus().name(),
        cmd.getCaseId(),
        reviewCase.getStatus().name(),
        reviewCase.getFinalScore());
```

- [ ] **Step 5: Update ReviseInitialReviewUseCase return value**

The existing return (around line 115) builds a `RevisionResult` with 4 args. Update to the new 7-arg constructor:

```java
return new RevisionResult(
        newSubmissionId,
        submissionId,               // oldSubmissionId (method parameter)
        newSubmission.getRevisionNo(),
        result.getTotalScore(),
        newSubmission.getStatus().name(),
        newSubmission.getStage().name(),
        caseId);                    // caseId (method parameter)
```

- [ ] **Step 6: Update ReviseRereviewUseCase return type**

Change `public void execute(...)` → `public RevisionResult execute(...)`.

Add return at the end of the method (after event publishing, around line 128):

```java
return new RevisionResult(
        newSubmissionId,
        cmd.getSubmissionId(),      // oldSubmissionId
        revision.getRevisionNo(),
        result.getTotalScore(),
        revision.getStatus().name(),
        revision.getStage().name(),
        cmd.getCaseId());
```

---

## Task 13: GAP-05 — Update Controller to Map Results to Responses

**Files:**
- Modify: `SRC/interfaces/rest/ReviewCaseController.java`

- [ ] **Step 1: Update saveInitialReviewDraft method**

```java
@PostMapping("/{caseId}/initial-review/draft")
public ResponseEntity<ApiResponse<SaveDraftResponse>> saveInitialReviewDraft(
        @CurrentActor ActorContext actor,
        @PathVariable String caseId,
        @Valid @RequestBody SaveInitialReviewDraftRequest request) {
    List<AnswerInput> answers = request.getAnswers().stream()
            .filter(Objects::nonNull)
            .map(dto -> new AnswerInput(dto.getItemId(), dto.getAnswerValue()))
            .collect(Collectors.toList());
    SaveDraftResult result = saveInitialReviewDraftUseCase.execute(
            actor, caseId, answers, request.getComment());
    return ResponseEntity.ok(ApiResponse.ok(new SaveDraftResponse(
            result.getSubmissionId(), result.getStatus(),
            result.getStage(), result.getTotalScore())));
}
```

- [ ] **Step 2: Update submitInitialReview method**

```java
@PostMapping("/{caseId}/initial-review/submit")
public ResponseEntity<ApiResponse<SubmitInitialReviewResponse>> submitInitialReview(
        @CurrentActor ActorContext actor,
        @PathVariable String caseId) {
    SubmitInitialReviewResult result = submitInitialReviewUseCase.execute(actor, caseId);
    return ResponseEntity.ok(ApiResponse.ok(new SubmitInitialReviewResponse(
            result.getSubmissionId(), result.getStatus(),
            result.getCaseId(), result.getCaseStatus(),
            result.getTotalScore(), result.isRereviewRequired())));
}
```

- [ ] **Step 3: Update reviseInitialReview method**

Update the response construction (around line 238):

```java
return ResponseEntity.ok(ApiResponse.ok(new RevisionResultResponse(
        result.getNewSubmissionId(), result.getOldSubmissionId(),
        result.getRevisionNo(), result.getTotalScore(),
        result.getStatus(), result.getStage(), result.getCaseId())));
```

- [ ] **Step 4: Update saveRereviewDraft method**

```java
@PostMapping("/{caseId}/rereview/draft")
public ResponseEntity<ApiResponse<SaveDraftResponse>> saveRereviewDraft(
        @CurrentActor ActorContext actor,
        @PathVariable String caseId,
        @RequestBody @Valid SaveRereviewDraftRequest request) {
    SaveRereviewDraftCommand cmd = new SaveRereviewDraftCommand();
    cmd.setCaseId(caseId);
    cmd.setSubmissionId(request.getSubmissionId());
    if (request.getAnswers() != null) {
        cmd.setAnswers(request.getAnswers().stream()
                .filter(Objects::nonNull)
                .map(a -> AnswerItem.ofRaw(a.getItemId(), a.getAnswerValue()))
                .collect(Collectors.toList()));
    }
    cmd.setTotalScore(request.getTotalScore());
    cmd.setComment(request.getComment());
    SaveDraftResult result = saveRereviewDraftUseCase.execute(actor, cmd);
    return ResponseEntity.ok(ApiResponse.ok(new SaveDraftResponse(
            result.getSubmissionId(), result.getStatus(),
            result.getStage(), result.getTotalScore())));
}
```

- [ ] **Step 5: Update submitRereview method**

```java
@PostMapping("/{caseId}/rereview/submit")
public ResponseEntity<ApiResponse<SubmitRereviewResponse>> submitRereview(
        @CurrentActor ActorContext actor,
        @PathVariable String caseId,
        @RequestBody @Valid SubmitRereviewRequest request) {
    SubmitRereviewCommand cmd = new SubmitRereviewCommand();
    cmd.setCaseId(caseId);
    cmd.setSubmissionId(request.getSubmissionId());
    if (request.getAnswers() != null) {
        cmd.setAnswers(request.getAnswers().stream()
                .filter(Objects::nonNull)
                .map(a -> AnswerItem.ofRaw(a.getItemId(), a.getAnswerValue()))
                .collect(Collectors.toList()));
    }
    cmd.setComment(request.getComment());
    SubmitRereviewResult result = submitRereviewUseCase.execute(actor, cmd);
    return ResponseEntity.ok(ApiResponse.ok(new SubmitRereviewResponse(
            result.getSubmissionId(), result.getStatus(),
            result.getCaseId(), result.getCaseStatus(),
            result.getFinalScore())));
}
```

- [ ] **Step 6: Update reviseRereview method**

```java
@PostMapping("/{caseId}/rereview/submissions/{submissionId}/revise")
public ResponseEntity<ApiResponse<RevisionResultResponse>> reviseRereview(
        @CurrentActor ActorContext actor,
        @PathVariable String caseId,
        @PathVariable String submissionId,
        @RequestBody @Valid ReviseRereviewRequest request) {
    ReviseRereviewCommand cmd = new ReviseRereviewCommand();
    cmd.setCaseId(caseId);
    cmd.setSubmissionId(submissionId);
    if (request.getAnswers() != null) {
        cmd.setAnswers(request.getAnswers().stream()
                .filter(Objects::nonNull)
                .map(a -> AnswerItem.ofRaw(a.getItemId(), a.getAnswerValue()))
                .collect(Collectors.toList()));
    }
    cmd.setComment(request.getComment());
    RevisionResult result = reviseRereviewUseCase.execute(actor, cmd);
    return ResponseEntity.ok(ApiResponse.ok(new RevisionResultResponse(
            result.getNewSubmissionId(), result.getOldSubmissionId(),
            result.getRevisionNo(), result.getTotalScore(),
            result.getStatus(), result.getStage(), result.getCaseId())));
}
```

---

## Task 14: GAP-05 — Update Tests for New Payloads

**Files:**
- Modify: `TEST/interfaces/rest/InitialReviewApiTest.java`
- Modify: `TEST/interfaces/rest/RevisionApiTest.java`
- Modify: `TEST/interfaces/rest/RereviewApiTest.java`
- Modify: `TEST/application/reviewcase/SaveInitialReviewDraftUseCaseTest.java`
- Modify: `TEST/application/reviewcase/SubmitInitialReviewUseCaseTest.java`
- Modify: other use case tests referencing changed return types

- [ ] **Step 1: Update InitialReviewApiTest — save draft assertions**

The `saveDraft_shouldReturn200WithTotalScore` test (line 241) currently asserts `$.data.totalScore`. Update to also check new fields:

```java
.andExpect(jsonPath("$.data.submissionId").isString())
.andExpect(jsonPath("$.data.status").value("DRAFT"))
.andExpect(jsonPath("$.data.stage").value("INITIAL"))
.andExpect(jsonPath("$.data.totalScore").value(17.5));
```

- [ ] **Step 2: Update InitialReviewApiTest — submit assertions**

The `submit_shouldReturn200` test (line 292) currently asserts only `$.success`. Update:

```java
.andExpect(jsonPath("$.data.submissionId").isString())
.andExpect(jsonPath("$.data.status").value("SUBMITTED"))
.andExpect(jsonPath("$.data.caseId").value(CASE_ID))
.andExpect(jsonPath("$.data.caseStatus").isString())
.andExpect(jsonPath("$.data.totalScore").isNumber());
```

- [ ] **Step 3: Update RevisionApiTest — revise assertions**

The `revise_returns200_withRevisionResult` test already checks `$.data.newSubmissionId`, `$.data.revisionNo`, `$.data.status`, `$.data.totalScore`. Add new fields:

```java
.andExpect(jsonPath("$.data.oldSubmissionId").isString())
.andExpect(jsonPath("$.data.stage").value("INITIAL"))
.andExpect(jsonPath("$.data.caseId").value(CASE_ID));
```

- [ ] **Step 4: Update RereviewApiTest — save draft and submit assertions**

Update the `fullRereviewFlow` test to assert richer responses for save-draft and submit:

For save draft response:
```java
.andExpect(jsonPath("$.data.submissionId").isString())
.andExpect(jsonPath("$.data.status").value("DRAFT"))
.andExpect(jsonPath("$.data.stage").value("REREVIEW"));
```

For submit response:
```java
.andExpect(jsonPath("$.data.submissionId").isString())
.andExpect(jsonPath("$.data.status").value("SUBMITTED"))
.andExpect(jsonPath("$.data.caseId").value(CASE_ID))
.andExpect(jsonPath("$.data.caseStatus").isString())
.andExpect(jsonPath("$.data.finalScore").isNumber());
```

For revise response:
```java
.andExpect(jsonPath("$.data.newSubmissionId").isString())
.andExpect(jsonPath("$.data.oldSubmissionId").isString())
.andExpect(jsonPath("$.data.stage").value("REREVIEW"))
.andExpect(jsonPath("$.data.caseId").value(CASE_ID));
```

- [ ] **Step 5: Update unit tests for changed return types**

For `SaveInitialReviewDraftUseCaseTest`: change assertions from `BigDecimal` to `SaveDraftResult`:

```java
// Before
BigDecimal totalScore = useCase.execute(actor, caseId, answers, comment);
assertEquals(new BigDecimal("17.50"), totalScore);

// After
SaveDraftResult result = useCase.execute(actor, caseId, answers, comment);
assertEquals(new BigDecimal("17.50"), result.getTotalScore());
assertNotNull(result.getSubmissionId());
assertEquals("DRAFT", result.getStatus());
assertEquals("INITIAL", result.getStage());
```

For `SubmitInitialReviewUseCaseTest`: change from void to `SubmitInitialReviewResult`:

```java
// Before
useCase.execute(actor, caseId);

// After
SubmitInitialReviewResult result = useCase.execute(actor, caseId);
assertNotNull(result.getSubmissionId());
assertEquals("SUBMITTED", result.getStatus());
```

Apply similar patterns to rereview use case tests.

---

## Task 15: GAP-05 — Run Tests and Commit

- [ ] **Step 1: Run all tests**

```bash
cd services/quality-inspection-service && .\gradlew clean test
```

Expected: All tests pass.

- [ ] **Step 2: Commit**

```bash
git add -A
git commit -m "fix(gap-05): align request/response payloads with API spec

- Save draft response: add submissionId, status, stage
- Submit initial response: add submissionId, status, caseId, caseStatus, totalScore, rereviewRequired
- Submit rereview response: add submissionId, status, caseId, caseStatus, finalScore
- Revise response: add oldSubmissionId, stage, caseId
- Add attachments field to 4 request DTOs (accepted but not persisted)
- Create AttachmentRef DTO
- Use cases return rich result objects instead of void/BigDecimal

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 16: GAP-01 — Add Hospital Scope Guard to 7 Use Cases

**Files:**
- Modify: `SRC/application/sampling/ExecuteSamplingUseCase.java`
- Modify: `SRC/application/reviewcase/AssignRereviewerUseCase.java`
- Modify: `SRC/application/reviewcase/SendToRereviewUseCase.java`
- Modify: `SRC/application/reviewcase/CloseWithoutRereviewUseCase.java`
- Modify: `SRC/application/reviewcase/StartInitialReviewUseCase.java`
- Modify: `SRC/application/reviewcase/SaveInitialReviewDraftUseCase.java`
- Modify: `SRC/application/reviewcase/SubmitInitialReviewUseCase.java`

- [ ] **Step 1: Add scope guard to ExecuteSamplingUseCase**

After `actor.requireRole(DomainRole.HOSPITAL_MANAGER)` (line 48), and after loading the project (line ~51), add:

```java
actor.requireHospitalScope(project.getHospitalId());
```

The project is loaded at line ~51 as `ReviewProject project`. Insert after the project is loaded.

- [ ] **Step 2: Add scope guard to 3 HOSPITAL_MANAGER use cases**

For `AssignRereviewerUseCase`, `SendToRereviewUseCase`, `CloseWithoutRereviewUseCase`:

After `actor.requireRole(DomainRole.HOSPITAL_MANAGER)` and after loading `ReviewCase reviewCase`, add:

```java
actor.requireHospitalScope(reviewCase.getHospitalId());
```

- [ ] **Step 3: Add scope guard to StartInitialReviewUseCase**

After `actor.requireRole(DomainRole.REVIEWER)` (line 52) and after loading `ReviewCase reviewCase` (line 54), add:

```java
actor.requireHospitalScope(reviewCase.getHospitalId());
```

Insert between the role check and the identity guard (before line 60).

- [ ] **Step 4: Add scope guard to SaveInitialReviewDraftUseCase**

This use case does NOT currently load `ReviewCase`. It only loads `ReviewSubmission`. Add `ReviewCaseRepository` as a dependency.

Add field and constructor parameter:

```java
private final ReviewCaseRepository caseRepository;
// ... add to constructor
```

After `actor.requireRole(DomainRole.REVIEWER)` (line 49), add:

```java
ReviewCase reviewCase = caseRepository.findById(caseId)
        .orElseThrow(() -> new ResourceNotFoundException("CASE_NOT_FOUND",
                "ReviewCase not found: " + caseId));
actor.requireHospitalScope(reviewCase.getHospitalId());
```

Add imports:
```java
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
```

- [ ] **Step 5: Add scope guard to SubmitInitialReviewUseCase**

ReviewCase is already loaded at line 73. Add scope guard after loading it:

```java
// After line 75 (reviewCase loaded)
actor.requireHospitalScope(reviewCase.getHospitalId());
```

---

## Task 17: GAP-01 — Add Scope Guard Unit Tests

**Files:**
- Modify: use case test files for all 7 use cases

- [ ] **Step 1: Add negative test to StartInitialReviewUseCaseTest**

```java
@Test
void execute_wrongHospital_shouldThrowAccessDenied() {
    seedAssignedCase();
    ActorContext wrongHospital = new ActorContext(REVIEWER_ID, "Test Reviewer",
            EnumSet.of(DomainRole.REVIEWER), Optional.of("OTHER_HOSPITAL"));
    assertThrows(AccessDeniedException.class, () -> useCase.execute(wrongHospital, CASE_ID));
}
```

- [ ] **Step 2: Add negative tests to remaining 6 use cases**

Follow the same pattern for each use case test:
1. Seed a case/project with `HOSPITAL_ID = "H001"`
2. Create an actor with `Optional.of("OTHER_HOSPITAL")`
3. Assert `AccessDeniedException` is thrown

For `SaveInitialReviewDraftUseCaseTest`, also update the constructor call to include the new `ReviewCaseRepository` dependency (use `InMemoryReviewCaseRepository`).

---

## Task 18: GAP-01 — Run Tests and Commit

- [ ] **Step 1: Run all tests**

```bash
cd services/quality-inspection-service && .\gradlew clean test
```

Expected: All tests pass.

- [ ] **Step 2: Commit**

```bash
git add -A
git commit -m "fix(gap-01): add requireHospitalScope to 7 use cases

Defense-in-depth hospital scope guard added to:
- ExecuteSamplingUseCase
- AssignRereviewerUseCase
- SendToRereviewUseCase
- CloseWithoutRereviewUseCase
- StartInitialReviewUseCase
- SaveInitialReviewDraftUseCase (new ReviewCaseRepository dependency)
- SubmitInitialReviewUseCase

Each use case now throws AccessDeniedException when the actor's
hospital does not match the target resource's hospital.

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 19: GAP-13 — Add latestRereviewSubmissionId to Domain and Persistence

**Files:**
- Modify: `SRC/domain/reviewcase/ReviewCase.java`
- Modify: `SRC/infrastructure/persistence/reviewcase/ReviewCaseEntity.java`
- Modify: `SRC/infrastructure/persistence/reviewcase/ReviewCaseRepositoryImpl.java`
- Modify: `SRC/application/reviewcase/query/ReviewCaseDetailView.java`

- [ ] **Step 1: Add field to ReviewCase domain entity**

Add field after `initialReviewScore` (around line 47):

```java
private String latestRereviewSubmissionId;
```

Add getter:

```java
public String getLatestRereviewSubmissionId() { return latestRereviewSubmissionId; }
```

Add parameter to `reconstitute()` method signature and body:

```java
public static ReviewCase reconstitute(
        // ... existing params ...,
        BigDecimal initialReviewScore,
        String latestRereviewSubmissionId) {   // NEW
    // ... existing body ...
    rc.initialReviewScore = initialReviewScore;
    rc.latestRereviewSubmissionId = latestRereviewSubmissionId;  // NEW
    return rc;
}
```

Add setter method for use by domain logic:

```java
public void setLatestRereviewSubmissionId(String submissionId) {
    this.latestRereviewSubmissionId = submissionId;
}
```

- [ ] **Step 2: Add column to ReviewCaseEntity**

After `initialReviewScore` field (line 90), add:

```java
@Column(name = "latest_rereview_submission_id")
private String latestRereviewSubmissionId;
```

Add getter/setter:

```java
public String getLatestRereviewSubmissionId() { return latestRereviewSubmissionId; }
public void setLatestRereviewSubmissionId(String id) { this.latestRereviewSubmissionId = id; }
```

- [ ] **Step 3: Update ReviewCaseRepositoryImpl mapping**

In `toDomain()` method, add the new parameter to the `reconstitute()` call:

```java
return ReviewCase.reconstitute(
        // ... existing params ...,
        e.getInitialReviewScore(),
        e.getLatestRereviewSubmissionId());  // NEW
```

In `toEntity()` method, add:

```java
e.setLatestRereviewSubmissionId(rc.getLatestRereviewSubmissionId());
```

- [ ] **Step 4: Update ReviewCaseDetailView**

Add field:

```java
private final String latestRereviewSubmissionId;
```

Add to constructor parameters and assignment. Add getter:

```java
public String getLatestRereviewSubmissionId() { return latestRereviewSubmissionId; }
```

Update all callers of this constructor (likely in the query infrastructure layer).

---

## Task 20: GAP-13 — Set latestRereviewSubmissionId in Use Cases

**Files:**
- Modify: `SRC/application/reviewcase/StartRereviewUseCase.java`
- Modify: `SRC/application/reviewcase/ReviseRereviewUseCase.java`

- [ ] **Step 1: Set in StartRereviewUseCase**

After the rereview draft submission is created and saved, set the field on ReviewCase:

```java
reviewCase.setLatestRereviewSubmissionId(submission.getSubmissionId());
caseRepository.save(reviewCase);
```

- [ ] **Step 2: Set in ReviseRereviewUseCase**

After the new revision is created (around line 91-104), update the case:

```java
reviewCase.setLatestRereviewSubmissionId(newSubmissionId);
// This save is already done later in the method (line 118: caseRepository.save(reviewCase))
```

---

## Task 21: GAP-13 — Run Tests and Commit

- [ ] **Step 1: Run all tests**

```bash
cd services/quality-inspection-service && .\gradlew clean test
```

Expected: All tests pass. The new field is nullable, so existing tests won't break.

- [ ] **Step 2: Commit**

```bash
git add -A
git commit -m "fix(gap-13): add latestRereviewSubmissionId to ReviewCase

- New nullable field on ReviewCase aggregate
- Set when rereview is started and when rereview is revised
- Persisted via ReviewCaseEntity column
- Included in ReviewCaseDetailView for query

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 22: GAP-15 — Implement D-018 Rereview Answer Masking

**Files:**
- Modify: `SRC/application/reviewcase/GetSubmissionDetailUseCase.java`
- Modify: `SRC/application/reviewcase/query/SubmissionDetailView.java`
- Create: `TEST/application/reviewcase/GetSubmissionDetailUseCaseMaskingTest.java`

- [ ] **Step 1: Add maskedFields and mutability to SubmissionDetailView**

Add a `maskedFields` list and make initially-masked fields nullable. Add `@JsonInclude(NON_NULL)` annotation:

```java
package com.tzuchi.his.quality.inspection.application.reviewcase.query;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubmissionDetailView {

    private final String submissionId;
    private final String caseId;
    private final String stage;
    private final int revisionNo;
    private final String status;
    private final String reviewerId;
    private List<AnswerDetail> answers;         // mutable for masking
    private String comment;                      // mutable for masking
    private final String attachmentRefsJson;
    private BigDecimal totalScore;               // mutable for masking
    private final LocalDateTime createdAt;
    private final LocalDateTime submittedAt;
    private List<String> maskedFields;           // NEW

    // Constructor stays the same (all params)
    // ... existing constructor ...

    // Add masking method
    public void applyMask(List<String> fieldNames) {
        this.maskedFields = fieldNames;
        for (String field : fieldNames) {
            switch (field) {
                case "answers": this.answers = null; break;
                case "comment": this.comment = null; break;
                case "totalScore": this.totalScore = null; break;
            }
        }
    }

    // ... existing getters ...
    public List<String> getMaskedFields() { return maskedFields; }
}
```

Change `answers`, `comment`, `totalScore` from `final` to non-final to support masking.

- [ ] **Step 2: Add masking logic to GetSubmissionDetailUseCase**

Add dependencies for checking rereview status:

```java
@Service
@Transactional(readOnly = true)
public class GetSubmissionDetailUseCase {

    private final SubmissionQueryPort queryPort;
    private final ReviewCaseRepository caseRepository;
    private final ReviewSubmissionRepository submissionRepository;

    public GetSubmissionDetailUseCase(SubmissionQueryPort queryPort,
                                       ReviewCaseRepository caseRepository,
                                       ReviewSubmissionRepository submissionRepository) {
        this.queryPort = queryPort;
        this.caseRepository = caseRepository;
        this.submissionRepository = submissionRepository;
    }

    public SubmissionDetailView execute(ActorContext actor, String submissionId) {
        actor.requireAnyRole(
                DomainRole.HOSPITAL_MANAGER, DomainRole.REVIEWER,
                DomainRole.REREVIEWER, DomainRole.AUDITOR, DomainRole.SYSTEM_ADMIN);

        SubmissionDetailView view = queryPort.findDetailById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("ReviewSubmission", submissionId));

        // D-018: Mask initial review data for rereview reviewer
        // who has not yet submitted their rereview
        if (shouldMaskInitialReview(actor, view)) {
            view.applyMask(java.util.Arrays.asList("answers", "comment", "totalScore"));
        }

        return view;
    }

    private boolean shouldMaskInitialReview(ActorContext actor, SubmissionDetailView view) {
        // Only mask INITIAL stage submissions
        if (!"INITIAL".equals(view.getStage())) {
            return false;
        }
        // Only for REREVIEWER role
        if (!actor.hasRole(DomainRole.REREVIEWER)) {
            return false;
        }
        // Check if actor is the assigned rereviewer for this case
        ReviewCase reviewCase = caseRepository.findById(view.getCaseId()).orElse(null);
        if (reviewCase == null || reviewCase.getRereviewerAssignment() == null) {
            return false;
        }
        if (!actor.getUserId().equals(reviewCase.getRereviewerAssignment().getReviewerId())) {
            return false;
        }
        // Check if rereview has already been submitted
        boolean hasSubmittedRereview = submissionRepository
                .findLatestByCaseIdAndStage(view.getCaseId(),
                        com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewStage.REREVIEW)
                .map(s -> s.getStatus() == com.tzuchi.his.quality.inspection.domain.reviewcase.submission.SubmissionStatus.SUBMITTED
                        || s.getStatus() == com.tzuchi.his.quality.inspection.domain.reviewcase.submission.SubmissionStatus.SUPERSEDED)
                .orElse(false);
        // Mask when rereview NOT yet submitted
        return !hasSubmittedRereview;
    }
}
```

- [ ] **Step 3: Write masking unit test**

```java
package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.SubmissionDetailView;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.SubmissionQueryPort;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewerAssignment;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewStage;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.SubmissionStatus;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryReviewCaseRepository;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryReviewSubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GetSubmissionDetailUseCaseMaskingTest {

    private InMemoryReviewCaseRepository caseRepo;
    private InMemoryReviewSubmissionRepository submissionRepo;
    private GetSubmissionDetailUseCase useCase;

    private static final String CASE_ID = "c1";
    private static final String REREVIEWER_ID = "rereviewer-1";
    private static final String HOSPITAL_ID = "H001";

    @BeforeEach
    void setUp() {
        caseRepo = new InMemoryReviewCaseRepository();
        submissionRepo = new InMemoryReviewSubmissionRepository();

        // Stub query port that returns a fixed SubmissionDetailView
        SubmissionQueryPort queryPort = submissionId -> Optional.of(
                new SubmissionDetailView(
                        submissionId, CASE_ID, "INITIAL", 1, "SUBMITTED",
                        "reviewer-1", Collections.emptyList(), "some comment",
                        null, new BigDecimal("85.0"),
                        LocalDateTime.now(), LocalDateTime.now()));

        useCase = new GetSubmissionDetailUseCase(queryPort, caseRepo, submissionRepo);
    }

    @Test
    void rereviewerBeforeSubmit_shouldMaskInitialData() {
        seedCaseWithRereviewer();
        // No rereview submission exists → masking active
        ActorContext rereviewer = new ActorContext(REREVIEWER_ID, "RR",
                EnumSet.of(DomainRole.REREVIEWER), Optional.of(HOSPITAL_ID));

        SubmissionDetailView result = useCase.execute(rereviewer, "sub-1");

        assertNull(result.getAnswers());
        assertNull(result.getComment());
        assertNull(result.getTotalScore());
        assertNotNull(result.getMaskedFields());
        assertTrue(result.getMaskedFields().contains("answers"));
    }

    @Test
    void rereviewerAfterSubmit_shouldNotMask() {
        seedCaseWithRereviewer();
        // Add a submitted rereview submission
        ReviewSubmission rereviewSub = ReviewSubmission.createRereviewDraft(
                "rr-sub-1", CASE_ID, REREVIEWER_ID, "ss-1");
        rereviewSub.submit();
        submissionRepo.save(rereviewSub);

        ActorContext rereviewer = new ActorContext(REREVIEWER_ID, "RR",
                EnumSet.of(DomainRole.REREVIEWER), Optional.of(HOSPITAL_ID));

        SubmissionDetailView result = useCase.execute(rereviewer, "sub-1");

        assertNotNull(result.getAnswers());
        assertNotNull(result.getTotalScore());
        assertNull(result.getMaskedFields());
    }

    @Test
    void nonRereviewer_shouldNotMask() {
        seedCaseWithRereviewer();
        ActorContext reviewer = new ActorContext("reviewer-1", "Rev",
                EnumSet.of(DomainRole.REVIEWER), Optional.of(HOSPITAL_ID));

        SubmissionDetailView result = useCase.execute(reviewer, "sub-1");

        assertNotNull(result.getAnswers());
        assertNotNull(result.getTotalScore());
        assertNull(result.getMaskedFields());
    }

    private void seedCaseWithRereviewer() {
        ReviewCase rc = ReviewCase.reconstitute(
                CASE_ID, "b1", "p1", HOSPITAL_ID, "src-1", null,
                CaseStatus.REREVIEW_IN_PROGRESS,
                new ReviewerAssignment("reviewer-1", LocalDateTime.now()),
                null, null, null, null, null,
                true, null, null, null, null, null,
                new ReviewerAssignment(REREVIEWER_ID, LocalDateTime.now()),
                null, null, null);
        caseRepo.save(rc);
    }
}
```

Note: the `reconstitute()` call includes the new `latestRereviewSubmissionId` param (last `null`) from Task 19.

- [ ] **Step 4: Run the masking test**

```bash
cd services/quality-inspection-service && .\gradlew test --tests "*GetSubmissionDetailUseCaseMaskingTest"
```

Expected: All 3 tests pass.

---

## Task 23: GAP-15 — Run All Tests and Commit

- [ ] **Step 1: Run full test suite**

```bash
cd services/quality-inspection-service && .\gradlew clean test
```

Expected: All tests pass.

- [ ] **Step 2: Commit**

```bash
git add -A
git commit -m "fix(gap-15): implement D-018 rereview answer masking

GetSubmissionDetailUseCase now masks initial review answers, comments,
and scores when the actor is the assigned rereviewer and has not yet
submitted their rereview.

Masking uses @JsonInclude(NON_NULL) + maskedFields list so the
frontend can distinguish masked from absent.

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 24: Docs Sync — Update Design Documents

**Files:**
- Modify: `docs/01-requirements/decision-log.md`
- Modify: `docs/03-contracts/api-spec.md`
- Modify: `docs/04-testing/specification-by-example.md`
- Modify: `docs/04-testing/test-scenarios.md`
- Modify: `docs/02-ddd/application-service.md`

- [ ] **Step 1: Add D-022 to decision-log.md**

Append a new decision entry:

```markdown
### D-022 — API Path Model: Case-Rooted Sub-Resource Style

**Date**: 2026-04-12
**Status**: Accepted
**Context**: GAP-04 identified divergence between api-spec.md (submission-rooted) and implementation (case-rooted) for review workflow endpoints. No frontend consumers exist yet.
**Decision**: Adopt case-rooted sub-resource style. All review workflow commands use `/review-cases/{caseId}/{stage}/action`. All command methods use POST. Old paths deleted.
**Rationale**: Implementation already established with 78+ tests. Zero consumer migration cost. Internal consistency achieved.
**Consequences**: api-spec.md must be updated to match. Frontend builds to the unified paths from day one.
```

- [ ] **Step 2: Update api-spec.md paths**

Update the endpoint table in `docs/03-contracts/api-spec.md` to match the unified paths from GAP-04. Add `ROLE_DENIED` and `VALIDATION_ERROR` to the error code table.

- [ ] **Step 3: Update specification-by-example.md and test-scenarios.md**

Search and replace old endpoint path references in both files. Replace:
- `/start-initial-review` → `/initial-review/start`
- `/initial-review/save-draft` → `/initial-review/draft`
- `/submit-rereview` → `/rereview/submit`
- etc.

Also replace `UNAUTHORIZED` error code references with `ROLE_DENIED`.

- [ ] **Step 4: Update application-service.md**

Sync the exception naming: `UnauthorizedException` → `RoleDeniedException` (GAP-16 piggyback).

---

## Task 25: Docs Sync — Commit

- [ ] **Step 1: Commit**

```bash
git add -A
git commit -m "docs: sync decision-log, api-spec, spec-by-example, test-scenarios

- Add D-022 (API path model decision) to decision-log.md
- Update api-spec.md: unified paths, ROLE_DENIED, VALIDATION_ERROR
- Update specification-by-example.md: path references
- Update test-scenarios.md: path references
- Update application-service.md: exception naming (GAP-16 piggyback)

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Verification Checklist

After all commits:

- [ ] `cd services/quality-inspection-service && .\gradlew clean test` — all green
- [ ] `grep -r "UnauthorizedException" --include="*.java" services/` — no results
- [ ] `grep -r "start-initial-review" --include="*.java" services/` — no results (except comments)
- [ ] `grep -r "save-draft" --include="*.java" services/` — no results (except comments)
- [ ] `grep -r "revise-rereview" --include="*.java" services/` — no results (except comments)
- [ ] `git log --oneline -8` — 7 commits in correct order
