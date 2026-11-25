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
import com.tzuchi.his.quality.inspection.domain.reviewcase.event.InitialReviewCompletedEvent;
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
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewStage;
import java.time.Clock;

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
                caseRepo, submissionRepo, projectRepo, scoreSheetRepo, eventPublisher, scoreCalcService, Clock.systemDefaultZone());
        
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
        rc.assignReviewer(REVIEWER_ID, Instant.parse("2026-02-01T23:59:00Z"), Instant.now());
        rc.startInitialReview(SUBMISSION_ID, com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetVersionId.of(SCORE_SHEET_ID, 1));
        caseRepo.save(rc);
        return rc;
    }

    private ReviewSubmission seedDraftWithAnswers() {
        ReviewSubmission sub = ReviewSubmission.createInitialDraft(
                SUBMISSION_ID, CASE_ID, REVIEWER_ID, com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetVersionId.of(SCORE_SHEET_ID, 1), Instant.now());
        List<AnswerItem> answers = Arrays.asList(
                AnswerItem.ofScored("Q001", "true", new BigDecimal("10.0")),
                AnswerItem.ofScored("Q002", "15", new BigDecimal("7.5"))
        );
        sub.updateDraft(answers, new BigDecimal("17.5"), "draft comment", Instant.now());
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

        assertEquals(2, eventPublisher.events.size());
        assertInstanceOf(ReviewSubmissionSubmittedEvent.class, eventPublisher.events.get(0));
        // D-067 第 3 點：初審完成，帶專案目前版本的品質線
        InitialReviewCompletedEvent completed = assertInstanceOf(InitialReviewCompletedEvent.class, eventPublisher.events.get(1));
        assertEquals(CASE_ID, completed.getCaseId());
        assertEquals(PROJECT_ID, completed.getProjectId());
        assertEquals(SUBMISSION_ID, completed.getSubmissionId());
        assertEquals(REVIEWER_ID, completed.getReviewerId());
        assertEquals(0, new BigDecimal("80").compareTo(completed.getQualityThreshold()));
    }

    /** RC-SUB-04（D-085 第 8、16 點）：草稿的作答不合法回 VALIDATION_ERROR，在必答題之前，不寫入、不發事件。 */
    @Test
    void execute_draftWithInvalidAnswer_isRejectedBeforeRequiredItems() {
        seedScoreSheet();
        seedCaseInProgress();
        ReviewSubmission sub = ReviewSubmission.createInitialDraft(
                SUBMISSION_ID, CASE_ID, REVIEWER_ID,
                com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetVersionId.of(SCORE_SHEET_ID, 1), Instant.now());
        // 必答的 Q001 沒有作答，Q002 的作答不是數字：先回作答的錯誤
        sub.updateDraft(Collections.singletonList(AnswerItem.ofScored("Q002", "abc", BigDecimal.ZERO)),
                BigDecimal.ZERO, "draft comment", Instant.now());
        submissionRepo.save(sub);

        com.tzuchi.his.quality.inspection.domain.common.DomainValidationException e = assertThrows(
                com.tzuchi.his.quality.inspection.domain.common.DomainValidationException.class,
                () -> useCase.execute(reviewer(), CASE_ID));

        assertEquals("VALIDATION_ERROR", e.getErrorCode());
        assertEquals(SubmissionStatus.DRAFT, submissionRepo.store.get(SUBMISSION_ID).getStatus());
        assertEquals(CaseStatus.INITIAL_REVIEW_IN_PROGRESS, caseRepo.store.get(CASE_ID).getStatus());
        assertTrue(eventPublisher.events.isEmpty());
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
                SUBMISSION_ID, CASE_ID, REVIEWER_ID, com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetVersionId.of(SCORE_SHEET_ID, 1), Instant.now());
        sub.updateDraft(Collections.emptyList(), BigDecimal.ZERO, null, Instant.now());
        sub.submit(Instant.now());
        submissionRepo.save(sub);

        assertThrows(DomainStateConflictException.class,
                () -> useCase.execute(reviewer(), CASE_ID));
    }

    @Test
    void execute_missingRequiredItems_shouldThrow() {
        seedScoreSheet();
        seedCaseInProgress();

        ReviewSubmission sub = ReviewSubmission.createInitialDraft(
                SUBMISSION_ID, CASE_ID, REVIEWER_ID, com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetVersionId.of(SCORE_SHEET_ID, 1), Instant.now());
        List<AnswerItem> partialAnswers = Collections.singletonList(
                AnswerItem.ofScored("Q002", "15", new BigDecimal("7.5")));
        sub.updateDraft(partialAnswers, new BigDecimal("7.5"), null, Instant.now());
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
        rc.assignReviewer(REVIEWER_ID, Instant.parse("2026-02-01T23:59:00Z"), Instant.now());
        // Don't call rc.startInitialReview() — status stays PENDING_INITIAL_REVIEW
        caseRepo.save(rc);

        // Seed a submission manually so it passes submission lookup
        ReviewSubmission sub = ReviewSubmission.createInitialDraft(
                SUBMISSION_ID, CASE_ID, REVIEWER_ID, com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetVersionId.of(SCORE_SHEET_ID, 1), Instant.now());
        sub.updateDraft(Collections.emptyList(), BigDecimal.ZERO, null, Instant.now());
        submissionRepo.save(sub);

        assertThrows(DomainStateConflictException.class,
                () -> useCase.execute(reviewer(), CASE_ID));
    }

    /** D-031、D-054 第 3 點：提交不判斷品質線，分數再高也停在 INITIAL_REVIEW_COMPLETED，等初審固定。 */
    @Test
    void execute_scoreAboveThreshold_staysCompletedUntilTheInitialReviewIsFixed_D054() {
        projectRepo.store.clear();
        seedProject(BigDecimal.ZERO); // any positive score is above the threshold

        seedScoreSheet();
        seedCaseInProgress();
        seedDraftWithAnswers();

        SubmitInitialReviewResult result = useCase.execute(reviewer(), CASE_ID);

        ReviewCase savedCase = caseRepo.store.get(CASE_ID);
        assertEquals(CaseStatus.INITIAL_REVIEW_COMPLETED, savedCase.getStatus());
        assertFalse(savedCase.isInitialReviewFixed());
        assertTrue(eventPublisher.events.stream().noneMatch(
                e -> e instanceof com.tzuchi.his.quality.inspection.domain.reviewcase.event.ReviewCaseEscalatedToRereviewEvent));
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

    @Test
    void restart_submitsReferencedDraftDespiteEqualRevisionNumbers() {
        seedProject(null);
        seedScoreSheet();
        ReviewCase rc = seedCaseInProgress();
        ReviewSubmission old = seedDraftWithAnswers();
        rc.returnToInitialReview("return");
        rc.reopenInitialReview();
        rc.startInitialReview("sub-2", com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetVersionId.of(SCORE_SHEET_ID, 1));
        caseRepo.save(rc);
        ReviewSubmission current = ReviewSubmission.createInitialDraft("sub-2", CASE_ID, REVIEWER_ID, com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetVersionId.of(SCORE_SHEET_ID, 1), Instant.now());
        current.updateDraft(old.getAnswerSet(), old.getTotalScore(), "current", Instant.now());
        submissionRepo.save(current);
        InMemoryReviewSubmissionRepository selected = spy(submissionRepo);
        doReturn(Optional.of(old)).when(selected).findLatestByCaseIdAndStage(CASE_ID, ReviewStage.INITIAL);
        SubmitInitialReviewUseCase submitting = new SubmitInitialReviewUseCase(
                caseRepo, selected, projectRepo, scoreSheetRepo, eventPublisher, new ScoreCalculationService(), Clock.systemDefaultZone());
        assertEquals(1, old.getRevisionNo());
        assertEquals(1, current.getRevisionNo());
        SubmitInitialReviewResult result = submitting.execute(reviewer(), CASE_ID);
        assertEquals("sub-2", result.getSubmissionId());
        assertEquals(SubmissionStatus.SUBMITTED, current.getStatus());
        assertEquals(SubmissionStatus.DRAFT, old.getStatus());
        assertEquals(new BigDecimal("17.5"), old.getTotalScore());
        assertEquals("draft comment", old.getComment());
        assertEquals("15", old.getAnswerSet().get(1).getAnswerValue());
        assertNull(old.getSubmittedAt());
        verify(selected, never()).findLatestByCaseIdAndStage(CASE_ID, ReviewStage.INITIAL);
    }
}
