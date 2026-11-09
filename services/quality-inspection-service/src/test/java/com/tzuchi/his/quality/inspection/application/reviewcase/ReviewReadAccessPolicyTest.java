package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tzuchi.his.quality.inspection.application.audit.*;
import com.tzuchi.his.quality.inspection.application.common.*;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.ReviewCaseDetailView;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReviewReadAccessPolicyTest {
    @ParameterizedTest
    @ValueSource(strings = {"historical", "otherCase", "otherHospital", "initialAssignment", "malformed", "empty", "otherAssignee"})
    void historicalEvidenceMustMatchCaseHospitalAndRereviewAction(String scenario) {
        AuditTrailQueryPort audit = mock(AuditTrailQueryPort.class);
        AuditTrailEntry entry = new AuditTrailEntry("event", "ReviewCase", scenario.equals("otherCase") ? "other" : "c",
                scenario.equals("initialAssignment") ? "ASSIGNED" : "REREVIEWER_ASSIGNED",
                "manager", "Manager", scenario.equals("otherHospital") ? "H002" : "H001", Instant.EPOCH,
                scenario.equals("malformed") ? "broken JSON" : scenario.equals("otherAssignee") ? "{\"rereviewerId\":\"B\"}" : "{\"rereviewerId\":\"A\"}");
        when(audit.findLifecycleByEntityId("ReviewCase", "c")).thenReturn(scenario.equals("empty") ? List.of() : List.of(entry));
        ReviewReadAccessPolicy policy = new ReviewReadAccessPolicy(audit, new ObjectMapper());
        ReviewCaseDetailView raw = new ReviewCaseDetailView("c", "p", "b", "H001", "CLOSED", "initial", "B",
                null, null, null, null, null, null, null, null, null, null, List.of(), null, null, null, false);
        ActorContext actor = new ActorContext("A", "A", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));
        ReviewReadAccessPolicy.HistoricalIdentity identity = policy.assessHistoricalIdentity("c", "H001", "A");
        assertEquals(scenario.equals("historical") ? ReviewReadAccessPolicy.HistoricalIdentity.PROVEN_HISTORICAL
                : ReviewReadAccessPolicy.HistoricalIdentity.UNKNOWN, identity);
        assertNotEquals(ReviewReadAccessPolicy.HistoricalIdentity.PROVEN_NON_HISTORICAL, identity);
        assertThrows(AccessDeniedException.class, () -> policy.requireAccess(actor, raw));
        verify(audit, times(2)).findLifecycleByEntityId("ReviewCase", "c");
    }
}
