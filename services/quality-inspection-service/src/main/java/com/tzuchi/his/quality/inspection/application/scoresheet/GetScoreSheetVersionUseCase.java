package com.tzuchi.his.quality.inspection.application.scoresheet;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.HospitalIdResolver;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetDefinition;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetRepository;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetScoreSheetVersionUseCase {

    private final ScoreSheetRepository repository;
    private final HospitalIdResolver hospitalIdResolver;

    public GetScoreSheetVersionUseCase(ScoreSheetRepository repository,
                                        HospitalIdResolver hospitalIdResolver) {
        this.repository = repository;
        this.hospitalIdResolver = hospitalIdResolver;
    }

    public ScoreSheetVersionInfo execute(ActorContext actor, String scoreSheetId) {
        // D-024：角色 → 載入 → 院區。讀取角色依 D-050 第 7 點；SYSTEM_ADMIN 不受院區限制（D-047）
        actor.requireAnyRole(DomainRole.SYSTEM_ADMIN, DomainRole.HOSPITAL_MANAGER, DomainRole.AUDITOR);
        ScoreSheetDefinition scoreSheet = repository.findById(scoreSheetId)
                .orElseThrow(() -> new ResourceNotFoundException("SCORE_SHEET_NOT_FOUND",
                        "ScoreSheet not found: " + scoreSheetId));

        if (!actor.hasRole(DomainRole.SYSTEM_ADMIN)) {
            actor.requireReadScope(hospitalIdResolver.resolveByProjectId(scoreSheet.getProjectId()));
        }
        return new ScoreSheetVersionInfo(scoreSheet.getVersion(),
                scoreSheet.getStatus() == ScoreSheetStatus.PUBLISHED);
    }
}
