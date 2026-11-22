package com.tzuchi.his.quality.inspection.domain;

import com.tzuchi.his.quality.inspection.domain.reviewcase.event.InitialReviewCompletedEvent;
import com.tzuchi.his.quality.inspection.domain.reviewcase.event.ReviewCaseAssignedEvent;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.event.InitialReviewStartedEvent;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.event.ReviewDraftSavedEvent;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.event.ReviewSubmissionSubmittedEvent;
import com.tzuchi.his.quality.inspection.domain.sampling.event.ReviewCasesSelectedEvent;
import com.tzuchi.his.quality.inspection.domain.sampling.event.SamplingBatchCompletedEvent;
import com.tzuchi.his.quality.inspection.domain.sampling.event.SamplingBatchStartedEvent;
import com.tzuchi.his.quality.inspection.domain.sampling.event.SamplingCandidatesPreparedEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * D-066 第 2 點、D-067 第 5、6 點：直向切片的事件類別有規格用到的屬性，稽核紀錄的 detail 逐字不變。
 * 期望的 detail 是加屬性之前的輸出；InitialReviewCompleted 是 D-067 第 3 點新增的事件，期望值是它的格式。
 * SamplingBatchCompleted 依 D-068 第 3 點多了兩個計數，是逐字不變的例外。
 */
class SliceEventPropertiesTest {

    private static final Object SOURCE = new Object();

    @Test
    void samplingBatchStarted() {
        SamplingBatchStartedEvent e = new SamplingBatchStartedEvent(SOURCE, "u1", "U1", "H001", "b1", "p1", "MANUAL");
        assertAll(
                () -> assertEquals("b1", e.getBatchId()),
                () -> assertEquals("p1", e.getProjectId()),
                () -> assertEquals("{\"projectId\":\"p1\",\"executionMode\":\"MANUAL\"}", e.getDetail()));
    }

    @Test
    void samplingCandidatesPrepared() {
        SamplingCandidatesPreparedEvent e = new SamplingCandidatesPreparedEvent(SOURCE, "u1", "U1", "H001", "b1", 3, 2, 1);
        assertAll(
                () -> assertEquals("b1", e.getBatchId()),
                () -> assertEquals(3, e.getPopulationCount()),
                () -> assertEquals(2, e.getEligibleCount()),
                () -> assertEquals(1, e.getExcludedCount()),
                () -> assertEquals("{\"populationCount\":3,\"eligibleCount\":2,\"excludedCount\":1}", e.getDetail()));
    }

    @Test
    void reviewCasesSelected() {
        ReviewCasesSelectedEvent e = new ReviewCasesSelectedEvent(SOURCE, "u1", "U1", "H001", "b1", 2);
        assertAll(
                () -> assertEquals("b1", e.getBatchId()),
                () -> assertEquals(2, e.getSelectedCount()),
                () -> assertEquals("{\"selectedCount\":2}", e.getDetail()));
    }

    @Test
    void samplingBatchCompleted() {
        SamplingBatchCompletedEvent e = new SamplingBatchCompletedEvent(SOURCE, "u1", "U1", "H001", "b1", "p1", 2, 3, 2, 1);
        assertAll(
                () -> assertEquals("b1", e.getBatchId()),
                () -> assertEquals("p1", e.getProjectId()),
                () -> assertEquals(2, e.getSelectedCount()),
                () -> assertEquals(3, e.getPopulationCount()),
                () -> assertEquals(2, e.getEligibleCount()),
                () -> assertEquals(1, e.getExcludedCount()),
                () -> assertEquals("{\"projectId\":\"p1\",\"selectedCount\":2,\"populationCount\":3,"
                        + "\"eligibleCount\":2,\"excludedCount\":1}", e.getDetail()));
    }

    @Test
    void reviewCaseAssigned() {
        ReviewCaseAssignedEvent e = new ReviewCaseAssignedEvent(SOURCE, "u1", "U1", "H001", "c1", "r1",
                LocalDateTime.of(2026, 12, 1, 17, 0));
        assertAll(
                () -> assertEquals("c1", e.getCaseId()),
                () -> assertEquals("r1", e.getAssignedReviewerId()),
                () -> assertEquals("{\"reviewerId\":\"r1\",\"dueAt\":\"2026-12-01T17:00\"}", e.getDetail()));
    }

    @Test
    void initialReviewStarted() {
        InitialReviewStartedEvent e = new InitialReviewStartedEvent(SOURCE, "r1", "R1", "H001", "c1", "r1", "s1");
        assertAll(
                () -> assertEquals("c1", e.getCaseId()),
                () -> assertEquals("s1", e.getSubmissionId()),
                () -> assertEquals("r1", e.getReviewerId()),
                () -> assertEquals("{\"reviewerId\":\"r1\",\"submissionId\":\"s1\"}", e.getDetail()));
    }

    @Test
    void reviewDraftSaved() {
        ReviewDraftSavedEvent e = new ReviewDraftSavedEvent(SOURCE, "r1", "R1", "H001", "c1", "s1", new BigDecimal("85.50"));
        assertAll(
                () -> assertEquals("s1", e.getSubmissionId()),
                () -> assertEquals("c1", e.getCaseId()),
                () -> assertEquals("{\"caseId\":\"c1\",\"totalScore\":85.50}", e.getDetail()));
    }

    @Test
    void reviewSubmissionSubmitted() {
        ReviewSubmissionSubmittedEvent e = new ReviewSubmissionSubmittedEvent(SOURCE, "r1", "R1", "H001", "c1", "s1",
                new BigDecimal("85.50"), "r1");
        assertAll(
                () -> assertEquals("c1", e.getCaseId()),
                () -> assertEquals("s1", e.getSubmissionId()),
                () -> assertEquals("{\"caseId\":\"c1\",\"totalScore\":85.50,\"reviewerId\":\"r1\"}", e.getDetail()));
    }

    @Test
    void initialReviewCompleted() {
        InitialReviewCompletedEvent e = new InitialReviewCompletedEvent(SOURCE, "r1", "R1", "H001", "c1", "p1", "s1", "r1",
                new BigDecimal("85.50"), null);
        assertAll(
                () -> assertEquals("c1", e.getCaseId()),
                () -> assertEquals("p1", e.getProjectId()),
                () -> assertEquals("s1", e.getSubmissionId()),
                () -> assertEquals("r1", e.getReviewerId()),
                () -> assertEquals(new BigDecimal("85.50"), e.getTotalScore()),
                () -> assertEquals(null, e.getQualityThreshold()),
                () -> assertEquals("{\"projectId\":\"p1\",\"submissionId\":\"s1\",\"reviewerId\":\"r1\","
                        + "\"totalScore\":85.50,\"qualityThreshold\":null}", e.getDetail()));
    }
}
