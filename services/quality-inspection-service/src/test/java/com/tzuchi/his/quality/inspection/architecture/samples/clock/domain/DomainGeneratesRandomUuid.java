package com.tzuchi.his.quality.inspection.architecture.samples.clock.domain;

import java.util.UUID;

/** 違規樣本：domain 直接產生亂數 id。 */
public class DomainGeneratesRandomUuid {
    UUID generate() {
        return UUID.randomUUID();
    }
}
