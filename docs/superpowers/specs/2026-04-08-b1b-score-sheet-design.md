# B1b — ScoreSheetDefinition CRUD + Query Design Spec

> Phase 1 第二部分：ScoreSheetDefinition 的完整 CRUD、state machine、action-based 更新與 query。
> 在既有程式碼上增量修改，不重寫。

---

## 1. Scope & Boundaries

### 1.1 In Scope

- ScoreSheetDefinition aggregate 增量修改（updateDraft + requiredRule 欄位）
- HospitalIdResolver port（查 ReviewProject 取 hospitalId，供 scope guard 使用）
- 5 個新 use case（UpdateDraft + 4 query）
- 修改 2 個既有 use case（CreateDraft 加 event + scope guard、Publish 加 event + scope guard + 404 修正）
- 7 個 controller endpoint（修改 2 + 新增 5）
- JPA 映射更新（requiredRule 欄位）
- InMemoryScoreSheetRepository testfixture
- Domain / Application / API integration tests

### 1.2 Out of Scope（不實作，僅紀錄）

| 項目 | 備註 |
|------|------|
| freezeVersion / snapshotForSubmission | B3（Assignment + Initial Review）才需要，ReviewSubmission 建立時凍結 |
| Multi-version evolution | 目前 DRAFT→PUBLISHED 不可逆；新版本需建立新 ScoreSheetDefinition |
| visibilityRule | 未確認需求 |
| changeWeight / reorderSections API | YAGNI |
| Cross-aggregate transaction | ScoreSheet 與 ReviewProject 是獨立 aggregate，不跨 transaction boundary |
| api-spec.md Score Sheet section 更新 | B1b 完成後單獨更新 |

### 1.3 State Machine

```
DRAFT → PUBLISHED
```

不變，已正確實作於 `ScoreSheetDefinition.publishVersion()`。

---

## 2. Domain Layer Changes

### 2.1 RequiredRule — New Enum

```java
package com.tzuchi.his.quality.inspection.domain.scoresheet;

public enum RequiredRule {
    ALWAYS_REQUIRED,
    OPTIONAL,
    CONDITIONAL
}
```

### 2.2 ScoreItem — Add requiredRule Fields

```java
// 新增欄位
private RequiredRule requiredRule;
private String conditionExpression;    // nullable, 僅 CONDITIONAL 時有值

// Constructor 新增兩個參數（尾部）
public ScoreItem(String itemId, String code, String title,
                 ScoreType scoreType, BigDecimal maxScore, BigDecimal weight,
                 RequiredRule requiredRule, String conditionExpression) {
    // 既有 validation...
    Objects.requireNonNull(requiredRule, "requiredRule must not be null");
    if (requiredRule == RequiredRule.CONDITIONAL) {
        if (conditionExpression == null || conditionExpression.isBlank()) {
            throw new IllegalArgumentException(
                "conditionExpression must not be blank when requiredRule is CONDITIONAL");
        }
    }
    this.requiredRule = requiredRule;
    this.conditionExpression = conditionExpression;
}

// Getters
public RequiredRule getRequiredRule() { return requiredRule; }
public String getConditionExpression() { return conditionExpression; }
```

**Backward compatibility:** 既有的 6-param constructor 保留，requiredRule 預設 `ALWAYS_REQUIRED`，conditionExpression 預設 `null`。

### 2.3 ScoreSheetSection — Add Mutation Methods

```java
public void updateTitle(String newTitle) {
    Objects.requireNonNull(newTitle, "title must not be null");
    this.title = newTitle;
}

public void removeItem(String itemId) {
    boolean removed = items.removeIf(i -> i.getItemId().equals(itemId));
    if (!removed) {
        throw new DomainRuleViolationException("ITEM_NOT_FOUND",
            "Item not found: " + itemId);
    }
}

// addItem() 已存在，不需修改
```

### 2.4 UpdateDraftCommand — New Value Object

```java
package com.tzuchi.his.quality.inspection.domain.scoresheet;

import java.util.List;
import java.util.Collections;

public class UpdateDraftCommand {
    private final List<AddSectionCommand> addSections;
    private final List<UpdateSectionCommand> updateSections;
    private final List<String> removeSectionIds;
    private final List<AddItemCommand> addItems;
    private final List<UpdateItemCommand> updateItems;
    private final List<String> removeItemIds;

    // Constructor, getters (all lists default to empty)
}
```

Sub-commands:

```java
public class AddSectionCommand {
    private final String sectionId;
    private final String title;
    private final List<AddItemCommand> items;  // optional initial items
}

public class UpdateSectionCommand {
    private final String sectionId;
    private final String title;  // new title
}

public class AddItemCommand {
    private final String targetSectionId;
    private final String itemId;
    private final String code;
    private final String title;
    private final ScoreType scoreType;
    private final BigDecimal maxScore;
    private final BigDecimal weight;
    private final RequiredRule requiredRule;
    private final String conditionExpression;
}

public class UpdateItemCommand {
    private final String itemId;
    private final String code;
    private final String title;
    private final ScoreType scoreType;
    private final BigDecimal maxScore;
    private final BigDecimal weight;
    private final RequiredRule requiredRule;
    private final String conditionExpression;
}
```

### 2.5 ScoreSheetDefinition.updateDraft() — New Method

```java
public void updateDraft(UpdateDraftCommand command) {
    requireDraft();
    Objects.requireNonNull(command, "command must not be null");

    // 1. Remove sections
    for (String sectionId : command.getRemoveSectionIds()) {
        boolean removed = sections.removeIf(s -> s.getSectionId().equals(sectionId));
        if (!removed) {
            throw new DomainRuleViolationException("SECTION_NOT_FOUND",
                "Section not found: " + sectionId);
        }
    }

    // 2. Remove items (cross-section search)
    for (String itemId : command.getRemoveItemIds()) {
        boolean removed = false;
        for (ScoreSheetSection section : sections) {
            try {
                section.removeItem(itemId);
                removed = true;
                break;
            } catch (DomainRuleViolationException e) {
                // not in this section, continue
            }
        }
        if (!removed) {
            throw new DomainRuleViolationException("ITEM_NOT_FOUND",
                "Item not found: " + itemId);
        }
    }

    // 3. Update section titles
    for (UpdateSectionCommand update : command.getUpdateSections()) {
        ScoreSheetSection section = findSection(update.getSectionId());
        section.updateTitle(update.getTitle());
    }

    // 4. Update items
    for (UpdateItemCommand update : command.getUpdateItems()) {
        updateItemInSections(update);
    }

    // 5. Add new sections (with cross-section uniqueness check, reuses addSection())
    for (AddSectionCommand add : command.getAddSections()) {
        ScoreSheetSection newSection = new ScoreSheetSection(add.getSectionId(), add.getTitle());
        for (AddItemCommand itemCmd : add.getItems()) {
            newSection.addItem(toScoreItem(itemCmd));
        }
        addSection(newSection);  // existing method handles uniqueness
    }

    // 6. Add new items to existing sections
    for (AddItemCommand add : command.getAddItems()) {
        ScoreSheetSection section = findSection(add.getTargetSectionId());
        ScoreItem newItem = toScoreItem(add);
        // Cross-section uniqueness check
        validateCrossSectionUniqueness(newItem, section);
        section.addItem(newItem);
    }
}

private ScoreSheetSection findSection(String sectionId) {
    return sections.stream()
        .filter(s -> s.getSectionId().equals(sectionId))
        .findFirst()
        .orElseThrow(() -> new DomainRuleViolationException("SECTION_NOT_FOUND",
            "Section not found: " + sectionId));
}

private void validateCrossSectionUniqueness(ScoreItem newItem, ScoreSheetSection excludeSection) {
    for (ScoreSheetSection section : sections) {
        if (section == excludeSection) continue;
        for (ScoreItem existing : section.getItems()) {
            if (existing.getItemId().equals(newItem.getItemId())) {
                throw new DomainStateConflictException("DUPLICATE_ITEM_ID",
                    "Duplicate itemId across sections: " + newItem.getItemId());
            }
            if (existing.getCode().equals(newItem.getCode())) {
                throw new DomainStateConflictException("DUPLICATE_ITEM_CODE",
                    "Duplicate item code across sections: " + newItem.getCode());
            }
        }
    }
}
```

**Invariant maintenance:**
- addSection: existing cross-section uniqueness check (unchanged)
- addItem to existing section: new cross-section uniqueness check
- updateItem: if code changes, validate new code doesn't conflict
- removeSection / removeItem: no uniqueness check needed

---

## 3. Application Layer Changes

### 3.1 HospitalIdResolver — New Port

```java
package com.tzuchi.his.quality.inspection.application.common;

/**
 * Resolves hospitalId via cross-aggregate relationships.
 * Implementations are in the infrastructure layer.
 */
public interface HospitalIdResolver {
    String resolveByProjectId(String projectId);
}
```

### 3.2 Modify: CreateScoreSheetDraftUseCase

Changes:
- Add `HospitalIdResolver` → resolve hospitalId via projectId → `actor.requireHospitalScope()`
- Add `DomainEventPublisher` → publish `ScoreSheetDraftCreatedEvent`
- Keep existing logic intact

```java
@Service
@Transactional
public class CreateScoreSheetDraftUseCase {

    private final ScoreSheetRepository repository;
    private final HospitalIdResolver hospitalIdResolver;
    private final DomainEventPublisher eventPublisher;

    public String execute(ActorContext actor, String projectId) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        String hospitalId = hospitalIdResolver.resolveByProjectId(projectId);
        actor.requireHospitalScope(hospitalId);

        String scoreSheetId = UUID.randomUUID().toString();
        ScoreSheetDefinition scoreSheet = ScoreSheetDefinition.createDraft(scoreSheetId, projectId);
        repository.save(scoreSheet);
        eventPublisher.publish(new ScoreSheetDraftCreatedEvent(this, actor, scoreSheetId, projectId, hospitalId));
        return scoreSheetId;
    }
}
```

### 3.3 Modify: PublishScoreSheetVersionUseCase

Changes:
- Add `HospitalIdResolver` → scope guard
- Add `DomainEventPublisher` → publish `ScoreSheetVersionPublishedEvent`
- Replace `IllegalArgumentException` with `ResourceNotFoundException`

```java
@Service
@Transactional
public class PublishScoreSheetVersionUseCase {

    private final ScoreSheetRepository repository;
    private final HospitalIdResolver hospitalIdResolver;
    private final DomainEventPublisher eventPublisher;

    public void execute(ActorContext actor, String scoreSheetId) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        ScoreSheetDefinition scoreSheet = repository.findById(scoreSheetId)
                .orElseThrow(() -> new ResourceNotFoundException("SCORE_SHEET_NOT_FOUND",
                    "ScoreSheet not found: " + scoreSheetId));
        String hospitalId = hospitalIdResolver.resolveByProjectId(scoreSheet.getProjectId());
        actor.requireHospitalScope(hospitalId);

        scoreSheet.publishVersion();
        repository.save(scoreSheet);
        eventPublisher.publish(new ScoreSheetVersionPublishedEvent(this, actor, scoreSheetId,
                scoreSheet.getProjectId(), hospitalId));
    }
}
```

### 3.4 New: UpdateScoreSheetDraftUseCase

```java
@Service
@Transactional
public class UpdateScoreSheetDraftUseCase {

    private final ScoreSheetRepository repository;
    private final HospitalIdResolver hospitalIdResolver;
    private final DomainEventPublisher eventPublisher;

    public void execute(ActorContext actor, String scoreSheetId, UpdateDraftCommand command) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        ScoreSheetDefinition scoreSheet = repository.findById(scoreSheetId)
                .orElseThrow(() -> new ResourceNotFoundException("SCORE_SHEET_NOT_FOUND",
                    "ScoreSheet not found: " + scoreSheetId));
        String hospitalId = hospitalIdResolver.resolveByProjectId(scoreSheet.getProjectId());
        actor.requireHospitalScope(hospitalId);

        scoreSheet.updateDraft(command);
        repository.save(scoreSheet);
        eventPublisher.publish(new ScoreSheetDraftUpdatedEvent(this, actor, scoreSheetId,
                scoreSheet.getProjectId(), hospitalId));
    }
}
```

### 3.5 New: Query Use Cases

#### ScoreSheetQueryPort — New Port

```java
package com.tzuchi.his.quality.inspection.application.scoresheet;

import java.util.List;
import java.util.Optional;

/**
 * Read-model query port for ScoreSheetDefinition summaries.
 * Implementations are in the infrastructure layer.
 */
public interface ScoreSheetQueryPort {
    List<ScoreSheetSummary> findByHospitalId(String hospitalId);
    List<ScoreSheetSummary> findAll();
    Optional<ScoreSheetSummary> findSummaryById(String scoreSheetId);
}
```

#### ScoreSheetSummary — New Read Model

```java
package com.tzuchi.his.quality.inspection.application.scoresheet;

public class ScoreSheetSummary {
    private final String scoreSheetId;
    private final String projectId;
    private final ScoreSheetStatus status;
    private final int version;
    private final int sectionCount;
    private final int itemCount;

    // constructor + getters
}
```

#### ListScoreSheetSummariesUseCase

```java
@Service
@Transactional(readOnly = true)
public class ListScoreSheetSummariesUseCase {

    private final ScoreSheetQueryPort queryPort;

    public List<ScoreSheetSummary> execute(ActorContext actor) {
        if (actor.hasRole(DomainRole.SYSTEM_ADMIN)) {
            return queryPort.findAll();
        }
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        return queryPort.findByHospitalId(actor.getHospitalId());
    }
}
```

#### GetScoreSheetDetailUseCase

```java
@Service
@Transactional(readOnly = true)
public class GetScoreSheetDetailUseCase {

    private final ScoreSheetQueryPort queryPort;
    private final HospitalIdResolver hospitalIdResolver;

    public ScoreSheetSummary execute(ActorContext actor, String scoreSheetId) {
        ScoreSheetSummary summary = queryPort.findSummaryById(scoreSheetId)
                .orElseThrow(() -> new ResourceNotFoundException("SCORE_SHEET_NOT_FOUND",
                    "ScoreSheet not found: " + scoreSheetId));

        if (actor.hasRole(DomainRole.SYSTEM_ADMIN)) {
            return summary;
        }
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        String hospitalId = hospitalIdResolver.resolveByProjectId(summary.getProjectId());
        actor.requireHospitalScope(hospitalId);
        return summary;
    }
}
```

#### GetScoreSheetVersionUseCase

```java
@Service
@Transactional(readOnly = true)
public class GetScoreSheetVersionUseCase {

    private final ScoreSheetRepository repository;
    private final HospitalIdResolver hospitalIdResolver;

    public ScoreSheetVersionInfo execute(ActorContext actor, String scoreSheetId) {
        ScoreSheetDefinition scoreSheet = repository.findById(scoreSheetId)
                .orElseThrow(() -> new ResourceNotFoundException("SCORE_SHEET_NOT_FOUND",
                    "ScoreSheet not found: " + scoreSheetId));

        if (!actor.hasRole(DomainRole.SYSTEM_ADMIN)) {
            actor.requireRole(DomainRole.HOSPITAL_MANAGER);
            String hospitalId = hospitalIdResolver.resolveByProjectId(scoreSheet.getProjectId());
            actor.requireHospitalScope(hospitalId);
        }
        return new ScoreSheetVersionInfo(scoreSheet.getVersion(),
                scoreSheet.getStatus() == ScoreSheetStatus.PUBLISHED);
    }
}
```

`ScoreSheetVersionInfo` is a simple value object: `int version`, `boolean published`.

#### GetScoreSheetSectionsUseCase — HM Only

```java
@Service
@Transactional(readOnly = true)
public class GetScoreSheetSectionsUseCase {

    private final ScoreSheetRepository repository;
    private final HospitalIdResolver hospitalIdResolver;

    public List<ScoreSheetSection> execute(ActorContext actor, String scoreSheetId) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);  // SA cannot access
        ScoreSheetDefinition scoreSheet = repository.findById(scoreSheetId)
                .orElseThrow(() -> new ResourceNotFoundException("SCORE_SHEET_NOT_FOUND",
                    "ScoreSheet not found: " + scoreSheetId));
        String hospitalId = hospitalIdResolver.resolveByProjectId(scoreSheet.getProjectId());
        actor.requireHospitalScope(hospitalId);
        return scoreSheet.getSections();
    }
}
```

### 3.6 Domain Events

Three new events, all extend `ApplicationEvent`:

| Event | Fields |
|-------|--------|
| `ScoreSheetDraftCreatedEvent` | actor, scoreSheetId, projectId, hospitalId |
| `ScoreSheetDraftUpdatedEvent` | actor, scoreSheetId, projectId, hospitalId |
| `ScoreSheetVersionPublishedEvent` | actor, scoreSheetId, projectId, hospitalId |

Package: `com.tzuchi.his.quality.inspection.domain.scoresheet`

---

## 4. Infrastructure Layer Changes

### 4.1 ScoreItemEntity — Add requiredRule Fields

```java
@Column(name = "required_rule", nullable = false)
private String requiredRule;           // "ALWAYS_REQUIRED" / "OPTIONAL" / "CONDITIONAL"

@Column(name = "condition_expression")
private String conditionExpression;    // nullable

// + getters/setters
```

### 4.2 ScoreSheetRepositoryImpl — Update Mapping

`toItemEntity()` and `toDomain()` updated to include `requiredRule` + `conditionExpression`.

### 4.3 HospitalIdResolverImpl — New

```java
package com.tzuchi.his.quality.inspection.infrastructure.adapter;

@Component
public class HospitalIdResolverImpl implements HospitalIdResolver {
    private final ReviewProjectRepository projectRepository;

    public String resolveByProjectId(String projectId) {
        return projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("PROJECT_NOT_FOUND",
                "ReviewProject not found: " + projectId))
            .getHospitalId();
    }
}
```

### 4.4 ScoreSheetQueryAdapter — New

```java
package com.tzuchi.his.quality.inspection.infrastructure.persistence.scoresheet;

@Component
public class ScoreSheetQueryAdapter implements ScoreSheetQueryPort {
    private final ScoreSheetJpaRepository jpa;

    // findByHospitalId: JPQL join score_sheet + review_project on projectId, filter by hospitalId
    // findAll: load all score_sheet entities, map to ScoreSheetSummary
    // findSummaryById: load by ID, map to ScoreSheetSummary
}
```

Note: `findByHospitalId` requires a JPQL query joining `score_sheet` with the `review_project` table (via `project_id`), since `score_sheet` doesn't store `hospital_id` directly.

### 4.5 InMemoryScoreSheetRepository — Testfixture

```java
package com.tzuchi.his.quality.inspection.testfixture;

public class InMemoryScoreSheetRepository implements ScoreSheetRepository {
    private final Map<String, ScoreSheetDefinition> store = new LinkedHashMap<>();

    @Override
    public void save(ScoreSheetDefinition scoreSheet) {
        store.put(scoreSheet.getScoreSheetId(), scoreSheet);
    }

    @Override
    public Optional<ScoreSheetDefinition> findById(String scoreSheetId) {
        return Optional.ofNullable(store.get(scoreSheetId));
    }

    // test helper methods: clear(), size(), getAll()
}
```

---

## 5. Controller & DTO Changes

### 5.1 Modify Existing Endpoints

| Endpoint | Changes |
|----------|---------|
| `POST /score-sheets` | No code change needed (already uses @CurrentActor) |
| `POST /score-sheets/{id}/publish-version` | Rename path to `/publish` (aligning with B1a pattern) |

### 5.2 New Endpoints

| Method | Path | Use Case | Role | Response |
|--------|------|----------|------|----------|
| PUT | `/score-sheets/{id}` | UpdateDraft | HM | `200` + `ApiResponse.ok(null)` |
| GET | `/score-sheets` | List | HM + SA | `200` + `ApiResponse.ok(List<ScoreSheetSummaryResponse>)` |
| GET | `/score-sheets/{id}` | Detail | HM + SA | `200` + `ApiResponse.ok(ScoreSheetSummaryResponse)` |
| GET | `/score-sheets/{id}/version` | Version | HM + SA | `200` + `ApiResponse.ok(Map)` |
| GET | `/score-sheets/{id}/sections` | Sections | **HM only** | `200` + `ApiResponse.ok(List<ScoreSheetSectionResponse>)` |

### 5.3 New DTOs

#### UpdateScoreSheetDraftRequest

```java
public class UpdateScoreSheetDraftRequest {
    private List<AddSectionRequest> addSections;
    private List<UpdateSectionRequest> updateSections;
    private List<String> removeSectionIds;
    private List<AddItemRequest> addItems;
    private List<UpdateItemRequest> updateItems;
    private List<String> removeItemIds;

    // Each sub-request maps to corresponding domain command
}
```

#### ScoreSheetSummaryResponse

```java
public class ScoreSheetSummaryResponse {
    private String scoreSheetId;
    private String projectId;
    private String status;
    private int version;
    private int sectionCount;
    private int itemCount;
}
```

#### ScoreSheetSectionResponse (for GET /sections)

```java
public class ScoreSheetSectionResponse {
    private String sectionId;
    private String title;
    private List<ScoreSheetItemResponse> items;
}

public class ScoreSheetItemResponse {
    private String itemId;
    private String code;
    private String title;
    private String scoreType;
    private BigDecimal maxScore;
    private BigDecimal weight;
    private String requiredRule;
    private String conditionExpression;
}
```

---

## 6. Permission Summary

| Use Case | Required Role | Scope Guard |
|----------|---------------|-------------|
| CreateScoreSheetDraft | HM | hospitalId via HospitalIdResolver |
| UpdateDraft | HM | hospitalId via HospitalIdResolver |
| PublishVersion | HM | hospitalId via HospitalIdResolver |
| ListSummaries | HM / SA | HM: own hospital; SA: all |
| GetDetail | HM / SA | HM: requireHospitalScope; SA: no scope |
| GetVersion | HM / SA | HM: requireHospitalScope; SA: no scope |
| GetSections | **HM only** | requireHospitalScope (SA cannot access) |

---

## 7. Testing Strategy

### 7.1 Domain Tests (expand ScoreSheetDefinitionTest)

- updateDraft: add/update/remove sections & items (happy path)
- updateDraft on published sheet → DomainStateConflictException
- Cross-section uniqueness after addItem / updateItem
- requiredRule validation (CONDITIONAL without expression → throws)
- removeSection / removeItem on nonexistent → DomainRuleViolationException
- updateSection title
- ScoreItem backward-compatible 6-param constructor
- Estimated: ~15-20 new test cases

### 7.2 Application Tests (use case layer)

- InMemoryScoreSheetRepository + CapturingDomainEventPublisher (shared testfixture)
- Stub HospitalIdResolver (returns fixed hospitalId)
- Per use case:
  - Happy path + event published
  - Permission denied → UnauthorizedException
  - Scope guard failed → AccessDeniedException
  - Not found → ResourceNotFoundException
  - Query: HM sees own hospital, SA sees all, SA cannot access sections
- Estimated: ~25-30 new test cases

### 7.3 API Tests (@WebMvcTest + MockMvc)

- 7 endpoints × (happy path + primary error cases)
- JSON structure, status codes, error envelope
- Estimated: ~15-20 new test cases

### 7.4 Totals

- New: ~55-70 test cases
- Existing ScoreSheet domain: 13 tests
- Expected total: ~215+ tests (repo-wide)

---

## 8. Task Breakdown

| Task | Layer | Content | Depends On |
|------|-------|---------|------------|
| T1 | Domain | RequiredRule enum + ScoreItem 8-param constructor + backward-compat 6-param + ScoreSheetSection mutation methods | — |
| T2 | Domain | UpdateDraftCommand + sub-commands + ScoreSheetDefinition.updateDraft() | T1 |
| T3 | Domain | Domain tests (updateDraft + requiredRule + mutations) | T2 |
| T4 | Infra | ScoreItemEntity requiredRule fields + RepositoryImpl mapping update | T1 |
| T5 | Application | HospitalIdResolver port + stub impl + modify CreateDraft/Publish use cases (scope guard + events + 404) | T4 |
| T6 | Application | UpdateScoreSheetDraftUseCase + domain events (3 events) | T5 |
| T7 | Application | 4 Query use cases + ScoreSheetQueryPort + ScoreSheetSummary + ScoreSheetVersionInfo | T5 |
| T8 | Infra | ScoreSheetQueryAdapter + InMemoryScoreSheetRepository testfixture | T7 |
| T9 | Application | Application layer tests (all use cases) | T6, T8 |
| T10 | Interfaces | Controller 7 endpoints + DTOs + API integration tests | T9 |

---

## 9. Spec Deviations

Known differences from authority docs (spec follows code, same pattern as B1a):

1. **Publish path**: `/publish` not `/publish-version` (aligning with B1a pattern)
2. **Version path**: `/version` (singular) not `/versions`
3. **No pagination**: List returns `List<>` not `Page<>`
4. **ResourceNotFoundException** for 404 (not IllegalArgumentException)
5. **Query use cases** in `application/scoresheet/` (no sub-package)
6. **ScoreSheetSummary has no hospitalId** — ScoreSheet relates to hospital via projectId
7. **GET /sections HM only** — SA=metadata-only interpretation; permission-matrix doesn't explicitly list this endpoint

---

## 10. Existing Code Inventory

Files that will be modified (not rewritten):

| File | Change |
|------|--------|
| `domain/scoresheet/ScoreItem.java` | Add requiredRule fields + backward-compat constructor |
| `domain/scoresheet/ScoreSheetSection.java` | Add updateTitle(), removeItem() |
| `domain/scoresheet/ScoreSheetDefinition.java` | Add updateDraft() + helper methods |
| `application/scoresheet/CreateScoreSheetDraftUseCase.java` | Add HospitalIdResolver + event + scope guard |
| `application/scoresheet/PublishScoreSheetVersionUseCase.java` | Add HospitalIdResolver + event + scope guard + 404 fix |
| `interfaces/rest/ScoreSheetController.java` | Add 5 endpoints, modify publish path |
| `infrastructure/persistence/scoresheet/ScoreItemEntity.java` | Add requiredRule columns |
| `infrastructure/persistence/scoresheet/ScoreSheetRepositoryImpl.java` | Update mapping |

New files:

| File | Purpose |
|------|---------|
| `domain/scoresheet/RequiredRule.java` | Enum |
| `domain/scoresheet/UpdateDraftCommand.java` | Value object (includes sub-commands as inner classes) |
| `domain/scoresheet/ScoreSheetDraftCreatedEvent.java` | Domain event |
| `domain/scoresheet/ScoreSheetDraftUpdatedEvent.java` | Domain event |
| `domain/scoresheet/ScoreSheetVersionPublishedEvent.java` | Domain event |
| `application/common/HospitalIdResolver.java` | Port interface |
| `application/scoresheet/UpdateScoreSheetDraftUseCase.java` | Use case |
| `application/scoresheet/ListScoreSheetSummariesUseCase.java` | Query use case |
| `application/scoresheet/GetScoreSheetDetailUseCase.java` | Query use case |
| `application/scoresheet/GetScoreSheetVersionUseCase.java` | Query use case |
| `application/scoresheet/GetScoreSheetSectionsUseCase.java` | Query use case |
| `application/scoresheet/ScoreSheetQueryPort.java` | Query port |
| `application/scoresheet/ScoreSheetSummary.java` | Read model |
| `application/scoresheet/ScoreSheetVersionInfo.java` | Value object |
| `infrastructure/adapter/HospitalIdResolverImpl.java` | Port impl |
| `infrastructure/persistence/scoresheet/ScoreSheetQueryAdapter.java` | Query adapter |
| `testfixture/InMemoryScoreSheetRepository.java` | Test double |
| `interfaces/rest/dto/UpdateScoreSheetDraftRequest.java` | Request DTO |
| `interfaces/rest/dto/ScoreSheetSummaryResponse.java` | Response DTO |
| `interfaces/rest/dto/ScoreSheetSectionResponse.java` | Response DTO |
| `interfaces/rest/dto/ScoreSheetItemResponse.java` | Response DTO |
