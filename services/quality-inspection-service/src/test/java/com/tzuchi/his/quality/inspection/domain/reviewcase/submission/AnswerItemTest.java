package com.tzuchi.his.quality.inspection.domain.reviewcase.submission;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class AnswerItemTest {

    @Test
    void ofRaw_shouldCreateWithNullDerivedScore() {
        AnswerItem item = AnswerItem.ofRaw("Q001", "true");

        assertEquals("Q001", item.getItemId());
        assertEquals("true", item.getAnswerValue());
        assertNull(item.getDerivedScore());
    }

    @Test
    void ofScored_shouldCreateWithAllFields() {
        AnswerItem item = AnswerItem.ofScored("Q001", "true", new BigDecimal("10.00"));

        assertEquals("Q001", item.getItemId());
        assertEquals("true", item.getAnswerValue());
        assertEquals(new BigDecimal("10.00"), item.getDerivedScore());
    }

    @Test
    void reconstitute_shouldRestoreAllFields() {
        AnswerItem item = AnswerItem.reconstitute("Q002", "85", new BigDecimal("85.00"));

        assertEquals("Q002", item.getItemId());
        assertEquals("85", item.getAnswerValue());
        assertEquals(new BigDecimal("85.00"), item.getDerivedScore());
    }

    @Test
    void ofRaw_nullItemId_shouldThrow() {
        assertThrows(NullPointerException.class,
                () -> AnswerItem.ofRaw(null, "true"));
    }

    @Test
    void ofRaw_nullAnswerValue_shouldThrow() {
        assertThrows(NullPointerException.class,
                () -> AnswerItem.ofRaw("Q001", null));
    }

    @Test
    void ofScored_nullDerivedScore_shouldThrow() {
        assertThrows(NullPointerException.class,
                () -> AnswerItem.ofScored("Q001", "true", null));
    }

    @Test
    void ofScored_nullItemId_shouldThrow() {
        assertThrows(NullPointerException.class,
                () -> AnswerItem.ofScored(null, "true", new BigDecimal("10.00")));
    }

    @Test
    void ofScored_nullAnswerValue_shouldThrow() {
        assertThrows(NullPointerException.class,
                () -> AnswerItem.ofScored("Q001", null, new BigDecimal("10.00")));
    }
}
