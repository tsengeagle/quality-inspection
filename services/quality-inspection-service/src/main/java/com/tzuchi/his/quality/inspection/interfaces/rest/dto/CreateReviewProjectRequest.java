package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Set;

public class CreateReviewProjectRequest {

    @NotNull(message = "must not be null")
    @NotBlank(message = "must not be blank")
    private String hospitalId;

    @NotNull(message = "must not be null")
    @NotBlank(message = "must not be blank")
    private String name;

    @NotNull(message = "must not be null")
    private Set<String> medicalRecordTypes;

    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Set<String> getMedicalRecordTypes() { return medicalRecordTypes; }
    public void setMedicalRecordTypes(Set<String> medicalRecordTypes) { this.medicalRecordTypes = medicalRecordTypes; }
}
