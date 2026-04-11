package com.tzuchi.his.quality.inspection.application.reviewcase.query;

import com.tzuchi.his.quality.inspection.domain.reviewcase.CaseStatus;

public class ReviewCaseQueryCriteria {

    private final String hospitalId;
    private final String projectId;
    private final String batchId;
    private final CaseStatus status;
    private final String reviewerId;
    private final String rereviewerId;
    private final String medicalRecordType;
    private final int page;
    private final int size;

    public ReviewCaseQueryCriteria(String hospitalId, String projectId, String batchId,
                                    CaseStatus status, String reviewerId, String rereviewerId,
                                    String medicalRecordType, int page, int size) {
        this.hospitalId = hospitalId;
        this.projectId = projectId;
        this.batchId = batchId;
        this.status = status;
        this.reviewerId = reviewerId;
        this.rereviewerId = rereviewerId;
        this.medicalRecordType = medicalRecordType;
        this.page = Math.max(0, page);
        this.size = size > 0 ? Math.min(size, 100) : 20;
    }

    public String getHospitalId() { return hospitalId; }
    public String getProjectId() { return projectId; }
    public String getBatchId() { return batchId; }
    public CaseStatus getStatus() { return status; }
    public String getReviewerId() { return reviewerId; }
    public String getRereviewerId() { return rereviewerId; }
    public String getMedicalRecordType() { return medicalRecordType; }
    public int getPage() { return page; }
    public int getSize() { return size; }
}
