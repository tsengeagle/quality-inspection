package com.tzuchi.his.quality.inspection.domain.scoresheet;

import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 同一版本內 itemId 與 code 不重複，不論是否在同一章節（domain-model.md §5.2 Domain Invariants；
 * 錯誤代碼依 api-spec.md §8.2）。跨章節的情況已由 ScoreSheetDefinitionTest 涵蓋。
 */
class ScoreSheetItemUniquenessTest {

    private static ScoreItem item(String id, String code) {
        return new ScoreItem(id, code, "Item " + id, ScoreType.FIXED, BigDecimal.TEN, BigDecimal.ONE);
    }

    private static ScoreSheetDefinition sheetWithOneSection(ScoreItem... items) {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft("ss1", "p1");
        ScoreSheetSection section = new ScoreSheetSection("sec1", "General");
        for (ScoreItem i : items) {
            section.addItem(i);
        }
        ss.addSection(section);
        return ss;
    }

    private static UpdateDraftCommand updateItem(String id, String code) {
        return new UpdateDraftCommand(null, null, null, null,
                List.of(new UpdateDraftCommand.UpdateItemCommand(id, code, "Item " + id,
                        ScoreType.FIXED, BigDecimal.TEN, BigDecimal.ONE, RequiredRule.ALWAYS_REQUIRED, null)),
                null);
    }

    private static UpdateDraftCommand addItem(String sectionId, String id, String code) {
        return new UpdateDraftCommand(null, null, null,
                List.of(new UpdateDraftCommand.AddItemCommand(sectionId, id, code, "Item " + id,
                        ScoreType.FIXED, BigDecimal.TEN, BigDecimal.ONE, RequiredRule.ALWAYS_REQUIRED, null)),
                null, null);
    }

    @Test
    void updatingAnItemToTheCodeOfAnotherItemInTheSameSectionIsRejected() {
        ScoreSheetDefinition ss = sheetWithOneSection(item("i1", "C01"), item("i2", "C02"));

        DomainStateConflictException e = assertThrows(DomainStateConflictException.class,
                () -> ss.updateDraft(updateItem("i2", "C01")));

        assertEquals("DUPLICATE_ITEM_CODE", e.getErrorCode());
        assertEquals("C02", ss.getSections().get(0).getItems().get(1).getCode(), "題目沒有被取代");
    }

    @Test
    void updatingAnItemKeepingItsOwnCodeIsAccepted() {
        ScoreSheetDefinition ss = sheetWithOneSection(item("i1", "C01"), item("i2", "C02"));

        ss.updateDraft(updateItem("i2", "C02"));

        assertEquals("C02", ss.getSections().get(0).getItems().get(1).getCode());
    }

    @Test
    void addingAnItemWithADuplicateIdToTheSameSectionIsRejectedWithItsCode() {
        ScoreSheetDefinition ss = sheetWithOneSection(item("i1", "C01"));

        DomainStateConflictException e = assertThrows(DomainStateConflictException.class,
                () -> ss.updateDraft(addItem("sec1", "i1", "C09")));

        assertEquals("DUPLICATE_ITEM_ID", e.getErrorCode());
    }

    @Test
    void addingAnItemWithADuplicateCodeToTheSameSectionIsRejectedWithItsCode() {
        ScoreSheetDefinition ss = sheetWithOneSection(item("i1", "C01"));

        DomainStateConflictException e = assertThrows(DomainStateConflictException.class,
                () -> ss.updateDraft(addItem("sec1", "i9", "C01")));

        assertEquals("DUPLICATE_ITEM_CODE", e.getErrorCode());
    }

    @Test
    void aNewSectionWhoseOwnItemsRepeatACodeIsRejectedWithItsCode() {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft("ss1", "p1");
        UpdateDraftCommand cmd = new UpdateDraftCommand(
                List.of(new UpdateDraftCommand.AddSectionCommand("sec1", "General", List.of(
                        new UpdateDraftCommand.AddItemCommand(null, "i1", "C01", "Item i1",
                                ScoreType.FIXED, BigDecimal.TEN, BigDecimal.ONE, RequiredRule.ALWAYS_REQUIRED, null),
                        new UpdateDraftCommand.AddItemCommand(null, "i2", "C01", "Item i2",
                                ScoreType.FIXED, BigDecimal.TEN, BigDecimal.ONE, RequiredRule.ALWAYS_REQUIRED, null)))),
                null, null, null, null, null);

        DomainStateConflictException e = assertThrows(DomainStateConflictException.class, () -> ss.updateDraft(cmd));

        assertEquals("DUPLICATE_ITEM_CODE", e.getErrorCode());
        assertTrue(ss.getSections().isEmpty());
    }
}
