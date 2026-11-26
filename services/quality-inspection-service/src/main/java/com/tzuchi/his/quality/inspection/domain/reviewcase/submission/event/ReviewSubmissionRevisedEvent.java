package com.tzuchi.his.quality.inspection.domain.reviewcase.submission.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.time.Instant;

/**
 * 已提交的初審或複審被修正（domain-events.md §8.1）。修正初審與修正複審都發出（D-083 第 3 點）。
 * 稽核 detail 不帶分數，格式 {"caseId","oldSubmissionId","stage","revisionNo"}，以 JSON 序列化產生（D-083 第 5 點）。
 */
public class ReviewSubmissionRevisedEvent extends DomainEvent {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final String caseId;
    private final String oldSubmissionId;
    private final String stage;
    private final int revisionNo;

    public ReviewSubmissionRevisedEvent(Object source, String eventId, Instant occurredAt, String actorUserId, String actorUserName,
                                         String hospitalId, String caseId,
                                         String oldSubmissionId, String newSubmissionId,
                                         String stage, int revisionNo) {
        super(source, eventId, occurredAt, actorUserId, actorUserName,
              hospitalId, "ReviewSubmission", newSubmissionId, "REVISED",
              detail(caseId, oldSubmissionId, stage, revisionNo));
        this.caseId = caseId;
        this.oldSubmissionId = oldSubmissionId;
        this.stage = stage;
        this.revisionNo = revisionNo;
    }

    private static String detail(String caseId, String oldSubmissionId, String stage, int revisionNo) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("caseId", caseId);
        d.put("oldSubmissionId", oldSubmissionId);
        d.put("stage", stage);
        d.put("revisionNo", revisionNo);
        try {
            return JSON.writeValueAsString(d);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    public String getCaseId() { return caseId; }
    public String getOldSubmissionId() { return oldSubmissionId; }
    public String getNewSubmissionId() { return getEntityId(); }
    public String getStage() { return stage; }
    public int getRevisionNo() { return revisionNo; }
}
