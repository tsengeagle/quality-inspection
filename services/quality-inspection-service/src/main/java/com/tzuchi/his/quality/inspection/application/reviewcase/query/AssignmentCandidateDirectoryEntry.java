package com.tzuchi.his.quality.inspection.application.reviewcase.query;

/** Directory entry from the assignment candidate source. */
public class AssignmentCandidateDirectoryEntry {

    private final String userId;
    private final String displayName;
    private final String displayLabel;
    private final AssignmentCandidateRole role;
    private final boolean active;

    public AssignmentCandidateDirectoryEntry(String userId, String displayName,
                                             String displayLabel,
                                             AssignmentCandidateRole role,
                                             boolean active) {
        this.userId = userId;
        this.displayName = displayName;
        this.displayLabel = displayLabel;
        this.role = role;
        this.active = active;
    }

    public String getUserId() { return userId; }
    public String getDisplayName() { return displayName; }
    public String getDisplayLabel() { return displayLabel; }
    public AssignmentCandidateRole getRole() { return role; }
    public boolean isActive() { return active; }
}
