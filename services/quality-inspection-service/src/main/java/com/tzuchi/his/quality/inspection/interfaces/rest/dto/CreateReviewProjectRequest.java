package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import java.math.BigDecimal;
import java.util.Set;

public class CreateReviewProjectRequest {

    private String name;

    private String description;

    private Set<String> medicalRecordTypes;

    // D-046, D-047: optional in a draft; validated by the domain (threshold) and at publish (weights)
    private BigDecimal qualityThreshold;
    private BigDecimal initialReviewWeight;
    private BigDecimal rereviewWeight;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Set<String> getMedicalRecordTypes() { return medicalRecordTypes; }
    public void setMedicalRecordTypes(Set<String> medicalRecordTypes) { this.medicalRecordTypes = medicalRecordTypes; }

    public BigDecimal getQualityThreshold() { return qualityThreshold; }
    public void setQualityThreshold(BigDecimal qualityThreshold) { this.qualityThreshold = qualityThreshold; }

    public BigDecimal getInitialReviewWeight() { return initialReviewWeight; }
    public void setInitialReviewWeight(BigDecimal initialReviewWeight) { this.initialReviewWeight = initialReviewWeight; }

    public BigDecimal getRereviewWeight() { return rereviewWeight; }
    public void setRereviewWeight(BigDecimal rereviewWeight) { this.rereviewWeight = rereviewWeight; }
}
