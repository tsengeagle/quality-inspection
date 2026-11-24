package com.tzuchi.his.quality.inspection.domain.reviewcase.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

import java.util.Collections;

/**
 * Published when a case is returned to initial review.
 */
public class ReviewCaseReturnedEvent extends DomainEvent {

    // detail 以 JSON 序列化產生，理由含引號、反斜線、換行仍是合法 JSON（D-080 第 5 點）。
    private static final ObjectMapper JSON = new ObjectMapper();

    private final String reason;

    public ReviewCaseReturnedEvent(Object source, String actorUserId, String actorUserName,
                                    String hospitalId, String caseId, String reason) {
        super(source, actorUserId, actorUserName,
              hospitalId, "ReviewCase", caseId, "RETURNED",
              reasonDetail(reason));
        this.reason = reason;
    }

    private static String reasonDetail(String reason) {
        try {
            return JSON.writeValueAsString(Collections.singletonMap("reason", reason));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    public String getCaseId() { return getEntityId(); }
    public String getReason() { return reason; }
}
