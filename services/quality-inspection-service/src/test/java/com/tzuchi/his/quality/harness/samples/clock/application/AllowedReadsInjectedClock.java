package com.tzuchi.his.quality.harness.samples.clock.application;

import java.time.Clock;
import java.time.Instant;

/** 合規對照：時間來自外部傳入的 Clock。 */
public class AllowedReadsInjectedClock {
    Instant read(Clock clock) {
        return Instant.now(clock);
    }
}
