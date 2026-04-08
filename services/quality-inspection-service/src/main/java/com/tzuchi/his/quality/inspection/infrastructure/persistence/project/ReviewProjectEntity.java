package com.tzuchi.his.quality.inspection.infrastructure.persistence.project;

import com.tzuchi.his.quality.inspection.domain.project.ProjectStatus;

import javax.persistence.*;
import java.util.Set;

@Entity
@Table(name = "review_project")
public class ReviewProjectEntity {

    @Id
    @Column(name = "project_id")
    private String projectId;

    @Column(name = "hospital_id", nullable = false)
    private String hospitalId;

    @Column(nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "bound_score_sheet_id")
    private String boundScoreSheetId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "review_project_record_types",
            joinColumns = @JoinColumn(name = "project_id"))
    @Column(name = "record_type")
    private Set<String> medicalRecordTypes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectStatus status;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(name = "version_published", nullable = false)
    private boolean versionPublished;

    public ReviewProjectEntity() {}

    // --- Getters and Setters ---

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getBoundScoreSheetId() { return boundScoreSheetId; }
    public void setBoundScoreSheetId(String boundScoreSheetId) { this.boundScoreSheetId = boundScoreSheetId; }

    public Set<String> getMedicalRecordTypes(){ return medicalRecordTypes; }
    public void setMedicalRecordTypes(Set<String> medicalRecordTypes) { this.medicalRecordTypes = medicalRecordTypes; }

    public ProjectStatus getStatus() { return status; }
    public void setStatus(ProjectStatus status) { this.status = status; }

    public int getVersionNumber() { return versionNumber; }
    public void setVersionNumber(int versionNumber) { this.versionNumber = versionNumber; }

    public boolean isVersionPublished() { return versionPublished; }
    public void setVersionPublished(boolean versionPublished) { this.versionPublished = versionPublished; }
}
