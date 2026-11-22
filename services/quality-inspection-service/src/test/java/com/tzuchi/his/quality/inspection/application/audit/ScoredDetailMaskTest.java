package com.tzuchi.his.quality.inspection.application.audit;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.RereviewerHistoryQuery;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** D-071 第 1～3 點。 */
class ScoredDetailMaskTest {

    private static final ActorContext MANAGER =
            new ActorContext("u-m1", "Manager", EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));

    /** 狀態表裡沒有的案件視為找不到；歷史一律完整、沒有人出現過。 */
    static ScoredDetailMask maskWith(Map<String, AuditCaseStateQuery.CaseState> states) {
        return maskWith(states, Map.of(), Set.of());
    }

    static ScoredDetailMask maskWith(Map<String, AuditCaseStateQuery.CaseState> states,
                                     Map<String, Set<String>> historyByCase, Set<String> incomplete) {
        AuditCaseStateQuery caseStates = ids -> states.entrySet().stream()
                .filter(e -> ids.contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        RereviewerHistoryQuery history = new RereviewerHistoryQuery() {
            @Override
            public Set<String> findCaseIdsAssignedTo(String rereviewerId, Collection<String> caseIds) {
                return caseIds.stream()
                        .filter(id -> historyByCase.getOrDefault(id, Set.of()).contains(rereviewerId))
                        .collect(Collectors.toSet());
            }

            @Override
            public Set<String> findHistoryIncompleteCaseIds(Collection<String> caseIds) {
                return caseIds.stream().filter(incomplete::contains).collect(Collectors.toSet());
            }
        };
        return new ScoredDetailMask(caseStates, history);
    }

    private static AuditTrailEntry entry(String caseId, String action) {
        return new AuditTrailEntry("e-" + action, "ReviewCase", caseId, action, "u1", "U", "H001",
                Instant.parse("2026-11-22T00:00:00Z"), "{\"x\":1}");
    }

    private static AuditTrailEntry only(List<AuditTrailEntry> result) {
        assertEquals(1, result.size());
        return result.get(0);
    }

    @Test
    void scoredActionsAreTheFiveListedInD071() {
        assertEquals(Set.of("INITIAL_REVIEW_COMPLETED", "CASE_ESCALATED_TO_REREVIEW", "REREVIEW_SUBMITTED",
                "REREVIEW_REVISED", "CASE_CLOSED_WITHOUT_REREVIEW"), ScoredDetailMask.SCORED_ACTIONS);
    }

    @Test
    void masksScoredDetailBeforeRereview() {
        ScoredDetailMask mask = maskWith(Map.of("c1", new AuditCaseStateQuery.CaseState(CaseStatus.PENDING_REREVIEW, null)));
        List<AuditTrailEntry> result = mask.apply(MANAGER,
                List.of(entry("c1", "ASSIGNED"), entry("c1", "INITIAL_REVIEW_COMPLETED")));
        assertEquals("{\"x\":1}", result.get(0).getDetail());
        assertNull(result.get(1).getDetail());
    }

    @Test
    void showsScoredDetailOnceNoFurtherRereviewerIsPossible() {
        for (CaseStatus status : List.of(CaseStatus.REREVIEW_IN_PROGRESS, CaseStatus.CLOSED, CaseStatus.CANCELLED)) {
            ScoredDetailMask mask = maskWith(Map.of("c1", new AuditCaseStateQuery.CaseState(status, "u-rr1")));
            assertEquals("{\"x\":1}", only(mask.apply(MANAGER, List.of(entry("c1", "REREVIEW_SUBMITTED")))).getDetail(),
                    status.name());
        }
    }

    @Test
    void masksForTheCurrentRereviewer() {
        ScoredDetailMask mask = maskWith(Map.of("c1", new AuditCaseStateQuery.CaseState(CaseStatus.CLOSED, "u-m1")));
        assertNull(only(mask.apply(MANAGER, List.of(entry("c1", "REREVIEW_SUBMITTED")))).getDetail());
    }

    @Test
    void masksForAHistoricalRereviewer() {
        ScoredDetailMask mask = maskWith(Map.of("c1", new AuditCaseStateQuery.CaseState(CaseStatus.CLOSED, "u-rr2")),
                Map.of("c1", Set.of("u-m1", "u-rr2")), Set.of());
        assertNull(only(mask.apply(MANAGER, List.of(entry("c1", "INITIAL_REVIEW_COMPLETED")))).getDetail());
    }

    @Test
    void masksWhenTheHistoryIsIncomplete() {
        ScoredDetailMask mask = maskWith(Map.of("c1", new AuditCaseStateQuery.CaseState(CaseStatus.CLOSED, null)),
                Map.of(), Set.of("c1"));
        assertNull(only(mask.apply(MANAGER, List.of(entry("c1", "CASE_CLOSED_WITHOUT_REREVIEW")))).getDetail());
    }

    @Test
    void masksWhenTheCaseCannotBeFound() {
        assertNull(only(maskWith(Map.of()).apply(MANAGER, List.of(entry("gone", "REREVIEW_REVISED")))).getDetail());
    }

    @Test
    void judgesEachCaseOnItsOwnAndLeavesOtherEntityTypesAlone() {
        ScoredDetailMask mask = maskWith(Map.of(
                "open", new AuditCaseStateQuery.CaseState(CaseStatus.INITIAL_REVIEW_COMPLETED, null),
                "done", new AuditCaseStateQuery.CaseState(CaseStatus.CLOSED, null)));
        AuditTrailEntry project = new AuditTrailEntry("p", "ReviewProject", "open", "INITIAL_REVIEW_COMPLETED",
                "u1", "U", "H001", Instant.parse("2026-11-22T00:00:00Z"), "{\"y\":2}");
        List<AuditTrailEntry> result = mask.apply(MANAGER,
                List.of(entry("open", "INITIAL_REVIEW_COMPLETED"), entry("done", "INITIAL_REVIEW_COMPLETED"), project));
        assertNull(result.get(0).getDetail());
        assertEquals("{\"x\":1}", result.get(1).getDetail());
        assertEquals("{\"y\":2}", result.get(2).getDetail());
    }
}
