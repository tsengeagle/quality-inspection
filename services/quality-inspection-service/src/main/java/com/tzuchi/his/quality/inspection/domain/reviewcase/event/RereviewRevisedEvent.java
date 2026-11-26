package com.tzuchi.his.quality.inspection.domain.reviewcase.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.time.Instant;

/**
 * 已提交、還沒結案的複審被修正（domain-events.md §8.2，D-083 第 5 點）。
 * finalScore 可反推初審分數：稽核 detail 依 D-071 遮罩。detail 格式不變，以 JSON 序列化產生。
 */
public class RereviewRevisedEvent extends DomainEvent {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final String oldSubmissionId;
    private final String newSubmissionId;
    private final BigDecimal rereviewScore;
    private final BigDecimal finalScore;

    public RereviewRevisedEvent(Object source, String eventId, Instant occurredAt,
                                 String actorUserId,
                                 String actorUserName,
                                 String hospitalId,
                                 String caseId,
                                 String oldSubmissionId,
                                 String newSubmissionId,
                                 BigDecimal rereviewScore,
                                 BigDecimal finalScore) {
        super(source, eventId, occurredAt, actorUserId, actorUserName, hospitalId, "ReviewCase", caseId, "REREVIEW_REVISED",
                detail(newSubmissionId, oldSubmissionId, rereviewScore, finalScore));
        this.oldSubmissionId = oldSubmissionId;
        this.newSubmissionId = newSubmissionId;
        this.rereviewScore = rereviewScore;
        this.finalScore = finalScore;
    }

    private static String detail(String newSubmissionId, String oldSubmissionId,
                                 BigDecimal rereviewScore, BigDecimal finalScore) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("newSubmissionId", newSubmissionId);
        d.put("oldSubmissionId", oldSubmissionId);
        d.put("rereviewScore", rereviewScore);
        d.put("finalScore", finalScore);
        try {
            return JSON.writeValueAsString(d);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    public String getCaseId() { return getEntityId(); }
    public String getOldSubmissionId() { return oldSubmissionId; }
    public String getNewSubmissionId() { return newSubmissionId; }
    public BigDecimal getRereviewScore() { return rereviewScore; }
    public BigDecimal getFinalScore() { return finalScore; }
}
