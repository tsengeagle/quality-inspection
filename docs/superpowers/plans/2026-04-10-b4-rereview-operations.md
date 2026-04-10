# B4 — Re-review Operations & Closure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement threshold-based auto-escalation, manual case control, rereviewer assignment, rereview operations (start/save/submit), rereview revision, and weighted-average final-score calculation.

**Architecture:** Domain-driven, TDD, layered architecture. Domain methods on ReviewCase aggregate handle all state transitions and invariants. Application-layer use cases orchestrate domain calls with actor guards. The existing `completeInitialReview` is modified to accept a quality threshold; new methods handle rereview lifecycle. Final score = weighted average of initial and rereview scores (configurable per ProjectVersion).

**Tech Stack:** Java 11, Spring Boot 2.7.18, Gradle 7.6.4, JPA/Hibernate, H2 (tests), JUnit 5, InMemory test doubles.

**Design Spec:** `docs/superpowers/specs/2026-04-10-b4-rereview-operations-design.md`

**Build/Test Command:** `cd services/quality-inspection-service && ./gradlew clean test`

---

## Path Abbreviations

Throughout this plan:

- **SVC** = `services/quality-inspection-service/src/main/java/com/tzuchi/his/quality/inspection`
- **TEST** = `services/quality-inspection-service/src/test/java/com/tzuchi/his/quality/inspection`

---

## File Structure

### Modified Files (13)

| # | File | Change Summary |
|---|------|---------------|
| 1 | `docs/01-requirements/decision-log.md` | Modify D-001, D-012; add D-017 through D-021 |
| 2 | `SVC/domain/project/ProjectVersion.java` | Add qualityThreshold, initialReviewWeight, rereviewWeight |
| 3 | `SVC/domain/project/ReviewProject.java` | Update reconstitute() for new ProjectVersion params |
| 4 | `SVC/infrastructure/persistence/project/ReviewProjectEntity.java` | Add 3 columns |
| 5 | `SVC/infrastructure/persistence/project/ReviewProjectRepositoryImpl.java` | Update toDomain/toEntity |
| 6 | `SVC/domain/reviewcase/ReviewCase.java` | Add fields, 7 new methods, modify completeInitialReview, extend reconstitute |
| 7 | `SVC/domain/reviewcase/submission/ReviewSubmission.java` | Add createRereviewDraft() |
| 8 | `SVC/infrastructure/persistence/reviewcase/ReviewCaseEntity.java` | Add 4 columns |
| 9 | `SVC/infrastructure/persistence/reviewcase/ReviewCaseRepositoryImpl.java` | Update toDomain/toEntity |
| 10 | `SVC/application/reviewcase/SubmitInitialReviewUseCase.java` | Load project, pass threshold |
| 11 | `SVC/interfaces/rest/ReviewCaseController.java` | Add 9 endpoints |
| 12 | `TEST/TestTokenFactory.java` | Add rereviewerToken() |
| 13 | `TEST/application/reviewcase/SubmitInitialReviewUseCaseTest.java` | Update for threshold |

### New Files (31)

**Domain Events (6):**

| # | File |
|---|------|
| 14 | `SVC/domain/reviewcase/event/RereviewerAssignedEvent.java` |
| 15 | `SVC/domain/reviewcase/event/RereviewStartedEvent.java` |
| 16 | `SVC/domain/reviewcase/event/RereviewCompletedEvent.java` |
| 17 | `SVC/domain/reviewcase/event/ReviewCaseClosedEvent.java` |
| 18 | `SVC/domain/reviewcase/event/ReviewCaseEscalatedToRereviewEvent.java` |
| 19 | `SVC/domain/reviewcase/event/RereviewSubmissionRevisedEvent.java` |

**Use Cases (9):**

| # | File |
|---|------|
| 20 | `SVC/application/reviewcase/CloseWithoutRereviewUseCase.java` |
| 21 | `SVC/application/reviewcase/SendToRereviewUseCase.java` |
| 22 | `SVC/application/reviewcase/AssignRereviewerUseCase.java` |
| 23 | `SVC/application/reviewcase/ReassignRereviewerUseCase.java` |
| 24 | `SVC/application/reviewcase/StartRereviewUseCase.java` |
| 25 | `SVC/application/reviewcase/SaveRereviewDraftUseCase.java` |
| 26 | `SVC/application/reviewcase/SubmitRereviewUseCase.java` |
| 27 | `SVC/application/reviewcase/ReviseRereviewUseCase.java` |
| 28 | `SVC/application/reviewcase/GetRereviewRevisionHistoryUseCase.java` |

**DTOs (4):**

| # | File |
|---|------|
| 29 | `SVC/interfaces/rest/dto/AssignRereviewerRequest.java` |
| 30 | `SVC/interfaces/rest/dto/RereviewSubmitRequest.java` |
| 31 | `SVC/interfaces/rest/dto/RereviewReviseRequest.java` |
| 32 | `SVC/interfaces/rest/dto/CaseStatusResponse.java` |

**Tests (12):**

| # | File |
|---|------|
| 33 | `TEST/domain/reviewcase/ReviewCaseRereviewTest.java` |
| 34 | `TEST/domain/reviewcase/submission/ReviewSubmissionRereviewTest.java` |
| 35 | `TEST/application/reviewcase/CloseWithoutRereviewUseCaseTest.java` |
| 36 | `TEST/application/reviewcase/SendToRereviewUseCaseTest.java` |
| 37 | `TEST/application/reviewcase/AssignRereviewerUseCaseTest.java` |
| 38 | `TEST/application/reviewcase/ReassignRereviewerUseCaseTest.java` |
| 39 | `TEST/application/reviewcase/StartRereviewUseCaseTest.java` |
| 40 | `TEST/application/reviewcase/SaveRereviewDraftUseCaseTest.java` |
| 41 | `TEST/application/reviewcase/SubmitRereviewUseCaseTest.java` |
| 42 | `TEST/application/reviewcase/ReviseRereviewUseCaseTest.java` |
| 43 | `TEST/application/reviewcase/GetRereviewRevisionHistoryUseCaseTest.java` |
| 44 | `TEST/interfaces/rest/RereviewApiTest.java` |

---

## Tasks

---

### Task 1: Update decision-log.md

**Files:**
- Modify: `docs/01-requirements/decision-log.md`

This is a docs-only task. Update the decision log with B4 decisions before touching code.

- [ ] **Step 1: Add/modify decisions in decision-log.md**

Append the following entries (use the existing format in the file):

**Modify D-001:**
> - **Before:** Results in CLOSED status cannot be revised.
> - **After:** CLOSED cases cannot be reopened or have state transitions. However, **rereview revision** (append-only score correction) is allowed on CLOSED cases because: (1) revision does not change case state, (2) revision is not reopen, (3) case remains CLOSED, (4) only finalScore is recalculated. Initial review revision on CLOSED cases remains prohibited.

**Modify D-012:**
> - **Before:** Final score set by re-reviewer.
> - **After:** Final score = weighted average of initial review score and re-review score. Default weight 50/50, configurable per ProjectVersion.

**Add D-017:** Re-reviewer must not be the same person as the initial reviewer for the same case.

**Add D-018:** Re-reviewer cannot see initial review answers or scores during re-review (independent evaluation).

**Add D-019:** Threshold auto-evaluation: when initial score > quality threshold → auto PENDING_REREVIEW. When score ≤ threshold or threshold is null, case stays at INITIAL_REVIEW_COMPLETED; HOSPITAL_MANAGER may manually close or send to re-review.

**Add D-020:** Scoring weight (initialReviewWeight, rereviewWeight) is stored in ProjectVersion, default 0.5/0.5. Invariant: initialReviewWeight + rereviewWeight = 1.0, both > 0 and ≤ 1.

**Add D-021:** Rereview revision is allowed on CLOSED cases (exception to terminal state no-revision rule). Rationale: rereview submission auto-closes the case, so the only window for rereview revision is after CLOSED. This is safe because revision is append-only and does not change case lifecycle state.

- [ ] **Step 2: Commit**

```bash
cd services/quality-inspection-service
git add docs/01-requirements/decision-log.md
git commit -m "docs: update decision-log with B4 decisions (D-001, D-012, D-017–D-021)"
```

---

### Task 2: Extend ProjectVersion with Scoring Configuration

**Files:**
- Modify: `SVC/domain/project/ProjectVersion.java`
- Create: `TEST/domain/project/ProjectVersionTest.java`

ProjectVersion is currently a simple value object with (versionNumber, published). We add qualityThreshold, initialReviewWeight, and rereviewWeight.

- [ ] **Step 1: Write failing tests for ProjectVersion extension**

Create `TEST/domain/project/ProjectVersionTest.java`:

```java
package com.tzuchi.his.quality.inspection.domain.project;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class ProjectVersionTest {

    @Test
    void constructor_withAllFields_shouldStoreValues() {
        ProjectVersion pv = new ProjectVersion(1, false,
                new BigDecimal("80.00"),
                new BigDecimal("0.6"),
                new BigDecimal("0.4"));
        assertEquals(1, pv.getVersionNumber());
        assertFalse(pv.isPublished());
        assertEquals(new BigDecimal("80.00"), pv.getQualityThreshold());
        assertEquals(new BigDecimal("0.6"), pv.getInitialReviewWeight());
        assertEquals(new BigDecimal("0.4"), pv.getRereviewWeight());
    }

    @Test
    void constructor_twoArg_shouldUseDefaults() {
        ProjectVersion pv = new ProjectVersion(1, false);
        assertNull(pv.getQualityThreshold());
        assertEquals(new BigDecimal("0.5"), pv.getInitialReviewWeight());
        assertEquals(new BigDecimal("0.5"), pv.getRereviewWeight());
    }

    @Test
    void publish_shouldCarryOverScoringConfig() {
        ProjectVersion draft = new ProjectVersion(1, false,
                new BigDecimal("80.00"),
                new BigDecimal("0.6"),
                new BigDecimal("0.4"));
        ProjectVersion published = draft.publish();
        assertTrue(published.isPublished());
        assertEquals(new BigDecimal("80.00"), published.getQualityThreshold());
        assertEquals(new BigDecimal("0.6"), published.getInitialReviewWeight());
        assertEquals(new BigDecimal("0.4"), published.getRereviewWeight());
    }

    @Test
    void nextDraft_shouldCarryOverScoringConfig() {
        ProjectVersion published = new ProjectVersion(1, true,
                new BigDecimal("80.00"),
                new BigDecimal("0.6"),
                new BigDecimal("0.4"));
        ProjectVersion next = published.nextDraft();
        assertEquals(2, next.getVersionNumber());
        assertFalse(next.isPublished());
        assertEquals(new BigDecimal("80.00"), next.getQualityThreshold());
        assertEquals(new BigDecimal("0.6"), next.getInitialReviewWeight());
        assertEquals(new BigDecimal("0.4"), next.getRereviewWeight());
    }

    @Test
    void validateWeights_sumNotOne_shouldThrow() {
        ProjectVersion pv = new ProjectVersion(1, false,
                new BigDecimal("80.00"),
                new BigDecimal("0.6"),
                new BigDecimal("0.6"));
        assertThrows(IllegalArgumentException.class, pv::validateWeightsForPublish);
    }

    @Test
    void validateWeights_zeroWeight_shouldThrow() {
        ProjectVersion pv = new ProjectVersion(1, false,
                new BigDecimal("80.00"),
                BigDecimal.ZERO,
                new BigDecimal("1.0"));
        assertThrows(IllegalArgumentException.class, pv::validateWeightsForPublish);
    }

    @Test
    void validateWeights_validWeights_shouldNotThrow() {
        ProjectVersion pv = new ProjectVersion(1, false,
                new BigDecimal("80.00"),
                new BigDecimal("0.5"),
                new BigDecimal("0.5"));
        assertDoesNotThrow(pv::validateWeightsForPublish);
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd services/quality-inspection-service && ./gradlew test --tests "*.ProjectVersionTest" 2>&1 | tail -5
```

Expected: Compilation error — `getQualityThreshold()`, `getInitialReviewWeight()`, `getRereviewWeight()`, `validateWeightsForPublish()` do not exist.

- [ ] **Step 3: Implement ProjectVersion extension**

Replace the full content of `SVC/domain/project/ProjectVersion.java`:

```java
package com.tzuchi.his.quality.inspection.domain.project;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Immutable version snapshot for a ReviewProject.
 * Carries scoring configuration: qualityThreshold and review weights.
 */
public class ProjectVersion {

    private static final BigDecimal DEFAULT_WEIGHT = new BigDecimal("0.5");

    private final int versionNumber;
    private final boolean published;
    private final BigDecimal qualityThreshold;
    private final BigDecimal initialReviewWeight;
    private final BigDecimal rereviewWeight;

    public ProjectVersion(int versionNumber, boolean published) {
        this(versionNumber, published, null, DEFAULT_WEIGHT, DEFAULT_WEIGHT);
    }

    public ProjectVersion(int versionNumber, boolean published,
                          BigDecimal qualityThreshold,
                          BigDecimal initialReviewWeight,
                          BigDecimal rereviewWeight) {
        if (versionNumber < 1) {
            throw new IllegalArgumentException("Version number must be >= 1");
        }
        this.versionNumber = versionNumber;
        this.published = published;
        this.qualityThreshold = qualityThreshold;
        this.initialReviewWeight = initialReviewWeight != null ? initialReviewWeight : DEFAULT_WEIGHT;
        this.rereviewWeight = rereviewWeight != null ? rereviewWeight : DEFAULT_WEIGHT;
    }

    /**
     * Validate that weights sum to 1.0 and each is > 0 and <= 1.
     * Called before publishing a version.
     */
    public void validateWeightsForPublish() {
        if (initialReviewWeight.compareTo(BigDecimal.ZERO) <= 0
                || initialReviewWeight.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException(
                    "initialReviewWeight must be > 0 and <= 1, got: " + initialReviewWeight);
        }
        if (rereviewWeight.compareTo(BigDecimal.ZERO) <= 0
                || rereviewWeight.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException(
                    "rereviewWeight must be > 0 and <= 1, got: " + rereviewWeight);
        }
        if (initialReviewWeight.add(rereviewWeight).compareTo(BigDecimal.ONE) != 0) {
            throw new IllegalArgumentException(
                    "Weights must sum to 1.0, got: " + initialReviewWeight + " + " + rereviewWeight);
        }
    }

    public int getVersionNumber() { return versionNumber; }
    public boolean isPublished() { return published; }
    public BigDecimal getQualityThreshold() { return qualityThreshold; }
    public BigDecimal getInitialReviewWeight() { return initialReviewWeight; }
    public BigDecimal getRereviewWeight() { return rereviewWeight; }

    public ProjectVersion publish() {
        return new ProjectVersion(this.versionNumber, true,
                this.qualityThreshold, this.initialReviewWeight, this.rereviewWeight);
    }

    public ProjectVersion nextDraft() {
        return new ProjectVersion(this.versionNumber + 1, false,
                this.qualityThreshold, this.initialReviewWeight, this.rereviewWeight);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProjectVersion that = (ProjectVersion) o;
        return versionNumber == that.versionNumber && published == that.published;
    }

    @Override
    public int hashCode() {
        return Objects.hash(versionNumber, published);
    }

    @Override
    public String toString() {
        return "v" + versionNumber + (published ? " (published)" : " (draft)");
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
cd services/quality-inspection-service && ./gradlew test --tests "*.ProjectVersionTest" 2>&1 | tail -5
```

Expected: All 7 tests PASS.

- [ ] **Step 5: Run full test suite to check backward compatibility**

```bash
cd services/quality-inspection-service && ./gradlew test 2>&1 | tail -10
```

Expected: All existing tests still pass (the 2-arg constructor provides defaults).

- [ ] **Step 6: Commit**

```bash
git add -A && git commit -m "feat(B4): extend ProjectVersion with qualityThreshold and scoring weights"
```

---

### Task 3: Cascade ProjectVersion Changes to ReviewProject, Entity, and Repo

**Files:**
- Modify: `SVC/domain/project/ReviewProject.java:67-83` (reconstitute)
- Modify: `SVC/infrastructure/persistence/project/ReviewProjectEntity.java` (add 3 columns)
- Modify: `SVC/infrastructure/persistence/project/ReviewProjectRepositoryImpl.java` (toDomain/toEntity)

- [ ] **Step 1: Add columns to ReviewProjectEntity**

Add these three fields to `ReviewProjectEntity.java` after the `versionPublished` field (line ~43):

```java
    @Column(name = "quality_threshold", precision = 10, scale = 2)
    private BigDecimal qualityThreshold;

    @Column(name = "initial_review_weight", precision = 3, scale = 2)
    private BigDecimal initialReviewWeight;

    @Column(name = "rereview_weight", precision = 3, scale = 2)
    private BigDecimal rereviewWeight;
```

Add the import at the top:
```java
import java.math.BigDecimal;
```

Add getters and setters after the existing ones:

```java
    public BigDecimal getQualityThreshold() { return qualityThreshold; }
    public void setQualityThreshold(BigDecimal qualityThreshold) { this.qualityThreshold = qualityThreshold; }

    public BigDecimal getInitialReviewWeight() { return initialReviewWeight; }
    public void setInitialReviewWeight(BigDecimal initialReviewWeight) { this.initialReviewWeight = initialReviewWeight; }

    public BigDecimal getRereviewWeight() { return rereviewWeight; }
    public void setRereviewWeight(BigDecimal rereviewWeight) { this.rereviewWeight = rereviewWeight; }
```

- [ ] **Step 2: Update ReviewProject.reconstitute() to accept new params**

Modify the `reconstitute` method signature in `ReviewProject.java` (line 67) to add 3 new parameters at the end:

```java
    public static ReviewProject reconstitute(String projectId, String hospitalId, String name,
                                              String description, String boundScoreSheetId,
                                              Set<MedicalRecordType> medicalRecordTypes,
                                              ProjectStatus status, int versionNumber,
                                              boolean versionPublished,
                                              BigDecimal qualityThreshold,
                                              BigDecimal initialReviewWeight,
                                              BigDecimal rereviewWeight) {
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
        project.currentVersion = new ProjectVersion(versionNumber, versionPublished,
                qualityThreshold, initialReviewWeight, rereviewWeight);
        return project;
    }
```

Add the import:
```java
import java.math.BigDecimal;
```

- [ ] **Step 3: Update ReviewProjectRepositoryImpl toDomain/toEntity**

In `ReviewProjectRepositoryImpl.java`, update the `toDomain` method (line 50) to pass the new fields:

```java
    private ReviewProject toDomain(ReviewProjectEntity e) {
        Set<MedicalRecordType> types = e.getMedicalRecordTypes().stream()
                .map(MedicalRecordType::valueOf)
                .collect(Collectors.toSet());
        return ReviewProject.reconstitute(
                e.getProjectId(),
                e.getHospitalId(),
                e.getName(),
                e.getDescription(),
                e.getBoundScoreSheetId(),
                types,
                e.getStatus(),
                e.getVersionNumber(),
                e.isVersionPublished(),
                e.getQualityThreshold(),
                e.getInitialReviewWeight(),
                e.getRereviewWeight()
        );
    }
```

Update the `toEntity` method (line 32) to persist the new fields. Add after `e.setVersionPublished(...)`:

```java
        e.setQualityThreshold(project.getCurrentVersion().getQualityThreshold());
        e.setInitialReviewWeight(project.getCurrentVersion().getInitialReviewWeight());
        e.setRereviewWeight(project.getCurrentVersion().getRereviewWeight());
```

- [ ] **Step 4: Run full test suite**

```bash
cd services/quality-inspection-service && ./gradlew test 2>&1 | tail -10
```

Expected: All tests pass. The InMemoryReviewProjectRepository doesn't use reconstitute(), so no changes needed there. The repo impl is updated for JPA round-tripping.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(B4): cascade ProjectVersion scoring config to entity and repo mapping"
```

---

### Task 4: Add ReviewCase New Fields and Extend Infrastructure Mapping

**Files:**
- Modify: `SVC/domain/reviewcase/ReviewCase.java` (add fields, extend reconstitute, add getters)
- Modify: `SVC/infrastructure/persistence/reviewcase/ReviewCaseEntity.java` (add 4 columns)
- Modify: `SVC/infrastructure/persistence/reviewcase/ReviewCaseRepositoryImpl.java` (update mapping)

This task adds the structural fields without new domain methods. The methods come in Tasks 5-8.

- [ ] **Step 1: Add new fields and extend reconstitute in ReviewCase.java**

Add these fields after the `scoreSheetVersionRef` field (around line 41):

```java
    // --- B4: rereview ---
    private ReviewerAssignment rereviewerAssignment;
    private LocalDateTime rereviewerDueAt;
    private BigDecimal initialReviewScore;
```

Extend the `reconstitute` method signature to accept 3 new parameters. Replace the existing reconstitute (lines 71-98) with:

```java
    public static ReviewCase reconstitute(
            String caseId, String batchId, String projectId, String hospitalId,
            String sourceCaseKey, MedicalRecordType medicalRecordType,
            CaseStatus status, ReviewerAssignment reviewerAssignment, LocalDateTime dueAt,
            String cancelReason, String returnReason, LocalDateTime cancelledAt, LocalDateTime closedAt,
            boolean rereviewRequired, BigDecimal finalScore, String finalConclusion,
            String latestInitialSubmissionId, String ruleSnapshotRef, String scoreSheetVersionRef,
            ReviewerAssignment rereviewerAssignment, LocalDateTime rereviewerDueAt,
            BigDecimal initialReviewScore) {
        ReviewCase rc = new ReviewCase();
        rc.caseId = caseId;
        rc.batchId = batchId;
        rc.projectId = projectId;
        rc.hospitalId = hospitalId;
        rc.sourceCaseKey = sourceCaseKey;
        rc.medicalRecordType = medicalRecordType;
        rc.status = status;
        rc.reviewerAssignment = reviewerAssignment;
        rc.dueAt = dueAt;
        rc.cancelReason = cancelReason;
        rc.returnReason = returnReason;
        rc.cancelledAt = cancelledAt;
        rc.closedAt = closedAt;
        rc.rereviewRequired = rereviewRequired;
        rc.finalScore = finalScore;
        rc.finalConclusion = finalConclusion;
        rc.latestInitialSubmissionId = latestInitialSubmissionId;
        rc.ruleSnapshotRef = ruleSnapshotRef;
        rc.scoreSheetVersionRef = scoreSheetVersionRef;
        rc.rereviewerAssignment = rereviewerAssignment;
        rc.rereviewerDueAt = rereviewerDueAt;
        rc.initialReviewScore = initialReviewScore;
        return rc;
    }
```

Add getters at the bottom of the getters block:

```java
    public ReviewerAssignment getRereviewerAssignment() { return rereviewerAssignment; }
    public LocalDateTime getRereviewerDueAt() { return rereviewerDueAt; }
    public BigDecimal getInitialReviewScore() { return initialReviewScore; }
```

- [ ] **Step 2: Add columns to ReviewCaseEntity.java**

Add these fields after the `scoreSheetVersionRef` field (around line 77):

```java
    // --- B4: rereview ---
    @Column(name = "rereviewer_id")
    private String rereviewerId;

    @Column(name = "rereviewer_assigned_at")
    private LocalDateTime rereviewerAssignedAt;

    @Column(name = "rereviewer_due_at")
    private LocalDateTime rereviewerDueAt;

    @Column(name = "initial_review_score", precision = 10, scale = 2)
    private BigDecimal initialReviewScore;
```

Add getters and setters:

```java
    public String getRereviewerId() { return rereviewerId; }
    public void setRereviewerId(String rereviewerId) { this.rereviewerId = rereviewerId; }

    public LocalDateTime getRereviewerAssignedAt() { return rereviewerAssignedAt; }
    public void setRereviewerAssignedAt(LocalDateTime rereviewerAssignedAt) { this.rereviewerAssignedAt = rereviewerAssignedAt; }

    public LocalDateTime getRereviewerDueAt() { return rereviewerDueAt; }
    public void setRereviewerDueAt(LocalDateTime rereviewerDueAt) { this.rereviewerDueAt = rereviewerDueAt; }

    public BigDecimal getInitialReviewScore() { return initialReviewScore; }
    public void setInitialReviewScore(BigDecimal initialReviewScore) { this.initialReviewScore = initialReviewScore; }
```

- [ ] **Step 3: Update ReviewCaseRepositoryImpl toDomain/toEntity**

In `ReviewCaseRepositoryImpl.java`, update `toDomain` (line 37). Add rereviewerAssignment reconstruction after the initial reviewer assignment block, and pass 3 new args to reconstitute:

```java
    private ReviewCase toDomain(ReviewCaseEntity e) {
        ReviewerAssignment assignment = null;
        if (e.getReviewerId() != null && e.getReviewerAssignedAt() != null) {
            assignment = new ReviewerAssignment(e.getReviewerId(), e.getReviewerAssignedAt());
        }

        ReviewerAssignment rereviewerAssignment = null;
        if (e.getRereviewerId() != null && e.getRereviewerAssignedAt() != null) {
            rereviewerAssignment = new ReviewerAssignment(e.getRereviewerId(), e.getRereviewerAssignedAt());
        }

        MedicalRecordType mrt = null;
        if (e.getMedicalRecordType() != null) {
            mrt = MedicalRecordType.valueOf(e.getMedicalRecordType());
        }

        return ReviewCase.reconstitute(
                e.getCaseId(), e.getBatchId(), e.getProjectId(), e.getHospitalId(),
                e.getSourceCaseKey(), mrt,
                e.getStatus(), assignment, e.getDueAt(),
                e.getCancelReason(), e.getReturnReason(), e.getCancelledAt(), e.getClosedAt(),
                e.isRereviewRequired(), e.getFinalScore(), e.getFinalConclusion(),
                e.getLatestInitialSubmissionId(), e.getRuleSnapshotRef(), e.getScoreSheetVersionRef(),
                rereviewerAssignment, e.getRereviewerDueAt(), e.getInitialReviewScore());
    }
```

Update `toEntity` (line 57). Add after the initial reviewer mapping block:

```java
        if (rc.getRereviewerAssignment() != null) {
            e.setRereviewerId(rc.getRereviewerAssignment().getReviewerId());
            e.setRereviewerAssignedAt(rc.getRereviewerAssignment().getAssignedAt());
        }
        e.setRereviewerDueAt(rc.getRereviewerDueAt());
        e.setInitialReviewScore(rc.getInitialReviewScore());
```

- [ ] **Step 4: Run full test suite**

```bash
cd services/quality-inspection-service && ./gradlew test 2>&1 | tail -10
```

Expected: All tests pass. Existing tests use createFromSampling() factory — they never call reconstitute() directly.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(B4): add ReviewCase rereview fields and update entity/repo mapping"
```

---

### Task 5: Modify completeInitialReview for Threshold Evaluation

**Files:**
- Modify: `SVC/domain/reviewcase/ReviewCase.java:168-176` (completeInitialReview)
- Create: `TEST/domain/reviewcase/ReviewCaseRereviewTest.java` (start with threshold tests)

- [ ] **Step 1: Write failing tests for threshold-based completeInitialReview**

Create `TEST/domain/reviewcase/ReviewCaseRereviewTest.java`:

```java
package com.tzuchi.his.quality.inspection.domain.reviewcase;

import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ReviewCaseRereviewTest {

    private ReviewCase caseInProgress() {
        ReviewCase rc = ReviewCase.createFromSampling(
                "c1", "b1", "p1", "H001", "SK-001", MedicalRecordType.OUTPATIENT);
        rc.assignReviewer("reviewer-1", LocalDateTime.of(2026, 12, 31, 23, 59));
        rc.startInitialReview("sub-1", "ss-1");
        return rc;
    }

    // --- completeInitialReview threshold tests ---

    @Test
    void completeInitialReview_scoreAboveThreshold_shouldEscalateToPendingRereview() {
        ReviewCase rc = caseInProgress();
        rc.completeInitialReview(new BigDecimal("90"), new BigDecimal("80"));
        assertEquals(CaseStatus.PENDING_REREVIEW, rc.getStatus());
        assertEquals(new BigDecimal("90"), rc.getInitialReviewScore());
        assertNull(rc.getFinalScore());
    }

    @Test
    void completeInitialReview_scoreEqualToThreshold_shouldStayCompleted() {
        ReviewCase rc = caseInProgress();
        rc.completeInitialReview(new BigDecimal("80"), new BigDecimal("80"));
        assertEquals(CaseStatus.INITIAL_REVIEW_COMPLETED, rc.getStatus());
        assertEquals(new BigDecimal("80"), rc.getInitialReviewScore());
        assertEquals(new BigDecimal("80"), rc.getFinalScore());
    }

    @Test
    void completeInitialReview_scoreBelowThreshold_shouldStayCompleted() {
        ReviewCase rc = caseInProgress();
        rc.completeInitialReview(new BigDecimal("60"), new BigDecimal("80"));
        assertEquals(CaseStatus.INITIAL_REVIEW_COMPLETED, rc.getStatus());
        assertEquals(new BigDecimal("60"), rc.getInitialReviewScore());
        assertEquals(new BigDecimal("60"), rc.getFinalScore());
    }

    @Test
    void completeInitialReview_nullThreshold_shouldStayCompleted() {
        ReviewCase rc = caseInProgress();
        rc.completeInitialReview(new BigDecimal("90"), null);
        assertEquals(CaseStatus.INITIAL_REVIEW_COMPLETED, rc.getStatus());
        assertEquals(new BigDecimal("90"), rc.getInitialReviewScore());
        assertEquals(new BigDecimal("90"), rc.getFinalScore());
    }

    @Test
    void completeInitialReview_wrongState_shouldThrow() {
        ReviewCase rc = ReviewCase.createFromSampling(
                "c1", "b1", "p1", "H001", "SK-001", MedicalRecordType.OUTPATIENT);
        assertThrows(DomainStateConflictException.class,
                () -> rc.completeInitialReview(new BigDecimal("90"), new BigDecimal("80")));
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd services/quality-inspection-service && ./gradlew test --tests "*.ReviewCaseRereviewTest" 2>&1 | tail -5
```

Expected: Compilation error — `completeInitialReview` takes 1 arg, not 2; `getInitialReviewScore()` does not exist.

- [ ] **Step 3: Implement modified completeInitialReview**

Replace the `completeInitialReview` method in `ReviewCase.java` (lines 168-176) with:

```java
    public void completeInitialReview(BigDecimal totalScore, BigDecimal qualityThreshold) {
        if (this.status != CaseStatus.INITIAL_REVIEW_IN_PROGRESS) {
            throw new DomainStateConflictException("CASE_NOT_IN_INITIAL_REVIEW",
                    "Case must be in INITIAL_REVIEW_IN_PROGRESS to complete. Current: " + this.status);
        }
        Objects.requireNonNull(totalScore, "totalScore must not be null");
        this.initialReviewScore = totalScore;
        if (qualityThreshold != null && totalScore.compareTo(qualityThreshold) > 0) {
            this.status = CaseStatus.PENDING_REREVIEW;
        } else {
            this.finalScore = totalScore;
            this.status = CaseStatus.INITIAL_REVIEW_COMPLETED;
        }
    }
```

Also update `updateFinalScore` (line 191) to also track initialReviewScore for initial review revisions:

```java
    public void updateFinalScore(BigDecimal newScore) {
        requireNotTerminal();
        Objects.requireNonNull(newScore, "newScore must not be null");
        this.initialReviewScore = newScore;
        this.finalScore = newScore;
    }
```

- [ ] **Step 4: Fix compilation errors in existing code**

The old `completeInitialReview(BigDecimal)` call in `SubmitInitialReviewUseCase.java` (line 120) will no longer compile. Temporarily change it to pass `null` as threshold (backward compatible — null threshold = no escalation):

```java
        reviewCase.completeInitialReview(result.getTotalScore(), null);
```

This keeps existing behavior intact. Task 11 will properly wire up the real threshold.

- [ ] **Step 5: Run full test suite**

```bash
cd services/quality-inspection-service && ./gradlew test 2>&1 | tail -10
```

Expected: All tests pass — new ReviewCaseRereviewTest tests pass AND existing tests still pass (null threshold = INITIAL_REVIEW_COMPLETED, same as before).

- [ ] **Step 6: Commit**

```bash
git add -A && git commit -m "feat(B4): modify completeInitialReview with threshold-based escalation"
```

---

### Task 6: Add closeWithoutRereview and sendToRereview Domain Methods

**Files:**
- Modify: `SVC/domain/reviewcase/ReviewCase.java` (add 2 methods)
- Modify: `TEST/domain/reviewcase/ReviewCaseRereviewTest.java` (add tests)

- [ ] **Step 1: Write failing tests**

Append to `ReviewCaseRereviewTest.java`:

```java
    private ReviewCase caseAtInitialReviewCompleted() {
        ReviewCase rc = caseInProgress();
        rc.completeInitialReview(new BigDecimal("60"), new BigDecimal("80"));
        // score 60 <= threshold 80 → INITIAL_REVIEW_COMPLETED
        return rc;
    }

    // --- closeWithoutRereview tests ---

    @Test
    void closeWithoutRereview_happyPath_shouldCloseCaseWithInitialScore() {
        ReviewCase rc = caseAtInitialReviewCompleted();
        rc.closeWithoutRereview();
        assertEquals(CaseStatus.CLOSED, rc.getStatus());
        assertEquals(new BigDecimal("60"), rc.getFinalScore());
        assertNotNull(rc.getClosedAt());
    }

    @Test
    void closeWithoutRereview_wrongState_shouldThrow() {
        ReviewCase rc = caseInProgress();
        assertThrows(DomainStateConflictException.class, rc::closeWithoutRereview);
    }

    // --- sendToRereview tests ---

    @Test
    void sendToRereview_happyPath_shouldEscalate() {
        ReviewCase rc = caseAtInitialReviewCompleted();
        rc.sendToRereview();
        assertEquals(CaseStatus.PENDING_REREVIEW, rc.getStatus());
    }

    @Test
    void sendToRereview_wrongState_shouldThrow() {
        ReviewCase rc = caseInProgress();
        assertThrows(DomainStateConflictException.class, rc::sendToRereview);
    }
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd services/quality-inspection-service && ./gradlew test --tests "*.ReviewCaseRereviewTest" 2>&1 | tail -5
```

Expected: Compilation error — `closeWithoutRereview()` and `sendToRereview()` do not exist.

- [ ] **Step 3: Implement the two methods**

Add to `ReviewCase.java` after the `completeInitialReview` method:

```java
    // --- B4 Domain Methods ---

    public void closeWithoutRereview() {
        if (this.status != CaseStatus.INITIAL_REVIEW_COMPLETED) {
            throw new DomainStateConflictException("CASE_NOT_INITIAL_REVIEW_COMPLETED",
                    "Can only close-without-rereview from INITIAL_REVIEW_COMPLETED. Current: " + this.status);
        }
        this.finalScore = this.initialReviewScore;
        this.closedAt = LocalDateTime.now();
        this.status = CaseStatus.CLOSED;
    }

    public void sendToRereview() {
        if (this.status != CaseStatus.INITIAL_REVIEW_COMPLETED) {
            throw new DomainStateConflictException("CASE_NOT_INITIAL_REVIEW_COMPLETED",
                    "Can only send-to-rereview from INITIAL_REVIEW_COMPLETED. Current: " + this.status);
        }
        this.status = CaseStatus.PENDING_REREVIEW;
    }
```

- [ ] **Step 4: Run tests**

```bash
cd services/quality-inspection-service && ./gradlew test --tests "*.ReviewCaseRereviewTest" 2>&1 | tail -5
```

Expected: All tests pass.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(B4): add closeWithoutRereview and sendToRereview domain methods"
```

---

### Task 7: Add Rereviewer Assignment and Start Rereview Domain Methods

**Files:**
- Modify: `SVC/domain/reviewcase/ReviewCase.java` (add 3 methods)
- Modify: `TEST/domain/reviewcase/ReviewCaseRereviewTest.java` (add tests)

- [ ] **Step 1: Write failing tests**

Add import to `ReviewCaseRereviewTest.java`:
```java
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
```

Append test methods:

```java
    private ReviewCase caseAtPendingRereview() {
        ReviewCase rc = caseInProgress();
        rc.completeInitialReview(new BigDecimal("90"), new BigDecimal("80"));
        // score 90 > threshold 80 → PENDING_REREVIEW
        return rc;
    }

    // --- assignRereviewer tests ---

    @Test
    void assignRereviewer_happyPath_shouldSetAssignment() {
        ReviewCase rc = caseAtPendingRereview();
        LocalDateTime dueAt = LocalDateTime.of(2026, 6, 30, 23, 59);
        rc.assignRereviewer("rereviewer-1", dueAt);
        assertNotNull(rc.getRereviewerAssignment());
        assertEquals("rereviewer-1", rc.getRereviewerAssignment().getReviewerId());
        assertEquals(dueAt, rc.getRereviewerDueAt());
        assertEquals(CaseStatus.PENDING_REREVIEW, rc.getStatus());
    }

    @Test
    void assignRereviewer_sameAsInitialReviewer_shouldThrow() {
        ReviewCase rc = caseAtPendingRereview();
        // initial reviewer is "reviewer-1" (set in caseInProgress)
        assertThrows(DomainRuleViolationException.class,
                () -> rc.assignRereviewer("reviewer-1", LocalDateTime.now()));
    }

    @Test
    void assignRereviewer_wrongState_shouldThrow() {
        ReviewCase rc = caseInProgress(); // INITIAL_REVIEW_IN_PROGRESS
        assertThrows(DomainStateConflictException.class,
                () -> rc.assignRereviewer("rereviewer-1", LocalDateTime.now()));
    }

    // --- reassignRereviewer tests ---

    @Test
    void reassignRereviewer_happyPath_shouldReplaceAssignment() {
        ReviewCase rc = caseAtPendingRereview();
        rc.assignRereviewer("rereviewer-1", LocalDateTime.now());
        LocalDateTime newDue = LocalDateTime.of(2026, 7, 15, 23, 59);
        rc.reassignRereviewer("rereviewer-2", newDue);
        assertEquals("rereviewer-2", rc.getRereviewerAssignment().getReviewerId());
        assertEquals(newDue, rc.getRereviewerDueAt());
    }

    @Test
    void reassignRereviewer_sameAsInitialReviewer_shouldThrow() {
        ReviewCase rc = caseAtPendingRereview();
        rc.assignRereviewer("rereviewer-1", LocalDateTime.now());
        assertThrows(DomainRuleViolationException.class,
                () -> rc.reassignRereviewer("reviewer-1", LocalDateTime.now()));
    }

    // --- startRereview tests ---

    @Test
    void startRereview_happyPath_shouldTransitionToInProgress() {
        ReviewCase rc = caseAtPendingRereview();
        rc.assignRereviewer("rereviewer-1", LocalDateTime.now());
        rc.startRereview("rereviewer-1");
        assertEquals(CaseStatus.REREVIEW_IN_PROGRESS, rc.getStatus());
    }

    @Test
    void startRereview_wrongActor_shouldThrow() {
        ReviewCase rc = caseAtPendingRereview();
        rc.assignRereviewer("rereviewer-1", LocalDateTime.now());
        assertThrows(DomainRuleViolationException.class,
                () -> rc.startRereview("someone-else"));
    }

    @Test
    void startRereview_wrongState_shouldThrow() {
        ReviewCase rc = caseInProgress();
        assertThrows(DomainStateConflictException.class,
                () -> rc.startRereview("rereviewer-1"));
    }

    @Test
    void startRereview_noAssignment_shouldThrow() {
        ReviewCase rc = caseAtPendingRereview();
        // no assignRereviewer called
        assertThrows(DomainRuleViolationException.class,
                () -> rc.startRereview("rereviewer-1"));
    }
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd services/quality-inspection-service && ./gradlew test --tests "*.ReviewCaseRereviewTest" 2>&1 | tail -5
```

Expected: Compilation error — `assignRereviewer`, `reassignRereviewer`, `startRereview` do not exist.

- [ ] **Step 3: Implement the three methods**

Add to `ReviewCase.java` after `sendToRereview()`, and add the import:

```java
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
```

```java
    public void assignRereviewer(String rereviewerId, LocalDateTime dueAt) {
        if (this.status != CaseStatus.PENDING_REREVIEW) {
            throw new DomainStateConflictException("CASE_NOT_PENDING_REREVIEW",
                    "Can only assign rereviewer in PENDING_REREVIEW. Current: " + this.status);
        }
        Objects.requireNonNull(rereviewerId, "rereviewerId must not be null");
        if (this.reviewerAssignment != null
                && rereviewerId.equals(this.reviewerAssignment.getReviewerId())) {
            throw new DomainRuleViolationException("REREVIEWER_SAME_AS_REVIEWER",
                    "Re-reviewer must not be the same person as the initial reviewer (D-017)");
        }
        this.rereviewerAssignment = new ReviewerAssignment(rereviewerId, LocalDateTime.now());
        this.rereviewerDueAt = dueAt;
    }

    public void reassignRereviewer(String rereviewerId, LocalDateTime dueAt) {
        if (this.status != CaseStatus.PENDING_REREVIEW) {
            throw new DomainStateConflictException("CASE_NOT_PENDING_REREVIEW",
                    "Can only reassign rereviewer in PENDING_REREVIEW. Current: " + this.status);
        }
        Objects.requireNonNull(rereviewerId, "rereviewerId must not be null");
        if (this.reviewerAssignment != null
                && rereviewerId.equals(this.reviewerAssignment.getReviewerId())) {
            throw new DomainRuleViolationException("REREVIEWER_SAME_AS_REVIEWER",
                    "Re-reviewer must not be the same person as the initial reviewer (D-017)");
        }
        this.rereviewerAssignment = new ReviewerAssignment(rereviewerId, LocalDateTime.now());
        this.rereviewerDueAt = dueAt;
    }

    public void startRereview(String actorId) {
        if (this.status != CaseStatus.PENDING_REREVIEW) {
            throw new DomainStateConflictException("CASE_NOT_PENDING_REREVIEW",
                    "Can only start rereview in PENDING_REREVIEW. Current: " + this.status);
        }
        if (this.rereviewerAssignment == null) {
            throw new DomainRuleViolationException("NO_REREVIEWER_ASSIGNED",
                    "Cannot start rereview: no rereviewer has been assigned");
        }
        Objects.requireNonNull(actorId, "actorId must not be null");
        if (!actorId.equals(this.rereviewerAssignment.getReviewerId())) {
            throw new DomainRuleViolationException("NOT_ASSIGNED_REREVIEWER",
                    "Only the assigned rereviewer can start the rereview");
        }
        this.status = CaseStatus.REREVIEW_IN_PROGRESS;
    }
```

- [ ] **Step 4: Run tests**

```bash
cd services/quality-inspection-service && ./gradlew test --tests "*.ReviewCaseRereviewTest" 2>&1 | tail -5
```

Expected: All tests pass.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(B4): add assignRereviewer, reassignRereviewer, startRereview domain methods"
```

---

### Task 8: Add completeRereview and Rereview Revision Guard Domain Methods

**Files:**
- Modify: `SVC/domain/reviewcase/ReviewCase.java` (add 2 methods + rereview revision guard)
- Modify: `TEST/domain/reviewcase/ReviewCaseRereviewTest.java` (add tests)

- [ ] **Step 1: Write failing tests**

Append to `ReviewCaseRereviewTest.java`:

```java
    private ReviewCase caseAtRereviewInProgress() {
        ReviewCase rc = caseAtPendingRereview();
        rc.assignRereviewer("rereviewer-1", LocalDateTime.of(2026, 6, 30, 23, 59));
        rc.startRereview("rereviewer-1");
        return rc;
    }

    // --- completeRereview tests ---

    @Test
    void completeRereview_happyPath_shouldCloseWithWeightedScore() {
        ReviewCase rc = caseAtRereviewInProgress();
        // initialReviewScore = 90 (from caseAtPendingRereview)
        // rereviewScore = 70, weights = 0.5/0.5 → finalScore = 80.00
        rc.completeRereview(
                new BigDecimal("70"),
                new BigDecimal("0.5"),
                new BigDecimal("0.5"));
        assertEquals(CaseStatus.CLOSED, rc.getStatus());
        assertEquals(0, new BigDecimal("80.00").compareTo(rc.getFinalScore()));
        assertNotNull(rc.getClosedAt());
    }

    @Test
    void completeRereview_asymmetricWeights_shouldCalculateCorrectly() {
        ReviewCase rc = caseAtRereviewInProgress();
        // initialReviewScore = 90, rereviewScore = 70
        // weights = 0.7/0.3 → 90*0.7 + 70*0.3 = 63 + 21 = 84.00
        rc.completeRereview(
                new BigDecimal("70"),
                new BigDecimal("0.7"),
                new BigDecimal("0.3"));
        assertEquals(0, new BigDecimal("84.00").compareTo(rc.getFinalScore()));
    }

    @Test
    void completeRereview_wrongState_shouldThrow() {
        ReviewCase rc = caseAtPendingRereview(); // not REREVIEW_IN_PROGRESS
        assertThrows(DomainStateConflictException.class,
                () -> rc.completeRereview(new BigDecimal("70"), new BigDecimal("0.5"), new BigDecimal("0.5")));
    }

    // --- updateFinalScoreForRereview tests (rereview revision) ---

    @Test
    void updateFinalScoreForRereview_closedCase_shouldUpdateScore() {
        ReviewCase rc = caseAtRereviewInProgress();
        rc.completeRereview(new BigDecimal("70"), new BigDecimal("0.5"), new BigDecimal("0.5"));
        // CLOSED state
        assertEquals(CaseStatus.CLOSED, rc.getStatus());
        // Rereview revision updates final score
        rc.updateFinalScoreForRereview(
                new BigDecimal("80"),
                new BigDecimal("0.5"),
                new BigDecimal("0.5"));
        // 90*0.5 + 80*0.5 = 85.00
        assertEquals(0, new BigDecimal("85.00").compareTo(rc.getFinalScore()));
    }

    @Test
    void updateFinalScoreForRereview_cancelledCase_shouldThrow() {
        ReviewCase rc = caseAtPendingRereview();
        rc.assignRereviewer("rereviewer-1", LocalDateTime.now());
        rc.startRereview("rereviewer-1");
        // Force cancel for test — use reconstitute
        ReviewCase cancelledCase = ReviewCase.reconstitute(
                "c1", "b1", "p1", "H001", "SK-001", MedicalRecordType.OUTPATIENT,
                CaseStatus.CANCELLED, null, null,
                "test", null, LocalDateTime.now(), null,
                false, null, null, null, null, null,
                null, null, null);
        assertThrows(DomainStateConflictException.class,
                () -> cancelledCase.updateFinalScoreForRereview(
                        new BigDecimal("80"), new BigDecimal("0.5"), new BigDecimal("0.5")));
    }
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd services/quality-inspection-service && ./gradlew test --tests "*.ReviewCaseRereviewTest" 2>&1 | tail -5
```

Expected: Compilation error — `completeRereview`, `updateFinalScoreForRereview` do not exist.

- [ ] **Step 3: Implement the methods**

Add to `ReviewCase.java` after `startRereview`:

```java
    public void completeRereview(BigDecimal rereviewScore,
                                  BigDecimal initialWeight,
                                  BigDecimal rereviewWeight) {
        if (this.status != CaseStatus.REREVIEW_IN_PROGRESS) {
            throw new DomainStateConflictException("CASE_NOT_REREVIEW_IN_PROGRESS",
                    "Can only complete rereview in REREVIEW_IN_PROGRESS. Current: " + this.status);
        }
        Objects.requireNonNull(rereviewScore, "rereviewScore must not be null");
        Objects.requireNonNull(initialWeight, "initialWeight must not be null");
        Objects.requireNonNull(rereviewWeight, "rereviewWeight must not be null");
        this.finalScore = this.initialReviewScore.multiply(initialWeight)
                .add(rereviewScore.multiply(rereviewWeight))
                .setScale(2, java.math.RoundingMode.HALF_UP);
        this.closedAt = LocalDateTime.now();
        this.status = CaseStatus.CLOSED;
    }

    public void updateFinalScoreForRereview(BigDecimal newRereviewScore,
                                             BigDecimal initialWeight,
                                             BigDecimal rereviewWeight) {
        requireNotCancelled();
        Objects.requireNonNull(newRereviewScore, "newRereviewScore must not be null");
        this.finalScore = this.initialReviewScore.multiply(initialWeight)
                .add(newRereviewScore.multiply(rereviewWeight))
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private void requireNotCancelled() {
        if (this.status == CaseStatus.CANCELLED) {
            throw new DomainStateConflictException("CASE_CANCELLED",
                    "Operation not allowed on CANCELLED case");
        }
    }
```

- [ ] **Step 4: Run tests**

```bash
cd services/quality-inspection-service && ./gradlew test --tests "*.ReviewCaseRereviewTest" 2>&1 | tail -5
```

Expected: All tests pass.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(B4): add completeRereview and rereview revision scoring methods"
```

---

### Task 9: Add ReviewSubmission.createRereviewDraft

**Files:**
- Modify: `SVC/domain/reviewcase/submission/ReviewSubmission.java` (add factory)
- Modify: `TEST/domain/reviewcase/submission/ReviewSubmissionTest.java` (add test)

- [ ] **Step 1: Write failing test**

Append to `ReviewSubmissionTest.java`:

```java
    @Test
    void createRereviewDraft_shouldCreateWithStageRereview() {
        ReviewSubmission sub = ReviewSubmission.createRereviewDraft("sub-r1", "c1", "rereviewer-1");
        assertEquals("sub-r1", sub.getSubmissionId());
        assertEquals("c1", sub.getCaseId());
        assertEquals("rereviewer-1", sub.getReviewerId());
        assertEquals(ReviewStage.REREVIEW, sub.getStage());
        assertEquals(SubmissionStatus.DRAFT, sub.getStatus());
        assertEquals(1, sub.getRevisionNumber());
    }
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd services/quality-inspection-service && ./gradlew test --tests "*.ReviewSubmissionTest.createRereviewDraft*" 2>&1 | tail -5
```

Expected: Compilation error — `createRereviewDraft` does not exist.

- [ ] **Step 3: Implement the factory method**

Add to `ReviewSubmission.java` after the `createInitialDraft` factory:

```java
    public static ReviewSubmission createRereviewDraft(String submissionId, String caseId, String reviewerId) {
        ReviewSubmission s = new ReviewSubmission();
        s.submissionId = submissionId;
        s.caseId = caseId;
        s.reviewerId = reviewerId;
        s.stage = ReviewStage.REREVIEW;
        s.status = SubmissionStatus.DRAFT;
        s.revisionNumber = 1;
        s.answers = new ArrayList<>();
        s.createdAt = LocalDateTime.now();
        s.updatedAt = LocalDateTime.now();
        return s;
    }
```

- [ ] **Step 4: Run tests**

```bash
cd services/quality-inspection-service && ./gradlew test --tests "*.ReviewSubmissionTest" 2>&1 | tail -5
```

Expected: All tests pass.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(B4): add ReviewSubmission.createRereviewDraft factory"
```

---

### Task 10: Create 6 Domain Event Classes

**Files:**
- Create: `SVC/domain/reviewcase/event/CaseEscalatedToRereviewEvent.java`
- Create: `SVC/domain/reviewcase/event/CaseClosedWithoutRereviewEvent.java`
- Create: `SVC/domain/reviewcase/event/RereviewerAssignedEvent.java`
- Create: `SVC/domain/reviewcase/event/RereviewStartedEvent.java`
- Create: `SVC/domain/reviewcase/event/RereviewSubmittedEvent.java`
- Create: `SVC/domain/reviewcase/event/RereviewRevisedEvent.java`

All events follow the same pattern as `ReviewCaseAssignedEvent`. Each extends `DomainEvent` with a JSON detail string.

- [ ] **Step 1: Create CaseEscalatedToRereviewEvent.java**

```java
package com.tzuchi.his.quality.inspection.domain.reviewcase.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

public class CaseEscalatedToRereviewEvent extends DomainEvent {

    public CaseEscalatedToRereviewEvent(Object source,
                                         String actorUserId,
                                         String actorUserName,
                                         String hospitalId,
                                         String caseId,
                                         String detail) {
        super(source, actorUserId, actorUserName, hospitalId, "ReviewCase", caseId, "CASE_ESCALATED_TO_REREVIEW", detail);
    }
}
```

- [ ] **Step 2: Create CaseClosedWithoutRereviewEvent.java**

```java
package com.tzuchi.his.quality.inspection.domain.reviewcase.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

public class CaseClosedWithoutRereviewEvent extends DomainEvent {

    public CaseClosedWithoutRereviewEvent(Object source,
                                           String actorUserId,
                                           String actorUserName,
                                           String hospitalId,
                                           String caseId,
                                           String detail) {
        super(source, actorUserId, actorUserName, hospitalId, "ReviewCase", caseId, "CASE_CLOSED_WITHOUT_REREVIEW", detail);
    }
}
```

- [ ] **Step 3: Create RereviewerAssignedEvent.java**

```java
package com.tzuchi.his.quality.inspection.domain.reviewcase.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

public class RereviewerAssignedEvent extends DomainEvent {

    public RereviewerAssignedEvent(Object source,
                                    String actorUserId,
                                    String actorUserName,
                                    String hospitalId,
                                    String caseId,
                                    String detail) {
        super(source, actorUserId, actorUserName, hospitalId, "ReviewCase", caseId, "REREVIEWER_ASSIGNED", detail);
    }
}
```

- [ ] **Step 4: Create RereviewStartedEvent.java**

```java
package com.tzuchi.his.quality.inspection.domain.reviewcase.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

public class RereviewStartedEvent extends DomainEvent {

    public RereviewStartedEvent(Object source,
                                 String actorUserId,
                                 String actorUserName,
                                 String hospitalId,
                                 String caseId,
                                 String detail) {
        super(source, actorUserId, actorUserName, hospitalId, "ReviewCase", caseId, "REREVIEW_STARTED", detail);
    }
}
```

- [ ] **Step 5: Create RereviewSubmittedEvent.java**

```java
package com.tzuchi.his.quality.inspection.domain.reviewcase.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

public class RereviewSubmittedEvent extends DomainEvent {

    public RereviewSubmittedEvent(Object source,
                                   String actorUserId,
                                   String actorUserName,
                                   String hospitalId,
                                   String caseId,
                                   String detail) {
        super(source, actorUserId, actorUserName, hospitalId, "ReviewCase", caseId, "REREVIEW_SUBMITTED", detail);
    }
}
```

- [ ] **Step 6: Create RereviewRevisedEvent.java**

```java
package com.tzuchi.his.quality.inspection.domain.reviewcase.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

public class RereviewRevisedEvent extends DomainEvent {

    public RereviewRevisedEvent(Object source,
                                 String actorUserId,
                                 String actorUserName,
                                 String hospitalId,
                                 String caseId,
                                 String detail) {
        super(source, actorUserId, actorUserName, hospitalId, "ReviewCase", caseId, "REREVIEW_REVISED", detail);
    }
}
```

- [ ] **Step 7: Run full test suite (compile check)**

```bash
cd services/quality-inspection-service && ./gradlew test 2>&1 | tail -5
```

Expected: All tests pass — events are just classes, nothing references them yet.

- [ ] **Step 8: Commit**

```bash
git add -A && git commit -m "feat(B4): add 6 rereview domain event classes"
```

---

### Task 11: Modify SubmitInitialReviewUseCase for Threshold Wiring

**Files:**
- Modify: `SVC/application/reviewcase/SubmitInitialReviewUseCase.java` (wire ReviewProjectRepository, load threshold)
- Modify: `TEST/application/reviewcase/SubmitInitialReviewUseCaseTest.java` (update tests)

- [ ] **Step 1: Update existing tests for threshold-aware behavior**

In `SubmitInitialReviewUseCaseTest.java`, you need to:

1. Add `InMemoryProjectRepository` to the test setup
2. Seed a ReviewProject with a published version that has threshold/weights
3. Pass it to the use case constructor
4. Add new test: score above threshold → PENDING_REREVIEW

Add import:
```java
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryProjectRepository;
import java.math.BigDecimal;
```

Replace the `setUp()` method and add new fields:

```java
    private InMemoryProjectRepository projectRepository;

    @BeforeEach
    void setUp() {
        caseRepository = new InMemoryReviewCaseRepository();
        submissionRepository = new InMemoryReviewSubmissionRepository();
        projectRepository = new InMemoryProjectRepository();
        eventPublisher = new CapturingDomainEventPublisher();

        seedProjectWithThreshold("p1", "H001", new BigDecimal("80"));

        useCase = new SubmitInitialReviewUseCase(
                caseRepository, submissionRepository, projectRepository, eventPublisher);
    }

    private void seedProjectWithThreshold(String projectId, String hospitalId, BigDecimal threshold) {
        ReviewProject project = ReviewProject.create(projectId, "Test Project", hospitalId);
        project.updateDraft(
                null, null, null, null,
                threshold,
                new BigDecimal("0.5"), new BigDecimal("0.5"));
        project.publishNewVersion();
        project.activate();
        projectRepository.save(project);
    }
```

Add new test case:
```java
    @Test
    void submit_scoreAboveThreshold_shouldEscalateToPendingRereview() {
        // Setup: create case with high-scoring answers that exceed threshold
        // Use a submission with totalScore = 90 > threshold 80
        ReviewCase rc = seedCase("p1", "H001");
        ReviewSubmission sub = seedDraftSubmission(rc.getCaseId(), REVIEWER_ID);

        // Set answers that result in high score (depends on existing test setup pattern)
        // ... follow existing test pattern for seeding answers
        SubmitInitialReviewCommand cmd = new SubmitInitialReviewCommand();
        cmd.setCaseId(rc.getCaseId());
        cmd.setSubmissionId(sub.getSubmissionId());

        useCase.execute(actorContext(), cmd);

        ReviewCase saved = caseRepository.findById(rc.getCaseId()).orElseThrow();
        // If total score > 80, status should be PENDING_REREVIEW
        // If total score <= 80, status should be INITIAL_REVIEW_COMPLETED
        // (Depends on answer scoring — verify based on actual test fixtures)
    }
```

> **Note to implementer:** The exact test for PENDING_REREVIEW depends on how existing test fixtures compute `totalScore`. Examine the existing happy-path test to determine what score the default answers produce, then set the threshold below that score to trigger escalation. Alternatively, seed a project with `threshold = BigDecimal.ZERO` so that any positive score triggers escalation.

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd services/quality-inspection-service && ./gradlew test --tests "*.SubmitInitialReviewUseCaseTest" 2>&1 | tail -5
```

Expected: Compilation error — SubmitInitialReviewUseCase constructor doesn't accept projectRepository.

- [ ] **Step 3: Modify SubmitInitialReviewUseCase**

Add `ReviewProjectRepository` as a new constructor dependency and load threshold in `execute()`:

1. Add field:
```java
    private final ReviewProjectRepository projectRepository;
```

2. Update constructor:
```java
    public SubmitInitialReviewUseCase(
            ReviewCaseRepository caseRepository,
            ReviewSubmissionRepository submissionRepository,
            ReviewProjectRepository projectRepository,
            DomainEventPublisher eventPublisher) {
        this.caseRepository = caseRepository;
        this.submissionRepository = submissionRepository;
        this.projectRepository = projectRepository;
        this.eventPublisher = eventPublisher;
    }
```

3. In `execute()`, replace the `null` threshold placeholder (from Task 5) with actual loading. After the ReviewCase is loaded and before `completeInitialReview`:

```java
        // Load quality threshold from the project's active version
        ReviewProject project = projectRepository.findById(reviewCase.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("ReviewProject", reviewCase.getProjectId()));
        BigDecimal qualityThreshold = project.getActiveVersion()
                .map(v -> v.getQualityThreshold())
                .orElse(null);

        reviewCase.completeInitialReview(result.getTotalScore(), qualityThreshold);
```

4. Add the CASE_ESCALATED_TO_REREVIEW event publication when status becomes PENDING_REREVIEW (after completeInitialReview):

```java
        if (reviewCase.getStatus() == CaseStatus.PENDING_REREVIEW) {
            eventPublisher.publish(new CaseEscalatedToRereviewEvent(
                    this, actor.getUserId(), actor.getUserName(), reviewCase.getHospitalId(),
                    reviewCase.getCaseId(),
                    String.format("{\"score\":%.2f,\"threshold\":%.2f}",
                            result.getTotalScore(), qualityThreshold)));
        }
```

- [ ] **Step 4: Run tests**

```bash
cd services/quality-inspection-service && ./gradlew test --tests "*.SubmitInitialReviewUseCaseTest" 2>&1 | tail -10
```

Expected: All tests pass. Existing tests that used null threshold still produce INITIAL_REVIEW_COMPLETED.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(B4): wire threshold into SubmitInitialReviewUseCase for auto-escalation"
```

---

### Task 12: Create CloseWithoutRereviewUseCase

**Files:**
- Create: `SVC/application/reviewcase/CloseWithoutRereviewUseCase.java`
- Create: `SVC/application/reviewcase/command/CloseWithoutRereviewCommand.java`
- Create: `TEST/application/reviewcase/CloseWithoutRereviewUseCaseTest.java`

- [ ] **Step 1: Write the command DTO**

Create `CloseWithoutRereviewCommand.java`:

```java
package com.tzuchi.his.quality.inspection.application.reviewcase.command;

import javax.validation.constraints.NotBlank;

public class CloseWithoutRereviewCommand {

    @NotBlank
    private String caseId;

    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }
}
```

- [ ] **Step 2: Write failing test**

Create `CloseWithoutRereviewUseCaseTest.java`:

```java
package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.reviewcase.command.CloseWithoutRereviewCommand;
import com.tzuchi.his.quality.inspection.domain.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.testfixture.CapturingDomainEventPublisher;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryReviewCaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CloseWithoutRereviewUseCaseTest {

    private InMemoryReviewCaseRepository caseRepository;
    private CapturingDomainEventPublisher eventPublisher;
    private CloseWithoutRereviewUseCase useCase;

    @BeforeEach
    void setUp() {
        caseRepository = new InMemoryReviewCaseRepository();
        eventPublisher = new CapturingDomainEventPublisher();
        useCase = new CloseWithoutRereviewUseCase(caseRepository, eventPublisher);
    }

    private ActorContext managerActor() {
        return new ActorContext("mgr-1", "Manager", "H001", DomainRole.HOSPITAL_MANAGER);
    }

    private ReviewCase seedCompletedCase() {
        ReviewCase rc = ReviewCase.createFromSampling(
                "c1", "b1", "p1", "H001", "SK-001", MedicalRecordType.OUTPATIENT);
        rc.assignReviewer("reviewer-1", LocalDateTime.now());
        rc.startInitialReview("sub-1", "ss-1");
        rc.completeInitialReview(new BigDecimal("70"), new BigDecimal("80"));
        // score 70 <= threshold 80 → INITIAL_REVIEW_COMPLETED
        caseRepository.save(rc);
        return rc;
    }

    @Test
    void execute_happyPath_shouldCloseCase() {
        seedCompletedCase();
        CloseWithoutRereviewCommand cmd = new CloseWithoutRereviewCommand();
        cmd.setCaseId("c1");

        useCase.execute(managerActor(), cmd);

        ReviewCase saved = caseRepository.findById("c1").orElseThrow();
        assertEquals(CaseStatus.CLOSED, saved.getStatus());
        assertNotNull(saved.getClosedAt());
        assertEquals(1, eventPublisher.getEvents().size());
    }

    @Test
    void execute_wrongRole_shouldThrow() {
        seedCompletedCase();
        CloseWithoutRereviewCommand cmd = new CloseWithoutRereviewCommand();
        cmd.setCaseId("c1");

        ActorContext wrongActor = new ActorContext("reviewer-1", "Reviewer", "H001", DomainRole.REVIEWER);
        assertThrows(Exception.class, () -> useCase.execute(wrongActor, cmd));
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

```bash
cd services/quality-inspection-service && ./gradlew test --tests "*.CloseWithoutRereviewUseCaseTest" 2>&1 | tail -5
```

Expected: Compilation error — `CloseWithoutRereviewUseCase` does not exist.

- [ ] **Step 4: Implement the use case**

Create `CloseWithoutRereviewUseCase.java`:

```java
package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.reviewcase.command.CloseWithoutRereviewCommand;
import com.tzuchi.his.quality.inspection.domain.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.event.CaseClosedWithoutRereviewEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CloseWithoutRereviewUseCase {

    private final ReviewCaseRepository caseRepository;
    private final DomainEventPublisher eventPublisher;

    public CloseWithoutRereviewUseCase(ReviewCaseRepository caseRepository,
                                        DomainEventPublisher eventPublisher) {
        this.caseRepository = caseRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void execute(ActorContext actor, CloseWithoutRereviewCommand cmd) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        ReviewCase reviewCase = caseRepository.findById(cmd.getCaseId())
                .orElseThrow(() -> new ResourceNotFoundException("ReviewCase", cmd.getCaseId()));
        reviewCase.closeWithoutRereview();
        caseRepository.save(reviewCase);
        eventPublisher.publish(new CaseClosedWithoutRereviewEvent(
                this, actor.getUserId(), actor.getUserName(), reviewCase.getHospitalId(),
                reviewCase.getCaseId(),
                String.format("{\"finalScore\":%.2f}", reviewCase.getFinalScore())));
    }
}
```

- [ ] **Step 5: Run tests**

```bash
cd services/quality-inspection-service && ./gradlew test --tests "*.CloseWithoutRereviewUseCaseTest" 2>&1 | tail -5
```

Expected: All tests pass.

- [ ] **Step 6: Commit**

```bash
git add -A && git commit -m "feat(B4): add CloseWithoutRereviewUseCase"
```

---

### Task 13: Create SendToRereviewUseCase

**Files:**
- Create: `SVC/application/reviewcase/SendToRereviewUseCase.java`
- Create: `SVC/application/reviewcase/command/SendToRereviewCommand.java`
- Create: `TEST/application/reviewcase/SendToRereviewUseCaseTest.java`

- [ ] **Step 1: Write the command DTO**

Create `SendToRereviewCommand.java`:

```java
package com.tzuchi.his.quality.inspection.application.reviewcase.command;

import javax.validation.constraints.NotBlank;

public class SendToRereviewCommand {

    @NotBlank
    private String caseId;

    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }
}
```

- [ ] **Step 2: Write failing test**

Create `SendToRereviewUseCaseTest.java`:

```java
package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.reviewcase.command.SendToRereviewCommand;
import com.tzuchi.his.quality.inspection.domain.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.testfixture.CapturingDomainEventPublisher;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryReviewCaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SendToRereviewUseCaseTest {

    private InMemoryReviewCaseRepository caseRepository;
    private CapturingDomainEventPublisher eventPublisher;
    private SendToRereviewUseCase useCase;

    @BeforeEach
    void setUp() {
        caseRepository = new InMemoryReviewCaseRepository();
        eventPublisher = new CapturingDomainEventPublisher();
        useCase = new SendToRereviewUseCase(caseRepository, eventPublisher);
    }

    private ActorContext managerActor() {
        return new ActorContext("mgr-1", "Manager", "H001", DomainRole.HOSPITAL_MANAGER);
    }

    private ReviewCase seedCompletedCase() {
        ReviewCase rc = ReviewCase.createFromSampling(
                "c1", "b1", "p1", "H001", "SK-001", MedicalRecordType.OUTPATIENT);
        rc.assignReviewer("reviewer-1", LocalDateTime.now());
        rc.startInitialReview("sub-1", "ss-1");
        rc.completeInitialReview(new BigDecimal("70"), new BigDecimal("80"));
        caseRepository.save(rc);
        return rc;
    }

    @Test
    void execute_happyPath_shouldEscalate() {
        seedCompletedCase();
        SendToRereviewCommand cmd = new SendToRereviewCommand();
        cmd.setCaseId("c1");

        useCase.execute(managerActor(), cmd);

        ReviewCase saved = caseRepository.findById("c1").orElseThrow();
        assertEquals(CaseStatus.PENDING_REREVIEW, saved.getStatus());
        assertEquals(1, eventPublisher.getEvents().size());
    }

    @Test
    void execute_wrongRole_shouldThrow() {
        seedCompletedCase();
        SendToRereviewCommand cmd = new SendToRereviewCommand();
        cmd.setCaseId("c1");

        ActorContext wrongActor = new ActorContext("reviewer-1", "Reviewer", "H001", DomainRole.REVIEWER);
        assertThrows(Exception.class, () -> useCase.execute(wrongActor, cmd));
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

```bash
cd services/quality-inspection-service && ./gradlew test --tests "*.SendToRereviewUseCaseTest" 2>&1 | tail -5
```

Expected: Compilation error.

- [ ] **Step 4: Implement the use case**

Create `SendToRereviewUseCase.java`:

```java
package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.reviewcase.command.SendToRereviewCommand;
import com.tzuchi.his.quality.inspection.domain.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.event.CaseEscalatedToRereviewEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SendToRereviewUseCase {

    private final ReviewCaseRepository caseRepository;
    private final DomainEventPublisher eventPublisher;

    public SendToRereviewUseCase(ReviewCaseRepository caseRepository,
                                  DomainEventPublisher eventPublisher) {
        this.caseRepository = caseRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void execute(ActorContext actor, SendToRereviewCommand cmd) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        ReviewCase reviewCase = caseRepository.findById(cmd.getCaseId())
                .orElseThrow(() -> new ResourceNotFoundException("ReviewCase", cmd.getCaseId()));
        reviewCase.sendToRereview();
        caseRepository.save(reviewCase);
        eventPublisher.publish(new CaseEscalatedToRereviewEvent(
                this, actor.getUserId(), actor.getUserName(), reviewCase.getHospitalId(),
                reviewCase.getCaseId(),
                "{\"trigger\":\"manual\"}"));
    }
}
```

- [ ] **Step 5: Run tests**

```bash
cd services/quality-inspection-service && ./gradlew test --tests "*.SendToRereviewUseCaseTest" 2>&1 | tail -5
```

Expected: All tests pass.

- [ ] **Step 6: Commit**

```bash
git add -A && git commit -m "feat(B4): add SendToRereviewUseCase"
```

---

### Task 14: Create AssignRereviewerUseCase and ReassignRereviewerUseCase

**Files:**
- Create: `SVC/application/reviewcase/AssignRereviewerUseCase.java`
- Create: `SVC/application/reviewcase/command/AssignRereviewerCommand.java`
- Create: `SVC/application/reviewcase/ReassignRereviewerUseCase.java`
- Create: `SVC/application/reviewcase/command/ReassignRereviewerCommand.java`
- Create: `TEST/application/reviewcase/AssignRereviewerUseCaseTest.java`
- Create: `TEST/application/reviewcase/ReassignRereviewerUseCaseTest.java`

- [ ] **Step 1: Write both command DTOs**

Create `AssignRereviewerCommand.java`:

```java
package com.tzuchi.his.quality.inspection.application.reviewcase.command;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public class AssignRereviewerCommand {

    @NotBlank
    private String caseId;
    @NotBlank
    private String rereviewerId;
    private LocalDateTime dueAt;

    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }

    public String getRereviewerId() { return rereviewerId; }
    public void setRereviewerId(String rereviewerId) { this.rereviewerId = rereviewerId; }

    public LocalDateTime getDueAt() { return dueAt; }
    public void setDueAt(LocalDateTime dueAt) { this.dueAt = dueAt; }
}
```

Create `ReassignRereviewerCommand.java`:

```java
package com.tzuchi.his.quality.inspection.application.reviewcase.command;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public class ReassignRereviewerCommand {

    @NotBlank
    private String caseId;
    @NotBlank
    private String rereviewerId;
    private LocalDateTime dueAt;

    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }

    public String getRereviewerId() { return rereviewerId; }
    public void setRereviewerId(String rereviewerId) { this.rereviewerId = rereviewerId; }

    public LocalDateTime getDueAt() { return dueAt; }
    public void setDueAt(LocalDateTime dueAt) { this.dueAt = dueAt; }
}
```

- [ ] **Step 2: Write failing tests for AssignRereviewerUseCase**

Create `AssignRereviewerUseCaseTest.java`:

```java
package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.reviewcase.command.AssignRereviewerCommand;
import com.tzuchi.his.quality.inspection.domain.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.testfixture.CapturingDomainEventPublisher;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryReviewCaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AssignRereviewerUseCaseTest {

    private InMemoryReviewCaseRepository caseRepository;
    private CapturingDomainEventPublisher eventPublisher;
    private AssignRereviewerUseCase useCase;

    @BeforeEach
    void setUp() {
        caseRepository = new InMemoryReviewCaseRepository();
        eventPublisher = new CapturingDomainEventPublisher();
        useCase = new AssignRereviewerUseCase(caseRepository, eventPublisher);
    }

    private ActorContext managerActor() {
        return new ActorContext("mgr-1", "Manager", "H001", DomainRole.HOSPITAL_MANAGER);
    }

    private ReviewCase seedPendingRereviewCase() {
        ReviewCase rc = ReviewCase.createFromSampling(
                "c1", "b1", "p1", "H001", "SK-001", MedicalRecordType.OUTPATIENT);
        rc.assignReviewer("reviewer-1", LocalDateTime.now());
        rc.startInitialReview("sub-1", "ss-1");
        rc.completeInitialReview(new BigDecimal("90"), new BigDecimal("80"));
        caseRepository.save(rc);
        return rc;
    }

    @Test
    void execute_happyPath_shouldAssignRereviewer() {
        seedPendingRereviewCase();
        AssignRereviewerCommand cmd = new AssignRereviewerCommand();
        cmd.setCaseId("c1");
        cmd.setRereviewerId("rereviewer-1");
        cmd.setDueAt(LocalDateTime.of(2026, 6, 30, 23, 59));

        useCase.execute(managerActor(), cmd);

        ReviewCase saved = caseRepository.findById("c1").orElseThrow();
        assertNotNull(saved.getRereviewerAssignment());
        assertEquals("rereviewer-1", saved.getRereviewerAssignment().getReviewerId());
        assertEquals(1, eventPublisher.getEvents().size());
    }

    @Test
    void execute_sameAsInitialReviewer_shouldThrow() {
        seedPendingRereviewCase();
        AssignRereviewerCommand cmd = new AssignRereviewerCommand();
        cmd.setCaseId("c1");
        cmd.setRereviewerId("reviewer-1"); // same as initial reviewer
        cmd.setDueAt(LocalDateTime.now());

        assertThrows(Exception.class, () -> useCase.execute(managerActor(), cmd));
    }
}
```

- [ ] **Step 3: Write failing tests for ReassignRereviewerUseCase**

Create `ReassignRereviewerUseCaseTest.java`:

```java
package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.reviewcase.command.ReassignRereviewerCommand;
import com.tzuchi.his.quality.inspection.domain.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.testfixture.CapturingDomainEventPublisher;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryReviewCaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ReassignRereviewerUseCaseTest {

    private InMemoryReviewCaseRepository caseRepository;
    private CapturingDomainEventPublisher eventPublisher;
    private ReassignRereviewerUseCase useCase;

    @BeforeEach
    void setUp() {
        caseRepository = new InMemoryReviewCaseRepository();
        eventPublisher = new CapturingDomainEventPublisher();
        useCase = new ReassignRereviewerUseCase(caseRepository, eventPublisher);
    }

    private ActorContext managerActor() {
        return new ActorContext("mgr-1", "Manager", "H001", DomainRole.HOSPITAL_MANAGER);
    }

    private ReviewCase seedAssignedRereviewCase() {
        ReviewCase rc = ReviewCase.createFromSampling(
                "c1", "b1", "p1", "H001", "SK-001", MedicalRecordType.OUTPATIENT);
        rc.assignReviewer("reviewer-1", LocalDateTime.now());
        rc.startInitialReview("sub-1", "ss-1");
        rc.completeInitialReview(new BigDecimal("90"), new BigDecimal("80"));
        rc.assignRereviewer("rereviewer-1", LocalDateTime.now());
        caseRepository.save(rc);
        return rc;
    }

    @Test
    void execute_happyPath_shouldReassign() {
        seedAssignedRereviewCase();
        ReassignRereviewerCommand cmd = new ReassignRereviewerCommand();
        cmd.setCaseId("c1");
        cmd.setRereviewerId("rereviewer-2");
        cmd.setDueAt(LocalDateTime.of(2026, 7, 15, 23, 59));

        useCase.execute(managerActor(), cmd);

        ReviewCase saved = caseRepository.findById("c1").orElseThrow();
        assertEquals("rereviewer-2", saved.getRereviewerAssignment().getReviewerId());
        assertEquals(1, eventPublisher.getEvents().size());
    }

    @Test
    void execute_sameAsInitialReviewer_shouldThrow() {
        seedAssignedRereviewCase();
        ReassignRereviewerCommand cmd = new ReassignRereviewerCommand();
        cmd.setCaseId("c1");
        cmd.setRereviewerId("reviewer-1");
        cmd.setDueAt(LocalDateTime.now());

        assertThrows(Exception.class, () -> useCase.execute(managerActor(), cmd));
    }
}
```

- [ ] **Step 4: Run tests to verify they fail**

```bash
cd services/quality-inspection-service && ./gradlew test --tests "*.AssignRereviewerUseCaseTest" --tests "*.ReassignRereviewerUseCaseTest" 2>&1 | tail -5
```

Expected: Compilation error.

- [ ] **Step 5: Implement AssignRereviewerUseCase**

```java
package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.reviewcase.command.AssignRereviewerCommand;
import com.tzuchi.his.quality.inspection.domain.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.event.RereviewerAssignedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssignRereviewerUseCase {

    private final ReviewCaseRepository caseRepository;
    private final DomainEventPublisher eventPublisher;

    public AssignRereviewerUseCase(ReviewCaseRepository caseRepository,
                                    DomainEventPublisher eventPublisher) {
        this.caseRepository = caseRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void execute(ActorContext actor, AssignRereviewerCommand cmd) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        ReviewCase reviewCase = caseRepository.findById(cmd.getCaseId())
                .orElseThrow(() -> new ResourceNotFoundException("ReviewCase", cmd.getCaseId()));
        reviewCase.assignRereviewer(cmd.getRereviewerId(), cmd.getDueAt());
        caseRepository.save(reviewCase);
        eventPublisher.publish(new RereviewerAssignedEvent(
                this, actor.getUserId(), actor.getUserName(), reviewCase.getHospitalId(),
                reviewCase.getCaseId(),
                String.format("{\"rereviewerId\":\"%s\"}", cmd.getRereviewerId())));
    }
}
```

- [ ] **Step 6: Implement ReassignRereviewerUseCase**

```java
package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.reviewcase.command.ReassignRereviewerCommand;
import com.tzuchi.his.quality.inspection.domain.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.event.RereviewerAssignedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReassignRereviewerUseCase {

    private final ReviewCaseRepository caseRepository;
    private final DomainEventPublisher eventPublisher;

    public ReassignRereviewerUseCase(ReviewCaseRepository caseRepository,
                                      DomainEventPublisher eventPublisher) {
        this.caseRepository = caseRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void execute(ActorContext actor, ReassignRereviewerCommand cmd) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        ReviewCase reviewCase = caseRepository.findById(cmd.getCaseId())
                .orElseThrow(() -> new ResourceNotFoundException("ReviewCase", cmd.getCaseId()));
        reviewCase.reassignRereviewer(cmd.getRereviewerId(), cmd.getDueAt());
        caseRepository.save(reviewCase);
        eventPublisher.publish(new RereviewerAssignedEvent(
                this, actor.getUserId(), actor.getUserName(), reviewCase.getHospitalId(),
                reviewCase.getCaseId(),
                String.format("{\"rereviewerId\":\"%s\",\"reassignment\":true}", cmd.getRereviewerId())));
    }
}
```

- [ ] **Step 7: Run tests**

```bash
cd services/quality-inspection-service && ./gradlew test --tests "*.AssignRereviewerUseCaseTest" --tests "*.ReassignRereviewerUseCaseTest" 2>&1 | tail -5
```

Expected: All tests pass.

- [ ] **Step 8: Commit**

```bash
git add -A && git commit -m "feat(B4): add AssignRereviewerUseCase and ReassignRereviewerUseCase"
```

---

### Task 15: Create StartRereviewUseCase

**Files:**
- Create: `SVC/application/reviewcase/StartRereviewUseCase.java`
- Create: `SVC/application/reviewcase/command/StartRereviewCommand.java`
- Create: `TEST/application/reviewcase/StartRereviewUseCaseTest.java`

- [ ] **Step 1: Write the command DTO**

Create `StartRereviewCommand.java`:

```java
package com.tzuchi.his.quality.inspection.application.reviewcase.command;

import javax.validation.constraints.NotBlank;

public class StartRereviewCommand {

    @NotBlank
    private String caseId;

    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }
}
```

- [ ] **Step 2: Write failing test**

Create `StartRereviewUseCaseTest.java`:

```java
package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.reviewcase.command.StartRereviewCommand;
import com.tzuchi.his.quality.inspection.domain.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewStage;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.SubmissionStatus;
import com.tzuchi.his.quality.inspection.testfixture.CapturingDomainEventPublisher;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryReviewCaseRepository;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryReviewSubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StartRereviewUseCaseTest {

    private InMemoryReviewCaseRepository caseRepository;
    private InMemoryReviewSubmissionRepository submissionRepository;
    private CapturingDomainEventPublisher eventPublisher;
    private StartRereviewUseCase useCase;

    @BeforeEach
    void setUp() {
        caseRepository = new InMemoryReviewCaseRepository();
        submissionRepository = new InMemoryReviewSubmissionRepository();
        eventPublisher = new CapturingDomainEventPublisher();
        useCase = new StartRereviewUseCase(caseRepository, submissionRepository, eventPublisher);
    }

    private ActorContext rereviewerActor() {
        return new ActorContext("rereviewer-1", "ReReviewer", "H001", DomainRole.REREVIEWER);
    }

    private ReviewCase seedAssignedCase() {
        ReviewCase rc = ReviewCase.createFromSampling(
                "c1", "b1", "p1", "H001", "SK-001", MedicalRecordType.OUTPATIENT);
        rc.assignReviewer("reviewer-1", LocalDateTime.now());
        rc.startInitialReview("sub-1", "ss-1");
        rc.completeInitialReview(new BigDecimal("90"), new BigDecimal("80"));
        rc.assignRereviewer("rereviewer-1", LocalDateTime.of(2026, 6, 30, 23, 59));
        caseRepository.save(rc);
        return rc;
    }

    @Test
    void execute_happyPath_shouldTransitionAndCreateDraft() {
        seedAssignedCase();
        StartRereviewCommand cmd = new StartRereviewCommand();
        cmd.setCaseId("c1");

        useCase.execute(rereviewerActor(), cmd);

        ReviewCase saved = caseRepository.findById("c1").orElseThrow();
        assertEquals(CaseStatus.REREVIEW_IN_PROGRESS, saved.getStatus());

        // A rereview draft submission should be created
        List<ReviewSubmission> subs = submissionRepository.findByCaseId("c1");
        ReviewSubmission rereviewDraft = subs.stream()
                .filter(s -> s.getStage() == ReviewStage.REREVIEW)
                .findFirst().orElseThrow();
        assertEquals(SubmissionStatus.DRAFT, rereviewDraft.getStatus());
        assertEquals("rereviewer-1", rereviewDraft.getReviewerId());
        assertEquals(1, eventPublisher.getEvents().size());
    }

    @Test
    void execute_wrongActor_shouldThrow() {
        seedAssignedCase();
        StartRereviewCommand cmd = new StartRereviewCommand();
        cmd.setCaseId("c1");

        ActorContext wrongActor = new ActorContext("someone-else", "Other", "H001", DomainRole.REREVIEWER);
        assertThrows(Exception.class, () -> useCase.execute(wrongActor, cmd));
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

```bash
cd services/quality-inspection-service && ./gradlew test --tests "*.StartRereviewUseCaseTest" 2>&1 | tail -5
```

Expected: Compilation error.

- [ ] **Step 4: Implement the use case**

Create `StartRereviewUseCase.java`:

```java
package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.reviewcase.command.StartRereviewCommand;
import com.tzuchi.his.quality.inspection.domain.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.event.RereviewStartedEvent;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class StartRereviewUseCase {

    private final ReviewCaseRepository caseRepository;
    private final ReviewSubmissionRepository submissionRepository;
    private final DomainEventPublisher eventPublisher;

    public StartRereviewUseCase(ReviewCaseRepository caseRepository,
                                 ReviewSubmissionRepository submissionRepository,
                                 DomainEventPublisher eventPublisher) {
        this.caseRepository = caseRepository;
        this.submissionRepository = submissionRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void execute(ActorContext actor, StartRereviewCommand cmd) {
        actor.requireRole(DomainRole.REREVIEWER);
        ReviewCase reviewCase = caseRepository.findById(cmd.getCaseId())
                .orElseThrow(() -> new ResourceNotFoundException("ReviewCase", cmd.getCaseId()));
        reviewCase.startRereview(actor.getUserId());
        ReviewSubmission rereviewDraft = ReviewSubmission.createRereviewDraft(
                UUID.randomUUID().toString(), cmd.getCaseId(), actor.getUserId());
        submissionRepository.save(rereviewDraft);
        caseRepository.save(reviewCase);
        eventPublisher.publish(new RereviewStartedEvent(
                this, actor.getUserId(), actor.getUserName(), reviewCase.getHospitalId(),
                reviewCase.getCaseId(),
                String.format("{\"submissionId\":\"%s\"}", rereviewDraft.getSubmissionId())));
    }
}
```

- [ ] **Step 5: Run tests**

```bash
cd services/quality-inspection-service && ./gradlew test --tests "*.StartRereviewUseCaseTest" 2>&1 | tail -5
```

Expected: All tests pass.

- [ ] **Step 6: Commit**

```bash
git add -A && git commit -m "feat(B4): add StartRereviewUseCase"
```

---

### Task 16: Create SaveRereviewDraftUseCase

**Files:**
- Create: `SVC/application/reviewcase/SaveRereviewDraftUseCase.java`
- Create: `TEST/application/reviewcase/SaveRereviewDraftUseCaseTest.java`

This use case mirrors `SaveDraftUseCase` (initial review) but validates actor is the assigned rereviewer.

- [ ] **Step 1: Write failing test**

Create `SaveRereviewDraftUseCaseTest.java`:

```java
package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.reviewcase.command.SaveDraftCommand;
import com.tzuchi.his.quality.inspection.domain.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.SubmissionStatus;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryReviewCaseRepository;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryReviewSubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class SaveRereviewDraftUseCaseTest {

    private InMemoryReviewCaseRepository caseRepository;
    private InMemoryReviewSubmissionRepository submissionRepository;
    private SaveRereviewDraftUseCase useCase;

    @BeforeEach
    void setUp() {
        caseRepository = new InMemoryReviewCaseRepository();
        submissionRepository = new InMemoryReviewSubmissionRepository();
        useCase = new SaveRereviewDraftUseCase(caseRepository, submissionRepository);
    }

    private ActorContext rereviewerActor() {
        return new ActorContext("rereviewer-1", "ReReviewer", "H001", DomainRole.REREVIEWER);
    }

    private void seedCaseAndDraft() {
        ReviewCase rc = ReviewCase.createFromSampling(
                "c1", "b1", "p1", "H001", "SK-001", MedicalRecordType.OUTPATIENT);
        rc.assignReviewer("reviewer-1", LocalDateTime.now());
        rc.startInitialReview("sub-init", "ss-1");
        rc.completeInitialReview(new BigDecimal("90"), new BigDecimal("80"));
        rc.assignRereviewer("rereviewer-1", LocalDateTime.of(2026, 6, 30, 23, 59));
        rc.startRereview("rereviewer-1");
        caseRepository.save(rc);

        ReviewSubmission draft = ReviewSubmission.createRereviewDraft("sub-r1", "c1", "rereviewer-1");
        submissionRepository.save(draft);
    }

    @Test
    void execute_happyPath_shouldSaveDraft() {
        seedCaseAndDraft();
        SaveDraftCommand cmd = new SaveDraftCommand();
        cmd.setCaseId("c1");
        cmd.setSubmissionId("sub-r1");
        cmd.setAnswers(Collections.emptyList());

        useCase.execute(rereviewerActor(), cmd);

        ReviewSubmission saved = submissionRepository.findById("sub-r1").orElseThrow();
        assertEquals(SubmissionStatus.DRAFT, saved.getStatus());
    }

    @Test
    void execute_wrongActor_shouldThrow() {
        seedCaseAndDraft();
        SaveDraftCommand cmd = new SaveDraftCommand();
        cmd.setCaseId("c1");
        cmd.setSubmissionId("sub-r1");
        cmd.setAnswers(Collections.emptyList());

        ActorContext wrongActor = new ActorContext("someone-else", "Other", "H001", DomainRole.REREVIEWER);
        assertThrows(Exception.class, () -> useCase.execute(wrongActor, cmd));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd services/quality-inspection-service && ./gradlew test --tests "*.SaveRereviewDraftUseCaseTest" 2>&1 | tail -5
```

Expected: Compilation error.

- [ ] **Step 3: Implement the use case**

Create `SaveRereviewDraftUseCase.java`:

```java
package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.reviewcase.command.SaveDraftCommand;
import com.tzuchi.his.quality.inspection.domain.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SaveRereviewDraftUseCase {

    private final ReviewCaseRepository caseRepository;
    private final ReviewSubmissionRepository submissionRepository;

    public SaveRereviewDraftUseCase(ReviewCaseRepository caseRepository,
                                     ReviewSubmissionRepository submissionRepository) {
        this.caseRepository = caseRepository;
        this.submissionRepository = submissionRepository;
    }

    @Transactional
    public void execute(ActorContext actor, SaveDraftCommand cmd) {
        actor.requireRole(DomainRole.REREVIEWER);
        ReviewCase reviewCase = caseRepository.findById(cmd.getCaseId())
                .orElseThrow(() -> new ResourceNotFoundException("ReviewCase", cmd.getCaseId()));
        if (reviewCase.getRereviewerAssignment() == null
                || !actor.getUserId().equals(reviewCase.getRereviewerAssignment().getReviewerId())) {
            throw new DomainRuleViolationException("NOT_ASSIGNED_REREVIEWER",
                    "Only the assigned rereviewer can save this draft");
        }
        ReviewSubmission submission = submissionRepository.findById(cmd.getSubmissionId())
                .orElseThrow(() -> new ResourceNotFoundException("ReviewSubmission", cmd.getSubmissionId()));
        submission.updateDraft(cmd.getAnswers());
        submissionRepository.save(submission);
    }
}
```

- [ ] **Step 4: Run tests**

```bash
cd services/quality-inspection-service && ./gradlew test --tests "*.SaveRereviewDraftUseCaseTest" 2>&1 | tail -5
```

Expected: All tests pass.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(B4): add SaveRereviewDraftUseCase"
```

---

### Task 17: Create SubmitRereviewUseCase

**Files:**
- Create: `SVC/application/reviewcase/SubmitRereviewUseCase.java`
- Create: `SVC/application/reviewcase/command/SubmitRereviewCommand.java`
- Create: `TEST/application/reviewcase/SubmitRereviewUseCaseTest.java`

This is the heaviest use case — it submits the rereview, calculates the weighted final score, and closes the case.

- [ ] **Step 1: Write the command DTO**

Create `SubmitRereviewCommand.java`:

```java
package com.tzuchi.his.quality.inspection.application.reviewcase.command;

import com.tzuchi.his.quality.inspection.application.reviewcase.dto.AnswerDto;

import javax.validation.constraints.NotBlank;
import java.util.List;

public class SubmitRereviewCommand {

    @NotBlank
    private String caseId;
    @NotBlank
    private String submissionId;
    private List<AnswerDto> answers;

    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }

    public String getSubmissionId() { return submissionId; }
    public void setSubmissionId(String submissionId) { this.submissionId = submissionId; }

    public List<AnswerDto> getAnswers() { return answers; }
    public void setAnswers(List<AnswerDto> answers) { this.answers = answers; }
}
```

- [ ] **Step 2: Write failing test**

Create `SubmitRereviewUseCaseTest.java`:

```java
package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.reviewcase.command.SubmitRereviewCommand;
import com.tzuchi.his.quality.inspection.domain.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.SubmissionStatus;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetDefinition;
import com.tzuchi.his.quality.inspection.testfixture.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class SubmitRereviewUseCaseTest {

    private InMemoryReviewCaseRepository caseRepository;
    private InMemoryReviewSubmissionRepository submissionRepository;
    private InMemoryProjectRepository projectRepository;
    private InMemoryScoreSheetRepository scoreSheetRepository;
    private CapturingDomainEventPublisher eventPublisher;
    private SubmitRereviewUseCase useCase;

    @BeforeEach
    void setUp() {
        caseRepository = new InMemoryReviewCaseRepository();
        submissionRepository = new InMemoryReviewSubmissionRepository();
        projectRepository = new InMemoryProjectRepository();
        scoreSheetRepository = new InMemoryScoreSheetRepository();
        eventPublisher = new CapturingDomainEventPublisher();
        useCase = new SubmitRereviewUseCase(
                caseRepository, submissionRepository, projectRepository,
                scoreSheetRepository, eventPublisher);

        seedProjectAndScoreSheet();
    }

    private void seedProjectAndScoreSheet() {
        // Create a score sheet with sections/items for scoring
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft("ss-1", "Score Sheet", "H001");
        ss.addSection("sec-1", "Section 1", 1);
        ss.addItemToSection("sec-1", "item-1", "Item 1", 100, 1);
        ss.publishVersion();
        scoreSheetRepository.save(ss);

        ReviewProject project = ReviewProject.create("p1", "Test Project", "H001");
        project.updateDraft(null, null, null, null,
                new BigDecimal("80"), new BigDecimal("0.5"), new BigDecimal("0.5"));
        project.bindScoreSheet("ss-1");
        project.publishNewVersion();
        project.activate();
        projectRepository.save(project);
    }

    private ActorContext rereviewerActor() {
        return new ActorContext("rereviewer-1", "ReReviewer", "H001", DomainRole.REREVIEWER);
    }

    private ReviewCase seedRereviewInProgressCase() {
        ReviewCase rc = ReviewCase.createFromSampling(
                "c1", "b1", "p1", "H001", "SK-001", MedicalRecordType.OUTPATIENT);
        rc.assignReviewer("reviewer-1", LocalDateTime.now());
        rc.startInitialReview("sub-init", "ss-1");
        rc.completeInitialReview(new BigDecimal("90"), new BigDecimal("80"));
        rc.assignRereviewer("rereviewer-1", LocalDateTime.of(2026, 6, 30, 23, 59));
        rc.startRereview("rereviewer-1");
        caseRepository.save(rc);

        ReviewSubmission draft = ReviewSubmission.createRereviewDraft("sub-r1", "c1", "rereviewer-1");
        submissionRepository.save(draft);
        return rc;
    }

    @Test
    void execute_happyPath_shouldCloseWithWeightedScore() {
        seedRereviewInProgressCase();
        SubmitRereviewCommand cmd = new SubmitRereviewCommand();
        cmd.setCaseId("c1");
        cmd.setSubmissionId("sub-r1");
        cmd.setAnswers(Collections.emptyList());

        useCase.execute(rereviewerActor(), cmd);

        ReviewCase saved = caseRepository.findById("c1").orElseThrow();
        assertEquals(CaseStatus.CLOSED, saved.getStatus());
        assertNotNull(saved.getFinalScore());
        assertNotNull(saved.getClosedAt());

        ReviewSubmission sub = submissionRepository.findById("sub-r1").orElseThrow();
        assertEquals(SubmissionStatus.SUBMITTED, sub.getStatus());
        assertTrue(eventPublisher.getEvents().size() >= 1);
    }

    @Test
    void execute_wrongActor_shouldThrow() {
        seedRereviewInProgressCase();
        SubmitRereviewCommand cmd = new SubmitRereviewCommand();
        cmd.setCaseId("c1");
        cmd.setSubmissionId("sub-r1");
        cmd.setAnswers(Collections.emptyList());

        ActorContext wrongActor = new ActorContext("someone-else", "Other", "H001", DomainRole.REREVIEWER);
        assertThrows(Exception.class, () -> useCase.execute(wrongActor, cmd));
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

```bash
cd services/quality-inspection-service && ./gradlew test --tests "*.SubmitRereviewUseCaseTest" 2>&1 | tail -5
```

Expected: Compilation error.

- [ ] **Step 4: Implement the use case**

Create `SubmitRereviewUseCase.java`:

```java
package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.reviewcase.command.SubmitRereviewCommand;
import com.tzuchi.his.quality.inspection.domain.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.project.ProjectVersion;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.event.RereviewSubmittedEvent;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmissionRepository;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetDefinition;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class SubmitRereviewUseCase {

    private final ReviewCaseRepository caseRepository;
    private final ReviewSubmissionRepository submissionRepository;
    private final ReviewProjectRepository projectRepository;
    private final ScoreSheetRepository scoreSheetRepository;
    private final DomainEventPublisher eventPublisher;

    public SubmitRereviewUseCase(ReviewCaseRepository caseRepository,
                                  ReviewSubmissionRepository submissionRepository,
                                  ReviewProjectRepository projectRepository,
                                  ScoreSheetRepository scoreSheetRepository,
                                  DomainEventPublisher eventPublisher) {
        this.caseRepository = caseRepository;
        this.submissionRepository = submissionRepository;
        this.projectRepository = projectRepository;
        this.scoreSheetRepository = scoreSheetRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void execute(ActorContext actor, SubmitRereviewCommand cmd) {
        actor.requireRole(DomainRole.REREVIEWER);
        ReviewCase reviewCase = caseRepository.findById(cmd.getCaseId())
                .orElseThrow(() -> new ResourceNotFoundException("ReviewCase", cmd.getCaseId()));

        if (reviewCase.getRereviewerAssignment() == null
                || !actor.getUserId().equals(reviewCase.getRereviewerAssignment().getReviewerId())) {
            throw new DomainRuleViolationException("NOT_ASSIGNED_REREVIEWER",
                    "Only the assigned rereviewer can submit this rereview");
        }

        ReviewSubmission submission = submissionRepository.findById(cmd.getSubmissionId())
                .orElseThrow(() -> new ResourceNotFoundException("ReviewSubmission", cmd.getSubmissionId()));

        if (cmd.getAnswers() != null) {
            submission.updateDraft(cmd.getAnswers());
        }

        // Load score sheet for scoring
        ScoreSheetDefinition scoreSheet = scoreSheetRepository.findById(reviewCase.getScoreSheetVersionRef())
                .orElseThrow(() -> new ResourceNotFoundException("ScoreSheetDefinition", reviewCase.getScoreSheetVersionRef()));
        var result = scoreSheet.calculateScore(submission.getAnswers());
        submission.submit(result.getTotalScore());
        submissionRepository.save(submission);

        // Load project for weights
        ReviewProject project = projectRepository.findById(reviewCase.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("ReviewProject", reviewCase.getProjectId()));
        ProjectVersion version = project.getActiveVersion()
                .orElseThrow(() -> new DomainRuleViolationException("NO_ACTIVE_VERSION",
                        "ReviewProject has no active version"));

        reviewCase.completeRereview(
                result.getTotalScore(),
                version.getInitialReviewWeight(),
                version.getRereviewWeight());
        caseRepository.save(reviewCase);

        eventPublisher.publish(new RereviewSubmittedEvent(
                this, actor.getUserId(), actor.getUserName(), reviewCase.getHospitalId(),
                reviewCase.getCaseId(),
                String.format("{\"rereviewScore\":%.2f,\"finalScore\":%.2f}",
                        result.getTotalScore(), reviewCase.getFinalScore())));
    }
}
```

- [ ] **Step 5: Run tests**

```bash
cd services/quality-inspection-service && ./gradlew test --tests "*.SubmitRereviewUseCaseTest" 2>&1 | tail -10
```

Expected: All tests pass.

- [ ] **Step 6: Commit**

```bash
git add -A && git commit -m "feat(B4): add SubmitRereviewUseCase with weighted final score"
```

---

### Task 18: Create ReviseRereviewUseCase

**Files:**
- Create: `SVC/application/reviewcase/ReviseRereviewUseCase.java`
- Create: `SVC/application/reviewcase/command/ReviseRereviewCommand.java`
- Create: `TEST/application/reviewcase/ReviseRereviewUseCaseTest.java`

The rereview revision use case. Per D-021, CLOSED cases are allowed rereview revision (not CANCELLED). It creates a new revision of the rereview submission and recalculates the weighted final score.

- [ ] **Step 1: Write the command DTO**

Create `ReviseRereviewCommand.java`:

```java
package com.tzuchi.his.quality.inspection.application.reviewcase.command;

import com.tzuchi.his.quality.inspection.application.reviewcase.dto.AnswerDto;

import javax.validation.constraints.NotBlank;
import java.util.List;

public class ReviseRereviewCommand {

    @NotBlank
    private String caseId;
    @NotBlank
    private String submissionId;
    private List<AnswerDto> answers;

    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }

    public String getSubmissionId() { return submissionId; }
    public void setSubmissionId(String submissionId) { this.submissionId = submissionId; }

    public List<AnswerDto> getAnswers() { return answers; }
    public void setAnswers(List<AnswerDto> answers) { this.answers = answers; }
}
```

- [ ] **Step 2: Write failing test**

Create `ReviseRereviewUseCaseTest.java`:

```java
package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.reviewcase.command.ReviseRereviewCommand;
import com.tzuchi.his.quality.inspection.domain.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.SubmissionStatus;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetDefinition;
import com.tzuchi.his.quality.inspection.testfixture.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReviseRereviewUseCaseTest {

    private InMemoryReviewCaseRepository caseRepository;
    private InMemoryReviewSubmissionRepository submissionRepository;
    private InMemoryProjectRepository projectRepository;
    private InMemoryScoreSheetRepository scoreSheetRepository;
    private CapturingDomainEventPublisher eventPublisher;
    private ReviseRereviewUseCase useCase;

    @BeforeEach
    void setUp() {
        caseRepository = new InMemoryReviewCaseRepository();
        submissionRepository = new InMemoryReviewSubmissionRepository();
        projectRepository = new InMemoryProjectRepository();
        scoreSheetRepository = new InMemoryScoreSheetRepository();
        eventPublisher = new CapturingDomainEventPublisher();
        useCase = new ReviseRereviewUseCase(
                caseRepository, submissionRepository, projectRepository,
                scoreSheetRepository, eventPublisher);
        seedProjectAndScoreSheet();
    }

    private void seedProjectAndScoreSheet() {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft("ss-1", "Score Sheet", "H001");
        ss.addSection("sec-1", "Section 1", 1);
        ss.addItemToSection("sec-1", "item-1", "Item 1", 100, 1);
        ss.publishVersion();
        scoreSheetRepository.save(ss);

        ReviewProject project = ReviewProject.create("p1", "Test Project", "H001");
        project.updateDraft(null, null, null, null,
                new BigDecimal("80"), new BigDecimal("0.5"), new BigDecimal("0.5"));
        project.bindScoreSheet("ss-1");
        project.publishNewVersion();
        project.activate();
        projectRepository.save(project);
    }

    private ActorContext rereviewerActor() {
        return new ActorContext("rereviewer-1", "ReReviewer", "H001", DomainRole.REREVIEWER);
    }

    private ReviewCase seedClosedCaseWithRereview() {
        ReviewCase rc = ReviewCase.createFromSampling(
                "c1", "b1", "p1", "H001", "SK-001", MedicalRecordType.OUTPATIENT);
        rc.assignReviewer("reviewer-1", LocalDateTime.now());
        rc.startInitialReview("sub-init", "ss-1");
        rc.completeInitialReview(new BigDecimal("90"), new BigDecimal("80"));
        rc.assignRereviewer("rereviewer-1", LocalDateTime.of(2026, 6, 30, 23, 59));
        rc.startRereview("rereviewer-1");
        rc.completeRereview(new BigDecimal("70"), new BigDecimal("0.5"), new BigDecimal("0.5"));
        caseRepository.save(rc);

        // A submitted rereview submission
        ReviewSubmission sub = ReviewSubmission.createRereviewDraft("sub-r1", "c1", "rereviewer-1");
        sub.submit(new BigDecimal("70"));
        submissionRepository.save(sub);
        return rc;
    }

    @Test
    void execute_happyPath_shouldCreateRevisionAndRecalculate() {
        seedClosedCaseWithRereview();
        ReviseRereviewCommand cmd = new ReviseRereviewCommand();
        cmd.setCaseId("c1");
        cmd.setSubmissionId("sub-r1");
        cmd.setAnswers(Collections.emptyList());

        useCase.execute(rereviewerActor(), cmd);

        // Case remains CLOSED (D-021) but finalScore is recalculated
        ReviewCase saved = caseRepository.findById("c1").orElseThrow();
        assertEquals(CaseStatus.CLOSED, saved.getStatus());
        assertNotNull(saved.getFinalScore());

        // A new revision submission is created
        List<ReviewSubmission> subs = submissionRepository.findByCaseId("c1");
        assertTrue(subs.size() >= 2); // original + revision
        assertTrue(eventPublisher.getEvents().size() >= 1);
    }

    @Test
    void execute_wrongActor_shouldThrow() {
        seedClosedCaseWithRereview();
        ReviseRereviewCommand cmd = new ReviseRereviewCommand();
        cmd.setCaseId("c1");
        cmd.setSubmissionId("sub-r1");
        cmd.setAnswers(Collections.emptyList());

        ActorContext wrongActor = new ActorContext("someone-else", "Other", "H001", DomainRole.REREVIEWER);
        assertThrows(Exception.class, () -> useCase.execute(wrongActor, cmd));
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

```bash
cd services/quality-inspection-service && ./gradlew test --tests "*.ReviseRereviewUseCaseTest" 2>&1 | tail -5
```

Expected: Compilation error.

- [ ] **Step 4: Implement the use case**

Create `ReviseRereviewUseCase.java`:

```java
package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.reviewcase.command.ReviseRereviewCommand;
import com.tzuchi.his.quality.inspection.domain.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.project.ProjectVersion;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.event.RereviewRevisedEvent;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmissionRepository;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetDefinition;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ReviseRereviewUseCase {

    private final ReviewCaseRepository caseRepository;
    private final ReviewSubmissionRepository submissionRepository;
    private final ReviewProjectRepository projectRepository;
    private final ScoreSheetRepository scoreSheetRepository;
    private final DomainEventPublisher eventPublisher;

    public ReviseRereviewUseCase(ReviewCaseRepository caseRepository,
                                  ReviewSubmissionRepository submissionRepository,
                                  ReviewProjectRepository projectRepository,
                                  ScoreSheetRepository scoreSheetRepository,
                                  DomainEventPublisher eventPublisher) {
        this.caseRepository = caseRepository;
        this.submissionRepository = submissionRepository;
        this.projectRepository = projectRepository;
        this.scoreSheetRepository = scoreSheetRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void execute(ActorContext actor, ReviseRereviewCommand cmd) {
        actor.requireRole(DomainRole.REREVIEWER);
        ReviewCase reviewCase = caseRepository.findById(cmd.getCaseId())
                .orElseThrow(() -> new ResourceNotFoundException("ReviewCase", cmd.getCaseId()));

        if (reviewCase.getRereviewerAssignment() == null
                || !actor.getUserId().equals(reviewCase.getRereviewerAssignment().getReviewerId())) {
            throw new DomainRuleViolationException("NOT_ASSIGNED_REREVIEWER",
                    "Only the assigned rereviewer can revise this rereview");
        }

        ReviewSubmission oldSubmission = submissionRepository.findById(cmd.getSubmissionId())
                .orElseThrow(() -> new ResourceNotFoundException("ReviewSubmission", cmd.getSubmissionId()));

        // Create revision (append-only)
        ReviewSubmission revision = oldSubmission.createRevision(UUID.randomUUID().toString());
        if (cmd.getAnswers() != null) {
            revision.updateDraft(cmd.getAnswers());
        }

        // Score the revision
        ScoreSheetDefinition scoreSheet = scoreSheetRepository.findById(reviewCase.getScoreSheetVersionRef())
                .orElseThrow(() -> new ResourceNotFoundException("ScoreSheetDefinition", reviewCase.getScoreSheetVersionRef()));
        var result = scoreSheet.calculateScore(revision.getAnswers());
        revision.submit(result.getTotalScore());
        submissionRepository.save(revision);

        // Recalculate weighted final score
        ReviewProject project = projectRepository.findById(reviewCase.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("ReviewProject", reviewCase.getProjectId()));
        ProjectVersion version = project.getActiveVersion()
                .orElseThrow(() -> new DomainRuleViolationException("NO_ACTIVE_VERSION",
                        "ReviewProject has no active version"));

        reviewCase.updateFinalScoreForRereview(
                result.getTotalScore(),
                version.getInitialReviewWeight(),
                version.getRereviewWeight());
        caseRepository.save(reviewCase);

        eventPublisher.publish(new RereviewRevisedEvent(
                this, actor.getUserId(), actor.getUserName(), reviewCase.getHospitalId(),
                reviewCase.getCaseId(),
                String.format("{\"revisionNumber\":%d,\"rereviewScore\":%.2f,\"finalScore\":%.2f}",
                        revision.getRevisionNumber(), result.getTotalScore(), reviewCase.getFinalScore())));
    }
}
```

- [ ] **Step 5: Run tests**

```bash
cd services/quality-inspection-service && ./gradlew test --tests "*.ReviseRereviewUseCaseTest" 2>&1 | tail -10
```

Expected: All tests pass.

- [ ] **Step 6: Commit**

```bash
git add -A && git commit -m "feat(B4): add ReviseRereviewUseCase with D-021 CLOSED revision support"
```

---

### Task 19: Create GetRereviewRevisionHistoryUseCase

**Files:**
- Create: `SVC/application/reviewcase/GetRereviewRevisionHistoryUseCase.java`
- Create: `SVC/application/reviewcase/query/GetRereviewRevisionHistoryQuery.java`
- Create: `TEST/application/reviewcase/GetRereviewRevisionHistoryUseCaseTest.java`

- [ ] **Step 1: Write the query DTO**

Create `GetRereviewRevisionHistoryQuery.java`:

```java
package com.tzuchi.his.quality.inspection.application.reviewcase.query;

import javax.validation.constraints.NotBlank;

public class GetRereviewRevisionHistoryQuery {

    @NotBlank
    private String caseId;

    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }
}
```

- [ ] **Step 2: Write failing test**

Create `GetRereviewRevisionHistoryUseCaseTest.java`:

```java
package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.GetRereviewRevisionHistoryQuery;
import com.tzuchi.his.quality.inspection.domain.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewStage;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryReviewSubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GetRereviewRevisionHistoryUseCaseTest {

    private InMemoryReviewSubmissionRepository submissionRepository;
    private GetRereviewRevisionHistoryUseCase useCase;

    @BeforeEach
    void setUp() {
        submissionRepository = new InMemoryReviewSubmissionRepository();
        useCase = new GetRereviewRevisionHistoryUseCase(submissionRepository);
    }

    @Test
    void execute_shouldReturnRereviewSubmissionsOnly() {
        // Seed initial review submission
        ReviewSubmission initial = ReviewSubmission.createInitialDraft("sub-1", "c1", "reviewer-1");
        initial.submit(new BigDecimal("90"));
        submissionRepository.save(initial);

        // Seed rereview submissions
        ReviewSubmission rereview1 = ReviewSubmission.createRereviewDraft("sub-r1", "c1", "rereviewer-1");
        rereview1.submit(new BigDecimal("70"));
        submissionRepository.save(rereview1);

        ReviewSubmission revision = rereview1.createRevision("sub-r2");
        revision.submit(new BigDecimal("80"));
        submissionRepository.save(revision);

        GetRereviewRevisionHistoryQuery query = new GetRereviewRevisionHistoryQuery();
        query.setCaseId("c1");

        ActorContext actor = new ActorContext("mgr-1", "Manager", "H001", DomainRole.HOSPITAL_MANAGER);
        List<ReviewSubmission> result = useCase.execute(actor, query);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(s -> s.getStage() == ReviewStage.REREVIEW));
    }

    @Test
    void execute_noRereviewSubmissions_shouldReturnEmpty() {
        GetRereviewRevisionHistoryQuery query = new GetRereviewRevisionHistoryQuery();
        query.setCaseId("c1");

        ActorContext actor = new ActorContext("mgr-1", "Manager", "H001", DomainRole.HOSPITAL_MANAGER);
        List<ReviewSubmission> result = useCase.execute(actor, query);

        assertTrue(result.isEmpty());
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

```bash
cd services/quality-inspection-service && ./gradlew test --tests "*.GetRereviewRevisionHistoryUseCaseTest" 2>&1 | tail -5
```

Expected: Compilation error.

- [ ] **Step 4: Implement the use case**

Create `GetRereviewRevisionHistoryUseCase.java`:

```java
package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.GetRereviewRevisionHistoryQuery;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewStage;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class GetRereviewRevisionHistoryUseCase {

    private final ReviewSubmissionRepository submissionRepository;

    public GetRereviewRevisionHistoryUseCase(ReviewSubmissionRepository submissionRepository) {
        this.submissionRepository = submissionRepository;
    }

    public List<ReviewSubmission> execute(ActorContext actor, GetRereviewRevisionHistoryQuery query) {
        // Any authenticated role can query — visibility is enforced at API/UI layer
        return submissionRepository.findByCaseId(query.getCaseId()).stream()
                .filter(s -> s.getStage() == ReviewStage.REREVIEW)
                .sorted((a, b) -> Integer.compare(a.getRevisionNumber(), b.getRevisionNumber()))
                .collect(Collectors.toList());
    }
}
```

- [ ] **Step 5: Run tests**

```bash
cd services/quality-inspection-service && ./gradlew test --tests "*.GetRereviewRevisionHistoryUseCaseTest" 2>&1 | tail -5
```

Expected: All tests pass.

- [ ] **Step 6: Commit**

```bash
git add -A && git commit -m "feat(B4): add GetRereviewRevisionHistoryUseCase"
```

---

### Task 20: Add TestTokenFactory, DTOs, and Controller Endpoints

**Files:**
- Modify: `TEST/testfixture/TestTokenFactory.java` (add rereviewerToken)
- Create: `SVC/interfaces/rest/dto/CloseWithoutRereviewRequest.java`
- Create: `SVC/interfaces/rest/dto/SendToRereviewRequest.java`
- Create: `SVC/interfaces/rest/dto/AssignRereviewerRequest.java`
- Create: `SVC/interfaces/rest/dto/ReassignRereviewerRequest.java`
- Create: `SVC/interfaces/rest/dto/SubmitRereviewRequest.java`
- Create: `SVC/interfaces/rest/dto/ReviseRereviewRequest.java`
- Modify: `SVC/interfaces/rest/ReviewCaseController.java` (add 9 endpoints)

- [ ] **Step 1: Add rereviewerToken to TestTokenFactory**

In `TestTokenFactory.java`, add a new method after `reviewerToken()`:

```java
    public static String rereviewerToken() {
        return buildToken("rereviewer-1", "ReReviewer", "H001", "rereviewer");
    }

    public static String rereviewerToken(String userId, String hospitalId) {
        return buildToken(userId, "ReReviewer", hospitalId, "rereviewer");
    }
```

- [ ] **Step 2: Create request DTOs**

Create `CloseWithoutRereviewRequest.java`:
```java
package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

public class CloseWithoutRereviewRequest {
    // No additional fields — caseId comes from URL path
}
```

Create `SendToRereviewRequest.java`:
```java
package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

public class SendToRereviewRequest {
    // No additional fields — caseId comes from URL path
}
```

Create `AssignRereviewerRequest.java`:
```java
package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public class AssignRereviewerRequest {
    @NotBlank
    private String rereviewerId;
    private LocalDateTime dueAt;

    public String getRereviewerId() { return rereviewerId; }
    public void setRereviewerId(String rereviewerId) { this.rereviewerId = rereviewerId; }
    public LocalDateTime getDueAt() { return dueAt; }
    public void setDueAt(LocalDateTime dueAt) { this.dueAt = dueAt; }
}
```

Create `ReassignRereviewerRequest.java`:
```java
package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public class ReassignRereviewerRequest {
    @NotBlank
    private String rereviewerId;
    private LocalDateTime dueAt;

    public String getRereviewerId() { return rereviewerId; }
    public void setRereviewerId(String rereviewerId) { this.rereviewerId = rereviewerId; }
    public LocalDateTime getDueAt() { return dueAt; }
    public void setDueAt(LocalDateTime dueAt) { this.dueAt = dueAt; }
}
```

Create `SubmitRereviewRequest.java`:
```java
package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import com.tzuchi.his.quality.inspection.application.reviewcase.dto.AnswerDto;

import javax.validation.constraints.NotBlank;
import java.util.List;

public class SubmitRereviewRequest {
    @NotBlank
    private String submissionId;
    private List<AnswerDto> answers;

    public String getSubmissionId() { return submissionId; }
    public void setSubmissionId(String submissionId) { this.submissionId = submissionId; }
    public List<AnswerDto> getAnswers() { return answers; }
    public void setAnswers(List<AnswerDto> answers) { this.answers = answers; }
}
```

Create `ReviseRereviewRequest.java`:
```java
package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import com.tzuchi.his.quality.inspection.application.reviewcase.dto.AnswerDto;

import javax.validation.constraints.NotBlank;
import java.util.List;

public class ReviseRereviewRequest {
    @NotBlank
    private String submissionId;
    private List<AnswerDto> answers;

    public String getSubmissionId() { return submissionId; }
    public void setSubmissionId(String submissionId) { this.submissionId = submissionId; }
    public List<AnswerDto> getAnswers() { return answers; }
    public void setAnswers(List<AnswerDto> answers) { this.answers = answers; }
}
```

- [ ] **Step 3: Add 9 new endpoints to ReviewCaseController**

Add new use case dependencies to the constructor:

```java
    // B4 dependencies
    private final CloseWithoutRereviewUseCase closeWithoutRereviewUseCase;
    private final SendToRereviewUseCase sendToRereviewUseCase;
    private final AssignRereviewerUseCase assignRereviewerUseCase;
    private final ReassignRereviewerUseCase reassignRereviewerUseCase;
    private final StartRereviewUseCase startRereviewUseCase;
    private final SaveRereviewDraftUseCase saveRereviewDraftUseCase;
    private final SubmitRereviewUseCase submitRereviewUseCase;
    private final ReviseRereviewUseCase reviseRereviewUseCase;
    private final GetRereviewRevisionHistoryUseCase getRereviewRevisionHistoryUseCase;
```

Update the constructor to accept all new dependencies.

Add new endpoints:

```java
    // --- B4 Rereview Endpoints ---

    @PostMapping("/{caseId}/close-without-rereview")
    public ResponseEntity<ApiResponse<Void>> closeWithoutRereview(
            @PathVariable String caseId,
            @CurrentActor ActorContext actor) {
        CloseWithoutRereviewCommand cmd = new CloseWithoutRereviewCommand();
        cmd.setCaseId(caseId);
        closeWithoutRereviewUseCase.execute(actor, cmd);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{caseId}/send-to-rereview")
    public ResponseEntity<ApiResponse<Void>> sendToRereview(
            @PathVariable String caseId,
            @CurrentActor ActorContext actor) {
        SendToRereviewCommand cmd = new SendToRereviewCommand();
        cmd.setCaseId(caseId);
        sendToRereviewUseCase.execute(actor, cmd);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{caseId}/assign-rereviewer")
    public ResponseEntity<ApiResponse<Void>> assignRereviewer(
            @PathVariable String caseId,
            @RequestBody @Valid AssignRereviewerRequest request,
            @CurrentActor ActorContext actor) {
        AssignRereviewerCommand cmd = new AssignRereviewerCommand();
        cmd.setCaseId(caseId);
        cmd.setRereviewerId(request.getRereviewerId());
        cmd.setDueAt(request.getDueAt());
        assignRereviewerUseCase.execute(actor, cmd);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{caseId}/reassign-rereviewer")
    public ResponseEntity<ApiResponse<Void>> reassignRereviewer(
            @PathVariable String caseId,
            @RequestBody @Valid ReassignRereviewerRequest request,
            @CurrentActor ActorContext actor) {
        ReassignRereviewerCommand cmd = new ReassignRereviewerCommand();
        cmd.setCaseId(caseId);
        cmd.setRereviewerId(request.getRereviewerId());
        cmd.setDueAt(request.getDueAt());
        reassignRereviewerUseCase.execute(actor, cmd);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{caseId}/start-rereview")
    public ResponseEntity<ApiResponse<Void>> startRereview(
            @PathVariable String caseId,
            @CurrentActor ActorContext actor) {
        StartRereviewCommand cmd = new StartRereviewCommand();
        cmd.setCaseId(caseId);
        startRereviewUseCase.execute(actor, cmd);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PutMapping("/{caseId}/rereview-draft")
    public ResponseEntity<ApiResponse<Void>> saveRereviewDraft(
            @PathVariable String caseId,
            @RequestBody @Valid SaveDraftCommand cmd,
            @CurrentActor ActorContext actor) {
        cmd.setCaseId(caseId);
        saveRereviewDraftUseCase.execute(actor, cmd);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{caseId}/submit-rereview")
    public ResponseEntity<ApiResponse<Void>> submitRereview(
            @PathVariable String caseId,
            @RequestBody @Valid SubmitRereviewRequest request,
            @CurrentActor ActorContext actor) {
        SubmitRereviewCommand cmd = new SubmitRereviewCommand();
        cmd.setCaseId(caseId);
        cmd.setSubmissionId(request.getSubmissionId());
        cmd.setAnswers(request.getAnswers());
        submitRereviewUseCase.execute(actor, cmd);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{caseId}/revise-rereview")
    public ResponseEntity<ApiResponse<Void>> reviseRereview(
            @PathVariable String caseId,
            @RequestBody @Valid ReviseRereviewRequest request,
            @CurrentActor ActorContext actor) {
        ReviseRereviewCommand cmd = new ReviseRereviewCommand();
        cmd.setCaseId(caseId);
        cmd.setSubmissionId(request.getSubmissionId());
        cmd.setAnswers(request.getAnswers());
        reviseRereviewUseCase.execute(actor, cmd);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/{caseId}/rereview-revisions")
    public ResponseEntity<ApiResponse<Object>> getRereviewRevisionHistory(
            @PathVariable String caseId,
            @CurrentActor ActorContext actor) {
        GetRereviewRevisionHistoryQuery query = new GetRereviewRevisionHistoryQuery();
        query.setCaseId(caseId);
        var result = getRereviewRevisionHistoryUseCase.execute(actor, query);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
```

- [ ] **Step 4: Run full test suite to check compilation**

```bash
cd services/quality-inspection-service && ./gradlew test 2>&1 | tail -10
```

Expected: All tests pass — controller compiles, Spring context loads with new beans.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(B4): add rereview controller endpoints and request DTOs"
```

---

### Task 21: API Integration Tests

**Files:**
- Create: `TEST/interfaces/rest/RereviewApiTest.java`

Comprehensive integration tests that exercise the full stack (controller → use case → domain → repository) via MockMvc.

- [ ] **Step 1: Write the integration test class**

Create `RereviewApiTest.java`:

```java
package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tzuchi.his.quality.inspection.testfixture.TestTokenFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RereviewApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private static final String BASE = "/review-cases";

    // --- Helper: Create project, batch, case, and complete initial review ---
    // (This follows the same pattern as existing InitialReviewApiTest)

    @Test
    void fullRereviewFlow() throws Exception {
        // 1. Setup: create project with threshold, sampling, case
        //    (Use existing setup patterns from InitialReviewApiTest or ReviewCaseApiTest)

        // 2. Complete initial review with score above threshold
        //    → expect status PENDING_REREVIEW

        // 3. Assign rereviewer
        String assignBody = "{\"rereviewerId\":\"rereviewer-1\",\"dueAt\":\"2026-06-30T23:59:00\"}";
        mockMvc.perform(post(BASE + "/{caseId}/assign-rereviewer", "CASE_ID")
                        .header("api-key", TestTokenFactory.hospitalManagerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignBody))
                .andExpect(status().isOk());

        // 4. Start rereview (as rereviewer)
        mockMvc.perform(post(BASE + "/{caseId}/start-rereview", "CASE_ID")
                        .header("api-key", TestTokenFactory.rereviewerToken()))
                .andExpect(status().isOk());

        // 5. Submit rereview
        // → expect CLOSED with weighted final score

        // 6. Revise rereview (on CLOSED case — D-021)
        // → expect CLOSED with recalculated final score

        // 7. Get rereview revision history
        mockMvc.perform(get(BASE + "/{caseId}/rereview-revisions", "CASE_ID")
                        .header("api-key", TestTokenFactory.hospitalManagerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    void closeWithoutRereview_happyPath() throws Exception {
        // 1. Setup: case at INITIAL_REVIEW_COMPLETED (score <= threshold or manual)
        // 2. Close without rereview
        mockMvc.perform(post(BASE + "/{caseId}/close-without-rereview", "CASE_ID")
                        .header("api-key", TestTokenFactory.hospitalManagerToken()))
                .andExpect(status().isOk());
    }

    @Test
    void sendToRereview_manualEscalation() throws Exception {
        // 1. Setup: case at INITIAL_REVIEW_COMPLETED
        // 2. Send to rereview
        mockMvc.perform(post(BASE + "/{caseId}/send-to-rereview", "CASE_ID")
                        .header("api-key", TestTokenFactory.hospitalManagerToken()))
                .andExpect(status().isOk());
    }

    @Test
    void assignRereviewer_sameAsInitialReviewer_shouldReturn422() throws Exception {
        // 1. Setup: case at PENDING_REREVIEW, initial reviewer = "reviewer-1"
        // 2. Try to assign "reviewer-1" as rereviewer → 422
        String body = "{\"rereviewerId\":\"reviewer-1\"}";
        mockMvc.perform(post(BASE + "/{caseId}/assign-rereviewer", "CASE_ID")
                        .header("api-key", TestTokenFactory.hospitalManagerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void startRereview_wrongActor_shouldReturn422() throws Exception {
        // 1. Setup: case at PENDING_REREVIEW with rereviewer-1 assigned
        // 2. Try to start as wrong user → 422
        mockMvc.perform(post(BASE + "/{caseId}/start-rereview", "CASE_ID")
                        .header("api-key", TestTokenFactory.rereviewerToken("wrong-user", "H001")))
                .andExpect(status().isUnprocessableEntity());
    }
}
```

> **Note to implementer:** The integration test outline above uses placeholder `CASE_ID` values. During implementation, follow the exact setup pattern from `InitialReviewApiTest.java` to create proper test data (project → publish → activate → sampling → case creation → assign → start → complete). Replace `CASE_ID` with the actual IDs returned during setup. The full rereview flow test should be a single sequential test that exercises the entire happy path.

- [ ] **Step 2: Run integration tests**

```bash
cd services/quality-inspection-service && ./gradlew test --tests "*.RereviewApiTest" 2>&1 | tail -20
```

Expected: All tests pass. Fix any wiring issues found (Spring context loading, missing beans, etc.).

- [ ] **Step 3: Run FULL test suite to catch regressions**

```bash
cd services/quality-inspection-service && ./gradlew test 2>&1 | tail -10
```

Expected: ALL tests pass — old and new.

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "test(B4): add rereview API integration tests"
```

---

### Task 22: Final Build Verification and Self-Review

**Files:** None (verification only)

- [ ] **Step 1: Clean build**

```bash
cd services/quality-inspection-service && ./gradlew clean build 2>&1 | tail -10
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Full test suite with verbose output**

```bash
cd services/quality-inspection-service && ./gradlew test 2>&1 | tail -20
```

Expected: All tests pass. Count total tests to confirm coverage growth.

- [ ] **Step 3: Self-review checklist**

Verify each item:

1. **Decision log updated**: D-019 (weight config), D-020 (threshold), D-021 (CLOSED rereview revision)
2. **Domain invariants enforced**: rereviewer ≠ initial reviewer (D-017), independent evaluation (D-018)
3. **State machine transitions**: INITIAL_REVIEW_COMPLETED → PENDING_REREVIEW (auto/manual), PENDING_REREVIEW → REREVIEW_IN_PROGRESS → CLOSED
4. **Weighted score formula**: `initialScore × initialWeight + rereviewScore × rereviewWeight`
5. **Events published**: 6 new event types, each use case publishes appropriate event
6. **Rereview revision on CLOSED**: D-021 — uses `requireNotCancelled()` instead of `requireNotTerminal()`
7. **Backward compatibility**: existing tests pass without modification (except SubmitInitialReviewUseCase)
8. **No incidental refactors**: only B4 scope changes

- [ ] **Step 4: Commit any final fixes**

```bash
git add -A && git commit -m "chore(B4): final verification pass"
```

---

## Summary

| Task | Description | New Files | Modified Files |
|------|-------------|-----------|---------------|
| 1 | Decision log updates | 0 | 1 |
| 2 | ProjectVersion extension | 1 test | 1 source |
| 3 | Cascade to ReviewProject, Entity, Repo | 0 | 4 |
| 4 | ReviewCase fields + entity mapping | 0 | 3 |
| 5 | completeInitialReview threshold | 1 test | 2 source |
| 6 | closeWithoutRereview + sendToRereview | 0 | 2 (test + source) |
| 7 | Rereviewer assignment + startRereview | 0 | 2 (test + source) |
| 8 | completeRereview + rereview revision guard | 0 | 2 (test + source) |
| 9 | ReviewSubmission.createRereviewDraft | 0 | 2 (test + source) |
| 10 | 6 domain event classes | 6 | 0 |
| 11 | SubmitInitialReview threshold wiring | 0 | 2 |
| 12 | CloseWithoutRereviewUseCase | 3 | 0 |
| 13 | SendToRereviewUseCase | 3 | 0 |
| 14 | Assign + Reassign Rereviewer | 6 | 0 |
| 15 | StartRereviewUseCase | 3 | 0 |
| 16 | SaveRereviewDraftUseCase | 1 | 0 |
| 17 | SubmitRereviewUseCase | 3 | 0 |
| 18 | ReviseRereviewUseCase | 3 | 0 |
| 19 | GetRereviewRevisionHistoryUseCase | 3 | 0 |
| 20 | Controller + DTOs + TestTokenFactory | 6 | 2 |
| 21 | API integration tests | 1 | 0 |
| 22 | Final verification | 0 | 0 |
