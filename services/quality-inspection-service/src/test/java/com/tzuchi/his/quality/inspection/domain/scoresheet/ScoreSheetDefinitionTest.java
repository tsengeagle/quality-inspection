package com.tzuchi.his.quality.inspection.domain.scoresheet;

import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

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

    private ScoreSheetDefinition createSheetWithOneItem() {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft("ss1", "p1");
        ScoreSheetSection section = new ScoreSheetSection("sec1", "General");
        section.addItem(new ScoreItem("i1", "C01", "Item1",
                ScoreType.FIXED, BigDecimal.TEN, BigDecimal.ONE));
        ss.addSection(section);
        return ss;
    }
}
