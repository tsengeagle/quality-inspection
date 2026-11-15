package com.tzuchi.his.quality.inspection.infrastructure.time;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * 系統時鐘。domain 與 application 不得直接取系統時間（ARCH-02），改由這裡提供的 {@link Clock} 注入；
 * 測試可傳入固定的 Clock。
 */
@Configuration
public class SystemClockConfig {

    @Bean
    public Clock systemClock() {
        return Clock.systemDefaultZone();
    }
}
