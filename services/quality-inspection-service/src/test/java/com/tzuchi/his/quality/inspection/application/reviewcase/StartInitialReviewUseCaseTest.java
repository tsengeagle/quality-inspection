package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.RoleDeniedException;
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewStage;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.SubmissionStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.event.InitialReviewStartedEvent;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetDefinition;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetSection;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreItem;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreType;
import com.tzuchi.his.quality.inspection.testfixture.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Clock;

class StartInitialReviewUseCaseTest {

    private InMemoryReviewCaseRepository caseRepo;
    private InMemoryReviewProjectRepository projectRepo;
    private InMemoryScoreSheetRepository scoreSheetRepo;
    private InMemoryReviewSubmissionRepository submissionRepo;
    private CapturingDomainEventPublisher eventPublisher;
    private StartInitialReviewUseCase useCase;

    private static final String CASE_ID = "c1";
    private static final String PROJECT_ID = "p1";
    private static final String HOSPITAL_ID = "H001";
    private static final String REVIEWER_ID = "reviewer-1";
    private static final String SCORE_SHEET_ID = "ss-1";

    @BeforeEach
    void setUp() {
        caseRepo = new InMemoryReviewCaseRepository();
        projectRepo = new InMemoryReviewProjectRepository();
        scoreSheetRepo = new InMemoryScoreSheetRepository();
        submissionRepo = new InMemoryReviewSubmissionRepository();
        eventPublisher = new CapturingDomainEventPublisher();
        useCase = new StartInitialReviewUseCase(
                caseRepo, projectRepo, scoreSheetRepo, submissionRepo, eventPublisher, Clock.systemDefaultZone());
    }

    private ActorContext reviewer() {
        return new ActorContext(REVIEWER_ID, "Test Reviewer",
                EnumSet.of(DomainRole.REVIEWER), Optional.of(HOSPITAL_ID));
    }

    private ReviewCase seedAssignedCase() {
        ReviewCase rc = ReviewCase.createFromSampling(
                CASE_ID, "b1", PROJECT_ID, HOSPITAL_ID, "SK-001", MedicalRecordType.OUTPATIENT);
        rc.assignReviewer(REVIEWER_ID, Instant.parse("2026-02-01T23:59:00Z"), Instant.now());
        caseRepo.save(rc);
        return rc;
    }

    private void seedProjectWithScoreSheet() {
        // Create and publish a score sheet
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft(SCORE_SHEET_ID, PROJECT_ID);
        ScoreSheetSection section = new ScoreSheetSection("sec-1", "Section 1");
        section.addItem(new ScoreItem("Q001", "Q001", "Check", ScoreType.BOOLEAN,
                new BigDecimal("10"), new BigDecimal("1.0")));
        ss.addSection(section);
        ss.publishVersion();
        scoreSheetRepo.save(ss);

        // Create a project bound to the score sheet
        ReviewProject project = ReviewProject.create(PROJECT_ID, HOSPITAL_ID, "Test Project",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        project.bindScoreSheet(com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetVersionId.of(SCORE_SHEET_ID, 1));
        com.tzuchi.his.quality.inspection.testfixture.PublishableProjects.prepare(project);
        project.publishNewVersion();
        project.activate();
        projectRepo.save(project);
    }

    @Test
    void execute_happyPath_shouldCreateSubmissionAndTransitionCase() {
        seedAssignedCase();
        seedProjectWithScoreSheet();

        String submissionId = useCase.execute(reviewer(), CASE_ID);

        assertNotNull(submissionId);

        // Case should be IN_PROGRESS
        ReviewCase savedCase = caseRepo.store.get(CASE_ID);
        assertEquals(CaseStatus.INITIAL_REVIEW_IN_PROGRESS, savedCase.getStatus());
        assertEquals(submissionId, savedCase.getLatestInitialSubmissionId());
        assertEquals(com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetVersionId.of(SCORE_SHEET_ID, 1), savedCase.getScoreSheetVersionRef());

        // Submission should exist as DRAFT
        ReviewSubmission savedSub = submissionRepo.store.get(submissionId);
        assertNotNull(savedSub);
        assertEquals(SubmissionStatus.DRAFT, savedSub.getStatus());
        assertEquals(ReviewStage.INITIAL, savedSub.getStage());
        assertEquals(REVIEWER_ID, savedSub.getReviewerId());
        assertEquals(com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetVersionId.of(SCORE_SHEET_ID, 1), savedSub.getBasedOnScoreSheetVersionId());
    }

    @Test
    void execute_shouldPublishEvent() {
        seedAssignedCase();
        seedProjectWithScoreSheet();

        useCase.execute(reviewer(), CASE_ID);

        assertEquals(1, eventPublisher.events.size());
        assertInstanceOf(InitialReviewStartedEvent.class, eventPublisher.events.get(0));
    }

    @Test
    void execute_wrongRole_shouldThrow() {
        ActorContext manager = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of(HOSPITAL_ID));

        assertThrows(RoleDeniedException.class,
                () -> useCase.execute(manager, CASE_ID));
    }

    @Test
    void execute_notAssignedReviewer_shouldThrow() {
        seedAssignedCase();
        seedProjectWithScoreSheet();

        ActorContext otherReviewer = new ActorContext("other-reviewer", "Other",
                EnumSet.of(DomainRole.REVIEWER), Optional.of(HOSPITAL_ID));

        assertThrows(AccessDeniedException.class,
                () -> useCase.execute(otherReviewer, CASE_ID));
    }

    @Test
    void execute_caseNotFound_shouldThrow() {
        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(reviewer(), "no-such"));
    }

    @Test
    void execute_caseNotPendingInitialReview_shouldThrow() {
        ReviewCase rc = ReviewCase.createFromSampling(
                CASE_ID, "b1", PROJECT_ID, HOSPITAL_ID, "SK-001", MedicalRecordType.OUTPATIENT);
        // Still in SELECTED_PENDING_ASSIGNMENT — not assigned yet
        caseRepo.save(rc);
        seedProjectWithScoreSheet();

        DomainRuleViolationException e = assertThrows(DomainRuleViolationException.class,
                () -> useCase.execute(reviewer(), CASE_ID));
        assertEquals("CASE_NOT_IN_INITIAL_REVIEW_STAGE", e.getErrorCode());
    }

    @Test
    void execute_scoreSheetNotPublished_shouldThrow() {
        seedAssignedCase();

        // Create an unpublished score sheet
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft(SCORE_SHEET_ID, PROJECT_ID);
        ScoreSheetSection section = new ScoreSheetSection("sec-1", "Section 1");
        section.addItem(new ScoreItem("Q001", "Q001", "Check", ScoreType.BOOLEAN,
                new BigDecimal("10"), new BigDecimal("1.0")));
        ss.addSection(section);
        // NOT published
        scoreSheetRepo.save(ss);

        ReviewProject project = ReviewProject.create(PROJECT_ID, HOSPITAL_ID, "Test Project",
                null, EnumSet.of(MedicalRecordType.OUTPATIENT));
        project.bindScoreSheet(com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetVersionId.of(SCORE_SHEET_ID, 1));
        com.tzuchi.his.quality.inspection.testfixture.PublishableProjects.prepare(project);
        project.publishNewVersion();
        project.activate();
        projectRepo.save(project);

        assertThrows(DomainRuleViolationException.class,
                () -> useCase.execute(reviewer(), CASE_ID));
    }

    @Test
    void execute_wrongHospital_shouldThrowAccessDenied() {
        seedAssignedCase();
        seedProjectWithScoreSheet();
        ActorContext wrongHospital = new ActorContext(REVIEWER_ID, "Test Reviewer",
                EnumSet.of(DomainRole.REVIEWER), Optional.of("OTHER_HOSPITAL"));
        assertThrows(AccessDeniedException.class, () -> useCase.execute(wrongHospital, CASE_ID));
    }
}
