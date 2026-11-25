package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import javax.validation.Valid;
import javax.validation.constraints.Size;
import java.util.List;

public class SaveInitialReviewDraftRequest {

    @Valid
    private List<AnswerInputDto> answers;

    private String comment;

    private List<AttachmentRef> attachments;

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

    public List<AttachmentRef> getAttachments() { return attachments; }
    public void setAttachments(List<AttachmentRef> attachments) { this.attachments = attachments; }

    public static class AnswerInputDto {
        private String itemId;

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
