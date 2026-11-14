package com.tzuchi.his.quality.inspection.architecture.samples.layering.interfaces;

import
        com.tzuchi.his.quality.inspection.architecture.samples.layering.infrastructure
        .InfrastructureTarget;

/** 違規樣本：import 敘述跨行，而且只引用編譯期常數。 */
public class InterfacesUsesInfrastructureMultilineImport {
    String name = InfrastructureTarget.NAME;
}
