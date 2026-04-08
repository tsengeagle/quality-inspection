package com.tzuchi.his.quality.inspection.domain.scoresheet;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

public class UpdateDraftCommand {

    private final List<AddSectionCommand> addSections;
    private final List<UpdateSectionCommand> updateSections;
    private final List<String> removeSectionIds;
    private final List<AddItemCommand> addItems;
    private final List<UpdateItemCommand> updateItems;
    private final List<String> removeItemIds;

    public UpdateDraftCommand(List<AddSectionCommand> addSections,
                              List<UpdateSectionCommand> updateSections,
                              List<String> removeSectionIds,
                              List<AddItemCommand> addItems,
                              List<UpdateItemCommand> updateItems,
                              List<String> removeItemIds) {
        this.addSections = addSections != null ? addSections : Collections.emptyList();
        this.updateSections = updateSections != null ? updateSections : Collections.emptyList();
        this.removeSectionIds = removeSectionIds != null ? removeSectionIds : Collections.emptyList();
        this.addItems = addItems != null ? addItems : Collections.emptyList();
        this.updateItems = updateItems != null ? updateItems : Collections.emptyList();
        this.removeItemIds = removeItemIds != null ? removeItemIds : Collections.emptyList();
    }

    public List<AddSectionCommand> getAddSections() { return addSections; }
    public List<UpdateSectionCommand> getUpdateSections() { return updateSections; }
    public List<String> getRemoveSectionIds() { return removeSectionIds; }
    public List<AddItemCommand> getAddItems() { return addItems; }
    public List<UpdateItemCommand> getUpdateItems() { return updateItems; }
    public List<String> getRemoveItemIds() { return removeItemIds; }

    // --- Inner command classes ---

    public static class AddSectionCommand {
        private final String sectionId;
        private final String title;
        private final List<AddItemCommand> items;

        public AddSectionCommand(String sectionId, String title, List<AddItemCommand> items) {
            this.sectionId = sectionId;
            this.title = title;
            this.items = items != null ? items : Collections.emptyList();
        }

        public String getSectionId() { return sectionId; }
        public String getTitle() { return title; }
        public List<AddItemCommand> getItems() { return items; }
    }

    public static class UpdateSectionCommand {
        private final String sectionId;
        private final String title;

        public UpdateSectionCommand(String sectionId, String title) {
            this.sectionId = sectionId;
            this.title = title;
        }

        public String getSectionId() { return sectionId; }
        public String getTitle() { return title; }
    }

    public static class AddItemCommand {
        private final String targetSectionId;
        private final String itemId;
        private final String code;
        private final String title;
        private final ScoreType scoreType;
        private final BigDecimal maxScore;
        private final BigDecimal weight;
        private final RequiredRule requiredRule;
        private final String conditionExpression;

        public AddItemCommand(String targetSectionId, String itemId, String code, String title,
                              ScoreType scoreType, BigDecimal maxScore, BigDecimal weight,
                              RequiredRule requiredRule, String conditionExpression) {
            this.targetSectionId = targetSectionId;
            this.itemId = itemId;
            this.code = code;
            this.title = title;
            this.scoreType = scoreType;
            this.maxScore = maxScore;
            this.weight = weight;
            this.requiredRule = requiredRule;
            this.conditionExpression = conditionExpression;
        }

        public String getTargetSectionId() { return targetSectionId; }
        public String getItemId() { return itemId; }
        public String getCode() { return code; }
        public String getTitle() { return title; }
        public ScoreType getScoreType() { return scoreType; }
        public BigDecimal getMaxScore() { return maxScore; }
        public BigDecimal getWeight() { return weight; }
        public RequiredRule getRequiredRule() { return requiredRule; }
        public String getConditionExpression() { return conditionExpression; }
    }

    public static class UpdateItemCommand {
        private final String itemId;
        private final String code;
        private final String title;
        private final ScoreType scoreType;
        private final BigDecimal maxScore;
        private final BigDecimal weight;
        private final RequiredRule requiredRule;
        private final String conditionExpression;

        public UpdateItemCommand(String itemId, String code, String title,
                                 ScoreType scoreType, BigDecimal maxScore, BigDecimal weight,
                                 RequiredRule requiredRule, String conditionExpression) {
            this.itemId = itemId;
            this.code = code;
            this.title = title;
            this.scoreType = scoreType;
            this.maxScore = maxScore;
            this.weight = weight;
            this.requiredRule = requiredRule;
            this.conditionExpression = conditionExpression;
        }

        public String getItemId() { return itemId; }
        public String getCode() { return code; }
        public String getTitle() { return title; }
        public ScoreType getScoreType() { return scoreType; }
        public BigDecimal getMaxScore() { return maxScore; }
        public BigDecimal getWeight() { return weight; }
        public RequiredRule getRequiredRule() { return requiredRule; }
        public String getConditionExpression() { return conditionExpression; }
    }
}
