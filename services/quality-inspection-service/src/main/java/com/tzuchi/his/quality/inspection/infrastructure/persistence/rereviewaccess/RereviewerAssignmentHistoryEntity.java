package com.tzuchi.his.quality.inspection.infrastructure.persistence.rereviewaccess;

import javax.persistence.*;
import java.time.Instant;

/** D-055 第 1 點：複審指派歷史。只能新增：所有欄位 updatable = false，沒有 setter。 */
@Entity
@Table(name = "rereviewer_assignment_history")
public class RereviewerAssignmentHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long historyId;

    @Column(name = "case_id", nullable = false, updatable = false)
    private String caseId;

    @Column(name = "rereviewer_id", nullable = false, updatable = false)
    private String rereviewerId;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt;

    @Column(name = "assigned_by", nullable = false, updatable = false)
    private String assignedBy;

    @Column(name = "assignment_kind", nullable = false, updatable = false, length = 16)
    private String kind;

    protected RereviewerAssignmentHistoryEntity() {
    }

    public RereviewerAssignmentHistoryEntity(String caseId, String rereviewerId, Instant assignedAt,
                                             String assignedBy, String kind) {
        this.caseId = caseId;
        this.rereviewerId = rereviewerId;
        this.assignedAt = assignedAt;
        this.assignedBy = assignedBy;
        this.kind = kind;
    }

    public Long getHistoryId() { return historyId; }
    public String getCaseId() { return caseId; }
    public String getRereviewerId() { return rereviewerId; }
    public Instant getAssignedAt() { return assignedAt; }
    public String getAssignedBy() { return assignedBy; }
    public String getKind() { return kind; }
}
