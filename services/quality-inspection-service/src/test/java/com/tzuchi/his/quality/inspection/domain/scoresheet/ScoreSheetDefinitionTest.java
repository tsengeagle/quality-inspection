package com.tzuchi.his.quality.inspection.domain.scoresheet;

import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScoreSheetDefinitionTest {

    @Test
    void createDraft_shouldStartAsDraftV1() {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft("ss1", "p1");

        assertEquals("ss1", ss.getScoreSheetId());
        assertEquals("p1", ss.getProjectId());
        assertEquals(1, ss.getVersion());
        assertEquals(ScoreSheetStatus.DRAFT, ss.getStatus());
        assertTrue(ss.getSections().isEmpty());
    }

    @Test
    void addSection_draft_shouldSucceed() {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft("ss1", "p1");
        ScoreSheetSection section = new ScoreSheetSection("sec1", "General");

        ss.addSection(section);

        assertEquals(1, ss.getSections().size());
    }

    @Test
    void addSection_duplicateSectionId_shouldThrow() {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft("ss1", "p1");
        ss.addSection(new ScoreSheetSection("sec1", "General"));

        assertThrows(DomainStateConflictException.class,
                () -> ss.addSection(new ScoreSheetSection("sec1", "Another")));
    }

    @Test
    void publishVersion_withSectionsAndItems_shouldSucceed() {
        ScoreSheetDefinition ss = createSheetWithOneItem();

        ss.publishVersion();

        assertEquals(ScoreSheetStatus.PUBLISHED, ss.getStatus());
    }

    @Test
    void publishVersion_emptySections_shouldThrow() {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft("ss1", "p1");

        assertThrows(DomainStateConflictException.class, ss::publishVersion);
    }

    @Test
    void publishVersion_sectionWithNoItems_shouldThrow() {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft("ss1", "p1");
        ss.addSection(new ScoreSheetSection("sec1", "Empty"));

        assertThrows(DomainStateConflictException.class, ss::publishVersion);
    }

    @Test
    void publishVersion_alreadyPublished_shouldThrow() {
        ScoreSheetDefinition ss = createSheetWithOneItem();
        ss.publishVersion();

        assertThrows(DomainStateConflictException.class, ss::publishVersion);
    }

    @Test
    void addSection_afterPublish_shouldThrow() {
        ScoreSheetDefinition ss = createSheetWithOneItem();
        ss.publishVersion();

        assertThrows(DomainStateConflictException.class,
                () -> ss.addSection(new ScoreSheetSection("sec2", "Extra")));
    }

    @Test
    void scoreItem_negativeMaxScore_shouldThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> new ScoreItem("i1", "C01", "Item",
                        ScoreType.FIXED, BigDecimal.valueOf(-1), BigDecimal.ONE));
    }

    @Test
    void section_duplicateItemId_shouldThrow() {
        ScoreSheetSection section = new ScoreSheetSection("sec1", "Test");
        section.addItem(new ScoreItem("i1", "C01", "Item1",
                ScoreType.FIXED, BigDecimal.TEN, BigDecimal.ONE));

        assertThrows(IllegalArgumentException.class,
                () -> section.addItem(new ScoreItem("i1", "C02", "Item2",
                        ScoreType.BOOLEAN, BigDecimal.ONE, BigDecimal.ONE)));
    }

    @Test
    void section_duplicateCode_shouldThrow() {
        ScoreSheetSection section = new ScoreSheetSection("sec1", "Test");
        section.addItem(new ScoreItem("i1", "C01", "Item1",
                ScoreType.FIXED, BigDecimal.TEN, BigDecimal.ONE));

        assertThrows(IllegalArgumentException.class,
                () -> section.addItem(new ScoreItem("i2", "C01", "Item2",
                        ScoreType.BOOLEAN, BigDecimal.ONE, BigDecimal.ONE)));
    }

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

    // Step 1: requiredRule tests

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

    // Step 2: updateDraft tests

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

    // Step 3: section mutation tests

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

    @Test
    void updateDraft_updateItemCode_shouldSucceed() {
        ScoreSheetDefinition ss = createSheetWithOneItem();
        UpdateDraftCommand cmd = new UpdateDraftCommand(
                null, null, null, null,
                List.of(new UpdateDraftCommand.UpdateItemCommand("i1", "C99", "Item1",
                        ScoreType.FIXED, BigDecimal.TEN, BigDecimal.ONE,
                        RequiredRule.ALWAYS_REQUIRED, null)),
                null);

        ss.updateDraft(cmd);

        assertEquals("C99", ss.getSections().get(0).getItems().get(0).getCode());
    }

    @Test
    void updateDraft_updateItemCode_crossSectionDuplicate_shouldThrow() {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft("ss1", "p1");
        ScoreSheetSection s1 = new ScoreSheetSection("sec1", "Section 1");
        s1.addItem(new ScoreItem("i1", "C01", "Item1",
                ScoreType.FIXED, BigDecimal.TEN, BigDecimal.ONE));
        ss.addSection(s1);
        ScoreSheetSection s2 = new ScoreSheetSection("sec2", "Section 2");
        s2.addItem(new ScoreItem("i2", "C02", "Item2",
                ScoreType.FIXED, BigDecimal.TEN, BigDecimal.ONE));
        ss.addSection(s2);

        UpdateDraftCommand cmd = new UpdateDraftCommand(
                null, null, null, null,
                List.of(new UpdateDraftCommand.UpdateItemCommand("i1", "C02", "Item1",
                        ScoreType.FIXED, BigDecimal.TEN, BigDecimal.ONE,
                        RequiredRule.ALWAYS_REQUIRED, null)),
                null);

        assertThrows(DomainStateConflictException.class, () -> ss.updateDraft(cmd));
    }

    @Test
    void updateDraft_updateItem_shouldPreservePosition() {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft("ss1", "p1");
        ScoreSheetSection section = new ScoreSheetSection("sec1", "General");
        section.addItem(new ScoreItem("i1", "C01", "Item1",
                ScoreType.FIXED, BigDecimal.TEN, BigDecimal.ONE));
        section.addItem(new ScoreItem("i2", "C02", "Item2",
                ScoreType.FIXED, BigDecimal.TEN, BigDecimal.ONE));
        section.addItem(new ScoreItem("i3", "C03", "Item3",
                ScoreType.FIXED, BigDecimal.TEN, BigDecimal.ONE));
        ss.addSection(section);

        UpdateDraftCommand cmd = new UpdateDraftCommand(
                null, null, null, null,
                List.of(new UpdateDraftCommand.UpdateItemCommand("i2", "C02", "Updated Item2",
                        ScoreType.RANGE, BigDecimal.valueOf(20), BigDecimal.ONE,
                        RequiredRule.OPTIONAL, null)),
                null);

        ss.updateDraft(cmd);

        List<ScoreItem> items = ss.getSections().get(0).getItems();
        assertEquals(3, items.size());
        assertEquals("i1", items.get(0).getItemId());
        assertEquals("i2", items.get(1).getItemId());
        assertEquals("Updated Item2", items.get(1).getTitle());
        assertEquals("i3", items.get(2).getItemId());
    }

    private ScoreSheetDefinition createSheetWithOneItem() {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft("ss1", "p1");
        ScoreSheetSection section = new ScoreSheetSection("sec1", "General");
        section.addItem(new ScoreItem("i1", "C01", "Item1",
                ScoreType.FIXED, BigDecimal.TEN, BigDecimal.ONE));
        ss.addSection(section);
        return ss;
    }
}
