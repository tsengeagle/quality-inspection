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
        ScoreSheetDefinition scoreSheet = repository.findById(scoreSheetId)
                .orElseThrow(() -> new ResourceNotFoundException("SCORE_SHEET_NOT_FOUND",
                        "ScoreSheet not found: " + scoreSheetId));

        if (!actor.hasRole(DomainRole.SYSTEM_ADMIN)) {
            actor.requireRole(DomainRole.HOSPITAL_MANAGER);
            String hospitalId = hospitalIdResolver.resolveByProjectId(scoreSheet.getProjectId());
            actor.requireHospitalScope(hospitalId);
        }
        return new ScoreSheetVersionInfo(scoreSheet.getVersion(),
                scoreSheet.getStatus() == ScoreSheetStatus.PUBLISHED);
    }
}
