package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.RoleDeniedException;
import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewStage;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Clock;

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
        useCase = new ReviseRereviewUseCase(caseRepo, submissionRepo, projectRepo, scoreSheetRepo, eventPublisher, scoreCalcService, Clock.systemDefaultZone(),
                () -> java.util.UUID.randomUUID().toString());
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

    private void seedInProgressCaseWithSubmittedRereview() {
        // Submitted but not closed: D-032 permits revision only before terminal state.
        ReviewCase rc = ReviewCase.createFromSampling(CASE_ID, "b1", PROJECT_ID, HOSPITAL_ID, "SK-001", MedicalRecordType.OUTPATIENT);
        rc.assignReviewer("reviewer-1", Instant.now(), Instant.now());
        rc.startInitialReview("sub-init", com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetVersionId.of(SCORE_SHEET_ID, 1));
        rc.completeInitialReview(new BigDecimal("90"));
        // D-054：先固定初審，才依品質線判斷流向。 → PENDING_REREVIEW
        rc.fixInitialReview(rc.getLatestInitialSubmissionId(), "reviewer-1",
                java.time.Instant.parse("2026-01-01T00:00:00Z"), new BigDecimal("90"), new BigDecimal("80"));
        rc.assignRereviewer(REREVIEWER_ID, Instant.parse("2026-06-30T23:59:00Z"), Instant.now());
        rc.startRereview(REREVIEWER_ID); // → REREVIEW_IN_PROGRESS
        rc.completeRereview(new BigDecimal("70"), new BigDecimal("0.5"), new BigDecimal("0.5"));
        com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseTestSupport.pointRereviewAt(rc, OLD_SUBMISSION_ID);
        caseRepo.save(rc);

        // Seed the submitted rereview submission (old submission that will be revised)
        ReviewSubmission sub = ReviewSubmission.createRereviewDraft(OLD_SUBMISSION_ID, CASE_ID, REREVIEWER_ID, Instant.now());
        List<AnswerItem> oldAnswers = Arrays.asList(
                AnswerItem.ofScored("Q001", "true", new BigDecimal("10.0")),
                AnswerItem.ofScored("Q002", "10", new BigDecimal("5.0")));
        sub.updateDraft(oldAnswers, new BigDecimal("15.0"), "original comment", Instant.now());
        sub.submit(Instant.now()); // → SUBMITTED
        submissionRepo.save(sub);
    }

    @Test
    void execute_happyPath_shouldCreateRevisionAndRecalculateFinalScore() {
        seedProject();
        seedScoreSheet();
        seedInProgressCaseWithSubmittedRereview();

        // New answers for revision - higher score
        List<AnswerItem> newAnswers = Arrays.asList(
                AnswerItem.ofRaw("Q001", "true"),
                AnswerItem.ofRaw("Q002", "20")); // max score


        RevisionResult result = useCase.execute(rereviewerActor(), CASE_ID, OLD_SUBMISSION_ID, RevisionTestInputs.of(newAnswers), "revised comment");

        assertNotNull(result.getNewSubmissionId());
        assertEquals(OLD_SUBMISSION_ID, result.getOldSubmissionId());
        assertEquals(CASE_ID, result.getCaseId());
        assertEquals("REREVIEW", result.getStage());
        assertEquals("SUBMITTED", result.getStatus());

        // Verify old submission is SUPERSEDED
        ReviewSubmission oldSub = submissionRepo.findById(OLD_SUBMISSION_ID).orElseThrow();
        assertEquals(SubmissionStatus.SUPERSEDED, oldSub.getStatus());
        assertEquals(result.getNewSubmissionId(), oldSub.getSupersededBySubmissionId());
        assertEquals("original comment", oldSub.getComment());
        assertEquals(0, new BigDecimal("15.0").compareTo(oldSub.getTotalScore()));
        assertEquals("10", oldSub.getAnswerSet().get(1).getAnswerValue());

        // Verify new revision exists and is SUBMITTED
        List<ReviewSubmission> allSubmissions = submissionRepo.findAllByCaseId(CASE_ID);
        assertEquals(2, allSubmissions.size());
        ReviewSubmission newRevision = allSubmissions.stream()
                .filter(s -> s.getStatus() == SubmissionStatus.SUBMITTED && !s.getSubmissionId().equals(OLD_SUBMISSION_ID))
                .findFirst()
                .orElseThrow();
        assertEquals(2, newRevision.getRevisionNo());
        assertEquals(oldSub.getCaseId(), newRevision.getCaseId());
        assertEquals(oldSub.getReviewerId(), newRevision.getReviewerId());
        assertEquals(oldSub.getBasedOnScoreSheetVersionId(), newRevision.getBasedOnScoreSheetVersionId());
        assertEquals("revised comment", newRevision.getComment());
        // New score should be Q001(10) + Q002(20*0.5) = 20.0
        assertEquals(0, new BigDecimal("20.0").compareTo(newRevision.getTotalScore()));

        // Verify case final score is recalculated
        // initialScore=90, rereviewScore=20, weights=0.5/0.5
        // finalScore = 90*0.5 + 20*0.5 = 55
        ReviewCase savedCase = caseRepo.findById(CASE_ID).orElseThrow();
        assertEquals(CaseStatus.REREVIEW_IN_PROGRESS, savedCase.getStatus());
        assertNull(savedCase.getClosedAt());
        assertEquals(0, new BigDecimal("55.0").compareTo(savedCase.getFinalScore()));
        assertEquals(result.getNewSubmissionId(), savedCase.getLatestRereviewSubmissionId());

        // Verify event published
        assertTrue(eventPublisher.events.stream()
                .anyMatch(e -> e instanceof RereviewRevisedEvent));
    }

    @Test
    void execute_wrongActor_shouldThrow() {
        seedProject();
        seedScoreSheet();
        seedInProgressCaseWithSubmittedRereview();

        List<AnswerItem> newAnswers = Arrays.asList(
                AnswerItem.ofRaw("Q001", "true"),
                AnswerItem.ofRaw("Q002", "20"));


        ActorContext wrongActor = new ActorContext("someone-else", "Other", EnumSet.of(DomainRole.REREVIEWER), Optional.of(HOSPITAL_ID));

        // D-068 第 2 點
        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () -> useCase.execute(wrongActor, CASE_ID, OLD_SUBMISSION_ID, RevisionTestInputs.of(newAnswers), "revised comment"));
        assertEquals("NOT_ASSIGNED_REREVIEWER", ex.getErrorCode());
    }

    @Test
    void execute_nonRereviewer_shouldThrow() {
        seedProject();
        seedScoreSheet();
        seedInProgressCaseWithSubmittedRereview();

        List<AnswerItem> newAnswers = Arrays.asList(
                AnswerItem.ofRaw("Q001", "true"),
                AnswerItem.ofRaw("Q002", "20"));


        ActorContext reviewer = new ActorContext(REREVIEWER_ID, "Reviewer", EnumSet.of(DomainRole.REVIEWER), Optional.of(HOSPITAL_ID));

        assertThrows(RoleDeniedException.class, () -> useCase.execute(reviewer, CASE_ID, OLD_SUBMISSION_ID, RevisionTestInputs.of(newAnswers), "revised comment"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"otherCase", "initialStage", "otherOwner", "stale", "missingPointer", "draft", "superseded", "closed", "cancelled", "wrongHospital"})
    void execute_invalidSubmissionOrCase_rejectsWithoutMutation(String scenario) {
        seedProject();
        seedScoreSheet();
        seedInProgressCaseWithSubmittedRereview();
        ReviewCase reviewCase = caseRepo.findById(CASE_ID).orElseThrow();
        ReviewSubmission original = submissionRepo.findById(OLD_SUBMISSION_ID).orElseThrow();
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
            target = ReviewSubmission.createRereviewDraft(OLD_SUBMISSION_ID, CASE_ID, REREVIEWER_ID, Instant.now());
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
            com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseTestSupport.pointRereviewAt(reviewCase, newer.getSubmissionId());
        }
        if (scenario.equals("missingPointer")) com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseTestSupport.pointRereviewAt(reviewCase, null);
        if (scenario.equals("closed")) { reviewCase.completeRereview(BigDecimal.TEN, new BigDecimal("0.5"), new BigDecimal("0.5")); reviewCase.closeAfterRereview(Instant.now()); }
        if (scenario.equals("cancelled")) reviewCase.cancel("cancelled", Instant.now());
        caseRepo.save(reviewCase);
        List<Object> beforeCase = caseState(reviewCase);
        Map<String, List<Object>> beforeSubmissions = submissionState();
        ActorContext actor = scenario.equals("wrongHospital")
                ? new ActorContext(REREVIEWER_ID, "ReReviewer", EnumSet.of(DomainRole.REREVIEWER), Optional.of("OTHER"))
                : rereviewerActor();
        RuntimeException failure = assertThrows(RuntimeException.class, () -> useCase.execute(actor, CASE_ID, OLD_SUBMISSION_ID, RevisionTestInputs.of(Arrays.asList(AnswerItem.ofRaw("Q001", "true"), AnswerItem.ofRaw("Q002", "20"))), null));
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

    // --- D-083 ---

    private RevisionResult reviseWith(List<AnswerItem> answers, String comment) {
        return useCase.execute(rereviewerActor(), CASE_ID, OLD_SUBMISSION_ID,
                RevisionTestInputs.of(answers), comment);
    }

    @Test
    void execute_requiredItemMissing_rejectsWithoutMutation_D083() {
        seedProject();
        seedScoreSheet();
        seedInProgressCaseWithSubmittedRereview();
        List<Object> beforeCase = caseState(caseRepo.findById(CASE_ID).orElseThrow());
        Map<String, List<Object>> beforeSubmissions = submissionState();

        com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException e = assertThrows(
                com.tzuchi.his.quality.inspection.domain.common.DomainRuleViolationException.class,
                () -> reviseWith(Collections.singletonList(AnswerItem.ofRaw("Q002", "20")), "revised"));
        assertEquals("REQUIRED_ITEMS_NOT_ANSWERED", e.getErrorCode());
        assertEquals(beforeCase, caseState(caseRepo.findById(CASE_ID).orElseThrow()));
        assertEquals(beforeSubmissions, submissionState());
        assertTrue(eventPublisher.events.isEmpty());
    }

    @Test
    void execute_noChanges_rejectsWithoutMutation_D083() {
        seedProject();
        seedScoreSheet();
        seedInProgressCaseWithSubmittedRereview();
        Map<String, List<Object>> beforeSubmissions = submissionState();

        DomainStateConflictException e = assertThrows(DomainStateConflictException.class,
                () -> reviseWith(Arrays.asList(AnswerItem.ofRaw("Q002", "10"), AnswerItem.ofRaw("Q001", "true")),
                        "original comment"));
        assertEquals("NO_CHANGES", e.getErrorCode());
        assertEquals(beforeSubmissions, submissionState());
        assertTrue(eventPublisher.events.isEmpty());
    }

    @Test
    void execute_publishesSubmissionRevisedThenRereviewRevised_D083() {
        seedProject();
        seedScoreSheet();
        seedInProgressCaseWithSubmittedRereview();

        RevisionResult result = reviseWith(
                Arrays.asList(AnswerItem.ofRaw("Q001", "true"), AnswerItem.ofRaw("Q002", "20")), "revised");

        // 第 6 點：新提交 ID 是純 UUID
        assertEquals(result.getNewSubmissionId(), UUID.fromString(result.getNewSubmissionId()).toString());

        assertEquals(2, eventPublisher.events.size());
        com.tzuchi.his.quality.inspection.domain.reviewcase.submission.event.ReviewSubmissionRevisedEvent revised =
                (com.tzuchi.his.quality.inspection.domain.reviewcase.submission.event.ReviewSubmissionRevisedEvent)
                        eventPublisher.events.get(0);
        assertEquals(CASE_ID, revised.getCaseId());
        assertEquals(OLD_SUBMISSION_ID, revised.getOldSubmissionId());
        assertEquals(result.getNewSubmissionId(), revised.getNewSubmissionId());
        assertEquals("REREVIEW", revised.getStage());
        assertEquals(2, revised.getRevisionNo());
        assertFalse(revised.getDetail().contains("Score"), "ReviewSubmissionRevised 不帶分數");

        RereviewRevisedEvent rr = (RereviewRevisedEvent) eventPublisher.events.get(1);
        assertEquals(CASE_ID, rr.getCaseId());
        assertEquals(OLD_SUBMISSION_ID, rr.getOldSubmissionId());
        assertEquals(result.getNewSubmissionId(), rr.getNewSubmissionId());
        assertEquals(0, new BigDecimal("20").compareTo(rr.getRereviewScore()));
        assertEquals(0, new BigDecimal("55").compareTo(rr.getFinalScore()));
        // 稽核 detail 格式不變（D-083 第 5 點）
        assertEquals("{\"newSubmissionId\":\"" + result.getNewSubmissionId() + "\",\"oldSubmissionId\":\""
                + OLD_SUBMISSION_ID + "\",\"rereviewScore\":" + rr.getRereviewScore()
                + ",\"finalScore\":" + rr.getFinalScore() + "}", rr.getDetail());
    }
}
