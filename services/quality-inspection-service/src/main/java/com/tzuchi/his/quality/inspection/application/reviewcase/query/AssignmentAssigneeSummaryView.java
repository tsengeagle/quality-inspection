package com.tzuchi.his.quality.inspection.application.reviewcase.query;

import java.time.LocalDateTime;

/** Read model for current assignee summary on UI-011. */
public class AssignmentAssigneeSummaryView {

    private final String userId;
    private final String displayName;
    private final String displayLabel;
    private final LocalDateTime assignedAt;

    public AssignmentAssigneeSummaryView(String userId, String displayName,
                                         String displayLabel,
                                         LocalDateTime assignedAt) {
        this.userId = userId;
        this.displayName = displayName;
        this.displayLabel = displayLabel;
        this.assignedAt = assignedAt;
    }

    public String getUserId() { return userId; }
    public String getDisplayName() { return displayName; }
    public String getDisplayLabel() { return displayLabel; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
}
