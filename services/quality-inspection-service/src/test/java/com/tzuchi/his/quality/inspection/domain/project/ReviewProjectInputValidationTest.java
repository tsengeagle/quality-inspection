package com.tzuchi.his.quality.inspection.domain.project;

import com.tzuchi.his.quality.inspection.domain.common.DomainValidationException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 名稱與病歷類型的輸入檢查（D-040 第 2、6 點；specs/review-project 的 RP-CRT-01、RP-CRT-02、RP-UPD-02、RP-UPD-03）：
 * 名稱為 null 或空白、病歷類型為 null 或空集合，一律 VALIDATION_ERROR。
 */
class ReviewProjectInputValidationTest {

    private static final Set<MedicalRecordType> OUTPATIENT = EnumSet.of(MedicalRecordType.OUTPATIENT);

    private static ReviewProject create(String name, Set<MedicalRecordType> types) {
        return ReviewProject.create("p1", "h1", name, null, types, null, null, null);
    }

    private static void assertValidationError(Runnable action) {
        DomainValidationException e = assertThrows(DomainValidationException.class, action::run);
        assertEquals("VALIDATION_ERROR", e.getErrorCode());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void createRejectsANullOrBlankName(String name) {
        assertValidationError(() -> create(name, OUTPATIENT));
    }

    @Test
    void createRejectsMissingMedicalRecordTypes() {
        assertValidationError(() -> create("門診", null));
        assertValidationError(() -> create("門診", EnumSet.noneOf(MedicalRecordType.class)));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void updateRejectsANullOrBlankNameAndChangesNothing(String name) {
        ReviewProject p = create("門診", OUTPATIENT);
        assertValidationError(() -> p.updateDraft(name, null, OUTPATIENT, null, null, null));
        assertEquals("門診", p.getName());
    }

    @Test
    void updateRejectsMissingMedicalRecordTypesAndChangesNothing() {
        ReviewProject p = create("門診", OUTPATIENT);
        assertValidationError(() -> p.updateDraft("住院", null, null, null, null, null));
        assertValidationError(() -> p.updateDraft("住院", null, EnumSet.noneOf(MedicalRecordType.class), null, null, null));
        assertEquals("門診", p.getName());
        assertEquals(OUTPATIENT, p.getMedicalRecordTypes());
    }

    @Test
    void renameRejectsABlankName() {
        ReviewProject p = create("門診", OUTPATIENT);
        assertValidationError(() -> p.updateName(" "));
        assertEquals("門診", p.getName());
    }
}
