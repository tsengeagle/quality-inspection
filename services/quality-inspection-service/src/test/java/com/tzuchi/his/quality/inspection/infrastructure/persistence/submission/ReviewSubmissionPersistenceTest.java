package com.tzuchi.his.quality.inspection.infrastructure.persistence.submission;

import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewStage;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.SubmissionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ReviewSubmissionPersistenceTest {

    @Autowired
    private ReviewSubmissionRepositoryImpl repository;

    @Autowired
    private ReviewSubmissionJpaRepository jpa;

    @Test
    void saveAndFindById_draftSubmission_shouldRoundTrip() {
        ReviewSubmission sub = ReviewSubmission.createInitialDraft(
                "sub-1", "c1", "reviewer-1", "ss-1", Instant.now());

        repository.save(sub);

        Optional<ReviewSubmission> found = repository.findById("sub-1");
        assertTrue(found.isPresent());
        ReviewSubmission loaded = found.get();
        assertEquals("sub-1", loaded.getSubmissionId());
        assertEquals("c1", loaded.getCaseId());
        assertEquals(ReviewStage.INITIAL, loaded.getStage());
        assertEquals("reviewer-1", loaded.getReviewerId());
        assertEquals(SubmissionStatus.DRAFT, loaded.getStatus());
        assertEquals(1, loaded.getRevisionNo());
        assertEquals(0, loaded.getTotalScore().compareTo(BigDecimal.ZERO));
        assertNotNull(loaded.getCreatedAt());
        assertTrue(loaded.getAnswerSet().isEmpty());
    }

    @Test
    void saveAndFindById_withAnswerSet_shouldPreserveJsonRoundTrip() {
        ReviewSubmission sub = ReviewSubmission.createInitialDraft(
                "sub-2", "c1", "reviewer-1", "ss-1", Instant.now());
        List<AnswerItem> answers = Arrays.asList(
                AnswerItem.ofScored("Q001", "true", new BigDecimal("10.0")),
                AnswerItem.ofScored("Q002", "15", new BigDecimal("7.5"))
        );
        sub.updateDraft(answers, new BigDecimal("17.5"), "draft comment", Instant.now());

        repository.save(sub);

        ReviewSubmission loaded = repository.findById("sub-2").orElseThrow();
        assertEquals(2, loaded.getAnswerSet().size());
        assertEquals("Q001", loaded.getAnswerSet().get(0).getItemId());
        assertEquals("true", loaded.getAnswerSet().get(0).getAnswerValue());
        assertEquals(0, new BigDecimal("10.0").compareTo(loaded.getAnswerSet().get(0).getDerivedScore()));
        assertEquals("Q002", loaded.getAnswerSet().get(1).getItemId());
        assertEquals(0, new BigDecimal("17.5").compareTo(loaded.getTotalScore()));
        assertEquals("draft comment", loaded.getComment());
    }

    @Test
    void saveAndFindById_submittedSubmission_shouldPreserveStatus() {
        ReviewSubmission sub = ReviewSubmission.createInitialDraft(
                "sub-3", "c1", "reviewer-1", "ss-1", Instant.now());
        sub.updateDraft(Collections.emptyList(), BigDecimal.ZERO, null, Instant.now());
        sub.submit(Instant.now());

        repository.save(sub);

        ReviewSubmission loaded = repository.findById("sub-3").orElseThrow();
        assertEquals(SubmissionStatus.SUBMITTED, loaded.getStatus());
        assertNotNull(loaded.getSubmittedAt());
    }

    @Test
    void findByCaseIdAndStage_shouldReturnMatchingSubmissions() {
        ReviewSubmission sub1 = ReviewSubmission.createInitialDraft(
                "sub-a", "c1", "reviewer-1", "ss-1", Instant.now());
        ReviewSubmission sub2 = ReviewSubmission.createInitialDraft(
                "sub-b", "c1", "reviewer-1", "ss-1", Instant.now());
        repository.save(sub1);
        repository.save(sub2);

        List<ReviewSubmission> results = repository.findByCaseIdAndStage("c1", ReviewStage.INITIAL);
        assertEquals(2, results.size());
    }

    @Test
    void findLatestByCaseIdAndStage_shouldReturnHighestRevisionNo() {
        ReviewSubmission sub1 = ReviewSubmission.createInitialDraft(
                "sub-first", "c1", "reviewer-1", "ss-1", Instant.now());
        repository.save(sub1);

        // Seed a second submission with higher revisionNo directly via JPA
        ReviewSubmissionEntity e2 = new ReviewSubmissionEntity();
        e2.setSubmissionId("sub-second");
        e2.setCaseId("c1");
        e2.setStage(ReviewStage.INITIAL);
        e2.setReviewerId("reviewer-1");
        e2.setStatus(SubmissionStatus.DRAFT);
        e2.setRevisionNo(2);
        e2.setTotalScore(BigDecimal.ZERO);
        e2.setBasedOnScoreSheetVersionId("ss-1");
        e2.setCreatedAt(Instant.now());
        e2.setUpdatedAt(Instant.now());
        jpa.save(e2);

        Optional<ReviewSubmission> latest = repository.findLatestByCaseIdAndStage("c1", ReviewStage.INITIAL);
        assertTrue(latest.isPresent());
        assertEquals("sub-second", latest.get().getSubmissionId());
    }

    @Test
    void findById_notFound_shouldReturnEmpty() {
        assertTrue(repository.findById("nonexistent").isEmpty());
    }
}
