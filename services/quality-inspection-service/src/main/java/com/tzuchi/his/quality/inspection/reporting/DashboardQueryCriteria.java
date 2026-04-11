package com.tzuchi.his.quality.inspection.reporting;

import java.time.Instant;

public class DashboardQueryCriteria {

    private final String hospitalId;
    private final String projectId;
    private final Instant from;
    private final Instant to;

    public DashboardQueryCriteria(String hospitalId, String projectId, Instant from, Instant to) {
        this.hospitalId = hospitalId;
        this.projectId = projectId;
        this.from = from;
        this.to = to;
    }

    public String getHospitalId() { return hospitalId; }
    public String getProjectId() { return projectId; }
    public Instant getFrom() { return from; }
    public Instant getTo() { return to; }
}
