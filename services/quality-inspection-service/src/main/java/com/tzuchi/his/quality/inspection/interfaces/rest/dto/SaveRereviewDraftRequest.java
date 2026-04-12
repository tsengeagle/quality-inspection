package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.List;

public class SaveRereviewDraftRequest {
    @NotBlank
    private String submissionId;
    private List<SaveInitialReviewDraftRequest.AnswerInputDto> answers;
    private BigDecimal totalScore;
    private String comment;
    private List<AttachmentRef> attachments;

    public String getSubmissionId() { return submissionId; }
    public void setSubmissionId(String submissionId) { this.submissionId = submissionId; }
    public List<SaveInitialReviewDraftRequest.AnswerInputDto> getAnswers() { return answers; }
    public void setAnswers(List<SaveInitialReviewDraftRequest.AnswerInputDto> answers) { this.answers = answers; }
    public BigDecimal getTotalScore() { return totalScore; }
    public void setTotalScore(BigDecimal totalScore) { this.totalScore = totalScore; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public List<AttachmentRef> getAttachments() { return attachments; }
    public void setAttachments(List<AttachmentRef> attachments) { this.attachments = attachments; }
}
