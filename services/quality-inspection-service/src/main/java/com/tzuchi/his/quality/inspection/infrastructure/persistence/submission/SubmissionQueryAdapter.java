package com.tzuchi.his.quality.inspection.infrastructure.persistence.submission;

import com.tzuchi.his.quality.inspection.application.reviewcase.query.AnswerDetail;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.SubmissionDetailView;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.SubmissionQueryPort;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.AnswerItem;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class SubmissionQueryAdapter implements SubmissionQueryPort {

    private final EntityManager em;

    public SubmissionQueryAdapter(EntityManager em) {
        this.em = em;
    }

    @Override
    public Optional<SubmissionDetailView> findDetailById(String submissionId) {
        ReviewSubmissionEntity entity = em.find(ReviewSubmissionEntity.class, submissionId);
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.of(toDetailView(entity));
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
                e.getTotalScore(), e.getCreatedAt(), e.getSubmittedAt());
    }
}
