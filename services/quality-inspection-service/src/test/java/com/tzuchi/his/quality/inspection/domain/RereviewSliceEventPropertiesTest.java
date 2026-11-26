package com.tzuchi.his.quality.inspection.domain;

import com.tzuchi.his.quality.inspection.domain.reviewcase.event.InitialReviewFixedEvent;
import com.tzuchi.his.quality.inspection.domain.reviewcase.event.RereviewCompletedEvent;
import com.tzuchi.his.quality.inspection.domain.reviewcase.event.RereviewStartedEvent;
import com.tzuchi.his.quality.inspection.domain.reviewcase.event.RereviewerAssignedEvent;
import com.tzuchi.his.quality.inspection.domain.reviewcase.event.ReviewCaseClosedEvent;
import com.tzuchi.his.quality.inspection.domain.reviewcase.event.ReviewCaseEscalatedToRereviewEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * D-069 第 4 點、D-070：複審切片的事件類別有規格用到的屬性；改名與加屬性之後，稽核紀錄的 action 與 detail 逐字不變。
 * 期望的 action 與 detail 是改名之前的輸出。
 */
class RereviewSliceEventPropertiesTest {

    private static final Object SOURCE = new Object();
    private static final Instant DUE_AT = Instant.parse("2026-12-03T23:59:59Z");

    @Test
    void initialReviewFixed() {
        InitialReviewFixedEvent e = new InitialReviewFixedEvent(SOURCE, "e-1", java.time.Instant.EPOCH, "u1", "U1", "H001", "c1", "s1");
        assertAll(
                () -> assertEquals("c1", e.getCaseId()),
                () -> assertEquals("s1", e.getSubmissionId()),
                () -> assertEquals("INITIAL_REVIEW_FIXED", e.getAction()),
                () -> assertEquals("{\"submissionId\":\"s1\"}", e.getDetail()));
    }

    @Test
    void escalatedOnFix() {
        ReviewCaseEscalatedToRereviewEvent e = ReviewCaseEscalatedToRereviewEvent.onFix(
                SOURCE, "e-1", java.time.Instant.EPOCH, "u1", "U1", "H001", "c1", "p1", "s1", new BigDecimal("20"), new BigDecimal("5"));
        assertAll(
                () -> assertEquals("c1", e.getCaseId()),
                () -> assertEquals("p1", e.getProjectId()),
                () -> assertEquals("s1", e.getTriggeredBySubmissionId()),
                () -> assertEquals(new BigDecimal("20"), e.getInitialScore()),
                () -> assertEquals(new BigDecimal("5"), e.getQualityThreshold()),
                () -> assertEquals("CASE_ESCALATED_TO_REREVIEW", e.getAction()),
                () -> assertEquals("{\"score\":20,\"threshold\":5}", e.getDetail()));
    }

    @Test
    void escalatedManually() {
        ReviewCaseEscalatedToRereviewEvent e = ReviewCaseEscalatedToRereviewEvent.manual(
                SOURCE, "e-1", java.time.Instant.EPOCH, "u1", "U1", "H001", "c1", "p1", new BigDecimal("5"));
        assertAll(
                () -> assertEquals("c1", e.getCaseId()),
                () -> assertEquals("p1", e.getProjectId()),
                () -> assertNull(e.getTriggeredBySubmissionId()),
                () -> assertNull(e.getQualityThreshold()),
                () -> assertEquals("CASE_ESCALATED_TO_REREVIEW", e.getAction()),
                () -> assertEquals("{\"trigger\":\"manual\"}", e.getDetail()));
    }

    @Test
    void rereviewerAssigned() {
        RereviewerAssignedEvent e = new RereviewerAssignedEvent(SOURCE, "e-1", java.time.Instant.EPOCH, "u1", "U1", "H001", "c1", "rr1", DUE_AT, false);
        assertAll(
                () -> assertEquals("c1", e.getCaseId()),
                () -> assertEquals("rr1", e.getRereviewerId()),
                () -> assertEquals(DUE_AT, e.getDueAt()),
                () -> assertEquals("REREVIEWER_ASSIGNED", e.getAction()),
                () -> assertEquals("{\"rereviewerId\":\"rr1\"}", e.getDetail()));
    }

    @Test
    void rereviewerReassigned() {
        RereviewerAssignedEvent e = new RereviewerAssignedEvent(SOURCE, "e-1", java.time.Instant.EPOCH, "u1", "U1", "H001", "c1", "rr2", DUE_AT, true);
        assertAll(
                () -> assertEquals("rr2", e.getRereviewerId()),
                () -> assertEquals("REREVIEWER_ASSIGNED", e.getAction()),
                () -> assertEquals("{\"rereviewerId\":\"rr2\",\"reassignment\":true}", e.getDetail()));
    }

    @Test
    void rereviewStarted() {
        RereviewStartedEvent e = new RereviewStartedEvent(SOURCE, "e-1", java.time.Instant.EPOCH, "u1", "U1", "H001", "c1", "s2", "rr1");
        assertAll(
                () -> assertEquals("c1", e.getCaseId()),
                () -> assertEquals("s2", e.getSubmissionId()),
                () -> assertEquals("rr1", e.getRereviewerId()),
                () -> assertEquals("REREVIEW_STARTED", e.getAction()),
                () -> assertEquals("{\"submissionId\":\"s2\"}", e.getDetail()));
    }

    @Test
    void rereviewCompleted() {
        RereviewCompletedEvent e = new RereviewCompletedEvent(SOURCE, "e-1", java.time.Instant.EPOCH, "u1", "U1", "H001", "c1", "s2", "rr1",
                new BigDecimal("15"), new BigDecimal("10.00"));
        assertAll(
                () -> assertEquals("c1", e.getCaseId()),
                () -> assertEquals("s2", e.getSubmissionId()),
                () -> assertEquals("rr1", e.getRereviewerId()),
                () -> assertEquals(new BigDecimal("15"), e.getRereviewScore()),
                () -> assertEquals(new BigDecimal("10.00"), e.getFinalScore()),
                () -> assertEquals("REREVIEW_SUBMITTED", e.getAction()),
                () -> assertEquals("{\"rereviewScore\":15,\"finalScore\":10.00}", e.getDetail()));
    }

    @Test
    void reviewCaseClosed() {
        ReviewCaseClosedEvent e = new ReviewCaseClosedEvent(SOURCE, "e-1", java.time.Instant.EPOCH, "u1", "U1", "H001", "c1", "s2");
        assertAll(
                () -> assertEquals("c1", e.getCaseId()),
                () -> assertEquals("s2", e.getSubmissionId()),
                () -> assertEquals("CASE_CLOSED_AFTER_REREVIEW", e.getAction()),
                () -> assertEquals("{\"submissionId\":\"s2\"}", e.getDetail()));
    }
}
