package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.RoleDeniedException;
import com.tzuchi.his.quality.inspection.application.reviewcase.dto.AnswerInput;
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.SubmissionStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.event.ReviewDraftSavedEvent;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetDefinition;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetSection;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreItem;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreType;
import com.tzuchi.his.quality.inspection.testfixture.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewStage;

class SaveInitialReviewDraftUseCaseTest {

    private InMemoryReviewSubmissionRepository submissionRepo;
    private InMemoryScoreSheetRepository scoreSheetRepo;
    private InMemoryReviewCaseRepository caseRepo;
    private CapturingDomainEventPublisher eventPublisher;
    private SaveInitialReviewDraftUseCase useCase;

    private static final String SUBMISSION_ID = "sub-1";
    private static final String CASE_ID = "c1";
    private static final String REVIEWER_ID = "reviewer-1";
    private static final String HOSPITAL_ID = "H001";
    private static final String SCORE_SHEET_ID = "ss-1";

    @BeforeEach
    void setUp() {
        submissionRepo = new InMemoryReviewSubmissionRepository();
        scoreSheetRepo = new InMemoryScoreSheetRepository();
        caseRepo = new InMemoryReviewCaseRepository();
        eventPublisher = new CapturingDomainEventPublisher();
        ScoreCalculationService scoreCalcService = new ScoreCalculationService();
        useCase = new SaveInitialReviewDraftUseCase(
                submissionRepo, scoreSheetRepo, eventPublisher, scoreCalcService, caseRepo);
    }

    private ActorContext reviewer() {
        return new ActorContext(REVIEWER_ID, "Test Reviewer",
                EnumSet.of(DomainRole.REVIEWER), Optional.of(HOSPITAL_ID));
    }

    private ScoreSheetDefinition seedPublishedScoreSheet() {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft(SCORE_SHEET_ID, "p1");
        ScoreSheetSection section = new ScoreSheetSection("sec-1", "Section 1");
        section.addItem(new ScoreItem("Q001", "Q001", "Bool Question", ScoreType.BOOLEAN,
                new BigDecimal("10"), new BigDecimal("1.0")));
        section.addItem(new ScoreItem("Q002", "Q002", "Range Question", ScoreType.RANGE,
                new BigDecimal("20"), new BigDecimal("0.5")));
        ss.addSection(section);
        ss.publishVersion();
        scoreSheetRepo.save(ss);
        return ss;
    }

    private ReviewSubmission seedDraftSubmission() {
        ReviewSubmission sub = ReviewSubmission.createInitialDraft(
                SUBMISSION_ID, CASE_ID, REVIEWER_ID, SCORE_SHEET_ID);
        submissionRepo.save(sub);
        return sub;
    }

    private ReviewCase seedReviewCase() {
        ReviewCase rc = ReviewCase.createFromSampling(
                CASE_ID, "b1", "p1", HOSPITAL_ID, "SK-001", MedicalRecordType.OUTPATIENT);
        rc.assignReviewer(REVIEWER_ID, java.time.LocalDateTime.now());
        rc.startInitialReview(SUBMISSION_ID, SCORE_SHEET_ID);
        caseRepo.save(rc);
        return rc;
    }

    @Test
    void execute_happyPath_shouldUpdateDraftAndReturnScore() {
        seedPublishedScoreSheet();
        seedReviewCase();
        seedDraftSubmission();

        List<AnswerInput> answers = Arrays.asList(
                new AnswerInput("Q001", "true"),
                new AnswerInput("Q002", "15")
        );

        SaveDraftResult result = useCase.execute(reviewer(), CASE_ID, answers, "draft comment");

        // Score: Q001 = 10*1.0 = 10, Q002 = min(15,20)*0.5 = 7.5 -> total = 17.5
        assertEquals(new BigDecimal("17.5"), result.getTotalScore());
        assertNotNull(result.getSubmissionId());
        assertEquals("DRAFT", result.getStatus());
        assertEquals("INITIAL", result.getStage());

        ReviewSubmission saved = submissionRepo.store.get(SUBMISSION_ID);
        assertEquals(SubmissionStatus.DRAFT, saved.getStatus());
        assertEquals(2, saved.getAnswerSet().size());
        assertEquals("draft comment", saved.getComment());

        // Each answer should have derived score
        AnswerItem q001 = saved.getAnswerSet().stream()
                .filter(a -> a.getItemId().equals("Q001")).findFirst().orElseThrow();
        assertEquals(new BigDecimal("10.0"), q001.getDerivedScore());
    }

    @Test
    void execute_shouldPublishEvent() {
        seedPublishedScoreSheet();
        seedReviewCase();
        seedDraftSubmission();

        List<AnswerInput> answers = Collections.singletonList(new AnswerInput("Q001", "true"));

        useCase.execute(reviewer(), CASE_ID, answers, null);

        assertEquals(1, eventPublisher.events.size());
        assertInstanceOf(ReviewDraftSavedEvent.class, eventPublisher.events.get(0));
    }

    @Test
    void execute_wrongRole_shouldThrow() {
        ActorContext manager = new ActorContext("u1", "Manager",
                EnumSet.of(DomainRole.HOSPITAL_MANAGER), Optional.of(HOSPITAL_ID));

        assertThrows(RoleDeniedException.class,
                () -> useCase.execute(manager, CASE_ID, Collections.emptyList(), null));
    }

    @Test
    void execute_notOwningReviewer_shouldThrow() {
        seedPublishedScoreSheet();
        seedReviewCase();
        seedDraftSubmission();

        ActorContext other = new ActorContext("other-reviewer", "Other",
                EnumSet.of(DomainRole.REVIEWER), Optional.of(HOSPITAL_ID));

        assertThrows(AccessDeniedException.class,
                () -> useCase.execute(other, CASE_ID,
                        Collections.singletonList(new AnswerInput("Q001", "true")), null));
    }

    @Test
    void execute_submissionNotFound_shouldThrow() {
        // No submission seeded for CASE_ID
        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(reviewer(), CASE_ID, Collections.emptyList(), null));
    }

    @Test
    void execute_submissionNotDraft_shouldThrow() {
        seedPublishedScoreSheet();
        seedReviewCase();
        ReviewSubmission sub = ReviewSubmission.createInitialDraft(
                SUBMISSION_ID, CASE_ID, REVIEWER_ID, SCORE_SHEET_ID);
        sub.updateDraft(Collections.emptyList(), BigDecimal.ZERO, null);
        sub.submit();
        submissionRepo.save(sub);

        assertThrows(DomainStateConflictException.class,
                () -> useCase.execute(reviewer(), CASE_ID,
                        Collections.singletonList(new AnswerInput("Q001", "true")), null));
    }

    @Test
    void execute_emptyAnswers_shouldReturnZeroScore() {
        seedPublishedScoreSheet();
        seedReviewCase();
        seedDraftSubmission();

        // 評語有變更，才不會被判為無變更（D-038）；本測試驗證的是空白答案得 0 分
        SaveDraftResult result = useCase.execute(reviewer(), CASE_ID, Collections.emptyList(), "cleared");

        assertEquals(BigDecimal.ZERO, result.getTotalScore());
        assertEquals("DRAFT", result.getStatus());
        assertEquals("INITIAL", result.getStage());
    }

    @Test
    void execute_unchangedBlankDraft_isRejectedWithNoChanges() {
        // D-038、D-044：空白草稿再存一次空白答案（評語 null 與空字串視為相同），不寫入、不發事件
        seedPublishedScoreSheet();
        seedReviewCase();
        seedDraftSubmission();

        DomainStateConflictException e = assertThrows(DomainStateConflictException.class,
                () -> useCase.execute(reviewer(), CASE_ID, Collections.emptyList(), ""));

        assertEquals("NO_CHANGES", e.getErrorCode());
        assertTrue(eventPublisher.events.isEmpty());
    }

    @Test
    void execute_answersWithNullElement_shouldIgnoreNull() {
        seedPublishedScoreSheet();
        seedReviewCase();
        seedDraftSubmission();

        List<AnswerInput> answers = new ArrayList<>();
        answers.add(new AnswerInput("Q001", "true"));
        answers.add(null);

        SaveDraftResult result = useCase.execute(reviewer(), CASE_ID, answers, null);

        assertEquals(new BigDecimal("10.0"), result.getTotalScore());
    }

    @Test
    void execute_wrongHospital_shouldThrowAccessDenied() {
        seedPublishedScoreSheet();
        seedReviewCase();
        seedDraftSubmission();

        ActorContext wrongHospital = new ActorContext(REVIEWER_ID, "Test Reviewer",
                EnumSet.of(DomainRole.REVIEWER), Optional.of("OTHER_HOSPITAL"));
        assertThrows(AccessDeniedException.class,
                () -> useCase.execute(wrongHospital, CASE_ID, Collections.emptyList(), null));
    }

    @Test
    void restart_savesReferencedDraftDespiteEqualRevisionNumbers() {
        seedPublishedScoreSheet();
        ReviewCase rc = seedReviewCase();
        ReviewSubmission old = seedDraftSubmission();
        old.updateDraft(Collections.emptyList(), new BigDecimal("7"), "historical");
        rc.returnToInitialReview("return");
        rc.reopenInitialReview();
        rc.startInitialReview("sub-2", SCORE_SHEET_ID);
        caseRepo.save(rc);
        ReviewSubmission current = ReviewSubmission.createInitialDraft("sub-2", CASE_ID, REVIEWER_ID, SCORE_SHEET_ID);
        submissionRepo.save(current);
        InMemoryReviewSubmissionRepository selected = spy(submissionRepo);
        doReturn(Optional.of(old)).when(selected).findLatestByCaseIdAndStage(CASE_ID, ReviewStage.INITIAL);
        SaveInitialReviewDraftUseCase saving = new SaveInitialReviewDraftUseCase(
                selected, scoreSheetRepo, eventPublisher, new ScoreCalculationService(), caseRepo);
        assertEquals(1, old.getRevisionNo());
        assertEquals(1, current.getRevisionNo());
        SaveDraftResult result = saving.execute(reviewer(), CASE_ID,
                Collections.singletonList(new AnswerInput("Q001", "true")), "current");
        assertEquals("sub-2", result.getSubmissionId());
        assertEquals("current", current.getComment());
        assertEquals(SubmissionStatus.DRAFT, old.getStatus());
        assertEquals(new BigDecimal("7"), old.getTotalScore());
        assertEquals("historical", old.getComment());
        assertTrue(old.getAnswerSet().isEmpty());
        verify(selected, never()).findLatestByCaseIdAndStage(CASE_ID, ReviewStage.INITIAL);
    }
}
