package com.tzuchi.his.quality.inspection.application.scoresheet;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ListScoreSheetSummariesUseCase {

    private final ScoreSheetQueryPort queryPort;

    public ListScoreSheetSummariesUseCase(ScoreSheetQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    /**
     * @param hospitalId 只有 SYSTEM_ADMIN 會採用，用來縮小範圍；不給就是全部（D-047 第 1 點）。
     *                   其他角色一律是自己的院區，這個參數不採用（D-042、D-047 第 2 點）。
     */
    public List<ScoreSheetSummary> execute(ActorContext actor, String hospitalId) {
        if (!actor.hasRole(DomainRole.SYSTEM_ADMIN)) {
            actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        }
        String scope = actor.readScopeHospitalId(hospitalId);
        return scope == null ? queryPort.findAll() : queryPort.findByHospitalId(scope);
    }
}
