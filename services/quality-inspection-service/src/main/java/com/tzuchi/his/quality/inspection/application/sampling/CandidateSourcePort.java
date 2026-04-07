package com.tzuchi.his.quality.inspection.application.sampling;

import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.sampling.SelectedCaseRef;

import java.util.List;

/**
 * Port for querying the medical record population.
 * In production, this would call HIS or an external data source.
 * For development, an adapter-level stub/mock provider is used.
 */
public interface CandidateSourcePort {

    /**
     * Query candidates for a project scope.
     * Returns case keys matching the project criteria.
     */
    List<SelectedCaseRef> queryCandidates(String projectId, String hospitalId,
                                           List<MedicalRecordType> types);
}
