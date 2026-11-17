package com.tzuchi.his.quality.inspection.reporting;

import java.time.Instant;

public class ExportCriteria {

    private final String reportType;
    private final String hospitalId;
    private final Instant from;
    private final Instant to;
    private final int page;
    private final int size;

    /** D-057 第 3 點：分頁的預設值與上限。非正值比照現有列表 API 正規化，不回錯誤。 */
    public static final int DEFAULT_SIZE = 100;
    public static final int MAX_SIZE = 1000;

    public ExportCriteria(String reportType, String hospitalId, Instant from, Instant to, int page, int size) {
        this.reportType = reportType;
        this.hospitalId = hospitalId;
        this.from = from;
        this.to = to;
        this.page = Math.max(0, page);
        this.size = size > 0 ? Math.min(size, MAX_SIZE) : DEFAULT_SIZE;
    }

    public String getReportType() { return reportType; }
    public String getHospitalId() { return hospitalId; }
    public Instant getFrom() { return from; }
    public Instant getTo() { return to; }
    public int getPage() { return page; }
    public int getSize() { return size; }
}
