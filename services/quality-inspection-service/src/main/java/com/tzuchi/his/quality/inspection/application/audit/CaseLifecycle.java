package com.tzuchi.his.quality.inspection.application.audit;

import java.util.List;

/** D-071 第 6 點：生命週期查詢的輸出，以 entries 包住清單；HTTP 回應仍是清單本身。 */
public class CaseLifecycle {

    private final List<AuditTrailEntry> entries;

    public CaseLifecycle(List<AuditTrailEntry> entries) {
        this.entries = List.copyOf(entries);
    }

    public List<AuditTrailEntry> getEntries() { return entries; }
}
