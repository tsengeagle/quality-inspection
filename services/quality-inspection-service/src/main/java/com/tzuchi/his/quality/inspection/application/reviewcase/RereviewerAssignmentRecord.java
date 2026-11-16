package com.tzuchi.his.quality.inspection.application.reviewcase;

import java.time.Instant;
import java.util.Objects;

/** D-055 第 1 點：一次複審指派或改派（案件、複審醫師、指派時間、操作者、類別）。 */
public final class RereviewerAssignmentRecord {

    public enum Kind { ASSIGN, REASSIGN }

    private final String caseId;
    private final String rereviewerId;
    private final Instant assignedAt;
    private final String assignedBy;
    private final Kind kind;

    public RereviewerAssignmentRecord(String caseId, String rereviewerId, Instant assignedAt, String assignedBy, Kind kind) {
        this.caseId = Objects.requireNonNull(caseId, "caseId");
        this.rereviewerId = Objects.requireNonNull(rereviewerId, "rereviewerId");
        this.assignedAt = Objects.requireNonNull(assignedAt, "assignedAt");
        this.assignedBy = Objects.requireNonNull(assignedBy, "assignedBy");
        this.kind = Objects.requireNonNull(kind, "kind");
    }

    public String getCaseId() { return caseId; }
    public String getRereviewerId() { return rereviewerId; }
    public Instant getAssignedAt() { return assignedAt; }
    public String getAssignedBy() { return assignedBy; }
    public Kind getKind() { return kind; }
}
