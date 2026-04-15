package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.Page;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.ReviewCaseQueryCriteria;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.ReviewCaseQueryPort;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.ReviewCaseSummary;
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

    public Page<ReviewCaseSummary> execute(ActorContext actor, ReviewCaseQueryCriteria criteria) {
        actor.requireAnyRole(
                DomainRole.HOSPITAL_MANAGER, DomainRole.REVIEWER,
                DomainRole.REREVIEWER, DomainRole.AUDITOR, DomainRole.SYSTEM_ADMIN);

        Page<ReviewCaseSummary> raw = queryPort.findCases(criteria);
        String projectionType = actor.hasRole(DomainRole.SYSTEM_ADMIN) ? "METADATA_ONLY" : "FULL";
        List<ReviewCaseSummary> enriched = raw.getContent().stream()
                .map(s -> s.withProjectionType(projectionType))
                .collect(Collectors.toList());
        return new Page<>(enriched, raw.getPage(), raw.getSize(), raw.getTotalElements());
    }
}
