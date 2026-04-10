package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.UnauthorizedException;
import com.tzuchi.his.quality.inspection.application.reviewcase.command.ReassignRereviewerCommand;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.testfixture.CapturingDomainEventPublisher;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryReviewCaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ReassignRereviewerUseCaseTest {

    private InMemoryReviewCaseRepository caseRepository;
    private CapturingDomainEventPublisher eventPublisher;
    private ReassignRereviewerUseCase useCase;

    @BeforeEach
    void setUp() {
        caseRepository = new InMemoryReviewCaseRepository();
        eventPublisher = new CapturingDomainEventPublisher();
        useCase = new ReassignRereviewerUseCase(caseRepository, eventPublisher);
    }

    private ActorContext managerActor() {
        return new ActorContext("mgr-1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));
    }

    private ReviewCase seedAssignedRereviewCase() {
        ReviewCase rc = ReviewCase.createFromSampling(
                "c1", "b1", "p1", "H001", "SK-001", MedicalRecordType.OUTPATIENT);
        rc.assignReviewer("reviewer-1", LocalDateTime.now());
        rc.startInitialReview("sub-1", "ss-1");
        rc.completeInitialReview(new BigDecimal("90"), new BigDecimal("80"));
        rc.assignRereviewer("rereviewer-1", LocalDateTime.now());
        caseRepository.save(rc);
        return rc;
    }

    @Test
    void execute_happyPath_shouldReassign() {
        seedAssignedRereviewCase();
        ReassignRereviewerCommand cmd = new ReassignRereviewerCommand();
        cmd.setCaseId("c1");
        cmd.setRereviewerId("rereviewer-2");
        cmd.setDueAt(LocalDateTime.of(2026, 7, 15, 23, 59));

        useCase.execute(managerActor(), cmd);

        ReviewCase saved = caseRepository.findById("c1").orElseThrow();
        assertEquals("rereviewer-2", saved.getRereviewerAssignment().getReviewerId());
        assertEquals(1, eventPublisher.events.size());
    }

    @Test
    void execute_sameAsInitialReviewer_shouldThrow() {
        seedAssignedRereviewCase();
        ReassignRereviewerCommand cmd = new ReassignRereviewerCommand();
        cmd.setCaseId("c1");
        cmd.setRereviewerId("reviewer-1"); // same as initial reviewer → D-017
        cmd.setDueAt(LocalDateTime.now());

        assertThrows(Exception.class, () -> useCase.execute(managerActor(), cmd));
    }

    @Test
    void execute_nonManager_shouldThrow() {
        seedAssignedRereviewCase();
        ReassignRereviewerCommand cmd = new ReassignRereviewerCommand();
        cmd.setCaseId("c1");
        cmd.setRereviewerId("rereviewer-2");

        ActorContext reviewer = new ActorContext("rev-1", "Reviewer",
                EnumSet.of(DomainRole.REVIEWER), Optional.of("H001"));
        assertThrows(UnauthorizedException.class, () -> useCase.execute(reviewer, cmd));
    }
}
