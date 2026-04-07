# Backend Review Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix all 2 Critical, 6 High, 4 Medium, 4 Low issues and 7 missing tests identified in the code review, making the B1+B2 backend production-ready.

**Architecture:** Three phases — (1) auth infrastructure via JWT parsing and ActorContext passed as first param to all use cases; (2) domain fixes via typed exceptions, reconstitute() factories, and DB constraints; (3) medium/low cleanup, test coverage, and doc alignment.

**Tech Stack:** Java 11, Spring Boot 2.7.18, Gradle 7.6.4, H2 (test), JPA/Hibernate, JUnit 5, MockMvc.

---

## File Structure

All paths relative to `services/quality-inspection-service/src/`.

**New files (main):**

| File | Responsibility |
|---|---|
| `main/.../domain/common/DomainRuleViolationException.java` | HTTP 422 typed exception; constructor `(String errorCode, String message)` |
| `main/.../domain/common/DomainStateConflictException.java` | HTTP 409 typed exception; constructor `(String errorCode, String message)` |
| `main/.../application/common/DomainRole.java` | Enum: SYSTEM_ADMIN, HOSPITAL_MANAGER, REVIEWER, REREVIEWER, AUDITOR |
| `main/.../application/common/ActorContext.java` | Auth value object with requireRole() / requireAnyRole(); hospitalId reserved as empty |
| `main/.../application/common/UnauthorizedException.java` | HTTP 403 exception |
| `main/.../infrastructure/security/ParsedJwtUser.java` | Intermediate DTO from JWT parsing |
| `main/.../infrastructure/security/JwtParseException.java` | JWT parse failure exception (caught by filter → 401) |
| `main/.../infrastructure/security/TzJwtParser.java` | Parse tz-user claim from api-key JWT header value |
| `main/.../infrastructure/security/ApiKeyAuthFilter.java` | OncePerRequestFilter: api-key header → ActorContext in request attribute |
| `main/.../interfaces/rest/CurrentActor.java` | @CurrentActor parameter annotation |
| `main/.../interfaces/rest/ActorContextArgumentResolver.java` | HandlerMethodArgumentResolver for @CurrentActor ActorContext params |
| `main/.../interfaces/rest/BadRequestException.java` | HTTP 400 exception for controller-layer bad input |
| `main/.../interfaces/config/WebMvcConfig.java` | Register ActorContextArgumentResolver |

**New files (test):**

| File | Responsibility |
|---|---|
| `test/.../infrastructure/security/TzJwtParserTest.java` | Unit test JWT parsing edge cases |
| `test/.../TestTokenFactory.java` | Produces test JWTs (no signature verification in service) |
| `test/.../infrastructure/persistence/project/ReviewProjectIntegrationTest.java` | T-004: @SpringBootTest reconstitution correctness |
| `test/.../interfaces/rest/SamplingControllerApiTest.java` | T-007: Sampling HTTP contract tests |

**Modified files:**

| File | Change |
|---|---|
| `main/.../interfaces/rest/GlobalExceptionHandler.java` | Add typed handlers: 422/409/403/400 |
| `main/.../interfaces/rest/dto/CreateReviewProjectRequest.java` | Add @NotNull @NotBlank @NotNull |
| `main/.../interfaces/rest/ReviewProjectController.java` | Add @Valid, @CurrentActor, fix MedicalRecordType error |
| `main/.../interfaces/rest/SamplingController.java` | Add @CurrentActor ActorContext |
| `main/.../interfaces/rest/ScoreSheetController.java` | Add @CurrentActor ActorContext |
| `main/.../domain/project/ReviewProject.java` | Add reconstitute(), change IllegalStateException → typed |
| `main/.../domain/scoresheet/ScoreSheetDefinition.java` | Add reconstitute(), cross-section uniqueness, change exceptions |
| `main/.../domain/sampling/SamplingBatch.java` | Add reconstitute(), failureReason field, change exceptions |
| `main/.../domain/sampling/RuleSnapshot.java` | Add TODO Javadoc (M-002) |
| `main/.../application/project/CreateReviewProjectUseCase.java` | Add ActorContext first param |
| `main/.../application/project/PublishReviewProjectVersionUseCase.java` | Add ActorContext first param |
| `main/.../application/scoresheet/CreateScoreSheetDraftUseCase.java` | Add ActorContext first param |
| `main/.../application/scoresheet/PublishScoreSheetVersionUseCase.java` | Add ActorContext first param |
| `main/.../application/sampling/PreviewSamplingPopulationUseCase.java` | Add ActorContext first param, fix exception type |
| `main/.../application/sampling/ExecuteSamplingUseCase.java` | Add ActorContext first param, fix exception types |
| `main/.../application/sampling/RetrySamplingBatchUseCase.java` | Add ActorContext first param, fix C-001 snapshot bug and exception types |
| `main/.../application/sampling/SamplingPreviewResult.java` | Remove sourceCaseKeys (M-005) |
| `main/.../infrastructure/persistence/project/ReviewProjectRepositoryImpl.java` | Use reconstitute() |
| `main/.../infrastructure/persistence/scoresheet/ScoreSheetRepositoryImpl.java` | Use reconstitute() |
| `main/.../infrastructure/persistence/sampling/SamplingBatchRepositoryImpl.java` | Use reconstitute() |
| `main/.../infrastructure/persistence/sampling/SamplingBatchEntity.java` | Add failure_reason column |
| `main/.../infrastructure/persistence/sampling/SelectedCaseRefEntity.java` | Add batch_id column, unique constraint, fix table name |
| All 6 entity files (M-003) | Fix @Table names to match persistence-model.md |
| `main/.../infrastructure/persistence/scoresheet/ScoreSheetSectionEntity.java` | Fix @JoinColumn(name) (L-003) |
| `main/resources/application.yml` | Move ddl-auto: update to local profile only (L-004) |
| `test/.../interfaces/rest/ReviewProjectApiTest.java` | Add api-key header to all tests, add T-003 and T-006 |
| `test/.../application/sampling/SamplingUseCaseTest.java` | Add ActorContext, T-001 and T-005, fix exception types |
| `docs/03-contracts/api-spec.md` | Fix base path /api/quality-review → /api/quality-inspection (SD-001) |

---

## Phase 1 — Auth Infrastructure

### Task 1: Domain Exception Types

**Files:**
- Create: `main/java/com/tzuchi/his/quality/inspection/domain/common/DomainRuleViolationException.java`
- Create: `main/java/com/tzuchi/his/quality/inspection/domain/common/DomainStateConflictException.java`

- [ ] **Step 1: Create DomainRuleViolationException**

```java
package com.tzuchi.his.quality.inspection.domain.common;

/** Thrown when a business rule prevents the requested operation. Maps to HTTP 422. */
public class DomainRuleViolationException extends RuntimeException {

    private final String errorCode;

    public DomainRuleViolationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
```

- [ ] **Step 2: Create DomainStateConflictException**

```java
package com.tzuchi.his.quality.inspection.domain.common;

/** Thrown when the requested operation conflicts with the current aggregate state. Maps to HTTP 409. */
public class DomainStateConflictException extends RuntimeException {

    private final String errorCode;

    public DomainStateConflictException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
```

- [ ] **Step 3: Commit**

```bash
cd services/quality-inspection-service
git add src/main/java/com/tzuchi/his/quality/inspection/domain/common/DomainRuleViolationException.java
git add src/main/java/com/tzuchi/his/quality/inspection/domain/common/DomainStateConflictException.java
git commit -m "feat: add typed domain exception hierarchy (DomainRuleViolationException, DomainStateConflictException)

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 2: Auth Value Objects

**Files:**
- Create: `main/java/com/tzuchi/his/quality/inspection/application/common/DomainRole.java`
- Create: `main/java/com/tzuchi/his/quality/inspection/application/common/ActorContext.java`
- Create: `main/java/com/tzuchi/his/quality/inspection/application/common/UnauthorizedException.java`

- [ ] **Step 1: Create DomainRole**

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

- [ ] **Step 2: Create UnauthorizedException**

```java
package com.tzuchi.his.quality.inspection.application.common;

/** Thrown when the actor does not have the required role. Maps to HTTP 403. */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
```

- [ ] **Step 3: Create ActorContext**

```java
package com.tzuchi.his.quality.inspection.application.common;

import java.util.*;

/**
 * Immutable value object representing the authenticated actor making a request.
 * Passed as the first argument to every use case execute() method.
 * hospitalId is reserved for future use and always returns Optional.empty() in this phase.
 */
public class ActorContext {

    private final String userId;
    private final String userName;
    private final Set<DomainRole> roles;

    public ActorContext(String userId, String userName, Set<DomainRole> roles) {
        this.userId = userId;
        this.userName = userName;
        this.roles = Collections.unmodifiableSet(new HashSet<>(roles));
    }

    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
    public Set<DomainRole> getRoles() { return roles; }

    /** Reserved for future hospital-scope validation. Always returns empty in this phase. */
    public Optional<String> getHospitalId() { return Optional.empty(); }

    public boolean hasRole(DomainRole role) {
        return roles.contains(role);
    }

    /** Throws UnauthorizedException if the actor does not have the required role. */
    public void requireRole(DomainRole role) {
        if (!hasRole(role)) {
            throw new UnauthorizedException(
                    "Required role: " + role + ", but actor has: " + roles);
        }
    }

    /** Throws UnauthorizedException if the actor does not have any of the required roles. */
    public void requireAnyRole(DomainRole... requiredRoles) {
        for (DomainRole role : requiredRoles) {
            if (hasRole(role)) return;
        }
        throw new UnauthorizedException(
                "Required any of: " + Arrays.toString(requiredRoles) + ", but actor has: " + roles);
    }
}
```

- [ ] **Step 4: Run tests to ensure nothing is broken**

```bash
./gradlew test
```

Expected: all existing tests PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/tzuchi/his/quality/inspection/application/common/
git commit -m "feat: add ActorContext, DomainRole, UnauthorizedException (application/common)

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 3: JWT Infrastructure + TestTokenFactory + Update ReviewProjectApiTest

**Files:**
- Create: `main/java/com/tzuchi/his/quality/inspection/infrastructure/security/JwtParseException.java`
- Create: `main/java/com/tzuchi/his/quality/inspection/infrastructure/security/ParsedJwtUser.java`
- Create: `main/java/com/tzuchi/his/quality/inspection/infrastructure/security/TzJwtParser.java`
- Create: `main/java/com/tzuchi/his/quality/inspection/infrastructure/security/ApiKeyAuthFilter.java`
- Create: `test/java/com/tzuchi/his/quality/inspection/TestTokenFactory.java`
- Create: `test/java/com/tzuchi/his/quality/inspection/infrastructure/security/TzJwtParserTest.java`
- Modify: `test/java/com/tzuchi/his/quality/inspection/interfaces/rest/ReviewProjectApiTest.java`

- [ ] **Step 1: Create JwtParseException**

```java
package com.tzuchi.his.quality.inspection.infrastructure.security;

public class JwtParseException extends RuntimeException {
    public JwtParseException(String message) {
        super(message);
    }
}
```

- [ ] **Step 2: Create ParsedJwtUser**

```java
package com.tzuchi.his.quality.inspection.infrastructure.security;

import com.tzuchi.his.quality.inspection.application.common.DomainRole;

import java.util.Set;

/** Intermediate DTO representing user data parsed from the JWT tz-user claim. */
public class ParsedJwtUser {

    private final String userId;
    private final String userName;
    private final Set<DomainRole> roles;

    public ParsedJwtUser(String userId, String userName, Set<DomainRole> roles) {
        this.userId = userId;
        this.userName = userName;
        this.roles = roles;
    }

    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
    public Set<DomainRole> getRoles() { return roles; }
}
```

- [ ] **Step 3: Create TzJwtParser**

JWT structure: `{header}.{payload}.{sig}` — each segment is Base64URL-encoded (no padding).
The payload contains a `tz-user` key whose **value is a JSON string** (double-encoded).
That inner string contains: `{"user":{"user_id":"...","user_name":"...","user_role":[{"role_name":"...","enabled":true}]}}`.
The service does NOT verify the signature (gateway already did).

```java
package com.tzuchi.his.quality.inspection.infrastructure.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

@Component
public class TzJwtParser {

    private final ObjectMapper objectMapper;

    public TzJwtParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Parses the raw JWT value from the api-key header.
     * Does NOT verify signature — gateway has already done so.
     * @throws JwtParseException if token is malformed or missing required claims
     */
    public ParsedJwtUser parse(String rawToken) {
        try {
            String[] parts = rawToken.split("\\.", -1);
            if (parts.length < 2) {
                throw new JwtParseException("Invalid JWT: expected at least 2 segments");
            }

            byte[] payloadBytes = Base64.getUrlDecoder().decode(padBase64(parts[1]));
            String payloadJson = new String(payloadBytes, StandardCharsets.UTF_8);

            JsonNode payloadNode = objectMapper.readTree(payloadJson);
            JsonNode tzUserNode = payloadNode.get("tz-user");
            if (tzUserNode == null) {
                throw new JwtParseException("Missing tz-user claim in JWT payload");
            }

            // tz-user is a JSON string value that itself contains JSON
            JsonNode userWrapper = objectMapper.readTree(tzUserNode.asText());
            JsonNode userNode = userWrapper.get("user");
            if (userNode == null) {
                throw new JwtParseException("Missing user object in tz-user claim");
            }

            String userId = userNode.path("user_id").asText();
            String userName = userNode.path("user_name").asText();

            Set<DomainRole> roles = new HashSet<>();
            JsonNode roleArray = userNode.get("user_role");
            if (roleArray != null) {
                for (JsonNode roleNode : roleArray) {
                    boolean enabled = roleNode.path("enabled").asBoolean(false);
                    if (enabled) {
                        String roleName = roleNode.path("role_name").asText().toUpperCase();
                        try {
                            roles.add(DomainRole.valueOf(roleName));
                        } catch (IllegalArgumentException ignored) {
                            // Unknown role — skip silently
                        }
                    }
                }
            }

            return new ParsedJwtUser(userId, userName, roles);

        } catch (JwtParseException e) {
            throw e;
        } catch (Exception e) {
            throw new JwtParseException("Failed to parse JWT: " + e.getMessage());
        }
    }

    private String padBase64(String s) {
        int mod = s.length() % 4;
        if (mod == 0) return s;
        return s + "=".repeat(4 - mod);
    }
}
```

- [ ] **Step 4: Create ApiKeyAuthFilter**

```java
package com.tzuchi.his.quality.inspection.infrastructure.security;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    static final String ACTOR_CONTEXT_ATTR = "ACTOR_CONTEXT";

    private final TzJwtParser jwtParser;

    public ApiKeyAuthFilter(TzJwtParser jwtParser) {
        this.jwtParser = jwtParser;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String apiKey = request.getHeader("api-key");
        if (apiKey == null || apiKey.isBlank()) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "MISSING_API_KEY", "Missing api-key header");
            return;
        }

        ActorContext actor;
        try {
            ParsedJwtUser jwtUser = jwtParser.parse(apiKey);
            actor = new ActorContext(jwtUser.getUserId(), jwtUser.getUserName(), jwtUser.getRoles());
        } catch (JwtParseException e) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "INVALID_TOKEN", e.getMessage());
            return;
        }

        request.setAttribute(ACTOR_CONTEXT_ATTR, actor);
        chain.doFilter(request, response);
    }

    private void writeError(HttpServletResponse response,
                             int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"success\":false,\"error\":{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}}");
    }
}
```

- [ ] **Step 5: Create TestTokenFactory**

Place in the test source root (accessible from all test classes):

```java
package com.tzuchi.his.quality.inspection;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/** Produces unsigned test JWTs for @SpringBootTest tests. Signature is not verified by the service. */
public class TestTokenFactory {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Returns a valid test JWT token for a HOSPITAL_MANAGER actor. */
    public static String hospitalManagerToken() {
        String tzUser = "{\"user\":{\"user_id\":\"test-user\",\"user_name\":\"Test User\","
                + "\"user_role\":[{\"role_name\":\"hospital_manager\",\"enabled\":true}]}}";
        return buildToken(tzUser);
    }

    /** Returns a valid test JWT token for a SYSTEM_ADMIN actor. */
    public static String systemAdminToken() {
        String tzUser = "{\"user\":{\"user_id\":\"admin-user\",\"user_name\":\"Admin\","
                + "\"user_role\":[{\"role_name\":\"system_admin\",\"enabled\":true}]}}";
        return buildToken(tzUser);
    }

    private static String buildToken(String tzUserJson) {
        try {
            String headerJson = "{\"alg\":\"RS512\",\"typ\":\"JWT\"}";
            Map<String, String> payloadMap = Map.of("tz-user", tzUserJson);
            String payloadJson = MAPPER.writeValueAsString(payloadMap);

            String header = b64UrlEncode(headerJson);
            String payload = b64UrlEncode(payloadJson);
            return header + "." + payload + ".test-sig";
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static String b64UrlEncode(String json) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
```

- [ ] **Step 6: Create TzJwtParserTest**

```java
package com.tzuchi.his.quality.inspection.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tzuchi.his.quality.inspection.TestTokenFactory;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TzJwtParserTest {

    private final TzJwtParser parser = new TzJwtParser(new ObjectMapper());

    @Test
    void hospitalManagerToken_shouldParseCorrently() {
        String token = TestTokenFactory.hospitalManagerToken();
        ParsedJwtUser user = parser.parse(token);

        assertEquals("test-user", user.getUserId());
        assertEquals("Test User", user.getUserName());
        assertTrue(user.getRoles().contains(DomainRole.HOSPITAL_MANAGER));
    }

    @Test
    void missingApiKey_shouldThrowJwtParseException() {
        assertThrows(JwtParseException.class, () -> parser.parse("notajwt"));
    }

    @Test
    void missingTzUserClaim_shouldThrowJwtParseException() {
        // Build a JWT without tz-user claim
        String header = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"RS512\"}".getBytes());
        String payload = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"no-tz-user\"}".getBytes());
        String token = header + "." + payload + ".sig";

        assertThrows(JwtParseException.class, () -> parser.parse(token));
    }
}
```

- [ ] **Step 7: Update ReviewProjectApiTest — add api-key header to ALL existing tests**

After adding `ApiKeyAuthFilter` as a `@Component`, every request to `@SpringBootTest` tests needs the `api-key` header or it returns 401. Update all 4 existing tests:

Replace the entire `ReviewProjectApiTest.java` with:

```java
package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tzuchi.his.quality.inspection.TestTokenFactory;
import com.tzuchi.his.quality.inspection.interfaces.rest.dto.CreateReviewProjectRequest;
import com.tzuchi.his.quality.inspection.interfaces.rest.dto.CreateScoreSheetDraftRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ReviewProjectApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Note: use TestTokenFactory.hospitalManagerToken() for api-key in all tests.
    // After Task 12, use cases will also call actor.requireRole(HOSPITAL_MANAGER),
    // but at this stage the use cases don't yet take ActorContext — adding the header
    // here now prevents 401s once the filter is wired in.

    @Test
    void createProject_shouldReturn201() throws Exception {
        CreateReviewProjectRequest req = new CreateReviewProjectRequest();
        req.setHospitalId("H001");
        req.setName("Test Project");
        req.setMedicalRecordTypes(Set.of("OUTPATIENT", "INPATIENT"));

        mockMvc.perform(post("/review-projects")
                        .header("api-key", TestTokenFactory.hospitalManagerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.projectId").isNotEmpty());
    }

    @Test
    void publishVersion_newProject_shouldSucceed() throws Exception {
        CreateReviewProjectRequest req = new CreateReviewProjectRequest();
        req.setHospitalId("H001");
        req.setName("Publish Test");
        req.setMedicalRecordTypes(Set.of("OUTPATIENT"));

        MvcResult result = mockMvc.perform(post("/review-projects")
                        .header("api-key", TestTokenFactory.hospitalManagerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        String projectId = objectMapper.readTree(
                result.getResponse().getContentAsString())
                .get("data").get("projectId").asText();

        mockMvc.perform(post("/review-projects/" + projectId + "/publish-version")
                        .header("api-key", TestTokenFactory.hospitalManagerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void publishVersion_alreadyPublished_shouldReturn409() throws Exception {
        CreateReviewProjectRequest req = new CreateReviewProjectRequest();
        req.setHospitalId("H001");
        req.setName("Double Publish Test");
        req.setMedicalRecordTypes(Set.of("OUTPATIENT"));

        MvcResult result = mockMvc.perform(post("/review-projects")
                        .header("api-key", TestTokenFactory.hospitalManagerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        String projectId = objectMapper.readTree(
                result.getResponse().getContentAsString())
                .get("data").get("projectId").asText();

        mockMvc.perform(post("/review-projects/" + projectId + "/publish-version")
                        .header("api-key", TestTokenFactory.hospitalManagerToken()))
                .andExpect(status().isOk());

        // Publish again — should conflict
        mockMvc.perform(post("/review-projects/" + projectId + "/publish-version")
                        .header("api-key", TestTokenFactory.hospitalManagerToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createScoreSheetDraft_shouldReturn201() throws Exception {
        CreateScoreSheetDraftRequest req = new CreateScoreSheetDraftRequest();
        req.setProjectId("p-test");

        mockMvc.perform(post("/score-sheets")
                        .header("api-key", TestTokenFactory.hospitalManagerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.scoreSheetId").isNotEmpty());
    }

    @Test
    void request_withoutApiKey_shouldReturn401() throws Exception {
        mockMvc.perform(post("/review-projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("MISSING_API_KEY"));
    }
}
```

- [ ] **Step 8: Run tests to verify all pass**

```bash
./gradlew test
```

Expected: All 5 tests in `ReviewProjectApiTest` PASS. `TzJwtParserTest` (3 tests) PASS.

- [ ] **Step 9: Commit**

```bash
git add src/main/java/com/tzuchi/his/quality/inspection/infrastructure/security/
git add src/test/java/com/tzuchi/his/quality/inspection/TestTokenFactory.java
git add src/test/java/com/tzuchi/his/quality/inspection/infrastructure/security/TzJwtParserTest.java
git add src/test/java/com/tzuchi/his/quality/inspection/interfaces/rest/ReviewProjectApiTest.java
git commit -m "feat: add JWT auth filter (ApiKeyAuthFilter, TzJwtParser), TestTokenFactory; update ReviewProjectApiTest

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 4: Controller Infrastructure (@CurrentActor + ArgumentResolver + WebMvcConfig)

**Files:**
- Create: `main/java/com/tzuchi/his/quality/inspection/interfaces/rest/CurrentActor.java`
- Create: `main/java/com/tzuchi/his/quality/inspection/interfaces/rest/ActorContextArgumentResolver.java`
- Create: `main/java/com/tzuchi/his/quality/inspection/interfaces/config/WebMvcConfig.java`

- [ ] **Step 1: Create @CurrentActor annotation**

```java
package com.tzuchi.his.quality.inspection.interfaces.rest;

import java.lang.annotation.*;

/** Marks a controller method parameter to be resolved from the authenticated ActorContext. */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentActor {
}
```

- [ ] **Step 2: Create ActorContextArgumentResolver**

```java
package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.UnauthorizedException;
import com.tzuchi.his.quality.inspection.infrastructure.security.ApiKeyAuthFilter;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.servlet.http.HttpServletRequest;

@Component
public class ActorContextArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(ActorContext.class)
                && parameter.hasParameterAnnotation(CurrentActor.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                   ModelAndViewContainer mavContainer,
                                   NativeWebRequest webRequest,
                                   WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            throw new UnauthorizedException("No HTTP request context");
        }
        ActorContext actor = (ActorContext) request.getAttribute(ApiKeyAuthFilter.ACTOR_CONTEXT_ATTR);
        if (actor == null) {
            throw new UnauthorizedException("No authenticated actor in request");
        }
        return actor;
    }
}
```

- [ ] **Step 3: Create WebMvcConfig**

```java
package com.tzuchi.his.quality.inspection.interfaces.config;

import com.tzuchi.his.quality.inspection.interfaces.rest.ActorContextArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final ActorContextArgumentResolver actorContextArgumentResolver;

    public WebMvcConfig(ActorContextArgumentResolver actorContextArgumentResolver) {
        this.actorContextArgumentResolver = actorContextArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(actorContextArgumentResolver);
    }
}
```

- [ ] **Step 4: Run tests**

```bash
./gradlew test
```

Expected: All existing tests PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/tzuchi/his/quality/inspection/interfaces/rest/CurrentActor.java
git add src/main/java/com/tzuchi/his/quality/inspection/interfaces/rest/ActorContextArgumentResolver.java
git add src/main/java/com/tzuchi/his/quality/inspection/interfaces/config/WebMvcConfig.java
git commit -m "feat: add @CurrentActor annotation, ActorContextArgumentResolver, WebMvcConfig

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 5: GlobalExceptionHandler Updates

**Files:**
- Create: `main/java/com/tzuchi/his/quality/inspection/interfaces/rest/BadRequestException.java`
- Modify: `main/java/com/tzuchi/his/quality/inspection/interfaces/rest/GlobalExceptionHandler.java`

- [ ] **Step 1: Create BadRequestException**

```java
package com.tzuchi.his.quality.inspection.interfaces.rest;

/** Thrown in the interfaces layer when a request parameter is invalid. Maps to HTTP 400. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
```

- [ ] **Step 2: Replace GlobalExceptionHandler**

```java
package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.tzuchi.his.quality.inspection.application.common.UnauthorizedException;
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.interfaces.rest.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 422 Unprocessable Entity — business rule violated */
    @ExceptionHandler(DomainRuleViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuleViolation(DomainRuleViolationException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage()));
    }

    /** 409 Conflict — aggregate state conflict */
    @ExceptionHandler(DomainStateConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleStateConflict(DomainStateConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage()));
    }

    /** 403 Forbidden — insufficient role */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("UNAUTHORIZED", ex.getMessage()));
    }

    /** 400 Bad Request — invalid input value (interfaces layer) */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(BadRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("BAD_REQUEST", ex.getMessage()));
    }

    /** 400 Bad Request — @Valid constraint violation */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + " " + f.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("VALIDATION_ERROR", message));
    }

    /** 400 Bad Request — generic illegal argument */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("BAD_REQUEST", ex.getMessage()));
    }

    /** 409 Conflict — legacy fallback for any uncaught IllegalStateException */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("CONFLICT", ex.getMessage()));
    }
}
```

- [ ] **Step 3: Run tests**

```bash
./gradlew test
```

Expected: All existing tests PASS.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/tzuchi/his/quality/inspection/interfaces/rest/BadRequestException.java
git add src/main/java/com/tzuchi/his/quality/inspection/interfaces/rest/GlobalExceptionHandler.java
git commit -m "feat: update GlobalExceptionHandler with typed 422/409/403/400 handlers; add BadRequestException

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Phase 2 — Domain Fixes

### Task 6: C-001 Fix — Retry Snapshot (TDD: T-001)

**Files:**
- Modify: `test/java/com/tzuchi/his/quality/inspection/application/sampling/SamplingUseCaseTest.java`
- Modify: `main/java/com/tzuchi/his/quality/inspection/application/sampling/RetrySamplingBatchUseCase.java`

**Bug (C-001):** `RetrySamplingBatchUseCase` at lines 57-59 builds a new `RuleSnapshot` from `project.getCurrentVersion()` instead of copying `previousBatch.getRuleSnapshot()`. This breaks the audit trail — the retry batch should use the exact same snapshot as the original failed batch.

**Also fix:** The `IllegalStateException` for non-retryable batch status must change to `DomainRuleViolationException("BATCH_NOT_RETRYABLE", ...)`. Update the existing test that asserts `IllegalStateException`.

- [ ] **Step 1: Add T-001 test and fix existing exception assertion in SamplingUseCaseTest**

Add these two tests, and update the existing `retry_nonFailedBatch_shouldThrow` assertion:

```java
// Add these imports at the top of SamplingUseCaseTest:
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;

// Add T-001 test:
@Test
void retry_shouldUseOriginalBatchSnapshot_notCurrentProjectVersion() {
    ReviewProject project = createActiveProject("p1");
    projectRepo.save(project);

    RuleSnapshot originalSnapshot = new RuleSnapshot("project=p1,version=1", "policy-v1");
    SamplingBatch failedBatch = SamplingBatch.start("failed-b", "p1",
            ExecutionMode.MANUAL, originalSnapshot);
    failedBatch.fail("timeout");
    batchRepo.save(failedBatch);

    // Advance project version — the retry should still use originalSnapshot, not current version
    project.publishNewVersion();
    project.prepareDraftVersion();
    projectRepo.save(project);

    candidateSource.setCandidates(List.of(
            new SelectedCaseRef("C1", "H1", MedicalRecordType.OUTPATIENT)
    ));

    String newBatchId = retryUseCase.execute("failed-b");

    SamplingBatch newBatch = batchRepo.store.get(newBatchId);
    assertEquals(originalSnapshot.getProjectVersionInfo(),
            newBatch.getRuleSnapshot().getProjectVersionInfo(),
            "Retry batch must use the original failed batch snapshot, not current project version");
    assertEquals(originalSnapshot.getSamplingPolicyInfo(),
            newBatch.getRuleSnapshot().getSamplingPolicyInfo());
}

// Replace existing retry_nonFailedBatch_shouldThrow:
@Test
void retry_nonFailedBatch_shouldThrow() {
    ReviewProject project = createActiveProject("p1");
    projectRepo.save(project);

    SamplingBatch completedBatch = SamplingBatch.start("done-b", "p1",
            ExecutionMode.MANUAL, new RuleSnapshot("v1", "policy"));
    completedBatch.recordCandidateCount(1, 1, 0);
    completedBatch.selectCases(List.of(
            new SelectedCaseRef("C1", "H1", MedicalRecordType.OUTPATIENT)
    ));
    completedBatch.complete();
    batchRepo.save(completedBatch);

    assertThrows(DomainRuleViolationException.class, () -> retryUseCase.execute("done-b"));
}
```

- [ ] **Step 2: Run T-001 to verify it FAILS**

```bash
./gradlew test --tests "*.SamplingUseCaseTest.retry_shouldUseOriginalBatchSnapshot_notCurrentProjectVersion"
```

Expected: FAIL — the test catches that the current (wrong) snapshot uses the project's current version.

- [ ] **Step 3: Fix RetrySamplingBatchUseCase**

In `RetrySamplingBatchUseCase.java`, make these two changes:

1. Replace the `RuleSnapshot` construction at lines 57–59:

```java
// BEFORE (buggy):
RuleSnapshot snapshot = new RuleSnapshot(
        "project=" + projectId + ",version=" + project.getCurrentVersion().getVersionNumber(),
        "default-sampling-policy"
);

// AFTER (correct):
RuleSnapshot snapshot = previousBatch.getRuleSnapshot();
```

2. Change the non-retryable batch check from `IllegalStateException` to `DomainRuleViolationException`:

```java
// Add import at top of file:
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;

// Replace:
if (previousBatch.getStatus() != BatchStatus.FAILED) {
    throw new IllegalStateException("Only FAILED batches can be retried");
}

// With:
if (previousBatch.getStatus() != BatchStatus.FAILED) {
    throw new DomainRuleViolationException("BATCH_NOT_RETRYABLE",
            "Only FAILED batches can be retried. Current status: " + previousBatch.getStatus());
}
```

Also fix the "Project must be ACTIVE" check in the same file:

```java
// Replace:
if (project.getStatus() != ProjectStatus.ACTIVE) {
    throw new IllegalStateException("Project must be ACTIVE for retry");
}

// With:
if (project.getStatus() != ProjectStatus.ACTIVE) {
    throw new DomainRuleViolationException("PROJECT_NOT_ACTIVE",
            "Project must be ACTIVE for sampling retry. Current status: " + project.getStatus());
}
```

- [ ] **Step 4: Run all SamplingUseCaseTest to verify they all PASS**

```bash
./gradlew test --tests "*.SamplingUseCaseTest"
```

Expected: All 7 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/tzuchi/his/quality/inspection/application/sampling/RetrySamplingBatchUseCase.java
git add src/test/java/com/tzuchi/his/quality/inspection/application/sampling/SamplingUseCaseTest.java
git commit -m "fix(C-001): retry batch uses original snapshot; change to DomainRuleViolationException; add T-001

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 7: H-001 Fix — Cross-Section Uniqueness (TDD: T-002)

**Files:**
- Create: `test/java/com/tzuchi/his/quality/inspection/domain/scoresheet/ScoreSheetDefinitionTest.java`
- Modify: `main/java/com/tzuchi/his/quality/inspection/domain/scoresheet/ScoreSheetDefinition.java`

**Bug (H-001):** `ScoreSheetDefinition.addSection()` only checks for duplicate `sectionId` — it does not check whether the new section's items have `itemId` or `code` conflicts with items already in other sections of the same definition. This allows cross-section duplicates that violate the aggregate invariant.

- [ ] **Step 1: Write failing tests T-002**

```java
package com.tzuchi.his.quality.inspection.domain.scoresheet;

import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ScoreSheetDefinitionTest {

    private ScoreItem item(String itemId, String code) {
        return new ScoreItem(itemId, code, "Title " + itemId,
                ScoreType.SCORE, BigDecimal.TEN, BigDecimal.ONE);
    }

    @Test
    void addSection_crossSectionDuplicateItemId_shouldThrow() {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft("ss1", "p1");

        ScoreSheetSection s1 = new ScoreSheetSection("sec1", "Section 1");
        s1.addItem(item("item-1", "CODE-A"));
        ss.addSection(s1);

        ScoreSheetSection s2 = new ScoreSheetSection("sec2", "Section 2");
        s2.addItem(item("item-1", "CODE-B")); // same itemId as s1

        assertThrows(DomainStateConflictException.class, () -> ss.addSection(s2));
    }

    @Test
    void addSection_crossSectionDuplicateCode_shouldThrow() {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft("ss1", "p1");

        ScoreSheetSection s1 = new ScoreSheetSection("sec1", "Section 1");
        s1.addItem(item("item-1", "CODE-A"));
        ss.addSection(s1);

        ScoreSheetSection s2 = new ScoreSheetSection("sec2", "Section 2");
        s2.addItem(item("item-2", "CODE-A")); // same code as s1

        assertThrows(DomainStateConflictException.class, () -> ss.addSection(s2));
    }

    @Test
    void addSection_noCrossConflict_shouldSucceed() {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft("ss1", "p1");

        ScoreSheetSection s1 = new ScoreSheetSection("sec1", "Section 1");
        s1.addItem(item("item-1", "CODE-A"));
        ss.addSection(s1);

        ScoreSheetSection s2 = new ScoreSheetSection("sec2", "Section 2");
        s2.addItem(item("item-2", "CODE-B")); // distinct itemId and code

        assertDoesNotThrow(() -> ss.addSection(s2));
        assertEquals(2, ss.getSections().size());
    }
}
```

- [ ] **Step 2: Run tests to verify they FAIL**

```bash
./gradlew test --tests "*.ScoreSheetDefinitionTest"
```

Expected: `addSection_crossSectionDuplicateItemId_shouldThrow` and `addSection_crossSectionDuplicateCode_shouldThrow` FAIL (no exception thrown). `addSection_noCrossConflict_shouldSucceed` PASSES.

- [ ] **Step 3: Fix ScoreSheetDefinition.addSection()**

Add these imports at the top of `ScoreSheetDefinition.java`:

```java
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import java.util.Set;
import java.util.stream.Collectors;
```

Replace the full `addSection()` method:

```java
public void addSection(ScoreSheetSection section) {
    requireDraft();
    Objects.requireNonNull(section, "section must not be null");

    boolean duplicateSectionId = sections.stream()
            .anyMatch(s -> s.getSectionId().equals(section.getSectionId()));
    if (duplicateSectionId) {
        throw new DomainStateConflictException("INVALID_STATE_TRANSITION",
                "Duplicate sectionId: " + section.getSectionId());
    }

    Set<String> existingItemIds = sections.stream()
            .flatMap(s -> s.getItems().stream())
            .map(ScoreItem::getItemId)
            .collect(Collectors.toSet());
    Set<String> existingCodes = sections.stream()
            .flatMap(s -> s.getItems().stream())
            .map(ScoreItem::getCode)
            .collect(Collectors.toSet());

    for (ScoreItem item : section.getItems()) {
        if (existingItemIds.contains(item.getItemId())) {
            throw new DomainStateConflictException("DUPLICATE_ITEM_ID",
                    "Item ID already exists in another section: " + item.getItemId());
        }
        if (existingCodes.contains(item.getCode())) {
            throw new DomainStateConflictException("DUPLICATE_ITEM_CODE",
                    "Item code already exists in another section: " + item.getCode());
        }
    }

    this.sections.add(section);
}
```

- [ ] **Step 4: Run all scoresheet tests to verify they PASS**

```bash
./gradlew test --tests "*.ScoreSheetDefinitionTest"
```

Expected: All 3 tests PASS.

- [ ] **Step 5: Run full test suite**

```bash
./gradlew test
```

Expected: All tests PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/tzuchi/his/quality/inspection/domain/scoresheet/ScoreSheetDefinition.java
git add src/test/java/com/tzuchi/his/quality/inspection/domain/scoresheet/ScoreSheetDefinitionTest.java
git commit -m "fix(H-001): add cross-section itemId/code uniqueness guard in ScoreSheetDefinition; add T-002

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 8: ReviewProject.reconstitute() + Repository Fix + T-004

**Files:**
- Modify: `main/java/com/tzuchi/his/quality/inspection/domain/project/ReviewProject.java`
- Modify: `main/java/com/tzuchi/his/quality/inspection/infrastructure/persistence/project/ReviewProjectRepositoryImpl.java`
- Create: `test/java/com/tzuchi/his/quality/inspection/infrastructure/persistence/project/ReviewProjectIntegrationTest.java`

**Bug (H-006):** `ReviewProjectRepositoryImpl.toDomain()` uses `alignVersion()` and `alignStatus()` — helper methods that replay `publishNewVersion()` and `activate()` N times to reach the persisted state. These domain methods trigger invariant checks and side effects, making reconstitution fragile and expensive. Fix by adding a `reconstitute()` static factory to `ReviewProject`.

**Also fix exception types** in `ReviewProject`:
- `publishNewVersion()` already published → `DomainStateConflictException("INVALID_STATE_TRANSITION", ...)`
- `activate()` no published version → `DomainStateConflictException("INVALID_STATE_TRANSITION", ...)`
- `requireDraft()` → `DomainStateConflictException("INVALID_STATE_TRANSITION", ...)`

- [ ] **Step 1: Write T-004 test (will fail before fix)**

```java
package com.tzuchi.his.quality.inspection.infrastructure.persistence.project;

import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.EnumSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ReviewProjectIntegrationTest {

    @Autowired
    private ReviewProjectRepository repository;

    @Test
    void reconstitution_versionAndStatus_shouldBeCorrect() {
        // 1. Create a project and publish its first version, then prepare a second draft
        String projectId = UUID.randomUUID().toString();
        ReviewProject project = ReviewProject.create(projectId, "H001", "Reconstitute Test",
                EnumSet.of(MedicalRecordType.OUTPATIENT));

        project.publishNewVersion(); // v1 published
        project.prepareDraftVersion(); // v2 draft

        repository.save(project);

        // 2. Reload from DB
        ReviewProject loaded = repository.findById(projectId)
                .orElseThrow(() -> new AssertionError("Project not found after save"));

        // 3. Assert state is correctly restored without replaying domain methods
        assertEquals(2, loaded.getCurrentVersion().getVersionNumber(),
                "Version number must be 2 after publish+prepareDraft");
        assertFalse(loaded.getCurrentVersion().isPublished(),
                "Version 2 must be a draft");
        assertEquals(project.getStatus(), loaded.getStatus());
        assertEquals("H001", loaded.getHospitalId());
    }
}
```

- [ ] **Step 2: Run T-004 to verify it FAILS (or passes by coincidence — check the version assertion)**

```bash
./gradlew test --tests "*.ReviewProjectIntegrationTest"
```

Expected: FAIL — current `alignVersion()` logic replays domain methods incorrectly for v2-draft state.

- [ ] **Step 3: Add reconstitute() to ReviewProject and fix exception types**

Replace the full content of `ReviewProject.java` with:

```java
package com.tzuchi.his.quality.inspection.domain.project;

import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;

import java.util.*;

/**
 * Aggregate root for ReviewProject.
 *
 * Invariants:
 * - An unpublished project cannot execute sampling.
 * - A published version is immutable.
 * - Version publication creates a new immutable snapshot.
 */
public class ReviewProject {

    private String projectId;
    private String hospitalId;
    private String name;
    private Set<MedicalRecordType> medicalRecordTypes;
    private ProjectStatus status;
    private ProjectVersion currentVersion;

    protected ReviewProject() {
        // for JPA
    }

    private ReviewProject(String projectId, String hospitalId, String name,
                          Set<MedicalRecordType> medicalRecordTypes) {
        Objects.requireNonNull(projectId, "projectId must not be null");
        Objects.requireNonNull(hospitalId, "hospitalId must not be null");
        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Project name must not be blank");
        }
        this.projectId = projectId;
        this.hospitalId = hospitalId;
        this.name = name;
        this.medicalRecordTypes = (medicalRecordTypes != null && !medicalRecordTypes.isEmpty())
                ? EnumSet.copyOf(medicalRecordTypes)
                : EnumSet.noneOf(MedicalRecordType.class);
        this.status = ProjectStatus.DRAFT;
        this.currentVersion = new ProjectVersion(1, false);
    }

    public static ReviewProject create(String projectId, String hospitalId, String name,
                                        Set<MedicalRecordType> medicalRecordTypes) {
        return new ReviewProject(projectId, hospitalId, name, medicalRecordTypes);
    }

    /**
     * Reconstitutes a ReviewProject from persisted state.
     * FOR INFRASTRUCTURE USE ONLY — do not call from application or domain code.
     * Assigns fields directly without triggering domain method invariants.
     */
    public static ReviewProject reconstitute(String projectId, String hospitalId, String name,
                                              Set<MedicalRecordType> medicalRecordTypes,
                                              ProjectStatus status, int versionNumber,
                                              boolean versionPublished) {
        ReviewProject project = new ReviewProject();
        project.projectId = projectId;
        project.hospitalId = hospitalId;
        project.name = name;
        project.medicalRecordTypes = (medicalRecordTypes != null && !medicalRecordTypes.isEmpty())
                ? EnumSet.copyOf(medicalRecordTypes)
                : EnumSet.noneOf(MedicalRecordType.class);
        project.status = status;
        project.currentVersion = new ProjectVersion(versionNumber, versionPublished);
        return project;
    }

    public void activate() {
        if (this.status == ProjectStatus.ACTIVE) {
            return;
        }
        if (!this.currentVersion.isPublished()) {
            throw new DomainStateConflictException("INVALID_STATE_TRANSITION",
                    "Cannot activate project without a published version");
        }
        this.status = ProjectStatus.ACTIVE;
    }

    public void deactivate() {
        if (this.status == ProjectStatus.DEACTIVATED || this.status == ProjectStatus.DRAFT) {
            return;
        }
        this.status = ProjectStatus.DEACTIVATED;
    }

    public void publishNewVersion() {
        if (this.currentVersion.isPublished()) {
            throw new DomainStateConflictException("INVALID_STATE_TRANSITION",
                    "Current version is already published. Cannot re-publish without creating a new draft.");
        }
        this.currentVersion = this.currentVersion.publish();
    }

    public void prepareDraftVersion() {
        if (!this.currentVersion.isPublished()) {
            throw new DomainStateConflictException("INVALID_STATE_TRANSITION",
                    "A draft version already exists");
        }
        this.currentVersion = this.currentVersion.nextDraft();
    }

    public void updateName(String name) {
        requireDraft();
        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Project name must not be blank");
        }
        this.name = name;
    }

    private void requireDraft() {
        if (this.currentVersion.isPublished()) {
            throw new DomainStateConflictException("INVALID_STATE_TRANSITION",
                    "Cannot modify a published version");
        }
    }

    // --- Getters ---

    public String getProjectId() { return projectId; }
    public String getHospitalId() { return hospitalId; }
    public String getName() { return name; }
    public Set<MedicalRecordType> getMedicalRecordTypes() { return Collections.unmodifiableSet(medicalRecordTypes); }
    public ProjectStatus getStatus() { return status; }
    public ProjectVersion getCurrentVersion() { return currentVersion; }
}
```

- [ ] **Step 4: Fix ReviewProjectRepositoryImpl.toDomain() to use reconstitute()**

Replace the `toDomain()` method (remove `alignVersion` and `alignStatus` helper methods entirely):

```java
private ReviewProject toDomain(ReviewProjectEntity e) {
    Set<MedicalRecordType> types = e.getMedicalRecordTypes().stream()
            .map(MedicalRecordType::valueOf)
            .collect(java.util.stream.Collectors.toSet());
    return ReviewProject.reconstitute(
            e.getProjectId(), e.getHospitalId(), e.getName(), types,
            e.getStatus(), e.getVersionNumber(), e.isVersionPublished());
}
```

Delete the `alignVersion()` and `alignStatus()` methods from `ReviewProjectRepositoryImpl`.

- [ ] **Step 5: Run T-004 and all tests to verify they PASS**

```bash
./gradlew test
```

Expected: All tests PASS including `ReviewProjectIntegrationTest`.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/tzuchi/his/quality/inspection/domain/project/ReviewProject.java
git add src/main/java/com/tzuchi/his/quality/inspection/infrastructure/persistence/project/ReviewProjectRepositoryImpl.java
git add src/test/java/com/tzuchi/his/quality/inspection/infrastructure/persistence/project/ReviewProjectIntegrationTest.java
git commit -m "fix(H-006): add ReviewProject.reconstitute(); remove alignVersion/alignStatus anti-pattern; fix exception types; add T-004

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 9: ScoreSheetDefinition.reconstitute() + Repository Fix

**Files:**
- Modify: `main/java/com/tzuchi/his/quality/inspection/domain/scoresheet/ScoreSheetDefinition.java`
- Modify: `main/java/com/tzuchi/his/quality/inspection/infrastructure/persistence/scoresheet/ScoreSheetRepositoryImpl.java`

**Bug (H-002):** `ScoreSheetRepositoryImpl.toDomain()` calls `ScoreSheetDefinition.createDraft()` which hardcodes `version = 1`, and then replays `addSection()` + `publishVersion()`. This loses the actual persisted version number and re-triggers invariant checks.

**Also fix exception types** in `ScoreSheetDefinition`:
- `requireDraft()` → `DomainStateConflictException("INVALID_STATE_TRANSITION", ...)`
- `publishVersion()` empty/no-items → `DomainRuleViolationException("SCORE_SHEET_NOT_PUBLISHABLE", ...)`

- [ ] **Step 1: Add reconstitute() to ScoreSheetDefinition and fix exception types**

Add these imports to `ScoreSheetDefinition.java`:

```java
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import java.util.stream.Collectors;
```

Add this `reconstitute()` static factory (below `createDraft()`):

```java
/**
 * Reconstitutes a ScoreSheetDefinition from persisted state.
 * FOR INFRASTRUCTURE USE ONLY — assigns fields directly without triggering invariants.
 */
public static ScoreSheetDefinition reconstitute(String scoreSheetId, String projectId,
                                                  ScoreSheetStatus status, int version,
                                                  List<ScoreSheetSection> sections) {
    ScoreSheetDefinition ss = new ScoreSheetDefinition();
    ss.scoreSheetId = scoreSheetId;
    ss.projectId = projectId;
    ss.status = status;
    ss.version = version;
    ss.sections = new ArrayList<>(sections);
    return ss;
}
```

Replace `requireDraft()` and `publishVersion()` with typed exceptions:

```java
public void publishVersion() {
    requireDraft();
    if (sections.isEmpty()) {
        throw new DomainRuleViolationException("SCORE_SHEET_NOT_PUBLISHABLE",
                "Cannot publish an empty score sheet");
    }
    boolean hasItems = sections.stream().anyMatch(s -> !s.getItems().isEmpty());
    if (!hasItems) {
        throw new DomainRuleViolationException("SCORE_SHEET_NOT_PUBLISHABLE",
                "Cannot publish a score sheet with no items");
    }
    this.status = ScoreSheetStatus.PUBLISHED;
}

private void requireDraft() {
    if (this.status == ScoreSheetStatus.PUBLISHED) {
        throw new DomainStateConflictException("INVALID_STATE_TRANSITION",
                "Cannot modify a published score sheet version");
    }
}
```

- [ ] **Step 2: Fix ScoreSheetRepositoryImpl.toDomain() to use reconstitute()**

Replace the `toDomain()` method in `ScoreSheetRepositoryImpl.java`:

```java
private ScoreSheetDefinition toDomain(ScoreSheetEntity e) {
    List<ScoreSheetSection> sections = e.getSections().stream()
            .map(this::toDomainSection)
            .collect(java.util.stream.Collectors.toList());
    return ScoreSheetDefinition.reconstitute(
            e.getScoreSheetId(), e.getProjectId(),
            e.getStatus(), e.getVersion(), sections);
}

private ScoreSheetSection toDomainSection(ScoreSheetSectionEntity se) {
    ScoreSheetSection section = new ScoreSheetSection(se.getSectionId(), se.getTitle());
    for (ScoreItemEntity ie : se.getItems()) {
        section.addItem(new ScoreItem(
                ie.getItemId(), ie.getCode(), ie.getTitle(),
                ScoreType.valueOf(ie.getScoreType()),
                ie.getMaxScore(), ie.getWeight()
        ));
    }
    return section;
}
```

Remove the old `toDomain()` body (the one that called `createDraft()`, `addSection()`, and `publishVersion()`).

- [ ] **Step 3: Run tests**

```bash
./gradlew test
```

Expected: All tests PASS.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/tzuchi/his/quality/inspection/domain/scoresheet/ScoreSheetDefinition.java
git add src/main/java/com/tzuchi/his/quality/inspection/infrastructure/persistence/scoresheet/ScoreSheetRepositoryImpl.java
git commit -m "fix(H-002): add ScoreSheetDefinition.reconstitute(); fix reconstitution bug (version always 1); fix exception types

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 10: SamplingBatch.reconstitute() + failureReason + Repository Fix

**Files:**
- Modify: `main/java/com/tzuchi/his/quality/inspection/domain/sampling/SamplingBatch.java`
- Modify: `main/java/com/tzuchi/his/quality/inspection/infrastructure/persistence/sampling/SamplingBatchEntity.java`
- Modify: `main/java/com/tzuchi/his/quality/inspection/infrastructure/persistence/sampling/SamplingBatchRepositoryImpl.java`

**Bug (H-003):** `SamplingBatchRepositoryImpl.toDomain()` has `if (e.getPopulationCount() > 0 || e.getEligibleCount() > 0)` guard that skips `recordCandidateCount()` for zero-candidate batches, losing that count data.

**Also fix (M-001):** `SamplingBatch.fail(String reason)` drops the `reason` argument — it never stores it. Add `failureReason` field.

**Also fix exception types** in `SamplingBatch`:
- `selectCases()` duplicate → `DomainStateConflictException("DUPLICATE_SELECTED_CASE_IN_BATCH", ...)`
- `complete()`, `fail()`, `requireNotCompleted()` → `DomainStateConflictException("INVALID_STATE_TRANSITION", ...)`

- [ ] **Step 1: Add failureReason field and fix fail() in SamplingBatch**

Add these imports to `SamplingBatch.java`:

```java
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
```

Add field after `retryOfBatchId`:

```java
private String failureReason;
```

Add getter after `getRetryOfBatchId()`:

```java
public String getFailureReason() { return failureReason; }
```

Replace `fail()`:

```java
public void fail(String reason) {
    if (this.status == BatchStatus.COMPLETED) {
        throw new DomainStateConflictException("INVALID_STATE_TRANSITION",
                "Cannot fail a completed batch");
    }
    this.status = BatchStatus.FAILED;
    this.failureReason = reason;
    this.finishedAt = Instant.now();
}
```

Replace `requireNotCompleted()`:

```java
private void requireNotCompleted() {
    if (this.status == BatchStatus.COMPLETED) {
        throw new DomainStateConflictException("INVALID_STATE_TRANSITION",
                "Batch is already completed and immutable");
    }
    if (this.status == BatchStatus.FAILED) {
        throw new DomainStateConflictException("INVALID_STATE_TRANSITION",
                "Batch has failed and cannot be modified");
    }
}
```

In `selectCases()`, replace the `IllegalArgumentException` for duplicates:

```java
// Replace:
throw new IllegalArgumentException(
        "Duplicate sourceCaseKey in same batch: " + ref.getSourceCaseKey());

// With:
throw new DomainStateConflictException("DUPLICATE_SELECTED_CASE_IN_BATCH",
        "Duplicate sourceCaseKey in same batch: " + ref.getSourceCaseKey());
```

(There are two throw sites in `selectCases()` — replace both.)

In `complete()`, replace:

```java
// Replace:
throw new IllegalStateException("selectedCount exceeds eligibleCount");

// With:
throw new DomainStateConflictException("INVALID_STATE_TRANSITION",
        "selectedCount exceeds eligibleCount");
```

- [ ] **Step 2: Add reconstitute() to SamplingBatch**

Add this static factory to `SamplingBatch.java` after `start()`:

```java
/**
 * Reconstitutes a SamplingBatch from persisted state.
 * FOR INFRASTRUCTURE USE ONLY — assigns fields directly without triggering domain invariants.
 */
public static SamplingBatch reconstitute(String batchId, String projectId,
                                          ExecutionMode executionMode, Instant startedAt,
                                          Instant finishedAt, BatchStatus status,
                                          RuleSnapshot ruleSnapshot, int populationCount,
                                          int eligibleCount, int excludedCount, int selectedCount,
                                          List<SelectedCaseRef> selectedCases,
                                          String retryOfBatchId, String failureReason) {
    SamplingBatch batch = new SamplingBatch();
    batch.batchId = batchId;
    batch.projectId = projectId;
    batch.executionMode = executionMode;
    batch.startedAt = startedAt;
    batch.finishedAt = finishedAt;
    batch.status = status;
    batch.ruleSnapshot = ruleSnapshot;
    batch.populationCount = populationCount;
    batch.eligibleCount = eligibleCount;
    batch.excludedCount = excludedCount;
    batch.selectedCount = selectedCount;
    batch.selectedCases = new ArrayList<>(selectedCases);
    batch.retryOfBatchId = retryOfBatchId;
    batch.failureReason = failureReason;
    return batch;
}
```

- [ ] **Step 3: Add failureReason column to SamplingBatchEntity**

In `SamplingBatchEntity.java`, add field after `retryOfBatchId`:

```java
@Column(name = "failure_reason")
private String failureReason;
```

Add getter and setter:

```java
public String getFailureReason() { return failureReason; }
public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
```

- [ ] **Step 4: Fix SamplingBatchRepositoryImpl.toDomain() to use reconstitute()**

Replace the entire `toDomain()` method in `SamplingBatchRepositoryImpl.java`:

```java
private SamplingBatch toDomain(SamplingBatchEntity e) {
    RuleSnapshot snapshot = new RuleSnapshot(
            e.getRuleSnapshotProject(), e.getRuleSnapshotPolicy());

    List<SelectedCaseRef> refs = e.getSelectedCases().stream()
            .map(re -> new SelectedCaseRef(
                    re.getSourceCaseKey(), re.getHospitalId(),
                    MedicalRecordType.valueOf(re.getMedicalRecordType())))
            .collect(java.util.stream.Collectors.toList());

    return SamplingBatch.reconstitute(
            e.getBatchId(), e.getProjectId(), e.getExecutionMode(),
            e.getStartedAt(), e.getFinishedAt(), e.getStatus(),
            snapshot, e.getPopulationCount(), e.getEligibleCount(),
            e.getExcludedCount(), e.getSelectedCount(), refs,
            e.getRetryOfBatchId(), e.getFailureReason());
}
```

Also update `toEntity()` to persist `failureReason`:

```java
// Add inside toEntity() after e.setRetryOfBatchId(...):
e.setFailureReason(b.getFailureReason());
```

- [ ] **Step 5: Run tests**

```bash
./gradlew test
```

Expected: All tests PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/tzuchi/his/quality/inspection/domain/sampling/SamplingBatch.java
git add src/main/java/com/tzuchi/his/quality/inspection/infrastructure/persistence/sampling/SamplingBatchEntity.java
git add src/main/java/com/tzuchi/his/quality/inspection/infrastructure/persistence/sampling/SamplingBatchRepositoryImpl.java
git commit -m "fix(H-003/M-001): add SamplingBatch.reconstitute(); remove if-count guard; add failureReason; fix exception types

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 11: H-005 — SelectedCaseRefEntity Unique Constraint + batch_id Column

**Files:**
- Modify: `main/java/com/tzuchi/his/quality/inspection/infrastructure/persistence/sampling/SelectedCaseRefEntity.java`

**Bug (H-005):** `SelectedCaseRefEntity` has no explicit `batch_id` column (the FK is managed by JPA via `@JoinColumn` on the parent `SamplingBatchEntity`), and there is no `(batch_id, source_case_key)` unique constraint. If the H2 ddl-auto schema is generated without this constraint, duplicate selected cases can be persisted silently.

**Note:** Table name `sampling_batch_selected_case` (M-003 fix) is included here since this is the same entity.

- [ ] **Step 1: Replace SelectedCaseRefEntity**

```java
package com.tzuchi.his.quality.inspection.infrastructure.persistence.sampling;

import javax.persistence.*;

@Entity
@Table(
    name = "sampling_batch_selected_case",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_selected_case_batch_key",
        columnNames = {"batch_id", "source_case_key"}
    )
)
public class SelectedCaseRefEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id", nullable = false)
    private String batchId;

    @Column(name = "source_case_key", nullable = false)
    private String sourceCaseKey;

    @Column(name = "hospital_id", nullable = false)
    private String hospitalId;

    @Column(name = "medical_record_type", nullable = false)
    private String medicalRecordType;

    protected SelectedCaseRefEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }

    public String getSourceCaseKey() { return sourceCaseKey; }
    public void setSourceCaseKey(String sourceCaseKey) { this.sourceCaseKey = sourceCaseKey; }

    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }

    public String getMedicalRecordType() { return medicalRecordType; }
    public void setMedicalRecordType(String medicalRecordType) { this.medicalRecordType = medicalRecordType; }
}
```

- [ ] **Step 2: Update SamplingBatchRepositoryImpl.toEntity() to set batchId on child entity**

In `SamplingBatchRepositoryImpl.toEntity()`, update the selected cases mapping to set the `batchId`:

```java
// Replace the selectedCases mapping in toEntity():
e.setSelectedCases(
        b.getSelectedCases().stream().map(ref -> {
            SelectedCaseRefEntity re = new SelectedCaseRefEntity();
            re.setBatchId(b.getBatchId());
            re.setSourceCaseKey(ref.getSourceCaseKey());
            re.setHospitalId(ref.getHospitalId());
            re.setMedicalRecordType(ref.getMedicalRecordType().name());
            return re;
        }).collect(java.util.stream.Collectors.toList())
);
```

- [ ] **Step 3: Run tests**

```bash
./gradlew test
```

Expected: All tests PASS.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/tzuchi/his/quality/inspection/infrastructure/persistence/sampling/SelectedCaseRefEntity.java
git add src/main/java/com/tzuchi/his/quality/inspection/infrastructure/persistence/sampling/SamplingBatchRepositoryImpl.java
git commit -m "fix(H-005): add batch_id column + unique constraint to SelectedCaseRefEntity; fix table name

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Phase 3 — Wire Auth + Medium/Low/Tests/Spec

### Task 12: Wire ActorContext into All 7 Use Cases + 3 Controllers + Update SamplingUseCaseTest

**Files:**
- Modify: `main/.../application/project/CreateReviewProjectUseCase.java`
- Modify: `main/.../application/project/PublishReviewProjectVersionUseCase.java`
- Modify: `main/.../application/scoresheet/CreateScoreSheetDraftUseCase.java`
- Modify: `main/.../application/scoresheet/PublishScoreSheetVersionUseCase.java`
- Modify: `main/.../application/sampling/PreviewSamplingPopulationUseCase.java`
- Modify: `main/.../application/sampling/ExecuteSamplingUseCase.java`
- Modify: `main/.../application/sampling/RetrySamplingBatchUseCase.java`
- Modify: `main/.../interfaces/rest/ReviewProjectController.java`
- Modify: `main/.../interfaces/rest/ScoreSheetController.java`
- Modify: `main/.../interfaces/rest/SamplingController.java`
- Modify: `test/.../application/sampling/SamplingUseCaseTest.java`

Per permission-matrix.md and spec §3.4: all B1/B2 use cases require `HOSPITAL_MANAGER`. Add `ActorContext actor` as the first parameter and call `actor.requireRole(DomainRole.HOSPITAL_MANAGER)` as the first line.

- [ ] **Step 1: Update CreateReviewProjectUseCase**

```java
package com.tzuchi.his.quality.inspection.application.project;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class CreateReviewProjectUseCase {

    private final ReviewProjectRepository repository;

    public CreateReviewProjectUseCase(ReviewProjectRepository repository) {
        this.repository = repository;
    }

    public String execute(ActorContext actor, String hospitalId, String name,
                          Set<MedicalRecordType> medicalRecordTypes) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        String projectId = UUID.randomUUID().toString();
        ReviewProject project = ReviewProject.create(projectId, hospitalId, name, medicalRecordTypes);
        repository.save(project);
        return projectId;
    }
}
```

- [ ] **Step 2: Update PublishReviewProjectVersionUseCase**

```java
package com.tzuchi.his.quality.inspection.application.project;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PublishReviewProjectVersionUseCase {

    private final ReviewProjectRepository repository;

    public PublishReviewProjectVersionUseCase(ReviewProjectRepository repository) {
        this.repository = repository;
    }

    public void execute(ActorContext actor, String projectId) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        ReviewProject project = repository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        project.publishNewVersion();
        repository.save(project);
    }
}
```

- [ ] **Step 3: Update CreateScoreSheetDraftUseCase**

```java
package com.tzuchi.his.quality.inspection.application.scoresheet;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetDefinition;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class CreateScoreSheetDraftUseCase {

    private final ScoreSheetRepository repository;

    public CreateScoreSheetDraftUseCase(ScoreSheetRepository repository) {
        this.repository = repository;
    }

    public String execute(ActorContext actor, String projectId) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        String scoreSheetId = UUID.randomUUID().toString();
        ScoreSheetDefinition scoreSheet = ScoreSheetDefinition.createDraft(scoreSheetId, projectId);
        repository.save(scoreSheet);
        return scoreSheetId;
    }
}
```

- [ ] **Step 4: Update PublishScoreSheetVersionUseCase**

```java
package com.tzuchi.his.quality.inspection.application.scoresheet;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetDefinition;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PublishScoreSheetVersionUseCase {

    private final ScoreSheetRepository repository;

    public PublishScoreSheetVersionUseCase(ScoreSheetRepository repository) {
        this.repository = repository;
    }

    public void execute(ActorContext actor, String scoreSheetId) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        ScoreSheetDefinition scoreSheet = repository.findById(scoreSheetId)
                .orElseThrow(() -> new IllegalArgumentException("ScoreSheet not found: " + scoreSheetId));
        scoreSheet.publishVersion();
        repository.save(scoreSheet);
    }
}
```

- [ ] **Step 5: Update PreviewSamplingPopulationUseCase** (also fix exception type)

```java
package com.tzuchi.his.quality.inspection.application.sampling;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.project.ProjectStatus;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import com.tzuchi.his.quality.inspection.domain.sampling.SelectedCaseRef;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Preview sampling population without creating a batch.
 * Per domain model: preview does NOT create a SamplingBatch.
 */
@Service
public class PreviewSamplingPopulationUseCase {

    private final ReviewProjectRepository projectRepository;
    private final CandidateSourcePort candidateSource;

    public PreviewSamplingPopulationUseCase(ReviewProjectRepository projectRepository,
                                             CandidateSourcePort candidateSource) {
        this.projectRepository = projectRepository;
        this.candidateSource = candidateSource;
    }

    public SamplingPreviewResult execute(ActorContext actor, String projectId) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);

        ReviewProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        if (project.getStatus() != ProjectStatus.ACTIVE) {
            throw new DomainRuleViolationException("PROJECT_NOT_ACTIVE",
                    "Project must be ACTIVE for sampling preview. Current status: " + project.getStatus());
        }

        List<MedicalRecordType> types = new ArrayList<>(project.getMedicalRecordTypes());
        List<SelectedCaseRef> candidates = candidateSource.queryCandidates(
                projectId, project.getHospitalId(), types);

        int populationCount = candidates.size();
        int excludedCount = 0;
        int eligibleCount = populationCount - excludedCount;

        return new SamplingPreviewResult(populationCount, eligibleCount, excludedCount);
    }
}
```

- [ ] **Step 6: Update ExecuteSamplingUseCase** (also fix exception types)

```java
package com.tzuchi.his.quality.inspection.application.sampling;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.project.ProjectStatus;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import com.tzuchi.his.quality.inspection.domain.sampling.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/** Execute sampling: create batch, snapshot, select cases, create ReviewCases. */
@Service
@Transactional
public class ExecuteSamplingUseCase {

    private final ReviewProjectRepository projectRepository;
    private final SamplingBatchRepository batchRepository;
    private final ReviewCaseRepository caseRepository;
    private final CandidateSourcePort candidateSource;

    public ExecuteSamplingUseCase(ReviewProjectRepository projectRepository,
                                  SamplingBatchRepository batchRepository,
                                  ReviewCaseRepository caseRepository,
                                  CandidateSourcePort candidateSource) {
        this.projectRepository = projectRepository;
        this.batchRepository = batchRepository;
        this.caseRepository = caseRepository;
        this.candidateSource = candidateSource;
    }

    public String execute(ActorContext actor, String projectId) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);

        ReviewProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        if (project.getStatus() != ProjectStatus.ACTIVE) {
            throw new DomainRuleViolationException("PROJECT_NOT_ACTIVE",
                    "Project must be ACTIVE for sampling execution. Current status: " + project.getStatus());
        }
        if (!project.getCurrentVersion().isPublished()) {
            throw new DomainStateConflictException("INVALID_STATE_TRANSITION",
                    "Project must have a published version before executing sampling");
        }

        RuleSnapshot snapshot = new RuleSnapshot(
                "project=" + projectId + ",version=" + project.getCurrentVersion().getVersionNumber(),
                "default-sampling-policy"
        );

        String batchId = UUID.randomUUID().toString();
        SamplingBatch batch = SamplingBatch.start(batchId, projectId, ExecutionMode.MANUAL, snapshot);

        List<MedicalRecordType> types = new ArrayList<>(project.getMedicalRecordTypes());
        List<SelectedCaseRef> candidates = candidateSource.queryCandidates(
                projectId, project.getHospitalId(), types);

        int populationCount = candidates.size();
        int excludedCount = 0;
        int eligibleCount = populationCount - excludedCount;

        batch.recordCandidateCount(populationCount, eligibleCount, excludedCount);
        batch.selectCases(candidates);
        batch.complete();

        batchRepository.save(batch);

        List<ReviewCase> reviewCases = candidates.stream()
                .map(ref -> ReviewCase.createFromSampling(
                        UUID.randomUUID().toString(), batchId, projectId,
                        ref.getHospitalId(), ref.getSourceCaseKey(), ref.getMedicalRecordType()))
                .collect(Collectors.toList());

        caseRepository.saveAll(reviewCases);

        return batchId;
    }
}
```

- [ ] **Step 7: Update RetrySamplingBatchUseCase** (add ActorContext; C-001 fix already done in Task 6)

Add `ActorContext actor` as first param and `actor.requireRole(DomainRole.HOSPITAL_MANAGER)` as the first line. The snapshot fix and exception type fix were already done in Task 6 — only add the actor parameter here.

Replace `public String execute(String previousBatchId)` with:

```java
public String execute(ActorContext actor, String previousBatchId) {
    actor.requireRole(DomainRole.HOSPITAL_MANAGER);
    // ... rest of method unchanged
```

Also add this import at the top:
```java
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
```

- [ ] **Step 8: Update ReviewProjectController**

```java
package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.project.CreateReviewProjectUseCase;
import com.tzuchi.his.quality.inspection.application.project.PublishReviewProjectVersionUseCase;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.interfaces.rest.dto.ApiResponse;
import com.tzuchi.his.quality.inspection.interfaces.rest.dto.CreateReviewProjectRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/review-projects")
public class ReviewProjectController {

    private final CreateReviewProjectUseCase createUseCase;
    private final PublishReviewProjectVersionUseCase publishUseCase;

    public ReviewProjectController(CreateReviewProjectUseCase createUseCase,
                                   PublishReviewProjectVersionUseCase publishUseCase) {
        this.createUseCase = createUseCase;
        this.publishUseCase = publishUseCase;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> createProject(
            @CurrentActor ActorContext actor,
            @Valid @RequestBody CreateReviewProjectRequest request) {
        Set<MedicalRecordType> types = request.getMedicalRecordTypes() != null
                ? request.getMedicalRecordTypes().stream()
                    .map(s -> {
                        try { return MedicalRecordType.valueOf(s); }
                        catch (IllegalArgumentException e) {
                            throw new BadRequestException("Invalid medicalRecordType: " + s);
                        }
                    })
                    .collect(Collectors.toSet())
                : Collections.emptySet();

        String projectId = createUseCase.execute(actor, request.getHospitalId(),
                request.getName(), types);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(Map.of("projectId", projectId)));
    }

    @PostMapping("/{projectId}/publish-version")
    public ResponseEntity<ApiResponse<Void>> publishVersion(
            @CurrentActor ActorContext actor,
            @PathVariable String projectId) {
        publishUseCase.execute(actor, projectId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
```

- [ ] **Step 9: Update ScoreSheetController**

```java
package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.scoresheet.CreateScoreSheetDraftUseCase;
import com.tzuchi.his.quality.inspection.application.scoresheet.PublishScoreSheetVersionUseCase;
import com.tzuchi.his.quality.inspection.interfaces.rest.dto.ApiResponse;
import com.tzuchi.his.quality.inspection.interfaces.rest.dto.CreateScoreSheetDraftRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/score-sheets")
public class ScoreSheetController {

    private final CreateScoreSheetDraftUseCase createDraftUseCase;
    private final PublishScoreSheetVersionUseCase publishUseCase;

    public ScoreSheetController(CreateScoreSheetDraftUseCase createDraftUseCase,
                                PublishScoreSheetVersionUseCase publishUseCase) {
        this.createDraftUseCase = createDraftUseCase;
        this.publishUseCase = publishUseCase;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> createDraft(
            @CurrentActor ActorContext actor,
            @RequestBody CreateScoreSheetDraftRequest request) {
        String scoreSheetId = createDraftUseCase.execute(actor, request.getProjectId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(Map.of("scoreSheetId", scoreSheetId)));
    }

    @PostMapping("/{scoreSheetId}/publish-version")
    public ResponseEntity<ApiResponse<Void>> publishVersion(
            @CurrentActor ActorContext actor,
            @PathVariable String scoreSheetId) {
        publishUseCase.execute(actor, scoreSheetId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
```

- [ ] **Step 10: Update SamplingController**

```java
package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.sampling.ExecuteSamplingUseCase;
import com.tzuchi.his.quality.inspection.application.sampling.PreviewSamplingPopulationUseCase;
import com.tzuchi.his.quality.inspection.application.sampling.RetrySamplingBatchUseCase;
import com.tzuchi.his.quality.inspection.application.sampling.SamplingPreviewResult;
import com.tzuchi.his.quality.inspection.interfaces.rest.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class SamplingController {

    private final PreviewSamplingPopulationUseCase previewUseCase;
    private final ExecuteSamplingUseCase executeUseCase;
    private final RetrySamplingBatchUseCase retryUseCase;

    public SamplingController(PreviewSamplingPopulationUseCase previewUseCase,
                               ExecuteSamplingUseCase executeUseCase,
                               RetrySamplingBatchUseCase retryUseCase) {
        this.previewUseCase = previewUseCase;
        this.executeUseCase = executeUseCase;
        this.retryUseCase = retryUseCase;
    }

    @PostMapping("/review-projects/{projectId}/sampling/preview")
    public ResponseEntity<ApiResponse<SamplingPreviewResult>> previewSampling(
            @CurrentActor ActorContext actor,
            @PathVariable String projectId) {
        SamplingPreviewResult result = previewUseCase.execute(actor, projectId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/review-projects/{projectId}/sampling/execute")
    public ResponseEntity<ApiResponse<Map<String, String>>> executeSampling(
            @CurrentActor ActorContext actor,
            @PathVariable String projectId) {
        String batchId = executeUseCase.execute(actor, projectId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(Map.of("batchId", batchId)));
    }

    @PostMapping("/sampling-batches/{batchId}/retry")
    public ResponseEntity<ApiResponse<Map<String, String>>> retrySampling(
            @CurrentActor ActorContext actor,
            @PathVariable String batchId) {
        String newBatchId = retryUseCase.execute(actor, batchId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(Map.of("batchId", newBatchId)));
    }
}
```

- [ ] **Step 11: Update SamplingUseCaseTest — add ActorContext, fix exception assertions**

Add these imports to `SamplingUseCaseTest.java`:

```java
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import java.util.Set;
```

Add a helper field and setup in `@BeforeEach`:

```java
private ActorContext hospitalManagerActor;

// In setUp():
hospitalManagerActor = new ActorContext("test-user", "Test User", Set.of(DomainRole.HOSPITAL_MANAGER));
```

Update all `execute()` calls throughout the test class (add `hospitalManagerActor` as first arg):

```java
// previewUseCase calls:
previewUseCase.execute(hospitalManagerActor, "p1")

// executeUseCase calls:
executeUseCase.execute(hospitalManagerActor, "p1")

// retryUseCase calls:
retryUseCase.execute(hospitalManagerActor, "failed-b")
retryUseCase.execute(hospitalManagerActor, "done-b")
```

Update `execute_inactiveProject_shouldThrow` assertion:

```java
@Test
void execute_inactiveProject_shouldThrow() {
    ReviewProject project = ReviewProject.create("p1", "H1", "Test",
            EnumSet.of(MedicalRecordType.OUTPATIENT));
    projectRepo.save(project);

    assertThrows(DomainRuleViolationException.class,
            () -> executeUseCase.execute(hospitalManagerActor, "p1"));
}
```

- [ ] **Step 12: Run full test suite**

```bash
./gradlew test
```

Expected: All tests PASS.

- [ ] **Step 13: Commit**

```bash
git add src/main/java/com/tzuchi/his/quality/inspection/application/
git add src/main/java/com/tzuchi/his/quality/inspection/interfaces/rest/ReviewProjectController.java
git add src/main/java/com/tzuchi/his/quality/inspection/interfaces/rest/ScoreSheetController.java
git add src/main/java/com/tzuchi/his/quality/inspection/interfaces/rest/SamplingController.java
git add src/test/java/com/tzuchi/his/quality/inspection/application/sampling/SamplingUseCaseTest.java
git commit -m "feat: wire ActorContext into all 7 use cases + 3 controllers; fix PROJECT_NOT_ACTIVE exception type

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 13: L-001 @Valid + L-002 BadRequestException + T-006

**Files:**
- Modify: `main/.../interfaces/rest/dto/CreateReviewProjectRequest.java`
- Modify: `test/.../interfaces/rest/ReviewProjectApiTest.java`

**Note:** The `ReviewProjectController` changes for `@Valid` and `BadRequestException` were already applied in Task 12 Step 8. This task adds the DTO annotations and the test.

- [ ] **Step 1: Add validation annotations to CreateReviewProjectRequest**

```java
package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Set;

public class CreateReviewProjectRequest {

    @NotNull(message = "must not be null")
    @NotBlank(message = "must not be blank")
    private String hospitalId;

    @NotNull(message = "must not be null")
    @NotBlank(message = "must not be blank")
    private String name;

    @NotNull(message = "must not be null")
    private Set<String> medicalRecordTypes;

    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Set<String> getMedicalRecordTypes() { return medicalRecordTypes; }
    public void setMedicalRecordTypes(Set<String> medicalRecordTypes) { this.medicalRecordTypes = medicalRecordTypes; }
}
```

- [ ] **Step 2: Add T-006 test to ReviewProjectApiTest**

Add this test to `ReviewProjectApiTest.java`:

```java
@Test
void createProject_nullMedicalRecordTypes_shouldReturn400() throws Exception {
    // medicalRecordTypes is @NotNull — sending null must return 400
    String body = "{\"hospitalId\":\"H001\",\"name\":\"Test\",\"medicalRecordTypes\":null}";

    mockMvc.perform(post("/review-projects")
                    .header("api-key", TestTokenFactory.hospitalManagerToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
}
```

- [ ] **Step 3: Run tests**

```bash
./gradlew test
```

Expected: All tests PASS including T-006 (400 for null medicalRecordTypes).

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/tzuchi/his/quality/inspection/interfaces/rest/dto/CreateReviewProjectRequest.java
git add src/test/java/com/tzuchi/his/quality/inspection/interfaces/rest/ReviewProjectApiTest.java
git commit -m "fix(L-001/L-002/T-006): add @NotNull @NotBlank validation; BadRequestException for invalid MedicalRecordType; add T-006

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 14: M-003 Table Name Alignment

**Files:**
- Modify: `main/.../infrastructure/persistence/project/ReviewProjectEntity.java`
- Modify: `main/.../infrastructure/persistence/sampling/SamplingBatchEntity.java`
- Modify: `main/.../infrastructure/persistence/scoresheet/ScoreSheetEntity.java`
- Modify: `main/.../infrastructure/persistence/scoresheet/ScoreSheetSectionEntity.java`
- Modify: `main/.../infrastructure/persistence/scoresheet/ScoreItemEntity.java`
- Modify: `main/.../infrastructure/persistence/reviewcase/ReviewCaseEntity.java`

`SelectedCaseRefEntity` was already fixed in Task 11. This task fixes the remaining 6 entities to align with `persistence-model.md`.

| Entity | Old `@Table` name | New `@Table` name |
|---|---|---|
| ReviewProjectEntity | `review_projects` | `review_project` |
| SamplingBatchEntity | `sampling_batches` | `sampling_batch` |
| ScoreSheetEntity | `score_sheet_definitions` | `score_sheet` |
| ScoreSheetSectionEntity | `score_sheet_sections` | `score_sheet_section` |
| ScoreItemEntity | `score_items` | `score_sheet_item` |
| ReviewCaseEntity | `review_cases` | `review_case` |

- [ ] **Step 1: Fix ReviewProjectEntity table name**

In `ReviewProjectEntity.java`, change:
```java
@Table(name = "review_projects")
```
to:
```java
@Table(name = "review_project")
```

- [ ] **Step 2: Fix SamplingBatchEntity table name**

In `SamplingBatchEntity.java`, change:
```java
@Table(name = "sampling_batches")
```
to:
```java
@Table(name = "sampling_batch")
```

- [ ] **Step 3: Fix ScoreSheetEntity table name**

In `ScoreSheetEntity.java`, change:
```java
@Table(name = "score_sheet_definitions")
```
to:
```java
@Table(name = "score_sheet")
```

- [ ] **Step 4: Fix ScoreSheetSectionEntity table name**

In `ScoreSheetSectionEntity.java`, change:
```java
@Table(name = "score_sheet_sections")
```
to:
```java
@Table(name = "score_sheet_section")
```

- [ ] **Step 5: Fix ScoreItemEntity table name**

In `ScoreItemEntity.java`, change:
```java
@Table(name = "score_items")
```
to:
```java
@Table(name = "score_sheet_item")
```

- [ ] **Step 6: Fix ReviewCaseEntity table name**

Find `ReviewCaseEntity.java` in `infrastructure/persistence/reviewcase/`. Change its `@Table(name = ...)` to:
```java
@Table(name = "review_case")
```

- [ ] **Step 7: Run tests**

```bash
./gradlew test
```

Expected: All tests PASS. (ddl-auto: update in local profile still creates/updates tables with new names.)

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/tzuchi/his/quality/inspection/infrastructure/persistence/
git commit -m "fix(M-003): align all @Table names to persistence-model.md

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 15: M-005 Remove sourceCaseKeys + M-002 RuleSnapshot TODO + T-005

**Files:**
- Modify: `main/.../application/sampling/SamplingPreviewResult.java`
- Modify: `main/.../domain/sampling/RuleSnapshot.java`
- Modify: `test/.../application/sampling/SamplingUseCaseTest.java`

**M-005:** `SamplingPreviewResult` exposes `sourceCaseKeys: List<String>`, which leaks individual source case identifiers per api-spec.md §9.1. Remove this field.

**M-002:** `RuleSnapshot` has no TODO about its stub nature. Add a Javadoc TODO per decision-log.md D-011.

**T-005:** `PreviewSamplingPopulationUseCase` inactive project check currently has no test.

- [ ] **Step 1: Remove sourceCaseKeys from SamplingPreviewResult**

Replace `SamplingPreviewResult.java` with:

```java
package com.tzuchi.his.quality.inspection.application.sampling;

/**
 * Result of a sampling preview — population estimation without batch creation.
 * sourceCaseKeys is intentionally excluded (api-spec.md §9.1 — do not expose individual keys).
 */
public class SamplingPreviewResult {

    private final int populationCount;
    private final int eligibleCount;
    private final int excludedCount;

    public SamplingPreviewResult(int populationCount, int eligibleCount, int excludedCount) {
        this.populationCount = populationCount;
        this.eligibleCount = eligibleCount;
        this.excludedCount = excludedCount;
    }

    public int getPopulationCount() { return populationCount; }
    public int getEligibleCount() { return eligibleCount; }
    public int getExcludedCount() { return excludedCount; }
}
```

- [ ] **Step 2: Update PreviewSamplingPopulationUseCase constructor call**

In `PreviewSamplingPopulationUseCase.java`, the return statement now omits `keys`. The `keys` local variable can be removed entirely. Confirm the return line is:

```java
return new SamplingPreviewResult(populationCount, eligibleCount, excludedCount);
```

Remove the `keys` local variable and the `Collectors` import if it's no longer needed.

- [ ] **Step 3: Update existing SamplingUseCaseTest preview_shouldNotCreateBatch test**

In `SamplingUseCaseTest.java`, the `preview_shouldNotCreateBatch` test currently calls `result.getPopulationCount()` and `result.getEligibleCount()` — those still exist, so no change needed.

If the test references `result.getSourceCaseKeys()`, remove that assertion. With the field removed, the test will fail to compile if it references `getSourceCaseKeys()`. Remove any such reference.

- [ ] **Step 4: Add T-005 test to SamplingUseCaseTest**

```java
@Test
void preview_inactiveProject_shouldThrow() {
    ReviewProject project = ReviewProject.create("p-draft", "H1", "Draft Project",
            EnumSet.of(MedicalRecordType.OUTPATIENT));
    // Not published, not activated — status is DRAFT
    projectRepo.save(project);

    assertThrows(DomainRuleViolationException.class,
            () -> previewUseCase.execute(hospitalManagerActor, "p-draft"));
}
```

- [ ] **Step 5: Add TODO Javadoc to RuleSnapshot**

In `RuleSnapshot.java`, update the Javadoc:

```java
/**
 * Snapshot of the rules used at the time of batch execution.
 * Preserved for traceability — must not be modified after batch completion.
 *
 * TODO(D-011): This is a stub. Replace with a full immutable snapshot containing
 *              snapshot_json and checksum per decision-log.md D-011 and
 *              persistence-model.md §5.9 (sampling_batch_snapshot table).
 */
public class RuleSnapshot {
```

- [ ] **Step 6: Run tests**

```bash
./gradlew test
```

Expected: All tests PASS including T-005.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/tzuchi/his/quality/inspection/application/sampling/SamplingPreviewResult.java
git add src/main/java/com/tzuchi/his/quality/inspection/application/sampling/PreviewSamplingPopulationUseCase.java
git add src/main/java/com/tzuchi/his/quality/inspection/domain/sampling/RuleSnapshot.java
git add src/test/java/com/tzuchi/his/quality/inspection/application/sampling/SamplingUseCaseTest.java
git commit -m "fix(M-005/M-002): remove sourceCaseKeys from preview result; add RuleSnapshot TODO; add T-005

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 16: T-003 Tests + L-003 JoinColumn Fix + L-004 ddl-auto Profile

**Files:**
- Modify: `test/.../interfaces/rest/ReviewProjectApiTest.java`
- Modify: `main/.../infrastructure/persistence/scoresheet/ScoreSheetSectionEntity.java`
- Modify: `main/resources/application.yml`

- [ ] **Step 1: Add T-003 test to ReviewProjectApiTest**

T-003 verifies that business rule violations return 422 (not 409) and state conflicts return 409 (not a generic error). Add:

```java
@Test
void executeSampling_inactiveProject_shouldReturn422() throws Exception {
    // Create a DRAFT project — not published, not activated
    CreateReviewProjectRequest req = new CreateReviewProjectRequest();
    req.setHospitalId("H001");
    req.setName("T-003 Inactive Project");
    req.setMedicalRecordTypes(Set.of("OUTPATIENT"));

    MvcResult result = mockMvc.perform(post("/review-projects")
                    .header("api-key", TestTokenFactory.hospitalManagerToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andReturn();

    String projectId = objectMapper.readTree(
            result.getResponse().getContentAsString())
            .get("data").get("projectId").asText();

    // Execute sampling on DRAFT project → 422 PROJECT_NOT_ACTIVE (not 409)
    mockMvc.perform(post("/review-projects/" + projectId + "/sampling/execute")
                    .header("api-key", TestTokenFactory.hospitalManagerToken()))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.error.code").value("PROJECT_NOT_ACTIVE"));
}
```

The existing `publishVersion_alreadyPublished_shouldReturn409` test already covers the 409 case for T-003's second scenario (`alreadyPublished_shouldReturn409`). After Tasks 8+5, `DomainStateConflictException` is correctly mapped to 409 — that test continues to pass.

- [ ] **Step 2: Fix ScoreSheetSectionEntity @JoinColumn name (L-003)**

In `ScoreSheetSectionEntity.java`, change:

```java
@JoinColumn(name = "section_pk")
```

to:

```java
@JoinColumn(name = "section_id")
```

- [ ] **Step 3: Fix application.yml — move ddl-auto: update to local profile only (L-004)**

Replace the entire `application.yml` content:

```yaml
server:
  port: 8080
  servlet:
    context-path: /api/quality-inspection

spring:
  application:
    name: quality-inspection-service
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local}
  jpa:
    hibernate:
      ddl-auto: none  # Override per profile; never auto-update in production
    show-sql: false
    open-in-view: false

---
# Local development profile
spring:
  config:
    activate:
      on-profile: local
  datasource:
    url: jdbc:h2:mem:quality-inspection;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password:
  h2:
    console:
      enabled: true
      path: /h2-console
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

# NOTE: In production, this service sits behind the organization existing API Gateway.
# The API Gateway enforces auth via api-key header.
# In local development mode, direct calls or a local dev proxy may be used
# as a gateway-layer substitute. See AGENTS.md §9.4 for layered substitute strategy.
```

- [ ] **Step 4: Run tests**

```bash
./gradlew test
```

Expected: All tests PASS.

- [ ] **Step 5: Commit**

```bash
git add src/test/java/com/tzuchi/his/quality/inspection/interfaces/rest/ReviewProjectApiTest.java
git add src/main/java/com/tzuchi/his/quality/inspection/infrastructure/persistence/scoresheet/ScoreSheetSectionEntity.java
git add src/main/resources/application.yml
git commit -m "test(T-003): add inactiveProject_shouldReturn422; fix(L-003): JoinColumn name; fix(L-004): ddl-auto in local only

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 17: T-007 SamplingControllerApiTest

**Files:**
- Create: `test/java/com/tzuchi/his/quality/inspection/interfaces/rest/SamplingControllerApiTest.java`

This @SpringBootTest API test covers the complete HTTP contract for all 3 sampling endpoints, verifying response shape (not just status codes).

- [ ] **Step 1: Create SamplingControllerApiTest**

```java
package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tzuchi.his.quality.inspection.TestTokenFactory;
import com.tzuchi.his.quality.inspection.domain.sampling.BatchStatus;
import com.tzuchi.his.quality.inspection.domain.sampling.ExecutionMode;
import com.tzuchi.his.quality.inspection.domain.sampling.RuleSnapshot;
import com.tzuchi.his.quality.inspection.domain.sampling.SamplingBatch;
import com.tzuchi.his.quality.inspection.domain.sampling.SamplingBatchRepository;
import com.tzuchi.his.quality.inspection.interfaces.rest.dto.CreateReviewProjectRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SamplingControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SamplingBatchRepository batchRepository;

    @Test
    void previewSampling_activeProject_shouldReturn200WithCounts() throws Exception {
        String projectId = createAndActivateProject("Preview Test");

        mockMvc.perform(post("/review-projects/" + projectId + "/sampling/preview")
                        .header("api-key", TestTokenFactory.hospitalManagerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.populationCount").exists())
                .andExpect(jsonPath("$.data.eligibleCount").exists())
                .andExpect(jsonPath("$.data.excludedCount").exists())
                .andExpect(jsonPath("$.data.sourceCaseKeys").doesNotExist());
    }

    @Test
    void executeSampling_activeProject_shouldReturn201WithBatchId() throws Exception {
        String projectId = createAndActivateProject("Execute Test");

        mockMvc.perform(post("/review-projects/" + projectId + "/sampling/execute")
                        .header("api-key", TestTokenFactory.hospitalManagerToken()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.batchId").isNotEmpty());
    }

    @Test
    void retrySampling_failedBatch_shouldReturn201WithNewBatchId() throws Exception {
        String projectId = createAndActivateProject("Retry Test");

        // Save a FAILED batch directly via domain repository
        SamplingBatch failedBatch = SamplingBatch.start(
                "test-failed-batch", projectId, ExecutionMode.MANUAL,
                new RuleSnapshot("project=" + projectId + ",version=1", "policy"));
        failedBatch.fail("simulated failure");
        batchRepository.save(failedBatch);

        MvcResult result = mockMvc.perform(
                        post("/sampling-batches/test-failed-batch/retry")
                                .header("api-key", TestTokenFactory.hospitalManagerToken()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.batchId").isNotEmpty())
                .andReturn();

        // New batch ID must differ from the original
        String newBatchId = objectMapper.readTree(
                result.getResponse().getContentAsString())
                .get("data").get("batchId").asText();
        assert !newBatchId.equals("test-failed-batch") : "Retry must produce a new batchId";
    }

    @Test
    void retrySampling_completedBatch_shouldReturn422() throws Exception {
        String projectId = createAndActivateProject("Retry-Completed Test");

        // Execute sampling to get a COMPLETED batch
        MvcResult execResult = mockMvc.perform(
                        post("/review-projects/" + projectId + "/sampling/execute")
                                .header("api-key", TestTokenFactory.hospitalManagerToken()))
                .andExpect(status().isCreated())
                .andReturn();

        String batchId = objectMapper.readTree(
                execResult.getResponse().getContentAsString())
                .get("data").get("batchId").asText();

        // Try to retry the COMPLETED batch → 422 BATCH_NOT_RETRYABLE
        mockMvc.perform(post("/sampling-batches/" + batchId + "/retry")
                        .header("api-key", TestTokenFactory.hospitalManagerToken()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("BATCH_NOT_RETRYABLE"));
    }

    // --- Helpers ---

    private String createAndActivateProject(String name) throws Exception {
        CreateReviewProjectRequest req = new CreateReviewProjectRequest();
        req.setHospitalId("H001");
        req.setName(name);
        req.setMedicalRecordTypes(Set.of("OUTPATIENT"));

        MvcResult result = mockMvc.perform(post("/review-projects")
                        .header("api-key", TestTokenFactory.hospitalManagerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        String projectId = objectMapper.readTree(
                result.getResponse().getContentAsString())
                .get("data").get("projectId").asText();

        mockMvc.perform(post("/review-projects/" + projectId + "/publish-version")
                        .header("api-key", TestTokenFactory.hospitalManagerToken()))
                .andExpect(status().isOk());

        return projectId;
    }
}
```

**Note:** `createAndActivateProject` publishes the version but does NOT activate the project (no activate endpoint exists yet in B1/B2). The sampling endpoints check for `ACTIVE` status — but the CandidateSourcePort stub returns an empty list. In this setup, the project's status after `publishNewVersion()` is still `DRAFT`, which means `executeSampling_activeProject_shouldReturn201` will get a `PROJECT_NOT_ACTIVE` 422.

To make the test work without an activate endpoint, inject the `ReviewProjectRepository` and call `project.activate()` directly:

```java
@Autowired
private com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository projectRepository;

// Update createAndActivateProject helper to:
private String createAndActivateProject(String name) throws Exception {
    CreateReviewProjectRequest req = new CreateReviewProjectRequest();
    req.setHospitalId("H001");
    req.setName(name);
    req.setMedicalRecordTypes(Set.of("OUTPATIENT"));

    MvcResult result = mockMvc.perform(post("/review-projects")
                    .header("api-key", TestTokenFactory.hospitalManagerToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andReturn();

    String projectId = objectMapper.readTree(
            result.getResponse().getContentAsString())
            .get("data").get("projectId").asText();

    // Publish version
    mockMvc.perform(post("/review-projects/" + projectId + "/publish-version")
                    .header("api-key", TestTokenFactory.hospitalManagerToken()))
            .andExpect(status().isOk());

    // Activate directly via repository (no activate endpoint in B1/B2)
    com.tzuchi.his.quality.inspection.domain.project.ReviewProject project =
            projectRepository.findById(projectId)
                    .orElseThrow(() -> new AssertionError("Project not found"));
    project.activate();
    projectRepository.save(project);

    return projectId;
}
```

Update the class to inject `projectRepository` and use this helper in all 4 tests.

- [ ] **Step 2: Run T-007 tests**

```bash
./gradlew test --tests "*.SamplingControllerApiTest"
```

Expected: All 4 tests PASS.

- [ ] **Step 3: Run full test suite**

```bash
./gradlew test
```

Expected: All tests PASS.

- [ ] **Step 4: Commit**

```bash
git add src/test/java/com/tzuchi/his/quality/inspection/interfaces/rest/SamplingControllerApiTest.java
git commit -m "test(T-007): add SamplingControllerApiTest covering preview/execute/retry HTTP contracts

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 18: SD-001 api-spec.md Base Path Fix

**Files:**
- Modify: `docs/03-contracts/api-spec.md`

**SD-001:** Section §3.1 of `api-spec.md` currently states `Base Path: /api/quality-review`. The actual deployed base path (confirmed in `application.yml`) is `/api/quality-inspection`. This is a spec-code mismatch.

- [ ] **Step 1: Find and fix the base path in api-spec.md**

Open `docs/03-contracts/api-spec.md`. Search for the text `/api/quality-review` (or `quality-review` in the base path section). Replace with `/api/quality-inspection`.

The diff should be:
```
- Base Path: /api/quality-review
+ Base Path: /api/quality-inspection
```

If the text appears in examples or other places, fix all occurrences where the context is the service base path.

- [ ] **Step 2: Commit**

```bash
git add docs/03-contracts/api-spec.md
git commit -m "fix(SD-001): correct api-spec.md base path from /api/quality-review to /api/quality-inspection

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

### Task 19: Final Verification

- [ ] **Step 1: Run the complete test suite**

```bash
cd services/quality-inspection-service
./gradlew test
```

Expected output: `BUILD SUCCESSFUL` — all tests pass with no failures.

If any test fails, fix it before proceeding.

- [ ] **Step 2: Verify test count has increased**

The original test suite had 10 tests (6 in SamplingUseCaseTest + 4 in ReviewProjectApiTest).
After all tasks, the expected test count is approximately 30+ tests across:
- `SamplingUseCaseTest` (6 original + T-001 + T-005 = 8 tests)
- `ReviewProjectApiTest` (4 original + T-003 + T-006 + missing-api-key = 7 tests)
- `ScoreSheetDefinitionTest` (3 new = T-002)
- `TzJwtParserTest` (3 new)
- `ReviewProjectIntegrationTest` (1 new = T-004)
- `SamplingControllerApiTest` (4 new = T-007)

- [ ] **Step 3: Verify no regressions in existing behavior**

Confirm the following existing tests still pass:
- `createProject_shouldReturn201` (ReviewProjectApiTest)
- `publishVersion_alreadyPublished_shouldReturn409` (ReviewProjectApiTest — now maps via `DomainStateConflictException`)
- `retry_shouldCreateNewBatch_notOverwriteOldBatch` (SamplingUseCaseTest)
- `execute_shouldCreateBatchAndReviewCases` (SamplingUseCaseTest)

- [ ] **Step 4: Final commit (if any uncommitted changes remain)**

```bash
git status
# If there are any uncommitted changes, investigate and commit them.
```

- [ ] **Step 5: Summary**

All 19 tasks complete. Fixes applied:
- **C-001**: Retry snapshot fixed — uses `previousBatch.getRuleSnapshot()`
- **C-002**: All 7 use cases protected by `actor.requireRole(HOSPITAL_MANAGER)`
- **H-001**: Cross-section itemId/code uniqueness in `ScoreSheetDefinition.addSection()`
- **H-002**: `ScoreSheetDefinition.reconstitute()` — version no longer hardcoded to 1
- **H-003**: `SamplingBatch.reconstitute()` — no more `if (count > 0)` guard
- **H-004**: Typed exception hierarchy (DomainRuleViolationException 422, DomainStateConflictException 409)
- **H-005**: `(batch_id, source_case_key)` unique constraint on `sampling_batch_selected_case`
- **H-006**: `ReviewProject.reconstitute()` — `alignVersion()`/`alignStatus()` anti-pattern removed
- **M-001**: `SamplingBatch.fail()` stores `failureReason`
- **M-002**: `RuleSnapshot` TODO Javadoc added
- **M-003**: All 7 entity table names aligned to `persistence-model.md`
- **M-005**: `sourceCaseKeys` removed from `SamplingPreviewResult`
- **L-001**: `@NotNull @NotBlank` on `CreateReviewProjectRequest` fields
- **L-002**: `BadRequestException` wraps `MedicalRecordType.valueOf()` failure
- **L-003**: `@JoinColumn(name = "section_id")` in `ScoreSheetSectionEntity`
- **L-004**: `ddl-auto: update` moved to local profile only
- **SD-001**: `api-spec.md` base path corrected to `/api/quality-inspection`
- **T-001 to T-007**: All 7 missing tests added

