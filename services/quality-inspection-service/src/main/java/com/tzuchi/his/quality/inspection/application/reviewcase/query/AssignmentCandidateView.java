package com.tzuchi.his.quality.inspection.application.reviewcase.query;

/** Read model for a UI-011 candidate option. */
public class AssignmentCandidateView {

    private final String userId;
    private final String displayName;
    private final String displayLabel;
    private final String role;
    private final boolean active;
    private final boolean selectable;
    private final String blockedReason;

    public AssignmentCandidateView(String userId, String displayName,
                                   String displayLabel, String role,
                                   boolean active, boolean selectable,
                                   String blockedReason) {
        this.userId = userId;
        this.displayName = displayName;
        this.displayLabel = displayLabel;
        this.role = role;
        this.active = active;
        this.selectable = selectable;
        this.blockedReason = blockedReason;
    }

    public String getUserId() { return userId; }
    public String getDisplayName() { return displayName; }
    public String getDisplayLabel() { return displayLabel; }
    public String getRole() { return role; }
    public boolean isActive() { return active; }
    public boolean isSelectable() { return selectable; }
    public String getBlockedReason() { return blockedReason; }
}
