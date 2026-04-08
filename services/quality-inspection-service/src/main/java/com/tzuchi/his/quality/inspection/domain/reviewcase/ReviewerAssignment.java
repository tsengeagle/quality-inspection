package com.tzuchi.his.quality.inspection.domain.reviewcase;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Value object representing the assignment of a reviewer to a ReviewCase.
 */
public class ReviewerAssignment {

    private final String reviewerId;
    private final LocalDateTime assignedAt;

    public ReviewerAssignment(String reviewerId, LocalDateTime assignedAt) {
        Objects.requireNonNull(reviewerId, "reviewerId must not be null");
        Objects.requireNonNull(assignedAt, "assignedAt must not be null");
        this.reviewerId = reviewerId;
        this.assignedAt = assignedAt;
    }

    public String getReviewerId() { return reviewerId; }

    public LocalDateTime getAssignedAt() { return assignedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReviewerAssignment that = (ReviewerAssignment) o;
        return reviewerId.equals(that.reviewerId) && assignedAt.equals(that.assignedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reviewerId, assignedAt);
    }

    @Override
    public String toString() {
        return "ReviewerAssignment{reviewerId='" + reviewerId + "', assignedAt=" + assignedAt + "}";
    }
}
