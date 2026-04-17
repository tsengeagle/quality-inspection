package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

public class SamplingExecuteContextResponse {

    private String projectId;
    private String projectName;
    private String hospitalId;
    private String ruleSummary;
    private boolean previewReady;
    private boolean executing;
    private boolean executeFailed;
    private String executedBatchId;
    private String blockedReason;
    private PreviewStatsDto previewStats;
    private PostExecuteSummaryDto postExecuteSummary;

    public SamplingExecuteContextResponse() {}

    public SamplingExecuteContextResponse(String projectId, String projectName, String hospitalId,
                                          String ruleSummary, boolean previewReady, boolean executing,
                                          boolean executeFailed, String executedBatchId,
                                          String blockedReason, PreviewStatsDto previewStats,
                                          PostExecuteSummaryDto postExecuteSummary) {
        this.projectId = projectId;
        this.projectName = projectName;
        this.hospitalId = hospitalId;
        this.ruleSummary = ruleSummary;
        this.previewReady = previewReady;
        this.executing = executing;
        this.executeFailed = executeFailed;
        this.executedBatchId = executedBatchId;
        this.blockedReason = blockedReason;
        this.previewStats = previewStats;
        this.postExecuteSummary = postExecuteSummary;
    }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }
    public String getRuleSummary() { return ruleSummary; }
    public void setRuleSummary(String ruleSummary) { this.ruleSummary = ruleSummary; }
    public boolean isPreviewReady() { return previewReady; }
    public void setPreviewReady(boolean previewReady) { this.previewReady = previewReady; }
    public boolean isExecuting() { return executing; }
    public void setExecuting(boolean executing) { this.executing = executing; }
    public boolean isExecuteFailed() { return executeFailed; }
    public void setExecuteFailed(boolean executeFailed) { this.executeFailed = executeFailed; }
    public String getExecutedBatchId() { return executedBatchId; }
    public void setExecutedBatchId(String executedBatchId) { this.executedBatchId = executedBatchId; }
    public String getBlockedReason() { return blockedReason; }
    public void setBlockedReason(String blockedReason) { this.blockedReason = blockedReason; }
    public PreviewStatsDto getPreviewStats() { return previewStats; }
    public void setPreviewStats(PreviewStatsDto previewStats) { this.previewStats = previewStats; }
    public PostExecuteSummaryDto getPostExecuteSummary() { return postExecuteSummary; }
    public void setPostExecuteSummary(PostExecuteSummaryDto postExecuteSummary) { this.postExecuteSummary = postExecuteSummary; }

    public static class PreviewStatsDto {
        private int populationCount;
        private int eligibleCount;
        private int excludedCount;

        public PreviewStatsDto() {}

        public PreviewStatsDto(int populationCount, int eligibleCount, int excludedCount) {
            this.populationCount = populationCount;
            this.eligibleCount = eligibleCount;
            this.excludedCount = excludedCount;
        }

        public int getPopulationCount() { return populationCount; }
        public void setPopulationCount(int populationCount) { this.populationCount = populationCount; }
        public int getEligibleCount() { return eligibleCount; }
        public void setEligibleCount(int eligibleCount) { this.eligibleCount = eligibleCount; }
        public int getExcludedCount() { return excludedCount; }
        public void setExcludedCount(int excludedCount) { this.excludedCount = excludedCount; }
    }

    public static class PostExecuteSummaryDto {
        private String tone;
        private String title;
        private String description;
        private String batchId;

        public PostExecuteSummaryDto() {}

        public PostExecuteSummaryDto(String tone, String title, String description, String batchId) {
            this.tone = tone;
            this.title = title;
            this.description = description;
            this.batchId = batchId;
        }

        public String getTone() { return tone; }
        public void setTone(String tone) { this.tone = tone; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getBatchId() { return batchId; }
        public void setBatchId(String batchId) { this.batchId = batchId; }
    }
}
