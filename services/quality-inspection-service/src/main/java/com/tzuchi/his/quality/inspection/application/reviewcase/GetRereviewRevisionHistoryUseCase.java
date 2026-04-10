package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.GetRereviewRevisionHistoryQuery;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewStage;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class GetRereviewRevisionHistoryUseCase {

    private final ReviewSubmissionRepository submissionRepository;

    public GetRereviewRevisionHistoryUseCase(ReviewSubmissionRepository submissionRepository) {
        this.submissionRepository = submissionRepository;
    }

    public List<ReviewSubmission> execute(ActorContext actor, GetRereviewRevisionHistoryQuery query) {
        // Any authenticated role can query — visibility enforced at API/UI layer
        return submissionRepository.findAllByCaseId(query.getCaseId()).stream()
                .filter(s -> s.getStage() == ReviewStage.REREVIEW)
                .sorted(Comparator.comparingInt(ReviewSubmission::getRevisionNo))
                .collect(Collectors.toList());
    }
}
