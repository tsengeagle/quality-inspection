package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import java.time.OffsetDateTime;

public class ReassignReviewerRequest {

    private String reviewerId;

    /** D-073：必須帶時區；不帶時區的值讀不進來，回 400 VALIDATION_ERROR。 */
    private OffsetDateTime dueAt;

    public String getReviewerId() { return reviewerId; }
    public void setReviewerId(String reviewerId) { this.reviewerId = reviewerId; }

    public OffsetDateTime getDueAt() { return dueAt; }
    public void setDueAt(OffsetDateTime dueAt) { this.dueAt = dueAt; }
}
