# B1a ReviewProject Command + Query — Design Spec

> Created: 2026-04-08
> Phase: B1a (B1 拆分之一)
> Scope: ReviewProject aggregate 完整 Command + Query + Events
> Depends on: B0 底座 (完成)
> Parent spec: `2026-04-07-b1-review-project-config-design.md`

---

## 1. Goal

完成 ReviewProject aggregate 的所有 B1 use case，包括：

- 修改既有 domain（新增 `description`、`boundScoreSheetId` 欄位；修正 `activate()`/`deactivate()` state guard）
- 修改既有 use case（CreateReviewProject 移除 hospitalId、加 event；Publish 加 event + scope guard）
- 新增 Command use case（UpdateDraft、Activate、Deactivate、BindScoreSheet）
- 新增 Query use case（List、Detail、Version）
- 新增 6 個 Domain Events
- 更新 JPA Entity / Repository / Controller / DTO
- 完整測試覆蓋

---

## 2. Domain Changes

### 2.1 ReviewProject — New Fields

```java
private String description;        // nullable
private String boundScoreSheetId;  // nullable
```

- `description` — 專案描述，可在 DRAFT 狀態修改
- `boundScoreSheetId` — 綁定的評量表 ID，activate 前必須綁定

### 2.2 ReviewProject.create() — Signature Change

```java
public static ReviewProject create(String projectId, String hospitalId, String name,
                                    String description,
                                    Set<MedicalRecordType> medicalRecordTypes) {
    // description nullable, stored as-is
}
```

- 新增 `description` 參數
- `boundScoreSheetId` 創建時不設定（null），後續由 `bindScoreSheet()` 設定

### 2.3 ReviewProject.reconstitute() — Signature Change

```java
public static ReviewProject reconstitute(String projectId, String hospitalId, String name,
                                          String description, String boundScoreSheetId,
                                          Set<MedicalRecordType> medicalRecordTypes,
                                          ProjectStatus status, int versionNumber,
                                          boolean versionPublished) {
    // 新增 description + boundScoreSheetId 恢復
}
```

### 2.4 New Methods

#### `updateDraft(String name, String description, Set<MedicalRecordType> medicalRecordTypes)`

```java
public void updateDraft(String name, String description, Set<MedicalRecordType> types) {
    requireStatusDraft();  // status == DRAFT
    Objects.requireNonNull(name, "name must not be null");
    if (name.isBlank()) {
        throw new DomainRuleViolationException("INVALID_PROJECT_NAME", "Project name must not be blank");
    }
    this.name = name;
    this.description = description;  // nullable
    this.medicalRecordTypes = (types != null && !types.isEmpty())
            ? EnumSet.copyOf(types)
            : EnumSet.noneOf(MedicalRecordType.class);
}
```

#### `bindScoreSheet(String scoreSheetId)`

```java
public void bindScoreSheet(String scoreSheetId) {
    requireStatusDraft();  // status == DRAFT
    Objects.requireNonNull(scoreSheetId, "scoreSheetId must not be null");
    this.boundScoreSheetId = scoreSheetId;
}
```

#### `requireStatusDraft()` — New Private Helper

```java
private void requireStatusDraft() {
    if (this.status != ProjectStatus.DRAFT) {
        throw new DomainStateConflictException("INVALID_STATE_TRANSITION",
                "Operation only allowed in DRAFT status, current: " + this.status);
    }
}
```

### 2.5 Existing Method Changes

#### `activate()` — Fix State Guard + Add Precondition

Current code allows DEACTIVATED → ACTIVE (bug). Fix:

```java
public void activate() {
    if (this.status == ProjectStatus.ACTIVE) {
        return;  // idempotent
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
```

State machine: `DRAFT → ACTIVE` only (DEACTIVATED is terminal, no re-activate).

Preconditions:
1. `currentVersion.isPublished()` — must have published version
2. `boundScoreSheetId != null` — must have bound score sheet (domain-only, no cross-aggregate validation)

#### `deactivate()` — Fix to Strict ACTIVE-only

Current code silently returns for DRAFT (should throw). Fix:

```java
public void deactivate() {
    if (this.status == ProjectStatus.DEACTIVATED) {
        return;  // idempotent
    }
    if (this.status != ProjectStatus.ACTIVE) {
        throw new DomainStateConflictException("INVALID_STATE_TRANSITION",
                "Can only deactivate from ACTIVE status, current: " + this.status);
    }
    this.status = ProjectStatus.DEACTIVATED;
}
```

State machine: `ACTIVE → DEACTIVATED` only. DEACTIVATED is terminal (not reversible).

#### `updateName()` — Deprecation / Coexistence Note

Existing `updateName()` checks `currentVersion.isPublished()` (version-level guard). The new `updateDraft()` checks `status == DRAFT` (lifecycle-level guard). For B1a:
- Keep `updateName()` for backward compatibility (existing tests use it)
- New code should use `updateDraft()` instead
- Consider removing `updateName()` in a future cleanup task

### 2.6 New Getters

```java
public String getDescription() { return description; }
public String getBoundScoreSheetId() { return boundScoreSheetId; }
```

### 2.7 ProjectStatus Javadoc Fix

Update from `DRAFT → ACTIVE ↔ DEACTIVATED` to `DRAFT → ACTIVE → DEACTIVATED` (one-way).

---

## 3. JPA / Persistence Changes

### 3.1 ReviewProjectEntity — New Columns

```java
@Column(name = "description")
private String description;

@Column(name = "bound_score_sheet_id")
private String boundScoreSheetId;
```

Both nullable. Add getters/setters.

### 3.2 ReviewProjectRepositoryImpl — Mapping Update

`toEntity()`:
```java
e.setDescription(project.getDescription());
e.setBoundScoreSheetId(project.getBoundScoreSheetId());
```

`toDomain()`:
```java
return ReviewProject.reconstitute(
    e.getProjectId(), e.getHospitalId(), e.getName(),
    e.getDescription(), e.getBoundScoreSheetId(),
    types, e.getStatus(), e.getVersionNumber(), e.isVersionPublished());
```

### 3.3 ReviewProjectJpaRepository — New Query Methods

```java
public interface ReviewProjectJpaRepository extends JpaRepository<ReviewProjectEntity, String> {
    Page<ReviewProjectEntity> findByHospitalId(String hospitalId, Pageable pageable);
}
```

### 3.4 Query Repository — Separate Read-Side Port

Query methods do NOT go on the domain `ReviewProjectRepository` (to avoid leaking Spring Data `Page`/`Pageable` into domain). Instead, create a separate query port in the application layer:

```java
// application.project.query package
public interface ReviewProjectQueryPort {
    Page<ReviewProjectSummary> findByHospitalId(String hospitalId, Pageable pageable);
    Page<ReviewProjectSummary> findAll(Pageable pageable);
}
```

Implementation in `infrastructure.persistence.project.ReviewProjectQueryAdapter` (uses the same JPA repository).

The domain `ReviewProjectRepository` stays unchanged:
```java
public interface ReviewProjectRepository {
    void save(ReviewProject project);
    Optional<ReviewProject> findById(String projectId);
}
```

### 3.5 ReviewProjectSummary — New Read Model

```java
// application.project.query package
public class ReviewProjectSummary {
    private final String projectId;
    private final String hospitalId;
    private final String name;
    private final String description;
    private final ProjectStatus status;
    private final String boundScoreSheetId;
    private final int currentVersionNumber;
    private final boolean currentVersionPublished;
    // constructor + getters
}
```

---

## 4. Use Case Changes

### 4.1 Modify: CreateReviewProjectUseCase

Changes:
- Remove `hospitalId` parameter → use `actor.requireHospitalId()`
- Add `description` parameter
- Add `DomainEventPublisher` dependency
- Publish `ReviewProjectCreatedEvent` after save

```java
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
```

### 4.2 Modify: PublishReviewProjectVersionUseCase

Changes:
- Add `actor.requireHospitalScope(project.getHospitalId())` — scope guard
- Add `DomainEventPublisher` dependency
- Publish `ReviewProjectVersionPublishedEvent` after save

```java
public void execute(ActorContext actor, String projectId) {
    actor.requireRole(DomainRole.HOSPITAL_MANAGER);
    ReviewProject project = repository.findById(projectId)
            .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
    actor.requireHospitalScope(project.getHospitalId());
    project.publishNewVersion();
    repository.save(project);
    eventPublisher.publish(new ReviewProjectVersionPublishedEvent(this, actor, projectId,
            project.getHospitalId()));
}
```

### 4.3 New: UpdateReviewProjectDraftUseCase

```java
@Service
@Transactional
public class UpdateReviewProjectDraftUseCase {
    private final ReviewProjectRepository repository;
    private final DomainEventPublisher eventPublisher;

    public void execute(ActorContext actor, String projectId, String name,
                        String description, Set<MedicalRecordType> medicalRecordTypes) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        ReviewProject project = repository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        actor.requireHospitalScope(project.getHospitalId());
        project.updateDraft(name, description, medicalRecordTypes);
        repository.save(project);
        eventPublisher.publish(new ReviewProjectDraftUpdatedEvent(this, actor, projectId,
                project.getHospitalId()));
    }
}
```

### 4.4 New: ActivateReviewProjectUseCase

```java
@Service
@Transactional
public class ActivateReviewProjectUseCase {
    private final ReviewProjectRepository repository;
    private final DomainEventPublisher eventPublisher;

    public void execute(ActorContext actor, String projectId) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        ReviewProject project = repository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        actor.requireHospitalScope(project.getHospitalId());
        project.activate();
        repository.save(project);
        eventPublisher.publish(new ReviewProjectActivatedEvent(this, actor, projectId,
                project.getHospitalId()));
    }
}
```

### 4.5 New: DeactivateReviewProjectUseCase

```java
@Service
@Transactional
public class DeactivateReviewProjectUseCase {
    private final ReviewProjectRepository repository;
    private final DomainEventPublisher eventPublisher;

    public void execute(ActorContext actor, String projectId) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        ReviewProject project = repository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        actor.requireHospitalScope(project.getHospitalId());
        project.deactivate();
        repository.save(project);
        eventPublisher.publish(new ReviewProjectDeactivatedEvent(this, actor, projectId,
                project.getHospitalId()));
    }
}
```

### 4.6 New: BindScoreSheetToProjectUseCase

```java
@Service
@Transactional
public class BindScoreSheetToProjectUseCase {
    private final ReviewProjectRepository repository;
    private final DomainEventPublisher eventPublisher;

    public void execute(ActorContext actor, String projectId, String scoreSheetId) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        ReviewProject project = repository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        actor.requireHospitalScope(project.getHospitalId());
        project.bindScoreSheet(scoreSheetId);
        repository.save(project);
        eventPublisher.publish(new ScoreSheetBoundToProjectEvent(this, actor, projectId,
                project.getHospitalId(), scoreSheetId));
    }
}
```

Note: `bindScoreSheet()` does NOT cross-validate that the scoreSheetId exists or is published. That responsibility belongs to the caller / API layer if needed in the future.

### 4.7 New: ListReviewProjectsUseCase

```java
@Service
@Transactional(readOnly = true)
public class ListReviewProjectsUseCase {
    private final ReviewProjectQueryPort queryPort;

    public Page<ReviewProjectSummary> execute(ActorContext actor, Pageable pageable) {
        actor.requireAnyRole(DomainRole.HOSPITAL_MANAGER, DomainRole.SYSTEM_ADMIN);
        if (actor.hasRole(DomainRole.SYSTEM_ADMIN)) {
            return queryPort.findAll(pageable);
        }
        String hospitalId = actor.requireHospitalId();
        return queryPort.findByHospitalId(hospitalId, pageable);
    }
}
```

### 4.8 New: GetReviewProjectDetailUseCase

```java
@Service
@Transactional(readOnly = true)
public class GetReviewProjectDetailUseCase {
    private final ReviewProjectRepository repository;

    public ReviewProject execute(ActorContext actor, String projectId) {
        actor.requireAnyRole(DomainRole.HOSPITAL_MANAGER, DomainRole.SYSTEM_ADMIN);
        ReviewProject project = repository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        if (!actor.hasRole(DomainRole.SYSTEM_ADMIN)) {
            actor.requireHospitalScope(project.getHospitalId());
        }
        return project;
    }
}
```

### 4.9 New: GetReviewProjectVersionUseCase

```java
@Service
@Transactional(readOnly = true)
public class GetReviewProjectVersionUseCase {
    private final ReviewProjectRepository repository;

    public ProjectVersion execute(ActorContext actor, String projectId) {
        actor.requireAnyRole(DomainRole.HOSPITAL_MANAGER, DomainRole.SYSTEM_ADMIN);
        ReviewProject project = repository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
        if (!actor.hasRole(DomainRole.SYSTEM_ADMIN)) {
            actor.requireHospitalScope(project.getHospitalId());
        }
        return project.getCurrentVersion();
    }
}
```

Simplified: returns current version only (no history table yet). API wraps in array for future compatibility.

---

## 5. Domain Events

Package: `domain.project.event`

All events extend `DomainEvent` and use the constructor:
```java
DomainEvent(source, actorUserId, actorUserName, hospitalId, entityType, entityId, action, detail)
```

### 5.1 ReviewProjectCreatedEvent

```java
public class ReviewProjectCreatedEvent extends DomainEvent {
    public ReviewProjectCreatedEvent(Object source, ActorContext actor,
                                      String projectId, String hospitalId) {
        super(source, actor.getUserId(), actor.getUserName(),
              hospitalId, "ReviewProject", projectId, "CREATED", null);
    }
}
```

### 5.2 ReviewProjectDraftUpdatedEvent

```java
public class ReviewProjectDraftUpdatedEvent extends DomainEvent {
    public ReviewProjectDraftUpdatedEvent(Object source, ActorContext actor,
                                           String projectId, String hospitalId) {
        super(source, actor.getUserId(), actor.getUserName(),
              hospitalId, "ReviewProject", projectId, "DRAFT_UPDATED", null);
    }
}
```

### 5.3 ReviewProjectVersionPublishedEvent

```java
public class ReviewProjectVersionPublishedEvent extends DomainEvent {
    public ReviewProjectVersionPublishedEvent(Object source, ActorContext actor,
                                               String projectId, String hospitalId) {
        super(source, actor.getUserId(), actor.getUserName(),
              hospitalId, "ReviewProject", projectId, "VERSION_PUBLISHED", null);
    }
}
```

### 5.4 ReviewProjectActivatedEvent

```java
public class ReviewProjectActivatedEvent extends DomainEvent {
    public ReviewProjectActivatedEvent(Object source, ActorContext actor,
                                        String projectId, String hospitalId) {
        super(source, actor.getUserId(), actor.getUserName(),
              hospitalId, "ReviewProject", projectId, "ACTIVATED", null);
    }
}
```

### 5.5 ReviewProjectDeactivatedEvent

```java
public class ReviewProjectDeactivatedEvent extends DomainEvent {
    public ReviewProjectDeactivatedEvent(Object source, ActorContext actor,
                                          String projectId, String hospitalId) {
        super(source, actor.getUserId(), actor.getUserName(),
              hospitalId, "ReviewProject", projectId, "DEACTIVATED", null);
    }
}
```

### 5.6 ScoreSheetBoundToProjectEvent

```java
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

---

## 6. DTOs & Controller

### 6.1 CreateReviewProjectRequest — Modify

Remove `hospitalId` field. Add `description` field.

```java
public class CreateReviewProjectRequest {
    @NotBlank
    private String name;
    private String description;  // nullable
    private Set<String> medicalRecordTypes;
    // getters/setters
}
```

### 6.2 UpdateReviewProjectRequest — New

```java
public class UpdateReviewProjectRequest {
    @NotBlank
    private String name;
    private String description;  // nullable
    private Set<String> medicalRecordTypes;
    // getters/setters
}
```

### 6.3 BindScoreSheetRequest — New

```java
public class BindScoreSheetRequest {
    @NotBlank
    private String scoreSheetId;
    // getter/setter
}
```

### 6.4 ReviewProjectResponse — New (for detail/list)

```java
public class ReviewProjectResponse {
    private String projectId;
    private String hospitalId;
    private String name;
    private String description;
    private ProjectStatus status;
    private String boundScoreSheetId;
    private int currentVersionNumber;
    private boolean currentVersionPublished;
    // constructor from domain or summary
}
```

### 6.5 ReviewProjectController — Full Endpoint Table

| Method | Path | Action | Use Case |
|--------|------|--------|----------|
| POST | `/review-projects` | Create | CreateReviewProjectUseCase |
| PUT | `/review-projects/{projectId}` | UpdateDraft | UpdateReviewProjectDraftUseCase |
| POST | `/review-projects/{projectId}/publish` | Publish | PublishReviewProjectVersionUseCase |
| POST | `/review-projects/{projectId}/activate` | Activate | ActivateReviewProjectUseCase |
| POST | `/review-projects/{projectId}/deactivate` | Deactivate | DeactivateReviewProjectUseCase |
| POST | `/review-projects/{projectId}/bind-score-sheet` | Bind | BindScoreSheetToProjectUseCase |
| GET | `/review-projects` | List | ListReviewProjectsUseCase |
| GET | `/review-projects/{projectId}` | Detail | GetReviewProjectDetailUseCase |
| GET | `/review-projects/{projectId}/version` | Version | GetReviewProjectVersionUseCase |

---

## 7. Permission Summary

| Use Case | Required Role | Scope Guard |
|----------|---------------|-------------|
| CreateReviewProject | HM | hospitalId from actor |
| UpdateDraft | HM | requireHospitalScope |
| PublishVersion | HM | requireHospitalScope |
| ActivateProject | HM | requireHospitalScope |
| DeactivateProject | HM | requireHospitalScope |
| BindScoreSheet | HM | requireHospitalScope |
| ListProjects | HM / SA | HM: auto-filter by hospitalId; SA: all |
| GetProjectDetail | HM / SA | HM: requireHospitalScope; SA: allow all |
| GetProjectVersion | HM / SA | same as detail |

HM = HOSPITAL_MANAGER, SA = SYSTEM_ADMIN

---

## 8. Test Strategy

### 8.1 Domain Unit Tests (no Spring context)

| Test | Covers |
|------|--------|
| create() with description | New field |
| updateDraft() happy path | DRAFT → update name/desc/types |
| updateDraft() rejects non-DRAFT | ACTIVE → throw DomainStateConflictException |
| bindScoreSheet() happy path | DRAFT → set boundScoreSheetId |
| bindScoreSheet() rejects non-DRAFT | ACTIVE → throw |
| bindScoreSheet() rejects null | null → throw NPE |
| activate() with boundScoreSheetId | DRAFT + published + bound → ACTIVE |
| activate() rejects no bound | DRAFT + published + null bound → throw DomainRuleViolation |
| activate() rejects DEACTIVATED | DEACTIVATED → throw DomainStateConflict |
| activate() idempotent | ACTIVE → ACTIVE (no throw) |
| deactivate() happy path | ACTIVE → DEACTIVATED |
| deactivate() rejects DRAFT | DRAFT → throw |
| deactivate() idempotent | DEACTIVATED → DEACTIVATED (no throw) |
| reconstitute() with new fields | All fields roundtrip |

### 8.2 Use Case Unit Tests (mock repo + mock event publisher)

For each use case:
- Happy path: correct actor, correct state → success + save + event published
- Wrong role → UnauthorizedException
- Wrong hospital scope → AccessDeniedException
- Project not found → IllegalArgumentException
- Invalid state → DomainStateConflictException or DomainRuleViolationException

### 8.3 Controller Integration Tests (@SpringBootTest + MockMvc)

- POST create → 201 + projectId
- PUT update → 200
- POST activate/deactivate/bind → 200
- GET list (HM) → filtered by hospital
- GET list (SA) → all projects
- GET detail → full response
- Wrong role → 403
- Cross-hospital → 403

### 8.4 Estimated New Tests

~40-50 new test methods across all layers.

---

## 9. Task Breakdown

### Phase 1: Domain Foundation

**T1 — ReviewProject Domain Changes**
- Add `description`, `boundScoreSheetId` fields
- Add `updateDraft()`, `bindScoreSheet()`, `requireStatusDraft()` methods
- Fix `activate()` — add DRAFT check + boundScoreSheetId check
- Fix `deactivate()` — strict ACTIVE-only check
- Update `create()` signature (add description)
- Update `reconstitute()` signature (add description + boundScoreSheetId)
- Add new getters
- Update `ProjectStatus` javadoc
- Domain unit tests (~14 tests)

**T2 — JPA Entity + Repository Updates**
- Add `description`, `boundScoreSheetId` to `ReviewProjectEntity`
- Update `ReviewProjectRepositoryImpl` mapping (toEntity + toDomain)
- Add `findByHospitalId(hospitalId, Pageable)` to JPA repo
- Create `ReviewProjectSummary` read model in `application.project.query`
- Create `ReviewProjectQueryPort` interface in `application.project.query`
- Create `ReviewProjectQueryAdapter` implementation in `infrastructure.persistence.project`

**T3 — Domain Events**
- Create `domain.project.event` package
- 6 event classes (Created, DraftUpdated, VersionPublished, Activated, Deactivated, ScoreSheetBound)

### Phase 2: Modify Existing Use Cases

**T4 — Modify CreateReviewProjectUseCase**
- Remove `hospitalId` param, use `actor.requireHospitalId()`
- Add `description` param
- Add `DomainEventPublisher` dependency + publish event
- Update `CreateReviewProjectRequest` DTO (remove hospitalId, add description)
- Update existing tests + new tests

**T5 — Modify PublishReviewProjectVersionUseCase**
- Add `actor.requireHospitalScope()` guard
- Add `DomainEventPublisher` dependency + publish event
- Update tests

### Phase 3: New Command Use Cases

**T6 — UpdateReviewProjectDraftUseCase**
- New use case + `UpdateReviewProjectRequest` DTO
- Unit tests (happy + wrong role + wrong scope + wrong state)

**T7 — ActivateReviewProjectUseCase**
- New use case
- Unit tests (happy + no bound + no published version + wrong status)

**T8 — DeactivateReviewProjectUseCase**
- New use case
- Unit tests (happy + wrong status)

**T9 — BindScoreSheetToProjectUseCase**
- New use case + `BindScoreSheetRequest` DTO
- Unit tests (happy + wrong role + wrong scope + wrong state)

### Phase 4: Query Use Cases

**T10 — ListReviewProjectsUseCase**
- New use case
- Unit tests (HM filtered + SA all + wrong role)

**T11 — GetReviewProjectDetailUseCase + GetReviewProjectVersionUseCase**
- Two use cases (simple, can combine in one task)
- Unit tests (HM scope check + SA allow all + not found)

### Phase 5: API Layer

**T12 — ReviewProjectController Updates**
- Add all new endpoints (PUT, POST activate/deactivate/bind, GET list/detail/version)
- Wire new use cases
- Add `ReviewProjectResponse` DTO
- Integration tests (happy paths + 403 + 409)

### Phase 6: Verification

**T13 — Full Verification**
- Run full test suite
- bootRun smoke test
- Push to origin

---

## 10. Not in Scope

- ScoreSheetDefinition changes (→ B1b)
- Version history table / multi-version persistence
- SYSTEM_ADMIN metadata-only response differentiation
- Cross-aggregate validation (bindScoreSheet does not verify scoreSheetId exists)
- Pagination filter parameters beyond hospitalId (status, name fuzzy search — future)
- `updateName()` removal (keep for backward compat, deprecate later)
