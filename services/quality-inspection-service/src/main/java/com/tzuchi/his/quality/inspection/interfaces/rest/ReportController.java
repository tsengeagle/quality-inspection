package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.interfaces.rest.dto.ApiResponse;
import com.tzuchi.his.quality.inspection.interfaces.rest.dto.ExportRequest;
import com.tzuchi.his.quality.inspection.reporting.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.Instant;
import com.tzuchi.his.quality.inspection.reporting.DashboardSummary;
import com.tzuchi.his.quality.inspection.reporting.ExportReportUseCase;
import com.tzuchi.his.quality.inspection.reporting.ExportResult;
import com.tzuchi.his.quality.inspection.reporting.GetDashboardSummaryUseCase;

@RestController
@RequestMapping("/reports")
public class ReportController {

    private final GetDashboardSummaryUseCase dashboardSummaryUseCase;
    private final ExportReportUseCase exportReportUseCase;

    public ReportController(GetDashboardSummaryUseCase dashboardSummaryUseCase,
                             ExportReportUseCase exportReportUseCase) {
        this.dashboardSummaryUseCase = dashboardSummaryUseCase;
        this.exportReportUseCase = exportReportUseCase;
    }

    /** from、to 的檢查在 use case（D-087 第 2 點）：角色 → 輸入。 */
    @GetMapping("/dashboard-summary")
    public ResponseEntity<ApiResponse<DashboardSummary>> getDashboardSummary(
            @CurrentActor ActorContext actor,
            @RequestParam(required = false) String hospitalId,
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        DashboardSummary result = dashboardSummaryUseCase.execute(actor, hospitalId, projectId, from, to);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/export")
    public ResponseEntity<ApiResponse<ExportResult>> exportReport(
            @CurrentActor ActorContext actor,
            @Valid @RequestBody ExportRequest request) {
        ExportResult result = exportReportUseCase.execute(actor,
                request.getReportType(), request.getHospitalId(),
                request.getFrom(), request.getTo(), request.getPage(), request.getSize());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
