# Review Round 2 Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix 1 Critical + 3 High + 5 Medium findings from the second code review of the B1/B2 backend.

**Architecture:** Surgical changes to existing files — no new packages or architectural shifts. Each task targets specific lines with exact replacements. Tests updated to match new exception types.

**Tech Stack:** Java 11, Spring Boot 2.7.18, JUnit 5, H2 (test), Gradle

**Spec:** `docs/superpowers/specs/2026-04-07-review-round2-fixes-design.md`

---

## File Map

| # | File | Action | Finding |
|---|---|---|---|
| 1 | `application/common/DomainRole.java` | Modify | H-1 |
| 2 | `application/common/ActorContext.java` | Modify | H-3 |
| 3 | `domain/project/ReviewProject.java` | Modify | H-2 |
| 4 | `domain/sampling/SamplingBatch.java` | Modify | H-2 |
| 5 | `application/sampling/ExecuteSamplingUseCase.java` | Modify | C-1 |
| 6 | `application/sampling/PreviewSamplingPopulationUseCase.java` | Modify | C-1 |
| 7 | `application/sampling/RetrySamplingBatchUseCase.java` | Modify | C-1 |
| 8 | `interfaces/rest/SamplingController.java` | Modify | C-1 |
| 9 | `interfaces/rest/GlobalExceptionHandler.java` | Modify | M-4, M-5 |
| 10 | `infrastructure/security/ApiKeyAuthFilter.java` | Modify | error codes |
| 11 | `infrastructure/security/TzJwtParser.java` | Modify | H-3 |
| 12 | `test/.../TestTokenFactory.java` | Modify | H-3 |
| 13 | `test/.../ReviewProjectTest.java` | Modify | H-2 |
| 14 | `test/.../SamplingBatchTest.java` | Modify | H-2 |
| 15 | `test/.../SamplingUseCaseTest.java` | Modify | C-1, H-3 |
| 16 | `test/.../ScoreSheetDefinitionTest.java` | Modify | M-1 |
| 17 | `test/.../SamplingControllerApiTest.java` | Modify | M-6 |
| 18 | `test/.../ReviewProjectIntegrationTest.java` | **Create** | M-2 |

All paths are relative to `services/quality-inspection-service/src/main/java/com/tzuchi/his/quality/inspection/` (main) or `.../src/test/java/com/tzuchi/his/quality/inspection/` (test).

---

### Task 1: H-1 — Add REVIEWER and REREVIEWER to DomainRole

**Files:**
- Modify: `application/common/DomainRole.java`

- [ ] **Step 1: Replace enum body**

Replace the entire content of `DomainRole.java`:

```java
package com.tzuchi.his.quality.inspection.application.common;

public enum DomainRole {
    SYSTEM_ADMIN,
    HOSPITAL_MANAGER,
    REVIEWER,
    REREVIEWER,
    AUDITOR
}
```

- [ ] **Step 2: Run tests to confirm no breakage**

Run: `cd services/quality-inspection-service && ./gradlew test`
Expected: BUILD SUCCESSFUL (all 53 tests pass — no code depends on the enum having exactly 3 values)

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "fix(auth): H-1 add REVIEWER and REREVIEWER to DomainRole enum

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 2: H-3 — Complete ActorContext (hospitalId, hasRole, requireAnyRole)

**Files:**
- Modify: `application/common/ActorContext.java`
- Modify: `infrastructure/security/TzJwtParser.java:95`
- Modify: `test/.../TestTokenFactory.java:28`

- [ ] **Step 1: Update ActorContext.java**

Replace the entire content of `ActorContext.java`:

```java
package com.tzuchi.his.quality.inspection.application.common;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Immutable value object representing the authenticated actor making a request.
 * Passed as the first parameter to every use case execute() method.
 */
public class ActorContext {

    private final String userId;
    private final String userName;
    private final Set<DomainRole> roles;
    private final Optional<String> hospitalId;

    public ActorContext(String userId, String userName, Set<DomainRole> roles,
                        Optional<String> hospitalId) {
        this.userId = userId;
        this.userName = userName;
        this.roles = (roles != null && !roles.isEmpty())
                ? EnumSet.copyOf(roles)
                : EnumSet.noneOf(DomainRole.class);
        this.hospitalId = (hospitalId != null) ? hospitalId : Optional.empty();
    }

    public void requireRole(DomainRole required) {
        if (!roles.contains(required)) {
            throw new UnauthorizedException(
                    "Actor '" + userId + "' does not have required role: " + required);
        }
    }

    public boolean hasRole(DomainRole role) {
        return roles.contains(role);
    }

    public void requireAnyRole(DomainRole... required) {
        for (DomainRole r : required) {
            if (roles.contains(r)) return;
        }
        throw new UnauthorizedException(
                "Actor '" + userId + "' does not have any of the required roles");
    }

    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
    public Set<DomainRole> getRoles() { return Collections.unmodifiableSet(roles); }
    public Optional<String> getHospitalId() { return hospitalId; }
}
```

- [ ] **Step 2: Update TzJwtParser.java line 95**

Change line 95 from:
```java
        return new ActorContext(userId, userName, roles);
```
To:
```java
        return new ActorContext(userId, userName, roles, Optional.empty());
```

Add this import at the top of TzJwtParser.java:
```java
import java.util.Optional;
```

- [ ] **Step 3: Update TestTokenFactory.java line 28**

The `buildToken` method constructs `ActorContext` indirectly via JWT — it doesn't call the constructor. But `SamplingUseCaseTest` constructs `ActorContext` directly. No change needed to `TestTokenFactory` itself (it produces JWTs, not ActorContext objects). The `TzJwtParser` change in Step 2 handles the JWT path.

Verify: search for all direct `new ActorContext(` calls:

```bash
cd services/quality-inspection-service && grep -rn "new ActorContext(" src/
```

Expected results:
- `TzJwtParser.java:95` — updated in Step 2
- Possibly `SamplingUseCaseTest.java` — will be updated in Task 5

- [ ] **Step 4: Run tests**

Run: `cd services/quality-inspection-service && ./gradlew test`
Expected: BUILD SUCCESSFUL — the only existing 3-arg caller was `TzJwtParser` (updated in Step 2). No other code currently constructs `ActorContext` directly.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "fix(auth): H-3 add hospitalId, hasRole(), requireAnyRole() to ActorContext

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 3: H-2 — Migrate ReviewProject exceptions to typed exceptions

**Files:**
- Modify: `domain/project/ReviewProject.java:38,80,110,119`
- Modify: `test/.../ReviewProjectTest.java:60,108`

- [ ] **Step 1: Add import to ReviewProject.java**

Add this import (if not already present):
```java
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
```

(The file already imports `DomainStateConflictException`.)

- [ ] **Step 2: Replace line 38 in ReviewProject.java (constructor blank name)**

Change:
```java
            throw new IllegalArgumentException("Project name must not be blank");
```
To:
```java
            throw new DomainRuleViolationException("INVALID_PROJECT_NAME", "Project name must not be blank");
```

- [ ] **Step 3: Replace line 80 in ReviewProject.java (activate without published)**

Change:
```java
            throw new IllegalStateException("Cannot activate project without a published version");
```
To:
```java
            throw new DomainStateConflictException("INVALID_STATE_TRANSITION",
                    "Cannot activate project without a published version");
```

- [ ] **Step 4: Replace line 110 in ReviewProject.java (draft already exists)**

Change:
```java
            throw new IllegalStateException("A draft version already exists");
```
To:
```java
            throw new DomainStateConflictException("INVALID_STATE_TRANSITION",
                    "A draft version already exists");
```

- [ ] **Step 5: Replace line 119 in ReviewProject.java (updateName blank)**

Change:
```java
            throw new IllegalArgumentException("Project name must not be blank");
```
To:
```java
            throw new DomainRuleViolationException("INVALID_PROJECT_NAME", "Project name must not be blank");
```

- [ ] **Step 6: Update ReviewProjectTest.java line 60**

Add this import to ReviewProjectTest.java (if not already present):
```java
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
```

Change:
```java
        assertThrows(IllegalStateException.class, project::activate);
```
To:
```java
        assertThrows(DomainStateConflictException.class, project::activate);
```

- [ ] **Step 7: Update ReviewProjectTest.java line 108**

Change:
```java
        assertThrows(IllegalArgumentException.class,
```
To:
```java
        assertThrows(DomainRuleViolationException.class,
```

Add this import to ReviewProjectTest.java:
```java
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
```

- [ ] **Step 8: Run tests**

Run: `cd services/quality-inspection-service && ./gradlew test`
Expected: BUILD SUCCESSFUL

- [ ] **Step 9: Commit**

```bash
git add -A && git commit -m "fix(domain): H-2 migrate ReviewProject to typed exceptions

INVALID_PROJECT_NAME (422) for blank name
INVALID_STATE_TRANSITION (409) for activate without published / draft exists

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 4: H-2 — Migrate SamplingBatch.requireNonNegative to typed exception

**Files:**
- Modify: `domain/sampling/SamplingBatch.java:171-173`
- Modify: `test/.../SamplingBatchTest.java:151`

- [ ] **Step 1: Add import to SamplingBatch.java**

`DomainRuleViolationException` — check if already imported. If not, add:
```java
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
```

(The file already imports this — confirmed from the code.)

- [ ] **Step 2: Replace requireNonNegative in SamplingBatch.java**

Change lines 170-174:
```java
    private void requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
    }
```
To:
```java
    private void requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new DomainRuleViolationException("INVALID_METRIC_VALUE",
                    name + " must not be negative");
        }
    }
```

- [ ] **Step 3: Update SamplingBatchTest.java line 151**

Change:
```java
        assertThrows(IllegalArgumentException.class,
                () -> batch.recordCandidateCount(-1, 0, 0));
```
To:
```java
        assertThrows(DomainRuleViolationException.class,
                () -> batch.recordCandidateCount(-1, 0, 0));
```

Add this import to SamplingBatchTest.java:
```java
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
```

- [ ] **Step 4: Run tests**

Run: `cd services/quality-inspection-service && ./gradlew test`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "fix(domain): H-2 migrate SamplingBatch.requireNonNegative to typed exception

INVALID_METRIC_VALUE (422) for negative metrics

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 5: C-1 — Wire ActorContext into sampling use cases + controller

**Files:**
- Modify: `application/sampling/ExecuteSamplingUseCase.java:40`
- Modify: `application/sampling/PreviewSamplingPopulationUseCase.java:30`
- Modify: `application/sampling/RetrySamplingBatchUseCase.java:42`
- Modify: `interfaces/rest/SamplingController.java:34,42,51`
- Modify: `test/.../SamplingUseCaseTest.java:49,66,84,105,133,142,158,175`

- [ ] **Step 1: Update ExecuteSamplingUseCase.java**

Add imports:
```java
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
```

Change line 40 from:
```java
    public String execute(String projectId) {
```
To:
```java
    public String execute(ActorContext actor, String projectId) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
```

- [ ] **Step 2: Update PreviewSamplingPopulationUseCase.java**

Add imports:
```java
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
```

Change line 30 from:
```java
    public SamplingPreviewResult execute(String projectId) {
```
To:
```java
    public SamplingPreviewResult execute(ActorContext actor, String projectId) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
```

- [ ] **Step 3: Update RetrySamplingBatchUseCase.java**

Add imports:
```java
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
```

Change line 42 from:
```java
    public String execute(String previousBatchId) {
```
To:
```java
    public String execute(ActorContext actor, String previousBatchId) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
```

- [ ] **Step 4: Update SamplingController.java — pass actor to use cases**

Change line 34 from:
```java
        SamplingPreviewResult result = previewUseCase.execute(projectId);
```
To:
```java
        SamplingPreviewResult result = previewUseCase.execute(actor, projectId);
```

Change line 42 from:
```java
        String batchId = executeUseCase.execute(projectId);
```
To:
```java
        String batchId = executeUseCase.execute(actor, projectId);
```

Change line 51 from:
```java
        String newBatchId = retryUseCase.execute(batchId);
```
To:
```java
        String newBatchId = retryUseCase.execute(actor, batchId);
```

- [ ] **Step 5: Update SamplingUseCaseTest.java — add actor field + update all execute calls**

Add these imports to SamplingUseCaseTest.java:
```java
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import java.util.EnumSet;
import java.util.Optional;
```

Add field after line 26 (after `retryUseCase` declaration):
```java
    private ActorContext hospitalManagerActor;
```

Add to setUp() after line 37 (after retryUseCase creation):
```java
        hospitalManagerActor = new ActorContext("u-test", "Test", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.empty());
```

Update all `execute()` calls — replace each occurrence:

| Line | Old | New |
|---|---|---|
| 49 | `previewUseCase.execute("p1")` | `previewUseCase.execute(hospitalManagerActor, "p1")` |
| 66 | `executeUseCase.execute("p1")` | `executeUseCase.execute(hospitalManagerActor, "p1")` |
| 84 | `executeUseCase.execute("p1")` | `executeUseCase.execute(hospitalManagerActor, "p1")` |
| 105 | `retryUseCase.execute("failed-b")` | `retryUseCase.execute(hospitalManagerActor, "failed-b")` |
| 133 | `retryUseCase.execute("done-b")` | `retryUseCase.execute(hospitalManagerActor, "done-b")` |
| 142 | `executeUseCase.execute("p1")` | `executeUseCase.execute(hospitalManagerActor, "p1")` |
| 158 | `retryUseCase.execute("failed-b2")` | `retryUseCase.execute(hospitalManagerActor, "failed-b2")` |
| 175 | `previewUseCase.execute("p-draft")` | `previewUseCase.execute(hospitalManagerActor, "p-draft")` |

- [ ] **Step 6: Run tests**

Run: `cd services/quality-inspection-service && ./gradlew test`
Expected: BUILD SUCCESSFUL — all tests pass including the updated unit tests

- [ ] **Step 7: Commit**

```bash
git add -A && git commit -m "fix(auth): C-1 wire ActorContext into all 3 sampling use cases

All 7 B1/B2 use cases now require HOSPITAL_MANAGER role.
SamplingController forwards actor to use case calls.

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 6: M-4 + M-5 — GlobalExceptionHandler error code + catch-all fixes

**Files:**
- Modify: `interfaces/rest/GlobalExceptionHandler.java:3,16,32-33,51-61`

- [ ] **Step 1: Replace entire GlobalExceptionHandler.java**

```java
package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.tzuchi.his.quality.inspection.application.common.UnauthorizedException;
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.interfaces.rest.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DomainRuleViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDomainRuleViolation(DomainRuleViolationException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(DomainStateConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleDomainStateConflict(DomainStateConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("UNAUTHORIZED", ex.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(BadRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("BAD_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("VALIDATION_ERROR", message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Unexpected IllegalArgumentException — likely a programming error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("INTERNAL_ERROR", "An unexpected error occurred"));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException ex) {
        log.warn("Unexpected IllegalStateException — likely a programming error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("INTERNAL_ERROR", "An unexpected error occurred"));
    }
}
```

- [ ] **Step 2: Run tests**

Run: `cd services/quality-inspection-service && ./gradlew test`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "fix(api): M-4 UNAUTHORIZED error code + M-5 catch-all handlers return 500

- UnauthorizedException now returns error code UNAUTHORIZED (was FORBIDDEN)
- IllegalStateException/IllegalArgumentException catch-all → 500 + log.warn

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 7: ApiKeyAuthFilter error code separation

**Files:**
- Modify: `infrastructure/security/ApiKeyAuthFilter.java:38,46-47,53-58`

- [ ] **Step 1: Update writeUnauthorized to accept error code**

Replace the `writeUnauthorized` method and its callers. Replace the full file content:

```java
package com.tzuchi.his.quality.inspection.infrastructure.security;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.interfaces.rest.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Reads the api-key JWT header, parses it via TzJwtParser, and stores the ActorContext
 * as a request attribute. If the header is missing or invalid, returns 401.
 */
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    public static final String ACTOR_CONTEXT_ATTR = "ACTOR_CONTEXT";

    private final TzJwtParser jwtParser;
    private final ObjectMapper objectMapper;

    public ApiKeyAuthFilter(TzJwtParser jwtParser, ObjectMapper objectMapper) {
        this.jwtParser = jwtParser;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String apiKey = request.getHeader("api-key");

        if (apiKey == null || apiKey.isBlank()) {
            writeError(response, "MISSING_API_KEY", "Missing api-key header");
            return;
        }

        try {
            ActorContext actor = jwtParser.parse(apiKey);
            request.setAttribute(ACTOR_CONTEXT_ATTR, actor);
        } catch (JwtParseException e) {
            writeError(response, "INVALID_TOKEN", "Invalid api-key: " + e.getMessage());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeError(HttpServletResponse response, String code, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                objectMapper.writeValueAsString(ApiResponse.error(code, message)));
    }
}
```

- [ ] **Step 2: Run tests**

Run: `cd services/quality-inspection-service && ./gradlew test`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "fix(auth): differentiate MISSING_API_KEY vs INVALID_TOKEN error codes

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 8: M-1 (T-002) — Cross-section uniqueness tests

> **Dependencies:** None (cross-section check was implemented in Phase 2)

**Files:**
- Modify: `test/.../ScoreSheetDefinitionTest.java`

- [ ] **Step 1: Add two test methods to ScoreSheetDefinitionTest.java**

Add this import if not already present:
```java
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
```

Add these two tests before the `createSheetWithOneItem()` helper (before line 112):

```java
    @Test
    void addSection_crossSectionDuplicateItemId_shouldThrow() {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft("ss1", "p1");
        ScoreSheetSection s1 = new ScoreSheetSection("sec1", "Section 1");
        s1.addItem(new ScoreItem("i1", "C01", "Item1",
                ScoreType.FIXED, BigDecimal.TEN, BigDecimal.ONE));
        ss.addSection(s1);

        ScoreSheetSection s2 = new ScoreSheetSection("sec2", "Section 2");
        s2.addItem(new ScoreItem("i1", "C99", "Different",
                ScoreType.FIXED, BigDecimal.TEN, BigDecimal.ONE));

        assertThrows(DomainStateConflictException.class, () -> ss.addSection(s2));
    }

    @Test
    void addSection_crossSectionDuplicateCode_shouldThrow() {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft("ss1", "p1");
        ScoreSheetSection s1 = new ScoreSheetSection("sec1", "Section 1");
        s1.addItem(new ScoreItem("i1", "C01", "Item1",
                ScoreType.FIXED, BigDecimal.TEN, BigDecimal.ONE));
        ss.addSection(s1);

        ScoreSheetSection s2 = new ScoreSheetSection("sec2", "Section 2");
        s2.addItem(new ScoreItem("i99", "C01", "Different",
                ScoreType.FIXED, BigDecimal.TEN, BigDecimal.ONE));

        assertThrows(DomainStateConflictException.class, () -> ss.addSection(s2));
    }
```

- [ ] **Step 2: Run tests**

Run: `cd services/quality-inspection-service && ./gradlew test`
Expected: BUILD SUCCESSFUL — both new tests pass (the cross-section check was already implemented in Phase 2)

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "test: M-1 add T-002 cross-section uniqueness tests for ScoreSheetDefinition

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 9: M-6 — Negative auth test for sampling endpoints

> **Dependencies:** Task 5 (C-1 — use cases must have `requireRole()`) and Task 6 (M-4 — error code `"UNAUTHORIZED"`)

**Files:**
- Modify: `test/.../SamplingControllerApiTest.java`

- [ ] **Step 1: Add negative auth test**

Add this test method after `retrySampling_completedBatch_shouldReturn422` (before the `// --- Helpers ---` comment):

```java
    @Test
    void executeSampling_nonManagerRole_shouldReturn403() throws Exception {
        mockMvc.perform(post("/review-projects/any-id/sampling/execute")
                        .header("api-key", TestTokenFactory.unknownRoleToken())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }
```

- [ ] **Step 2: Run tests**

Run: `cd services/quality-inspection-service && ./gradlew test`
Expected: BUILD SUCCESSFUL — the new test passes because:
1. `unknownRoleToken()` produces a JWT with `unknown_role` which maps to no `DomainRole`
2. The use case calls `actor.requireRole(HOSPITAL_MANAGER)` which throws `UnauthorizedException`
3. `GlobalExceptionHandler` returns 403 with error code `"UNAUTHORIZED"`

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "test: M-6 add negative auth test for sampling endpoints

Verifies non-HOSPITAL_MANAGER role gets 403 UNAUTHORIZED.

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 10: M-2 (T-004) — ReviewProjectIntegrationTest

**Files:**
- Create: `test/.../infrastructure/persistence/project/ReviewProjectIntegrationTest.java`

- [ ] **Step 1: Create the integration test file**

Create file at `services/quality-inspection-service/src/test/java/com/tzuchi/his/quality/inspection/infrastructure/persistence/project/ReviewProjectIntegrationTest.java`:

```java
package com.tzuchi.his.quality.inspection.infrastructure.persistence.project;

import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.project.ProjectStatus;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ReviewProjectIntegrationTest {

    @Autowired
    private ReviewProjectRepository projectRepository;

    @Test
    void reconstitution_versionAndStatus_shouldBeCorrect() {
        ReviewProject project = ReviewProject.create("p-int-1", "h1", "Test Project",
                EnumSet.of(MedicalRecordType.OUTPATIENT));
        project.publishNewVersion();
        project.prepareDraftVersion();
        projectRepository.save(project);

        ReviewProject loaded = projectRepository.findById("p-int-1")
                .orElseThrow(() -> new AssertionError("Project not found after save"));

        assertEquals(2, loaded.getCurrentVersion().getVersionNumber(),
                "After publish + prepareDraft, version should be 2");
        assertFalse(loaded.getCurrentVersion().isPublished(),
                "New draft version should not be published");
        assertEquals(ProjectStatus.DRAFT, loaded.getStatus(),
                "Status should remain DRAFT");
        assertEquals("Test Project", loaded.getName(),
                "Name should survive round-trip");
        assertEquals("h1", loaded.getHospitalId(),
                "HospitalId should survive round-trip");
    }
}
```

- [ ] **Step 2: Run tests**

Run: `cd services/quality-inspection-service && ./gradlew test`
Expected: BUILD SUCCESSFUL — the reconstitution test verifies JPA round-trip correctness

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "test: M-2 add T-004 ReviewProjectIntegrationTest for reconstitution

Verifies save → publish → prepareDraft → save → reload round-trip.

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 11: Final verification + squash commit

- [ ] **Step 1: Run full clean test suite**

Run: `cd services/quality-inspection-service && ./gradlew clean test`
Expected: BUILD SUCCESSFUL — all tests pass (53 original + 4 new = ~57 tests)

- [ ] **Step 2: Verify git log**

Run: `git --no-pager log --oneline -15`
Expected: 10 new commits since the design spec commit

- [ ] **Step 3: Push to origin**

```bash
git push origin main
```

Expected: Push succeeds.
