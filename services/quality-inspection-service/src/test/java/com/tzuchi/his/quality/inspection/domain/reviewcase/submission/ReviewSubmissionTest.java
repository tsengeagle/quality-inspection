package com.tzuchi.his.quality.inspection.domain.reviewcase.submission;

import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReviewSubmissionTest {

    // --- createInitialDraft ---

    @Test
    void createInitialDraft_shouldCreateWithCorrectDefaults() {
        ReviewSubmission sub = ReviewSubmission.createInitialDraft(
                "sub-1", "c1", "reviewer-1", "ss-v1");

        assertEquals("sub-1", sub.getSubmissionId());
        assertEquals("c1", sub.getCaseId());
        assertEquals(ReviewStage.INITIAL, sub.getStage());
        assertEquals("reviewer-1", sub.getReviewerId());
        assertEquals(SubmissionStatus.DRAFT, sub.getStatus());
        assertEquals(1, sub.getRevisionNo());
        assertEquals(BigDecimal.ZERO, sub.getTotalScore());
        assertTrue(sub.getAnswerSet().isEmpty());
        assertNull(sub.getComment());
        assertNull(sub.getSubmittedAt());
        assertEquals("ss-v1", sub.getBasedOnScoreSheetVersionId());
        assertNotNull(sub.getCreatedAt());
    }

    @Test
    void createInitialDraft_nullSubmissionId_shouldThrow() {
        assertThrows(NullPointerException.class,
                () -> ReviewSubmission.createInitialDraft(null, "c1", "r1", "ss-v1"));
    }

    @Test
    void createInitialDraft_nullCaseId_shouldThrow() {
        assertThrows(NullPointerException.class,
                () -> ReviewSubmission.createInitialDraft("sub-1", null, "r1", "ss-v1"));
    }

    @Test
    void createInitialDraft_nullReviewerId_shouldThrow() {
        assertThrows(NullPointerException.class,
                () -> ReviewSubmission.createInitialDraft("sub-1", "c1", null, "ss-v1"));
    }

    @Test
    void createInitialDraft_nullScoreSheetVersionId_shouldThrow() {
        assertThrows(NullPointerException.class,
                () -> ReviewSubmission.createInitialDraft("sub-1", "c1", "r1", null));
    }

    // --- updateDraft ---

    @Test
    void updateDraft_shouldUpdateAnswersAndScore() {
        ReviewSubmission sub = ReviewSubmission.createInitialDraft(
                "sub-1", "c1", "reviewer-1", "ss-v1");

        List<AnswerItem> scored = Arrays.asList(
                AnswerItem.ofScored("Q001", "true", new BigDecimal("10.00")),
                AnswerItem.ofScored("Q002", "85", new BigDecimal("85.00"))
        );
        sub.updateDraft(scored, new BigDecimal("95.00"), "looks good");

        assertEquals(2, sub.getAnswerSet().size());
        assertEquals(new BigDecimal("95.00"), sub.getTotalScore());
        assertEquals("looks good", sub.getComment());
        assertNotNull(sub.getUpdatedAt());
    }

    @Test
    void updateDraft_calledMultipleTimes_shouldOverwrite() {
        ReviewSubmission sub = ReviewSubmission.createInitialDraft(
                "sub-1", "c1", "reviewer-1", "ss-v1");

        List<AnswerItem> first = Collections.singletonList(
                AnswerItem.ofScored("Q001", "true", new BigDecimal("10.00")));
        sub.updateDraft(first, new BigDecimal("10.00"), "first draft");

        List<AnswerItem> second = Arrays.asList(
                AnswerItem.ofScored("Q001", "true", new BigDecimal("10.00")),
                AnswerItem.ofScored("Q002", "85", new BigDecimal("85.00"))
        );
        sub.updateDraft(second, new BigDecimal("95.00"), "second draft");

        assertEquals(2, sub.getAnswerSet().size());
        assertEquals(new BigDecimal("95.00"), sub.getTotalScore());
        assertEquals("second draft", sub.getComment());
    }

    @Test
    void updateDraft_afterSubmit_shouldThrow() {
        ReviewSubmission sub = ReviewSubmission.createInitialDraft(
                "sub-1", "c1", "reviewer-1", "ss-v1");
        sub.submit();

        List<AnswerItem> answers = Collections.singletonList(
                AnswerItem.ofScored("Q001", "true", new BigDecimal("10.00")));

        assertThrows(DomainStateConflictException.class,
                () -> sub.updateDraft(answers, new BigDecimal("10.00"), "too late"));
    }

    @Test
    void updateDraft_nullScoredAnswers_shouldThrow() {
        ReviewSubmission sub = ReviewSubmission.createInitialDraft("sub-1", "c1", "r1", "ss-v1");
        assertThrows(NullPointerException.class,
                () -> sub.updateDraft(null, java.math.BigDecimal.ZERO, "comment"));
    }

    @Test
    void updateDraft_nullTotalScore_shouldThrow() {
        ReviewSubmission sub = ReviewSubmission.createInitialDraft("sub-1", "c1", "r1", "ss-v1");
        assertThrows(NullPointerException.class,
                () -> sub.updateDraft(java.util.Collections.emptyList(), null, "comment"));
    }

    // --- submit ---

    @Test
    void submit_fromDraft_shouldTransitionToSubmitted() {
        ReviewSubmission sub = ReviewSubmission.createInitialDraft(
                "sub-1", "c1", "reviewer-1", "ss-v1");

        sub.submit();

        assertEquals(SubmissionStatus.SUBMITTED, sub.getStatus());
        assertNotNull(sub.getSubmittedAt());
    }

    @Test
    void submit_alreadySubmitted_shouldThrow() {
        ReviewSubmission sub = ReviewSubmission.createInitialDraft(
                "sub-1", "c1", "reviewer-1", "ss-v1");
        sub.submit();

        assertThrows(DomainStateConflictException.class, sub::submit);
    }

    // --- reconstitute ---

    @Test
    void reconstitute_shouldRestoreAllFields() {
        List<AnswerItem> answers = Collections.singletonList(
                AnswerItem.reconstitute("Q001", "true", new BigDecimal("10.00")));
        LocalDateTime now = LocalDateTime.now();

        ReviewSubmission sub = ReviewSubmission.reconstitute(
                "sub-1", "c1", ReviewStage.INITIAL, "reviewer-1",
                SubmissionStatus.SUBMITTED, 1,
                answers, new BigDecimal("10.00"), "comment", now,
                "ss-v1", null, null, now, now);

        assertEquals("sub-1", sub.getSubmissionId());
        assertEquals(SubmissionStatus.SUBMITTED, sub.getStatus());
        assertEquals(1, sub.getAnswerSet().size());
        assertEquals(new BigDecimal("10.00"), sub.getTotalScore());
        assertEquals("comment", sub.getComment());
        assertEquals(now, sub.getSubmittedAt());
    }
}
