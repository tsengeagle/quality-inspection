package com.tzuchi.his.quality.inspection.application.project;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.project.query.ReviewProjectQueryPort;
import com.tzuchi.his.quality.inspection.application.project.query.ReviewProjectSummary;
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
    public List<ReviewProjectSummary> execute(ActorContext actor, String hospitalId) {
        // api-spec.md §7.7。REVIEWER／REREVIEWER 的唯讀範圍未定義（Q-12），在定義前不開放
        actor.requireAnyRole(DomainRole.SYSTEM_ADMIN, DomainRole.HOSPITAL_MANAGER, DomainRole.AUDITOR);
        String scope = actor.readScopeHospitalId(hospitalId);
        return scope == null ? queryPort.findAll() : queryPort.findByHospitalId(scope);
    }
}