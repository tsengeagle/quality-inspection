package com.tzuchi.his.quality.inspection.application.project;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.project.query.ReviewProjectQueryPort;
import com.tzuchi.his.quality.inspection.application.project.query.ReviewProjectSummary;
import com.tzuchi.his.quality.inspection.application.project.query.ReviewProjectSummaryList;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ListReviewProjectsUseCase {

    private final ReviewProjectQueryPort queryPort;

    public ListReviewProjectsUseCase(ReviewProjectQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    /**
     * @param hospitalId 只有 SYSTEM_ADMIN 會採用，用來縮小範圍；不給就是全部（D-047 第 1 點）。
     *                   其他角色一律是自己的院區，這個參數不採用（D-042、D-047 第 2 點）。
     */
    public ReviewProjectSummaryList execute(ActorContext actor, String hospitalId) {
        // api-spec.md §7.7。REVIEWER／REREVIEWER 不能讀取專案（D-051）
        actor.requireAnyRole(DomainRole.SYSTEM_ADMIN, DomainRole.HOSPITAL_MANAGER, DomainRole.AUDITOR);
        String scope = actor.readScopeHospitalId(hospitalId);
        List<ReviewProjectSummary> items = scope == null ? queryPort.findAll() : queryPort.findByHospitalId(scope);
        return new ReviewProjectSummaryList(items);   // D-062 第 1 點：{ items }
    }
}