package com.tzuchi.his.quality.inspection.infrastructure.persistence.sampling;

import javax.persistence.*;

@Entity
@Table(
    name = "sampling_batch_selected_case",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_sampling_batch_selected_case_batch_source",
        columnNames = {"batch_id", "source_case_key"}
    )
)
public class SelectedCaseRefEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_case_key", nullable = false)
    private String sourceCaseKey;

    @Column(name = "hospital_id", nullable = false)
    private String hospitalId;

    @Column(name = "medical_record_type", nullable = false)
    private String medicalRecordType;

    protected SelectedCaseRefEntity() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSourceCaseKey() { return sourceCaseKey; }
    public void setSourceCaseKey(String sourceCaseKey) { this.sourceCaseKey = sourceCaseKey; }

    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }

    public String getMedicalRecordType() { return medicalRecordType; }
    public void setMedicalRecordType(String medicalRecordType) { this.medicalRecordType = medicalRecordType; }
}
