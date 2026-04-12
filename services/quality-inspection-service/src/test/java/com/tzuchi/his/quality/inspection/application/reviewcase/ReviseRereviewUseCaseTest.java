package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.RoleDeniedException;
import com.tzuchi.his.quality.inspection.application.reviewcase.command.ReviseRereviewCommand;
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.project.ProjectStatus;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.event.RereviewRevisedEvent;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.SubmissionStatus;
import com.tzuchi.his.quality.inspection.domain.scoresheet.*;
import com.tzuchi.his.quality.inspection.testfixture.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ReviseRereviewUseCaseTest {
    private InMemoryReviewCaseRepository caseRepo;
    private InMemoryReviewSubmissionRepository submissionRepo;
    private InMemoryReviewProjectRepository projectRepo;
    private InMemoryScoreSheetRepository scoreSheetRepo;
    private CapturingDomainEventPublisher eventPublisher;
    private ReviseRereviewUseCase useCase;

    private static final String CASE_ID = "c1";
    private static final String OLD_SUBMISSION_ID = "sub-r1";
    private static final String PROJECT_ID = "p1";
    private static final String HOSPITAL_ID = "H001";
    private static final String SCORE_SHEET_ID = "ss-1";
    private static final String REREVIEWER_ID = "rereviewer-1";

    @BeforeEach
    void setUp() {
        caseRepo = new InMemoryReviewCaseRepository();
        submissionRepo = new InMemoryReviewSubmissionRepository();
        projectRepo = new InMemoryReviewProjectRepository();
        scoreSheetRepo = new InMemoryScoreSheetRepository();
        eventPublisher = new CapturingDomainEventPublisher();
        ScoreCalculationService scoreCalcService = new ScoreCalculationService();
        useCase = new ReviseRereviewUseCase(caseRepo, submissionRepo, projectRepo, scoreSheetRepo, eventPublisher, scoreCalcService);
    }

    private ActorContext rereviewerActor() {
        return new ActorContext(REREVIEWER_ID, "ReReviewer", EnumSet.of(DomainRole.REREVIEWER), Optional.of(HOSPITAL_ID));
    }

    private void seedProject() {
        ReviewProject project = ReviewProject.reconstitute(
                PROJECT_ID, HOSPITAL_ID, "Test Project", null, SCORE_SHEET_ID,
                null, ProjectStatus.ACTIVE, 1, true,
                new BigDecimal("80"), new BigDecimal("0.5"), new BigDecimal("0.5"));
        projectRepo.save(project);
    }

    private void seedScoreSheet() {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft(SCORE_SHEET_ID, PROJECT_ID);
        ScoreSheetSection section = new ScoreSheetSection("sec-1", "Section 1");
        section.addItem(new ScoreItem("Q001", "Q001", "Bool Question", ScoreType.BOOLEAN,
                new BigDecimal("10"), new BigDecimal("1.0"), RequiredRule.ALWAYS_REQUIRED, null));
        section.addItem(new ScoreItem("Q002", "Q002", "Range Question", ScoreType.RANGE,
                new BigDecimal("20"), new BigDecimal("0.5"), RequiredRule.OPTIONAL, null));
        ss.addSection(section);
        ss.publishVersion();
        scoreSheetRepo.save(ss);
    }

    private void seedClosedCaseWithSubmittedRereview() {
        // Create case and go through full lifecycle to CLOSED
        ReviewCase rc = ReviewCase.createFromSampling(CASE_ID, "b1", PROJECT_ID, HOSPITAL_ID, "SK-001", MedicalRecordType.OUTPATIENT);
        rc.assignReviewer("reviewer-1", LocalDateTime.now());
        rc.startInitialReview("sub-init", SCORE_SHEET_ID);
        rc.completeInitialReview(new BigDecimal("90"), new BigDecimal("80")); // → PENDING_REREVIEW
        rc.assignRereviewer(REREVIEWER_ID, LocalDateTime.of(2026, 6, 30, 23, 59));
        rc.startRereview(REREVIEWER_ID); // → REREVIEW_IN_PROGRESS
        rc.completeRereview(new BigDecimal("70"), new BigDecimal("0.5"), new BigDecimal("0.5")); // → CLOSED, finalScore = 80
        caseRepo.save(rc);

        // Seed the submitted rereview submission (old submission that will be revised)
        ReviewSubmission sub = ReviewSubmission.createRereviewDraft(OLD_SUBMISSION_ID, CASE_ID, REREVIEWER_ID);
        List<AnswerItem> oldAnswers = Arrays.asList(
                AnswerItem.ofScored("Q001", "true", new BigDecimal("10.0")),
                AnswerItem.ofScored("Q002", "10", new BigDecimal("5.0")));
        sub.updateDraft(oldAnswers, new BigDecimal("15.0"), "original comment");
        sub.submit(); // → SUBMITTED
        submissionRepo.save(sub);
    }

    @Test
    void execute_happyPath_shouldCreateRevisionAndRecalculateFinalScore() {
        seedProject();
        seedScoreSheet();
        seedClosedCaseWithSubmittedRereview();

        // New answers for revision - higher score
        List<AnswerItem> newAnswers = Arrays.asList(
                AnswerItem.ofRaw("Q001", "true"),
                AnswerItem.ofRaw("Q002", "20")); // max score

        ReviseRereviewCommand cmd = new ReviseRereviewCommand();
        cmd.setCaseId(CASE_ID);
        cmd.setSubmissionId(OLD_SUBMISSION_ID);
        cmd.setAnswers(newAnswers);
        cmd.setComment("revised comment");

        useCase.execute(rereviewerActor(), cmd);

        // Verify old submission is SUPERSEDED
        ReviewSubmission oldSub = submissionRepo.findById(OLD_SUBMISSION_ID).orElseThrow();
        assertEquals(SubmissionStatus.SUPERSEDED, oldSub.getStatus());
        assertNotNull(oldSub.getSupersededBySubmissionId());

        // Verify new revision exists and is SUBMITTED
        List<ReviewSubmission> allSubmissions = submissionRepo.findAllByCaseId(CASE_ID);
        ReviewSubmission newRevision = allSubmissions.stream()
                .filter(s -> s.getStatus() == SubmissionStatus.SUBMITTED && !s.getSubmissionId().equals(OLD_SUBMISSION_ID))
                .findFirst()
                .orElseThrow();
        assertEquals(2, newRevision.getRevisionNo());
        assertEquals("revised comment", newRevision.getComment());
        // New score should be Q001(10) + Q002(20*0.5) = 20.0
        assertEquals(0, new BigDecimal("20.0").compareTo(newRevision.getTotalScore()));

        // Verify case final score is recalculated
        // initialScore=90, rereviewScore=20, weights=0.5/0.5
        // finalScore = 90*0.5 + 20*0.5 = 55
        ReviewCase savedCase = caseRepo.findById(CASE_ID).orElseThrow();
        assertEquals(CaseStatus.CLOSED, savedCase.getStatus()); // Status remains CLOSED (D-021)
        assertEquals(0, new BigDecimal("55.0").compareTo(savedCase.getFinalScore()));

        // Verify event published
        assertTrue(eventPublisher.events.stream()
                .anyMatch(e -> e instanceof RereviewRevisedEvent));
    }

    @Test
    void execute_wrongActor_shouldThrow() {
        seedProject();
        seedScoreSheet();
        seedClosedCaseWithSubmittedRereview();

        List<AnswerItem> newAnswers = Arrays.asList(
                AnswerItem.ofRaw("Q001", "true"),
                AnswerItem.ofRaw("Q002", "20"));

        ReviseRereviewCommand cmd = new ReviseRereviewCommand();
        cmd.setCaseId(CASE_ID);
        cmd.setSubmissionId(OLD_SUBMISSION_ID);
        cmd.setAnswers(newAnswers);
        cmd.setComment("revised comment");

        ActorContext wrongActor = new ActorContext("someone-else", "Other", EnumSet.of(DomainRole.REREVIEWER), Optional.of(HOSPITAL_ID));

        assertThrows(DomainRuleViolationException.class, () -> useCase.execute(wrongActor, cmd));
    }

    @Test
    void execute_nonRereviewer_shouldThrow() {
        seedProject();
        seedScoreSheet();
        seedClosedCaseWithSubmittedRereview();

        List<AnswerItem> newAnswers = Arrays.asList(
                AnswerItem.ofRaw("Q001", "true"),
                AnswerItem.ofRaw("Q002", "20"));

        ReviseRereviewCommand cmd = new ReviseRereviewCommand();
        cmd.setCaseId(CASE_ID);
        cmd.setSubmissionId(OLD_SUBMISSION_ID);
        cmd.setAnswers(newAnswers);
        cmd.setComment("revised comment");

        ActorContext reviewer = new ActorContext(REREVIEWER_ID, "Reviewer", EnumSet.of(DomainRole.REVIEWER), Optional.of(HOSPITAL_ID));

        assertThrows(RoleDeniedException.class, () -> useCase.execute(reviewer, cmd));
    }
}
