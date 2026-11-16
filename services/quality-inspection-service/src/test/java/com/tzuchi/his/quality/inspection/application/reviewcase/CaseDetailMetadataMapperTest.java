package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tzuchi.his.quality.inspection.application.project.query.*;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.*;
import com.tzuchi.his.quality.inspection.domain.project.ProjectStatus;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CaseDetailMetadataMapperTest {
    private static final Set<String> ALLOWED = Set.of("caseId", "caseStatus", "projectName", "hospitalId",
            "reviewerId", "rereviewerId", "updatedAt", "latestRevisionSummary");

    @Test
    void fullViewAndFuturePropertiesAreNeverSerializedThroughMetadataProjection() {
        ReviewProjectQueryPort projects = mock(ReviewProjectQueryPort.class);
        when(projects.findSummaryById("p")).thenReturn(Optional.of(new ReviewProjectSummary(
                "p", "H001", "Project name", "secret description",
                java.util.EnumSet.of(com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType.OUTPATIENT),
                ProjectStatus.ACTIVE, "sheet", 1, true, null, null, null)));
        ReviewCaseDetailView raw = new ReviewCaseDetailView("c", "p", "batch", "H001", "CLOSED", "initial", "rr",
                "type", "secret source", java.math.BigDecimal.TEN, java.math.BigDecimal.ONE, "secret conclusion",
                null, null, null, null, null, List.of(), null, "FULL", "HOSPITAL_MANAGER", false) {
            public String getFutureSensitiveField() { return "secret future field"; }
        };
        CaseDetailMetadataView result = new CaseDetailMetadataMapper(projects).project(raw);
        JsonNode json = new ObjectMapper().valueToTree(result);
        assertEquals(ALLOWED, fields(json));
        assertFalse(json.has("lifecycleLatestStage"));
        assertEquals("Project name", json.path("projectName").asText());
        assertEquals("CLOSED", json.path("caseStatus").asText());
        assertFalse(json.toString().contains("secret"));
        assertEquals(java.math.BigDecimal.TEN, raw.getInitialReviewScore());
        assertThrows(UnsupportedOperationException.class, () -> result.metadata().put("futureSensitiveField", "secret"));
    }

    @Test
    void addingAnInheritedDtoGetterCannotExpandTheWireFieldSet() {
        CaseDetailMetadataView future = new CaseDetailMetadataView("c", "CLOSED", "name", "H001", "initial", "rr", null) {
            public Map<String, Object> getFutureBootstrap() { return Map.of("score", 99, "comment", "private"); }
        };
        JsonNode json = new ObjectMapper().valueToTree(future);
        assertEquals(ALLOWED, fields(json));
        assertFalse(json.has("lifecycleLatestStage"));
        for (JsonNode value : json) assertFalse(value.isContainerNode());
        assertFalse(json.has("initialReviewContext"));
        assertFalse(json.has("rereviewContext"));
        assertFalse(json.has("futureBootstrap"));
    }

    private Set<String> fields(JsonNode json) {
        Set<String> fields = new HashSet<>(); json.fieldNames().forEachRemaining(fields::add); return fields;
    }
}
