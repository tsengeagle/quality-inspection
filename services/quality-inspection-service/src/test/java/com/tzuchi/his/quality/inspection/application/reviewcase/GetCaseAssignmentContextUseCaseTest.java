package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.*;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class GetCaseAssignmentContextUseCaseTest {

    private InMemoryAssignmentQueryPort queryPort;
    private StubDirectoryPort directoryPort;
    private GetCaseAssignmentContextUseCase useCase;

    @BeforeEach
    void setUp() {
        queryPort = new InMemoryAssignmentQueryPort();
        directoryPort = new StubDirectoryPort();
        useCase = new GetCaseAssignmentContextUseCase(queryPort, directoryPort);
    }

    @Test
    void selectedPendingAssignment_returnsAssignInitialMode() {
        queryPort.put(new AssignmentCaseSnapshotView(
                "c1", "p1", "Project 1", "b1", "H001", "SK-001", "OUTPATIENT",
                "SELECTED_PENDING_ASSIGNMENT", null, null,
                null, null, null, null));

        AssignmentBootstrapView result = useCase.execute(managerActor(), "c1");

        assertEquals("ASSIGN_INITIAL", result.getPageMode());
        assertEquals(List.of("ASSIGN_REVIEWER"), result.getAllowedActions());
        assertFalse(result.isBlocked());
        assertNull(result.getCurrentReviewer());
    }

    @Test
    void pendingRereview_returnsMixedAssignmentMode() {
        queryPort.put(new AssignmentCaseSnapshotView(
                "c1", "p1", "Project 1", "b1", "H001", "SK-001", "OUTPATIENT",
                "PENDING_REREVIEW", "reviewer-1",
                Instant.parse("2026-04-01T10:00:00Z"),
                Instant.parse("2026-04-10T23:59:00Z"),
                null, null, null));

        AssignmentBootstrapView result = useCase.execute(managerActor(), "c1");

        assertEquals("MIXED_ASSIGNMENT", result.getPageMode());
        assertEquals(List.of("ASSIGN_REREVIEWER"), result.getAllowedActions());
        assertEquals("reviewer-1", result.getCurrentReviewer().getUserId());
        assertEquals("Dr. Reviewer One", result.getCurrentReviewer().getDisplayName());
        assertEquals(Instant.parse("2026-04-10T23:59:00Z"), result.getReviewerDueAt());
        assertNull(result.getEffectiveDueAt());
    }

    @Test
    void closedCase_returnsBlockedContext() {
        queryPort.put(new AssignmentCaseSnapshotView(
                "c1", "p1", "Project 1", "b1", "H001", "SK-001", "OUTPATIENT",
                "CLOSED", "reviewer-1", null, null,
                "rereviewer-1", null, null));

        AssignmentBootstrapView result = useCase.execute(managerActor(), "c1");

        assertEquals("READONLY_BLOCKED", result.getPageMode());
        assertTrue(result.isBlocked());
        assertEquals("CASE_ALREADY_CLOSED", result.getBlockedReason());
        assertTrue(result.getAllowedActions().isEmpty());
    }

    @Test
    void missingCase_throwsNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(managerActor(), "missing"));
    }

    private ActorContext managerActor() {
        return new ActorContext("manager-1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));
    }

    static class InMemoryAssignmentQueryPort implements ReviewCaseAssignmentQueryPort {
        private final Map<String, AssignmentCaseSnapshotView> snapshots = new HashMap<>();

        void put(AssignmentCaseSnapshotView view) {
            snapshots.put(view.getCaseId(), view);
        }

        @Override
        public Optional<AssignmentCaseSnapshotView> findAssignmentSnapshotById(String caseId) {
            return Optional.ofNullable(snapshots.get(caseId));
        }
    }

    static class StubDirectoryPort implements AssignmentCandidateDirectoryPort {
        private final Map<String, AssignmentCandidateDirectoryEntry> entries = Map.of(
                "reviewer-1", new AssignmentCandidateDirectoryEntry(
                        "reviewer-1", "Dr. Reviewer One", "reviewer-1 | Dr. Reviewer One",
                        AssignmentCandidateRole.REVIEWER, true),
                "rereviewer-1", new AssignmentCandidateDirectoryEntry(
                        "rereviewer-1", "Dr. ReReviewer One", "rereviewer-1 | Dr. ReReviewer One",
                        AssignmentCandidateRole.REREVIEWER, true));

        @Override
        public List<AssignmentCandidateDirectoryEntry> findCandidates(String hospitalId,
                                                                      AssignmentCandidateRole role,
                                                                      String keyword) {
            return List.of();
        }

        @Override
        public Optional<AssignmentCandidateDirectoryEntry> findByUserId(String hospitalId, String userId) {
            return Optional.ofNullable(entries.get(userId));
        }
    }

    // D-084 第 5 點：退回的案件要先重開（D-080），才能改派（D-082）
    @Test
    void returnedCase_isReadOnlyBlocked_D084() {
        queryPort.put(new AssignmentCaseSnapshotView(
                "c1", "p1", "Project 1", "b1", "H001", "SK-001", "OUTPATIENT",
                "RETURNED", "reviewer-1",
                Instant.parse("2026-04-01T10:00:00Z"),
                Instant.parse("2026-04-10T23:59:00Z"),
                null, null, null));

        AssignmentBootstrapView result = useCase.execute(managerActor(), "c1");

        assertEquals("READONLY_BLOCKED", result.getPageMode());
        assertTrue(result.isBlocked());
        assertEquals("CASE_NOT_ASSIGNABLE_IN_CURRENT_STATUS", result.getBlockedReason());
        assertEquals(List.of(), result.getAllowedActions());
    }
}
