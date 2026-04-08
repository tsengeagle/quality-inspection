package com.tzuchi.his.quality.inspection.domain.reviewcase.submission;

import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Aggregate root for a review submission.
 * B3a: shell with getters only.
 * B3b: full business methods — createDraft, updateDraft, submit.
 */
public class ReviewSubmission {

    private String submissionId;
    private String caseId;
    private ReviewStage stage;
    private String reviewerId;
    private SubmissionStatus status;
    private int revisionNo;
    private List<AnswerItem> answerSet;
    private BigDecimal totalScore;
    private String comment;
    private LocalDateTime submittedAt;
    private String basedOnScoreSheetVersionId;
    private String supersededBySubmissionId;
    private String attachmentRefsJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected ReviewSubmission() {
        // for persistence / reconstitution
    }

    /**
     * Create an initial review draft submission.
     * Called by StartInitialReviewUseCase.
     */
    public static ReviewSubmission createInitialDraft(
            String submissionId, String caseId, String reviewerId,
            String scoreSheetVersionId) {
        Objects.requireNonNull(submissionId, "submissionId must not be null");
        Objects.requireNonNull(caseId, "caseId must not be null");
        Objects.requireNonNull(reviewerId, "reviewerId must not be null");
        Objects.requireNonNull(scoreSheetVersionId, "scoreSheetVersionId must not be null");

        ReviewSubmission sub = new ReviewSubmission();
        sub.submissionId = submissionId;
        sub.caseId = caseId;
        sub.stage = ReviewStage.INITIAL;
        sub.reviewerId = reviewerId;
        sub.status = SubmissionStatus.DRAFT;
        sub.revisionNo = 1;
        sub.answerSet = new ArrayList<>();
        sub.totalScore = BigDecimal.ZERO;
        sub.basedOnScoreSheetVersionId = scoreSheetVersionId;
        sub.createdAt = LocalDateTime.now();
        sub.updatedAt = sub.createdAt;
        return sub;
    }

    /**
     * Reconstitute from persistence. No business validation.
     */
    public static ReviewSubmission reconstitute(
            String submissionId, String caseId, ReviewStage stage,
            String reviewerId, SubmissionStatus status, int revisionNo,
            List<AnswerItem> answerSet, BigDecimal totalScore,
            String comment, LocalDateTime submittedAt,
            String basedOnScoreSheetVersionId,
            String supersededBySubmissionId,
            String attachmentRefsJson,
            LocalDateTime createdAt, LocalDateTime updatedAt) {
        ReviewSubmission sub = new ReviewSubmission();
        sub.submissionId = submissionId;
        sub.caseId = caseId;
        sub.stage = stage;
        sub.reviewerId = reviewerId;
        sub.status = status;
        sub.revisionNo = revisionNo;
        sub.answerSet = answerSet != null ? new ArrayList<>(answerSet) : new ArrayList<>();
        sub.totalScore = totalScore;
        sub.comment = comment;
        sub.submittedAt = submittedAt;
        sub.basedOnScoreSheetVersionId = basedOnScoreSheetVersionId;
        sub.supersededBySubmissionId = supersededBySubmissionId;
        sub.attachmentRefsJson = attachmentRefsJson;
        sub.createdAt = createdAt;
        sub.updatedAt = updatedAt;
        return sub;
    }

    /**
     * Update draft answers and score. Can be called multiple times while in DRAFT.
     */
    public void updateDraft(List<AnswerItem> scoredAnswers, BigDecimal totalScore, String comment) {
        requireDraft();
        Objects.requireNonNull(scoredAnswers, "scoredAnswers must not be null");
        Objects.requireNonNull(totalScore, "totalScore must not be null");
        this.answerSet = new ArrayList<>(scoredAnswers);
        this.totalScore = totalScore;
        this.comment = comment;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Submit this draft. Transitions DRAFT → SUBMITTED. Irreversible.
     */
    public void submit() {
        requireDraft();
        this.status = SubmissionStatus.SUBMITTED;
        this.submittedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    private void requireDraft() {
        if (this.status != SubmissionStatus.DRAFT) {
            throw new DomainStateConflictException("SUBMISSION_NOT_DRAFT",
                    "Submission must be in DRAFT status. Current: " + this.status);
        }
    }

    // --- Getters ---

    public String getSubmissionId() { return submissionId; }
    public String getCaseId() { return caseId; }
    public ReviewStage getStage() { return stage; }
    public String getReviewerId() { return reviewerId; }
    public SubmissionStatus getStatus() { return status; }
    public int getRevisionNo() { return revisionNo; }
    public List<AnswerItem> getAnswerSet() { return Collections.unmodifiableList(answerSet != null ? answerSet : Collections.emptyList()); }
    public BigDecimal getTotalScore() { return totalScore; }
    public String getComment() { return comment; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public String getBasedOnScoreSheetVersionId() { return basedOnScoreSheetVersionId; }
    public String getSupersededBySubmissionId() { return supersededBySubmissionId; }
    public String getAttachmentRefsJson() { return attachmentRefsJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
