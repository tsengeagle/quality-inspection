package com.tzuchi.his.quality.inspection.domain.project;

import com.tzuchi.his.quality.inspection.domain.common.DomainValidationException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 品質線與權重的輸入（D-046、D-047、D-049；specs/review-project 的 RP-CRT-03、RP-CRT-S4、RP-CRT-S5、
 * RP-UPD-04、RP-UPD-S1、RP-UPD-S3）。權重在建立與更新時不驗證，發布時才驗證（RP-INV-02、RP-PUB-03）。
 */
class ReviewProjectScoringInputTest {

    private static final EnumSet<MedicalRecordType> OUTPATIENT = EnumSet.of(MedicalRecordType.OUTPATIENT);

    private static ReviewProject create(BigDecimal threshold, BigDecimal initial, BigDecimal rereview) {
        return ReviewProject.create("p1", "h1", "門診品質審查", null, OUTPATIENT, threshold, initial, rereview);
    }

    private static BigDecimal d(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    // --- 建立 ---

    @Test
    void createStoresGivenThresholdAndWeights() {
        ProjectVersion v = create(d("7"), d("0.70"), d("0.30")).getCurrentVersion();
        assertEquals(0, d("7").compareTo(v.getQualityThreshold()));
        assertEquals(0, d("0.70").compareTo(v.getInitialReviewWeight()));
        assertEquals(0, d("0.30").compareTo(v.getRereviewWeight()));
    }

    @Test
    void createWithoutScoringInputsKeepsTheDefaults() {
        ProjectVersion v = create(null, null, null).getCurrentVersion();
        assertNull(v.getQualityThreshold());
        assertEquals(0, d("0.50").compareTo(v.getInitialReviewWeight()));
        assertEquals(0, d("0.50").compareTo(v.getRereviewWeight()));
    }

    @Test
    void weightNotGivenIsHalfEvenWhenTheOtherIsGiven() {
        // RP-CRT-S4：沒有輸入的權重是 0.50。相加 1.20 在建立時不驗證
        ProjectVersion v = create(null, d("0.70"), null).getCurrentVersion();
        assertEquals(0, d("0.70").compareTo(v.getInitialReviewWeight()));
        assertEquals(0, d("0.50").compareTo(v.getRereviewWeight()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "11", "5.5", "-1"})
    void createRejectsAThresholdThatIsNotAnIntegerFromOneToTen(String threshold) {
        DomainValidationException e = assertThrows(DomainValidationException.class,
                () -> create(d(threshold), null, null));
        assertEquals("VALIDATION_ERROR", e.getErrorCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "10", "7.0"})
    void createAcceptsIntegerThresholdsFromOneToTen(String threshold) {
        assertDoesNotThrow(() -> create(d(threshold), null, null));
    }

    // --- 更新草稿 ---

    @Test
    void updateReplacesThresholdAndWeightsAsAWhole() {
        ReviewProject p = create(d("5"), d("0.70"), d("0.30"));
        p.updateDraft("住院品質審查", null, OUTPATIENT, null, null, d("0.40"));
        ProjectVersion v = p.getCurrentVersion();
        assertNull(v.getQualityThreshold(), "沒給的品質線變成 null（RP-UPD-S1）");
        assertEquals(0, d("0.50").compareTo(v.getInitialReviewWeight()), "沒給的權重回到 0.50（RP-UPD-S3）");
        assertEquals(0, d("0.40").compareTo(v.getRereviewWeight()));
        assertEquals(1, v.getVersionNumber(), "RP-UPD-S2");
        assertFalse(v.isPublished(), "RP-UPD-S2");
    }

    @Test
    void updateOfAPublishedVersionIsRejectedAndKeepsItsScoringInputs() {
        // 已發布但尚未啟用時狀態仍是 DRAFT；版本內容（含品質線與權重）仍不可改（RP-UPD-01、RP-INV-01、RP-INV-03）
        ReviewProject p = create(d("5"), d("0.70"), d("0.30"));
        p.publishNewVersion();
        assertEquals(ProjectStatus.DRAFT, p.getStatus());

        com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException e = assertThrows(
                com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException.class,
                () -> p.updateDraft("改名", null, OUTPATIENT, null, null, null));
        assertEquals("INVALID_STATE_TRANSITION", e.getErrorCode());
        ProjectVersion v = p.getCurrentVersion();
        assertTrue(v.isPublished());
        assertEquals(0, d("5").compareTo(v.getQualityThreshold()));
        assertEquals(0, d("0.70").compareTo(v.getInitialReviewWeight()));
        assertEquals(0, d("0.30").compareTo(v.getRereviewWeight()));
        assertEquals("門診品質審查", p.getName());
    }

    @Test
    void updateRejectsAnOutOfRangeThresholdAndChangesNothing() {
        ReviewProject p = create(d("5"), null, null);
        DomainValidationException e = assertThrows(DomainValidationException.class,
                () -> p.updateDraft("改名", null, OUTPATIENT, d("11"), null, null));
        assertEquals("VALIDATION_ERROR", e.getErrorCode());
        assertEquals("門診品質審查", p.getName());
        assertEquals(0, d("5").compareTo(p.getCurrentVersion().getQualityThreshold()));
    }
}
