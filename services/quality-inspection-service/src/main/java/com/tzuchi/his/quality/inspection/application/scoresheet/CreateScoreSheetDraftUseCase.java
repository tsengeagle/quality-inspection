package com.tzuchi.his.quality.inspection.application.scoresheet;

import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetDefinition;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class CreateScoreSheetDraftUseCase {

    private final ScoreSheetRepository repository;

    public CreateScoreSheetDraftUseCase(ScoreSheetRepository repository) {
        this.repository = repository;
    }

    public String execute(String projectId) {
        String scoreSheetId = UUID.randomUUID().toString();
        ScoreSheetDefinition scoreSheet = ScoreSheetDefinition.createDraft(scoreSheetId, projectId);
        repository.save(scoreSheet);
        return scoreSheetId;
    }
}
