package com.tzuchi.his.quality.harness.samples.clock.application;

import java.time.LocalDateTime;

/** 違規樣本：application 直接取系統時間。 */
public class ApplicationReadsLocalDateTimeNow {
    LocalDateTime read() {
        return LocalDateTime.now();
    }
}
