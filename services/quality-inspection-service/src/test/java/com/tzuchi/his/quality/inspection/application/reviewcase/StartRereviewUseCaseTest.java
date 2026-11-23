package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.RoleDeniedException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewStage;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.SubmissionStatus;
import com.tzuchi.his.quality.inspection.testfixture.CapturingDomainEventPublisher;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryReviewCaseRepository;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryReviewSubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Clock;

class StartRereviewUseCaseTest {

    private InMemoryReviewCaseRepository caseRepository;
    private InMemoryReviewSubmissionRepository submissionRepository;
    private CapturingDomainEventPublisher eventPublisher;
    private StartRereviewUseCase useCase;

    @BeforeEach
    void setUp() {
        caseRepository = new InMemoryReviewCaseRepository();
        submissionRepository = new InMemoryReviewSubmissionRepository();
        eventPublisher = new CapturingDomainEventPublisher();
        useCase = new StartRereviewUseCase(caseRepository, submissionRepository, eventPublisher, Clock.systemDefaultZone(),
                () -> java.util.UUID.randomUUID().toString());
    }

    private ActorContext rereviewerActor() {
        return new ActorContext("rereviewer-1", "ReReviewer",
                EnumSet.of(DomainRole.REREVIEWER), Optional.of("H001"));
    }

    private ReviewCase seedAssignedCase() {
        ReviewCase rc = ReviewCase.createFromSampling(
                "c1", "b1", "p1", "H001", "SK-001", MedicalRecordType.OUTPATIENT);
        rc.assignReviewer("reviewer-1", Instant.now(), Instant.now());
        rc.startInitialReview("sub-1", "ss-1");
        rc.completeInitialReview(new BigDecimal("90"));
        // D-054：先固定初審，才依品質線判斷流向。
        rc.fixInitialReview(rc.getLatestInitialSubmissionId(), "reviewer-1",
                java.time.Instant.parse("2026-01-01T00:00:00Z"), new BigDecimal("90"), new BigDecimal("80"));
        // score 90 > threshold 80 → PENDING_REREVIEW
        rc.assignRereviewer("rereviewer-1", Instant.parse("2026-06-30T23:59:00Z"), Instant.now());
        caseRepository.save(rc);
        return rc;
    }

    @Test
    void execute_happyPath_shouldTransitionAndCreateDraft() {
        seedAssignedCase();

        useCase.execute(rereviewerActor(), "c1");

        ReviewCase saved = caseRepository.findById("c1").orElseThrow();
        assertEquals(CaseStatus.REREVIEW_IN_PROGRESS, saved.getStatus());

        // A rereview draft submission should be created
        List<ReviewSubmission> subs = submissionRepository.findAllByCaseId("c1");
        ReviewSubmission rereviewDraft = subs.stream()
                .filter(s -> s.getStage() == ReviewStage.REREVIEW)
                .findFirst().orElseThrow();
        assertEquals(SubmissionStatus.DRAFT, rereviewDraft.getStatus());
        assertEquals("rereviewer-1", rereviewDraft.getReviewerId());
        assertEquals(rereviewDraft.getSubmissionId(), saved.getLatestRereviewSubmissionId());
        assertEquals(1, eventPublisher.events.size());
    }

    @Test
    void execute_wrongActor_shouldThrow() {
        seedAssignedCase();

        // Not the assigned rereviewer
        ActorContext wrongActor = new ActorContext("someone-else", "Other",
                EnumSet.of(DomainRole.REREVIEWER), Optional.of("H001"));
        // D-068 第 2 點
        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () -> useCase.execute(wrongActor, "c1"));
        assertEquals("NOT_ASSIGNED_REREVIEWER", ex.getErrorCode());
    }

    @Test
    void execute_nonRereviewer_shouldThrow() {
        seedAssignedCase();

        ActorContext reviewer = new ActorContext("rereviewer-1", "Reviewer",
                EnumSet.of(DomainRole.REVIEWER), Optional.of("H001"));
        assertThrows(RoleDeniedException.class, () -> useCase.execute(reviewer, "c1"));
    }

    @Test
    void execute_wrongHospital_shouldThrowAccessDenied() {
        seedAssignedCase();

        ActorContext wrongHospital = new ActorContext("rereviewer-1", "ReReviewer",
                EnumSet.of(DomainRole.REREVIEWER), Optional.of("OTHER_HOSPITAL"));
        assertThrows(AccessDeniedException.class, () -> useCase.execute(wrongHospital, "c1"));
    }
}
