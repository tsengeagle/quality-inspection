package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import java.util.List;

public class ReviseRereviewRequest {

    private List<SaveInitialReviewDraftRequest.AnswerInputDto> answers;
    private String comment;

    public List<SaveInitialReviewDraftRequest.AnswerInputDto> getAnswers() { return answers; }
    public void setAnswers(List<SaveInitialReviewDraftRequest.AnswerInputDto> answers) { this.answers = answers; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
