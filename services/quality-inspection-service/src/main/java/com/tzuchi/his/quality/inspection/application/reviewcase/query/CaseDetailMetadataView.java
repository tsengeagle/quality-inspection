package com.tzuchi.his.quality.inspection.application.reviewcase.query;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CaseDetailMetadataView extends ReviewCaseDetailView {
    private final Map<String, Object> metadata;

    public CaseDetailMetadataView(String caseId, String caseStatus, String projectName,
                                  String hospitalId, String reviewerId, String rereviewerId,
                                  java.time.Instant updatedAt) {
        super(caseId, null, null, hospitalId, caseStatus, reviewerId, rereviewerId,
                null, null, null, null, null, null, null, null, null, null,
                List.of(), null, "METADATA_ONLY", "SYSTEM_ADMIN", false);
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("caseId", caseId);
        fields.put("caseStatus", caseStatus);
        fields.put("projectName", projectName);
        fields.put("hospitalId", hospitalId);
        fields.put("reviewerId", reviewerId);
        fields.put("rereviewerId", rereviewerId);
        // D-037: persisted UTC case time as ISO 8601 (e.g. 2026-11-16T10:00:00Z); legacy cases stay null.
        fields.put("updatedAt", updatedAt == null ? null : updatedAt.toString());
        // latestRevisionSummary value contract remains UNKNOWN.
        fields.put("latestRevisionSummary", null);
        this.metadata = Collections.unmodifiableMap(fields);
    }

    @JsonValue
    public Map<String, Object> metadata() {
        return metadata;
    }
}
