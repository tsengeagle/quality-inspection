package com.tzuchi.his.quality.inspection.architecture.samples.clock.application;

/** 違規樣本：application 直接取系統時間。 */
public class ApplicationReadsCurrentTimeMillis {
    long read() {
        return System.currentTimeMillis();
    }
}
