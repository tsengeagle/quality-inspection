package com.tzuchi.his.quality.inspection.infrastructure.persistence.sampling;

import com.tzuchi.his.quality.inspection.domain.sampling.BatchStatus;
import com.tzuchi.his.quality.inspection.domain.sampling.ExecutionMode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** 測試用：在同一個 package 建立建構子為 protected 的抽選 entity。 */
public final class SamplingEntityFixtures {

    private SamplingEntityFixtures() {}

    public static SamplingBatchEntity completedBatchWithOneSelectedCase(String batchId, String projectId) {
        SelectedCaseRefEntity selected = new SelectedCaseRefEntity();
        selected.setSourceCaseKey("SK-1");
        selected.setHospitalId("H001");
        selected.setMedicalRecordType("OUTPATIENT");
        SamplingBatchEntity batch = new SamplingBatchEntity();
        batch.setBatchId(batchId);
        batch.setProjectId(projectId);
        batch.setExecutionMode(ExecutionMode.MANUAL);
        batch.setStatus(BatchStatus.COMPLETED);
        batch.setStartedAt(Instant.parse("2026-01-01T00:00:00Z"));
        batch.setSelectedCases(new ArrayList<>(List.of(selected)));
        return batch;
    }
}
