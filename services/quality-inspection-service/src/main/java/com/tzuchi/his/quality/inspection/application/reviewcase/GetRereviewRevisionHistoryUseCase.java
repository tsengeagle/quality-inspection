package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.GetRereviewRevisionHistoryQuery;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.RevisionHistoryEntry;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.RevisionHistoryView;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCase;
import com.tzuchi.his.quality.inspection.domain.reviewcase.ReviewCaseRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewStage;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmission;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.ReviewSubmissionRepository;
import com.tzuchi.his.quality.inspection.domain.reviewcase.submission.SubmissionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class GetRereviewRevisionHistoryUseCase {

    private final ReviewSubmissionRepository submissionRepository;
    private final ReviewCaseRepository reviewCaseRepository;

    public GetRereviewRevisionHistoryUseCase(ReviewSubmissionRepository submissionRepository,
                                              ReviewCaseRepository reviewCaseRepository) {
        this.submissionRepository = submissionRepository;
        this.reviewCaseRepository = reviewCaseRepository;
    }

    public RevisionHistoryView execute(ActorContext actor, GetRereviewRevisionHistoryQuery query) {
        String caseId = query.getCaseId();

        ReviewCase reviewCase = reviewCaseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("CASE_NOT_FOUND", "ReviewCase not found: " + caseId));
        actor.requireReadScope(reviewCase.getHospitalId());

        // Any authenticated role can query — visibility enforced at API/UI layer
        List<ReviewSubmission> sorted = submissionRepository.findAllByCaseId(caseId).stream()
                .filter(s -> s.getStage() == ReviewStage.REREVIEW)
                .sorted(Comparator.comparingInt(ReviewSubmission::getRevisionNo))
                .collect(Collectors.toList());

        boolean isClosed = CaseStatus.CLOSED == reviewCase.getStatus();
        boolean isOriginalRereviewer = sorted.stream()
                .anyMatch(s -> (s.getStatus() == SubmissionStatus.SUBMITTED
                                || s.getStatus() == SubmissionStatus.SUPERSEDED)
                        && actor.getUserId().equals(s.getReviewerId()));
        boolean hasExceptionEntry = isClosed && isOriginalRereviewer;

        List<RevisionHistoryEntry> entries = sorted.stream()
                .map(s -> new RevisionHistoryEntry(
                        s.getSubmissionId(), s.getStage().name(), s.getReviewerId(),
                        s.getStatus().name(), s.getRevisionNo(), s.getTotalScore(),
                        s.getComment(), s.getSubmittedAt(), s.getSupersededBySubmissionId()))
                .collect(Collectors.toList());

        return new RevisionHistoryView(caseId, entries, hasExceptionEntry);
    }
}
