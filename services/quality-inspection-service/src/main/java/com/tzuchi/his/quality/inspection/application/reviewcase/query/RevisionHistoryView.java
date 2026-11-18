package com.tzuchi.his.quality.inspection.application.reviewcase.query;

import java.util.List;

/** Read model for the full revision history of a review case. */
public class RevisionHistoryView {

    private final String caseId;
    private final List<RevisionHistoryEntry> entries;

    public RevisionHistoryView(String caseId, List<RevisionHistoryEntry> entries) {
        this.caseId = caseId;
        this.entries = entries;
    }

    public String getCaseId() { return caseId; }
    public List<RevisionHistoryEntry> getEntries() { return entries; }
}
