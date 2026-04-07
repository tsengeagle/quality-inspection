package com.tzuchi.his.quality.inspection.application.sampling;

/**
 * Result of a sampling preview — population estimation without batch creation.
 * sourceCaseKeys is intentionally excluded (api-spec.md §9.1 — do not expose individual keys).
 */
public class SamplingPreviewResult {

    private final int populationCount;
    private final int eligibleCount;
    private final int excludedCount;

    public SamplingPreviewResult(int populationCount, int eligibleCount, int excludedCount) {
        this.populationCount = populationCount;
        this.eligibleCount = eligibleCount;
        this.excludedCount = excludedCount;
    }

    public int getPopulationCount() { return populationCount; }
    public int getEligibleCount() { return eligibleCount; }
    public int getExcludedCount() { return excludedCount; }
}
