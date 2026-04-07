package com.tzuchi.his.quality.inspection.application.common;

import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

/**
 * Port for publishing domain events from application services.
 * Implementations are in the infrastructure layer.
 */
public interface DomainEventPublisher {
    void publish(DomainEvent event);
}
