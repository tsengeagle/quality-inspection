package com.tzuchi.his.quality.inspection.domain.reviewcase.event;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 複審提交完成（domain-events.md §6.8，D-070 第 3 點；D-069 第 4 點由 RereviewSubmittedEvent 改名，action 不變）。
 * finalScore 依 D-012／D-020 加權，可反推初審分數：事件不回給複審醫師，稽核的 detail 對複審醫師遮罩（D-033）。
 */
public class RereviewCompletedEvent extends DomainEvent {

    private final String submissionId;
    private final String rereviewerId;
    private final BigDecimal rereviewScore;
    private final BigDecimal finalScore;

    public RereviewCompletedEvent(Object source, String eventId, Instant occurredAt,
                                  String actorUserId,
                                  String actorUserName,
                                  String hospitalId,
                                  String caseId,
                                  String submissionId,
                                  String rereviewerId,
                                  BigDecimal rereviewScore,
                                  BigDecimal finalScore) {
        super(source, eventId, occurredAt, actorUserId, actorUserName, hospitalId, "ReviewCase", caseId, "REREVIEW_SUBMITTED",
                String.format("{\"rereviewScore\":%s,\"finalScore\":%s}", rereviewScore, finalScore));
        this.submissionId = submissionId;
        this.rereviewerId = rereviewerId;
        this.rereviewScore = rereviewScore;
        this.finalScore = finalScore;
    }

    public String getCaseId() { return getEntityId(); }
    public String getSubmissionId() { return submissionId; }
    public String getRereviewerId() { return rereviewerId; }
    public BigDecimal getRereviewScore() { return rereviewScore; }
    public BigDecimal getFinalScore() { return finalScore; }
}
