package com.tzuchi.his.quality.inspection.architecture.samples.layering.reporting;

import com.tzuchi.his.quality.inspection.architecture.samples.layering.application.ApplicationTarget;

/** 違規樣本（ARCH-05）：reporting 依賴 application.common 以外的 application 類別。 */
public class ReportingCallsUseCase {
    ApplicationTarget useCase = new ApplicationTarget();
}
