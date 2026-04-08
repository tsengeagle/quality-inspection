package com.tzuchi.his.quality.inspection.application.scoresheet;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.application.common.HospitalIdResolver;
import com.tzuchi.his.quality.inspection.domain.common.ResourceNotFoundException;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetDefinition;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetRepository;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetSection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class GetScoreSheetSectionsUseCase {

    private final ScoreSheetRepository repository;
    private final HospitalIdResolver hospitalIdResolver;

    public GetScoreSheetSectionsUseCase(ScoreSheetRepository repository,
                                         HospitalIdResolver hospitalIdResolver) {
        this.repository = repository;
        this.hospitalIdResolver = hospitalIdResolver;
    }

    public List<ScoreSheetSection> execute(ActorContext actor, String scoreSheetId) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        ScoreSheetDefinition scoreSheet = repository.findById(scoreSheetId)
                .orElseThrow(() -> new ResourceNotFoundException("SCORE_SHEET_NOT_FOUND",
                        "ScoreSheet not found: " + scoreSheetId));
        String hospitalId = hospitalIdResolver.resolveByProjectId(scoreSheet.getProjectId());
        actor.requireHospitalScope(hospitalId);
        return scoreSheet.getSections();
    }
}
