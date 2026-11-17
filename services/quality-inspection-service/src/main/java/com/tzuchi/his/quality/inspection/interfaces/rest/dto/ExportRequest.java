package com.tzuchi.his.quality.inspection.interfaces.rest.dto;

import javax.validation.constraints.NotBlank;
import java.time.Instant;

public class ExportRequest {

    @NotBlank
    private String reportType;
    private String hospitalId;
    private Instant from;
    private Instant to;
    /** D-057 第 3 點：0 起算，未給為 0。 */
    private Integer page;
    /** D-057 第 3 點：未給為 100，上限 1000。 */
    private Integer size;

    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }
    public String getHospitalId() { return hospitalId; }
    public void setHospitalId(String hospitalId) { this.hospitalId = hospitalId; }
    public Instant getFrom() { return from; }
    public void setFrom(Instant from) { this.from = from; }
    public Instant getTo() { return to; }
    public void setTo(Instant to) { this.to = to; }
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }
}
