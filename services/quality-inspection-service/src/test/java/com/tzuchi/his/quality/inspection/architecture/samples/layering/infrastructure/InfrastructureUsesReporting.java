package com.tzuchi.his.quality.inspection.architecture.samples.layering.infrastructure;

import com.tzuchi.his.quality.inspection.architecture.samples.layering.reporting.ReportingTarget;

/** 違規樣本：infrastructure 依賴 reporting。 */
public class InfrastructureUsesReporting {
    ReportingTarget target = new ReportingTarget();
}
