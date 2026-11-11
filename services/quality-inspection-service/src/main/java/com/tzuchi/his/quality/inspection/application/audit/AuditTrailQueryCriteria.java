package com.tzuchi.his.quality.inspection.application.audit;

import java.time.Instant;

public class AuditTrailQueryCriteria {

    private final String aggregateType;
    private final String aggregateId;
    private final String hospitalId;
    private final String eventName;
    private final Instant from;
    private final Instant to;
    private final int page;
    private final int size;

    public AuditTrailQueryCriteria(String aggregateType, String aggregateId,
                                    String hospitalId, String eventName,
                                    Instant from, Instant to, int page, int size) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.hospitalId = hospitalId;
        this.eventName = eventName;
        this.from = from;
        this.to = to;
        this.page = Math.max(0, page);
        this.size = size > 0 ? Math.min(size, 100) : 20;
    }

    public AuditTrailQueryCriteria withHospitalId(String hospitalId) {
        return new AuditTrailQueryCriteria(aggregateType, aggregateId, hospitalId, eventName, from, to, page, size);
    }

    public String getAggregateType() { return aggregateType; }
    public String getAggregateId() { return aggregateId; }
    public String getHospitalId() { return hospitalId; }
    public String getEventName() { return eventName; }
    public Instant getFrom() { return from; }
    public Instant getTo() { return to; }
    public int getPage() { return page; }
    public int getSize() { return size; }
}
