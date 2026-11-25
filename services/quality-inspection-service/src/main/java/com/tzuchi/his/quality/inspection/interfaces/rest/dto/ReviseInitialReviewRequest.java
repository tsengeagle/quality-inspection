package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import javax.validation.Valid;
import java.util.List;

public class ReviseInitialReviewRequest {

    @Valid
    private List<SaveInitialReviewDraftRequest.AnswerInputDto> answers;

    private String comment;

    private List<AttachmentRef> attachments;

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

    public List<AttachmentRef> getAttachments() { return attachments; }
    public void setAttachments(List<AttachmentRef> attachments) { this.attachments = attachments; }
}