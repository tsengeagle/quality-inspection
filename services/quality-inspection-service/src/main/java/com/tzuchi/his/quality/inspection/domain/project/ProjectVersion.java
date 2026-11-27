package com.tzuchi.his.quality.inspection.domain.project;

import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Immutable version snapshot for a ReviewProject.
 * Carries scoring configuration: qualityThreshold and review weights.
 */
public class ProjectVersion {

    private static final BigDecimal DEFAULT_WEIGHT = new BigDecimal("0.5");

    private final int versionNumber;
    private final boolean published;
    private final BigDecimal qualityThreshold;
    private final BigDecimal initialReviewWeight;
    private final BigDecimal rereviewWeight;

    public ProjectVersion(int versionNumber, boolean published) {
        this(versionNumber, published, null, DEFAULT_WEIGHT, DEFAULT_WEIGHT);
    }

    public ProjectVersion(int versionNumber, boolean published,
                          BigDecimal qualityThreshold,
                          BigDecimal initialReviewWeight,
                          BigDecimal rereviewWeight) {
        if (versionNumber < 1) {
            // 版本號不來自使用者輸入（常數 1、既有的版本、資料庫讀回的值），小於 1 是內部狀態出錯，不是輸入錯誤。
            throw new IllegalStateException("Version number must be >= 1");
        }
        this.versionNumber = versionNumber;
        this.published = published;
        this.qualityThreshold = qualityThreshold;
        this.initialReviewWeight = initialReviewWeight != null ? initialReviewWeight : DEFAULT_WEIGHT;
        this.rereviewWeight = rereviewWeight != null ? rereviewWeight : DEFAULT_WEIGHT;
    }

    /**
     * Validate that weights sum to 1.0 and each is > 0 and <= 1 (D-020).
     * Called before publishing a version (RP-PUB-03); drafts may hold other values (D-047).
     */
    public void validateWeightsForPublish() {
        if (initialReviewWeight.compareTo(BigDecimal.ZERO) <= 0
                || initialReviewWeight.compareTo(BigDecimal.ONE) > 0) {
            throw notPublishable("initialReviewWeight must be > 0 and <= 1, got: " + initialReviewWeight);
        }
        if (rereviewWeight.compareTo(BigDecimal.ZERO) <= 0
                || rereviewWeight.compareTo(BigDecimal.ONE) > 0) {
            throw notPublishable("rereviewWeight must be > 0 and <= 1, got: " + rereviewWeight);
        }
        if (initialReviewWeight.add(rereviewWeight).compareTo(BigDecimal.ONE) != 0) {
            throw notPublishable("Weights must sum to 1.0, got: " + initialReviewWeight + " + " + rereviewWeight);
        }
    }

    private static DomainRuleViolationException notPublishable(String message) {
        return new DomainRuleViolationException("PROJECT_VERSION_NOT_PUBLISHABLE", message);
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public boolean isPublished() {
        return published;
    }

    public BigDecimal getQualityThreshold() {
        return qualityThreshold;
    }

    public BigDecimal getInitialReviewWeight() {
        return initialReviewWeight;
    }

    public BigDecimal getRereviewWeight() {
        return rereviewWeight;
    }

    public ProjectVersion publish() {
        return new ProjectVersion(this.versionNumber, true,
                this.qualityThreshold, this.initialReviewWeight, this.rereviewWeight);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProjectVersion that = (ProjectVersion) o;
        return versionNumber == that.versionNumber && published == that.published;
    }

    @Override
    public int hashCode() {
        return Objects.hash(versionNumber, published);
    }

    @Override
    public String toString() {
        return "v" + versionNumber + (published ? " (published)" : " (draft)");
    }
}
