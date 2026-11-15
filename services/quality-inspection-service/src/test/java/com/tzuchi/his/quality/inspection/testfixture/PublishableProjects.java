package com.tzuchi.his.quality.inspection.testfixture;

import com.tzuchi.his.quality.inspection.domain.project.ProjectVersion;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;

import java.math.BigDecimal;

/**
 * 測試用：讓一個草稿專案滿足發布的前置條件（D-041 已綁定評分表、D-046 有品質線），權重維持原值。
 * <p>
 * 只給「要發布、但測試的重點不在發布條件」的測試使用。測發布條件本身的測試不要用它。
 */
public final class PublishableProjects {

    public static final String SCORE_SHEET_ID = "ss-test";
    public static final BigDecimal QUALITY_THRESHOLD = new BigDecimal("5");

    private PublishableProjects() {
    }

    public static ReviewProject prepare(ReviewProject project) {
        if (project.getBoundScoreSheetId() == null) {
            project.bindScoreSheet(SCORE_SHEET_ID);
        }
        ProjectVersion version = project.getCurrentVersion();
        if (!version.isPublished() && version.getQualityThreshold() == null) {
            project.updateDraft(project.getName(), project.getDescription(), project.getMedicalRecordTypes(),
                    QUALITY_THRESHOLD, version.getInitialReviewWeight(), version.getRereviewWeight());
        }
        return project;
    }
}
