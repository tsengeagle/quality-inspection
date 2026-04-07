package com.tzuchi.his.quality.inspection.infrastructure.audit;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditTrailJpaRepository extends JpaRepository<AuditTrailEntity, String> {
}
