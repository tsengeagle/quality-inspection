# Design Spec: Review Round 2 Fixes (C-1, H-1~H-3, M-1~M-6)

**Date**: 2026-04-07  
**Status**: Approved  
**Based on**: Code Review Report `53399be..9db85a4`  
**Scope**: `services/quality-inspection-service`  
**Authority baseline**: Design spec `2026-04-07-backend-review-fixes-design.md`, AGENTS.md, docs/01–04

---

## 1. Problem Statement

Second-round code review identified 1 Critical + 3 High + 6 Medium findings in the B1/B2 backend implementation. The critical finding (C-1) is a security disqualifier: three sampling use cases have no role authorization despite the controller receiving `@CurrentActor`. The high findings are spec misalignments: missing enum values, residual untyped exceptions, and a missing reserved field.

---

## 2. Findings Addressed

| ID | Severity | Summary |
|---|---|---|
| C-1 | Critical | 3 sampling use cases have no `ActorContext` param — auth bypass |
| H-1 | High | `DomainRole` enum has 3 values, spec requires 5 |
| H-2 | High | `ReviewProject` + `SamplingBatch` still throw `IllegalStateException`/`IllegalArgumentException` |
| H-3 | High | `ActorContext` missing `hospitalId`, `hasRole()`, `requireAnyRole()` |
| M-1 | Medium | T-002 missing — cross-section uniqueness untested |
| M-2 | Medium | T-004 missing — `ReviewProjectIntegrationTest` not created |
| M-4 | Medium | Error code `"FORBIDDEN"` should be `"UNAUTHORIZED"` |
| M-5 | Medium | Catch-all handlers mask bugs as 400/409 |
| M-6 | Medium | No negative auth test on sampling endpoints |
| — | Medium | `ApiKeyAuthFilter` error codes not differentiated |

**Not in scope**: L-1 (reconstitute visibility), L-2 (filter path exclusion), L-3 (more test tokens), M-3 (folded into H-3).

---

## 3. Design Decisions

### 3.1 C-1 — Sampling Use Case Auth Bypass

All 3 sampling use cases gain `ActorContext actor` as the **first parameter** (matching the existing 4 project/scoresheet use cases):

```java
// ExecuteSamplingUseCase
public String execute(ActorContext actor, String projectId) {
    actor.requireRole(DomainRole.HOSPITAL_MANAGER);
    // ... existing logic
}

// PreviewSamplingPopulationUseCase
public SamplingPreviewResult execute(ActorContext actor, String projectId) {
    actor.requireRole(DomainRole.HOSPITAL_MANAGER);
    // ... existing logic
}

// RetrySamplingBatchUseCase
public String execute(ActorContext actor, String previousBatchId) {
    actor.requireRole(DomainRole.HOSPITAL_MANAGER);
    // ... existing logic
}
```

`SamplingController` forwards the `actor` parameter:

```java
previewUseCase.execute(actor, projectId);
executeUseCase.execute(actor, projectId);
retryUseCase.execute(actor, batchId);
```

`SamplingUseCaseTest` passes `hospitalManagerActor` to all `execute()` calls.

### 3.2 H-1 — DomainRole Enum Completion

Add `REVIEWER` and `REREVIEWER` to match design spec §3.3 and permission-matrix.md:

```java
public enum DomainRole {
    SYSTEM_ADMIN,
    HOSPITAL_MANAGER,
    REVIEWER,
    REREVIEWER,
    AUDITOR
}
```

`TzJwtParser` already uses `DomainRole.valueOf(roleName.toUpperCase())` — no code change needed there. Previously, `reviewer` and `rereviewer` JWT roles were silently dropped; now they will be correctly parsed.

### 3.3 H-2 — Typed Exception Migration

Five `throw` statements in domain aggregates are migrated from legacy exceptions to typed domain exceptions:

**ReviewProject.java:**

| Line | Old | New | Error Code |
|---|---|---|---|
| 38 | `IllegalArgumentException("Project name must not be blank")` | `DomainRuleViolationException("INVALID_PROJECT_NAME", "Project name must not be blank")` | `INVALID_PROJECT_NAME` |
| 80 | `IllegalStateException("Cannot activate project without a published version")` | `DomainStateConflictException("INVALID_STATE_TRANSITION", "Cannot activate project without a published version")` | `INVALID_STATE_TRANSITION` |
| 110 | `IllegalStateException("A draft version already exists")` | `DomainStateConflictException("INVALID_STATE_TRANSITION", "A draft version already exists")` | `INVALID_STATE_TRANSITION` |
| 119 | `IllegalArgumentException("Project name must not be blank")` | `DomainRuleViolationException("INVALID_PROJECT_NAME", "Project name must not be blank")` | `INVALID_PROJECT_NAME` |

**SamplingBatch.java:**

| Line | Old | New | Error Code |
|---|---|---|---|
| 172 | `IllegalArgumentException(name + " must not be negative")` | `DomainRuleViolationException("INVALID_METRIC_VALUE", name + " must not be negative")` | `INVALID_METRIC_VALUE` |

### 3.4 H-3 — ActorContext Completion

Constructor changes to 4-arg:

```java
public ActorContext(String userId, String userName, Set<DomainRole> roles, Optional<String> hospitalId) {
    this.userId = userId;
    this.userName = userName;
    this.roles = (roles != null && !roles.isEmpty())
            ? EnumSet.copyOf(roles) : EnumSet.noneOf(DomainRole.class);
    this.hospitalId = (hospitalId != null) ? hospitalId : Optional.empty();
}
```

New methods:

```java
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

public Optional<String> getHospitalId() { return hospitalId; }
```

Callers updated to pass `Optional.empty()` for `hospitalId`:
- `TzJwtParser.parse()` → `new ActorContext(userId, userName, roles, Optional.empty())`
- `TestTokenFactory.buildToken()` → same
- `SamplingUseCaseTest.hospitalManagerActor` → same

### 3.5 M-4 — Error Code Alignment

`GlobalExceptionHandler` line 33:

```java
// Before
.body(ApiResponse.error("FORBIDDEN", ex.getMessage()));
// After
.body(ApiResponse.error("UNAUTHORIZED", ex.getMessage()));
```

### 3.6 M-5 — Catch-All Handlers → 500

After H-2 migration, remaining `IllegalStateException`/`IllegalArgumentException` indicate programming errors, not domain violations.

```java
private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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
```

### 3.7 ApiKeyAuthFilter Error Code Separation

```java
// Missing header
writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "MISSING_API_KEY", "Missing api-key header");

// Malformed JWT
writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "INVALID_TOKEN", "Invalid api-key: " + e.getMessage());
```

### 3.8 M-1 (T-002) — Cross-Section Uniqueness Tests

Two new test methods in `ScoreSheetDefinitionTest`:

```java
@Test
void addSection_crossSectionDuplicateItemId_shouldThrow() {
    ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft("ss1", "p1");
    ScoreSheetSection s1 = new ScoreSheetSection("sec1", "Section 1");
    s1.addItem(new ScoreItem("i1", "C01", "Item1", ScoreType.FIXED, BigDecimal.TEN, BigDecimal.ONE));
    ss.addSection(s1);

    ScoreSheetSection s2 = new ScoreSheetSection("sec2", "Section 2");
    s2.addItem(new ScoreItem("i1", "C99", "Different", ScoreType.FIXED, BigDecimal.TEN, BigDecimal.ONE));

    assertThrows(DomainStateConflictException.class, () -> ss.addSection(s2));
}

@Test
void addSection_crossSectionDuplicateCode_shouldThrow() {
    ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft("ss1", "p1");
    ScoreSheetSection s1 = new ScoreSheetSection("sec1", "Section 1");
    s1.addItem(new ScoreItem("i1", "C01", "Item1", ScoreType.FIXED, BigDecimal.TEN, BigDecimal.ONE));
    ss.addSection(s1);

    ScoreSheetSection s2 = new ScoreSheetSection("sec2", "Section 2");
    s2.addItem(new ScoreItem("i99", "C01", "Different", ScoreType.FIXED, BigDecimal.TEN, BigDecimal.ONE));

    assertThrows(DomainStateConflictException.class, () -> ss.addSection(s2));
}
```

### 3.9 M-2 (T-004) — ReviewProjectIntegrationTest

New file: `test/.../infrastructure/persistence/project/ReviewProjectIntegrationTest.java`

```java
@SpringBootTest
class ReviewProjectIntegrationTest {

    @Autowired ReviewProjectRepository projectRepository;

    @Test
    void reconstitution_versionAndStatus_shouldBeCorrect() {
        ReviewProject project = ReviewProject.create("p-int-1", "h1", "Test Project",
                EnumSet.of(MedicalRecordType.OUTPATIENT));
        project.publishNewVersion();
        project.prepareDraftVersion();
        projectRepository.save(project);

        ReviewProject loaded = projectRepository.findById("p-int-1").orElseThrow();

        assertEquals(2, loaded.getCurrentVersion().getVersionNumber());
        assertFalse(loaded.getCurrentVersion().isPublished());
        assertEquals(ProjectStatus.DRAFT, loaded.getStatus());
        assertEquals("Test Project", loaded.getName());
    }
}
```

### 3.10 M-6 — Negative Auth Test

New test method in `SamplingControllerApiTest`:

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

---

## 4. Files Affected

| # | File | Changes |
|---|---|---|
| 1 | `application/common/ActorContext.java` | H-3: 4-arg constructor, `hospitalId`, `hasRole()`, `requireAnyRole()` |
| 2 | `application/common/DomainRole.java` | H-1: add `REVIEWER`, `REREVIEWER` |
| 3 | `application/sampling/ExecuteSamplingUseCase.java` | C-1: add `ActorContext` param + `requireRole()` |
| 4 | `application/sampling/PreviewSamplingPopulationUseCase.java` | C-1: add `ActorContext` param + `requireRole()` |
| 5 | `application/sampling/RetrySamplingBatchUseCase.java` | C-1: add `ActorContext` param + `requireRole()` |
| 6 | `domain/project/ReviewProject.java` | H-2: 4 exception replacements |
| 7 | `domain/sampling/SamplingBatch.java` | H-2: 1 exception replacement |
| 8 | `infrastructure/security/ApiKeyAuthFilter.java` | Error codes: `MISSING_API_KEY` / `INVALID_TOKEN` |
| 9 | `infrastructure/security/TzJwtParser.java` | H-3: pass `Optional.empty()` for hospitalId |
| 10 | `interfaces/rest/GlobalExceptionHandler.java` | M-4: `UNAUTHORIZED` code; M-5: catch-all → 500 + log |
| 11 | `interfaces/rest/SamplingController.java` | C-1: forward `actor` to use cases |
| 12 | `test/.../SamplingUseCaseTest.java` | C-1 + H-3: actor param in all execute calls |
| 13 | `test/.../TestTokenFactory.java` | H-3: pass `Optional.empty()` |
| 14 | `test/.../ReviewProjectTest.java` | H-2: update assertThrows types |
| 15 | `test/.../SamplingBatchTest.java` | H-2: update assertThrows type |
| 16 | `test/.../ScoreSheetDefinitionTest.java` | M-1: 2 cross-section tests |
| 17 | `test/.../SamplingControllerApiTest.java` | M-6: negative auth test |
| 18 | `test/.../ReviewProjectIntegrationTest.java` | **NEW** M-2: reconstitution round-trip |

---

## 5. Not In Scope

- L-1: `reconstitute()` visibility (pragmatically acceptable as `public`)
- L-2: `ApiKeyAuthFilter` path exclusion (no health check in B1/B2)
- L-3: Additional `TestTokenFactory` variants (nice-to-have)
