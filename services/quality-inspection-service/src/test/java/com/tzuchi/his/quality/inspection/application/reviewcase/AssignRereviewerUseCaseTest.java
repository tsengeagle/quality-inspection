package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
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
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AssignRereviewerUseCaseTest {

    private InMemoryReviewCaseRepository caseRepository;
    private CapturingDomainEventPublisher eventPublisher;
    private AssignRereviewerUseCase useCase;
    private InMemoryRereviewAccessRecords records;
    private static final java.time.Instant NOW = java.time.Instant.parse("2026-11-16T10:00:00Z");

    @BeforeEach
    void setUp() {
        caseRepository = new InMemoryReviewCaseRepository();
        eventPublisher = new CapturingDomainEventPublisher();
        records = new InMemoryRereviewAccessRecords();
        useCase = new AssignRereviewerUseCase(caseRepository, eventPublisher, records, records,
                java.time.Clock.fixed(NOW, java.time.ZoneOffset.UTC));
    }

    private ActorContext managerActor() {
        return new ActorContext("mgr-1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));
    }

    private ReviewCase seedPendingRereviewCase() {
        ReviewCase rc = ReviewCase.createFromSampling(
                "c1", "b1", "p1", "H001", "SK-001", MedicalRecordType.OUTPATIENT);
        rc.assignReviewer("reviewer-1", LocalDateTime.now(), LocalDateTime.now());
        rc.startInitialReview("sub-1", "ss-1");
        rc.completeInitialReview(new BigDecimal("90"));
        // D-054：先固定初審，才依品質線判斷流向。
        rc.fixInitialReview(rc.getLatestInitialSubmissionId(), "reviewer-1",
                java.time.Instant.parse("2026-01-01T00:00:00Z"), new BigDecimal("90"), new BigDecimal("80"));
        // score 90 > threshold 80 → auto-escalated to PENDING_REREVIEW
        caseRepository.save(rc);
        return rc;
    }

    @Test
    void execute_happyPath_shouldAssignRereviewer() {
        seedPendingRereviewCase();

        useCase.execute(managerActor(), "c1", "rereviewer-1", LocalDateTime.of(2026, 6, 30, 23, 59));

        ReviewCase saved = caseRepository.findById("c1").orElseThrow();
        assertNotNull(saved.getRereviewerAssignment());
        assertEquals("rereviewer-1", saved.getRereviewerAssignment().getReviewerId());
        assertEquals(1, eventPublisher.events.size());
    }

    @Test
    void execute_sameAsInitialReviewer_shouldThrow() {
        seedPendingRereviewCase();
        // same as initial reviewer → D-017

        assertThrows(Exception.class, () -> useCase.execute(managerActor(), "c1", "reviewer-1", LocalDateTime.now()));
    }

    @Test
    void execute_nonManager_shouldThrow() {
        seedPendingRereviewCase();

        ActorContext reviewer = new ActorContext("rev-1", "Reviewer",
                EnumSet.of(DomainRole.REVIEWER), Optional.of("H001"));
        assertThrows(RoleDeniedException.class, () -> useCase.execute(reviewer, "c1", "rereviewer-1", null));
    }

    @Test
    void execute_wrongHospital_shouldThrowAccessDenied() {
        seedPendingRereviewCase();

        ActorContext wrongHospital = new ActorContext("mgr-1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("OTHER_HOSPITAL"));
        assertThrows(AccessDeniedException.class, () -> useCase.execute(wrongHospital, "c1", "rereviewer-1", LocalDateTime.of(2026, 6, 30, 23, 59)));
    }
    // ---- D-055 第 1、4 點：每次指派／改派都寫一筆歷史 ----

    @Test
    void execute_writesAssignmentHistory() {
        seedPendingRereviewCase();

        useCase.execute(managerActor(), "c1", "rereviewer-9", LocalDateTime.of(2026, 6, 30, 23, 59));

        assertEquals(1, records.history.size());
        RereviewerAssignmentRecord r = records.history.get(0);
        assertEquals("c1", r.getCaseId());
        assertEquals("rereviewer-9", r.getRereviewerId());
        assertEquals("mgr-1", r.getAssignedBy());
        assertEquals(NOW, r.getAssignedAt());
        assertEquals(RereviewerAssignmentRecord.Kind.ASSIGN, r.getKind());
    }

    @Test
    void execute_rejectedAssignment_writesNoHistory() {
        seedPendingRereviewCase();
        // D-017

        assertThrows(DomainRuleViolationException.class, () -> useCase.execute(managerActor(), "c1", "reviewer-1", LocalDateTime.now()));
        assertTrue(records.history.isEmpty());
    }

    // ---- D-055 第 5 點：讀過完整初審的人不得受派 ----

    @Test
    void execute_targetHasReadInitial_shouldReject422AndLeaveCaseUnchanged() {
        ReviewCase before = seedPendingRereviewCase();
        String previous = before.getRereviewerAssignment() == null ? null
                : before.getRereviewerAssignment().getReviewerId();
        records.record("c1", "auditor-7", NOW);

        DomainRuleViolationException ex = assertThrows(DomainRuleViolationException.class,
                () -> useCase.execute(managerActor(), "c1", "auditor-7", LocalDateTime.of(2026, 6, 30, 23, 59)));

        assertEquals("REREVIEWER_HAS_READ_INITIAL", ex.getErrorCode());
        ReviewCase saved = caseRepository.findById("c1").orElseThrow();
        assertEquals(previous, saved.getRereviewerAssignment() == null ? null
                : saved.getRereviewerAssignment().getReviewerId());
        assertTrue(records.history.isEmpty());
        assertTrue(eventPublisher.events.isEmpty());
    }

    @Test
    void execute_readerOfAnotherCase_isNotBlocked() {
        seedPendingRereviewCase();
        records.record("other-case", "auditor-7", NOW);

        assertDoesNotThrow(() -> useCase.execute(managerActor(), "c1", "auditor-7", LocalDateTime.of(2026, 6, 30, 23, 59)));
    }
}
