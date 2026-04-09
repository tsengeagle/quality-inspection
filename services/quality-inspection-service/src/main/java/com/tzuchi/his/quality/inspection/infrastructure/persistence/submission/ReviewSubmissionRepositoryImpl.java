package com.tzuchi.his.quality.inspection.infrastructure.persistence.submission;

import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewStage;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmissionRepository;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class ReviewSubmissionRepositoryImpl implements ReviewSubmissionRepository {

    private final ReviewSubmissionJpaRepository jpa;

    public ReviewSubmissionRepositoryImpl(ReviewSubmissionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(ReviewSubmission submission) {
        jpa.save(toEntity(submission));
    }

    @Override
    public Optional<ReviewSubmission> findById(String submissionId) {
        return jpa.findById(submissionId).map(this::toDomain);
    }

    @Override
    public List<ReviewSubmission> findByCaseIdAndStage(String caseId, ReviewStage stage) {
        return jpa.findByCaseIdAndStage(caseId, stage).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ReviewSubmission> findLatestByCaseIdAndStage(String caseId, ReviewStage stage) {
        List<ReviewSubmissionEntity> results =
                jpa.findByCaseIdAndStageOrderByRevisionNoDesc(caseId, stage);
        return results.isEmpty() ? Optional.empty() : Optional.of(toDomain(results.get(0)));
    }

    @Override
    public List<ReviewSubmission> findAllByCaseId(String caseId) {
        return jpa.findByCaseId(caseId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private ReviewSubmission toDomain(ReviewSubmissionEntity e) {
        return ReviewSubmission.reconstitute(
                e.getSubmissionId(), e.getCaseId(), e.getStage(),
                e.getReviewerId(), e.getStatus(), e.getRevisionNo(),
                e.getAnswerSet() != null ? e.getAnswerSet() : Collections.emptyList(),
                e.getTotalScore(), e.getComment(), e.getSubmittedAt(),
                e.getBasedOnScoreSheetVersionId(), e.getSupersededBySubmissionId(),
                e.getAttachmentRefsJson(),
                e.getCreatedAt(), e.getUpdatedAt());
    }

    private ReviewSubmissionEntity toEntity(ReviewSubmission s) {
        ReviewSubmissionEntity e = new ReviewSubmissionEntity();
        e.setSubmissionId(s.getSubmissionId());
        e.setCaseId(s.getCaseId());
        e.setStage(s.getStage());
        e.setReviewerId(s.getReviewerId());
        e.setStatus(s.getStatus());
        e.setRevisionNo(s.getRevisionNo());
        e.setAnswerSet(s.getAnswerSet());
        e.setTotalScore(s.getTotalScore());
        e.setComment(s.getComment());
        e.setSubmittedAt(s.getSubmittedAt());
        e.setBasedOnScoreSheetVersionId(s.getBasedOnScoreSheetVersionId());
        e.setSupersededBySubmissionId(s.getSupersededBySubmissionId());
        e.setAttachmentRefsJson(s.getAttachmentRefsJson());
        e.setCreatedAt(s.getCreatedAt());
        e.setUpdatedAt(s.getUpdatedAt());
        return e;
    }
}
