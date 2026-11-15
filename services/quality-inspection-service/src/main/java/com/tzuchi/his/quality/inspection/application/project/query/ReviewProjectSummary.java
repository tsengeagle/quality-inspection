package com.tzuchi.his.quality.inspection.application.project.query;

import com.tzuchi.his.quality.inspection.domain.common.MedicalRecordType;
import com.tzuchi.his.quality.inspection.domain.project.ProjectStatus;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Read view of a review project and its current version.
 * <p>
 * Field names follow the GetReviewProjectDetail output in specs/review-project (D-043):
 * the acceptance runner compares properties by name, so the view uses the spec's vocabulary.
 */
public class ReviewProjectSummary {

    private final String projectId;
    private final String hospitalId;
    private final String name;
    private final String description;
    private final Set<MedicalRecordType> medicalRecordTypes;
    private final ProjectStatus status;
    private final String boundScoreSheetId;
    private final int versionNo;
    private final boolean versionPublished;
    private final BigDecimal qualityThreshold;
    private final BigDecimal initialReviewWeight;
    private final BigDecimal rereviewWeight;

    public ReviewProjectSummary(String projectId, String hospitalId, String name,
                                 String description, Set<MedicalRecordType> medicalRecordTypes,
                                 ProjectStatus status, String boundScoreSheetId,
                                 int versionNo, boolean versionPublished,
                                 BigDecimal qualityThreshold, BigDecimal initialReviewWeight,
                                 BigDecimal rereviewWeight) {
        this.projectId = projectId;
        this.hospitalId = hospitalId;
        this.name = name;
        this.description = description;
        this.medicalRecordTypes = (medicalRecordTypes == null || medicalRecordTypes.isEmpty())
                ? Collections.unmodifiableSet(EnumSet.noneOf(MedicalRecordType.class))
                : Collections.unmodifiableSet(EnumSet.copyOf(medicalRecordTypes));
        this.status = status;
        this.boundScoreSheetId = boundScoreSheetId;
        this.versionNo = versionNo;
        this.versionPublished = versionPublished;
        this.qualityThreshold = qualityThreshold;
        this.initialReviewWeight = initialReviewWeight;
        this.rereviewWeight = rereviewWeight;
    }

    public String getProjectId() { return projectId; }
    public String getHospitalId() { return hospitalId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Set<MedicalRecordType> getMedicalRecordTypes() { return medicalRecordTypes; }
    public ProjectStatus getStatus() { return status; }
    public String getBoundScoreSheetId() { return boundScoreSheetId; }
    public int getVersionNo() { return versionNo; }
    public boolean isVersionPublished() { return versionPublished; }
    public BigDecimal getQualityThreshold() { return qualityThreshold; }
    public BigDecimal getInitialReviewWeight() { return initialReviewWeight; }
    public BigDecimal getRereviewWeight() { return rereviewWeight; }
}
