package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import java.math.BigDecimal;
import java.util.List;

public class UpdateScoreSheetDraftRequest {

    private List<AddSectionRequest> addSections;
    private List<UpdateSectionRequest> updateSections;
    private List<String> removeSectionIds;
    private List<AddItemRequest> addItems;
    private List<UpdateItemRequest> updateItems;
    private List<String> removeItemIds;

    // getters/setters
    public List<AddSectionRequest> getAddSections() { return addSections; }
    public void setAddSections(List<AddSectionRequest> addSections) { this.addSections = addSections; }
    public List<UpdateSectionRequest> getUpdateSections() { return updateSections; }
    public void setUpdateSections(List<UpdateSectionRequest> updateSections) { this.updateSections = updateSections; }
    public List<String> getRemoveSectionIds() { return removeSectionIds; }
    public void setRemoveSectionIds(List<String> removeSectionIds) { this.removeSectionIds = removeSectionIds; }
    public List<AddItemRequest> getAddItems() { return addItems; }
    public void setAddItems(List<AddItemRequest> addItems) { this.addItems = addItems; }
    public List<UpdateItemRequest> getUpdateItems() { return updateItems; }
    public void setUpdateItems(List<UpdateItemRequest> updateItems) { this.updateItems = updateItems; }
    public List<String> getRemoveItemIds() { return removeItemIds; }
    public void setRemoveItemIds(List<String> removeItemIds) { this.removeItemIds = removeItemIds; }

    public static class AddSectionRequest {
        private String sectionId;
        private String title;
        private List<AddItemRequest> items;
        public String getSectionId() { return sectionId; }
        public void setSectionId(String sectionId) { this.sectionId = sectionId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public List<AddItemRequest> getItems() { return items; }
        public void setItems(List<AddItemRequest> items) { this.items = items; }
    }

    public static class UpdateSectionRequest {
        private String sectionId;
        private String title;
        public String getSectionId() { return sectionId; }
        public void setSectionId(String sectionId) { this.sectionId = sectionId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
    }

    public static class AddItemRequest {
        private String targetSectionId;
        private String itemId;
        private String code;
        private String title;
        private String scoreType;
        private BigDecimal maxScore;
        private BigDecimal weight;
        private String requiredRule;
        private String conditionExpression;
        // getters/setters
        public String getTargetSectionId() { return targetSectionId; }
        public void setTargetSectionId(String targetSectionId) { this.targetSectionId = targetSectionId; }
        public String getItemId() { return itemId; }
        public void setItemId(String itemId) { this.itemId = itemId; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getScoreType() { return scoreType; }
        public void setScoreType(String scoreType) { this.scoreType = scoreType; }
        public BigDecimal getMaxScore() { return maxScore; }
        public void setMaxScore(BigDecimal maxScore) { this.maxScore = maxScore; }
        public BigDecimal getWeight() { return weight; }
        public void setWeight(BigDecimal weight) { this.weight = weight; }
        public String getRequiredRule() { return requiredRule; }
        public void setRequiredRule(String requiredRule) { this.requiredRule = requiredRule; }
        public String getConditionExpression() { return conditionExpression; }
        public void setConditionExpression(String conditionExpression) { this.conditionExpression = conditionExpression; }
    }

    public static class UpdateItemRequest {
        private String itemId;
        private String code;
        private String title;
        private String scoreType;
        private BigDecimal maxScore;
        private BigDecimal weight;
        private String requiredRule;
        private String conditionExpression;
        // getters/setters
        public String getItemId() { return itemId; }
        public void setItemId(String itemId) { this.itemId = itemId; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getScoreType() { return scoreType; }
        public void setScoreType(String scoreType) { this.scoreType = scoreType; }
        public BigDecimal getMaxScore() { return maxScore; }
        public void setMaxScore(BigDecimal maxScore) { this.maxScore = maxScore; }
        public BigDecimal getWeight() { return weight; }
        public void setWeight(BigDecimal weight) { this.weight = weight; }
        public String getRequiredRule() { return requiredRule; }
        public void setRequiredRule(String requiredRule) { this.requiredRule = requiredRule; }
        public String getConditionExpression() { return conditionExpression; }
        public void setConditionExpression(String conditionExpression) { this.conditionExpression = conditionExpression; }
    }
}
