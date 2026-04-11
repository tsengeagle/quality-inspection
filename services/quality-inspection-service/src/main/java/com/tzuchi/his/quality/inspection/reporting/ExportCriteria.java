package com.tzuchi.his.quality.inspection.reporting;

import java.time.Instant;

public class ExportCriteria {

    private final String reportType;
    private final String hospitalId;
    private final Instant from;
    private final Instant to;

    public ExportCriteria(String reportType, String hospitalId, Instant from, Instant to) {
        this.reportType = reportType;
        this.hospitalId = hospitalId;
        this.from = from;
        this.to = to;
    }

    public String getReportType() { return reportType; }
    public String getHospitalId() { return hospitalId; }
    public Instant getFrom() { return from; }
    public Instant getTo() { return to; }
}
