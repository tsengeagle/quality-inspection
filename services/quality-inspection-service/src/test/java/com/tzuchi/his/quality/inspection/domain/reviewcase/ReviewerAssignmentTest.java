package com.tzuchi.his.quality.inspection.domain.reviewcase;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ReviewerAssignmentTest {

    @Test
    void create_withValidArgs_shouldSetFields() {
        Instant now = Instant.parse("2026-01-15T10:00:00Z");
        ReviewerAssignment assignment = new ReviewerAssignment("reviewer-1", now);

        assertEquals("reviewer-1", assignment.getReviewerId());
        assertEquals(now, assignment.getAssignedAt());
    }

    @Test
    void create_nullReviewerId_shouldThrow() {
        assertThrows(NullPointerException.class,
                () -> new ReviewerAssignment(null, Instant.now()));
    }

    @Test
    void create_nullAssignedAt_shouldThrow() {
        assertThrows(NullPointerException.class,
                () -> new ReviewerAssignment("reviewer-1", null));
    }

    @Test
    void equals_sameValues_shouldBeEqual() {
        Instant now = Instant.parse("2026-01-15T10:00:00Z");
        ReviewerAssignment a1 = new ReviewerAssignment("reviewer-1", now);
        ReviewerAssignment a2 = new ReviewerAssignment("reviewer-1", now);

        assertEquals(a1, a2);
        assertEquals(a1.hashCode(), a2.hashCode());
    }

    @Test
    void equals_differentValues_shouldNotBeEqual() {
        Instant now = Instant.parse("2026-01-15T10:00:00Z");
        ReviewerAssignment a1 = new ReviewerAssignment("reviewer-1", now);
        ReviewerAssignment a2 = new ReviewerAssignment("reviewer-2", now);

        assertNotEquals(a1, a2);
    }
}
