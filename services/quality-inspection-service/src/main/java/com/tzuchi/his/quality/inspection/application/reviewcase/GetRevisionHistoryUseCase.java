package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class GetRevisionHistoryUseCase {

    private final ReviewSubmissionRepository submissionRepository;

    public GetRevisionHistoryUseCase(ReviewSubmissionRepository submissionRepository) {
        this.submissionRepository = submissionRepository;
    }

    public List<ReviewSubmission> execute(ActorContext actor, String caseId) {
        actor.requireAnyRole(DomainRole.REVIEWER, DomainRole.HOSPITAL_MANAGER,
                DomainRole.REREVIEWER, DomainRole.AUDITOR);

        List<ReviewSubmission> submissions = submissionRepository.findAllByCaseId(caseId);

        return submissions.stream()
                .sorted(Comparator.comparing(ReviewSubmission::getStage)
                        .thenComparing(ReviewSubmission::getRevisionNo))
                .collect(Collectors.toList());
    }
}