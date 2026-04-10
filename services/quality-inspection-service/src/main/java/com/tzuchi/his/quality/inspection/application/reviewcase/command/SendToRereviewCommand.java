package com.tzuchi.his.quality.inspection.application.reviewcase.command;

import javax.validation.constraints.NotBlank;

public class SendToRereviewCommand {

    @NotBlank
    private String caseId;

    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }
}
