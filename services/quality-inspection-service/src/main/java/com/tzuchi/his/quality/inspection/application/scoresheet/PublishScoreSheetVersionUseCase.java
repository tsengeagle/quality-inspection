package com.tzuchi.his.quality.inspection.application.scoresheet;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.DomainRole;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetDefinition;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PublishScoreSheetVersionUseCase {

    private final ScoreSheetRepository repository;

    public PublishScoreSheetVersionUseCase(ScoreSheetRepository repository) {
        this.repository = repository;
    }

    public void execute(ActorContext actor, String scoreSheetId) {
        actor.requireRole(DomainRole.HOSPITAL_MANAGER);
        ScoreSheetDefinition scoreSheet = repository.findById(scoreSheetId)
                .orElseThrow(() -> new IllegalArgumentException("ScoreSheet not found: " + scoreSheetId));
        scoreSheet.publishVersion();
        repository.save(scoreSheet);
    }
}
