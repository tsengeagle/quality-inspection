package com.tzuchi.his.quality.inspection.application.reviewcase.query;

import java.util.List;
import java.util.Optional;

/** Query port for reviewer and rereviewer candidate directory lookups. */
public interface AssignmentCandidateDirectoryPort {
    List<AssignmentCandidateDirectoryEntry> findCandidates(String hospitalId,
                                                           AssignmentCandidateRole role,
                                                           String keyword);

    Optional<AssignmentCandidateDirectoryEntry> findByUserId(String hospitalId, String userId);
}
