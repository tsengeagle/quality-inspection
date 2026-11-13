package com.tzuchi.his.quality.harness.samples.layering.application;

import com.tzuchi.his.quality.harness.samples.layering.interfaces.InterfacesTarget;

/** 違規樣本：application 依賴 interfaces。 */
public class ApplicationUsesInterfaces {
    InterfacesTarget target = new InterfacesTarget();
}
