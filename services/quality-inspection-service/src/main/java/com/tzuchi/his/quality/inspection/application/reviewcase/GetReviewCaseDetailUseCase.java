package com.tzuchi.his.quality.inspection.application.reviewcase;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.ReviewCaseDetailView;
import com.tzuchi.his.quality.inspection.application.reviewcase.query.ReviewCaseQueryPort;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetReviewCaseDetailUseCase {

    private final ReviewCaseQueryPort queryPort;

    public GetReviewCaseDetailUseCase(ReviewCaseQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    public ReviewCaseDetailView execute(ActorContext actor, String caseId) {
        actor.requireAnyRole(
                DomainRole.HOSPITAL_MANAGER, DomainRole.REVIEWER,
                DomainRole.REREVIEWER, DomainRole.AUDITOR, DomainRole.SYSTEM_ADMIN);
        return queryPort.findDetailById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("ReviewCase", caseId));
    }
}
