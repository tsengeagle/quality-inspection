package com.tzuchi.his.quality.inspection.domain.reviewcase.submission;

import com.tzuchi.his.quality.inspection.domain.common.DomainStateConflictException;

import java.math.BigDecimal;
import java.time.Instant;
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
    private Instant submittedAt;
    private String basedOnScoreSheetVersionId;
    private String supersededBySubmissionId;
    private String attachmentRefsJson;
    private Instant createdAt;
    private Instant updatedAt;

    protected ReviewSubmission() {
        // for persistence / reconstitution
    }

    /**
     * Create an initial review draft submission.
     * Called by StartInitialReviewUseCase.
     */
    public static ReviewSubmission createInitialDraft(
            String submissionId, String caseId, String reviewerId,
            String scoreSheetVersionId, Instant createdAt) {
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
        sub.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        sub.updatedAt = sub.createdAt;
        return sub;
    }

    /**
     * Create a rereview draft submission.
     * Called by StartRereviewUseCase.
     * RS-INV-05（D-085 第 11 點）：scoreSheetVersionId 是案件的評分表版本。
     */
    public static ReviewSubmission createRereviewDraft(String submissionId, String caseId, String reviewerId,
                                                       String scoreSheetVersionId, Instant createdAt) {
        Objects.requireNonNull(scoreSheetVersionId, "scoreSheetVersionId must not be null");
        ReviewSubmission s = new ReviewSubmission();
        s.basedOnScoreSheetVersionId = scoreSheetVersionId;
        s.submissionId = submissionId;
        s.caseId = caseId;
        s.reviewerId = reviewerId;
        s.stage = ReviewStage.REREVIEW;
        s.status = SubmissionStatus.DRAFT;
        s.revisionNo = 1;
        s.answerSet = new ArrayList<>();
        s.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        s.updatedAt = createdAt;
        return s;
    }

    /**
     * Reconstitute from persistence. No business validation.
     */
    public static ReviewSubmission reconstitute(
            String submissionId, String caseId, ReviewStage stage,
            String reviewerId, SubmissionStatus status, int revisionNo,
            List<AnswerItem> answerSet, BigDecimal totalScore,
            String comment, Instant submittedAt,
            String basedOnScoreSheetVersionId,
            String supersededBySubmissionId,
            String attachmentRefsJson,
            Instant createdAt, Instant updatedAt) {
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
    public void updateDraft(List<AnswerItem> scoredAnswers, BigDecimal totalScore, String comment,
                            Instant updatedAt) {
        requireDraft();
        Objects.requireNonNull(scoredAnswers, "scoredAnswers must not be null");
        Objects.requireNonNull(totalScore, "totalScore must not be null");
        this.answerSet = new ArrayList<>(scoredAnswers);
        this.totalScore = totalScore;
        this.comment = comment;
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    /**
     * Submit this draft. Transitions DRAFT → SUBMITTED. Irreversible.
     */
    public void submit(Instant submittedAt) {
        requireDraft();
        Objects.requireNonNull(submittedAt, "submittedAt must not be null");
        this.status = SubmissionStatus.SUBMITTED;
        this.submittedAt = submittedAt;
        this.updatedAt = submittedAt;
    }

    /**
     * Create a revision from an existing SUBMITTED submission.
     * The new submission is directly SUBMITTED (no DRAFT phase for revisions).
     */
    public static ReviewSubmission createRevision(
            String newSubmissionId,
            ReviewSubmission oldSubmission,
            List<AnswerItem> scoredAnswers,
            BigDecimal totalScore,
            String comment,
            Instant submittedAt) {
        Objects.requireNonNull(submittedAt, "submittedAt must not be null");
        Objects.requireNonNull(newSubmissionId, "newSubmissionId must not be null");
        Objects.requireNonNull(oldSubmission, "oldSubmission must not be null");
        Objects.requireNonNull(scoredAnswers, "scoredAnswers must not be null");
        Objects.requireNonNull(totalScore, "totalScore must not be null");

        if (oldSubmission.status != SubmissionStatus.SUBMITTED) {
            throw new DomainStateConflictException("SUBMISSION_NOT_SUBMITTED",
                    "Can only revise a SUBMITTED submission. Current: " + oldSubmission.status);
        }

        ReviewSubmission rev = new ReviewSubmission();
        rev.submissionId = newSubmissionId;
        rev.caseId = oldSubmission.caseId;
        rev.stage = oldSubmission.stage;
        rev.reviewerId = oldSubmission.reviewerId;
        rev.status = SubmissionStatus.SUBMITTED;
        rev.revisionNo = oldSubmission.revisionNo + 1;
        rev.answerSet = new ArrayList<>(scoredAnswers);
        rev.totalScore = totalScore;
        rev.comment = comment;
        rev.submittedAt = submittedAt;
        rev.basedOnScoreSheetVersionId = oldSubmission.basedOnScoreSheetVersionId;
        rev.createdAt = submittedAt;
        rev.updatedAt = rev.createdAt;
        return rev;
    }

    /**
     * Mark this submission as SUPERSEDED by a newer revision.
     * Only SUBMITTED submissions can be superseded.
     */
    public void markSuperseded(String newSubmissionId, Instant updatedAt) {
        if (this.status != SubmissionStatus.SUBMITTED) {
            throw new DomainStateConflictException("SUBMISSION_NOT_SUBMITTED",
                    "Can only supersede a SUBMITTED submission. Current: " + this.status);
        }
        Objects.requireNonNull(newSubmissionId, "newSubmissionId must not be null");
        this.supersededBySubmissionId = newSubmissionId;
        this.status = SubmissionStatus.SUPERSEDED;
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
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
    public Instant getSubmittedAt() { return submittedAt; }
    public String getBasedOnScoreSheetVersionId() { return basedOnScoreSheetVersionId; }
    public String getSupersededBySubmissionId() { return supersededBySubmissionId; }
    public String getAttachmentRefsJson() { return attachmentRefsJson; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
