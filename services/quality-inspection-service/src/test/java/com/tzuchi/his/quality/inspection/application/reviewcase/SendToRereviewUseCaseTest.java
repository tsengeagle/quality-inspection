package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.RoleDeniedException;
import com.tzuchi.his.quality.inspection.application.reviewcase.command.SendToRereviewCommand;
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

class SendToRereviewUseCaseTest {

    private InMemoryReviewCaseRepository caseRepository;
    private CapturingDomainEventPublisher eventPublisher;
    private SendToRereviewUseCase useCase;

    @BeforeEach
    void setUp() {
        caseRepository = new InMemoryReviewCaseRepository();
        eventPublisher = new CapturingDomainEventPublisher();
        useCase = new SendToRereviewUseCase(caseRepository, eventPublisher);
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
        rc.completeInitialReview(new BigDecimal("70"), new BigDecimal("80"));
        // score 70 <= threshold 80 → INITIAL_REVIEW_COMPLETED
        caseRepository.save(rc);
        return rc;
    }

    @Test
    void execute_happyPath_shouldEscalate() {
        seedCompletedCase();
        SendToRereviewCommand cmd = new SendToRereviewCommand();
        cmd.setCaseId("c1");

        useCase.execute(managerActor(), cmd);

        ReviewCase saved = caseRepository.findById("c1").orElseThrow();
        assertEquals(CaseStatus.PENDING_REREVIEW, saved.getStatus());
        assertEquals(1, eventPublisher.events.size());
    }

    @Test
    void execute_wrongRole_shouldThrow() {
        seedCompletedCase();
        SendToRereviewCommand cmd = new SendToRereviewCommand();
        cmd.setCaseId("c1");

        ActorContext wrongActor = new ActorContext("reviewer-1", "Reviewer",
                EnumSet.of(DomainRole.REVIEWER), Optional.of("H001"));
        assertThrows(RoleDeniedException.class, () -> useCase.execute(wrongActor, cmd));
    }

    @Test
    void execute_wrongHospital_shouldThrowAccessDenied() {
        seedCompletedCase();
        SendToRereviewCommand cmd = new SendToRereviewCommand();
        cmd.setCaseId("c1");

        ActorContext wrongHospital = new ActorContext("mgr-1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of("OTHER_HOSPITAL"));
        assertThrows(AccessDeniedException.class, () -> useCase.execute(wrongHospital, cmd));
    }
}
