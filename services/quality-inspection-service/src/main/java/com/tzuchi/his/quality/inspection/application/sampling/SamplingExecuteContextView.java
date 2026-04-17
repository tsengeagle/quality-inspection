package com.tzuchi.his.quality.inspection.application.sampling;

/**
 * Bootstrap context for UI-006 sampling execute page.
 * Preview stats stay null on initial bootstrap because preview is not persisted server-side.
 */
public class SamplingExecuteContextView {

    private final String projectId;
    private final String projectName;
    private final String hospitalId;
    private final String ruleSummary;
    private final boolean previewReady;
    private final boolean executing;
    private final boolean executeFailed;
    private final String executedBatchId;
    private final String blockedReason;
    private final SamplingPreviewResult previewStats;
    private final PostExecuteSummary postExecuteSummary;

    public SamplingExecuteContextView(String projectId, String projectName, String hospitalId,
                                      String ruleSummary, boolean previewReady, boolean executing,
                                      boolean executeFailed, String executedBatchId, String blockedReason,
                                      SamplingPreviewResult previewStats,
                                      PostExecuteSummary postExecuteSummary) {
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
    public String getProjectName() { return projectName; }
    public String getHospitalId() { return hospitalId; }
    public String getRuleSummary() { return ruleSummary; }
    public boolean isPreviewReady() { return previewReady; }
    public boolean isExecuting() { return executing; }
    public boolean isExecuteFailed() { return executeFailed; }
    public String getExecutedBatchId() { return executedBatchId; }
    public String getBlockedReason() { return blockedReason; }
    public SamplingPreviewResult getPreviewStats() { return previewStats; }
    public PostExecuteSummary getPostExecuteSummary() { return postExecuteSummary; }

    public static class PostExecuteSummary {
        private final String tone;
        private final String title;
        private final String description;
        private final String batchId;

        public PostExecuteSummary(String tone, String title, String description, String batchId) {
            this.tone = tone;
            this.title = title;
            this.description = description;
            this.batchId = batchId;
        }

        public String getTone() { return tone; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getBatchId() { return batchId; }
    }
}
