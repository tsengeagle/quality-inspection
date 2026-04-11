package com.tzuchi.his.quality.inspection.interfaces.rest;

import com.tzuchi.his.quality.inspection.application.audit.AuditTrailEntry;
import com.tzuchi.his.quality.inspection.application.audit.AuditTrailQueryCriteria;
import com.tzuchi.his.quality.inspection.application.audit.ListAuditTrailsUseCase;
import com.tzuchi.his.quality.inspection.application.common.ActorContext;
import com.tzuchi.his.quality.inspection.application.common.Page;
import com.tzuchi.his.quality.inspection.interfaces.rest.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/audit-trails")
public class AuditController {

    private final ListAuditTrailsUseCase listAuditTrailsUseCase;

    public AuditController(ListAuditTrailsUseCase listAuditTrailsUseCase) {
        this.listAuditTrailsUseCase = listAuditTrailsUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AuditTrailEntry>>> listAuditTrails(
            @CurrentActor ActorContext actor,
            @RequestParam(required = false) String aggregateType,
            @RequestParam(required = false) String aggregateId,
            @RequestParam(required = false) String hospitalId,
            @RequestParam(required = false) String eventName,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        AuditTrailQueryCriteria criteria = new AuditTrailQueryCriteria(
                aggregateType, aggregateId, hospitalId, eventName, from, to, page, size);
        Page<AuditTrailEntry> result = listAuditTrailsUseCase.execute(actor, criteria);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
