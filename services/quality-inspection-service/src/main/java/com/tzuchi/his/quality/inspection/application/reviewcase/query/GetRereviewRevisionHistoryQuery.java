package com.tzuchi.his.quality.inspection.application.reviewcase.query;

import javax.validation.constraints.NotBlank;

public class GetRereviewRevisionHistoryQuery {
    @NotBlank
    private String caseId;

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }
}
