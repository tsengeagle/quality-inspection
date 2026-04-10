package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import javax.validation.constraints.NotBlank;
import java.util.List;

public class SubmitRereviewRequest {
    @NotBlank
    private String submissionId;
    private List<SaveInitialReviewDraftRequest.AnswerInputDto> answers;
    private String comment;

    public String getSubmissionId() { return submissionId; }
    public void setSubmissionId(String submissionId) { this.submissionId = submissionId; }
    public List<SaveInitialReviewDraftRequest.AnswerInputDto> getAnswers() { return answers; }
    public void setAnswers(List<SaveInitialReviewDraftRequest.AnswerInputDto> answers) { this.answers = answers; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
