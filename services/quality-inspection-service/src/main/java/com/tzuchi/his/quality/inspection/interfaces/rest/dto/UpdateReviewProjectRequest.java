package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import javax.validation.constraints.NotBlank;
import java.util.Set;

public class UpdateReviewProjectRequest {

    @NotBlank(message = "must not be blank")
    private String name;

    private String description;

    private Set<String> medicalRecordTypes;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Set<String> getMedicalRecordTypes() { return medicalRecordTypes; }
    public void setMedicalRecordTypes(Set<String> medicalRecordTypes) { this.medicalRecordTypes = medicalRecordTypes; }
}