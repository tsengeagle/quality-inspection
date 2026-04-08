package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

public class ReviewProjectResponse {

    private String projectId;
    private String hospitalId;
    private String name;
    private String description;
    private String status;
    private String boundScoreSheetId;
    private int currentVersionNumber;
    private boolean currentVersionPublished;

    public ReviewProjectResponse() {}

    public ReviewProjectResponse(String projectId, String hospitalId, String name,
                                  String description, String status,
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
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getBoundScoreSheetId() { return boundScoreSheetId; }
    public void setBoundScoreSheetId(String boundScoreSheetId) { this.boundScoreSheetId = boundScoreSheetId; }

    public int getCurrentVersionNumber() { return currentVersionNumber; }
    public void setCurrentVersionNumber(int currentVersionNumber) { this.currentVersionNumber = currentVersionNumber; }

    public boolean isCurrentVersionPublished() { return currentVersionPublished; }
    public void setCurrentVersionPublished(boolean currentVersionPublished) { this.currentVersionPublished = currentVersionPublished; }
}
