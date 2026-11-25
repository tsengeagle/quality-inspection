package com.tzuchi.his.quality.inspection.infrastructure;

import com.tzuchi.his.quality.inspection.application.sampling.CandidateSourcePort;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.sampling.SelectedCaseRef;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Stub implementation of CandidateSourcePort for local development.
 * In production, this would be replaced by an adapter calling HIS or external data sources.
 * This is an adapter-level stub/mock provider (upstream-layer substitute).
 */
@Component
public class StubCandidateSourceAdapter implements CandidateSourcePort {

    /** D-086 第 5 點：替身的母體每次回傳 20 筆。 */
    static final int CANDIDATES_PER_QUERY = 20;

    /**
     * 病歷的流水號。從啟動時間開始往上加，所以每次回傳的 sourceCaseKey 都與先前回傳的不同，
     * 重新啟動之後也不會重複（D-086 第 5 點：多次抽選要看得出效果）。
     */
    private final AtomicLong sequence = new AtomicLong(System.currentTimeMillis());

    @Override
    public List<SelectedCaseRef> queryCandidates(String projectId, String hospitalId,
                                                   List<MedicalRecordType> types) {
        List<SelectedCaseRef> candidates = new ArrayList<>();
        for (int i = 1; i <= CANDIDATES_PER_QUERY; i++) {
            MedicalRecordType type = types.isEmpty()
                    ? MedicalRecordType.OUTPATIENT
                    : types.get(i % types.size());
            candidates.add(new SelectedCaseRef(
                    "CASE-" + projectId + "-" + Long.toString(sequence.incrementAndGet(), 36).toUpperCase(),
                    hospitalId,
                    type
            ));
        }
        return candidates;
    }
}
