# B1a ReviewProject Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement all ReviewProject aggregate commands, queries, events, and controller endpoints for B1a.

**Architecture:** Domain-driven design with hexagonal architecture. Domain model changes cascade through JPA entity, repository, use cases, and REST controller. Each use case is an independent @Service class. Domain events published via Spring ApplicationEvent. Query reads use a separate port/adapter (not domain repository) to avoid leaking Spring Data into the domain.

**Tech Stack:** Java 11, Spring Boot 2.7.18, JPA/Hibernate, H2 (test), Gradle 7.6.4

**Spec:** `docs/superpowers/specs/2026-04-08-b1a-review-project-design.md`

**Build/Test command:** `cd services/quality-inspection-service && .\gradlew clean test`

**Package root:** `com.tzuchi.his.quality.inspection`

**Source root:** `services/quality-inspection-service/src/main/java/com/tzuchi/his/quality/inspection`

**Test root:** `services/quality-inspection-service/src/test/java/com/tzuchi/his/quality/inspection`

---

## File Structure

### Files to Modify

| File | Responsibility |
|------|---------------|
| `domain/project/ReviewProject.java` | Add description, boundScoreSheetId fields; add updateDraft(), bindScoreSheet(), requireStatusDraft(); fix activate()/deactivate() |
| `domain/project/ProjectStatus.java` | Fix javadoc (one-way DEACTIVATED) |
| `infrastructure/persistence/project/ReviewProjectEntity.java` | Add description, boundScoreSheetId columns |
| `infrastructure/persistence/project/ReviewProjectRepositoryImpl.java` | Update toEntity/toDomain mapping |
| `infrastructure/persistence/project/ReviewProjectJpaRepository.java` | Add findByHospitalId query |
| `application/project/CreateReviewProjectUseCase.java` | Remove hospitalId param, add description, add event publishing |
| `application/project/PublishReviewProjectVersionUseCase.java` | Add scope guard, event publishing, ResourceNotFoundException |
| `interfaces/rest/ReviewProjectController.java` | Update createProject, add 7 new endpoints |
| `interfaces/rest/dto/CreateReviewProjectRequest.java` | Remove hospitalId, add description |
| `interfaces/rest/GlobalExceptionHandler.java` | Add ResourceNotFoundException handler (404) |

### Files to Create

| File | Responsibility |
|------|---------------|
| `domain/project/event/ReviewProjectCreatedEvent.java` | Domain event for project creation |
| `domain/project/event/ReviewProjectDraftUpdatedEvent.java` | Domain event for draft update |
| `domain/project/event/ReviewProjectVersionPublishedEvent.java` | Domain event for version publish |
| `domain/project/event/ReviewProjectActivatedEvent.java` | Domain event for activation |
| `domain/project/event/ReviewProjectDeactivatedEvent.java` | Domain event for deactivation |
| `domain/project/event/ScoreSheetBoundToProjectEvent.java` | Domain event for score sheet binding |
| `domain/common/ResourceNotFoundException.java` | Exception for entity not found → 404 |
| `application/project/UpdateReviewProjectDraftUseCase.java` | Update draft name/desc/types |
| `application/project/ActivateReviewProjectUseCase.java` | Activate project |
| `application/project/DeactivateReviewProjectUseCase.java` | Deactivate project |
| `application/project/BindScoreSheetToProjectUseCase.java` | Bind score sheet to project |
| `application/project/query/ReviewProjectQueryPort.java` | Query port interface |
| `application/project/query/ReviewProjectSummary.java` | Read model for list queries |
| `application/project/ListReviewProjectsUseCase.java` | List projects use case |
| `application/project/GetReviewProjectDetailUseCase.java` | Get project detail use case |
| `application/project/GetReviewProjectVersionUseCase.java` | Get project version use case |
| `infrastructure/persistence/project/ReviewProjectQueryAdapter.java` | Query port implementation |
| `interfaces/rest/dto/UpdateReviewProjectRequest.java` | DTO for update draft |
| `interfaces/rest/dto/BindScoreSheetRequest.java` | DTO for bind score sheet |
| `interfaces/rest/dto/ReviewProjectResponse.java` | DTO for project detail/list |

### Test Files to Modify

| File | Reason |
|------|--------|
| `domain/project/ReviewProjectTest.java` | Update create() calls (5-param), add new method tests |
| `infrastructure/persistence/project/ReviewProjectIntegrationTest.java` | Update create() call (5-param) |
| `application/sampling/SamplingUseCaseTest.java` | Update create() calls (5-param), add bindScoreSheet before activate |
| `interfaces/rest/ReviewProjectApiTest.java` | Remove hospitalId from create tests, add new endpoint tests |

### Test Files to Create

| File | Responsibility |
|------|---------------|
| `application/project/CreateReviewProjectUseCaseTest.java` | Unit tests with mock repo/publisher |
| `application/project/PublishReviewProjectVersionUseCaseTest.java` | Unit tests with mock repo/publisher |
| `application/project/UpdateReviewProjectDraftUseCaseTest.java` | Unit tests |
| `application/project/ActivateReviewProjectUseCaseTest.java` | Unit tests |
| `application/project/DeactivateReviewProjectUseCaseTest.java` | Unit tests |
| `application/project/BindScoreSheetToProjectUseCaseTest.java` | Unit tests |
| `application/project/ListReviewProjectsUseCaseTest.java` | Unit tests |
| `application/project/GetReviewProjectDetailUseCaseTest.java` | Unit tests for detail |
| `application/project/GetReviewProjectVersionUseCaseTest.java` | Unit tests for version |

---

## Task Dependencies

```
T1 (Domain Foundation)
├── T2 (Domain Events) - independent of T1
├── T3 (ResourceNotFoundException) - independent of T1
│
T4 (Modify CreateUseCase) ← depends on T1, T2, T3
T5 (Modify PublishUseCase) ← depends on T1, T2, T3
│
T6 (UpdateDraftUseCase) ← depends on T1, T2, T3
T7 (ActivateUseCase) ← depends on T1, T2, T3
T8 (DeactivateUseCase) ← depends on T1, T2, T3
T9 (BindScoreSheetUseCase) ← depends on T1, T2, T3
│
T10 (Query Infrastructure) ← depends on T1
T11 (ListProjectsUseCase) ← depends on T10
T12 (Detail + Version UseCases) ← depends on T1, T3
│
T13 (Controller + Integration Tests) ← depends on T4-T12
T14 (Full Verification) ← depends on T13
```
---

## Task 1: Domain Foundation

Update ReviewProject aggregate with new fields, methods, and state guard fixes. Update JPA entity and repository mapping. Fix all existing tests that break due to signature changes.

**Files:**
- Modify: `domain/project/ReviewProject.java`
- Modify: `domain/project/ProjectStatus.java`
- Modify: `infrastructure/persistence/project/ReviewProjectEntity.java`
- Modify: `infrastructure/persistence/project/ReviewProjectRepositoryImpl.java`
- Modify (tests): `domain/project/ReviewProjectTest.java`
- Modify (tests): `infrastructure/persistence/project/ReviewProjectIntegrationTest.java`
- Modify (tests): `application/sampling/SamplingUseCaseTest.java`

- [ ] **Step 1: Write new domain tests in ReviewProjectTest.java**

Add new test methods to `ReviewProjectTest.java`. Also update ALL existing test methods to use the 5-parameter `create()` (add `null` as description). Update `activate_withPublishedVersion_shouldSucceed` and `deactivate_activeProject_shouldSucceed` to add `bindScoreSheet("ss-1")` before `activate()`.

Replace the ENTIRE content of `ReviewProjectTest.java` with:

```java
package com.tzuchi.his.quality.inspection.domain.project;

import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

class ReviewProjectTest {

    // --- Existing tests (updated for 5-param create) ---

    @Test
    void create_shouldStartAsDraftV1() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Test Project",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));

        assertEquals("p1", project.getProjectId());
        assertEquals(ProjectStatus.DRAFT, project.getStatus());
        assertEquals(1, project.getCurrentVersion().getVersionNumber());
        assertFalse(project.getCurrentVersion().isPublished());
    }

    @Test
    void publishNewVersion_shouldMakeCurrentVersionPublished() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Test",
                null, EnumSet.of(MedicalRecordType.INPATIENT));

        project.publishNewVersion();

        assertTrue(project.getCurrentVersion().isPublished());
        assertEquals(1, project.getCurrentVersion().getVersionNumber());
    }

    @Test
    void publishNewVersion_alreadyPublished_shouldThrow() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Test",
                null, EnumSet.of(MedicalRecordType.INPATIENT));
        project.publishNewVersion();

        assertThrows(DomainStateConflictException.class, project::publishNewVersion);
    }

    @Test
    void activate_withPublishedVersion_shouldSucceed() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        project.publishNewVersion();
        project.bindScoreSheet("ss-1");

        project.activate();

        assertEquals(ProjectStatus.ACTIVE, project.getStatus());
    }

    @Test
    void activate_withoutPublishedVersion_shouldThrow() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));

        assertThrows(DomainStateConflictException.class, project::activate);
    }

    @Test
    void deactivate_activeProject_shouldSucceed() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        project.publishNewVersion();
        project.bindScoreSheet("ss-1");
        project.activate();

        project.deactivate();

        assertEquals(ProjectStatus.DEACTIVATED, project.getStatus());
    }

    @Test
    void updateName_draftVersion_shouldSucceed() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Old Name",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));

        project.updateName("New Name");

        assertEquals("New Name", project.getName());
    }

    @Test
    void updateName_publishedVersion_shouldThrow() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        project.publishNewVersion();

        assertThrows(DomainStateConflictException.class, () -> project.updateName("New"));
    }

    @Test
    void prepareDraftVersion_afterPublish_shouldCreateNextDraft() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        project.publishNewVersion();

        project.prepareDraftVersion();

        assertEquals(2, project.getCurrentVersion().getVersionNumber());
        assertFalse(project.getCurrentVersion().isPublished());
    }

    @Test
    void create_withBlankName_shouldThrow() {
        assertThrows(DomainRuleViolationException.class,
                () -> ReviewProject.create("p1", "h1", "  ",
                        null, EnumSet.of(MedicalRecordType.OUTPATIENT)));
    }

    // --- New B1a tests ---

    @Test
    void create_withDescription_shouldStoreDescription() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Test",
                "A description", EnumSet.of(MedicalRecordType.OUTPATIENT));

        assertEquals("A description", project.getDescription());
    }

    @Test
    void create_withNullDescription_shouldAllowNull() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));

        assertNull(project.getDescription());
    }

    @Test
    void updateDraft_draftStatus_shouldUpdateAllFields() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Old",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));

        project.updateDraft("New Name", "New Desc",
                EnumSet.of(MedicalRecordType.INPATIENT));

        assertEquals("New Name", project.getName());
        assertEquals("New Desc", project.getDescription());
        assertTrue(project.getMedicalRecordTypes().contains(MedicalRecordType.INPATIENT));
        assertFalse(project.getMedicalRecordTypes().contains(MedicalRecordType.OUTPATIENT));
    }

    @Test
    void updateDraft_activeStatus_shouldThrow() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        project.publishNewVersion();
        project.bindScoreSheet("ss-1");
        project.activate();

        DomainStateConflictException ex = assertThrows(DomainStateConflictException.class,
                () -> project.updateDraft("X", null, EnumSet.of(MedicalRecordType.OUTPATIENT)));
        assertEquals("INVALID_STATE_TRANSITION", ex.getErrorCode());
    }

    @Test
    void bindScoreSheet_draftStatus_shouldSucceed() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));

        project.bindScoreSheet("ss-123");

        assertEquals("ss-123", project.getBoundScoreSheetId());
    }

    @Test
    void bindScoreSheet_activeStatus_shouldThrow() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        project.publishNewVersion();
        project.bindScoreSheet("ss-1");
        project.activate();

        assertThrows(DomainStateConflictException.class,
                () -> project.bindScoreSheet("ss-2"));
    }

    @Test
    void bindScoreSheet_nullId_shouldThrow() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));

        assertThrows(NullPointerException.class,
                () -> project.bindScoreSheet(null));
    }

    @Test
    void activate_withBoundScoreSheet_shouldSucceed() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        project.publishNewVersion();
        project.bindScoreSheet("ss-1");

        project.activate();

        assertEquals(ProjectStatus.ACTIVE, project.getStatus());
    }

    @Test
    void activate_withoutBoundScoreSheet_shouldThrow() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        project.publishNewVersion();

        DomainRuleViolationException ex = assertThrows(DomainRuleViolationException.class,
                project::activate);
        assertEquals("SCORE_SHEET_NOT_BOUND", ex.getErrorCode());
    }

    @Test
    void activate_fromDeactivated_shouldThrow() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        project.publishNewVersion();
        project.bindScoreSheet("ss-1");
        project.activate();
        project.deactivate();

        DomainStateConflictException ex = assertThrows(DomainStateConflictException.class,
                project::activate);
        assertEquals("INVALID_STATE_TRANSITION", ex.getErrorCode());
    }

    @Test
    void activate_alreadyActive_shouldBeIdempotent() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        project.publishNewVersion();
        project.bindScoreSheet("ss-1");
        project.activate();

        project.activate(); // no throw

        assertEquals(ProjectStatus.ACTIVE, project.getStatus());
    }

    @Test
    void deactivate_activeProject_shouldBecomeDeactivated() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        project.publishNewVersion();
        project.bindScoreSheet("ss-1");
        project.activate();

        project.deactivate();

        assertEquals(ProjectStatus.DEACTIVATED, project.getStatus());
    }

    @Test
    void deactivate_draftProject_shouldThrow() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));

        DomainStateConflictException ex = assertThrows(DomainStateConflictException.class,
                project::deactivate);
        assertEquals("INVALID_STATE_TRANSITION", ex.getErrorCode());
    }

    @Test
    void deactivate_alreadyDeactivated_shouldBeIdempotent() {
        ReviewProject project = ReviewProject.create("p1", "h1", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        project.publishNewVersion();
        project.bindScoreSheet("ss-1");
        project.activate();
        project.deactivate();

        project.deactivate(); // no throw

        assertEquals(ProjectStatus.DEACTIVATED, project.getStatus());
    }

    @Test
    void reconstitute_withNewFields_shouldRoundTrip() {
        ReviewProject project = ReviewProject.reconstitute("p1", "h1", "Test",
                "My Desc", "ss-99",
                EnumSet.of(MedicalRecordType.OUTPATIENT),
                ProjectStatus.ACTIVE, 2, true);

        assertEquals("My Desc", project.getDescription());
        assertEquals("ss-99", project.getBoundScoreSheetId());
        assertEquals(ProjectStatus.ACTIVE, project.getStatus());
        assertEquals(2, project.getCurrentVersion().getVersionNumber());
        assertTrue(project.getCurrentVersion().isPublished());
    }
}
```

- [ ] **Step 2: Run tests to verify new tests fail (compilation error)**

Run: `cd services/quality-inspection-service && .\gradlew clean test`

Expected: Compilation errors — `create()` method has 4 params, tests use 5.

- [ ] **Step 3: Update ReviewProject.java**

Replace the ENTIRE content of `ReviewProject.java` with the new version that adds `description`, `boundScoreSheetId`, `updateDraft()`, `bindScoreSheet()`, `requireStatusDraft()`, and fixes `activate()`/`deactivate()`.

```java
package com.tzuchi.his.quality.inspection.domain.project;

import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;

import java.util.*;

/**
 * Aggregate root for ReviewProject.
 * <p>
 * A ReviewProject defines a quality review activity including sampling policy,
 * score sheet binding, reviewer pool, quality threshold, and effective period.
 * <p>
 * Invariants:
 * - An unpublished project cannot execute sampling.
 * - A published version is immutable.
 * - Version publication creates a new immutable snapshot.
 * - Activation requires a published version AND a bound score sheet.
 * - DEACTIVATED is terminal — no re-activation.
 */
public class ReviewProject {

    private String projectId;
    private String hospitalId;
    private String name;
    private String description;
    private String boundScoreSheetId;
    private Set<MedicalRecordType> medicalRecordTypes;
    private ProjectStatus status;
    private ProjectVersion currentVersion;

    protected ReviewProject() {
        // for JPA
    }

    private ReviewProject(String projectId, String hospitalId, String name,
                          String description, Set<MedicalRecordType> medicalRecordTypes) {
        Objects.requireNonNull(projectId, "projectId must not be null");
        Objects.requireNonNull(hospitalId, "hospitalId must not be null");
        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank()) {
            throw new DomainRuleViolationException("INVALID_PROJECT_NAME",
                    "Project name must not be blank");
        }
        this.projectId = projectId;
        this.hospitalId = hospitalId;
        this.name = name;
        this.description = description;
        this.medicalRecordTypes = (medicalRecordTypes != null && !medicalRecordTypes.isEmpty())
                ? EnumSet.copyOf(medicalRecordTypes)
                : EnumSet.noneOf(MedicalRecordType.class);
        this.status = ProjectStatus.DRAFT;
        this.currentVersion = new ProjectVersion(1, false);
    }

    public static ReviewProject create(String projectId, String hospitalId, String name,
                                        String description,
                                        Set<MedicalRecordType> medicalRecordTypes) {
        return new ReviewProject(projectId, hospitalId, name, description, medicalRecordTypes);
    }

    /**
     * Reconstitute a ReviewProject from persisted state.
     * FOR INFRASTRUCTURE USE ONLY — do not call from domain or application code.
     */
    public static ReviewProject reconstitute(String projectId, String hospitalId, String name,
                                              String description, String boundScoreSheetId,
                                              Set<MedicalRecordType> medicalRecordTypes,
                                              ProjectStatus status, int versionNumber,
                                              boolean versionPublished) {
        ReviewProject project = new ReviewProject();
        project.projectId = projectId;
        project.hospitalId = hospitalId;
        project.name = name;
        project.description = description;
        project.boundScoreSheetId = boundScoreSheetId;
        project.medicalRecordTypes = (medicalRecordTypes != null && !medicalRecordTypes.isEmpty())
                ? EnumSet.copyOf(medicalRecordTypes)
                : EnumSet.noneOf(MedicalRecordType.class);
        project.status = status;
        project.currentVersion = new ProjectVersion(versionNumber, versionPublished);
        return project;
    }

    public void activate() {
        if (this.status == ProjectStatus.ACTIVE) {
            return; // idempotent
        }
        if (this.status != ProjectStatus.DRAFT) {
            throw new DomainStateConflictException("INVALID_STATE_TRANSITION",
                    "Can only activate from DRAFT status, current: " + this.status);
        }
        if (!this.currentVersion.isPublished()) {
            throw new DomainStateConflictException("INVALID_STATE_TRANSITION",
                    "Cannot activate project without a published version");
        }
        if (this.boundScoreSheetId == null) {
            throw new DomainRuleViolationException("SCORE_SHEET_NOT_BOUND",
                    "Cannot activate project without a bound score sheet");
        }
        this.status = ProjectStatus.ACTIVE;
    }

    public void deactivate() {
        if (this.status == ProjectStatus.DEACTIVATED) {
            return; // idempotent
        }
        if (this.status != ProjectStatus.ACTIVE) {
            throw new DomainStateConflictException("INVALID_STATE_TRANSITION",
                    "Can only deactivate from ACTIVE status, current: " + this.status);
        }
        this.status = ProjectStatus.DEACTIVATED;
    }

    /**
     * Update draft project fields. Only allowed in DRAFT status.
     */
    public void updateDraft(String name, String description, Set<MedicalRecordType> types) {
        requireStatusDraft();
        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank()) {
            throw new DomainRuleViolationException("INVALID_PROJECT_NAME",
                    "Project name must not be blank");
        }
        this.name = name;
        this.description = description;
        this.medicalRecordTypes = (types != null && !types.isEmpty())
                ? EnumSet.copyOf(types)
                : EnumSet.noneOf(MedicalRecordType.class);
    }

    /**
     * Bind a score sheet definition to this project. Only allowed in DRAFT status.
     * Does NOT cross-validate that the scoreSheetId exists or is published.
     */
    public void bindScoreSheet(String scoreSheetId) {
        requireStatusDraft();
        Objects.requireNonNull(scoreSheetId, "scoreSheetId must not be null");
        this.boundScoreSheetId = scoreSheetId;
    }

    /**
     * Publish the current draft version, making it immutable.
     */
    public void publishNewVersion() {
        if (this.currentVersion.isPublished()) {
            throw new DomainStateConflictException("INVALID_STATE_TRANSITION",
                    "Current version is already published. Cannot re-publish without creating a new draft.");
        }
        this.currentVersion = this.currentVersion.publish();
    }

    /**
     * Prepare a new draft version for future edits.
     * Can only be called when the current version is published.
     */
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
            throw new DomainRuleViolationException("INVALID_PROJECT_NAME",
                    "Project name must not be blank");
        }
        this.name = name;
    }

    private void requireDraft() {
        if (this.currentVersion.isPublished()) {
            throw new DomainStateConflictException("INVALID_STATE_TRANSITION",
                    "Cannot modify a published version");
        }
    }

    private void requireStatusDraft() {
        if (this.status != ProjectStatus.DRAFT) {
            throw new DomainStateConflictException("INVALID_STATE_TRANSITION",
                    "Operation only allowed in DRAFT status, current: " + this.status);
        }
    }

    // --- Getters ---

    public String getProjectId() { return projectId; }
    public String getHospitalId() { return hospitalId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getBoundScoreSheetId() { return boundScoreSheetId; }
    public Set<MedicalRecordType> getMedicalRecordTypes() { return Collections.unmodifiableSet(medicalRecordTypes); }
    public ProjectStatus getStatus() { return status; }
    public ProjectVersion getCurrentVersion() { return currentVersion; }
}
```

- [ ] **Step 4: Update ProjectStatus.java javadoc**

Replace content:

```java
package com.tzuchi.his.quality.inspection.domain.project;

/**
 * Lifecycle status of a ReviewProject.
 * DRAFT → ACTIVE → DEACTIVATED (one-way; DEACTIVATED is terminal)
 */
public enum ProjectStatus {
    DRAFT,
    ACTIVE,
    DEACTIVATED
}
```

- [ ] **Step 5: Update ReviewProjectEntity.java — add new columns**

Add after the `name` field:

```java
@Column(name = "description")
private String description;

@Column(name = "bound_score_sheet_id")
private String boundScoreSheetId;
```

Add getters/setters after the `name` getter/setter:

```java
public String getDescription() { return description; }
public void setDescription(String description) { this.description = description; }

public String getBoundScoreSheetId() { return boundScoreSheetId; }
public void setBoundScoreSheetId(String boundScoreSheetId) { this.boundScoreSheetId = boundScoreSheetId; }
```

- [ ] **Step 6: Update ReviewProjectRepositoryImpl.java — mapping**

In `toEntity()`, add after `e.setName(...)`:

```java
e.setDescription(project.getDescription());
e.setBoundScoreSheetId(project.getBoundScoreSheetId());
```

Replace the `toDomain()` reconstitute call with 9-param version:

```java
return ReviewProject.reconstitute(
        e.getProjectId(),
        e.getHospitalId(),
        e.getName(),
        e.getDescription(),
        e.getBoundScoreSheetId(),
        types,
        e.getStatus(),
        e.getVersionNumber(),
        e.isVersionPublished()
);
```

- [ ] **Step 7: Fix ReviewProjectIntegrationTest.java**

Change:
```java
ReviewProject project = ReviewProject.create("p-int-1", "h1", "Test Project",
        EnumSet.of(MedicalRecordType.OUTPATIENT));
```
To:
```java
ReviewProject project = ReviewProject.create("p-int-1", "h1", "Test Project",
        null, EnumSet.of(MedicalRecordType.OUTPATIENT));
```

- [ ] **Step 8: Fix SamplingUseCaseTest.java**

Update `createActiveProject()` helper — add `null` description + `bindScoreSheet` before `activate`:
```java
private ReviewProject createActiveProject(String projectId) {
    ReviewProject project = ReviewProject.create(projectId, "H1", "Test Project",
            null, EnumSet.of(MedicalRecordType.OUTPATIENT, MedicalRecordType.INPATIENT));
    project.publishNewVersion();
    project.bindScoreSheet("ss-test");
    project.activate();
    return project;
}
```

Update `execute_inactiveProject_shouldThrow()` create call:
```java
ReviewProject project = ReviewProject.create("p1", "H1", "Test",
        null, EnumSet.of(MedicalRecordType.OUTPATIENT));
```

Update `preview_inactiveProject_shouldThrow()` create call:
```java
ReviewProject project = ReviewProject.create("p-draft", "H1", "Draft Project",
        null, EnumSet.of(MedicalRecordType.OUTPATIENT));
```

- [ ] **Step 9: Run full test suite**

Run: `cd services/quality-inspection-service && .\gradlew clean test`

Expected: ALL tests pass.

- [ ] **Step 10: Commit**

```bash
git add -A
git commit -m "feat(b1a): update ReviewProject domain model — add description, boundScoreSheetId, fix state guards

- Add description and boundScoreSheetId fields to ReviewProject
- Add updateDraft(), bindScoreSheet(), requireStatusDraft() methods
- Fix activate(): DRAFT-only + require boundScoreSheetId
- Fix deactivate(): ACTIVE-only (strict)
- Update JPA entity and repository mapping
- Add 14 new domain tests, fix all existing tests

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 2: Domain Events

Create 6 domain event classes in `domain.project.event` package. All extend `DomainEvent` (B0 base class).

**Files:**
- Create: `domain/project/event/ReviewProjectCreatedEvent.java`
- Create: `domain/project/event/ReviewProjectDraftUpdatedEvent.java`
- Create: `domain/project/event/ReviewProjectVersionPublishedEvent.java`
- Create: `domain/project/event/ReviewProjectActivatedEvent.java`
- Create: `domain/project/event/ReviewProjectDeactivatedEvent.java`
- Create: `domain/project/event/ScoreSheetBoundToProjectEvent.java`

- [ ] **Step 1: Create all 6 event classes**

Create `ReviewProjectCreatedEvent.java`:
```java
package com.tzuchi.his.quality.inspection.domain.project.event;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

public class ReviewProjectCreatedEvent extends DomainEvent {
    public ReviewProjectCreatedEvent(Object source, ActorContext actor,
                                      String projectId, String hospitalId) {
        super(source, actor.getUserId(), actor.getUserName(),
              hospitalId, "ReviewProject", projectId, "CREATED", null);
    }
}
```

Create `ReviewProjectDraftUpdatedEvent.java`:
```java
package com.tzuchi.his.quality.inspection.domain.project.event;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

public class ReviewProjectDraftUpdatedEvent extends DomainEvent {
    public ReviewProjectDraftUpdatedEvent(Object source, ActorContext actor,
                                           String projectId, String hospitalId) {
        super(source, actor.getUserId(), actor.getUserName(),
              hospitalId, "ReviewProject", projectId, "DRAFT_UPDATED", null);
    }
}
```

Create `ReviewProjectVersionPublishedEvent.java`:
```java
package com.tzuchi.his.quality.inspection.domain.project.event;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

public class ReviewProjectVersionPublishedEvent extends DomainEvent {
    public ReviewProjectVersionPublishedEvent(Object source, ActorContext actor,
                                               String projectId, String hospitalId) {
        super(source, actor.getUserId(), actor.getUserName(),
              hospitalId, "ReviewProject", projectId, "VERSION_PUBLISHED", null);
    }
}
```

Create `ReviewProjectActivatedEvent.java`:
```java
package com.tzuchi.his.quality.inspection.domain.project.event;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

public class ReviewProjectActivatedEvent extends DomainEvent {
    public ReviewProjectActivatedEvent(Object source, ActorContext actor,
                                        String projectId, String hospitalId) {
        super(source, actor.getUserId(), actor.getUserName(),
              hospitalId, "ReviewProject", projectId, "ACTIVATED", null);
    }
}
```

Create `ReviewProjectDeactivatedEvent.java`:
```java
package com.tzuchi.his.quality.inspection.domain.project.event;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

public class ReviewProjectDeactivatedEvent extends DomainEvent {
    public ReviewProjectDeactivatedEvent(Object source, ActorContext actor,
                                          String projectId, String hospitalId) {
        super(source, actor.getUserId(), actor.getUserName(),
              hospitalId, "ReviewProject", projectId, "DEACTIVATED", null);
    }
}
```

Create `ScoreSheetBoundToProjectEvent.java`:
```java
package com.tzuchi.his.quality.inspection.domain.project.event;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

public class ScoreSheetBoundToProjectEvent extends DomainEvent {

    private final String scoreSheetId;

    public ScoreSheetBoundToProjectEvent(Object source, ActorContext actor,
                                          String projectId, String hospitalId,
                                          String scoreSheetId) {
        super(source, actor.getUserId(), actor.getUserName(),
              hospitalId, "ReviewProject", projectId, "SCORE_SHEET_BOUND",
              "{\"scoreSheetId\":\"" + scoreSheetId + "\"}");
        this.scoreSheetId = scoreSheetId;
    }

    public String getScoreSheetId() { return scoreSheetId; }
}
```

- [ ] **Step 2: Run test suite**

Run: `cd services/quality-inspection-service && .\gradlew clean test`

Expected: ALL tests pass.

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "feat(b1a): add 6 ReviewProject domain events

- ReviewProjectCreatedEvent
- ReviewProjectDraftUpdatedEvent
- ReviewProjectVersionPublishedEvent
- ReviewProjectActivatedEvent
- ReviewProjectDeactivatedEvent
- ScoreSheetBoundToProjectEvent

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 3: ResourceNotFoundException

Add `ResourceNotFoundException` for proper 404 responses. Currently, "entity not found" uses `IllegalArgumentException` which maps to 500 — incorrect HTTP semantics.

**Files:**
- Create: `domain/common/ResourceNotFoundException.java`
- Modify: `interfaces/rest/GlobalExceptionHandler.java`

- [ ] **Step 1: Create ResourceNotFoundException.java**

```java
package com.tzuchi.his.quality.inspection.domain.common;

/**
 * Thrown when a requested resource (aggregate) is not found.
 * Maps to HTTP 404 Not Found.
 */
public class ResourceNotFoundException extends RuntimeException {

    private final String errorCode;

    public ResourceNotFoundException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ResourceNotFoundException(String message) {
        super(message);
        this.errorCode = "NOT_FOUND";
    }

    public String getErrorCode() { return errorCode; }
}
```

- [ ] **Step 2: Add handler in GlobalExceptionHandler.java**

Add this method **before** the `IllegalArgumentException` handler. Also add the import.

Import to add:
```java
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
```

Method to add:
```java
@ExceptionHandler(ResourceNotFoundException.class)
public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage()));
}
```

- [ ] **Step 3: Run test suite**

Run: `cd services/quality-inspection-service && .\gradlew clean test`

Expected: ALL tests pass.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat(b1a): add ResourceNotFoundException with 404 handler

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 4: Modify CreateReviewProjectUseCase

Remove `hospitalId` parameter (use `actor.requireHospitalId()`), add `description`, add event publishing. Update DTO and controller create method. Fix existing API tests.

**Files:**
- Modify: `application/project/CreateReviewProjectUseCase.java`
- Modify: `interfaces/rest/dto/CreateReviewProjectRequest.java`
- Modify: `interfaces/rest/ReviewProjectController.java` (createProject method only)
- Modify (tests): `interfaces/rest/ReviewProjectApiTest.java`
- Create (tests): `application/project/CreateReviewProjectUseCaseTest.java`

- [ ] **Step 1: Write unit tests — CreateReviewProjectUseCaseTest.java**

```java
package com.tzuchi.his.quality.inspection.application.project;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.UnauthorizedException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import com.tzuchi.his.quality.inspection.domain.project.event.ReviewProjectCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CreateReviewProjectUseCaseTest {

    private InMemoryProjectRepo repository;
    private CapturingEventPublisher eventPublisher;
    private CreateReviewProjectUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryProjectRepo();
        eventPublisher = new CapturingEventPublisher();
        useCase = new CreateReviewProjectUseCase(repository, eventPublisher);
    }

    @Test
    void execute_hospitalManager_shouldCreateProject() {
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        String projectId = useCase.execute(actor, "Test Project", "A desc",
                EnumSet.of(MedicalRecordType.OUTPATIENT));

        assertNotNull(projectId);
        ReviewProject saved = repository.store.get(projectId);
        assertNotNull(saved);
        assertEquals("H001", saved.getHospitalId());
        assertEquals("Test Project", saved.getName());
        assertEquals("A desc", saved.getDescription());
    }

    @Test
    void execute_shouldPublishCreatedEvent() {
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        String projectId = useCase.execute(actor, "Test", null,
                EnumSet.of(MedicalRecordType.OUTPATIENT));

        assertEquals(1, eventPublisher.events.size());
        assertInstanceOf(ReviewProjectCreatedEvent.class, eventPublisher.events.get(0));
    }

    @Test
    void execute_wrongRole_shouldThrow() {
        ActorContext actor = new ActorContext("u1", "Admin",
                EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.of("H001"));

        assertThrows(UnauthorizedException.class,
                () -> useCase.execute(actor, "Test", null,
                        EnumSet.of(MedicalRecordType.OUTPATIENT)));
    }

    @Test
    void execute_noHospitalId_shouldThrow() {
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.empty());

        assertThrows(Exception.class,
                () -> useCase.execute(actor, "Test", null,
                        EnumSet.of(MedicalRecordType.OUTPATIENT)));
    }

    // --- Test doubles ---

    static class InMemoryProjectRepo implements ReviewProjectRepository {
        final Map<String, ReviewProject> store = new HashMap<>();
        @Override public void save(ReviewProject p) { store.put(p.getProjectId(), p); }
        @Override public Optional<ReviewProject> findById(String id) { return Optional.ofNullable(store.get(id)); }
    }

    static class CapturingEventPublisher implements DomainEventPublisher {
        final List<com.tzuchi.his.quality.inspection.domain.common.DomainEvent> events = new ArrayList<>();
        @Override public void publish(com.tzuchi.his.quality.inspection.domain.common.DomainEvent event) { events.add(event); }
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd services/quality-inspection-service && .\gradlew clean test`

Expected: Compilation error — `CreateReviewProjectUseCase` constructor doesn't accept `DomainEventPublisher`, `execute()` signature mismatch.

- [ ] **Step 3: Update CreateReviewProjectUseCase.java**

Replace content:

```java
package com.tzuchi.his.quality.inspection.application.project;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import com.tzuchi.his.quality.inspection.domain.project.event.ReviewProjectCreatedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class CreateReviewProjectUseCase {

    private final ReviewProjectRepository repository;
    private final DomainEventPublisher eventPublisher;

    public CreateReviewProjectUseCase(ReviewProjectRepository repository,
                                       DomainEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    public String execute(ActorContext actor, String name, String description,
                          Set<MedicalRecordType> medicalRecordTypes) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        String hospitalId = actor.requireHospitalId();
        String projectId = UUID.randomUUID().toString();
        ReviewProject project = ReviewProject.create(projectId, hospitalId, name,
                description, medicalRecordTypes);
        repository.save(project);
        eventPublisher.publish(new ReviewProjectCreatedEvent(this, actor, projectId, hospitalId));
        return projectId;
    }
}
```

- [ ] **Step 4: Update CreateReviewProjectRequest.java**

Replace content (remove hospitalId, add description):

```java
package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import javax.validation.constraints.NotBlank;
import java.util.Set;

public class CreateReviewProjectRequest {

    @NotBlank(message = "must not be blank")
    private String name;

    private String description;

    private Set<String> medicalRecordTypes;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Set<String> getMedicalRecordTypes() { return medicalRecordTypes; }
    public void setMedicalRecordTypes(Set<String> medicalRecordTypes) { this.medicalRecordTypes = medicalRecordTypes; }
}
```

- [ ] **Step 5: Update ReviewProjectController.java createProject method**

Replace the `createProject` method body. Change from `createUseCase.execute(actor, request.getHospitalId(), request.getName(), types)` to `createUseCase.execute(actor, request.getName(), request.getDescription(), types)`:

```java
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

    String projectId = createUseCase.execute(actor, request.getName(),
            request.getDescription(), types);

    return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(Map.of("projectId", projectId)));
}
```

- [ ] **Step 6: Fix ReviewProjectApiTest.java — update create calls**

Remove all `req.setHospitalId("H001")` lines from existing tests. The hospitalId now comes from the JWT token.

In `createProject_shouldReturn201()`:
```java
CreateReviewProjectRequest req = new CreateReviewProjectRequest();
req.setName("Test Project");
req.setMedicalRecordTypes(Set.of("OUTPATIENT", "INPATIENT"));
```

In `publishVersion_newProject_shouldSucceed()`:
```java
CreateReviewProjectRequest req = new CreateReviewProjectRequest();
req.setName("Publish Test");
req.setMedicalRecordTypes(Set.of("OUTPATIENT"));
```

In `publishVersion_alreadyPublished_shouldReturn409()`:
```java
CreateReviewProjectRequest req = new CreateReviewProjectRequest();
req.setName("Double Publish Test");
req.setMedicalRecordTypes(Set.of("OUTPATIENT"));
```

In `createProject_nullMedicalRecordTypes_shouldReturn400()`:
```java
String body = "{\"name\":\"Test\",\"medicalRecordTypes\":null}";
```

In `executeSampling_inactiveProject_shouldReturn422()`:
```java
CreateReviewProjectRequest req = new CreateReviewProjectRequest();
req.setName("T-003 Inactive Project");
req.setMedicalRecordTypes(Set.of("OUTPATIENT"));
```

- [ ] **Step 7: Run full test suite**

Run: `cd services/quality-inspection-service && .\gradlew clean test`

Expected: ALL tests pass.

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "feat(b1a): modify CreateReviewProjectUseCase — remove hospitalId, add description, publish event

- hospitalId from actor.requireHospitalId() instead of request
- Add description parameter
- Add DomainEventPublisher + ReviewProjectCreatedEvent
- Update DTO and controller
- Add 4 unit tests, fix existing API tests

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 5: Modify PublishReviewProjectVersionUseCase

Add hospital scope guard, event publishing, and switch to `ResourceNotFoundException`.

**Files:**
- Modify: `application/project/PublishReviewProjectVersionUseCase.java`
- Create (tests): `application/project/PublishReviewProjectVersionUseCaseTest.java`

- [ ] **Step 1: Write unit tests — PublishReviewProjectVersionUseCaseTest.java**

```java
package com.tzuchi.his.quality.inspection.application.project;

import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.UnauthorizedException;
import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import com.tzuchi.his.quality.inspection.domain.project.event.ReviewProjectVersionPublishedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class PublishReviewProjectVersionUseCaseTest {

    private InMemoryProjectRepo repository;
    private CapturingEventPublisher eventPublisher;
    private PublishReviewProjectVersionUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryProjectRepo();
        eventPublisher = new CapturingEventPublisher();
        useCase = new PublishReviewProjectVersionUseCase(repository, eventPublisher);
    }

    @Test
    void execute_happyPath_shouldPublishVersion() {
        ReviewProject project = ReviewProject.create("p1", "H001", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        repository.save(project);
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        useCase.execute(actor, "p1");

        assertTrue(repository.store.get("p1").getCurrentVersion().isPublished());
    }

    @Test
    void execute_shouldPublishEvent() {
        ReviewProject project = ReviewProject.create("p1", "H001", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        repository.save(project);
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        useCase.execute(actor, "p1");

        assertEquals(1, eventPublisher.events.size());
        assertInstanceOf(ReviewProjectVersionPublishedEvent.class, eventPublisher.events.get(0));
    }

    @Test
    void execute_wrongRole_shouldThrow() {
        ActorContext actor = new ActorContext("u1", "Admin",
                EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.of("H001"));

        assertThrows(UnauthorizedException.class,
                () -> useCase.execute(actor, "p1"));
    }

    @Test
    void execute_crossHospital_shouldThrow() {
        ReviewProject project = ReviewProject.create("p1", "H001", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        repository.save(project);
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H999"));

        assertThrows(AccessDeniedException.class,
                () -> useCase.execute(actor, "p1"));
    }

    @Test
    void execute_notFound_shouldThrow() {
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(actor, "no-such-project"));
    }

    // --- Test doubles ---

    static class InMemoryProjectRepo implements ReviewProjectRepository {
        final Map<String, ReviewProject> store = new HashMap<>();
        @Override public void save(ReviewProject p) { store.put(p.getProjectId(), p); }
        @Override public Optional<ReviewProject> findById(String id) { return Optional.ofNullable(store.get(id)); }
    }

    static class CapturingEventPublisher implements DomainEventPublisher {
        final List<DomainEvent> events = new ArrayList<>();
        @Override public void publish(DomainEvent event) { events.add(event); }
    }
}
```

- [ ] **Step 2: Run tests to verify new tests fail**

Run: `cd services/quality-inspection-service && .\gradlew clean test`

Expected: Compilation error — constructor mismatch, `ResourceNotFoundException` not thrown.

- [ ] **Step 3: Update PublishReviewProjectVersionUseCase.java**

Replace content:

```java
package com.tzuchi.his.quality.inspection.application.project;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import com.tzuchi.his.quality.inspection.domain.project.event.ReviewProjectVersionPublishedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PublishReviewProjectVersionUseCase {

    private final ReviewProjectRepository repository;
    private final DomainEventPublisher eventPublisher;

    public PublishReviewProjectVersionUseCase(ReviewProjectRepository repository,
                                               DomainEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    public void execute(ActorContext actor, String projectId) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        ReviewProject project = repository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("PROJECT_NOT_FOUND",
                        "Project not found: " + projectId));
        actor.requireHospitalScope(project.getHospitalId());
        project.publishNewVersion();
        repository.save(project);
        eventPublisher.publish(new ReviewProjectVersionPublishedEvent(this, actor, projectId,
                project.getHospitalId()));
    }
}
```

- [ ] **Step 4: Run full test suite**

Run: `cd services/quality-inspection-service && .\gradlew clean test`

Expected: ALL tests pass.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(b1a): modify PublishReviewProjectVersionUseCase — add scope guard, event, 404

- Add actor.requireHospitalScope()
- Add DomainEventPublisher + ReviewProjectVersionPublishedEvent
- Switch to ResourceNotFoundException for not-found
- Add 5 unit tests

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 6: UpdateReviewProjectDraftUseCase

New use case + DTO for updating a draft project's name, description, and medical record types.

**Files:**
- Create: `application/project/UpdateReviewProjectDraftUseCase.java`
- Create: `interfaces/rest/dto/UpdateReviewProjectRequest.java`
- Create (tests): `application/project/UpdateReviewProjectDraftUseCaseTest.java`

- [ ] **Step 1: Write unit tests — UpdateReviewProjectDraftUseCaseTest.java**

```java
package com.tzuchi.his.quality.inspection.application.project;

import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.UnauthorizedException;
import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import com.tzuchi.his.quality.inspection.domain.project.event.ReviewProjectDraftUpdatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class UpdateReviewProjectDraftUseCaseTest {

    private InMemoryProjectRepo repository;
    private CapturingEventPublisher eventPublisher;
    private UpdateReviewProjectDraftUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryProjectRepo();
        eventPublisher = new CapturingEventPublisher();
        useCase = new UpdateReviewProjectDraftUseCase(repository, eventPublisher);
    }

    @Test
    void execute_happyPath_shouldUpdateDraft() {
        ReviewProject project = ReviewProject.create("p1", "H001", "Old",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        repository.save(project);
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        useCase.execute(actor, "p1", "New Name", "New Desc",
                EnumSet.of(MedicalRecordType.INPATIENT));

        ReviewProject updated = repository.store.get("p1");
        assertEquals("New Name", updated.getName());
        assertEquals("New Desc", updated.getDescription());
    }

    @Test
    void execute_shouldPublishEvent() {
        ReviewProject project = ReviewProject.create("p1", "H001", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        repository.save(project);
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        useCase.execute(actor, "p1", "New", null,
                EnumSet.of(MedicalRecordType.OUTPATIENT));

        assertEquals(1, eventPublisher.events.size());
        assertInstanceOf(ReviewProjectDraftUpdatedEvent.class, eventPublisher.events.get(0));
    }

    @Test
    void execute_wrongRole_shouldThrow() {
        ActorContext actor = new ActorContext("u1", "Admin",
                EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.of("H001"));

        assertThrows(UnauthorizedException.class,
                () -> useCase.execute(actor, "p1", "X", null,
                        EnumSet.of(MedicalRecordType.OUTPATIENT)));
    }

    @Test
    void execute_crossHospital_shouldThrow() {
        ReviewProject project = ReviewProject.create("p1", "H001", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        repository.save(project);
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H999"));

        assertThrows(AccessDeniedException.class,
                () -> useCase.execute(actor, "p1", "X", null,
                        EnumSet.of(MedicalRecordType.OUTPATIENT)));
    }

    @Test
    void execute_activeProject_shouldThrow() {
        ReviewProject project = ReviewProject.create("p1", "H001", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        project.publishNewVersion();
        project.bindScoreSheet("ss-1");
        project.activate();
        repository.save(project);
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        assertThrows(DomainStateConflictException.class,
                () -> useCase.execute(actor, "p1", "X", null,
                        EnumSet.of(MedicalRecordType.OUTPATIENT)));
    }

    @Test
    void execute_notFound_shouldThrow() {
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(actor, "no-such", "X", null,
                        EnumSet.of(MedicalRecordType.OUTPATIENT)));
    }

    // --- Test doubles ---

    static class InMemoryProjectRepo implements ReviewProjectRepository {
        final Map<String, ReviewProject> store = new HashMap<>();
        @Override public void save(ReviewProject p) { store.put(p.getProjectId(), p); }
        @Override public Optional<ReviewProject> findById(String id) { return Optional.ofNullable(store.get(id)); }
    }

    static class CapturingEventPublisher implements DomainEventPublisher {
        final List<DomainEvent> events = new ArrayList<>();
        @Override public void publish(DomainEvent event) { events.add(event); }
    }
}
```

- [ ] **Step 2: Create UpdateReviewProjectDraftUseCase.java**

```java
package com.tzuchi.his.quality.inspection.application.project;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import com.tzuchi.his.quality.inspection.domain.project.event.ReviewProjectDraftUpdatedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@Transactional
public class UpdateReviewProjectDraftUseCase {

    private final ReviewProjectRepository repository;
    private final DomainEventPublisher eventPublisher;

    public UpdateReviewProjectDraftUseCase(ReviewProjectRepository repository,
                                            DomainEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    public void execute(ActorContext actor, String projectId, String name,
                        String description, Set<MedicalRecordType> medicalRecordTypes) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        ReviewProject project = repository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("PROJECT_NOT_FOUND",
                        "Project not found: " + projectId));
        actor.requireHospitalScope(project.getHospitalId());
        project.updateDraft(name, description, medicalRecordTypes);
        repository.save(project);
        eventPublisher.publish(new ReviewProjectDraftUpdatedEvent(this, actor, projectId,
                project.getHospitalId()));
    }
}
```

- [ ] **Step 3: Create UpdateReviewProjectRequest.java**

```java
package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import javax.validation.constraints.NotBlank;
import java.util.Set;

public class UpdateReviewProjectRequest {

    @NotBlank(message = "must not be blank")
    private String name;

    private String description;

    private Set<String> medicalRecordTypes;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Set<String> getMedicalRecordTypes() { return medicalRecordTypes; }
    public void setMedicalRecordTypes(Set<String> medicalRecordTypes) { this.medicalRecordTypes = medicalRecordTypes; }
}
```

- [ ] **Step 4: Run full test suite**

Run: `cd services/quality-inspection-service && .\gradlew clean test`

Expected: ALL tests pass.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(b1a): add UpdateReviewProjectDraftUseCase

- DRAFT-only update of name, description, medicalRecordTypes
- Hospital scope guard + event publishing
- 6 unit tests

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 7: ActivateReviewProjectUseCase

New use case for activating a review project (DRAFT → ACTIVE).

**Files:**
- Create: `application/project/ActivateReviewProjectUseCase.java`
- Create (tests): `application/project/ActivateReviewProjectUseCaseTest.java`

- [ ] **Step 1: Write unit tests — ActivateReviewProjectUseCaseTest.java**

```java
package com.tzuchi.his.quality.inspection.application.project;

import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.UnauthorizedException;
import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.project.ProjectStatus;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import com.tzuchi.his.quality.inspection.domain.project.event.ReviewProjectActivatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ActivateReviewProjectUseCaseTest {

    private InMemoryProjectRepo repository;
    private CapturingEventPublisher eventPublisher;
    private ActivateReviewProjectUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryProjectRepo();
        eventPublisher = new CapturingEventPublisher();
        useCase = new ActivateReviewProjectUseCase(repository, eventPublisher);
    }

    @Test
    void execute_happyPath_shouldActivate() {
        ReviewProject project = ReviewProject.create("p1", "H001", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        project.publishNewVersion();
        project.bindScoreSheet("ss-1");
        repository.save(project);
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        useCase.execute(actor, "p1");

        assertEquals(ProjectStatus.ACTIVE, repository.store.get("p1").getStatus());
    }

    @Test
    void execute_shouldPublishEvent() {
        ReviewProject project = ReviewProject.create("p1", "H001", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        project.publishNewVersion();
        project.bindScoreSheet("ss-1");
        repository.save(project);
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        useCase.execute(actor, "p1");

        assertEquals(1, eventPublisher.events.size());
        assertInstanceOf(ReviewProjectActivatedEvent.class, eventPublisher.events.get(0));
    }

    @Test
    void execute_noBoundScoreSheet_shouldThrow() {
        ReviewProject project = ReviewProject.create("p1", "H001", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        project.publishNewVersion();
        repository.save(project);
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        assertThrows(DomainRuleViolationException.class,
                () -> useCase.execute(actor, "p1"));
    }

    @Test
    void execute_noPublishedVersion_shouldThrow() {
        ReviewProject project = ReviewProject.create("p1", "H001", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        project.bindScoreSheet("ss-1");
        repository.save(project);
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        assertThrows(DomainStateConflictException.class,
                () -> useCase.execute(actor, "p1"));
    }

    @Test
    void execute_wrongRole_shouldThrow() {
        ActorContext actor = new ActorContext("u1", "Admin",
                EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.of("H001"));

        assertThrows(UnauthorizedException.class,
                () -> useCase.execute(actor, "p1"));
    }

    @Test
    void execute_crossHospital_shouldThrow() {
        ReviewProject project = ReviewProject.create("p1", "H001", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        project.publishNewVersion();
        project.bindScoreSheet("ss-1");
        repository.save(project);
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H999"));

        assertThrows(AccessDeniedException.class,
                () -> useCase.execute(actor, "p1"));
    }

    @Test
    void execute_notFound_shouldThrow() {
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(actor, "no-such"));
    }

    // --- Test doubles ---

    static class InMemoryProjectRepo implements ReviewProjectRepository {
        final Map<String, ReviewProject> store = new HashMap<>();
        @Override public void save(ReviewProject p) { store.put(p.getProjectId(), p); }
        @Override public Optional<ReviewProject> findById(String id) { return Optional.ofNullable(store.get(id)); }
    }

    static class CapturingEventPublisher implements DomainEventPublisher {
        final List<DomainEvent> events = new ArrayList<>();
        @Override public void publish(DomainEvent event) { events.add(event); }
    }
}
```

- [ ] **Step 2: Create ActivateReviewProjectUseCase.java**

```java
package com.tzuchi.his.quality.inspection.application.project;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import com.tzuchi.his.quality.inspection.domain.project.event.ReviewProjectActivatedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ActivateReviewProjectUseCase {

    private final ReviewProjectRepository repository;
    private final DomainEventPublisher eventPublisher;

    public ActivateReviewProjectUseCase(ReviewProjectRepository repository,
                                         DomainEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    public void execute(ActorContext actor, String projectId) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        ReviewProject project = repository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("PROJECT_NOT_FOUND",
                        "Project not found: " + projectId));
        actor.requireHospitalScope(project.getHospitalId());
        project.activate();
        repository.save(project);
        eventPublisher.publish(new ReviewProjectActivatedEvent(this, actor, projectId,
                project.getHospitalId()));
    }
}
```

- [ ] **Step 3: Run full test suite**

Run: `cd services/quality-inspection-service && .\gradlew clean test`

Expected: ALL tests pass.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat(b1a): add ActivateReviewProjectUseCase

- DRAFT → ACTIVE with published version + bound score sheet preconditions
- Hospital scope guard + event publishing
- 7 unit tests

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 8: DeactivateReviewProjectUseCase

New use case for deactivating a review project (ACTIVE → DEACTIVATED).

**Files:**
- Create: `application/project/DeactivateReviewProjectUseCase.java`
- Create (tests): `application/project/DeactivateReviewProjectUseCaseTest.java`

- [ ] **Step 1: Write unit tests — DeactivateReviewProjectUseCaseTest.java**

```java
package com.tzuchi.his.quality.inspection.application.project;

import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.UnauthorizedException;
import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.project.ProjectStatus;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import com.tzuchi.his.quality.inspection.domain.project.event.ReviewProjectDeactivatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DeactivateReviewProjectUseCaseTest {

    private InMemoryProjectRepo repository;
    private CapturingEventPublisher eventPublisher;
    private DeactivateReviewProjectUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryProjectRepo();
        eventPublisher = new CapturingEventPublisher();
        useCase = new DeactivateReviewProjectUseCase(repository, eventPublisher);
    }

    @Test
    void execute_happyPath_shouldDeactivate() {
        ReviewProject project = ReviewProject.create("p1", "H001", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        project.publishNewVersion();
        project.bindScoreSheet("ss-1");
        project.activate();
        repository.save(project);
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        useCase.execute(actor, "p1");

        assertEquals(ProjectStatus.DEACTIVATED, repository.store.get("p1").getStatus());
    }

    @Test
    void execute_shouldPublishEvent() {
        ReviewProject project = ReviewProject.create("p1", "H001", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        project.publishNewVersion();
        project.bindScoreSheet("ss-1");
        project.activate();
        repository.save(project);
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        useCase.execute(actor, "p1");

        assertEquals(1, eventPublisher.events.size());
        assertInstanceOf(ReviewProjectDeactivatedEvent.class, eventPublisher.events.get(0));
    }

    @Test
    void execute_draftProject_shouldThrow() {
        ReviewProject project = ReviewProject.create("p1", "H001", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        repository.save(project);
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        assertThrows(DomainStateConflictException.class,
                () -> useCase.execute(actor, "p1"));
    }

    @Test
    void execute_wrongRole_shouldThrow() {
        ActorContext actor = new ActorContext("u1", "Admin",
                EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.of("H001"));

        assertThrows(UnauthorizedException.class,
                () -> useCase.execute(actor, "p1"));
    }

    @Test
    void execute_notFound_shouldThrow() {
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(actor, "no-such"));
    }

    // --- Test doubles ---

    static class InMemoryProjectRepo implements ReviewProjectRepository {
        final Map<String, ReviewProject> store = new HashMap<>();
        @Override public void save(ReviewProject p) { store.put(p.getProjectId(), p); }
        @Override public Optional<ReviewProject> findById(String id) { return Optional.ofNullable(store.get(id)); }
    }

    static class CapturingEventPublisher implements DomainEventPublisher {
        final List<DomainEvent> events = new ArrayList<>();
        @Override public void publish(DomainEvent event) { events.add(event); }
    }
}
```

- [ ] **Step 2: Create DeactivateReviewProjectUseCase.java**

```java
package com.tzuchi.his.quality.inspection.application.project;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import com.tzuchi.his.quality.inspection.domain.project.event.ReviewProjectDeactivatedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DeactivateReviewProjectUseCase {

    private final ReviewProjectRepository repository;
    private final DomainEventPublisher eventPublisher;

    public DeactivateReviewProjectUseCase(ReviewProjectRepository repository,
                                           DomainEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    public void execute(ActorContext actor, String projectId) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        ReviewProject project = repository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("PROJECT_NOT_FOUND",
                        "Project not found: " + projectId));
        actor.requireHospitalScope(project.getHospitalId());
        project.deactivate();
        repository.save(project);
        eventPublisher.publish(new ReviewProjectDeactivatedEvent(this, actor, projectId,
                project.getHospitalId()));
    }
}
```

- [ ] **Step 3: Run full test suite**

Run: `cd services/quality-inspection-service && .\gradlew clean test`

Expected: ALL tests pass.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat(b1a): add DeactivateReviewProjectUseCase

- ACTIVE → DEACTIVATED (terminal)
- Hospital scope guard + event publishing
- 5 unit tests

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 9: BindScoreSheetToProjectUseCase

New use case + DTO for binding a score sheet to a draft project.

**Files:**
- Create: `application/project/BindScoreSheetToProjectUseCase.java`
- Create: `interfaces/rest/dto/BindScoreSheetRequest.java`
- Create (tests): `application/project/BindScoreSheetToProjectUseCaseTest.java`

- [ ] **Step 1: Write unit tests — BindScoreSheetToProjectUseCaseTest.java**

```java
package com.tzuchi.his.quality.inspection.application.project;

import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.UnauthorizedException;
import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import com.tzuchi.his.quality.inspection.domain.project.event.ScoreSheetBoundToProjectEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class BindScoreSheetToProjectUseCaseTest {

    private InMemoryProjectRepo repository;
    private CapturingEventPublisher eventPublisher;
    private BindScoreSheetToProjectUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryProjectRepo();
        eventPublisher = new CapturingEventPublisher();
        useCase = new BindScoreSheetToProjectUseCase(repository, eventPublisher);
    }

    @Test
    void execute_happyPath_shouldBindScoreSheet() {
        ReviewProject project = ReviewProject.create("p1", "H001", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        repository.save(project);
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        useCase.execute(actor, "p1", "ss-123");

        assertEquals("ss-123", repository.store.get("p1").getBoundScoreSheetId());
    }

    @Test
    void execute_shouldPublishEvent() {
        ReviewProject project = ReviewProject.create("p1", "H001", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        repository.save(project);
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        useCase.execute(actor, "p1", "ss-123");

        assertEquals(1, eventPublisher.events.size());
        assertInstanceOf(ScoreSheetBoundToProjectEvent.class, eventPublisher.events.get(0));
    }

    @Test
    void execute_activeProject_shouldThrow() {
        ReviewProject project = ReviewProject.create("p1", "H001", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        project.publishNewVersion();
        project.bindScoreSheet("ss-old");
        project.activate();
        repository.save(project);
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        assertThrows(DomainStateConflictException.class,
                () -> useCase.execute(actor, "p1", "ss-new"));
    }

    @Test
    void execute_wrongRole_shouldThrow() {
        ActorContext actor = new ActorContext("u1", "Admin",
                EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.of("H001"));

        assertThrows(UnauthorizedException.class,
                () -> useCase.execute(actor, "p1", "ss-1"));
    }

    @Test
    void execute_crossHospital_shouldThrow() {
        ReviewProject project = ReviewProject.create("p1", "H001", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        repository.save(project);
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H999"));

        assertThrows(AccessDeniedException.class,
                () -> useCase.execute(actor, "p1", "ss-1"));
    }

    @Test
    void execute_notFound_shouldThrow() {
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(actor, "no-such", "ss-1"));
    }

    // --- Test doubles ---

    static class InMemoryProjectRepo implements ReviewProjectRepository {
        final Map<String, ReviewProject> store = new HashMap<>();
        @Override public void save(ReviewProject p) { store.put(p.getProjectId(), p); }
        @Override public Optional<ReviewProject> findById(String id) { return Optional.ofNullable(store.get(id)); }
    }

    static class CapturingEventPublisher implements DomainEventPublisher {
        final List<DomainEvent> events = new ArrayList<>();
        @Override public void publish(DomainEvent event) { events.add(event); }
    }
}
```

- [ ] **Step 2: Create BindScoreSheetToProjectUseCase.java**

```java
package com.tzuchi.his.quality.inspection.application.project;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import com.tzuchi.his.quality.inspection.domain.project.event.ScoreSheetBoundToProjectEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BindScoreSheetToProjectUseCase {

    private final ReviewProjectRepository repository;
    private final DomainEventPublisher eventPublisher;

    public BindScoreSheetToProjectUseCase(ReviewProjectRepository repository,
                                           DomainEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    public void execute(ActorContext actor, String projectId, String scoreSheetId) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        ReviewProject project = repository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("PROJECT_NOT_FOUND",
                        "Project not found: " + projectId));
        actor.requireHospitalScope(project.getHospitalId());
        project.bindScoreSheet(scoreSheetId);
        repository.save(project);
        eventPublisher.publish(new ScoreSheetBoundToProjectEvent(this, actor, projectId,
                project.getHospitalId(), scoreSheetId));
    }
}
```

- [ ] **Step 3: Create BindScoreSheetRequest.java**

```java
package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import javax.validation.constraints.NotBlank;

public class BindScoreSheetRequest {

    @NotBlank(message = "must not be blank")
    private String scoreSheetId;

    public String getScoreSheetId() { return scoreSheetId; }
    public void setScoreSheetId(String scoreSheetId) { this.scoreSheetId = scoreSheetId; }
}
```

- [ ] **Step 4: Run full test suite**

Run: `cd services/quality-inspection-service && .\gradlew clean test`

Expected: ALL tests pass.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(b1a): add BindScoreSheetToProjectUseCase

- Bind score sheet to DRAFT project (no cross-aggregate validation)
- Hospital scope guard + event publishing
- 6 unit tests

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 10: Query Infrastructure

Create the query port, summary DTO, query adapter, and JPA query method.

**Files:**
- Create: `application/project/query/ReviewProjectQueryPort.java`
- Create: `application/project/query/ReviewProjectSummary.java`
- Create: `infrastructure/project/ReviewProjectQueryAdapter.java`
- Modify: `infrastructure/project/ReviewProjectJpaRepository.java`

- [ ] **Step 1: Create ReviewProjectSummary.java**

```java
package com.tzuchi.his.quality.inspection.application.project.query;

import com.tzuchi.his.quality.inspection.domain.project.ProjectStatus;

public class ReviewProjectSummary {

    private final String projectId;
    private final String hospitalId;
    private final String name;
    private final String description;
    private final ProjectStatus status;
    private final String boundScoreSheetId;
    private final int currentVersionNumber;
    private final boolean currentVersionPublished;

    public ReviewProjectSummary(String projectId, String hospitalId, String name,
                                 String description, ProjectStatus status,
                                 String boundScoreSheetId,
                                 int currentVersionNumber,
                                 boolean currentVersionPublished) {
        this.projectId = projectId;
        this.hospitalId = hospitalId;
        this.name = name;
        this.description = description;
        this.status = status;
        this.boundScoreSheetId = boundScoreSheetId;
        this.currentVersionNumber = currentVersionNumber;
        this.currentVersionPublished = currentVersionPublished;
    }

    public String getProjectId() { return projectId; }
    public String getHospitalId() { return hospitalId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public ProjectStatus getStatus() { return status; }
    public String getBoundScoreSheetId() { return boundScoreSheetId; }
    public int getCurrentVersionNumber() { return currentVersionNumber; }
    public boolean isCurrentVersionPublished() { return currentVersionPublished; }
}
```

- [ ] **Step 2: Create ReviewProjectQueryPort.java**

```java
package com.tzuchi.his.quality.inspection.application.project.query;

import java.util.List;
import java.util.Optional;

/**
 * Read-only query port for ReviewProject projections.
 * Implementations are in the infrastructure layer.
 */
public interface ReviewProjectQueryPort {

    List<ReviewProjectSummary> findByHospitalId(String hospitalId);

    List<ReviewProjectSummary> findAll();

    Optional<ReviewProjectSummary> findSummaryById(String projectId);
}
```

- [ ] **Step 3: Add JPA query method to ReviewProjectJpaRepository.java**

Add method:

```java
package com.tzuchi.his.quality.inspection.infrastructure.persistence.project;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewProjectJpaRepository extends JpaRepository<ReviewProjectEntity, String> {

    List<ReviewProjectEntity> findByHospitalId(String hospitalId);
}
```

- [ ] **Step 4: Create ReviewProjectQueryAdapter.java**

```java
package com.tzuchi.his.quality.inspection.infrastructure.persistence.project;

import com.tzuchi.his.quality.inspection.application.project.query.ReviewProjectQueryPort;
import com.tzuchi.his.quality.inspection.application.project.query.ReviewProjectSummary;
import com.tzuchi.his.quality.inspection.domain.project.ProjectStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ReviewProjectQueryAdapter implements ReviewProjectQueryPort {

    private final ReviewProjectJpaRepository jpaRepository;

    public ReviewProjectQueryAdapter(ReviewProjectJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<ReviewProjectSummary> findByHospitalId(String hospitalId) {
        return jpaRepository.findByHospitalId(hospitalId).stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    @Override
    public List<ReviewProjectSummary> findAll() {
        return jpaRepository.findAll().stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ReviewProjectSummary> findSummaryById(String projectId) {
        return jpaRepository.findById(projectId).map(this::toSummary);
    }

    private ReviewProjectSummary toSummary(ReviewProjectEntity entity) {
        return new ReviewProjectSummary(
                entity.getProjectId(),
                entity.getHospitalId(),
                entity.getName(),
                entity.getDescription(),
                ProjectStatus.valueOf(entity.getStatus()),
                entity.getBoundScoreSheetId(),
                entity.getCurrentVersionNumber(),
                entity.isCurrentVersionPublished()
        );
    }
}
```

- [ ] **Step 5: Run full test suite**

Run: `cd services/quality-inspection-service && .\gradlew clean test`

Expected: ALL tests pass. (No new tests in this task — query adapter tested via integration tests in Task 13.)

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat(b1a): add query infrastructure — QueryPort, Summary, QueryAdapter

- ReviewProjectQueryPort (read-only query port)
- ReviewProjectSummary (projection DTO)
- ReviewProjectQueryAdapter (JPA-based implementation)
- Add findByHospitalId to JpaRepository

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 11: ListReviewProjectsUseCase

New query use case with role-based visibility: HM sees own hospital, SA sees all.

**Files:**
- Create: `application/project/ListReviewProjectsUseCase.java`
- Create (tests): `application/project/ListReviewProjectsUseCaseTest.java`

- [ ] **Step 1: Write unit tests — ListReviewProjectsUseCaseTest.java**

```java
package com.tzuchi.his.quality.inspection.application.project;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.UnauthorizedException;
import com.tzuchi.his.quality.inspection.application.project.query.ReviewProjectQueryPort;
import com.tzuchi.his.quality.inspection.application.project.query.ReviewProjectSummary;
import com.tzuchi.his.quality.inspection.domain.project.ProjectStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ListReviewProjectsUseCaseTest {

    private InMemoryQueryPort queryPort;
    private ListReviewProjectsUseCase useCase;

    @BeforeEach
    void setUp() {
        queryPort = new InMemoryQueryPort();
        useCase = new ListReviewProjectsUseCase(queryPort);
    }

    @Test
    void execute_hospitalManager_shouldReturnOwnHospitalOnly() {
        queryPort.summaries.add(summary("p1", "H001"));
        queryPort.summaries.add(summary("p2", "H002"));
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        List<ReviewProjectSummary> result = useCase.execute(actor);

        assertEquals(1, result.size());
        assertEquals("p1", result.get(0).getProjectId());
    }

    @Test
    void execute_systemAdmin_shouldReturnAll() {
        queryPort.summaries.add(summary("p1", "H001"));
        queryPort.summaries.add(summary("p2", "H002"));
        ActorContext actor = new ActorContext("u1", "Admin",
                EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.empty());

        List<ReviewProjectSummary> result = useCase.execute(actor);

        assertEquals(2, result.size());
    }

    @Test
    void execute_noValidRole_shouldThrow() {
        ActorContext actor = new ActorContext("u1", "Auditor",
                EnumSet.of(DomainRole.AUDITOR), Optional.empty());

        assertThrows(UnauthorizedException.class,
                () -> useCase.execute(actor));
    }

    private ReviewProjectSummary summary(String id, String hospitalId) {
        return new ReviewProjectSummary(id, hospitalId, "Name", null,
                ProjectStatus.DRAFT, null, 1, false);
    }

    // --- Test double ---

    static class InMemoryQueryPort implements ReviewProjectQueryPort {
        final List<ReviewProjectSummary> summaries = new ArrayList<>();

        @Override
        public List<ReviewProjectSummary> findByHospitalId(String hospitalId) {
            List<ReviewProjectSummary> result = new ArrayList<>();
            for (ReviewProjectSummary s : summaries) {
                if (s.getHospitalId().equals(hospitalId)) result.add(s);
            }
            return result;
        }

        @Override
        public List<ReviewProjectSummary> findAll() {
            return new ArrayList<>(summaries);
        }

        @Override
        public Optional<ReviewProjectSummary> findSummaryById(String projectId) {
            return summaries.stream().filter(s -> s.getProjectId().equals(projectId)).findFirst();
        }
    }
}
```

- [ ] **Step 2: Create ListReviewProjectsUseCase.java**

```java
package com.tzuchi.his.quality.inspection.application.project;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.UnauthorizedException;
import com.tzuchi.his.quality.inspection.application.project.query.ReviewProjectQueryPort;
import com.tzuchi.his.quality.inspection.application.project.query.ReviewProjectSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ListReviewProjectsUseCase {

    private final ReviewProjectQueryPort queryPort;

    public ListReviewProjectsUseCase(ReviewProjectQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    public List<ReviewProjectSummary> execute(ActorContext actor) {
        if (actor.hasRole(DomainRole.SYSTEM_ADMIN)) {
            return queryPort.findAll();
        }
        if (actor.hasRole(DomainRole.HOSPITAL_MANAGER)) {
            String hospitalId = actor.requireHospitalId();
            return queryPort.findByHospitalId(hospitalId);
        }
        throw new UnauthorizedException("Insufficient role to list review projects");
    }
}
```

- [ ] **Step 3: Run full test suite**

Run: `cd services/quality-inspection-service && .\gradlew clean test`

Expected: ALL tests pass.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat(b1a): add ListReviewProjectsUseCase — role-based visibility

- HOSPITAL_MANAGER: own hospital only
- SYSTEM_ADMIN: all projects
- 3 unit tests

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 12: GetReviewProjectDetailUseCase + GetReviewProjectVersionUseCase

Two query use cases: get project detail and get current version info.

**Files:**
- Create: `application/project/GetReviewProjectDetailUseCase.java`
- Create: `application/project/GetReviewProjectVersionUseCase.java`
- Create (tests): `application/project/GetReviewProjectDetailUseCaseTest.java`
- Create (tests): `application/project/GetReviewProjectVersionUseCaseTest.java`

- [ ] **Step 1: Write unit tests — GetReviewProjectDetailUseCaseTest.java**

```java
package com.tzuchi.his.quality.inspection.application.project;

import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.UnauthorizedException;
import com.tzuchi.his.quality.inspection.application.project.query.ReviewProjectQueryPort;
import com.tzuchi.his.quality.inspection.application.project.query.ReviewProjectSummary;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.project.ProjectStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GetReviewProjectDetailUseCaseTest {

    private InMemoryQueryPort queryPort;
    private GetReviewProjectDetailUseCase useCase;

    @BeforeEach
    void setUp() {
        queryPort = new InMemoryQueryPort();
        useCase = new GetReviewProjectDetailUseCase(queryPort);
    }

    @Test
    void execute_hospitalManager_ownHospital_shouldReturn() {
        queryPort.summaries.put("p1", summary("p1", "H001"));
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        ReviewProjectSummary result = useCase.execute(actor, "p1");

        assertEquals("p1", result.getProjectId());
    }

    @Test
    void execute_systemAdmin_shouldReturnAny() {
        queryPort.summaries.put("p1", summary("p1", "H001"));
        ActorContext actor = new ActorContext("u1", "Admin",
                EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.empty());

        ReviewProjectSummary result = useCase.execute(actor, "p1");

        assertEquals("p1", result.getProjectId());
    }

    @Test
    void execute_hospitalManager_crossHospital_shouldThrow() {
        queryPort.summaries.put("p1", summary("p1", "H001"));
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H999"));

        assertThrows(AccessDeniedException.class,
                () -> useCase.execute(actor, "p1"));
    }

    @Test
    void execute_notFound_shouldThrow() {
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(actor, "no-such"));
    }

    @Test
    void execute_noValidRole_shouldThrow() {
        ActorContext actor = new ActorContext("u1", "Auditor",
                EnumSet.of(DomainRole.AUDITOR), Optional.empty());

        assertThrows(UnauthorizedException.class,
                () -> useCase.execute(actor, "p1"));
    }

    private ReviewProjectSummary summary(String id, String hospitalId) {
        return new ReviewProjectSummary(id, hospitalId, "Name", null,
                ProjectStatus.DRAFT, null, 1, false);
    }

    // --- Test double ---

    static class InMemoryQueryPort implements ReviewProjectQueryPort {
        final Map<String, ReviewProjectSummary> summaries = new HashMap<>();

        @Override
        public List<ReviewProjectSummary> findByHospitalId(String hospitalId) {
            List<ReviewProjectSummary> result = new ArrayList<>();
            for (ReviewProjectSummary s : summaries.values()) {
                if (s.getHospitalId().equals(hospitalId)) result.add(s);
            }
            return result;
        }

        @Override
        public List<ReviewProjectSummary> findAll() {
            return new ArrayList<>(summaries.values());
        }

        @Override
        public Optional<ReviewProjectSummary> findSummaryById(String projectId) {
            return Optional.ofNullable(summaries.get(projectId));
        }
    }
}
```

- [ ] **Step 2: Create GetReviewProjectDetailUseCase.java**

```java
package com.tzuchi.his.quality.inspection.application.project;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.UnauthorizedException;
import com.tzuchi.his.quality.inspection.application.project.query.ReviewProjectQueryPort;
import com.tzuchi.his.quality.inspection.application.project.query.ReviewProjectSummary;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetReviewProjectDetailUseCase {

    private final ReviewProjectQueryPort queryPort;

    public GetReviewProjectDetailUseCase(ReviewProjectQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    public ReviewProjectSummary execute(ActorContext actor, String projectId) {
        if (!actor.hasRole(DomainRole.HOSPITAL_MANAGER) && !actor.hasRole(DomainRole.SYSTEM_ADMIN)) {
            throw new UnauthorizedException("Insufficient role to view project detail");
        }
        ReviewProjectSummary summary = queryPort.findSummaryById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("PROJECT_NOT_FOUND",
                        "Project not found: " + projectId));
        if (actor.hasRole(DomainRole.HOSPITAL_MANAGER) && !actor.hasRole(DomainRole.SYSTEM_ADMIN)) {
            actor.requireHospitalScope(summary.getHospitalId());
        }
        return summary;
    }
}
```

- [ ] **Step 3: Write unit tests — GetReviewProjectVersionUseCaseTest.java**

```java
package com.tzuchi.his.quality.inspection.application.project;

import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.UnauthorizedException;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.project.ProjectVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GetReviewProjectVersionUseCaseTest {

    private InMemoryProjectRepo repository;
    private GetReviewProjectVersionUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryProjectRepo();
        useCase = new GetReviewProjectVersionUseCase(repository);
    }

    @Test
    void execute_hospitalManager_ownHospital_shouldReturn() {
        ReviewProject project = ReviewProject.create("p1", "H001", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        project.publishNewVersion();
        repository.save(project);
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        ProjectVersion version = useCase.execute(actor, "p1");

        assertNotNull(version);
        assertEquals(1, version.getVersionNumber());
        assertTrue(version.isPublished());
    }

    @Test
    void execute_systemAdmin_shouldReturnAny() {
        ReviewProject project = ReviewProject.create("p1", "H001", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        repository.save(project);
        ActorContext actor = new ActorContext("u1", "Admin",
                EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.empty());

        ProjectVersion version = useCase.execute(actor, "p1");

        assertNotNull(version);
    }

    @Test
    void execute_crossHospital_shouldThrow() {
        ReviewProject project = ReviewProject.create("p1", "H001", "Test",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        repository.save(project);
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H999"));

        assertThrows(AccessDeniedException.class,
                () -> useCase.execute(actor, "p1"));
    }

    @Test
    void execute_notFound_shouldThrow() {
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(actor, "no-such"));
    }

    @Test
    void execute_noValidRole_shouldThrow() {
        ActorContext actor = new ActorContext("u1", "Auditor",
                EnumSet.of(DomainRole.AUDITOR), Optional.empty());

        assertThrows(UnauthorizedException.class,
                () -> useCase.execute(actor, "p1"));
    }

    // --- Test double ---

    static class InMemoryProjectRepo implements ReviewProjectRepository {
        final Map<String, ReviewProject> store = new HashMap<>();
        @Override public void save(ReviewProject p) { store.put(p.getProjectId(), p); }
        @Override public Optional<ReviewProject> findById(String id) { return Optional.ofNullable(store.get(id)); }
    }
}
```

- [ ] **Step 4: Create GetReviewProjectVersionUseCase.java**

```java
package com.tzuchi.his.quality.inspection.application.project;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.UnauthorizedException;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.project.ProjectVersion;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetReviewProjectVersionUseCase {

    private final ReviewProjectRepository repository;

    public GetReviewProjectVersionUseCase(ReviewProjectRepository repository) {
        this.repository = repository;
    }

    public ProjectVersion execute(ActorContext actor, String projectId) {
        if (!actor.hasRole(DomainRole.HOSPITAL_MANAGER) && !actor.hasRole(DomainRole.SYSTEM_ADMIN)) {
            throw new UnauthorizedException("Insufficient role to view project version");
        }
        ReviewProject project = repository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("PROJECT_NOT_FOUND",
                        "Project not found: " + projectId));
        if (actor.hasRole(DomainRole.HOSPITAL_MANAGER) && !actor.hasRole(DomainRole.SYSTEM_ADMIN)) {
            actor.requireHospitalScope(project.getHospitalId());
        }
        return project.getCurrentVersion();
    }
}
```

- [ ] **Step 5: Run full test suite**

Run: `cd services/quality-inspection-service && .\gradlew clean test`

Expected: ALL tests pass.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat(b1a): add GetReviewProjectDetailUseCase + GetReviewProjectVersionUseCase

- Detail: via QueryPort, HM own hospital / SA all
- Version: current version via domain repo
- 10 unit tests total

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 13: Controller Update — 7 New Endpoints + Integration Tests

Add all remaining endpoints to `ReviewProjectController`, create `ReviewProjectResponse` DTO, and write comprehensive integration tests.

**Files:**
- Modify: `interfaces/rest/ReviewProjectController.java`
- Create: `interfaces/rest/dto/ReviewProjectResponse.java`
- Modify (tests): `interfaces/rest/ReviewProjectApiTest.java`

- [ ] **Step 1: Create ReviewProjectResponse.java**

```java
package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

public class ReviewProjectResponse {

    private String projectId;
    private String hospitalId;
    private String name;
    private String description;
    private String status;
    private String boundScoreSheetId;
    private int currentVersionNumber;
    private boolean currentVersionPublished;

    public ReviewProjectResponse() {}

    public ReviewProjectResponse(String projectId, String hospitalId, String name,
                                  String description, String status,
                                  String boundScoreSheetId,
                                  int currentVersionNumber,
                                  boolean currentVersionPublished) {
        this.projectId = projectId;
        this.hospitalId = hospitalId;
        this.name = name;
        this.description = description;
        this.status = status;
        this.boundScoreSheetId = boundScoreSheetId;
        this.currentVersionNumber = currentVersionNumber;
        this.currentVersionPublished = currentVersionPublished;
    }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getBoundScoreSheetId() { return boundScoreSheetId; }
    public void setBoundScoreSheetId(String boundScoreSheetId) { this.boundScoreSheetId = boundScoreSheetId; }

    public int getCurrentVersionNumber() { return currentVersionNumber; }
    public void setCurrentVersionNumber(int currentVersionNumber) { this.currentVersionNumber = currentVersionNumber; }

    public boolean isCurrentVersionPublished() { return currentVersionPublished; }
    public void setCurrentVersionPublished(boolean currentVersionPublished) { this.currentVersionPublished = currentVersionPublished; }
}
```

- [ ] **Step 2: Update ReviewProjectController.java — full replacement**

Replace the entire controller with all 9 endpoints:

```java
package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.CurrentActor;
import com.tzuchi.his.quality.inspection.application.project.*;
import com.tzuchi.his.quality.inspection.application.project.query.ReviewProjectSummary;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.project.ProjectVersion;
import com.tzuchi.his.quality.inspection.interfaces.rest.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/quality-inspection/review-projects")
public class ReviewProjectController {

    private final CreateReviewProjectUseCase createUseCase;
    private final PublishReviewProjectVersionUseCase publishUseCase;
    private final UpdateReviewProjectDraftUseCase updateDraftUseCase;
    private final ActivateReviewProjectUseCase activateUseCase;
    private final DeactivateReviewProjectUseCase deactivateUseCase;
    private final BindScoreSheetToProjectUseCase bindScoreSheetUseCase;
    private final ListReviewProjectsUseCase listUseCase;
    private final GetReviewProjectDetailUseCase getDetailUseCase;
    private final GetReviewProjectVersionUseCase getVersionUseCase;

    public ReviewProjectController(CreateReviewProjectUseCase createUseCase,
                                    PublishReviewProjectVersionUseCase publishUseCase,
                                    UpdateReviewProjectDraftUseCase updateDraftUseCase,
                                    ActivateReviewProjectUseCase activateUseCase,
                                    DeactivateReviewProjectUseCase deactivateUseCase,
                                    BindScoreSheetToProjectUseCase bindScoreSheetUseCase,
                                    ListReviewProjectsUseCase listUseCase,
                                    GetReviewProjectDetailUseCase getDetailUseCase,
                                    GetReviewProjectVersionUseCase getVersionUseCase) {
        this.createUseCase = createUseCase;
        this.publishUseCase = publishUseCase;
        this.updateDraftUseCase = updateDraftUseCase;
        this.activateUseCase = activateUseCase;
        this.deactivateUseCase = deactivateUseCase;
        this.bindScoreSheetUseCase = bindScoreSheetUseCase;
        this.listUseCase = listUseCase;
        this.getDetailUseCase = getDetailUseCase;
        this.getVersionUseCase = getVersionUseCase;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> createProject(
            @CurrentActor ActorContext actor,
            @Valid @RequestBody CreateReviewProjectRequest request) {
        Set<MedicalRecordType> types = parseMedicalRecordTypes(request.getMedicalRecordTypes());
        String projectId = createUseCase.execute(actor, request.getName(),
                request.getDescription(), types);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(Map.of("projectId", projectId)));
    }

    @PostMapping("/{projectId}/publish")
    public ResponseEntity<ApiResponse<Void>> publishVersion(
            @CurrentActor ActorContext actor,
            @PathVariable String projectId) {
        publishUseCase.execute(actor, projectId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PutMapping("/{projectId}")
    public ResponseEntity<ApiResponse<Void>> updateDraft(
            @CurrentActor ActorContext actor,
            @PathVariable String projectId,
            @Valid @RequestBody UpdateReviewProjectRequest request) {
        Set<MedicalRecordType> types = parseMedicalRecordTypes(request.getMedicalRecordTypes());
        updateDraftUseCase.execute(actor, projectId, request.getName(),
                request.getDescription(), types);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{projectId}/activate")
    public ResponseEntity<ApiResponse<Void>> activate(
            @CurrentActor ActorContext actor,
            @PathVariable String projectId) {
        activateUseCase.execute(actor, projectId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{projectId}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivate(
            @CurrentActor ActorContext actor,
            @PathVariable String projectId) {
        deactivateUseCase.execute(actor, projectId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{projectId}/bind-score-sheet")
    public ResponseEntity<ApiResponse<Void>> bindScoreSheet(
            @CurrentActor ActorContext actor,
            @PathVariable String projectId,
            @Valid @RequestBody BindScoreSheetRequest request) {
        bindScoreSheetUseCase.execute(actor, projectId, request.getScoreSheetId());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ReviewProjectResponse>>> listProjects(
            @CurrentActor ActorContext actor) {
        List<ReviewProjectSummary> summaries = listUseCase.execute(actor);
        List<ReviewProjectResponse> responses = summaries.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ApiResponse<ReviewProjectResponse>> getProject(
            @CurrentActor ActorContext actor,
            @PathVariable String projectId) {
        ReviewProjectSummary summary = getDetailUseCase.execute(actor, projectId);
        return ResponseEntity.ok(ApiResponse.ok(toResponse(summary)));
    }

    @GetMapping("/{projectId}/version")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getVersion(
            @CurrentActor ActorContext actor,
            @PathVariable String projectId) {
        ProjectVersion version = getVersionUseCase.execute(actor, projectId);
        Map<String, Object> versionMap = new LinkedHashMap<>();
        versionMap.put("versionNumber", version.getVersionNumber());
        versionMap.put("published", version.isPublished());
        return ResponseEntity.ok(ApiResponse.ok(versionMap));
    }

    private Set<MedicalRecordType> parseMedicalRecordTypes(Set<String> raw) {
        if (raw == null) return Collections.emptySet();
        return raw.stream()
                .map(s -> {
                    try { return MedicalRecordType.valueOf(s); }
                    catch (IllegalArgumentException e) {
                        throw new BadRequestException("Invalid medicalRecordType: " + s);
                    }
                })
                .collect(Collectors.toSet());
    }

    private ReviewProjectResponse toResponse(ReviewProjectSummary summary) {
        return new ReviewProjectResponse(
                summary.getProjectId(),
                summary.getHospitalId(),
                summary.getName(),
                summary.getDescription(),
                summary.getStatus().name(),
                summary.getBoundScoreSheetId(),
                summary.getCurrentVersionNumber(),
                summary.isCurrentVersionPublished()
        );
    }
}
```

- [ ] **Step 3: Update ReviewProjectApiTest.java — full replacement**

Replace with comprehensive integration tests covering all 9 endpoints:

```java
package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tzuchi.his.quality.inspection.interfaces.rest.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ReviewProjectApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private static final String BASE = "/api/quality-inspection/review-projects";
    private String hmToken = TestTokenFactory.hospitalManagerToken();
    private String saToken = TestTokenFactory.systemAdminToken();

    // --- CREATE ---

    @Test
    void createProject_shouldReturn201() throws Exception {
        CreateReviewProjectRequest req = new CreateReviewProjectRequest();
        req.setName("Test Project");
        req.setDescription("A test description");
        req.setMedicalRecordTypes(Set.of("OUTPATIENT", "INPATIENT"));

        mockMvc.perform(post(BASE)
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.projectId").isNotEmpty());
    }

    @Test
    void createProject_nullName_shouldReturn400() throws Exception {
        String body = "{\"medicalRecordTypes\":[\"OUTPATIENT\"]}";

        mockMvc.perform(post(BASE)
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createProject_systemAdmin_shouldReturn403() throws Exception {
        CreateReviewProjectRequest req = new CreateReviewProjectRequest();
        req.setName("Test");
        req.setMedicalRecordTypes(Set.of("OUTPATIENT"));

        mockMvc.perform(post(BASE)
                        .header("api-key", saToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // --- PUBLISH ---

    @Test
    void publishVersion_shouldReturn200() throws Exception {
        String projectId = createProjectAndGetId("Publish Test");

        mockMvc.perform(post(BASE + "/" + projectId + "/publish")
                        .header("api-key", hmToken))
                .andExpect(status().isOk());
    }

    @Test
    void publishVersion_notFound_shouldReturn404() throws Exception {
        mockMvc.perform(post(BASE + "/no-such/publish")
                        .header("api-key", hmToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void publishVersion_alreadyPublished_shouldReturn409() throws Exception {
        String projectId = createProjectAndGetId("Double Publish");

        mockMvc.perform(post(BASE + "/" + projectId + "/publish")
                        .header("api-key", hmToken))
                .andExpect(status().isOk());

        mockMvc.perform(post(BASE + "/" + projectId + "/publish")
                        .header("api-key", hmToken))
                .andExpect(status().isConflict());
    }

    // --- UPDATE DRAFT ---

    @Test
    void updateDraft_shouldReturn200() throws Exception {
        String projectId = createProjectAndGetId("Before Update");

        UpdateReviewProjectRequest req = new UpdateReviewProjectRequest();
        req.setName("After Update");
        req.setDescription("Updated desc");
        req.setMedicalRecordTypes(Set.of("INPATIENT"));

        mockMvc.perform(put(BASE + "/" + projectId)
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void updateDraft_activeProject_shouldReturn409() throws Exception {
        String projectId = createActiveProject();

        UpdateReviewProjectRequest req = new UpdateReviewProjectRequest();
        req.setName("Changed");
        req.setMedicalRecordTypes(Set.of("OUTPATIENT"));

        mockMvc.perform(put(BASE + "/" + projectId)
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    // --- BIND SCORE SHEET ---

    @Test
    void bindScoreSheet_shouldReturn200() throws Exception {
        String projectId = createProjectAndGetId("Bind Test");

        BindScoreSheetRequest req = new BindScoreSheetRequest();
        req.setScoreSheetId("ss-123");

        mockMvc.perform(post(BASE + "/" + projectId + "/bind-score-sheet")
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    // --- ACTIVATE ---

    @Test
    void activate_shouldReturn200() throws Exception {
        String projectId = createProjectAndGetId("Activate Test");

        // publish version
        mockMvc.perform(post(BASE + "/" + projectId + "/publish")
                        .header("api-key", hmToken))
                .andExpect(status().isOk());

        // bind score sheet
        BindScoreSheetRequest bindReq = new BindScoreSheetRequest();
        bindReq.setScoreSheetId("ss-1");
        mockMvc.perform(post(BASE + "/" + projectId + "/bind-score-sheet")
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bindReq)))
                .andExpect(status().isOk());

        // activate
        mockMvc.perform(post(BASE + "/" + projectId + "/activate")
                        .header("api-key", hmToken))
                .andExpect(status().isOk());
    }

    @Test
    void activate_noBoundScoreSheet_shouldReturn422() throws Exception {
        String projectId = createProjectAndGetId("No SS");

        mockMvc.perform(post(BASE + "/" + projectId + "/publish")
                        .header("api-key", hmToken))
                .andExpect(status().isOk());

        mockMvc.perform(post(BASE + "/" + projectId + "/activate")
                        .header("api-key", hmToken))
                .andExpect(status().isUnprocessableEntity());
    }

    // --- DEACTIVATE ---

    @Test
    void deactivate_shouldReturn200() throws Exception {
        String projectId = createActiveProject();

        mockMvc.perform(post(BASE + "/" + projectId + "/deactivate")
                        .header("api-key", hmToken))
                .andExpect(status().isOk());
    }

    @Test
    void deactivate_draftProject_shouldReturn409() throws Exception {
        String projectId = createProjectAndGetId("Draft Only");

        mockMvc.perform(post(BASE + "/" + projectId + "/deactivate")
                        .header("api-key", hmToken))
                .andExpect(status().isConflict());
    }

    // --- LIST ---

    @Test
    void listProjects_hospitalManager_shouldReturnOwnOnly() throws Exception {
        createProjectAndGetId("HM Project");

        mockMvc.perform(get(BASE)
                        .header("api-key", hmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("HM Project"));
    }

    @Test
    void listProjects_systemAdmin_shouldReturnAll() throws Exception {
        createProjectAndGetId("SA Project");

        mockMvc.perform(get(BASE)
                        .header("api-key", saToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    // --- GET DETAIL ---

    @Test
    void getProject_shouldReturn200() throws Exception {
        String projectId = createProjectAndGetId("Detail Test");

        mockMvc.perform(get(BASE + "/" + projectId)
                        .header("api-key", hmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.projectId").value(projectId))
                .andExpect(jsonPath("$.data.name").value("Detail Test"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    void getProject_notFound_shouldReturn404() throws Exception {
        mockMvc.perform(get(BASE + "/no-such")
                        .header("api-key", hmToken))
                .andExpect(status().isNotFound());
    }

    // --- GET VERSION ---

    @Test
    void getVersion_shouldReturn200() throws Exception {
        String projectId = createProjectAndGetId("Version Test");

        mockMvc.perform(get(BASE + "/" + projectId + "/version")
                        .header("api-key", hmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.versionNumber").value(1))
                .andExpect(jsonPath("$.data.published").value(false));
    }

    // --- Helpers ---

    private String createProjectAndGetId(String name) throws Exception {
        CreateReviewProjectRequest req = new CreateReviewProjectRequest();
        req.setName(name);
        req.setMedicalRecordTypes(Set.of("OUTPATIENT"));

        MvcResult result = mockMvc.perform(post(BASE)
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        Map<String, Object> body = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        return (String) data.get("projectId");
    }

    private String createActiveProject() throws Exception {
        String projectId = createProjectAndGetId("Active Project");

        mockMvc.perform(post(BASE + "/" + projectId + "/publish")
                        .header("api-key", hmToken))
                .andExpect(status().isOk());

        BindScoreSheetRequest bindReq = new BindScoreSheetRequest();
        bindReq.setScoreSheetId("ss-1");
        mockMvc.perform(post(BASE + "/" + projectId + "/bind-score-sheet")
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bindReq)))
                .andExpect(status().isOk());

        mockMvc.perform(post(BASE + "/" + projectId + "/activate")
                        .header("api-key", hmToken))
                .andExpect(status().isOk());

        return projectId;
    }
}
```

- [ ] **Step 4: Run full test suite**

Run: `cd services/quality-inspection-service && .\gradlew clean test`

Expected: ALL tests pass.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(b1a): update ReviewProjectController — 9 endpoints, full integration tests

- 7 new endpoints: updateDraft, activate, deactivate, bindScoreSheet, list, getDetail, getVersion
- ReviewProjectResponse DTO
- 20 integration tests covering all endpoints and error cases

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 14: Full Verification

Run the entire test suite and verify the application starts cleanly.

**Files:** None (verification only)

- [ ] **Step 1: Run full test suite**

Run: `cd services/quality-inspection-service && .\gradlew clean test`

Expected: ALL tests pass. Count should be approximately 100+ tests (74 existing + ~30 new).

- [ ] **Step 2: Verify bootRun starts cleanly**

Run: `cd services/quality-inspection-service && .\gradlew bootRun`

Expected: Application starts without errors. `Started QualityInspectionApplication` in console output.

Stop with Ctrl+C after confirming startup.

- [ ] **Step 3: Final commit (if any small fixes)**

If any fixes were needed during verification:

```bash
git add -A
git commit -m "fix(b1a): address verification findings

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

- [ ] **Step 4: Tag completion**

```bash
git tag b1a-complete
```

---

## Spec Deviations (Intentional Simplifications)

The following intentional deviations from the spec were made:

1. **No pagination** — Spec uses `Page<>`/`Pageable` in query port/use cases. Plan uses `List<>` for simplicity. Pagination can be added in a future task without changing domain or existing tests.

2. **Publish endpoint path** — Spec says `POST /publish-version`. Plan keeps existing `POST /publish` (backward compatible with B0 code). Renaming would break existing API tests and any consumers.

3. **Version endpoint path** — Spec says `GET /versions` (plural). Plan uses `GET /version` (singular) since only current version is returned (no history table). Can rename if needed.

4. **GetReviewProjectDetailUseCase uses QueryPort** — Spec §4.8 uses domain `ReviewProjectRepository` returning `ReviewProject`. Plan uses `ReviewProjectQueryPort` returning `ReviewProjectSummary`. This is a better separation (avoids exposing aggregate internals through REST).

5. **ResourceNotFoundException instead of IllegalArgumentException** — Spec §4.2–4.6 still reference `IllegalArgumentException` for "not found". Plan uses `ResourceNotFoundException` → 404 (as decided in brainstorming). This is a correction over the spec.

6. **`requireAnyRole()` not available** — Spec uses `actor.requireAnyRole()`. Plan uses explicit `if/else` with `actor.hasRole()` since `requireAnyRole()` doesn't exist on `ActorContext`. Semantically identical.

7. **Query use cases in `application.project` package** — Spec places query port/summary in `application.project.query` sub-package. Plan keeps use cases in `application.project` alongside command use cases (only port + summary in the `query` sub-package). This avoids an unusual split where use cases live in a sub-package of their port.
