package com.tzuchi.his.quality.inspection.application.reviewcase.query;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubmissionDetailView {

    private final String submissionId;
    private final String caseId;
    private final String stage;
    private final int revisionNo;
    private final String status;
    private final String reviewerId;
    private final List<AnswerDetail> answers;
    private final String comment;
    private final String attachmentRefsJson;
    private final BigDecimal totalScore;
    private final Instant createdAt;
    private final Instant submittedAt;
    private final List<String> maskedFields;

    public SubmissionDetailView(String submissionId, String caseId, String stage,
                                 int revisionNo, String status, String reviewerId,
                                 List<AnswerDetail> answers, String comment,
                                 String attachmentRefsJson, BigDecimal totalScore,
                                 Instant createdAt, Instant submittedAt,
                                 List<String> maskedFields) {
        this.submissionId = submissionId;
        this.caseId = caseId;
        this.stage = stage;
        this.revisionNo = revisionNo;
        this.status = status;
        this.reviewerId = reviewerId;
        this.answers = answers;
        this.comment = comment;
        this.attachmentRefsJson = attachmentRefsJson;
        this.totalScore = totalScore;
        this.createdAt = createdAt;
        this.submittedAt = submittedAt;
        this.maskedFields = maskedFields;
    }

    public String getSubmissionId() { return submissionId; }
    public String getCaseId() { return caseId; }
    public String getStage() { return stage; }
    public int getRevisionNo() { return revisionNo; }
    public String getStatus() { return status; }
    public String getReviewerId() { return reviewerId; }
    public List<AnswerDetail> getAnswers() { return answers; }
    public String getComment() { return comment; }
    public String getAttachmentRefsJson() { return attachmentRefsJson; }
    public BigDecimal getTotalScore() { return totalScore; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getSubmittedAt() { return submittedAt; }
    public List<String> getMaskedFields() { return maskedFields; }
}
