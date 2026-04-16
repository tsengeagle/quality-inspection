package com.tzuchi.his.quality.inspection.application.reviewcase.query;

import java.util.Optional;

/** Read-model query port for UI-011 assignment snapshot data. */
public interface ReviewCaseAssignmentQueryPort {
    Optional<AssignmentCaseSnapshotView> findAssignmentSnapshotById(String caseId);
}
