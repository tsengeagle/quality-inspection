package com.tzuchi.his.quality.inspection.domain.sampling;

import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;

import java.util.Objects;

/**
 * Reference to a case selected during sampling.
 * Value object — immutable after creation.
 */
public class SelectedCaseRef {

    private final String sourceCaseKey;
    private final String hospitalId;
    private final MedicalRecordType medicalRecordType;

    public SelectedCaseRef(String sourceCaseKey, String hospitalId, MedicalRecordType medicalRecordType) {
        Objects.requireNonNull(sourceCaseKey, "sourceCaseKey must not be null");
        Objects.requireNonNull(hospitalId, "hospitalId must not be null");
        Objects.requireNonNull(medicalRecordType, "medicalRecordType must not be null");
        this.sourceCaseKey = sourceCaseKey;
        this.hospitalId = hospitalId;
        this.medicalRecordType = medicalRecordType;
    }

    public String getSourceCaseKey() { return sourceCaseKey; }
    public String getHospitalId() { return hospitalId; }
    public MedicalRecordType getMedicalRecordType() { return medicalRecordType; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SelectedCaseRef that = (SelectedCaseRef) o;
        return sourceCaseKey.equals(that.sourceCaseKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceCaseKey);
    }
}
