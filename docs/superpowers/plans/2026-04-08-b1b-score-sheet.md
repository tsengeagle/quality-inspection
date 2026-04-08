# B1b ScoreSheetDefinition Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement ScoreSheetDefinition CRUD (action-based updateDraft + requiredRule), query endpoints, and HospitalIdResolver port — incremental modification of existing code.

**Architecture:** Domain-driven design with hexagonal architecture. Incremental changes to existing ScoreSheetDefinition aggregate, adding updateDraft (action-based), requiredRule fields, HospitalIdResolver port for cross-aggregate scope guard, query use cases with separate port/adapter, and REST controller endpoints. Same patterns as B1a.

**Tech Stack:** Java 11, Spring Boot 2.7.18, JPA/Hibernate, H2 (test), Gradle 7.6.4

**Spec:** `docs/superpowers/specs/2026-04-08-b1b-score-sheet-design.md`

**Build/Test command:** `cd services/quality-inspection-service && .\gradlew clean test`

**Package root:** `com.tzuchi.his.quality.inspection`

**Source root:** `services/quality-inspection-service/src/main/java/com/tzuchi/his/quality/inspection`

**Test root:** `services/quality-inspection-service/src/test/java/com/tzuchi/his/quality/inspection`

---

## File Structure

### Files to Modify

| File | Responsibility |
|------|---------------|
| `domain/scoresheet/ScoreItem.java` | Add requiredRule + conditionExpression fields, 8-param constructor, keep 6-param backward-compat |
| `domain/scoresheet/ScoreSheetSection.java` | Add updateTitle(), removeItem() mutation methods |
| `domain/scoresheet/ScoreSheetDefinition.java` | Add updateDraft(UpdateDraftCommand), findSection(), validateCrossSectionUniqueness(), toScoreItem() helpers |
| `application/scoresheet/CreateScoreSheetDraftUseCase.java` | Add HospitalIdResolver + DomainEventPublisher deps, scope guard, event |
| `application/scoresheet/PublishScoreSheetVersionUseCase.java` | Add HospitalIdResolver + DomainEventPublisher deps, scope guard, event, fix 404 |
| `interfaces/rest/ScoreSheetController.java` | Rename publish path, add 5 new endpoints, inject new use cases |
| `infrastructure/persistence/scoresheet/ScoreItemEntity.java` | Add required_rule + condition_expression columns |
| `infrastructure/persistence/scoresheet/ScoreSheetRepositoryImpl.java` | Update toEntity/toDomain for requiredRule fields |
| `domain/scoresheet/ScoreSheetDefinitionTest.java` (test) | Add updateDraft + requiredRule tests |

### Files to Create

| File | Responsibility |
|------|---------------|
| `domain/scoresheet/RequiredRule.java` | Enum: ALWAYS_REQUIRED, OPTIONAL, CONDITIONAL |
| `domain/scoresheet/UpdateDraftCommand.java` | Value object with sub-command inner classes |
| `domain/scoresheet/event/ScoreSheetDraftCreatedEvent.java` | Domain event |
| `domain/scoresheet/event/ScoreSheetDraftUpdatedEvent.java` | Domain event |
| `domain/scoresheet/event/ScoreSheetVersionPublishedEvent.java` | Domain event |
| `application/common/HospitalIdResolver.java` | Port interface |
| `application/scoresheet/UpdateScoreSheetDraftUseCase.java` | Command use case |
| `application/scoresheet/ListScoreSheetSummariesUseCase.java` | Query use case |
| `application/scoresheet/GetScoreSheetDetailUseCase.java` | Query use case |
| `application/scoresheet/GetScoreSheetVersionUseCase.java` | Query use case |
| `application/scoresheet/GetScoreSheetSectionsUseCase.java` | Query use case (HM only) |
| `application/scoresheet/ScoreSheetQueryPort.java` | Query port interface |
| `application/scoresheet/ScoreSheetSummary.java` | Read model |
| `application/scoresheet/ScoreSheetVersionInfo.java` | Value object |
| `infrastructure/adapter/HospitalIdResolverImpl.java` | Port implementation |
| `infrastructure/persistence/scoresheet/ScoreSheetQueryAdapter.java` | Query port implementation |
| `testfixture/InMemoryScoreSheetRepository.java` | Test double |
| `interfaces/rest/dto/UpdateScoreSheetDraftRequest.java` | Request DTO (with inner sub-request classes) |
| `interfaces/rest/dto/ScoreSheetSummaryResponse.java` | Response DTO |
| `interfaces/rest/dto/ScoreSheetSectionResponse.java` | Response DTO |
| `interfaces/rest/dto/ScoreSheetItemResponse.java` | Response DTO |

### Test Files to Create

| File | Responsibility |
|------|---------------|
| `application/scoresheet/CreateScoreSheetDraftUseCaseTest.java` | Use case tests with InMemory repo + CapturingPublisher |
| `application/scoresheet/PublishScoreSheetVersionUseCaseTest.java` | Use case tests |
| `application/scoresheet/UpdateScoreSheetDraftUseCaseTest.java` | Use case tests |
| `application/scoresheet/ListScoreSheetSummariesUseCaseTest.java` | Query use case tests |
| `application/scoresheet/GetScoreSheetDetailUseCaseTest.java` | Query use case tests |
| `application/scoresheet/GetScoreSheetVersionUseCaseTest.java` | Query use case tests |
| `application/scoresheet/GetScoreSheetSectionsUseCaseTest.java` | Query use case tests (HM only enforcement) |
| `interfaces/rest/ScoreSheetApiTest.java` | @SpringBootTest integration tests |

### Test Files to Modify

| File | Reason |
|------|--------|
| `domain/scoresheet/ScoreSheetDefinitionTest.java` | Add updateDraft + requiredRule test cases |

---

## Task 1: Domain — RequiredRule Enum + ScoreItem Enhancement

**Files:**
- Create: `domain/scoresheet/RequiredRule.java`
- Modify: `domain/scoresheet/ScoreItem.java`

- [ ] **Step 1: Create RequiredRule enum**

Create `services/quality-inspection-service/src/main/java/com/tzuchi/his/quality/inspection/domain/scoresheet/RequiredRule.java`:

```java
package com.tzuchi.his.quality.inspection.domain.scoresheet;

public enum RequiredRule {
    ALWAYS_REQUIRED,
    OPTIONAL,
    CONDITIONAL
}
```

- [ ] **Step 2: Add 8-param constructor and fields to ScoreItem**

Modify `domain/scoresheet/ScoreItem.java`:

Add two new fields after `weight`:

```java
private RequiredRule requiredRule;
private String conditionExpression;
```

Add the new 8-param constructor (keep existing 6-param as backward-compat):

```java
public ScoreItem(String itemId, String code, String title,
                 ScoreType scoreType, BigDecimal maxScore, BigDecimal weight,
                 RequiredRule requiredRule, String conditionExpression) {
    Objects.requireNonNull(itemId, "itemId must not be null");
    Objects.requireNonNull(code, "code must not be null");
    Objects.requireNonNull(title, "title must not be null");
    Objects.requireNonNull(scoreType, "scoreType must not be null");
    Objects.requireNonNull(maxScore, "maxScore must not be null");
    if (maxScore.compareTo(BigDecimal.ZERO) < 0) {
        throw new IllegalArgumentException("maxScore must not be negative");
    }
    Objects.requireNonNull(requiredRule, "requiredRule must not be null");
    if (requiredRule == RequiredRule.CONDITIONAL) {
        if (conditionExpression == null || conditionExpression.isBlank()) {
            throw new IllegalArgumentException(
                    "conditionExpression must not be blank when requiredRule is CONDITIONAL");
        }
    }
    this.itemId = itemId;
    this.code = code;
    this.title = title;
    this.scoreType = scoreType;
    this.maxScore = maxScore;
    this.weight = weight != null ? weight : BigDecimal.ONE;
    this.requiredRule = requiredRule;
    this.conditionExpression = conditionExpression;
}
```

Update the existing 6-param constructor to delegate to the 8-param one:

```java
public ScoreItem(String itemId, String code, String title,
                 ScoreType scoreType, BigDecimal maxScore, BigDecimal weight) {
    this(itemId, code, title, scoreType, maxScore, weight,
         RequiredRule.ALWAYS_REQUIRED, null);
}
```

Add getters:

```java
public RequiredRule getRequiredRule() { return requiredRule; }
public String getConditionExpression() { return conditionExpression; }
```

- [ ] **Step 3: Add mutation methods to ScoreSheetSection**

Modify `domain/scoresheet/ScoreSheetSection.java`:

Add import at top:

```java
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
```

Add two new methods after `addItem()`:

```java
public void updateTitle(String newTitle) {
    Objects.requireNonNull(newTitle, "title must not be null");
    if (newTitle.isBlank()) {
        throw new IllegalArgumentException("title must not be blank");
    }
    this.title = newTitle;
}

public void removeItem(String itemId) {
    boolean removed = items.removeIf(i -> i.getItemId().equals(itemId));
    if (!removed) {
        throw new DomainRuleViolationException("ITEM_NOT_FOUND",
                "Item not found in section: " + itemId);
    }
}
```

- [ ] **Step 4: Run tests to verify backward compatibility**

Run: `cd services/quality-inspection-service && .\gradlew clean test`
Expected: All 147 existing tests PASS (6-param constructor still works everywhere).

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(b1b): add RequiredRule enum, enhance ScoreItem and ScoreSheetSection

- RequiredRule enum: ALWAYS_REQUIRED, OPTIONAL, CONDITIONAL
- ScoreItem: 8-param constructor with requiredRule + conditionExpression
- ScoreItem: backward-compat 6-param constructor delegates to 8-param
- ScoreSheetSection: add updateTitle(), removeItem() mutation methods
- CONDITIONAL requires non-blank conditionExpression"
```

---

## Task 2: Domain — UpdateDraftCommand + ScoreSheetDefinition.updateDraft()

**Files:**
- Create: `domain/scoresheet/UpdateDraftCommand.java`
- Modify: `domain/scoresheet/ScoreSheetDefinition.java`

- [ ] **Step 1: Create UpdateDraftCommand with inner sub-command classes**

Create `services/quality-inspection-service/src/main/java/com/tzuchi/his/quality/inspection/domain/scoresheet/UpdateDraftCommand.java`:

```java
package com.tzuchi.his.quality.inspection.domain.scoresheet;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

public class UpdateDraftCommand {

    private final List<AddSectionCommand> addSections;
    private final List<UpdateSectionCommand> updateSections;
    private final List<String> removeSectionIds;
    private final List<AddItemCommand> addItems;
    private final List<UpdateItemCommand> updateItems;
    private final List<String> removeItemIds;

    public UpdateDraftCommand(List<AddSectionCommand> addSections,
                              List<UpdateSectionCommand> updateSections,
                              List<String> removeSectionIds,
                              List<AddItemCommand> addItems,
                              List<UpdateItemCommand> updateItems,
                              List<String> removeItemIds) {
        this.addSections = addSections != null ? addSections : Collections.emptyList();
        this.updateSections = updateSections != null ? updateSections : Collections.emptyList();
        this.removeSectionIds = removeSectionIds != null ? removeSectionIds : Collections.emptyList();
        this.addItems = addItems != null ? addItems : Collections.emptyList();
        this.updateItems = updateItems != null ? updateItems : Collections.emptyList();
        this.removeItemIds = removeItemIds != null ? removeItemIds : Collections.emptyList();
    }

    public List<AddSectionCommand> getAddSections() { return addSections; }
    public List<UpdateSectionCommand> getUpdateSections() { return updateSections; }
    public List<String> getRemoveSectionIds() { return removeSectionIds; }
    public List<AddItemCommand> getAddItems() { return addItems; }
    public List<UpdateItemCommand> getUpdateItems() { return updateItems; }
    public List<String> getRemoveItemIds() { return removeItemIds; }

    // --- Inner command classes ---

    public static class AddSectionCommand {
        private final String sectionId;
        private final String title;
        private final List<AddItemCommand> items;

        public AddSectionCommand(String sectionId, String title, List<AddItemCommand> items) {
            this.sectionId = sectionId;
            this.title = title;
            this.items = items != null ? items : Collections.emptyList();
        }

        public String getSectionId() { return sectionId; }
        public String getTitle() { return title; }
        public List<AddItemCommand> getItems() { return items; }
    }

    public static class UpdateSectionCommand {
        private final String sectionId;
        private final String title;

        public UpdateSectionCommand(String sectionId, String title) {
            this.sectionId = sectionId;
            this.title = title;
        }

        public String getSectionId() { return sectionId; }
        public String getTitle() { return title; }
    }

    public static class AddItemCommand {
        private final String targetSectionId;
        private final String itemId;
        private final String code;
        private final String title;
        private final ScoreType scoreType;
        private final BigDecimal maxScore;
        private final BigDecimal weight;
        private final RequiredRule requiredRule;
        private final String conditionExpression;

        public AddItemCommand(String targetSectionId, String itemId, String code, String title,
                              ScoreType scoreType, BigDecimal maxScore, BigDecimal weight,
                              RequiredRule requiredRule, String conditionExpression) {
            this.targetSectionId = targetSectionId;
            this.itemId = itemId;
            this.code = code;
            this.title = title;
            this.scoreType = scoreType;
            this.maxScore = maxScore;
            this.weight = weight;
            this.requiredRule = requiredRule;
            this.conditionExpression = conditionExpression;
        }

        public String getTargetSectionId() { return targetSectionId; }
        public String getItemId() { return itemId; }
        public String getCode() { return code; }
        public String getTitle() { return title; }
        public ScoreType getScoreType() { return scoreType; }
        public BigDecimal getMaxScore() { return maxScore; }
        public BigDecimal getWeight() { return weight; }
        public RequiredRule getRequiredRule() { return requiredRule; }
        public String getConditionExpression() { return conditionExpression; }
    }

    public static class UpdateItemCommand {
        private final String itemId;
        private final String code;
        private final String title;
        private final ScoreType scoreType;
        private final BigDecimal maxScore;
        private final BigDecimal weight;
        private final RequiredRule requiredRule;
        private final String conditionExpression;

        public UpdateItemCommand(String itemId, String code, String title,
                                 ScoreType scoreType, BigDecimal maxScore, BigDecimal weight,
                                 RequiredRule requiredRule, String conditionExpression) {
            this.itemId = itemId;
            this.code = code;
            this.title = title;
            this.scoreType = scoreType;
            this.maxScore = maxScore;
            this.weight = weight;
            this.requiredRule = requiredRule;
            this.conditionExpression = conditionExpression;
        }

        public String getItemId() { return itemId; }
        public String getCode() { return code; }
        public String getTitle() { return title; }
        public ScoreType getScoreType() { return scoreType; }
        public BigDecimal getMaxScore() { return maxScore; }
        public BigDecimal getWeight() { return weight; }
        public RequiredRule getRequiredRule() { return requiredRule; }
        public String getConditionExpression() { return conditionExpression; }
    }
}
```

- [ ] **Step 2: Add updateDraft() and helper methods to ScoreSheetDefinition**

Modify `domain/scoresheet/ScoreSheetDefinition.java`:

Add import:

```java
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
```

Add the following methods after the existing `publishVersion()` method (before `requireDraft()`):

```java
public void updateDraft(UpdateDraftCommand command) {
    requireDraft();
    Objects.requireNonNull(command, "command must not be null");

    // 1. Remove sections first
    for (String sectionId : command.getRemoveSectionIds()) {
        boolean removed = sections.removeIf(s -> s.getSectionId().equals(sectionId));
        if (!removed) {
            throw new DomainRuleViolationException("SECTION_NOT_FOUND",
                    "Section not found: " + sectionId);
        }
    }

    // 2. Remove items (cross-section search)
    for (String itemId : command.getRemoveItemIds()) {
        boolean found = false;
        for (ScoreSheetSection section : sections) {
            if (section.getItems().stream().anyMatch(i -> i.getItemId().equals(itemId))) {
                section.removeItem(itemId);
                found = true;
                break;
            }
        }
        if (!found) {
            throw new DomainRuleViolationException("ITEM_NOT_FOUND",
                    "Item not found: " + itemId);
        }
    }

    // 3. Update section titles
    for (UpdateDraftCommand.UpdateSectionCommand update : command.getUpdateSections()) {
        ScoreSheetSection section = findSection(update.getSectionId());
        section.updateTitle(update.getTitle());
    }

    // 4. Update items (find item across sections, replace it)
    for (UpdateDraftCommand.UpdateItemCommand update : command.getUpdateItems()) {
        updateItemInSections(update);
    }

    // 5. Add new sections (reuses existing addSection() which enforces uniqueness)
    for (UpdateDraftCommand.AddSectionCommand add : command.getAddSections()) {
        ScoreSheetSection newSection = new ScoreSheetSection(add.getSectionId(), add.getTitle());
        for (UpdateDraftCommand.AddItemCommand itemCmd : add.getItems()) {
            newSection.addItem(toScoreItem(itemCmd));
        }
        addSection(newSection);
    }

    // 6. Add items to existing sections (with cross-section uniqueness)
    for (UpdateDraftCommand.AddItemCommand add : command.getAddItems()) {
        ScoreSheetSection section = findSection(add.getTargetSectionId());
        ScoreItem newItem = toScoreItem(add);
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

private void updateItemInSections(UpdateDraftCommand.UpdateItemCommand update) {
    for (ScoreSheetSection section : sections) {
        for (int i = 0; i < section.getItems().size(); i++) {
            if (section.getItems().get(i).getItemId().equals(update.getItemId())) {
                // Check code uniqueness if code changed
                ScoreItem oldItem = section.getItems().get(i);
                if (!oldItem.getCode().equals(update.getCode())) {
                    ScoreItem tempItem = new ScoreItem(update.getItemId(), update.getCode(),
                            update.getTitle(), update.getScoreType(), update.getMaxScore(),
                            update.getWeight(), update.getRequiredRule(), update.getConditionExpression());
                    validateCrossSectionUniqueness(tempItem, null);
                }
                // Replace the item — remove old, add new at same position
                section.removeItem(update.getItemId());
                ScoreItem replacement = new ScoreItem(update.getItemId(), update.getCode(),
                        update.getTitle(), update.getScoreType(), update.getMaxScore(),
                        update.getWeight(), update.getRequiredRule(), update.getConditionExpression());
                section.addItem(replacement);
                return;
            }
        }
    }
    throw new DomainRuleViolationException("ITEM_NOT_FOUND",
            "Item not found: " + update.getItemId());
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

private ScoreItem toScoreItem(UpdateDraftCommand.AddItemCommand cmd) {
    return new ScoreItem(cmd.getItemId(), cmd.getCode(), cmd.getTitle(),
            cmd.getScoreType(), cmd.getMaxScore(), cmd.getWeight(),
            cmd.getRequiredRule(), cmd.getConditionExpression());
}
```

Note: `ScoreSheetSection.getItems()` returns an unmodifiable list. The `updateItemInSections` method uses `section.removeItem()` (which operates on the internal mutable list) then `section.addItem()`.

- [ ] **Step 3: Compile check**

Run: `cd services/quality-inspection-service && .\gradlew compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Run existing tests to verify no regression**

Run: `cd services/quality-inspection-service && .\gradlew clean test`
Expected: All 147 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(b1b): add UpdateDraftCommand and ScoreSheetDefinition.updateDraft()

- UpdateDraftCommand value object with 4 inner sub-command classes
- Action-based updateDraft: remove sections/items, update titles/items, add sections/items
- Cross-section uniqueness enforced for add and update operations
- Execution order: remove → update → add (safe for all combinations)"
```

---

## Task 3: Domain — Domain Tests for updateDraft + requiredRule

**Files:**
- Modify: `domain/scoresheet/ScoreSheetDefinitionTest.java` (test)

- [ ] **Step 1: Add requiredRule tests to ScoreSheetDefinitionTest**

Add these tests to `ScoreSheetDefinitionTest.java`:

```java
@Test
void scoreItem_8param_withRequiredRule_shouldSucceed() {
    ScoreItem item = new ScoreItem("i1", "C01", "Item",
            ScoreType.FIXED, BigDecimal.TEN, BigDecimal.ONE,
            RequiredRule.ALWAYS_REQUIRED, null);
    assertEquals(RequiredRule.ALWAYS_REQUIRED, item.getRequiredRule());
    assertNull(item.getConditionExpression());
}

@Test
void scoreItem_conditional_withExpression_shouldSucceed() {
    ScoreItem item = new ScoreItem("i1", "C01", "Item",
            ScoreType.FIXED, BigDecimal.TEN, BigDecimal.ONE,
            RequiredRule.CONDITIONAL, "age > 65");
    assertEquals(RequiredRule.CONDITIONAL, item.getRequiredRule());
    assertEquals("age > 65", item.getConditionExpression());
}

@Test
void scoreItem_conditional_noExpression_shouldThrow() {
    assertThrows(IllegalArgumentException.class,
            () -> new ScoreItem("i1", "C01", "Item",
                    ScoreType.FIXED, BigDecimal.TEN, BigDecimal.ONE,
                    RequiredRule.CONDITIONAL, null));
}

@Test
void scoreItem_conditional_blankExpression_shouldThrow() {
    assertThrows(IllegalArgumentException.class,
            () -> new ScoreItem("i1", "C01", "Item",
                    ScoreType.FIXED, BigDecimal.TEN, BigDecimal.ONE,
                    RequiredRule.CONDITIONAL, "  "));
}

@Test
void scoreItem_6param_shouldDefaultToAlwaysRequired() {
    ScoreItem item = new ScoreItem("i1", "C01", "Item",
            ScoreType.FIXED, BigDecimal.TEN, BigDecimal.ONE);
    assertEquals(RequiredRule.ALWAYS_REQUIRED, item.getRequiredRule());
    assertNull(item.getConditionExpression());
}

@Test
void scoreItem_nullRequiredRule_shouldThrow() {
    assertThrows(NullPointerException.class,
            () -> new ScoreItem("i1", "C01", "Item",
                    ScoreType.FIXED, BigDecimal.TEN, BigDecimal.ONE,
                    null, null));
}
```

Add the import: `import com.tzuchi.his.quality.inspection.domain.scoresheet.RequiredRule;`

Also add: `import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;`

- [ ] **Step 2: Add updateDraft tests**

Add these tests to `ScoreSheetDefinitionTest.java`:

```java
// --- test helper ---

private ScoreSheetDefinition createSheetWithOneItem() {
    ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft("ss1", "p1");
    ScoreSheetSection section = new ScoreSheetSection("sec1", "General");
    section.addItem(new ScoreItem("i1", "C01", "Item1",
            ScoreType.FIXED, BigDecimal.TEN, BigDecimal.ONE));
    ss.addSection(section);
    return ss;
}

// --- updateDraft tests ---

@Test
void updateDraft_addSection_shouldSucceed() {
    ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft("ss1", "p1");
    UpdateDraftCommand cmd = new UpdateDraftCommand(
            List.of(new UpdateDraftCommand.AddSectionCommand("sec1", "General",
                    List.of(new UpdateDraftCommand.AddItemCommand("sec1", "i1", "C01", "Item1",
                            ScoreType.FIXED, BigDecimal.TEN, BigDecimal.ONE,
                            RequiredRule.ALWAYS_REQUIRED, null)))),
            null, null, null, null, null);

    ss.updateDraft(cmd);

    assertEquals(1, ss.getSections().size());
    assertEquals("General", ss.getSections().get(0).getTitle());
    assertEquals(1, ss.getSections().get(0).getItems().size());
}

@Test
void updateDraft_removeSection_shouldSucceed() {
    ScoreSheetDefinition ss = createSheetWithOneItem();
    UpdateDraftCommand cmd = new UpdateDraftCommand(
            null, null, List.of("sec1"), null, null, null);

    ss.updateDraft(cmd);

    assertTrue(ss.getSections().isEmpty());
}

@Test
void updateDraft_removeNonexistentSection_shouldThrow() {
    ScoreSheetDefinition ss = createSheetWithOneItem();
    UpdateDraftCommand cmd = new UpdateDraftCommand(
            null, null, List.of("nonexistent"), null, null, null);

    assertThrows(DomainRuleViolationException.class, () -> ss.updateDraft(cmd));
}

@Test
void updateDraft_updateSectionTitle_shouldSucceed() {
    ScoreSheetDefinition ss = createSheetWithOneItem();
    UpdateDraftCommand cmd = new UpdateDraftCommand(
            null,
            List.of(new UpdateDraftCommand.UpdateSectionCommand("sec1", "Updated Title")),
            null, null, null, null);

    ss.updateDraft(cmd);

    assertEquals("Updated Title", ss.getSections().get(0).getTitle());
}

@Test
void updateDraft_addItemToExistingSection_shouldSucceed() {
    ScoreSheetDefinition ss = createSheetWithOneItem();
    UpdateDraftCommand cmd = new UpdateDraftCommand(
            null, null, null,
            List.of(new UpdateDraftCommand.AddItemCommand("sec1", "i2", "C02", "Item2",
                    ScoreType.BOOLEAN, BigDecimal.ONE, BigDecimal.ONE,
                    RequiredRule.OPTIONAL, null)),
            null, null);

    ss.updateDraft(cmd);

    assertEquals(2, ss.getSections().get(0).getItems().size());
}

@Test
void updateDraft_removeItem_shouldSucceed() {
    ScoreSheetDefinition ss = createSheetWithOneItem();
    UpdateDraftCommand cmd = new UpdateDraftCommand(
            null, null, null, null, null, List.of("i1"));

    ss.updateDraft(cmd);

    assertTrue(ss.getSections().get(0).getItems().isEmpty());
}

@Test
void updateDraft_removeNonexistentItem_shouldThrow() {
    ScoreSheetDefinition ss = createSheetWithOneItem();
    UpdateDraftCommand cmd = new UpdateDraftCommand(
            null, null, null, null, null, List.of("nonexistent"));

    assertThrows(DomainRuleViolationException.class, () -> ss.updateDraft(cmd));
}

@Test
void updateDraft_updateItem_shouldSucceed() {
    ScoreSheetDefinition ss = createSheetWithOneItem();
    UpdateDraftCommand cmd = new UpdateDraftCommand(
            null, null, null, null,
            List.of(new UpdateDraftCommand.UpdateItemCommand("i1", "C01", "Updated Item",
                    ScoreType.RANGE, BigDecimal.valueOf(20), BigDecimal.ONE,
                    RequiredRule.OPTIONAL, null)),
            null);

    ss.updateDraft(cmd);

    ScoreItem updated = ss.getSections().get(0).getItems().get(0);
    assertEquals("Updated Item", updated.getTitle());
    assertEquals(ScoreType.RANGE, updated.getScoreType());
    assertEquals(RequiredRule.OPTIONAL, updated.getRequiredRule());
}

@Test
void updateDraft_addItemCrossSectionDuplicateCode_shouldThrow() {
    ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft("ss1", "p1");
    ScoreSheetSection s1 = new ScoreSheetSection("sec1", "Section 1");
    s1.addItem(new ScoreItem("i1", "C01", "Item1",
            ScoreType.FIXED, BigDecimal.TEN, BigDecimal.ONE));
    ss.addSection(s1);
    ss.addSection(new ScoreSheetSection("sec2", "Section 2"));

    UpdateDraftCommand cmd = new UpdateDraftCommand(
            null, null, null,
            List.of(new UpdateDraftCommand.AddItemCommand("sec2", "i2", "C01", "Dup Code",
                    ScoreType.FIXED, BigDecimal.TEN, BigDecimal.ONE,
                    RequiredRule.ALWAYS_REQUIRED, null)),
            null, null);

    assertThrows(DomainStateConflictException.class, () -> ss.updateDraft(cmd));
}

@Test
void updateDraft_onPublishedSheet_shouldThrow() {
    ScoreSheetDefinition ss = createSheetWithOneItem();
    ss.publishVersion();

    UpdateDraftCommand cmd = new UpdateDraftCommand(
            null, null, null, null, null, null);

    assertThrows(DomainStateConflictException.class, () -> ss.updateDraft(cmd));
}
```

Add import: `import java.util.List;`

- [ ] **Step 3: Add section mutation tests**

```java
@Test
void section_updateTitle_shouldSucceed() {
    ScoreSheetSection section = new ScoreSheetSection("sec1", "Old");
    section.updateTitle("New Title");
    assertEquals("New Title", section.getTitle());
}

@Test
void section_updateTitle_null_shouldThrow() {
    ScoreSheetSection section = new ScoreSheetSection("sec1", "Old");
    assertThrows(NullPointerException.class, () -> section.updateTitle(null));
}

@Test
void section_removeItem_shouldSucceed() {
    ScoreSheetSection section = new ScoreSheetSection("sec1", "Test");
    section.addItem(new ScoreItem("i1", "C01", "Item1",
            ScoreType.FIXED, BigDecimal.TEN, BigDecimal.ONE));

    section.removeItem("i1");

    assertTrue(section.getItems().isEmpty());
}

@Test
void section_removeItem_notFound_shouldThrow() {
    ScoreSheetSection section = new ScoreSheetSection("sec1", "Test");
    assertThrows(DomainRuleViolationException.class,
            () -> section.removeItem("nonexistent"));
}
```

- [ ] **Step 4: Run all tests**

Run: `cd services/quality-inspection-service && .\gradlew clean test`
Expected: All tests PASS (147 old + ~20 new ≈ 167).

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "test(b1b): add domain tests for updateDraft, requiredRule, section mutations

- requiredRule validation: 6 tests (8-param, conditional, null, backward-compat)
- updateDraft: 10 tests (add/remove/update sections/items, cross-section uniqueness, published guard)
- section mutations: 4 tests (updateTitle, removeItem, error cases)"
```

---

## Task 4: Infrastructure — JPA Mapping Update for requiredRule

**Files:**
- Modify: `infrastructure/persistence/scoresheet/ScoreItemEntity.java`
- Modify: `infrastructure/persistence/scoresheet/ScoreSheetRepositoryImpl.java`

- [ ] **Step 1: Add requiredRule columns to ScoreItemEntity**

Modify `ScoreItemEntity.java`, add these fields after `weight`:

```java
@Column(name = "required_rule", nullable = false)
private String requiredRule;

@Column(name = "condition_expression")
private String conditionExpression;
```

Add getters/setters:

```java
public String getRequiredRule() { return requiredRule; }
public void setRequiredRule(String requiredRule) { this.requiredRule = requiredRule; }

public String getConditionExpression() { return conditionExpression; }
public void setConditionExpression(String conditionExpression) { this.conditionExpression = conditionExpression; }
```

- [ ] **Step 2: Update ScoreSheetRepositoryImpl toItemEntity()**

In `ScoreSheetRepositoryImpl.java`, update `toItemEntity()`:

```java
private ScoreItemEntity toItemEntity(ScoreItem item) {
    ScoreItemEntity ie = new ScoreItemEntity();
    ie.setItemId(item.getItemId());
    ie.setCode(item.getCode());
    ie.setTitle(item.getTitle());
    ie.setScoreType(item.getScoreType().name());
    ie.setMaxScore(item.getMaxScore());
    ie.setWeight(item.getWeight());
    ie.setRequiredRule(item.getRequiredRule().name());
    ie.setConditionExpression(item.getConditionExpression());
    return ie;
}
```

- [ ] **Step 3: Update ScoreSheetRepositoryImpl toDomain() section**

In the `toDomain()` method, update the `ScoreItem` construction inside the section loop:

```java
section.addItem(new ScoreItem(
        ie.getItemId(), ie.getCode(), ie.getTitle(),
        ScoreType.valueOf(ie.getScoreType()),
        ie.getMaxScore(), ie.getWeight(),
        RequiredRule.valueOf(ie.getRequiredRule()),
        ie.getConditionExpression()
));
```

Add import: `import com.tzuchi.his.quality.inspection.domain.scoresheet.RequiredRule;`

- [ ] **Step 4: Run tests**

Run: `cd services/quality-inspection-service && .\gradlew clean test`
Expected: All tests PASS. The existing persistence tests use 6-param ScoreItem which defaults to ALWAYS_REQUIRED.

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "feat(b1b): update JPA mapping for requiredRule fields

- ScoreItemEntity: add required_rule (not null) + condition_expression columns
- ScoreSheetRepositoryImpl: update toEntity/toDomain to map requiredRule + conditionExpression"
```

---

## Task 5: Application — HospitalIdResolver Port + Modify CreateDraft/Publish Use Cases

**Files:**
- Create: `application/common/HospitalIdResolver.java`
- Create: `infrastructure/adapter/HospitalIdResolverImpl.java`
- Create: `domain/scoresheet/event/ScoreSheetDraftCreatedEvent.java`
- Create: `domain/scoresheet/event/ScoreSheetVersionPublishedEvent.java`
- Modify: `application/scoresheet/CreateScoreSheetDraftUseCase.java`
- Modify: `application/scoresheet/PublishScoreSheetVersionUseCase.java`

- [ ] **Step 1: Create HospitalIdResolver port**

Create `services/quality-inspection-service/src/main/java/com/tzuchi/his/quality/inspection/application/common/HospitalIdResolver.java`:

```java
package com.tzuchi.his.quality.inspection.application.common;

/**
 * Resolves hospitalId via cross-aggregate relationships.
 * Implementations are in the infrastructure layer.
 */
public interface HospitalIdResolver {
    /**
     * Resolves the hospitalId for a given projectId.
     * @throws ResourceNotFoundException if the project does not exist
     */
    String resolveByProjectId(String projectId);
}
```

- [ ] **Step 2: Create HospitalIdResolverImpl**

Create `services/quality-inspection-service/src/main/java/com/tzuchi/his/quality/inspection/infrastructure/adapter/HospitalIdResolverImpl.java`:

```java
package com.tzuchi.his.quality.inspection.infrastructure.adapter;

import com.tzuchi.his.quality.inspection.application.common.HospitalIdResolver;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProjectRepository;
import org.springframework.stereotype.Component;

@Component
public class HospitalIdResolverImpl implements HospitalIdResolver {

    private final ReviewProjectRepository projectRepository;

    public HospitalIdResolverImpl(ReviewProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Override
    public String resolveByProjectId(String projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("PROJECT_NOT_FOUND",
                        "ReviewProject not found: " + projectId))
                .getHospitalId();
    }
}
```

- [ ] **Step 3: Create event directory and ScoreSheetDraftCreatedEvent**

Create directory `domain/scoresheet/event/` and file:

`services/quality-inspection-service/src/main/java/com/tzuchi/his/quality/inspection/domain/scoresheet/event/ScoreSheetDraftCreatedEvent.java`:

```java
package com.tzuchi.his.quality.inspection.domain.scoresheet.event;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

public class ScoreSheetDraftCreatedEvent extends DomainEvent {
    public ScoreSheetDraftCreatedEvent(Object source, ActorContext actor,
                                        String scoreSheetId, String projectId,
                                        String hospitalId) {
        super(source, actor.getUserId(), actor.getUserName(),
              hospitalId, "ScoreSheetDefinition", scoreSheetId, "DRAFT_CREATED",
              "{\"projectId\":\"" + projectId + "\"}");
    }
}
```

- [ ] **Step 4: Create ScoreSheetVersionPublishedEvent**

Create `services/quality-inspection-service/src/main/java/com/tzuchi/his/quality/inspection/domain/scoresheet/event/ScoreSheetVersionPublishedEvent.java`:

```java
package com.tzuchi.his.quality.inspection.domain.scoresheet.event;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

public class ScoreSheetVersionPublishedEvent extends DomainEvent {
    public ScoreSheetVersionPublishedEvent(Object source, ActorContext actor,
                                            String scoreSheetId, String projectId,
                                            String hospitalId) {
        super(source, actor.getUserId(), actor.getUserName(),
              hospitalId, "ScoreSheetDefinition", scoreSheetId, "VERSION_PUBLISHED",
              "{\"projectId\":\"" + projectId + "\"}");
    }
}
```

- [ ] **Step 5: Modify CreateScoreSheetDraftUseCase**

Replace the entire content of `application/scoresheet/CreateScoreSheetDraftUseCase.java`:

```java
package com.tzuchi.his.quality.inspection.application.scoresheet;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.HospitalIdResolver;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetDefinition;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetRepository;
import com.tzuchi.his.quality.inspection.domain.scoresheet.event.ScoreSheetDraftCreatedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class CreateScoreSheetDraftUseCase {

    private final ScoreSheetRepository repository;
    private final HospitalIdResolver hospitalIdResolver;
    private final DomainEventPublisher eventPublisher;

    public CreateScoreSheetDraftUseCase(ScoreSheetRepository repository,
                                         HospitalIdResolver hospitalIdResolver,
                                         DomainEventPublisher eventPublisher) {
        this.repository = repository;
        this.hospitalIdResolver = hospitalIdResolver;
        this.eventPublisher = eventPublisher;
    }

    public String execute(ActorContext actor, String projectId) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        String hospitalId = hospitalIdResolver.resolveByProjectId(projectId);
        actor.requireHospitalScope(hospitalId);

        String scoreSheetId = UUID.randomUUID().toString();
        ScoreSheetDefinition scoreSheet = ScoreSheetDefinition.createDraft(scoreSheetId, projectId);
        repository.save(scoreSheet);
        eventPublisher.publish(new ScoreSheetDraftCreatedEvent(this, actor, scoreSheetId,
                projectId, hospitalId));
        return scoreSheetId;
    }
}
```

- [ ] **Step 6: Modify PublishScoreSheetVersionUseCase**

Replace the entire content of `application/scoresheet/PublishScoreSheetVersionUseCase.java`:

```java
package com.tzuchi.his.quality.inspection.application.scoresheet;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.HospitalIdResolver;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetDefinition;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetRepository;
import com.tzuchi.his.quality.inspection.domain.scoresheet.event.ScoreSheetVersionPublishedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PublishScoreSheetVersionUseCase {

    private final ScoreSheetRepository repository;
    private final HospitalIdResolver hospitalIdResolver;
    private final DomainEventPublisher eventPublisher;

    public PublishScoreSheetVersionUseCase(ScoreSheetRepository repository,
                                            HospitalIdResolver hospitalIdResolver,
                                            DomainEventPublisher eventPublisher) {
        this.repository = repository;
        this.hospitalIdResolver = hospitalIdResolver;
        this.eventPublisher = eventPublisher;
    }

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

- [ ] **Step 7: Compile and run tests**

Run: `cd services/quality-inspection-service && .\gradlew clean test`
Expected: All tests PASS. Existing tests may need minor adjustments if they directly instantiate CreateScoreSheetDraftUseCase (they only had 1 dependency before, now have 3).

Note: If any existing test directly constructs `CreateScoreSheetDraftUseCase(repository)` or `PublishScoreSheetVersionUseCase(repository)`, those tests will fail because the constructor signature changed. Check and fix any such tests by adding stub HospitalIdResolver and CapturingDomainEventPublisher.

- [ ] **Step 8: Commit**

```bash
git add -A && git commit -m "feat(b1b): add HospitalIdResolver port, update CreateDraft/Publish with scope guard + events

- HospitalIdResolver port in application/common, impl in infrastructure/adapter
- CreateScoreSheetDraftUseCase: add scope guard + ScoreSheetDraftCreatedEvent
- PublishScoreSheetVersionUseCase: add scope guard + ScoreSheetVersionPublishedEvent + 404 fix
- Two new domain events in domain/scoresheet/event/"
```

---

## Task 6: Application — UpdateScoreSheetDraftUseCase + ScoreSheetDraftUpdatedEvent

**Files:**
- Create: `domain/scoresheet/event/ScoreSheetDraftUpdatedEvent.java`
- Create: `application/scoresheet/UpdateScoreSheetDraftUseCase.java`

- [ ] **Step 1: Create ScoreSheetDraftUpdatedEvent**

Create `services/quality-inspection-service/src/main/java/com/tzuchi/his/quality/inspection/domain/scoresheet/event/ScoreSheetDraftUpdatedEvent.java`:

```java
package com.tzuchi.his.quality.inspection.domain.scoresheet.event;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

public class ScoreSheetDraftUpdatedEvent extends DomainEvent {
    public ScoreSheetDraftUpdatedEvent(Object source, ActorContext actor,
                                        String scoreSheetId, String projectId,
                                        String hospitalId) {
        super(source, actor.getUserId(), actor.getUserName(),
              hospitalId, "ScoreSheetDefinition", scoreSheetId, "DRAFT_UPDATED",
              "{\"projectId\":\"" + projectId + "\"}");
    }
}
```

- [ ] **Step 2: Create UpdateScoreSheetDraftUseCase**

Create `services/quality-inspection-service/src/main/java/com/tzuchi/his/quality/inspection/application/scoresheet/UpdateScoreSheetDraftUseCase.java`:

```java
package com.tzuchi.his.quality.inspection.application.scoresheet;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.HospitalIdResolver;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetDefinition;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetRepository;
import com.tzuchi.his.quality.inspection.domain.scoresheet.UpdateDraftCommand;
import com.tzuchi.his.quality.inspection.domain.scoresheet.event.ScoreSheetDraftUpdatedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdateScoreSheetDraftUseCase {

    private final ScoreSheetRepository repository;
    private final HospitalIdResolver hospitalIdResolver;
    private final DomainEventPublisher eventPublisher;

    public UpdateScoreSheetDraftUseCase(ScoreSheetRepository repository,
                                         HospitalIdResolver hospitalIdResolver,
                                         DomainEventPublisher eventPublisher) {
        this.repository = repository;
        this.hospitalIdResolver = hospitalIdResolver;
        this.eventPublisher = eventPublisher;
    }

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

- [ ] **Step 3: Compile and run tests**

Run: `cd services/quality-inspection-service && .\gradlew clean test`
Expected: All tests PASS.

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "feat(b1b): add UpdateScoreSheetDraftUseCase + ScoreSheetDraftUpdatedEvent

- Action-based update: delegates to ScoreSheetDefinition.updateDraft(command)
- HM only with scope guard via HospitalIdResolver
- Publishes ScoreSheetDraftUpdatedEvent on success"
```

---

## Task 7: Application — Query Use Cases + ScoreSheetQueryPort + Read Models

**Files:**
- Create: `application/scoresheet/ScoreSheetQueryPort.java`
- Create: `application/scoresheet/ScoreSheetSummary.java`
- Create: `application/scoresheet/ScoreSheetVersionInfo.java`
- Create: `application/scoresheet/ListScoreSheetSummariesUseCase.java`
- Create: `application/scoresheet/GetScoreSheetDetailUseCase.java`
- Create: `application/scoresheet/GetScoreSheetVersionUseCase.java`
- Create: `application/scoresheet/GetScoreSheetSectionsUseCase.java`

- [ ] **Step 1: Create ScoreSheetSummary read model**

Create `services/quality-inspection-service/src/main/java/com/tzuchi/his/quality/inspection/application/scoresheet/ScoreSheetSummary.java`:

```java
package com.tzuchi.his.quality.inspection.application.scoresheet;

import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetStatus;

public class ScoreSheetSummary {

    private final String scoreSheetId;
    private final String projectId;
    private final ScoreSheetStatus status;
    private final int version;
    private final int sectionCount;
    private final int itemCount;

    public ScoreSheetSummary(String scoreSheetId, String projectId, ScoreSheetStatus status,
                              int version, int sectionCount, int itemCount) {
        this.scoreSheetId = scoreSheetId;
        this.projectId = projectId;
        this.status = status;
        this.version = version;
        this.sectionCount = sectionCount;
        this.itemCount = itemCount;
    }

    public String getScoreSheetId() { return scoreSheetId; }
    public String getProjectId() { return projectId; }
    public ScoreSheetStatus getStatus() { return status; }
    public int getVersion() { return version; }
    public int getSectionCount() { return sectionCount; }
    public int getItemCount() { return itemCount; }
}
```

- [ ] **Step 2: Create ScoreSheetVersionInfo value object**

Create `services/quality-inspection-service/src/main/java/com/tzuchi/his/quality/inspection/application/scoresheet/ScoreSheetVersionInfo.java`:

```java
package com.tzuchi.his.quality.inspection.application.scoresheet;

public class ScoreSheetVersionInfo {

    private final int version;
    private final boolean published;

    public ScoreSheetVersionInfo(int version, boolean published) {
        this.version = version;
        this.published = published;
    }

    public int getVersion() { return version; }
    public boolean isPublished() { return published; }
}
```

- [ ] **Step 3: Create ScoreSheetQueryPort**

Create `services/quality-inspection-service/src/main/java/com/tzuchi/his/quality/inspection/application/scoresheet/ScoreSheetQueryPort.java`:

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

- [ ] **Step 4: Create ListScoreSheetSummariesUseCase**

Create `services/quality-inspection-service/src/main/java/com/tzuchi/his/quality/inspection/application/scoresheet/ListScoreSheetSummariesUseCase.java`:

```java
package com.tzuchi.his.quality.inspection.application.scoresheet;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ListScoreSheetSummariesUseCase {

    private final ScoreSheetQueryPort queryPort;

    public ListScoreSheetSummariesUseCase(ScoreSheetQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    public List<ScoreSheetSummary> execute(ActorContext actor) {
        if (actor.hasRole(DomainRole.SYSTEM_ADMIN)) {
            return queryPort.findAll();
        }
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        return queryPort.findByHospitalId(actor.requireHospitalId());
    }
}
```

- [ ] **Step 5: Create GetScoreSheetDetailUseCase**

Create `services/quality-inspection-service/src/main/java/com/tzuchi/his/quality/inspection/application/scoresheet/GetScoreSheetDetailUseCase.java`:

```java
package com.tzuchi.his.quality.inspection.application.scoresheet;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.HospitalIdResolver;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetScoreSheetDetailUseCase {

    private final ScoreSheetQueryPort queryPort;
    private final HospitalIdResolver hospitalIdResolver;

    public GetScoreSheetDetailUseCase(ScoreSheetQueryPort queryPort,
                                       HospitalIdResolver hospitalIdResolver) {
        this.queryPort = queryPort;
        this.hospitalIdResolver = hospitalIdResolver;
    }

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

- [ ] **Step 6: Create GetScoreSheetVersionUseCase**

Create `services/quality-inspection-service/src/main/java/com/tzuchi/his/quality/inspection/application/scoresheet/GetScoreSheetVersionUseCase.java`:

```java
package com.tzuchi.his.quality.inspection.application.scoresheet;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.HospitalIdResolver;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetDefinition;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetRepository;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetScoreSheetVersionUseCase {

    private final ScoreSheetRepository repository;
    private final HospitalIdResolver hospitalIdResolver;

    public GetScoreSheetVersionUseCase(ScoreSheetRepository repository,
                                        HospitalIdResolver hospitalIdResolver) {
        this.repository = repository;
        this.hospitalIdResolver = hospitalIdResolver;
    }

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

- [ ] **Step 7: Create GetScoreSheetSectionsUseCase (HM only)**

Create `services/quality-inspection-service/src/main/java/com/tzuchi/his/quality/inspection/application/scoresheet/GetScoreSheetSectionsUseCase.java`:

```java
package com.tzuchi.his.quality.inspection.application.scoresheet;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.HospitalIdResolver;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetDefinition;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetRepository;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetSection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class GetScoreSheetSectionsUseCase {

    private final ScoreSheetRepository repository;
    private final HospitalIdResolver hospitalIdResolver;

    public GetScoreSheetSectionsUseCase(ScoreSheetRepository repository,
                                         HospitalIdResolver hospitalIdResolver) {
        this.repository = repository;
        this.hospitalIdResolver = hospitalIdResolver;
    }

    public List<ScoreSheetSection> execute(ActorContext actor, String scoreSheetId) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        ScoreSheetDefinition scoreSheet = repository.findById(scoreSheetId)
                .orElseThrow(() -> new ResourceNotFoundException("SCORE_SHEET_NOT_FOUND",
                        "ScoreSheet not found: " + scoreSheetId));
        String hospitalId = hospitalIdResolver.resolveByProjectId(scoreSheet.getProjectId());
        actor.requireHospitalScope(hospitalId);
        return scoreSheet.getSections();
    }
}
```

- [ ] **Step 8: Compile check**

Run: `cd services/quality-inspection-service && .\gradlew compileJava`
Expected: BUILD SUCCESSFUL (ScoreSheetQueryPort has no impl yet — compile will succeed because Spring wiring is runtime).

- [ ] **Step 9: Commit**

```bash
git add -A && git commit -m "feat(b1b): add 4 query use cases + ScoreSheetQueryPort + read models

- ScoreSheetQueryPort: findByHospitalId, findAll, findSummaryById
- ScoreSheetSummary: read model with sectionCount + itemCount
- ScoreSheetVersionInfo: simple value object
- ListScoreSheetSummaries: HM (own hospital) + SA (all)
- GetScoreSheetDetail: HM (scope guard) + SA (no scope)
- GetScoreSheetVersion: HM (scope guard) + SA (no scope)
- GetScoreSheetSections: HM only (SA cannot access)"
```

---

## Task 8: Infrastructure — ScoreSheetQueryAdapter + InMemoryScoreSheetRepository

**Files:**
- Create: `infrastructure/persistence/scoresheet/ScoreSheetQueryAdapter.java`
- Create: `testfixture/InMemoryScoreSheetRepository.java`

- [ ] **Step 1: Add findByProjectId to ScoreSheetJpaRepository**

Modify `infrastructure/persistence/scoresheet/ScoreSheetJpaRepository.java` — add a query method:

```java
import java.util.List;

// add this method to the interface:
List<ScoreSheetEntity> findByProjectIdIn(List<String> projectIds);
```

- [ ] **Step 2: Add findByHospitalId to ReviewProjectJpaRepository**

Check if `ReviewProjectJpaRepository` already has a `findByHospitalId` method. If not, add:

```java
List<ReviewProjectEntity> findByHospitalId(String hospitalId);
```

(This may already exist from B1a. If so, skip this step.)

- [ ] **Step 3: Create ScoreSheetQueryAdapter**

Create `services/quality-inspection-service/src/main/java/com/tzuchi/his/quality/inspection/infrastructure/persistence/scoresheet/ScoreSheetQueryAdapter.java`:

```java
package com.tzuchi.his.quality.inspection.infrastructure.persistence.scoresheet;

import com.tzuchi.his.quality.inspection.application.scoresheet.ScoreSheetQueryPort;
import com.tzuchi.his.quality.inspection.application.scoresheet.ScoreSheetSummary;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetStatus;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.project.ReviewProjectEntity;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.project.ReviewProjectJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ScoreSheetQueryAdapter implements ScoreSheetQueryPort {

    private final ScoreSheetJpaRepository scoreSheetJpa;
    private final ReviewProjectJpaRepository projectJpa;

    public ScoreSheetQueryAdapter(ScoreSheetJpaRepository scoreSheetJpa,
                                   ReviewProjectJpaRepository projectJpa) {
        this.scoreSheetJpa = scoreSheetJpa;
        this.projectJpa = projectJpa;
    }

    @Override
    public List<ScoreSheetSummary> findByHospitalId(String hospitalId) {
        List<String> projectIds = projectJpa.findByHospitalId(hospitalId).stream()
                .map(ReviewProjectEntity::getProjectId)
                .collect(Collectors.toList());
        if (projectIds.isEmpty()) {
            return List.of();
        }
        return scoreSheetJpa.findByProjectIdIn(projectIds).stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    @Override
    public List<ScoreSheetSummary> findAll() {
        return scoreSheetJpa.findAll().stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ScoreSheetSummary> findSummaryById(String scoreSheetId) {
        return scoreSheetJpa.findById(scoreSheetId).map(this::toSummary);
    }

    private ScoreSheetSummary toSummary(ScoreSheetEntity entity) {
        int sectionCount = entity.getSections().size();
        int itemCount = entity.getSections().stream()
                .mapToInt(s -> s.getItems().size())
                .sum();
        return new ScoreSheetSummary(
                entity.getScoreSheetId(),
                entity.getProjectId(),
                entity.getStatus(),
                entity.getVersion(),
                sectionCount,
                itemCount
        );
    }
}
```

- [ ] **Step 4: Create InMemoryScoreSheetRepository**

Create `services/quality-inspection-service/src/test/java/com/tzuchi/his/quality/inspection/testfixture/InMemoryScoreSheetRepository.java`:

```java
package com.tzuchi.his.quality.inspection.testfixture;

import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetDefinition;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetRepository;

import java.util.*;

public class InMemoryScoreSheetRepository implements ScoreSheetRepository {
    public final Map<String, ScoreSheetDefinition> store = new LinkedHashMap<>();

    @Override
    public void save(ScoreSheetDefinition scoreSheet) {
        store.put(scoreSheet.getScoreSheetId(), scoreSheet);
    }

    @Override
    public Optional<ScoreSheetDefinition> findById(String scoreSheetId) {
        return Optional.ofNullable(store.get(scoreSheetId));
    }

    public void clear() { store.clear(); }
    public int size() { return store.size(); }
    public Collection<ScoreSheetDefinition> getAll() { return store.values(); }
}
```

- [ ] **Step 5: Run tests**

Run: `cd services/quality-inspection-service && .\gradlew clean test`
Expected: All tests PASS.

- [ ] **Step 6: Commit**

```bash
git add -A && git commit -m "feat(b1b): add ScoreSheetQueryAdapter + InMemoryScoreSheetRepository

- ScoreSheetQueryAdapter: implements ScoreSheetQueryPort via JPA
- findByHospitalId: joins through ReviewProject to filter by hospital
- InMemoryScoreSheetRepository: test double in testfixture package"
```

---

## Task 9: Application Tests — All ScoreSheet Use Cases

**Files:**
- Create: `application/scoresheet/CreateScoreSheetDraftUseCaseTest.java` (test)
- Create: `application/scoresheet/PublishScoreSheetVersionUseCaseTest.java` (test)
- Create: `application/scoresheet/UpdateScoreSheetDraftUseCaseTest.java` (test)
- Create: `application/scoresheet/ListScoreSheetSummariesUseCaseTest.java` (test)
- Create: `application/scoresheet/GetScoreSheetDetailUseCaseTest.java` (test)
- Create: `application/scoresheet/GetScoreSheetVersionUseCaseTest.java` (test)
- Create: `application/scoresheet/GetScoreSheetSectionsUseCaseTest.java` (test)

Note: All command use case tests use `InMemoryScoreSheetRepository` + `CapturingDomainEventPublisher` + a stub `HospitalIdResolver`. Query use case tests use a stub `ScoreSheetQueryPort` (anonymous inner class or simple in-memory implementation).

The stub HospitalIdResolver should be a simple lambda or inner class:

```java
private static HospitalIdResolver stubResolver(String hospitalId) {
    return projectId -> hospitalId;
}
```

- [ ] **Step 1: Create CreateScoreSheetDraftUseCaseTest**

Create `services/quality-inspection-service/src/test/java/com/tzuchi/his/quality/inspection/application/scoresheet/CreateScoreSheetDraftUseCaseTest.java`:

```java
package com.tzuchi.his.quality.inspection.application.scoresheet;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.HospitalIdResolver;
import com.tzuchi.his.quality.inspection.application.common.UnauthorizedException;
import com.tzuchi.his.quality.inspection.domain.scoresheet.event.ScoreSheetDraftCreatedEvent;
import com.tzuchi.his.quality.inspection.testfixture.CapturingDomainEventPublisher;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryScoreSheetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CreateScoreSheetDraftUseCaseTest {

    private InMemoryScoreSheetRepository repository;
    private CapturingDomainEventPublisher eventPublisher;
    private CreateScoreSheetDraftUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryScoreSheetRepository();
        eventPublisher = new CapturingDomainEventPublisher();
        HospitalIdResolver resolver = projectId -> "H001";
        useCase = new CreateScoreSheetDraftUseCase(repository, resolver, eventPublisher);
    }

    @Test
    void execute_hm_shouldCreateDraft() {
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        String id = useCase.execute(actor, "project1");

        assertNotNull(id);
        assertEquals(1, repository.size());
    }

    @Test
    void execute_shouldPublishEvent() {
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        useCase.execute(actor, "project1");

        assertEquals(1, eventPublisher.events.size());
        assertInstanceOf(ScoreSheetDraftCreatedEvent.class, eventPublisher.events.get(0));
    }

    @Test
    void execute_wrongRole_shouldThrow() {
        ActorContext actor = new ActorContext("u1", "Admin",
                EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.of("H001"));

        assertThrows(UnauthorizedException.class, () -> useCase.execute(actor, "project1"));
    }

    @Test
    void execute_wrongHospital_shouldThrow() {
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H999"));

        assertThrows(AccessDeniedException.class, () -> useCase.execute(actor, "project1"));
    }
}
```

- [ ] **Step 2: Create PublishScoreSheetVersionUseCaseTest**

Create `services/quality-inspection-service/src/test/java/com/tzuchi/his/quality/inspection/application/scoresheet/PublishScoreSheetVersionUseCaseTest.java`:

```java
package com.tzuchi.his.quality.inspection.application.scoresheet;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.HospitalIdResolver;
import com.tzuchi.his.quality.inspection.application.common.UnauthorizedException;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.scoresheet.*;
import com.tzuchi.his.quality.inspection.domain.scoresheet.event.ScoreSheetVersionPublishedEvent;
import com.tzuchi.his.quality.inspection.testfixture.CapturingDomainEventPublisher;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryScoreSheetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PublishScoreSheetVersionUseCaseTest {

    private InMemoryScoreSheetRepository repository;
    private CapturingDomainEventPublisher eventPublisher;
    private PublishScoreSheetVersionUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryScoreSheetRepository();
        eventPublisher = new CapturingDomainEventPublisher();
        HospitalIdResolver resolver = projectId -> "H001";
        useCase = new PublishScoreSheetVersionUseCase(repository, resolver, eventPublisher);
    }

    @Test
    void execute_shouldPublish() {
        ScoreSheetDefinition ss = createDraftWithItem("ss1", "p1");
        repository.save(ss);
        ActorContext actor = hmActor("H001");

        useCase.execute(actor, "ss1");

        assertEquals(ScoreSheetStatus.PUBLISHED, repository.store.get("ss1").getStatus());
        assertEquals(1, eventPublisher.events.size());
        assertInstanceOf(ScoreSheetVersionPublishedEvent.class, eventPublisher.events.get(0));
    }

    @Test
    void execute_notFound_shouldThrow404() {
        ActorContext actor = hmActor("H001");
        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(actor, "nonexistent"));
    }

    @Test
    void execute_wrongRole_shouldThrow() {
        ScoreSheetDefinition ss = createDraftWithItem("ss1", "p1");
        repository.save(ss);
        ActorContext actor = new ActorContext("u1", "Admin",
                EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.of("H001"));

        assertThrows(UnauthorizedException.class, () -> useCase.execute(actor, "ss1"));
    }

    @Test
    void execute_wrongHospital_shouldThrow() {
        ScoreSheetDefinition ss = createDraftWithItem("ss1", "p1");
        repository.save(ss);
        ActorContext actor = hmActor("H999");

        assertThrows(AccessDeniedException.class, () -> useCase.execute(actor, "ss1"));
    }

    private ScoreSheetDefinition createDraftWithItem(String id, String projectId) {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft(id, projectId);
        ScoreSheetSection section = new ScoreSheetSection("sec1", "General");
        section.addItem(new ScoreItem("i1", "C01", "Item1",
                ScoreType.FIXED, BigDecimal.TEN, BigDecimal.ONE));
        ss.addSection(section);
        return ss;
    }

    private ActorContext hmActor(String hospitalId) {
        return new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of(hospitalId));
    }
}
```

- [ ] **Step 3: Create UpdateScoreSheetDraftUseCaseTest**

Create `services/quality-inspection-service/src/test/java/com/tzuchi/his/quality/inspection/application/scoresheet/UpdateScoreSheetDraftUseCaseTest.java`:

```java
package com.tzuchi.his.quality.inspection.application.scoresheet;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.HospitalIdResolver;
import com.tzuchi.his.quality.inspection.application.common.UnauthorizedException;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.scoresheet.*;
import com.tzuchi.his.quality.inspection.domain.scoresheet.event.ScoreSheetDraftUpdatedEvent;
import com.tzuchi.his.quality.inspection.testfixture.CapturingDomainEventPublisher;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryScoreSheetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UpdateScoreSheetDraftUseCaseTest {

    private InMemoryScoreSheetRepository repository;
    private CapturingDomainEventPublisher eventPublisher;
    private UpdateScoreSheetDraftUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryScoreSheetRepository();
        eventPublisher = new CapturingDomainEventPublisher();
        HospitalIdResolver resolver = projectId -> "H001";
        useCase = new UpdateScoreSheetDraftUseCase(repository, resolver, eventPublisher);
    }

    @Test
    void execute_addSection_shouldSucceed() {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft("ss1", "p1");
        repository.save(ss);
        ActorContext actor = hmActor("H001");

        UpdateDraftCommand cmd = new UpdateDraftCommand(
                List.of(new UpdateDraftCommand.AddSectionCommand("sec1", "General",
                        List.of(new UpdateDraftCommand.AddItemCommand("sec1", "i1", "C01", "Item1",
                                ScoreType.FIXED, BigDecimal.TEN, BigDecimal.ONE,
                                RequiredRule.ALWAYS_REQUIRED, null)))),
                null, null, null, null, null);

        useCase.execute(actor, "ss1", cmd);

        ScoreSheetDefinition saved = repository.store.get("ss1");
        assertEquals(1, saved.getSections().size());
        assertEquals(1, eventPublisher.events.size());
        assertInstanceOf(ScoreSheetDraftUpdatedEvent.class, eventPublisher.events.get(0));
    }

    @Test
    void execute_notFound_shouldThrow404() {
        ActorContext actor = hmActor("H001");
        UpdateDraftCommand cmd = new UpdateDraftCommand(null, null, null, null, null, null);

        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(actor, "nonexistent", cmd));
    }

    @Test
    void execute_wrongRole_shouldThrow() {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft("ss1", "p1");
        repository.save(ss);
        ActorContext actor = new ActorContext("u1", "Admin",
                EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.of("H001"));
        UpdateDraftCommand cmd = new UpdateDraftCommand(null, null, null, null, null, null);

        assertThrows(UnauthorizedException.class,
                () -> useCase.execute(actor, "ss1", cmd));
    }

    @Test
    void execute_wrongHospital_shouldThrow() {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft("ss1", "p1");
        repository.save(ss);
        ActorContext actor = hmActor("H999");
        UpdateDraftCommand cmd = new UpdateDraftCommand(null, null, null, null, null, null);

        assertThrows(AccessDeniedException.class,
                () -> useCase.execute(actor, "ss1", cmd));
    }

    private ActorContext hmActor(String hospitalId) {
        return new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of(hospitalId));
    }
}
```

- [ ] **Step 4: Create ListScoreSheetSummariesUseCaseTest**

Create `services/quality-inspection-service/src/test/java/com/tzuchi/his/quality/inspection/application/scoresheet/ListScoreSheetSummariesUseCaseTest.java`:

```java
package com.tzuchi.his.quality.inspection.application.scoresheet;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.UnauthorizedException;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetStatus;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ListScoreSheetSummariesUseCaseTest {

    private final ScoreSheetSummary summary1 = new ScoreSheetSummary("ss1", "p1",
            ScoreSheetStatus.DRAFT, 1, 2, 5);
    private final ScoreSheetSummary summary2 = new ScoreSheetSummary("ss2", "p2",
            ScoreSheetStatus.PUBLISHED, 1, 1, 3);

    @Test
    void execute_hm_shouldReturnOwnHospital() {
        ScoreSheetQueryPort queryPort = stubQueryPort(List.of(summary1), List.of(summary1, summary2));
        ListScoreSheetSummariesUseCase useCase = new ListScoreSheetSummariesUseCase(queryPort);
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        List<ScoreSheetSummary> result = useCase.execute(actor);

        assertEquals(1, result.size());
        assertEquals("ss1", result.get(0).getScoreSheetId());
    }

    @Test
    void execute_sa_shouldReturnAll() {
        ScoreSheetQueryPort queryPort = stubQueryPort(List.of(summary1), List.of(summary1, summary2));
        ListScoreSheetSummariesUseCase useCase = new ListScoreSheetSummariesUseCase(queryPort);
        ActorContext actor = new ActorContext("u1", "Admin",
                EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.empty());

        List<ScoreSheetSummary> result = useCase.execute(actor);

        assertEquals(2, result.size());
    }

    @Test
    void execute_noRole_shouldThrow() {
        ScoreSheetQueryPort queryPort = stubQueryPort(List.of(), List.of());
        ListScoreSheetSummariesUseCase useCase = new ListScoreSheetSummariesUseCase(queryPort);
        ActorContext actor = new ActorContext("u1", "Reviewer",
                EnumSet.of(DomainRole.REVIEWER), Optional.of("H001"));

        assertThrows(UnauthorizedException.class, () -> useCase.execute(actor));
    }

    private ScoreSheetQueryPort stubQueryPort(List<ScoreSheetSummary> byHospital,
                                               List<ScoreSheetSummary> all) {
        return new ScoreSheetQueryPort() {
            @Override
            public List<ScoreSheetSummary> findByHospitalId(String hospitalId) { return byHospital; }
            @Override
            public List<ScoreSheetSummary> findAll() { return all; }
            @Override
            public Optional<ScoreSheetSummary> findSummaryById(String id) { return Optional.empty(); }
        };
    }
}
```

- [ ] **Step 5: Create GetScoreSheetDetailUseCaseTest**

Create `services/quality-inspection-service/src/test/java/com/tzuchi/his/quality/inspection/application/scoresheet/GetScoreSheetDetailUseCaseTest.java`:

```java
package com.tzuchi.his.quality.inspection.application.scoresheet;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.HospitalIdResolver;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetStatus;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GetScoreSheetDetailUseCaseTest {

    private final ScoreSheetSummary summary = new ScoreSheetSummary("ss1", "p1",
            ScoreSheetStatus.DRAFT, 1, 2, 5);

    @Test
    void execute_hm_shouldReturn() {
        ScoreSheetQueryPort queryPort = stubQueryPortWithSummary(summary);
        HospitalIdResolver resolver = projectId -> "H001";
        GetScoreSheetDetailUseCase useCase = new GetScoreSheetDetailUseCase(queryPort, resolver);
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        ScoreSheetSummary result = useCase.execute(actor, "ss1");

        assertEquals("ss1", result.getScoreSheetId());
    }

    @Test
    void execute_sa_shouldReturn() {
        ScoreSheetQueryPort queryPort = stubQueryPortWithSummary(summary);
        HospitalIdResolver resolver = projectId -> "H001";
        GetScoreSheetDetailUseCase useCase = new GetScoreSheetDetailUseCase(queryPort, resolver);
        ActorContext actor = new ActorContext("u1", "Admin",
                EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.empty());

        ScoreSheetSummary result = useCase.execute(actor, "ss1");

        assertEquals("ss1", result.getScoreSheetId());
    }

    @Test
    void execute_hm_wrongHospital_shouldThrow() {
        ScoreSheetQueryPort queryPort = stubQueryPortWithSummary(summary);
        HospitalIdResolver resolver = projectId -> "H001";
        GetScoreSheetDetailUseCase useCase = new GetScoreSheetDetailUseCase(queryPort, resolver);
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H999"));

        assertThrows(AccessDeniedException.class, () -> useCase.execute(actor, "ss1"));
    }

    @Test
    void execute_notFound_shouldThrow404() {
        ScoreSheetQueryPort queryPort = stubQueryPortEmpty();
        HospitalIdResolver resolver = projectId -> "H001";
        GetScoreSheetDetailUseCase useCase = new GetScoreSheetDetailUseCase(queryPort, resolver);
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(actor, "ss1"));
    }

    private ScoreSheetQueryPort stubQueryPortWithSummary(ScoreSheetSummary s) {
        return new ScoreSheetQueryPort() {
            @Override
            public List<ScoreSheetSummary> findByHospitalId(String hospitalId) { return List.of(); }
            @Override
            public List<ScoreSheetSummary> findAll() { return List.of(); }
            @Override
            public Optional<ScoreSheetSummary> findSummaryById(String id) {
                return id.equals(s.getScoreSheetId()) ? Optional.of(s) : Optional.empty();
            }
        };
    }

    private ScoreSheetQueryPort stubQueryPortEmpty() {
        return new ScoreSheetQueryPort() {
            @Override
            public List<ScoreSheetSummary> findByHospitalId(String hospitalId) { return List.of(); }
            @Override
            public List<ScoreSheetSummary> findAll() { return List.of(); }
            @Override
            public Optional<ScoreSheetSummary> findSummaryById(String id) { return Optional.empty(); }
        };
    }
}
```

- [ ] **Step 6: Create GetScoreSheetVersionUseCaseTest**

Create `services/quality-inspection-service/src/test/java/com/tzuchi/his/quality/inspection/application/scoresheet/GetScoreSheetVersionUseCaseTest.java`:

```java
package com.tzuchi.his.quality.inspection.application.scoresheet;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.HospitalIdResolver;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.scoresheet.*;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryScoreSheetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GetScoreSheetVersionUseCaseTest {

    private InMemoryScoreSheetRepository repository;
    private GetScoreSheetVersionUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryScoreSheetRepository();
        HospitalIdResolver resolver = projectId -> "H001";
        useCase = new GetScoreSheetVersionUseCase(repository, resolver);
    }

    @Test
    void execute_hm_shouldReturnVersion() {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft("ss1", "p1");
        repository.save(ss);
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        ScoreSheetVersionInfo info = useCase.execute(actor, "ss1");

        assertEquals(1, info.getVersion());
        assertFalse(info.isPublished());
    }

    @Test
    void execute_sa_shouldReturnVersion() {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft("ss1", "p1");
        repository.save(ss);
        ActorContext actor = new ActorContext("u1", "Admin",
                EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.empty());

        ScoreSheetVersionInfo info = useCase.execute(actor, "ss1");

        assertEquals(1, info.getVersion());
    }

    @Test
    void execute_hm_wrongHospital_shouldThrow() {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft("ss1", "p1");
        repository.save(ss);
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H999"));

        assertThrows(AccessDeniedException.class, () -> useCase.execute(actor, "ss1"));
    }

    @Test
    void execute_notFound_shouldThrow404() {
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(actor, "nonexistent"));
    }
}
```

- [ ] **Step 7: Create GetScoreSheetSectionsUseCaseTest (HM only)**

Create `services/quality-inspection-service/src/test/java/com/tzuchi/his/quality/inspection/application/scoresheet/GetScoreSheetSectionsUseCaseTest.java`:

```java
package com.tzuchi.his.quality.inspection.application.scoresheet;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.HospitalIdResolver;
import com.tzuchi.his.quality.inspection.application.common.UnauthorizedException;
import com.tzuchi.his.quality.inspection.domain.scoresheet.*;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryScoreSheetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GetScoreSheetSectionsUseCaseTest {

    private InMemoryScoreSheetRepository repository;
    private GetScoreSheetSectionsUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new InMemoryScoreSheetRepository();
        HospitalIdResolver resolver = projectId -> "H001";
        useCase = new GetScoreSheetSectionsUseCase(repository, resolver);
    }

    @Test
    void execute_hm_shouldReturnSections() {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft("ss1", "p1");
        ScoreSheetSection section = new ScoreSheetSection("sec1", "General");
        section.addItem(new ScoreItem("i1", "C01", "Item1",
                ScoreType.FIXED, BigDecimal.TEN, BigDecimal.ONE));
        ss.addSection(section);
        repository.save(ss);
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

        List<ScoreSheetSection> result = useCase.execute(actor, "ss1");

        assertEquals(1, result.size());
        assertEquals("General", result.get(0).getTitle());
    }

    @Test
    void execute_sa_shouldThrow() {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft("ss1", "p1");
        repository.save(ss);
        ActorContext actor = new ActorContext("u1", "Admin",
                EnumSet.of(DomainRole.SYSTEM_ADMIN), Optional.empty());

        assertThrows(UnauthorizedException.class, () -> useCase.execute(actor, "ss1"));
    }

    @Test
    void execute_hm_wrongHospital_shouldThrow() {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft("ss1", "p1");
        repository.save(ss);
        ActorContext actor = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H999"));

        assertThrows(AccessDeniedException.class, () -> useCase.execute(actor, "ss1"));
    }
}
```

- [ ] **Step 8: Run all tests**

Run: `cd services/quality-inspection-service && .\gradlew clean test`
Expected: All tests PASS (~190+ tests).

- [ ] **Step 9: Commit**

```bash
git add -A && git commit -m "test(b1b): add application layer tests for all ScoreSheet use cases

- CreateScoreSheetDraftUseCaseTest: 4 tests (happy path, event, wrong role, wrong hospital)
- PublishScoreSheetVersionUseCaseTest: 4 tests (publish, not found, wrong role, wrong hospital)
- UpdateScoreSheetDraftUseCaseTest: 4 tests (add section, not found, wrong role, wrong hospital)
- ListScoreSheetSummariesUseCaseTest: 3 tests (HM own hospital, SA all, no role)
- GetScoreSheetDetailUseCaseTest: 4 tests (HM, SA, wrong hospital, not found)
- GetScoreSheetVersionUseCaseTest: 4 tests (HM, SA, wrong hospital, not found)
- GetScoreSheetSectionsUseCaseTest: 3 tests (HM, SA rejected, wrong hospital)"
```

---

## Task 10: Interfaces — Controller 7 Endpoints + DTOs + API Integration Tests

**Files:**
- Create: `interfaces/rest/dto/UpdateScoreSheetDraftRequest.java`
- Create: `interfaces/rest/dto/ScoreSheetSummaryResponse.java`
- Create: `interfaces/rest/dto/ScoreSheetSectionResponse.java`
- Create: `interfaces/rest/dto/ScoreSheetItemResponse.java`
- Modify: `interfaces/rest/ScoreSheetController.java`
- Create: `interfaces/rest/ScoreSheetApiTest.java` (test)

- [ ] **Step 1: Create UpdateScoreSheetDraftRequest DTO**

Create `services/quality-inspection-service/src/main/java/com/tzuchi/his/quality/inspection/interfaces/rest/dto/UpdateScoreSheetDraftRequest.java`:

```java
package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import java.math.BigDecimal;
import java.util.List;

public class UpdateScoreSheetDraftRequest {

    private List<AddSectionRequest> addSections;
    private List<UpdateSectionRequest> updateSections;
    private List<String> removeSectionIds;
    private List<AddItemRequest> addItems;
    private List<UpdateItemRequest> updateItems;
    private List<String> removeItemIds;

    // getters/setters
    public List<AddSectionRequest> getAddSections() { return addSections; }
    public void setAddSections(List<AddSectionRequest> addSections) { this.addSections = addSections; }
    public List<UpdateSectionRequest> getUpdateSections() { return updateSections; }
    public void setUpdateSections(List<UpdateSectionRequest> updateSections) { this.updateSections = updateSections; }
    public List<String> getRemoveSectionIds() { return removeSectionIds; }
    public void setRemoveSectionIds(List<String> removeSectionIds) { this.removeSectionIds = removeSectionIds; }
    public List<AddItemRequest> getAddItems() { return addItems; }
    public void setAddItems(List<AddItemRequest> addItems) { this.addItems = addItems; }
    public List<UpdateItemRequest> getUpdateItems() { return updateItems; }
    public void setUpdateItems(List<UpdateItemRequest> updateItems) { this.updateItems = updateItems; }
    public List<String> getRemoveItemIds() { return removeItemIds; }
    public void setRemoveItemIds(List<String> removeItemIds) { this.removeItemIds = removeItemIds; }

    public static class AddSectionRequest {
        private String sectionId;
        private String title;
        private List<AddItemRequest> items;
        public String getSectionId() { return sectionId; }
        public void setSectionId(String sectionId) { this.sectionId = sectionId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public List<AddItemRequest> getItems() { return items; }
        public void setItems(List<AddItemRequest> items) { this.items = items; }
    }

    public static class UpdateSectionRequest {
        private String sectionId;
        private String title;
        public String getSectionId() { return sectionId; }
        public void setSectionId(String sectionId) { this.sectionId = sectionId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
    }

    public static class AddItemRequest {
        private String targetSectionId;
        private String itemId;
        private String code;
        private String title;
        private String scoreType;
        private BigDecimal maxScore;
        private BigDecimal weight;
        private String requiredRule;
        private String conditionExpression;
        // getters/setters
        public String getTargetSectionId() { return targetSectionId; }
        public void setTargetSectionId(String targetSectionId) { this.targetSectionId = targetSectionId; }
        public String getItemId() { return itemId; }
        public void setItemId(String itemId) { this.itemId = itemId; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getScoreType() { return scoreType; }
        public void setScoreType(String scoreType) { this.scoreType = scoreType; }
        public BigDecimal getMaxScore() { return maxScore; }
        public void setMaxScore(BigDecimal maxScore) { this.maxScore = maxScore; }
        public BigDecimal getWeight() { return weight; }
        public void setWeight(BigDecimal weight) { this.weight = weight; }
        public String getRequiredRule() { return requiredRule; }
        public void setRequiredRule(String requiredRule) { this.requiredRule = requiredRule; }
        public String getConditionExpression() { return conditionExpression; }
        public void setConditionExpression(String conditionExpression) { this.conditionExpression = conditionExpression; }
    }

    public static class UpdateItemRequest {
        private String itemId;
        private String code;
        private String title;
        private String scoreType;
        private BigDecimal maxScore;
        private BigDecimal weight;
        private String requiredRule;
        private String conditionExpression;
        // getters/setters
        public String getItemId() { return itemId; }
        public void setItemId(String itemId) { this.itemId = itemId; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getScoreType() { return scoreType; }
        public void setScoreType(String scoreType) { this.scoreType = scoreType; }
        public BigDecimal getMaxScore() { return maxScore; }
        public void setMaxScore(BigDecimal maxScore) { this.maxScore = maxScore; }
        public BigDecimal getWeight() { return weight; }
        public void setWeight(BigDecimal weight) { this.weight = weight; }
        public String getRequiredRule() { return requiredRule; }
        public void setRequiredRule(String requiredRule) { this.requiredRule = requiredRule; }
        public String getConditionExpression() { return conditionExpression; }
        public void setConditionExpression(String conditionExpression) { this.conditionExpression = conditionExpression; }
    }
}
```

- [ ] **Step 2: Create response DTOs**

Create `ScoreSheetSummaryResponse.java`:

```java
package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

public class ScoreSheetSummaryResponse {
    private String scoreSheetId;
    private String projectId;
    private String status;
    private int version;
    private int sectionCount;
    private int itemCount;

    public ScoreSheetSummaryResponse(String scoreSheetId, String projectId, String status,
                                      int version, int sectionCount, int itemCount) {
        this.scoreSheetId = scoreSheetId;
        this.projectId = projectId;
        this.status = status;
        this.version = version;
        this.sectionCount = sectionCount;
        this.itemCount = itemCount;
    }

    public String getScoreSheetId() { return scoreSheetId; }
    public String getProjectId() { return projectId; }
    public String getStatus() { return status; }
    public int getVersion() { return version; }
    public int getSectionCount() { return sectionCount; }
    public int getItemCount() { return itemCount; }
}
```

Create `ScoreSheetSectionResponse.java`:

```java
package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import java.util.List;

public class ScoreSheetSectionResponse {
    private String sectionId;
    private String title;
    private List<ScoreSheetItemResponse> items;

    public ScoreSheetSectionResponse(String sectionId, String title, List<ScoreSheetItemResponse> items) {
        this.sectionId = sectionId;
        this.title = title;
        this.items = items;
    }

    public String getSectionId() { return sectionId; }
    public String getTitle() { return title; }
    public List<ScoreSheetItemResponse> getItems() { return items; }
}
```

Create `ScoreSheetItemResponse.java`:

```java
package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import java.math.BigDecimal;

public class ScoreSheetItemResponse {
    private String itemId;
    private String code;
    private String title;
    private String scoreType;
    private BigDecimal maxScore;
    private BigDecimal weight;
    private String requiredRule;
    private String conditionExpression;

    public ScoreSheetItemResponse(String itemId, String code, String title, String scoreType,
                                   BigDecimal maxScore, BigDecimal weight,
                                   String requiredRule, String conditionExpression) {
        this.itemId = itemId;
        this.code = code;
        this.title = title;
        this.scoreType = scoreType;
        this.maxScore = maxScore;
        this.weight = weight;
        this.requiredRule = requiredRule;
        this.conditionExpression = conditionExpression;
    }

    public String getItemId() { return itemId; }
    public String getCode() { return code; }
    public String getTitle() { return title; }
    public String getScoreType() { return scoreType; }
    public BigDecimal getMaxScore() { return maxScore; }
    public BigDecimal getWeight() { return weight; }
    public String getRequiredRule() { return requiredRule; }
    public String getConditionExpression() { return conditionExpression; }
}
```

- [ ] **Step 3: Replace ScoreSheetController with full implementation**

Replace entire content of `interfaces/rest/ScoreSheetController.java`:

```java
package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.scoresheet.*;
import com.tzuchi.his.quality.inspection.domain.scoresheet.RequiredRule;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreItem;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetSection;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreType;
import com.tzuchi.his.quality.inspection.domain.scoresheet.UpdateDraftCommand;
import com.tzuchi.his.quality.inspection.interfaces.rest.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/score-sheets")
public class ScoreSheetController {

    private final CreateScoreSheetDraftUseCase createDraftUseCase;
    private final PublishScoreSheetVersionUseCase publishVersionUseCase;
    private final UpdateScoreSheetDraftUseCase updateDraftUseCase;
    private final ListScoreSheetSummariesUseCase listUseCase;
    private final GetScoreSheetDetailUseCase detailUseCase;
    private final GetScoreSheetVersionUseCase versionUseCase;
    private final GetScoreSheetSectionsUseCase sectionsUseCase;

    public ScoreSheetController(CreateScoreSheetDraftUseCase createDraftUseCase,
                                 PublishScoreSheetVersionUseCase publishVersionUseCase,
                                 UpdateScoreSheetDraftUseCase updateDraftUseCase,
                                 ListScoreSheetSummariesUseCase listUseCase,
                                 GetScoreSheetDetailUseCase detailUseCase,
                                 GetScoreSheetVersionUseCase versionUseCase,
                                 GetScoreSheetSectionsUseCase sectionsUseCase) {
        this.createDraftUseCase = createDraftUseCase;
        this.publishVersionUseCase = publishVersionUseCase;
        this.updateDraftUseCase = updateDraftUseCase;
        this.listUseCase = listUseCase;
        this.detailUseCase = detailUseCase;
        this.versionUseCase = versionUseCase;
        this.sectionsUseCase = sectionsUseCase;
    }

    @PostMapping
    public ResponseEntity<?> createDraft(@CurrentActor ActorContext actor,
                                          @RequestBody CreateScoreSheetDraftRequest request) {
        String scoreSheetId = createDraftUseCase.execute(actor, request.getProjectId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(Map.of("scoreSheetId", scoreSheetId)));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<?> publishVersion(@CurrentActor ActorContext actor,
                                             @PathVariable("id") String scoreSheetId) {
        publishVersionUseCase.execute(actor, scoreSheetId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateDraft(@CurrentActor ActorContext actor,
                                          @PathVariable("id") String scoreSheetId,
                                          @RequestBody UpdateScoreSheetDraftRequest request) {
        UpdateDraftCommand command = toCommand(request);
        updateDraftUseCase.execute(actor, scoreSheetId, command);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping
    public ResponseEntity<?> list(@CurrentActor ActorContext actor) {
        List<ScoreSheetSummary> summaries = listUseCase.execute(actor);
        List<ScoreSheetSummaryResponse> response = summaries.stream()
                .map(this::toSummaryResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> detail(@CurrentActor ActorContext actor,
                                     @PathVariable("id") String scoreSheetId) {
        ScoreSheetSummary summary = detailUseCase.execute(actor, scoreSheetId);
        return ResponseEntity.ok(ApiResponse.ok(toSummaryResponse(summary)));
    }

    @GetMapping("/{id}/version")
    public ResponseEntity<?> version(@CurrentActor ActorContext actor,
                                      @PathVariable("id") String scoreSheetId) {
        ScoreSheetVersionInfo info = versionUseCase.execute(actor, scoreSheetId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "version", info.getVersion(),
                "published", info.isPublished())));
    }

    @GetMapping("/{id}/sections")
    public ResponseEntity<?> sections(@CurrentActor ActorContext actor,
                                       @PathVariable("id") String scoreSheetId) {
        List<ScoreSheetSection> sections = sectionsUseCase.execute(actor, scoreSheetId);
        List<ScoreSheetSectionResponse> response = sections.stream()
                .map(this::toSectionResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // --- Mapping helpers ---

    private UpdateDraftCommand toCommand(UpdateScoreSheetDraftRequest req) {
        List<UpdateDraftCommand.AddSectionCommand> addSections = req.getAddSections() != null
                ? req.getAddSections().stream().map(s -> new UpdateDraftCommand.AddSectionCommand(
                        s.getSectionId(), s.getTitle(),
                        s.getItems() != null ? s.getItems().stream().map(this::toAddItemCmd).collect(Collectors.toList()) : Collections.emptyList()))
                .collect(Collectors.toList()) : null;

        List<UpdateDraftCommand.UpdateSectionCommand> updateSections = req.getUpdateSections() != null
                ? req.getUpdateSections().stream().map(s -> new UpdateDraftCommand.UpdateSectionCommand(
                        s.getSectionId(), s.getTitle()))
                .collect(Collectors.toList()) : null;

        List<UpdateDraftCommand.AddItemCommand> addItems = req.getAddItems() != null
                ? req.getAddItems().stream().map(this::toAddItemCmd).collect(Collectors.toList()) : null;

        List<UpdateDraftCommand.UpdateItemCommand> updateItems = req.getUpdateItems() != null
                ? req.getUpdateItems().stream().map(i -> new UpdateDraftCommand.UpdateItemCommand(
                        i.getItemId(), i.getCode(), i.getTitle(),
                        ScoreType.valueOf(i.getScoreType()),
                        i.getMaxScore(), i.getWeight(),
                        RequiredRule.valueOf(i.getRequiredRule()),
                        i.getConditionExpression()))
                .collect(Collectors.toList()) : null;

        return new UpdateDraftCommand(addSections, updateSections, req.getRemoveSectionIds(),
                addItems, updateItems, req.getRemoveItemIds());
    }

    private UpdateDraftCommand.AddItemCommand toAddItemCmd(UpdateScoreSheetDraftRequest.AddItemRequest i) {
        return new UpdateDraftCommand.AddItemCommand(
                i.getTargetSectionId(), i.getItemId(), i.getCode(), i.getTitle(),
                ScoreType.valueOf(i.getScoreType()),
                i.getMaxScore(), i.getWeight(),
                RequiredRule.valueOf(i.getRequiredRule()),
                i.getConditionExpression());
    }

    private ScoreSheetSummaryResponse toSummaryResponse(ScoreSheetSummary s) {
        return new ScoreSheetSummaryResponse(s.getScoreSheetId(), s.getProjectId(),
                s.getStatus().name(), s.getVersion(), s.getSectionCount(), s.getItemCount());
    }

    private ScoreSheetSectionResponse toSectionResponse(ScoreSheetSection section) {
        List<ScoreSheetItemResponse> items = section.getItems().stream()
                .map(this::toItemResponse)
                .collect(Collectors.toList());
        return new ScoreSheetSectionResponse(section.getSectionId(), section.getTitle(), items);
    }

    private ScoreSheetItemResponse toItemResponse(ScoreItem item) {
        return new ScoreSheetItemResponse(item.getItemId(), item.getCode(), item.getTitle(),
                item.getScoreType().name(), item.getMaxScore(), item.getWeight(),
                item.getRequiredRule().name(), item.getConditionExpression());
    }
}
```

Note: `CreateScoreSheetDraftRequest` already exists in the codebase — do not recreate it. The existing DTO has a `projectId` field.

- [ ] **Step 4: Create ScoreSheetApiTest**

Create `services/quality-inspection-service/src/test/java/com/tzuchi/his/quality/inspection/interfaces/rest/ScoreSheetApiTest.java`:

```java
package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tzuchi.his.quality.inspection.TestTokenFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ScoreSheetApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private static final String PROJECTS_BASE = "/review-projects";
    private static final String SS_BASE = "/score-sheets";
    private String hmToken = TestTokenFactory.hospitalManagerToken();
    private String saToken = TestTokenFactory.systemAdminToken();

    // --- Helper: create a project first (needed for HospitalIdResolver) ---

    private String createProject() throws Exception {
        String body = "{\"name\":\"Test Project\",\"medicalRecordTypes\":[\"OUTPATIENT\"]}";
        MvcResult result = mockMvc.perform(post(PROJECTS_BASE)
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        String json = result.getResponse().getContentAsString();
        return objectMapper.readTree(json).at("/data/projectId").asText();
    }

    private String createScoreSheet(String projectId) throws Exception {
        String body = "{\"projectId\":\"" + projectId + "\"}";
        MvcResult result = mockMvc.perform(post(SS_BASE)
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        String json = result.getResponse().getContentAsString();
        return objectMapper.readTree(json).at("/data/scoreSheetId").asText();
    }

    private void addSectionToSheet(String scoreSheetId) throws Exception {
        Map<String, Object> addItem = new LinkedHashMap<>();
        addItem.put("targetSectionId", "sec1");
        addItem.put("itemId", "i1");
        addItem.put("code", "C01");
        addItem.put("title", "Item 1");
        addItem.put("scoreType", "FIXED");
        addItem.put("maxScore", 10);
        addItem.put("weight", 1);
        addItem.put("requiredRule", "ALWAYS_REQUIRED");

        Map<String, Object> addSection = new LinkedHashMap<>();
        addSection.put("sectionId", "sec1");
        addSection.put("title", "General");
        addSection.put("items", List.of(addItem));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("addSections", List.of(addSection));

        mockMvc.perform(put(SS_BASE + "/" + scoreSheetId)
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    // --- CREATE ---

    @Test
    void createDraft_shouldReturn201() throws Exception {
        String projectId = createProject();
        String body = "{\"projectId\":\"" + projectId + "\"}";

        mockMvc.perform(post(SS_BASE)
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.scoreSheetId").isNotEmpty());
    }

    @Test
    void createDraft_sa_shouldReturn403() throws Exception {
        String projectId = createProject();
        String body = "{\"projectId\":\"" + projectId + "\"}";

        mockMvc.perform(post(SS_BASE)
                        .header("api-key", saToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    // --- PUBLISH ---

    @Test
    void publish_shouldReturn200() throws Exception {
        String projectId = createProject();
        String ssId = createScoreSheet(projectId);
        addSectionToSheet(ssId);

        mockMvc.perform(post(SS_BASE + "/" + ssId + "/publish")
                        .header("api-key", hmToken))
                .andExpect(status().isOk());
    }

    @Test
    void publish_nonexistent_shouldReturn404() throws Exception {
        mockMvc.perform(post(SS_BASE + "/nonexistent/publish")
                        .header("api-key", hmToken))
                .andExpect(status().isNotFound());
    }

    // --- UPDATE DRAFT ---

    @Test
    void updateDraft_addSection_shouldReturn200() throws Exception {
        String projectId = createProject();
        String ssId = createScoreSheet(projectId);

        Map<String, Object> addItem = new LinkedHashMap<>();
        addItem.put("targetSectionId", "sec1");
        addItem.put("itemId", "i1");
        addItem.put("code", "C01");
        addItem.put("title", "Item 1");
        addItem.put("scoreType", "FIXED");
        addItem.put("maxScore", 10);
        addItem.put("weight", 1);
        addItem.put("requiredRule", "ALWAYS_REQUIRED");

        Map<String, Object> addSection = new LinkedHashMap<>();
        addSection.put("sectionId", "sec1");
        addSection.put("title", "General");
        addSection.put("items", List.of(addItem));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("addSections", List.of(addSection));

        mockMvc.perform(put(SS_BASE + "/" + ssId)
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    @Test
    void updateDraft_onPublished_shouldReturn409() throws Exception {
        String projectId = createProject();
        String ssId = createScoreSheet(projectId);
        addSectionToSheet(ssId);
        mockMvc.perform(post(SS_BASE + "/" + ssId + "/publish")
                        .header("api-key", hmToken))
                .andExpect(status().isOk());

        Map<String, Object> body = Map.of("removeSectionIds", List.of("sec1"));

        mockMvc.perform(put(SS_BASE + "/" + ssId)
                        .header("api-key", hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict());
    }

    // --- LIST ---

    @Test
    void list_hm_shouldReturn200() throws Exception {
        String projectId = createProject();
        createScoreSheet(projectId);

        mockMvc.perform(get(SS_BASE)
                        .header("api-key", hmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    void list_sa_shouldReturn200() throws Exception {
        String projectId = createProject();
        createScoreSheet(projectId);

        mockMvc.perform(get(SS_BASE)
                        .header("api-key", saToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    // --- DETAIL ---

    @Test
    void detail_hm_shouldReturn200() throws Exception {
        String projectId = createProject();
        String ssId = createScoreSheet(projectId);

        mockMvc.perform(get(SS_BASE + "/" + ssId)
                        .header("api-key", hmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scoreSheetId").value(ssId));
    }

    @Test
    void detail_sa_shouldReturn200() throws Exception {
        String projectId = createProject();
        String ssId = createScoreSheet(projectId);

        mockMvc.perform(get(SS_BASE + "/" + ssId)
                        .header("api-key", saToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scoreSheetId").value(ssId));
    }

    // --- VERSION ---

    @Test
    void version_shouldReturn200() throws Exception {
        String projectId = createProject();
        String ssId = createScoreSheet(projectId);

        mockMvc.perform(get(SS_BASE + "/" + ssId + "/version")
                        .header("api-key", hmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(1))
                .andExpect(jsonPath("$.data.published").value(false));
    }

    // --- SECTIONS ---

    @Test
    void sections_hm_shouldReturn200() throws Exception {
        String projectId = createProject();
        String ssId = createScoreSheet(projectId);
        addSectionToSheet(ssId);

        mockMvc.perform(get(SS_BASE + "/" + ssId + "/sections")
                        .header("api-key", hmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].sectionId").value("sec1"))
                .andExpect(jsonPath("$.data[0].items", hasSize(1)));
    }

    @Test
    void sections_sa_shouldReturn403() throws Exception {
        String projectId = createProject();
        String ssId = createScoreSheet(projectId);

        mockMvc.perform(get(SS_BASE + "/" + ssId + "/sections")
                        .header("api-key", saToken))
                .andExpect(status().isForbidden());
    }
}
```

- [ ] **Step 5: Run all tests**

Run: `cd services/quality-inspection-service && .\gradlew clean test`
Expected: All tests PASS (~210+ tests).

- [ ] **Step 6: Commit**

```bash
git add -A && git commit -m "feat(b1b): add ScoreSheetController 7 endpoints + DTOs + API integration tests

- 7 endpoints: create, publish, updateDraft, list, detail, version, sections
- Publish path changed from /publish-version to /publish
- UpdateScoreSheetDraftRequest: action-based with add/update/remove sections and items
- ScoreSheetSummaryResponse, ScoreSheetSectionResponse, ScoreSheetItemResponse DTOs
- ScoreSheetApiTest: ~15 integration tests covering all endpoints and permission cases"
```

---

## Final Verification

After all 10 tasks are complete:

- [ ] **Run full test suite**

```bash
cd services/quality-inspection-service && .\gradlew clean test
```

Expected: ~210+ tests, 0 failures.

- [ ] **Verify bootRun**

```bash
cd services/quality-inspection-service && .\gradlew bootRun
```

Expected: Application starts successfully within 5 seconds. `Ctrl+C` to stop.

- [ ] **Review commit log**

```bash
cd services/quality-inspection-service && git --no-pager log --oneline -12
```

Expected: 10 task commits + any fixup commits, all on main branch.
