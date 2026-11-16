package com.tzuchi.his.quality.inspection.infrastructure.persistence.scoresheet;

import com.tzuchi.his.quality.inspection.domain.scoresheet.RequiredRule;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreItem;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetDefinition;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetRepository;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreSheetStatus;
import com.tzuchi.his.quality.inspection.domain.scoresheet.ScoreType;
import com.tzuchi.his.quality.inspection.domain.scoresheet.UpdateDraftCommand;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 載入已儲存的評分表時不做新輸入的欄位驗證（D-050 第 4 點只約束新的輸入）。
 * 舊版允許的資料（例如非 CONDITIONAL 卻有 conditionExpression）仍要能載入、查詢，並透過更新修正。
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ScoreSheetRepositoryImpl.class)
class ScoreSheetLegacyDataTest {

    @Autowired
    private ScoreSheetJpaRepository jpa;

    @Autowired
    private ScoreSheetRepository repository;

    private void saveLegacySheet() {
        ScoreItemEntity item = new ScoreItemEntity();
        item.setItemId("I1");
        item.setCode("C01");
        item.setTitle("病歷完整性");
        item.setScoreType("FIXED");
        item.setMaxScore(BigDecimal.TEN);
        item.setWeight(BigDecimal.ONE);
        item.setRequiredRule("OPTIONAL");
        item.setConditionExpression("age > 65"); // 舊版允許：非 CONDITIONAL 也存了 expression
        ScoreSheetSectionEntity section = new ScoreSheetSectionEntity();
        section.setSectionId("S1");
        section.setTitle("完整性");
        section.setItems(new java.util.ArrayList<>(List.of(item)));
        ScoreSheetEntity sheet = new ScoreSheetEntity();
        sheet.setScoreSheetId("ss-legacy");
        sheet.setProjectId("p1");
        sheet.setVersion(1);
        sheet.setStatus(ScoreSheetStatus.DRAFT);
        sheet.setSections(new java.util.ArrayList<>(List.of(section)));
        jpa.saveAndFlush(sheet);
    }

    @Test
    void legacyItemLoadsAsStored() {
        saveLegacySheet();

        ScoreSheetDefinition loaded = repository.findById("ss-legacy").orElseThrow();

        ScoreItem item = loaded.getSections().get(0).getItems().get(0);
        assertThat(item.getRequiredRule()).isEqualTo(RequiredRule.OPTIONAL);
        assertThat(item.getConditionExpression()).isEqualTo("age > 65");
    }

    @Test
    void legacyItemCanBeCorrectedByAnUpdate() {
        saveLegacySheet();
        ScoreSheetDefinition loaded = repository.findById("ss-legacy").orElseThrow();

        loaded.updateDraft(new UpdateDraftCommand(null, null, null, null,
                List.of(new UpdateDraftCommand.UpdateItemCommand("I1", "C01", "病歷完整性",
                        ScoreType.FIXED, BigDecimal.TEN, BigDecimal.ONE, RequiredRule.OPTIONAL, null)),
                null));
        repository.save(loaded);

        ScoreItem item = repository.findById("ss-legacy").orElseThrow().getSections().get(0).getItems().get(0);
        assertThat(item.getConditionExpression()).isNull();
    }
}
