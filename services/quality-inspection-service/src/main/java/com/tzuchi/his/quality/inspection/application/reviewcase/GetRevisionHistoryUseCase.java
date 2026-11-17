package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
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
// D-055 第 5 點：准許完整初審讀取時會寫入讀取者紀錄並遞增案件版本，所以是可寫入的交易；
// 紀錄在回應送出前 commit，commit 失敗即拒絕這次讀取。
@Transactional
public class GetRevisionHistoryUseCase {

    private final ReviewSubmissionRepository submissionRepository;
    private final ReviewCaseRepository reviewCaseRepository;

    private final ReviewReadAccessPolicy accessPolicy;

    public GetRevisionHistoryUseCase(ReviewSubmissionRepository submissionRepository,
                                     ReviewCaseRepository reviewCaseRepository, ReviewReadAccessPolicy accessPolicy) {
        this.submissionRepository = submissionRepository;
        this.reviewCaseRepository = reviewCaseRepository;
        this.accessPolicy = accessPolicy;
    }

    public RevisionHistoryView execute(ActorContext actor, String caseId) {
        actor.requireAnyRole(DomainRole.REVIEWER, DomainRole.HOSPITAL_MANAGER,
                DomainRole.REREVIEWER, DomainRole.AUDITOR, DomainRole.SYSTEM_ADMIN);

        ReviewCase reviewCase = reviewCaseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("CASE_NOT_FOUND", "ReviewCase not found: " + caseId));

        List<ReviewSubmission> submissions = submissionRepository.findAllByCaseId(caseId);
        accessPolicy.requireAccess(actor, reviewCase, submissions);

        List<ReviewSubmission> sorted = submissions.stream()
                .sorted(Comparator.comparing(ReviewSubmission::getStage)
                        .thenComparing(ReviewSubmission::getRevisionNo))
                .collect(Collectors.toList());

        boolean isClosed = CaseStatus.CLOSED == reviewCase.getStatus();
        boolean isOriginalRereviewer = sorted.stream()
                .anyMatch(s -> s.getStage() == ReviewStage.REREVIEW
                        && (s.getStatus() == SubmissionStatus.SUBMITTED
                                || s.getStatus() == SubmissionStatus.SUPERSEDED)
                        && actor.getUserId().equals(s.getReviewerId()));
        boolean hasExceptionEntry = isClosed && isOriginalRereviewer;

        List<RevisionHistoryEntry> entries = sorted.stream()
                .map(s -> new RevisionHistoryEntry(
                        s.getSubmissionId(), s.getStage().name(), s.getReviewerId(),
                        s.getStatus().name(), s.getRevisionNo(),
                        accessPolicy.canReadContent(actor, s.getStage().name(), s.getReviewerId()) ? s.getTotalScore() : null,
                        accessPolicy.canReadContent(actor, s.getStage().name(), s.getReviewerId()) ? s.getComment() : null, s.getSubmittedAt(), s.getSupersededBySubmissionId()))
                .collect(Collectors.toList());

        return new RevisionHistoryView(caseId, entries, hasExceptionEntry);
    }
}
