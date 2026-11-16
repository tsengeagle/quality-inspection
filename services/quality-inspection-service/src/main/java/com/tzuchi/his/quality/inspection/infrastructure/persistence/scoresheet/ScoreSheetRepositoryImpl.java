package com.tzuchi.his.quality.inspection.infrastructure.persistence.scoresheet;

import com.tzuchi.his.quality.inspection.domain.scoresheet.*;
import org.springframework.stereotype.Repository;

import java.util.List;
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
        ie.setRequiredRule(item.getRequiredRule().name());
        ie.setConditionExpression(item.getConditionExpression());
        return ie;
    }

    private ScoreSheetDefinition toDomain(ScoreSheetEntity e) {
        List<ScoreSheetSection> sections = e.getSections().stream().map(se -> {
            // 還原已儲存的資料，不套用新輸入的驗證（舊資料要能載入並透過更新修正）
            List<ScoreItem> items = se.getItems().stream().map(ie -> ScoreItem.reconstitute(
                    ie.getItemId(), ie.getCode(), ie.getTitle(),
                    ScoreType.valueOf(ie.getScoreType()),
                    ie.getMaxScore(), ie.getWeight(),
                    RequiredRule.valueOf(ie.getRequiredRule()),
                    ie.getConditionExpression())).collect(Collectors.toList());
            return ScoreSheetSection.reconstitute(se.getSectionId(), se.getTitle(), items);
        }).collect(Collectors.toList());

        return ScoreSheetDefinition.reconstitute(
                e.getScoreSheetId(),
                e.getProjectId(),
                e.getStatus(),
                e.getVersion(),
                sections
        );
    }
}
