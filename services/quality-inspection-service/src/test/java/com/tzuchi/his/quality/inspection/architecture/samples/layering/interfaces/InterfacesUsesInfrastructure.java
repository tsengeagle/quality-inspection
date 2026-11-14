package com.tzuchi.his.quality.inspection.architecture.samples.layering.interfaces;

import com.tzuchi.his.quality.inspection.architecture.samples.layering.infrastructure.InfrastructureTarget;

/** 違規樣本：interfaces 依賴 infrastructure。 */
public class InterfacesUsesInfrastructure {
    InfrastructureTarget target = new InfrastructureTarget();
}
