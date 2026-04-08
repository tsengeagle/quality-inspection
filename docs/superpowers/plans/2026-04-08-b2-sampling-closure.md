# B2 Sampling Closure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete all remaining B2 (Sampling) items — RuleSnapshot rewrite, domain events, event emission, query read-side, and GET endpoints — so the Sampling bounded context can be sealed.

**Architecture:** Hexagonal architecture with domain/application/interfaces/infrastructure layers. Write-side uses aggregate + use case pattern. Read-side uses QueryPort (application) + QueryAdapter (infrastructure) + read model DTOs. Events follow DomainEvent → DomainEventPublisher → SpringDomainEventPublisher pattern.

**Tech Stack:** Java 11, Spring Boot 2.7.18, JPA/Hibernate, H2 (tests), JUnit 5

**Design spec:** `docs/superpowers/specs/2026-04-08-b2-sampling-closure-design.md`

---

## File Map

### New files

| File | Responsibility |
|------|---------------|
| `domain/sampling/event/SamplingBatchStartedEvent.java` | Domain event: batch started |
| `domain/sampling/event/SamplingCandidatesPreparedEvent.java` | Domain event: candidates prepared |
| `domain/sampling/event/ReviewCasesSelectedEvent.java` | Domain event: cases selected |
| `domain/sampling/event/SamplingBatchCompletedEvent.java` | Domain event: batch completed |
| `domain/sampling/event/SamplingBatchFailedEvent.java` | Domain event: batch failed |
| `application/sampling/SamplingBatchQueryPort.java` | Read-side query port interface |
| `application/sampling/SamplingBatchSummary.java` | List-view read model |
| `application/sampling/SamplingBatchDetailView.java` | Detail-view read model |
| `application/sampling/RuleSnapshotSummary.java` | Rule snapshot read model |
| `application/sampling/SelectedCaseRefView.java` | Selected case read model |
| `application/sampling/SamplingBatchQueryCriteria.java` | Query criteria DTO |
| `application/sampling/SamplingBatchExportResult.java` | Export result wrapper |
| `application/sampling/ListSamplingBatchesUseCase.java` | List batches use case |
| `application/sampling/GetSamplingBatchDetailUseCase.java` | Get batch detail use case |
| `application/sampling/ExportSamplingBatchResultUseCase.java` | Export skeleton use case |
| `application/common/Page.java` | Generic page wrapper |
| `infrastructure/persistence/sampling/SamplingBatchQueryAdapter.java` | JPA query adapter |

### Modified files

| File | Changes |
|------|---------|
| `domain/sampling/RuleSnapshot.java` | Rewrite: 2 stub fields → 5 structured fields |
| `application/sampling/ExecuteSamplingUseCase.java` | Add DomainEventPublisher + emit 4 events + new RuleSnapshot construction |
| `application/sampling/RetrySamplingBatchUseCase.java` | Add DomainEventPublisher + emit 4 events |
| `infrastructure/persistence/sampling/SamplingBatchEntity.java` | Replace 2 snapshot columns → 5 snapshot columns |
| `infrastructure/persistence/sampling/SamplingBatchRepositoryImpl.java` | Update toEntity/toDomain for new RuleSnapshot fields |
| `interfaces/rest/SamplingController.java` | Add 3 GET endpoints (list, detail, export) + inject 3 new use cases |

### Modified test files

| File | Changes |
|------|---------|
| `domain/sampling/SamplingBatchTest.java` | Update RuleSnapshot construction in helper |
| `application/sampling/SamplingUseCaseTest.java` | Add CapturingDomainEventPublisher + event assertions + query use case tests |
| `interfaces/rest/SamplingControllerApiTest.java` | Add GET endpoint integration tests |

**All file paths below are relative to:**
`services/quality-inspection-service/src/main/java/com/tzuchi/his/quality/inspection/`
(test files under `src/test/java/com/tzuchi/his/quality/inspection/`)

---

## Task 1: Rewrite RuleSnapshot (Domain)

**Files:**
- Modify: `domain/sampling/RuleSnapshot.java`
- Test: `domain/sampling/SamplingBatchTest.java`

- [ ] **Step 1: Update RuleSnapshot with structured fields**

Replace the entire content of `domain/sampling/RuleSnapshot.java`:

```java
package com.tzuchi.his.quality.inspection.domain.sampling;

import java.util.Objects;

/**
 * Snapshot of the rules used at the time of batch execution.
 * Preserved for traceability (D-011) — must not be modified after batch completion.
 *
 * This is the simplified version: stores version IDs + key parameters.
 * Full snapshot_json serialization + checksum deferred to B5.
 */
public class RuleSnapshot {

    private final String projectVersionId;
    private final int projectVersionNumber;
    private final String scoreSheetVersionId;
    private final String samplingPolicy;
    private final String snapshotSummary;

    public RuleSnapshot(String projectVersionId, int projectVersionNumber,
                        String scoreSheetVersionId, String samplingPolicy,
                        String snapshotSummary) {
        Objects.requireNonNull(projectVersionId, "projectVersionId must not be null");
        Objects.requireNonNull(scoreSheetVersionId, "scoreSheetVersionId must not be null");
        Objects.requireNonNull(samplingPolicy, "samplingPolicy must not be null");
        this.projectVersionId = projectVersionId;
        this.projectVersionNumber = projectVersionNumber;
        this.scoreSheetVersionId = scoreSheetVersionId;
        this.samplingPolicy = samplingPolicy;
        this.snapshotSummary = snapshotSummary;
    }

    public String getProjectVersionId() { return projectVersionId; }
    public int getProjectVersionNumber() { return projectVersionNumber; }
    public String getScoreSheetVersionId() { return scoreSheetVersionId; }
    public String getSamplingPolicy() { return samplingPolicy; }
    public String getSnapshotSummary() { return snapshotSummary; }
}
```

- [ ] **Step 2: Update SamplingBatchTest helper to use new RuleSnapshot**

In `SamplingBatchTest.java`, replace the `createBatch` helper method:

```java
private SamplingBatch createBatch(String batchId, String projectId, ExecutionMode mode) {
    return SamplingBatch.start(batchId, projectId, mode,
            new RuleSnapshot("PV-001-v1", 1, "SS-001-v1", "MANUAL", null));
}
```

- [ ] **Step 3: Add RuleSnapshot field validation test**

Add test in `SamplingBatchTest.java`:

```java
@Test
void ruleSnapshot_nullProjectVersionId_shouldThrow() {
    assertThrows(NullPointerException.class, () ->
            new RuleSnapshot(null, 1, "SS-001-v1", "MANUAL", null));
}

@Test
void ruleSnapshot_shouldPreserveAllFields() {
    RuleSnapshot snapshot = new RuleSnapshot("PV-001-v2", 2, "SS-001-v1",
            "MANUAL", "{\"key\":\"value\"}");
    assertEquals("PV-001-v2", snapshot.getProjectVersionId());
    assertEquals(2, snapshot.getProjectVersionNumber());
    assertEquals("SS-001-v1", snapshot.getScoreSheetVersionId());
    assertEquals("MANUAL", snapshot.getSamplingPolicy());
    assertEquals("{\"key\":\"value\"}", snapshot.getSnapshotSummary());
}
```

- [ ] **Step 4: Run tests to verify**

Run: `cd services/quality-inspection-service && .\gradlew test`
Expected: Compilation errors in files still using old RuleSnapshot constructor — this is expected; we fix them in the next steps.

---

## Task 2: Update RuleSnapshot in Use Cases + Tests

**Files:**
- Modify: `application/sampling/ExecuteSamplingUseCase.java` (RuleSnapshot construction only)
- Modify: `application/sampling/SamplingUseCaseTest.java` (RuleSnapshot construction in test helpers)
- Modify: `interfaces/rest/SamplingControllerApiTest.java` (RuleSnapshot construction in test helpers)

- [ ] **Step 1: Update ExecuteSamplingUseCase RuleSnapshot construction**

In `ExecuteSamplingUseCase.java`, replace lines 56-59:

Old:
```java
RuleSnapshot snapshot = new RuleSnapshot(
        "project=" + projectId + ",version=" + project.getCurrentVersion().getVersionNumber(),
        "default-sampling-policy"
);
```

New (note: `ProjectVersion` has no `projectVersionId` field — construct from projectId + versionNumber):
```java
RuleSnapshot snapshot = new RuleSnapshot(
        projectId + "-v" + project.getCurrentVersion().getVersionNumber(),
        project.getCurrentVersion().getVersionNumber(),
        project.getBoundScoreSheetId() != null ? project.getBoundScoreSheetId() : "UNBOUND",
        "MANUAL",
        null
);
```

- [ ] **Step 2: Update SamplingUseCaseTest RuleSnapshot construction**

In `SamplingUseCaseTest.java`, update all `new RuleSnapshot(...)` calls.

Replace in `retry_shouldCreateNewBatch_notOverwriteOldBatch` (line 105):
```java
SamplingBatch failedBatch = SamplingBatch.start("failed-b", "p1",
        ExecutionMode.MANUAL, new RuleSnapshot("PV-p1-v1", 1, "SS-001-v1", "MANUAL", null));
```

Replace in `retry_shouldUseSnapshotFromPreviousBatch` (lines 158-159):
```java
RuleSnapshot originalSnapshot = new RuleSnapshot("PV-p1-v1", 1, "SS-001-v1", "original-policy", null);
```

And update the assertion (line 169-170):
```java
assertEquals("PV-p1-v1",
        newBatch.getRuleSnapshot().getProjectVersionId(),
        "Retry batch must use original batch's snapshot, not current project version");
assertEquals("original-policy",
        newBatch.getRuleSnapshot().getSamplingPolicy());
```

Replace in `retry_nonFailedBatch_shouldThrow` (line 133):
```java
SamplingBatch completedBatch = SamplingBatch.start("done-b", "p1",
        ExecutionMode.MANUAL, new RuleSnapshot("PV-p1-v1", 1, "SS-001-v1", "MANUAL", null));
```

- [ ] **Step 3: Update SamplingControllerApiTest RuleSnapshot construction**

In `SamplingControllerApiTest.java`, replace line 73:
```java
new RuleSnapshot("PV-" + projectId + "-v1", 1, "SS-001-v1", "MANUAL", null));
```

- [ ] **Step 4: Run tests to verify all pass**

Run: `cd services/quality-inspection-service && .\gradlew test`
Expected: ALL tests PASS (213+2 new = ~215 tests)

- [ ] **Step 5: Commit**

```bash
cd services/quality-inspection-service
git add -A
git commit -m "feat(B2/S1): rewrite RuleSnapshot with structured fields

Replace 2-field stub with 5-field structured version:
- projectVersionId, projectVersionNumber, scoreSheetVersionId
- samplingPolicy, snapshotSummary (nullable JSON)
Full snapshot_json + checksum deferred to B5 (D-011).

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 3: Create Sampling Domain Events

**Files:**
- Create: `domain/sampling/event/SamplingBatchStartedEvent.java`
- Create: `domain/sampling/event/SamplingCandidatesPreparedEvent.java`
- Create: `domain/sampling/event/ReviewCasesSelectedEvent.java`
- Create: `domain/sampling/event/SamplingBatchCompletedEvent.java`
- Create: `domain/sampling/event/SamplingBatchFailedEvent.java`

- [ ] **Step 1: Create SamplingBatchStartedEvent**

```java
package com.tzuchi.his.quality.inspection.domain.sampling.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

public class SamplingBatchStartedEvent extends DomainEvent {
    public SamplingBatchStartedEvent(Object source, String actorUserId, String actorUserName,
                                      String hospitalId, String batchId, String projectId,
                                      String executionMode) {
        super(source, actorUserId, actorUserName,
              hospitalId, "SamplingBatch", batchId, "STARTED",
              String.format("{\"projectId\":\"%s\",\"executionMode\":\"%s\"}", projectId, executionMode));
    }
}
```

- [ ] **Step 2: Create SamplingCandidatesPreparedEvent**

```java
package com.tzuchi.his.quality.inspection.domain.sampling.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

public class SamplingCandidatesPreparedEvent extends DomainEvent {
    public SamplingCandidatesPreparedEvent(Object source, String actorUserId, String actorUserName,
                                            String hospitalId, String batchId,
                                            int populationCount, int eligibleCount, int excludedCount) {
        super(source, actorUserId, actorUserName,
              hospitalId, "SamplingBatch", batchId, "CANDIDATES_PREPARED",
              String.format("{\"populationCount\":%d,\"eligibleCount\":%d,\"excludedCount\":%d}",
                      populationCount, eligibleCount, excludedCount));
    }
}
```

- [ ] **Step 3: Create ReviewCasesSelectedEvent**

```java
package com.tzuchi.his.quality.inspection.domain.sampling.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

public class ReviewCasesSelectedEvent extends DomainEvent {
    public ReviewCasesSelectedEvent(Object source, String actorUserId, String actorUserName,
                                     String hospitalId, String batchId, int selectedCount) {
        super(source, actorUserId, actorUserName,
              hospitalId, "SamplingBatch", batchId, "CASES_SELECTED",
              String.format("{\"selectedCount\":%d}", selectedCount));
    }
}
```

- [ ] **Step 4: Create SamplingBatchCompletedEvent**

```java
package com.tzuchi.his.quality.inspection.domain.sampling.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

public class SamplingBatchCompletedEvent extends DomainEvent {
    public SamplingBatchCompletedEvent(Object source, String actorUserId, String actorUserName,
                                        String hospitalId, String batchId, String projectId,
                                        int selectedCount, int populationCount) {
        super(source, actorUserId, actorUserName,
              hospitalId, "SamplingBatch", batchId, "COMPLETED",
              String.format("{\"projectId\":\"%s\",\"selectedCount\":%d,\"populationCount\":%d}",
                      projectId, selectedCount, populationCount));
    }
}
```

- [ ] **Step 5: Create SamplingBatchFailedEvent**

```java
package com.tzuchi.his.quality.inspection.domain.sampling.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

public class SamplingBatchFailedEvent extends DomainEvent {
    public SamplingBatchFailedEvent(Object source, String actorUserId, String actorUserName,
                                     String hospitalId, String batchId, String failureReason) {
        super(source, actorUserId, actorUserName,
              hospitalId, "SamplingBatch", batchId, "FAILED",
              String.format("{\"failureReason\":\"%s\"}", failureReason));
    }
}
```

- [ ] **Step 6: Run tests to verify compilation**

Run: `cd services/quality-inspection-service && .\gradlew test`
Expected: ALL tests PASS (events created but not yet used)

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat(B2/S2): create 5 sampling domain event classes

Per domain-events.md §5:
- SamplingBatchStartedEvent
- SamplingCandidatesPreparedEvent
- ReviewCasesSelectedEvent
- SamplingBatchCompletedEvent
- SamplingBatchFailedEvent

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 4: Integrate Event Emission into Use Cases

**Files:**
- Modify: `application/sampling/ExecuteSamplingUseCase.java`
- Modify: `application/sampling/RetrySamplingBatchUseCase.java`
- Test: `application/sampling/SamplingUseCaseTest.java`

- [ ] **Step 1: Add DomainEventPublisher to ExecuteSamplingUseCase**

Replace the full `ExecuteSamplingUseCase.java`:

```java
package com.tzuchi.his.quality.inspection.application.sampling;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
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
import com.tzuchi.his.quality.inspection.domain.sampling.event.*;
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
    private final DomainEventPublisher eventPublisher;

    public ExecuteSamplingUseCase(ReviewProjectRepository projectRepository,
                                  SamplingBatchRepository batchRepository,
                                  ReviewCaseRepository caseRepository,
                                  CandidateSourcePort candidateSource,
                                  DomainEventPublisher eventPublisher) {
        this.projectRepository = projectRepository;
        this.batchRepository = batchRepository;
        this.caseRepository = caseRepository;
        this.candidateSource = candidateSource;
        this.eventPublisher = eventPublisher;
    }

    public String execute(ActorContext actor, String projectId) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        String hospitalId = actor.requireHospitalId();
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
                projectId + "-v" + project.getCurrentVersion().getVersionNumber(),
                project.getCurrentVersion().getVersionNumber(),
                project.getBoundScoreSheetId() != null ? project.getBoundScoreSheetId() : "UNBOUND",
                "MANUAL",
                null
        );

        String batchId = UUID.randomUUID().toString();
        SamplingBatch batch = SamplingBatch.start(batchId, projectId, ExecutionMode.MANUAL, snapshot);
        eventPublisher.publish(new SamplingBatchStartedEvent(this,
                actor.getUserId(), actor.getUserName(), hospitalId, batchId, projectId, "MANUAL"));

        List<MedicalRecordType> types = new ArrayList<>(project.getMedicalRecordTypes());
        List<SelectedCaseRef> candidates = candidateSource.queryCandidates(
                projectId, project.getHospitalId(), types);

        int populationCount = candidates.size();
        int excludedCount = 0;
        int eligibleCount = populationCount - excludedCount;

        batch.recordCandidateCount(populationCount, eligibleCount, excludedCount);
        eventPublisher.publish(new SamplingCandidatesPreparedEvent(this,
                actor.getUserId(), actor.getUserName(), hospitalId, batchId,
                populationCount, eligibleCount, excludedCount));

        batch.selectCases(candidates);
        eventPublisher.publish(new ReviewCasesSelectedEvent(this,
                actor.getUserId(), actor.getUserName(), hospitalId, batchId,
                batch.getSelectedCount()));

        batch.complete();
        eventPublisher.publish(new SamplingBatchCompletedEvent(this,
                actor.getUserId(), actor.getUserName(), hospitalId, batchId, projectId,
                batch.getSelectedCount(), populationCount));

        batchRepository.save(batch);

        List<ReviewCase> reviewCases = candidates.stream()
                .map(ref -> ReviewCase.createFromSampling(
                        UUID.randomUUID().toString(),
                        batchId,
                        projectId,
                        ref.getHospitalId(),
                        ref.getSourceCaseKey(),
                        ref.getMedicalRecordType()))
                .collect(Collectors.toList());

        caseRepository.saveAll(reviewCases);

        return batchId;
    }
}
```

- [ ] **Step 2: Add DomainEventPublisher to RetrySamplingBatchUseCase**

Replace the full `RetrySamplingBatchUseCase.java`:

```java
package com.tzuchi.his.quality.inspection.application.sampling;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.project.ProjectStatus;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import com.tzuchi.his.quality.inspection.domain.sampling.*;
import com.tzuchi.his.quality.inspection.domain.sampling.event.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Retry a failed sampling batch.
 * Creates a NEW batch -- does NOT overwrite the failed batch.
 */
@Service
@Transactional
public class RetrySamplingBatchUseCase {

    private final ReviewProjectRepository projectRepository;
    private final SamplingBatchRepository batchRepository;
    private final ReviewCaseRepository caseRepository;
    private final CandidateSourcePort candidateSource;
    private final DomainEventPublisher eventPublisher;

    public RetrySamplingBatchUseCase(ReviewProjectRepository projectRepository,
                                     SamplingBatchRepository batchRepository,
                                     ReviewCaseRepository caseRepository,
                                     CandidateSourcePort candidateSource,
                                     DomainEventPublisher eventPublisher) {
        this.projectRepository = projectRepository;
        this.batchRepository = batchRepository;
        this.caseRepository = caseRepository;
        this.candidateSource = candidateSource;
        this.eventPublisher = eventPublisher;
    }

    public String execute(ActorContext actor, String previousBatchId) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        String hospitalId = actor.requireHospitalId();
        SamplingBatch previousBatch = batchRepository.findById(previousBatchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + previousBatchId));

        if (previousBatch.getStatus() != BatchStatus.FAILED) {
            throw new DomainRuleViolationException("BATCH_NOT_RETRYABLE",
                    "Only FAILED batches can be retried. Current status: " + previousBatch.getStatus());
        }

        String projectId = previousBatch.getProjectId();
        ReviewProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        if (project.getStatus() != ProjectStatus.ACTIVE) {
            throw new DomainRuleViolationException("PROJECT_NOT_ACTIVE",
                    "Project must be ACTIVE for retry. Current status: " + project.getStatus());
        }

        // D-013: use the PREVIOUS batch's snapshot, not the current project version
        RuleSnapshot snapshot = previousBatch.getRuleSnapshot();

        String newBatchId = UUID.randomUUID().toString();
        SamplingBatch newBatch = SamplingBatch.start(newBatchId, projectId, ExecutionMode.RETRY, snapshot);
        newBatch.markRetryOf(previousBatchId);
        eventPublisher.publish(new SamplingBatchStartedEvent(this,
                actor.getUserId(), actor.getUserName(), hospitalId, newBatchId, projectId, "RETRY"));

        List<MedicalRecordType> types = new ArrayList<>(project.getMedicalRecordTypes());
        List<SelectedCaseRef> candidates = candidateSource.queryCandidates(
                projectId, project.getHospitalId(), types);

        int populationCount = candidates.size();
        int excludedCount = 0;
        int eligibleCount = populationCount - excludedCount;

        newBatch.recordCandidateCount(populationCount, eligibleCount, excludedCount);
        eventPublisher.publish(new SamplingCandidatesPreparedEvent(this,
                actor.getUserId(), actor.getUserName(), hospitalId, newBatchId,
                populationCount, eligibleCount, excludedCount));

        newBatch.selectCases(candidates);
        eventPublisher.publish(new ReviewCasesSelectedEvent(this,
                actor.getUserId(), actor.getUserName(), hospitalId, newBatchId,
                newBatch.getSelectedCount()));

        newBatch.complete();
        eventPublisher.publish(new SamplingBatchCompletedEvent(this,
                actor.getUserId(), actor.getUserName(), hospitalId, newBatchId, projectId,
                newBatch.getSelectedCount(), populationCount));

        batchRepository.save(newBatch);

        List<ReviewCase> reviewCases = candidates.stream()
                .map(ref -> ReviewCase.createFromSampling(
                        UUID.randomUUID().toString(),
                        newBatchId,
                        projectId,
                        ref.getHospitalId(),
                        ref.getSourceCaseKey(),
                        ref.getMedicalRecordType()
                ))
                .collect(Collectors.toList());

        caseRepository.saveAll(reviewCases);

        return newBatchId;
    }
}
```

- [ ] **Step 3: Update SamplingUseCaseTest to inject CapturingDomainEventPublisher**

In `SamplingUseCaseTest.java`, add field and update `setUp()`:

Add import:
```java
import com.tzuchi.his.quality.inspection.testfixture.CapturingDomainEventPublisher;
import com.tzuchi.his.quality.inspection.domain.sampling.event.*;
```

Add field:
```java
private CapturingDomainEventPublisher eventPublisher;
```

Update `setUp()`:
```java
@BeforeEach
void setUp() {
    projectRepo = new InMemoryReviewProjectRepository();
    batchRepo = new InMemorySamplingBatchRepo();
    caseRepo = new InMemoryReviewCaseRepo();
    candidateSource = new StubCandidateSource();
    eventPublisher = new CapturingDomainEventPublisher();

    previewUseCase = new PreviewSamplingPopulationUseCase(projectRepo, candidateSource);
    executeUseCase = new ExecuteSamplingUseCase(projectRepo, batchRepo, caseRepo, candidateSource, eventPublisher);
    retryUseCase = new RetrySamplingBatchUseCase(projectRepo, batchRepo, caseRepo, candidateSource, eventPublisher);

    hospitalManagerActor = new ActorContext("u-test", "Test", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));
}
```

Note: also update the `hospitalManagerActor` to include `Optional.of("H001")` since the use cases now call `actor.requireHospitalId()`.

- [ ] **Step 4: Add event emission tests**

Add these tests in `SamplingUseCaseTest.java`:

```java
@Test
void execute_shouldEmit4Events() {
    ReviewProject project = createActiveProject("p1");
    projectRepo.save(project);
    candidateSource.setCandidates(List.of(
            new SelectedCaseRef("C1", "H1", MedicalRecordType.OUTPATIENT)
    ));

    executeUseCase.execute(hospitalManagerActor, "p1");

    assertEquals(4, eventPublisher.events.size());
    assertInstanceOf(SamplingBatchStartedEvent.class, eventPublisher.events.get(0));
    assertInstanceOf(SamplingCandidatesPreparedEvent.class, eventPublisher.events.get(1));
    assertInstanceOf(ReviewCasesSelectedEvent.class, eventPublisher.events.get(2));
    assertInstanceOf(SamplingBatchCompletedEvent.class, eventPublisher.events.get(3));
}

@Test
void retry_shouldEmit4Events() {
    ReviewProject project = createActiveProject("p1");
    projectRepo.save(project);

    SamplingBatch failedBatch = SamplingBatch.start("failed-ev", "p1",
            ExecutionMode.MANUAL, new RuleSnapshot("PV-p1-v1", 1, "SS-001-v1", "MANUAL", null));
    failedBatch.fail("test failure");
    batchRepo.save(failedBatch);

    candidateSource.setCandidates(List.of(
            new SelectedCaseRef("C1", "H1", MedicalRecordType.OUTPATIENT)
    ));

    retryUseCase.execute(hospitalManagerActor, "failed-ev");

    assertEquals(4, eventPublisher.events.size());
    assertInstanceOf(SamplingBatchStartedEvent.class, eventPublisher.events.get(0));
    assertInstanceOf(SamplingBatchCompletedEvent.class, eventPublisher.events.get(3));
}
```

- [ ] **Step 5: Run tests to verify**

Run: `cd services/quality-inspection-service && .\gradlew test`
Expected: ALL tests PASS

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat(B2/S3): integrate event emission into Execute and Retry use cases

Inject DomainEventPublisher, emit 4 events per batch lifecycle:
STARTED → CANDIDATES_PREPARED → CASES_SELECTED → COMPLETED
SamplingBatchFailedEvent exists but not yet emitted (no fail path in use cases).

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 5: Create Read Models + Query Port + Page

**Files:**
- Create: `application/common/Page.java`
- Create: `application/sampling/SamplingBatchQueryPort.java`
- Create: `application/sampling/SamplingBatchSummary.java`
- Create: `application/sampling/SamplingBatchDetailView.java`
- Create: `application/sampling/RuleSnapshotSummary.java`
- Create: `application/sampling/SelectedCaseRefView.java`
- Create: `application/sampling/SamplingBatchQueryCriteria.java`

- [ ] **Step 1: Create generic Page class**

```java
package com.tzuchi.his.quality.inspection.application.common;

import java.util.List;

/**
 * Generic page wrapper for paginated query results.
 */
public class Page<T> {

    private final List<T> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;

    public Page(List<T> content, int page, int size, long totalElements) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
    }

    public List<T> getContent() { return content; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public long getTotalElements() { return totalElements; }
    public int getTotalPages() { return totalPages; }
}
```

- [ ] **Step 2: Create SamplingBatchQueryCriteria**

```java
package com.tzuchi.his.quality.inspection.application.sampling;

import com.tzuchi.his.quality.inspection.domain.sampling.BatchStatus;
import com.tzuchi.his.quality.inspection.domain.sampling.ExecutionMode;

/**
 * Criteria for querying sampling batches.
 */
public class SamplingBatchQueryCriteria {

    private final String projectId;
    private final BatchStatus status;
    private final ExecutionMode executionMode;
    private final int page;
    private final int size;

    public SamplingBatchQueryCriteria(String projectId, BatchStatus status,
                                      ExecutionMode executionMode, int page, int size) {
        this.projectId = projectId;
        this.status = status;
        this.executionMode = executionMode;
        this.page = Math.max(0, page);
        this.size = size > 0 ? Math.min(size, 100) : 20;
    }

    public String getProjectId() { return projectId; }
    public BatchStatus getStatus() { return status; }
    public ExecutionMode getExecutionMode() { return executionMode; }
    public int getPage() { return page; }
    public int getSize() { return size; }
}
```

- [ ] **Step 3: Create RuleSnapshotSummary**

```java
package com.tzuchi.his.quality.inspection.application.sampling;

/**
 * Read model for rule snapshot display.
 */
public class RuleSnapshotSummary {

    private final String projectVersionId;
    private final int projectVersionNumber;
    private final String scoreSheetVersionId;
    private final String samplingPolicy;

    public RuleSnapshotSummary(String projectVersionId, int projectVersionNumber,
                                String scoreSheetVersionId, String samplingPolicy) {
        this.projectVersionId = projectVersionId;
        this.projectVersionNumber = projectVersionNumber;
        this.scoreSheetVersionId = scoreSheetVersionId;
        this.samplingPolicy = samplingPolicy;
    }

    public String getProjectVersionId() { return projectVersionId; }
    public int getProjectVersionNumber() { return projectVersionNumber; }
    public String getScoreSheetVersionId() { return scoreSheetVersionId; }
    public String getSamplingPolicy() { return samplingPolicy; }
}
```

- [ ] **Step 4: Create SelectedCaseRefView**

```java
package com.tzuchi.his.quality.inspection.application.sampling;

/**
 * Read model for a selected case reference in batch detail view.
 */
public class SelectedCaseRefView {

    private final String sourceCaseKey;
    private final String hospitalId;
    private final String medicalRecordType;

    public SelectedCaseRefView(String sourceCaseKey, String hospitalId, String medicalRecordType) {
        this.sourceCaseKey = sourceCaseKey;
        this.hospitalId = hospitalId;
        this.medicalRecordType = medicalRecordType;
    }

    public String getSourceCaseKey() { return sourceCaseKey; }
    public String getHospitalId() { return hospitalId; }
    public String getMedicalRecordType() { return medicalRecordType; }
}
```

- [ ] **Step 5: Create SamplingBatchSummary**

```java
package com.tzuchi.his.quality.inspection.application.sampling;

import com.tzuchi.his.quality.inspection.domain.sampling.BatchStatus;
import com.tzuchi.his.quality.inspection.domain.sampling.ExecutionMode;

import java.time.Instant;

/**
 * Read model for sampling batch list view.
 */
public class SamplingBatchSummary {

    private final String batchId;
    private final String projectId;
    private final ExecutionMode executionMode;
    private final BatchStatus status;
    private final int populationCount;
    private final int eligibleCount;
    private final int selectedCount;
    private final String retryOfBatchId;
    private final Instant startedAt;
    private final Instant finishedAt;

    public SamplingBatchSummary(String batchId, String projectId, ExecutionMode executionMode,
                                 BatchStatus status, int populationCount, int eligibleCount,
                                 int selectedCount, String retryOfBatchId,
                                 Instant startedAt, Instant finishedAt) {
        this.batchId = batchId;
        this.projectId = projectId;
        this.executionMode = executionMode;
        this.status = status;
        this.populationCount = populationCount;
        this.eligibleCount = eligibleCount;
        this.selectedCount = selectedCount;
        this.retryOfBatchId = retryOfBatchId;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
    }

    public String getBatchId() { return batchId; }
    public String getProjectId() { return projectId; }
    public ExecutionMode getExecutionMode() { return executionMode; }
    public BatchStatus getStatus() { return status; }
    public int getPopulationCount() { return populationCount; }
    public int getEligibleCount() { return eligibleCount; }
    public int getSelectedCount() { return selectedCount; }
    public String getRetryOfBatchId() { return retryOfBatchId; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
}
```

- [ ] **Step 6: Create SamplingBatchDetailView**

```java
package com.tzuchi.his.quality.inspection.application.sampling;

import com.tzuchi.his.quality.inspection.domain.sampling.BatchStatus;
import com.tzuchi.his.quality.inspection.domain.sampling.ExecutionMode;

import java.time.Instant;
import java.util.List;

/**
 * Read model for sampling batch detail view.
 */
public class SamplingBatchDetailView {

    private final String batchId;
    private final String projectId;
    private final ExecutionMode executionMode;
    private final BatchStatus status;
    private final int populationCount;
    private final int eligibleCount;
    private final int excludedCount;
    private final int selectedCount;
    private final String retryOfBatchId;
    private final String failureReason;
    private final Instant startedAt;
    private final Instant finishedAt;
    private final RuleSnapshotSummary ruleSnapshot;
    private final List<SelectedCaseRefView> selectedCases;

    public SamplingBatchDetailView(String batchId, String projectId, ExecutionMode executionMode,
                                    BatchStatus status, int populationCount, int eligibleCount,
                                    int excludedCount, int selectedCount,
                                    String retryOfBatchId, String failureReason,
                                    Instant startedAt, Instant finishedAt,
                                    RuleSnapshotSummary ruleSnapshot,
                                    List<SelectedCaseRefView> selectedCases) {
        this.batchId = batchId;
        this.projectId = projectId;
        this.executionMode = executionMode;
        this.status = status;
        this.populationCount = populationCount;
        this.eligibleCount = eligibleCount;
        this.excludedCount = excludedCount;
        this.selectedCount = selectedCount;
        this.retryOfBatchId = retryOfBatchId;
        this.failureReason = failureReason;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.ruleSnapshot = ruleSnapshot;
        this.selectedCases = selectedCases;
    }

    public String getBatchId() { return batchId; }
    public String getProjectId() { return projectId; }
    public ExecutionMode getExecutionMode() { return executionMode; }
    public BatchStatus getStatus() { return status; }
    public int getPopulationCount() { return populationCount; }
    public int getEligibleCount() { return eligibleCount; }
    public int getExcludedCount() { return excludedCount; }
    public int getSelectedCount() { return selectedCount; }
    public String getRetryOfBatchId() { return retryOfBatchId; }
    public String getFailureReason() { return failureReason; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public RuleSnapshotSummary getRuleSnapshot() { return ruleSnapshot; }
    public List<SelectedCaseRefView> getSelectedCases() { return selectedCases; }
}
```

- [ ] **Step 7: Create SamplingBatchQueryPort**

```java
package com.tzuchi.his.quality.inspection.application.sampling;

import com.tzuchi.his.quality.inspection.application.common.Page;

import java.util.Optional;

/**
 * Read-model query port for SamplingBatch summaries and details.
 * Implementations are in the infrastructure layer.
 */
public interface SamplingBatchQueryPort {
    Page<SamplingBatchSummary> findBatches(SamplingBatchQueryCriteria criteria);
    Optional<SamplingBatchDetailView> findDetailById(String batchId);
}
```

- [ ] **Step 8: Run tests to verify compilation**

Run: `cd services/quality-inspection-service && .\gradlew test`
Expected: ALL tests PASS

- [ ] **Step 9: Commit**

```bash
git add -A
git commit -m "feat(B2/S4): create read models, query port, and Page wrapper

- Page<T> generic pagination wrapper
- SamplingBatchQueryPort (hexagonal query port)
- SamplingBatchSummary, SamplingBatchDetailView
- RuleSnapshotSummary, SelectedCaseRefView
- SamplingBatchQueryCriteria

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 6: Create Query Use Cases + Export Skeleton

**Files:**
- Create: `application/sampling/ListSamplingBatchesUseCase.java`
- Create: `application/sampling/GetSamplingBatchDetailUseCase.java`
- Create: `application/sampling/SamplingBatchExportResult.java`
- Create: `application/sampling/ExportSamplingBatchResultUseCase.java`
- Test: `application/sampling/SamplingUseCaseTest.java`

- [ ] **Step 1: Create ListSamplingBatchesUseCase**

```java
package com.tzuchi.his.quality.inspection.application.sampling;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ListSamplingBatchesUseCase {

    private final SamplingBatchQueryPort queryPort;

    public ListSamplingBatchesUseCase(SamplingBatchQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    public Page<SamplingBatchSummary> execute(ActorContext actor,
                                               SamplingBatchQueryCriteria criteria) {
        actor.requireAnyRole(DomainRole.HOSPITAL_MANAGER, DomainRole.AUDITOR, DomainRole.SYSTEM_ADMIN);
        return queryPort.findBatches(criteria);
    }
}
```

- [ ] **Step 2: Create GetSamplingBatchDetailUseCase**

```java
package com.tzuchi.his.quality.inspection.application.sampling;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetSamplingBatchDetailUseCase {

    private final SamplingBatchQueryPort queryPort;

    public GetSamplingBatchDetailUseCase(SamplingBatchQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    public SamplingBatchDetailView execute(ActorContext actor, String batchId) {
        actor.requireAnyRole(DomainRole.HOSPITAL_MANAGER, DomainRole.AUDITOR, DomainRole.SYSTEM_ADMIN);
        return queryPort.findDetailById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("SAMPLING_BATCH_NOT_FOUND",
                        "SamplingBatch not found: " + batchId));
    }
}
```

- [ ] **Step 3: Create SamplingBatchExportResult**

```java
package com.tzuchi.his.quality.inspection.application.sampling;

/**
 * Wrapper for export result. Actual CSV/Excel conversion deferred to B7.
 */
public class SamplingBatchExportResult {

    private final SamplingBatchDetailView detail;

    public SamplingBatchExportResult(SamplingBatchDetailView detail) {
        this.detail = detail;
    }

    public SamplingBatchDetailView getDetail() { return detail; }
}
```

- [ ] **Step 4: Create ExportSamplingBatchResultUseCase**

```java
package com.tzuchi.his.quality.inspection.application.sampling;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ExportSamplingBatchResultUseCase {

    private final SamplingBatchQueryPort queryPort;

    public ExportSamplingBatchResultUseCase(SamplingBatchQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    public SamplingBatchExportResult execute(ActorContext actor, String batchId) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        SamplingBatchDetailView detail = queryPort.findDetailById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("SAMPLING_BATCH_NOT_FOUND",
                        "SamplingBatch not found: " + batchId));
        return new SamplingBatchExportResult(detail);
    }
}
```

- [ ] **Step 5: Add query use case tests in SamplingUseCaseTest**

Add an in-memory query port and tests. First, add the inner class:

```java
static class InMemoryBatchQueryPort implements SamplingBatchQueryPort {
    final List<SamplingBatchSummary> summaries = new ArrayList<>();
    final Map<String, SamplingBatchDetailView> details = new HashMap<>();

    @Override
    public com.tzuchi.his.quality.inspection.application.common.Page<SamplingBatchSummary> findBatches(
            SamplingBatchQueryCriteria criteria) {
        List<SamplingBatchSummary> filtered = summaries.stream()
                .filter(s -> criteria.getProjectId() == null || s.getProjectId().equals(criteria.getProjectId()))
                .filter(s -> criteria.getStatus() == null || s.getStatus().equals(criteria.getStatus()))
                .collect(Collectors.toList());
        return new com.tzuchi.his.quality.inspection.application.common.Page<>(
                filtered, criteria.getPage(), criteria.getSize(), filtered.size());
    }

    @Override
    public Optional<SamplingBatchDetailView> findDetailById(String batchId) {
        return Optional.ofNullable(details.get(batchId));
    }
}
```

Then add the tests:

```java
@Test
void listBatches_shouldDelegateToQueryPort() {
    InMemoryBatchQueryPort queryPort = new InMemoryBatchQueryPort();
    queryPort.summaries.add(new SamplingBatchSummary(
            "b1", "p1", ExecutionMode.MANUAL, BatchStatus.COMPLETED,
            10, 8, 5, null, Instant.now(), Instant.now()));
    ListSamplingBatchesUseCase listUseCase = new ListSamplingBatchesUseCase(queryPort);

    com.tzuchi.his.quality.inspection.application.common.Page<SamplingBatchSummary> result =
            listUseCase.execute(hospitalManagerActor,
                    new SamplingBatchQueryCriteria(null, null, null, 0, 20));

    assertEquals(1, result.getContent().size());
    assertEquals("b1", result.getContent().get(0).getBatchId());
}

@Test
void listBatches_unauthorizedRole_shouldThrow() {
    InMemoryBatchQueryPort queryPort = new InMemoryBatchQueryPort();
    ListSamplingBatchesUseCase listUseCase = new ListSamplingBatchesUseCase(queryPort);
    ActorContext reviewerActor = new ActorContext("u-rev", "Reviewer",
            EnumSet.of(DomainRole.REVIEWER), Optional.of("H001"));

    assertThrows(com.tzuchi.his.quality.inspection.application.common.UnauthorizedException.class,
            () -> listUseCase.execute(reviewerActor,
                    new SamplingBatchQueryCriteria(null, null, null, 0, 20)));
}

@Test
void getDetail_notFound_shouldThrow404() {
    InMemoryBatchQueryPort queryPort = new InMemoryBatchQueryPort();
    GetSamplingBatchDetailUseCase detailUseCase = new GetSamplingBatchDetailUseCase(queryPort);

    assertThrows(com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException.class,
            () -> detailUseCase.execute(hospitalManagerActor, "nonexistent"));
}

@Test
void export_shouldWrapDetailView() {
    InMemoryBatchQueryPort queryPort = new InMemoryBatchQueryPort();
    SamplingBatchDetailView detail = new SamplingBatchDetailView(
            "b1", "p1", ExecutionMode.MANUAL, BatchStatus.COMPLETED,
            10, 8, 2, 5, null, null, Instant.now(), Instant.now(),
            new RuleSnapshotSummary("PV-1", 1, "SS-1", "MANUAL"),
            List.of());
    queryPort.details.put("b1", detail);
    ExportSamplingBatchResultUseCase exportUseCase = new ExportSamplingBatchResultUseCase(queryPort);

    SamplingBatchExportResult result = exportUseCase.execute(hospitalManagerActor, "b1");

    assertEquals("b1", result.getDetail().getBatchId());
}
```

Add these imports at the top of SamplingUseCaseTest:
```java
import java.time.Instant;
import java.util.stream.Collectors;
import com.tzuchi.his.quality.inspection.application.common.Page;
```

- [ ] **Step 6: Run tests to verify**

Run: `cd services/quality-inspection-service && .\gradlew test`
Expected: ALL tests PASS

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat(B2/S5-S7): create List, GetDetail, Export use cases

- ListSamplingBatchesUseCase (HOSPITAL_MANAGER, AUDITOR, SYSTEM_ADMIN)
- GetSamplingBatchDetailUseCase (same roles, 404 on not found)
- ExportSamplingBatchResultUseCase skeleton (HOSPITAL_MANAGER only)
- SamplingBatchExportResult wrapper
Role scope filtering deferred to B6.

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 7: Update Infrastructure (Entity + Repository + Query Adapter)

**Files:**
- Modify: `infrastructure/persistence/sampling/SamplingBatchEntity.java`
- Modify: `infrastructure/persistence/sampling/SamplingBatchRepositoryImpl.java`
- Create: `infrastructure/persistence/sampling/SamplingBatchQueryAdapter.java`

- [ ] **Step 1: Update SamplingBatchEntity with new RuleSnapshot columns**

Replace the snapshot fields in `SamplingBatchEntity.java`. Remove:
```java
@Column(name = "rule_snapshot_project")
private String ruleSnapshotProject;

@Column(name = "rule_snapshot_policy")
private String ruleSnapshotPolicy;
```

Add:
```java
@Column(name = "project_version_id")
private String projectVersionId;

@Column(name = "project_version_number")
private int projectVersionNumber;

@Column(name = "score_sheet_version_id")
private String scoreSheetVersionId;

@Column(name = "sampling_policy")
private String samplingPolicy;

@Column(name = "snapshot_summary", length = 2000)
private String snapshotSummary;
```

Replace the old getters/setters:
```java
public String getRuleSnapshotProject() { return ruleSnapshotProject; }
public void setRuleSnapshotProject(String s) { this.ruleSnapshotProject = s; }

public String getRuleSnapshotPolicy() { return ruleSnapshotPolicy; }
public void setRuleSnapshotPolicy(String s) { this.ruleSnapshotPolicy = s; }
```

With:
```java
public String getProjectVersionId() { return projectVersionId; }
public void setProjectVersionId(String s) { this.projectVersionId = s; }

public int getProjectVersionNumber() { return projectVersionNumber; }
public void setProjectVersionNumber(int n) { this.projectVersionNumber = n; }

public String getScoreSheetVersionId() { return scoreSheetVersionId; }
public void setScoreSheetVersionId(String s) { this.scoreSheetVersionId = s; }

public String getSamplingPolicy() { return samplingPolicy; }
public void setSamplingPolicy(String s) { this.samplingPolicy = s; }

public String getSnapshotSummary() { return snapshotSummary; }
public void setSnapshotSummary(String s) { this.snapshotSummary = s; }
```

- [ ] **Step 2: Update SamplingBatchRepositoryImpl toEntity/toDomain**

In `SamplingBatchRepositoryImpl.java`, update `toEntity`:

Replace:
```java
e.setRuleSnapshotProject(b.getRuleSnapshot().getProjectVersionInfo());
e.setRuleSnapshotPolicy(b.getRuleSnapshot().getSamplingPolicyInfo());
```

With:
```java
e.setProjectVersionId(b.getRuleSnapshot().getProjectVersionId());
e.setProjectVersionNumber(b.getRuleSnapshot().getProjectVersionNumber());
e.setScoreSheetVersionId(b.getRuleSnapshot().getScoreSheetVersionId());
e.setSamplingPolicy(b.getRuleSnapshot().getSamplingPolicy());
e.setSnapshotSummary(b.getRuleSnapshot().getSnapshotSummary());
```

Update `toDomain`, replace:
```java
RuleSnapshot snapshot = new RuleSnapshot(e.getRuleSnapshotProject(), e.getRuleSnapshotPolicy());
```

With:
```java
RuleSnapshot snapshot = new RuleSnapshot(
        e.getProjectVersionId(),
        e.getProjectVersionNumber(),
        e.getScoreSheetVersionId(),
        e.getSamplingPolicy(),
        e.getSnapshotSummary()
);
```

- [ ] **Step 3: Create SamplingBatchQueryAdapter**

```java
package com.tzuchi.his.quality.inspection.infrastructure.persistence.sampling;

import com.tzuchi.his.quality.inspection.application.common.Page;
import com.tzuchi.his.quality.inspection.application.sampling.*;
import com.tzuchi.his.quality.inspection.domain.sampling.BatchStatus;
import com.tzuchi.his.quality.inspection.domain.sampling.ExecutionMode;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class SamplingBatchQueryAdapter implements SamplingBatchQueryPort {

    private final EntityManager em;

    public SamplingBatchQueryAdapter(EntityManager em) {
        this.em = em;
    }

    @Override
    public Page<SamplingBatchSummary> findBatches(SamplingBatchQueryCriteria criteria) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<SamplingBatchEntity> countRoot = countQuery.from(SamplingBatchEntity.class);
        countQuery.select(cb.count(countRoot));
        countQuery.where(buildPredicates(cb, countRoot, criteria).toArray(new Predicate[0]));
        long total = em.createQuery(countQuery).getSingleResult();

        // Data query
        CriteriaQuery<SamplingBatchEntity> dataQuery = cb.createQuery(SamplingBatchEntity.class);
        Root<SamplingBatchEntity> dataRoot = dataQuery.from(SamplingBatchEntity.class);
        dataQuery.where(buildPredicates(cb, dataRoot, criteria).toArray(new Predicate[0]));
        dataQuery.orderBy(cb.desc(dataRoot.get("startedAt")));

        TypedQuery<SamplingBatchEntity> typedQuery = em.createQuery(dataQuery);
        typedQuery.setFirstResult(criteria.getPage() * criteria.getSize());
        typedQuery.setMaxResults(criteria.getSize());

        List<SamplingBatchSummary> content = typedQuery.getResultList().stream()
                .map(this::toSummary)
                .collect(Collectors.toList());

        return new Page<>(content, criteria.getPage(), criteria.getSize(), total);
    }

    @Override
    public Optional<SamplingBatchDetailView> findDetailById(String batchId) {
        SamplingBatchEntity entity = em.find(SamplingBatchEntity.class, batchId);
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.of(toDetailView(entity));
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<SamplingBatchEntity> root,
                                             SamplingBatchQueryCriteria criteria) {
        List<Predicate> predicates = new ArrayList<>();
        if (criteria.getProjectId() != null) {
            predicates.add(cb.equal(root.get("projectId"), criteria.getProjectId()));
        }
        if (criteria.getStatus() != null) {
            predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
        }
        if (criteria.getExecutionMode() != null) {
            predicates.add(cb.equal(root.get("executionMode"), criteria.getExecutionMode()));
        }
        return predicates;
    }

    private SamplingBatchSummary toSummary(SamplingBatchEntity e) {
        return new SamplingBatchSummary(
                e.getBatchId(),
                e.getProjectId(),
                e.getExecutionMode(),
                e.getStatus(),
                e.getPopulationCount(),
                e.getEligibleCount(),
                e.getSelectedCount(),
                e.getRetryOfBatchId(),
                e.getStartedAt(),
                e.getFinishedAt()
        );
    }

    private SamplingBatchDetailView toDetailView(SamplingBatchEntity e) {
        RuleSnapshotSummary snapshotSummary = new RuleSnapshotSummary(
                e.getProjectVersionId(),
                e.getProjectVersionNumber(),
                e.getScoreSheetVersionId(),
                e.getSamplingPolicy()
        );

        List<SelectedCaseRefView> cases = e.getSelectedCases().stream()
                .map(c -> new SelectedCaseRefView(
                        c.getSourceCaseKey(),
                        c.getHospitalId(),
                        c.getMedicalRecordType()))
                .collect(Collectors.toList());

        return new SamplingBatchDetailView(
                e.getBatchId(),
                e.getProjectId(),
                e.getExecutionMode(),
                e.getStatus(),
                e.getPopulationCount(),
                e.getEligibleCount(),
                e.getExcludedCount(),
                e.getSelectedCount(),
                e.getRetryOfBatchId(),
                e.getFailureReason(),
                e.getStartedAt(),
                e.getFinishedAt(),
                snapshotSummary,
                cases
        );
    }
}
```

- [ ] **Step 4: Run tests to verify**

Run: `cd services/quality-inspection-service && .\gradlew test`
Expected: ALL tests PASS

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(B2/S8a-b): update entity + repo + create query adapter

- SamplingBatchEntity: 2 stub columns → 5 structured RuleSnapshot columns
- SamplingBatchRepositoryImpl: update toEntity/toDomain mapping
- SamplingBatchQueryAdapter: JPA CriteriaQuery for list + detail

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 8: Add GET Endpoints to SamplingController

**Files:**
- Modify: `interfaces/rest/SamplingController.java`
- Test: `interfaces/rest/SamplingControllerApiTest.java`

- [ ] **Step 1: Update SamplingController with GET endpoints**

Replace the full `SamplingController.java`:

```java
package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.Page;
import com.tzuchi.his.quality.inspection.application.sampling.*;
import com.tzuchi.his.quality.inspection.domain.sampling.BatchStatus;
import com.tzuchi.his.quality.inspection.domain.sampling.ExecutionMode;
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
    private final ListSamplingBatchesUseCase listUseCase;
    private final GetSamplingBatchDetailUseCase detailUseCase;
    private final ExportSamplingBatchResultUseCase exportUseCase;

    public SamplingController(PreviewSamplingPopulationUseCase previewUseCase,
                               ExecuteSamplingUseCase executeUseCase,
                               RetrySamplingBatchUseCase retryUseCase,
                               ListSamplingBatchesUseCase listUseCase,
                               GetSamplingBatchDetailUseCase detailUseCase,
                               ExportSamplingBatchResultUseCase exportUseCase) {
        this.previewUseCase = previewUseCase;
        this.executeUseCase = executeUseCase;
        this.retryUseCase = retryUseCase;
        this.listUseCase = listUseCase;
        this.detailUseCase = detailUseCase;
        this.exportUseCase = exportUseCase;
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

    @GetMapping("/sampling-batches")
    public ResponseEntity<ApiResponse<Page<SamplingBatchSummary>>> listBatches(
            @CurrentActor ActorContext actor,
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String executionMode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        BatchStatus batchStatus = status != null ? BatchStatus.valueOf(status) : null;
        ExecutionMode mode = executionMode != null ? ExecutionMode.valueOf(executionMode) : null;
        SamplingBatchQueryCriteria criteria = new SamplingBatchQueryCriteria(
                projectId, batchStatus, mode, page, size);
        Page<SamplingBatchSummary> result = listUseCase.execute(actor, criteria);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/sampling-batches/{batchId}")
    public ResponseEntity<ApiResponse<SamplingBatchDetailView>> getBatchDetail(
            @CurrentActor ActorContext actor,
            @PathVariable String batchId) {
        SamplingBatchDetailView detail = detailUseCase.execute(actor, batchId);
        return ResponseEntity.ok(ApiResponse.ok(detail));
    }

    @GetMapping("/sampling-batches/{batchId}/export")
    public ResponseEntity<ApiResponse<SamplingBatchExportResult>> exportBatchResult(
            @CurrentActor ActorContext actor,
            @PathVariable String batchId) {
        SamplingBatchExportResult result = exportUseCase.execute(actor, batchId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
```

- [ ] **Step 2: Add enum validation for status/executionMode query params**

The `valueOf` call above will throw `IllegalArgumentException` on invalid enum values, which `GlobalExceptionHandler` maps to 500. To return 400 instead, wrap the parsing:

In the `listBatches` method, replace the enum parsing lines with:

```java
BatchStatus batchStatus = null;
if (status != null) {
    try {
        batchStatus = BatchStatus.valueOf(status);
    } catch (IllegalArgumentException e) {
        throw new BadRequestException("Invalid status: " + status);
    }
}
ExecutionMode mode = null;
if (executionMode != null) {
    try {
        mode = ExecutionMode.valueOf(executionMode);
    } catch (IllegalArgumentException e) {
        throw new BadRequestException("Invalid executionMode: " + executionMode);
    }
}
```

- [ ] **Step 3: Add GET endpoint integration tests**

Add these tests in `SamplingControllerApiTest.java`:

Add import:
```java
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
```

```java
@Test
void listBatches_shouldReturn200WithPage() throws Exception {
    String projectId = createAndActivateProject("List Test");

    // Execute sampling to create a batch
    mockMvc.perform(post("/review-projects/" + projectId + "/sampling/execute")
                    .header("api-key", TestTokenFactory.hospitalManagerToken()))
            .andExpect(status().isCreated());

    mockMvc.perform(get("/sampling-batches")
                    .header("api-key", TestTokenFactory.hospitalManagerToken())
                    .param("projectId", projectId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isArray())
            .andExpect(jsonPath("$.data.content[0].batchId").isNotEmpty())
            .andExpect(jsonPath("$.data.totalElements").value(1));
}

@Test
void listBatches_invalidStatus_shouldReturn400() throws Exception {
    mockMvc.perform(get("/sampling-batches")
                    .header("api-key", TestTokenFactory.hospitalManagerToken())
                    .param("status", "INVALID_STATUS"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
}

@Test
void getBatchDetail_shouldReturn200WithDetail() throws Exception {
    String projectId = createAndActivateProject("Detail Test");

    MvcResult execResult = mockMvc.perform(
                    post("/review-projects/" + projectId + "/sampling/execute")
                            .header("api-key", TestTokenFactory.hospitalManagerToken()))
            .andExpect(status().isCreated())
            .andReturn();

    String batchId = objectMapper.readTree(
            execResult.getResponse().getContentAsString())
            .get("data").get("batchId").asText();

    mockMvc.perform(get("/sampling-batches/" + batchId)
                    .header("api-key", TestTokenFactory.hospitalManagerToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.batchId").value(batchId))
            .andExpect(jsonPath("$.data.projectId").value(projectId))
            .andExpect(jsonPath("$.data.status").value("COMPLETED"))
            .andExpect(jsonPath("$.data.ruleSnapshot.projectVersionId").isNotEmpty())
            .andExpect(jsonPath("$.data.selectedCases").isArray());
}

@Test
void getBatchDetail_notFound_shouldReturn404() throws Exception {
    mockMvc.perform(get("/sampling-batches/nonexistent")
                    .header("api-key", TestTokenFactory.hospitalManagerToken()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error.code").value("SAMPLING_BATCH_NOT_FOUND"));
}

@Test
void exportBatchResult_shouldReturn200() throws Exception {
    String projectId = createAndActivateProject("Export Test");

    MvcResult execResult = mockMvc.perform(
                    post("/review-projects/" + projectId + "/sampling/execute")
                            .header("api-key", TestTokenFactory.hospitalManagerToken()))
            .andExpect(status().isCreated())
            .andReturn();

    String batchId = objectMapper.readTree(
            execResult.getResponse().getContentAsString())
            .get("data").get("batchId").asText();

    mockMvc.perform(get("/sampling-batches/" + batchId + "/export")
                    .header("api-key", TestTokenFactory.hospitalManagerToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.detail.batchId").value(batchId));
}

@Test
void listBatches_systemAdmin_shouldReturn200() throws Exception {
    mockMvc.perform(get("/sampling-batches")
                    .header("api-key", TestTokenFactory.systemAdminToken()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isArray());
}

@Test
void listBatches_unknownRole_shouldReturn403() throws Exception {
    mockMvc.perform(get("/sampling-batches")
                    .header("api-key", TestTokenFactory.unknownRoleToken()))
            .andExpect(status().isForbidden());
}
```

- [ ] **Step 4: Run ALL tests**

Run: `cd services/quality-inspection-service && .\gradlew test`
Expected: ALL tests PASS (~230 tests)

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(B2/S8): add GET endpoints for sampling batch list, detail, export

- GET /sampling-batches (list with filters + pagination)
- GET /sampling-batches/{batchId} (detail with snapshot + cases)
- GET /sampling-batches/{batchId}/export (skeleton)
- Enum validation returns 400 for invalid status/executionMode
- Integration tests for all new endpoints

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

---

## Task 9: Final Verification

- [ ] **Step 1: Run full test suite**

Run: `cd services/quality-inspection-service && .\gradlew test`
Expected: ALL tests PASS

- [ ] **Step 2: Run build**

Run: `cd services/quality-inspection-service && .\gradlew build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Verify commit log**

Run: `cd services/quality-inspection-service && git --no-pager log --oneline -10`
Expected: 4-5 new commits for B2 tasks visible

---

## Out of Scope (Deferred)

| Item | Deferred To |
|------|------------|
| Full snapshot_json + checksum | B5 (Revision + History) |
| Role-based scope filtering (SYSTEM_ADMIN metadata only) | B6 (Permission-aware Queries) |
| CSV/Excel actual format conversion | B7 (Reporting / Audit) |
| CandidateSourcePort real HIS adapter | Integration phase |
| Cross-batch duplicate checking | N/A — D-007 scopes duplicate check per-batch |
