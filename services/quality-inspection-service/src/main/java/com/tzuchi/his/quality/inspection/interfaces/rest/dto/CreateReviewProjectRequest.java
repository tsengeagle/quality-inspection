package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import java.util.Set;

public class CreateReviewProjectRequest {

    private String hospitalId;
    private String name;
    private Set<String> medicalRecordTypes;

    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Set<String> getMedicalRecordTypes() { return medicalRecordTypes; }
    public void setMedicalRecordTypes(Set<String> medicalRecordTypes) { this.medicalRecordTypes = medicalRecordTypes; }
}
