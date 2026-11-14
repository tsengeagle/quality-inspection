package com.tzuchi.his.quality.inspection.architecture.samples.layering.interfaces;

import java.util.List; import com.tzuchi.his.quality.inspection.architecture.samples.layering.infrastructure.InfrastructureTarget;

/** 違規樣本：違規的 import 接在另一個 import 後面，寫在同一行。 */
public class InterfacesUsesInfrastructureSameLineImport {
    List<String> names = List.of(InfrastructureTarget.NAME);
}
