package com.tzuchi.his.quality.inspection.domain.reviewcase;

/**
 * Status values for a ReviewCase.
 * Per state-machine.md §3.2-3.3.
 */
public enum CaseStatus {
    SELECTED_PENDING_ASSIGNMENT,
    PENDING_INITIAL_REVIEW,
    INITIAL_REVIEW_IN_PROGRESS,
    INITIAL_REVIEW_COMPLETED,
    RETURNED,
    PENDING_REREVIEW,
    REREVIEW_IN_PROGRESS,
    CLOSED,
    CANCELLED
}
