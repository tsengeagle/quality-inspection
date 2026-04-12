package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.RoleDeniedException;
import com.tzuchi.his.quality.inspection.application.reviewcase.command.SubmitRereviewCommand;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.project.ProjectStatus;
import com.tzuchi.his.quality.inspection.domain.project.ReviewProject;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.SubmissionStatus;
import com.tzuchi.his.quality.inspection.domain.scoresheet.RequiredRule;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreItem;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetDefinition;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetSection;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreType;
import com.tzuchi.his.quality.inspection.testfixture.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SubmitRereviewUseCaseTest {

    private InMemoryReviewCaseRepository caseRepo;
    private InMemoryReviewSubmissionRepository submissionRepo;
    private InMemoryReviewProjectRepository projectRepo;
    private InMemoryScoreSheetRepository scoreSheetRepo;
    private CapturingDomainEventPublisher eventPublisher;
    private SubmitRereviewUseCase useCase;

    private static final String CASE_ID = "c1";
    private static final String SUBMISSION_ID = "sub-r1";
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
        useCase = new SubmitRereviewUseCase(
                caseRepo, submissionRepo, projectRepo, scoreSheetRepo, eventPublisher, scoreCalcService);
    }

    private ActorContext rereviewerActor() {
        return new ActorContext(REREVIEWER_ID, "ReReviewer",
                EnumSet.of(DomainRole.REREVIEWER), Optional.of(HOSPITAL_ID));
    }

    private void seedProject() {
        // Use reconstitute to seed project with scoring config (threshold=80, 50/50 weights)
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
                new BigDecimal("10"), new BigDecimal("1.0"),
                RequiredRule.ALWAYS_REQUIRED, null));
        section.addItem(new ScoreItem("Q002", "Q002", "Range Question", ScoreType.RANGE,
                new BigDecimal("20"), new BigDecimal("0.5"),
                RequiredRule.OPTIONAL, null));
        ss.addSection(section);
        ss.publishVersion();
        scoreSheetRepo.save(ss);
    }

    private void seedRereviewInProgressCase() {
        ReviewCase rc = ReviewCase.createFromSampling(
                CASE_ID, "b1", PROJECT_ID, HOSPITAL_ID, "SK-001", MedicalRecordType.OUTPATIENT);
        rc.assignReviewer("reviewer-1", LocalDateTime.now());
        rc.startInitialReview("sub-init", SCORE_SHEET_ID);
        rc.completeInitialReview(new BigDecimal("90"), new BigDecimal("80"));
        // score 90 > threshold 80 → PENDING_REREVIEW
        rc.assignRereviewer(REREVIEWER_ID, LocalDateTime.of(2026, 6, 30, 23, 59));
        rc.startRereview(REREVIEWER_ID);
        caseRepo.save(rc);
    }

    private void seedRereviewDraftWithAnswers() {
        ReviewSubmission draft = ReviewSubmission.createRereviewDraft(
                SUBMISSION_ID, CASE_ID, REREVIEWER_ID);
        List<AnswerItem> answers = Arrays.asList(
                AnswerItem.ofScored("Q001", "true", new BigDecimal("10.0")),
                AnswerItem.ofScored("Q002", "15", new BigDecimal("7.5"))
        );
        draft.updateDraft(answers, new BigDecimal("17.5"), "rereview comment");
        submissionRepo.save(draft);
    }

    @Test
    void execute_happyPath_shouldCloseWithWeightedScore() {
        seedProject();
        seedScoreSheet();
        seedRereviewInProgressCase();
        seedRereviewDraftWithAnswers();

        SubmitRereviewCommand cmd = new SubmitRereviewCommand();
        cmd.setCaseId(CASE_ID);
        cmd.setSubmissionId(SUBMISSION_ID);
        // Don't pass answers — use existing draft answers

        SubmitRereviewResult result = useCase.execute(rereviewerActor(), cmd);

        assertNotNull(result.getSubmissionId());
        assertEquals("SUBMITTED", result.getStatus());
        assertEquals(CASE_ID, result.getCaseId());

        ReviewCase saved = caseRepo.findById(CASE_ID).orElseThrow();
        assertEquals(CaseStatus.CLOSED, saved.getStatus());
        assertNotNull(saved.getFinalScore());
        assertNotNull(saved.getClosedAt());

        // finalScore = initialScore(90) * 0.5 + rereviewScore * 0.5
        // rereviewScore = Q001(10*1.0=10) + Q002(15*0.5=7.5) = 17.5
        // finalScore = 90*0.5 + 17.5*0.5 = 45 + 8.75 = 53.75
        assertEquals(0, saved.getFinalScore().compareTo(new BigDecimal("53.75")));

        ReviewSubmission sub = submissionRepo.findById(SUBMISSION_ID).orElseThrow();
        assertEquals(SubmissionStatus.SUBMITTED, sub.getStatus());
        assertTrue(eventPublisher.events.size() >= 1);
    }

    @Test
    void execute_wrongActor_shouldThrow() {
        seedProject();
        seedScoreSheet();
        seedRereviewInProgressCase();
        seedRereviewDraftWithAnswers();

        SubmitRereviewCommand cmd = new SubmitRereviewCommand();
        cmd.setCaseId(CASE_ID);
        cmd.setSubmissionId(SUBMISSION_ID);

        ActorContext wrongActor = new ActorContext("someone-else", "Other",
                EnumSet.of(DomainRole.REREVIEWER), Optional.of(HOSPITAL_ID));
        assertThrows(Exception.class, () -> useCase.execute(wrongActor, cmd));
    }

    @Test
    void execute_nonRereviewer_shouldThrow() {
        seedProject();
        seedScoreSheet();
        seedRereviewInProgressCase();
        seedRereviewDraftWithAnswers();

        SubmitRereviewCommand cmd = new SubmitRereviewCommand();
        cmd.setCaseId(CASE_ID);
        cmd.setSubmissionId(SUBMISSION_ID);

        ActorContext reviewer = new ActorContext(REREVIEWER_ID, "Reviewer",
                EnumSet.of(DomainRole.REVIEWER), Optional.of(HOSPITAL_ID));
        assertThrows(RoleDeniedException.class, () -> useCase.execute(reviewer, cmd));
    }
}
