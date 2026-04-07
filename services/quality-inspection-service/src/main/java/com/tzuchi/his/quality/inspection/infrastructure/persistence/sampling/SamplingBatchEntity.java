package com.tzuchi.his.quality.inspection.infrastructure.persistence.sampling;

import com.tzuchi.his.quality.inspection.domain.sampling.BatchStatus;
import com.tzuchi.his.quality.inspection.domain.sampling.ExecutionMode;

import javax.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sampling_batch")
public class SamplingBatchEntity {

    @Id
    @Column(name = "batch_id")
    private String batchId;

    @Column(name = "project_id", nullable = false)
    private String projectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_mode", nullable = false)
    private ExecutionMode executionMode;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BatchStatus status;

    @Column(name = "population_count")
    private int populationCount;

    @Column(name = "eligible_count")
    private int eligibleCount;

    @Column(name = "excluded_count")
    private int excludedCount;

    @Column(name = "selected_count")
    private int selectedCount;

    @Column(name = "rule_snapshot_project")
    private String ruleSnapshotProject;

    @Column(name = "rule_snapshot_policy")
    private String ruleSnapshotPolicy;

    @Column(name = "retry_of_batch_id")
    private String retryOfBatchId;

    @Column(name = "failure_reason")
    private String failureReason;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "batch_id")
    private List<SelectedCaseRefEntity> selectedCases = new ArrayList<>();

    protected SamplingBatchEntity() {}

    // --- Getters and Setters ---

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public ExecutionMode getExecutionMode() { return executionMode; }
    public void setExecutionMode(ExecutionMode executionMode) { this.executionMode = executionMode; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Instant finishedAt) { this.finishedAt = finishedAt; }

    public BatchStatus getStatus() { return status; }
    public void setStatus(BatchStatus status) { this.status = status; }

    public int getPopulationCount() { return populationCount; }
    public void setPopulationCount(int populationCount) { this.populationCount = populationCount; }

    public int getEligibleCount() { return eligibleCount; }
    public void setEligibleCount(int eligibleCount) { this.eligibleCount = eligibleCount; }

    public int getExcludedCount() { return excludedCount; }
    public void setExcludedCount(int excludedCount) { this.excludedCount = excludedCount; }

    public int getSelectedCount() { return selectedCount; }
    public void setSelectedCount(int selectedCount) { this.selectedCount = selectedCount; }

    public String getRuleSnapshotProject() { return ruleSnapshotProject; }
    public void setRuleSnapshotProject(String s) { this.ruleSnapshotProject = s; }

    public String getRuleSnapshotPolicy() { return ruleSnapshotPolicy; }
    public void setRuleSnapshotPolicy(String s) { this.ruleSnapshotPolicy = s; }

    public String getRetryOfBatchId() { return retryOfBatchId; }
    public void setRetryOfBatchId(String retryOfBatchId) { this.retryOfBatchId = retryOfBatchId; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public List<SelectedCaseRefEntity> getSelectedCases() { return selectedCases; }
    public void setSelectedCases(List<SelectedCaseRefEntity> selectedCases) { this.selectedCases = selectedCases; }
}
