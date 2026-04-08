package com.tzuchi.his.quality.inspection.infrastructure.persistence.scoresheet;

import com.tzuchi.his.quality.inspection.application.scoresheet.ScoreSheetQueryPort;
import com.tzuchi.his.quality.inspection.application.scoresheet.ScoreSheetSummary;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetStatus;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.project.ReviewProjectEntity;
import com.tzuchi.his.quality.inspection.infrastructure.persistence.project.ReviewProjectJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class ScoreSheetQueryAdapter implements ScoreSheetQueryPort {

    private final ScoreSheetJpaRepository scoreSheetJpa;
    private final ReviewProjectJpaRepository projectJpa;

    public ScoreSheetQueryAdapter(ScoreSheetJpaRepository scoreSheetJpa,
                                   ReviewProjectJpaRepository projectJpa) {
        this.scoreSheetJpa = scoreSheetJpa;
        this.projectJpa = projectJpa;
    }

    @Override
    public List<ScoreSheetSummary> findByHospitalId(String hospitalId) {
        List<String> projectIds = projectJpa.findByHospitalId(hospitalId).stream()
                .map(ReviewProjectEntity::getProjectId)
                .collect(Collectors.toList());
        if (projectIds.isEmpty()) {
            return List.of();
        }
        return scoreSheetJpa.findByProjectIdIn(projectIds).stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    @Override
    public List<ScoreSheetSummary> findAll() {
        return scoreSheetJpa.findAll().stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ScoreSheetSummary> findSummaryById(String scoreSheetId) {
        return scoreSheetJpa.findById(scoreSheetId).map(this::toSummary);
    }

    private ScoreSheetSummary toSummary(ScoreSheetEntity entity) {
        int sectionCount = entity.getSections().size();
        int itemCount = entity.getSections().stream()
                .mapToInt(s -> s.getItems().size())
                .sum();
        return new ScoreSheetSummary(
                entity.getScoreSheetId(),
                entity.getProjectId(),
                entity.getStatus(),
                entity.getVersion(),
                sectionCount,
                itemCount
        );
    }
}
