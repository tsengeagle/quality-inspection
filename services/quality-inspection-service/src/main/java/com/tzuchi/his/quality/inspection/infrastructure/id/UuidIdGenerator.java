package com.tzuchi.his.quality.inspection.infrastructure.id;

import com.tzuchi.his.quality.inspection.application.common.IdGenerator;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Random UUIDs as identifiers. */
@Component
public class UuidIdGenerator implements IdGenerator {

    @Override
    public String newId() {
        return UUID.randomUUID().toString();
    }
}
