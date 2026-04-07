package com.tzuchi.his.quality.inspection.infrastructure.persistence.sampling;

import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.sampling.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class SamplingBatchRepositoryImpl implements SamplingBatchRepository {

    private final SamplingBatchJpaRepository jpa;

    public SamplingBatchRepositoryImpl(SamplingBatchJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(SamplingBatch batch) {
        jpa.save(toEntity(batch));
    }

    @Override
    public Optional<SamplingBatch> findById(String batchId) {
        return jpa.findById(batchId).map(this::toDomain);
    }

    private SamplingBatchEntity toEntity(SamplingBatch b) {
        SamplingBatchEntity e = new SamplingBatchEntity();
        e.setBatchId(b.getBatchId());
        e.setProjectId(b.getProjectId());
        e.setExecutionMode(b.getExecutionMode());
        e.setStartedAt(b.getStartedAt());
        e.setFinishedAt(b.getFinishedAt());
        e.setStatus(b.getStatus());
        e.setPopulationCount(b.getPopulationCount());
        e.setEligibleCount(b.getEligibleCount());
        e.setExcludedCount(b.getExcludedCount());
        e.setSelectedCount(b.getSelectedCount());
        e.setRuleSnapshotProject(b.getRuleSnapshot().getProjectVersionInfo());
        e.setRuleSnapshotPolicy(b.getRuleSnapshot().getSamplingPolicyInfo());
        e.setRetryOfBatchId(b.getRetryOfBatchId());
        e.setSelectedCases(
                b.getSelectedCases().stream().map(ref -> {
                    SelectedCaseRefEntity re = new SelectedCaseRefEntity();
                    re.setSourceCaseKey(ref.getSourceCaseKey());
                    re.setHospitalId(ref.getHospitalId());
                    re.setMedicalRecordType(ref.getMedicalRecordType().name());
                    return re;
                }).collect(Collectors.toList())
        );
        return e;
    }

    private SamplingBatch toDomain(SamplingBatchEntity e) {
        RuleSnapshot snapshot = new RuleSnapshot(e.getRuleSnapshotProject(), e.getRuleSnapshotPolicy());
        SamplingBatch batch = SamplingBatch.start(e.getBatchId(), e.getProjectId(),
                e.getExecutionMode(), snapshot);

        if (e.getRetryOfBatchId() != null && e.getExecutionMode() == ExecutionMode.RETRY) {
            batch.markRetryOf(e.getRetryOfBatchId());
        }

        if (e.getPopulationCount() > 0 || e.getEligibleCount() > 0) {
            batch.recordCandidateCount(e.getPopulationCount(), e.getEligibleCount(), e.getExcludedCount());
        }

        if (!e.getSelectedCases().isEmpty()) {
            List<SelectedCaseRef> refs = e.getSelectedCases().stream()
                    .map(re -> new SelectedCaseRef(
                            re.getSourceCaseKey(), re.getHospitalId(),
                            MedicalRecordType.valueOf(re.getMedicalRecordType())))
                    .collect(Collectors.toList());
            batch.selectCases(refs);
        }

        if (e.getStatus() == BatchStatus.COMPLETED) {
            batch.complete();
        } else if (e.getStatus() == BatchStatus.FAILED) {
            batch.fail("reconstituted");
        }

        return batch;
    }
}
