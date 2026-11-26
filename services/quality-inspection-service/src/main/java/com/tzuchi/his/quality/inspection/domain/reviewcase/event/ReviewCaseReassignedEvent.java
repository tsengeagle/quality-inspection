package com.tzuchi.his.quality.inspection.domain.reviewcase.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Published when a case reviewer is changed (reassigned)（domain-events.md §6.15，D-082 第 6 點）.
 */
public class ReviewCaseReassignedEvent extends DomainEvent {

    // detail 以 JSON 序列化產生；dueAt 輸出 UTC（憲法第一條）。
    private static final ObjectMapper JSON = new ObjectMapper();

    private final String previousReviewerId;
    private final String newReviewerId;
    private final Instant dueAt;

    public ReviewCaseReassignedEvent(Object source, String eventId, Instant occurredAt, String actorUserId, String actorUserName,
                                      String hospitalId, String caseId,
                                      String previousReviewerId, String newReviewerId,
                                      Instant dueAt) {
        super(source, eventId, occurredAt, actorUserId, actorUserName,
              hospitalId, "ReviewCase", caseId, "REASSIGNED",
              detail(previousReviewerId, newReviewerId, dueAt));
        this.previousReviewerId = previousReviewerId;
        this.newReviewerId = newReviewerId;
        this.dueAt = dueAt;
    }

    private static String detail(String previousReviewerId, String newReviewerId, Instant dueAt) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("previousReviewerId", previousReviewerId);
        detail.put("newReviewerId", newReviewerId);
        detail.put("dueAt", dueAt == null ? null : dueAt.toString());
        try {
            return JSON.writeValueAsString(detail);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    public String getCaseId() { return getEntityId(); }
    public String getPreviousReviewerId() { return previousReviewerId; }
    public String getNewReviewerId() { return newReviewerId; }
    public Instant getDueAt() { return dueAt; }
}
