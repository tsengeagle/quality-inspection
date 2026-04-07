package com.tzuchi.his.quality.inspection.domain.sampling;

import java.time.Instant;
import java.util.*;

/**
 * Aggregate root for SamplingBatch.
 * <p>
 * Represents a single sampling execution. Records metrics, rule snapshot,
 * and selected case references.
 * <p>
 * Invariants:
 * - Completed batch is immutable.
 * - Metrics must be non-negative.
 * - selectedCount <= eligibleCount.
 * - No duplicate sourceCaseKey within the same batch.
 * - Retry must set retryOfBatchId; does not overwrite the failed batch.
 */
public class SamplingBatch {

    private String batchId;
    private String projectId;
    private ExecutionMode executionMode;
    private Instant startedAt;
    private Instant finishedAt;
    private BatchStatus status;

    private int populationCount;
    private int eligibleCount;
    private int excludedCount;
    private int selectedCount;

    private RuleSnapshot ruleSnapshot;
    private String retryOfBatchId;

    private List<SelectedCaseRef> selectedCases;

    protected SamplingBatch() {
        // for persistence
    }

    private SamplingBatch(String batchId, String projectId, ExecutionMode executionMode,
                          RuleSnapshot ruleSnapshot) {
        Objects.requireNonNull(batchId, "batchId must not be null");
        Objects.requireNonNull(projectId, "projectId must not be null");
        Objects.requireNonNull(executionMode, "executionMode must not be null");
        Objects.requireNonNull(ruleSnapshot, "ruleSnapshot must not be null");
        this.batchId = batchId;
        this.projectId = projectId;
        this.executionMode = executionMode;
        this.ruleSnapshot = ruleSnapshot;
        this.startedAt = Instant.now();
        this.status = BatchStatus.STARTED;
        this.selectedCases = new ArrayList<>();
    }

    public static SamplingBatch start(String batchId, String projectId,
                                       ExecutionMode executionMode, RuleSnapshot ruleSnapshot) {
        return new SamplingBatch(batchId, projectId, executionMode, ruleSnapshot);
    }

    public void markRetryOf(String previousBatchId) {
        Objects.requireNonNull(previousBatchId, "previousBatchId must not be null");
        if (this.executionMode != ExecutionMode.RETRY) {
            throw new IllegalStateException("markRetryOf only valid for RETRY mode");
        }
        this.retryOfBatchId = previousBatchId;
    }

    public void recordCandidateCount(int populationCount, int eligibleCount, int excludedCount) {
        requireNotCompleted();
        requireNonNegative(populationCount, "populationCount");
        requireNonNegative(eligibleCount, "eligibleCount");
        requireNonNegative(excludedCount, "excludedCount");
        this.populationCount = populationCount;
        this.eligibleCount = eligibleCount;
        this.excludedCount = excludedCount;
        this.status = BatchStatus.COLLECTING_CANDIDATES;
    }

    public void selectCases(List<SelectedCaseRef> cases) {
        requireNotCompleted();
        Objects.requireNonNull(cases, "cases must not be null");

        Set<String> seenKeys = new HashSet<>();
        for (SelectedCaseRef ref : cases) {
            if (!seenKeys.add(ref.getSourceCaseKey())) {
                throw new IllegalArgumentException(
                        "Duplicate sourceCaseKey in same batch: " + ref.getSourceCaseKey());
            }
        }

        // Also check against already-selected cases
        for (SelectedCaseRef existing : this.selectedCases) {
            if (seenKeys.contains(existing.getSourceCaseKey())) {
                throw new IllegalArgumentException(
                        "Duplicate sourceCaseKey in same batch: " + existing.getSourceCaseKey());
            }
        }

        this.selectedCases.addAll(cases);
        this.selectedCount = this.selectedCases.size();
        this.status = BatchStatus.SAMPLING;
    }

    public void complete() {
        requireNotCompleted();
        if (selectedCount > eligibleCount) {
            throw new IllegalStateException("selectedCount exceeds eligibleCount");
        }
        this.status = BatchStatus.COMPLETED;
        this.finishedAt = Instant.now();
    }

    public void fail(String reason) {
        if (this.status == BatchStatus.COMPLETED) {
            throw new IllegalStateException("Cannot fail a completed batch");
        }
        this.status = BatchStatus.FAILED;
        this.finishedAt = Instant.now();
    }

    private void requireNotCompleted() {
        if (this.status == BatchStatus.COMPLETED) {
            throw new IllegalStateException("Batch is already completed and immutable");
        }
        if (this.status == BatchStatus.FAILED) {
            throw new IllegalStateException("Batch has failed and cannot be modified");
        }
    }

    private void requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
    }

    // --- Getters ---

    public String getBatchId() { return batchId; }
    public String getProjectId() { return projectId; }
    public ExecutionMode getExecutionMode() { return executionMode; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public BatchStatus getStatus() { return status; }
    public int getPopulationCount() { return populationCount; }
    public int getEligibleCount() { return eligibleCount; }
    public int getExcludedCount() { return excludedCount; }
    public int getSelectedCount() { return selectedCount; }
    public RuleSnapshot getRuleSnapshot() { return ruleSnapshot; }
    public String getRetryOfBatchId() { return retryOfBatchId; }
    public List<SelectedCaseRef> getSelectedCases() { return Collections.unmodifiableList(selectedCases); }
}
