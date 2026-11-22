package com.tzuchi.his.quality.inspection.domain;

import com.tzuchi.his.quality.inspection.domain.reviewcase.event.InitialReviewFixedEvent;
import com.tzuchi.his.quality.inspection.domain.reviewcase.event.RereviewCompletedEvent;
import com.tzuchi.his.quality.inspection.domain.reviewcase.event.RereviewStartedEvent;
import com.tzuchi.his.quality.inspection.domain.reviewcase.event.RereviewerAssignedEvent;
import com.tzuchi.his.quality.inspection.domain.reviewcase.event.ReviewCaseClosedEvent;
import com.tzuchi.his.quality.inspection.domain.reviewcase.event.ReviewCaseEscalatedToRereviewEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * D-069 第 4 點：複審切片的事件類別有規格用到的屬性；改名與加屬性之後，稽核紀錄的 action 與 detail 逐字不變。
 * 期望的 action 與 detail 是改名之前的輸出。
 */
class RereviewSliceEventPropertiesTest {

    private static final Object SOURCE = new Object();

    @Test
    void initialReviewFixed() {
        InitialReviewFixedEvent e = new InitialReviewFixedEvent(SOURCE, "u1", "U1", "H001", "c1", "s1");
        assertAll(
                () -> assertEquals("c1", e.getCaseId()),
                () -> assertEquals("s1", e.getSubmissionId()),
                () -> assertEquals("INITIAL_REVIEW_FIXED", e.getAction()),
                () -> assertEquals("{\"submissionId\":\"s1\"}", e.getDetail()));
    }

    @Test
    void escalatedOnFix() {
        ReviewCaseEscalatedToRereviewEvent e = ReviewCaseEscalatedToRereviewEvent.onFix(
                SOURCE, "u1", "U1", "H001", "c1", "p1", "s1", new BigDecimal("20"), new BigDecimal("5"));
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
                SOURCE, "u1", "U1", "H001", "c1", "p1", new BigDecimal("5"));
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
        RereviewerAssignedEvent e = new RereviewerAssignedEvent(SOURCE, "u1", "U1", "H001", "c1", "{\"rereviewerId\":\"rr1\"}");
        assertAll(
                () -> assertEquals("c1", e.getCaseId()),
                () -> assertEquals("REREVIEWER_ASSIGNED", e.getAction()));
    }

    @Test
    void rereviewStarted() {
        RereviewStartedEvent e = new RereviewStartedEvent(SOURCE, "u1", "U1", "H001", "c1", "{\"submissionId\":\"s2\"}");
        assertAll(
                () -> assertEquals("c1", e.getCaseId()),
                () -> assertEquals("REREVIEW_STARTED", e.getAction()));
    }

    @Test
    void rereviewCompleted() {
        RereviewCompletedEvent e = new RereviewCompletedEvent(SOURCE, "u1", "U1", "H001", "c1", "{\"rereviewScore\":20,\"finalScore\":20.00}");
        assertAll(
                () -> assertEquals("c1", e.getCaseId()),
                () -> assertEquals("REREVIEW_SUBMITTED", e.getAction()));
    }

    @Test
    void reviewCaseClosed() {
        ReviewCaseClosedEvent e = new ReviewCaseClosedEvent(SOURCE, "u1", "U1", "H001", "c1", "{\"submissionId\":\"s2\"}");
        assertAll(
                () -> assertEquals("c1", e.getCaseId()),
                () -> assertEquals("CASE_CLOSED_AFTER_REREVIEW", e.getAction()));
    }
}
