package com.tzuchi.his.quality.inspection.domain.sampling;

import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;

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
    private String failureReason;

    private List<SelectedCaseRef> selectedCases;

    protected SamplingBatch() {
        // for persistence
    }

    private SamplingBatch(String batchId, String projectId, ExecutionMode executionMode,
                          RuleSnapshot ruleSnapshot, Instant startedAt) {
        Objects.requireNonNull(startedAt, "startedAt must not be null");
        Objects.requireNonNull(batchId, "batchId must not be null");
        Objects.requireNonNull(projectId, "projectId must not be null");
        Objects.requireNonNull(executionMode, "executionMode must not be null");
        Objects.requireNonNull(ruleSnapshot, "ruleSnapshot must not be null");
        this.batchId = batchId;
        this.projectId = projectId;
        this.executionMode = executionMode;
        this.ruleSnapshot = ruleSnapshot;
        this.startedAt = startedAt;
        this.status = BatchStatus.STARTED;
        this.selectedCases = new ArrayList<>();
    }

    public static SamplingBatch start(String batchId, String projectId,
                                       ExecutionMode executionMode, RuleSnapshot ruleSnapshot,
                                       Instant startedAt) {
        return new SamplingBatch(batchId, projectId, executionMode, ruleSnapshot, startedAt);
    }

    /**
     * Reconstitute a SamplingBatch from persisted state.
     * FOR INFRASTRUCTURE USE ONLY — do not call from domain or application code.
     */
    public static SamplingBatch reconstitute(String batchId, String projectId,
                                              ExecutionMode executionMode, RuleSnapshot ruleSnapshot,
                                              BatchStatus status, Instant startedAt, Instant finishedAt,
                                              int populationCount, int eligibleCount, int excludedCount,
                                              int selectedCount, String retryOfBatchId,
                                              String failureReason,
                                              List<SelectedCaseRef> selectedCases) {
        SamplingBatch batch = new SamplingBatch();
        batch.batchId = batchId;
        batch.projectId = projectId;
        batch.executionMode = executionMode;
        batch.ruleSnapshot = ruleSnapshot;
        batch.status = status;
        batch.startedAt = startedAt;
        batch.finishedAt = finishedAt;
        batch.populationCount = populationCount;
        batch.eligibleCount = eligibleCount;
        batch.excludedCount = excludedCount;
        batch.selectedCount = selectedCount;
        batch.retryOfBatchId = retryOfBatchId;
        batch.failureReason = failureReason;
        batch.selectedCases = selectedCases != null ? new ArrayList<>(selectedCases) : new ArrayList<>();
        return batch;
    }

    public void markRetryOf(String previousBatchId) {
        Objects.requireNonNull(previousBatchId, "previousBatchId must not be null");
        if (this.executionMode != ExecutionMode.RETRY) {
            throw new DomainStateConflictException("INVALID_STATE_TRANSITION",
                    "markRetryOf only valid for RETRY mode");
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
                throw new DomainStateConflictException("DUPLICATE_SELECTED_CASE_IN_BATCH",
                        "Duplicate sourceCaseKey in same batch: " + ref.getSourceCaseKey());
            }
        }

        for (SelectedCaseRef existing : this.selectedCases) {
            if (seenKeys.contains(existing.getSourceCaseKey())) {
                throw new DomainStateConflictException("DUPLICATE_SELECTED_CASE_IN_BATCH",
                        "Duplicate sourceCaseKey in same batch: " + existing.getSourceCaseKey());
            }
        }

        this.selectedCases.addAll(cases);
        this.selectedCount = this.selectedCases.size();
        this.status = BatchStatus.SAMPLING;
    }

    public void complete(Instant finishedAt) {
        Objects.requireNonNull(finishedAt, "finishedAt must not be null");
        requireNotCompleted();
        if (selectedCount > eligibleCount) {
            throw new DomainStateConflictException("INVALID_STATE_TRANSITION",
                    "selectedCount exceeds eligibleCount");
        }
        this.status = BatchStatus.COMPLETED;
        this.finishedAt = finishedAt;
    }

    public void fail(String reason, Instant finishedAt) {
        Objects.requireNonNull(finishedAt, "finishedAt must not be null");
        if (this.status == BatchStatus.COMPLETED) {
            throw new DomainStateConflictException("INVALID_STATE_TRANSITION",
                    "Cannot fail a completed batch");
        }
        this.status = BatchStatus.FAILED;
        this.failureReason = reason;
        this.finishedAt = finishedAt;
    }

    private void requireNotCompleted() {
        if (this.status == BatchStatus.COMPLETED) {
            throw new DomainStateConflictException("INVALID_STATE_TRANSITION",
                    "Batch is already completed and immutable");
        }
        if (this.status == BatchStatus.FAILED) {
            throw new DomainStateConflictException("INVALID_STATE_TRANSITION",
                    "Batch has failed and cannot be modified");
        }
    }

    private void requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new DomainRuleViolationException("INVALID_METRIC_VALUE",
                    name + " must not be negative");
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
    public String getFailureReason() { return failureReason; }
    public List<SelectedCaseRef> getSelectedCases() { return Collections.unmodifiableList(selectedCases); }
}
