package com.tzuchi.his.quality.inspection.application.sampling;

/**
 * Read model for a selected case reference in batch detail view.
 */
public class SelectedCaseRefView {

    private final String sourceCaseKey;
    private final String hospitalId;
    private final String medicalRecordType;

    public SelectedCaseRefView(String sourceCaseKey, String hospitalId, String medicalRecordType) {
        this.sourceCaseKey = sourceCaseKey;
        this.hospitalId = hospitalId;
        this.medicalRecordType = medicalRecordType;
    }

    public String getSourceCaseKey() { return sourceCaseKey; }
    public String getHospitalId() { return hospitalId; }
    public String getMedicalRecordType() { return medicalRecordType; }
}
