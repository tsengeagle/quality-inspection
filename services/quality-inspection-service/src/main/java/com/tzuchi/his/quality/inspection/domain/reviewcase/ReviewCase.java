package com.tzuchi.his.quality.inspection.domain.reviewcase;

import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;

import java.util.Objects;

/**
 * Aggregate root for ReviewCase.
 * <p>
 * Created from sampling selection. Starts at SELECTED_PENDING_ASSIGNMENT.
 * Full lifecycle (assignment, review, re-review) to be implemented in later stages.
 */
public class ReviewCase {

    private String caseId;
    private String batchId;
    private String projectId;
    private String hospitalId;
    private String sourceCaseKey;
    private MedicalRecordType medicalRecordType;
    private CaseStatus status;

    protected ReviewCase() {
        // for persistence
    }

    private ReviewCase(String caseId, String batchId, String projectId,
                       String hospitalId, String sourceCaseKey,
                       MedicalRecordType medicalRecordType) {
        Objects.requireNonNull(caseId, "caseId must not be null");
        Objects.requireNonNull(batchId, "batchId must not be null");
        Objects.requireNonNull(projectId, "projectId must not be null");
        this.caseId = caseId;
        this.batchId = batchId;
        this.projectId = projectId;
        this.hospitalId = hospitalId;
        this.sourceCaseKey = sourceCaseKey;
        this.medicalRecordType = medicalRecordType;
        this.status = CaseStatus.SELECTED_PENDING_ASSIGNMENT;
    }

    public static ReviewCase createFromSampling(String caseId, String batchId, String projectId,
                                                 String hospitalId, String sourceCaseKey,
                                                 MedicalRecordType medicalRecordType) {
        return new ReviewCase(caseId, batchId, projectId, hospitalId, sourceCaseKey, medicalRecordType);
    }

    // --- Getters ---

    public String getCaseId() { return caseId; }
    public String getBatchId() { return batchId; }
    public String getProjectId() { return projectId; }
    public String getHospitalId() { return hospitalId; }
    public String getSourceCaseKey() { return sourceCaseKey; }
    public MedicalRecordType getMedicalRecordType() { return medicalRecordType; }
    public CaseStatus getStatus() { return status; }
}
