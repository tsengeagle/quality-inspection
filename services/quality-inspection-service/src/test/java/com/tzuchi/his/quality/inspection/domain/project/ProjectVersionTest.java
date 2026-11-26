package com.tzuchi.his.quality.inspection.domain.project;

import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;

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
    void validateWeights_sumNotOne_shouldThrow() {
        ProjectVersion pv = new ProjectVersion(1, false,
                new BigDecimal("80.00"),
                new BigDecimal("0.6"),
                new BigDecimal("0.6"));
        assertEquals("PROJECT_VERSION_NOT_PUBLISHABLE",
                assertThrows(DomainRuleViolationException.class, pv::validateWeightsForPublish).getErrorCode());
    }

    @Test
    void validateWeights_zeroWeight_shouldThrow() {
        ProjectVersion pv = new ProjectVersion(1, false,
                new BigDecimal("80.00"),
                BigDecimal.ZERO,
                new BigDecimal("1.0"));
        assertEquals("PROJECT_VERSION_NOT_PUBLISHABLE",
                assertThrows(DomainRuleViolationException.class, pv::validateWeightsForPublish).getErrorCode());
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
