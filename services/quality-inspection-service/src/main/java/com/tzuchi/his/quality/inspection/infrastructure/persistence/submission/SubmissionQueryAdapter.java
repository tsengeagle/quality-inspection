package com.tzuchi.his.quality.inspection.infrastructure.persistence.submission;

import com.tzuchi.his.quality.inspection.application.reviewcase.query.AnswerDetail;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.SubmissionDetailView;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.SubmissionQueryPort;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewStage;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.SubmissionStatus;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class SubmissionQueryAdapter implements SubmissionQueryPort {

    private final EntityManager em;
    private final ReviewSubmissionJpaRepository submissionRepository;

    public SubmissionQueryAdapter(EntityManager em, ReviewSubmissionJpaRepository submissionRepository) {
        this.em = em;
        this.submissionRepository = submissionRepository;
    }

    @Override
    public Optional<SubmissionDetailView> findDetailById(String submissionId) {
        ReviewSubmissionEntity entity = em.find(ReviewSubmissionEntity.class, submissionId);
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.of(toDetailView(entity));
    }

    @Override
    public boolean existsSubmittedRereview(String caseId) {
        return submissionRepository.existsByCaseIdAndStageAndStatus(
                caseId, ReviewStage.REREVIEW, SubmissionStatus.SUBMITTED);
    }

    private SubmissionDetailView toDetailView(ReviewSubmissionEntity e) {
        List<AnswerDetail> answers = (e.getAnswerSet() != null)
                ? e.getAnswerSet().stream()
                    .map(a -> new AnswerDetail(a.getItemId(), a.getAnswerValue(), a.getDerivedScore()))
                    .collect(Collectors.toList())
                : Collections.emptyList();

        return new SubmissionDetailView(
                e.getSubmissionId(), e.getCaseId(), e.getStage().name(),
                e.getRevisionNo(), e.getStatus().name(), e.getReviewerId(),
                answers, e.getComment(), e.getAttachmentRefsJson(),
                e.getTotalScore(), e.getCreatedAt(), e.getSubmittedAt(), null);
    }
}
