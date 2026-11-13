package com.tzuchi.his.quality.harness.samples.layering.infrastructure;

import com.tzuchi.his.quality.harness.samples.layering.interfaces.InterfacesTarget;

/** 違規樣本：infrastructure 依賴 interfaces。 */
public class InfrastructureUsesInterfaces {
    InterfacesTarget target = new InterfacesTarget();
}
