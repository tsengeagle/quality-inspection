package com.tzuchi.his.quality.harness.samples.layering.application;

import com.tzuchi.his.quality.harness.samples.layering.infrastructure.InfrastructureTarget;

/** 違規樣本：application 依賴 infrastructure。 */
public class ApplicationUsesInfrastructure {
    InfrastructureTarget target = new InfrastructureTarget();
}
