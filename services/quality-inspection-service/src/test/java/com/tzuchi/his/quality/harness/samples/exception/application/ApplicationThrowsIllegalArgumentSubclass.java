package com.tzuchi.his.quality.harness.samples.exception.application;

/** 違規樣本：application 丟 IllegalArgumentException 的子類別。 */
public class ApplicationThrowsIllegalArgumentSubclass {
    void reject() {
        throw new NumberFormatException("sample");
    }
}
