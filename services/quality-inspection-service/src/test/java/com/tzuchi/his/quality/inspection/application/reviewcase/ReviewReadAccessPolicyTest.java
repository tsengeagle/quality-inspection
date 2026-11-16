package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.*;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.ReviewCaseDetailView;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryRereviewAccessRecords;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** D-055 第 3、5 點：以複審指派歷史判斷身分，准許完整讀取時記錄讀取者。 */
class ReviewReadAccessPolicyTest {

    private static final Instant NOW = Instant.parse("2026-11-16T10:00:00Z");
    private InMemoryRereviewAccessRecords records;
    private ReviewReadAccessPolicy policy;

    @BeforeEach
    void setUp() {
        records = new InMemoryRereviewAccessRecords();
        policy = new ReviewReadAccessPolicy(records, records, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static ReviewCaseDetailView caseView(String rereviewer) {
        return new ReviewCaseDetailView("c", "p", "b", "H001", "CLOSED", "initial", rereviewer,
                null, null, null, null, null, null, null, null, null, null, List.of(), null, null, null, false);
    }

    private static ActorContext actor(String id, DomainRole role) {
        return new ActorContext(id, id, EnumSet.of(role), Optional.of("H001"));
    }

    private void history(String rereviewerId) {
        records.history.add(new RereviewerAssignmentRecord("c", rereviewerId, NOW, "mgr",
                RereviewerAssignmentRecord.Kind.ASSIGN));
    }

    @ParameterizedTest
    @EnumSource(value = DomainRole.class, names = {"HOSPITAL_MANAGER", "AUDITOR"})
    void completeHistoryAndNotInHistory_grantsAndRecordsReader(DomainRole role) {
        history("someone-else");

        assertDoesNotThrow(() -> policy.requireAccess(actor("A", role), caseView("someone-else")));

        assertEquals(NOW, records.readers.get("c|A"));
    }

    @Test
    void initialReviewer_grantedAndRecorded() {
        assertDoesNotThrow(() -> policy.requireAccess(actor("initial", DomainRole.REVIEWER), caseView(null)));
        assertTrue(records.hasRead("c", "initial"));
    }

    @Test
    void currentRereviewer_deniedAndNotRecorded() {
        assertThrows(AccessDeniedException.class,
                () -> policy.requireAccess(actor("A", DomainRole.HOSPITAL_MANAGER), caseView("A")));
        assertTrue(records.readers.isEmpty());
    }

    @Test
    void historicalRereviewer_deniedAndNotRecorded() {
        history("A");

        assertThrows(AccessDeniedException.class,
                () -> policy.requireAccess(actor("A", DomainRole.AUDITOR), caseView("B")));
        assertTrue(records.readers.isEmpty());
    }

    @Test
    void incompleteHistory_deniedAsUnknown() {
        records.incompleteCases.add("c");

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> policy.requireAccess(actor("A", DomainRole.HOSPITAL_MANAGER), caseView(null)));
        assertEquals("ACCESS_DENIED", ex.getErrorCode());
        assertTrue(records.readers.isEmpty());
    }

    @Test
    void recordFailure_failsClosed() {
        records.failOnRecord = new IllegalStateException("db down");

        assertThrows(IllegalStateException.class,
                () -> policy.requireAccess(actor("A", DomainRole.HOSPITAL_MANAGER), caseView(null)));
    }

    @Test
    void caseVersionPinnedBeforeIdentityIsAssessed_thenRecorded() {
        policy.requireAccess(actor("A", DomainRole.HOSPITAL_MANAGER), caseView(null));

        List<String> calls = records.calls;
        assertEquals("pin:c", calls.get(0), "必須先固定案件版本，再查歷史：" + calls);
        assertEquals("record:c", calls.get(calls.size() - 1));
        assertTrue(calls.indexOf("contains:c") > 0 && calls.indexOf("isComplete:c") > 0, calls.toString());
    }

    @Test
    void rereviewerAndSystemAdmin_doNotProduceReaderRecords() {
        policy.requireAccess(actor("B", DomainRole.REREVIEWER), caseView("B"));
        policy.requireAccess(actor("S", DomainRole.SYSTEM_ADMIN), caseView("B"));

        assertTrue(records.readers.isEmpty());
        assertFalse(records.calls.contains("pin:c"));
    }

    @Test
    void multiRole_stillDenied() {
        ActorContext multi = new ActorContext("A", "A",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER, DomainRole.AUDITOR), Optional.of("H001"));

        assertThrows(AccessDeniedException.class, () -> policy.requireAccess(multi, caseView(null)));
        assertTrue(records.readers.isEmpty());
    }
}
