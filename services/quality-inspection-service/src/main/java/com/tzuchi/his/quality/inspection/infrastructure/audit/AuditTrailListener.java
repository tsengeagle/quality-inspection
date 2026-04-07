package com.tzuchi.his.quality.inspection.infrastructure.audit;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listens for all DomainEvent subclasses and persists them to the audit_trail table.
 * Runs synchronously within the same transaction as the publishing use case.
 */
@Component
public class AuditTrailListener {

    private static final Logger log = LoggerFactory.getLogger(AuditTrailListener.class);

    private final AuditTrailJpaRepository repository;

    public AuditTrailListener(AuditTrailJpaRepository repository) {
        this.repository = repository;
    }

    @EventListener
    public void handle(DomainEvent event) {
        AuditTrailEntity entity = new AuditTrailEntity();
        entity.setId(event.getEventId());
        entity.setEntityType(event.getEntityType());
        entity.setEntityId(event.getEntityId());
        entity.setAction(event.getAction());
        entity.setActorUserId(event.getActorUserId());
        entity.setActorUserName(event.getActorUserName());
        entity.setHospitalId(event.getHospitalId());
        entity.setOccurredAt(event.getOccurredAt());
        entity.setDetail(event.getDetail());
        repository.save(entity);

        log.debug("Audit trail recorded: {} {} {} by {}",
                event.getEntityType(), event.getEntityId(),
                event.getAction(), event.getActorUserId());
    }
}
