package com.tzuchi.his.quality.inspection.infrastructure.persistence.scoresheet;

import com.tzuchi.his.quality.inspection.domain.scoresheet.*;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class ScoreSheetRepositoryImpl implements ScoreSheetRepository {

    private final ScoreSheetJpaRepository jpa;

    public ScoreSheetRepositoryImpl(ScoreSheetJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(ScoreSheetDefinition scoreSheet) {
        ScoreSheetEntity entity = toEntity(scoreSheet);
        jpa.save(entity);
    }

    @Override
    public Optional<ScoreSheetDefinition> findById(String scoreSheetId) {
        return jpa.findById(scoreSheetId).map(this::toDomain);
    }

    private ScoreSheetEntity toEntity(ScoreSheetDefinition ss) {
        ScoreSheetEntity e = new ScoreSheetEntity();
        e.setScoreSheetId(ss.getScoreSheetId());
        e.setProjectId(ss.getProjectId());
        e.setVersion(ss.getVersion());
        e.setStatus(ss.getStatus());
        e.setSections(
                ss.getSections().stream().map(this::toSectionEntity).collect(Collectors.toList())
        );
        return e;
    }

    private ScoreSheetSectionEntity toSectionEntity(ScoreSheetSection section) {
        ScoreSheetSectionEntity se = new ScoreSheetSectionEntity();
        se.setSectionId(section.getSectionId());
        se.setTitle(section.getTitle());
        se.setItems(
                section.getItems().stream().map(this::toItemEntity).collect(Collectors.toList())
        );
        return se;
    }

    private ScoreItemEntity toItemEntity(ScoreItem item) {
        ScoreItemEntity ie = new ScoreItemEntity();
        ie.setItemId(item.getItemId());
        ie.setCode(item.getCode());
        ie.setTitle(item.getTitle());
        ie.setScoreType(item.getScoreType().name());
        ie.setMaxScore(item.getMaxScore());
        ie.setWeight(item.getWeight());
        return ie;
    }

    private ScoreSheetDefinition toDomain(ScoreSheetEntity e) {
        ScoreSheetDefinition ss = ScoreSheetDefinition.createDraft(e.getScoreSheetId(), e.getProjectId());
        for (ScoreSheetSectionEntity se : e.getSections()) {
            ScoreSheetSection section = new ScoreSheetSection(se.getSectionId(), se.getTitle());
            for (ScoreItemEntity ie : se.getItems()) {
                section.addItem(new ScoreItem(
                        ie.getItemId(), ie.getCode(), ie.getTitle(),
                        ScoreType.valueOf(ie.getScoreType()),
                        ie.getMaxScore(), ie.getWeight()
                ));
            }
            ss.addSection(section);
        }
        if (e.getStatus() == ScoreSheetStatus.PUBLISHED) {
            ss.publishVersion();
        }
        return ss;
    }
}
