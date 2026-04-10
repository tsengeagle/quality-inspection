package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.UnauthorizedException;
import com.tzuchi.his.quality.inspection.application.reviewcase.dto.AnswerInput;
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.SubmissionStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.event.ReviewSubmissionRevisedEvent;
import com.tzuchi.his.quality.inspection.domain.scoresheet.*;
import com.tzuchi.his.quality.inspection.interfaces.rest.BadRequestException;
import com.tzuchi.his.quality.inspection.testfixture.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ReviseInitialReviewUseCaseTest {

    private InMemoryReviewCaseRepository caseRepo;
    private InMemoryReviewSubmissionRepository submissionRepo;
    private InMemoryScoreSheetRepository scoreSheetRepo;
    private CapturingDomainEventPublisher eventPublisher;
    private ReviseInitialReviewUseCase useCase;

    private static final String CASE_ID = "c1";
    private static final String SUBMISSION_ID = "sub-1";
    private static final String PROJECT_ID = "p1";
    private static final String HOSPITAL_ID = "H001";
    private static final String REVIEWER_ID = "reviewer-1";
    private static final String SCORE_SHEET_ID = "ss-1";

    @BeforeEach
    void setUp() {
        caseRepo = new InMemoryReviewCaseRepository();
        submissionRepo = new InMemoryReviewSubmissionRepository();
        scoreSheetRepo = new InMemoryScoreSheetRepository();
        eventPublisher = new CapturingDomainEventPublisher();
        ScoreCalculationService scoreCalcService = new ScoreCalculationService();
        useCase = new ReviseInitialReviewUseCase(
                caseRepo, submissionRepo, scoreSheetRepo, eventPublisher, scoreCalcService);
    }

    private ActorContext reviewer() {
        return new ActorContext(REVIEWER_ID, "Test Reviewer",
                EnumSet.of(DomainRole.REVIEWER), Optional.of(HOSPITAL_ID));
    }

    private ScoreSheetDefinition seedScoreSheet() {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft(SCORE_SHEET_ID, PROJECT_ID);
        ScoreSheetSection section = new ScoreSheetSection("sec-1", "Section 1");
        section.addItem(new ScoreItem("Q001", "Q001", "Bool Question", ScoreType.BOOLEAN,
                new BigDecimal("10"), new BigDecimal("1.0"),
                RequiredRule.ALWAYS_REQUIRED, null));
        section.addItem(new ScoreItem("Q002", "Q002", "Range Question", ScoreType.RANGE,
                new BigDecimal("20"), new BigDecimal("0.5"),
                RequiredRule.OPTIONAL, null));
        ss.addSection(section);
        ss.publishVersion();
        scoreSheetRepo.save(ss);
        return ss;
    }

    private ReviewCase seedCompletedCase() {
        ReviewCase rc = ReviewCase.createFromSampling(
                CASE_ID, "b1", PROJECT_ID, HOSPITAL_ID, "SK-001", MedicalRecordType.OUTPATIENT);
        rc.assignReviewer(REVIEWER_ID, LocalDateTime.of(2026, 2, 1, 23, 59));
        rc.startInitialReview(SUBMISSION_ID, SCORE_SHEET_ID);
        rc.completeInitialReview(new BigDecimal("17.5"), null);
        caseRepo.save(rc);
        return rc;
    }

    private ReviewSubmission seedSubmittedSubmission() {
        ReviewSubmission sub = ReviewSubmission.createInitialDraft(
                SUBMISSION_ID, CASE_ID, REVIEWER_ID, SCORE_SHEET_ID);
        List<AnswerItem> answers = Arrays.asList(
                AnswerItem.ofScored("Q001", "true", new BigDecimal("10.0")),
                AnswerItem.ofScored("Q002", "15", new BigDecimal("7.5"))
        );
        sub.updateDraft(answers, new BigDecimal("17.5"), "initial comment");
        sub.submit();
        submissionRepo.save(sub);
        return sub;
    }

    private List<AnswerInput> revisedAnswers() {
        return Arrays.asList(
                new AnswerInput("Q001", "true"),
                new AnswerInput("Q002", "18")
        );
    }

    @Test
    void revise_happyPath_createsNewSubmission() {
        seedScoreSheet();
        seedCompletedCase();
        seedSubmittedSubmission();

        RevisionResult result = useCase.execute(
                reviewer(), CASE_ID, SUBMISSION_ID, revisedAnswers(), "revised comment");

        assertNotNull(result.getNewSubmissionId());
        assertEquals(2, result.getRevisionNo());
        assertEquals("SUBMITTED", result.getStatus());

        // Old submission is SUPERSEDED
        ReviewSubmission oldSub = submissionRepo.store.get(SUBMISSION_ID);
        assertEquals(SubmissionStatus.SUPERSEDED, oldSub.getStatus());
        assertEquals(result.getNewSubmissionId(), oldSub.getSupersededBySubmissionId());

        // New submission exists and is SUBMITTED
        ReviewSubmission newSub = submissionRepo.store.get(result.getNewSubmissionId());
        assertNotNull(newSub);
        assertEquals(SubmissionStatus.SUBMITTED, newSub.getStatus());
        assertEquals(2, newSub.getRevisionNo());
        assertEquals("revised comment", newSub.getComment());

        // Case finalScore updated
        ReviewCase savedCase = caseRepo.store.get(CASE_ID);
        assertEquals(result.getTotalScore(), savedCase.getFinalScore());

        // Event published
        assertEquals(1, eventPublisher.events.size());
        assertInstanceOf(ReviewSubmissionRevisedEvent.class, eventPublisher.events.get(0));
    }

    @Test
    void revise_actorNotReviewer_throwsUnauthorized() {
        ActorContext manager = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of(HOSPITAL_ID));

        assertThrows(UnauthorizedException.class,
                () -> useCase.execute(manager, CASE_ID, SUBMISSION_ID, revisedAnswers(), null));
    }

    @Test
    void revise_caseNotFound_throwsNotFound() {
        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(reviewer(), "nonexistent", SUBMISSION_ID, revisedAnswers(), null));
    }

    @Test
    void revise_caseClosed_throwsConflict() {
        seedScoreSheet();
        // Use reconstitute() since forceStatus is package-private in domain.reviewcase
        ReviewCase rc = ReviewCase.reconstitute(
                CASE_ID, "b1", PROJECT_ID, HOSPITAL_ID, "SK-001", MedicalRecordType.OUTPATIENT,
                CaseStatus.CLOSED, null, null,
                null, null, null, LocalDateTime.now(),
                false, null, null, null, null, null,
                null, null, null);
        caseRepo.save(rc);

        assertThrows(DomainStateConflictException.class,
                () -> useCase.execute(reviewer(), CASE_ID, SUBMISSION_ID, revisedAnswers(), null));
    }

    @Test
    void revise_caseCancelled_throwsConflict() {
        seedScoreSheet();
        // Use reconstitute() since forceStatus is package-private in domain.reviewcase
        ReviewCase rc = ReviewCase.reconstitute(
                CASE_ID, "b1", PROJECT_ID, HOSPITAL_ID, "SK-001", MedicalRecordType.OUTPATIENT,
                CaseStatus.CANCELLED, null, null,
                null, null, LocalDateTime.now(), null,
                false, null, null, null, null, null,
                null, null, null);
        caseRepo.save(rc);

        assertThrows(DomainStateConflictException.class,
                () -> useCase.execute(reviewer(), CASE_ID, SUBMISSION_ID, revisedAnswers(), null));
    }

    @Test
    void revise_submissionNotFound_throwsNotFound() {
        seedScoreSheet();
        seedCompletedCase();

        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(reviewer(), CASE_ID, "nonexistent", revisedAnswers(), null));
    }

    @Test
    void revise_submissionCaseIdMismatch_throwsBadRequest() {
        seedScoreSheet();
        seedCompletedCase();

        // Submission belongs to different case
        ReviewSubmission sub = ReviewSubmission.createInitialDraft(
                SUBMISSION_ID, "different-case", REVIEWER_ID, SCORE_SHEET_ID);
        sub.submit();
        submissionRepo.save(sub);

        assertThrows(BadRequestException.class,
                () -> useCase.execute(reviewer(), CASE_ID, SUBMISSION_ID, revisedAnswers(), null));
    }

    @Test
    void revise_submissionNotInitialStage_throwsRuleViolation() {
        seedScoreSheet();
        seedCompletedCase();

        // Create a REREVIEW stage submission via reconstitute
        ReviewSubmission sub = ReviewSubmission.reconstitute(
                SUBMISSION_ID, CASE_ID, com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewStage.REREVIEW,
                REVIEWER_ID, SubmissionStatus.SUBMITTED, 1,
                Collections.emptyList(), BigDecimal.ZERO, null, LocalDateTime.now(),
                SCORE_SHEET_ID, null, null, LocalDateTime.now(), LocalDateTime.now());
        submissionRepo.save(sub);

        assertThrows(DomainRuleViolationException.class,
                () -> useCase.execute(reviewer(), CASE_ID, SUBMISSION_ID, revisedAnswers(), null));
    }

    @Test
    void revise_actorNotSubmissionOwner_throwsAccessDenied() {
        seedScoreSheet();
        seedCompletedCase();
        seedSubmittedSubmission();

        ActorContext other = new ActorContext("other-reviewer", "Other",
                EnumSet.of(DomainRole.REVIEWER), Optional.of(HOSPITAL_ID));

        assertThrows(AccessDeniedException.class,
                () -> useCase.execute(other, CASE_ID, SUBMISSION_ID, revisedAnswers(), null));
    }

    @Test
    void revise_submissionIsDraft_throwsConflict() {
        seedScoreSheet();
        seedCompletedCase();

        // Submission in DRAFT status (not submitted)
        ReviewSubmission sub = ReviewSubmission.createInitialDraft(
                SUBMISSION_ID, CASE_ID, REVIEWER_ID, SCORE_SHEET_ID);
        submissionRepo.save(sub);

        assertThrows(DomainStateConflictException.class,
                () -> useCase.execute(reviewer(), CASE_ID, SUBMISSION_ID, revisedAnswers(), null));
    }

    @Test
    void revise_scoreSheetNotFound_throwsNotFound() {
        // Case is completed but score sheet is NOT seeded
        seedCompletedCase();
        seedSubmittedSubmission();

        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(reviewer(), CASE_ID, SUBMISSION_ID, revisedAnswers(), null));
    }
}