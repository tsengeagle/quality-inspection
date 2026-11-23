package com.tzuchi.his.quality.inspection.application.reviewcase.command;

import javax.validation.constraints.NotBlank;
import java.time.Instant;

public class ReassignRereviewerCommand {

    @NotBlank
    private String caseId;
    @NotBlank
    private String rereviewerId;
    private Instant dueAt;

    public String getCaseId() { return caseId; }
    public void setCaseId(String caseId) { this.caseId = caseId; }

    public String getRereviewerId() { return rereviewerId; }
    public void setRereviewerId(String rereviewerId) { this.rereviewerId = rereviewerId; }

    public Instant getDueAt() { return dueAt; }
    public void setDueAt(Instant dueAt) { this.dueAt = dueAt; }
}
