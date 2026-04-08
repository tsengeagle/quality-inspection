package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

public class SaveInitialReviewDraftRequest {

    @NotNull(message = "answers must not be null")
    @Valid
    private List<AnswerInputDto> answers;

    private String comment;

    public List<AnswerInputDto> getAnswers() {
        return answers;
    }

    public void setAnswers(List<AnswerInputDto> answers) {
        this.answers = answers;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public static class AnswerInputDto {
        @NotBlank(message = "itemId must not be blank")
        private String itemId;

        @NotNull(message = "answerValue must not be null")
        private String answerValue;

        public AnswerInputDto() {}

        public AnswerInputDto(String itemId, String answerValue) {
            this.itemId = itemId;
            this.answerValue = answerValue;
        }

        public String getItemId() {
            return itemId;
        }

        public void setItemId(String itemId) {
            this.itemId = itemId;
        }

        public String getAnswerValue() {
            return answerValue;
        }

        public void setAnswerValue(String answerValue) {
            this.answerValue = answerValue;
        }
    }
}
