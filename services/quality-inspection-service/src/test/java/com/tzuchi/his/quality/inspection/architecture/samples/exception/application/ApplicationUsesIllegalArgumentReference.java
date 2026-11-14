package com.tzuchi.his.quality.inspection.architecture.samples.exception.application;

import java.util.function.Function;

/** 違規樣本：以建構子參考建立 IllegalArgumentException。 */
public class ApplicationUsesIllegalArgumentReference {
    Function<String, RuntimeException> factory = IllegalArgumentException::new;
}
