package com.tzuchi.his.quality.inspection.domain.project;

import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 發布的前置檢查（specs/review-project 的 RP-PUB-02、RP-PUB-04；D-041、D-046）：
 * 尚未綁定評分表、或品質線為 null，一律 PROJECT_VERSION_NOT_PUBLISHABLE，版本維持未發布。
 */
class ReviewProjectPublishTest {

    private static ReviewProject draft(BigDecimal threshold) {
        return ReviewProject.create("p1", "h1", "門診", null, EnumSet.of(MedicalRecordType.OUTPATIENT),
                threshold, null, null);
    }

    private static void assertNotPublishable(ReviewProject p) {
        DomainRuleViolationException e = assertThrows(DomainRuleViolationException.class, p::publishNewVersion);
        assertEquals("PROJECT_VERSION_NOT_PUBLISHABLE", e.getErrorCode());
        assertFalse(p.getCurrentVersion().isPublished());
    }

    @Test
    void publishRequiresABoundScoreSheet() {
        assertNotPublishable(draft(new BigDecimal("5")));
    }

    @Test
    void publishRequiresAQualityThreshold() {
        ReviewProject p = draft(null);
        p.bindScoreSheet("ss-1");
        assertNotPublishable(p);
    }

    @Test
    void boundProjectWithThresholdAndDefaultWeightsCanBePublished() {
        ReviewProject p = draft(new BigDecimal("5"));
        p.bindScoreSheet("ss-1");
        p.publishNewVersion();
        assertTrue(p.getCurrentVersion().isPublished());
    }
}
