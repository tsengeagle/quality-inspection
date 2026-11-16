package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.RoleDeniedException;
import com.tzuchi.his.quality.inspection.application.reviewcase.command.CloseWithoutRereviewCommand;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
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

class CloseWithoutRereviewUseCaseTest {

    private InMemoryReviewCaseRepository caseRepository;
    private CapturingDomainEventPublisher eventPublisher;
    private CloseWithoutRereviewUseCase useCase;

    @BeforeEach
    void setUp() {
        caseRepository = new InMemoryReviewCaseRepository();
        eventPublisher = new CapturingDomainEventPublisher();
        useCase = new CloseWithoutRereviewUseCase(caseRepository, eventPublisher);
    }

    private ActorContext managerActor() {
        return new ActorContext("mgr-1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("H001"));
    }

    private ReviewCase seedCompletedCase() {
        ReviewCase rc = ReviewCase.createFromSampling(
                "c1", "b1", "p1", "H001", "SK-001", MedicalRecordType.OUTPATIENT);
        rc.assignReviewer("reviewer-1", LocalDateTime.now());
        rc.startInitialReview("sub-1", "ss-1");
        rc.completeInitialReview(new BigDecimal("70"));
        // D-054：先固定初審，才依品質線判斷流向。
        rc.fixInitialReview(rc.getLatestInitialSubmissionId(), "reviewer-1",
                java.time.Instant.parse("2026-01-01T00:00:00Z"), new BigDecimal("70"), new BigDecimal("80"));
        // score 70 <= threshold 80 → INITIAL_REVIEW_COMPLETED
        caseRepository.save(rc);
        return rc;
    }

    @Test
    void execute_happyPath_shouldCloseCase() {
        seedCompletedCase();
        CloseWithoutRereviewCommand cmd = new CloseWithoutRereviewCommand();
        cmd.setCaseId("c1");

        useCase.execute(managerActor(), cmd);

        ReviewCase saved = caseRepository.findById("c1").orElseThrow();
        assertEquals(CaseStatus.CLOSED, saved.getStatus());
        assertNotNull(saved.getClosedAt());
        assertEquals(1, eventPublisher.events.size());
    }

    @Test
    void execute_wrongRole_shouldThrow() {
        seedCompletedCase();
        CloseWithoutRereviewCommand cmd = new CloseWithoutRereviewCommand();
        cmd.setCaseId("c1");

        ActorContext wrongActor = new ActorContext("reviewer-1", "Reviewer",
                EnumSet.of(DomainRole.REVIEWER), Optional.of("H001"));
        assertThrows(RoleDeniedException.class, () -> useCase.execute(wrongActor, cmd));
    }

    @Test
    void execute_wrongHospital_shouldThrowAccessDenied() {
        seedCompletedCase();
        CloseWithoutRereviewCommand cmd = new CloseWithoutRereviewCommand();
        cmd.setCaseId("c1");

        ActorContext wrongHospital = new ActorContext("mgr-1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("OTHER_HOSPITAL"));
        assertThrows(AccessDeniedException.class, () -> useCase.execute(wrongHospital, cmd));
    }
}
