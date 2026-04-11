package com.tzuchi.his.quality.inspection.application.audit;

import com.tzuchi.his.quality.inspection.application.common.Page;

import java.util.List;

/**
 * Read-model query port for audit trail entries.
 * Implementations are in the infrastructure layer.
 */
public interface AuditTrailQueryPort {
    Page<AuditTrailEntry> findAuditTrails(AuditTrailQueryCriteria criteria);
    List<AuditTrailEntry> findLifecycleByEntityId(String entityType, String entityId);
}
