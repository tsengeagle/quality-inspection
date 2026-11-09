package com.tzuchi.his.quality.inspection.application.reviewcase.query;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CaseDetailMetadataView extends ReviewCaseDetailView {
    private final Map<String, Object> metadata;

    public CaseDetailMetadataView(String caseId, String caseStatus, String projectName,
                                  String hospitalId, String reviewerId, String rereviewerId) {
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
        // updatedAt and latestRevisionSummary value contracts/sources remain UNKNOWN.
        fields.put("updatedAt", null);
        fields.put("latestRevisionSummary", null);
        this.metadata = Collections.unmodifiableMap(fields);
    }

    @JsonValue
    public Map<String, Object> metadata() {
        return metadata;
    }
}
