package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ListCaseAssignmentCandidatesUseCaseTest {

    private InMemoryAssignmentQueryPort queryPort;
    private InMemoryDirectoryPort directoryPort;
    private StubReaderPort readerPort;
    private ListCaseAssignmentCandidatesUseCase useCase;

    @BeforeEach
    void setUp() {
        queryPort = new InMemoryAssignmentQueryPort();
        directoryPort = new InMemoryDirectoryPort();
        readerPort = new StubReaderPort();
        useCase = new ListCaseAssignmentCandidatesUseCase(queryPort, directoryPort, readerPort);
    }

    @Test
    void reviewerCandidates_areSelectableWhenInitialAssignmentAllowed() {
        queryPort.put(new AssignmentCaseSnapshotView(
                "c1", "p1", "Project 1", "b1", "H001", "SK-001", "OUTPATIENT",
                "SELECTED_PENDING_ASSIGNMENT", null, null,
                null, null, null, null));

        List<AssignmentCandidateView> result = useCase.execute(managerActor(), "c1",
                AssignmentCandidateRole.REVIEWER, null);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(AssignmentCandidateView::isSelectable));
    }

    @Test
    void rereviewerSameAsReviewer_isReturnedButBlocked() {
        queryPort.put(new AssignmentCaseSnapshotView(
                "c1", "p1", "Project 1", "b1", "H001", "SK-001", "OUTPATIENT",
                "PENDING_REREVIEW", "reviewer-1", null,
                null, null, null, null));

        List<AssignmentCandidateView> result = useCase.execute(managerActor(), "c1",
                AssignmentCandidateRole.REREVIEWER, null);

        AssignmentCandidateView samePerson = result.stream()
                .filter(item -> item.getUserId().equals("reviewer-1"))
                .findFirst()
                .orElseThrow();

        assertFalse(samePerson.isSelectable());
        assertEquals("REREVIEWER_SAME_AS_REVIEWER", samePerson.getBlockedReason());
    }

    @Test
    void initialReviewInProgress_blocksReviewerLookupSelection() {
        queryPort.put(new AssignmentCaseSnapshotView(
                "c1", "p1", "Project 1", "b1", "H001", "SK-001", "OUTPATIENT",
                "INITIAL_REVIEW_IN_PROGRESS", "reviewer-1", null,
                null, null, null, null));

        List<AssignmentCandidateView> result = useCase.execute(managerActor(), "c1",
                AssignmentCandidateRole.REVIEWER, null);

        assertTrue(result.stream().allMatch(item -> !item.isSelectable()));
        assertTrue(result.stream().allMatch(item -> "INITIAL_ASSIGNMENT_NOT_ALLOWED".equals(item.getBlockedReason())));
    }

    // D-084 第 5 點：退回的案件要先重開，候選人與分派頁一樣不可選
    @Test
    void returnedCase_blocksReviewerSelection_D084() {
        queryPort.put(new AssignmentCaseSnapshotView(
                "c1", "p1", "Project 1", "b1", "H001", "SK-001", "OUTPATIENT",
                "RETURNED", "reviewer-1", null,
                null, null, null, null));

        List<AssignmentCandidateView> result = useCase.execute(managerActor(), "c1",
                AssignmentCandidateRole.REVIEWER, null);

        assertTrue(result.stream().noneMatch(AssignmentCandidateView::isSelectable));
        assertTrue(result.stream().allMatch(item -> "INITIAL_ASSIGNMENT_NOT_ALLOWED".equals(item.getBlockedReason())));
    }

    // D-084 第 6 點：讀過完整初審的複審候選人不可選，與指派的 422 一致；查詢不寫入紀錄
    @Test
    void rereviewerWhoReadTheInitialReview_isBlocked_D084() {
        queryPort.put(new AssignmentCaseSnapshotView(
                "c1", "p1", "Project 1", "b1", "H001", "SK-001", "OUTPATIENT",
                "PENDING_REREVIEW", "reviewer-1", null,
                null, null, null, null));
        readerPort.read.add("c1:rereviewer-1");

        List<AssignmentCandidateView> result = useCase.execute(managerActor(), "c1",
                AssignmentCandidateRole.REREVIEWER, null);

        AssignmentCandidateView reader = result.stream()
                .filter(item -> item.getUserId().equals("rereviewer-1")).findFirst().orElseThrow();
        assertFalse(reader.isSelectable());
        assertEquals("REREVIEWER_HAS_READ_INITIAL", reader.getBlockedReason());
        assertEquals(0, readerPort.recorded);
    }

    // 初審候選人不看讀取者紀錄（初審醫師本來就讀初審）
    @Test
    void reviewerWhoReadTheInitialReview_isStillSelectable_D084() {
        queryPort.put(new AssignmentCaseSnapshotView(
                "c1", "p1", "Project 1", "b1", "H001", "SK-001", "OUTPATIENT",
                "SELECTED_PENDING_ASSIGNMENT", null, null,
                null, null, null, null));
        readerPort.read.add("c1:reviewer-2");

        List<AssignmentCandidateView> result = useCase.execute(managerActor(), "c1",
                AssignmentCandidateRole.REVIEWER, null);

        assertTrue(result.stream().allMatch(AssignmentCandidateView::isSelectable));
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

    static class StubReaderPort implements InitialReviewReaderPort {
        final Set<String> read = new HashSet<>();
        int recorded;

        @Override
        public void pinCaseVersion(String caseId) {
        }

        @Override
        public boolean hasRead(String caseId, String readerId) {
            return read.contains(caseId + ":" + readerId);
        }

        @Override
        public void record(String caseId, String readerId, java.time.Instant readAt) {
            recorded++;
        }
    }

    static class InMemoryDirectoryPort implements AssignmentCandidateDirectoryPort {
        @Override
        public List<AssignmentCandidateDirectoryEntry> findCandidates(String hospitalId,
                                                                      AssignmentCandidateRole role,
                                                                      String keyword) {
            if (role == AssignmentCandidateRole.REVIEWER) {
                return List.of(
                        new AssignmentCandidateDirectoryEntry("reviewer-1", "Dr. Reviewer One", "reviewer-1", role, true),
                        new AssignmentCandidateDirectoryEntry("reviewer-2", "Dr. Reviewer Two", "reviewer-2", role, true));
            }
            return List.of(
                    new AssignmentCandidateDirectoryEntry("reviewer-1", "Dr. Reviewer One", "reviewer-1", role, true),
                    new AssignmentCandidateDirectoryEntry("rereviewer-1", "Dr. ReReviewer One", "rereviewer-1", role, true));
        }

        @Override
        public Optional<AssignmentCandidateDirectoryEntry> findByUserId(String hospitalId, String userId) {
            return Optional.empty();
        }
    }
}
