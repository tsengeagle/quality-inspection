package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

public class ReviseInitialReviewRequest {

    @NotNull(message = "answers must not be null")
    @Valid
    private List<SaveInitialReviewDraftRequest.AnswerInputDto> answers;

    private String comment;

    public List<SaveInitialReviewDraftRequest.AnswerInputDto> getAnswers() {
        return answers;
    }

    public void setAnswers(List<SaveInitialReviewDraftRequest.AnswerInputDto> answers) {
        this.answers = answers;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}