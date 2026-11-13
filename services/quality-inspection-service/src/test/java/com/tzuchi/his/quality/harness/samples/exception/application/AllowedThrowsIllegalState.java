package com.tzuchi.his.quality.harness.samples.exception.application;

/** 合規對照：這條規則不管 IllegalStateException。 */
public class AllowedThrowsIllegalState {
    void reject() {
        throw new IllegalStateException("sample");
    }
}
