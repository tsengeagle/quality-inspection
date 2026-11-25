package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.SubmissionDetailView;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.SubmissionQueryPort;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.ReviewCaseQueryPort;
import com.tzuchi.his.quality.inspection.application.common.AccessDeniedException;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
// D-055 第 5 點：准許完整初審讀取時會寫入讀取者紀錄並遞增案件版本，所以是可寫入的交易；
// 紀錄在回應送出前 commit，commit 失敗即拒絕這次讀取。
@Transactional
public class GetSubmissionDetailUseCase {

    private final SubmissionQueryPort queryPort;

    private final ReviewCaseQueryPort caseQueryPort;
    private final ReviewReadAccessPolicy accessPolicy;

    public GetSubmissionDetailUseCase(SubmissionQueryPort queryPort, ReviewCaseQueryPort caseQueryPort,
                                      ReviewReadAccessPolicy accessPolicy) {
        this.queryPort = queryPort;
        this.caseQueryPort = caseQueryPort;
        this.accessPolicy = accessPolicy;
    }

    public SubmissionDetailView execute(ActorContext actor, String submissionId) {
        actor.requireAnyRole(
                DomainRole.HOSPITAL_MANAGER, DomainRole.REVIEWER,
                DomainRole.REREVIEWER, DomainRole.AUDITOR, DomainRole.SYSTEM_ADMIN);

        SubmissionDetailView view = queryPort.findDetailById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("SUBMISSION_NOT_FOUND", "ReviewSubmission not found: " + submissionId));

        accessPolicy.requireAccess(actor, caseQueryPort.findDetailById(view.getCaseId())
                .orElseThrow(() -> new ResourceNotFoundException("CASE_NOT_FOUND", "ReviewCase not found: " + view.getCaseId())));
        if (actor.hasRole(DomainRole.REREVIEWER) && "REREVIEW".equals(view.getStage())
                && !actor.getUserId().equals(view.getReviewerId())) {
            throw new AccessDeniedException("Cannot read another rereviewer's submission content");
        }
        boolean shouldMask = !accessPolicy.canReadContent(actor, view.getStage(), view.getReviewerId());

        if (!shouldMask) {
            return view;
        }

        return new SubmissionDetailView(
                view.getSubmissionId(),
                view.getCaseId(),
                view.getStage(),
                view.getRevisionNo(),
                view.getStatus(),
                view.getReviewerId(),
                null,
                null,
                null,
                null,
                // D-084 第 4 點：SYSTEM_ADMIN 只取得白名單欄位（permission-matrix §5.1），沒有 createdAt
                actor.hasRole(DomainRole.SYSTEM_ADMIN) ? null : view.getCreatedAt(),
                view.getSubmittedAt(),
                List.of("answers", "comment", "totalScore", "attachmentRefsJson"));
    }
}
