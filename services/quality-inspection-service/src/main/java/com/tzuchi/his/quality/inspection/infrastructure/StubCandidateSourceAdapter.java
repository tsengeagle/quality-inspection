package com.tzuchi.his.quality.inspection.infrastructure;

import com.tzuchi.his.quality.inspection.application.sampling.CandidateSourcePort;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.sampling.SelectedCaseRef;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Stub implementation of CandidateSourcePort for local development.
 * In production, this would be replaced by an adapter calling HIS or external data sources.
 * This is an adapter-level stub/mock provider (upstream-layer substitute).
 */
@Component
public class StubCandidateSourceAdapter implements CandidateSourcePort {

    @Override
    public List<SelectedCaseRef> queryCandidates(String projectId, String hospitalId,
                                                   List<MedicalRecordType> types) {
        // Return sample candidates for development/testing
        List<SelectedCaseRef> candidates = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            MedicalRecordType type = types.isEmpty()
                    ? MedicalRecordType.OUTPATIENT
                    : types.get(i % types.size());
            candidates.add(new SelectedCaseRef(
                    "CASE-" + projectId + "-" + i,
                    hospitalId,
                    type
            ));
        }
        return candidates;
    }
}
