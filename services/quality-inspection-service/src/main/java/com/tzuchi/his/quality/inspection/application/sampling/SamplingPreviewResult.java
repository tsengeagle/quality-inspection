package com.tzuchi.his.quality.inspection.application.sampling;

import java.util.List;

/**
 * Result of a sampling preview — population estimation without batch creation.
 */
public class SamplingPreviewResult {

    private final int populationCount;
    private final int eligibleCount;
    private final int excludedCount;
    private final List<String> sourceCaseKeys;

    public SamplingPreviewResult(int populationCount, int eligibleCount,
                                  int excludedCount, List<String> sourceCaseKeys) {
        this.populationCount = populationCount;
        this.eligibleCount = eligibleCount;
        this.excludedCount = excludedCount;
        this.sourceCaseKeys = sourceCaseKeys;
    }

    public int getPopulationCount() { return populationCount; }
    public int getEligibleCount() { return eligibleCount; }
    public int getExcludedCount() { return excludedCount; }
    public List<String> getSourceCaseKeys() { return sourceCaseKeys; }
}
