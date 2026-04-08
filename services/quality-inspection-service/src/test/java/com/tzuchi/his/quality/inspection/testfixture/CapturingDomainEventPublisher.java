package com.tzuchi.his.quality.inspection.testfixture;

import com.tzuchi.his.quality.inspection.application.common.DomainEventPublisher;
import com.tzuchi.his.quality.inspection.domain.common.DomainEvent;

import java.util.ArrayList;
import java.util.List;

public class CapturingDomainEventPublisher implements DomainEventPublisher {
    public final List<DomainEvent> events = new ArrayList<>();

    @Override
    public void publish(DomainEvent event) {
        events.add(event);
    }
}
