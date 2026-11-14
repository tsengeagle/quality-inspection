package com.tzuchi.his.quality.inspection.architecture.samples.layering.interfaces;

import com.tzuchi.his.quality.inspection.architecture.samples.layering.infrastructure.InfrastructureTarget;

/** 違規樣本：只引用 infrastructure 的編譯期常數。位元組碼裡沒有這個依賴，只有 import 看得到。 */
public class InterfacesUsesInfrastructureConstant {
    String name = InfrastructureTarget.NAME;
}
