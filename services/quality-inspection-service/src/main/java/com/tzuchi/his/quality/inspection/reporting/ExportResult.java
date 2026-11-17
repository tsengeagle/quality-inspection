package com.tzuchi.his.quality.inspection.reporting;

import java.util.List;

public class ExportResult {

    private final String reportType;
    private final List<ExportRow> rows;
    private final DashboardSummary summary;
    /** D-057 第 3 點：rows 為當頁內容；totalElements、totalPages 以整個過濾範圍計算。 */
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;

    public ExportResult(String reportType, List<ExportRow> rows, DashboardSummary summary,
                        int page, int size, long totalElements, int totalPages) {
        this.reportType = reportType;
        this.rows = rows;
        this.summary = summary;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    public String getReportType() { return reportType; }
    public List<ExportRow> getRows() { return rows; }
    public DashboardSummary getSummary() { return summary; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public long getTotalElements() { return totalElements; }
    public int getTotalPages() { return totalPages; }
}
