package com.tzuchi.his.quality.inspection.domain.reviewcase;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ReviewerAssignmentTest {

    @Test
    void create_withValidArgs_shouldSetFields() {
        LocalDateTime now = LocalDateTime.of(2026, 1, 15, 10, 0);
        ReviewerAssignment assignment = new ReviewerAssignment("reviewer-1", now);

        assertEquals("reviewer-1", assignment.getReviewerId());
        assertEquals(now, assignment.getAssignedAt());
    }

    @Test
    void create_nullReviewerId_shouldThrow() {
        assertThrows(NullPointerException.class,
                () -> new ReviewerAssignment(null, LocalDateTime.now()));
    }

    @Test
    void create_nullAssignedAt_shouldThrow() {
        assertThrows(NullPointerException.class,
                () -> new ReviewerAssignment("reviewer-1", null));
    }

    @Test
    void equals_sameValues_shouldBeEqual() {
        LocalDateTime now = LocalDateTime.of(2026, 1, 15, 10, 0);
        ReviewerAssignment a1 = new ReviewerAssignment("reviewer-1", now);
        ReviewerAssignment a2 = new ReviewerAssignment("reviewer-1", now);

        assertEquals(a1, a2);
        assertEquals(a1.hashCode(), a2.hashCode());
    }

    @Test
    void equals_differentValues_shouldNotBeEqual() {
        LocalDateTime now = LocalDateTime.of(2026, 1, 15, 10, 0);
        ReviewerAssignment a1 = new ReviewerAssignment("reviewer-1", now);
        ReviewerAssignment a2 = new ReviewerAssignment("reviewer-2", now);

        assertNotEquals(a1, a2);
    }
}
