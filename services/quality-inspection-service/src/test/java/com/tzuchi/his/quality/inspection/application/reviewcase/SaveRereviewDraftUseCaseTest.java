package com.tzuchi.his.quality.inspection.application.reviewcase;

import java.util.List;
import com.tzuchi.his.quality.inspection.application.reviewcase.dto.AnswerInput;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.RoleDeniedException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.SubmissionStatus;
import com.tzuchi.his.quality.inspection.testfixture.CapturingDomainEventPublisher;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryReviewCaseRepository;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryReviewSubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Clock;

class SaveRereviewDraftUseCaseTest {

    private InMemoryReviewCaseRepository caseRepository;
    private InMemoryReviewSubmissionRepository submissionRepository;
    private SaveRereviewDraftUseCase useCase;

    @BeforeEach
    void setUp() {
        caseRepository = new InMemoryReviewCaseRepository();
        submissionRepository = new InMemoryReviewSubmissionRepository();
        useCase = new SaveRereviewDraftUseCase(caseRepository, submissionRepository,
                new CapturingDomainEventPublisher(), Clock.systemDefaultZone());
    }

    private ActorContext rereviewerActor() {
        return new ActorContext("rereviewer-1", "ReReviewer",
                EnumSet.of(DomainRole.REREVIEWER), Optional.of("H001"));
    }

    private void seedCaseAndDraft() {
        ReviewCase rc = ReviewCase.createFromSampling(
                "c1", "b1", "p1", "H001", "SK-001", MedicalRecordType.OUTPATIENT);
        rc.assignReviewer("reviewer-1", Instant.now(), Instant.now());
        rc.startInitialReview("sub-init", "ss-1");
        rc.completeInitialReview(new BigDecimal("90"));
        // D-054：先固定初審，才依品質線判斷流向。
        rc.fixInitialReview(rc.getLatestInitialSubmissionId(), "reviewer-1",
                java.time.Instant.parse("2026-01-01T00:00:00Z"), new BigDecimal("90"), new BigDecimal("80"));
        rc.assignRereviewer("rereviewer-1", Instant.parse("2026-06-30T23:59:00Z"), Instant.now());
        rc.startRereview("rereviewer-1");
        com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseTestSupport.pointRereviewAt(rc, "sub-r1");
        caseRepository.save(rc);

        ReviewSubmission draft = ReviewSubmission.createRereviewDraft("sub-r1", "c1", "rereviewer-1", Instant.now());
        submissionRepository.save(draft);
    }

    @Test
    void execute_happyPath_shouldSaveDraft() {
        seedCaseAndDraft();

        SaveDraftResult result = useCase.execute(rereviewerActor(), "c1", "sub-r1", Collections.emptyList(), "draft comment");

        assertNotNull(result.getSubmissionId());
        assertEquals("DRAFT", result.getStatus());
        assertEquals("REREVIEW", result.getStage());

        ReviewSubmission saved = submissionRepository.findById("sub-r1").orElseThrow();
        assertEquals(SubmissionStatus.DRAFT, saved.getStatus());
        assertEquals("draft comment", saved.getComment());
    }

    @Test
    void execute_wrongActor_shouldThrow() {
        seedCaseAndDraft();

        ActorContext wrongActor = new ActorContext("someone-else", "Other",
                EnumSet.of(DomainRole.REREVIEWER), Optional.of("H001"));
        // D-068 第 2 點
        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () -> useCase.execute(wrongActor, "c1", "sub-r1", Collections.emptyList(), null));
        assertEquals("NOT_ASSIGNED_REREVIEWER", ex.getErrorCode());
    }

    @Test
    void execute_nonRereviewer_shouldThrow() {
        seedCaseAndDraft();

        ActorContext reviewer = new ActorContext("rereviewer-1", "Reviewer",
                EnumSet.of(DomainRole.REVIEWER), Optional.of("H001"));
        assertThrows(RoleDeniedException.class, () -> useCase.execute(reviewer, "c1", "sub-r1", Collections.emptyList(), null));
    }

    @Test
    void execute_wrongHospital_shouldThrowAccessDenied() {
        seedCaseAndDraft();

        ActorContext wrongHospital = new ActorContext("rereviewer-1", "ReReviewer",
                EnumSet.of(DomainRole.REREVIEWER), Optional.of("OTHER_HOSPITAL"));
        assertThrows(AccessDeniedException.class, () -> useCase.execute(wrongHospital, "c1", "sub-r1", Collections.emptyList(), null));
    }
}
