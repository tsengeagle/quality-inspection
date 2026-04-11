package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.interfaces.rest.dto.ApiResponse;
import com.tzuchi.his.quality.inspection.interfaces.rest.dto.ExportRequest;
import com.tzuchi.his.quality.inspection.reporting.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.Instant;

@RestController
@RequestMapping("/reports")
public class ReportController {

    private final DashboardQueryService dashboardQueryService;
    private final ExportQueryService exportQueryService;

    public ReportController(DashboardQueryService dashboardQueryService,
                             ExportQueryService exportQueryService) {
        this.dashboardQueryService = dashboardQueryService;
        this.exportQueryService = exportQueryService;
    }

    @GetMapping("/dashboard-summary")
    public ResponseEntity<ApiResponse<DashboardSummary>> getDashboardSummary(
            @CurrentActor ActorContext actor,
            @RequestParam(required = false) String hospitalId,
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        DashboardQueryCriteria criteria = new DashboardQueryCriteria(hospitalId, projectId, from, to);
        DashboardSummary result = dashboardQueryService.getSummary(actor, criteria);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/export")
    public ResponseEntity<ApiResponse<ExportResult>> exportReport(
            @CurrentActor ActorContext actor,
            @Valid @RequestBody ExportRequest request) {
        ExportCriteria criteria = new ExportCriteria(
                request.getReportType(), request.getHospitalId(),
                request.getFrom(), request.getTo());
        ExportResult result = exportQueryService.export(actor, criteria);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
