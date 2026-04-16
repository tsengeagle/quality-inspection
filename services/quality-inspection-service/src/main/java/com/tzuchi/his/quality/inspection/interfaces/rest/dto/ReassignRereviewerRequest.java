package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class ReassignRereviewerRequest {
    @NotBlank
    private String rereviewerId;

    @NotNull(message = "dueAt must not be null")
    private LocalDateTime dueAt;

    public String getRereviewerId() { return rereviewerId; }
    public void setRereviewerId(String rereviewerId) { this.rereviewerId = rereviewerId; }
    public LocalDateTime getDueAt() { return dueAt; }
    public void setDueAt(LocalDateTime dueAt) { this.dueAt = dueAt; }
}
