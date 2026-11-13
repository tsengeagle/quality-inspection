package com.tzuchi.his.quality.harness.samples.clock.domain;

import java.time.Instant;

/** 違規樣本：domain 直接取系統時間。 */
public class DomainReadsInstantNow {
    Instant read() {
        return Instant.now();
    }
}
