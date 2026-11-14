package com.tzuchi.his.quality.inspection.architecture.samples.layering.reporting;

import com.tzuchi.his.quality.inspection.architecture.samples.layering.infrastructure.InfrastructureTarget;

/** 違規樣本：reporting 依賴 infrastructure。 */
public class ReportingUsesInfrastructure {
    InfrastructureTarget target = new InfrastructureTarget();
}
