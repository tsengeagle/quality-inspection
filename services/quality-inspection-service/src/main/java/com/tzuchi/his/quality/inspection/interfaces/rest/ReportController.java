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
        // D-057 第 4 點：from、to 選填；只給其中一個，或 from 不早於 to，回 400
        if ((from == null) != (to == null)) {
            throw new BadRequestException("from and to must be given together");
        }
        if (from != null) {
            requireOrderedRange(from, to);
        }
        DashboardQueryCriteria criteria = new DashboardQueryCriteria(hospitalId, projectId, from, to);
        DashboardSummary result = dashboardQueryService.getSummary(actor, criteria);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/export")
    public ResponseEntity<ApiResponse<ExportResult>> exportReport(
            @CurrentActor ActorContext actor,
            @Valid @RequestBody ExportRequest request) {
        // D-057 第 2 點：from、to 必填，from 必須早於 to（含起不含迄）
        if (request.getFrom() == null || request.getTo() == null) {
            throw new BadRequestException("from and to are required");
        }
        requireOrderedRange(request.getFrom(), request.getTo());
        ExportCriteria criteria = new ExportCriteria(
                request.getReportType(), request.getHospitalId(),
                request.getFrom(), request.getTo(),
                request.getPage() == null ? 0 : request.getPage(),
                request.getSize() == null ? ExportCriteria.DEFAULT_SIZE : request.getSize());
        ExportResult result = exportQueryService.export(actor, criteria);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    private static void requireOrderedRange(Instant from, Instant to) {
        if (!from.isBefore(to)) {
            throw new BadRequestException("from must be earlier than to");
        }
    }
}
