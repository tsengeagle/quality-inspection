package com.tzuchi.his.quality.inspection.infrastructure.persistence.reviewcase;

import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;

import javax.persistence.*;

@Entity
@Table(name = "review_case")
public class ReviewCaseEntity {

    @Id
    @Column(name = "case_id")
    private String caseId;

    @Column(name = "batch_id", nullable = false)
    private String batchId;

    @Column(name = "project_id", nullable = false)
    private String projectId;

    @Column(name = "hospital_id")
    private String hospitalId;

    @Column(name = "source_case_key")
    private String sourceCaseKey;

    @Column(name = "medical_record_type")
    private String medicalRecordType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CaseStatus status;

    protected ReviewCaseEntity() {}

    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }

    public String getSourceCaseKey() { return sourceCaseKey; }
    public void setSourceCaseKey(String sourceCaseKey) { this.sourceCaseKey = sourceCaseKey; }

    public String getMedicalRecordType() { return medicalRecordType; }
    public void setMedicalRecordType(String medicalRecordType) { this.medicalRecordType = medicalRecordType; }

    public CaseStatus getStatus() { return status; }
    public void setStatus(CaseStatus status) { this.status = status; }
}
