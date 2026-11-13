package com.tzuchi.his.quality.inspection.architecture.samples.clock.application;

import java.util.UUID;
import java.util.function.Supplier;

/** 違規樣本：以方法參考取得亂數 id。 */
public class ApplicationUsesRandomUuidReference {
    Supplier<UUID> ids = UUID::randomUUID;
}
