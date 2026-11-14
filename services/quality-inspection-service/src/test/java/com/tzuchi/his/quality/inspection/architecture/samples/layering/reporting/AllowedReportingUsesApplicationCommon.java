package com.tzuchi.his.quality.inspection.architecture.samples.layering.reporting;

import com.tzuchi.his.quality.inspection.architecture.samples.layering.application.common.ActorTarget;

/** 合規對照（ARCH-05）：reporting 只依賴 application.common。 */
public class AllowedReportingUsesApplicationCommon {
    ActorTarget actor = new ActorTarget();
}
