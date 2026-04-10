package com.tzuchi.his.quality.inspection.application.reviewcase.command;

import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.List;

public class SaveRereviewDraftCommand {

    @NotBlank
    private String caseId;
    @NotBlank
    private String submissionId;
    private List<AnswerItem> answers;
    private BigDecimal totalScore;
    private String comment;

    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }

    public String getSubmissionId() { return submissionId; }
    public void setSubmissionId(String submissionId) { this.submissionId = submissionId; }

    public List<AnswerItem> getAnswers() { return answers; }
    public void setAnswers(List<AnswerItem> answers) { this.answers = answers; }

    public BigDecimal getTotalScore() { return totalScore; }
    public void setTotalScore(BigDecimal totalScore) { this.totalScore = totalScore; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
