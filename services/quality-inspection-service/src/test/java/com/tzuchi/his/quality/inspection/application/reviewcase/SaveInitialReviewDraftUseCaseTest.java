package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.UnauthorizedException;
import com.tzuchi.his.quality.inspection.application.reviewcase.dto.AnswerInput;
import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
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

class SaveInitialReviewDraftUseCaseTest {

    private InMemoryReviewSubmissionRepository submissionRepo;
    private InMemoryScoreSheetRepository scoreSheetRepo;
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
        eventPublisher = new CapturingDomainEventPublisher();
        ScoreCalculationService scoreCalcService = new ScoreCalculationService();
        useCase = new SaveInitialReviewDraftUseCase(
                submissionRepo, scoreSheetRepo, eventPublisher, scoreCalcService);
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

    @Test
    void execute_happyPath_shouldUpdateDraftAndReturnScore() {
        seedPublishedScoreSheet();
        seedDraftSubmission();

        List<AnswerInput> answers = Arrays.asList(
                new AnswerInput("Q001", "true"),
                new AnswerInput("Q002", "15")
        );

        BigDecimal totalScore = useCase.execute(reviewer(), CASE_ID, answers, "draft comment");

        // Score: Q001 = 10*1.0 = 10, Q002 = min(15,20)*0.5 = 7.5 -> total = 17.5
        assertEquals(new BigDecimal("17.5"), totalScore);

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

        assertThrows(UnauthorizedException.class,
                () -> useCase.execute(manager, CASE_ID, Collections.emptyList(), null));
    }

    @Test
    void execute_notOwningReviewer_shouldThrow() {
        seedPublishedScoreSheet();
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
        seedDraftSubmission();

        BigDecimal totalScore = useCase.execute(reviewer(), CASE_ID, Collections.emptyList(), null);

        assertEquals(BigDecimal.ZERO, totalScore);
    }

    @Test
    void execute_answersWithNullElement_shouldIgnoreNull() {
        seedPublishedScoreSheet();
        seedDraftSubmission();

        List<AnswerInput> answers = new ArrayList<>();
        answers.add(new AnswerInput("Q001", "true"));
        answers.add(null);

        BigDecimal totalScore = useCase.execute(reviewer(), CASE_ID, answers, null);

        assertEquals(new BigDecimal("10.0"), totalScore);
    }
}
