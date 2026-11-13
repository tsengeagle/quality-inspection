package com.tzuchi.his.quality.harness.samples.layering.interfaces;

import com.tzuchi.his.quality.harness.samples.layering.application.ApplicationTarget;

/** 合規對照：interfaces 依賴 application 是允許的方向。 */
public class AllowedInterfacesUsesApplication {
    ApplicationTarget target = new ApplicationTarget();
}
