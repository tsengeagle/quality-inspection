package com.tzuchi.his.quality.inspection.domain.reviewcase.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

import java.math.BigDecimal;

/**
 * 案件進入複審（domain-events.md §6.5，D-069 第 4 點）。固定初審時分數大於品質線自動進入，
 * 或管理者手工送複審；手工時 triggeredBySubmissionId、qualityThreshold 是 null。
 * action 與 detail 與改名前逐字相同（稽核紀錄不變）。
 */
public class ReviewCaseEscalatedToRereviewEvent extends DomainEvent {

    private final String projectId;
    private final String triggeredBySubmissionId;
    private final BigDecimal initialScore;
    private final BigDecimal qualityThreshold;

    private ReviewCaseEscalatedToRereviewEvent(Object source, String actorUserId, String actorUserName,
                                               String hospitalId, String caseId, String projectId,
                                               String triggeredBySubmissionId, BigDecimal initialScore,
                                               BigDecimal qualityThreshold, String detail) {
        super(source, actorUserId, actorUserName, hospitalId, "ReviewCase", caseId, "CASE_ESCALATED_TO_REREVIEW", detail);
        this.projectId = projectId;
        this.triggeredBySubmissionId = triggeredBySubmissionId;
        this.initialScore = initialScore;
        this.qualityThreshold = qualityThreshold;
    }

    /** 固定初審時，被固定那份提交的分數大於品質線（D-054 第 3 點）。 */
    public static ReviewCaseEscalatedToRereviewEvent onFix(Object source, String actorUserId, String actorUserName,
                                                           String hospitalId, String caseId, String projectId,
                                                           String fixedSubmissionId, BigDecimal initialScore,
                                                           BigDecimal qualityThreshold) {
        return new ReviewCaseEscalatedToRereviewEvent(source, actorUserId, actorUserName, hospitalId, caseId,
                projectId, fixedSubmissionId, initialScore, qualityThreshold,
                String.format("{\"score\":%s,\"threshold\":%s}", initialScore, qualityThreshold));
    }

    /** 管理者手工送複審。 */
    public static ReviewCaseEscalatedToRereviewEvent manual(Object source, String actorUserId, String actorUserName,
                                                            String hospitalId, String caseId, String projectId,
                                                            BigDecimal initialScore) {
        return new ReviewCaseEscalatedToRereviewEvent(source, actorUserId, actorUserName, hospitalId, caseId,
                projectId, null, initialScore, null, "{\"trigger\":\"manual\"}");
    }

    public String getCaseId() { return getEntityId(); }
    public String getProjectId() { return projectId; }
    public String getTriggeredBySubmissionId() { return triggeredBySubmissionId; }
    public BigDecimal getInitialScore() { return initialScore; }
    public BigDecimal getQualityThreshold() { return qualityThreshold; }
}
