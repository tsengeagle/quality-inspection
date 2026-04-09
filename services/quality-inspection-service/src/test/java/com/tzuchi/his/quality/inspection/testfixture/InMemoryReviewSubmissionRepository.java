package com.tzuchi.his.quality.inspection.testfixture;

import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewStage;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmissionRepository;

import java.util.*;
import java.util.stream.Collectors;

public class InMemoryReviewSubmissionRepository implements ReviewSubmissionRepository {
    public final Map<String, ReviewSubmission> store = new LinkedHashMap<>();

    @Override
    public void save(ReviewSubmission submission) {
        store.put(submission.getSubmissionId(), submission);
    }

    @Override
    public Optional<ReviewSubmission> findById(String submissionId) {
        return Optional.ofNullable(store.get(submissionId));
    }

    @Override
    public List<ReviewSubmission> findByCaseIdAndStage(String caseId, ReviewStage stage) {
        return store.values().stream()
                .filter(s -> caseId.equals(s.getCaseId()) && s.getStage() == stage)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ReviewSubmission> findLatestByCaseIdAndStage(String caseId, ReviewStage stage) {
        List<ReviewSubmission> matching = findByCaseIdAndStage(caseId, stage);
        return matching.isEmpty() ? Optional.empty() : Optional.of(matching.get(matching.size() - 1));
    }

    @Override
    public List<ReviewSubmission> findAllByCaseId(String caseId) {
        return store.values().stream()
                .filter(s -> caseId.equals(s.getCaseId()))
                .collect(Collectors.toList());
    }
}
