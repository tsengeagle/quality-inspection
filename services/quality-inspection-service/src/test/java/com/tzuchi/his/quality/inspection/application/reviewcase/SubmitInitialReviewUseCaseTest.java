package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.RoleDeniedException;
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.SubmissionStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.event.ReviewSubmissionSubmittedEvent;
import com.tzuchi.his.quality.inspection.domain.scoresheet.RequiredRule;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetDefinition;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetSection;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreItem;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreType;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.project.ProjectStatus;
import com.tzuchi.his.quality.inspection.testfixture.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SubmitInitialReviewUseCaseTest {

    private InMemoryReviewCaseRepository caseRepo;
    private InMemoryReviewSubmissionRepository submissionRepo;
    private InMemoryReviewProjectRepository projectRepo;
    private InMemoryScoreSheetRepository scoreSheetRepo;
    private CapturingDomainEventPublisher eventPublisher;
    private SubmitInitialReviewUseCase useCase;

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
        projectRepo = new InMemoryReviewProjectRepository();
        scoreSheetRepo = new InMemoryScoreSheetRepository();
        eventPublisher = new CapturingDomainEventPublisher();
        ScoreCalculationService scoreCalcService = new ScoreCalculationService();
        useCase = new SubmitInitialReviewUseCase(
                caseRepo, submissionRepo, projectRepo, scoreSheetRepo, eventPublisher, scoreCalcService);
        
        // Seed project with threshold 80 (score 17.5 won't escalate)
        seedProject(new BigDecimal("80"));
    }

    private ActorContext reviewer() {
        return new ActorContext(REVIEWER_ID, "Test Reviewer",
                EnumSet.of(DomainRole.REVIEWER), Optional.of(HOSPITAL_ID));
    }

    private void seedProject(BigDecimal threshold) {
        ReviewProject project = ReviewProject.reconstitute(
                PROJECT_ID, HOSPITAL_ID, "Test Project", null, SCORE_SHEET_ID,
                null, ProjectStatus.ACTIVE, 1, true,
                threshold, new BigDecimal("0.5"), new BigDecimal("0.5"));
        projectRepo.save(project);
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

    private ReviewCase seedCaseInProgress() {
        ReviewCase rc = ReviewCase.createFromSampling(
                CASE_ID, "b1", PROJECT_ID, HOSPITAL_ID, "SK-001", MedicalRecordType.OUTPATIENT);
        rc.assignReviewer(REVIEWER_ID, LocalDateTime.of(2026, 2, 1, 23, 59));
        rc.startInitialReview(SUBMISSION_ID, SCORE_SHEET_ID);
        caseRepo.save(rc);
        return rc;
    }

    private ReviewSubmission seedDraftWithAnswers() {
        ReviewSubmission sub = ReviewSubmission.createInitialDraft(
                SUBMISSION_ID, CASE_ID, REVIEWER_ID, SCORE_SHEET_ID);
        List<AnswerItem> answers = Arrays.asList(
                AnswerItem.ofScored("Q001", "true", new BigDecimal("10.0")),
                AnswerItem.ofScored("Q002", "15", new BigDecimal("7.5"))
        );
        sub.updateDraft(answers, new BigDecimal("17.5"), "draft comment");
        submissionRepo.save(sub);
        return sub;
    }

    @Test
    void execute_happyPath_shouldSubmitAndCompleteCase() {
        seedScoreSheet();
        seedCaseInProgress();
        seedDraftWithAnswers();

        SubmitInitialReviewResult result = useCase.execute(reviewer(), CASE_ID);
        assertNotNull(result.getSubmissionId());
        assertEquals("SUBMITTED", result.getStatus());
        assertEquals(CASE_ID, result.getCaseId());
        assertEquals("INITIAL_REVIEW_COMPLETED", result.getCaseStatus());
        assertFalse(result.isRereviewRequired());

        ReviewSubmission saved = submissionRepo.store.get(SUBMISSION_ID);
        assertEquals(SubmissionStatus.SUBMITTED, saved.getStatus());

        ReviewCase savedCase = caseRepo.store.get(CASE_ID);
        assertEquals(CaseStatus.INITIAL_REVIEW_COMPLETED, savedCase.getStatus());
        assertEquals(new BigDecimal("17.5"), savedCase.getFinalScore());
    }

    @Test
    void execute_shouldPublishEvent() {
        seedScoreSheet();
        seedCaseInProgress();
        seedDraftWithAnswers();

        useCase.execute(reviewer(), CASE_ID);

        assertEquals(1, eventPublisher.events.size());
        assertInstanceOf(ReviewSubmissionSubmittedEvent.class, eventPublisher.events.get(0));
    }

    @Test
    void execute_wrongRole_shouldThrow() {
        ActorContext manager = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of(HOSPITAL_ID));

        assertThrows(RoleDeniedException.class,
                () -> useCase.execute(manager, CASE_ID));
    }

    @Test
    void execute_notOwningReviewer_shouldThrow() {
        seedScoreSheet();
        seedCaseInProgress();
        seedDraftWithAnswers();

        ActorContext other = new ActorContext("other-reviewer", "Other",
                EnumSet.of(DomainRole.REVIEWER), Optional.of(HOSPITAL_ID));

        assertThrows(AccessDeniedException.class,
                () -> useCase.execute(other, CASE_ID));
    }

    @Test
    void execute_submissionNotFound_shouldThrow() {
        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(reviewer(), CASE_ID));
    }

    @Test
    void execute_submissionAlreadySubmitted_shouldThrow() {
        seedScoreSheet();
        seedCaseInProgress();
        ReviewSubmission sub = ReviewSubmission.createInitialDraft(
                SUBMISSION_ID, CASE_ID, REVIEWER_ID, SCORE_SHEET_ID);
        sub.updateDraft(Collections.emptyList(), BigDecimal.ZERO, null);
        sub.submit();
        submissionRepo.save(sub);

        assertThrows(DomainStateConflictException.class,
                () -> useCase.execute(reviewer(), CASE_ID));
    }

    @Test
    void execute_missingRequiredItems_shouldThrow() {
        seedScoreSheet();
        seedCaseInProgress();

        ReviewSubmission sub = ReviewSubmission.createInitialDraft(
                SUBMISSION_ID, CASE_ID, REVIEWER_ID, SCORE_SHEET_ID);
        List<AnswerItem> partialAnswers = Collections.singletonList(
                AnswerItem.ofScored("Q002", "15", new BigDecimal("7.5")));
        sub.updateDraft(partialAnswers, new BigDecimal("7.5"), null);
        submissionRepo.save(sub);

        assertThrows(DomainRuleViolationException.class,
                () -> useCase.execute(reviewer(), CASE_ID));
    }

    @Test
    void execute_caseNotInProgress_shouldThrow() {
        seedScoreSheet();
        // Create case but DON'T start initial review (status is PENDING_INITIAL_REVIEW, not IN_PROGRESS)
        ReviewCase rc = ReviewCase.createFromSampling(
                CASE_ID, "b1", PROJECT_ID, HOSPITAL_ID, "SK-001", MedicalRecordType.OUTPATIENT);
        rc.assignReviewer(REVIEWER_ID, LocalDateTime.of(2026, 2, 1, 23, 59));
        // Don't call rc.startInitialReview() — status stays PENDING_INITIAL_REVIEW
        caseRepo.save(rc);

        // Seed a submission manually so it passes submission lookup
        ReviewSubmission sub = ReviewSubmission.createInitialDraft(
                SUBMISSION_ID, CASE_ID, REVIEWER_ID, SCORE_SHEET_ID);
        sub.updateDraft(Collections.emptyList(), BigDecimal.ZERO, null);
        submissionRepo.save(sub);

        assertThrows(DomainStateConflictException.class,
                () -> useCase.execute(reviewer(), CASE_ID));
    }

    @Test
    void execute_scoreAboveThreshold_shouldEscalateToPendingRereview() {
        // Seed project with very low threshold so 17.5 > 0 triggers escalation
        projectRepo.store.clear();
        seedProject(BigDecimal.ZERO); // threshold 0 → any positive score escalates
        
        seedScoreSheet();
        seedCaseInProgress();
        seedDraftWithAnswers();

        useCase.execute(reviewer(), CASE_ID);

        ReviewCase savedCase = caseRepo.store.get(CASE_ID);
        assertEquals(CaseStatus.PENDING_REREVIEW, savedCase.getStatus());
        assertNull(savedCase.getFinalScore()); // no finalScore when escalated
        // Should have 2 events: escalation + submission
        assertTrue(eventPublisher.events.size() >= 2);
    }

    @Test
    void execute_wrongHospital_shouldThrowAccessDenied() {
        seedScoreSheet();
        seedCaseInProgress();
        seedDraftWithAnswers();

        ActorContext wrongHospital = new ActorContext(REVIEWER_ID, "Test Reviewer",
                EnumSet.of(DomainRole.REVIEWER), Optional.of("OTHER_HOSPITAL"));
        assertThrows(AccessDeniedException.class, () -> useCase.execute(wrongHospital, CASE_ID));
    }

    @Test
    void execute_wrongHospitalDifferentUser_scopeCheckFiresBeforeIdentityCheck() {
        seedScoreSheet();
        seedCaseInProgress();
        seedDraftWithAnswers();

        // Different userId AND wrong hospital — scope check must fire before identity check
        ActorContext wrongHospital = new ActorContext("different-user", "Other Reviewer",
                EnumSet.of(DomainRole.REVIEWER), Optional.of("OTHER_HOSPITAL"));
        assertThrows(AccessDeniedException.class, () -> useCase.execute(wrongHospital, CASE_ID));
    }
}
