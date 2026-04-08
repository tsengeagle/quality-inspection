package com.tzuchi.his.quality.inspection.application.project.query;

import com.tzuchi.his.quality.inspection.domain.project.ProjectStatus;

public class ReviewProjectSummary {

    private final String projectId;
    private final String hospitalId;
    private final String name;
    private final String description;
    private final ProjectStatus status;
    private final String boundScoreSheetId;
    private final int currentVersionNumber;
    private final boolean currentVersionPublished;

    public ReviewProjectSummary(String projectId, String hospitalId, String name,
                                 String description, ProjectStatus status,
                                 String boundScoreSheetId,
                                 int currentVersionNumber,
                                 boolean currentVersionPublished) {
        this.projectId = projectId;
        this.hospitalId = hospitalId;
        this.name = name;
        this.description = description;
        this.status = status;
        this.boundScoreSheetId = boundScoreSheetId;
        this.currentVersionNumber = currentVersionNumber;
        this.currentVersionPublished = currentVersionPublished;
    }

    public String getProjectId() { return projectId; }
    public String getHospitalId() { return hospitalId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public ProjectStatus getStatus() { return status; }
    public String getBoundScoreSheetId() { return boundScoreSheetId; }
    public int getCurrentVersionNumber() { return currentVersionNumber; }
    public boolean isCurrentVersionPublished() { return currentVersionPublished; }
}