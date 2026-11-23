package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.RoleDeniedException;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewStage;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Clock;

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
                caseRepo, submissionRepo, projectRepo, scoreSheetRepo, eventPublisher, scoreCalcService, Clock.systemDefaultZone());
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
        rc.assignReviewer("reviewer-1", Instant.now(), Instant.now());
        rc.startInitialReview("sub-init", com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetVersionId.of(SCORE_SHEET_ID, 1));
        rc.completeInitialReview(new BigDecimal("90"));
        // D-054：先固定初審，才依品質線判斷流向。
        rc.fixInitialReview(rc.getLatestInitialSubmissionId(), "reviewer-1",
                java.time.Instant.parse("2026-01-01T00:00:00Z"), new BigDecimal("90"), new BigDecimal("80"));
        // score 90 > threshold 80 → PENDING_REREVIEW
        rc.assignRereviewer(REREVIEWER_ID, Instant.parse("2026-06-30T23:59:00Z"), Instant.now());
        rc.startRereview(REREVIEWER_ID);
        rc.setLatestRereviewSubmissionId(SUBMISSION_ID);
        caseRepo.save(rc);
    }

    private void seedRereviewDraftWithAnswers() {
        ReviewSubmission draft = ReviewSubmission.createRereviewDraft(
                SUBMISSION_ID, CASE_ID, REREVIEWER_ID, Instant.now());
        List<AnswerItem> answers = Arrays.asList(
                AnswerItem.ofScored("Q001", "true", new BigDecimal("10.0")),
                AnswerItem.ofScored("Q002", "15", new BigDecimal("7.5"))
        );
        draft.updateDraft(answers, new BigDecimal("17.5"), "rereview comment", Instant.now());
        submissionRepo.save(draft);
    }

    @Test
    void execute_happyPath_shouldSubmitAndCalculateScore() {
        seedProject();
        seedScoreSheet();
        seedRereviewInProgressCase();
        seedRereviewDraftWithAnswers();

        // Don't pass answers — use existing draft answers

        SubmitRereviewResult result = useCase.execute(rereviewerActor(), CASE_ID, SUBMISSION_ID, null, null);

        assertNotNull(result.getSubmissionId());
        assertEquals("SUBMITTED", result.getStatus());
        assertEquals(CASE_ID, result.getCaseId());
        // D-032／D-053 第 4 點：提交後案件維持 REREVIEW_IN_PROGRESS
        assertEquals("REREVIEW_IN_PROGRESS", result.getCaseStatus());

        ReviewCase saved = caseRepo.findById(CASE_ID).orElseThrow();
        assertNotNull(saved.getFinalScore());

        // finalScore = initialScore(90) * 0.5 + rereviewScore * 0.5
        // rereviewScore = Q001(10*1.0=10) + Q002(15*0.5=7.5) = 17.5
        // finalScore = 90*0.5 + 17.5*0.5 = 45 + 8.75 = 53.75
        assertEquals(0, saved.getFinalScore().compareTo(new BigDecimal("53.75")));

        ReviewSubmission sub = submissionRepo.findById(SUBMISSION_ID).orElseThrow();
        assertEquals(SubmissionStatus.SUBMITTED, sub.getStatus());
        assertTrue(eventPublisher.events.size() >= 1);
    }

    @Test
    void result_mustNotExposeFinalScore_D033() {
        assertThrows(NoSuchMethodException.class, () -> SubmitRereviewResult.class.getMethod("getFinalScore"));
    }

    @Test
    void execute_submitMustNotCloseCase_D032() {
        seedProject();
        seedScoreSheet();
        seedRereviewInProgressCase();
        seedRereviewDraftWithAnswers();
        useCase.execute(rereviewerActor(), CASE_ID, SUBMISSION_ID, null, null);
        ReviewCase saved = caseRepo.findById(CASE_ID).orElseThrow();
        assertNotEquals(CaseStatus.CLOSED, saved.getStatus());
        assertNull(saved.getClosedAt());
    }

    @Test
    void execute_wrongActor_shouldThrow() {
        seedProject();
        seedScoreSheet();
        seedRereviewInProgressCase();
        seedRereviewDraftWithAnswers();


        ActorContext wrongActor = new ActorContext("someone-else", "Other",
                EnumSet.of(DomainRole.REREVIEWER), Optional.of(HOSPITAL_ID));
        // D-068 第 2 點
        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () -> useCase.execute(wrongActor, CASE_ID, SUBMISSION_ID, null, null));
        assertEquals("NOT_ASSIGNED_REREVIEWER", ex.getErrorCode());
    }

    @Test
    void execute_nonRereviewer_shouldThrow() {
        seedProject();
        seedScoreSheet();
        seedRereviewInProgressCase();
        seedRereviewDraftWithAnswers();


        ActorContext reviewer = new ActorContext(REREVIEWER_ID, "Reviewer",
                EnumSet.of(DomainRole.REVIEWER), Optional.of(HOSPITAL_ID));
        assertThrows(RoleDeniedException.class, () -> useCase.execute(reviewer, CASE_ID, SUBMISSION_ID, null, null));
    }

    @Test
    void execute_wrongHospital_shouldThrowAccessDenied() {
        seedProject();
        seedScoreSheet();
        seedRereviewInProgressCase();
        seedRereviewDraftWithAnswers();


        ActorContext wrongHospital = new ActorContext(REREVIEWER_ID, "ReReviewer",
                EnumSet.of(DomainRole.REREVIEWER), Optional.of("OTHER_HOSPITAL"));
        assertThrows(AccessDeniedException.class, () -> useCase.execute(wrongHospital, CASE_ID, SUBMISSION_ID, null, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"otherCase", "initialStage", "otherOwner", "stale", "missingPointer", "submitted", "superseded", "closed", "cancelled", "wrongHospital"})
    void execute_invalidSubmissionOrCase_rejectsWithoutMutation(String scenario) {
        seedProject();
        seedScoreSheet();
        seedRereviewInProgressCase();
        seedRereviewDraftWithAnswers();
        ReviewCase reviewCase = caseRepo.findById(CASE_ID).orElseThrow();
        ReviewSubmission original = submissionRepo.findById(SUBMISSION_ID).orElseThrow();
        ReviewSubmission target = original;
        if (scenario.equals("otherCase") || scenario.equals("initialStage") || scenario.equals("otherOwner")) {
            target = ReviewSubmission.reconstitute(original.getSubmissionId(),
                    scenario.equals("otherCase") ? "other-case" : CASE_ID,
                    scenario.equals("initialStage") ? ReviewStage.INITIAL : ReviewStage.REREVIEW,
                    scenario.equals("otherOwner") ? "other-owner" : REREVIEWER_ID,
                    original.getStatus(), original.getRevisionNo(), original.getAnswerSet(),
                    original.getTotalScore(), original.getComment(), original.getSubmittedAt(),
                    original.getBasedOnScoreSheetVersionId(), original.getSupersededBySubmissionId(),
                    original.getAttachmentRefsJson(), original.getCreatedAt(), original.getUpdatedAt());
            submissionRepo.save(target);
        }
        if (scenario.equals("draft")) {
            target = ReviewSubmission.createRereviewDraft(SUBMISSION_ID, CASE_ID, REREVIEWER_ID, Instant.now());
            submissionRepo.save(target);
        }
        if (scenario.equals("submitted")) target.submit(Instant.now());
        if (scenario.equals("superseded")) {
            if (target.getStatus() == SubmissionStatus.DRAFT) target.submit(Instant.now());
            target.markSuperseded("newer-submission", Instant.now());
        }
        if (scenario.equals("stale")) {
            ReviewSubmission newer = ReviewSubmission.createRereviewDraft("newer-submission", CASE_ID, REREVIEWER_ID, Instant.now());
            submissionRepo.save(newer);
            reviewCase.setLatestRereviewSubmissionId(newer.getSubmissionId());
        }
        if (scenario.equals("missingPointer")) reviewCase.setLatestRereviewSubmissionId(null);
        if (scenario.equals("closed")) { reviewCase.completeRereview(BigDecimal.TEN, new BigDecimal("0.5"), new BigDecimal("0.5")); reviewCase.closeAfterRereview(Instant.now()); }
        if (scenario.equals("cancelled")) reviewCase.cancel("cancelled", Instant.now());
        caseRepo.save(reviewCase);
        List<Object> beforeCase = caseState(reviewCase);
        Map<String, List<Object>> beforeSubmissions = submissionState();

        ActorContext actor = scenario.equals("wrongHospital")
                ? new ActorContext(REREVIEWER_ID, "ReReviewer", EnumSet.of(DomainRole.REREVIEWER), Optional.of("OTHER"))
                : rereviewerActor();
        RuntimeException failure = assertThrows(RuntimeException.class, () -> useCase.execute(actor, CASE_ID, SUBMISSION_ID, null, null));
        assertTrue(failure instanceof DomainStateConflictException
                || failure instanceof com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException
                || failure instanceof com.tzuchi.his.quality.inspection.application.common.AccessDeniedException,
                "Expected an explicit guard rejection, not an incidental failure: " + failure);
        assertEquals(beforeCase, caseState(caseRepo.findById(CASE_ID).orElseThrow()));
        assertEquals(beforeSubmissions, submissionState());
        assertTrue(eventPublisher.events.isEmpty());
    }

    private List<Object> caseState(ReviewCase rc) {
        return Arrays.asList(rc.getStatus(), rc.getFinalScore(), rc.getInitialReviewScore(),
                rc.getLatestInitialSubmissionId(), rc.getLatestRereviewSubmissionId(), rc.getClosedAt(), rc.getCancelledAt());
    }

    private Map<String, List<Object>> submissionState() {
        Map<String, List<Object>> state = new LinkedHashMap<>();
        submissionRepo.store.forEach((id, sub) -> state.put(id, Arrays.asList(
                sub.getCaseId(), sub.getStage(), sub.getReviewerId(), sub.getStatus(), sub.getRevisionNo(),
                new ArrayList<>(sub.getAnswerSet()), sub.getTotalScore(), sub.getComment(), sub.getSubmittedAt(),
                sub.getSupersededBySubmissionId(), sub.getCreatedAt(), sub.getUpdatedAt())));
        return state;
    }
}
