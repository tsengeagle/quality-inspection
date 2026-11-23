package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.RoleDeniedException;
import com.tzuchi.his.quality.inspection.application.reviewcase.command.ReviseRereviewCommand;
import com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException;
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
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
import com.tzuchi.his.quality.inspection.testfixture.CapturingDomainEventPublisher;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryReviewCaseRepository;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryReviewProjectRepository;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryReviewSubmissionRepository;
import com.tzuchi.his.quality.inspection.testfixture.InMemoryScoreSheetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * D-034／D-053：複審的獨立結案命令。
 * 錯誤代碼見 D-053 第 5 點；重複結案見第 6 點；結案與 revision 見第 7 點；回應不含 finalScore 見第 2 點（D-033）。
 */
class CloseRereviewUseCaseTest {

    private static final String CASE_ID = "c1";
    private static final String SUBMISSION_ID = "sub-r1";
    private static final String PROJECT_ID = "p1";
    private static final String HOSPITAL_ID = "H001";
    private static final String SCORE_SHEET_ID = "ss-1";
    private static final String REREVIEWER_ID = "rereviewer-1";
    private static final ZoneId ZONE = ZoneId.of("Asia/Taipei");
    private static final Instant NOW = Instant.parse("2026-11-15T02:00:00Z");

    private InMemoryReviewCaseRepository caseRepo;
    private InMemoryReviewSubmissionRepository submissionRepo;
    private InMemoryReviewProjectRepository projectRepo;
    private InMemoryScoreSheetRepository scoreSheetRepo;
    private CapturingDomainEventPublisher eventPublisher;
    private CloseRereviewUseCase useCase;

    @BeforeEach
    void setUp() {
        caseRepo = new InMemoryReviewCaseRepository();
        submissionRepo = new InMemoryReviewSubmissionRepository();
        projectRepo = new InMemoryReviewProjectRepository();
        scoreSheetRepo = new InMemoryScoreSheetRepository();
        eventPublisher = new CapturingDomainEventPublisher();
        useCase = new CloseRereviewUseCase(caseRepo, submissionRepo, eventPublisher, Clock.fixed(NOW, ZONE));
    }

    private static ActorContext rereviewer(String userId, String hospitalId) {
        return new ActorContext(userId, "ReReviewer", EnumSet.of(DomainRole.REREVIEWER), Optional.of(hospitalId));
    }

    private static ActorContext rereviewer() {
        return rereviewer(REREVIEWER_ID, HOSPITAL_ID);
    }

    /** 複審已提交、尚未結案：案件 REREVIEW_IN_PROGRESS，最新複審提交為 SUBMITTED（D-053 第 4 點）。 */
    private ReviewCase seedSubmittedRereview() {
        ReviewCase rc = ReviewCase.createFromSampling(CASE_ID, "b1", PROJECT_ID, HOSPITAL_ID, "SK-001", MedicalRecordType.OUTPATIENT);
        rc.assignReviewer("reviewer-1", Instant.now(), Instant.now());
        rc.startInitialReview("sub-init", com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetVersionId.of(SCORE_SHEET_ID, 1));
        rc.completeInitialReview(new BigDecimal("90"));
        // D-054：先固定初審，才依品質線判斷流向。 → PENDING_REREVIEW
        rc.fixInitialReview(rc.getLatestInitialSubmissionId(), "reviewer-1",
                java.time.Instant.parse("2026-01-01T00:00:00Z"), new BigDecimal("90"), new BigDecimal("80"));
        rc.assignRereviewer(REREVIEWER_ID, Instant.parse("2026-06-30T23:59:00Z"), Instant.now());
        rc.startRereview(REREVIEWER_ID); // → REREVIEW_IN_PROGRESS
        rc.completeRereview(new BigDecimal("70"), new BigDecimal("0.5"), new BigDecimal("0.5")); // finalScore 80.00
        rc.setLatestRereviewSubmissionId(SUBMISSION_ID);
        caseRepo.save(rc);

        ReviewSubmission sub = ReviewSubmission.createRereviewDraft(SUBMISSION_ID, CASE_ID, REREVIEWER_ID, Instant.now());
        sub.updateDraft(Arrays.asList(
                AnswerItem.ofScored("Q001", "true", new BigDecimal("10.0")),
                AnswerItem.ofScored("Q002", "10", new BigDecimal("5.0"))), new BigDecimal("15.0"), "comment", Instant.now());
        sub.submit(Instant.now());
        submissionRepo.save(sub);
        return rc;
    }

    private void assertUnchanged(CaseStatus expectedStatus) {
        ReviewCase rc = caseRepo.findById(CASE_ID).orElseThrow();
        assertEquals(expectedStatus, rc.getStatus());
        if (expectedStatus != CaseStatus.CLOSED) {
            assertNull(rc.getClosedAt());
        }
        assertTrue(eventPublisher.events.isEmpty(), "rejected close must not publish events");
    }

    // --- 成功 ---

    @Test
    void execute_validSubmitter_shouldCloseCaseAndKeepFinalScore() {
        seedSubmittedRereview();

        CloseRereviewResult result = useCase.execute(rereviewer(), CASE_ID, SUBMISSION_ID);

        assertEquals(CASE_ID, result.getCaseId());
        assertEquals("CLOSED", result.getCaseStatus());
        assertEquals(SUBMISSION_ID, result.getSubmissionId());

        ReviewCase saved = caseRepo.findById(CASE_ID).orElseThrow();
        assertEquals(CaseStatus.CLOSED, saved.getStatus());
        assertEquals(NOW, saved.getClosedAt());
        assertEquals(0, new BigDecimal("80.00").compareTo(saved.getFinalScore()));
        assertEquals(SUBMISSION_ID, saved.getLatestRereviewSubmissionId());

        assertEquals(1, eventPublisher.events.size());
        assertEquals("CASE_CLOSED_AFTER_REREVIEW", eventPublisher.events.get(0).getAction());
        assertEquals(REREVIEWER_ID, eventPublisher.events.get(0).getActorUserId());
        assertFalse(eventPublisher.events.get(0).getDetail().contains("finalScore"),
                "close event detail must not carry finalScore (D-033)");
    }

    @Test
    void result_mustNotExposeFinalScore_D033() {
        assertThrows(NoSuchMethodException.class, () -> CloseRereviewResult.class.getMethod("getFinalScore"));
    }

    // --- 錯誤代碼（D-053 第 5 點），依序：角色 → 載入 → 院區 → 領域規則 ---

    @Test
    void execute_nonRereviewer_shouldRejectBeforeLoading() {
        ActorContext manager = new ActorContext("hm-1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of(HOSPITAL_ID));
        RoleDeniedException e = assertThrows(RoleDeniedException.class,
                () -> useCase.execute(manager, "missing-case", SUBMISSION_ID));
        assertEquals("ROLE_DENIED", e.getErrorCode());
    }

    @Test
    void execute_missingCase_shouldReturnNotFound() {
        // D-053 第 5 點：沿用既有的資源不存在代碼（ResourceNotFoundException → 404）
        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(rereviewer(), "missing-case", SUBMISSION_ID));
    }

    @Test
    void execute_otherHospital_shouldRejectWithScopeMismatch() {
        seedSubmittedRereview();
        AccessDeniedException e = assertThrows(AccessDeniedException.class,
                () -> useCase.execute(rereviewer(REREVIEWER_ID, "H999"), CASE_ID, SUBMISSION_ID));
        assertEquals("HOSPITAL_SCOPE_MISMATCH", e.getErrorCode());
        assertUnchanged(CaseStatus.REREVIEW_IN_PROGRESS);
    }

    @Test
    void execute_cancelledCase_shouldRejectWithCaseTerminal() {
        ReviewCase rc = seedSubmittedRereview();
        rc.cancel("cancelled", Instant.now());
        caseRepo.save(rc);
        DomainStateConflictException e = assertThrows(DomainStateConflictException.class,
                () -> useCase.execute(rereviewer(), CASE_ID, SUBMISSION_ID));
        assertEquals("CASE_TERMINAL", e.getErrorCode());
        assertUnchanged(CaseStatus.CANCELLED);
    }

    @Test
    void execute_secondClose_shouldRejectWithCaseTerminal_D053Point6() {
        seedSubmittedRereview();
        useCase.execute(rereviewer(), CASE_ID, SUBMISSION_ID);
        Instant firstClosedAt = caseRepo.findById(CASE_ID).orElseThrow().getClosedAt();
        eventPublisher.events.clear();

        DomainStateConflictException e = assertThrows(DomainStateConflictException.class,
                () -> useCase.execute(rereviewer(), CASE_ID, SUBMISSION_ID));
        assertEquals("CASE_TERMINAL", e.getErrorCode());
        assertEquals(firstClosedAt, caseRepo.findById(CASE_ID).orElseThrow().getClosedAt());
        assertUnchanged(CaseStatus.CLOSED);
    }

    @Test
    void execute_caseNotInRereview_shouldRejectWithCaseNotInRereviewStage() {
        ReviewCase rc = ReviewCase.createFromSampling(CASE_ID, "b1", PROJECT_ID, HOSPITAL_ID, "SK-001", MedicalRecordType.OUTPATIENT);
        rc.assignReviewer("reviewer-1", Instant.now(), Instant.now());
        rc.startInitialReview("sub-init", com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetVersionId.of(SCORE_SHEET_ID, 1));
        rc.completeInitialReview(new BigDecimal("90"));
        // D-054：先固定初審，才依品質線判斷流向。 → PENDING_REREVIEW
        rc.fixInitialReview(rc.getLatestInitialSubmissionId(), "reviewer-1",
                java.time.Instant.parse("2026-01-01T00:00:00Z"), new BigDecimal("90"), new BigDecimal("80"));
        caseRepo.save(rc);
        // D-069 第 3 點：複審的狀態錯誤共用一個代碼（422）
        DomainRuleViolationException e = assertThrows(DomainRuleViolationException.class,
                () -> useCase.execute(rereviewer(), CASE_ID, SUBMISSION_ID));
        assertEquals("CASE_NOT_IN_REREVIEW_STAGE", e.getErrorCode());
        assertUnchanged(CaseStatus.PENDING_REREVIEW);
    }

    @Test
    void execute_missingSubmission_shouldReturnNotFound() {
        seedSubmittedRereview();
        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(rereviewer(), CASE_ID, "missing-sub"));
        assertUnchanged(CaseStatus.REREVIEW_IN_PROGRESS);
    }

    @Test
    void execute_submissionOfAnotherCase_shouldRejectWithConflict() {
        seedSubmittedRereview();
        ReviewSubmission other = ReviewSubmission.createRereviewDraft("sub-other", "c-other", REREVIEWER_ID, Instant.now());
        other.submit(Instant.now());
        submissionRepo.save(other);
        DomainStateConflictException e = assertThrows(DomainStateConflictException.class,
                () -> useCase.execute(rereviewer(), CASE_ID, "sub-other"));
        assertEquals("CONFLICT", e.getErrorCode());
        assertUnchanged(CaseStatus.REREVIEW_IN_PROGRESS);
    }

    @Test
    void execute_initialStageSubmission_shouldRejectWithConflict() {
        seedSubmittedRereview();
        ReviewSubmission initial = ReviewSubmission.createInitialDraft("sub-init", CASE_ID, REREVIEWER_ID, com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetVersionId.of(SCORE_SHEET_ID, 1), Instant.now());
        initial.submit(Instant.now());
        submissionRepo.save(initial);
        DomainStateConflictException e = assertThrows(DomainStateConflictException.class,
                () -> useCase.execute(rereviewer(), CASE_ID, "sub-init"));
        assertEquals("CONFLICT", e.getErrorCode());
        assertUnchanged(CaseStatus.REREVIEW_IN_PROGRESS);
    }

    @Test
    void execute_staleSubmissionAfterRevision_shouldRejectWithConflict_D053Point7() {
        seedSubmittedRereview();
        seedProject();
        seedScoreSheet();
        ReviseRereviewUseCase revise = new ReviseRereviewUseCase(caseRepo, submissionRepo, projectRepo,
                scoreSheetRepo, new CapturingDomainEventPublisher(), new ScoreCalculationService(), Clock.systemDefaultZone());
        ReviseRereviewCommand reviseCmd = new ReviseRereviewCommand();
        reviseCmd.setCaseId(CASE_ID);
        reviseCmd.setSubmissionId(SUBMISSION_ID);
        reviseCmd.setAnswers(Arrays.asList(AnswerItem.ofRaw("Q001", "true"), AnswerItem.ofRaw("Q002", "20")));
        reviseCmd.setComment("revised");
        revise.execute(rereviewer(), reviseCmd);

        // revision 已先完成：指向舊版的結案回 CONFLICT
        DomainStateConflictException e = assertThrows(DomainStateConflictException.class,
                () -> useCase.execute(rereviewer(), CASE_ID, SUBMISSION_ID));
        assertEquals("CONFLICT", e.getErrorCode());
        assertUnchanged(CaseStatus.REREVIEW_IN_PROGRESS);
    }

    @Test
    void execute_draftSubmission_shouldRejectWithSubmissionNotSubmitted() {
        ReviewCase rc = seedSubmittedRereview();
        ReviewSubmission draft = ReviewSubmission.createRereviewDraft("sub-draft", CASE_ID, REREVIEWER_ID, Instant.now());
        submissionRepo.save(draft);
        rc.setLatestRereviewSubmissionId("sub-draft");
        caseRepo.save(rc);
        DomainStateConflictException e = assertThrows(DomainStateConflictException.class,
                () -> useCase.execute(rereviewer(), CASE_ID, "sub-draft"));
        assertEquals("SUBMISSION_NOT_SUBMITTED", e.getErrorCode());
        assertUnchanged(CaseStatus.REREVIEW_IN_PROGRESS);
    }

    @Test
    void execute_notSubmissionOwner_shouldRejectWithNotSubmissionOwner() {
        seedSubmittedRereview();
        AccessDeniedException e = assertThrows(AccessDeniedException.class,
                () -> useCase.execute(rereviewer("rereviewer-2", HOSPITAL_ID), CASE_ID, SUBMISSION_ID));
        assertEquals("NOT_SUBMISSION_OWNER", e.getErrorCode());
        assertUnchanged(CaseStatus.REREVIEW_IN_PROGRESS);
    }

    // --- 結案之後（D-032、D-053 第 7 點） ---

    @Test
    void reviseAfterClose_shouldRejectWithCaseTerminal() {
        seedSubmittedRereview();
        seedProject();
        seedScoreSheet();
        useCase.execute(rereviewer(), CASE_ID, SUBMISSION_ID);
        BigDecimal closedScore = caseRepo.findById(CASE_ID).orElseThrow().getFinalScore();

        ReviseRereviewUseCase revise = new ReviseRereviewUseCase(caseRepo, submissionRepo, projectRepo,
                scoreSheetRepo, new CapturingDomainEventPublisher(), new ScoreCalculationService(), Clock.systemDefaultZone());
        ReviseRereviewCommand reviseCmd = new ReviseRereviewCommand();
        reviseCmd.setCaseId(CASE_ID);
        reviseCmd.setSubmissionId(SUBMISSION_ID);
        reviseCmd.setAnswers(Arrays.asList(AnswerItem.ofRaw("Q001", "true"), AnswerItem.ofRaw("Q002", "20")));
        reviseCmd.setComment("revised");
        DomainStateConflictException e = assertThrows(DomainStateConflictException.class,
                () -> revise.execute(rereviewer(), reviseCmd));
        assertEquals("CASE_TERMINAL", e.getErrorCode());

        ReviewCase rc = caseRepo.findById(CASE_ID).orElseThrow();
        assertEquals(0, closedScore.compareTo(rc.getFinalScore()));
        assertEquals(SubmissionStatus.SUBMITTED, submissionRepo.findById(SUBMISSION_ID).orElseThrow().getStatus());
    }

    private void seedProject() {
        projectRepo.save(ReviewProject.reconstitute(
                PROJECT_ID, HOSPITAL_ID, "Test Project", null, SCORE_SHEET_ID,
                null, ProjectStatus.ACTIVE, 1, true,
                new BigDecimal("80"), new BigDecimal("0.5"), new BigDecimal("0.5")));
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
}
