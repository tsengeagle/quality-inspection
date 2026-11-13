package com.tzuchi.his.quality.harness.samples.layering.domain;

import com.tzuchi.his.quality.harness.samples.layering.application.ApplicationTarget;

/** 違規樣本：domain 依賴 application。 */
public class DomainUsesApplication {
    ApplicationTarget target = new ApplicationTarget();
}
