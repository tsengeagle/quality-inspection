package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.RoleDeniedException;
import com.tzuchi.his.quality.inspection.application.reviewcase.command.SaveRereviewDraftCommand;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.SubmissionStatus;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryReviewCaseRepository;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryReviewSubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SaveRereviewDraftUseCaseTest {

    private InMemoryReviewCaseRepository caseRepository;
    private InMemoryReviewSubmissionRepository submissionRepository;
    private SaveRereviewDraftUseCase useCase;

    @BeforeEach
    void setUp() {
        caseRepository = new InMemoryReviewCaseRepository();
        submissionRepository = new InMemoryReviewSubmissionRepository();
        useCase = new SaveRereviewDraftUseCase(caseRepository, submissionRepository);
    }

    private ActorContext rereviewerActor() {
        return new ActorContext("rereviewer-1", "ReReviewer",
                EnumSet.of(DomainRole.REREVIEWER), Optional.of("H001"));
    }

    private void seedCaseAndDraft() {
        ReviewCase rc = ReviewCase.createFromSampling(
                "c1", "b1", "p1", "H001", "SK-001", MedicalRecordType.OUTPATIENT);
        rc.assignReviewer("reviewer-1", LocalDateTime.now());
        rc.startInitialReview("sub-init", "ss-1");
        rc.completeInitialReview(new BigDecimal("90"), new BigDecimal("80"));
        rc.assignRereviewer("rereviewer-1", LocalDateTime.of(2026, 6, 30, 23, 59));
        rc.startRereview("rereviewer-1");
        caseRepository.save(rc);

        ReviewSubmission draft = ReviewSubmission.createRereviewDraft("sub-r1", "c1", "rereviewer-1");
        submissionRepository.save(draft);
    }

    @Test
    void execute_happyPath_shouldSaveDraft() {
        seedCaseAndDraft();
        SaveRereviewDraftCommand cmd = new SaveRereviewDraftCommand();
        cmd.setCaseId("c1");
        cmd.setSubmissionId("sub-r1");
        cmd.setAnswers(Collections.emptyList());
        cmd.setTotalScore(BigDecimal.ZERO);
        cmd.setComment("draft comment");

        SaveDraftResult result = useCase.execute(rereviewerActor(), cmd);

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
        SaveRereviewDraftCommand cmd = new SaveRereviewDraftCommand();
        cmd.setCaseId("c1");
        cmd.setSubmissionId("sub-r1");
        cmd.setAnswers(Collections.emptyList());

        ActorContext wrongActor = new ActorContext("someone-else", "Other",
                EnumSet.of(DomainRole.REREVIEWER), Optional.of("H001"));
        assertThrows(Exception.class, () -> useCase.execute(wrongActor, cmd));
    }

    @Test
    void execute_nonRereviewer_shouldThrow() {
        seedCaseAndDraft();
        SaveRereviewDraftCommand cmd = new SaveRereviewDraftCommand();
        cmd.setCaseId("c1");
        cmd.setSubmissionId("sub-r1");
        cmd.setAnswers(Collections.emptyList());

        ActorContext reviewer = new ActorContext("rereviewer-1", "Reviewer",
                EnumSet.of(DomainRole.REVIEWER), Optional.of("H001"));
        assertThrows(RoleDeniedException.class, () -> useCase.execute(reviewer, cmd));
    }

    @Test
    void execute_wrongHospital_shouldThrowAccessDenied() {
        seedCaseAndDraft();
        SaveRereviewDraftCommand cmd = new SaveRereviewDraftCommand();
        cmd.setCaseId("c1");
        cmd.setSubmissionId("sub-r1");
        cmd.setAnswers(Collections.emptyList());

        ActorContext wrongHospital = new ActorContext("rereviewer-1", "ReReviewer",
                EnumSet.of(DomainRole.REREVIEWER), Optional.of("OTHER_HOSPITAL"));
        assertThrows(AccessDeniedException.class, () -> useCase.execute(wrongHospital, cmd));
    }
}
