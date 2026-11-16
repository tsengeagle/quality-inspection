package com.tzuchi.his.quality.inspection.application.scoresheet;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


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
    public ScoreSheetSummaryList execute(ActorContext actor, String hospitalId) {
        // 讀取角色依 D-050 第 7 點
        actor.requireAnyRole(DomainRole.SYSTEM_ADMIN, DomainRole.HOSPITAL_MANAGER, DomainRole.AUDITOR);
        String scope = actor.readScopeHospitalId(hospitalId);
        return new ScoreSheetSummaryList(scope == null ? queryPort.findAll() : queryPort.findByHospitalId(scope));
    }
}
