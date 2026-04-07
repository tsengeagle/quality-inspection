# B0 Engineering Foundation — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build shared infrastructure (Domain Event, Audit Trail, HospitalId parsing, Scope Guard) required by B1 and B2.

**Architecture:** Add domain event base class with Spring ApplicationEvent integration, audit trail via @EventListener, hospitalId resolution (env var > JWT > empty), and scope guard methods on ActorContext.

**Tech Stack:** Java 11, Spring Boot 2.7.18, Spring Data JPA, H2 (dev/test), JUnit 5

**Spec:** `docs/superpowers/specs/2026-04-07-b0-engineering-foundation-design.md`

**Backend root:** `services/quality-inspection-service`
**Package root:** `com.tzuchi.his.quality.inspection`
**Build/test:** `cd services/quality-inspection-service && .\gradlew clean test`

---

## File Structure

### New Files
| File | Responsibility |
|---|---|
| `domain/common/DomainEvent.java` | Abstract base class for all domain events |
| `application/common/DomainEventPublisher.java` | Event publishing interface (domain layer contract) |
| `application/common/AccessDeniedException.java` | Scope/access violation exception (HTTP 403) |
| `infrastructure/event/SpringDomainEventPublisher.java` | Spring ApplicationEventPublisher adapter |
| `infrastructure/audit/AuditTrailEntity.java` | JPA entity for audit_trail table |
| `infrastructure/audit/AuditTrailJpaRepository.java` | Spring Data JPA repository |
| `infrastructure/audit/AuditTrailListener.java` | @EventListener that persists DomainEvent to audit_trail |
| test: `application/common/ActorContextScopeGuardTest.java` | Unit tests for scope guard methods |
| test: `infrastructure/security/TzJwtParserHospitalIdTest.java` | Unit tests for hospitalId resolution |
| test: `infrastructure/audit/AuditTrailListenerIntegrationTest.java` | Integration test for event → audit_trail |
| test: `infrastructure/event/DomainEventBaseTest.java` | Unit test for DomainEvent base class |

### Modified Files
| File | Change |
|---|---|
| `application/common/ActorContext.java` | Add `requireHospitalId()`, `requireHospitalScope()` |
| `infrastructure/security/TzJwtParser.java` | Add hospitalId resolution (env var > JWT > empty) |
| `interfaces/rest/GlobalExceptionHandler.java` | Add `AccessDeniedException` handler → 403 |
| `src/main/resources/application.yml` | Add `quality-inspection.hospital-id` property |
| test: `TestTokenFactory.java` | Add `hospitalManagerToken(hospitalId)`, `systemAdminToken(hospitalId)` |

---

## Task 1: AccessDeniedException + GlobalExceptionHandler

**Files:**
- Create: `src/main/java/com/tzuchi/his/quality/inspection/application/common/AccessDeniedException.java`
- Modify: `src/main/java/com/tzuchi/his/quality/inspection/interfaces/rest/GlobalExceptionHandler.java`

- [ ] **Step 1: Create AccessDeniedException**

```java
package com.tzuchi.his.quality.inspection.application.common;

/**
 * Thrown when the actor does not have access to a resource (hospital scope mismatch, etc.).
 * Maps to HTTP 403 Forbidden with error code "ACCESS_DENIED".
 */
public class AccessDeniedException extends RuntimeException {

    private final String errorCode;

    public AccessDeniedException(String message) {
        super(message);
        this.errorCode = "ACCESS_DENIED";
    }

    public AccessDeniedException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}
```

- [ ] **Step 2: Add handler to GlobalExceptionHandler**

Add this method to `GlobalExceptionHandler.java`, after the `handleUnauthorized` method:

```java
@ExceptionHandler(AccessDeniedException.class)
public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage()));
}
```

Add import:
```java
import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
```

- [ ] **Step 3: Run tests to verify no regressions**

Run: `cd services/quality-inspection-service && .\gradlew clean test`
Expected: All existing tests pass (57 tests).

- [ ] **Step 4: Commit**

```
git add -A && git commit -m "feat(b0): add AccessDeniedException and handler (HTTP 403)

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 2: ActorContext Scope Guard Methods

**Files:**
- Modify: `src/main/java/com/tzuchi/his/quality/inspection/application/common/ActorContext.java`
- Create: `src/test/java/com/tzuchi/his/quality/inspection/application/common/ActorContextScopeGuardTest.java`

- [ ] **Step 1: Write the failing tests**

```java
package com.tzuchi.his.quality.inspection.application.common;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ActorContextScopeGuardTest {

    @Test
    void requireHospitalId_withHospitalId_shouldReturn() {
        ActorContext actor = new ActorContext("u1", "User1",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        assertEquals("H001", actor.requireHospitalId());
    }

    @Test
    void requireHospitalId_withoutHospitalId_shouldThrow() {
        ActorContext actor = new ActorContext("u1", "User1",
                EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.empty());

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                actor::requireHospitalId);
        assertTrue(ex.getMessage().contains("Hospital context required"));
    }

    @Test
    void requireHospitalScope_matching_shouldPass() {
        ActorContext actor = new ActorContext("u1", "User1",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        assertDoesNotThrow(() -> actor.requireHospitalScope("H001"));
    }

    @Test
    void requireHospitalScope_mismatch_shouldThrow() {
        ActorContext actor = new ActorContext("u1", "User1",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> actor.requireHospitalScope("H002"));
        assertTrue(ex.getMessage().contains("Hospital scope mismatch"));
    }

    @Test
    void requireHospitalScope_noHospitalId_shouldThrow() {
        ActorContext actor = new ActorContext("u1", "User1",
                EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.empty());

        assertThrows(AccessDeniedException.class,
                () -> actor.requireHospitalScope("H001"));
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd services/quality-inspection-service && .\gradlew test --tests "*.ActorContextScopeGuardTest"`
Expected: FAIL — `requireHospitalId` and `requireHospitalScope` methods do not exist.

- [ ] **Step 3: Implement scope guard methods**

Add the following two methods to `ActorContext.java`, after the `requireAnyRole` method (before the getters):

```java
    /**
     * Returns the actor's hospitalId, or throws AccessDeniedException if not present.
     */
    public String requireHospitalId() {
        return hospitalId.orElseThrow(() ->
                new AccessDeniedException("Hospital context required"));
    }

    /**
     * Verifies the actor's hospitalId matches the target. Throws AccessDeniedException on mismatch.
     */
    public void requireHospitalScope(String targetHospitalId) {
        String myHospitalId = requireHospitalId();
        if (!myHospitalId.equals(targetHospitalId)) {
            throw new AccessDeniedException("Hospital scope mismatch");
        }
    }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd services/quality-inspection-service && .\gradlew test --tests "*.ActorContextScopeGuardTest"`
Expected: All 5 tests PASS.

- [ ] **Step 5: Run full test suite**

Run: `cd services/quality-inspection-service && .\gradlew clean test`
Expected: All tests pass (57 existing + 5 new = 62).

- [ ] **Step 6: Commit**

```
git add -A && git commit -m "feat(b0): add scope guard methods to ActorContext

requireHospitalId() and requireHospitalScope() for hospital-level
access control. Throws AccessDeniedException on violation.

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 3: TzJwtParser HospitalId Resolution

**Files:**
- Modify: `src/main/java/com/tzuchi/his/quality/inspection/infrastructure/security/TzJwtParser.java`
- Modify: `src/main/java/com/tzuchi/his/quality/inspection/infrastructure/security/ParsedJwtUser.java`
- Create: `src/test/java/com/tzuchi/his/quality/inspection/infrastructure/security/TzJwtParserHospitalIdTest.java`

- [ ] **Step 1: Write the failing tests**

```java
package com.tzuchi.his.quality.inspection.infrastructure.security;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TzJwtParserHospitalIdTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void parse_withJwtHospitalId_shouldExtract() {
        TzJwtParser parser = new TzJwtParser();
        // no env var configured
        String token = buildTokenWithHospitalId("H-JWT-001");

        ActorContext actor = parser.parse(token);

        assertTrue(actor.getHospitalId().isPresent());
        assertEquals("H-JWT-001", actor.getHospitalId().get());
    }

    @Test
    void parse_withoutHospitalId_shouldReturnEmpty() {
        TzJwtParser parser = new TzJwtParser();
        String token = buildTokenWithoutHospitalId();

        ActorContext actor = parser.parse(token);

        assertFalse(actor.getHospitalId().isPresent());
    }

    @Test
    void parse_withEnvHospitalId_shouldOverrideJwt() {
        TzJwtParser parser = new TzJwtParser();
        parser.setConfiguredHospitalId("H-ENV-001");
        String token = buildTokenWithHospitalId("H-JWT-001");

        ActorContext actor = parser.parse(token);

        assertTrue(actor.getHospitalId().isPresent());
        assertEquals("H-ENV-001", actor.getHospitalId().get());
    }

    @Test
    void parse_withEnvHospitalId_noJwtHospitalId_shouldUseEnv() {
        TzJwtParser parser = new TzJwtParser();
        parser.setConfiguredHospitalId("H-ENV-002");
        String token = buildTokenWithoutHospitalId();

        ActorContext actor = parser.parse(token);

        assertTrue(actor.getHospitalId().isPresent());
        assertEquals("H-ENV-002", actor.getHospitalId().get());
    }

    @Test
    void parse_withBlankEnvHospitalId_shouldFallbackToJwt() {
        TzJwtParser parser = new TzJwtParser();
        parser.setConfiguredHospitalId("  ");
        String token = buildTokenWithHospitalId("H-JWT-002");

        ActorContext actor = parser.parse(token);

        assertTrue(actor.getHospitalId().isPresent());
        assertEquals("H-JWT-002", actor.getHospitalId().get());
    }

    private String buildTokenWithHospitalId(String hospitalId) {
        try {
            String header = base64url(mapper.writeValueAsString(
                    Map.of("alg", "RS512", "typ", "JWT")));
            Map<String, Object> userObj = Map.of(
                    "user_id", "u-test",
                    "user_name", "Test User",
                    "user_role", new Object[]{Map.of("role_name", "hospital_manager", "enabled", true)},
                    "hospital_id", hospitalId
            );
            String tzUserJson = mapper.writeValueAsString(Map.of("user", userObj));
            String payload = base64url(mapper.writeValueAsString(
                    Map.of("iss", "api-gateway", "tz-user", tzUserJson)));
            return header + "." + payload + ".test-sig";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String buildTokenWithoutHospitalId() {
        try {
            String header = base64url(mapper.writeValueAsString(
                    Map.of("alg", "RS512", "typ", "JWT")));
            Map<String, Object> userObj = Map.of(
                    "user_id", "u-test",
                    "user_name", "Test User",
                    "user_role", new Object[]{Map.of("role_name", "hospital_manager", "enabled", true)}
            );
            String tzUserJson = mapper.writeValueAsString(Map.of("user", userObj));
            String payload = base64url(mapper.writeValueAsString(
                    Map.of("iss", "api-gateway", "tz-user", tzUserJson)));
            return header + "." + payload + ".test-sig";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String base64url(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes());
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd services/quality-inspection-service && .\gradlew test --tests "*.TzJwtParserHospitalIdTest"`
Expected: FAIL — `setConfiguredHospitalId` doesn't exist, `hospital_id` not parsed from JWT.

- [ ] **Step 3: Add hospital_id to ParsedJwtUser**

Add `hospital_id` field to the inner `User` class in `ParsedJwtUser.java`:

```java
public static class User {
    private String user_id;
    private String user_name;
    private List<UserRole> user_role;
    private String hospital_id;

    public String getUser_id() { return user_id; }
    public void setUser_id(String user_id) { this.user_id = user_id; }
    public String getUser_name() { return user_name; }
    public void setUser_name(String user_name) { this.user_name = user_name; }
    public List<UserRole> getUser_role() { return user_role; }
    public void setUser_role(List<UserRole> user_role) { this.user_role = user_role; }
    public String getHospital_id() { return hospital_id; }
    public void setHospital_id(String hospital_id) { this.hospital_id = hospital_id; }
}
```

- [ ] **Step 4: Implement TzJwtParser hospitalId resolution**

Add field and setter to `TzJwtParser.java`:

```java
import org.springframework.beans.factory.annotation.Value;

@Component
public class TzJwtParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Value("${quality-inspection.hospital-id:}")
    private String configuredHospitalId;

    // Package-private setter for unit testing without Spring context
    void setConfiguredHospitalId(String configuredHospitalId) {
        this.configuredHospitalId = configuredHospitalId;
    }
```

Replace the final return statement `return new ActorContext(userId, userName, roles, Optional.empty());` with:

```java
        // HospitalId resolution: env var > JWT claim > empty
        Optional<String> hospitalId;
        if (configuredHospitalId != null && !configuredHospitalId.isBlank()) {
            hospitalId = Optional.of(configuredHospitalId.trim());
        } else {
            String jwtHospitalId = parsedUser.getUser().getHospital_id();
            if (jwtHospitalId != null && !jwtHospitalId.isBlank()) {
                hospitalId = Optional.of(jwtHospitalId.trim());
            } else {
                hospitalId = Optional.empty();
            }
        }

        return new ActorContext(userId, userName, roles, hospitalId);
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `cd services/quality-inspection-service && .\gradlew test --tests "*.TzJwtParserHospitalIdTest"`
Expected: All 5 tests PASS.

- [ ] **Step 6: Run full test suite**

Run: `cd services/quality-inspection-service && .\gradlew clean test`
Expected: All tests pass.

- [ ] **Step 7: Commit**

```
git add -A && git commit -m "feat(b0): add hospitalId resolution to TzJwtParser

Priority: QUALITY_INSPECTION_HOSPITAL_ID env var > JWT hospital_id claim > empty.
Each hospital instance can override via env var for per-hospital deployment.

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 4: TestTokenFactory Update

**Files:**
- Modify: `src/test/java/com/tzuchi/his/quality/inspection/TestTokenFactory.java`

- [ ] **Step 1: Add hospitalId-aware token methods**

Replace the entire `TestTokenFactory.java` with:

```java
package com.tzuchi.his.quality.inspection;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generates unsigned test JWTs for @SpringBootTest integration tests.
 * The service does not re-verify the JWT signature (gateway already did),
 * so a fake signature is accepted.
 */
public class TestTokenFactory {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    public static final String DEFAULT_HOSPITAL_ID = "H001";

    /** Returns a JWT with HOSPITAL_MANAGER role and default hospitalId (H001). */
    public static String hospitalManagerToken() {
        return hospitalManagerToken(DEFAULT_HOSPITAL_ID);
    }

    /** Returns a JWT with HOSPITAL_MANAGER role and specified hospitalId. */
    public static String hospitalManagerToken(String hospitalId) {
        return buildToken("u-test-01", "Test Manager", hospitalId,
                Map.of("role_name", "hospital_manager", "enabled", true));
    }

    /** Returns a JWT with SYSTEM_ADMIN role and specified hospitalId. */
    public static String systemAdminToken(String hospitalId) {
        return buildToken("u-admin-01", "Test Admin", hospitalId,
                Map.of("role_name", "system_admin", "enabled", true));
    }

    /** Returns a JWT with SYSTEM_ADMIN role and default hospitalId (H001). */
    public static String systemAdminToken() {
        return systemAdminToken(DEFAULT_HOSPITAL_ID);
    }

    /** Returns a JWT with an unknown role (no DomainRole match). */
    public static String unknownRoleToken() {
        return buildToken("u-unknown", "Unknown Role User", null,
                Map.of("role_name", "unknown_role", "enabled", true));
    }

    private static String buildToken(String userId, String userName,
                                     String hospitalId, Map<String, Object> role) {
        try {
            String header = base64url(MAPPER.writeValueAsString(
                    Map.of("alg", "RS512", "typ", "JWT")));

            // Build user object — use LinkedHashMap to allow null values
            Map<String, Object> userObj = new LinkedHashMap<>();
            userObj.put("user_id", userId);
            userObj.put("user_name", userName);
            userObj.put("user_role", new Object[]{role});
            if (hospitalId != null) {
                userObj.put("hospital_id", hospitalId);
            }
            String tzUserJson = MAPPER.writeValueAsString(Map.of("user", userObj));

            String payloadJson = MAPPER.writeValueAsString(Map.of(
                    "iss", "api-gateway",
                    "tz-user", tzUserJson
            ));
            String payload = base64url(payloadJson);

            return header + "." + payload + ".test-sig";
        } catch (Exception e) {
            throw new RuntimeException("Failed to build test token", e);
        }
    }

    private static String base64url(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes());
    }
}
```

- [ ] **Step 2: Run full test suite to verify backward compatibility**

Run: `cd services/quality-inspection-service && .\gradlew clean test`
Expected: All tests pass. The existing `hospitalManagerToken()` (no-arg) still works — it now delegates to `hospitalManagerToken("H001")`.

- [ ] **Step 3: Commit**

```
git add -A && git commit -m "feat(b0): update TestTokenFactory with hospitalId support

Add hospitalManagerToken(hospitalId), systemAdminToken(hospitalId).
Default hospitalId is H001. Backward compatible with existing tests.

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 5: application.yml — Add hospital-id property

**Files:**
- Modify: `src/main/resources/application.yml`

- [ ] **Step 1: Add quality-inspection.hospital-id property**

Add the following block at the end of `application.yml`, before the `---` separator for the local profile:

```yaml
quality-inspection:
  hospital-id: ${QUALITY_INSPECTION_HOSPITAL_ID:}
```

The final `application.yml` should be:

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
      ddl-auto: none
    show-sql: false
    open-in-view: false

quality-inspection:
  hospital-id: ${QUALITY_INSPECTION_HOSPITAL_ID:}

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

- [ ] **Step 2: Run tests to verify**

Run: `cd services/quality-inspection-service && .\gradlew clean test`
Expected: All tests pass.

- [ ] **Step 3: Commit**

```
git add -A && git commit -m "feat(b0): add quality-inspection.hospital-id config property

Maps to QUALITY_INSPECTION_HOSPITAL_ID env var. Defaults to empty.
Per-hospital deployment: env var overrides JWT hospital_id claim.

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 6: DomainEvent Base Class

**Files:**
- Create: `src/main/java/com/tzuchi/his/quality/inspection/domain/common/DomainEvent.java`
- Create: `src/test/java/com/tzuchi/his/quality/inspection/infrastructure/event/DomainEventBaseTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.tzuchi.his.quality.inspection.infrastructure.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DomainEventBaseTest {

    @Test
    void domainEvent_shouldAutoGenerateEventIdAndTimestamp() {
        TestDomainEvent event = new TestDomainEvent(
                this, "u-001", "Test User", "H001",
                "ReviewProject", "p-001", "CREATED", null);

        assertNotNull(event.getEventId());
        assertFalse(event.getEventId().isBlank());
        assertNotNull(event.getOccurredAt());
        assertEquals("u-001", event.getActorUserId());
        assertEquals("Test User", event.getActorUserName());
        assertEquals("H001", event.getHospitalId());
        assertEquals("ReviewProject", event.getEntityType());
        assertEquals("p-001", event.getEntityId());
        assertEquals("CREATED", event.getAction());
        assertNull(event.getDetail());
    }

    @Test
    void domainEvent_twoInstances_shouldHaveDifferentEventIds() {
        TestDomainEvent e1 = new TestDomainEvent(
                this, "u-001", "User", null,
                "SamplingBatch", "b-001", "EXECUTED", null);
        TestDomainEvent e2 = new TestDomainEvent(
                this, "u-001", "User", null,
                "SamplingBatch", "b-002", "EXECUTED", null);

        assertNotEquals(e1.getEventId(), e2.getEventId());
    }

    @Test
    void domainEvent_withDetail_shouldReturnDetail() {
        String detail = "{\"version\":1}";
        TestDomainEvent event = new TestDomainEvent(
                this, "u-001", "User", "H001",
                "ReviewProject", "p-001", "PUBLISHED", detail);

        assertEquals(detail, event.getDetail());
    }

    @Test
    void domainEvent_withNullHospitalId_shouldAccept() {
        TestDomainEvent event = new TestDomainEvent(
                this, "u-001", "User", null,
                "ScoreSheet", "s-001", "CREATED", null);

        assertNull(event.getHospitalId());
    }

    /** Concrete subclass for testing the abstract base class. */
    static class TestDomainEvent extends DomainEvent {
        TestDomainEvent(Object source, String actorUserId, String actorUserName,
                        String hospitalId, String entityType, String entityId,
                        String action, String detail) {
            super(source, actorUserId, actorUserName, hospitalId,
                    entityType, entityId, action, detail);
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd services/quality-inspection-service && .\gradlew test --tests "*.DomainEventBaseTest"`
Expected: FAIL — `DomainEvent` class does not exist.

- [ ] **Step 3: Implement DomainEvent base class**

```java
package com.tzuchi.his.quality.inspection.domain.common;

import org.springframework.context.ApplicationEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Abstract base class for all domain events.
 * Extends Spring ApplicationEvent for direct integration with Spring's event mechanism.
 *
 * Subclasses should provide concrete event types (e.g., ReviewProjectCreatedEvent).
 * The AuditTrailListener automatically persists all DomainEvent subclasses.
 */
public abstract class DomainEvent extends ApplicationEvent {

    private final String eventId;
    private final Instant occurredAt;
    private final String actorUserId;
    private final String actorUserName;
    private final String hospitalId;
    private final String entityType;
    private final String entityId;
    private final String action;
    private final String detail;

    /**
     * @param source        the object that published this event (typically the application service)
     * @param actorUserId   the authenticated user who triggered the action
     * @param actorUserName display name of the actor
     * @param hospitalId    hospital context (nullable — null for global operations)
     * @param entityType    aggregate type name (e.g., "ReviewProject")
     * @param entityId      aggregate root ID
     * @param action        action name (e.g., "CREATED", "PUBLISHED")
     * @param detail        optional JSON detail (nullable)
     */
    protected DomainEvent(Object source, String actorUserId, String actorUserName,
                          String hospitalId, String entityType, String entityId,
                          String action, String detail) {
        super(source);
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = Instant.now();
        this.actorUserId = actorUserId;
        this.actorUserName = actorUserName;
        this.hospitalId = hospitalId;
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.detail = detail;
    }

    public String getEventId() { return eventId; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getActorUserId() { return actorUserId; }
    public String getActorUserName() { return actorUserName; }
    public String getHospitalId() { return hospitalId; }
    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
    public String getAction() { return action; }
    public String getDetail() { return detail; }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd services/quality-inspection-service && .\gradlew test --tests "*.DomainEventBaseTest"`
Expected: All 4 tests PASS.

- [ ] **Step 5: Commit**

```
git add -A && git commit -m "feat(b0): add DomainEvent abstract base class

Extends Spring ApplicationEvent. Auto-generates eventId (UUID) and
occurredAt (Instant.now()). Carries actor, entity, and action metadata
for audit trail recording.

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 7: DomainEventPublisher Interface + Spring Implementation

**Files:**
- Create: `src/main/java/com/tzuchi/his/quality/inspection/application/common/DomainEventPublisher.java`
- Create: `src/main/java/com/tzuchi/his/quality/inspection/infrastructure/event/SpringDomainEventPublisher.java`

- [ ] **Step 1: Create DomainEventPublisher interface**

```java
package com.tzuchi.his.quality.inspection.application.common;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

/**
 * Port for publishing domain events from application services.
 * Implementations are in the infrastructure layer.
 */
public interface DomainEventPublisher {
    void publish(DomainEvent event);
}
```

- [ ] **Step 2: Create SpringDomainEventPublisher**

```java
package com.tzuchi.his.quality.inspection.infrastructure.event;

import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Spring-based implementation of DomainEventPublisher.
 * Delegates to Spring's ApplicationEventPublisher for synchronous in-transaction delivery.
 */
@Component
public class SpringDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher publisher;

    public SpringDomainEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(DomainEvent event) {
        publisher.publishEvent(event);
    }
}
```

- [ ] **Step 3: Run full test suite**

Run: `cd services/quality-inspection-service && .\gradlew clean test`
Expected: All tests pass. SpringDomainEventPublisher is auto-registered as a bean.

- [ ] **Step 4: Commit**

```
git add -A && git commit -m "feat(b0): add DomainEventPublisher interface and Spring impl

Application services inject DomainEventPublisher to publish events.
SpringDomainEventPublisher delegates to Spring ApplicationEventPublisher.

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 8: Audit Trail Entity + JPA Repository + Listener

**Files:**
- Create: `src/main/java/com/tzuchi/his/quality/inspection/infrastructure/audit/AuditTrailEntity.java`
- Create: `src/main/java/com/tzuchi/his/quality/inspection/infrastructure/audit/AuditTrailJpaRepository.java`
- Create: `src/main/java/com/tzuchi/his/quality/inspection/infrastructure/audit/AuditTrailListener.java`
- Create: `src/test/java/com/tzuchi/his/quality/inspection/infrastructure/audit/AuditTrailListenerIntegrationTest.java`

- [ ] **Step 1: Write the failing integration test**

```java
package com.tzuchi.his.quality.inspection.infrastructure.audit;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AuditTrailListenerIntegrationTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private AuditTrailJpaRepository auditRepo;

    @Test
    void publishDomainEvent_shouldPersistAuditTrailEntry() {
        TestAuditEvent event = new TestAuditEvent(
                this, "u-001", "Test User", "H001",
                "ReviewProject", "p-001", "CREATED", "{\"name\":\"Test\"}");

        eventPublisher.publishEvent(event);

        List<AuditTrailEntity> entries = auditRepo.findAll();
        assertEquals(1, entries.size());

        AuditTrailEntity entry = entries.get(0);
        assertEquals(event.getEventId(), entry.getId());
        assertEquals("ReviewProject", entry.getEntityType());
        assertEquals("p-001", entry.getEntityId());
        assertEquals("CREATED", entry.getAction());
        assertEquals("u-001", entry.getActorUserId());
        assertEquals("Test User", entry.getActorUserName());
        assertEquals("H001", entry.getHospitalId());
        assertNotNull(entry.getOccurredAt());
        assertEquals("{\"name\":\"Test\"}", entry.getDetail());
    }

    @Test
    void publishDomainEvent_nullHospitalId_shouldPersist() {
        TestAuditEvent event = new TestAuditEvent(
                this, "u-admin", "Admin User", null,
                "ScoreSheet", "s-001", "PUBLISHED", null);

        eventPublisher.publishEvent(event);

        List<AuditTrailEntity> entries = auditRepo.findAll();
        assertEquals(1, entries.size());

        AuditTrailEntity entry = entries.get(0);
        assertNull(entry.getHospitalId());
        assertNull(entry.getDetail());
    }

    @Test
    void publishMultipleEvents_shouldPersistAll() {
        eventPublisher.publishEvent(new TestAuditEvent(
                this, "u-001", "User", "H001",
                "ReviewProject", "p-001", "CREATED", null));
        eventPublisher.publishEvent(new TestAuditEvent(
                this, "u-001", "User", "H001",
                "ReviewProject", "p-001", "PUBLISHED", null));

        List<AuditTrailEntity> entries = auditRepo.findAll();
        assertEquals(2, entries.size());
    }

    /** Concrete test event extending DomainEvent. */
    static class TestAuditEvent extends DomainEvent {
        TestAuditEvent(Object source, String actorUserId, String actorUserName,
                       String hospitalId, String entityType, String entityId,
                       String action, String detail) {
            super(source, actorUserId, actorUserName, hospitalId,
                    entityType, entityId, action, detail);
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd services/quality-inspection-service && .\gradlew test --tests "*.AuditTrailListenerIntegrationTest"`
Expected: FAIL — `AuditTrailJpaRepository`, `AuditTrailEntity`, `AuditTrailListener` do not exist.

- [ ] **Step 3: Create AuditTrailEntity**

```java
package com.tzuchi.his.quality.inspection.infrastructure.audit;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.Instant;

/**
 * JPA entity for the audit_trail table.
 * Records every domain event for traceability.
 */
@Entity
@Table(name = "audit_trail")
public class AuditTrailEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id", nullable = false, length = 36)
    private String entityId;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(name = "actor_user_id", nullable = false, length = 50)
    private String actorUserId;

    @Column(name = "actor_user_name", nullable = false, length = 100)
    private String actorUserName;

    @Column(name = "hospital_id", length = 50)
    private String hospitalId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(columnDefinition = "TEXT")
    private String detail;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getActorUserId() { return actorUserId; }
    public void setActorUserId(String actorUserId) { this.actorUserId = actorUserId; }

    public String getActorUserName() { return actorUserName; }
    public void setActorUserName(String actorUserName) { this.actorUserName = actorUserName; }

    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }

    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
}
```

- [ ] **Step 4: Create AuditTrailJpaRepository**

```java
package com.tzuchi.his.quality.inspection.infrastructure.audit;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditTrailJpaRepository extends JpaRepository<AuditTrailEntity, String> {
}
```

- [ ] **Step 5: Create AuditTrailListener**

```java
package com.tzuchi.his.quality.inspection.infrastructure.audit;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listens for all DomainEvent subclasses and persists them to the audit_trail table.
 * Runs synchronously within the same transaction as the publishing use case.
 */
@Component
public class AuditTrailListener {

    private static final Logger log = LoggerFactory.getLogger(AuditTrailListener.class);

    private final AuditTrailJpaRepository repository;

    public AuditTrailListener(AuditTrailJpaRepository repository) {
        this.repository = repository;
    }

    @EventListener
    public void handle(DomainEvent event) {
        AuditTrailEntity entity = new AuditTrailEntity();
        entity.setId(event.getEventId());
        entity.setEntityType(event.getEntityType());
        entity.setEntityId(event.getEntityId());
        entity.setAction(event.getAction());
        entity.setActorUserId(event.getActorUserId());
        entity.setActorUserName(event.getActorUserName());
        entity.setHospitalId(event.getHospitalId());
        entity.setOccurredAt(event.getOccurredAt());
        entity.setDetail(event.getDetail());
        repository.save(entity);

        log.debug("Audit trail recorded: {} {} {} by {}",
                event.getEntityType(), event.getEntityId(),
                event.getAction(), event.getActorUserId());
    }
}
```

- [ ] **Step 6: Run integration tests**

Run: `cd services/quality-inspection-service && .\gradlew test --tests "*.AuditTrailListenerIntegrationTest"`
Expected: All 3 tests PASS.

- [ ] **Step 7: Run full test suite**

Run: `cd services/quality-inspection-service && .\gradlew clean test`
Expected: All tests pass.

- [ ] **Step 8: Commit**

```
git add -A && git commit -m "feat(b0): add audit trail infrastructure

AuditTrailEntity (JPA), AuditTrailJpaRepository, and AuditTrailListener
(@EventListener). All DomainEvent subclasses are automatically persisted
to audit_trail table within the same transaction.

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 9: Final Verification

**Files:** (none — verification only)

- [ ] **Step 1: Run full test suite**

Run: `cd services/quality-inspection-service && .\gradlew clean test`
Expected: All tests pass (57 existing + new tests).

- [ ] **Step 2: Verify Spring Boot starts**

Run: `cd services/quality-inspection-service && .\gradlew bootRun` (stop after confirming startup)
Expected: Application starts on port 8080 without errors. All beans wire correctly.

- [ ] **Step 3: Push to origin**

```
git push origin main
```

---

## Verification Checklist

| Deliverable | Task | Verified |
|---|---|---|
| AccessDeniedException (HTTP 403) | Task 1 | ☐ |
| ActorContext.requireHospitalId() | Task 2 | ☐ |
| ActorContext.requireHospitalScope() | Task 2 | ☐ |
| TzJwtParser hospitalId (env > JWT > empty) | Task 3 | ☐ |
| TestTokenFactory hospitalManagerToken(hospitalId) | Task 4 | ☐ |
| TestTokenFactory systemAdminToken(hospitalId) | Task 4 | ☐ |
| application.yml quality-inspection.hospital-id | Task 5 | ☐ |
| DomainEvent base class | Task 6 | ☐ |
| DomainEventPublisher + SpringDomainEventPublisher | Task 7 | ☐ |
| AuditTrailEntity + JPA repo | Task 8 | ☐ |
| AuditTrailListener | Task 8 | ☐ |
| Full test suite passes | Task 9 | ☐ |
