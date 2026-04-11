package com.tzuchi.his.quality.inspection.reporting;

import java.util.List;

public class ExportResult {

    private final String reportType;
    private final List<ExportRow> rows;
    private final DashboardSummary summary;

    public ExportResult(String reportType, List<ExportRow> rows, DashboardSummary summary) {
        this.reportType = reportType;
        this.rows = rows;
        this.summary = summary;
    }

    public String getReportType() { return reportType; }
    public List<ExportRow> getRows() { return rows; }
    public DashboardSummary getSummary() { return summary; }
}
