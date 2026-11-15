package com.tzuchi.his.quality.inspection.application.scoresheet;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.HospitalIdResolver;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetScoreSheetDetailUseCase {

    private final ScoreSheetQueryPort queryPort;
    private final HospitalIdResolver hospitalIdResolver;

    public GetScoreSheetDetailUseCase(ScoreSheetQueryPort queryPort,
                                       HospitalIdResolver hospitalIdResolver) {
        this.queryPort = queryPort;
        this.hospitalIdResolver = hospitalIdResolver;
    }

    public ScoreSheetSummary execute(ActorContext actor, String scoreSheetId) {
        // D-024：角色 → 載入 → 院區
        boolean systemAdmin = actor.hasRole(DomainRole.SYSTEM_ADMIN);
        if (!systemAdmin) {
            actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        }
        ScoreSheetSummary summary = queryPort.findSummaryById(scoreSheetId)
                .orElseThrow(() -> new ResourceNotFoundException("SCORE_SHEET_NOT_FOUND",
                        "ScoreSheet not found: " + scoreSheetId));

        if (systemAdmin) {
            return summary;
        }
        String hospitalId = hospitalIdResolver.resolveByProjectId(summary.getProjectId());
        actor.requireHospitalScope(hospitalId);
        return summary;
    }
}
