package com.tzuchi.his.quality.harness.samples.exception.domain;

/** 違規樣本：domain 丟 IllegalArgumentException。 */
public class DomainThrowsIllegalArgument {
    void reject() {
        throw new IllegalArgumentException("sample");
    }
}
