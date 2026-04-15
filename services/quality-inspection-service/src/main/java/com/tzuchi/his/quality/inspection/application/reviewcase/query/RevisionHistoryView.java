package com.tzuchi.his.quality.inspection.application.reviewcase.query;

import java.util.List;

/** Read model for the full revision history of a review case. */
public class RevisionHistoryView {

    private final String caseId;
    private final List<RevisionHistoryEntry> entries;
    private final boolean hasExceptionEntry;

    public RevisionHistoryView(String caseId, List<RevisionHistoryEntry> entries,
                               boolean hasExceptionEntry) {
        this.caseId = caseId;
        this.entries = entries;
        this.hasExceptionEntry = hasExceptionEntry;
    }

    public String getCaseId() { return caseId; }
    public List<RevisionHistoryEntry> getEntries() { return entries; }
    public boolean isHasExceptionEntry() { return hasExceptionEntry; }
}
