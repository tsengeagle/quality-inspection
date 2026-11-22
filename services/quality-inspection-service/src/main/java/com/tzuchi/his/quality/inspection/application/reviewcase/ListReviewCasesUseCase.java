package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.Page;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.ReviewCaseQueryCriteria;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.ReviewCaseQueryPort;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.ReviewCaseSummary;
import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ListReviewCasesUseCase {

    private final ReviewCaseQueryPort queryPort;

    public ListReviewCasesUseCase(ReviewCaseQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    /**
     * 輸入依規格的名稱攤平（D-066 第 4 點）。沒給的條件是 null；page、size 沒給時是第 0 頁、每頁 20，size 上限 100。
     */
    public Page<ReviewCaseSummary> execute(ActorContext actor, String hospitalId, String projectId,
                                           String batchId, CaseStatus status, String reviewerId,
                                           String rereviewerId, String medicalRecordType,
                                           Integer page, Integer size) {
        actor.requireAnyRole(
                DomainRole.HOSPITAL_MANAGER, DomainRole.REVIEWER,
                DomainRole.REREVIEWER, DomainRole.AUDITOR, DomainRole.SYSTEM_ADMIN);

        ReviewCaseQueryCriteria criteria = new ReviewCaseQueryCriteria(
                actor.readScopeHospitalId(hospitalId), projectId, batchId, status,
                reviewerId, rereviewerId, medicalRecordType,
                page == null ? 0 : page, size == null ? 0 : size);
        Page<ReviewCaseSummary> raw = queryPort.findCases(criteria);
        String projectionType = actor.hasRole(DomainRole.SYSTEM_ADMIN) ? "METADATA_ONLY" : "FULL";
        List<ReviewCaseSummary> enriched = raw.getContent().stream()
                .map(s -> s.withProjectionType(projectionType))
                .collect(Collectors.toList());
        return new Page<>(enriched, raw.getPage(), raw.getSize(), raw.getTotalElements());
    }
}
