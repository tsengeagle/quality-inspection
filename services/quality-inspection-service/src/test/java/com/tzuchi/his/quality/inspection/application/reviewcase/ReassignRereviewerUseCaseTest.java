package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.RoleDeniedException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.testfixture.CapturingDomainEventPublisher;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryReviewCaseRepository;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryRereviewAccessRecords;
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ReassignRereviewerUseCaseTest {

    private InMemoryReviewCaseRepository caseRepository;
    private CapturingDomainEventPublisher eventPublisher;
    private ReassignRereviewerUseCase useCase;
    private InMemoryRereviewAccessRecords records;
    private static final java.time.Instant NOW = java.time.Instant.parse("2026-11-16T10:00:00Z");

    @BeforeEach
    void setUp() {
        caseRepository = new InMemoryReviewCaseRepository();
        eventPublisher = new CapturingDomainEventPublisher();
        records = new InMemoryRereviewAccessRecords();
        useCase = new ReassignRereviewerUseCase(caseRepository, eventPublisher, records, records,
                java.time.Clock.fixed(NOW, java.time.ZoneOffset.UTC));
    }

    private ActorContext managerActor() {
        return new ActorContext("mgr-1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));
    }

    private ReviewCase seedAssignedRereviewCase() {
        ReviewCase rc = ReviewCase.createFromSampling(
                "c1", "b1", "p1", "H001", "SK-001", MedicalRecordType.OUTPATIENT);
        rc.assignReviewer("reviewer-1", Instant.now(), Instant.now());
        rc.startInitialReview("sub-1", "ss-1");
        rc.completeInitialReview(new BigDecimal("90"));
        // D-054：先固定初審，才依品質線判斷流向。
        rc.fixInitialReview(rc.getLatestInitialSubmissionId(), "reviewer-1",
                java.time.Instant.parse("2026-01-01T00:00:00Z"), new BigDecimal("90"), new BigDecimal("80"));
        rc.assignRereviewer("rereviewer-1", Instant.now(), Instant.now());
        caseRepository.save(rc);
        return rc;
    }

    @Test
    void execute_happyPath_shouldReassign() {
        seedAssignedRereviewCase();
        Input cmd = new Input();
        cmd.setCaseId("c1");
        cmd.setRereviewerId("rereviewer-2");
        cmd.setDueAt(Instant.parse("2026-07-15T23:59:00Z"));

        useCase.execute(managerActor(), cmd.caseId, cmd.rereviewerId, cmd.dueAt);

        ReviewCase saved = caseRepository.findById("c1").orElseThrow();
        assertEquals("rereviewer-2", saved.getRereviewerAssignment().getReviewerId());
        assertEquals(1, eventPublisher.events.size());
    }

    @Test
    void execute_sameAsInitialReviewer_shouldThrow() {
        seedAssignedRereviewCase();
        Input cmd = new Input();
        cmd.setCaseId("c1");
        cmd.setRereviewerId("reviewer-1"); // same as initial reviewer → D-017
        cmd.setDueAt(Instant.now());

        assertThrows(Exception.class, () -> useCase.execute(managerActor(), cmd.caseId, cmd.rereviewerId, cmd.dueAt));
    }

    @Test
    void execute_nonManager_shouldThrow() {
        seedAssignedRereviewCase();
        Input cmd = new Input();
        cmd.setCaseId("c1");
        cmd.setRereviewerId("rereviewer-2");

        ActorContext reviewer = new ActorContext("rev-1", "Reviewer",
                EnumSet.of(DomainRole.REVIEWER), Optional.of("H001"));
        assertThrows(RoleDeniedException.class, () -> useCase.execute(reviewer, cmd.caseId, cmd.rereviewerId, cmd.dueAt));
    }

    @Test
    void execute_otherHospitalManager_shouldThrowAccessDenied() {
        seedAssignedRereviewCase();
        ActorContext otherHospital = new ActorContext("mgr-2", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H002"));
        Input cmd = new Input();
        cmd.setCaseId("c1");
        cmd.setRereviewerId("rereviewer-2");

        assertThrows(com.tzuchi.his.quality.inspection.application.common.AccessDeniedException.class, () -> useCase.execute(otherHospital, cmd.caseId, cmd.rereviewerId, cmd.dueAt));
        assertEquals("rereviewer-1",
                caseRepository.findById("c1").get().getRereviewerAssignment().getReviewerId());
    }
    // ---- D-055 第 1、4 點：每次指派／改派都寫一筆歷史 ----

    @Test
    void execute_writesAssignmentHistory() {
        seedAssignedRereviewCase();
        Input cmd = new Input();
        cmd.setCaseId("c1");
        cmd.setRereviewerId("rereviewer-9");
        cmd.setDueAt(Instant.parse("2026-06-30T23:59:00Z"));

        useCase.execute(managerActor(), cmd.caseId, cmd.rereviewerId, cmd.dueAt);

        assertEquals(1, records.history.size());
        RereviewerAssignmentRecord r = records.history.get(0);
        assertEquals("c1", r.getCaseId());
        assertEquals("rereviewer-9", r.getRereviewerId());
        assertEquals("mgr-1", r.getAssignedBy());
        assertEquals(NOW, r.getAssignedAt());
        assertEquals(RereviewerAssignmentRecord.Kind.REASSIGN, r.getKind());
    }

    @Test
    void execute_rejectedAssignment_writesNoHistory() {
        seedAssignedRereviewCase();
        Input cmd = new Input();
        cmd.setCaseId("c1");
        cmd.setRereviewerId("reviewer-1"); // D-017
        cmd.setDueAt(Instant.now());

        assertThrows(DomainRuleViolationException.class, () -> useCase.execute(managerActor(), cmd.caseId, cmd.rereviewerId, cmd.dueAt));
        assertTrue(records.history.isEmpty());
    }

    // ---- D-055 第 5 點：讀過完整初審的人不得受派 ----

    @Test
    void execute_targetHasReadInitial_shouldReject422AndLeaveCaseUnchanged() {
        ReviewCase before = seedAssignedRereviewCase();
        String previous = before.getRereviewerAssignment() == null ? null
                : before.getRereviewerAssignment().getReviewerId();
        records.record("c1", "auditor-7", NOW);
        Input cmd = new Input();
        cmd.setCaseId("c1");
        cmd.setRereviewerId("auditor-7");
        cmd.setDueAt(Instant.parse("2026-06-30T23:59:00Z"));

        DomainRuleViolationException ex = assertThrows(DomainRuleViolationException.class,
                () -> useCase.execute(managerActor(), cmd.caseId, cmd.rereviewerId, cmd.dueAt));

        assertEquals("REREVIEWER_HAS_READ_INITIAL", ex.getErrorCode());
        ReviewCase saved = caseRepository.findById("c1").orElseThrow();
        assertEquals(previous, saved.getRereviewerAssignment() == null ? null
                : saved.getRereviewerAssignment().getReviewerId());
        assertTrue(records.history.isEmpty());
        assertTrue(eventPublisher.events.isEmpty());
    }

    @Test
    void execute_readerOfAnotherCase_isNotBlocked() {
        seedAssignedRereviewCase();
        records.record("other-case", "auditor-7", NOW);
        Input cmd = new Input();
        cmd.setCaseId("c1");
        cmd.setRereviewerId("auditor-7");
        cmd.setDueAt(Instant.parse("2026-06-30T23:59:00Z"));

        assertDoesNotThrow(() -> useCase.execute(managerActor(), cmd.caseId, cmd.rereviewerId, cmd.dueAt));
    }

    /** 改派複審醫師的輸入（D-082 第 7 點：use case 改收參數）。 */
    private static class Input {
        String caseId;
        String rereviewerId;
        Instant dueAt;
        void setCaseId(String caseId) { this.caseId = caseId; }
        void setRereviewerId(String rereviewerId) { this.rereviewerId = rereviewerId; }
        void setDueAt(Instant dueAt) { this.dueAt = dueAt; }
    }
}
