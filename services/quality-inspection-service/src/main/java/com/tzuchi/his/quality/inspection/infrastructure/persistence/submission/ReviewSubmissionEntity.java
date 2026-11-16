package com.tzuchi.his.quality.inspection.infrastructure.persistence.submission;

import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewStage;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.SubmissionStatus;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "review_submission")
public class ReviewSubmissionEntity {

    @Id
    @Column(name = "submission_id")
    private String submissionId;

    @Column(name = "case_id", nullable = false)
    private String caseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewStage stage;

    @Column(name = "reviewer_id", nullable = false)
    private String reviewerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubmissionStatus status;

    @Column(name = "revision_no")
    private int revisionNo;

    @Convert(converter = AnswerSetJsonConverter.class)
    @Lob
    @Column(name = "answer_set_json")
    private List<AnswerItem> answerSet;

    @Column(name = "total_score", precision = 10, scale = 2)
    private BigDecimal totalScore;

    // COMMENT 是 Oracle 保留字（D-045 第 5 點），欄位名用 comment_text；Java 欄位與 API 名稱不變。
    @Column(name = "comment_text")
    private String comment;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "based_on_score_sheet_version_id")
    private String basedOnScoreSheetVersionId;

    @Column(name = "superseded_by_submission_id")
    private String supersededBySubmissionId;

    @Lob
    @Column(name = "attachment_refs_json")
    private String attachmentRefsJson;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public ReviewSubmissionEntity() {}

    public String getSubmissionId() { return submissionId; }
    public void setSubmissionId(String submissionId) { this.submissionId = submissionId; }
    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }
    public ReviewStage getStage() { return stage; }
    public void setStage(ReviewStage stage) { this.stage = stage; }
    public String getReviewerId() { return reviewerId; }
    public void setReviewerId(String reviewerId) { this.reviewerId = reviewerId; }
    public SubmissionStatus getStatus() { return status; }
    public void setStatus(SubmissionStatus status) { this.status = status; }
    public int getRevisionNo() { return revisionNo; }
    public void setRevisionNo(int revisionNo) { this.revisionNo = revisionNo; }
    public List<AnswerItem> getAnswerSet() { return answerSet; }
    public void setAnswerSet(List<AnswerItem> answerSet) { this.answerSet = answerSet; }
    public BigDecimal getTotalScore() { return totalScore; }
    public void setTotalScore(BigDecimal totalScore) { this.totalScore = totalScore; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public String getBasedOnScoreSheetVersionId() { return basedOnScoreSheetVersionId; }
    public void setBasedOnScoreSheetVersionId(String v) { this.basedOnScoreSheetVersionId = v; }
    public String getSupersededBySubmissionId() { return supersededBySubmissionId; }
    public void setSupersededBySubmissionId(String v) { this.supersededBySubmissionId = v; }
    public String getAttachmentRefsJson() { return attachmentRefsJson; }
    public void setAttachmentRefsJson(String v) { this.attachmentRefsJson = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
